package dev.jagoba.lostielauncher.util.file

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.nio.file.AccessDeniedException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Ported from the `IsRetryable` half of the desktop's `Utils/FileFinalizerTests.cs`.
 *
 * The four `MoveAsync` declarations there are not here: they drive a real file through a
 * real move with a real lock on it, which is the filesystem half of the same type and
 * arrives with the download transfer in port plan step 08.
 */
@DisplayName("FileMoveRetryPolicy.isRetryable")
class FileMoveRetryPolicyTest {
    @ParameterizedTest
    @CsvSource("false, false, true", "true, false, false", "false, true, false")
    fun `an access denial is only retried while the destination can still change`(
        isDirectory: Boolean,
        isReadOnly: Boolean,
        expected: Boolean,
    ) {
        // Arrange — a directory, or a read-only file, blocks every future attempt exactly as
        // it blocks this one, so the user should not wait through a backoff that cannot help.
        val error = AccessDeniedException("denied")

        // Act
        val retryable = FileMoveRetryPolicy.isRetryable(
            error,
            FileMoveRetryPolicy.DestinationState(isDirectory, isReadOnly),
        )

        // Assert
        retryable shouldBe expected
    }

    @Test
    fun `an error that is neither an access nor an IO failure is not retried`() {
        // Arrange — retrying only makes sense for contention on the file itself.
        val error = IllegalStateException("bad state")

        // Act & Assert
        FileMoveRetryPolicy.isRetryable(error, freeDestination()).shouldBeFalse()
    }

    // ---- Android-only: the exception mapping this port had to choose ----

    @Test
    fun `a plain IO failure is retried`() {
        // Arrange — the transient case the whole retry exists for: something holds the
        // destination open for a moment just as a multi-gigabyte transfer finishes.
        // Act & Assert
        FileMoveRetryPolicy.isRetryable(IOException("locked"), freeDestination()).shouldBeTrue()
    }

    @Test
    fun `a security denial is retried, standing in for the desktop's access exception`() {
        // Arrange — the JVM has no separate type for a denied file operation, so the
        // desktop's UnauthorizedAccessException maps onto IOException plus this one.
        // Act & Assert
        FileMoveRetryPolicy.isRetryable(SecurityException("denied"), freeDestination()).shouldBeTrue()
    }

    private fun freeDestination() = FileMoveRetryPolicy.DestinationState(isDirectory = false, isReadOnly = false)
}
