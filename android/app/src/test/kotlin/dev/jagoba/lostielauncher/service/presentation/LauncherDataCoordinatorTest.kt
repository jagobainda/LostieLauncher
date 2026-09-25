package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.ui.viewmodel.TestContentService
import dev.jagoba.lostielauncher.ui.viewmodel.TestDownloads
import dev.jagoba.lostielauncher.ui.viewmodel.TestSettingsStore
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherDataCoordinatorTest {
    @Test
    fun `process startup loads home and catalogue without screen ViewModels`() = runTest {
        val content = TestContentService()
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), mockk<Logger>(relaxed = true))
        val settings = TestSettingsStore()
        coordinator.start(backgroundScope, settings, HomeRefreshOptions(2.minutes))
        runCurrent()
        coordinator.home.value.isLoading shouldBe false
        coordinator.catalogue.value.isLoading shouldBe false
        content.homeCalls shouldBe 1
        content.gameCalls shouldBe 1
        coordinator.start(backgroundScope, settings, HomeRefreshOptions(2.minutes))
        runCurrent()
        content.gameCalls shouldBe 1
        advanceTimeBy(2.minutes.inWholeMilliseconds)
        runCurrent()
        content.homeCalls shouldBe 2
    }

    @Test
    fun `global refresh completes both fetches before advancing installed projection`() = runTest {
        val feed = CompletableDeferred<HomeContent>()
        val games = CompletableDeferred<List<GameInfo>>()
        val content = object : ContentService {
            override suspend fun getGames(): List<GameInfo> = games.await()

            override suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean): HomeContent =
                feed.await()

            override suspend fun isServerActionBlocked(forceRefresh: Boolean): Boolean = false
        }
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), mockk<Logger>(relaxed = true))
        val refresh = async { coordinator.refreshAll(AppLanguage.ESP) }
        runCurrent()
        coordinator.isRefreshing.value shouldBe true
        coordinator.gamesRevision.value shouldBe 0
        games.complete(emptyList())
        runCurrent()
        coordinator.gamesRevision.value shouldBe 0
        feed.complete(HomeContent())
        refresh.await()
        coordinator.gamesRevision.value shouldBe 1
        coordinator.isRefreshing.value shouldBe false
    }

    @Test
    fun `empty catalogue never purges download cache`() = runTest {
        val downloads = TestDownloads()
        val content = object : ContentService {
            override suspend fun getGames(): List<GameInfo> = emptyList()

            override suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean): HomeContent =
                HomeContent()

            override suspend fun isServerActionBlocked(forceRefresh: Boolean): Boolean = false
        }
        LauncherDataCoordinator(content, downloads, mockk<Logger>(relaxed = true)).refreshCatalogue()
        downloads.purged shouldBe emptyList()
    }
}
