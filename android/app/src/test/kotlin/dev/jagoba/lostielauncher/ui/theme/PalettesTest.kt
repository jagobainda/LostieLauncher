package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.jagoba.lostielauncher.model.AppTheme
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("Palettes")
class PalettesTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    @DisplayName("matches its desktop resource dictionary, colour for colour")
    fun `matches the desktop theme exactly`(theme: AppTheme) {
        val expected = DesktopPalettes.getValue(theme)
        val actual = paletteFor(theme).byDesktopKey()

        expected.forEach { (key, hex) ->
            actual.getValue(key).toArgb() shouldBe parseWpfColor(hex)
        }
    }

    @Test
    @DisplayName("covers all ten themes and all fourteen keys of each")
    fun `the parity table is complete`() {
        DesktopPalettes.keys shouldContainExactly AppTheme.entries.toSet()
        DesktopPalettes.values.forEach { it.size shouldBe 14 }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    @DisplayName("uses the same five-step overlay alpha ramp as every other theme")
    fun `overlays follow the shared ramp`(theme: AppTheme) {
        val palette = paletteFor(theme)
        val base = if (palette.isDark()) 0xFFFFFF else 0x000000

        listOf(
            0x1A to palette.overlaySubtle,
            0x22 to palette.overlayLight,
            0x33 to palette.overlayMuted,
            0x55 to palette.overlayMedium,
            0x88 to palette.overlayStrong,
        ).forEach { (alpha, colour) ->
            colour.toArgb() shouldBe ((alpha shl 24) or base)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    @DisplayName("derives its dim foreground from its own text colour, not a separate hue")
    fun `the dim foreground is the text colour with an alpha prefix`(theme: AppTheme) {
        val palette = paletteFor(theme)

        val dimRgb = palette.secondaryFgDim.toArgb() and 0xFFFFFF
        val alpha = (palette.secondaryFgDim.toArgb() ushr 24) and 0xFF

        dimRgb shouldBe (palette.secondaryFg.toArgb() and 0xFFFFFF)
        (alpha == 0x88 || alpha == 0x99) shouldBe true
    }

    @Test
    @DisplayName("classifies seven themes dark and three light")
    fun `light and dark are derived from the overlays`() {
        val dark = AppTheme.entries.filter { paletteFor(it).isDark() }.toSet()

        dark shouldBe setOf(
            AppTheme.Volcarona,
            AppTheme.Zoroark,
            AppTheme.Infernape,
            AppTheme.Torterra,
            AppTheme.Empoleon,
            AppTheme.Mewtwo,
            AppTheme.Auretoskos,
        )
        (AppTheme.entries - dark) shouldContainExactly
            listOf(AppTheme.Cefireon, AppTheme.Sylveon, AppTheme.Astrem)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AppTheme::class, names = ["Cefireon", "Sylveon", "Astrem"])
    @DisplayName("keeps the light themes' tertiary background equal to their primary one")
    fun `light themes have no inset surface separation`(theme: AppTheme) {
        val palette = paletteFor(theme)

        palette.tertiaryBg shouldBe palette.primaryBg
    }

    @Test
    @DisplayName("gives every theme its own palette")
    fun `no two themes share a palette`() {
        val palettes = AppTheme.entries.map { paletteFor(it) }

        palettes.toSet().size shouldBe AppTheme.entries.size
        AppTheme.Default shouldBe AppTheme.Volcarona
        paletteFor(AppTheme.Default) shouldNotBe paletteFor(AppTheme.Zoroark)
    }

    private companion object {
        fun LauncherColors.byDesktopKey(): Map<String, Color> = mapOf(
            "PrimaryBgColor" to primaryBg,
            "SecondaryBgColor" to secondaryBg,
            "TertiaryBgColor" to tertiaryBg,
            "PrimaryFgColor" to primaryFg,
            "PrimaryFgHoverColor" to primaryFgHover,
            "PrimaryFgPressedColor" to primaryFgPressed,
            "SecondaryFgColor" to secondaryFg,
            "SecondaryFgDimColor" to secondaryFgDim,
            "SuccessColor" to success,
            "OverlaySubtleColor" to overlaySubtle,
            "OverlayLightColor" to overlayLight,
            "OverlayMutedColor" to overlayMuted,
            "OverlayMediumColor" to overlayMedium,
            "OverlayStrongColor" to overlayStrong,
        )

        fun parseWpfColor(hex: String): Int {
            val digits = hex.removePrefix("#")
            require(digits.length == 6 || digits.length == 8) { "not a WPF colour: $hex" }
            val padded = if (digits.length == 6) "FF$digits" else digits
            return padded.toLong(radix = 16).toInt()
        }
    }
}
