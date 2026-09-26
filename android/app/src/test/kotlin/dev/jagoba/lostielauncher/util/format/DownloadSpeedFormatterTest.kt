package dev.jagoba.lostielauncher.util.format

import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.jupiter.api.Test

class DownloadSpeedFormatterTest {
    @Test
    fun `formats megabytes kilobytes and zero like the desktop`() {
        DownloadSpeedFormatter.format(1_572_864.0) shouldBe "1.5 MB/s"
        DownloadSpeedFormatter.format(1_048_576.0) shouldBe "1.0 MB/s"
        DownloadSpeedFormatter.format(5 * 1_048_576.0) shouldBe "5.0 MB/s"
        DownloadSpeedFormatter.format(2048.0) shouldBe "2.0 KB/s"
        DownloadSpeedFormatter.format(512.0) shouldBe "0.5 KB/s"
        DownloadSpeedFormatter.format(0.0) shouldBe "0 KB/s"
        DownloadSpeedFormatter.format(-1.0) shouldBe "0 KB/s"
    }

    @Test
    fun `ignores the default locale decimal separator`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            DownloadSpeedFormatter.format(1_572_864.0) shouldBe "1.5 MB/s"
        } finally {
            Locale.setDefault(previous)
        }
    }
}
