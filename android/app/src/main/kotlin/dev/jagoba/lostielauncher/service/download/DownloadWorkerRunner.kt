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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface DownloadWorkerRuntime {
    fun canStartForeground(): Boolean = true

    suspend fun setForeground(displayName: String, phase: DownloadNotificationPhase)

    suspend fun notifyProgress(displayName: String, phase: DownloadNotificationPhase)

    fun clearProgress(displayName: String)

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
    private val workerGate = Mutex()

    suspend fun run(gameId: String, workId: String, runtime: DownloadWorkerRuntime): DownloadWorkerOutcome =
        workerGate.withLock {
            val entity = dao.get(gameId)
            if (entity?.workId != workId) return@withLock DownloadWorkerOutcome.SUCCESS
            val notifications = NotificationSession(entity.gameId, entity.displayName, runtime)
            try {
                val activeEntity = when {
                    entity.status !in DownloadStatus.ActiveNames -> return@withLock DownloadWorkerOutcome.SUCCESS

                    !fileStore.isManagedPath(entity.destinationPath) ->
                        relocateLegacy(entity, workId) ?: return@withLock DownloadWorkerOutcome.SUCCESS

                    else -> entity
                }
                when (activeEntity.status) {
                    DownloadStatus.INSTALLING.name -> install(activeEntity, workId, runtime, notifications)
                    in DownloadStatus.TransferringNames -> download(activeEntity, workId, runtime, notifications)
                    else -> DownloadWorkerOutcome.SUCCESS
                }
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    val current = dao.get(gameId)
                    if (current?.workId != workId) return@withContext
                    when (current.status) {
                        DownloadStatus.CANCELLED.name -> deleteArtifacts(current)

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
            } finally {
                withContext(NonCancellable) { notifications.clearFallback() }
            }
        }

    private suspend fun relocateLegacy(entity: DownloadEntity, workId: String): DownloadEntity? {
        deleteArtifacts(entity)
        val destinationPath = fileStore.relocatePath(entity.destinationPath)
        val changed = dao.relocate(
            gameId = entity.gameId,
            workId = workId,
            expectedStatuses = DownloadStatus.ActiveNames,
            destinationPath = destinationPath,
            status = DownloadStatus.QUEUED.name,
        )
        if (changed == 0) return null
        logger.info("Relocated download to internal storage: ${entity.gameId}.")
        return entity.copy(
            destinationPath = destinationPath,
            status = DownloadStatus.QUEUED.name,
            percent = 0.0,
            bytesPerSecond = 0.0,
            downloadedBytes = 0,
            totalBytes = null,
            errorMessage = null,
        )
    }

    private suspend fun download(
        entity: DownloadEntity,
        workId: String,
        runtime: DownloadWorkerRuntime,
        notifications: NotificationSession,
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
        notifications.update(DownloadNotificationPhase.Downloading(initialProgress))
        return when (
            val result = transfer.download(
                DownloadTransferRequest(entity.url, entity.destinationPath),
            ) { progress -> updateProgress(entity, workId, progress, notifications) }
        ) {
            DownloadTransferResult.Success -> complete(entity, workId, runtime, notifications)

            DownloadTransferResult.PermissionDenied -> {
                deleteArtifacts(entity)
                finish(
                    entity,
                    workId,
                    DownloadStatus.PERMISSION_DENIED,
                    PERMISSION_DENIED_MESSAGE,
                    clearSensitiveData = true,
                )
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
        notifications: NotificationSession,
    ) {
        val changed = dao.setProgress(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.DOWNLOADING.name,
            percent = progress.percent,
            bytesPerSecond = progress.bytesPerSecond,
            downloadedBytes = progress.downloadedBytes,
            totalBytes = progress.totalBytes,
        )
        if (changed != 0) notifications.update(DownloadNotificationPhase.Downloading(progress))
    }

    private suspend fun complete(
        entity: DownloadEntity,
        workId: String,
        runtime: DownloadWorkerRuntime,
        notifications: NotificationSession,
    ): DownloadWorkerOutcome {
        val changed = dao.complete(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.DOWNLOADING.name,
            status = DownloadStatus.INSTALLING.name,
        )
        if (changed == 0) return DownloadWorkerOutcome.SUCCESS
        logger.info("Download complete: ${entity.gameId}.")
        return install(entity, workId, runtime, notifications)
    }

    private suspend fun install(
        entity: DownloadEntity,
        workId: String,
        runtime: DownloadWorkerRuntime,
        notifications: NotificationSession,
    ): DownloadWorkerOutcome {
        runtime.clearResult(entity.displayName)
        notifications.update(DownloadNotificationPhase.Verifying)
        val result = try {
            deliver(entity, notifications)
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
            clearSensitiveData = installed,
        )
        notifications.clearFallback()
        try {
            runtime.notifyResult(entity.displayName, result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Installation result could not be notified for ${entity.gameId}.", error)
        }
        return if (installed) DownloadWorkerOutcome.SUCCESS else DownloadWorkerOutcome.FAILURE
    }

    private suspend fun deliver(entity: DownloadEntity, notifications: NotificationSession) = coroutineScope {
        val phases = launch(start = CoroutineStart.UNDISPATCHED) {
            installer.observeInstallation(entity.gameId).drop(1).collect { state ->
                state.notificationPhase()?.let { notifications.update(it) }
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
                    expectedSha256 = entity.expectedSha256,
                    variant = entity.variant,
                ),
            )
        } finally {
            phases.cancel()
        }
    }

    private inner class NotificationSession(
        private val gameId: String,
        private val displayName: String,
        private val runtime: DownloadWorkerRuntime,
    ) {
        private var foregroundActive = false
        private var foregroundBlocked = false
        private var fallbackActive = false
        private var backgroundLogged = false
        private var lastPhase: DownloadNotificationPhase? = null

        suspend fun update(phase: DownloadNotificationPhase) {
            val shouldPromote = !foregroundActive && !foregroundBlocked && runtime.canStartForeground()
            if (phase == lastPhase && !shouldPromote) return
            if (shouldPromote) {
                try {
                    runtime.setForeground(displayName, phase)
                    foregroundActive = true
                    fallbackActive = false
                    lastPhase = phase
                    return
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    foregroundBlocked = true
                    logger.error("Download notification could not be updated in foreground for $gameId.", error)
                }
            } else if (!foregroundActive && !foregroundBlocked && !backgroundLogged) {
                backgroundLogged = true
                logger.info("Running download without foreground service while app is in background: $gameId.")
            }
            try {
                runtime.notifyProgress(displayName, phase)
                if (!foregroundActive) fallbackActive = true
                lastPhase = phase
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Download notification could not be updated for $gameId.", error)
            }
        }

        fun clearFallback() {
            if (!fallbackActive) return
            fallbackActive = false
            try {
                runtime.clearProgress(displayName)
            } catch (error: Exception) {
                logger.error("Download progress notification could not be cleared for $gameId.", error)
            }
        }
    }

    private suspend fun finish(
        entity: DownloadEntity,
        workId: String,
        status: DownloadStatus,
        errorMessage: String?,
        clearSensitiveData: Boolean = false,
    ): DownloadWorkerOutcome {
        logger.error("Download failed: ${entity.gameId} ($status): $errorMessage")
        dao.finish(
            gameId = entity.gameId,
            workId = workId,
            expectedStatus = DownloadStatus.DOWNLOADING.name,
            status = status.name,
            errorMessage = errorMessage,
            clearSensitiveData = clearSensitiveData,
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
