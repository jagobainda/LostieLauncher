package dev.jagoba.lostielauncher.ui.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

enum class MessageBoxButtons { OK, YES_NO }

enum class MessageBoxIcon { UPDATE, INFORMATION, ERROR }

@Immutable
data class MessageBoxSpec(
    val title: String,
    val message: String,
    val buttons: MessageBoxButtons = MessageBoxButtons.OK,
    val icon: MessageBoxIcon = MessageBoxIcon.UPDATE,
)

@Composable
fun LauncherMessageBox(spec: MessageBoxSpec, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    LauncherDialog(
        icon = when (spec.icon) {
            MessageBoxIcon.UPDATE -> R.drawable.ic_card_update
            MessageBoxIcon.INFORMATION -> R.drawable.ic_dialog_information
            MessageBoxIcon.ERROR -> R.drawable.ic_dialog_alert
        },
        title = spec.title,
        width = LauncherSizes.MessageBoxWidth,
        minHeight = LauncherSizes.MessageBoxMinHeight,
        maxHeight = LauncherSizes.MessageBoxMaxHeight,
        onDismiss = onDismiss,
        centerBody = true,
        footer = {
            DialogFooterButtons {
                when (spec.buttons) {
                    MessageBoxButtons.OK -> DialogButton(
                        label = strings.btnOk,
                        style = DialogButtonStyle.ACCENT,
                        width = LauncherSizes.DialogOkButtonWidth,
                        onClick = onConfirm,
                    )

                    MessageBoxButtons.YES_NO -> {
                        DialogButton(
                            label = strings.btnYes,
                            style = DialogButtonStyle.ACCENT,
                            width = LauncherSizes.DialogYesNoButtonWidth,
                            onClick = onConfirm,
                        )
                        DialogButton(
                            label = strings.btnNo,
                            style = DialogButtonStyle.SECONDARY,
                            width = LauncherSizes.DialogYesNoButtonWidth,
                            onClick = onDismiss,
                        )
                    }
                }
            }
        },
    ) {
        Text(
            text = spec.message,
            color = LocalLauncherColors.current.secondaryFg,
            fontSize = LauncherType.BodySize,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LauncherSpacing.Screen),
        )
    }
}
