package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.util.version.VersionUtils

object KirinVersionPolicy {
    fun isSupported(version: String?, minimumVersion: String): Boolean {
        val actual = VersionUtils.parseBaseVersion(version) ?: return false
        val minimum = VersionUtils.parseBaseVersion(minimumVersion) ?: return false
        return actual >= minimum
    }

    fun status(installedVersion: String?, minimumVersion: String, latestVersion: String?): KirinStatus = when {
        installedVersion == null -> KirinStatus.Missing(minimumVersion)

        !isSupported(installedVersion, minimumVersion) -> KirinStatus.Outdated(installedVersion, minimumVersion)

        latestVersion != null && VersionUtils.isNewerVersion(latestVersion, installedVersion) ->
            KirinStatus.UpdateRecommended(installedVersion, latestVersion)

        else -> KirinStatus.Ready
    }

    fun isOffered(releaseVersion: String, installedVersion: String?, minimumVersion: String): Boolean =
        isSupported(releaseVersion, minimumVersion) &&
            (installedVersion == null || VersionUtils.isNewerVersion(releaseVersion, installedVersion))
}
