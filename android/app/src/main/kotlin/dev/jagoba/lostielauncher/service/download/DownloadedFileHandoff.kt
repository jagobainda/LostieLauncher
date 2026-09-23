package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadedFileHandoff {
    suspend fun deliver(file: DownloadedFile)
}

@Singleton
internal class InstallDownloadedFileHandoff @Inject constructor(
    private val installer: GameInstallationService,
    private val logger: Logger,
) : DownloadedFileHandoff {
    override suspend fun deliver(file: DownloadedFile) {
        // TODO-ANDROID-GAME-RUNTIME-08: Decide how to persist the catalogue UUID, hash and variant because the worker can outlive the catalogue screen.
        val result = installer.install(
            GameInstallationRequest(
                file = file,
                catalogueId = null,
                expectedSha256 = null,
                variant = null,
            ),
        )
        logger.info("Downloaded file handoff result for ${file.gameId}: $result.")
    }
}
