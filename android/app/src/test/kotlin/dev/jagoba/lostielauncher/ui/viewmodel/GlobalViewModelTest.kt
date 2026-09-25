package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameActivityState
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val downloads = TestDownloads()
    private val coordinator = LauncherDataCoordinator(TestContentService(), downloads, mockk<Logger>(relaxed = true))
    private val launch = TestLaunch()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `download flow drives busy state without a mutable flag`() = runTest(dispatcher) {
        val sut = GlobalViewModel(downloads, coordinator, launch)
        runCurrent()
        sut.state.value.isBusy shouldBe false
        downloads.current.value = listOf(testDownload(testGame(), DownloadStatus.QUEUED))
        runCurrent()
        sut.state.value.isDownloading shouldBe true
        sut.state.value.isBusy shouldBe true
        downloads.current.value = emptyList()
        runCurrent()
        sut.state.value.isBusy shouldBe false
    }

    @Test
    fun `unsupported running state remains unknown`() = runTest(dispatcher) {
        val sut = GlobalViewModel(downloads, coordinator, launch)
        runCurrent()
        sut.state.value.isGameRunning shouldBe null
    }

    @Test
    fun `active sessions derive the count and running flag`() = runTest(dispatcher) {
        val sut = GlobalViewModel(downloads, coordinator, launch)
        val first = GameTarget(UUID.randomUUID(), "One")
        val second = GameTarget(UUID.randomUUID(), "Two")
        launch.current.value = GameActivityState.Active(setOf(first, second))
        runCurrent()
        sut.state.value.isGameRunning shouldBe true
        launch.current.value = GameActivityState.Active(setOf(first))
        runCurrent()
        sut.state.value.isGameRunning shouldBe true
        launch.current.value = GameActivityState.Active(emptySet())
        runCurrent()
        sut.state.value.isGameRunning shouldBe false
    }

    @Test
    fun `refresh status alone makes the launcher busy`() = runTest(dispatcher) {
        val content = TestContentService()
        val blocked = CompletableDeferred<Unit>()
        content.gameBarrier = blocked
        val refreshing = LauncherDataCoordinator(content, downloads, mockk<Logger>(relaxed = true))
        val other = GlobalViewModel(downloads, refreshing, launch)
        val refresh = async { refreshing.refreshAll(dev.jagoba.lostielauncher.model.AppLanguage.ESP) }
        runCurrent()
        other.state.value.isRefreshing shouldBe true
        other.state.value.isBusy shouldBe true
        blocked.complete(Unit)
        refresh.await()
        runCurrent()
        other.state.value.isBusy shouldBe false
    }
}
