package dev.jagoba.lostielauncher.util.version

object VersionUtils {
    private const val UNKNOWN_VERSION = "unknown"

    fun isNewerVersion(remoteVersion: String?, localVersion: String?): Boolean {
        val remote = parseBaseVersion(remoteVersion) ?: return false
        val local = parseBaseVersion(localVersion) ?: return false
        return remote > local
    }

    fun formatDisplayVersion(version: String?): String {
        if (version.isNullOrBlank()) return UNKNOWN_VERSION

        val normalized = version.trim().trimStart('v', 'V')
        return if (normalized.isEmpty()) UNKNOWN_VERSION else "v$normalized"
    }

    internal fun parseBaseVersion(version: String?): BaseVersion? {
        if (version.isNullOrBlank()) return null

        val stripped = version.trimStart('v', 'V')
        val dashIndex = stripped.indexOf('-')
        val base = if (dashIndex >= 0) stripped.substring(0, dashIndex) else stripped
        return BaseVersion.parse(base)
    }
}
