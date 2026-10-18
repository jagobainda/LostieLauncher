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
}
