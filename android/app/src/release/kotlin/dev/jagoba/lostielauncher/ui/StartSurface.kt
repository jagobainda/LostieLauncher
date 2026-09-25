package dev.jagoba.lostielauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.ui.screen.LauncherShell

@Composable
fun StartSurface(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LauncherShell(modifier = modifier)
}
