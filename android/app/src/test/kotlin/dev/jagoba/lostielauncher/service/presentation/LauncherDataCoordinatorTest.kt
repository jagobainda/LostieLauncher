package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.ContentOptions
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.service.CatalogueResult
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.DefaultContentService
import dev.jagoba.lostielauncher.service.cdn.ContentApi
import dev.jagoba.lostielauncher.service.cdn.MaintenanceFlagApi
import dev.jagoba.lostielauncher.service.cdn.dto.GameDto
import dev.jagoba.lostielauncher.service.cdn.dto.HomeContentDto
import dev.jagoba.lostielauncher.ui.viewmodel.TestContentService
import dev.jagoba.lostielauncher.ui.viewmodel.TestDownloads
import dev.jagoba.lostielauncher.ui.viewmodel.TestNetworkMonitor
import dev.jagoba.lostielauncher.ui.viewmodel.TestSettingsStore
import dev.jagoba.lostielauncher.ui.viewmodel.testGame
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk<Logger>(relaxed = true))
        val settings = TestSettingsStore()
        coordinator.start(backgroundScope, settings, TestNetworkMonitor(), HomeRefreshOptions(2.minutes))
        runCurrent()
        coordinator.home.value.isLoading shouldBe false
        coordinator.catalogue.value.isLoading shouldBe false
        content.homeCalls shouldBe 1
        content.gameCalls shouldBe 1
        coordinator.start(backgroundScope, settings, TestNetworkMonitor(), HomeRefreshOptions(2.minutes))
        runCurrent()
        content.gameCalls shouldBe 1
        advanceTimeBy(2.minutes.inWholeMilliseconds)
        runCurrent()
        content.homeCalls shouldBe 2
    }

    @Test
    fun `global refresh completes both fetches before clearing the refresh state`() = runTest {
        val feed = CompletableDeferred<HomeContent>()
        val games = CompletableDeferred<CatalogueResult>()
        val content = object : ContentService {
            override suspend fun getGames(forceRefresh: Boolean): CatalogueResult = games.await()

            override suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean): HomeContent =
                feed.await()

            override suspend fun isServerActionBlocked(forceRefresh: Boolean): Boolean = false
        }
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk<Logger>(relaxed = true))
        val refresh = async { coordinator.refreshAll(AppLanguage.ESP) }
        runCurrent()
        coordinator.isRefreshing.value shouldBe true
        games.complete(CatalogueResult.Loaded(emptyList()))
        runCurrent()
        coordinator.isRefreshing.value shouldBe true
        feed.complete(HomeContent())
        refresh.await()
        coordinator.isRefreshing.value shouldBe false
    }

    @Test
    fun `empty catalogue never purges download cache`() = runTest {
        val downloads = TestDownloads()
        val content = object : ContentService {
            override suspend fun getGames(forceRefresh: Boolean): CatalogueResult = CatalogueResult.Loaded(emptyList())

            override suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean): HomeContent =
                HomeContent()

            override suspend fun isServerActionBlocked(forceRefresh: Boolean): Boolean = false
        }
        LauncherDataCoordinator(content, downloads, { false }, mockk<Logger>(relaxed = true)).refreshCatalogue()
        downloads.purged shouldBe emptyList()
    }

    @Test
    fun `catalogue entries sharing an id keep the first and log the rest`() = runTest {
        val logger = mockk<Logger>(relaxed = true)
        val content = TestContentService().apply {
            games = listOf(testGame(name = "Game 2"), testGame(name = "Game: 2"), testGame(name = "Other"))
        }
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, logger)

        coordinator.refreshCatalogue()

        coordinator.catalogue.value.games.map { it.name } shouldBe listOf("Game 2", "Other")
        verify(exactly = 1) { logger.info("Skipping duplicate catalogue entry: 'Game: 2' (id: game-2).") }
    }

    @Test
    fun `home carries whether the Twitch channel is live`() = runTest {
        var live = true
        val coordinator = LauncherDataCoordinator(
            TestContentService(),
            TestDownloads(),
            { live },
            mockk<Logger>(relaxed = true),
        )

        coordinator.refreshHome(AppLanguage.ESP)
        coordinator.home.value.isTwitchLive shouldBe true

        live = false
        coordinator.refreshHome(AppLanguage.ESP)
        coordinator.home.value.isTwitchLive shouldBe false
    }

    @Test
    fun `home timer waits while nothing is visible and refreshes on return`() = runTest {
        val content = TestContentService()
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk<Logger>(relaxed = true))
        coordinator.start(backgroundScope, TestSettingsStore(), TestNetworkMonitor(), HomeRefreshOptions(2.minutes))
        runCurrent()
        coordinator.setVisible(false)

        advanceTimeBy(10.minutes.inWholeMilliseconds)
        runCurrent()
        content.homeCalls shouldBe 1

        coordinator.setVisible(true)
        runCurrent()
        content.homeCalls shouldBe 2
    }

    @Test
    fun `losing the connection enters offline mode without refetching anything`() = runTest {
        val content = TestContentService()
        val network = TestNetworkMonitor()
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk<Logger>(relaxed = true))
        coordinator.start(backgroundScope, TestSettingsStore(), network, HomeRefreshOptions(2.minutes))
        runCurrent()
        content.blocked = true

        network.isOnline.value = false
        runCurrent()

        coordinator.home.value.isOffline shouldBe true
        content.homeForces.last() shouldBe false
        content.gameCalls shouldBe 1
    }

    @Test
    fun `regaining the connection reloads home and catalogue without a manual refresh`() = runTest {
        val content = TestContentService().apply { blocked = true }
        val network = TestNetworkMonitor(online = false)
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk<Logger>(relaxed = true))
        coordinator.start(backgroundScope, TestSettingsStore(), network, HomeRefreshOptions(2.minutes))
        runCurrent()
        coordinator.home.value.isOffline shouldBe true
        content.blocked = false
        content.games = listOf(testGame())

        network.isOnline.value = true
        runCurrent()

        coordinator.home.value.isOffline shouldBe false
        coordinator.catalogue.value.games.map { it.gameId } shouldBe listOf(testGame().gameId)
        content.flagForces.last() shouldBe true
        coordinator.isRefreshing.value shouldBe false
    }

    @Test
    fun `failed or blocked refresh keeps the previously loaded catalogue and marks it stale`() = runTest {
        val game = testGame()
        val downloads = TestDownloads()
        val content = TestContentService().apply { games = listOf(game) }
        val coordinator = LauncherDataCoordinator(content, downloads, { false }, mockk<Logger>(relaxed = true))

        coordinator.refreshCatalogue()
        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe false
        downloads.purged shouldBe listOf(setOf(game.gameId))

        content.catalogueResult = CatalogueResult.Blocked
        coordinator.refreshCatalogue()
        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe true
        coordinator.catalogue.value.isLoading shouldBe false
        downloads.purged shouldBe listOf(setOf(game.gameId))

        content.catalogueResult = CatalogueResult.Failed
        coordinator.refreshCatalogue()
        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe true

        content.catalogueResult = null
        coordinator.refreshCatalogue()
        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe false
    }

    @Test
    fun `distinguishes a failed cold catalogue load from a genuinely empty catalogue`() = runTest {
        val content = TestContentService().apply { catalogueResult = CatalogueResult.Failed }
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk<Logger>(relaxed = true))

        coordinator.refreshCatalogue()
        coordinator.catalogue.value.games shouldBe emptyList()
        coordinator.catalogue.value.isStale shouldBe true

        content.catalogueResult = CatalogueResult.Loaded(emptyList())
        coordinator.refreshCatalogue()
        coordinator.catalogue.value.games shouldBe emptyList()
        coordinator.catalogue.value.isStale shouldBe false
    }

    @Test
    fun `periodic tick retries a failed catalogue even while the network stays connected`() = runTest {
        val game = testGame()
        val content = TestContentService().apply { catalogueResult = CatalogueResult.Failed }
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk<Logger>(relaxed = true))
        coordinator.start(backgroundScope, TestSettingsStore(), TestNetworkMonitor(), HomeRefreshOptions(2.minutes))
        runCurrent()
        coordinator.catalogue.value.games shouldBe emptyList()
        coordinator.catalogue.value.isStale shouldBe true
        content.gameCalls shouldBe 1

        content.catalogueResult = null
        content.games = listOf(game)
        advanceTimeBy(2.minutes.inWholeMilliseconds)
        runCurrent()

        content.gameCalls shouldBe 2
        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe false

        advanceTimeBy(2.minutes.inWholeMilliseconds)
        runCurrent()
        content.gameCalls shouldBe 2
    }

    @Test
    fun `reconnection and global refresh probe the maintenance flag once while standalone retry probes afresh`() =
        runTest {
            val contentApi = mockk<ContentApi>()
            val flagApi = mockk<MaintenanceFlagApi>()
            coEvery { contentApi.homeContent(any()) } returns HomeContentDto()
            coEvery { contentApi.catalogue(any()) } returns listOf(GameDto(nombre = "Demo"))
            coEvery { flagApi.head(any()) } returns 404
            val network = TestNetworkMonitor(online = false)
            val content = createContentService(contentApi, flagApi, network)
            val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk(relaxed = true))
            coordinator.start(backgroundScope, TestSettingsStore(), network, HomeRefreshOptions(2.minutes))
            runCurrent()
            coVerify(exactly = 0) { flagApi.head(any()) }

            network.isOnline.value = true
            runCurrent()
            coVerify(exactly = 1) { flagApi.head(any()) }
            coordinator.catalogue.value.games.map { it.name } shouldBe listOf("Demo")

            coordinator.refreshAll(AppLanguage.ESP)
            coVerify(exactly = 2) { flagApi.head(any()) }

            coordinator.refreshCatalogue(force = true)
            coVerify(exactly = 3) { flagApi.head(any()) }
        }

    @Test
    fun `successful catalogue retry after maintenance clears offline mode immediately`() = runTest {
        val game = testGame()
        val content = TestContentService().apply {
            blocked = true
            catalogueResult = CatalogueResult.Blocked
        }
        val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk(relaxed = true))
        coordinator.start(backgroundScope, TestSettingsStore(), TestNetworkMonitor(), HomeRefreshOptions(2.minutes))
        runCurrent()
        coordinator.home.value.isOffline shouldBe true
        coordinator.catalogue.value.games shouldBe emptyList()
        coordinator.catalogue.value.isStale shouldBe true

        content.blocked = false
        content.catalogueResult = null
        content.games = listOf(game)
        coordinator.refreshCatalogue(force = true)

        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe false
        coordinator.home.value.isOffline shouldBe false
    }

    @Test
    fun `an older home refresh finishing after a successful catalogue retry does not restore offline mode`() = runTest {
        val game = testGame()
        val twitchGate = CompletableDeferred<Boolean>()
        val content = TestContentService().apply {
            blocked = true
            catalogueResult = CatalogueResult.Blocked
        }
        val coordinator = LauncherDataCoordinator(
            content,
            TestDownloads(),
            { twitchGate.await() },
            mockk(relaxed = true),
        )
        coordinator.refreshCatalogue(force = true)
        coordinator.home.value.isOffline shouldBe true

        val slowHome = async { coordinator.refreshHome(AppLanguage.ESP) }
        runCurrent()
        content.homeCalls shouldBe 1
        content.flagForces shouldBe listOf(true)

        content.blocked = false
        content.catalogueResult = null
        content.games = listOf(game)
        coordinator.refreshCatalogue(force = true)
        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe false
        coordinator.home.value.isOffline shouldBe false

        twitchGate.complete(false)
        slowHome.await()

        coordinator.catalogue.value.games shouldBe listOf(game)
        coordinator.catalogue.value.isStale shouldBe false
        coordinator.home.value.isOffline shouldBe false
    }

    @Test
    fun `an older catalogue response finishing after a newer maintenance check does not clear offline mode`() =
        runTest {
            var flagStatus = 404
            val catalogueGate = CompletableDeferred<List<GameDto>>()
            val contentApi = mockk<ContentApi>()
            val flagApi = mockk<MaintenanceFlagApi>()
            coEvery { contentApi.homeContent(any()) } returns HomeContentDto()
            coEvery { contentApi.catalogue(any()) } coAnswers { catalogueGate.await() }
            coEvery { flagApi.head(any()) } answers { flagStatus }
            val content = createContentService(contentApi, flagApi)
            val coordinator = LauncherDataCoordinator(content, TestDownloads(), { false }, mockk(relaxed = true))

            val slowCatalogue = async { coordinator.refreshCatalogue(force = true) }
            runCurrent()

            flagStatus = 200
            coordinator.refreshHome(AppLanguage.ESP, force = true)
            coordinator.home.value.isOffline shouldBe true

            catalogueGate.complete(listOf(GameDto(nombre = "Demo")))
            slowCatalogue.await()

            coordinator.catalogue.value.games.map { it.name } shouldBe listOf("Demo")
            coordinator.catalogue.value.isStale shouldBe false
            coordinator.home.value.isOffline shouldBe true
            content.isServerActionBlocked(forceRefresh = false) shouldBe true
        }

    private fun createContentService(
        contentApi: ContentApi,
        flagApi: MaintenanceFlagApi,
        network: TestNetworkMonitor = TestNetworkMonitor(),
    ) = DefaultContentService(
        contentApi = contentApi,
        maintenanceFlagApi = flagApi,
        network = network,
        options = ContentOptions(
            cdnBaseUrl = "https://content.test",
            catalogueUrl = "https://content.test/list.json",
            homeContentUrl = "https://content.test/notifications.json",
            maintenanceFlagUrl = "https://content.test/flag.txt",
            maintenanceFlagCacheDuration = 30.seconds,
        ),
        clock = Clock.fixed(Instant.parse("2026-06-19T12:00:00Z"), ZoneOffset.UTC),
        logger = mockk(relaxed = true),
    )
}
