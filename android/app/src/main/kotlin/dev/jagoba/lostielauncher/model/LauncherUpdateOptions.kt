package dev.jagoba.lostielauncher.model

import java.io.File

data class LauncherUpdateOptions(
    val packageName: String,
    val feedUrl: String,
    val installerFile: File,
    val maxInstallerBytes: Long = DEFAULT_MAX_INSTALLER_BYTES,
) {
    companion object {
        const val DEFAULT_MAX_INSTALLER_BYTES: Long = 64L * 1024 * 1024
    }
}
