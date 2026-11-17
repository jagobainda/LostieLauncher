package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal interface DownloadWorkerRuntime {
    suspend fun setForeground(displayName: String, phase: DownloadNotificationPhase)

    suspend fun reportProgress(displayName: String, progress: DownloadProgress)

    suspend fun notifyResult(displayName: String, result: GameInstallationResult)

    fun clearResult(displayName: String)
}

internal enum class DownloadWorkerOutcome {
    SUCCESS,
    FAILURE,
}

@Singleton
internal class DownloadWorkerRunner @Inject constructor(
    private val dao: DownloadDao,
    private val transfer: DownloadTransfer,
    private val handoff: DownloadedFileHandoff,
    private val installer: GameInstallationService,
    private val fileStore: DownloadFileStore,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) {
    suspend fun run(gameId: String, workId: String, runtime: DownloadWorkerRuntime): DownloadWorkerOutcome {
        val entity = dao.get(gameId)?.takeIf { it.workId == workId } ?: return DownloadWorkerOutcome.SUCCESS
        return try {
            when (entity.status) {
                DownloadStatus.INSTALLING.name -> install(entity, workId, runtime)
                in DownloadStatus.TransferringNames -> download(entity, workId, runtime)
                else -> DownloadWorkerOutcome.SUCCESS
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                val current = dao.get(gameId)
                if (current?.workId != workId) return@withContext
                when (current.status) {
                    DownloadStatus.CANCELLED.name -> deleteArtifacts(entity)

                    DownloadStatus.DOWNLOADING.name -> {
                        dao.setWorkerStatus(gameId, workId, DownloadStatus.QUEUED.name, listOf(current.status))
                        logger.info("Download interrupted by the system, waiting to resume: $gameId.")
                    }
                }
            }
            throw cancelled
        } catch (error: Exception) {
            logger.error("Download worker failed for $gameId.", error)
            finish(entity, workId, DownloadStatus.FAILED, error.message)
        }
    }

    private suspend fun download(
        entity: DownloadEntity,
        workId: String,
        runtime: DownloadWorkerRuntime,
    ): DownloadWorkerOutcome {
        val changed = dao.setWorkerStatus(
            gameId = entity.gameId,
            workId = workId,
            status = DownloadStatus.DOWNLOADING.name,
            expectedStatuses = DownloadStatus.TransferringNames,
        )
        if (changed == 0) return DownloadWorkerOutcome.SUCCESS
        val initialProgress = DownloadProgress(
            percent = entity.percent,
            bytesPerSecond = 0.0,
            downloadedBytes = entity.downloadedBytes,
            totalBytes = entity.totalBytes,
        )
        runtime.setForeground(entity.displayName, DownloadNotificationPhase.Downloading(initialProgress))
        return when (
            val result = transfer.download(
                DownloadTransferRequest(entity.url, entity.destinationPath),
            ) { progress -> updateProgress(entity, workId, progress, runtime) }
        ) {
            DownloadTransferResult.Success -> complete(entity, workId, runtime)

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

    private suspend fun complete(
        entity: DownloadEntity,
        workId: String,
        runtime: DownloadWorkerRuntime,
    ): DownloadWorkerOutcome {
        val changed = dao.complete(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.DOWNLOADING.name,
            status = DownloadStatus.INSTALLING.name,
        )
        if (changed == 0) return DownloadWorkerOutcome.SUCCESS
        logger.info("Download complete: ${entity.gameId}.")
        return install(entity, workId, runtime)
    }

    private suspend fun install(
        entity: DownloadEntity,
        workId: String,
        runtime: DownloadWorkerRuntime,
    ): DownloadWorkerOutcome {
        runtime.clearResult(entity.displayName)
        showPhase(entity, DownloadNotificationPhase.Verifying, runtime)
        val result = try {
            deliver(entity, runtime)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Downloaded file handoff failed for ${entity.gameId}.", error)
            GameInstallationResult.ExtractionFailed
        }
        val installed = result is GameInstallationResult.Installed
        dao.finish(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.INSTALLING.name,
            status = if (installed) DownloadStatus.COMPLETED.name else DownloadStatus.INSTALLATION_FAILED.name,
            errorMessage = result.takeUnless { installed }?.toString(),
        )
        try {
            runtime.notifyResult(entity.displayName, result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Installation result could not be notified for ${entity.gameId}.", error)
        }
        return if (installed) DownloadWorkerOutcome.SUCCESS else DownloadWorkerOutcome.FAILURE
    }

    private suspend fun deliver(entity: DownloadEntity, runtime: DownloadWorkerRuntime) = coroutineScope {
        val phases = launch(start = CoroutineStart.UNDISPATCHED) {
            installer.observeInstallation(entity.gameId).drop(1).collect { state ->
                state.notificationPhase()?.let { showPhase(entity, it, runtime) }
            }
        }
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
        } finally {
            phases.cancel()
        }
    }

    private suspend fun showPhase(
        entity: DownloadEntity,
        phase: DownloadNotificationPhase,
        runtime: DownloadWorkerRuntime,
    ) {
        try {
            runtime.setForeground(entity.displayName, phase)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Download notification could not be updated for ${entity.gameId}.", error)
        }
    }

    private suspend fun finish(
        entity: DownloadEntity,
        workId: String,
        status: DownloadStatus,
        errorMessage: String?,
    ): DownloadWorkerOutcome {
        logger.error("Download failed: ${entity.gameId} ($status): $errorMessage")
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
    }
}
