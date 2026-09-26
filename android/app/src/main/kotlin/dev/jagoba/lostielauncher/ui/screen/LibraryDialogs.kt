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
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryViewModel
import dev.jagoba.lostielauncher.util.format.FreeSpaceFormatter

@Composable
fun LibraryDialogs(viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val notice = state.notice
    val download = downloadConfirmState(state)
    when {
        notice != null -> LauncherMessageBox(
            spec = notice.messageBox(strings),
            onConfirm = viewModel::clearNotice,
            onDismiss = viewModel::clearNotice,
        )

        download != null -> DownloadConfirmDialog(
            state = download,
            onViewPage = viewModel::openPendingGamePage,
            onConfirm = viewModel::confirmDownload,
            onDismiss = viewModel::dismissPrompt,
        )

        state.pendingCancelGameId != null -> LauncherMessageBox(
            spec = cancelDownloadMessageBox(strings),
            onConfirm = viewModel::confirmCancel,
            onDismiss = viewModel::dismissPrompt,
        )

        state.pendingSpecialGameId != null -> SpecialVersionDialog(
            onConfirm = viewModel::submitSpecialVersionKey,
            onDismiss = viewModel::dismissPrompt,
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
        size = game.formattedSize,
        freeSpace = FreeSpaceFormatter.format(destination.freeBytes),
    )
}

internal fun cancelDownloadMessageBox(strings: Strings) = MessageBoxSpec(
    strings.cancelDownloadConfirmTitle,
    strings.cancelDownloadConfirmMessage,
    MessageBoxButtons.YES_NO,
    MessageBoxIcon.INFORMATION,
)

internal fun LibraryNotice.messageBox(strings: Strings): MessageBoxSpec = when (this) {
    LibraryNotice.SERVER_ACTIONS_UNAVAILABLE -> MessageBoxSpec(
        strings.serverActionsUnavailableTitle,
        strings.serverActionsUnavailableMessage,
        icon = MessageBoxIcon.INFORMATION,
    )

    LibraryNotice.DOWNLOAD_BUSY,
    LibraryNotice.DOWNLOAD_INVALID,
    LibraryNotice.DOWNLOAD_NOT_FOUND,
    LibraryNotice.DOWNLOAD_FAILED,
    LibraryNotice.INSTALLATION_FAILED,
    LibraryNotice.SPECIAL_DOWNLOAD_ERROR,
    -> errorMessageBox(strings.downloadErrorTitle, strings.downloadErrorMessage)

    LibraryNotice.DOWNLOAD_PERMISSION_DENIED ->
        errorMessageBox(strings.downloadPermissionDeniedTitle, strings.downloadPermissionDeniedMessage)

    LibraryNotice.INSTALLATION_HASH_MISMATCH -> errorMessageBox(strings.hashMismatchTitle, strings.hashMismatchMessage)

    LibraryNotice.SPECIAL_KEY_INVALID -> errorMessageBox(
        strings.downloadKeyInvalidTitle,
        strings.downloadKeyInvalidMessage,
    )

    LibraryNotice.SPECIAL_KEY_NOT_FOUND ->
        errorMessageBox(strings.downloadKeyNotFoundTitle, strings.downloadKeyNotFoundMessage)

    LibraryNotice.SPECIAL_KEY_MISMATCH ->
        errorMessageBox(strings.downloadKeyMismatchTitle, strings.downloadKeyMismatchMessage)
}

internal fun errorMessageBox(title: String, message: String, buttons: MessageBoxButtons = MessageBoxButtons.OK) =
    MessageBoxSpec(title, message, buttons, MessageBoxIcon.ERROR)
