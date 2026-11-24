package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadDestination
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.DownloadSnapshot
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameDownloadArgs
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.gameSlug
import dev.jagoba.lostielauncher.model.hasActiveDownload
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionLookup
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.download.NotificationAccess
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.network.NetworkMonitor
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.service.storage.StorageAccess
import dev.jagoba.lostielauncher.util.download.SpecialVersionPolicy
import dev.jagoba.lostielauncher.util.format.RemainingTimeFormatter
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.policy.GameIdentityMatcher
import dev.jagoba.lostielauncher.util.version.VersionUtils
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryGameUiState(
    val game: GameInfo,
    val installed: LocalGame?,
    val installedStateKnown: Boolean,
    val hasUpdate: Boolean,
    val download: DownloadSnapshot?,
    val installation: GameInstallationState?,
    val status: LibraryCardStatus,
    val remainingTime: String?,
    val canStart: Boolean,
    val canUpdate: Boolean,
    val canSwitchSpecialVersion: Boolean,
    val gameId: String = game.gameId,
    val formattedSize: String = game.formattedSize,
) {
    val canPause: Boolean get() = download?.status?.isTransferring == true
    val canResume: Boolean get() = canStart && download?.status == DownloadStatus.PAUSED
    val canCancel: Boolean get() = download != null && !download.status.isFinished
    val installationUnsupported: Boolean
        get() = installation ==
            GameInstallationState.Finished(dev.jagoba.lostielauncher.model.GameInstallationResult.NotSupportedYet)
}

enum class LibraryNotice {
    SERVER_ACTIONS_UNAVAILABLE,
    OFFLINE,
    DOWNLOAD_BUSY,
    DOWNLOAD_INVALID,
    DOWNLOAD_NOT_FOUND,
    DOWNLOAD_FAILED,
    DOWNLOAD_PERMISSION_DENIED,
    STORAGE_ACCESS_DENIED,
    INSTALLATION_HASH_MISMATCH,
    INSTALLATION_FAILED,
    SPECIAL_KEY_NOT_FOUND,
    SPECIAL_DOWNLOAD_ERROR,
    SPECIAL_KEY_MISMATCH,
    SPECIAL_VERSION_BUSY,
}

enum class LibraryPermissionPrompt {
    STORAGE_RATIONALE,
    STORAGE_SETTINGS,
    NOTIFICATION_RATIONALE,
    NOTIFICATION_SETTINGS,
}

private fun cardStatus(
    installed: LocalGame?,
    hasUpdate: Boolean,
    download: DownloadSnapshot?,
    installation: GameInstallationState?,
): LibraryCardStatus = when (download?.status) {
    DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> LibraryCardStatus.DOWNLOADING

    DownloadStatus.PAUSED -> LibraryCardStatus.PAUSED

    DownloadStatus.INSTALLING -> when (installation) {
        GameInstallationState.VerifyingIntegrity -> LibraryCardStatus.VERIFYING_INTEGRITY
        is GameInstallationState.Extracting -> LibraryCardStatus.EXTRACTING
        else -> LibraryCardStatus.INSTALLATION_PENDING
    }

    DownloadStatus.INSTALLATION_FAILED ->
        if (installation == GameInstallationState.Finished(GameInstallationResult.NotSupportedYet)) {
            LibraryCardStatus.INSTALLATION_UNSUPPORTED
        } else {
            LibraryCardStatus.INSTALLATION_FAILED
        }

    else -> when {
        hasUpdate -> LibraryCardStatus.UPDATE_AVAILABLE
        installed != null -> LibraryCardStatus.DOWNLOADED
        else -> LibraryCardStatus.AVAILABLE
    }
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val isContentStale: Boolean = false,
    val games: List<LibraryGameUiState> = emptyList(),
    val pendingDownloadGameId: String? = null,
    val downloadDestination: DownloadDestination? = null,
    val pendingCancelGameId: String? = null,
    val pendingSpecialGameId: String? = null,
    val isKeyInvalid: Boolean = false,
    val permissionPrompt: LibraryPermissionPrompt? = null,
    val notice: LibraryNotice? = null,
) {
    val isStorageAccessPromptVisible: Boolean
        get() = permissionPrompt == LibraryPermissionPrompt.STORAGE_RATIONALE
    val pendingDownloadGame: GameInfo?
        get() = pendingDownloadGameId?.let { id -> games.firstOrNull { it.gameId == id }?.game }
    val isEmpty: Boolean get() = !isLoading && games.isEmpty()
    val isListVisible: Boolean get() = !isLoading && games.isNotEmpty()
    val isOutOfDateWarningVisible: Boolean get() = isContentStale && !isOffline
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val coordinator: LauncherDataCoordinator,
    private val downloads: DownloadManager,
    private val installation: GameInstallationService,
    private val content: ContentService,
    private val network: NetworkMonitor,
    private val specialVersions: SpecialVersionService,
    private val navigation: NavigationStore,
    private val externalLinks: ExternalLinkService,
    private val storageAccess: StorageAccess,
    private val notificationAccess: NotificationAccess,
    private val logger: Logger,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val transient = MutableStateFlow(restoreTransient(savedState))
    private var pendingStorageAction: PendingStorageAction? = restoreStorageAction(savedState)

    private val installationStates = downloads.downloads.flatMapLatest { rows ->
        val installing = rows.filter { it.status in INSTALLATION_STATUSES }
        if (installing.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                installing.map { row ->
                    installation.observeInstallation(row.gameId).map { row.gameId to it }
                },
            ) { pairs -> pairs.toMap() }
        }
    }

    private val cards = combine(
        coordinator.catalogue,
        coordinator.home,
        downloads.downloads,
        installation.installedGames,
        installationStates,
    ) { catalogue, home, downloadRows, installedState, phases ->
        val installed = (installedState as? InstalledGamesState.Available)?.games.orEmpty()
        val active = downloadRows.hasActiveDownload
        val catalogueIds = catalogue.games.mapTo(mutableSetOf()) { it.gameId }
        val catalogueCards = catalogue.games.map { game ->
            val local = GameIdentityMatcher.findLocal(game, installed)
            val download = downloadRows.firstOrNull { it.gameId == game.gameId }
            buildCardState(
                game = game,
                gameId = game.gameId,
                formattedSize = game.formattedSize,
                local = local,
                installedStateKnown = installedState is InstalledGamesState.Available,
                download = download,
                phase = phases[game.gameId],
                hasActiveDownload = active,
                catalogueLoading = catalogue.isLoading,
                inCatalogue = true,
            )
        }
        val orphanCards = downloadRows
            .filter { it.gameId !in catalogueIds && it.status.isOngoing }
            .map { row ->
                val fallback = fallbackGame(row)
                val local = installed.firstOrNull { gameSlug(it.name) == row.gameId }
                buildCardState(
                    game = fallback,
                    gameId = row.gameId,
                    formattedSize = if (row.totalBytes != null && row.totalBytes > 0L) fallback.formattedSize else "",
                    local = local,
                    installedStateKnown = installedState is InstalledGamesState.Available,
                    download = row,
                    phase = phases[row.gameId],
                    hasActiveDownload = active,
                    catalogueLoading = catalogue.isLoading,
                    inCatalogue = false,
                )
            }
        LibraryUiState(
            isLoading = catalogue.isLoading,
            isOffline = home.isOffline,
            isContentStale = catalogue.isStale,
            games = catalogueCards + orphanCards,
        )
    }

    val state: StateFlow<LibraryUiState> = combine(cards, transient) { base, prompts ->
        prompts.copy(
            isLoading = base.isLoading,
            isOffline = base.isOffline,
            isContentStale = base.isContentStale,
            games = base.games,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, transient.value)

    init {
        viewModelScope.launch {
            var previous = emptyMap<String, DownloadStatus>()
            downloads.downloads.collect { rows ->
                val pendingCancel = transient.value.pendingCancelGameId
                if (pendingCancel != null && rows.none { it.gameId == pendingCancel && !it.status.isFinished }) {
                    updateTransient { it.copy(pendingCancelGameId = null) }
                }
                val notice = rows.firstNotNullOfOrNull { row ->
                    row.status.failureNotice()?.takeIf { previous[row.gameId]?.isActive == true }
                }
                previous = rows.associate { it.gameId to it.status }
                if (notice != null) updateTransient { it.copy(notice = notice) }
            }
        }
        viewModelScope.launch {
            var previous: Map<String, GameInstallationState>? = null
            installationStates.collect { phases ->
                val before = previous
                previous = phases
                if (before == null) return@collect
                val notice = phases.entries.firstNotNullOfOrNull { (gameId, phase) ->
                    phase.failureNotice()?.takeIf { gameId in before && before[gameId] != phase }
                }
                if (notice != null) updateTransient { it.copy(notice = notice) }
            }
        }
        viewModelScope.launch {
            combine(navigation.state, state) { target, current -> target to current }.collect { (target, current) ->
                val pending = target.pendingLibraryAction ?: return@collect
                if (current.isLoading || current.games.none { it.gameId == pending.gameId }) return@collect
                if (!navigation.consumeLibraryAction(pending.gameId)) return@collect
                when (pending.action) {
                    LibraryNavigationAction.UPDATE -> startUpdate(pending.gameId)
                    LibraryNavigationAction.SPECIAL_VERSION -> requestSpecialVersion(pending.gameId)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            coordinator.refreshCatalogue(force = true)
        }
    }

    fun requestDownload(gameId: String) {
        val row = find(gameId) ?: return
        if (!row.canStart) {
            logger.debug("Download request ignored for $gameId: its card is ${row.status}.")
            return
        }
        if (row.download?.status == DownloadStatus.INSTALLATION_FAILED) {
            withStorageAccess(PendingStorageAction.RetryInstallation(gameId))
            return
        }
        viewModelScope.launch {
            if (!serverActionsAvailable()) return@launch
            if (row.download?.status == DownloadStatus.PAUSED) {
                record(downloads.resume(gameId))
            } else {
                val destination = downloads.destination()
                updateTransient {
                    it.copy(
                        pendingDownloadGameId = gameId,
                        downloadDestination = destination,
                        isKeyInvalid = false,
                        notice = null,
                    )
                }
            }
        }
    }

    fun confirmDownload(key: String? = null): Boolean {
        val gameId = transient.value.pendingDownloadGameId ?: return false
        val trimmedKey = key?.trim()?.takeIf(String::isNotEmpty)
        if (trimmedKey != null && !acceptKeyFormat(gameId, trimmedKey)) return false
        updateTransient { it.copy(pendingDownloadGameId = null, downloadDestination = null, isKeyInvalid = false) }
        if (find(gameId) == null) return false
        withStorageAccess(PendingStorageAction.Download(gameId, trimmedKey))
        return true
    }

    fun openPendingGamePage() {
        state.value.pendingDownloadGame?.pageUrl?.takeIf(String::isNotBlank)?.let(externalLinks::openUrl)
    }

    fun startUpdate(gameId: String) {
        val row = find(gameId) ?: return
        if (!row.canUpdate) {
            logger.debug("Update request ignored for $gameId: its card is ${row.status}.")
            return
        }
        withStorageAccess(PendingStorageAction.Update(gameId))
    }

    fun requestStorageAccess() {
        updateTransient { it.copy(permissionPrompt = null) }
    }

    fun onStorageAccessResult(permanentlyDenied: Boolean = false) {
        val action = pendingStorageAction ?: return
        if (storageAccess.isGranted()) {
            logger.info("Shared storage access granted.")
            continueWithNotifications(action)
        } else if (permanentlyDenied) {
            logger.info("Shared storage access permanently denied; offering app settings.")
            updateTransient { it.copy(permissionPrompt = LibraryPermissionPrompt.STORAGE_SETTINGS, notice = null) }
        } else {
            setPendingStorageAction(null)
            logger.info("Shared storage access denied; the download was not started.")
            updateTransient { it.copy(notice = LibraryNotice.STORAGE_ACCESS_DENIED) }
        }
    }

    fun requestNotificationPermission() {
        updateTransient { it.copy(permissionPrompt = null) }
    }

    fun skipNotificationPermission() {
        val action = pendingStorageAction
        setPendingStorageAction(null)
        updateTransient { it.copy(permissionPrompt = null) }
        if (action != null) {
            logger.info("Notification permission skipped; continuing with the download.")
            runStorageAction(action)
        }
    }

    fun onNotificationPermissionResult(permanentlyDenied: Boolean = false) {
        val action = pendingStorageAction ?: return
        if (notificationAccess.isGranted()) {
            setPendingStorageAction(null)
            logger.info("Notification permission granted.")
            runStorageAction(action)
        } else if (permanentlyDenied) {
            logger.info("Notification permission permanently denied; offering notification settings.")
            updateTransient {
                it.copy(permissionPrompt = LibraryPermissionPrompt.NOTIFICATION_SETTINGS, notice = null)
            }
        } else {
            setPendingStorageAction(null)
            logger.info("Notification permission denied; continuing with the download.")
            runStorageAction(action)
        }
    }

    fun requestSpecialVersion(gameId: String) {
        val row = find(gameId) ?: return
        if (!row.canSwitchSpecialVersion) {
            logger.debug("Special version request blocked for $gameId: another download is active.")
            updateTransient { it.copy(notice = LibraryNotice.SPECIAL_VERSION_BUSY) }
            return
        }
        viewModelScope.launch {
            if (serverActionsAvailable()) {
                updateTransient { it.copy(pendingSpecialGameId = gameId, isKeyInvalid = false, notice = null) }
            }
        }
    }

    fun submitSpecialVersionKey(key: String) {
        val gameId = transient.value.pendingSpecialGameId ?: return
        val trimmedKey = key.trim()
        if (!acceptKeyFormat(gameId, trimmedKey)) return
        updateTransient { it.copy(pendingSpecialGameId = null, isKeyInvalid = false) }
        viewModelScope.launch {
            if (!serverActionsAvailable()) return@launch
            val game = resolveGame(gameId) ?: return@launch
            startSpecialVersion(game, trimmedKey)
        }
    }

    fun clearKeyError() {
        if (transient.value.isKeyInvalid) updateTransient { it.copy(isKeyInvalid = false) }
    }

    private fun acceptKeyFormat(gameId: String, key: String): Boolean {
        if (SpecialVersionPolicy.isValidKey(key)) return true
        logger.info("Download key rejected for $gameId: invalid format.")
        updateTransient { it.copy(isKeyInvalid = true) }
        return false
    }

    private suspend fun startSpecialVersion(game: GameInfo, key: String) {
        logger.debug("Fetching special version config for ${game.gameId}.")
        when (val result = specialVersions.lookup(key)) {
            is SpecialVersionLookup.Found -> {
                if (!SpecialVersionPolicy.isValidConfig(result.config)) {
                    logger.error(
                        "Invalid special version config for ${game.gameId}: archivo='${result.config.fileName}'.",
                    )
                    updateTransient { it.copy(notice = LibraryNotice.SPECIAL_KEY_NOT_FOUND) }
                } else if (!SpecialVersionPolicy.matchesGame(result.config, game.id)) {
                    logger.info("Special version config for ${game.gameId} belongs to another game.")
                    updateTransient { it.copy(notice = LibraryNotice.SPECIAL_KEY_MISMATCH) }
                } else {
                    logger.debug("Starting special version session for ${game.gameId} (${result.config.version}).")
                    start(
                        game,
                        GameDownloadArgs(game.gameId, result.config.version, "/${result.config.fileName}", key),
                        result.config.sha256,
                        result.config.type,
                    )
                }
            }

            SpecialVersionLookup.NotFound -> {
                logger.info("No special version config exists for ${game.gameId}.")
                updateTransient { it.copy(notice = LibraryNotice.SPECIAL_KEY_NOT_FOUND) }
            }

            SpecialVersionLookup.NetworkError, SpecialVersionLookup.InvalidResponse -> {
                logger.info("Special version config for ${game.gameId} could not be read: $result.")
                updateTransient { it.copy(notice = LibraryNotice.SPECIAL_DOWNLOAD_ERROR) }
            }
        }
    }

    fun pause(gameId: String) {
        viewModelScope.launch { record(downloads.pause(gameId)) }
    }

    fun resume(gameId: String) {
        viewModelScope.launch { record(downloads.resume(gameId)) }
    }

    fun requestCancel(gameId: String) {
        if (find(gameId)?.canCancel != true) return
        updateTransient { it.copy(pendingCancelGameId = gameId) }
    }

    fun confirmCancel() {
        val gameId = transient.value.pendingCancelGameId ?: return
        updateTransient { it.copy(pendingCancelGameId = null) }
        viewModelScope.launch { record(downloads.cancel(gameId)) }
    }

    fun dismissPrompt() {
        transient.value.pendingCancelGameId?.let { logger.debug("Cancel download aborted by user for $it.") }
        setPendingStorageAction(null)
        updateTransient { LibraryUiState(notice = it.notice) }
    }

    fun clearNotice() {
        updateTransient { it.copy(notice = null) }
    }

    private fun withStorageAccess(action: PendingStorageAction) {
        if (storageAccess.isGranted()) {
            continueWithNotifications(action)
            return
        }
        logger.info("Shared storage access is missing; asking before the download starts.")
        setPendingStorageAction(action)
        updateTransient { it.copy(permissionPrompt = LibraryPermissionPrompt.STORAGE_RATIONALE, notice = null) }
    }

    private fun continueWithNotifications(action: PendingStorageAction) {
        if (notificationAccess.isGranted()) {
            setPendingStorageAction(null)
            runStorageAction(action)
            return
        }
        logger.info("Notification permission is missing; explaining before the download starts.")
        setPendingStorageAction(action)
        updateTransient { it.copy(permissionPrompt = LibraryPermissionPrompt.NOTIFICATION_RATIONALE, notice = null) }
    }

    private fun runStorageAction(action: PendingStorageAction) {
        viewModelScope.launch {
            if (!serverActionsAvailable()) return@launch
            when (action) {
                is PendingStorageAction.RetryInstallation -> record(downloads.retry(action.gameId))

                is PendingStorageAction.Download -> {
                    val game = resolveGame(action.gameId) ?: return@launch
                    if (action.key.isNullOrEmpty()) {
                        start(game)
                    } else {
                        startSpecialVersion(game, action.key)
                    }
                }

                is PendingStorageAction.Update -> {
                    val game = resolveGame(action.gameId) ?: return@launch
                    logger.debug("Starting update session for ${action.gameId}.")
                    start(game)
                }
            }
        }
    }

    private fun find(gameId: String): LibraryGameUiState? = state.value.games.firstOrNull { it.gameId == gameId }

    private suspend fun resolveGame(gameId: String): GameInfo? {
        find(gameId)?.game?.let { return it }
        return coordinator.catalogue.first { !it.isLoading }.games.firstOrNull { it.gameId == gameId }
    }

    private fun updateTransient(transform: (LibraryUiState) -> LibraryUiState) {
        val updated = transform(transient.value)
        transient.value = updated
        savedState[PENDING_DOWNLOAD_GAME_ID_KEY] = updated.pendingDownloadGameId
        savedState[DOWNLOAD_DESTINATION_PATH_KEY] = updated.downloadDestination?.path
        savedState[DOWNLOAD_DESTINATION_FREE_BYTES_KEY] = updated.downloadDestination?.freeBytes
        savedState[PENDING_CANCEL_GAME_ID_KEY] = updated.pendingCancelGameId
        savedState[PENDING_SPECIAL_GAME_ID_KEY] = updated.pendingSpecialGameId
        savedState[KEY_INVALID_KEY] = updated.isKeyInvalid
        savedState[PERMISSION_PROMPT_KEY] = updated.permissionPrompt?.name
        savedState[NOTICE_KEY] = updated.notice?.name
    }

    private fun setPendingStorageAction(action: PendingStorageAction?) {
        pendingStorageAction = action
        savedState[STORAGE_ACTION_KIND_KEY] = when (action) {
            is PendingStorageAction.Download -> ACTION_DOWNLOAD
            is PendingStorageAction.Update -> ACTION_UPDATE
            is PendingStorageAction.RetryInstallation -> ACTION_RETRY
            null -> null
        }
        savedState[STORAGE_ACTION_GAME_ID_KEY] = action?.gameId
        savedState[STORAGE_ACTION_KEY_KEY] = (action as? PendingStorageAction.Download)?.key
    }

    private suspend fun start(
        game: GameInfo,
        args: GameDownloadArgs = GameDownloadArgs(game.gameId, game.version, game.relativePath),
        expectedSha256: String = game.sha256,
        variant: String? = null,
    ) {
        record(downloads.start(DownloadRequest(game.name, args, expectedSha256, variant)))
    }

    private suspend fun serverActionsAvailable(): Boolean {
        if (!content.isServerActionBlocked()) return true
        val offline = !network.isOnline.value
        logger.info(
            "Server-backed action blocked by ${if (offline) "missing internet connection" else "maintenance flag"}.",
        )
        val notice = if (offline) LibraryNotice.OFFLINE else LibraryNotice.SERVER_ACTIONS_UNAVAILABLE
        updateTransient { it.copy(notice = notice) }
        return false
    }

    private fun record(result: DownloadCommandResult) {
        if (result != DownloadCommandResult.ACCEPTED) logger.info("Download command rejected: $result.")
        val notice = when (result) {
            DownloadCommandResult.ACCEPTED -> null
            DownloadCommandResult.BUSY -> LibraryNotice.DOWNLOAD_BUSY
            DownloadCommandResult.NOT_FOUND -> LibraryNotice.DOWNLOAD_NOT_FOUND
            DownloadCommandResult.INVALID_STATE -> LibraryNotice.DOWNLOAD_INVALID
        }
        updateTransient { it.copy(notice = notice) }
    }

    private sealed interface PendingStorageAction {
        val gameId: String

        data class Download(override val gameId: String, val key: String? = null) : PendingStorageAction

        data class Update(override val gameId: String) : PendingStorageAction

        data class RetryInstallation(override val gameId: String) : PendingStorageAction
    }

    private companion object {
        const val PENDING_DOWNLOAD_GAME_ID_KEY = "library.pendingDownloadGameId"
        const val DOWNLOAD_DESTINATION_PATH_KEY = "library.downloadDestination.path"
        const val DOWNLOAD_DESTINATION_FREE_BYTES_KEY = "library.downloadDestination.freeBytes"
        const val PENDING_CANCEL_GAME_ID_KEY = "library.pendingCancelGameId"
        const val PENDING_SPECIAL_GAME_ID_KEY = "library.pendingSpecialGameId"
        const val KEY_INVALID_KEY = "library.isKeyInvalid"
        const val PERMISSION_PROMPT_KEY = "library.permissionPrompt"
        const val NOTICE_KEY = "library.notice"
        const val STORAGE_ACTION_KIND_KEY = "library.storageAction.kind"
        const val STORAGE_ACTION_GAME_ID_KEY = "library.storageAction.gameId"
        const val STORAGE_ACTION_KEY_KEY = "library.storageAction.key"
        const val ACTION_DOWNLOAD = "DOWNLOAD"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_RETRY = "RETRY"

        val INSTALLATION_STATUSES = setOf(DownloadStatus.INSTALLING, DownloadStatus.INSTALLATION_FAILED)
        val STARTABLE_STATUSES = setOf(
            LibraryCardStatus.AVAILABLE,
            LibraryCardStatus.PAUSED,
            LibraryCardStatus.INSTALLATION_FAILED,
        )
        const val BYTES_PER_GIB = 1024.0 * 1024.0 * 1024.0

        fun buildCardState(
            game: GameInfo,
            gameId: String,
            formattedSize: String,
            local: LocalGame?,
            installedStateKnown: Boolean,
            download: DownloadSnapshot?,
            phase: GameInstallationState?,
            hasActiveDownload: Boolean,
            catalogueLoading: Boolean,
            inCatalogue: Boolean,
        ): LibraryGameUiState {
            val canAct = !hasActiveDownload && !catalogueLoading
            val hasUpdate = inCatalogue && local != null && VersionUtils.isNewerVersion(game.version, local.version)
            val status = cardStatus(local, hasUpdate, download, phase)
            return LibraryGameUiState(
                game = game,
                installed = local,
                installedStateKnown = installedStateKnown,
                hasUpdate = hasUpdate,
                download = download,
                installation = phase,
                status = status,
                remainingTime = download?.takeIf { it.status == DownloadStatus.DOWNLOADING }
                    ?.let { row ->
                        row.totalBytes?.let {
                            RemainingTimeFormatter.format(it - row.downloadedBytes, row.bytesPerSecond)
                        }
                    },
                canStart = canAct && status in STARTABLE_STATUSES,
                canUpdate = canAct && status == LibraryCardStatus.UPDATE_AVAILABLE,
                canSwitchSpecialVersion = inCatalogue && canAct,
                gameId = gameId,
                formattedSize = formattedSize,
            )
        }

        fun fallbackGame(row: DownloadSnapshot): GameInfo = GameInfo(
            id = null,
            name = row.displayName,
            version = row.version,
            sizeGb = row.totalBytes?.takeIf { it > 0L }?.let { it.toDouble() / BYTES_PER_GIB } ?: 0.0,
            description = "",
            pageUrl = "",
            logoUrl = null,
            relativePath = "",
            sha256 = "",
        )

        fun restoreTransient(savedState: SavedStateHandle): LibraryUiState {
            val path = savedState.get<String>(DOWNLOAD_DESTINATION_PATH_KEY)
            val permissionPromptName = savedState.get<String>(PERMISSION_PROMPT_KEY)
            val noticeName = savedState.get<String>(NOTICE_KEY)
            return LibraryUiState(
                pendingDownloadGameId = savedState.get<String>(PENDING_DOWNLOAD_GAME_ID_KEY),
                downloadDestination = path?.let {
                    DownloadDestination(it, savedState.get<Long>(DOWNLOAD_DESTINATION_FREE_BYTES_KEY))
                },
                pendingCancelGameId = savedState.get<String>(PENDING_CANCEL_GAME_ID_KEY),
                pendingSpecialGameId = savedState.get<String>(PENDING_SPECIAL_GAME_ID_KEY),
                isKeyInvalid = savedState.get<Boolean>(KEY_INVALID_KEY) ?: false,
                permissionPrompt = LibraryPermissionPrompt.entries.firstOrNull { it.name == permissionPromptName },
                notice = LibraryNotice.entries.firstOrNull { it.name == noticeName },
            )
        }

        fun restoreStorageAction(savedState: SavedStateHandle): PendingStorageAction? {
            val gameId = savedState.get<String>(STORAGE_ACTION_GAME_ID_KEY) ?: return null
            return when (savedState.get<String>(STORAGE_ACTION_KIND_KEY)) {
                ACTION_DOWNLOAD -> PendingStorageAction.Download(gameId, savedState.get<String>(STORAGE_ACTION_KEY_KEY))
                ACTION_UPDATE -> PendingStorageAction.Update(gameId)
                ACTION_RETRY -> PendingStorageAction.RetryInstallation(gameId)
                else -> null
            }
        }

        fun GameInstallationState.failureNotice(): LibraryNotice? =
            when ((this as? GameInstallationState.Finished)?.result) {
                GameInstallationResult.InvalidHash, GameInstallationResult.HashMismatch ->
                    LibraryNotice.INSTALLATION_HASH_MISMATCH

                GameInstallationResult.StorageAccessDenied -> LibraryNotice.STORAGE_ACCESS_DENIED

                GameInstallationResult.MissingArchive,
                GameInstallationResult.ExtractionFailed,
                GameInstallationResult.RegistryFailed,
                -> LibraryNotice.INSTALLATION_FAILED

                else -> null
            }

        fun DownloadStatus.failureNotice(): LibraryNotice? = when (this) {
            DownloadStatus.FAILED -> LibraryNotice.DOWNLOAD_FAILED
            DownloadStatus.PERMISSION_DENIED -> LibraryNotice.DOWNLOAD_PERMISSION_DENIED
            else -> null
        }
    }
}
