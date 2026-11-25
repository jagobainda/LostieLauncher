package dev.jagoba.lostielauncher.model

import java.io.File

internal data class LocalLibraryOptions(
    val registryFile: File,
    val maxRegistryBytes: Long = DEFAULT_MAX_REGISTRY_BYTES,
) {
    companion object {
        const val DEFAULT_MAX_REGISTRY_BYTES: Long = 2L * 1024 * 1024
    }
}
