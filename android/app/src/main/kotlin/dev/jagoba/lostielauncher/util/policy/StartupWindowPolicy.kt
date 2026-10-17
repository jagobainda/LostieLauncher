package dev.jagoba.lostielauncher.util.policy

import kotlin.time.Duration

internal object StartupWindowPolicy {
    fun shouldKeep(settingsLoaded: Boolean, elapsedMilliseconds: Long, timeout: Duration): Boolean =
        !settingsLoaded && elapsedMilliseconds < timeout.inWholeMilliseconds
}
