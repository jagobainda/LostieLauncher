package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing

internal sealed interface GameCardLayout {
    data class Columns(val actionsWidth: Dp) : GameCardLayout

    data object Stacked : GameCardLayout
}

internal fun gameCardLayout(width: Dp, actionsWidth: Dp, actionsReserve: Dp): GameCardLayout {
    val column = maxOf(actionsWidth, actionsReserve)
    val text = width - LauncherSizes.GameCardLogoWidth - column - LauncherSpacing.Card * 2
    return if (text >= LauncherSizes.GameCardTitleWidth) GameCardLayout.Columns(column) else GameCardLayout.Stacked
}

internal fun cardActionRows(
    labelled: List<Int>,
    icons: List<Int>,
    labelledMin: Int,
    gap: Int,
    minWidth: Int,
    maxWidth: Int,
): List<List<Int>> {
    val fixed = icons.sum() + gap * (labelled.size + icons.size - 1)
    val preferred = labelled.map { maxOf(it, labelledMin) }
    return when {
        preferred.sum() + fixed <= maxWidth ->
            listOf(spread(preferred, maxOf(preferred.sum(), minWidth - fixed)) + icons)

        labelled.sum() + fixed <= maxWidth -> listOf(spread(labelled, maxWidth - fixed) + icons)

        else -> listOf(
            spread(labelled, maxWidth - gap * (labelled.size - 1)),
            spread(icons, maxWidth - gap * (icons.size - 1)),
        )
    }
}

private fun spread(bases: List<Int>, space: Int): List<Int> {
    var remaining = space
    var count = bases.size
    for (base in bases.sortedDescending()) {
        if (base * count <= remaining) break
        remaining -= base
        count--
    }
    val level = if (count > 0) remaining / count else 0
    return bases.map { maxOf(it, level) }
}

@Composable
internal fun CardActions(labelled: @Composable () -> Unit, icons: @Composable () -> Unit) {
    Layout(contents = listOf(labelled, icons)) { (labelledSlot, iconSlot), constraints ->
        val gap = LauncherSpacing.Small.roundToPx()
        val rows = cardActionRows(
            labelled = labelledSlot.map { it.maxIntrinsicWidth(Constraints.Infinity) },
            icons = iconSlot.map { it.maxIntrinsicWidth(Constraints.Infinity) },
            labelledMin = LauncherSizes.CardButtonMinWidth.roundToPx(),
            gap = gap,
            minWidth = constraints.minWidth,
            maxWidth = constraints.maxWidth,
        )
        val measurables = (labelledSlot + iconSlot).iterator()
        val placeables = rows.map { row -> row.map { measurables.next().measure(Constraints.fixedWidth(it)) } }
        val rowWidths = rows.map { it.sum() + gap * (it.size - 1) }
        val rowHeights = placeables.map { row -> row.maxOf { it.height } }
        val width = constraints.constrainWidth(rowWidths.max())
        layout(width, constraints.constrainHeight(rowHeights.sum() + gap * (rows.size - 1))) {
            var y = 0
            rows.forEachIndexed { index, row ->
                var x = width - rowWidths[index]
                row.forEachIndexed { column, slotWidth ->
                    placeables[index][column].place(x, y)
                    x += slotWidth + gap
                }
                y += rowHeights[index] + gap
            }
        }
    }
}

@Composable
internal fun GameCardColumns(
    logo: @Composable () -> Unit,
    text: @Composable () -> Unit,
    status: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    actionsReserve: Dp,
    modifier: Modifier = Modifier,
    fillStackedActions: Boolean = false,
) {
    Layout(
        contents = listOf(
            { Box { logo() } },
            { Box { text() } },
            { Box { status() } },
            { Box(propagateMinConstraints = true) { actions() } },
        ),
        modifier = modifier.fillMaxWidth(),
    ) { (logoSlot, textSlot, statusSlot, actionsSlot), constraints ->
        val width = constraints.maxWidth
        val inset = LauncherSpacing.Card.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val fixed = { slotWidth: Int -> slotWidth.coerceAtLeast(0).let { loose.copy(minWidth = it, maxWidth = it) } }
        val actions = actionsSlot.first()
        val logoPlaceable = logoSlot.first().measure(loose)
        val arrangement = gameCardLayout(
            width = width.toDp(),
            actionsWidth = actions.maxIntrinsicWidth(Constraints.Infinity).toDp(),
            actionsReserve = actionsReserve,
        )
        when (arrangement) {
            is GameCardLayout.Columns -> {
                val columnWidth = arrangement.actionsWidth.roundToPx()
                val textWidth = width - logoPlaceable.width - columnWidth - 2 * inset
                val textPlaceable = textSlot.first().measure(fixed(textWidth))
                val statusPlaceable = statusSlot.first().measure(fixed(textWidth))
                val actionsPlaceable = actions.measure(loose.copy(maxWidth = columnWidth))
                val statusGap = if (statusPlaceable.height > 0) LauncherSpacing.Medium.roundToPx() else 0
                val middle = textPlaceable.height + statusGap + statusPlaceable.height
                val height = maxOf(logoPlaceable.height, middle, actionsPlaceable.height)
                val middleTop = (height - middle) / 2
                layout(width, height) {
                    logoPlaceable.place(0, (height - logoPlaceable.height) / 2)
                    textPlaceable.place(logoPlaceable.width + inset, middleTop)
                    statusPlaceable.place(logoPlaceable.width + inset, middleTop + textPlaceable.height + statusGap)
                    actionsPlaceable.place(width - actionsPlaceable.width, (height - actionsPlaceable.height) / 2)
                }
            }

            GameCardLayout.Stacked -> {
                val gap = LauncherSpacing.Large.roundToPx()
                val textPlaceable = textSlot.first().measure(fixed(width - logoPlaceable.width - inset))
                val actionsPlaceable = actions.measure(if (fillStackedActions) fixed(width) else loose)
                val reserve = maxOf(actionsReserve.roundToPx(), actionsPlaceable.width)
                val statusPlaceable = statusSlot.first().measure(fixed(width - reserve - gap))
                val top = maxOf(logoPlaceable.height, textPlaceable.height)
                val footer = maxOf(
                    LauncherSizes.CardButtonHeight.roundToPx(),
                    statusPlaceable.height,
                    actionsPlaceable.height,
                )
                val footerTop = top + gap
                layout(width, footerTop + footer) {
                    logoPlaceable.place(0, (top - logoPlaceable.height) / 2)
                    textPlaceable.place(logoPlaceable.width + inset, (top - textPlaceable.height) / 2)
                    statusPlaceable.place(0, footerTop + (footer - statusPlaceable.height) / 2)
                    actionsPlaceable.place(
                        width - actionsPlaceable.width,
                        footerTop + (footer - actionsPlaceable.height) / 2,
                    )
                }
            }
        }
    }
}
