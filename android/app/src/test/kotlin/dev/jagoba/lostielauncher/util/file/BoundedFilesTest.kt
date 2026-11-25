package dev.jagoba.lostielauncher.util.file

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("BoundedFiles")
class BoundedFilesTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `reads a file whose size matches the limit`() {
        val file = directory.resolve("exact.json").toFile().apply { writeText("12345") }

        BoundedFiles.readText(file, maxBytes = 5) shouldBe "12345"
    }

    @Test
    fun `reads an empty file when the limit is zero`() {
        val file = directory.resolve("empty.json").toFile().apply { writeText("") }

        BoundedFiles.readText(file, maxBytes = 0) shouldBe ""
    }

    @Test
    fun `rejects a file larger than the limit`() {
        val file = directory.resolve("oversized.json").toFile().apply { writeText("123456") }

        shouldThrow<IOException> { BoundedFiles.readText(file, maxBytes = 5) }
    }
}
