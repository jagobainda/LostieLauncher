package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import dev.jagoba.lostielauncher.model.DownloadSnapshot
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.SpecialVersionConfig
import dev.jagoba.lostielauncher.service.CatalogueResult
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionLookup
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.LibraryNavigationAction
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = TestContentService()
    private val downloads = TestDownloads()
    private val installation = TestInstallation()
    private val special = TestSpecialVersionService()
    private val navigation = NavigationStore()
    private val settings = TestSettingsStore()
    private val externalLinks = TestExternalLinkService()
    private val network = TestNetworkMonitor()
    private val storageAccess = TestStorageAccess()
    private val notificationAccess = TestNotificationAccess()
    private val logger = mockk<Logger>(relaxed = true)
    private val coordinator = LauncherDataCoordinator(
        content,
        downloads,
        { false },
        settings,
        network,
        HomeRefreshOptions(2.minutes),
        mockk<Logger>(relaxed = true),
    )

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
    fun `missing storage access is explained before the download starts`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.confirmDownload()
        runCurrent()
        sut.state.value.isStorageAccessPromptVisible shouldBe true
        sut.state.value.pendingDownloadGameId shouldBe null
        downloads.starts.size shouldBe 0
        sut.requestStorageAccess()
        runCurrent()
        sut.state.value.isStorageAccessPromptVisible shouldBe false
        storageAccess.granted = true
        sut.onStorageAccessResult()
        runCurrent()
        downloads.starts.single().args.gameId shouldBe remote.gameId
        sut.state.value.notice shouldBe null
    }

    @Test
    fun `denied storage access starts nothing and says why`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.confirmDownload()
        sut.requestStorageAccess()
        sut.onStorageAccessResult()
        runCurrent()
        downloads.starts.size shouldBe 0
        sut.state.value.notice shouldBe LibraryNotice.STORAGE_ACCESS_DENIED
        sut.onStorageAccessResult()
        runCurrent()
        downloads.starts.size shouldBe 0
    }

    @Test
    fun `declining the storage explanation drops the pending download`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.confirmDownload()
        sut.dismissPrompt()
        runCurrent()
        sut.state.value.isStorageAccessPromptVisible shouldBe false
        storageAccess.granted = true
        sut.onStorageAccessResult()
        runCurrent()
        downloads.starts.size shouldBe 0
        sut.state.value.notice shouldBe null
    }

    @Test
    fun `an update asks for storage access too`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)))
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.startUpdate(remote.gameId)
        runCurrent()
        sut.state.value.isStorageAccessPromptVisible shouldBe true
        downloads.starts.size shouldBe 0
        storageAccess.granted = true
        sut.onStorageAccessResult()
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
    fun `unsupported installation shows installation seam outcome`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLATION_FAILED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().installation shouldBe
            GameInstallationState.Finished(GameInstallationResult.NotSupportedYet)
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
    fun `maintenance keeps action enabled and explains every blocked attempt`() = runTest(dispatcher) {
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
        sut.state.value.notice shouldBe LibraryNotice.SERVER_ACTIONS_UNAVAILABLE

        sut.clearNotice()
        sut.requestSpecialVersion(remote.gameId)
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.SERVER_ACTIONS_UNAVAILABLE

        content.blocked = false
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe remote.gameId
    }

    @Test
    fun `missing connection explains offline mode on every blocked action`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        network.isOnline.value = false
        content.blocked = true

        sut.requestDownload(remote.gameId)
        runCurrent()
        downloads.starts.size shouldBe 0
        sut.state.value.notice shouldBe LibraryNotice.OFFLINE

        sut.clearNotice()
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.OFFLINE

        sut.clearNotice()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.PAUSED))
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        downloads.resumed shouldBe emptyList()
        sut.state.value.notice shouldBe LibraryNotice.OFFLINE

        sut.clearNotice()
        sut.requestSpecialVersion(remote.gameId)
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.OFFLINE
    }

    @Test
    fun `stale catalogue preserves loaded cards and switches warning between offline and out of date`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            val sut = createSut(backgroundScope)
            runCurrent()
            sut.state.value.isContentStale shouldBe false
            sut.state.value.isOutOfDateWarningVisible shouldBe false

            content.catalogueResult = CatalogueResult.Failed
            sut.refresh()
            runCurrent()
            sut.state.value.games.map { it.gameId } shouldBe listOf(remote.gameId)
            sut.state.value.isContentStale shouldBe true
            sut.state.value.isOffline shouldBe false
            sut.state.value.isOutOfDateWarningVisible shouldBe true
            content.gameForces.last() shouldBe true

            content.blocked = true
            network.isOnline.value = false
            runCurrent()
            sut.state.value.isOffline shouldBe true
            sut.state.value.isOutOfDateWarningVisible shouldBe false
            sut.state.value.games.map { it.gameId } shouldBe listOf(remote.gameId)

            content.blocked = false
            content.catalogueResult = null
            network.isOnline.value = true
            runCurrent()
            sut.state.value.isOffline shouldBe false
            sut.state.value.isContentStale shouldBe false
            sut.state.value.isOutOfDateWarningVisible shouldBe false
        }

    @Test
    fun `retrying the library after maintenance ends clears the offline banner immediately`() = runTest(dispatcher) {
        val remote = testGame()
        content.blocked = true
        content.catalogueResult = CatalogueResult.Blocked
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.isEmpty shouldBe true
        sut.state.value.isOffline shouldBe true

        content.blocked = false
        content.catalogueResult = null
        content.games = listOf(remote)
        sut.refresh()
        runCurrent()

        sut.state.value.games.map { it.gameId } shouldBe listOf(remote.gameId)
        sut.state.value.isContentStale shouldBe false
        sut.state.value.isOffline shouldBe false
        sut.state.value.isOutOfDateWarningVisible shouldBe false
    }

    @Test
    fun `active or unfinished download missing from the catalogue keeps a manageable card`() = runTest(dispatcher) {
        content.catalogueResult = CatalogueResult.Failed
        val active = DownloadSnapshot(
            gameId = "orphan-game",
            displayName = "Orphan Game",
            version = "v1.0",
            status = DownloadStatus.DOWNLOADING,
            percent = 40.0,
            bytesPerSecond = 100.0,
            downloadedBytes = 400,
            totalBytes = 1000,
            errorMessage = null,
        )
        val finished = DownloadSnapshot(
            gameId = "done-game",
            displayName = "Done Game",
            version = "v1.0",
            status = DownloadStatus.COMPLETED,
            percent = 100.0,
            bytesPerSecond = 0.0,
            downloadedBytes = 1000,
            totalBytes = 1000,
            errorMessage = null,
        )
        downloads.current.value = listOf(active, finished)
        val sut = createSut(backgroundScope)
        runCurrent()

        sut.state.value.isEmpty shouldBe false
        sut.state.value.isListVisible shouldBe true
        val row = sut.state.value.games.single()
        row.gameId shouldBe "orphan-game"
        row.game.name shouldBe "Orphan Game"
        row.status shouldBe LibraryCardStatus.DOWNLOADING
        row.canPause shouldBe true
        row.canCancel shouldBe true
        row.canSwitchSpecialVersion shouldBe false

        sut.pause("orphan-game")
        runCurrent()
        downloads.paused shouldBe listOf("orphan-game")

        sut.requestCancel("orphan-game")
        runCurrent()
        sut.state.value.pendingCancelGameId shouldBe "orphan-game"
        sut.confirmCancel()
        runCurrent()
        downloads.cancelled shouldBe listOf("orphan-game")
    }

    @Test
    fun `orphan download without total bytes omits size and supports resume and retry`() = runTest(dispatcher) {
        content.catalogueResult = CatalogueResult.Failed
        val paused = DownloadSnapshot(
            gameId = "paused-orphan",
            displayName = "Paused Orphan",
            version = "v1.0",
            status = DownloadStatus.PAUSED,
            percent = 20.0,
            bytesPerSecond = 0.0,
            downloadedBytes = 200,
            totalBytes = null,
            errorMessage = null,
        )
        downloads.current.value = listOf(paused)
        val sut = createSut(backgroundScope)
        runCurrent()

        val pausedRow = sut.state.value.games.single()
        pausedRow.formattedSize shouldBe ""
        pausedRow.canStart shouldBe true
        sut.requestDownload("paused-orphan")
        runCurrent()
        downloads.resumed shouldBe listOf("paused-orphan")

        downloads.current.value = listOf(paused.copy(status = DownloadStatus.INSTALLATION_FAILED))
        installation.installation = flowOf(GameInstallationState.NotStarted)
        runCurrent()
        sut.requestDownload("paused-orphan")
        runCurrent()
        downloads.retried shouldBe listOf("paused-orphan")
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
        downloads.starts.single().expectedSha256 shouldBe "a".repeat(64)
        downloads.starts.single().variant shouldBe "special"
    }

    @Test
    fun `special version logs never contain the entered key`() = runTest(dispatcher) {
        val remote = testGame()
        val key = "ABCD-1234-EFGH-5678-IJKL"
        val config = SpecialVersionConfig("a".repeat(64), "special", remote.id!!, "v1.5", "special.zip")
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        val results = listOf(
            SpecialVersionLookup.Found(config),
            SpecialVersionLookup.Found(config.copy(sha256 = "bad")),
            SpecialVersionLookup.Found(config.copy(mainGameId = testGame(name = "Other").id!!)),
            SpecialVersionLookup.NotFound,
            SpecialVersionLookup.NetworkError,
            SpecialVersionLookup.InvalidResponse,
        )

        results.forEach { result ->
            special.result = result
            sut.requestSpecialVersion(remote.gameId)
            runCurrent()
            sut.submitSpecialVersionKey(key)
            runCurrent()
        }

        verify(exactly = 0) {
            logger.debug(match { key in it })
            logger.info(match { key in it })
            logger.error(match { key in it }, any())
        }
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
    fun `invalid special key keeps the dialog open with an error before network lookup`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestSpecialVersion(remote.gameId)
        runCurrent()
        sut.submitSpecialVersionKey("bad")
        runCurrent()
        sut.state.value.pendingSpecialGameId shouldBe remote.gameId
        sut.state.value.isKeyInvalid shouldBe true
        sut.state.value.notice shouldBe null
        special.lookups shouldBe 0

        sut.updatePendingKey("bad2")
        runCurrent()
        sut.state.value.isKeyInvalid shouldBe false
        sut.state.value.pendingKey shouldBe "bad2"
        sut.state.value.pendingSpecialGameId shouldBe remote.gameId
    }

    @Test
    fun `special version key trims surrounding spaces in the view model`() = runTest(dispatcher) {
        val remote = testGame()
        val key = "ABCD-1234-EFGH-5678-IJKL"
        content.games = listOf(remote)
        special.result = SpecialVersionLookup.Found(
            SpecialVersionConfig("a".repeat(64), "special", remote.id!!, "v1.5", "special.zip"),
        )
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestSpecialVersion(remote.gameId)
        runCurrent()
        sut.submitSpecialVersionKey("  $key  ")
        runCurrent()

        sut.state.value.pendingSpecialGameId shouldBe null
        sut.state.value.isKeyInvalid shouldBe false
        special.keys shouldBe listOf(key)
        downloads.starts.single().args.key shouldBe key
    }

    @Test
    fun `invalid download key keeps the confirm dialog open without requesting storage access`() = runTest(dispatcher) {
        val remote = testGame()
        val key = "ABCD-1234-EFGH-5678-IJKL"
        content.games = listOf(remote)
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()

        sut.confirmDownload("abc123") shouldBe false
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe remote.gameId
        sut.state.value.isKeyInvalid shouldBe true
        sut.state.value.isStorageAccessPromptVisible shouldBe false
        sut.state.value.notice shouldBe null
        downloads.starts shouldBe emptyList()

        sut.updatePendingKey(" $key ")
        runCurrent()
        sut.state.value.isKeyInvalid shouldBe false
        sut.state.value.pendingKey shouldBe " $key "

        sut.confirmDownload() shouldBe true
        runCurrent()
        sut.state.value.pendingDownloadGameId shouldBe null
        sut.state.value.pendingKey shouldBe ""
        sut.state.value.isStorageAccessPromptVisible shouldBe true
    }

    @Test
    fun `inline key error survives process death and clears when the dialog is dismissed`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val saved = SavedStateHandle()
        val original = createSut(backgroundScope, saved)
        runCurrent()
        original.requestDownload(remote.gameId)
        runCurrent()
        original.confirmDownload("bad") shouldBe false
        runCurrent()

        val restored = createSut(backgroundScope, saved)
        runCurrent()
        restored.state.value.pendingDownloadGameId shouldBe remote.gameId
        restored.state.value.isKeyInvalid shouldBe true

        restored.dismissPrompt()
        runCurrent()
        restored.state.value.pendingDownloadGameId shouldBe null
        restored.state.value.isKeyInvalid shouldBe false
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
        navigation.state.value.pendingLibraryAction shouldBe null
    }

    @Test
    fun `special version navigation still opens the dialog after the scroll target is consumed`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            navigation.navigate(LauncherSection.LIBRARY, remote.gameId, LibraryNavigationAction.SPECIAL_VERSION)
            navigation.consumeLibraryGame(remote.gameId)

            val sut = createSut(backgroundScope)
            runCurrent()

            sut.state.value.pendingSpecialGameId shouldBe remote.gameId
            navigation.state.value.pendingLibraryAction shouldBe null
        }

    @Test
    fun `special version navigation waits while the catalogue reloads`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val sut = createSut(backgroundScope)
        runCurrent()

        val barrier = CompletableDeferred<Unit>()
        content.gameBarrier = barrier
        sut.refresh()
        runCurrent()
        navigation.navigate(LauncherSection.LIBRARY, remote.gameId, LibraryNavigationAction.SPECIAL_VERSION)
        runCurrent()

        sut.state.value.pendingSpecialGameId shouldBe null
        sut.state.value.notice shouldBe null

        barrier.complete(Unit)
        runCurrent()

        sut.state.value.pendingSpecialGameId shouldBe remote.gameId
        navigation.state.value.pendingLibraryAction shouldBe null
    }

    @Test
    fun `special version navigation explains when another download is in progress`() = runTest(dispatcher) {
        val active = testGame(name = "Downloading")
        val target = testGame(name = "Installed")
        content.games = listOf(active, target)
        downloads.current.value = listOf(testDownload(active, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()

        navigation.navigate(LauncherSection.LIBRARY, target.gameId, LibraryNavigationAction.SPECIAL_VERSION)
        runCurrent()

        sut.state.value.pendingSpecialGameId shouldBe null
        sut.state.value.notice shouldBe LibraryNotice.SPECIAL_VERSION_BUSY
        navigation.state.value.pendingLibraryAction shouldBe null
    }

    @Test
    fun `reinstall navigation opens download confirmation even when the game is already downloaded`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            installation.current.value = InstalledGamesState.Available(
                listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
            )
            val sut = createSut(backgroundScope)
            runCurrent()
            sut.state.value.games.single().status shouldBe LibraryCardStatus.DOWNLOADED

            navigation.navigate(LauncherSection.LIBRARY, remote.gameId, LibraryNavigationAction.REINSTALL)
            runCurrent()

            sut.state.value.pendingDownloadGameId shouldBe remote.gameId
            navigation.state.value.pendingLibraryAction shouldBe null
        }

    @Test
    fun `reinstall navigation reports busy when another download is in progress`() = runTest(dispatcher) {
        val active = testGame(name = "Downloading")
        val target = testGame(name = "Installed")
        content.games = listOf(active, target)
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(target.id!!, target.name, target.version, null)),
        )
        downloads.current.value = listOf(testDownload(active, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()

        navigation.navigate(LauncherSection.LIBRARY, target.gameId, LibraryNavigationAction.REINSTALL)
        runCurrent()

        sut.state.value.pendingDownloadGameId shouldBe null
        sut.state.value.notice shouldBe LibraryNotice.DOWNLOAD_BUSY
        navigation.state.value.pendingLibraryAction shouldBe null
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
    fun `extraction progress updates the installing download card without enabling another download`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            val phase = MutableStateFlow<GameInstallationState>(GameInstallationState.Extracting(0))
            installation.installation = phase
            downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLING))
            val sut = createSut(backgroundScope)
            runCurrent()

            sut.state.value.games.single().status shouldBe LibraryCardStatus.EXTRACTING
            phase.value = GameInstallationState.Extracting(42)
            runCurrent()

            val row = sut.state.value.games.single()
            row.status shouldBe LibraryCardStatus.EXTRACTING
            row.installation shouldBe GameInstallationState.Extracting(42)
            row.canStart shouldBe false
            row.canPause shouldBe false
            sut.state.value.notice shouldBe null
        }

    @Test
    fun `an installation that turns into a failure raises its notice`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val phase = MutableStateFlow<GameInstallationState>(GameInstallationState.VerifyingIntegrity)
        installation.installation = phase
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLING))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.notice shouldBe null
        phase.value = GameInstallationState.Finished(GameInstallationResult.HashMismatch)
        runCurrent()
        sut.state.value.notice shouldBe LibraryNotice.INSTALLATION_HASH_MISMATCH
    }

    @ParameterizedTest
    @MethodSource("installationFailures")
    fun `a transfer that completes into a failed installation raises a notice with its cause`(
        result: GameInstallationResult,
        notice: LibraryNotice,
    ) = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val phase = MutableStateFlow<GameInstallationState>(GameInstallationState.NotStarted)
        installation.installation = phase
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLING))
        runCurrent()
        phase.value = GameInstallationState.Finished(result)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLATION_FAILED))
        runCurrent()
        sut.state.value.notice shouldBe notice
        sut.state.value.games.single().status shouldBe LibraryCardStatus.INSTALLATION_FAILED
    }

    @Test
    fun `a failure left by an earlier installation is not raised again by a new one`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        installation.installation = flowOf(GameInstallationState.Finished(GameInstallationResult.ExtractionFailed))
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLING))
        runCurrent()
        sut.state.value.notice shouldBe null
        sut.state.value.games.single().status shouldBe LibraryCardStatus.INSTALLATION_PENDING
    }

    @Test
    fun `an installation that had already failed before the screen existed raises nothing`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        installation.installation = flowOf(GameInstallationState.Finished(GameInstallationResult.InvalidHash))
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLATION_FAILED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.notice shouldBe null
        sut.state.value.games.single().status shouldBe LibraryCardStatus.INSTALLATION_FAILED
    }

    @Test
    fun `an installation interrupted by a restart waits to resume and can be cancelled`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        installation.installation = flowOf(GameInstallationState.NotStarted)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLING))
        val sut = createSut(backgroundScope)
        runCurrent()

        val row = sut.state.value.games.single()
        row.status shouldBe LibraryCardStatus.INSTALLATION_PENDING
        row.canStart shouldBe false
        row.canPause shouldBe false
        row.canCancel shouldBe true
    }

    @Test
    fun `a failed installation found after a restart is retried once storage access is granted`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            installation.installation = flowOf(GameInstallationState.NotStarted)
            downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLATION_FAILED))
            storageAccess.granted = false
            val sut = createSut(backgroundScope)
            runCurrent()
            val row = sut.state.value.games.single()
            row.status shouldBe LibraryCardStatus.INSTALLATION_FAILED
            row.canStart shouldBe true
            row.canCancel shouldBe true

            sut.requestDownload(remote.gameId)
            runCurrent()
            sut.state.value.isStorageAccessPromptVisible shouldBe true
            downloads.retried shouldBe emptyList()
            storageAccess.granted = true
            sut.onStorageAccessResult()
            runCurrent()

            downloads.retried shouldBe listOf(remote.gameId)
            downloads.starts shouldBe emptyList()
            sut.state.value.pendingDownloadGameId shouldBe null
        }

    @Test
    fun `a completed transfer leaves the card to the installed games`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.COMPLETED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.state.value.games.single().status shouldBe LibraryCardStatus.AVAILABLE
        sut.state.value.games.single().canStart shouldBe true

        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, remote.version, null)),
        )
        runCurrent()
        sut.state.value.games.single().status shouldBe LibraryCardStatus.DOWNLOADED
    }

    @ParameterizedTest
    @EnumSource(DownloadStatus::class)
    fun `cancel prompt follows the download phase`(status: DownloadStatus) = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestCancel(remote.gameId)
        runCurrent()
        sut.state.value.pendingCancelGameId shouldBe remote.gameId

        downloads.current.value = listOf(testDownload(remote, status))
        runCurrent()

        val canCancel = !status.isFinished
        sut.state.value.pendingCancelGameId shouldBe remote.gameId.takeIf { canCancel }
        sut.confirmCancel()
        runCurrent()
        downloads.cancelled shouldBe if (canCancel) listOf(remote.gameId) else emptyList()
        if (status == DownloadStatus.COMPLETED) sut.state.value.notice shouldBe null
    }

    @Test
    fun `removed download dismisses cancellation and does not reopen on a new transfer`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.PAUSED))
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestCancel(remote.gameId)
        runCurrent()

        downloads.current.value = emptyList()
        runCurrent()
        sut.state.value.pendingCancelGameId shouldBe null
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.DOWNLOADING))
        runCurrent()
        sut.confirmCancel()
        runCurrent()

        sut.state.value.pendingCancelGameId shouldBe null
        downloads.cancelled shouldBe emptyList()
    }

    @Test
    fun `download confirmation prompt and destination survive process death`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val saved = SavedStateHandle()
        val original = createSut(backgroundScope, saved)
        runCurrent()
        original.requestDownload(remote.gameId)
        runCurrent()

        val restored = createSut(backgroundScope, saved)
        runCurrent()

        restored.state.value.pendingDownloadGame shouldBe remote
        restored.state.value.downloadDestination shouldBe downloads.destination
        restored.confirmDownload()
        runCurrent()
        downloads.starts.single().args.gameId shouldBe remote.gameId
    }

    @Test
    fun `special version and cancel prompts survive process death`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        val savedSpecial = SavedStateHandle()
        val original = createSut(backgroundScope, savedSpecial)
        runCurrent()
        original.requestSpecialVersion(remote.gameId)
        runCurrent()

        val restoredSpecial = createSut(backgroundScope, savedSpecial)
        runCurrent()
        restoredSpecial.state.value.pendingSpecialGameId shouldBe remote.gameId

        val savedCancel = SavedStateHandle()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.PAUSED))
        val cancelOriginal = createSut(backgroundScope, savedCancel)
        runCurrent()
        cancelOriginal.requestCancel(remote.gameId)
        runCurrent()

        val restoredCancel = createSut(backgroundScope, savedCancel)
        runCurrent()
        restoredCancel.state.value.pendingCancelGameId shouldBe remote.gameId
        restoredCancel.confirmCancel()
        runCurrent()
        downloads.cancelled shouldBe listOf(remote.gameId)
    }

    @Test
    fun `granting storage access after process death starts the confirmed download even while catalogue reloads`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            storageAccess.granted = false
            val saved = SavedStateHandle()
            val original = createSut(backgroundScope, saved)
            runCurrent()
            original.requestDownload(remote.gameId)
            runCurrent()
            original.confirmDownload()
            runCurrent()
            original.state.value.isStorageAccessPromptVisible shouldBe true

            val onRationale = createSut(backgroundScope, saved)
            runCurrent()
            onRationale.state.value.isStorageAccessPromptVisible shouldBe true
            onRationale.requestStorageAccess()
            runCurrent()
            onRationale.state.value.isStorageAccessPromptVisible shouldBe false

            val barrier = CompletableDeferred<Unit>()
            content.gameBarrier = barrier
            val freshCoordinator = LauncherDataCoordinator(
                content,
                downloads,
                { false },
                settings,
                network,
                HomeRefreshOptions(2.minutes),
                mockk<Logger>(relaxed = true),
            )
            val restored = createSut(backgroundScope, saved, freshCoordinator)
            storageAccess.granted = true
            restored.onStorageAccessResult()
            runCurrent()
            downloads.starts shouldBe emptyList()

            barrier.complete(Unit)
            runCurrent()

            downloads.starts.single().args.gameId shouldBe remote.gameId
            restored.state.value.notice shouldBe null
        }

    @Test
    fun `pending key stays in memory without writing to saved state and clears on process death`() =
        runTest(dispatcher) {
            val remote = testGame()
            val key = "ABCD-1234-EFGH-5678-IJKL"
            content.games = listOf(remote)
            val saved = SavedStateHandle()
            val original = createSut(backgroundScope, saved)
            runCurrent()
            original.requestDownload(remote.gameId)
            runCurrent()
            original.updatePendingKey(key)
            runCurrent()

            original.state.value.pendingKey shouldBe key
            saved.keys().none { saved.get<Any?>(it) == key } shouldBe true

            val restored = createSut(backgroundScope, saved)
            runCurrent()
            restored.state.value.pendingDownloadGameId shouldBe remote.gameId
            restored.state.value.pendingKey shouldBe ""
        }

    @Test
    fun `special key download continues after permission grant in memory but is dropped across process death`() =
        runTest(dispatcher) {
            val remote = testGame()
            val key = "ABCD-1234-EFGH-5678-IJKL"
            content.games = listOf(remote)
            special.result = SpecialVersionLookup.Found(
                SpecialVersionConfig("a".repeat(64), "special", remote.id!!, "v1.5", "special.zip"),
            )
            storageAccess.granted = false
            val saved = SavedStateHandle()
            val original = createSut(backgroundScope, saved)
            runCurrent()
            original.requestDownload(remote.gameId)
            runCurrent()
            original.confirmDownload(" $key ")
            runCurrent()

            original.state.value.isStorageAccessPromptVisible shouldBe true
            saved.keys().none { saved.get<Any?>(it) == key } shouldBe true
            saved.get<String>("library.storageAction.kind") shouldBe null
            saved.get<String>("library.storageAction.gameId") shouldBe null
            saved.get<String>("library.permissionPrompt") shouldBe null

            val afterKill = createSut(backgroundScope, saved)
            runCurrent()
            afterKill.state.value.permissionPrompt shouldBe null
            storageAccess.granted = true
            afterKill.onStorageAccessResult()
            runCurrent()
            downloads.starts shouldBe emptyList()

            original.requestStorageAccess()
            original.onStorageAccessResult()
            runCurrent()
            downloads.starts.single().args.key shouldBe key
            downloads.starts.single().args.version shouldBe "v1.5"
        }

    @Test
    fun `update and installation retry survive storage access grant across process death`() = runTest(dispatcher) {
        val remote = testGame(version = "v2.0")
        content.games = listOf(remote)
        installation.current.value =
            InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)))
        storageAccess.granted = false
        val updateSaved = SavedStateHandle()
        val originalUpdate = createSut(backgroundScope, updateSaved)
        runCurrent()
        originalUpdate.startUpdate(remote.gameId)
        runCurrent()
        originalUpdate.requestStorageAccess()

        val restoredUpdate = createSut(backgroundScope, updateSaved)
        runCurrent()
        storageAccess.granted = true
        restoredUpdate.onStorageAccessResult()
        runCurrent()
        downloads.starts.single().args.gameId shouldBe remote.gameId

        storageAccess.granted = false
        installation.installation = flowOf(GameInstallationState.NotStarted)
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.INSTALLATION_FAILED))
        val retrySaved = SavedStateHandle()
        val originalRetry = createSut(backgroundScope, retrySaved)
        runCurrent()
        originalRetry.requestDownload(remote.gameId)
        runCurrent()
        originalRetry.requestStorageAccess()

        val restoredRetry = createSut(backgroundScope, retrySaved)
        runCurrent()
        storageAccess.granted = true
        restoredRetry.onStorageAccessResult()
        runCurrent()
        downloads.retried shouldBe listOf(remote.gameId)
    }

    @Test
    fun `denied storage access after process death shows notice and clears the saved continuation`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            storageAccess.granted = false
            val saved = SavedStateHandle()
            val original = createSut(backgroundScope, saved)
            runCurrent()
            original.requestDownload(remote.gameId)
            runCurrent()
            original.confirmDownload()
            original.requestStorageAccess()

            val restored = createSut(backgroundScope, saved)
            runCurrent()
            restored.onStorageAccessResult()
            runCurrent()

            downloads.starts shouldBe emptyList()
            restored.state.value.notice shouldBe LibraryNotice.STORAGE_ACCESS_DENIED
            storageAccess.granted = true
            restored.onStorageAccessResult()
            runCurrent()
            downloads.starts shouldBe emptyList()
        }

    @Test
    fun `permanently denied storage access offers app settings and continues once granted`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.confirmDownload()
        sut.requestStorageAccess()

        sut.onStorageAccessResult(permanentlyDenied = true)
        runCurrent()

        sut.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.STORAGE_SETTINGS
        sut.state.value.notice shouldBe null
        downloads.starts shouldBe emptyList()

        sut.requestStorageAccess()
        storageAccess.granted = true
        sut.onStorageAccessResult()
        runCurrent()

        sut.state.value.permissionPrompt shouldBe null
        sut.state.value.notice shouldBe null
        downloads.starts.single().args.gameId shouldBe remote.gameId
    }

    @Test
    fun `returning from storage settings without granting shows denied notice without looping`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.confirmDownload()
        sut.requestStorageAccess()
        sut.onStorageAccessResult(permanentlyDenied = true)
        runCurrent()

        sut.requestStorageAccess()
        sut.onStorageAccessResult(permanentlyDenied = false)
        runCurrent()

        sut.state.value.permissionPrompt shouldBe null
        sut.state.value.notice shouldBe LibraryNotice.STORAGE_ACCESS_DENIED
        downloads.starts shouldBe emptyList()
    }

    @Test
    fun `declining storage settings prompt drops the pending download`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        storageAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()
        sut.requestDownload(remote.gameId)
        runCurrent()
        sut.confirmDownload()
        sut.requestStorageAccess()
        sut.onStorageAccessResult(permanentlyDenied = true)
        runCurrent()

        sut.dismissPrompt()
        storageAccess.granted = true
        sut.onStorageAccessResult()
        runCurrent()

        sut.state.value.permissionPrompt shouldBe null
        sut.state.value.notice shouldBe null
        downloads.starts shouldBe emptyList()
    }

    @Test
    fun `missing notification permission explains itself before starting and starts once granted`() =
        runTest(dispatcher) {
            val remote = testGame()
            content.games = listOf(remote)
            notificationAccess.granted = false
            val sut = createSut(backgroundScope)
            runCurrent()
            sut.requestDownload(remote.gameId)
            runCurrent()

            sut.confirmDownload()
            runCurrent()

            sut.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.NOTIFICATION_RATIONALE
            downloads.starts shouldBe emptyList()

            sut.requestNotificationPermission()
            notificationAccess.granted = true
            sut.onNotificationPermissionResult()
            runCurrent()

            sut.state.value.permissionPrompt shouldBe null
            sut.state.value.notice shouldBe null
            downloads.starts.single().args.gameId shouldBe remote.gameId
        }

    @Test
    fun `skipping or temporarily denying notifications still starts the download`() = runTest(dispatcher) {
        val first = testGame(name = "First")
        val second = testGame(name = "Second")
        content.games = listOf(first, second)
        notificationAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()

        sut.requestDownload(first.gameId)
        runCurrent()
        sut.confirmDownload()
        runCurrent()
        sut.skipNotificationPermission()
        runCurrent()

        sut.state.value.permissionPrompt shouldBe null
        sut.state.value.notice shouldBe null
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId)

        sut.requestDownload(second.gameId)
        runCurrent()
        sut.confirmDownload()
        runCurrent()
        sut.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.NOTIFICATION_RATIONALE
        sut.requestNotificationPermission()
        sut.onNotificationPermissionResult(permanentlyDenied = false)
        runCurrent()

        sut.state.value.permissionPrompt shouldBe null
        sut.state.value.notice shouldBe null
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId, second.gameId)
    }

    @Test
    fun `permanently denied notifications offer settings and start the download either way`() = runTest(dispatcher) {
        val first = testGame(name = "First")
        val second = testGame(name = "Second")
        content.games = listOf(first, second)
        notificationAccess.granted = false
        val sut = createSut(backgroundScope)
        runCurrent()

        sut.requestDownload(first.gameId)
        runCurrent()
        sut.confirmDownload()
        sut.requestNotificationPermission()
        sut.onNotificationPermissionResult(permanentlyDenied = true)
        runCurrent()

        sut.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.NOTIFICATION_SETTINGS
        downloads.starts shouldBe emptyList()

        sut.requestNotificationPermission()
        sut.onNotificationPermissionResult(permanentlyDenied = false)
        runCurrent()

        sut.state.value.permissionPrompt shouldBe null
        sut.state.value.notice shouldBe null
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId)

        sut.requestDownload(second.gameId)
        runCurrent()
        sut.confirmDownload()
        sut.requestNotificationPermission()
        sut.onNotificationPermissionResult(permanentlyDenied = true)
        runCurrent()
        sut.skipNotificationPermission()
        runCurrent()

        sut.state.value.permissionPrompt shouldBe null
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId, second.gameId)
    }

    @Test
    fun `storage access is requested before notifications and update follows the same sequence`() =
        runTest(dispatcher) {
            val remote = testGame(version = "v2.0")
            content.games = listOf(remote)
            installation.current.value =
                InstalledGamesState.Available(listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)))
            storageAccess.granted = false
            notificationAccess.granted = false
            val sut = createSut(backgroundScope)
            runCurrent()

            sut.startUpdate(remote.gameId)
            runCurrent()
            sut.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.STORAGE_RATIONALE
            downloads.starts shouldBe emptyList()

            sut.requestStorageAccess()
            storageAccess.granted = true
            sut.onStorageAccessResult()
            runCurrent()
            sut.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.NOTIFICATION_RATIONALE
            downloads.starts shouldBe emptyList()

            sut.skipNotificationPermission()
            runCurrent()
            sut.state.value.permissionPrompt shouldBe null
            downloads.starts.single().args.gameId shouldBe remote.gameId
        }

    @Test
    fun `storage and notification settings prompts survive process death`() = runTest(dispatcher) {
        val remote = testGame()
        content.games = listOf(remote)
        storageAccess.granted = false
        notificationAccess.granted = false
        val saved = SavedStateHandle()
        val original = createSut(backgroundScope, saved)
        runCurrent()
        original.requestDownload(remote.gameId)
        runCurrent()
        original.confirmDownload()
        original.requestStorageAccess()
        original.onStorageAccessResult(permanentlyDenied = true)
        runCurrent()

        val restoredStorage = createSut(backgroundScope, saved)
        runCurrent()
        restoredStorage.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.STORAGE_SETTINGS
        restoredStorage.requestStorageAccess()
        storageAccess.granted = true
        restoredStorage.onStorageAccessResult()
        runCurrent()
        restoredStorage.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.NOTIFICATION_RATIONALE
        restoredStorage.requestNotificationPermission()
        restoredStorage.onNotificationPermissionResult(permanentlyDenied = true)
        runCurrent()

        val restoredNotifications = createSut(backgroundScope, saved)
        runCurrent()
        restoredNotifications.state.value.permissionPrompt shouldBe LibraryPermissionPrompt.NOTIFICATION_SETTINGS
        restoredNotifications.requestNotificationPermission()
        restoredNotifications.onNotificationPermissionResult()
        runCurrent()

        downloads.starts.single().args.gameId shouldBe remote.gameId
    }

    private fun createSut(
        scope: CoroutineScope,
        savedState: SavedStateHandle = SavedStateHandle(),
        coordinator: LauncherDataCoordinator = this.coordinator,
    ): LibraryViewModel {
        coordinator.start(scope)
        return LibraryViewModel(
            coordinator,
            downloads,
            installation,
            content,
            network,
            special,
            navigation,
            externalLinks,
            storageAccess,
            notificationAccess,
            logger,
            savedState,
        )
    }

    private companion object {
        @JvmStatic
        fun installationFailures() = listOf(
            arrayOf(GameInstallationResult.ExtractionFailed, LibraryNotice.INSTALLATION_FAILED),
            arrayOf(GameInstallationResult.StorageAccessDenied, LibraryNotice.STORAGE_ACCESS_DENIED),
            arrayOf(GameInstallationResult.HashMismatch, LibraryNotice.INSTALLATION_HASH_MISMATCH),
        )
    }
}
