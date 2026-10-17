package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppSettings
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.Appearance
import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadDestination
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.DownloadSnapshot
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.GameActivityState
import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameLocation
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.GameUninstallResult
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.model.InstalledGameResult
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.OpenGameLocationResult
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionLookup
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.game.GameLaunchService
import dev.jagoba.lostielauncher.service.game.GameLocationService
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.service.link.ExternalLinkResult
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class TestSettingsStore : SettingsStore {
    val current = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = current
    override val appearance: Flow<Appearance> = current.map { it.appearance }

    override suspend fun setTheme(theme: AppTheme) {
        current.value = current.value.copy(theme = theme)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        current.value = current.value.copy(language = language)
    }

    override suspend fun setHasSeenWelcome(hasSeenWelcome: Boolean) {
        current.value = current.value.copy(hasSeenWelcome = hasSeenWelcome)
    }

    override suspend fun setAutoUpdate(autoUpdate: Boolean) {
        current.value = current.value.copy(autoUpdate = autoUpdate)
    }

    override suspend fun flush() = Unit
}

internal class TestContentService : ContentService {
    var games = emptyList<GameInfo>()
    var home = HomeContent()
    var blocked = false
    var homeCalls = 0
    var gameCalls = 0
    val homeForces = mutableListOf<Boolean>()
    val flagForces = mutableListOf<Boolean>()
    var gameBarrier: CompletableDeferred<Unit>? = null

    override suspend fun getGames(): List<GameInfo> {
        gameCalls++
        gameBarrier?.await()
        return games
    }

    override suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean): HomeContent {
        homeCalls++
        homeForces += forceRefresh
        return home
    }

    override suspend fun isServerActionBlocked(forceRefresh: Boolean): Boolean {
        flagForces += forceRefresh
        return blocked
    }
}

internal class TestDownloads : DownloadManager {
    val current = MutableStateFlow<List<DownloadSnapshot>>(emptyList())
    override val downloads: Flow<List<DownloadSnapshot>> = current
    val starts = mutableListOf<DownloadRequest>()
    val resumed = mutableListOf<String>()
    val paused = mutableListOf<String>()
    val cancelled = mutableListOf<String>()
    val purged = mutableListOf<Set<String>>()
    var result = DownloadCommandResult.ACCEPTED

    override suspend fun start(request: DownloadRequest): DownloadCommandResult {
        starts += request
        return result
    }

    override suspend fun pause(gameId: String): DownloadCommandResult {
        paused += gameId
        return result
    }

    override suspend fun resume(gameId: String): DownloadCommandResult {
        resumed += gameId
        return result
    }

    override suspend fun cancel(gameId: String): DownloadCommandResult {
        cancelled += gameId
        return result
    }

    override suspend fun purgeStale(knownGameIds: Set<String>): Int {
        purged += knownGameIds
        return 0
    }

    var destination = DownloadDestination("/games/downloads", 64L * 1024 * 1024 * 1024)

    override suspend fun destination(): DownloadDestination = destination
}

internal class TestInstallation : GameInstallationService {
    val current = MutableStateFlow<InstalledGamesState>(InstalledGamesState.NotSupportedYet)
    override val installedGames: StateFlow<InstalledGamesState> = current
    var uninstalls = 0
    var uninstallBarrier: CompletableDeferred<Unit>? = null

    override suspend fun install(request: GameInstallationRequest): GameInstallationResult =
        GameInstallationResult.NotSupportedYet

    var installation: Flow<GameInstallationState> =
        flowOf(GameInstallationState.Finished(GameInstallationResult.NotSupportedYet))

    override fun observeInstallation(gameId: String): Flow<GameInstallationState> = installation

    override suspend fun uninstall(game: GameTarget): GameUninstallResult {
        uninstalls++
        uninstallBarrier?.await()
        return GameUninstallResult(GameUninstallOutcome.NOT_SUPPORTED_YET)
    }

    override suspend fun findInstalled(game: GameTarget): InstalledGameResult = InstalledGameResult.NotSupportedYet
}

internal class TestLaunch : GameLaunchService {
    val current = MutableStateFlow<GameActivityState>(GameActivityState.NotSupportedYet)
    override val activeSessions: StateFlow<GameActivityState> = current
    var launchResult: GameLaunchResult = GameLaunchResult.NotSupportedYet
    var signal = GameRunningSignal.NOT_SUPPORTED_YET
    var launches = 0

    override suspend fun launch(game: GameTarget): GameLaunchResult {
        launches++
        return launchResult
    }

    override suspend fun runningSignal(game: GameTarget): GameRunningSignal = signal
}

internal class TestLocations : GameLocationService {
    var help = GameHelpAvailability.NOT_SUPPORTED_YET
    var openResult: OpenGameLocationResult = OpenGameLocationResult.NotSupportedYet
    var opens = 0

    override suspend fun helpAvailability(game: GameTarget): GameHelpAvailability = help

    override suspend fun open(game: GameTarget, location: GameLocation): OpenGameLocationResult {
        opens++
        return openResult
    }

    override suspend fun openBlockingLocation(location: GameLocationReference): OpenGameLocationResult = openResult
}

internal class TestLibraryStore : LocalLibraryStore {
    var times = emptyMap<UUID, Int>()

    override suspend fun getGames(): List<LocalGame> = emptyList()

    override suspend fun registerGame(game: LocalGame) = Unit

    override suspend fun removeGame(gameName: String) = Unit

    override suspend fun addPlaytime(gameId: UUID, minutes: Int) = Unit

    override suspend fun getPlaytimes(): Map<UUID, Int> = times
}

internal class TestSpecialVersionService : SpecialVersionService {
    var result: SpecialVersionLookup = SpecialVersionLookup.NotFound
    var lookups = 0
    val keys = mutableListOf<String>()

    override suspend fun lookup(key: String): SpecialVersionLookup {
        lookups++
        keys += key
        return result
    }
}

internal class TestExternalLinkService : ExternalLinkService {
    val opened = mutableListOf<ExternalLink>()
    var result = ExternalLinkResult.OPENED

    val openedUrls = mutableListOf<String>()

    override fun open(link: ExternalLink): ExternalLinkResult {
        opened += link
        return result
    }

    override fun openUrl(url: String): ExternalLinkResult {
        openedUrls += url
        return result
    }
}

internal fun testGame(id: UUID? = UUID.randomUUID(), name: String = "Test Game", version: String = "v2.0"): GameInfo =
    GameInfo(id, name, version, 1.0, "", "", null, "/test.zip", "a".repeat(64))

internal fun testDownload(game: GameInfo, status: dev.jagoba.lostielauncher.model.DownloadStatus): DownloadSnapshot =
    DownloadSnapshot(game.gameId, game.name, game.version, status, 50.0, 10.0, 100, 200, null)
