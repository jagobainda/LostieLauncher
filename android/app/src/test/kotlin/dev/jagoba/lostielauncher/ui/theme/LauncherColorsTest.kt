package dev.jagoba.lostielauncher.ui.theme

import io.kotest.matchers.shouldBe
import java.lang.reflect.Modifier
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LauncherColors")
class LauncherColorsTest {
    @Test
    @DisplayName("declares exactly the fourteen colour keys the desktop themes define")
    fun `has fourteen roles, and they are the desktop's`() {
        val declared = LauncherColors::class.java.declaredFields
            .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
            .map { it.name }

        declared shouldBe listOf(
            "primaryBg",
            "secondaryBg",
            "tertiaryBg",
            "primaryFg",
            "primaryFgHover",
            "primaryFgPressed",
            "secondaryFg",
            "secondaryFgDim",
            "success",
            "overlaySubtle",
            "overlayLight",
            "overlayMuted",
            "overlayMedium",
            "overlayStrong",
        )
    }

    @Test
    @DisplayName("is a data class, so a new role is a compile error in all ten palettes")
    fun `is a data class`() {
        val names = LauncherColors::class.java.declaredMethods.map { it.name }
        val hasCopy = names.any { it.startsWith("copy") } && names.any { it.startsWith("component1") }

        hasCopy shouldBe true
    }
}
