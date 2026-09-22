package dev.jagoba.lostielauncher.service.storage

import java.io.File

interface StorageLocations {
    val gamesRoot: File
    val logsDirectory: File
}

internal data class FixedStorageLocations(override val gamesRoot: File, override val logsDirectory: File) :
    StorageLocations
