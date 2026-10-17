package dev.jagoba.lostielauncher.util.download

import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DownloadProgressCalculator")
class DownloadProgressCalculatorTest {
    @Test
    fun `calculates absolute progress including resumed bytes`() {
        val progress = DownloadProgressCalculator.calculate(75, 100, 20.0)

        progress.percent.shouldBeExactly(75.0)
        progress.downloadedBytes shouldBe 75
        progress.totalBytes shouldBe 100
        progress.bytesPerSecond.shouldBeExactly(20.0)
    }

    @Test
    fun `reports no percentage when total size is unknown`() {
        val progress = DownloadProgressCalculator.calculate(75, null, 20.0)

        progress.percent.shouldBeExactly(0.0)
        progress.totalBytes shouldBe null
    }

    @Test
    fun `clamps values that cannot form valid progress`() {
        val progress = DownloadProgressCalculator.calculate(-5, 0, -1.0)

        progress.percent.shouldBeExactly(0.0)
        progress.downloadedBytes shouldBe 0
        progress.totalBytes shouldBe null
        progress.bytesPerSecond.shouldBeExactly(0.0)
    }

    @Test
    fun `samples speed only at the configured interval`() {
        val sampler = DownloadSpeedSampler(500_000_000, 100, 1_000_000_000)

        sampler.sample(150, 1_250_000_000) shouldBe null
        sampler.sample(300, 1_500_000_000)?.shouldBeExactly(400.0)
        sampler.sample(350, 1_600_000_000) shouldBe null
    }
}
