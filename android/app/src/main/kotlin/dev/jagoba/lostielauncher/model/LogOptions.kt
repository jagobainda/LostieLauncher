package dev.jagoba.lostielauncher.model

import java.time.ZoneId

internal data class LogOptions(val maxFileSizeBytes: Long, val retentionMonths: Int, val zoneId: ZoneId)
