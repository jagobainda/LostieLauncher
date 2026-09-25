package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.service.link.ExternalLinkResult
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = TestContentService()
    private val downloads = TestDownloads()
    private val settings = TestSettingsStore()
    private val coordinator = LauncherDataCoordinator(content, downloads, mockk<Logger>(relaxed = true))
    private val navigation = NavigationStore()
    private val externalLinks = TestExternalLinkService()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `defaults to home and navigates with a localized title`() = runTest(dispatcher) {
        coordinator.refreshHome(AppLanguage.ESP)
        coordinator.refreshCatalogue()
        val sut = createSut()
        runCurrent()
        sut.state.value.section shouldBe LauncherSection.HOME
        sut.navigate(LauncherSection.GAMES)
        runCurrent()
        sut.state.value.section shouldBe LauncherSection.GAMES
        sut.state.value.title shouldBe dev.jagoba.lostielauncher.content.stringsFor(AppLanguage.ESP).titleGames
        settings.current.value = settings.current.value.copy(language = AppLanguage.FRA)
        runCurrent()
        sut.state.value.title shouldBe dev.jagoba.lostielauncher.content.stringsFor(AppLanguage.FRA).titleGames
    }

    @Test
    fun `refresh guard reacts to a transfer`() = runTest(dispatcher) {
        coordinator.refreshHome(AppLanguage.ESP)
        coordinator.refreshCatalogue()
        val sut = createSut()
        runCurrent()
        sut.state.value.canRefresh shouldBe true
        downloads.current.value = listOf(testDownload(testGame(), DownloadStatus.DOWNLOADING))
        runCurrent()
        sut.state.value.canRefresh shouldBe false
        sut.refresh()
        runCurrent()
        content.gameCalls shouldBe 1
    }

    @Test
    fun `refresh loads both feeds and restores the guard`() = runTest(dispatcher) {
        coordinator.refreshHome(AppLanguage.ESP)
        coordinator.refreshCatalogue()
        val sut = createSut()
        runCurrent()
        sut.refresh()
        runCurrent()
        content.homeCalls shouldBe 2
        content.gameCalls shouldBe 2
        sut.state.value.canRefresh shouldBe true
    }

    @Test
    fun `every section becomes active through a single navigation value`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        LauncherSection.entries.forEach { section ->
            sut.navigate(section)
            runCurrent()
            sut.state.value.section shouldBe section
        }
    }

    @Test
    fun `external social link outcome is visible without Android types in the ViewModel`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        externalLinks.result = ExternalLinkResult.NO_HANDLER
        sut.openExternalLink(ExternalLink.TWITCH)
        runCurrent()
        externalLinks.opened shouldBe listOf(ExternalLink.TWITCH)
        sut.state.value.externalLinkNotice shouldBe
            ExternalLinkNotice(ExternalLink.TWITCH, ExternalLinkResult.NO_HANDLER)
        sut.clearExternalLinkNotice()
        runCurrent()
        sut.state.value.externalLinkNotice shouldBe null
    }

    @Test
    fun `offline and screen loading state reach the shell`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value.canRefresh shouldBe false
        content.blocked = true
        coordinator.refreshHome(AppLanguage.ESP)
        coordinator.refreshCatalogue()
        runCurrent()
        sut.state.value.isOffline shouldBe true
        sut.state.value.canRefresh shouldBe true
    }

    private fun createSut() = MainViewModel(navigation, coordinator, settings, downloads, externalLinks)
}
