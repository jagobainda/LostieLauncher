package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.dialog.DownloadConfirmDialog
import dev.jagoba.lostielauncher.ui.dialog.DownloadConfirmState
import dev.jagoba.lostielauncher.ui.dialog.LauncherMessageBox
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxButtons
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxIcon
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxSpec
import dev.jagoba.lostielauncher.ui.dialog.SpecialVersionDialog
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryNotice
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryPermissionPrompt
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryViewModel
import dev.jagoba.lostielauncher.util.format.FreeSpaceFormatter

@Composable
fun LibraryDialogs(viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val notice = state.notice
    val download = downloadConfirmState(state)
    val permissionPrompt = state.permissionPrompt
    val notifications = rememberNotificationPermissionRequest(viewModel::onNotificationPermissionResult)
    val storageAccess = rememberStorageAccessRequest(viewModel::onStorageAccessResult)
    when {
        notice != null -> LauncherMessageBox(
            spec = notice.messageBox(strings),
            onConfirm = viewModel::clearNotice,
            onDismiss = viewModel::clearNotice,
        )

        download != null -> DownloadConfirmDialog(
            state = download,
            key = state.pendingKey,
            onKeyChange = viewModel::updatePendingKey,
            onViewPage = viewModel::openPendingGamePage,
            onConfirm = { viewModel.confirmDownload() },
            onDismiss = viewModel::dismissPrompt,
            keyError = strings.downloadKeyInvalidMessage.takeIf { state.isKeyInvalid },
        )

        permissionPrompt != null -> LauncherMessageBox(
            spec = permissionPrompt.messageBox(strings),
            onConfirm = {
                when (permissionPrompt) {
                    LibraryPermissionPrompt.STORAGE_RATIONALE -> {
                        viewModel.requestStorageAccess()
                        storageAccess.requestPermission()
                    }

                    LibraryPermissionPrompt.STORAGE_SETTINGS -> {
                        viewModel.requestStorageAccess()
                        storageAccess.openSettings()
                    }

                    LibraryPermissionPrompt.NOTIFICATION_RATIONALE -> {
                        viewModel.requestNotificationPermission()
                        notifications.requestPermission()
                    }

                    LibraryPermissionPrompt.NOTIFICATION_SETTINGS -> {
                        viewModel.requestNotificationPermission()
                        notifications.openSettings()
                    }
                }
            },
            onDismiss = {
                when (permissionPrompt) {
                    LibraryPermissionPrompt.STORAGE_RATIONALE,
                    LibraryPermissionPrompt.STORAGE_SETTINGS,
                    -> viewModel.dismissPrompt()

                    LibraryPermissionPrompt.NOTIFICATION_RATIONALE,
                    LibraryPermissionPrompt.NOTIFICATION_SETTINGS,
                    -> viewModel.skipNotificationPermission()
                }
            },
        )

        state.pendingCancelGameId != null -> LauncherMessageBox(
            spec = cancelDownloadMessageBox(strings),
            onConfirm = viewModel::confirmCancel,
            onDismiss = viewModel::dismissPrompt,
        )

        state.pendingSpecialGameId != null -> SpecialVersionDialog(
            key = state.pendingKey,
            onKeyChange = viewModel::updatePendingKey,
            onConfirm = viewModel::submitSpecialVersionKey,
            onDismiss = viewModel::dismissPrompt,
            keyError = strings.downloadKeyInvalidMessage.takeIf { state.isKeyInvalid },
        )
    }
}

internal fun downloadConfirmState(state: LibraryUiState): DownloadConfirmState? {
    val game = state.pendingDownloadGame ?: return null
    val destination = state.downloadDestination ?: return null
    return DownloadConfirmState(
        title = game.name,
        description = game.description,
        logoUrl = game.logoUrl,
        hasPage = game.pageUrl.isNotBlank(),
        path = destination.path,
        size = FreeSpaceFormatter.formatGigabytes(game.sizeGb),
        freeSpace = FreeSpaceFormatter.format(destination.freeBytes),
    )
}

internal fun cancelDownloadMessageBox(strings: Strings) = MessageBoxSpec(
    strings.cancelDownloadConfirmTitle,
    strings.cancelDownloadConfirmMessage,
    MessageBoxButtons.YES_NO,
    MessageBoxIcon.INFORMATION,
    protectFromOverlays = true,
)

internal fun LibraryPermissionPrompt.messageBox(strings: Strings): MessageBoxSpec = when (this) {
    LibraryPermissionPrompt.STORAGE_RATIONALE -> MessageBoxSpec(
        strings.storageAccessTitle,
        strings.storageAccessMessage,
        MessageBoxButtons.YES_NO,
        MessageBoxIcon.INFORMATION,
        protectFromOverlays = true,
    )

    LibraryPermissionPrompt.STORAGE_SETTINGS -> MessageBoxSpec(
        strings.storageAccessTitle,
        strings.storageAccessSettingsMessage,
        MessageBoxButtons.YES_NO,
        MessageBoxIcon.INFORMATION,
        protectFromOverlays = true,
    )

    LibraryPermissionPrompt.NOTIFICATION_RATIONALE -> MessageBoxSpec(
        strings.notificationPermissionTitle,
        strings.notificationPermissionMessage,
        MessageBoxButtons.YES_NO,
        MessageBoxIcon.INFORMATION,
        protectFromOverlays = true,
    )

    LibraryPermissionPrompt.NOTIFICATION_SETTINGS -> MessageBoxSpec(
        strings.notificationPermissionTitle,
        strings.notificationPermissionSettingsMessage,
        MessageBoxButtons.YES_NO,
        MessageBoxIcon.INFORMATION,
        protectFromOverlays = true,
    )
}

internal fun LibraryNotice.messageBox(strings: Strings): MessageBoxSpec = when (this) {
    LibraryNotice.SERVER_ACTIONS_UNAVAILABLE -> MessageBoxSpec(
        strings.serverActionsUnavailableTitle,
        strings.serverActionsUnavailableMessage,
        icon = MessageBoxIcon.INFORMATION,
    )

    LibraryNotice.OFFLINE -> MessageBoxSpec(
        strings.offlineModeLabel,
        strings.serverMaintenanceNotificationMessage,
        icon = MessageBoxIcon.INFORMATION,
    )

    LibraryNotice.DOWNLOAD_BUSY,
    LibraryNotice.DOWNLOAD_INVALID,
    LibraryNotice.DOWNLOAD_NOT_FOUND,
    LibraryNotice.DOWNLOAD_FAILED,
    LibraryNotice.SPECIAL_DOWNLOAD_ERROR,
    -> errorMessageBox(strings.downloadErrorTitle, strings.downloadErrorMessage)

    LibraryNotice.INSTALLATION_FAILED ->
        errorMessageBox(strings.installationErrorTitle, strings.installationErrorMessage)

    LibraryNotice.DOWNLOAD_PERMISSION_DENIED ->
        errorMessageBox(strings.downloadPermissionDeniedTitle, strings.downloadPermissionDeniedMessage)

    LibraryNotice.STORAGE_ACCESS_DENIED ->
        errorMessageBox(strings.storageAccessTitle, strings.storageAccessDeniedMessage)

    LibraryNotice.INSTALLATION_HASH_MISMATCH -> errorMessageBox(strings.hashMismatchTitle, strings.hashMismatchMessage)

    LibraryNotice.SPECIAL_KEY_NOT_FOUND ->
        errorMessageBox(strings.downloadKeyNotFoundTitle, strings.downloadKeyNotFoundMessage)

    LibraryNotice.SPECIAL_KEY_MISMATCH ->
        errorMessageBox(strings.downloadKeyMismatchTitle, strings.downloadKeyMismatchMessage)

    LibraryNotice.SPECIAL_VERSION_BUSY -> MessageBoxSpec(
        strings.updateCheckBusyTitle,
        strings.specialVersionBusyMessage,
        icon = MessageBoxIcon.INFORMATION,
    )
}

internal fun errorMessageBox(
    title: String,
    message: String,
    buttons: MessageBoxButtons = MessageBoxButtons.OK,
    protectFromOverlays: Boolean = false,
) = MessageBoxSpec(title, message, buttons, MessageBoxIcon.ERROR, protectFromOverlays)
