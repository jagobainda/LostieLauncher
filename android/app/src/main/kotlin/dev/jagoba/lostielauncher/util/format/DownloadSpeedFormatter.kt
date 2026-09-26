package dev.jagoba.lostielauncher.util.format

import java.util.Locale

object DownloadSpeedFormatter {
    private const val BYTES_PER_KIB = 1024.0
    private const val BYTES_PER_MIB = 1_048_576.0

    fun format(bytesPerSecond: Double): String = when {
        bytesPerSecond >= BYTES_PER_MIB -> String.format(Locale.ROOT, "%.1f MB/s", bytesPerSecond / BYTES_PER_MIB)
        bytesPerSecond > 0 -> String.format(Locale.ROOT, "%.1f KB/s", bytesPerSecond / BYTES_PER_KIB)
        else -> "0 KB/s"
    }
}
