package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.InstalledGameManifestLookup
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
    private val kirinSetup: KirinSetupService,
    private val manifests: InstalledGameManifestStore,
    private val options: KirinOptions,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : GameLaunchService by pending {
    override suspend fun launch(game: GameTarget): GameLaunchResult {
        val gameId = gameSlug(game.name)
        val directory = File(options.gamesRoot, gameId)
        val exists = withContext(dispatchers.io) {
            gameId.isNotEmpty() &&
                directory.canonicalFile.parentFile == options.gamesRoot.canonicalFile &&
                directory.isDirectory
        }
        if (!exists) {
            logger.error("Game directory not found for ${game.name}: ${directory.path}.")
            return GameLaunchResult.GameNotFound
        }
        if (!kirinSetup.isOfficialKirinInstalled()) {
            logger.error("Game launch refused because the official Kirin signer could not be verified.")
            return GameLaunchResult.LaunchFailed
        }
        when (val lookup = manifests.read(gameId)) {
            is InstalledGameManifestLookup.Found -> {
                val verification = withContext(dispatchers.io) {
                    InstalledGameIntegrity.verify(directory, lookup.manifest, options.archiveBufferSizeBytes)
                }
                if (verification is InstalledGameIntegrityResult.Mismatch) {
                    logger.error("Game integrity check failed for ${game.name} ($gameId): ${verification.reason}.")
                    return GameLaunchResult.IntegrityMismatch
                }
            }

            InstalledGameManifestLookup.NotFound ->
                logger.info("No integrity manifest found for $gameId; launching legacy installation.")

            InstalledGameManifestLookup.Corrupted -> {
                logger.error("Game integrity manifest is unreadable for ${game.name} ($gameId); refusing launch.")
                return GameLaunchResult.IntegrityMismatch
            }
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
