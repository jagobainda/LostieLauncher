package dev.jagoba.lostielauncher.util.version

/**
 * Version comparison and display, ported from the desktop's
 * `Utils/VersionUtils.cs`.
 *
 * Both entry points fail **closed**: anything that does not parse is treated as
 * "no update" and as "no version to show", never as a reason to prompt the user
 * for a download they did not ask for.
 *
 * The desktop writes an INFO line when a comparison is skipped because a
 * version is unparseable. This does not: `util/` takes no dependencies, and the
 * [Logger][dev.jagoba.lostielauncher.util.log.Logger] is injected rather than
 * static here, so the caller logs. Nothing observable changes — no desktop test
 * asserts that line either.
 */
object VersionUtils {
    private const val UNKNOWN_VERSION = "unknown"

    /**
     * Whether [remoteVersion] is strictly newer than [localVersion].
     *
     * Both are reduced to their base version first — a leading `v` and anything
     * from the first `-` onwards are dropped — so `1.2.0-beta` and `1.2.0`
     * compare equal. If either side fails to parse, the answer is `false`.
     *
     * Both parameters are nullable although the desktop's are not: its
     * `GameInfo.Version` is non-nullable but `System.Text.Json` will still bind
     * an explicit `"version": null` into it, and its own test covers that case.
     */
    fun isNewerVersion(remoteVersion: String?, localVersion: String?): Boolean {
        val remote = parseBaseVersion(remoteVersion) ?: return false
        val local = parseBaseVersion(localVersion) ?: return false
        return remote > local
    }

    /**
     * The version as a card shows it: exactly one `v` prefix, or `"unknown"`
     * when there is nothing left to show.
     *
     * Unlike [parseBaseVersion] this trims surrounding whitespace and keeps any
     * pre-release suffix — it formats, it does not compare.
     */
    fun formatDisplayVersion(version: String?): String {
        if (version.isNullOrBlank()) return UNKNOWN_VERSION

        val normalized = version.trim().trimStart('v', 'V')
        return if (normalized.isEmpty()) UNKNOWN_VERSION else "v$normalized"
    }

    /**
     * The numeric part of [version], or `null` when it is not comparable.
     *
     * Deliberately does **not** trim whitespace: the desktop does not either,
     * so `"  v2.11.0  "` is unparseable there and here, while
     * [formatDisplayVersion] still displays it.
     */
    internal fun parseBaseVersion(version: String?): BaseVersion? {
        if (version.isNullOrBlank()) return null

        val stripped = version.trimStart('v', 'V')
        val dashIndex = stripped.indexOf('-')
        val base = if (dashIndex >= 0) stripped.substring(0, dashIndex) else stripped
        return BaseVersion.parse(base)
    }
}
