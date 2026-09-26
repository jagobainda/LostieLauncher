package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.SpecialVersionConfig
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionLookup
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = TestContentService()
    private val downloads = TestDownloads()
    private val installation = TestInstallation()
    private val library = TestLibraryStore()
    private val special = TestSpecialVersionService()
    private val navigation = NavigationStore()
    private val settings = TestSettingsStore()
    private val externalLinks = TestExternalLinkService()
    private val coordinator = LauncherDataCoordinator(content, downloads, mockk<Logger>(relaxed = true))

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `catalogue load and empty state mirror the desktop`() = runTest(dispatcher) {
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.isLoading shouldBe false
        sut.state.value.isEmpty shouldBe true
        content.gameCalls shouldBe 1
    }

    @Test
    fun `older installed version is marked update available`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().hasUpdate shouldBe true
        sut.state.value.games.single().status shouldBe LibraryCardStatus.UPDATE_AVAILABLE
    }

    @Test
    fun `installed current game has downloaded status and no start action`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
        )
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().status shouldBe LibraryCardStatus.DOWNLOADED
        sut.state.value.games.single().canStart shouldBe false
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe null
    }

    @Test
    fun `missing game is available and active transfer is downloading`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().status shouldBe LibraryCardStatus.AVAILABLE
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        runCurrent()
        sut.state.value.games.single().status shouldBe LibraryCardStatus.DOWNLOADING
        sut.state.value.games.single().canStart shouldBe false
        sut.state.value.games.single().remainingTime shouldBe "10s"
    }

    @Test
    fun `download asks for confirmation and starts only after acceptance`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe remote.gameId
        downloads.starts.size shouldBe 0
        sut.confirmDownload()
        runCurrent()
        downloads.starts.single().args.gameId shouldBe remote.gameId
    }

    @Test
    fun `paused download resumes without confirmation`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.PAUSED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe null
        downloads.resumed shouldBe listOf(remote.gameId)
    }

    @Test
    fun `paused game resumes its own transfer while another paused row remains untouched`() = runTest(dispatcher) {
        val first = testGame(name = "First")
        val second = testGame(name = "Second")
        content.games = listOf(first, second)
        downloads.current.value = listOf(
            testDownload(first, DownloadStatus.PAUSED),
            testDownload(second, DownloadStatus.PAUSED),
        )
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(second.gameId)
        runCurrent()
        downloads.resumed shouldBe listOf(second.gameId)
    }

    @Test
    fun `active transfer prevents a second download prompt`() = runTest(dispatcher) {
        val first = testGame(name = "First")
        val second = testGame(name = "Second")
        content.games = listOf(first, second)
        downloads.current.value = listOf(testDownload(first, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(second.gameId)
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe null
        downloads.starts.size shouldBe 0
    }

    @Test
    fun `catalogue card uses stored playtime and global refresh reloads it`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        library.times = mapOf(remote.id!! to 12)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().playtimeMinutes shouldBe 12
        library.times = mapOf(remote.id to 28)
        coordinator.refreshAll(AppLanguage.ESP)
        runCurrent()
        sut.state.value.games.single().playtimeMinutes shouldBe 28
    }

    @Test
    fun `completed transfer shows installation seam outcome`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.COMPLETED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().installation shouldBe
            GameInstallationState.Finished(GameInstallationResult.NotSupportedYet)
        sut.state.value.games.single().installationUnsupported shouldBe true
        sut.state.value.games.single().status shouldBe LibraryCardStatus.INSTALLATION_UNSUPPORTED
    }

    @Test
    fun `maintenance blocks new download without calling manager`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        content.blocked = true
        sut.confirmDownload()
        runCurrent()
        downloads.starts.size shouldBe 0
        sut.state.value.notice shouldBe LibraryNotice.SERVER_ACTIONS_UNAVAILABLE
    }

    @Test
    fun `maintenance keeps action enabled and explains once until successful check`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        content.blocked = true
        sut.state.value.games.single().canStart shouldBe true
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.SERVER_ACTIONS_UNAVAILABLE
        sut.clearNotice()
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.notice shouldBe null
        content.blocked = false
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe remote.gameId
        sut.dismissPrompt()
        content.blocked = true
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.SERVER_ACTIONS_UNAVAILABLE
    }

    @Test
    fun `confirmation key selects the matching special build`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val key = "ABCD-1234-EFGH-5678-IJKL"
        special.result = SpecialVersionLookup.Found(
            SpecialVersionConfig("a".repeat(64), "special", remote.id!!, "v1.5", "special.zip"),
        )
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.confirmDownload(" $key ")
        runCurrent()
        special.keys shouldBe listOf(key)
        downloads.starts.single().args.key shouldBe key
        downloads.starts.single().args.version shouldBe "v1.5"
    }

    @Test
    fun `unparseable config and parsed invalid config show distinct notices`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestSpecialVersion(remote.gameId)
        runCurrent()
        special.result = SpecialVersionLookup.InvalidResponse
        sut.submitSpecialVersionKey("ABCD-1234-EFGH-5678-IJKL")
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.SPECIAL_DOWNLOAD_ERROR
        sut.requestSpecialVersion(remote.gameId)
        runCurrent()
        special.result = SpecialVersionLookup.Found(
            SpecialVersionConfig("bad", "special", remote.id!!, "v1", "special.zip"),
        )
        sut.submitSpecialVersionKey("ABCD-1234-EFGH-5678-IJKL")
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.SPECIAL_KEY_NOT_FOUND
    }

    @Test
    fun `invalid special key is rejected before network lookup`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestSpecialVersion(remote.gameId)
        runCurrent()
        sut.submitSpecialVersionKey("bad")
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.SPECIAL_KEY_INVALID
        special.lookups shouldBe 0
    }

    @Test
    fun `successful catalogue load purges only known downloads`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        createSut(backgroundScope)
        runCurrent()
        downloads.purged shouldBe listOf(setOf(remote.gameId))
    }

    @Test
    fun `navigation update waits for catalogue and preserves scroll target`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)))
        navigation.navigate(LauncherSection.LIBRARY, remote.gameId, LibraryNavigationAction.UPDATE)
        createSut(backgroundScope)
        runCurrent()
        downloads.starts.single().args.gameId shouldBe remote.gameId
        navigation.state.value.pendingLibraryGameId shouldBe remote.gameId
        navigation.state.value.libraryAction shouldBe null
    }

    @Test
    fun `missing folder navigation offers reinstall for an otherwise downloaded game`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
        )
        navigation.navigate(LauncherSection.LIBRARY, remote.gameId, LibraryNavigationAction.DOWNLOAD)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().canStart shouldBe false
        sut.state.value.pendingDownloadGameId shouldBe remote.gameId
        navigation.state.value.libraryAction shouldBe null
    }

    @Test
    fun `missing folder navigation offers current catalogue version when update exists`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)),
        )
        navigation.navigate(LauncherSection.LIBRARY, remote.gameId, LibraryNavigationAction.DOWNLOAD)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().status shouldBe LibraryCardStatus.UPDATE_AVAILABLE
        sut.state.value.games.single().canStart shouldBe false
        sut.state.value.pendingDownloadGameId shouldBe remote.gameId
        navigation.state.value.libraryAction shouldBe null
    }

    @Test
    fun `a transfer that fails or is denied raises its notice once`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.FAILED))
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.DOWNLOAD_FAILED
        sut.clearNotice()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.FAILED).copy(percent = 51.0))
        runCurrent()
        sut.state.value.notice shouldBe null
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.QUEUED))
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.PERMISSION_DENIED))
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.DOWNLOAD_PERMISSION_DENIED
    }

    @Test
    fun `a transfer that had already failed before the screen existed raises nothing`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.FAILED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.notice shouldBe null
    }

    @Test
    fun `download prompt carries the destination and the game page`() = runTest(dispatcher) {
        val remote = testGame().copy(pageUrl = "https://example.com/game")
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.pendingDownloadGame shouldBe remote
        sut.state.value.downloadDestination shouldBe downloads.destination
        sut.openPendingGamePage()
        externalLinks.openedUrls shouldBe listOf("https://example.com/game")
        sut.dismissPrompt()
        runCurrent()
        sut.state.value.pendingDownloadGame shouldBe null
        sut.state.value.downloadDestination shouldBe null
    }

    @Test
    fun `an installation that turns into a failure raises its notice`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val phase = MutableStateFlow<GameInstallationState>(GameInstallationState.VerifyingIntegrity)
        installation.installation = phase
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.COMPLETED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.notice shouldBe null
        phase.value = GameInstallationState.Finished(GameInstallationResult.HashMismatch)
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.INSTALLATION_HASH_MISMATCH
    }

    @Test
    fun `a transfer that completes into a failed installation raises the installation notice`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        installation.installation = flowOf(GameInstallationState.Finished(GameInstallationResult.ExtractionFailed))
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.COMPLETED))
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.INSTALLATION_FAILED
    }

    @Test
    fun `an installation that had already failed before the screen existed raises nothing`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        installation.installation = flowOf(GameInstallationState.Finished(GameInstallationResult.InvalidHash))
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.COMPLETED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.notice shouldBe null
        sut.state.value.games.single().status shouldBe LibraryCardStatus.INSTALLATION_FAILED
    }

    private fun createSut(scope: CoroutineScope): LibraryViewModel {
        coordinator.start(scope, settings, HomeRefreshOptions(2.minutes))
        return LibraryViewModel(
            coordinator,
            downloads,
            installation,
            library,
            content,
            special,
            navigation,
            externalLinks,
        )
    }
}
