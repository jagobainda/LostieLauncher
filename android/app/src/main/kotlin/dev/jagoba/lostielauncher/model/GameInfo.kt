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
        get() = gameSlug(name)
}

private val SLUG_SEPARATORS = Regex("[^a-z0-9]+")

fun gameSlug(name: String): String = SLUG_SEPARATORS.replace(name.lowercase(Locale.ROOT), "-").trim('-')
