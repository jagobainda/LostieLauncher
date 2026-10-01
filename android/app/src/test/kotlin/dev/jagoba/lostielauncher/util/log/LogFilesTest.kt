package dev.jagoba.lostielauncher.util.log

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("LogFiles")
class LogFilesTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @ParameterizedTest
    @CsvSource(
        "2026-06, 0, 2026-06.log",
        "2026-06, 1, 2026-06.1.log",
        "2026-06, 12, 2026-06.12.log",
        "2026-06, -1, 2026-06.log",
    )
    fun `builds base or numbered file names`(month: String, index: Int, expected: String) {
        LogFiles.buildFileName(month, index) shouldBe expected
    }

    @ParameterizedTest
    @CsvSource(
        "2026-06.log, 0",
        "2026-06.1.log, 1",
        "2026-06.10.log, 10",
    )
    fun `parses indexes for one month`(fileName: String, expected: Int) {
        LogFiles.tryParseIndex(fileName, "2026-06") shouldBe expected
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-05.log", "2026-06.0.log", "2026-06.x.log", "2026-06.log.bak", "random.log"])
    fun `rejects names outside the requested month`(fileName: String) {
        LogFiles.tryParseIndex(fileName, "2026-06") shouldBe null
    }

    @Test
    fun `selects only months older than retention`() {
        val files = listOf(
            "2025-11.log",
            "2025-01.3.log",
            "2025-12.log",
            "2026-06.log",
            "2026-06.2.log",
            "notes.log",
        )

        LogFiles.selectExpired(files, Now, ZoneOffset.UTC, 6)
            .shouldContainExactlyInAnyOrder("2025-11.log", "2025-01.3.log")
    }

    @Test
    fun `probes an empty month as index zero`() {
        LogFiles.probeMonth(temporaryDirectory.toFile(), "2026-06", 1024) shouldBe LogProbe(0, 0)
    }

    @Test
    fun `continues an existing base file below the cap`() {
        writeBytes("2026-06.log", 50)

        LogFiles.probeMonth(temporaryDirectory.toFile(), "2026-06", 1024) shouldBe LogProbe(0, 50)
    }

    @Test
    fun `rolls when the highest file is at the cap`() {
        writeBytes("2026-06.log", 1024)

        LogFiles.probeMonth(temporaryDirectory.toFile(), "2026-06", 1024) shouldBe LogProbe(1, 0)
    }

    @Test
    fun `continues the highest rolled file below the cap`() {
        writeBytes("2026-06.log", 1024)
        writeBytes("2026-06.1.log", 200)
        writeBytes("2026-05.log", 5000)

        LogFiles.probeMonth(temporaryDirectory.toFile(), "2026-06", 1024) shouldBe LogProbe(1, 200)
    }

    @Test
    fun `resolves the base file on the first write`() {
        val result = LogFiles.resolveActiveFile(temporaryDirectory.toFile(), "2026-06", 1024, null, null, 0, 0)

        result.path.name shouldBe "2026-06.log"
        result.index shouldBe 0
    }

    @Test
    fun `reprobes after a month change`() {
        writeBytes("2026-06.log", 200)

        val result = LogFiles.resolveActiveFile(
            temporaryDirectory.toFile(),
            "2026-06",
            1024,
            temporaryDirectory.resolve("2026-05.3.log").toFile(),
            "2026-05",
            3,
            9999,
        )

        result.path.name shouldBe "2026-06.log"
        result.size shouldBe 200
    }

    @Test
    fun `rolls a cached file at the cap`() {
        val result = LogFiles.resolveActiveFile(
            temporaryDirectory.toFile(),
            "2026-06",
            1024,
            temporaryDirectory.resolve("2026-06.log").toFile(),
            "2026-06",
            0,
            1024,
        )

        result.path.name shouldBe "2026-06.1.log"
        result.size shouldBe 0
    }

    @Test
    fun `keeps climbing rolled indexes`() {
        val result = LogFiles.resolveActiveFile(
            temporaryDirectory.toFile(),
            "2026-06",
            1024,
            temporaryDirectory.resolve("2026-06.1.log").toFile(),
            "2026-06",
            1,
            2048,
        )

        result.path.name shouldBe "2026-06.2.log"
        result.index shouldBe 2
    }

    @Test
    fun `keeps a cached file below the cap`() {
        val file = temporaryDirectory.resolve("2026-06.1.log").toFile()

        LogFiles.resolveActiveFile(temporaryDirectory.toFile(), "2026-06", 1024, file, "2026-06", 1, 500) shouldBe
            ActiveLogFile(file, "2026-06", 1, 500)
    }

    @Test
    fun `purges expired files and keeps unrelated names`() {
        temporaryDirectory.resolve("2025-11.log").createFile()
        temporaryDirectory.resolve("2025-12.log").createFile()
        temporaryDirectory.resolve("notes.log").createFile()

        LogFiles.purgeExpired(temporaryDirectory.toFile(), Now, ZoneOffset.UTC, 6) shouldBe 1
        temporaryDirectory.resolve("2025-11.log").exists() shouldBe false
        temporaryDirectory.resolve("2025-12.log").exists() shouldBe true
        temporaryDirectory.resolve("notes.log").exists() shouldBe true
    }

    @Test
    fun `purging a missing directory removes nothing`() {
        LogFiles.purgeExpired(temporaryDirectory.resolve("missing").toFile(), Now, ZoneOffset.UTC, 6) shouldBe 0
    }

    @ParameterizedTest
    @ValueSource(strings = ["ERROR", "DEBUG", "INFO"])
    fun `formats the bracketed level and arrow`(level: String) {
        LogFiles.formatLine("2026-06-15 10:30:00", level, "msg") shouldBe
            "2026-06-15 10:30:00 [$level] -> msg"
    }

    @Test
    fun `preserves the message verbatim`() {
        LogFiles.formatLine("2026-06-15 10:30:00", "INFO", "path=C:\\foo\\bar.txt")
            .shouldEndWith("path=C:\\foo\\bar.txt")
    }

    @Test
    fun `keeps the level and arrow for an empty message`() {
        LogFiles.formatLine("2026-06-15 10:30:00", "ERROR", "")
            .shouldEndWith("[ERROR] -> ")
    }

    @Test
    fun `separates timestamp and level with one space`() {
        val line = LogFiles.formatLine("2026-06-15 10:30:00", "INFO", "hello")

        line[19] shouldBe ' '
        line[20] shouldBe '['
    }

    private fun writeBytes(fileName: String, count: Int) {
        val path = temporaryDirectory.resolve(fileName)
        path.createFile()
        path.writeBytes(ByteArray(count))
    }

    private companion object {
        val Now: Instant = Instant.parse("2026-06-15T10:30:00Z")
    }
}
