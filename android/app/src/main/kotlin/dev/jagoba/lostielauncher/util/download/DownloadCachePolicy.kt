package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadCacheEntry
import java.time.Duration as JavaDuration
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

object DownloadCachePolicy {
    private const val PART_META_EXTENSION = DownloadPathUtils.PART_EXTENSION + DownloadPathUtils.META_EXTENSION

    private val MANAGED_EXTENSIONS = listOf(
        DownloadPathUtils.ZIP_EXTENSION,
        DownloadPathUtils.PART_EXTENSION,
        PART_META_EXTENSION,
    )

    fun selectStaleFiles(
        entries: Iterable<DownloadCacheEntry>,
        knownGameIds: Set<String>,
        now: Instant,
        maxAge: Duration,
    ): List<String> {
        val managed = entries.filter { isManaged(it.fileName) }
        val partFiles = managed
            .map { it.fileName.lowercase(Locale.ROOT) }
            .filterTo(mutableSetOf()) { it.endsWith(DownloadPathUtils.PART_EXTENSION) }
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

        if (fileName.endsWith(PART_META_EXTENSION) &&
            fileName.removeSuffix(DownloadPathUtils.META_EXTENSION) !in partFiles
        ) {
            return true
        }

        if (JavaDuration.between(entry.lastWriteTime, now).toKotlinDuration() > maxAge) return true

        return gameIdOf(fileName) !in knownGameIds
    }

    private fun isManaged(fileName: String): Boolean =
        MANAGED_EXTENSIONS.any { fileName.endsWith(it, ignoreCase = true) }

    private fun gameIdOf(fileName: String): String {
        val separator = fileName.indexOf('.')
        return if (separator <= 0) "" else fileName.substring(0, separator)
    }
}
