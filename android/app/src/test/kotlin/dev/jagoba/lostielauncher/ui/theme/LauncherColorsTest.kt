package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.kotest.matchers.shouldBe
import java.lang.reflect.Modifier
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Volcarona palette")
class LauncherColorsTest {
    // ---- parity with the desktop theme ----

    @ParameterizedTest(name = "{0} is #{1}")
    @CsvSource(
        // Every value here is the desktop's, from spec/06-design-tokens.md
        // (Themes/Volcarona.xaml). A failure means the palette drifted, which
        // is a bug in the port and never a licence to change the colour.
        "primaryBg,        FF4D4949",
        "secondaryBg,      FF3A3737",
        "tertiaryBg,       FF2E2C2C",
        "primaryFg,        FFF08058",
        "primaryFgHover,   FFD06038",
        "primaryFgPressed, FFB04020",
        "secondaryFg,      FFF3F7FA",
        "secondaryFgDim,   88F3F7FA",
        "success,          FF2E7D32",
        "overlaySubtle,    1AFFFFFF",
        "overlayLight,     22FFFFFF",
        "overlayMuted,     33FFFFFF",
        "overlayMedium,    55FFFFFF",
        "overlayStrong,    88FFFFFF",
    )
    fun `matches the desktop theme exactly`(role: String, expectedArgb: String) {
        // Arrange — look the role up by name, so renaming a property fails
        // loudly instead of silently dropping a case from the table.
        val colour = rolesByName.getValue(role)

        // Act
        val argb = "%08X".format(colour.toArgb())

        // Assert
        argb shouldBe expectedArgb
    }

    @Test
    @DisplayName("covers every colour key the desktop themes define")
    fun `has fourteen roles, and the table above covers all of them`() {
        // Arrange — read the roles off the class itself rather than trusting
        // the table. Counting the table's own entries would be circular: a
        // fifteenth property nobody added a row for would leave it green, and
        // that is precisely the drift this test exists to catch. (A property
        // *removed* from LauncherColors cannot slip through either — the table
        // would stop compiling.)
        val declared = LauncherColors::class.java.declaredFields
            .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        // Act / Assert — each desktop theme defines 14 colours, and every one
        // of them is checked by the table above.
        declared shouldBe rolesByName.keys
        declared.size shouldBe 14
    }

    private companion object {
        val rolesByName: Map<String, Color> = with(VolcaronaColors) {
            mapOf(
                "primaryBg" to primaryBg,
                "secondaryBg" to secondaryBg,
                "tertiaryBg" to tertiaryBg,
                "primaryFg" to primaryFg,
                "primaryFgHover" to primaryFgHover,
                "primaryFgPressed" to primaryFgPressed,
                "secondaryFg" to secondaryFg,
                "secondaryFgDim" to secondaryFgDim,
                "success" to success,
                "overlaySubtle" to overlaySubtle,
                "overlayLight" to overlayLight,
                "overlayMuted" to overlayMuted,
                "overlayMedium" to overlayMedium,
                "overlayStrong" to overlayStrong,
            )
        }
    }
}
