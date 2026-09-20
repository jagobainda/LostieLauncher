package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadCacheEntry
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * Decides which files in the download cache are no longer worth keeping, ported
 * from the desktop's `Utils/DownloadCachePolicy.cs`.
 *
 * It **selects only**. Listing the directory and deleting what comes back are
 * the caller's, which is what keeps the whole decision testable without a
 * filesystem and is the split this port is asked to preserve.
 *
 * The rule that matters most is the one it does *not* apply: a recent partial
 * file belonging to a game still in the catalogue is kept, because that is the
 * download the user paused and expects to resume.
 */
object DownloadCachePolicy {
    /** Fourteen days, the desktop's `DefaultMaxAge`. */
    val DEFAULT_MAX_AGE: Duration = Duration.ofDays(14)

    private const val ZIP_EXTENSION = ".zip"
    private const val PART_EXTENSION = ".part"
    private const val META_EXTENSION = ".meta"
    private const val PART_META_EXTENSION = PART_EXTENSION + META_EXTENSION

    private val MANAGED_EXTENSIONS = listOf(ZIP_EXTENSION, PART_EXTENSION, PART_META_EXTENSION)

    /**
     * The names of the entries that should go.
     *
     * A managed file — one ending in `.zip`, `.part` or `.part.meta` — is stale
     * when any of three things holds: it is resume metadata whose partial file
     * is gone, it has not been written to within [maxAge], or its game id is
     * not in [knownGameIds]. Anything else in the folder is left alone entirely
     * rather than deleted: the directory is the launcher's, but a file it did
     * not write is not its business.
     *
     * Names and ids are compared case-insensitively, as the desktop's ordinal
     * ignore-case comparisons do.
     */
    fun selectStaleFiles(
        entries: Iterable<DownloadCacheEntry>,
        knownGameIds: Set<String>,
        now: Instant,
        maxAge: Duration,
    ): List<String> {
        val managed = entries.filter { isManaged(it.fileName) }
        val partFiles = managed
            .map { it.fileName.lowercase(Locale.ROOT) }
            .filterTo(mutableSetOf()) { it.endsWith(PART_EXTENSION) }
        val known = knownGameIds.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }

        return managed.filter { isStale(it, partFiles, known, now, maxAge) }.map { it.fileName }
    }

    private fun isStale(
        entry: DownloadCacheEntry,
        partFiles: Set<String>,
        knownGameIds: Set<String>,
        now: Instant,
        maxAge: Duration,
    ): Boolean {
        val fileName = entry.fileName.lowercase(Locale.ROOT)

        // Metadata describing a partial file that no longer exists is dead weight.
        if (fileName.endsWith(PART_META_EXTENSION) && fileName.removeSuffix(META_EXTENSION) !in partFiles) {
            return true
        }

        if (Duration.between(entry.lastWriteTime, now) > maxAge) return true

        return gameIdOf(fileName) !in knownGameIds
    }

    private fun isManaged(fileName: String): Boolean =
        MANAGED_EXTENSIONS.any { fileName.endsWith(it, ignoreCase = true) }

    /**
     * Everything before the **first** dot.
     *
     * A name with no dot, or one starting with a dot, yields the empty string,
     * which is never a known game id and is therefore always stale.
     */
    private fun gameIdOf(fileName: String): String {
        val separator = fileName.indexOf('.')
        return if (separator <= 0) "" else fileName.substring(0, separator)
    }
}
