package dev.jagoba.lostielauncher.ui.component

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GameCardLayoutTest {
    @Test
    fun `a phone in portrait stacks the actions under the text`() {
        gameCardLayout(width = 339.dp, actionsWidth = 110.dp, actionsReserve = LibraryCardActionsReserve) shouldBe
            GameCardLayout.Stacked
    }

    @Test
    fun `a wide card keeps the desktop's three columns with the reserved actions column`() {
        gameCardLayout(width = 600.dp, actionsWidth = 110.dp, actionsReserve = LibraryCardActionsReserve) shouldBe
            GameCardLayout.Columns(actionsWidth = 148.dp)
    }

    @Test
    fun `the reserve, not the current actions, decides so a state change never flips the layout`() {
        gameCardLayout(width = 442.dp, actionsWidth = 110.dp, actionsReserve = LibraryCardActionsReserve) shouldBe
            GameCardLayout.Stacked
        gameCardLayout(width = 442.dp, actionsWidth = 148.dp, actionsReserve = LibraryCardActionsReserve) shouldBe
            GameCardLayout.Stacked
        gameCardLayout(width = 442.dp, actionsWidth = 0.dp, actionsReserve = LibraryCardActionsReserve) shouldBe
            GameCardLayout.Stacked
    }

    @Test
    fun `actions wider than the reserve widen the column`() {
        gameCardLayout(width = 800.dp, actionsWidth = 200.dp, actionsReserve = LibraryCardActionsReserve) shouldBe
            GameCardLayout.Columns(actionsWidth = 200.dp)
    }

    @Test
    fun `my games reserves every action so a card with an update matches one without`() {
        InstalledCardActionsReserve shouldBe 302.dp
        gameCardLayout(width = 600.dp, actionsWidth = 182.dp, actionsReserve = InstalledCardActionsReserve) shouldBe
            GameCardLayout.Stacked
        gameCardLayout(width = 700.dp, actionsWidth = 182.dp, actionsReserve = InstalledCardActionsReserve) shouldBe
            GameCardLayout.Columns(actionsWidth = 302.dp)
    }

    @Test
    fun `beside the text the actions keep their reserved widths`() {
        actionRows(labelled = listOf(54, 84), minWidth = 0, maxWidth = 302) shouldBe listOf(listOf(110, 110, 32, 32))
        actionRows(labelled = listOf(54), minWidth = 0, maxWidth = 302) shouldBe listOf(listOf(110, 32, 32))
    }

    @Test
    fun `stacked actions stretch their labelled buttons across the card`() {
        actionRows(labelled = listOf(54), minWidth = 291, maxWidth = 291) shouldBe listOf(listOf(215, 32, 32))
        actionRows(labelled = listOf(54, 84), minWidth = 400, maxWidth = 400) shouldBe
            listOf(listOf(159, 159, 32, 32))
    }

    @Test
    fun `labelled buttons shrink below their minimum before an icon wraps`() {
        actionRows(labelled = listOf(54, 84), minWidth = 291, maxWidth = 291) shouldBe
            listOf(listOf(104, 104, 32, 32))
    }

    @Test
    fun `labels too long for one row split the actions into two filled rows`() {
        actionRows(labelled = listOf(60, 100), minWidth = 230, maxWidth = 230) shouldBe
            listOf(listOf(112, 112), listOf(112, 112))
    }

    private fun actionRows(labelled: List<Int>, minWidth: Int, maxWidth: Int) = cardActionRows(
        labelled = labelled,
        icons = listOf(32, 32),
        labelledMin = 110,
        gap = 6,
        minWidth = minWidth,
        maxWidth = maxWidth,
    )
}
