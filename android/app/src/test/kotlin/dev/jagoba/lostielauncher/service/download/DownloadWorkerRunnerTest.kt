package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DownloadWorkerRunner")
class DownloadWorkerRunnerTest {
    private val dao = mockk<DownloadDao>()
    private val transfer = mockk<DownloadTransfer>()
    private val handoff = mockk<DownloadedFileHandoff>()
    private val fileStore = mockk<DownloadFileStore>()
    private val runtime = mockk<DownloadWorkerRuntime>()
    private val logger = mockk<Logger>(relaxed = true)

    @BeforeEach
    fun configureDefaults() {
        coEvery { dao.get(GAME_ID) } returns entity()
        coEvery { dao.setWorkerStatus(any(), any(), any(), any()) } returns 1
        coEvery { dao.setProgress(any(), any(), any(), any(), any(), any(), any(), any()) } returns 1
        coEvery { dao.finish(any(), any(), any(), any(), any()) } returns 1
        coEvery { dao.complete(any(), any(), any(), any()) } returns 1
        coEvery { handoff.deliver(any()) } returns Unit
        every { fileStore.deleteArtifacts(any()) } returns 2
        coEvery { runtime.setForeground(any(), any()) } returns Unit
        coEvery { runtime.reportProgress(any(), any()) } returns Unit
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
        coVerify(exactly = 1) { runtime.reportProgress("Demo", progress) }
        coVerify(exactly = 1) {
            dao.complete(GAME_ID, WORK_ID, DownloadStatus.DOWNLOADING.name, DownloadStatus.COMPLETED.name)
        }
        coVerify(exactly = 1) { handoff.deliver(match { it.gameId == GAME_ID && it.path == DESTINATION }) }
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

        coVerify(exactly = 0) { runtime.reportProgress(any(), any()) }
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
    fun `maps an unexpected worker failure without losing persisted progress`() = runTest {
        coEvery { transfer.download(any(), any()) } throws IOException("boom")
        val sut = createSut()

        sut.run(GAME_ID, WORK_ID, runtime) shouldBe DownloadWorkerOutcome.FAILURE

        coVerify(exactly = 1) {
            dao.finish(GAME_ID, WORK_ID, DownloadStatus.DOWNLOADING.name, DownloadStatus.FAILED.name, "boom")
        }
        verify(exactly = 1) { logger.error("Download worker failed for demo.", any()) }
    }

    private fun TestScope.createSut() = DownloadWorkerRunner(
        dao = dao,
        transfer = transfer,
        handoff = handoff,
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
        const val GAME_ID = "demo"
        const val WORK_ID = "11111111-2222-3333-4444-555555555555"
        const val DESTINATION = "C:/downloads/demo.zip"
    }
}
