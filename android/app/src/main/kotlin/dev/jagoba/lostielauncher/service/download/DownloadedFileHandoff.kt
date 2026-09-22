package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadedFileHandoff {
    suspend fun deliver(file: DownloadedFile)
}

@Singleton
internal class PendingDownloadedFileHandoff @Inject constructor(private val logger: Logger) : DownloadedFileHandoff {
    override suspend fun deliver(file: DownloadedFile) {
        logger.info("Downloaded file is ready for downstream handling: ${file.gameId}.")
    }
}
