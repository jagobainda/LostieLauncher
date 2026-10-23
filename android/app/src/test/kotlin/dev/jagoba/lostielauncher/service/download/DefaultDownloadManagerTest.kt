package dev.jagoba.lostielauncher.service.download

import app.cash.turbine.test
import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadOptions
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameDownloadArgs
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DefaultDownloadManager")
class DefaultDownloadManagerTest {
    private val dao = mockk<DownloadDao>()
    private val scheduler = mockk<DownloadWorkScheduler>()
    private val fileStore = mockk<DownloadFileStore>()
    private val logger = mockk<Logger>(relaxed = true)
    private val workId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val destinationPath = File("downloads/demo.zip").absolutePath
    private val keptDestinationPath = File("downloads/kept.zip").absolutePath

    @BeforeEach
    fun configureDefaults() {
        every { dao.observeAll() } returns flowOf(emptyList())
        every { scheduler.nextId() } returns workId
        every { scheduler.enqueue(any(), any()) } returns Unit
        coEvery { scheduler.cancel(any()) } returns Unit
        every { fileStore.destinationFor(any()) } returns File(destinationPath)
        every { fileStore.deleteArtifacts(any()) } returns 0
        every { fileStore.hasResumablePartial(any()) } returns false
        every { fileStore.hasArtifacts(any()) } returns true
        every { fileStore.purgeStale(any(), any()) } returns 0
        coEvery { dao.get(any()) } returns null
        coEvery { dao.countWithStatuses(any()) } returns 0
        coEvery { dao.getWithStatuses(any()) } returns emptyList()
        coEvery { dao.deleteByGameIds(any()) } returns 0
        coEvery { dao.upsert(any()) } returns Unit
        coEvery { dao.setStatus(any(), any(), any()) } returns Unit
    }

    @Test
    fun `starts one durable work request`() = runTest {
        val entity = slot<DownloadEntity>()
        val sut = createSut(StandardTestDispatcher(testScheduler))

        val result = sut.start(request())

        result shouldBe DownloadCommandResult.ACCEPTED
        coVerify(exactly = 1) { dao.upsert(capture(entity)) }
        entity.captured.status shouldBe DownloadStatus.QUEUED.name
        entity.captured.url shouldBe "https://cdn.test/games/demo/1.zip"
        verify(exactly = 1) { fileStore.deleteArtifacts(destinationPath) }
        verify(exactly = 1) { scheduler.enqueue("demo", workId) }
    }

    @Test
    fun `retries a failed matching download without deleting its resumable partial`() = runTest {
        coEvery { dao.get("demo") } returns entity(DownloadStatus.FAILED)
        coEvery { dao.replaceWork(any(), any(), any(), any()) } returns 1
        every { fileStore.hasResumablePartial(destinationPath) } returns true
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.start(request()) shouldBe DownloadCommandResult.ACCEPTED

        coVerify(exactly = 1) {
            dao.replaceWork("demo", DownloadStatus.FAILED.name, workId.toString(), DownloadStatus.QUEUED.name)
        }
        coVerify(exactly = 0) { dao.upsert(any()) }
        verify(exactly = 0) { fileStore.deleteArtifacts(any()) }
        verify(exactly = 1) { scheduler.enqueue("demo", workId) }
    }

    @Test
    fun `rejects a second active download`() = runTest {
        coEvery { dao.countWithStatuses(any()) } returns 1
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.start(request()) shouldBe DownloadCommandResult.BUSY

        coVerify(exactly = 0) { dao.upsert(any()) }
        verify(exactly = 0) { scheduler.enqueue(any(), any()) }
    }

    @Test
    fun `pauses active work after persisting the paused state`() = runTest {
        coEvery { dao.get("demo") } returns entity(DownloadStatus.DOWNLOADING)
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.pause("demo") shouldBe DownloadCommandResult.ACCEPTED

        coVerify(exactly = 1) { dao.setStatus("demo", DownloadStatus.PAUSED.name, null) }
        coVerify(exactly = 1) { scheduler.cancel(workId) }
    }

    @Test
    fun `resumes a paused transfer with a new worker`() = runTest {
        coEvery { dao.get("demo") } returns entity(DownloadStatus.PAUSED)
        coEvery { dao.replaceWork(any(), any(), any(), any()) } returns 1
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.resume("demo") shouldBe DownloadCommandResult.ACCEPTED

        coVerify(exactly = 1) {
            dao.replaceWork("demo", DownloadStatus.PAUSED.name, workId.toString(), DownloadStatus.QUEUED.name)
        }
        verify(exactly = 1) { scheduler.enqueue("demo", workId) }
    }

    @Test
    fun `cancels work and removes every transfer artifact`() = runTest {
        coEvery { dao.get("demo") } returns entity(DownloadStatus.PAUSED)
        every { fileStore.deleteArtifacts(destinationPath) } returns 2
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.cancel("demo") shouldBe DownloadCommandResult.ACCEPTED

        coVerify(exactly = 1) { dao.setStatus("demo", DownloadStatus.CANCELLED.name, null) }
        coVerify(exactly = 1) { scheduler.cancel(workId) }
        verify(exactly = 1) { fileStore.deleteArtifacts(destinationPath) }
    }

    @Test
    fun `waits for worker cancellation before removing transfer artifacts`() = runTest {
        val workerStopped = CompletableDeferred<Unit>()
        coEvery { dao.get("demo") } returns entity(DownloadStatus.DOWNLOADING)
        coEvery { scheduler.cancel(workId) } coAnswers { workerStopped.await() }
        val sut = createSut(StandardTestDispatcher(testScheduler))

        val result = async { sut.cancel("demo") }
        yield()
        verify(exactly = 0) { fileStore.deleteArtifacts(any()) }

        workerStopped.complete(Unit)
        result.await() shouldBe DownloadCommandResult.ACCEPTED
        verify(exactly = 1) { fileStore.deleteArtifacts(destinationPath) }
    }

    @Test
    fun `maps persistent entities into observable presentation state`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(entity(DownloadStatus.DOWNLOADING)))
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.downloads.test {
            val item = awaitItem().single()
            item.gameId shouldBe "demo"
            item.status shouldBe DownloadStatus.DOWNLOADING
            awaitComplete()
        }
    }

    @Test
    fun `purges stale files and inactive rows whose artifacts are gone`() = runTest {
        val failed = entity(DownloadStatus.FAILED)
        val completed = entity(
            DownloadStatus.COMPLETED,
        ).copy(gameId = "kept", destinationPath = keptDestinationPath)
        every { fileStore.purgeStale(setOf("demo", "kept"), any()) } returns 2
        every { fileStore.hasArtifacts(destinationPath) } returns false
        every { fileStore.hasArtifacts(keptDestinationPath) } returns true
        coEvery { dao.getWithStatuses(any()) } returns listOf(failed, completed)
        coEvery { dao.deleteByGameIds(listOf("demo")) } returns 1
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.purgeStale(setOf("demo", "kept")) shouldBe 3

        coVerify(exactly = 1) { dao.deleteByGameIds(listOf("demo")) }
        verify(exactly = 1) { fileStore.hasArtifacts(keptDestinationPath) }
    }

    @Test
    fun `does not purge when the catalogue is empty`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.purgeStale(emptySet()) shouldBe 0

        verify(exactly = 0) { fileStore.purgeStale(any(), any()) }
        coVerify(exactly = 0) { dao.getWithStatuses(any()) }
    }

    private fun createSut(dispatcher: CoroutineDispatcher) = DefaultDownloadManager(
        dao = dao,
        scheduler = scheduler,
        fileStore = fileStore,
        downloadOptions = DownloadOptions("https://cdn.test/games"),
        dispatchers = TestDispatcherProvider(dispatcher),
        clock = Clock.fixed(Instant.parse("2026-09-22T12:00:00Z"), ZoneOffset.UTC),
        logger = logger,
    )

    private fun request() = DownloadRequest(
        displayName = "Demo",
        args = GameDownloadArgs("demo", "1.0", "/demo/1.zip"),
    )

    private fun entity(status: DownloadStatus) = DownloadEntity(
        gameId = "demo",
        displayName = "Demo",
        version = "1.0",
        key = null,
        url = "https://cdn.test/games/demo/1.zip",
        destinationPath = destinationPath,
        workId = workId.toString(),
        status = status.name,
        percent = 25.0,
        bytesPerSecond = 100.0,
        downloadedBytes = 25,
        totalBytes = 100,
        errorMessage = null,
        createdAtEpochMillis = 1,
    )
}
