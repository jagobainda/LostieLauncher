package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadTransferOptions
import dev.jagoba.lostielauncher.model.GameDownloadArgs
import dev.jagoba.lostielauncher.service.storage.FixedStorageLocations
import dev.jagoba.lostielauncher.util.download.DownloadPathUtils
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("DownloadFileStore")
class DownloadFileStoreTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `places archives below the injected games root`() {
        val sut = createSut()

        val result = sut.destinationFor(GameDownloadArgs("demo", "1.0", "/demo.zip"))

        requireNotNull(result.parentFile).toPath() shouldBe directory.resolve("downloads")
    }

    @Test
    fun `cancellation deletes the final partial and metadata files`() {
        val sut = createSut()
        val destination = sut.destinationFor(GameDownloadArgs("demo", "1.0", "/demo.zip")).toPath()
        requireNotNull(destination.parent).createDirectories()
        destination.createFile()
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString())).createFile()
        val metadata = Path.of(DownloadPathUtils.getMetaFilePath(part.toString())).createFile()

        sut.deleteArtifacts(destination.toString()) shouldBe 3

        destination.exists() shouldBe false
        part.exists() shouldBe false
        metadata.exists() shouldBe false
    }

    @Test
    fun `purges old and unknown managed files while retaining a known recent partial`() {
        val sut = createSut()
        val cache = directory.resolve("downloads").createDirectories()
        val now = Instant.parse("2026-09-23T12:00:00Z")
        val old = cache.resolve("demo.old.zip").createFile()
        Files.setLastModifiedTime(old, FileTime.from(now.minus(Duration.ofDays(15))))
        val unknown = cache.resolve("removed.token.zip").createFile()
        val partial = cache.resolve("demo.token.zip.part").createFile()
        val metadata = cache.resolve("demo.token.zip.part.meta").createFile()
        val unmanaged = cache.resolve("keep.txt").createFile()

        sut.purgeStale(setOf("DEMO"), now) shouldBe 2

        old.exists() shouldBe false
        unknown.exists() shouldBe false
        partial.exists() shouldBe true
        metadata.exists() shouldBe true
        unmanaged.exists() shouldBe true
    }

    @Test
    fun `recognizes a resumable partial only when its metadata exists`() {
        val sut = createSut()
        val destination = sut.destinationFor(GameDownloadArgs("demo", "1.0", "/demo.zip")).toPath()
        requireNotNull(destination.parent).createDirectories()
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString())).createFile()

        sut.hasResumablePartial(destination.toString()) shouldBe false

        Path.of(DownloadPathUtils.getMetaFilePath(part.toString())).createFile()
        sut.hasResumablePartial(destination.toString()) shouldBe true
    }

    private fun createSut() = DefaultDownloadFileStore(
        storageLocations = FixedStorageLocations(directory.toFile(), directory.resolve("logs").toFile()),
        options = DownloadTransferOptions(
            cacheDirectoryName = "downloads",
            cacheMaxAge = Duration.ofDays(14),
            bufferSizeBytes = 4,
            maximumAttempts = 3,
            retryBaseDelay = 1.milliseconds,
            inactivityTimeout = 1.milliseconds,
            progressSampleInterval = 1.milliseconds,
            finalizationMaximumAttempts = 3,
            finalizationBaseDelay = 1.milliseconds,
        ),
    )
}
