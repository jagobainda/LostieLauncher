package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.GameDownloadArgs
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DownloadUrlResolver")
class DownloadUrlResolverTest {
    @Test
    fun `resolves a regular catalogue path below the download base`() {
        val args = GameDownloadArgs("demo", "1.0", "/demo/1-0.zip")

        DownloadUrlResolver.resolve("https://cdn.test/games", args) shouldBe
            "https://cdn.test/games/demo/1-0.zip"
    }

    @Test
    fun `places a special version key before its archive path`() {
        val args = GameDownloadArgs("demo", "1.0", "special.zip", "ABCD-EFGH")

        DownloadUrlResolver.resolve("https://cdn.test/games", args) shouldBe
            "https://cdn.test/games/ABCD-EFGH/special.zip"
    }

    @Test
    fun `rejects a relative path that can escape its base`() {
        val args = GameDownloadArgs("demo", "1.0", "../special.zip")

        shouldThrow<IllegalArgumentException> {
            DownloadUrlResolver.resolve("https://cdn.test/games", args)
        }
    }
}
