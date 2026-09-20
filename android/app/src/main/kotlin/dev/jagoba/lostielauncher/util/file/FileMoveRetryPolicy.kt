package dev.jagoba.lostielauncher.util.file

import java.io.IOException

/**
 * Decides whether a failed file move is worth trying again, ported from
 * `FileFinalizer.IsRetryable` in the desktop's `Utils/FileFinalizer.cs`.
 *
 * Only the predicate is here. The retry loop around it touches the filesystem
 * and belongs to the download transfer, which arrives with port plan step 08 —
 * the desktop keeps the same split, and it is what makes the decision testable
 * on its own.
 *
 * The case it exists for is the realistic one: a sync client or a scanner holds
 * the destination open for a moment just as a multi-gigabyte transfer finishes,
 * and discarding the download over that would be absurd. The case it refuses is
 * equally deliberate — a destination that is a directory, or a read-only file,
 * blocks every future attempt exactly as it blocks this one, so making the user
 * sit through a backoff that cannot help is worse than failing now.
 */
object FileMoveRetryPolicy {
    /** What the caller found at the destination, probed once before deciding. */
    data class DestinationState(val isDirectory: Boolean, val isReadOnly: Boolean)

    /**
     * Whether [error] might clear on its own.
     *
     * The desktop retries `UnauthorizedAccessException` and `IOException`. On
     * the JVM the first has no separate type — a denied file operation surfaces
     * as `AccessDeniedException`, an `IOException` — so the pair maps to
     * `IOException` and [SecurityException], which is what a policy denial
     * throws instead. Everything else means the move was wrong rather than
     * early, and waiting will not fix it.
     */
    fun isRetryable(error: Throwable, destination: DestinationState): Boolean {
        if (destination.isDirectory || destination.isReadOnly) return false

        return error is IOException || error is SecurityException
    }
}
