package dev.jagoba.lostielauncher.util.log

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FilteredLogger")
class FilteredLoggerTest {
    private val delegate = mockk<Logger>(relaxed = true)
    private val failure = IllegalStateException("boom")

    @Test
    fun `forwards every level when minimum level is debug`() {
        val sut = createSut(LogLevel.DEBUG)

        sut.debug("d")
        sut.info("i")
        sut.error("e", failure)

        verify(exactly = 1) { delegate.debug("d") }
        verify(exactly = 1) { delegate.info("i") }
        verify(exactly = 1) { delegate.error("e", failure) }
    }

    @Test
    fun `drops debug and keeps info and error when minimum level is info`() {
        val sut = createSut(LogLevel.INFO)

        sut.debug("d")
        sut.info("i")
        sut.error("e", failure)

        verify(exactly = 0) { delegate.debug(any()) }
        verify(exactly = 1) { delegate.info("i") }
        verify(exactly = 1) { delegate.error("e", failure) }
    }

    @Test
    fun `drops debug and info and keeps only error when minimum level is error`() {
        val sut = createSut(LogLevel.ERROR)

        sut.debug("d")
        sut.info("i")
        sut.error("e", failure)

        verify(exactly = 0) { delegate.debug(any()) }
        verify(exactly = 0) { delegate.info(any()) }
        verify(exactly = 1) { delegate.error("e", failure) }
    }

    private fun createSut(minLevel: LogLevel) = FilteredLogger(delegate, minLevel)
}
