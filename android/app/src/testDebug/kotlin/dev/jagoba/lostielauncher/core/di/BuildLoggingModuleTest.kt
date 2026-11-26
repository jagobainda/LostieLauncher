package dev.jagoba.lostielauncher.core.di

import android.util.Log
import dev.jagoba.lostielauncher.service.log.FileLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BuildLoggingModule (debug)")
class BuildLoggingModuleTest {
    private val fileLogger = mockk<FileLogger>(relaxed = true)
    private val failure = IllegalStateException("boom")

    @Test
    fun `provides the debug logger wired to logcat and the file logger`() {
        mockkStatic(Log::class) {
            every { Log.d(any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
            val sut = BuildLoggingModule.provideLogger(fileLogger)

            sut.debug("d")
            sut.info("i")
            sut.error("e", failure)

            verify(exactly = 1) { Log.d("LostieLauncher", "d") }
            verify(exactly = 1) { Log.i("LostieLauncher", "i") }
            verify(exactly = 1) { Log.e("LostieLauncher", "e", failure) }
            verify(exactly = 1) { fileLogger.debug("d") }
            verify(exactly = 1) { fileLogger.info("i") }
            verify(exactly = 1) { fileLogger.error("e", failure) }
        }
    }
}
