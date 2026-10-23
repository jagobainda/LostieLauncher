package dev.jagoba.lostielauncher.util.policy

import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("StartupWindowPolicy")
class StartupWindowPolicyTest {
    @Test
    fun `keeps the starting window before settings load`() {
        StartupWindowPolicy.shouldKeep(false, 1_999, 2.seconds) shouldBe true
    }

    @Test
    fun `releases the starting window when settings load`() {
        StartupWindowPolicy.shouldKeep(true, 1, 2.seconds) shouldBe false
    }

    @Test
    fun `releases the starting window when the timeout expires`() {
        StartupWindowPolicy.shouldKeep(false, 2_000, 2.seconds) shouldBe false
    }
}
