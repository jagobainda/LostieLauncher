package dev.jagoba.lostielauncher.ui.screen

import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.DownloadDestination
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxButtons
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxIcon
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxSpec
import dev.jagoba.lostielauncher.ui.viewmodel.GamesNotice
import dev.jagoba.lostielauncher.ui.viewmodel.GamesUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryGameUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryNotice
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryUiState
import dev.jagoba.lostielauncher.ui.viewmodel.testGame
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DialogMessagesTest {
    private val strings = stringsFor(AppLanguage.ENG)
    private val target = GameTarget(null, "Test Game")
    private val location = GameLocationReference("token", "/games/Test Game/save")

    @Test
    fun `library notices map to the desktop message boxes`() {
        LibraryNotice.SERVER_ACTIONS_UNAVAILABLE.messageBox(strings) shouldBe MessageBoxSpec(
            strings.serverActionsUnavailableTitle,
            strings.serverActionsUnavailableMessage,
            MessageBoxButtons.OK,
            MessageBoxIcon.INFORMATION,
        )
        LibraryNotice.DOWNLOAD_PERMISSION_DENIED.messageBox(strings) shouldBe error(
            strings.downloadPermissionDeniedTitle,
            strings.downloadPermissionDeniedMessage,
        )
        LibraryNotice.INSTALLATION_HASH_MISMATCH.messageBox(strings) shouldBe
            error(strings.hashMismatchTitle, strings.hashMismatchMessage)
        LibraryNotice.SPECIAL_KEY_MISMATCH.messageBox(strings) shouldBe
            error(strings.downloadKeyMismatchTitle, strings.downloadKeyMismatchMessage)
        listOf(
            LibraryNotice.DOWNLOAD_BUSY,
            LibraryNotice.DOWNLOAD_INVALID,
            LibraryNotice.DOWNLOAD_NOT_FOUND,
            LibraryNotice.DOWNLOAD_FAILED,
            LibraryNotice.INSTALLATION_FAILED,
            LibraryNotice.SPECIAL_DOWNLOAD_ERROR,
        ).forEach { it.messageBox(strings) shouldBe error(strings.downloadErrorTitle, strings.downloadErrorMessage) }
    }

    @Test
    fun `cancelling a download asks yes or no`() {
        cancelDownloadMessageBox(strings).buttons shouldBe MessageBoxButtons.YES_NO
    }

    @Test
    fun `uninstall confirmation names the game and switches to the warning when it may be running`() {
        val confirm = gamesMessage(GamesUiState(pendingUninstall = target), strings)!!
        val warning = gamesMessage(
            GamesUiState(pendingUninstall = target, notice = GamesNotice.POSSIBLY_RUNNING),
            strings,
        )!!

        confirm.confirmation shouldBe GamesConfirmation.UNINSTALL
        confirm.spec.icon shouldBe MessageBoxIcon.INFORMATION
        confirm.spec.message shouldContain "Test Game"
        warning.confirmation shouldBe GamesConfirmation.UNINSTALL
        warning.spec.icon shouldBe MessageBoxIcon.ERROR
        warning.spec.title shouldBe strings.uninstallGameRunningTitle
        warning.spec.message shouldContain "Test Game"
    }

    @Test
    fun `leftover files offer to open the blocking location and name game and path`() {
        val message = gamesMessage(
            GamesUiState(
                notice = GamesNotice.FILES_LEFT_BEHIND,
                noticeGameName = "Test Game",
                blockingLocation = location,
            ),
            strings,
        )!!

        message.confirmation shouldBe GamesConfirmation.OPEN_BLOCKING_LOCATION
        message.spec.buttons shouldBe MessageBoxButtons.YES_NO
        message.spec.message shouldContain "Test Game"
        message.spec.message shouldContain "/games/Test Game/save"
    }

    @Test
    fun `missing folder offers the download and unsupported explains itself`() {
        gamesMessage(GamesUiState(notice = GamesNotice.LOCATION_NOT_FOUND), strings)!!.confirmation shouldBe
            GamesConfirmation.DOWNLOAD_MISSING
        gamesMessage(GamesUiState(notice = GamesNotice.NOT_SUPPORTED_YET), strings)!!.spec shouldBe MessageBoxSpec(
            strings.statusNotSupportedYet,
            strings.notSupportedYetMessage,
            MessageBoxButtons.OK,
            MessageBoxIcon.INFORMATION,
        )
    }

    @Test
    fun `no notice and no prompt shows nothing`() {
        gamesMessage(GamesUiState(), strings) shouldBe null
    }

    @ParameterizedTest
    @EnumSource(AppLanguage::class)
    fun `no produced message leaves a placeholder behind`(language: AppLanguage) {
        val localized = stringsFor(language)
        val specs = LibraryNotice.entries.map { it.messageBox(localized) } +
            GamesNotice.entries.mapNotNull { notice ->
                gamesMessage(
                    GamesUiState(notice = notice, noticeGameName = "Test Game", blockingLocation = location),
                    localized,
                )?.spec
            } +
            gamesMessage(GamesUiState(pendingUninstall = target), localized)!!.spec +
            gamesMessage(
                GamesUiState(pendingUninstall = target, notice = GamesNotice.POSSIBLY_RUNNING),
                localized,
            )!!.spec

        specs.forEach { it.message shouldNotContain "{" }
    }

    @Test
    fun `download confirmation waits for the destination and formats it`() {
        val game = testGame().copy(description = "About", pageUrl = "")
        val row = LibraryGameUiState(
            game = game,
            installed = null,
            installedStateKnown = false,
            hasUpdate = false,
            playtimeMinutes = 0,
            download = null,
            installation = null,
            status = LibraryCardStatus.AVAILABLE,
            remainingTime = null,
            canStart = true,
            canUpdate = false,
            canSwitchSpecialVersion = true,
        )
        val pending = LibraryUiState(isLoading = false, games = listOf(row), pendingDownloadGameId = game.gameId)

        downloadConfirmState(pending) shouldBe null
        val state = downloadConfirmState(
            pending.copy(downloadDestination = DownloadDestination("/games/downloads", null)),
        )!!
        state.path shouldBe "/games/downloads"
        state.freeSpace shouldBe "—"
        state.hasPage shouldBe false
        state.size shouldBe game.formattedSize
    }

    private fun error(title: String, message: String) =
        MessageBoxSpec(title, message, MessageBoxButtons.OK, MessageBoxIcon.ERROR)
}
