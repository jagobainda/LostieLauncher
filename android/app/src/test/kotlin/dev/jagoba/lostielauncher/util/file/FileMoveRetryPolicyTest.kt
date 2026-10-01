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

@DisplayName("FileMoveRetryPolicy.isRetryable")
class FileMoveRetryPolicyTest {
    @ParameterizedTest
    @CsvSource("false, false, true", "true, false, false", "false, true, false")
    fun `an access denial is only retried while the destination can still change`(
        isDirectory: Boolean,
        isReadOnly: Boolean,
        expected: Boolean,
    ) {
        val error = AccessDeniedException("denied")

        val retryable = FileMoveRetryPolicy.isRetryable(
            error,
            FileMoveRetryPolicy.DestinationState(isDirectory, isReadOnly),
        )

        retryable shouldBe expected
    }

    @Test
    fun `an error that is neither an access nor an IO failure is not retried`() {
        val error = IllegalStateException("bad state")

        FileMoveRetryPolicy.isRetryable(error, freeDestination()).shouldBeFalse()
    }

    @Test
    fun `a plain IO failure is retried`() {
        FileMoveRetryPolicy.isRetryable(IOException("locked"), freeDestination()).shouldBeTrue()
    }

    @Test
    fun `a security denial is retried, standing in for the desktop's access exception`() {
        FileMoveRetryPolicy.isRetryable(SecurityException("denied"), freeDestination()).shouldBeTrue()
    }

    private fun freeDestination() = FileMoveRetryPolicy.DestinationState(isDirectory = false, isReadOnly = false)
}
