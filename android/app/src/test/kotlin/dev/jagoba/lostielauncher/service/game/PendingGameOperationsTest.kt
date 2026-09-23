package dev.jagoba.lostielauncher.service.game

import app.cash.turbine.test
import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameActivityState
import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameLocation
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GamePlaySession
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.GameUninstallResult
import dev.jagoba.lostielauncher.model.InstalledGameResult
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.OpenGameLocationResult
import dev.jagoba.lostielauncher.model.PlaySessionResult
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Pending game operations")
class PendingGameOperationsTest {
    private val logger = mockk<Logger>(relaxed = true)
    private val game = GameTarget(UUID.fromString("11111111-2222-3333-4444-555555555555"), "Demo")

    @Test
    fun `installation returns unsupported and preserves its input`() = runTest {
        val file = DownloadedFile("demo", "Demo", "v1", null, "downloads/demo.zip")
        val request = GameInstallationRequest(file, game.id, "abc", null)
        val sut = PendingGameInstallationService(logger)

        sut.install(request) shouldBe GameInstallationResult.NotSupportedYet
        verify(exactly = 1) { logger.info(match { it.contains("installation requested for demo") }) }
    }

    @Test
    fun `uninstall returns unsupported without claiming that files were removed`() = runTest {
        val sut = PendingGameInstallationService(logger)

        sut.uninstall(game) shouldBe GameUninstallResult(GameUninstallOutcome.NOT_SUPPORTED_YET)
        verify(exactly = 1) { logger.info(match { it.contains("uninstall requested for Demo") }) }
    }

    @Test
    fun `installed lookup does not mistake unsupported for absent`() = runTest {
        val sut = PendingGameInstallationService(logger)

        sut.findInstalled(game) shouldBe InstalledGameResult.NotSupportedYet
        verify(exactly = 1) { logger.info(match { it.contains("lookup requested for Demo") }) }
    }

    @Test
    fun `installed list remains explicitly unsupported instead of appearing empty`() = runTest {
        val sut = PendingGameInstallationService(logger)

        sut.installedGames.test {
            awaitItem() shouldBe InstalledGamesState.NotSupportedYet
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `installation phases replay the unsupported outcome for any game`() = runTest {
        val sut = PendingGameInstallationService(logger)

        sut.observeInstallation("demo").test {
            awaitItem() shouldBe GameInstallationState.Finished(GameInstallationResult.NotSupportedYet)
            awaitComplete()
        }
        verify(exactly = 1) { logger.info(match { it.contains("observation requested for demo") }) }
    }

    @Test
    fun `launch returns unsupported without claiming it launched`() = runTest {
        val sut = PendingGameLaunchService(logger)

        sut.launch(game) shouldBe GameLaunchResult.NotSupportedYet
        verify(exactly = 1) { logger.info(match { it.contains("launch requested for Demo") }) }
    }

    @Test
    fun `running lookup does not claim a game is stopped`() = runTest {
        val sut = PendingGameLaunchService(logger)

        sut.activeSessions.value shouldBe GameActivityState.NotSupportedYet
        sut.runningSignal(game) shouldBe GameRunningSignal.NOT_SUPPORTED_YET
        verify(exactly = 1) { logger.info(match { it.contains("Running-state lookup requested for Demo") }) }
    }

    @Test
    fun `play session is not recorded while game tracking is unsupported`() = runTest {
        val session = GamePlaySession(game.id ?: UUID(0, 0), Instant.EPOCH, Instant.EPOCH.plusSeconds(120))
        val sut = PendingPlaySessionService(logger)

        sut.recordCompletedSession(session) shouldBe PlaySessionResult.NotSupportedYet
        verify(exactly = 1) { logger.info(match { it.contains("Play-session accounting requested") }) }
    }

    @Test
    fun `both game and help locations return unsupported`() = runTest {
        val sut = PendingGameLocationService(logger)

        sut.open(game, GameLocation.GAME) shouldBe OpenGameLocationResult.NotSupportedYet
        sut.open(game, GameLocation.HELP) shouldBe OpenGameLocationResult.NotSupportedYet
        verify(exactly = 2) { logger.info(match { it.contains("Game location") }) }
    }

    @Test
    fun `help availability and partial uninstall location remain explicit`() = runTest {
        val sut = PendingGameLocationService(logger)
        val blocker = GameLocationReference("opaque-blocker", "Leftover game files")

        sut.helpAvailability(game) shouldBe GameHelpAvailability.NOT_SUPPORTED_YET
        sut.openBlockingLocation(blocker) shouldBe OpenGameLocationResult.NotSupportedYet
        verify(exactly = 1) { logger.info(match { it.contains("Help availability requested for Demo") }) }
        verify(exactly = 1) { logger.info(match { it.contains("Uninstall blocker location requested") }) }
    }
}
