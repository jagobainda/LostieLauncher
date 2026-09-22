package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Isolates WorkManager callbacks from the download state machine. */
internal interface DownloadWorkerRuntime {
    suspend fun setForeground(displayName: String, progress: DownloadProgress)

    suspend fun reportProgress(displayName: String, progress: DownloadProgress)
}

internal enum class DownloadWorkerOutcome {
    SUCCESS,
    FAILURE,
}

/** Coordinates one durable transfer without depending on Android worker types. */
@Singleton
internal class DownloadWorkerRunner @Inject constructor(
    private val dao: DownloadDao,
    private val transfer: DownloadTransfer,
    private val handoff: DownloadedFileHandoff,
    private val fileStore: DownloadFileStore,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) {
    suspend fun run(gameId: String, workId: String, runtime: DownloadWorkerRuntime): DownloadWorkerOutcome {
        val entity = dao.get(gameId) ?: return DownloadWorkerOutcome.SUCCESS
        if (entity.workId != workId || entity.status !in STARTABLE_STATUSES) return DownloadWorkerOutcome.SUCCESS
        val changed = dao.setWorkerStatus(
            gameId = gameId,
            workId = workId,
            status = DownloadStatus.DOWNLOADING.name,
            expectedStatuses = STARTABLE_STATUSES,
        )
        if (changed == 0) return DownloadWorkerOutcome.SUCCESS
        val initialProgress = DownloadProgress(
            percent = entity.percent,
            bytesPerSecond = 0.0,
            downloadedBytes = entity.downloadedBytes,
            totalBytes = entity.totalBytes,
        )
        return try {
            runtime.setForeground(entity.displayName, initialProgress)
            when (
                val result = transfer.download(
                    DownloadTransferRequest(entity.url, entity.destinationPath),
                ) { progress -> updateProgress(entity, workId, progress, runtime) }
            ) {
                DownloadTransferResult.Success -> complete(entity, workId)

                DownloadTransferResult.PermissionDenied -> {
                    deleteArtifacts(entity)
                    finish(entity, workId, DownloadStatus.PERMISSION_DENIED, PERMISSION_DENIED_MESSAGE)
                }

                is DownloadTransferResult.Failed -> finish(
                    entity,
                    workId,
                    DownloadStatus.FAILED,
                    result.message,
                )
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                val current = dao.get(gameId)
                if (current?.workId == workId && current.status == DownloadStatus.CANCELLED.name) {
                    deleteArtifacts(entity)
                }
            }
            throw cancelled
        } catch (error: Exception) {
            logger.error("Download worker failed for $gameId.", error)
            finish(entity, workId, DownloadStatus.FAILED, error.message)
        }
    }

    private suspend fun updateProgress(
        entity: DownloadEntity,
        workId: String,
        progress: DownloadProgress,
        runtime: DownloadWorkerRuntime,
    ) {
        val changed = dao.setProgress(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.DOWNLOADING.name,
            status = DownloadStatus.DOWNLOADING.name,
            percent = progress.percent,
            bytesPerSecond = progress.bytesPerSecond,
            downloadedBytes = progress.downloadedBytes,
            totalBytes = progress.totalBytes,
        )
        if (changed != 0) runtime.reportProgress(entity.displayName, progress)
    }

    private suspend fun complete(entity: DownloadEntity, workId: String): DownloadWorkerOutcome {
        val changed = dao.complete(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.DOWNLOADING.name,
            status = DownloadStatus.COMPLETED.name,
        )
        if (changed == 0) return DownloadWorkerOutcome.SUCCESS
        try {
            handoff.deliver(
                DownloadedFile(
                    gameId = entity.gameId,
                    displayName = entity.displayName,
                    version = entity.version,
                    key = entity.key,
                    path = entity.destinationPath,
                ),
            )
        } catch (error: Exception) {
            logger.error("Downloaded file handoff failed for ${entity.gameId}.", error)
        }
        return DownloadWorkerOutcome.SUCCESS
    }

    private suspend fun finish(
        entity: DownloadEntity,
        workId: String,
        status: DownloadStatus,
        errorMessage: String?,
    ): DownloadWorkerOutcome {
        dao.finish(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.DOWNLOADING.name,
            status = status.name,
            errorMessage = errorMessage,
        )
        return DownloadWorkerOutcome.FAILURE
    }

    private suspend fun deleteArtifacts(entity: DownloadEntity) {
        try {
            withContext(dispatchers.io) { fileStore.deleteArtifacts(entity.destinationPath) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Download artifacts could not be deleted for ${entity.gameId}.", error)
        }
    }

    private companion object {
        const val PERMISSION_DENIED_MESSAGE = "Download storage permission was denied."
        val STARTABLE_STATUSES = listOf(DownloadStatus.QUEUED.name, DownloadStatus.DOWNLOADING.name)
    }
}
