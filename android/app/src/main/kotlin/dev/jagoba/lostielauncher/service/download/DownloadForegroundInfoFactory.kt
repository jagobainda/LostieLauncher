package dev.jagoba.lostielauncher.service.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.model.DownloadProgress
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

internal interface DownloadForegroundInfoFactory {
    fun create(workId: UUID, displayName: String, progress: DownloadProgress, strings: Strings): ForegroundInfo
}

@Singleton
internal class AndroidDownloadForegroundInfoFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : DownloadForegroundInfoFactory {
    override fun create(
        workId: UUID,
        displayName: String,
        progress: DownloadProgress,
        strings: Strings,
    ): ForegroundInfo {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, strings.btnDownload, NotificationManager.IMPORTANCE_LOW),
        )
        val percent = progress.percent.roundToInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(strings.btnDownload)
            .setContentText(displayName)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(PROGRESS_MAX, percent, progress.totalBytes == null)
            .build()
        val notificationId = displayName.hashCode() and Int.MAX_VALUE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "game-downloads"
        const val PROGRESS_MAX = 100
    }
}
