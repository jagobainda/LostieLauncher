package dev.jagoba.lostielauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.ui.screen.DownloadHarnessScreen
import dev.jagoba.lostielauncher.ui.screen.TokenCatalogScreen
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing

@Composable
fun StartSurface(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var showDownloads by remember { mutableStateOf(true) }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(LauncherSpacing.Medium),
        ) {
            OutlinedButton(onClick = { showDownloads = true }) {
                Text(strings.btnDownload)
            }
            OutlinedButton(onClick = { showDownloads = false }) {
                Text(strings.settingsTheme)
            }
        }
        Box(Modifier.fillMaxSize()) {
            if (showDownloads) {
                DownloadHarnessScreen()
            } else {
                TokenCatalogScreen(
                    theme = theme,
                    onThemeSelected = onThemeSelected,
                    onLanguageSelected = onLanguageSelected,
                )
            }
        }
    }
}
