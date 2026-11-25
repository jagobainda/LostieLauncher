package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.runtime.Composable
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.withArgs
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.dialog.LauncherMessageBox
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxButtons
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxIcon
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxSpec
import dev.jagoba.lostielauncher.ui.viewmodel.MainUiState

@Composable
fun LauncherUpdateDialogs(
    state: MainUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onInstallPermissionResult: () -> Unit,
    onDismissPermission: () -> Unit,
    onDismissFailure: () -> Unit,
) {
    val openInstallSettings = rememberUnknownSourcesPermissionRequest(onInstallPermissionResult)
    val spec = launcherUpdateMessageBox(state, LocalStrings.current) ?: return
    when {
        state.launcherUpdateFailed -> LauncherMessageBox(
            spec = spec,
            onConfirm = onDismissFailure,
            onDismiss = onDismissFailure,
        )

        state.launcherUpdatePermissionRequired -> LauncherMessageBox(
            spec = spec,
            onConfirm = openInstallSettings,
            onDismiss = onDismissPermission,
        )

        else -> LauncherMessageBox(
            spec = spec,
            onConfirm = onAccept,
            onDismiss = onDecline,
        )
    }
}

internal fun launcherUpdateMessageBox(state: MainUiState, strings: Strings): MessageBoxSpec? = when {
    state.launcherUpdateFailed -> errorMessageBox(strings.downloadErrorTitle, strings.downloadErrorMessage)

    state.launcherUpdatePermissionRequired -> installPermissionMessageBox(strings)

    state.launcherUpdateCancelled -> installCancelledMessageBox(strings)

    state.launcherUpdatePrompt != null -> MessageBoxSpec(
        strings.updateAvailableTitle,
        strings.updateAvailableMessage.withArgs(state.launcherUpdatePrompt),
        MessageBoxButtons.YES_NO,
        MessageBoxIcon.UPDATE,
        protectFromOverlays = true,
    )

    else -> null
}

internal fun installPermissionMessageBox(strings: Strings) = MessageBoxSpec(
    strings.installPermissionTitle,
    strings.installPermissionMessage,
    MessageBoxButtons.YES_NO,
    MessageBoxIcon.INFORMATION,
    protectFromOverlays = true,
)

internal fun installCancelledMessageBox(strings: Strings) = MessageBoxSpec(
    strings.apkInstallCancelledTitle,
    strings.apkInstallCancelledMessage,
    MessageBoxButtons.YES_NO,
    MessageBoxIcon.INFORMATION,
    protectFromOverlays = true,
)

internal fun Strings.downloadingLabel(percent: Int) = "$statusDownloading $percent%"
