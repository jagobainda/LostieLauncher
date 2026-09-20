package dev.jagoba.lostielauncher.util.policy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Ported declaration for declaration from the desktop's `Utils/UnhandledExceptionPolicyTests.cs`. */
@DisplayName("UnhandledExceptionPolicy.decide")
class UnhandledExceptionPolicyTest {
    @Test
    fun `before startup completed, it is fatal`() {
        // Arrange — before startup completes there is nothing on screen, so swallowing the
        // failure leaves something the user can neither see nor close. Stopping is the only
        // honest outcome.
        // Act
        val action = UnhandledExceptionPolicy.decide(startupCompleted = false)

        // Assert
        action shouldBe UnhandledExceptionAction.Fatal
    }

    @Test
    fun `after startup completed, it keeps the app alive`() {
        // Arrange — a single runtime glitch must not take the user's session with it.
        // Act
        val action = UnhandledExceptionPolicy.decide(startupCompleted = true)

        // Assert
        action shouldBe UnhandledExceptionAction.KeepAlive
    }
}
