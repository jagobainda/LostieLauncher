package dev.jagoba.lostielauncher.model

import java.io.File

sealed interface KirinStatus {
    data object Ready : KirinStatus

    data class Missing(val minimumVersion: String) : KirinStatus

    data class Outdated(val installedVersion: String, val minimumVersion: String) : KirinStatus

    data class UpdateRecommended(val installedVersion: String, val latestVersion: String) : KirinStatus
}

data class KirinSetupOptions(val requirementsUrl: String, val fallbackMinimumVersion: String, val installerFile: File)
