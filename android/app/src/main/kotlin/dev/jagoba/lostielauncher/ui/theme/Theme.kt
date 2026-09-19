package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The active palette, readable from anywhere in the tree as
 * `LocalLauncherColors.current`.
 *
 * `staticCompositionLocalOf` rather than `compositionLocalOf`: the palette
 * changes rarely — only when the user picks another theme — and when it does,
 * everything below has to be redrawn anyway.
 */
val LocalLauncherColors = staticCompositionLocalOf { VolcaronaColors }

/**
 * The application's theme.
 *
 * Fixed to Volcarona for now. Step 05 of the port plan turns [colors] into the
 * user's persisted selection out of ten palettes, switched at runtime without
 * restarting, exactly as the desktop does it.
 */
@Composable
fun LostieLauncherTheme(colors: LauncherColors = VolcaronaColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLauncherColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            content = content,
        )
    }
}

/**
 * Projects the launcher's own roles onto the Material 3 scheme, so Material
 * components pick up the theme without every call site restating it.
 *
 * The mapping is lossy in one direction that matters: the desktop has no
 * dedicated "on accent" colour and uses [LauncherColors.secondaryFg] there, so
 * that is what `onPrimary` gets.
 */
private fun LauncherColors.toMaterialColorScheme() = darkColorScheme(
    primary = primaryFg,
    onPrimary = secondaryFg,
    secondary = primaryFgHover,
    onSecondary = secondaryFg,
    tertiary = success,
    onTertiary = secondaryFg,
    background = primaryBg,
    onBackground = secondaryFg,
    surface = secondaryBg,
    onSurface = secondaryFg,
    surfaceVariant = tertiaryBg,
    onSurfaceVariant = secondaryFgDim,
    outline = overlayLight,
    outlineVariant = overlaySubtle,
)
