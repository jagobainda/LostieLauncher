package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import dev.jagoba.lostielauncher.model.AppTheme

val LocalLauncherColors = staticCompositionLocalOf { VolcaronaColors }

private val ContentDirection = TextStyle(textDirection = TextDirection.Content)

private val LauncherTypography = Typography().let {
    it.copy(bodyLarge = ContentDirection, bodySmall = it.bodySmall.merge(ContentDirection))
}

@Composable
fun LostieLauncherTheme(theme: AppTheme = AppTheme.Default, content: @Composable () -> Unit) {
    val colors = paletteFor(theme)
    CompositionLocalProvider(LocalLauncherColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = LauncherTypography,
            content = content,
        )
    }
}

private fun LauncherColors.toMaterialColorScheme() = (if (isDark()) darkColorScheme() else lightColorScheme()).copy(
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
    surfaceTint = primaryFg,
    outline = overlayLight,
    outlineVariant = overlaySubtle,
)
