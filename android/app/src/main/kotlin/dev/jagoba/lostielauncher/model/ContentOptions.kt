package dev.jagoba.lostielauncher.model

import kotlin.time.Duration

data class ContentOptions(
    val cdnBaseUrl: String,
    val catalogueUrl: String,
    val homeContentUrl: String,
    val maintenanceFlagUrl: String,
    val maintenanceFlagCacheDuration: Duration,
)
