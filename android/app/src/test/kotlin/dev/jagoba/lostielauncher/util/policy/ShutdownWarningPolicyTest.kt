package dev.jagoba.lostielauncher.util.policy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Ported declaration for declaration from the desktop's `Utils/ShutdownWarningPolicyTests.cs`. */
@DisplayName("ShutdownWarningPolicy.decide")
class ShutdownWarningPolicyTest {
    @Test
    fun `when idle, warns about nothing`() {
        // Act
        val warning = ShutdownWarningPolicy.decide(isDownloading = false, isGameRunning = false)

        // Assert
        warning shouldBe ShutdownWarning.None
    }

    @Test
    fun `when downloading, warns about the download`() {
        // Act
        val warning = ShutdownWarningPolicy.decide(isDownloading = true, isGameRunning = false)

        // Assert
        warning shouldBe ShutdownWarning.Download
    }

    @Test
    fun `when a game is running, warns about the game`() {
        // Act
        val warning = ShutdownWarningPolicy.decide(isDownloading = false, isGameRunning = true)

        // Assert
        warning shouldBe ShutdownWarning.Game
    }

    @Test
    fun `when downloading and playing, warns about both`() {
        // Act
        val warning = ShutdownWarningPolicy.decide(isDownloading = true, isGameRunning = true)

        // Assert
        warning shouldBe ShutdownWarning.Both
    }
}
