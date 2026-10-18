package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.SpecialVersionConfig
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionLookup
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionService
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.presentation.CatalogueData
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.ui.viewmodel.testGame
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InstallDownloadedFileHandoff")
class InstallDownloadedFileHandoffTest {
    private val installer = mockk<GameInstallationService>()
    private val coordinator = mockk<LauncherDataCoordinator>()
    private val content = mockk<ContentService>()
    private val specialVersions = mockk<SpecialVersionService>()
    private val logger = mockk<Logger>(relaxed = true)
    private val request = slot<GameInstallationRequest>()
    private val game = testGame(name = "Demo")

    private fun createSut(catalogue: CatalogueData = CatalogueData(isLoading = false, games = listOf(game))) =
        InstallDownloadedFileHandoff(installer, coordinator, content, specialVersions, logger).also {
            every { coordinator.catalogue } returns MutableStateFlow(catalogue)
            coEvery { installer.install(capture(request)) } returns GameInstallationResult.ExtractionFailed
        }

    @Test
    fun `resolves the catalogue id and hash of a regular download from the loaded catalogue`() = runTest {
        val file = DownloadedFile("demo", "Demo", "v1", null, "downloads/demo.zip")
        val sut = createSut()

        sut.deliver(file)

        request.captured shouldBe GameInstallationRequest(file, game.id, game.sha256, null)
        coVerify(exactly = 0) { content.getGames() }
        verify(exactly = 1) { logger.info(match { it.contains("ExtractionFailed") }) }
    }

    @Test
    fun `fetches the catalogue when the loaded one does not contain the game`() = runTest {
        val file = DownloadedFile("demo", "Demo", "v1", null, "downloads/demo.zip")
        coEvery { content.getGames() } returns listOf(game)
        val sut = createSut(CatalogueData())

        sut.deliver(file)

        request.captured.catalogueId shouldBe game.id
        request.captured.expectedSha256 shouldBe game.sha256
    }

    @Test
    fun `uses the special version hash and type when the download has a key`() = runTest {
        val file = DownloadedFile("demo", "Demo", "v1", "special", "downloads/demo.zip")
        val config = SpecialVersionConfig("b".repeat(64), "Nuzlocke", requireNotNull(game.id), "v1", "demo.zip")
        coEvery { specialVersions.lookup("special") } returns SpecialVersionLookup.Found(config)
        val sut = createSut()

        sut.deliver(file)

        request.captured shouldBe GameInstallationRequest(file, game.id, config.sha256, config.type)
    }

    @Test
    fun `passes no hash when neither the catalogue nor a special version provides one`() = runTest {
        val file = DownloadedFile("demo", "Demo", "v1", "special", "downloads/demo.zip")
        coEvery { content.getGames() } returns emptyList()
        coEvery { specialVersions.lookup("special") } returns SpecialVersionLookup.NotFound
        val sut = createSut(CatalogueData())

        sut.deliver(file)

        request.captured shouldBe GameInstallationRequest(file, null, null, null)
    }
}
