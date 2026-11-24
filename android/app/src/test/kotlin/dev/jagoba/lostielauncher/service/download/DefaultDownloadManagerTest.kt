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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
        every { fileStore.purgeStale(any(), any(), any(), any()) } returns 0
        coEvery { dao.get(any()) } returns null
        coEvery { dao.countWithStatuses(any()) } returns 0
        coEvery { dao.getWithStatuses(any()) } returns emptyList()
        coEvery { dao.deleteByGameIds(any()) } returns 0
        coEvery { dao.upsert(any()) } returns Unit
        coEvery { dao.setStatus(any(), any(), any(), any()) } returns Unit
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
        entity.captured.expectedSha256 shouldBe "a".repeat(64)
        entity.captured.variant shouldBe null
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
    fun `retries a legacy failed download without deleting its resumable partial`() = runTest {
        coEvery { dao.get("demo") } returns entity(DownloadStatus.FAILED).copy(
            expectedSha256 = null,
            variant = null,
        )
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

    @ParameterizedTest
    @ValueSource(strings = ["hash", "variant"])
    fun `does not reuse a partial when its pinned metadata changes`(changedMetadata: String) = runTest {
        val existing = when (changedMetadata) {
            "hash" -> entity(DownloadStatus.FAILED).copy(expectedSha256 = "b".repeat(64))
            else -> entity(DownloadStatus.FAILED).copy(variant = "special")
        }
        val replacement = slot<DownloadEntity>()
        coEvery { dao.get("demo") } returns existing
        every { fileStore.hasResumablePartial(destinationPath) } returns true
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.start(request()) shouldBe DownloadCommandResult.ACCEPTED

        coVerify(exactly = 0) { dao.replaceWork(any(), any(), any(), any()) }
        coVerify(exactly = 1) { dao.upsert(capture(replacement)) }
        replacement.captured.expectedSha256 shouldBe request().expectedSha256
        replacement.captured.variant shouldBe request().variant
        verify(exactly = 1) { fileStore.deleteArtifacts(destinationPath) }
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

        coVerify(exactly = 1) { dao.setStatus("demo", DownloadStatus.PAUSED.name, null, false) }
        coVerify(exactly = 1) { scheduler.cancel(workId) }
    }

    @Test
    fun `does not pause an installation in progress`() = runTest {
        coEvery { dao.get("demo") } returns entity(DownloadStatus.INSTALLING)
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.pause("demo") shouldBe DownloadCommandResult.INVALID_STATE

        coVerify(exactly = 0) { dao.setStatus(any(), any(), any(), any()) }
        coVerify(exactly = 0) { scheduler.cancel(any()) }
    }

    @Test
    fun `retries a failed installation from the kept archive with a new worker`() = runTest {
        val entity = slot<DownloadEntity>()
        coEvery { dao.get("demo") } returns entity(DownloadStatus.INSTALLATION_FAILED).copy(
            workId = "old-work",
            errorMessage = "ExtractionFailed",
        )
        every { fileStore.hasArchive(destinationPath) } returns true
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.retry("demo") shouldBe DownloadCommandResult.ACCEPTED

        coVerify(exactly = 1) { dao.upsert(capture(entity)) }
        entity.captured shouldBe entity(DownloadStatus.INSTALLING)
        verify(exactly = 0) { fileStore.deleteArtifacts(any()) }
        verify(exactly = 1) { scheduler.enqueue("demo", workId) }
    }

    @Test
    fun `downloads the archive again when a failed installation no longer has it`() = runTest {
        val entity = slot<DownloadEntity>()
        coEvery { dao.get("demo") } returns entity(DownloadStatus.INSTALLATION_FAILED)
        every { fileStore.hasArchive(destinationPath) } returns false
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.retry("demo") shouldBe DownloadCommandResult.ACCEPTED

        coVerify(exactly = 1) { dao.upsert(capture(entity)) }
        entity.captured shouldBe
            entity(DownloadStatus.QUEUED).copy(percent = 0.0, downloadedBytes = 0, totalBytes = null)
        verify(exactly = 1) { scheduler.enqueue("demo", workId) }
    }

    @Test
    fun `only retries a failed installation when no other download is active`() = runTest {
        coEvery { dao.get("demo") } returnsMany listOf(
            entity(DownloadStatus.FAILED),
            entity(DownloadStatus.INSTALLATION_FAILED),
        )
        coEvery { dao.countWithStatuses(any()) } returns 1
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.retry("demo") shouldBe DownloadCommandResult.INVALID_STATE
        sut.retry("demo") shouldBe DownloadCommandResult.BUSY

        coVerify(exactly = 0) { dao.upsert(any()) }
        verify(exactly = 0) { scheduler.enqueue(any(), any()) }
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

        coVerify(exactly = 1) { dao.setStatus("demo", DownloadStatus.CANCELLED.name, null, true) }
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
    fun `reports an empty list when Room cannot read downloads`() = runTest {
        val failure = IllegalStateException("Cannot open database")
        every { dao.observeAll() } returns flow { throw failure }
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.downloads.test {
            awaitItem() shouldBe emptyList()
            awaitComplete()
        }
        verify { logger.error(any(), failure) }
    }

    @Test
    fun `propagates cancellation while observing downloads`() = runTest {
        val cancellation = CancellationException("Cancelled query")
        every { dao.observeAll() } returns flow { throw cancellation }
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.downloads.test {
            awaitError() shouldBe cancellation
        }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `purges stale files and inactive rows whose artifacts are gone`() = runTest {
        val failed = entity(DownloadStatus.FAILED)
        val completed = entity(
            DownloadStatus.COMPLETED,
        ).copy(gameId = "kept", destinationPath = keptDestinationPath)
        every { fileStore.purgeStale(setOf("demo", "kept"), any(), emptySet(), emptySet()) } returns 2
        every { fileStore.hasArtifacts(destinationPath) } returns false
        every { fileStore.hasArtifacts(keptDestinationPath) } returns true
        coEvery { dao.getWithStatuses(DownloadStatus.PurgeableNames) } returns listOf(failed, completed)
        coEvery { dao.deleteByGameIds(listOf("demo")) } returns 1
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.purgeStale(setOf("demo", "kept")) shouldBe 3

        coVerify(exactly = 1) { dao.deleteByGameIds(listOf("demo")) }
        verify(exactly = 1) { fileStore.hasArtifacts(keptDestinationPath) }
    }

    @Test
    fun `protects ongoing transfers while letting failed installations follow archive expiry`() = runTest {
        val paused = entity(DownloadStatus.PAUSED).copy(
            gameId = "orphan-paused",
            destinationPath = File("downloads/orphan-paused.zip").absolutePath,
        )
        val queued = entity(DownloadStatus.QUEUED).copy(
            gameId = "orphan-queued",
            destinationPath = File("downloads/orphan-queued.zip").absolutePath,
        )
        val failedInstall = entity(DownloadStatus.INSTALLATION_FAILED).copy(
            gameId = "orphan-install-failed",
            destinationPath = File("downloads/orphan-install-failed.zip").absolutePath,
        )
        val completed = entity(DownloadStatus.COMPLETED).copy(
            gameId = "orphan-completed",
            destinationPath = File("downloads/orphan-completed.zip").absolutePath,
        )
        val protectedPaths = setOf(paused.destinationPath, queued.destinationPath)
        val retainedArchivePaths = setOf(failedInstall.destinationPath)
        coEvery { dao.getWithStatuses(DownloadStatus.OngoingNames) } returns listOf(paused, queued, failedInstall)
        coEvery { dao.getWithStatuses(DownloadStatus.PurgeableNames) } returns listOf(completed)
        every { fileStore.purgeStale(setOf("catalogue-game"), any(), protectedPaths, retainedArchivePaths) } returns 0
        coEvery { dao.deleteByGameIds(listOf("orphan-completed")) } returns 1
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.purgeStale(setOf("catalogue-game")) shouldBe 1

        verify(exactly = 1) {
            fileStore.purgeStale(setOf("catalogue-game"), any(), protectedPaths, retainedArchivePaths)
        }
        coVerify(exactly = 1) { dao.deleteByGameIds(listOf("orphan-completed")) }
    }

    @Test
    fun `does not purge when the catalogue is empty`() = runTest {
        val sut = createSut(StandardTestDispatcher(testScheduler))

        sut.purgeStale(emptySet()) shouldBe 0

        verify(exactly = 0) { fileStore.purgeStale(any(), any(), any(), any()) }
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
        expectedSha256 = "a".repeat(64),
    )

    private fun entity(status: DownloadStatus) = DownloadEntity(
        gameId = "demo",
        displayName = "Demo",
        version = "1.0",
        key = null,
        url = "https://cdn.test/games/demo/1.zip",
        expectedSha256 = "a".repeat(64),
        variant = null,
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
