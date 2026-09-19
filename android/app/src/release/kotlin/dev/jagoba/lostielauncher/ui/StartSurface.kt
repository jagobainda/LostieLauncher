package dev.jagoba.lostielauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.ui.screen.EmptyScreen

/**
 * The release build's surface: still nothing, as port plan step 03 left it.
 *
 * The debug build's copy of this file shows the token catalogue instead. Two
 * source sets rather than one screen behind a `BuildConfig.DEBUG` branch,
 * because the catalogue names every colour in all ten palettes and every token
 * in the visual language, and none of that belongs in a shipped APK — this way
 * it is not merely unreachable there, it is not compiled.
 *
 * The theme and the two selection callbacks are unused here and that is the
 * point: the release build shows no token catalogue and has no way to change
 * either setting until port plan step 14 writes the Settings screen.
 *
 * Step 11 replaces both copies with the navigation shell.
 */
@Composable
fun StartSurface(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyScreen(modifier)
}
