package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppSettings
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.service.game.KirinSetupService
import dev.jagoba.lostielauncher.service.link.ExternalLinkResult
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.service.update.LauncherUpdateService
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val settings = TestSettingsStore().apply { current.value = AppSettings(hasSeenWelcome = true) }
    private var twitchLive = false
    private val coordinator = LauncherDataCoordinator(content, downloads, { twitchLive }, mockk<Logger>(relaxed = true))
    private val navigation = NavigationStore()
    private val externalLinks = TestExternalLinkService()
    private val kirinStatus = MutableStateFlow<KirinStatus?>(KirinStatus.Ready)
    private val kirinDownload = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Unavailable)
    private val kirin = mockk<KirinSetupService>(relaxed = true) {
        every { status } returns kirinStatus
        every { download } returns kirinDownload
    }
    private val launcherOffer = MutableStateFlow<String?>(null)
    private val launcherDownload = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Checking)
    private val launcherUpdate = mockk<LauncherUpdateService>(relaxed = true) {
        every { offeredVersion } returns launcherOffer
        every { download } returns launcherDownload
    }

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
    fun `external social link opens through the link seam and a failure stays silent`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        externalLinks.result = ExternalLinkResult.NO_HANDLER
        val before = sut.state.value
        sut.openExternalLink(ExternalLink.TWITCH)
        runCurrent()
        externalLinks.opened shouldBe listOf(ExternalLink.TWITCH)
        sut.state.value shouldBe before
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

    @Test
    fun `Twitch live status reaches the shell`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value.isTwitchLive shouldBe false
        twitchLive = true
        coordinator.refreshHome(AppLanguage.ESP)
        runCurrent()
        sut.state.value.isTwitchLive shouldBe true
    }

    @Test
    fun `title bar links follow the active section`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value.contextLinks shouldBe listOf(ExternalLink.TWITCH, ExternalLink.YOUTUBE, ExternalLink.TWITTER)
        sut.navigate(LauncherSection.SETTINGS)
        runCurrent()
        sut.state.value.contextLinks shouldBe listOf(ExternalLink.GITHUB)
        listOf(LauncherSection.GAMES, LauncherSection.LIBRARY, LauncherSection.FAQS).forEach { section ->
            sut.navigate(section)
            runCurrent()
            sut.state.value.contextLinks shouldBe emptyList()
        }
    }

    @Test
    fun `back returns to home from any section and is not handled on home`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value.canNavigateBack shouldBe false
        sut.navigateBack()
        runCurrent()
        sut.state.value.section shouldBe LauncherSection.HOME
        LauncherSection.entries.filter { it != LauncherSection.HOME }.forEach { section ->
            sut.navigate(section)
            runCurrent()
            sut.state.value.canNavigateBack shouldBe true
            sut.navigateBack()
            runCurrent()
            sut.state.value.section shouldBe LauncherSection.HOME
            sut.state.value.canNavigateBack shouldBe false
        }
    }

    @Test
    fun `refresh indicator is disabled while the global refresh runs`() = runTest(dispatcher) {
        coordinator.refreshHome(AppLanguage.ESP)
        coordinator.refreshCatalogue()
        val sut = createSut()
        runCurrent()
        content.gameBarrier = CompletableDeferred()
        sut.refresh()
        runCurrent()
        sut.state.value.isRefreshing shouldBe true
        sut.state.value.canRefresh shouldBe false
        content.gameBarrier?.complete(Unit)
        runCurrent()
        sut.state.value.isRefreshing shouldBe false
        sut.state.value.canRefresh shouldBe true
    }

    @Test
    fun `first run opens the library, persists the flag and shows the welcome once`() = runTest(dispatcher) {
        settings.current.value = AppSettings(hasSeenWelcome = false)
        val sut = createSut()
        runCurrent()
        sut.state.value.section shouldBe LauncherSection.LIBRARY
        sut.state.value.isWelcomeVisible shouldBe true
        settings.current.value.hasSeenWelcome shouldBe true
        sut.dismissWelcome()
        runCurrent()
        sut.state.value.isWelcomeVisible shouldBe false
    }

    @Test
    fun `a returning user sees no welcome and stays on home`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value.isWelcomeVisible shouldBe false
        sut.state.value.section shouldBe LauncherSection.HOME
    }

    @Test
    fun `offers the recommended Kirin update only on My Games while a release is available`() = runTest(dispatcher) {
        kirinStatus.value = KirinStatus.UpdateRecommended("0.4.0", "0.4.2")
        kirinDownload.value = ApkDownloadState.Available
        val sut = createSut()
        runCurrent()
        sut.state.value.kirinUpdate shouldBe null

        sut.navigate(LauncherSection.GAMES)
        runCurrent()
        sut.state.value.kirinUpdate shouldBe KirinUpdateUiState("0.4.0", "0.4.2")

        kirinDownload.value = ApkDownloadState.Downloading(42)
        runCurrent()
        sut.state.value.kirinUpdate?.downloadPercent shouldBe 42

        kirinDownload.value = ApkDownloadState.Unavailable
        runCurrent()
        sut.state.value.kirinUpdate shouldBe null
    }

    @Test
    fun `does not offer a Kirin update when none is recommended`() = runTest(dispatcher) {
        kirinDownload.value = ApkDownloadState.Available
        val sut = createSut()
        sut.navigate(LauncherSection.GAMES)
        runCurrent()

        sut.state.value.kirinUpdate shouldBe null
    }

    @Test
    fun `updates Kirin through the setup service`() = runTest(dispatcher) {
        val sut = createSut()

        sut.updateKirin()
        runCurrent()

        coVerify(exactly = 1) { kirin.install() }
    }

    @Test
    fun `asks about an offered launcher update in every section`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value.launcherUpdatePrompt shouldBe null

        launcherOffer.value = "1.1.0"
        launcherDownload.value = ApkDownloadState.Available
        runCurrent()

        LauncherSection.entries.forEach { section ->
            sut.navigate(section)
            runCurrent()
            sut.state.value.launcherUpdatePrompt shouldBe "1.1.0"
        }
    }

    @Test
    fun `waits for game downloads and the welcome before asking about a launcher update`() = runTest(dispatcher) {
        settings.current.value = AppSettings(hasSeenWelcome = false)
        launcherOffer.value = "1.1.0"
        launcherDownload.value = ApkDownloadState.Available
        val sut = createSut()
        runCurrent()
        sut.state.value.launcherUpdatePrompt shouldBe null

        sut.dismissWelcome()
        downloads.current.value = listOf(testDownload(testGame(), DownloadStatus.DOWNLOADING))
        runCurrent()
        sut.state.value.launcherUpdatePrompt shouldBe null

        downloads.current.value = emptyList()
        runCurrent()
        sut.state.value.launcherUpdatePrompt shouldBe "1.1.0"
    }

    @Test
    fun `follows the launcher update download in the title bar and reports its failure`() = runTest(dispatcher) {
        launcherOffer.value = "1.1.0"
        val sut = createSut()

        launcherDownload.value = ApkDownloadState.Downloading(42)
        runCurrent()
        sut.state.value.launcherUpdatePercent shouldBe 42
        sut.state.value.launcherUpdatePrompt shouldBe null

        launcherDownload.value = ApkDownloadState.Failed
        runCurrent()
        sut.state.value.launcherUpdatePercent shouldBe null
        sut.state.value.launcherUpdateFailed shouldBe true
        sut.state.value.launcherUpdatePrompt shouldBe null
    }

    @Test
    fun `answers the launcher update through the update service`() = runTest(dispatcher) {
        val sut = createSut()

        sut.acceptLauncherUpdate()
        sut.declineLauncherUpdate()
        sut.dismissLauncherUpdateFailure()
        runCurrent()

        coVerify(exactly = 1) { launcherUpdate.install() }
        verify(exactly = 1) { launcherUpdate.decline() }
        verify(exactly = 1) { launcherUpdate.acknowledgeFailure() }
    }

    private fun createSut() = MainViewModel(
        navigation,
        coordinator,
        settings,
        downloads,
        externalLinks,
        kirin,
        launcherUpdate,
        mockk<Logger>(relaxed = true),
    )
}
