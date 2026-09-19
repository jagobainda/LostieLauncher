package dev.jagoba.lostielauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.ui.screen.TokenCatalogScreen

/**
 * The debug build's surface: the token catalogue.
 *
 * The release copy of this file shows [dev.jagoba.lostielauncher.ui.screen.EmptyScreen]
 * instead. Two source sets rather than one screen behind a `BuildConfig.DEBUG`
 * branch, so the catalogue is not merely unreachable in a shipped APK — it is
 * not compiled into one.
 *
 * Port plan step 11 replaces both copies with the navigation shell. The
 * catalogue should survive that as a destination reachable only here, because
 * what it is for — seeing all ten palettes and every token at once — does not
 * stop being useful when there are real screens to compare them against.
 */
@Composable
fun StartSurface(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    TokenCatalogScreen(
        theme = theme,
        onThemeSelected = onThemeSelected,
        onLanguageSelected = onLanguageSelected,
        modifier = modifier,
    )
}
