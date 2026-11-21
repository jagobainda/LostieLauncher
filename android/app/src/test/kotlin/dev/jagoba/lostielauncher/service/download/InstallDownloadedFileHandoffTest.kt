package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.LocalGame
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
import java.io.IOException
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
    fun `uses the pinned hash instead of a changed catalogue hash for a regular download`() = runTest {
        val expectedSha256 = "b".repeat(64)
        val file = DownloadedFile(
            "demo",
            "Demo",
            "v1",
            null,
            "downloads/demo.zip",
            expectedSha256 = expectedSha256,
        )
        val sut = createSut()

        sut.deliver(file) shouldBe GameInstallationResult.ExtractionFailed

        request.captured shouldBe GameInstallationRequest(file, game.id, expectedSha256, null)
        coVerify(exactly = 0) { content.getGames() }
        coVerify(exactly = 0) { specialVersions.lookup(any()) }
        verify(exactly = 1) { logger.info(match { it.contains("ExtractionFailed") }) }
    }

    @Test
    fun `fetches the catalogue when the loaded one does not contain the game`() = runTest {
        val file = DownloadedFile(
            "demo",
            "Demo",
            "v1",
            null,
            "downloads/demo.zip",
            expectedSha256 = game.sha256,
        )
        coEvery { content.getGames() } returns listOf(game)
        val sut = createSut(CatalogueData())

        sut.deliver(file)

        request.captured.catalogueId shouldBe game.id
        request.captured.expectedSha256 shouldBe game.sha256
    }

    @Test
    fun `installs a special download with its pinned hash and type when the lookup would fail`() = runTest {
        val config = SpecialVersionConfig("b".repeat(64), "Nuzlocke", requireNotNull(game.id), "v1", "demo.zip")
        val file = DownloadedFile(
            "demo",
            "Demo",
            "v1",
            "special",
            "downloads/demo.zip",
            expectedSha256 = config.sha256,
            variant = config.type,
        )
        coEvery { specialVersions.lookup("special") } throws IOException("offline")
        val installed = GameInstallationResult.Installed(
            LocalGame(requireNotNull(game.id), game.name, config.version, config.type),
        )
        val sut = createSut()
        coEvery { installer.install(capture(request)) } returns installed

        sut.deliver(file) shouldBe installed

        request.captured shouldBe GameInstallationRequest(file, game.id, config.sha256, config.type)
        coVerify(exactly = 0) { specialVersions.lookup(any()) }
    }

    @Test
    fun `resolves the special version at handoff only for a legacy row`() = runTest {
        val file = DownloadedFile("demo", "Demo", "v1", "special", "downloads/demo.zip")
        val config = SpecialVersionConfig("b".repeat(64), "Nuzlocke", requireNotNull(game.id), "v1", "demo.zip")
        coEvery { specialVersions.lookup("special") } returns SpecialVersionLookup.Found(config)
        val sut = createSut()

        sut.deliver(file)

        request.captured shouldBe GameInstallationRequest(file, game.id, config.sha256, config.type)
        coVerify(exactly = 1) { specialVersions.lookup("special") }
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
