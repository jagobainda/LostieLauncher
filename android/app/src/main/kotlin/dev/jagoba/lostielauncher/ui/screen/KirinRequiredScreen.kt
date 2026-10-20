package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.content.withArgs
import dev.jagoba.lostielauncher.model.KirinDownloadState
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.EmptyState
import dev.jagoba.lostielauncher.ui.component.LinkText
import dev.jagoba.lostielauncher.ui.component.StatusNotice
import dev.jagoba.lostielauncher.ui.dialog.DialogButton
import dev.jagoba.lostielauncher.ui.dialog.DialogButtonStyle
import dev.jagoba.lostielauncher.ui.dialog.DialogLinkButton
import dev.jagoba.lostielauncher.ui.dialog.LauncherMessageBox
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxIcon
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxSpec
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.viewmodel.KirinUiState
import dev.jagoba.lostielauncher.ui.viewmodel.KirinViewModel

@Composable
fun KirinRequirement(
    modifier: Modifier = Modifier,
    viewModel: KirinViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    when {
        state.status == null -> Box(modifier.fillMaxSize().background(LocalLauncherColors.current.primaryBg))

        state.isBlocking -> KirinRequiredScreen(
            state = state,
            onInstall = viewModel::install,
            onOpenLink = viewModel::openLink,
            modifier = modifier,
        )

        else -> content()
    }
    if (state.download == KirinDownloadState.Failed) {
        LauncherMessageBox(
            spec = MessageBoxSpec(
                strings.downloadErrorTitle,
                strings.downloadErrorMessage,
                icon = MessageBoxIcon.ERROR,
            ),
            onConfirm = viewModel::dismissDownloadFailure,
            onDismiss = viewModel::dismissDownloadFailure,
        )
    }
}

@Composable
private fun KirinRequiredScreen(
    state: KirinUiState,
    onInstall: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val status = state.status
    var isFaqVisible by rememberSaveable { mutableStateOf(false) }

    EmptyState(
        icon = R.drawable.ic_games_gamepad,
        message = if (status is KirinStatus.Outdated) strings.kirinOutdatedTitle else strings.kirinMissingTitle,
        detail = when (status) {
            is KirinStatus.Outdated -> strings.kirinOutdatedMessage.withArgs(
                status.installedVersion,
                status.minimumVersion,
            )

            is KirinStatus.Missing -> strings.kirinMissingMessage.withArgs(status.minimumVersion)

            else -> null
        },
        modifier = modifier
            .background(LocalLauncherColors.current.primaryBg)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
        action = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (val download = state.download) {
                    KirinDownloadState.Checking -> Unit

                    KirinDownloadState.Unavailable -> StatusNotice(strings.kirinDownloadUnavailable)

                    KirinDownloadState.Available, KirinDownloadState.Failed -> DialogButton(
                        label = if (status is KirinStatus.Outdated) strings.btnUpdate else strings.btnDownload,
                        style = DialogButtonStyle.ACCENT,
                        onClick = onInstall,
                        horizontalPadding = LauncherSpacing.Screen,
                    )

                    is KirinDownloadState.Downloading -> DialogButton(
                        label = "${strings.statusDownloading} ${download.percent}%",
                        style = DialogButtonStyle.ACCENT,
                        onClick = {},
                        horizontalPadding = LauncherSpacing.Screen,
                        enabled = false,
                    )
                }
                state.faq?.let { faq ->
                    DialogLinkButton(
                        icon = R.drawable.ic_dialog_information,
                        iconSize = LauncherSizes.DialogLinkIcon,
                        text = faq.question,
                        onClick = { isFaqVisible = true },
                        modifier = Modifier.padding(top = LauncherSpacing.Large),
                    )
                }
                LinkText(
                    text = strings.kirinThanks,
                    color = LocalLauncherColors.current.secondaryFgDim,
                    fontSize = LauncherType.CaptionSize,
                    onOpenLink = onOpenLink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = LauncherSpacing.Section),
                )
            }
        },
    )

    val faq = state.faq
    if (isFaqVisible && faq != null) {
        LauncherMessageBox(
            spec = MessageBoxSpec(faq.question, faq.answer, icon = MessageBoxIcon.INFORMATION),
            onConfirm = { isFaqVisible = false },
            onDismiss = { isFaqVisible = false },
        )
    }
}
