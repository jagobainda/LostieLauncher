package dev.jagoba.lostielauncher.ui.viewmodel

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
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionLookup
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.util.download.SpecialVersionPolicy
import dev.jagoba.lostielauncher.util.format.RemainingTimeFormatter
import dev.jagoba.lostielauncher.util.policy.GameIdentityMatcher
import dev.jagoba.lostielauncher.util.version.VersionUtils
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val playtimeMinutes: Int,
    val download: DownloadSnapshot?,
    val installation: GameInstallationState?,
    val status: LibraryCardStatus,
    val remainingTime: String?,
    val canStart: Boolean,
    val canUpdate: Boolean,
    val canSwitchSpecialVersion: Boolean,
) {
    val canPause: Boolean get() = download?.status == DownloadStatus.QUEUED ||
        download?.status == DownloadStatus.DOWNLOADING
    val canResume: Boolean get() = canStart && download?.status == DownloadStatus.PAUSED
    val canCancel: Boolean get() = download != null &&
        download.status !in setOf(DownloadStatus.COMPLETED, DownloadStatus.CANCELLED)
    val installationUnsupported: Boolean
        get() = installation ==
            GameInstallationState.Finished(dev.jagoba.lostielauncher.model.GameInstallationResult.NotSupportedYet)
}

enum class LibraryNotice {
    SERVER_ACTIONS_UNAVAILABLE,
    DOWNLOAD_BUSY,
    DOWNLOAD_INVALID,
    DOWNLOAD_NOT_FOUND,
    DOWNLOAD_FAILED,
    DOWNLOAD_PERMISSION_DENIED,
    INSTALLATION_HASH_MISMATCH,
    INSTALLATION_FAILED,
    SPECIAL_KEY_INVALID,
    SPECIAL_KEY_NOT_FOUND,
    SPECIAL_DOWNLOAD_ERROR,
    SPECIAL_KEY_MISMATCH,
}

private fun cardStatus(
    installed: LocalGame?,
    hasUpdate: Boolean,
    download: DownloadSnapshot?,
    installation: GameInstallationState?,
): LibraryCardStatus = when (download?.status) {
    DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> LibraryCardStatus.DOWNLOADING

    DownloadStatus.PAUSED -> LibraryCardStatus.PAUSED

    DownloadStatus.COMPLETED -> when (installation) {
        GameInstallationState.VerifyingIntegrity -> LibraryCardStatus.VERIFYING_INTEGRITY

        GameInstallationState.Extracting -> LibraryCardStatus.EXTRACTING

        is GameInstallationState.Finished -> when (installation.result) {
            is GameInstallationResult.Installed -> LibraryCardStatus.DOWNLOADED
            GameInstallationResult.NotSupportedYet -> LibraryCardStatus.INSTALLATION_UNSUPPORTED
            else -> LibraryCardStatus.INSTALLATION_FAILED
        }

        else -> LibraryCardStatus.INSTALLATION_PENDING
    }

    else -> when {
        hasUpdate -> LibraryCardStatus.UPDATE_AVAILABLE
        installed != null -> LibraryCardStatus.DOWNLOADED
        else -> LibraryCardStatus.AVAILABLE
    }
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val games: List<LibraryGameUiState> = emptyList(),
    val pendingDownloadGameId: String? = null,
    val downloadDestination: DownloadDestination? = null,
    val pendingCancelGameId: String? = null,
    val pendingSpecialGameId: String? = null,
    val notice: LibraryNotice? = null,
) {
    val pendingDownloadGame: GameInfo?
        get() = pendingDownloadGameId?.let { id -> games.firstOrNull { it.game.gameId == id }?.game }
    val isEmpty: Boolean get() = !isLoading && games.isEmpty()
    val isListVisible: Boolean get() = !isLoading && games.isNotEmpty()
}

private data class LibraryTransient(
    val pendingDownloadGameId: String? = null,
    val downloadDestination: DownloadDestination? = null,
    val pendingCancelGameId: String? = null,
    val pendingSpecialGameId: String? = null,
    val notice: LibraryNotice? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val coordinator: LauncherDataCoordinator,
    private val downloads: DownloadManager,
    private val installation: GameInstallationService,
    private val library: LocalLibraryStore,
    private val content: ContentService,
    private val specialVersions: SpecialVersionService,
    private val navigation: NavigationStore,
    private val externalLinks: ExternalLinkService,
) : ViewModel() {
    private val transient = MutableStateFlow(LibraryTransient())
    private val playtimes = MutableStateFlow<Map<UUID, Int>>(emptyMap())
    private var maintenanceNoticeShown = false

    private val installationStates = downloads.downloads.flatMapLatest { rows ->
        val completed = rows.filter { it.status == DownloadStatus.COMPLETED }
        if (completed.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                completed.map { row ->
                    installation.observeInstallation(row.gameId).map { row.gameId to it }
                },
            ) { pairs -> pairs.toMap() }
        }
    }

    private val cards = combine(
        coordinator.catalogue,
        downloads.downloads,
        installation.installedGames,
        installationStates,
    ) { catalogue, downloadRows, installedState, phases ->
        val installed = (installedState as? InstalledGamesState.Available)?.games.orEmpty()
        val active = downloadRows.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
        LibraryUiState(
            isLoading = catalogue.isLoading,
            games = catalogue.games.map { game ->
                val local = GameIdentityMatcher.findLocal(game, installed)
                val download = downloadRows.firstOrNull { it.gameId == game.gameId }
                val canAct = !active && !catalogue.isLoading
                val hasUpdate = local != null && VersionUtils.isNewerVersion(game.version, local.version)
                val phase = if (download?.status == DownloadStatus.COMPLETED) phases[game.gameId] else null
                val status = cardStatus(local, hasUpdate, download, phase)
                LibraryGameUiState(
                    game = game,
                    installed = local,
                    installedStateKnown = installedState is InstalledGamesState.Available,
                    hasUpdate = hasUpdate,
                    playtimeMinutes = game.id?.let(playtimes.value::get) ?: 0,
                    download = download,
                    installation = phase,
                    status = status,
                    remainingTime = download?.takeIf { it.status == DownloadStatus.DOWNLOADING }
                        ?.let { row ->
                            row.totalBytes?.let {
                                RemainingTimeFormatter.format(it - row.downloadedBytes, row.bytesPerSecond)
                            }
                        },
                    canStart = canAct && status in setOf(LibraryCardStatus.AVAILABLE, LibraryCardStatus.PAUSED),
                    canUpdate = canAct && status == LibraryCardStatus.UPDATE_AVAILABLE,
                    canSwitchSpecialVersion = canAct,
                )
            },
        )
    }

    val state: StateFlow<LibraryUiState> = combine(cards, transient, playtimes) { base, transient, times ->
        base.copy(
            games = base.games.map { row -> row.copy(playtimeMinutes = row.game.id?.let(times::get) ?: 0) },
            pendingDownloadGameId = transient.pendingDownloadGameId,
            downloadDestination = transient.downloadDestination,
            pendingCancelGameId = transient.pendingCancelGameId,
            pendingSpecialGameId = transient.pendingSpecialGameId,
            notice = transient.notice,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    init {
        viewModelScope.launch {
            var previous = emptyMap<String, DownloadStatus>()
            downloads.downloads.collect { rows ->
                val notice = rows.firstNotNullOfOrNull { row ->
                    row.status.failureNotice()?.takeIf { previous[row.gameId] in ACTIVE_STATUSES }
                }
                previous = rows.associate { it.gameId to it.status }
                if (notice != null) transient.value = transient.value.copy(notice = notice)
            }
        }
        viewModelScope.launch {
            var previous: Map<String, GameInstallationState>? = null
            installationStates.collect { phases ->
                val before = previous
                previous = phases
                if (before == null) return@collect
                val notice = phases.entries.firstNotNullOfOrNull { (gameId, phase) ->
                    phase.failureNotice()?.takeIf { before[gameId] != phase }
                }
                if (notice != null) transient.value = transient.value.copy(notice = notice)
            }
        }
        viewModelScope.launch {
            coordinator.gamesRevision.collect {
                playtimes.value = library.getPlaytimes()
            }
        }
        viewModelScope.launch {
            combine(navigation.state, state) { target, current -> target to current }.collect { (target, current) ->
                val gameId = target.pendingLibraryGameId ?: return@collect
                if (current.games.none { it.game.gameId == gameId }) return@collect
                when (target.libraryAction) {
                    LibraryNavigationAction.DOWNLOAD -> requestDownload(gameId, allowInstalled = true)
                    LibraryNavigationAction.UPDATE -> startUpdate(gameId)
                    LibraryNavigationAction.SPECIAL_VERSION -> requestSpecialVersion(gameId)
                    null -> Unit
                }
                if (target.libraryAction != null) navigation.consumeLibraryAction(gameId)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            coordinator.refreshCatalogue()
            playtimes.value = library.getPlaytimes()
        }
    }

    fun requestDownload(gameId: String, allowInstalled: Boolean = false) {
        val row = state.value.games.firstOrNull { it.game.gameId == gameId } ?: return
        val canReplaceMissingInstall =
            allowInstalled && row.status in setOf(LibraryCardStatus.DOWNLOADED, LibraryCardStatus.UPDATE_AVAILABLE) &&
                row.canSwitchSpecialVersion
        if (!row.canStart && !canReplaceMissingInstall) return
        viewModelScope.launch {
            if (!serverActionsAvailable()) return@launch
            if (row.download?.status == DownloadStatus.PAUSED) {
                record(downloads.resume(gameId))
            } else {
                transient.value = transient.value.copy(
                    pendingDownloadGameId = gameId,
                    downloadDestination = downloads.destination(),
                    notice = null,
                )
            }
        }
    }

    fun confirmDownload(key: String? = null) {
        val gameId = transient.value.pendingDownloadGameId ?: return
        transient.value = transient.value.copy(pendingDownloadGameId = null, downloadDestination = null)
        val game = state.value.games.firstOrNull { it.game.gameId == gameId }?.game ?: return
        viewModelScope.launch {
            val trimmedKey = key?.trim().orEmpty()
            if (trimmedKey.isEmpty()) {
                start(game, GameDownloadArgs(game.gameId, game.version, game.relativePath))
            } else {
                startSpecialVersion(game, trimmedKey)
            }
        }
    }

    fun openPendingGamePage() {
        state.value.pendingDownloadGame?.pageUrl?.takeIf(String::isNotBlank)?.let(externalLinks::openUrl)
    }

    fun startUpdate(gameId: String) {
        val row = state.value.games.firstOrNull { it.game.gameId == gameId } ?: return
        if (!row.canUpdate) return
        viewModelScope.launch {
            if (!serverActionsAvailable()) return@launch
            start(row.game, GameDownloadArgs(row.game.gameId, row.game.version, row.game.relativePath))
        }
    }

    fun requestSpecialVersion(gameId: String) {
        val row = state.value.games.firstOrNull { it.game.gameId == gameId } ?: return
        if (!row.canSwitchSpecialVersion) return
        viewModelScope.launch {
            if (serverActionsAvailable()) {
                transient.value = transient.value.copy(pendingSpecialGameId = gameId, notice = null)
            }
        }
    }

    fun submitSpecialVersionKey(key: String) {
        val gameId = transient.value.pendingSpecialGameId ?: return
        transient.value = transient.value.copy(pendingSpecialGameId = null)
        val game = state.value.games.firstOrNull { it.game.gameId == gameId }?.game ?: return
        viewModelScope.launch { startSpecialVersion(game, key.trim()) }
    }

    private suspend fun startSpecialVersion(game: GameInfo, key: String) {
        if (!serverActionsAvailable()) return
        if (!SpecialVersionPolicy.isValidKey(key)) {
            transient.value = transient.value.copy(notice = LibraryNotice.SPECIAL_KEY_INVALID)
            return
        }
        when (val result = specialVersions.lookup(key)) {
            is SpecialVersionLookup.Found -> {
                if (!SpecialVersionPolicy.isValidConfig(result.config)) {
                    transient.value = transient.value.copy(notice = LibraryNotice.SPECIAL_KEY_NOT_FOUND)
                } else if (!SpecialVersionPolicy.matchesGame(result.config, game.id)) {
                    transient.value = transient.value.copy(notice = LibraryNotice.SPECIAL_KEY_MISMATCH)
                } else {
                    start(
                        game,
                        GameDownloadArgs(game.gameId, result.config.version, "/${result.config.fileName}", key),
                    )
                }
            }

            SpecialVersionLookup.NotFound ->
                transient.value =
                    transient.value.copy(notice = LibraryNotice.SPECIAL_KEY_NOT_FOUND)

            SpecialVersionLookup.NetworkError ->
                transient.value =
                    transient.value.copy(notice = LibraryNotice.SPECIAL_DOWNLOAD_ERROR)

            SpecialVersionLookup.InvalidResponse ->
                transient.value =
                    transient.value.copy(notice = LibraryNotice.SPECIAL_DOWNLOAD_ERROR)
        }
    }

    fun pause(gameId: String) {
        viewModelScope.launch { record(downloads.pause(gameId)) }
    }

    fun resume(gameId: String) {
        viewModelScope.launch { record(downloads.resume(gameId)) }
    }

    fun requestCancel(gameId: String) {
        if (state.value.games.none { it.game.gameId == gameId }) return
        transient.value = transient.value.copy(pendingCancelGameId = gameId)
    }

    fun confirmCancel() {
        val gameId = transient.value.pendingCancelGameId ?: return
        transient.value = transient.value.copy(pendingCancelGameId = null)
        viewModelScope.launch { record(downloads.cancel(gameId)) }
    }

    fun dismissPrompt() {
        transient.value = LibraryTransient(notice = transient.value.notice)
    }

    fun clearNotice() {
        transient.value = transient.value.copy(notice = null)
    }

    private suspend fun start(game: GameInfo, args: GameDownloadArgs) {
        if (!serverActionsAvailable()) return
        record(downloads.start(DownloadRequest(game.name, args)))
    }

    private suspend fun serverActionsAvailable(): Boolean {
        if (!content.isServerActionBlocked()) {
            maintenanceNoticeShown = false
            return true
        }
        if (!maintenanceNoticeShown) {
            transient.value = transient.value.copy(notice = LibraryNotice.SERVER_ACTIONS_UNAVAILABLE)
            maintenanceNoticeShown = true
        }
        return false
    }

    private fun record(result: DownloadCommandResult) {
        val notice = when (result) {
            DownloadCommandResult.ACCEPTED -> null
            DownloadCommandResult.BUSY -> LibraryNotice.DOWNLOAD_BUSY
            DownloadCommandResult.NOT_FOUND -> LibraryNotice.DOWNLOAD_NOT_FOUND
            DownloadCommandResult.INVALID_STATE -> LibraryNotice.DOWNLOAD_INVALID
        }
        transient.value = transient.value.copy(notice = notice)
    }

    private companion object {
        val ACTIVE_STATUSES = setOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING)

        fun GameInstallationState.failureNotice(): LibraryNotice? =
            when ((this as? GameInstallationState.Finished)?.result) {
                GameInstallationResult.InvalidHash, GameInstallationResult.HashMismatch ->
                    LibraryNotice.INSTALLATION_HASH_MISMATCH

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
