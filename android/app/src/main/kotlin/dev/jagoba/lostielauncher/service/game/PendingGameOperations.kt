package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GameActivityState
import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameLocation
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GamePlaySession
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.GameUninstallResult
import dev.jagoba.lostielauncher.model.InstalledGameResult
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.OpenGameLocationResult
import dev.jagoba.lostielauncher.model.PlaySessionResult
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

@Singleton
internal class PendingGameInstallationService @Inject constructor(private val logger: Logger) :
    GameInstallationService {
    override val installedGames: StateFlow<InstalledGamesState> = MutableStateFlow(InstalledGamesState.NotSupportedYet)

    override suspend fun install(request: GameInstallationRequest): GameInstallationResult {
        // TODO-ANDROID-GAME-RUNTIME-01: Decide the Android artifact and install strategy because the downloaded Windows ZIP cannot run on Android.
        logger.info(
            "Game installation requested for ${request.file.gameId}; Android installation is not supported yet.",
        )
        return GameInstallationResult.NotSupportedYet
    }

    override fun observeInstallation(gameId: String): Flow<GameInstallationState> {
        // TODO-ANDROID-GAME-RUNTIME-09: Decide durable phase and result storage because a worker can finish while no screen is alive.
        logger.info("Installation-state observation requested for $gameId; Android installation is not supported yet.")
        return flowOf(GameInstallationState.Finished(GameInstallationResult.NotSupportedYet))
    }

    override suspend fun uninstall(game: GameTarget): GameUninstallResult {
        // TODO-ANDROID-GAME-RUNTIME-02: Decide whether uninstall removes owned files or requests package removal because Android packages are system managed.
        logger.info("Game uninstall requested for ${game.name}; Android uninstall is not supported yet.")
        return GameUninstallResult(GameUninstallOutcome.NOT_SUPPORTED_YET)
    }

    override suspend fun findInstalled(game: GameTarget): InstalledGameResult {
        // TODO-ANDROID-GAME-RUNTIME-03: Decide the installed-state authority because Room entries, owned files and packages can disagree.
        logger.info("Installed-version lookup requested for ${game.name}; Android lookup is not supported yet.")
        return InstalledGameResult.NotSupportedYet
    }
}

@Singleton
internal class PendingGameLaunchService @Inject constructor(private val logger: Logger) : GameLaunchService {
    override val activeSessions: StateFlow<GameActivityState> = MutableStateFlow(GameActivityState.NotSupportedYet)

    override suspend fun launch(game: GameTarget): GameLaunchResult {
        // TODO-ANDROID-GAME-RUNTIME-04: Decide the game component and launch route because a Windows executable has no Android activity.
        logger.info("Game launch requested for ${game.name}; Android launch is not supported yet.")
        return GameLaunchResult.NotSupportedYet
    }

    override suspend fun runningSignal(game: GameTarget): GameRunningSignal {
        // TODO-ANDROID-GAME-RUNTIME-05: Decide how to observe active sessions because Android exposes neither another app's process handle nor its file lock.
        logger.info("Running-state lookup requested for ${game.name}; Android detection is not supported yet.")
        return GameRunningSignal.NOT_SUPPORTED_YET
    }
}

@Singleton
internal class PendingPlaySessionService @Inject constructor(private val logger: Logger) : PlaySessionService {
    override suspend fun recordCompletedSession(session: GamePlaySession): PlaySessionResult {
        // TODO-ANDROID-GAME-RUNTIME-06: Decide how to detect game exit and elapsed time because the launcher may die while a game runs.
        logger.info("Play-session accounting requested for ${session.gameId}; Android tracking is not supported yet.")
        return PlaySessionResult.NotSupportedYet
    }
}

@Singleton
internal class PendingGameLocationService @Inject constructor(private val logger: Logger) : GameLocationService {
    override suspend fun helpAvailability(game: GameTarget): GameHelpAvailability {
        // TODO-ANDROID-GAME-RUNTIME-10: Decide what counts as game help because Android packages and app-owned data expose different resources.
        logger.info("Help availability requested for ${game.name}; Android help lookup is not supported yet.")
        return GameHelpAvailability.NOT_SUPPORTED_YET
    }

    override suspend fun open(game: GameTarget, location: GameLocation): OpenGameLocationResult {
        // TODO-ANDROID-GAME-RUNTIME-07: Decide how to expose game and help files because Android document handlers cannot open private paths directly.
        logger.info("Game location ${location.name} requested for ${game.name}; Android opening is not supported yet.")
        return OpenGameLocationResult.NotSupportedYet
    }

    override suspend fun openBlockingLocation(location: GameLocationReference): OpenGameLocationResult {
        // TODO-ANDROID-GAME-RUNTIME-11: Decide how to expose a partial-uninstall blocker because its location may be an app file, document URI or package.
        logger.info("Uninstall blocker location requested for ${location.token}; Android opening is not supported yet.")
        return OpenGameLocationResult.NotSupportedYet
    }
}
