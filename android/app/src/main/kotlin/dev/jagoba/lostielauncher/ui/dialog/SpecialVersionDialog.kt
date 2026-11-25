package dev.jagoba.lostielauncher.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Composable
fun SpecialVersionDialog(
    key: String,
    onKeyChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    keyError: String? = null,
) {
    val strings = LocalStrings.current
    val confirm = { if (key.isNotBlank()) onConfirm() }
    LauncherDialog(
        icon = R.drawable.ic_dialog_key,
        title = strings.specialVersionDialogTitle,
        width = LauncherSizes.SpecialVersionDialogWidth,
        minHeight = LauncherSizes.SpecialVersionDialogHeight,
        maxHeight = Dp.Infinity,
        onDismiss = onDismiss,
        centerBody = true,
        secure = true,
        protectFromOverlays = true,
        footer = {
            DialogFooterButtons {
                DialogButton(
                    label = strings.btnConfirm,
                    style = DialogButtonStyle.ACCENT,
                    width = LauncherSizes.DialogConfirmButtonWidth,
                    onClick = confirm,
                )
                DialogCancelButton(onDismiss)
            }
        },
    ) {
        Column(Modifier.padding(horizontal = LauncherSpacing.Screen)) {
            Text(
                text = strings.specialVersionDialogDescription,
                color = LocalLauncherColors.current.secondaryFgDim,
                fontSize = LauncherType.CaptionSize,
                modifier = Modifier.padding(bottom = LauncherSpacing.Screen),
            )
            Column(Modifier.padding(bottom = LauncherSpacing.ExtraSmall)) {
                DialogKeyField(
                    label = strings.specialVersionDialogKeyLabel,
                    value = key,
                    onValueChange = onKeyChange,
                    onDone = confirm,
                    error = keyError,
                )
            }
        }
    }
}
