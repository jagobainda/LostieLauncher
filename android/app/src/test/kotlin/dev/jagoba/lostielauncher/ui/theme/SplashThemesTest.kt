package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.ui.graphics.toArgb
import dev.jagoba.lostielauncher.model.AppTheme
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("SplashThemes")
class SplashThemesTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    @DisplayName("paints the splash screen with the palette's window background")
    fun `the splash colour resource matches the palette`(theme: AppTheme) {
        ColorResources.getValue("${theme.name.lowercase()}_primary_bg") shouldBe paletteFor(theme).primaryBg.toArgb()
    }

    @Test
    @DisplayName("gives every theme its own splash style")
    fun `no two themes share a splash style`() {
        AppTheme.entries.map(::splashThemeFor).toSet().size shouldBe AppTheme.entries.size
    }

    private companion object {
        val ColorResources: Map<String, Int> =
            Regex("""<color name="(\w+)">#([0-9A-Fa-f]{8})</color>""")
                .findAll(File("src/main/res/values/colors.xml").readText())
                .associate { it.groupValues[1] to it.groupValues[2].toLong(radix = 16).toInt() }
    }
}
