package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameActivityState
import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameLocation
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.OpenGameLocationResult
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.game.GameLaunchService
import dev.jagoba.lostielauncher.service.game.GameLocationService
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
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
    val playtimeMinutes: Int,
    val helpAvailability: GameHelpAvailability,
    val isUpdating: Boolean,
    val isUninstalling: Boolean,
) {
    val canPlay: Boolean get() = !isUpdating && !isUninstalling && (!hasUpdate || !game.type.isNullOrEmpty())
    val canUpdate: Boolean get() = !isUpdating && !isUninstalling && hasUpdate
    val canSwitchSpecialVersion: Boolean get() = !isUpdating && !isUninstalling && remote != null
    val canOpenHelpLocation: Boolean get() = helpAvailability != GameHelpAvailability.NOT_FOUND
    val canOpenGameLocation: Boolean get() = !isUpdating && !isUninstalling
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
    LOCATION_NOT_FOUND,
    LOCATION_NO_HANDLER,
}

data class GamesUiState(
    val isLoading: Boolean = true,
    val installedStateKnown: Boolean = false,
    val games: List<InstalledGameUiState> = emptyList(),
    val pendingUninstall: GameTarget? = null,
    val pendingDownloadGameId: String? = null,
    val blockingLocation: GameLocationReference? = null,
    val notice: GamesNotice? = null,
    val noticeGameName: String? = null,
    val runtimeSupported: Boolean = true,
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
    private val library: LocalLibraryStore,
    private val coordinator: LauncherDataCoordinator,
    private val navigation: NavigationStore,
    private val downloads: DownloadManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GamesUiState())
    private val uninstalling = MutableStateFlow<Set<String>>(emptySet())
    val state: StateFlow<GamesUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            launch.activeSessions.collect { activity ->
                mutableState.value =
                    mutableState.value.copy(runtimeSupported = activity !is GameActivityState.NotSupportedYet)
            }
        }
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
                    val playtimes = if (known == null) emptyMap() else library.getPlaytimes()
                    val games = known?.games.orEmpty().map { local ->
                        toUiState(local, catalogue.games, playtimes, activity.first, activity.second)
                    }
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        installedStateKnown = known != null,
                        games = games,
                    )
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
        navigation.navigate(LauncherSection.LIBRARY, gameId, LibraryNavigationAction.UPDATE)
    }

    fun switchToSpecialVersion(gameName: String) {
        val row = find(gameName) ?: return
        if (!row.canSwitchSpecialVersion) return
        val gameId = row.remote?.gameId ?: return
        navigation.navigate(LauncherSection.LIBRARY, gameId, LibraryNavigationAction.SPECIAL_VERSION)
    }

    fun play(gameName: String) {
        val row = find(gameName) ?: return
        if (!row.canPlay) return
        viewModelScope.launch {
            val notice = when (launch.launch(target(row.game))) {
                GameLaunchResult.Launched -> null
                GameLaunchResult.GameNotFound -> GamesNotice.GAME_NOT_FOUND
                GameLaunchResult.LaunchFailed -> GamesNotice.LAUNCH_FAILED
                GameLaunchResult.NotSupportedYet -> GamesNotice.NOT_SUPPORTED_YET
            }
            mutableState.value = mutableState.value.copy(notice = notice)
        }
    }

    fun requestUninstall(gameName: String) {
        val row = find(gameName) ?: return
        if (!row.canUninstall) return
        viewModelScope.launch {
            val game = target(row.game)
            when (launch.runningSignal(game)) {
                GameRunningSignal.TRACKED_SESSION ->
                    mutableState.value =
                        mutableState.value.copy(notice = GamesNotice.GAME_RUNNING, noticeGameName = game.name)

                GameRunningSignal.POSSIBLY_RUNNING -> mutableState.value = mutableState.value.copy(
                    pendingUninstall = game,
                    notice = GamesNotice.POSSIBLY_RUNNING,
                )

                GameRunningSignal.NOT_RUNNING -> mutableState.value = mutableState.value.copy(pendingUninstall = game)

                GameRunningSignal.NOT_SUPPORTED_YET ->
                    mutableState.value =
                        mutableState.value.copy(notice = GamesNotice.NOT_SUPPORTED_YET)
            }
        }
    }

    fun confirmUninstall() {
        val game = mutableState.value.pendingUninstall ?: return
        mutableState.value = mutableState.value.copy(pendingUninstall = null, notice = null)
        viewModelScope.launch {
            uninstalling.value += game.name
            try {
                val result = installation.uninstall(game)
                val notice = when (result.outcome) {
                    GameUninstallOutcome.COMPLETED -> null
                    GameUninstallOutcome.FILES_NOT_FOUND -> GamesNotice.FILES_NOT_FOUND
                    GameUninstallOutcome.FILES_LEFT_BEHIND -> GamesNotice.FILES_LEFT_BEHIND
                    GameUninstallOutcome.NOTHING_DELETED -> GamesNotice.NOTHING_DELETED
                    GameUninstallOutcome.GAME_RUNNING -> GamesNotice.GAME_RUNNING
                    GameUninstallOutcome.NOT_SUPPORTED_YET -> GamesNotice.NOT_SUPPORTED_YET
                }
                mutableState.value = mutableState.value.copy(
                    notice = notice,
                    noticeGameName = game.name,
                    blockingLocation = result.blockingLocation,
                )
            } finally {
                uninstalling.value -= game.name
            }
        }
    }

    fun openGameLocation(gameName: String) {
        openLocation(gameName, GameLocation.GAME)
    }

    fun openHelpLocation(gameName: String) {
        openLocation(gameName, GameLocation.HELP)
    }

    fun openBlockingLocation() {
        val location = mutableState.value.blockingLocation ?: return
        viewModelScope.launch { reportOpen(locations.openBlockingLocation(location)) }
    }

    fun acceptMissingLocationDownload() {
        val gameId = mutableState.value.pendingDownloadGameId
        dismissPrompt()
        if (gameId != null) navigation.navigate(LauncherSection.LIBRARY, gameId, LibraryNavigationAction.DOWNLOAD)
    }

    fun dismissPrompt() {
        mutableState.value = mutableState.value.copy(
            pendingUninstall = null,
            pendingDownloadGameId = null,
            notice = null,
            noticeGameName = null,
        )
    }

    private fun openLocation(gameName: String, location: GameLocation) {
        val row = find(gameName) ?: return
        if (location == GameLocation.GAME && !row.canOpenGameLocation) return
        if (location == GameLocation.HELP && !row.canOpenHelpLocation) return
        viewModelScope.launch {
            val result = locations.open(target(row.game), location)
            reportOpen(result)
            if (location == GameLocation.GAME && result == OpenGameLocationResult.NotFound) {
                mutableState.value = mutableState.value.copy(pendingDownloadGameId = row.remote?.gameId)
            }
        }
    }

    private fun reportOpen(result: OpenGameLocationResult) {
        val notice = when (result) {
            OpenGameLocationResult.Opened -> null
            OpenGameLocationResult.NotFound -> GamesNotice.LOCATION_NOT_FOUND
            OpenGameLocationResult.NoHandler -> GamesNotice.LOCATION_NO_HANDLER
            OpenGameLocationResult.NotSupportedYet -> GamesNotice.NOT_SUPPORTED_YET
        }
        mutableState.value = mutableState.value.copy(notice = notice)
    }

    private suspend fun toUiState(
        local: LocalGame,
        catalogue: List<GameInfo>,
        playtimes: Map<UUID, Int>,
        downloadRows: List<dev.jagoba.lostielauncher.model.DownloadSnapshot>,
        uninstallingNames: Set<String>,
    ): InstalledGameUiState {
        val remote = GameIdentityMatcher.findRemote(local, catalogue)
        return InstalledGameUiState(
            game = local,
            remote = remote,
            hasUpdate = remote != null && VersionUtils.isNewerVersion(remote.version, local.version),
            playtimeMinutes = playtimes[local.id] ?: 0,
            helpAvailability = locations.helpAvailability(target(local)),
            isUpdating = remote != null && downloadRows.any {
                it.gameId == remote.gameId && it.status in setOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING)
            },
            isUninstalling = local.name in uninstallingNames,
        )
    }

    private fun find(gameName: String): InstalledGameUiState? =
        state.value.games.firstOrNull { it.game.name.equals(gameName, ignoreCase = true) }

    private fun target(game: LocalGame): GameTarget = GameTarget(game.id.takeUnless { it == UUID(0, 0) }, game.name)
}
