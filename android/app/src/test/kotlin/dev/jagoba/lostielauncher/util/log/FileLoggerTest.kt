package dev.jagoba.lostielauncher.util.log

import dev.jagoba.lostielauncher.model.LogOptions
import dev.jagoba.lostielauncher.service.storage.FixedStorageLocations
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("FileLogger")
class FileLoggerTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `writes the desktop line format`() {
        val sut = createSut()

        sut.info("hello")

        temporaryDirectory.resolve("2026-06.log").readText()
            .shouldEndWith("2026-06-15 10:30:00 [INFO] -> hello${System.lineSeparator()}")
    }

    @Test
    fun `writes a sortable invariant timestamp prefix`() {
        val sut = createSut()
        sut.debug("x")

        val prefix = temporaryDirectory.resolve("2026-06.log").readText().take(19)

        LocalDateTime.parse(prefix, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) shouldBe
            LocalDateTime.of(2026, 6, 15, 10, 30)
    }

    @Test
    fun `preserves an error message and throwable`() {
        val sut = createSut()

        sut.error("failed", IllegalStateException("boom"))

        val content = temporaryDirectory.resolve("2026-06.log").readText()
        content shouldContain "[ERROR] -> failed"
        content shouldContain "IllegalStateException: boom"
    }

    @Test
    fun `rolls after the active file reaches its cap`() {
        val sut = createSut(maxFileSizeBytes = 1)

        sut.debug("first")
        sut.debug("second")

        temporaryDirectory.resolve("2026-06.log").exists() shouldBe true
        temporaryDirectory.resolve("2026-06.1.log").exists() shouldBe true
    }

    @Test
    fun `serializes concurrent writes without losing lines`() {
        val sut = createSut()

        val writers = List(100) { index -> thread { sut.info("line-$index") } }
        writers.forEach(Thread::join)

        temporaryDirectory.resolve("2026-06.log").readLines() shouldHaveSize 100
    }

    @Test
    fun `purges files beyond the retention window`() {
        temporaryDirectory.resolve("2025-11.log").createFile()
        temporaryDirectory.resolve("2025-12.log").createFile()
        val sut = createSut()

        sut.purgeExpired()

        temporaryDirectory.resolve("2025-11.log").exists() shouldBe false
        temporaryDirectory.resolve("2025-12.log").exists() shouldBe true
    }

    @Test
    fun `never propagates a filesystem failure`() {
        val fileInsteadOfDirectory = temporaryDirectory.resolve("not-a-directory")
        fileInsteadOfDirectory.createFile()
        val sut = createSut(logsDirectory = fileInsteadOfDirectory)

        sut.info("lost")
    }

    private fun createSut(maxFileSizeBytes: Long = 10L * 1024 * 1024, logsDirectory: Path = temporaryDirectory) =
        FileLogger(
            locations = FixedStorageLocations(temporaryDirectory.resolve("games").toFile(), logsDirectory.toFile()),
            options = LogOptions(maxFileSizeBytes, 6, ZoneOffset.UTC),
            clock = Clock.fixed(Now, ZoneOffset.UTC),
        )

    private companion object {
        val Now: Instant = Instant.parse("2026-06-15T10:30:00Z")
    }
}
