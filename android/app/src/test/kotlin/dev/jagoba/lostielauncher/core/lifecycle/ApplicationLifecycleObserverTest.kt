package dev.jagoba.lostielauncher.core.lifecycle

import androidx.lifecycle.LifecycleOwner
import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.service.presentation.GameAutoUpdateCoordinator
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.LogMaintenance
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Duration.Companion.minutes
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
    private val coordinator = mockk<LauncherDataCoordinator>(relaxed = true)
    private val gameAutoUpdates = mockk<GameAutoUpdateCoordinator>(relaxed = true)

    @Test
    fun `purges expired logs when the process is created without starting launcher work`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.onCreate(owner)
        advanceUntilIdle()

        verify(exactly = 1) { logMaintenance.purgeExpired() }
        verify(exactly = 0) { coordinator.start(any(), any(), any()) }
        verify(exactly = 0) { gameAutoUpdates.start(any()) }
    }

    @Test
    fun `starts launcher work once when the process first becomes visible`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.onCreate(owner)
        sut.onStart(owner)
        sut.onStart(owner)

        verify(exactly = 1) { coordinator.start(any(), settingsStore, any()) }
        verify(exactly = 1) { gameAutoUpdates.start(any()) }
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
        coordinator = coordinator,
        homeRefreshOptions = HomeRefreshOptions(2.minutes),
        gameAutoUpdates = gameAutoUpdates,
        dispatchers = TestDispatcherProvider(dispatcher),
    )
}
