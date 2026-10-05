package dev.jagoba.lostielauncher.service.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.android.EntryPointAccessors
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.DownloadProgress
import kotlinx.coroutines.flow.first

internal class GameDownloadWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val gameId = inputData.getString(GAME_ID_INPUT) ?: return Result.failure()
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadWorkerEntryPoint::class.java,
        )
        val runtime = AndroidDownloadWorkerRuntime(dependencies)
        return when (dependencies.runner().run(gameId, id.toString(), runtime)) {
            DownloadWorkerOutcome.SUCCESS -> Result.success()
            DownloadWorkerOutcome.FAILURE -> Result.failure()
        }
    }

    private inner class AndroidDownloadWorkerRuntime(private val dependencies: DownloadWorkerEntryPoint) :
        DownloadWorkerRuntime {
        private var strings: Strings? = null

        override suspend fun setForeground(displayName: String, progress: DownloadProgress) {
            this@GameDownloadWorker.setForeground(
                dependencies.foregroundInfoFactory().create(id, displayName, progress, strings()),
            )
        }

        override suspend fun reportProgress(displayName: String, progress: DownloadProgress) {
            this@GameDownloadWorker.setProgress(
                workDataOf(
                    PROGRESS_PERCENT to progress.percent,
                    PROGRESS_DOWNLOADED_BYTES to progress.downloadedBytes,
                    PROGRESS_TOTAL_BYTES to (progress.totalBytes ?: UNKNOWN_TOTAL_BYTES),
                ),
            )
            setForeground(displayName, progress)
        }

        private suspend fun strings(): Strings = strings ?: stringsFor(
            dependencies.appearanceStore().appearance.first().language,
        ).also { strings = it }
    }

    companion object {
        const val GAME_ID_INPUT = "game_id"
        const val PROGRESS_PERCENT = "percent"
        const val PROGRESS_DOWNLOADED_BYTES = "downloaded_bytes"
        const val PROGRESS_TOTAL_BYTES = "total_bytes"
        const val UNKNOWN_TOTAL_BYTES = -1L
    }
}
