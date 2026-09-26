package dev.jagoba.lostielauncher.util.format

import java.util.Locale

object FreeSpaceFormatter {
    const val UNKNOWN = "—"
    private const val BYTES_PER_GIB = 1024.0 * 1024.0 * 1024.0
    private const val MIB_PER_GIB = 1024

    fun format(bytes: Long?): String {
        if (bytes == null) return UNKNOWN
        val gigabytes = bytes / BYTES_PER_GIB
        return if (gigabytes >= 1) {
            String.format(Locale.ROOT, "%.1f", gigabytes).removeSuffix(".0") + " GB"
        } else {
            String.format(Locale.ROOT, "%.0f", gigabytes * MIB_PER_GIB) + " MB"
        }
    }
}
