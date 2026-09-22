package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadCommandResult
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

    override val downloads: Flow<List<DownloadSnapshot>> = dao.observeAll().map { entities ->
        entities.map(DownloadEntity::toSnapshot)
    }

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
            return@withLock enqueue(request.args.gameId, workId)
        }
        withContext(dispatchers.io) { fileStore.deleteArtifacts(destination.absolutePath) }
        dao.upsert(
            DownloadEntity(
                gameId = request.args.gameId,
                displayName = request.displayName,
                version = request.args.version,
                key = request.args.key,
                url = url,
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
        enqueue(request.args.gameId, workId)
    }

    override suspend fun pause(gameId: String): DownloadCommandResult = commandGate.withLock {
        val entity = dao.get(gameId) ?: return@withLock DownloadCommandResult.NOT_FOUND
        if (entity.status !in ACTIVE_STATUSES) return@withLock DownloadCommandResult.INVALID_STATE
        dao.setStatus(gameId, DownloadStatus.PAUSED.name)
        scheduler.cancel(UUID.fromString(entity.workId))
        DownloadCommandResult.ACCEPTED
    }

    override suspend fun resume(gameId: String): DownloadCommandResult = commandGate.withLock {
        val entity = dao.get(gameId) ?: return@withLock DownloadCommandResult.NOT_FOUND
        if (entity.status != DownloadStatus.PAUSED.name) return@withLock DownloadCommandResult.INVALID_STATE
        if (hasActiveDownload()) return@withLock DownloadCommandResult.BUSY
        val workId = scheduler.nextId()
        val changed = dao.replaceWork(
            gameId = gameId,
            expectedStatus = DownloadStatus.PAUSED.name,
            workId = workId.toString(),
            status = DownloadStatus.QUEUED.name,
        )
        if (changed == 0) return@withLock DownloadCommandResult.INVALID_STATE
        enqueue(gameId, workId)
    }

    override suspend fun cancel(gameId: String): DownloadCommandResult = commandGate.withLock {
        val entity = dao.get(gameId) ?: return@withLock DownloadCommandResult.NOT_FOUND
        if (entity.status in TERMINAL_STATUSES) return@withLock DownloadCommandResult.INVALID_STATE
        dao.setStatus(gameId, DownloadStatus.CANCELLED.name)
        scheduler.cancel(UUID.fromString(entity.workId))
        withContext(dispatchers.io) { fileStore.deleteArtifacts(entity.destinationPath) }
        DownloadCommandResult.ACCEPTED
    }

    override suspend fun purgeStale(knownGameIds: Set<String>): Int = commandGate.withLock {
        if (knownGameIds.isEmpty()) return@withLock 0
        try {
            val known = knownGameIds.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
            val purgedFiles = withContext(dispatchers.io) {
                fileStore.purgeStale(knownGameIds, clock.instant())
            }
            val inactive = dao.getWithStatuses(INACTIVE_STATUSES)
            val staleRows = withContext(dispatchers.io) {
                inactive.filter { entity ->
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

    private suspend fun hasActiveDownload(): Boolean = dao.countWithStatuses(ACTIVE_STATUSES) > 0

    private suspend fun enqueue(gameId: String, workId: UUID): DownloadCommandResult = try {
        scheduler.enqueue(gameId, workId)
        DownloadCommandResult.ACCEPTED
    } catch (error: Exception) {
        dao.setStatus(gameId, DownloadStatus.FAILED.name, error.message)
        logger.error("Download work could not be scheduled for $gameId.", error)
        DownloadCommandResult.INVALID_STATE
    }

    private companion object {
        val ACTIVE_STATUSES = listOf(DownloadStatus.QUEUED.name, DownloadStatus.DOWNLOADING.name)
        val TERMINAL_STATUSES = setOf(
            DownloadStatus.COMPLETED.name,
            DownloadStatus.CANCELLED.name,
        )
        val INACTIVE_STATUSES = listOf(
            DownloadStatus.PAUSED.name,
            DownloadStatus.COMPLETED.name,
            DownloadStatus.FAILED.name,
            DownloadStatus.PERMISSION_DENIED.name,
            DownloadStatus.CANCELLED.name,
        )
    }
}
