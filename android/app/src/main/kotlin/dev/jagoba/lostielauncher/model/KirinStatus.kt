package dev.jagoba.lostielauncher.model

import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

sealed interface KirinStatus {
    data object Ready : KirinStatus

    data object UntrustedSigner : KirinStatus

    data class Missing(val minimumVersion: String) : KirinStatus

    data class Outdated(val installedVersion: String, val minimumVersion: String) : KirinStatus

    data class UpdateRecommended(val installedVersion: String, val latestVersion: String) : KirinStatus
}

data class KirinSetupOptions(
    val requirementsUrl: String,
    val fallbackMinimumVersion: String,
    val installerFile: File,
    val signerSha256: String,
    val supportedAbis: List<String> = emptyList(),
    val feedRetryInterval: Duration = 5.minutes,
)
