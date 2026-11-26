package dev.jagoba.lostielauncher.ui.component

import androidx.compose.ui.unit.IntRect
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LauncherComboBox popup placement")
class LauncherComboBoxTest {
    private val face = IntRect(left = 0, top = 900, right = 400, bottom = 980)

    @Test
    fun `a list that fits opens below the combo`() {
        val y = comboBoxPopupY(face, ComboBoxPopupSpace(above = 800, below = 600), popupHeight = 500, gap = 4)

        y shouldBe 984
    }

    @Test
    fun `a list that does not fit below opens above the combo when there is more room there`() {
        val y = comboBoxPopupY(face, ComboBoxPopupSpace(above = 800, below = 600), popupHeight = 700, gap = 4)

        y shouldBe 196
    }

    @Test
    fun `a list that does not fit below stays below when there is no more room above`() {
        val y = comboBoxPopupY(face, ComboBoxPopupSpace(above = 300, below = 600), popupHeight = 700, gap = 4)

        y shouldBe 984
    }
}
