package dev.jagoba.lostielauncher.util.format

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("FreeSpaceFormatter.format")
class FreeSpaceFormatterTest {
    @ParameterizedTest
    @CsvSource(
        "1073741824, 1 GB",
        "1610612736, 1.5 GB",
        "68719476736, 64 GB",
        "1181116006, 1.1 GB",
    )
    fun `at or above one gigabyte, shows one optional decimal with a dot`(bytes: Long, expected: String) {
        val text = FreeSpaceFormatter.format(bytes)

        text shouldBe expected
    }

    @ParameterizedTest
    @CsvSource("536870912, 512 MB", "1048576, 1 MB", "0, 0 MB")
    fun `below one gigabyte, shows whole megabytes`(bytes: Long, expected: String) {
        val text = FreeSpaceFormatter.format(bytes)

        text shouldBe expected
    }

    @Test
    fun `when the space cannot be queried, shows an em dash`() {
        val text = FreeSpaceFormatter.format(null)

        text shouldBe "—"
    }
}
