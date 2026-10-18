package dev.jagoba.lostielauncher.model

import java.io.File

data class KirinOptions(
    val gamesRoot: File,
    val libraryFile: File,
    val executableName: String,
    val runtimeMode: String,
)
