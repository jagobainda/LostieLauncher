package dev.jagoba.lostielauncher.model

import java.time.Instant

data class DownloadCacheEntry(val fileName: String, val lastWriteTime: Instant)
