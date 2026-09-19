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
        // Arrange — read the roles off the class itself rather than listing
        // them twice. A fifteenth property nobody noticed would otherwise slip
        // past every palette test, because those check the values of the roles
        // they know about, not that the set of roles is right.
        val declared = LauncherColors::class.java.declaredFields
            .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
            .map { it.name }

        // Act / Assert — the names are the desktop's colour keys with `Color`
        // dropped, in the order `Themes/Volcarona.xaml` declares them.
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
        // Arrange / Act — `copy` and `componentN` are generated only for a
        // data class. The property this guards is not cosmetic: the desktop can
        // lose a colour key from one theme and only find out when a user
        // selects it, and the whole reason this is a constructor-per-role
        // record is that the same mistake cannot compile here.
        //
        // `startsWith` rather than an exact name because `Color` is an inline
        // value class, so Kotlin mangles every generated signature that
        // mentions one — `copy-0d7_KjU`, not `copy`.
        val names = LauncherColors::class.java.declaredMethods.map { it.name }
        val hasCopy = names.any { it.startsWith("copy") } && names.any { it.startsWith("component1") }

        // Assert
        hasCopy shouldBe true
    }
}
