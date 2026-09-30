package dev.jagoba.lostielauncher.util.format

object PlaytimeFormatter {
    private const val MINUTES_PER_HOUR = 60

    fun format(minutes: Int): String {
        if (minutes <= 0) return ""
        if (minutes < MINUTES_PER_HOUR) return "$minutes min"

        val hours = minutes / MINUTES_PER_HOUR
        val remainder = minutes % MINUTES_PER_HOUR
        return if (remainder > 0) "$hours h $remainder min" else "$hours h"
    }
}
