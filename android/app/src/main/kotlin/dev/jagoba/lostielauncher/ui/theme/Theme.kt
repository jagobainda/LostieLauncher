package dev.jagoba.lostielauncher.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.jagoba.lostielauncher.model.AppTheme

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
 * Takes the selected [AppTheme] and provides its palette to the tree. Switching
 * is a recomposition and nothing more: no activity recreation, no reload, no
 * lost screen state — which is the behaviour `spec/06-design-tokens.md` asks
 * the port to keep, as opposed to the desktop's mechanism of swapping a merged
 * resource dictionary.
 *
 * Only colour is provided here. The type scale, spacings, radii, borders and
 * durations do not vary by theme, so they are plain objects in `Tokens.kt` with
 * nothing to provide and nothing to recompose.
 */
@Composable
fun LostieLauncherTheme(theme: AppTheme = AppTheme.Default, content: @Composable () -> Unit) {
    val colors = paletteFor(theme)

    // The window is edge-to-edge, so the status bar sits on top of the
    // theme's own background and the system draws its clock and icons there.
    // Which way to tint them is not a system-theme question but a
    // this-palette question — three of the ten are light — and it has to
    // follow a theme change like everything else.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val light = !colors.isDark()
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
    }

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
 * that is what `onPrimary` gets. `spec/06-design-tokens.md` records the same
 * thing, and its contrast review list is where the cases that makes awkward are
 * collected.
 *
 * Whether the scheme is built light or dark is derived from the palette's own
 * overlays rather than listed anywhere — see [isDark]. It changes nothing this
 * file sets; it only gives Material sensible values for the roles the launcher
 * does not define, and tells the platform which way to tint a ripple.
 */
private fun LauncherColors.toMaterialColorScheme() = if (isDark()) {
    darkColorScheme(
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
} else {
    lightColorScheme(
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
}
