package dev.jagoba.lostielauncher.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import dev.jagoba.lostielauncher.model.AppTheme

val LocalLauncherColors = staticCompositionLocalOf { VolcaronaColors }

@Composable
fun LostieLauncherTheme(theme: AppTheme = AppTheme.Default, content: @Composable () -> Unit) {
    val colors = paletteFor(theme)

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
            typography = Typography(bodyLarge = TextStyle.Default),
            content = content,
        )
    }
}

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
