package dev.jagoba.lostielauncher.util.format

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RemainingTimeFormatterTest {
    @Test
    fun `formats seconds minutes and hours from download progress`() {
        RemainingTimeFormatter.format(9, 1.0) shouldBe "9s"
        RemainingTimeFormatter.format(125, 1.0) shouldBe "2m 5s"
        RemainingTimeFormatter.format(3665, 1.0) shouldBe "1h 1m"
    }

    @Test
    fun `unknown speed or finished transfer has no remaining time`() {
        RemainingTimeFormatter.format(100, 0.0) shouldBe null
        RemainingTimeFormatter.format(100, Double.NaN) shouldBe null
        RemainingTimeFormatter.format(0, 1.0) shouldBe null
    }
}
