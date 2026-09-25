package dev.jagoba.lostielauncher.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherTooltip(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalLauncherColors.current
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(containerColor = colors.secondaryBg, contentColor = colors.secondaryFg) {
                Text(text, fontSize = LauncherType.CaptionSize)
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}
