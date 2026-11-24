package dev.jagoba.lostielauncher.service.download

import android.app.Notification
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
import dev.jagoba.lostielauncher.model.GameInstallationResult
import javax.inject.Inject
import javax.inject.Singleton

internal interface DownloadForegroundInfoFactory {
    fun create(displayName: String, phase: DownloadNotificationPhase, strings: Strings): ForegroundInfo

    fun notifyProgress(displayName: String, phase: DownloadNotificationPhase, strings: Strings)

    fun clearProgress(displayName: String)

    fun notifyResult(displayName: String, result: GameInstallationResult, strings: Strings)

    fun clearResult(displayName: String)
}

@Singleton
internal class AndroidDownloadForegroundInfoFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : DownloadForegroundInfoFactory {
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

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

    override fun create(displayName: String, phase: DownloadNotificationPhase, strings: Strings): ForegroundInfo {
        val notification = progressNotification(displayName, phase, strings, foreground = true)
        val notificationId = progressNotificationId(displayName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    override fun notifyProgress(displayName: String, phase: DownloadNotificationPhase, strings: Strings) {
        val notification = progressNotification(displayName, phase, strings, foreground = false)
        notificationManager.notify(progressNotificationId(displayName), notification)
    }

    override fun clearProgress(displayName: String) {
        notificationManager.cancel(progressNotificationId(displayName))
    }

    override fun notifyResult(displayName: String, result: GameInstallationResult, strings: Strings) {
        val text = installationResultNotificationText(result, strings) ?: return
        val notification = notificationBuilder(strings)
            .setContentTitle(displayName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(resultNotificationId(displayName), notification)
    }

    override fun clearResult(displayName: String) {
        notificationManager.cancel(resultNotificationId(displayName))
    }

    private fun progressNotification(
        displayName: String,
        phase: DownloadNotificationPhase,
        strings: Strings,
        foreground: Boolean,
    ): Notification {
        val content = downloadNotificationContent(displayName, phase, strings)
        return notificationBuilder(strings)
            .setContentTitle(content.title)
            .setSubText(content.status)
            .setContentText(content.detail)
            .setProgress(DOWNLOAD_NOTIFICATION_MAX_PROGRESS, content.percent, content.indeterminate)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .apply {
                if (foreground) {
                    setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }
            .setShowWhen(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun progressNotificationId(displayName: String) = displayName.hashCode() and Int.MAX_VALUE

    private fun resultNotificationId(displayName: String) = "$displayName$RESULT_TAG".hashCode() and Int.MAX_VALUE

    private fun notificationBuilder(strings: Strings): NotificationCompat.Builder {
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, strings.btnDownload, NotificationManager.IMPORTANCE_LOW),
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.volcarona_primary_fg))
            .setContentIntent(openLauncher)
    }

    private companion object {
        const val CHANNEL_ID = "game-downloads"
        const val RESULT_TAG = ":result"
    }
}
