package dev.jagoba.lostielauncher.model

import java.util.UUID

val MissingGameId: UUID = UUID(0, 0)

data class LocalGame(val id: UUID, val name: String, val version: String, val type: String?)
