package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ShellNavigationLayoutTest {
    @Test
    fun `a compact width always uses the bottom bar`() {
        shellNavigationLayout(width = 411.dp, availableHeight = 700.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Bar
    }

    @Test
    fun `a tall medium width keeps the desktop item size and inset`() {
        shellNavigationLayout(width = 800.dp, availableHeight = 1_100.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Rail(itemHeight = 60.dp, verticalInset = 15.dp)
        shellNavigationLayout(width = 800.dp, availableHeight = 390.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Rail(itemHeight = 60.dp, verticalInset = 15.dp)
    }

    @Test
    fun `items share the height before the inset gives way`() {
        shellNavigationLayout(width = 914.dp, availableHeight = 330.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Rail(itemHeight = 50.dp, verticalInset = 15.dp)
    }

    @Test
    fun `phone landscape shrinks the inset instead of an item below the touch target`() {
        shellNavigationLayout(width = 914.dp, availableHeight = 313.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Rail(itemHeight = 48.dp, verticalInset = 12.5.dp)
        shellNavigationLayout(width = 914.dp, availableHeight = 288.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Rail(itemHeight = 48.dp, verticalInset = 0.dp)
    }

    @Test
    fun `the bottom bar is the fallback only when six touch targets cannot fit`() {
        shellNavigationLayout(width = 914.dp, availableHeight = 287.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Bar
    }

    @Test
    fun `the breakpoint itself selects the rail`() {
        shellNavigationLayout(width = 600.dp, availableHeight = 390.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Rail(itemHeight = 60.dp, verticalInset = 15.dp)
        shellNavigationLayout(width = 599.dp, availableHeight = 390.dp, itemCount = 6) shouldBe
            ShellNavigationLayout.Bar
    }
}
