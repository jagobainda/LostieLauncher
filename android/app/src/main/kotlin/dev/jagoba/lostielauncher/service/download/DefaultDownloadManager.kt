package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadDestination
import dev.jagoba.lostielauncher.model.DownloadOptions
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.DownloadSnapshot
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.util.download.DownloadUrlResolver
import dev.jagoba.lostielauncher.util.log.Logger
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
internal class DefaultDownloadManager @Inject constructor(
    private val dao: DownloadDao,
    private val scheduler: DownloadWorkScheduler,
    private val fileStore: DownloadFileStore,
    private val downloadOptions: DownloadOptions,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
    private val logger: Logger,
) : DownloadManager {
    private val commandGate = Mutex()

    override val downloads: Flow<List<DownloadSnapshot>> = dao.observeAll()
        .catch { cause ->
            if (cause is CancellationException || cause !is Exception) throw cause
            logger.error("Could not read downloads.", cause)
            emit(emptyList())
        }
        .map { entities -> entities.map(DownloadEntity::toSnapshot) }

    override suspend fun start(request: DownloadRequest): DownloadCommandResult = commandGate.withLock {
        if (hasActiveDownload()) return@withLock DownloadCommandResult.BUSY
        val workId = scheduler.nextId()
        val destination = fileStore.destinationFor(request.args)
        val url = runCatching { DownloadUrlResolver.resolve(downloadOptions.baseUrl, request.args) }
            .getOrElse {
                logger.error("Download URL could not be resolved for ${request.args.gameId}.", it)
                return@withLock DownloadCommandResult.INVALID_STATE
            }
        val existing = dao.get(request.args.gameId)
        if (
            existing?.status == DownloadStatus.FAILED.name &&
            existing.url == url &&
            (
                existing.expectedSha256 == null ||
                    (existing.expectedSha256 == request.expectedSha256 && existing.variant == request.variant)
                ) &&
            existing.destinationPath == destination.absolutePath &&
            withContext(dispatchers.io) { fileStore.hasResumablePartial(destination.absolutePath) }
        ) {
            val changed = dao.replaceWork(
                gameId = request.args.gameId,
                expectedStatus = DownloadStatus.FAILED.name,
                workId = workId.toString(),
                status = DownloadStatus.QUEUED.name,
            )
            if (changed == 0) return@withLock DownloadCommandResult.INVALID_STATE
            logger.info("Retrying download from its partial: ${request.args.gameId} ${request.args.version}.")
            return@withLock enqueue(request.args.gameId, workId)
        }
        withContext(dispatchers.io) {
            fileStore.deleteArtifacts(destination.absolutePath)
            if (existing != null && existing.destinationPath != destination.absolutePath) {
                fileStore.deleteArtifacts(existing.destinationPath)
            }
        }
        dao.upsert(
            DownloadEntity(
                gameId = request.args.gameId,
                displayName = request.displayName,
                version = request.args.version,
                key = request.args.key,
                url = url,
                expectedSha256 = request.expectedSha256,
                variant = request.variant,
                destinationPath = destination.absolutePath,
                workId = workId.toString(),
                status = DownloadStatus.QUEUED.name,
                percent = 0.0,
                bytesPerSecond = 0.0,
                downloadedBytes = 0,
                totalBytes = null,
                errorMessage = null,
                createdAtEpochMillis = clock.millis(),
            ),
        )
        logger.info("Downloading: ${request.args.gameId} ${request.args.version}.")
        enqueue(request.args.gameId, workId)
    }

    override suspend fun pause(gameId: String): DownloadCommandResult = commandGate.withLock {
        val entity = dao.get(gameId) ?: return@withLock DownloadCommandResult.NOT_FOUND
        if (entity.status !in DownloadStatus.TransferringNames) return@withLock DownloadCommandResult.INVALID_STATE
        dao.setStatus(gameId, DownloadStatus.PAUSED.name)
        scheduler.cancel(UUID.fromString(entity.workId))
        logger.info("Download paused: $gameId.")
        DownloadCommandResult.ACCEPTED
    }

    override suspend fun resume(gameId: String): DownloadCommandResult = commandGate.withLock {
        val entity = dao.get(gameId) ?: return@withLock DownloadCommandResult.NOT_FOUND
        if (entity.status != DownloadStatus.PAUSED.name) return@withLock DownloadCommandResult.INVALID_STATE
        if (hasActiveDownload()) return@withLock DownloadCommandResult.BUSY
        val workId = scheduler.nextId()
        if (!fileStore.isManagedPath(entity.destinationPath)) {
            withContext(dispatchers.io) { fileStore.deleteArtifacts(entity.destinationPath) }
            dao.upsert(
                entity.copy(
                    destinationPath = fileStore.relocatePath(entity.destinationPath),
                    workId = workId.toString(),
                    status = DownloadStatus.QUEUED.name,
                    percent = 0.0,
                    bytesPerSecond = 0.0,
                    downloadedBytes = 0,
                    totalBytes = null,
                    errorMessage = null,
                ),
            )
            logger.info("Restarting download in internal storage: $gameId.")
            return@withLock enqueue(gameId, workId)
        }
        val changed = dao.replaceWork(
            gameId = gameId,
            expectedStatus = DownloadStatus.PAUSED.name,
            workId = workId.toString(),
            status = DownloadStatus.QUEUED.name,
        )
        if (changed == 0) return@withLock DownloadCommandResult.INVALID_STATE
        logger.info("Download resumed: $gameId.")
        enqueue(gameId, workId)
    }

    override suspend fun retry(gameId: String): DownloadCommandResult = commandGate.withLock {
        val entity = dao.get(gameId) ?: return@withLock DownloadCommandResult.NOT_FOUND
        if (entity.status !=
            DownloadStatus.INSTALLATION_FAILED.name
        ) {
            return@withLock DownloadCommandResult.INVALID_STATE
        }
        if (hasActiveDownload()) return@withLock DownloadCommandResult.BUSY
        val workId = scheduler.nextId()
        val managed = fileStore.isManagedPath(entity.destinationPath)
        val archived = managed && withContext(dispatchers.io) { fileStore.hasArchive(entity.destinationPath) }
        val retried = if (archived) {
            entity.copy(status = DownloadStatus.INSTALLING.name)
        } else {
            if (!managed) withContext(dispatchers.io) { fileStore.deleteArtifacts(entity.destinationPath) }
            entity.copy(
                destinationPath = if (managed) {
                    entity.destinationPath
                } else {
                    fileStore.relocatePath(entity.destinationPath)
                },
                status = DownloadStatus.QUEUED.name,
                percent = 0.0,
                downloadedBytes = 0,
                totalBytes = null,
            )
        }
        dao.upsert(retried.copy(workId = workId.toString(), errorMessage = null))
        logger.info("Retrying ${if (archived) "installation" else "download"}: $gameId.")
        enqueue(gameId, workId)
    }

    override suspend fun cancel(gameId: String): DownloadCommandResult = commandGate.withLock {
        val entity = dao.get(gameId) ?: return@withLock DownloadCommandResult.NOT_FOUND
        if (entity.status.toDownloadStatus().isFinished) return@withLock DownloadCommandResult.INVALID_STATE
        dao.setStatus(gameId, DownloadStatus.CANCELLED.name, clearSensitiveData = true)
        scheduler.cancel(UUID.fromString(entity.workId))
        withContext(dispatchers.io) { fileStore.deleteArtifacts(entity.destinationPath) }
        logger.info("Download cancelled: $gameId.")
        DownloadCommandResult.ACCEPTED
    }

    override suspend fun purgeStale(knownGameIds: Set<String>): Int = commandGate.withLock {
        if (knownGameIds.isEmpty()) return@withLock 0
        try {
            val ongoing = dao.getWithStatuses(DownloadStatus.OngoingNames)
            val (failedInstallations, activeOrPaused) = ongoing.partition {
                it.status == DownloadStatus.INSTALLATION_FAILED.name
            }
            val protectedPaths = activeOrPaused.mapTo(mutableSetOf()) { it.destinationPath }
            val retainedArchivePaths = failedInstallations.mapTo(mutableSetOf()) { it.destinationPath }
            val known = knownGameIds.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
            val purgedFiles = withContext(dispatchers.io) {
                fileStore.purgeStale(knownGameIds, clock.instant(), protectedPaths, retainedArchivePaths)
            }
            val purgeable = dao.getWithStatuses(DownloadStatus.PurgeableNames)
            val staleRows = withContext(dispatchers.io) {
                purgeable.filter { entity ->
                    entity.gameId.lowercase(Locale.ROOT) !in known || !fileStore.hasArtifacts(entity.destinationPath)
                }
            }
            val purgedRows = if (staleRows.isEmpty()) 0 else dao.deleteByGameIds(staleRows.map(DownloadEntity::gameId))
            if (purgedFiles > 0 || purgedRows > 0) {
                logger.info("Purged $purgedFiles stale file(s) and $purgedRows stale row(s) from the downloads cache.")
            }
            purgedFiles + purgedRows
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Download cache maintenance failed.", error)
            0
        }
    }

    override suspend fun destination(): DownloadDestination = withContext(dispatchers.io) {
        fileStore.destination()
    }

    private suspend fun hasActiveDownload(): Boolean = dao.countWithStatuses(DownloadStatus.ActiveNames) > 0

    private suspend fun enqueue(gameId: String, workId: UUID): DownloadCommandResult = try {
        scheduler.enqueue(gameId, workId)
        DownloadCommandResult.ACCEPTED
    } catch (error: Exception) {
        dao.setStatus(gameId, DownloadStatus.FAILED.name, error.message)
        logger.error("Download work could not be scheduled for $gameId.", error)
        DownloadCommandResult.INVALID_STATE
    }
}
