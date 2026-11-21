package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.MissingGameId
import dev.jagoba.lostielauncher.model.OpenGameLocationResult
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.game.GameLaunchService
import dev.jagoba.lostielauncher.service.game.GameLocationService
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.policy.GameIdentityMatcher
import dev.jagoba.lostielauncher.util.version.VersionUtils
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class InstalledGameUiState(
    val game: LocalGame,
    val remote: GameInfo?,
    val hasUpdate: Boolean,
    val isUpdating: Boolean,
    val isUninstalling: Boolean,
) {
    val canPlay: Boolean get() = !isUpdating && !isUninstalling && (!hasUpdate || !game.type.isNullOrEmpty())
    val canUpdate: Boolean get() = !isUpdating && !isUninstalling && hasUpdate
    val canSwitchSpecialVersion: Boolean get() = !isUpdating && !isUninstalling && remote != null
    val canUninstall: Boolean get() = !isUninstalling
}

enum class GamesNotice {
    NOT_SUPPORTED_YET,
    GAME_NOT_FOUND,
    LAUNCH_FAILED,
    GAME_RUNNING,
    POSSIBLY_RUNNING,
    FILES_NOT_FOUND,
    FILES_LEFT_BEHIND,
    NOTHING_DELETED,
    LOCATION_NO_HANDLER,
}

data class GamesUiState(
    val isLoading: Boolean = true,
    val installedStateKnown: Boolean = false,
    val games: List<InstalledGameUiState> = emptyList(),
    val pendingUninstall: GameTarget? = null,
    val blockingLocation: GameLocationReference? = null,
    val notice: GamesNotice? = null,
    val noticeGameName: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && installedStateKnown && games.isEmpty()
    val isListVisible: Boolean get() = !isLoading && games.isNotEmpty()
    val isInstalledStateUnsupported: Boolean get() = !isLoading && !installedStateKnown
}

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val installation: GameInstallationService,
    private val launch: GameLaunchService,
    private val locations: GameLocationService,
    private val coordinator: LauncherDataCoordinator,
    private val navigation: NavigationStore,
    private val downloads: DownloadManager,
    private val logger: Logger,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoreUiState(savedState))
    private val uninstalling = MutableStateFlow<Set<String>>(emptySet())
    val state: StateFlow<GamesUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                installation.installedGames,
                coordinator.catalogue,
                coordinator.gamesRevision,
                downloads.downloads,
                uninstalling,
            ) { installed, catalogue, _, downloadRows, uninstallingNames ->
                Triple(installed, catalogue, downloadRows to uninstallingNames)
            }.collectLatest { (installed, catalogue, activity) ->
                coordinator.setGamesLoading(true)
                mutableState.value = mutableState.value.copy(isLoading = true)
                try {
                    if (catalogue.isLoading) return@collectLatest
                    val known = installed as? InstalledGamesState.Available
                    val games = known?.games.orEmpty().map { local ->
                        toUiState(local, catalogue.games, activity.first, activity.second)
                    }
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        installedStateKnown = known != null,
                        games = games,
                    )
                    if (known != null) logger.debug("Installed games loaded: ${games.size} games.")
                } finally {
                    coordinator.setGamesLoading(false)
                }
            }
        }
    }

    fun refresh() {
        coordinator.refreshGamesProjection()
    }

    fun navigateToLibrary() {
        navigation.navigate(LauncherSection.LIBRARY)
    }

    fun update(gameName: String) {
        val row = find(gameName) ?: return
        if (!row.canUpdate) return
        val gameId = row.remote?.gameId ?: return
        logger.debug("Starting update: $gameName.")
        navigation.navigate(LauncherSection.LIBRARY, gameId, LibraryNavigationAction.UPDATE)
    }

    fun switchToSpecialVersion(gameName: String) {
        val row = find(gameName) ?: return
        if (!row.canSwitchSpecialVersion) return
        val gameId = row.remote?.gameId ?: return
        logger.debug("Switching to special version: $gameName.")
        navigation.navigate(LauncherSection.LIBRARY, gameId, LibraryNavigationAction.SPECIAL_VERSION)
    }

    fun play(gameName: String) {
        val row = find(gameName) ?: return
        if (!row.canPlay) return
        logger.debug("Launching game: $gameName.")
        viewModelScope.launch {
            val notice = when (launch.launch(target(row.game))) {
                GameLaunchResult.Launched -> null
                GameLaunchResult.GameNotFound -> GamesNotice.GAME_NOT_FOUND
                GameLaunchResult.LaunchFailed -> GamesNotice.LAUNCH_FAILED
                GameLaunchResult.NotSupportedYet -> GamesNotice.NOT_SUPPORTED_YET
            }
            updatePrompt { it.copy(notice = notice) }
        }
    }

    fun requestUninstall(gameName: String) {
        val row = find(gameName) ?: return
        if (!row.canUninstall) return
        viewModelScope.launch {
            val game = target(row.game)
            when (launch.runningSignal(game)) {
                GameRunningSignal.TRACKED_SESSION -> {
                    logger.info("Uninstall refused, the launcher is still tracking a live process for: ${game.name}.")
                    updatePrompt { it.copy(notice = GamesNotice.GAME_RUNNING, noticeGameName = game.name) }
                }

                GameRunningSignal.POSSIBLY_RUNNING -> {
                    logger.info("${game.name} may still be running; warning the user instead of refusing.")
                    updatePrompt { it.copy(pendingUninstall = game, notice = GamesNotice.POSSIBLY_RUNNING) }
                }

                GameRunningSignal.NOT_RUNNING, GameRunningSignal.NOT_SUPPORTED_YET ->
                    updatePrompt { it.copy(pendingUninstall = game) }
            }
        }
    }

    fun confirmUninstall() {
        val game = mutableState.value.pendingUninstall ?: return
        updatePrompt { it.copy(pendingUninstall = null, notice = null) }
        logger.info("Uninstalling game: ${game.name}.")
        viewModelScope.launch {
            uninstalling.value += game.name
            try {
                val result = installation.uninstall(game)
                if (result.outcome == GameUninstallOutcome.COMPLETED) logger.info("Game uninstalled: ${game.name}.")
                coordinator.purgeStaleDownloads()
                val notice = when (result.outcome) {
                    GameUninstallOutcome.COMPLETED -> null
                    GameUninstallOutcome.FILES_NOT_FOUND -> GamesNotice.FILES_NOT_FOUND
                    GameUninstallOutcome.FILES_LEFT_BEHIND -> GamesNotice.FILES_LEFT_BEHIND
                    GameUninstallOutcome.NOTHING_DELETED -> GamesNotice.NOTHING_DELETED
                    GameUninstallOutcome.GAME_RUNNING -> GamesNotice.GAME_RUNNING
                    GameUninstallOutcome.NOT_SUPPORTED_YET -> GamesNotice.NOT_SUPPORTED_YET
                }
                updatePrompt {
                    it.copy(
                        notice = notice,
                        noticeGameName = game.name,
                        blockingLocation = result.blockingLocation,
                    )
                }
            } finally {
                uninstalling.value -= game.name
            }
        }
    }

    fun openBlockingLocation() {
        val location = mutableState.value.blockingLocation ?: return
        viewModelScope.launch { reportOpen(locations.openBlockingLocation(location)) }
    }

    fun dismissPrompt() {
        mutableState.value.pendingUninstall?.let { logger.debug("Uninstall cancelled by user: ${it.name}.") }
        updatePrompt {
            it.copy(
                pendingUninstall = null,
                notice = null,
                noticeGameName = null,
            )
        }
    }

    private fun reportOpen(result: OpenGameLocationResult) {
        val notice = when (result) {
            OpenGameLocationResult.Opened, OpenGameLocationResult.NotFound -> null
            OpenGameLocationResult.NoHandler -> GamesNotice.LOCATION_NO_HANDLER
            OpenGameLocationResult.NotSupportedYet -> GamesNotice.NOT_SUPPORTED_YET
        }
        updatePrompt { it.copy(notice = notice) }
    }

    private fun updatePrompt(transform: (GamesUiState) -> GamesUiState) {
        val updated = transform(mutableState.value)
        mutableState.value = updated
        savedState[PENDING_UNINSTALL_ID_KEY] = updated.pendingUninstall?.id?.toString()
        savedState[PENDING_UNINSTALL_NAME_KEY] = updated.pendingUninstall?.name
        savedState[BLOCKING_LOCATION_TOKEN_KEY] = updated.blockingLocation?.token
        savedState[BLOCKING_LOCATION_DISPLAY_KEY] = updated.blockingLocation?.displayName
        savedState[NOTICE_KEY] = updated.notice?.name
        savedState[NOTICE_GAME_NAME_KEY] = updated.noticeGameName
    }

    private fun toUiState(
        local: LocalGame,
        catalogue: List<GameInfo>,
        downloadRows: List<dev.jagoba.lostielauncher.model.DownloadSnapshot>,
        uninstallingNames: Set<String>,
    ): InstalledGameUiState {
        val remote = GameIdentityMatcher.findRemote(local, catalogue)
        return InstalledGameUiState(
            game = local,
            remote = remote,
            hasUpdate = remote != null && VersionUtils.isNewerVersion(remote.version, local.version),
            isUpdating = remote != null && downloadRows.any {
                it.gameId == remote.gameId && it.status.isActive
            },
            isUninstalling = local.name in uninstallingNames,
        )
    }

    private fun find(gameName: String): InstalledGameUiState? =
        state.value.games.firstOrNull { it.game.name.equals(gameName, ignoreCase = true) }

    private fun target(game: LocalGame): GameTarget = GameTarget(game.id.takeUnless { it == MissingGameId }, game.name)

    private companion object {
        const val PENDING_UNINSTALL_ID_KEY = "games.pendingUninstall.id"
        const val PENDING_UNINSTALL_NAME_KEY = "games.pendingUninstall.name"
        const val BLOCKING_LOCATION_TOKEN_KEY = "games.blockingLocation.token"
        const val BLOCKING_LOCATION_DISPLAY_KEY = "games.blockingLocation.displayName"
        const val NOTICE_KEY = "games.notice"
        const val NOTICE_GAME_NAME_KEY = "games.noticeGameName"

        fun restoreUiState(savedState: SavedStateHandle): GamesUiState {
            val uninstallName = savedState.get<String>(PENDING_UNINSTALL_NAME_KEY)
            val uninstallId = savedState.get<String>(PENDING_UNINSTALL_ID_KEY)?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            }
            val locationToken = savedState.get<String>(BLOCKING_LOCATION_TOKEN_KEY)
            val noticeName = savedState.get<String>(NOTICE_KEY)
            return GamesUiState(
                pendingUninstall = uninstallName?.let { GameTarget(uninstallId, it) },
                blockingLocation = locationToken?.let {
                    GameLocationReference(it, savedState.get<String>(BLOCKING_LOCATION_DISPLAY_KEY))
                },
                notice = GamesNotice.entries.firstOrNull { it.name == noticeName },
                noticeGameName = savedState.get<String>(NOTICE_GAME_NAME_KEY),
            )
        }
    }
}
