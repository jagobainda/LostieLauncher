package dev.jagoba.lostielauncher.model

import java.util.UUID

data class SpecialVersionConfig(
    val sha256: String,
    val type: String,
    val mainGameId: UUID,
    val version: String,
    val fileName: String,
)
