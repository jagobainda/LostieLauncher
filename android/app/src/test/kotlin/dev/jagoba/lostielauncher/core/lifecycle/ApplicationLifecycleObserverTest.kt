package dev.jagoba.lostielauncher.core.lifecycle

import androidx.lifecycle.LifecycleOwner
import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.LogMaintenance
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ApplicationLifecycleObserver")
@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationLifecycleObserverTest {
    private val settingsStore = mockk<SettingsStore>(relaxed = true)
    private val logMaintenance = mockk<LogMaintenance>(relaxed = true)
    private val owner = mockk<LifecycleOwner>()

    @Test
    fun `purges expired logs when the process is created`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.onCreate(owner)
        advanceUntilIdle()

        verify(exactly = 1) { logMaintenance.purgeExpired() }
    }

    @Test
    fun `flushes pending settings when the process enters the background`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.onStop(owner)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsStore.flush() }
    }

    private fun createSut(dispatcher: CoroutineDispatcher) = ApplicationLifecycleObserver(
        settingsStore = settingsStore,
        logMaintenance = logMaintenance,
        dispatchers = TestDispatcherProvider(dispatcher),
    )
}
