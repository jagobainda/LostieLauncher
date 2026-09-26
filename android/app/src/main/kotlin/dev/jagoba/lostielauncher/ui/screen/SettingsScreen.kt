package dev.jagoba.lostielauncher.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.ui.component.LauncherComboBox
import dev.jagoba.lostielauncher.ui.component.StatusNotice
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val state = settings ?: return
    val strings = state.strings
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = LauncherSpacing.Screen,
                top = LauncherSpacing.Screen,
                end = LauncherSpacing.Screen,
                bottom = LauncherSpacing.ExtraSmall,
            ),
    ) {
        SettingsSection(strings.settingsGeneral, Modifier.padding(bottom = LauncherSpacing.Section)) {
            val interactions = remember { MutableInteractionSource() }
            SettingsRow(
                icon = R.drawable.ic_card_update,
                label = strings.settingsAutoUpdate,
                notice = strings.statusNotSupportedYet.takeUnless { state.autoUpdateSupported },
                modifier = Modifier.toggleable(
                    value = state.autoUpdate,
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Switch,
                    onValueChange = viewModel::setAutoUpdate,
                ),
            ) { ToggleSwitch(state.autoUpdate) }
            SettingsDivider()
            SettingsRow(icon = R.drawable.ic_settings_refresh, label = state.version)
        }
        SettingsSection(strings.settingsAppearance) {
            SettingsRow(icon = R.drawable.ic_dialog_translate, label = strings.settingsLanguage) {
                LauncherComboBox(
                    options = AppLanguage.entries,
                    selected = state.language,
                    label = AppLanguage::displayName,
                    onSelect = viewModel::selectLanguage,
                )
            }
            SettingsDivider()
            SettingsRow(icon = R.drawable.ic_settings_palette, label = strings.settingsTheme) {
                LauncherComboBox(
                    options = AppTheme.entries,
                    selected = state.theme,
                    label = AppTheme::name,
                    onSelect = viewModel::selectTheme,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, rows: @Composable () -> Unit) {
    val colors = LocalLauncherColors.current
    Column(modifier) {
        Text(
            text = title,
            color = colors.primaryFg,
            fontSize = LauncherType.SectionSize,
            fontWeight = LauncherType.SemiBold,
            modifier = Modifier.padding(start = LauncherSpacing.ExtraSmall, bottom = LauncherSpacing.Medium),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.secondaryBg, RoundedCornerShape(LauncherRadii.Medium)),
        ) { rows() }
    }
}

@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    notice: String? = null,
    control: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalLauncherColors.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = LauncherSpacing.Card, vertical = LauncherSpacing.ExtraLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.secondaryFgDim,
            modifier = Modifier.size(LauncherSizes.SettingsRowIcon),
        )
        Spacer(Modifier.width(LauncherSpacing.MediumLarge))
        Column(Modifier.weight(1f)) {
            Text(text = label, color = colors.secondaryFg, fontSize = LauncherType.BodySize)
            if (notice != null) StatusNotice(notice)
        }
        control()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier
            .padding(horizontal = LauncherSpacing.Card)
            .fillMaxWidth()
            .height(LauncherBorders.Thin)
            .background(LocalLauncherColors.current.overlayLight),
    )
}

@Composable
private fun ToggleSwitch(checked: Boolean) {
    val colors = LocalLauncherColors.current
    Box(
        Modifier
            .size(LauncherSizes.ToggleTrackWidth, LauncherSizes.ToggleTrackHeight)
            .background(
                if (checked) colors.primaryFg else colors.overlayMuted,
                RoundedCornerShape(LauncherRadii.ToggleTrack),
            )
            .padding(horizontal = LauncherSpacing.Micro),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(LauncherSizes.ToggleThumb)
                .background(colors.secondaryFg, RoundedCornerShape(LauncherRadii.ToggleThumb)),
        )
    }
}
