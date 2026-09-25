package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.OpenGameLocationResult
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
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
class GamesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = TestContentService()
    private val installation = TestInstallation()
    private val launch = TestLaunch()
    private val locations = TestLocations()
    private val library = TestLibraryStore()
    private val navigation = NavigationStore()
    private val downloads = TestDownloads()
    private val coordinator = LauncherDataCoordinator(content, downloads, mockk<Logger>(relaxed = true))

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `unsupported installed state stays distinct from empty library`() = runTest(dispatcher) {
        coordinator.refreshCatalogue()
        val sut = createSut()
        runCurrent()
        sut.state.value.installedStateKnown shouldBe false
        sut.state.value.isEmpty shouldBe false
    }

    @Test
    fun `installed game receives remote update and playtime`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)))
        library.times = mapOf(remote.id to 42)
        val sut = createSut()
        runCurrent()
        sut.state.value.games.single().hasUpdate shouldBe true
        sut.state.value.games.single().playtimeMinutes shouldBe 42
        sut.state.value.games.single().canPlay shouldBe false
        sut.update(remote.name)
        navigation.state.value.section shouldBe LauncherSection.LIBRARY
        navigation.state.value.libraryAction shouldBe LibraryNavigationAction.UPDATE
    }

    @Test
    fun `matching version has no update and missing playtime starts at zero`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
        )
        val sut = createSut()
        runCurrent()
        sut.state.value.games.single().hasUpdate shouldBe false
        sut.state.value.games.single().playtimeMinutes shouldBe 0
        sut.navigateToLibrary()
        navigation.state.value.section shouldBe LauncherSection.LIBRARY
    }

    @Test
    fun `launch result from pending seam is visible`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, remote.version, null)))
        val sut = createSut()
        runCurrent()
        sut.play(remote.name)
        runCurrent()
        launch.launches shouldBe 1
        sut.state.value.notice shouldBe GamesNotice.NOT_SUPPORTED_YET
    }

    @Test
    fun `running signal prevents uninstall before confirmation`() = runTest(dispatcher) {
        val id = UUID.randomUUID()
        installation.current.value = InstalledGamesState.Available(listOf(LocalGame(id, "Test Game", "v1", null)))
        coordinator.refreshCatalogue()
        launch.signal = GameRunningSignal.TRACKED_SESSION
        val sut = createSut()
        runCurrent()
        sut.requestUninstall("Test Game")
        runCurrent()
        sut.state.value.notice shouldBe GamesNotice.GAME_RUNNING
        installation.uninstalls shouldBe 0
    }

    @Test
    fun `pending uninstall result is represented explicitly`() = runTest(dispatcher) {
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(UUID.randomUUID(), "Test Game", "v1", null)))
        coordinator.refreshCatalogue()
        launch.signal = GameRunningSignal.NOT_RUNNING
        val sut = createSut()
        runCurrent()
        sut.requestUninstall("Test Game")
        runCurrent()
        sut.confirmUninstall()
        runCurrent()
        installation.uninstalls shouldBe 1
        sut.state.value.notice shouldBe GamesNotice.NOT_SUPPORTED_YET
    }

    @Test
    fun `special version can play despite newer catalogue version`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", "special")))
        val sut = createSut()
        runCurrent()
        sut.state.value.games.single().canPlay shouldBe true
        launch.launchResult = GameLaunchResult.Launched
        sut.play(remote.name)
        runCurrent()
        sut.state.value.notice shouldBe null
    }

    @Test
    fun `conflicting catalogue ID does not decorate installed game by name`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(UUID.randomUUID(), remote.name, "v1.0", null)),
        )
        val sut = createSut()
        runCurrent()
        sut.state.value.games.single().remote shouldBe null
        sut.state.value.games.single().hasUpdate shouldBe false
    }

    @Test
    fun `active update disables play switch and folder until transfer stops`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, "v1.0", "special")),
        )
        locations.help = GameHelpAvailability.AVAILABLE
        val sut = createSut()
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        runCurrent()
        val row = sut.state.value.games.single()
        row.isUpdating shouldBe true
        row.canPlay shouldBe false
        row.canSwitchSpecialVersion shouldBe false
        row.canOpenGameLocation shouldBe false
        row.canOpenHelpLocation shouldBe true
        row.canUninstall shouldBe true
        sut.play(remote.name)
        sut.switchToSpecialVersion(remote.name)
        sut.openGameLocation(remote.name)
        sut.openHelpLocation(remote.name)
        runCurrent()
        launch.launches shouldBe 0
        locations.opens shouldBe 1
        navigation.state.value.section shouldBe LauncherSection.HOME
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.PAUSED))
        runCurrent()
        sut.state.value.games.single().isUpdating shouldBe false
    }

    @Test
    fun `missing game folder offers matching catalogue download`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
        )
        locations.openResult = OpenGameLocationResult.NotFound
        val sut = createSut()
        runCurrent()
        sut.openGameLocation(remote.name)
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe remote.gameId
        sut.acceptMissingLocationDownload()
        navigation.state.value.libraryAction shouldBe LibraryNavigationAction.DOWNLOAD
        navigation.state.value.pendingLibraryGameId shouldBe remote.gameId
    }

    @Test
    fun `uninstalling flag blocks repeated uninstall while operation is pending`() = runTest(dispatcher) {
        val remote = testGame()
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
        )
        launch.signal = GameRunningSignal.NOT_RUNNING
        val barrier = CompletableDeferred<Unit>()
        installation.uninstallBarrier = barrier
        val sut = createSut()
        runCurrent()
        sut.requestUninstall(remote.name)
        runCurrent()
        sut.confirmUninstall()
        runCurrent()
        sut.state.value.games.single().isUninstalling shouldBe true
        sut.state.value.games.single().canUninstall shouldBe false
        sut.requestUninstall(remote.name)
        runCurrent()
        installation.uninstalls shouldBe 1
        barrier.complete(Unit)
        runCurrent()
        sut.state.value.games.single().isUninstalling shouldBe false
    }

    @Test
    fun `empty variant does not bypass an available update`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, "v1.0", "")),
        )
        val sut = createSut()
        runCurrent()
        sut.state.value.games.single().canPlay shouldBe false
    }

    private fun createSut() =
        GamesViewModel(installation, launch, locations, library, coordinator, navigation, downloads)
}
