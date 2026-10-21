package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.util.version.VersionUtils

object KirinVersionPolicy {
    fun status(installedVersion: String?, minimumVersion: String, latestVersion: String?): KirinStatus = when {
        installedVersion == null -> KirinStatus.Missing(minimumVersion)

        !ApkVersionPolicy.isSupported(installedVersion, minimumVersion) ->
            KirinStatus.Outdated(installedVersion, minimumVersion)

        latestVersion != null && VersionUtils.isNewerVersion(latestVersion, installedVersion) ->
            KirinStatus.UpdateRecommended(installedVersion, latestVersion)

        else -> KirinStatus.Ready
    }
}
