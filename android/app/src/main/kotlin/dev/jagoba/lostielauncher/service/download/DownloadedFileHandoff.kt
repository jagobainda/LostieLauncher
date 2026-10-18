package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionLookup
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionService
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadedFileHandoff {
    suspend fun deliver(file: DownloadedFile)
}

@Singleton
internal class InstallDownloadedFileHandoff @Inject constructor(
    private val installer: GameInstallationService,
    private val coordinator: LauncherDataCoordinator,
    private val content: ContentService,
    private val specialVersions: SpecialVersionService,
    private val logger: Logger,
) : DownloadedFileHandoff {
    override suspend fun deliver(file: DownloadedFile) {
        val game = findGame(file.gameId)
        val special = file.key?.let { (specialVersions.lookup(it) as? SpecialVersionLookup.Found)?.config }
        val result = installer.install(
            GameInstallationRequest(
                file = file,
                catalogueId = game?.id,
                expectedSha256 = special?.sha256 ?: game?.sha256,
                variant = special?.type,
            ),
        )
        logger.info("Downloaded file handoff result for ${file.gameId}: $result.")
    }

    private suspend fun findGame(gameId: String): GameInfo? =
        coordinator.catalogue.value.games.firstOrNull { it.gameId == gameId }
            ?: content.getGames().firstOrNull { it.gameId == gameId }
}
