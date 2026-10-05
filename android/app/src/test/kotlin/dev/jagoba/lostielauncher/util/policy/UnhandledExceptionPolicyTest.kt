package dev.jagoba.lostielauncher.util.policy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UnhandledExceptionPolicy.decide")
class UnhandledExceptionPolicyTest {
    @Test
    fun `before startup completed, it is fatal`() {
        val action = UnhandledExceptionPolicy.decide(startupCompleted = false)

        action shouldBe UnhandledExceptionAction.Fatal
    }

    @Test
    fun `after startup completed, it keeps the app alive`() {
        val action = UnhandledExceptionPolicy.decide(startupCompleted = true)

        action shouldBe UnhandledExceptionAction.KeepAlive
    }
}
