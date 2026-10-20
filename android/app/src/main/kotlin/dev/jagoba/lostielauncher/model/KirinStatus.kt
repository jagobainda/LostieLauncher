package dev.jagoba.lostielauncher.model

import java.io.File

sealed interface KirinStatus {
    data object Ready : KirinStatus

    data class Missing(val minimumVersion: String) : KirinStatus

    data class Outdated(val installedVersion: String, val minimumVersion: String) : KirinStatus

    data class UpdateRecommended(val installedVersion: String, val latestVersion: String) : KirinStatus
}

sealed interface KirinDownloadState {
    data object Checking : KirinDownloadState

    data object Unavailable : KirinDownloadState

    data object Available : KirinDownloadState

    data class Downloading(val percent: Int) : KirinDownloadState

    data object Failed : KirinDownloadState
}

data class KirinSetupOptions(
    val requirementsUrl: String,
    val fallbackMinimumVersion: String,
    val installerFile: File,
    val fileProviderAuthority: String,
)
