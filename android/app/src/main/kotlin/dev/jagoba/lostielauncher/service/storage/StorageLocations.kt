package dev.jagoba.lostielauncher.service.storage

import java.io.File

interface StorageLocations {
    val downloadsRoot: File
    val logsDirectory: File
    val legacyDownloadsRoots: List<File>
        get() = emptyList()
}

internal data class FixedStorageLocations(
    override val downloadsRoot: File,
    override val logsDirectory: File,
    override val legacyDownloadsRoots: List<File> = emptyList(),
) : StorageLocations
