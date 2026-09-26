package dev.jagoba.lostielauncher.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.GameLogo
import dev.jagoba.lostielauncher.ui.component.LauncherTooltip
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Immutable
data class DownloadConfirmState(
    val title: String,
    val description: String,
    val logoUrl: String?,
    val hasPage: Boolean,
    val path: String,
    val size: String,
    val freeSpace: String,
)

@Composable
fun DownloadConfirmDialog(
    state: DownloadConfirmState,
    onViewPage: () -> Unit,
    onConfirm: (key: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = LocalLauncherColors.current
    var key by rememberSaveable { mutableStateOf("") }
    LauncherDialog(
        icon = R.drawable.ic_card_download,
        title = strings.downloadDialogTitle,
        width = LauncherSizes.DownloadDialogWidth,
        minHeight = LauncherSizes.DownloadDialogHeight,
        maxHeight = LauncherSizes.DownloadDialogHeight,
        onDismiss = onDismiss,
        footer = {
            DialogFooterButtons {
                DialogButton(
                    label = strings.btnDownload,
                    style = DialogButtonStyle.ACCENT,
                    width = LauncherSizes.DialogConfirmButtonWidth,
                    onClick = { onConfirm(key) },
                )
                DialogCancelButton(onDismiss)
            }
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LauncherSpacing.Screen),
        ) {
            Row(Modifier.padding(bottom = LauncherSpacing.Card)) {
                GameLogo(
                    url = state.logoUrl,
                    width = LauncherSizes.DownloadDialogLogoWidth,
                    height = LauncherSizes.DownloadDialogLogoHeight,
                    placeholderSize = LauncherSizes.DownloadDialogLogoPlaceholder,
                )
                Column(Modifier.padding(start = LauncherSpacing.ExtraLarge)) {
                    Text(
                        text = state.title,
                        color = colors.secondaryFg,
                        fontSize = LauncherType.HeadingSize,
                        fontWeight = LauncherType.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.description.ifBlank { strings.downloadDialogNoDescription },
                        color = colors.secondaryFgDim,
                        fontSize = LauncherType.CaptionSize,
                        modifier = Modifier
                            .padding(top = LauncherSpacing.ExtraSmall)
                            .heightIn(max = LauncherType.DownloadDescriptionMaxHeight),
                    )
                    if (state.hasPage) {
                        DialogLinkButton(
                            icon = R.drawable.ic_dialog_open_in_new,
                            iconSize = LauncherSizes.DialogLinkIcon,
                            text = strings.downloadDialogViewPage,
                            onClick = onViewPage,
                            modifier = Modifier.padding(top = LauncherSpacing.Small),
                        )
                    }
                }
            }
            Text(
                text = strings.downloadDialogPath,
                color = colors.secondaryFgDim,
                fontSize = LauncherType.CaptionSize,
                modifier = Modifier.padding(bottom = LauncherSpacing.ExtraSmall),
            )
            LauncherTooltip(text = state.path) {
                Text(
                    text = state.path,
                    color = colors.secondaryFg,
                    fontSize = LauncherType.CaptionSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.tertiaryBg, RoundedCornerShape(LauncherRadii.Small))
                        .padding(horizontal = LauncherSpacing.MediumLarge, vertical = LauncherSpacing.Medium),
                )
            }
            Row(Modifier.padding(top = LauncherSpacing.ExtraLarge), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    DialogFieldLabel(
                        icon = R.drawable.ic_card_harddisk,
                        text = "${strings.downloadDialogGameSize}:",
                        iconSize = LauncherSizes.DownloadDialogMetricIcon,
                        trailing = state.size,
                    )
                }
                Column(Modifier.weight(1f)) {
                    DialogFieldLabel(
                        icon = R.drawable.ic_dialog_database_check,
                        text = "${strings.downloadDialogFreeSpace}:",
                        iconSize = LauncherSizes.DownloadDialogMetricIcon,
                        trailing = state.freeSpace,
                    )
                }
            }
            Column(Modifier.padding(top = LauncherSpacing.Block)) {
                DialogKeyField(
                    label = strings.downloadDialogKey,
                    value = key,
                    onValueChange = { key = it },
                    onDone = { onConfirm(key) },
                )
            }
        }
    }
}
