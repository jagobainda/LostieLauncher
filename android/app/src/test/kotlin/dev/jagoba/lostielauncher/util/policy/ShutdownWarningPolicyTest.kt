package dev.jagoba.lostielauncher.util.policy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ShutdownWarningPolicy.decide")
class ShutdownWarningPolicyTest {
    @Test
    fun `when idle, warns about nothing`() {
        val warning = ShutdownWarningPolicy.decide(isDownloading = false, isGameRunning = false)

        warning shouldBe ShutdownWarning.None
    }

    @Test
    fun `when downloading, warns about the download`() {
        val warning = ShutdownWarningPolicy.decide(isDownloading = true, isGameRunning = false)

        warning shouldBe ShutdownWarning.Download
    }

    @Test
    fun `when a game is running, warns about the game`() {
        val warning = ShutdownWarningPolicy.decide(isDownloading = false, isGameRunning = true)

        warning shouldBe ShutdownWarning.Game
    }

    @Test
    fun `when downloading and playing, warns about both`() {
        val warning = ShutdownWarningPolicy.decide(isDownloading = true, isGameRunning = true)

        warning shouldBe ShutdownWarning.Both
    }
}
