package dev.jagoba.lostielauncher.model

import java.io.File

sealed interface ApkDownloadState {
    data object Checking : ApkDownloadState

    data object Unavailable : ApkDownloadState

    data object Available : ApkDownloadState

    data class Downloading(val percent: Int) : ApkDownloadState

    data object Failed : ApkDownloadState
}

data class ApkTarget(val packageName: String, val installerFile: File, val label: String)

data class ApkInstallOptions(val fileProviderAuthority: String)
