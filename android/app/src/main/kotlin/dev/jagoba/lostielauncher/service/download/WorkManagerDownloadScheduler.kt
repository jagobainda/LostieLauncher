package dev.jagoba.lostielauncher.service.download

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
internal class WorkManagerDownloadScheduler @Inject constructor(private val workManager: WorkManager) :
    DownloadWorkScheduler {
    override fun nextId(): UUID = UUID.randomUUID()

    override fun enqueue(gameId: String, workId: UUID) {
        val request = OneTimeWorkRequestBuilder<GameDownloadWorker>()
            .setId(workId)
            .setInputData(workDataOf(GameDownloadWorker.GAME_ID_INPUT to gameId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(DOWNLOAD_TAG)
            .build()
        workManager.enqueueUniqueWork(uniqueName(gameId), ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun cancel(workId: UUID) {
        val future = workManager.cancelWorkById(workId).result
        suspendCancellableCoroutine { continuation ->
            future.addListener(
                {
                    try {
                        future.get()
                        if (continuation.isActive) continuation.resume(Unit)
                    } catch (error: ExecutionException) {
                        if (continuation.isActive) continuation.resumeWithException(error.cause ?: error)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                },
                DirectExecutor,
            )
        }
    }

    private fun uniqueName(gameId: String): String = "$UNIQUE_NAME_PREFIX$gameId"

    private companion object {
        const val DOWNLOAD_TAG = "game-download"
        const val UNIQUE_NAME_PREFIX = "game-download:"
        val DirectExecutor = Executor(Runnable::run)
    }
}
