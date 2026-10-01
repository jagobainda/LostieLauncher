package dev.jagoba.lostielauncher.model

data class GameDownloadArgs(val gameId: String, val version: String, val relativePath: String, val key: String? = null)
