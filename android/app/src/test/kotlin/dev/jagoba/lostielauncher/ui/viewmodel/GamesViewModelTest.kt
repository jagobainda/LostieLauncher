package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.GameUninstallResult
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.service.presentation.PendingLibraryAction
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
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
    private val navigation = NavigationStore()
    private val downloads = TestDownloads()
    private val logger = mockk<Logger>(relaxed = true)
    private val coordinator = LauncherDataCoordinator(content, downloads, { false }, mockk<Logger>(relaxed = true))

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
        sut.state.value.isInstalledStateUnsupported shouldBe true
    }

    @Test
    fun `installed game receives remote update`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)))
        val sut = createSut()
        runCurrent()
        sut.state.value.games.single().hasUpdate shouldBe true
        sut.state.value.games.single().canPlay shouldBe false
        sut.update(remote.name)
        navigation.state.value.section shouldBe LauncherSection.LIBRARY
        navigation.state.value.pendingLibraryAction shouldBe
            PendingLibraryAction(remote.gameId, LibraryNavigationAction.UPDATE)
    }

    @Test
    fun `matching version has no update`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
        )
        val sut = createSut()
        runCurrent()
        sut.state.value.games.single().hasUpdate shouldBe false
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
    fun `unknown running state still asks to confirm the uninstall`() = runTest(dispatcher) {
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(UUID.randomUUID(), "Test Game", "v1", null)))
        coordinator.refreshCatalogue()
        val sut = createSut()
        runCurrent()
        sut.requestUninstall("Test Game")
        runCurrent()
        sut.state.value.pendingUninstall?.name shouldBe "Test Game"
        sut.state.value.notice shouldBe null
    }

    @Test
    fun `uninstall purges the finished download so the library card is available again`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, remote.version, null)))
        val sut = createSut()
        runCurrent()
        downloads.purged.clear()
        sut.requestUninstall(remote.name)
        runCurrent()
        sut.confirmUninstall()
        runCurrent()
        downloads.purged shouldBe listOf(setOf(remote.gameId))
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
    fun `active update disables play and switch until transfer stops`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, "v1.0", "special")),
        )
        val sut = createSut()
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        runCurrent()
        val row = sut.state.value.games.single()
        row.isUpdating shouldBe true
        row.canPlay shouldBe false
        row.canSwitchSpecialVersion shouldBe false
        row.canUninstall shouldBe true
        sut.play(remote.name)
        sut.switchToSpecialVersion(remote.name)
        runCurrent()
        launch.launches shouldBe 0
        navigation.state.value.section shouldBe LauncherSection.HOME
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.PAUSED))
        runCurrent()
        sut.state.value.games.single().isUpdating shouldBe false
    }

    @Test
    fun `download progress does not emit a new installed games projection`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        coordinator.refreshCatalogue()
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)),
        )
        val sut = createSut()
        runCurrent()
        verify(exactly = 1) { logger.debug("Installed games loaded: 1 games.") }

        sut.state.test {
            awaitItem().games.single().isUpdating shouldBe false

            downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
            runCurrent()
            awaitItem().games.single().isUpdating shouldBe true
            verify(exactly = 2) { logger.debug("Installed games loaded: 1 games.") }

            downloads.current.value = downloads.current.value.map { it.copy(percent = 75.0) }
            runCurrent()
            expectNoEvents()
            verify(exactly = 2) { logger.debug("Installed games loaded: 1 games.") }
            cancelAndIgnoreRemainingEvents()
        }
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

    @Test
    fun `game running notice names the game its message is about`() = runTest(dispatcher) {
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(UUID.randomUUID(), "Test Game", "v1", null)))
        coordinator.refreshCatalogue()
        launch.signal = GameRunningSignal.TRACKED_SESSION
        val sut = createSut()
        runCurrent()
        sut.requestUninstall("Test Game")
        runCurrent()
        sut.state.value.noticeGameName shouldBe "Test Game"
        sut.dismissPrompt()
        sut.state.value.noticeGameName shouldBe null
    }

    @Test
    fun `confirming a possibly running uninstall closes the warning before the outcome`() = runTest(dispatcher) {
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(UUID.randomUUID(), "Test Game", "v1", null)))
        coordinator.refreshCatalogue()
        launch.signal = GameRunningSignal.POSSIBLY_RUNNING
        installation.uninstallBarrier = CompletableDeferred()
        val sut = createSut()
        runCurrent()
        sut.requestUninstall("Test Game")
        runCurrent()
        sut.state.value.notice shouldBe GamesNotice.POSSIBLY_RUNNING
        sut.confirmUninstall()
        runCurrent()
        sut.state.value.pendingUninstall shouldBe null
        sut.state.value.notice shouldBe null
        installation.uninstallBarrier?.complete(Unit)
        runCurrent()
        sut.state.value.notice shouldBe GamesNotice.NOT_SUPPORTED_YET
        sut.state.value.noticeGameName shouldBe "Test Game"
    }

    @Test
    fun `pending uninstall confirmation survives process death and uninstalls when confirmed`() = runTest(dispatcher) {
        val gameId = UUID.randomUUID()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(gameId, "Test Game", "v1", null)))
        coordinator.refreshCatalogue()
        val saved = SavedStateHandle()
        val original = createSut(saved)
        runCurrent()
        original.requestUninstall("Test Game")
        runCurrent()

        val restored = createSut(saved)
        runCurrent()

        restored.state.value.pendingUninstall?.name shouldBe "Test Game"
        restored.state.value.pendingUninstall?.id shouldBe gameId
        restored.confirmUninstall()
        runCurrent()
        installation.uninstalls shouldBe 1
    }

    @Test
    fun `possibly running warning and blocking location notice survive process death`() = runTest(dispatcher) {
        val gameId = UUID.randomUUID()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(gameId, "Test Game", "v1", null)))
        coordinator.refreshCatalogue()
        launch.signal = GameRunningSignal.POSSIBLY_RUNNING
        val saved = SavedStateHandle()
        val original = createSut(saved)
        runCurrent()
        original.requestUninstall("Test Game")
        runCurrent()

        val afterWarning = createSut(saved)
        runCurrent()
        afterWarning.state.value.pendingUninstall?.name shouldBe "Test Game"
        afterWarning.state.value.notice shouldBe GamesNotice.POSSIBLY_RUNNING

        installation.uninstallResult = GameUninstallResult(
            GameUninstallOutcome.FILES_LEFT_BEHIND,
            GameLocationReference("token-1", "/games/test"),
        )
        afterWarning.confirmUninstall()
        runCurrent()

        val afterOutcome = createSut(saved)
        runCurrent()
        afterOutcome.state.value.notice shouldBe GamesNotice.FILES_LEFT_BEHIND
        afterOutcome.state.value.noticeGameName shouldBe "Test Game"
        afterOutcome.state.value.blockingLocation shouldBe GameLocationReference("token-1", "/games/test")
    }

    @Test
    fun `integrity mismatch offers reinstall, survives process death and navigates to library on confirmation`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            coordinator.refreshCatalogue()
            installation.current.value =
                InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, remote.version, null)))
            launch.launchResult = GameLaunchResult.IntegrityMismatch
            val saved = SavedStateHandle()
            val sut = createSut(saved)
            runCurrent()

            sut.play(remote.name)
            runCurrent()

            sut.state.value.notice shouldBe GamesNotice.INTEGRITY_MISMATCH
            sut.state.value.noticeGameName shouldBe remote.name
            sut.state.value.reinstallGameId shouldBe remote.gameId

            val restored = createSut(saved)
            runCurrent()
            restored.state.value.notice shouldBe GamesNotice.INTEGRITY_MISMATCH
            restored.state.value.noticeGameName shouldBe remote.name
            restored.state.value.reinstallGameId shouldBe remote.gameId

            restored.confirmReinstall()
            runCurrent()
            restored.state.value.notice shouldBe null
            restored.state.value.reinstallGameId shouldBe null
            navigation.state.value.section shouldBe LauncherSection.LIBRARY
            navigation.state.value.pendingLibraryAction shouldBe
                PendingLibraryAction(remote.gameId, LibraryNavigationAction.REINSTALL)
        }

    @Test
    fun `integrity mismatch without a catalogue entry does not set a reinstall target`() = runTest(dispatcher) {
        coordinator.refreshCatalogue()
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(UUID.randomUUID(), "Offline Game", "v1", null)))
        launch.launchResult = GameLaunchResult.IntegrityMismatch
        val sut = createSut()
        runCurrent()

        sut.play("Offline Game")
        runCurrent()

        sut.state.value.notice shouldBe GamesNotice.INTEGRITY_MISMATCH
        sut.state.value.noticeGameName shouldBe "Offline Game"
        sut.state.value.reinstallGameId shouldBe null
    }

    private fun createSut(savedState: SavedStateHandle = SavedStateHandle()) = GamesViewModel(
        installation,
        launch,
        locations,
        coordinator,
        navigation,
        downloads,
        logger,
        savedState,
    )
}
