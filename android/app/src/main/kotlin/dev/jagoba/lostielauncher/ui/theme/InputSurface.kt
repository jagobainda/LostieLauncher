package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

val LauncherColors.hasInvisibleInputs: Boolean get() = tertiaryBg == primaryBg

fun Modifier.inputSurface(colors: LauncherColors, shape: Shape): Modifier = background(colors.tertiaryBg, shape).then(
    if (colors.hasInvisibleInputs) Modifier.border(LauncherBorders.Thin, colors.overlayStrong, shape) else Modifier,
)
