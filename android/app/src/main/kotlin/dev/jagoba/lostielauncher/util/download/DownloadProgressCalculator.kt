package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadProgress

internal object DownloadProgressCalculator {
    fun calculate(downloadedBytes: Long, totalBytes: Long?, bytesPerSecond: Double): DownloadProgress {
        val percent = if (totalBytes != null && totalBytes > 0) {
            downloadedBytes.toDouble().div(totalBytes).times(100).coerceIn(0.0, 100.0)
        } else {
            0.0
        }
        return DownloadProgress(
            percent = percent,
            bytesPerSecond = bytesPerSecond.coerceAtLeast(0.0),
            downloadedBytes = downloadedBytes.coerceAtLeast(0),
            totalBytes = totalBytes?.takeIf { it > 0 },
        )
    }
}

internal class DownloadSpeedSampler(private val intervalNanos: Long, initialBytes: Long, initialNanos: Long) {
    private var sampledBytes = initialBytes
    private var sampledNanos = initialNanos
    fun sample(totalBytes: Long, nowNanos: Long): Double? {
        val elapsed = nowNanos - sampledNanos
        if (elapsed < intervalNanos) return null
        val currentSpeed = (totalBytes - sampledBytes).toDouble() / elapsed * NANOS_PER_SECOND
        sampledBytes = totalBytes
        sampledNanos = nowNanos
        return currentSpeed
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
