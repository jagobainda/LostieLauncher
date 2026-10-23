package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.content.strings.EngStrings
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DownloadNotificationContent")
class DownloadNotificationContentTest {
    @Test
    fun `shows percent, speed and remaining time while downloading`() {
        val progress = DownloadProgress(
            percent = 45.4,
            bytesPerSecond = 2 * MIB,
            downloadedBytes = 45 * MIB.toLong(),
            totalBytes = 100 * MIB.toLong(),
        )

        val content = downloadNotificationContent("Demo", DownloadNotificationPhase.Downloading(progress), EngStrings)

        content shouldBe DownloadNotificationContent(
            title = "Demo",
            status = "Downloading...",
            detail = "45% · 2.0 MB/s · 27s",
            percent = 45,
            indeterminate = false,
        )
    }

    @Test
    fun `leaves out the speed and remaining time before the first sample`() {
        val progress = DownloadProgress(25.0, 0.0, downloadedBytes = 25, totalBytes = 100)

        val content = downloadNotificationContent("Demo", DownloadNotificationPhase.Downloading(progress), EngStrings)

        content.detail shouldBe "25%"
    }

    @Test
    fun `is indeterminate and shows only the speed when the size is unknown`() {
        val progress = DownloadProgress(0.0, 2048.0, downloadedBytes = 4096, totalBytes = null)

        val content = downloadNotificationContent("Demo", DownloadNotificationPhase.Downloading(progress), EngStrings)

        content.detail shouldBe "2.0 KB/s"
        content.indeterminate shouldBe true
    }

    @Test
    fun `has no detail and an indeterminate bar while verifying`() {
        val content = downloadNotificationContent("Demo", DownloadNotificationPhase.Verifying, EngStrings)

        content shouldBe DownloadNotificationContent("Demo", "Verifying integrity...", null, 0, true)
    }

    @Test
    fun `shows the extraction percent`() {
        val content = downloadNotificationContent("Demo", DownloadNotificationPhase.Extracting(40), EngStrings)

        content shouldBe DownloadNotificationContent("Demo", "Extracting...", "40%", 40, false)
    }

    @Test
    fun `is indeterminate while the extraction percent is unknown`() {
        val content = downloadNotificationContent("Demo", DownloadNotificationPhase.Extracting(null), EngStrings)

        content shouldBe DownloadNotificationContent("Demo", "Extracting...", null, 0, true)
    }

    @Test
    fun `maps only the running installation phases`() {
        GameInstallationState.NotStarted.notificationPhase() shouldBe null
        GameInstallationState.VerifyingIntegrity.notificationPhase() shouldBe DownloadNotificationPhase.Verifying
        GameInstallationState.Extracting(10).notificationPhase() shouldBe DownloadNotificationPhase.Extracting(10)
        GameInstallationState.Finished(GameInstallationResult.HashMismatch).notificationPhase() shouldBe null
    }

    private companion object {
        const val MIB = 1_048_576.0
    }
}
