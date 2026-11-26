package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.ui.viewmodel.TestContentService
import dev.jagoba.lostielauncher.ui.viewmodel.TestDownloads
import dev.jagoba.lostielauncher.ui.viewmodel.TestInstallation
import dev.jagoba.lostielauncher.ui.viewmodel.TestNetworkMonitor
import dev.jagoba.lostielauncher.ui.viewmodel.TestSettingsStore
import dev.jagoba.lostielauncher.ui.viewmodel.installedAs
import dev.jagoba.lostielauncher.ui.viewmodel.testDownload
import dev.jagoba.lostielauncher.ui.viewmodel.testGame
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("GameAutoUpdateCoordinator")
class GameAutoUpdateCoordinatorTest {
    private val content = TestContentService()
    private val downloads = TestDownloads()
    private val installation = TestInstallation()
    private val settings = TestSettingsStore()
    private val logger = mockk<Logger>(relaxed = true)
    private val launcher = LauncherDataCoordinator(
        content,
        downloads,
        { false },
        settings,
        TestNetworkMonitor(),
        HomeRefreshOptions(2.minutes),
        logger,
    )
    private val sut = GameAutoUpdateCoordinator(settings, launcher, installation, downloads, content, logger)

    @Test
    fun `disabled setting leaves installed games untouched`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(listOf(installedAs(remote, "v1.0")))
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
                installedAs(first, "v1.0"),
                installedAs(second, "v1.0"),
                installedAs(current),
                installedAs(special, "v1.0", "seasonal"),
            ),
        )
        settings.setAutoUpdate(true)
        sut.start(backgroundScope)
        runCurrent()
        downloads.starts shouldBe emptyList()
        launcher.refreshCatalogue()
        runCurrent()
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId)
        downloads.current.value = listOf(testDownload(first, DownloadStatus.INSTALLING))
        runCurrent()
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId)
        downloads.current.value = listOf(testDownload(first, DownloadStatus.COMPLETED))
        runCurrent()
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId, second.gameId)
        downloads.starts.all { it.requiresUnmeteredNetwork } shouldBe true
    }

    @Test
    fun `disabling auto update cancels the active automatic update and stops remaining updates`() = runTest {
        val first = testGame(name = "First")
        val second = testGame(name = "Second")
        content.games = listOf(first, second)
        installation.current.value = InstalledGamesState.Available(
            listOf(installedAs(first, "v1.0"), installedAs(second, "v1.0")),
        )
        settings.setAutoUpdate(true)
        launcher.refreshCatalogue()
        sut.start(backgroundScope)
        runCurrent()
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId)

        settings.setAutoUpdate(false)
        runCurrent()
        downloads.current.value = listOf(testDownload(first, DownloadStatus.CANCELLED))
        runCurrent()

        downloads.cancelled shouldBe listOf(first.gameId)
        downloads.starts.map { it.args.gameId } shouldBe listOf(first.gameId)
    }

    @Test
    fun `disabling auto update does not cancel an unrelated active download`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(listOf(installedAs(remote, "v1.0")))
        downloads.result = DownloadCommandResult.BUSY
        settings.setAutoUpdate(true)
        launcher.refreshCatalogue()
        sut.start(backgroundScope)
        runCurrent()

        settings.setAutoUpdate(false)
        runCurrent()

        downloads.cancelled shouldBe emptyList()
    }

    @Test
    fun `disabling auto update after updates finish does not cancel completed games`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(listOf(installedAs(remote, "v1.0")))
        settings.setAutoUpdate(true)
        launcher.refreshCatalogue()
        sut.start(backgroundScope)
        runCurrent()
        downloads.current.value = listOf(testDownload(remote, DownloadStatus.COMPLETED))
        runCurrent()

        settings.setAutoUpdate(false)
        runCurrent()

        downloads.cancelled shouldBe emptyList()
    }

    @Test
    fun `enabling auto update during the session starts pending updates`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(listOf(installedAs(remote, "v1.0")))
        launcher.refreshCatalogue()
        sut.start(backgroundScope)
        runCurrent()
        downloads.starts shouldBe emptyList()

        settings.setAutoUpdate(true)
        runCurrent()

        downloads.starts.map { it.args.gameId } shouldBe listOf(remote.gameId)
        downloads.starts.single().requiresUnmeteredNetwork shouldBe true
    }

    @Test
    fun `re-enabling auto update retries a previously cancelled update`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(listOf(installedAs(remote, "v1.0")))
        settings.setAutoUpdate(true)
        launcher.refreshCatalogue()
        sut.start(backgroundScope)
        runCurrent()

        settings.setAutoUpdate(false)
        runCurrent()
        settings.setAutoUpdate(true)
        runCurrent()

        downloads.cancelled shouldBe listOf(remote.gameId)
        downloads.starts.map { it.args.gameId } shouldBe listOf(remote.gameId, remote.gameId)
    }

    @Test
    fun `maintenance blocks automatic updates`() = runTest {
        val remote = testGame()
        content.games = listOf(remote)
        installation.current.value = InstalledGamesState.Available(listOf(installedAs(remote, "v1.0")))
        settings.setAutoUpdate(true)
        launcher.refreshCatalogue()
        content.blocked = true
        sut.start(backgroundScope)
        runCurrent()
        downloads.starts shouldBe emptyList()
    }
}
