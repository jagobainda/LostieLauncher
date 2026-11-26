package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Composable
internal fun LauncherDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(horizontal = LauncherSpacing.Card)
            .fillMaxWidth()
            .height(LauncherBorders.Thin)
            .background(LocalLauncherColors.current.overlayLight),
    )
}
