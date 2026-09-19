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
    // ---- parity with the desktop themes ----

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    @DisplayName("matches its desktop resource dictionary, colour for colour")
    fun `matches the desktop theme exactly`(theme: AppTheme) {
        // Arrange — DesktopPalettes holds the hex strings exactly as the XAML
        // writes them, and is parsed here rather than by the production code,
        // so a bug in the `#rrggbb` to `0xAARRGGBB` conversion cannot hide
        // behind a test that shares it. A failure means the palette drifted,
        // which is a bug in the port and never a licence to change the colour.
        val expected = DesktopPalettes.getValue(theme)
        val actual = paletteFor(theme).byDesktopKey()

        // Act / Assert — one assertion per key, so a failure names the key.
        expected.forEach { (key, hex) ->
            actual.getValue(key).toArgb() shouldBe parseWpfColor(hex)
        }
    }

    @Test
    @DisplayName("covers all ten themes and all fourteen keys of each")
    fun `the parity table is complete`() {
        // Arrange / Act / Assert — guards the table above rather than the
        // values: a theme or a key missing from DesktopPalettes would make
        // every case pass by simply not being checked.
        DesktopPalettes.keys shouldContainExactly AppTheme.entries.toSet()
        DesktopPalettes.values.forEach { it.size shouldBe 14 }
    }

    // ---- the structure the desktop palettes share ----

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    @DisplayName("uses the same five-step overlay alpha ramp as every other theme")
    fun `overlays follow the shared ramp`(theme: AppTheme) {
        // Arrange — spec/06-design-tokens.md: the five overlays are the same
        // alpha ramp in all ten themes, over white in a dark theme and black in
        // a light one. They are alpha, not opaque, and a palette that made one
        // of them solid would composite wrongly everywhere.
        val palette = paletteFor(theme)
        val base = if (palette.isDark()) 0xFFFFFF else 0x000000

        // Act / Assert
        listOf(
            0x1A to palette.overlaySubtle,
            0x22 to palette.overlayLight,
            0x33 to palette.overlayMuted,
            0x55 to palette.overlayMedium,
            0x88 to palette.overlayStrong,
        ).forEach { (alpha, colour) ->
            // The parentheses are load-bearing: `shouldBe` and `or` are both
            // infix and bind left to right, so without them this asserts
            // against the alpha alone and passes for a black overlay.
            colour.toArgb() shouldBe ((alpha shl 24) or base)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    @DisplayName("derives its dim foreground from its own text colour, not a separate hue")
    fun `the dim foreground is the text colour with an alpha prefix`(theme: AppTheme) {
        // Arrange — spec/06-design-tokens.md: SecondaryFgDimColor is
        // SecondaryFgColor with an alpha of 88 in six themes and 99 in four. It
        // is not an independent colour, and a port that re-picked it would
        // drift invisibly.
        val palette = paletteFor(theme)

        // Act
        val dimRgb = palette.secondaryFgDim.toArgb() and 0xFFFFFF
        val alpha = (palette.secondaryFgDim.toArgb() ushr 24) and 0xFF

        // Assert
        dimRgb shouldBe (palette.secondaryFg.toArgb() and 0xFFFFFF)
        (alpha == 0x88 || alpha == 0x99) shouldBe true
    }

    @Test
    @DisplayName("classifies seven themes dark and three light")
    fun `light and dark are derived from the overlays`() {
        // Arrange / Act — spec/06-design-tokens.md names them. Nothing stores
        // this on the desktop; here it is read back out of the overlay hue, so
        // the assertion is that the derivation agrees with the specification.
        val dark = AppTheme.entries.filter { paletteFor(it).isDark() }.toSet()

        // Assert
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
        // Arrange / Act — recorded rather than fixed. All three light themes
        // define TertiaryBgColor equal to PrimaryBgColor, so a text input has
        // no visual separation from the page behind it. spec/06-design-tokens.md
        // puts it on the contrast review list, which port plan step 15 owns;
        // this test is here so that a later step "tidying" one of them has to
        // do it deliberately.
        val palette = paletteFor(theme)

        // Assert
        palette.tertiaryBg shouldBe palette.primaryBg
    }

    @Test
    @DisplayName("gives every theme its own palette")
    fun `no two themes share a palette`() {
        // Arrange / Act — a copy-paste in a ten-way `when` is invisible
        // otherwise: the wrong theme simply looks like another one.
        val palettes = AppTheme.entries.map { paletteFor(it) }

        // Assert
        palettes.toSet().size shouldBe AppTheme.entries.size
        AppTheme.Default shouldBe AppTheme.Volcarona
        paletteFor(AppTheme.Default) shouldNotBe paletteFor(AppTheme.Zoroark)
    }

    private companion object {
        /** The palette keyed by the desktop's own XAML colour keys. */
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

        /**
         * A WPF colour string to a packed ARGB int. Six digits are opaque RGB,
         * eight are AARRGGBB — the two forms `Themes/` uses and nothing else.
         */
        fun parseWpfColor(hex: String): Int {
            val digits = hex.removePrefix("#")
            require(digits.length == 6 || digits.length == 8) { "not a WPF colour: $hex" }
            val padded = if (digits.length == 6) "FF$digits" else digits
            return padded.toLong(radix = 16).toInt()
        }
    }
}
