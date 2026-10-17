package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.ui.LocalAppLanguage
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherColors
import dev.jagoba.lostielauncher.ui.theme.LauncherMotion
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.theme.isDark

@Composable
fun TokenCatalogScreen(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val strings = LocalStrings.current
    val language = LocalAppLanguage.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryBg),
        contentPadding = WindowInsets.safeDrawing
            .asPaddingValues()
            .plus(LauncherSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Section),
    ) {
        item { AppearancePickers(theme, onThemeSelected, onLanguageSelected) }

        item { SectionHeader("Colour — the 14 theme keys") }
        items(colorRoles(colors)) { (name, color) -> Swatch(name, color) }

        item { SectionHeader("Colour — the four that never follow the theme") }
        items(fixedColorRoles()) { (name, color) -> Swatch(name, color) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium)) {
                SectionHeader("Type")
                TypeRow("DisplaySize 20", LauncherType.DisplaySize, LauncherType.Bold)
                TypeRow("TitleSize 18", LauncherType.TitleSize, LauncherType.SemiBold)
                TypeRow("HeadingSize 16", LauncherType.HeadingSize, LauncherType.Bold)
                TypeRow("SubheadingSize 15", LauncherType.SubheadingSize, LauncherType.SemiBold)
                TypeRow("SectionSize 14", LauncherType.SectionSize, LauncherType.SemiBold)
                TypeRow("BodySize 13", LauncherType.BodySize, LauncherType.Normal)
                TypeRow("CaptionSize 12", LauncherType.CaptionSize, LauncherType.Normal)
                TypeRow("LabelSize 11", LauncherType.LabelSize, LauncherType.SemiBold)
                TypeRow("BadgeSize 10", LauncherType.BadgeSize, LauncherType.Bold)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium)) {
                SectionHeader("Spacing")
                spacingScale().forEach { (name, value) -> BarRow(name, value) }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium)) {
                SectionHeader("Radius")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Large),
                ) {
                    radiusScale().forEach { (name, value) -> RadiusTile(name, value) }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium)) {
                SectionHeader("Border, size and motion")
                Caption("LauncherBorders.Thin ${LauncherBorders.Thin.value.toInt()} dp")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(LauncherSpacing.Section)
                        .border(LauncherBorders.Thin, colors.primaryFg, RoundedCornerShape(LauncherRadii.Small)),
                )
                Caption("LauncherBorders.Thick ${LauncherBorders.Thick.value.toInt()} dp")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(LauncherSpacing.Section)
                        .border(LauncherBorders.Thick, colors.primaryFg, RoundedCornerShape(LauncherRadii.Small)),
                )
                Caption(
                    "TitleBarHeight ${LauncherSizes.TitleBarHeight.value.toInt()} · " +
                        "NavigationRailWidth ${LauncherSizes.NavigationRailWidth.value.toInt()} · " +
                        "NavigationItemSize ${LauncherSizes.NavigationItemSize.value.toInt()} · " +
                        "ScrollbarWidth ${LauncherSizes.ScrollbarWidth.value.toInt()} dp",
                )
                Caption(
                    "Shimmer ${LauncherMotion.Shimmer} · Spinner ${LauncherMotion.SpinnerTurn} · " +
                        "Pulse ${LauncherMotion.Pulse} for ${LauncherMotion.PulseTotal}",
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium)) {
                SectionHeader("Text catalogue")
                Caption("The five screen titles, then one multi-line string, in ${language.displayName}.")
                Text(
                    text = listOf(
                        strings.titleHome,
                        strings.titleGames,
                        strings.titleLibrary,
                        strings.titleSettings,
                        strings.titleFaqs,
                    ).joinToString(" · "),
                    color = colors.secondaryFg,
                    fontSize = LauncherType.BodySize,
                )
                Text(
                    text = strings.welcomeDialogDescription,
                    color = colors.secondaryFgDim,
                    fontSize = LauncherType.CaptionSize,
                    lineHeight = LauncherType.WelcomeDescriptionLineHeight,
                )
            }
        }
    }
}

@Composable
internal fun AppearancePickers(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val colors = LocalLauncherColors.current
    val language = LocalAppLanguage.current
    Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium)) {
        SectionHeader("Theme")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
        ) {
            AppTheme.entries.forEach { candidate ->
                Chip(label = candidate.name, selected = candidate == theme, onClick = { onThemeSelected(candidate) })
            }
        }
        Caption(if (colors.isDark()) "dark palette" else "light palette")
        SectionHeader("Language")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
        ) {
            AppLanguage.entries.forEach { candidate ->
                Chip(
                    label = candidate.displayName,
                    selected = candidate == language,
                    onClick = { onLanguageSelected(candidate) },
                )
            }
        }
        Caption("${language.name} · wire code \"${language.code}\"")
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text = text,
        color = LocalLauncherColors.current.primaryFg,
        fontSize = LauncherType.SectionSize,
        fontWeight = LauncherType.SemiBold,
    )
}

@Composable
internal fun Caption(text: String) {
    Text(
        text = text,
        color = LocalLauncherColors.current.secondaryFgDim,
        fontSize = LauncherType.CaptionSize,
    )
}

@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalLauncherColors.current
    Box(
        modifier = Modifier
            .background(
                color = if (selected) colors.primaryFg else colors.overlayMuted,
                shape = RoundedCornerShape(LauncherRadii.Small),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = LauncherSpacing.Large, vertical = LauncherSpacing.Small),
    ) {
        Text(
            text = label,
            color = colors.secondaryFg,
            fontSize = LauncherType.CaptionSize,
            fontWeight = if (selected) LauncherType.SemiBold else LauncherType.Normal,
        )
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val colors = LocalLauncherColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LauncherSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Large),
    ) {
        Box(
            Modifier
                .size(LauncherSizes.NavigationItemSize, LauncherSpacing.Section)
                .background(colors.secondaryBg, RoundedCornerShape(LauncherRadii.Small))
                .background(color, RoundedCornerShape(LauncherRadii.Small))
                .border(LauncherBorders.Thin, colors.secondaryFgDim, RoundedCornerShape(LauncherRadii.Small)),
        )
        Text(
            text = name,
            color = colors.secondaryFg,
            fontSize = LauncherType.CaptionSize,
            modifier = Modifier.width(LauncherSizes.NavigationRailWidth * 2),
        )
        Text(text = color.toHex(), color = colors.secondaryFgDim, fontSize = LauncherType.LabelSize)
    }
}

@Composable
private fun TypeRow(name: String, size: TextUnit, weight: FontWeight) {
    val colors = LocalLauncherColors.current
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Large)) {
        Text(
            text = name,
            color = colors.secondaryFgDim,
            fontSize = LauncherType.LabelSize,
            modifier = Modifier.width(LauncherSizes.NavigationRailWidth + LauncherSpacing.Section),
        )
        Text(text = "Lostie Launcher", color = colors.secondaryFg, fontSize = size, fontWeight = weight)
    }
}

@Composable
private fun BarRow(name: String, value: Dp) {
    val colors = LocalLauncherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Large),
    ) {
        Text(
            text = name,
            color = colors.secondaryFgDim,
            fontSize = LauncherType.LabelSize,
            modifier = Modifier.width(LauncherSizes.NavigationRailWidth + LauncherSpacing.Section),
        )
        Box(
            Modifier
                .width(value)
                .height(LauncherSpacing.Large)
                .background(colors.primaryFg, RoundedCornerShape(LauncherRadii.Hair)),
        )
        Text(text = "${value.value.toInt()}", color = colors.secondaryFgDim, fontSize = LauncherType.LabelSize)
    }
}

@Composable
private fun RadiusTile(name: String, value: Dp) {
    val colors = LocalLauncherColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.ExtraSmall),
    ) {
        Box(
            Modifier
                .size(LauncherSizes.NavigationItemSize)
                .background(colors.overlayMuted, RoundedCornerShape(value))
                .border(LauncherBorders.Thin, colors.primaryFg, RoundedCornerShape(value)),
        )
        Text(
            text = name,
            color = colors.secondaryFgDim,
            fontSize = LauncherType.LabelSize,
            textAlign = TextAlign.Center,
        )
    }
}

private fun colorRoles(colors: LauncherColors): List<Pair<String, Color>> = listOf(
    "primaryBg" to colors.primaryBg,
    "secondaryBg" to colors.secondaryBg,
    "tertiaryBg" to colors.tertiaryBg,
    "primaryFg" to colors.primaryFg,
    "primaryFgHover" to colors.primaryFgHover,
    "primaryFgPressed" to colors.primaryFgPressed,
    "secondaryFg" to colors.secondaryFg,
    "secondaryFgDim" to colors.secondaryFgDim,
    "success" to colors.success,
    "overlaySubtle" to colors.overlaySubtle,
    "overlayLight" to colors.overlayLight,
    "overlayMuted" to colors.overlayMuted,
    "overlayMedium" to colors.overlayMedium,
    "overlayStrong" to colors.overlayStrong,
)

private fun fixedColorRoles(): List<Pair<String, Color>> = listOf(
    "FixedColors.Warning" to FixedColors.Warning,
    "FixedColors.WindowsClose" to FixedColors.WindowsClose,
    "FixedColors.NotificationInfo" to FixedColors.NotificationInfo,
    "FixedColors.NotificationWarning" to FixedColors.NotificationWarning,
    "FixedColors.NotificationExclamation" to FixedColors.NotificationExclamation,
    "FixedColors.ShimmerEdge" to FixedColors.ShimmerEdge,
    "FixedColors.ShimmerPeak" to FixedColors.ShimmerPeak,
)

private fun spacingScale(): List<Pair<String, Dp>> = listOf(
    "Hair" to LauncherSpacing.Hair,
    "Micro" to LauncherSpacing.Micro,
    "ExtraSmall" to LauncherSpacing.ExtraSmall,
    "Small" to LauncherSpacing.Small,
    "Medium" to LauncherSpacing.Medium,
    "MediumLarge" to LauncherSpacing.MediumLarge,
    "Large" to LauncherSpacing.Large,
    "ExtraLarge" to LauncherSpacing.ExtraLarge,
    "Card" to LauncherSpacing.Card,
    "Screen" to LauncherSpacing.Screen,
    "Section" to LauncherSpacing.Section,
    "Block" to LauncherSpacing.Block,
)

private fun radiusScale(): List<Pair<String, Dp>> = listOf(
    "Hair 2" to LauncherRadii.Hair,
    "Micro 3" to LauncherRadii.Micro,
    "Small 4" to LauncherRadii.Small,
    "Medium 6" to LauncherRadii.Medium,
    "Large 8" to LauncherRadii.Large,
    "ToggleThumb 9" to LauncherRadii.ToggleThumb,
    "ToggleTrack 12" to LauncherRadii.ToggleTrack,
    "ExtraLarge 16" to LauncherRadii.ExtraLarge,
)

private fun Color.toHex(): String {
    val argb = toArgb()
    val digits = "0123456789ABCDEF"
    val out = StringBuilder(9).append('#')
    for (shift in 28 downTo 0 step 4) {
        out.append(digits[(argb shr shift) and 0xF])
    }
    return out.toString()
}

@Composable
private fun PaddingValues.plus(all: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + all,
        top = calculateTopPadding() + all,
        end = calculateEndPadding(direction) + all,
        bottom = calculateBottomPadding() + all,
    )
}
