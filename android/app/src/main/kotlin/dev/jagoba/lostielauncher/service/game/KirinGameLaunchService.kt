package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.gameSlug
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
internal class KirinGameLaunchService @Inject constructor(
    pending: PendingGameLaunchService,
    private val shortcut: KirinShortcut,
    private val options: KirinOptions,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : GameLaunchService by pending {
    override suspend fun launch(game: GameTarget): GameLaunchResult {
        val directory = File(options.gamesRoot, gameSlug(game.name))
        if (!withContext(dispatchers.io) { directory.isDirectory }) {
            logger.error("Game directory not found for ${game.name}: ${directory.path}.")
            return GameLaunchResult.GameNotFound
        }
        return try {
            shortcut.open(directory)
            logger.info("Game launched through Kirin: ${game.name}.")
            GameLaunchResult.Launched
        } catch (error: Exception) {
            logger.error("Kirin could not launch ${game.name}.", error)
            GameLaunchResult.LaunchFailed
        }
    }
}
