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
)
