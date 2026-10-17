package dev.jagoba.lostielauncher.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    message: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    detail: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val color = LocalLauncherColors.current.secondaryFgDim
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = LauncherSpacing.Screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(
                    if (compact) LauncherSizes.EmptyStateIconSmall else LauncherSizes.EmptyStateIcon,
                ),
            )
            Text(
                text = message,
                color = color,
                fontSize = if (compact) LauncherType.BodySize else LauncherType.SectionSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = if (compact) LauncherSpacing.Large else LauncherSpacing.Card),
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = color,
                    fontSize = LauncherType.CaptionSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = LauncherSpacing.ExtraSmall),
                )
            }
            if (action != null) {
                Box(Modifier.padding(top = LauncherSpacing.Large)) { action() }
            }
        }
    }
}
