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
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryPermissionPrompt
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryUiState
import dev.jagoba.lostielauncher.ui.viewmodel.MainUiState
import dev.jagoba.lostielauncher.ui.viewmodel.SettingsNotice
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
        LibraryNotice.OFFLINE.messageBox(strings) shouldBe MessageBoxSpec(
            strings.offlineModeLabel,
            strings.serverMaintenanceNotificationMessage,
            MessageBoxButtons.OK,
            MessageBoxIcon.INFORMATION,
        )
        LibraryNotice.DOWNLOAD_PERMISSION_DENIED.messageBox(strings) shouldBe error(
            strings.downloadPermissionDeniedTitle,
            strings.downloadPermissionDeniedMessage,
        )
        LibraryNotice.STORAGE_ACCESS_DENIED.messageBox(strings) shouldBe
            error(strings.storageAccessTitle, strings.storageAccessDeniedMessage)
        LibraryNotice.INSTALLATION_HASH_MISMATCH.messageBox(strings) shouldBe
            error(strings.hashMismatchTitle, strings.hashMismatchMessage)
        LibraryNotice.SPECIAL_KEY_MISMATCH.messageBox(strings) shouldBe
            error(strings.downloadKeyMismatchTitle, strings.downloadKeyMismatchMessage)
        LibraryNotice.SPECIAL_VERSION_BUSY.messageBox(strings) shouldBe MessageBoxSpec(
            strings.updateCheckBusyTitle,
            strings.specialVersionBusyMessage,
            MessageBoxButtons.OK,
            MessageBoxIcon.INFORMATION,
        )
        listOf(
            LibraryNotice.DOWNLOAD_BUSY,
            LibraryNotice.DOWNLOAD_INVALID,
            LibraryNotice.DOWNLOAD_NOT_FOUND,
            LibraryNotice.DOWNLOAD_FAILED,
            LibraryNotice.SPECIAL_DOWNLOAD_ERROR,
        ).forEach { it.messageBox(strings) shouldBe error(strings.downloadErrorTitle, strings.downloadErrorMessage) }
        LibraryNotice.INSTALLATION_FAILED.messageBox(strings) shouldBe
            error(strings.installationErrorTitle, strings.installationErrorMessage)
    }

    @Test
    fun `cancelling a download asks yes or no and protects against overlays`() {
        val spec = cancelDownloadMessageBox(strings)

        spec.buttons shouldBe MessageBoxButtons.YES_NO
        spec.protectFromOverlays shouldBe true
    }

    @Test
    fun `permission prompts explain themselves, ask yes or no and protect against overlays`() {
        LibraryPermissionPrompt.STORAGE_RATIONALE.messageBox(strings) shouldBe MessageBoxSpec(
            strings.storageAccessTitle,
            strings.storageAccessMessage,
            MessageBoxButtons.YES_NO,
            MessageBoxIcon.INFORMATION,
            protectFromOverlays = true,
        )
        LibraryPermissionPrompt.STORAGE_SETTINGS.messageBox(strings) shouldBe MessageBoxSpec(
            strings.storageAccessTitle,
            strings.storageAccessSettingsMessage,
            MessageBoxButtons.YES_NO,
            MessageBoxIcon.INFORMATION,
            protectFromOverlays = true,
        )
        LibraryPermissionPrompt.NOTIFICATION_RATIONALE.messageBox(strings) shouldBe MessageBoxSpec(
            strings.notificationPermissionTitle,
            strings.notificationPermissionMessage,
            MessageBoxButtons.YES_NO,
            MessageBoxIcon.INFORMATION,
            protectFromOverlays = true,
        )
        LibraryPermissionPrompt.NOTIFICATION_SETTINGS.messageBox(strings) shouldBe MessageBoxSpec(
            strings.notificationPermissionTitle,
            strings.notificationPermissionSettingsMessage,
            MessageBoxButtons.YES_NO,
            MessageBoxIcon.INFORMATION,
            protectFromOverlays = true,
        )
    }

    @Test
    fun `uninstall confirmation names the game, protects against overlays and switches to warning when running`() {
        val confirm = gamesMessage(GamesUiState(pendingUninstall = target), strings)!!
        val warning = gamesMessage(
            GamesUiState(pendingUninstall = target, notice = GamesNotice.POSSIBLY_RUNNING),
            strings,
        )!!

        confirm.confirmation shouldBe GamesConfirmation.UNINSTALL
        confirm.spec.icon shouldBe MessageBoxIcon.INFORMATION
        confirm.spec.message shouldContain "Test Game"
        confirm.spec.protectFromOverlays shouldBe true
        warning.confirmation shouldBe GamesConfirmation.UNINSTALL
        warning.spec.icon shouldBe MessageBoxIcon.ERROR
        warning.spec.title shouldBe strings.uninstallGameRunningTitle
        warning.spec.message shouldContain "Test Game"
        warning.spec.protectFromOverlays shouldBe true
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
    fun `integrity mismatch offers reinstall when a catalogue entry is known and falls back to ok otherwise`() {
        val reinstall = gamesMessage(
            GamesUiState(
                notice = GamesNotice.INTEGRITY_MISMATCH,
                noticeGameName = "Test Game",
                reinstallGameId = "test-game",
            ),
            strings,
        )!!
        val inform = gamesMessage(
            GamesUiState(
                notice = GamesNotice.INTEGRITY_MISMATCH,
                noticeGameName = "Test Game",
            ),
            strings,
        )!!

        reinstall.confirmation shouldBe GamesConfirmation.REINSTALL
        reinstall.spec.buttons shouldBe MessageBoxButtons.YES_NO
        reinstall.spec.icon shouldBe MessageBoxIcon.ERROR
        reinstall.spec.title shouldBe strings.hashMismatchTitle
        reinstall.spec.message shouldContain "Test Game"
        inform.confirmation shouldBe GamesConfirmation.ACKNOWLEDGE
        inform.spec.buttons shouldBe MessageBoxButtons.OK
        inform.spec.icon shouldBe MessageBoxIcon.ERROR
        inform.spec.message shouldContain "Test Game"
    }

    @Test
    fun `unsupported explains itself`() {
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
        launcherUpdateMessageBox(MainUiState(), strings) shouldBe null
    }

    @Test
    fun `a launcher update asks yes or no with the offered version and protects against overlays`() {
        launcherUpdateMessageBox(MainUiState(launcherUpdatePrompt = "1.1.0"), strings) shouldBe MessageBoxSpec(
            strings.updateAvailableTitle,
            "New version 1.1.0 available. Download and install?",
            MessageBoxButtons.YES_NO,
            MessageBoxIcon.UPDATE,
            protectFromOverlays = true,
        )
    }

    @Test
    fun `a failed launcher update download shows the download error before any prompt`() {
        launcherUpdateMessageBox(
            MainUiState(
                launcherUpdatePrompt = "1.1.0",
                launcherUpdatePermissionRequired = true,
                launcherUpdateCancelled = true,
                launcherUpdateFailed = true,
            ),
            strings,
        ) shouldBe error(strings.downloadErrorTitle, strings.downloadErrorMessage)
    }

    @Test
    fun `unknown-sources permission and cancelled installation dialogs ask yes or no and protect against overlays`() {
        launcherUpdateMessageBox(
            MainUiState(launcherUpdatePrompt = "1.1.0", launcherUpdatePermissionRequired = true),
            strings,
        ) shouldBe MessageBoxSpec(
            strings.installPermissionTitle,
            strings.installPermissionMessage,
            MessageBoxButtons.YES_NO,
            MessageBoxIcon.INFORMATION,
            protectFromOverlays = true,
        )
        launcherUpdateMessageBox(
            MainUiState(launcherUpdatePrompt = "1.1.0", launcherUpdateCancelled = true),
            strings,
        ) shouldBe MessageBoxSpec(
            strings.apkInstallCancelledTitle,
            strings.apkInstallCancelledMessage,
            MessageBoxButtons.YES_NO,
            MessageBoxIcon.INFORMATION,
            protectFromOverlays = true,
        )
    }

    @Test
    fun `settings notices map to the desktop update message boxes`() {
        SettingsNotice.UPDATE_CHECK_BUSY.messageBox(strings) shouldBe MessageBoxSpec(
            strings.updateCheckBusyTitle,
            strings.updateCheckBusyMessage,
            MessageBoxButtons.OK,
            MessageBoxIcon.INFORMATION,
        )
        SettingsNotice.UP_TO_DATE.messageBox(strings) shouldBe MessageBoxSpec(
            strings.upToDateTitle,
            strings.upToDateMessage,
            MessageBoxButtons.OK,
            MessageBoxIcon.INFORMATION,
        )
        SettingsNotice.UPDATE_CHECK_FAILED.messageBox(strings) shouldBe
            error(strings.updateCheckFailedTitle, strings.updateCheckFailedMessage)
    }

    @ParameterizedTest
    @EnumSource(AppLanguage::class)
    fun `no produced message leaves a placeholder behind`(language: AppLanguage) {
        val localized = stringsFor(language)
        val specs = LibraryNotice.entries.map { it.messageBox(localized) } +
            LibraryPermissionPrompt.entries.map { it.messageBox(localized) } +
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
            )!!.spec +
            launcherUpdateMessageBox(MainUiState(launcherUpdatePrompt = "1.1.0"), localized)!! +
            installPermissionMessageBox(localized) +
            installCancelledMessageBox(localized) +
            SettingsNotice.entries.map { it.messageBox(localized) }

        specs.forEach { it.message shouldNotContain "{" }
    }

    @Test
    fun `download confirmation waits for the destination and formats it`() {
        val game = testGame().copy(description = "About", pageUrl = "")
        val row = LibraryGameUiState(
            game = game,
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
