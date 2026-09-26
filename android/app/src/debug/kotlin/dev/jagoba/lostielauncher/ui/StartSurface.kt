package dev.jagoba.lostielauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.ui.screen.ComponentCatalogScreen
import dev.jagoba.lostielauncher.ui.screen.DialogCatalogScreen
import dev.jagoba.lostielauncher.ui.screen.DownloadHarnessScreen
import dev.jagoba.lostielauncher.ui.screen.LauncherShell
import dev.jagoba.lostielauncher.ui.screen.TokenCatalogScreen
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing

private enum class DebugTab { DOWNLOADS, TOKENS, COMPONENTS, DIALOGS }

private const val COMPONENTS_LABEL = "Components"
private const val DIALOGS_LABEL = "Dialogs"

@Composable
fun StartSurface(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LauncherShell(
        onLanguageSelected = onLanguageSelected,
        modifier = modifier,
        debugIcon = painterResource(R.drawable.ic_debug_tools),
        debugContent = {
            DebugTools(theme = theme, onThemeSelected = onThemeSelected, onLanguageSelected = onLanguageSelected)
        },
    )
}

@Composable
private fun DebugTools(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val strings = LocalStrings.current
    var tab by rememberSaveable { mutableStateOf(DebugTab.DOWNLOADS) }
    Column(Modifier.fillMaxSize()) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(LauncherSpacing.Medium),
        ) {
            OutlinedButton(onClick = { tab = DebugTab.DOWNLOADS }) {
                Text(strings.btnDownload)
            }
            OutlinedButton(onClick = { tab = DebugTab.TOKENS }) {
                Text(strings.settingsTheme)
            }
            OutlinedButton(onClick = { tab = DebugTab.COMPONENTS }) {
                Text(COMPONENTS_LABEL)
            }
            OutlinedButton(onClick = { tab = DebugTab.DIALOGS }) {
                Text(DIALOGS_LABEL)
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                DebugTab.DOWNLOADS -> DownloadHarnessScreen()

                DebugTab.TOKENS -> TokenCatalogScreen(
                    theme = theme,
                    onThemeSelected = onThemeSelected,
                    onLanguageSelected = onLanguageSelected,
                )

                DebugTab.COMPONENTS -> ComponentCatalogScreen(
                    theme = theme,
                    onThemeSelected = onThemeSelected,
                    onLanguageSelected = onLanguageSelected,
                )

                DebugTab.DIALOGS -> DialogCatalogScreen(
                    theme = theme,
                    onThemeSelected = onThemeSelected,
                    onLanguageSelected = onLanguageSelected,
                )
            }
        }
    }
}
