package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.lerp
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import kotlin.math.floor

@Immutable
data class ShellNavigationEntry(
    val key: String,
    val label: String,
    val icon: Painter,
    val selected: Boolean,
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
    selection: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val itemModifier = Modifier.width(LauncherSizes.NavigationItemSize).height(itemHeight)
    val count = top.size + bottom.size
    BoxWithConstraints(
        modifier
            .width(LauncherSizes.NavigationItemSize)
            .fillMaxHeight()
            .background(colors.secondaryBg),
    ) {
        SelectionIndicator(IndicatorEdge.START, itemModifier) {
            val y = selectionOffset(selection()) {
                if (it < top.size) itemHeight * it else maxHeight - itemHeight * (count - it)
            }
            IntOffset(0, y.roundToPx())
        }
        Column(Modifier.fillMaxSize().selectableGroup()) {
            top.forEach { ShellNavigationItem(it, itemModifier) }
            Spacer(Modifier.weight(1f))
            bottom.forEach { ShellNavigationItem(it, itemModifier) }
        }
    }
}

@Composable
fun ShellNavigationBar(entries: List<ShellNavigationEntry>, selection: () -> Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val itemWidth = maxWidth / entries.size
        SelectionIndicator(IndicatorEdge.BOTTOM, Modifier.width(itemWidth).height(LauncherSizes.NavigationItemSize)) {
            IntOffset(selectionOffset(selection()) { itemWidth * it }.roundToPx(), 0)
        }
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            entries.forEach {
                ShellNavigationItem(it, Modifier.weight(1f).height(LauncherSizes.NavigationItemSize))
            }
        }
    }
}

private fun selectionOffset(selection: Float, offsetOf: (Int) -> Dp): Dp {
    val index = floor(selection).toInt()
    return lerp(offsetOf(index), offsetOf(index + 1), selection - index)
}

@Composable
private fun SelectionIndicator(edge: IndicatorEdge, modifier: Modifier = Modifier, offset: Density.() -> IntOffset) {
    val colors = LocalLauncherColors.current
    Box(modifier.offset(offset).background(colors.overlaySubtle)) {
        val line = when (edge) {
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
        Box(line.background(colors.primaryFg))
    }
}

@Composable
fun ShellNavigationItem(entry: ShellNavigationEntry, modifier: Modifier = Modifier) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val background = colors.overlayLight.takeIf { !entry.selected && (pressed || hovered) }
    Box(modifier) {
        LauncherTooltip(text = entry.label) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (background != null) Modifier.background(background) else Modifier)
                    .semantics { contentDescription = entry.label }
                    .selectable(
                        selected = entry.selected,
                        interactionSource = interactions,
                        indication = null,
                        role = Role.Tab,
                        onClick = entry.onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = entry.icon,
                    contentDescription = null,
                    tint = if (entry.selected) colors.primaryFg else colors.secondaryFgDim,
                    modifier = Modifier.size(LauncherSizes.NavigationIcon),
                )
            }
        }
    }
}
