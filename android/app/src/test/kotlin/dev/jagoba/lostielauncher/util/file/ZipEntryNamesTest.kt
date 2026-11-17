package dev.jagoba.lostielauncher.util.file

import io.kotest.matchers.shouldBe
import java.util.zip.ZipEntry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("ZipEntryNames.resolve")
class ZipEntryNamesTest {
    private fun entry(extra: ByteArray) = ZipEntry(HEADER_NAME).apply { this.extra = extra }

    @Test
    fun `keeps the header name of an entry without extra fields`() {
        ZipEntryNames.resolve(ZipEntry(HEADER_NAME)) shouldBe HEADER_NAME
    }

    @Test
    fun `prefers a Unicode Path field whose checksum matches the header name in the legacy code page`() {
        val entry = entry(OTHER_FIELD + unicodePathField(HEADER_NAME, UNICODE_NAME.toByteArray()))

        ZipEntryNames.resolve(entry) shouldBe UNICODE_NAME
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unusableFields")
    fun `keeps the header name when the Unicode Path field is not usable`(reason: String, extra: ByteArray) {
        ZipEntryNames.resolve(entry(extra)) shouldBe HEADER_NAME
    }

    private companion object {
        const val HEADER_NAME = "Audio/BGM/Tensión.mp3"
        const val UNICODE_NAME = "Audio/BGM/Tensión 夜.mp3"
        val OTHER_FIELD = byteArrayOf(0xFE.toByte(), 0xCA.toByte(), 2, 0, 1, 2)

        @JvmStatic
        fun unusableFields() = listOf(
            arrayOf("no Unicode Path among the fields", OTHER_FIELD),
            arrayOf(
                "checksum of another header name",
                unicodePathField("Audio/BGM/Renamed.mp3", UNICODE_NAME.toByteArray()),
            ),
            arrayOf("unsupported version", unicodePathField(HEADER_NAME, UNICODE_NAME.toByteArray(), version = 2)),
            arrayOf("empty name", unicodePathField(HEADER_NAME, ByteArray(0))),
            arrayOf("malformed UTF-8", unicodePathField(HEADER_NAME, byteArrayOf(0xC3.toByte(), 0x28))),
            arrayOf(
                "field longer than the extra data",
                unicodePathField(HEADER_NAME, UNICODE_NAME.toByteArray()).dropLast(1).toByteArray(),
            ),
        )
    }
}
