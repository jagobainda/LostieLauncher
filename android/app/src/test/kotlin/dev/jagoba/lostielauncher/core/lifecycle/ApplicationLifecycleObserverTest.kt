package dev.jagoba.lostielauncher.core.lifecycle

import androidx.lifecycle.LifecycleOwner
import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.service.presentation.GameAutoUpdateCoordinator
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.LogMaintenance
import dev.jagoba.lostielauncher.util.log.Logger
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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
    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `purges expired logs when the process is created without starting launcher work`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.onCreate(owner)
        advanceUntilIdle()

        verify(exactly = 1) { logMaintenance.purgeExpired() }
        verify(exactly = 1) { logger.info("Application started.") }
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

    @Test
    fun `tells the coordinator whether the launcher is visible`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.onStart(owner)
        sut.onStop(owner)
        sut.onStart(owner)

        verifyOrder {
            coordinator.setVisible(true)
            coordinator.setVisible(false)
            coordinator.setVisible(true)
        }
    }

    private fun createSut(dispatcher: CoroutineDispatcher) = ApplicationLifecycleObserver(
        settingsStore = settingsStore,
        logMaintenance = logMaintenance,
        coordinator = coordinator,
        homeRefreshOptions = HomeRefreshOptions(2.minutes),
        gameAutoUpdates = gameAutoUpdates,
        logger = logger,
        dispatchers = TestDispatcherProvider(dispatcher),
    )
}
