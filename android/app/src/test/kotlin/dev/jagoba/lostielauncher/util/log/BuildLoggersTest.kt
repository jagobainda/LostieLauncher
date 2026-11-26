package dev.jagoba.lostielauncher.util.log

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BuildLoggers")
class BuildLoggersTest {
    private val logcat = mockk<Logger>(relaxed = true)
    private val file = mockk<Logger>(relaxed = true)
    private val failure = IllegalStateException("boom")

    @Test
    fun `debug logger forwards debug, info and error to both logcat and file`() {
        val sut = BuildLoggers.debug(logcat, file)

        sut.debug("d")
        sut.info("i")
        sut.error("e", failure)

        verify(exactly = 1) { logcat.debug("d") }
        verify(exactly = 1) { logcat.info("i") }
        verify(exactly = 1) { logcat.error("e", failure) }
        verify(exactly = 1) { file.debug("d") }
        verify(exactly = 1) { file.info("i") }
        verify(exactly = 1) { file.error("e", failure) }
    }

    @Test
    fun `release logger drops debug everywhere, sends only error to logcat, and keeps info and error in file`() {
        val sut = BuildLoggers.release(logcat, file)

        sut.debug("d")
        sut.info("i")
        sut.error("e", failure)

        verify(exactly = 0) { logcat.debug(any()) }
        verify(exactly = 0) { logcat.info(any()) }
        verify(exactly = 1) { logcat.error("e", failure) }
        verify(exactly = 0) { file.debug(any()) }
        verify(exactly = 1) { file.info("i") }
        verify(exactly = 1) { file.error("e", failure) }
    }
}
