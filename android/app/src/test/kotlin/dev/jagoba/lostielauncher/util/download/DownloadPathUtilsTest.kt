package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.GameDownloadArgs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DownloadPathUtils")
class DownloadPathUtilsTest {
    private val downloadsDirectory = "/data/user/0/dev.jagoba.lostielauncher/files/downloads"
    private val specialKey = "ABCD-EFGH-IJKL-MNOP-QRST"

    @Test
    fun `the token is stable for the same version and key`() {
        val first = DownloadPathUtils.computeToken("1.0.0", specialKey)
        val second = DownloadPathUtils.computeToken("1.0.0", specialKey)

        first shouldBe second
    }

    @Test
    fun `a special version and the standard one of the same version get different tokens`() {
        val standard = DownloadPathUtils.computeToken("1.0.0", key = null)
        val special = DownloadPathUtils.computeToken("1.0.0", key = specialKey)

        standard shouldNotBe special
    }

    @Test
    fun `different versions get different tokens`() {
        val first = DownloadPathUtils.computeToken("1.0.0", key = null)
        val second = DownloadPathUtils.computeToken("2.0.0", key = null)

        first shouldNotBe second
    }

    @Test
    fun `the token is lowercase hex and safe in a file name`() {
        val token = DownloadPathUtils.computeToken("1.0.0-beta+meta", key = null)

        token.length shouldBe 16
        token.all { it in '0'..'9' || it in 'a'..'f' } shouldBe true
    }

    @Test
    fun `the archive name embeds the game id and the discriminating token`() {
        val args = GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip")

        val name = DownloadPathUtils.getZipFileName(args)

        name shouldStartWith "cool-game."
        name shouldEndWith ".zip"
    }

    @Test
    fun `a special version and the standard one of the same game do not collide`() {
        val standard = GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip")
        val special = GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip", specialKey)

        DownloadPathUtils.getZipFileName(standard) shouldNotBe DownloadPathUtils.getZipFileName(special)
    }

    @Test
    fun `the partial file appends the part extension`() {
        val partPath = DownloadPathUtils.getPartFilePath("$downloadsDirectory/cool-game.abcd1234.zip")

        partPath shouldBe "$downloadsDirectory/cool-game.abcd1234.zip.part"
    }

    @Test
    fun `the metadata file appends the meta extension to the partial path`() {
        val metaPath = DownloadPathUtils.getMetaFilePath("$downloadsDirectory/cool-game.abcd1234.zip.part")

        metaPath shouldBe "$downloadsDirectory/cool-game.abcd1234.zip.part.meta"
    }

    @Test
    fun `the writer and the cleaner derive the same partial and metadata paths`() {
        val finalPath = "$downloadsDirectory/cool-game.abcd1234.zip"

        val partPath = DownloadPathUtils.getPartFilePath(finalPath)
        val metaPath = DownloadPathUtils.getMetaFilePath(partPath)

        partPath shouldBe "$finalPath.part"
        metaPath shouldBe "$finalPath.part.meta"
    }

    @Test
    fun `the token matches the value the desktop computes`() {
        DownloadPathUtils.computeToken("1.0.0", key = null) shouldBe "e2ef44fdadfdb1b5"
        DownloadPathUtils.computeToken("1.0.0", specialKey) shouldBe "8385d97948eda59e"
    }
}
