package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.jagoba.lostielauncher.ui.theme.LauncherOpacity
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Immutable
data class ShellNavigationEntry(
    val key: String,
    val label: String,
    val icon: Painter,
    val selected: Boolean,
    val enabled: Boolean,
    val isAction: Boolean,
    val onClick: () -> Unit,
)

enum class IndicatorEdge {
    START,
    BOTTOM,
}

@Composable
fun ShellNavigationRail(
    top: List<ShellNavigationEntry>,
    bottom: List<ShellNavigationEntry>,
    itemHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val itemModifier = Modifier.width(LauncherSizes.NavigationItemSize).height(itemHeight)
    Column(
        modifier
            .width(LauncherSizes.NavigationItemSize)
            .fillMaxHeight()
            .background(colors.secondaryBg),
    ) {
        top.forEach { ShellNavigationItem(it, IndicatorEdge.START, itemModifier) }
        Spacer(Modifier.weight(1f))
        bottom.forEach { ShellNavigationItem(it, IndicatorEdge.START, itemModifier) }
    }
}

@Composable
fun ShellNavigationBar(entries: List<ShellNavigationEntry>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        entries.forEach {
            ShellNavigationItem(it, IndicatorEdge.BOTTOM, Modifier.weight(1f).height(LauncherSizes.NavigationItemSize))
        }
    }
}

@Composable
fun ShellNavigationItem(entry: ShellNavigationEntry, indicatorEdge: IndicatorEdge, modifier: Modifier = Modifier) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val background = when {
        entry.selected -> colors.overlaySubtle
        entry.enabled && (pressed || hovered) -> colors.overlayLight
        else -> null
    }
    val interaction = if (entry.isAction) {
        Modifier.clickable(
            interactionSource = interactions,
            indication = null,
            enabled = entry.enabled,
            role = Role.Button,
            onClick = entry.onClick,
        )
    } else {
        Modifier.selectable(
            selected = entry.selected,
            interactionSource = interactions,
            indication = null,
            role = Role.Tab,
            onClick = entry.onClick,
        )
    }
    Box(modifier) {
        LauncherTooltip(text = entry.label) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(if (entry.enabled) 1f else LauncherOpacity.NAVIGATION_ACTION_DISABLED)
                    .then(if (background != null) Modifier.background(background) else Modifier)
                    .semantics { contentDescription = entry.label }
                    .then(interaction),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = entry.icon,
                    contentDescription = null,
                    tint = if (entry.selected) colors.primaryFg else colors.secondaryFgDim,
                    modifier = Modifier.size(LauncherSizes.NavigationIcon),
                )
                if (entry.selected) {
                    val indicator = when (indicatorEdge) {
                        IndicatorEdge.START ->
                            Modifier
                                .align(Alignment.CenterStart)
                                .width(LauncherSizes.NavigationIndicator)
                                .fillMaxHeight()

                        IndicatorEdge.BOTTOM ->
                            Modifier
                                .align(Alignment.BottomCenter)
                                .height(LauncherSizes.NavigationIndicator)
                                .fillMaxWidth()
                    }
                    Box(indicator.background(colors.primaryFg))
                }
            }
        }
    }
}
