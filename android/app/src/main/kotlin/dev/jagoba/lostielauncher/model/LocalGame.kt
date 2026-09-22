package dev.jagoba.lostielauncher.model

import java.util.UUID

data class LocalGame(val id: UUID, val name: String, val version: String, val type: String?)
