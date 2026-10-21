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
    onDismissFailure: () -> Unit,
) {
    val spec = launcherUpdateMessageBox(state, LocalStrings.current) ?: return
    val failed = state.launcherUpdateFailed
    LauncherMessageBox(
        spec = spec,
        onConfirm = if (failed) onDismissFailure else onAccept,
        onDismiss = if (failed) onDismissFailure else onDecline,
    )
}

internal fun launcherUpdateMessageBox(state: MainUiState, strings: Strings): MessageBoxSpec? = when {
    state.launcherUpdateFailed -> errorMessageBox(strings.downloadErrorTitle, strings.downloadErrorMessage)

    state.launcherUpdatePrompt != null -> MessageBoxSpec(
        strings.updateAvailableTitle,
        strings.updateAvailableMessage.withArgs(state.launcherUpdatePrompt),
        MessageBoxButtons.YES_NO,
        MessageBoxIcon.UPDATE,
    )

    else -> null
}

internal fun Strings.downloadingLabel(percent: Int) = "$statusDownloading $percent%"
