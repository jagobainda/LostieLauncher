package dev.jagoba.lostielauncher.model

import java.util.Locale
import java.util.UUID

data class GameInfo(
    val id: UUID?,
    val name: String,
    val version: String,
    val sizeGb: Double,
    val description: String,
    val pageUrl: String,
    val logoUrl: String?,
    val relativePath: String,
    val sha256: String,
) {
    val gameId: String
        get() = SLUG_SEPARATORS.replace(name.lowercase(Locale.ROOT), "-").trim('-')

    val formattedSize: String
        get() = if (sizeGb >= 1) {
            trimTrailingZero(String.format(Locale.ROOT, "%.1f", sizeGb)) + " GB"
        } else {
            String.format(Locale.ROOT, "%.0f", sizeGb * BYTES_PER_KIB) + " MB"
        }

    private companion object {
        val SLUG_SEPARATORS = Regex("[^a-z0-9]+")

        const val BYTES_PER_KIB = 1024

        fun trimTrailingZero(value: String) = value.removeSuffix(".0")
    }
}
