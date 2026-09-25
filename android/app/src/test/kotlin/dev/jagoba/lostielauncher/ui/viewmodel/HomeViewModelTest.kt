package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.viewModelScope
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.model.NewsItem
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = TestContentService()
    private val settings = TestSettingsStore()
    private val coordinator = LauncherDataCoordinator(content, TestDownloads(), mockk<Logger>(relaxed = true))

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load exposes empty and stale content states`() = runTest(dispatcher) {
        content.home = HomeContent(isStale = true)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.isLoading shouldBe false
        sut.state.value.isEmpty shouldBe true
        sut.state.value.isOutOfDateWarningVisible shouldBe true
        sut.viewModelScope.cancel()
    }

    @Test
    fun `initial content publishes list state and news`() = runTest(dispatcher) {
        val news = NewsItem(null, "Title", "Description", "Tag", LocalDateTime.of(2026, 9, 23, 12, 0), null)
        content.home = HomeContent(news = listOf(news))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.news shouldBe listOf(news)
        sut.state.value.isListVisible shouldBe true
        sut.state.value.isEmpty shouldBe false
        sut.state.value.isNotificationsEmpty shouldBe true
        sut.viewModelScope.cancel()
    }

    @Test
    fun `maintenance mode suppresses stale warning but remains visible as offline`() = runTest(dispatcher) {
        content.home = HomeContent(isStale = true)
        content.blocked = true
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.isOffline shouldBe true
        sut.state.value.isOutOfDateWarningVisible shouldBe false
        sut.viewModelScope.cancel()
    }

    @Test
    fun `language change and periodic tick refresh the feed`() = runTest(dispatcher) {
        val sut = createSut(backgroundScope)
        runCurrent()
        content.homeCalls shouldBe 1
        settings.current.value = settings.current.value.copy(language = AppLanguage.FRA)
        runCurrent()
        content.homeCalls shouldBe 2
        advanceTimeBy(2.minutes.inWholeMilliseconds)
        runCurrent()
        content.homeCalls shouldBe 3
        sut.viewModelScope.cancel()
    }

    @Test
    fun `successful refresh clears a stale warning`() = runTest(dispatcher) {
        content.home = HomeContent(isStale = true)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.isOutOfDateWarningVisible shouldBe true
        content.home = HomeContent()
        sut.refresh()
        runCurrent()
        sut.state.value.isOutOfDateWarningVisible shouldBe false
        sut.viewModelScope.cancel()
    }

    @Test
    fun `manual and timer refreshes force both content and maintenance checks`() = runTest(dispatcher) {
        val sut = createSut(backgroundScope)
        runCurrent()
        content.homeForces shouldBe listOf(false)
        sut.refresh()
        runCurrent()
        advanceTimeBy(2.minutes.inWholeMilliseconds)
        runCurrent()
        content.homeForces shouldBe listOf(false, true, true)
        content.flagForces shouldBe listOf(false, true, true)
        sut.viewModelScope.cancel()
    }

    @Test
    fun `cleared Home ViewModel does not stop process refreshes`() = runTest(dispatcher) {
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.viewModelScope.cancel()
        advanceTimeBy(4.minutes.inWholeMilliseconds)
        runCurrent()
        content.homeCalls shouldBe 3
    }

    private fun createSut(scope: CoroutineScope): HomeViewModel {
        coordinator.start(scope, settings, HomeRefreshOptions(2.minutes))
        return HomeViewModel(coordinator, settings)
    }
}
