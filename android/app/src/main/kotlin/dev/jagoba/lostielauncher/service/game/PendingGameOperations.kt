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
        logger.info(
            "Game installation requested for ${request.file.gameId}; Android installation is not supported yet.",
        )
        return GameInstallationResult.NotSupportedYet
    }

    override fun observeInstallation(gameId: String): Flow<GameInstallationState> {
        logger.info("Installation-state observation requested for $gameId; Android installation is not supported yet.")
        return flowOf(GameInstallationState.Finished(GameInstallationResult.NotSupportedYet))
    }

    override suspend fun uninstall(game: GameTarget): GameUninstallResult {
        logger.info("Game uninstall requested for ${game.name}; Android uninstall is not supported yet.")
        return GameUninstallResult(GameUninstallOutcome.NOT_SUPPORTED_YET)
    }

    override suspend fun findInstalled(game: GameTarget): InstalledGameResult {
        logger.info("Installed-version lookup requested for ${game.name}; Android lookup is not supported yet.")
        return InstalledGameResult.NotSupportedYet
    }
}

@Singleton
internal class PendingGameLaunchService @Inject constructor(private val logger: Logger) : GameLaunchService {
    override val activeSessions: StateFlow<GameActivityState> = MutableStateFlow(GameActivityState.NotSupportedYet)

    override suspend fun launch(game: GameTarget): GameLaunchResult {
        logger.info("Game launch requested for ${game.name}; Android launch is not supported yet.")
        return GameLaunchResult.NotSupportedYet
    }

    override suspend fun runningSignal(game: GameTarget): GameRunningSignal {
        logger.info("Running-state lookup requested for ${game.name}; Android detection is not supported yet.")
        return GameRunningSignal.NOT_SUPPORTED_YET
    }
}

@Singleton
internal class PendingPlaySessionService @Inject constructor(private val logger: Logger) : PlaySessionService {
    override suspend fun recordCompletedSession(session: GamePlaySession): PlaySessionResult {
        logger.info("Play-session accounting requested for ${session.gameId}; Android tracking is not supported yet.")
        return PlaySessionResult.NotSupportedYet
    }
}

@Singleton
internal class PendingGameLocationService @Inject constructor(private val logger: Logger) : GameLocationService {
    override suspend fun helpAvailability(game: GameTarget): GameHelpAvailability {
        logger.info("Help availability requested for ${game.name}; Android help lookup is not supported yet.")
        return GameHelpAvailability.NOT_SUPPORTED_YET
    }

    override suspend fun open(game: GameTarget, location: GameLocation): OpenGameLocationResult {
        logger.info("Game location ${location.name} requested for ${game.name}; Android opening is not supported yet.")
        return OpenGameLocationResult.NotSupportedYet
    }

    override suspend fun openBlockingLocation(location: GameLocationReference): OpenGameLocationResult {
        logger.info("Uninstall blocker location requested for ${location.token}; Android opening is not supported yet.")
        return OpenGameLocationResult.NotSupportedYet
    }
}
