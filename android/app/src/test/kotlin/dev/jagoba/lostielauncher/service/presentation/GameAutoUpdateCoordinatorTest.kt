package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.ui.viewmodel.TestContentService
import dev.jagoba.lostielauncher.ui.viewmodel.TestDownloads
import dev.jagoba.lostielauncher.ui.viewmodel.TestInstallation
import dev.jagoba.lostielauncher.ui.viewmodel.TestSettingsStore
import dev.jagoba.lostielauncher.ui.viewmodel.testDownload
import dev.jagoba.lostielauncher.ui.viewmodel.testGame
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameAutoUpdateCoordinatorTest {
    private val content = TestContentService()
    private val downloads = TestDownloads()
    private val installation = TestInstallation()
    private val settings = TestSettingsStore()
    private val logger = mockk<Logger>(relaxed = true)
    private val launcher = LauncherDataCoordinator(content, downloads, logger)
    private val sut = GameAutoUpdateCoordinator(settings, launcher, installation, downloads, content, logger)

    @Test
    fun `disabled setting leaves installed games untouched`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)),
        )
        launcher.refreshCatalogue()
        sut.start(backgroundScope)
        runCurrent()
        downloads.starts shouldBe emptyList()
    }

    @Test
    fun `regular outdated games update sequentially after initial catalogue load`() = runTest {
        val first = testGame(name = "First")
        val second = testGame(name = "Second")
        val current = testGame(name = "Current")
        val special = testGame(name = "Special")
        content.games = listOf(first, second, current, special)
        installation.current.value = InstalledGamesState.Available(
            listOf(
                LocalGame(first.id!!, first.name, "v1.0", null),
                LocalGame(second.id!!, second.name, "v1.0", null),
                LocalGame(current.id!!, current.name, current.version, null),
                LocalGame(special.id!!, special.name, "v1.0", "seasonal"),
            ),
        )
        settings.setAutoUpdate(true)
        sut.start(backgroundScope)
        runCurrent()
        downloads.starts shouldBe emptyList()
        launcher.refreshCatalogue()
        runCurrent()
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId)
        downloads.current.value = listOf(testDownload(first, DownloadStatus.COMPLETED))
        runCurrent()
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId, second.gameId)
    }

    @Test
    fun `maintenance blocks automatic updates`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        content.blocked = true
        installation.current.value = InstalledGamesState.Available(
            listOf(LocalGame(remote.id!!, remote.name, "v1.0", null)),
        )
        settings.setAutoUpdate(true)
        launcher.refreshCatalogue()
        sut.start(backgroundScope)
        runCurrent()
        downloads.starts shouldBe emptyList()
    }
}
