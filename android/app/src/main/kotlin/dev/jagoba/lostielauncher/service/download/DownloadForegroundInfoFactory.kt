package dev.jagoba.lostielauncher.service.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.content.Strings
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal interface DownloadForegroundInfoFactory {
    fun create(workId: UUID, displayName: String, phase: DownloadNotificationPhase, strings: Strings): ForegroundInfo
}

@Singleton
internal class AndroidDownloadForegroundInfoFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : DownloadForegroundInfoFactory {
    private val openLauncher: PendingIntent? by lazy {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }

    override fun create(
        workId: UUID,
        displayName: String,
        phase: DownloadNotificationPhase,
        strings: Strings,
    ): ForegroundInfo {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, strings.btnDownload, NotificationManager.IMPORTANCE_LOW),
        )
        val content = downloadNotificationContent(displayName, phase, strings)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.volcarona_primary_fg))
            .setContentTitle(content.title)
            .setSubText(content.status)
            .setContentText(content.detail)
            .setProgress(DOWNLOAD_NOTIFICATION_MAX_PROGRESS, content.percent, content.indeterminate)
            .setContentIntent(openLauncher)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setShowWhen(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
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
    }
}
