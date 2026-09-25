package dev.jagoba.lostielauncher.util.format

object RemainingTimeFormatter {
    fun format(remainingBytes: Long, bytesPerSecond: Double): String? {
        if (remainingBytes <= 0 || bytesPerSecond <= 0.0 || !bytesPerSecond.isFinite()) return null
        val seconds = (remainingBytes / bytesPerSecond).toLong()
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainder = seconds % 60
        return when {
            hours >= 1 -> "${hours}h ${minutes}m"
            minutes >= 1 -> "${minutes}m ${remainder}s"
            else -> "${remainder}s"
        }
    }
}
