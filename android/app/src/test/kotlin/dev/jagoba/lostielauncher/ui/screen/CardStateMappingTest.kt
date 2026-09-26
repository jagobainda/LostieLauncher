package dev.jagoba.lostielauncher.ui.screen

import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.ui.viewmodel.InstalledGameUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryGameUiState
import dev.jagoba.lostielauncher.ui.viewmodel.testDownload
import dev.jagoba.lostielauncher.ui.viewmodel.testGame
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.Test

class CardStateMappingTest {
    private val game = testGame(version = "v2.0")

    @Test
    fun `downloading library row carries progress speed and remaining time`() {
        val card = libraryRow(DownloadStatus.DOWNLOADING, LibraryCardStatus.DOWNLOADING).toCardState()
        card.title shouldBe game.name
        card.size shouldBe game.formattedSize
        card.version shouldBe "v2.0"
        card.playtime shouldBe "2 h 5 min"
        card.progress shouldBe 50f
        card.speed shouldBe "0.0 KB/s"
        card.remainingTime shouldBe "10s"
        card.canPause shouldBe true
        card.canCancel shouldBe true
    }

    @Test
    fun `paused library row has no speed and keeps its progress`() {
        val card = libraryRow(DownloadStatus.PAUSED, LibraryCardStatus.PAUSED).toCardState()
        card.speed shouldBe null
        card.progress shouldBe 50f
        card.canPause shouldBe false
    }

    @Test
    fun `library row without transfer starts at zero`() {
        val row = libraryRow(null, LibraryCardStatus.AVAILABLE)
        val card = row.copy(download = null, playtimeMinutes = 0).toCardState()
        card.progress shouldBe 0f
        card.speed shouldBe null
        card.playtime shouldBe ""
        card.canCancel shouldBe false
    }

    @Test
    fun `installed row shows the catalogue version only when it is an update`() {
        installedRow(hasUpdate = true).toCardState(runtimeSupported = true).updateVersion shouldBe "v2.0"
        installedRow(hasUpdate = false).toCardState(runtimeSupported = true).updateVersion shouldBe null
    }

    @Test
    fun `installed row maps guards help and runtime support`() {
        val card = installedRow(hasUpdate = false, help = GameHelpAvailability.NOT_SUPPORTED_YET)
            .copy(isUpdating = true)
            .toCardState(runtimeSupported = false)
        card.title shouldBe "Local Name"
        card.installedVersion shouldBe "v1.0"
        card.specialType shouldBe "NUZLOCKE"
        card.logoUrl shouldBe game.logoUrl
        card.showHelp shouldBe true
        card.canOpenFolder shouldBe false
        card.canSwitchSpecialVersion shouldBe false
        card.canUninstall shouldBe true
        card.runtimeSupported shouldBe false
        installedRow(hasUpdate = false, help = GameHelpAvailability.NOT_FOUND)
            .toCardState(runtimeSupported = true).showHelp shouldBe false
    }

    private fun libraryRow(status: DownloadStatus?, cardStatus: LibraryCardStatus) = LibraryGameUiState(
        game = game,
        installed = null,
        installedStateKnown = false,
        hasUpdate = false,
        playtimeMinutes = 125,
        download = testDownload(game, status ?: DownloadStatus.PAUSED),
        installation = null,
        status = cardStatus,
        remainingTime = "10s",
        canStart = cardStatus == LibraryCardStatus.AVAILABLE,
        canUpdate = false,
        canSwitchSpecialVersion = true,
    )

    private fun installedRow(hasUpdate: Boolean, help: GameHelpAvailability = GameHelpAvailability.AVAILABLE) =
        InstalledGameUiState(
            game = LocalGame(UUID.randomUUID(), "Local Name", "v1.0", "NUZLOCKE"),
            remote = game,
            hasUpdate = hasUpdate,
            playtimeMinutes = 0,
            helpAvailability = help,
            isUpdating = false,
            isUninstalling = false,
        )
}
