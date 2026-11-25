package dev.jagoba.lostielauncher.model

import java.io.File
import kotlin.time.Duration

data class KirinOptions(
    val gamesRoot: File,
    val libraryFile: File,
    val executableName: String,
    val runtimeMode: String,
    val packageName: String,
    val launchActivity: String,
    val launchAction: String,
    val gamePathExtra: String,
    val archiveBufferSizeBytes: Int = DEFAULT_BUFFER_SIZE,
    val archiveExtractionWorkers: Int = 1,
    val archiveEntryCostBytes: Long = 0,
    val archiveProgressInterval: Duration = Duration.ZERO,
    val maxLibraryFileBytes: Long = DEFAULT_MAX_LIBRARY_FILE_BYTES,
    val archiveMaxEntries: Int = DEFAULT_ARCHIVE_MAX_ENTRIES,
    val archiveMaxUncompressedBytes: Long = DEFAULT_ARCHIVE_MAX_UNCOMPRESSED_BYTES,
    val archiveMaxCompressionRatio: Int = DEFAULT_ARCHIVE_MAX_COMPRESSION_RATIO,
    val archiveCompressionRatioThresholdBytes: Long = DEFAULT_ARCHIVE_COMPRESSION_RATIO_THRESHOLD_BYTES,
) {
    companion object {
        const val DEFAULT_MAX_LIBRARY_FILE_BYTES: Long = 2L * 1024 * 1024
        const val DEFAULT_ARCHIVE_MAX_ENTRIES: Int = 200_000
        const val DEFAULT_ARCHIVE_MAX_UNCOMPRESSED_BYTES: Long = 16L * 1024 * 1024 * 1024
        const val DEFAULT_ARCHIVE_MAX_COMPRESSION_RATIO: Int = 100
        const val DEFAULT_ARCHIVE_COMPRESSION_RATIO_THRESHOLD_BYTES: Long = 100L * 1024 * 1024
    }
}
