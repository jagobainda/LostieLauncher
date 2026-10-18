package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.util.format.DownloadSpeedFormatter
import dev.jagoba.lostielauncher.util.format.RemainingTimeFormatter
import kotlin.math.roundToInt

internal sealed interface DownloadNotificationPhase {
    data class Downloading(val progress: DownloadProgress) : DownloadNotificationPhase

    data object Verifying : DownloadNotificationPhase

    data class Extracting(val percent: Int?) : DownloadNotificationPhase
}

internal data class DownloadNotificationContent(
    val title: String,
    val status: String,
    val detail: String?,
    val percent: Int,
    val indeterminate: Boolean,
)

internal fun downloadNotificationContent(
    displayName: String,
    phase: DownloadNotificationPhase,
    strings: Strings,
): DownloadNotificationContent = when (phase) {
    is DownloadNotificationPhase.Downloading -> {
        val progress = phase.progress
        val total = progress.totalBytes
        val percent = progress.percent.roundToInt().coerceIn(0, MAX_PERCENT)
        val detail = listOfNotNull(
            total?.let { "$percent%" },
            progress.bytesPerSecond.takeIf { it > 0.0 }?.let(DownloadSpeedFormatter::format),
            total?.let { RemainingTimeFormatter.format(it - progress.downloadedBytes, progress.bytesPerSecond) },
        )
        DownloadNotificationContent(
            title = displayName,
            status = strings.statusDownloading,
            detail = detail.takeIf { it.isNotEmpty() }?.joinToString(DETAIL_SEPARATOR),
            percent = percent,
            indeterminate = total == null,
        )
    }

    DownloadNotificationPhase.Verifying -> DownloadNotificationContent(
        title = displayName,
        status = strings.statusVerifying,
        detail = null,
        percent = 0,
        indeterminate = true,
    )

    is DownloadNotificationPhase.Extracting -> DownloadNotificationContent(
        title = displayName,
        status = strings.statusExtracting,
        detail = phase.percent?.let { "$it%" },
        percent = phase.percent?.coerceIn(0, MAX_PERCENT) ?: 0,
        indeterminate = phase.percent == null,
    )
}

internal fun GameInstallationState.notificationPhase(): DownloadNotificationPhase? = when (this) {
    GameInstallationState.VerifyingIntegrity -> DownloadNotificationPhase.Verifying
    is GameInstallationState.Extracting -> DownloadNotificationPhase.Extracting(percent)
    GameInstallationState.NotStarted, is GameInstallationState.Finished -> null
}

internal const val DOWNLOAD_NOTIFICATION_MAX_PROGRESS = 100
private const val MAX_PERCENT = DOWNLOAD_NOTIFICATION_MAX_PROGRESS
private const val DETAIL_SEPARATOR = " · "
