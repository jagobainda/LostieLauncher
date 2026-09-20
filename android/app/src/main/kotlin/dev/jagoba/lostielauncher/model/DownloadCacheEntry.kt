package dev.jagoba.lostielauncher.model

import java.time.Instant

/**
 * One file sitting in the download cache, as the cleanup decision sees it.
 *
 * The desktop's `Models/DownloadCacheEntry.cs`. It carries a bare file **name**
 * rather than a path on purpose: listing the directory is the caller's job, and
 * [dev.jagoba.lostielauncher.util.download.DownloadCachePolicy] only decides.
 * That split is what lets the whole policy be tested without a filesystem.
 */
data class DownloadCacheEntry(
    /** The file name, extension included, without a directory. */
    val fileName: String,
    /**
     * When the file was last written.
     *
     * The desktop field is `LastWriteTimeUtc`, a `DateTime` whose kind has to
     * be honoured by hand; an [Instant] is a point on the timeline and cannot
     * be anything but UTC, so the suffix is dropped.
     */
    val lastWriteTime: Instant,
)
