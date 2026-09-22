package dev.jagoba.lostielauncher.model

import kotlin.time.Duration

data class DownloadTransferOptions(
    val cacheDirectoryName: String,
    val cacheMaxAge: java.time.Duration,
    val bufferSizeBytes: Int,
    val maximumAttempts: Int,
    val retryBaseDelay: Duration,
    val inactivityTimeout: Duration,
    val progressSampleInterval: Duration,
    val finalizationMaximumAttempts: Int,
    val finalizationBaseDelay: Duration,
)
