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
    val maxInstallerBytes: Long = DEFAULT_MAX_INSTALLER_BYTES,
    val allowedFeedHosts: AllowedHosts = DEFAULT_ALLOWED_FEED_HOSTS,
    val allowedDownloadHosts: AllowedHosts = DEFAULT_ALLOWED_DOWNLOAD_HOSTS,
) {
    companion object {
        const val DEFAULT_MAX_INSTALLER_BYTES: Long = 512L * 1024 * 1024
        val DEFAULT_ALLOWED_FEED_HOSTS = AllowedHosts(
            exact = setOf("api.github.com", "ericlostie-launcher.jagoba.dev", "cdn.jagoba.dev"),
        )
        val DEFAULT_ALLOWED_DOWNLOAD_HOSTS = AllowedHosts(
            exact = setOf("github.com", "ericlostie-launcher.jagoba.dev", "cdn.jagoba.dev"),
            suffixes = setOf("githubusercontent.com"),
        )
    }
}
