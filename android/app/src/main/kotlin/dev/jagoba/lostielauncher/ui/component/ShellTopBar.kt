package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

data class ShellTopBarAction(val key: String, val label: String?, val icon: Painter, val onClick: () -> Unit)

@Composable
fun ShellTopBar(
    title: String,
    logo: Painter,
    offlinePill: OfflinePillContent?,
    actions: List<ShellTopBarAction>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    Row(
        modifier.fillMaxWidth().height(LauncherSizes.TitleBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(LauncherSpacing.Card))
        Image(painter = logo, contentDescription = null, modifier = Modifier.size(LauncherSizes.TitleBarIcon))
        Spacer(Modifier.width(LauncherSpacing.Medium))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = colors.secondaryFg,
                fontSize = LauncherType.BodySize,
                fontWeight = LauncherType.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            )
            if (offlinePill != null) {
                Spacer(Modifier.width(LauncherSpacing.MediumLarge))
                OfflinePill(offlinePill)
            }
        }
        actions.forEach { ShellTopBarButton(it) }
    }
}

data class OfflinePillContent(val label: String, val tooltip: String, val icon: Painter)

@Composable
fun OfflinePill(content: OfflinePillContent, modifier: Modifier = Modifier) {
    val colors = LocalLauncherColors.current
    val shape = RoundedCornerShape(LauncherRadii.Small)
    LauncherTooltip(text = content.tooltip, modifier = modifier) {
        Row(
            Modifier
                .height(LauncherSizes.OfflinePillHeight)
                .background(colors.overlaySubtle, shape)
                .border(LauncherBorders.Thin, FixedColors.Warning, shape)
                .padding(horizontal = LauncherSpacing.Medium)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = content.icon,
                contentDescription = null,
                tint = FixedColors.Warning,
                modifier = Modifier.size(LauncherSizes.OfflinePillIcon),
            )
            Spacer(Modifier.width(LauncherSpacing.Snug))
            Text(
                text = content.label,
                color = colors.secondaryFg,
                fontSize = LauncherType.LabelSize,
                fontWeight = LauncherType.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ShellTopBarButton(action: ShellTopBarAction) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val button: @Composable () -> Unit = {
        Box(
            Modifier
                .size(LauncherSizes.TitleBarButton)
                .then(if (pressed || hovered) Modifier.background(colors.overlayMedium) else Modifier)
                .semantics { action.label?.let { contentDescription = it } }
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Button,
                    onClick = action.onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = action.icon,
                contentDescription = null,
                tint = colors.secondaryFg,
                modifier = Modifier.size(LauncherSizes.TitleBarButtonIcon),
            )
        }
    }
    val label = action.label
    if (label != null) LauncherTooltip(text = label, content = button) else button()
}
