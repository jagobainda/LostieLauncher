package dev.jagoba.lostielauncher.model

import kotlin.time.Duration

internal data class SettingsOptions(val saveDebounce: Duration, val startupLoadTimeout: Duration)
