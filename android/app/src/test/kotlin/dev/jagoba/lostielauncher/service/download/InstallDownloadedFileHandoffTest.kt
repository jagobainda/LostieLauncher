package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InstallDownloadedFileHandoff")
class InstallDownloadedFileHandoffTest {
    private val installer = mockk<GameInstallationService>()
    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `routes a completed download through the pending install seam`() = runTest {
        val file = DownloadedFile("demo", "Demo", "v1", "special", "downloads/demo.zip")
        val request = slot<GameInstallationRequest>()
        coEvery { installer.install(capture(request)) } returns GameInstallationResult.NotSupportedYet
        val sut = InstallDownloadedFileHandoff(installer, logger)

        sut.deliver(file)

        coVerify(exactly = 1) { installer.install(any()) }
        request.captured.file shouldBe file
        request.captured.catalogueId shouldBe null
        request.captured.expectedSha256 shouldBe null
        request.captured.variant shouldBe null
        verify(exactly = 1) { logger.info(match { it.contains("NotSupportedYet") }) }
    }
}
