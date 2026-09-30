package dev.jagoba.lostielauncher.util.file

import java.io.IOException

object FileMoveRetryPolicy {
    data class DestinationState(val isDirectory: Boolean, val isReadOnly: Boolean)

    fun isRetryable(error: Throwable, destination: DestinationState): Boolean {
        if (destination.isDirectory || destination.isReadOnly) return false

        return error is IOException || error is SecurityException
    }
}
