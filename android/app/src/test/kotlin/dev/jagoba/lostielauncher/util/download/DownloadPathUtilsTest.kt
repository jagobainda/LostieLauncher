package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.GameDownloadArgs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Ported declaration for declaration from the desktop's `Utils/DownloadPathUtilsTests.cs`.
 *
 * The path cases use an Android-shaped directory instead of a Windows one. Both functions
 * are string concatenation, so the shape proves nothing either way; using a path that could
 * not exist on the target would just be misleading.
 */
@DisplayName("DownloadPathUtils")
class DownloadPathUtilsTest {
    private val downloadsDirectory = "/data/user/0/dev.jagoba.lostielauncher/files/downloads"
    private val specialKey = "ABCD-EFGH-IJKL-MNOP-QRST"

    // ---- computeToken ----

    @Test
    fun `the token is stable for the same version and key`() {
        // Arrange & Act
        val first = DownloadPathUtils.computeToken("1.0.0", specialKey)
        val second = DownloadPathUtils.computeToken("1.0.0", specialKey)

        // Assert — a resumed download has to derive the exact partial file the paused one wrote.
        first shouldBe second
    }

    @Test
    fun `a special version and the standard one of the same version get different tokens`() {
        // Arrange — same game, same version: one keyed, one not. They must not share a
        // partial file, or the bytes of two different archives end up interleaved.
        // Act
        val standard = DownloadPathUtils.computeToken("1.0.0", key = null)
        val special = DownloadPathUtils.computeToken("1.0.0", key = specialKey)

        // Assert
        standard shouldNotBe special
    }

    @Test
    fun `different versions get different tokens`() {
        // Arrange & Act
        val first = DownloadPathUtils.computeToken("1.0.0", key = null)
        val second = DownloadPathUtils.computeToken("2.0.0", key = null)

        // Assert
        first shouldNotBe second
    }

    @Test
    fun `the token is lowercase hex and safe in a file name`() {
        // Arrange & Act
        val token = DownloadPathUtils.computeToken("1.0.0-beta+meta", key = null)

        // Assert — eight bytes are sixteen hex characters, none of them hostile to a path.
        token.length shouldBe 16
        token.all { it in '0'..'9' || it in 'a'..'f' } shouldBe true
    }

    // ---- getZipFileName ----

    @Test
    fun `the archive name embeds the game id and the discriminating token`() {
        // Arrange
        val args = GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip")

        // Act
        val name = DownloadPathUtils.getZipFileName(args)

        // Assert
        name shouldStartWith "cool-game."
        name shouldEndWith ".zip"
    }

    @Test
    fun `a special version and the standard one of the same game do not collide`() {
        // Arrange — same game and version, one standard and one keyed.
        val standard = GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip")
        val special = GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip", specialKey)

        // Act & Assert — different names mean different partial files, so no byte mixing.
        DownloadPathUtils.getZipFileName(standard) shouldNotBe DownloadPathUtils.getZipFileName(special)
    }

    // ---- getPartFilePath / getMetaFilePath ----

    @Test
    fun `the partial file appends the part extension`() {
        // Arrange & Act
        val partPath = DownloadPathUtils.getPartFilePath("$downloadsDirectory/cool-game.abcd1234.zip")

        // Assert
        partPath shouldBe "$downloadsDirectory/cool-game.abcd1234.zip.part"
    }

    @Test
    fun `the metadata file appends the meta extension to the partial path`() {
        // Arrange & Act
        val metaPath = DownloadPathUtils.getMetaFilePath("$downloadsDirectory/cool-game.abcd1234.zip.part")

        // Assert
        metaPath shouldBe "$downloadsDirectory/cool-game.abcd1234.zip.part.meta"
    }

    @Test
    fun `the writer and the cleaner derive the same partial and metadata paths`() {
        // Arrange — whoever writes the transfer and whoever later cleans the cache must derive
        // the SAME names from the final archive, or a cleanup misses the sidecar.
        val finalPath = "$downloadsDirectory/cool-game.abcd1234.zip"

        // Act — both sides go through the shared helpers.
        val partPath = DownloadPathUtils.getPartFilePath(finalPath)
        val metaPath = DownloadPathUtils.getMetaFilePath(partPath)

        // Assert
        partPath shouldBe "$finalPath.part"
        metaPath shouldBe "$finalPath.part.meta"
    }

    // ---- Android-only: the token is the same number on both runtimes ----

    @Test
    fun `the token matches the value the desktop computes`() {
        // Arrange — the digest is written out rather than recomputed, because the point is
        // cross-runtime agreement: a cache filled by one side has to be readable by the same
        // rules on the other. Both are the first eight bytes of the SHA-256 of "<version>|<key>".
        // Act & Assert
        DownloadPathUtils.computeToken("1.0.0", key = null) shouldBe "e2ef44fdadfdb1b5"
        DownloadPathUtils.computeToken("1.0.0", specialKey) shouldBe "8385d97948eda59e"
    }
}
