package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Composable
fun SectionPlaceholder(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = title,
            color = LocalLauncherColors.current.secondaryFgDim,
            fontSize = LauncherType.SectionSize,
            fontWeight = LauncherType.SemiBold,
        )
    }
}
