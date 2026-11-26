package dev.jagoba.lostielauncher.util.format

import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("FreeSpaceFormatter")
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

    @ParameterizedTest
    @CsvSource(
        "1.0, 1 GB",
        "1.5, 1.5 GB",
        "0.25, 256 MB",
        "0.5889, 603 MB",
        "0.0, 0 MB",
    )
    fun `formats gigabyte sizes with a 1024 factor and optional decimal`(gigabytes: Double, expected: String) {
        FreeSpaceFormatter.formatGigabytes(gigabytes) shouldBe expected
    }

    @Test
    fun `formats gigabyte sizes with a dot whatever the device locale is`() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
        try {
            FreeSpaceFormatter.formatGigabytes(1.5) shouldBe "1.5 GB"
            FreeSpaceFormatter.formatGigabytes(1.0) shouldBe "1 GB"
        } finally {
            Locale.setDefault(original)
        }
    }
}
