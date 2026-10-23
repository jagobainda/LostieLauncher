package dev.jagoba.lostielauncher.util.log

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UncaughtExceptionLogger")
class UncaughtExceptionLoggerTest {
    private val logger = mockk<Logger>(relaxed = true)
    private val next = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
    private val thread = Thread("worker")
    private val failure = IllegalStateException("boom")

    @Test
    fun `logs the failure before handing it to the platform handler`() {
        UncaughtExceptionLogger(logger, next).uncaughtException(thread, failure)

        verifyOrder {
            logger.error("Unhandled exception on thread worker.", failure)
            next.uncaughtException(thread, failure)
        }
    }

    @Test
    fun `still reaches the platform handler when logging fails`() {
        every { logger.error(any(), any()) } throws IllegalStateException("disk full")

        UncaughtExceptionLogger(logger, next).uncaughtException(thread, failure)

        verify(exactly = 1) { next.uncaughtException(thread, failure) }
    }

    @Test
    fun `logs without a platform handler to forward to`() {
        UncaughtExceptionLogger(logger, null).uncaughtException(thread, failure)

        verify(exactly = 1) { logger.error(any(), failure) }
    }
}
