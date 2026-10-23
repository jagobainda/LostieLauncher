package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.util.version.VersionUtils

object ApkVersionPolicy {
    fun isSupported(version: String?, minimumVersion: String): Boolean {
        val actual = VersionUtils.parseBaseVersion(version) ?: return false
        val minimum = VersionUtils.parseBaseVersion(minimumVersion) ?: return false
        return actual >= minimum
    }

    fun isOffered(releaseVersion: String, installedVersion: String?, minimumVersion: String?): Boolean =
        (minimumVersion == null || isSupported(releaseVersion, minimumVersion)) &&
            (installedVersion == null || VersionUtils.isNewerVersion(releaseVersion, installedVersion))
}
