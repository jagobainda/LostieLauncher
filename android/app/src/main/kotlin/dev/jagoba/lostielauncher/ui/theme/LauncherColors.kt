package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LauncherColors(
    val primaryBg: Color,
    val secondaryBg: Color,
    val tertiaryBg: Color,
    val primaryFg: Color,
    val primaryFgHover: Color,
    val primaryFgPressed: Color,
    val secondaryFg: Color,
    val secondaryFgDim: Color,
    val success: Color,
    val overlaySubtle: Color,
    val overlayLight: Color,
    val overlayMuted: Color,
    val overlayMedium: Color,
    val overlayStrong: Color,
)

internal fun LauncherColors.accentButton(enabled: Boolean, pressed: Boolean, hovered: Boolean): Color = when {
    enabled && pressed -> primaryFgPressed
    enabled && hovered -> primaryFgHover
    else -> primaryFg
}
