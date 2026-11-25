package dev.jagoba.lostielauncher.model

import java.io.File
import java.util.UUID

data class InstalledGameFileDigest(val size: Long, val sha256: String)

data class InstalledGameManifest(
    val gameId: String,
    val id: UUID,
    val name: String,
    val version: String,
    val variant: String?,
    val files: Map<String, InstalledGameFileDigest>,
) {
    val game: LocalGame get() = LocalGame(id, name, version, variant)
}

sealed interface InstalledGameManifestLookup {
    data class Found(val manifest: InstalledGameManifest) : InstalledGameManifestLookup

    data object NotFound : InstalledGameManifestLookup

    data object Corrupted : InstalledGameManifestLookup
}

internal data class InstalledGameManifestOptions(val directory: File)
