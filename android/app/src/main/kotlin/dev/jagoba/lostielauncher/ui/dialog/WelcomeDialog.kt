package dev.jagoba.lostielauncher.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.LauncherComboBox
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

private const val HTTPS_PREFIX = "https://"

@Composable
fun WelcomeDialog(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenRepository: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = LocalLauncherColors.current
    LauncherDialog(
        icon = R.drawable.ic_dialog_party_popper,
        title = strings.welcomeDialogTitle,
        width = LauncherSizes.WelcomeDialogWidth,
        minHeight = LauncherSizes.WelcomeDialogHeight,
        maxHeight = LauncherSizes.WelcomeDialogHeight,
        onDismiss = onDismiss,
        footerBackground = colors.secondaryBg,
        footer = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LauncherSpacing.Screen)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.CenterEnd,
            ) {
                DialogButton(
                    label = strings.welcomeDialogContinue,
                    style = DialogButtonStyle.ACCENT,
                    width = LauncherSizes.WelcomeContinueButtonWidth,
                    onClick = onDismiss,
                )
            }
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = LauncherSpacing.Section, top = LauncherSpacing.Medium, end = LauncherSpacing.Section),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = LauncherSpacing.Large, bottom = LauncherSpacing.Screen),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(LauncherSizes.WelcomeBrandTile)
                        .background(colors.tertiaryBg, RoundedCornerShape(LauncherRadii.ExtraLarge)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dialog_gamepad),
                        contentDescription = null,
                        tint = colors.primaryFg,
                        modifier = Modifier.size(LauncherSizes.WelcomeBrandIcon),
                    )
                }
                Text(
                    text = stringResource(R.string.app_name),
                    color = colors.secondaryFg,
                    fontSize = LauncherType.DisplaySize,
                    fontWeight = LauncherType.Bold,
                    modifier = Modifier.padding(top = LauncherSpacing.Large),
                )
            }
            Text(
                text = strings.welcomeDialogDescription,
                color = colors.secondaryFgDim,
                fontSize = LauncherType.CaptionSize,
                lineHeight = LauncherType.WelcomeDescriptionLineHeight,
                modifier = Modifier.padding(bottom = LauncherSpacing.Large),
            )
            Row(Modifier.padding(bottom = LauncherSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_dialog_translate),
                    contentDescription = null,
                    tint = colors.secondaryFgDim,
                    modifier = Modifier.size(LauncherSizes.WelcomeRowIcon),
                )
                Text(
                    text = strings.settingsLanguage,
                    color = colors.secondaryFg,
                    fontSize = LauncherType.BodySize,
                    modifier = Modifier
                        .padding(start = LauncherSpacing.MediumLarge)
                        .weight(1f),
                )
                LauncherComboBox(
                    options = AppLanguage.entries,
                    selected = language,
                    label = AppLanguage::displayName,
                    onSelect = onLanguageSelected,
                )
            }
            DialogLinkButton(
                icon = R.drawable.ic_dialog_github,
                iconSize = LauncherSizes.DialogFieldIcon,
                text = strings.repositoryUrl.removePrefix(HTTPS_PREFIX),
                onClick = onOpenRepository,
            )
        }
    }
}
