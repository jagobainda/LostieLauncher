package dev.jagoba.lostielauncher.util.format

/**
 * Formats a playtime total for a game card, as the desktop's
 * `Utils/PlaytimeFormatter.cs` does.
 *
 * The unit labels are hardcoded English abbreviations and are deliberately
 * **not** localized — the desktop does not localize them either, and the eight
 * text catalogues carry no key for them. Changing that is a product decision,
 * not a port decision.
 *
 * Zero or less yields an empty string rather than `"0 min"`, which is what lets
 * a card hide the whole playtime row instead of showing an empty one.
 */
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
