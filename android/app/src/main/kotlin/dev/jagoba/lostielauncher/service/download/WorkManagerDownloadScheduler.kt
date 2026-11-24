package dev.jagoba.lostielauncher.service.download

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WorkManagerDownloadScheduler @Inject constructor(private val workManager: WorkManager) :
    DownloadWorkScheduler {
    override fun nextId(): UUID = UUID.randomUUID()

    override fun enqueue(gameId: String, workId: UUID) {
        val request = OneTimeWorkRequestBuilder<GameDownloadWorker>()
            .setId(workId)
            .setInputData(workDataOf(GameDownloadWorker.GAME_ID_INPUT to gameId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(uniqueName(gameId), ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun cancel(workId: UUID) {
        workManager.cancelWorkById(workId).await()
    }

    private fun uniqueName(gameId: String): String = "$UNIQUE_NAME_PREFIX$gameId"

    private companion object {
        const val UNIQUE_NAME_PREFIX = "game-download:"
    }
}
