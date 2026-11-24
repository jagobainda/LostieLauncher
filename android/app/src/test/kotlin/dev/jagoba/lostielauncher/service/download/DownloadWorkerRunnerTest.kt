package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.MissingGameId
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("DownloadWorkerRunner")
class DownloadWorkerRunnerTest {
    private val dao = mockk<DownloadDao>()
    private val transfer = mockk<DownloadTransfer>()
    private val handoff = mockk<DownloadedFileHandoff>()
    private val fileStore = mockk<DownloadFileStore>()
    private val installer = mockk<GameInstallationService>()
    private val runtime = mockk<DownloadWorkerRuntime>()
    private val logger = mockk<Logger>(relaxed = true)

    @BeforeEach
    fun configureDefaults() {
        coEvery { dao.get(GAME_ID) } returns entity()
        coEvery { dao.setWorkerStatus(any(), any(), any(), any()) } returns 1
        coEvery { dao.setProgress(any(), any(), any(), any(), any(), any(), any(), any()) } returns 1
        coEvery { dao.finish(any(), any(), any(), any(), any(), any()) } returns 1
        coEvery { dao.complete(any(), any(), any(), any()) } returns 1
        coEvery { handoff.deliver(any()) } returns GameInstallationResult.Installed(INSTALLED_GAME)
        every { fileStore.deleteArtifacts(any()) } returns 2
        every { runtime.canStartForeground() } returns true
        coEvery { runtime.setForeground(any(), any()) } returns Unit
        coEvery { runtime.notifyProgress(any(), any()) } returns Unit
        every { runtime.clearProgress(any()) } returns Unit
        coEvery { runtime.notifyResult(any(), any()) } returns Unit
        every { runtime.clearResult(any()) } returns Unit
        every { installer.observeInstallation(any()) } returns emptyFlow()
    }

    @Test
    fun `completes a matching transfer and hands off the downloaded file`() = runTest {
        val progress = DownloadProgress(80.0, 10.0, 80, 100)
        coEvery { transfer.download(any(), any()) } coAnswers {
            secondArg<suspend (DownloadProgress) -> Unit>()(progress)
            DownloadTransferResult.Success
        }
        val sut = createSut()

        val result = sut.run(GAME_ID, WORK_ID, runtime)

        result shouldBe DownloadWorkerOutcome.SUCCESS
        coVerify(exactly = 1) {
            dao.setProgress(
                GAME_ID,
                WORK_ID,
                DownloadStatus.DOWNLOADING.name,
                DownloadStatus.DOWNLOADING.name,
                80.0,
                10.0,
                80,
                100,
            )
        }
        coVerify(exactly = 1) {
            runtime.setForeground("Demo", DownloadNotificationPhase.Downloading(DownloadProgress(25.0, 0.0, 25, 100)))
        }
        coVerify(exactly = 1) {
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(progress))
        }
        verify(exactly = 0) { runtime.clearProgress(any()) }
        coVerifyOrder {
            dao.complete(GAME_ID, WORK_ID, DownloadStatus.DOWNLOADING.name, DownloadStatus.INSTALLING.name)
            runtime.clearResult("Demo")
            handoff.deliver(
                match {
                    it.gameId == GAME_ID &&
                        it.path == DESTINATION &&
                        it.expectedSha256 == "a".repeat(64) &&
                        it.variant == null
                },
            )
            dao.finish(GAME_ID, WORK_ID, DownloadStatus.INSTALLING.name, DownloadStatus.COMPLETED.name, null, true)
            runtime.notifyResult("Demo", GameInstallationResult.Installed(INSTALLED_GAME))
        }
    }

    @ParameterizedTest
    @MethodSource("failedInstallations")
    fun `records a failed installation with its cause and notifies it`(result: GameInstallationResult) = runTest {
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Success
        coEvery { handoff.deliver(any()) } returns result
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.FAILURE

        coVerify(exactly = 1) {
            dao.finish(
                GAME_ID,
                WORK_ID,
                DownloadStatus.INSTALLING.name,
                DownloadStatus.INSTALLATION_FAILED.name,
                result.toString(),
                false,
            )
        }
        coVerify(exactly = 1) { runtime.notifyResult("Demo", result) }
        verify(exactly = 0) { fileStore.deleteArtifacts(any()) }
    }

    @Test
    fun `records an unexpected handoff failure as a failed installation`() = runTest {
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Success
        coEvery { handoff.deliver(any()) } throws IOException("catalogue")
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.FAILURE

        coVerify(exactly = 1) {
            dao.finish(
                GAME_ID,
                WORK_ID,
                DownloadStatus.INSTALLING.name,
                DownloadStatus.INSTALLATION_FAILED.name,
                "ExtractionFailed",
            )
        }
        coVerify(exactly = 1) { runtime.notifyResult("Demo", GameInstallationResult.ExtractionFailed) }
    }

    @Test
    fun `installs the downloaded archive again when the worker reruns an interrupted installation`() = runTest {
        coEvery { dao.get(GAME_ID) } returns entity().copy(status = DownloadStatus.INSTALLING.name)
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerify(exactly = 0) { transfer.download(any(), any()) }
        coVerify(exactly = 0) { dao.setWorkerStatus(any(), any(), any(), any()) }
        coVerify(exactly = 1) { handoff.deliver(match { it.path == DESTINATION }) }
        coVerify(exactly = 1) {
            dao.finish(GAME_ID, WORK_ID, DownloadStatus.INSTALLING.name, DownloadStatus.COMPLETED.name, null, true)
        }
    }

    @Test
    fun `propagates a cancellation during the installation and keeps it resumable`() = runTest {
        coEvery { dao.get(GAME_ID) } returns entity().copy(status = DownloadStatus.INSTALLING.name)
        coEvery { handoff.deliver(any()) } throws CancellationException("stopped")
        val sut = createSut()

        val error = runCatching { sut.run(GAME_ID, WORK_ID, runtime) }.exceptionOrNull()

        (error is CancellationException) shouldBe true
        coVerify(exactly = 0) { dao.finish(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { dao.setWorkerStatus(any(), any(), any(), any()) }
        coVerify(exactly = 0) { runtime.notifyResult(any(), any()) }
        verify(exactly = 0) { logger.error("Downloaded file handoff failed for demo.", any()) }
        verify(exactly = 0) { fileStore.deleteArtifacts(any()) }
    }

    @Test
    fun `keeps the notification on the installation phases until the handoff returns`() = runTest {
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Success
        every { installer.observeInstallation(GAME_ID) } returns flowOf(
            GameInstallationState.Extracting(90),
            GameInstallationState.VerifyingIntegrity,
            GameInstallationState.Extracting(40),
            GameInstallationState.Extracting(),
            GameInstallationState.Finished(GameInstallationResult.ExtractionFailed),
        )
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerifyOrder {
            runtime.setForeground("Demo", DownloadNotificationPhase.Downloading(DownloadProgress(25.0, 0.0, 25, 100)))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Verifying)
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Extracting(40))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Extracting(null))
            handoff.deliver(any())
        }
        coVerify(exactly = 1) { runtime.setForeground(any(), any()) }
        coVerify(exactly = 1) { runtime.notifyProgress("Demo", DownloadNotificationPhase.Verifying) }
        coVerify(exactly = 0) { runtime.notifyProgress("Demo", DownloadNotificationPhase.Extracting(90)) }
        verify(exactly = 0) { runtime.clearProgress(any()) }
    }

    @Test
    fun `still hands off the downloaded file when the notification cannot be updated`() = runTest {
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Success
        coEvery {
            runtime.notifyProgress(any(), DownloadNotificationPhase.Verifying)
        } throws IllegalStateException("notify")
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerify(exactly = 1) { handoff.deliver(any()) }
        verify(exactly = 1) { logger.error("Download notification could not be updated for demo.", any()) }
        verify(exactly = 0) { runtime.clearProgress(any()) }
    }

    @Test
    fun `uses a regular progress notification without setForeground while the app is in the background`() = runTest {
        val progress = DownloadProgress(50.0, 10.0, 50, 100)
        every { runtime.canStartForeground() } returns false
        coEvery { transfer.download(any(), any()) } coAnswers {
            secondArg<suspend (DownloadProgress) -> Unit>()(progress)
            DownloadTransferResult.Success
        }
        every { installer.observeInstallation(GAME_ID) } returns flowOf(
            GameInstallationState.NotStarted,
            GameInstallationState.VerifyingIntegrity,
            GameInstallationState.Extracting(40),
        )
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerify(exactly = 0) { runtime.setForeground(any(), any()) }
        coVerifyOrder {
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(DownloadProgress(25.0, 0.0, 25, 100)))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(progress))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Verifying)
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Extracting(40))
            runtime.clearProgress("Demo")
            runtime.notifyResult("Demo", GameInstallationResult.Installed(INSTALLED_GAME))
        }
        coVerify(exactly = 1) { runtime.notifyProgress("Demo", DownloadNotificationPhase.Verifying) }
        verify(exactly = 1) {
            logger.info("Running download without foreground service while app is in background: demo.")
        }
    }

    @Test
    fun `promotes to a foreground service once the app enters the foreground during a background run`() = runTest {
        var inForeground = false
        val first = DownloadProgress(50.0, 10.0, 50, 100)
        val second = DownloadProgress(75.0, 10.0, 75, 100)
        every { runtime.canStartForeground() } answers { inForeground }
        coEvery { transfer.download(any(), any()) } coAnswers {
            val report = secondArg<suspend (DownloadProgress) -> Unit>()
            report(first)
            inForeground = true
            report(second)
            inForeground = false
            DownloadTransferResult.Success
        }
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerifyOrder {
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(DownloadProgress(25.0, 0.0, 25, 100)))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(first))
            runtime.setForeground("Demo", DownloadNotificationPhase.Downloading(second))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Verifying)
        }
        coVerify(exactly = 1) { runtime.setForeground(any(), any()) }
        verify(exactly = 0) { runtime.clearProgress(any()) }
    }

    @Test
    fun `does not retry setForeground after a foreground failure and falls back to regular notifications`() = runTest {
        val first = DownloadProgress(50.0, 10.0, 50, 100)
        val second = DownloadProgress(75.0, 10.0, 75, 100)
        coEvery { runtime.setForeground(any(), any()) } throws IllegalStateException("fg denied")
        coEvery { transfer.download(any(), any()) } coAnswers {
            val report = secondArg<suspend (DownloadProgress) -> Unit>()
            report(first)
            report(second)
            DownloadTransferResult.Success
        }
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerify(exactly = 1) { runtime.setForeground(any(), any()) }
        coVerify(exactly = 1) { runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(first)) }
        coVerify(exactly = 1) { runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(second)) }
        coVerify(exactly = 1) { runtime.notifyProgress("Demo", DownloadNotificationPhase.Verifying) }
        verify(exactly = 1) { runtime.clearProgress("Demo") }
        verify(exactly = 1) {
            logger.error("Download notification could not be updated in foreground for demo.", any())
        }
        verify(exactly = 0) {
            logger.info("Running download without foreground service while app is in background: demo.")
        }
    }

    @Test
    fun `clears the fallback progress notification when a background download is cancelled`() = runTest {
        every { runtime.canStartForeground() } returns false
        coEvery { dao.get(GAME_ID) } returnsMany listOf(entity(), entity().copy(status = DownloadStatus.PAUSED.name))
        coEvery { transfer.download(any(), any()) } throws CancellationException("paused")
        val sut = createSut()

        val error = runCatching { sut.run(GAME_ID, WORK_ID, runtime) }.exceptionOrNull()

        (error is CancellationException) shouldBe true
        coVerify(exactly = 0) { runtime.setForeground(any(), any()) }
        verify(exactly = 1) { runtime.clearProgress("Demo") }
    }

    @Test
    fun `updates the active foreground notification without calling setForeground again`() = runTest {
        var inForeground = true
        val progress = DownloadProgress(60.0, 10.0, 60, 100)
        every { runtime.canStartForeground() } answers { inForeground }
        coEvery { transfer.download(any(), any()) } coAnswers {
            inForeground = false
            secondArg<suspend (DownloadProgress) -> Unit>()(progress)
            DownloadTransferResult.Success
        }
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerifyOrder {
            runtime.setForeground("Demo", DownloadNotificationPhase.Downloading(DownloadProgress(25.0, 0.0, 25, 100)))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(progress))
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Verifying)
        }
        coVerify(exactly = 1) { runtime.setForeground(any(), any()) }
        verify(exactly = 0) { runtime.clearProgress(any()) }
    }

    @Test
    fun `ignores a stale worker without starting a transfer`() = runTest {
        coEvery { dao.get(GAME_ID) } returns entity().copy(workId = "newer-work")
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerify(exactly = 0) { dao.setWorkerStatus(any(), any(), any(), any()) }
        coVerify(exactly = 0) { transfer.download(any(), any()) }
    }

    @Test
    fun `stops when the start transition loses a concurrent state change`() = runTest {
        coEvery { dao.setWorkerStatus(any(), any(), any(), any()) } returns 0
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.SUCCESS

        coVerify(exactly = 0) { runtime.setForeground(any(), any()) }
        coVerify(exactly = 0) { transfer.download(any(), any()) }
    }

    @Test
    fun `drops progress after a concurrent state transition`() = runTest {
        val progress = DownloadProgress(50.0, 10.0, 50, 100)
        coEvery { dao.setProgress(any(), any(), any(), any(), any(), any(), any(), any()) } returns 0
        coEvery { transfer.download(any(), any()) } coAnswers {
            secondArg<suspend (DownloadProgress) -> Unit>()(progress)
            DownloadTransferResult.Failed("network")
        }
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.FAILURE

        coVerify(exactly = 0) {
            runtime.notifyProgress("Demo", DownloadNotificationPhase.Downloading(progress))
        }
    }

    @Test
    fun `retains the latest persisted progress when a transfer fails`() = runTest {
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Failed("network")
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.FAILURE

        coVerify(exactly = 1) {
            dao.finish(GAME_ID, WORK_ID, DownloadStatus.DOWNLOADING.name, DownloadStatus.FAILED.name, "network")
        }
    }

    @Test
    fun `deletes artifacts when storage permission is denied`() = runTest {
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.PermissionDenied
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.FAILURE

        verify(exactly = 1) { fileStore.deleteArtifacts(DESTINATION) }
        coVerify(exactly = 1) {
            dao.finish(
                GAME_ID,
                WORK_ID,
                DownloadStatus.DOWNLOADING.name,
                DownloadStatus.PERMISSION_DENIED.name,
                "Download storage permission was denied.",
                true,
            )
        }
    }

    @Test
    fun `keeps a partial when cancellation represents a pause`() = runTest {
        coEvery { dao.get(GAME_ID) } returnsMany listOf(entity(), entity().copy(status = DownloadStatus.PAUSED.name))
        coEvery { transfer.download(any(), any()) } throws CancellationException("paused")
        val sut = createSut()

        val error = runCatching { sut.run(GAME_ID, WORK_ID, runtime) }.exceptionOrNull()

        (error is CancellationException) shouldBe true
        verify(exactly = 0) { fileStore.deleteArtifacts(any()) }
    }

    @Test
    fun `deletes a partial when cancellation represents user cancellation`() = runTest {
        coEvery { dao.get(GAME_ID) } returnsMany listOf(entity(), entity().copy(status = DownloadStatus.CANCELLED.name))
        coEvery { transfer.download(any(), any()) } throws CancellationException("cancelled")
        val sut = createSut()

        val error = runCatching { sut.run(GAME_ID, WORK_ID, runtime) }.exceptionOrNull()

        (error is CancellationException) shouldBe true
        verify(exactly = 1) { fileStore.deleteArtifacts(DESTINATION) }
    }

    @Test
    fun `returns a transfer stopped by the system to the queue with its partial`() = runTest {
        coEvery { dao.get(GAME_ID) } returnsMany listOf(
            entity(),
            entity().copy(status = DownloadStatus.DOWNLOADING.name),
        )
        coEvery { transfer.download(any(), any()) } throws CancellationException("constraints not met")
        val sut = createSut()

        val error = runCatching { sut.run(GAME_ID, WORK_ID, runtime) }.exceptionOrNull()

        (error is CancellationException) shouldBe true
        verify(exactly = 0) { fileStore.deleteArtifacts(any()) }
        coVerify(exactly = 1) {
            dao.setWorkerStatus(
                GAME_ID,
                WORK_ID,
                DownloadStatus.QUEUED.name,
                listOf(DownloadStatus.DOWNLOADING.name),
            )
        }
    }

    @Test
    fun `maps an unexpected worker failure without losing persisted progress`() = runTest {
        coEvery { transfer.download(any(), any()) } throws IOException("boom")
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.FAILURE

        coVerify(exactly = 1) {
            dao.finish(GAME_ID, WORK_ID, DownloadStatus.DOWNLOADING.name, DownloadStatus.FAILED.name, "boom")
        }
        verify(exactly = 1) { logger.error("Download worker failed for demo.", any()) }
    }

    @Test
    fun `a replacement worker waits for the previous transfer and its cancellation cleanup`() = runTest {
        val cleanupStarted = CompletableDeferred<Unit>()
        val releaseCleanup = CompletableDeferred<Unit>()
        coEvery { transfer.download(any(), any()) } coAnswers { awaitCancellation() }
        coEvery { dao.get(GAME_ID) } returnsMany
            listOf(entity(), entity().copy(status = DownloadStatus.DOWNLOADING.name), entity())
        coEvery {
            dao.setWorkerStatus(GAME_ID, WORK_ID, DownloadStatus.QUEUED.name, any())
        } coAnswers {
            cleanupStarted.complete(Unit)
            releaseCleanup.await()
            1
        }
        val sut = createSut()
        val original = launch(start = CoroutineStart.UNDISPATCHED) { sut.run(GAME_ID, WORK_ID, runtime) }
        original.cancel()
        cleanupStarted.await()
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Success
        val replacement = async(start = CoroutineStart.UNDISPATCHED) { sut.run(GAME_ID, WORK_ID, runtime) }

        try {
            coVerify(exactly = 1) { transfer.download(any(), any()) }
        } finally {
            releaseCleanup.complete(Unit)
        }

        original.join()
        replacement.await() shouldBe DownloadWorkerOutcome.SUCCESS
        coVerify(exactly = 1) { handoff.deliver(any()) }
    }

    private fun TestScope.createSut() = DownloadWorkerRunner(
        dao = dao,
        transfer = transfer,
        handoff = handoff,
        installer = installer,
        fileStore = fileStore,
        dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        logger = logger,
    )

    private fun entity() = DownloadEntity(
        gameId = GAME_ID,
        displayName = "Demo",
        version = "1.0",
        key = null,
        url = "https://cdn.test/demo.zip",
        expectedSha256 = "a".repeat(64),
        variant = null,
        destinationPath = DESTINATION,
        workId = WORK_ID,
        status = DownloadStatus.QUEUED.name,
        percent = 25.0,
        bytesPerSecond = 100.0,
        downloadedBytes = 25,
        totalBytes = 100,
        errorMessage = null,
        createdAtEpochMillis = 1,
    )

    private companion object {
        val INSTALLED_GAME = LocalGame(MissingGameId, "Demo", "1.0", null)

        @JvmStatic
        fun failedInstallations() = listOf(
            GameInstallationResult.ExtractionFailed,
            GameInstallationResult.HashMismatch,
            GameInstallationResult.StorageAccessDenied,
            GameInstallationResult.RegistryFailed,
        )

        const val GAME_ID = "demo"
        const val WORK_ID = "11111111-2222-3333-4444-555555555555"
        const val DESTINATION = "C:/downloads/demo.zip"
    }
}
