package dev.jagoba.lostielauncher.service.storage

import java.io.File

interface StorageLocations {
    val downloadsRoot: File
    val logsDirectory: File
}

internal data class FixedStorageLocations(override val downloadsRoot: File, override val logsDirectory: File) :
    StorageLocations
