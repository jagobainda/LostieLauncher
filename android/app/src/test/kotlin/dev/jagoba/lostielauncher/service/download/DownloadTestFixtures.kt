package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadTransferOptions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun testTransferOptions(
    maximumAttempts: Int = 3,
    inactivityTimeout: Duration = 1.seconds,
): DownloadTransferOptions = DownloadTransferOptions(
    cacheDirectoryName = "downloads",
    cacheMaxAge = 14.days,
    bufferSizeBytes = 4,
    maximumAttempts = maximumAttempts,
    retryBaseDelay = 1.milliseconds,
    inactivityTimeout = inactivityTimeout,
    progressSampleInterval = 1.milliseconds,
    finalizationMaximumAttempts = 3,
    finalizationBaseDelay = 1.milliseconds,
)
