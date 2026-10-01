package dev.jagoba.lostielauncher.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("AppTheme and AppLanguage, as persisted settings")
class AppearanceSettingsTest {

    @Test
    @DisplayName("defaults to Volcarona and Spanish, as the desktop does")
    fun `the defaults are the desktop's`() {
        AppTheme.Default shouldBe AppTheme.Volcarona
        AppLanguage.Default shouldBe AppLanguage.ESP
        Appearance() shouldBe Appearance(AppTheme.Volcarona, AppLanguage.ESP)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    fun `every theme round-trips through its persisted name`(theme: AppTheme) {
        AppTheme.fromNameOrDefault(theme.name) shouldBe theme
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    fun `every language round-trips through its persisted name`(language: AppLanguage) {
        AppLanguage.fromNameOrDefault(language.name) shouldBe language
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = ["", "volcarona", "Pikachu", "0", " Zoroark "])
    fun `an unrecognised theme name falls back to the default`(stored: String?) {
        AppTheme.fromNameOrDefault(stored) shouldBe AppTheme.Volcarona
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = ["", "es", "Esp", "SPANISH", "0"])
    fun `an unrecognised language name falls back to the default`(stored: String?) {
        AppLanguage.fromNameOrDefault(stored) shouldBe AppLanguage.ESP
    }

    @Test
    @DisplayName("keeps the desktop's member order, because the pickers list them in it")
    fun `member order matches the desktop`() {
        AppLanguage.entries.map { it.name } shouldBe
            listOf("ESP", "ENG", "CAT", "EUS", "GAL", "POR", "VAL", "FRA")
        AppTheme.entries.map { it.name } shouldBe
            listOf(
                "Volcarona",
                "Zoroark",
                "Infernape",
                "Torterra",
                "Empoleon",
                "Mewtwo",
                "Cefireon",
                "Sylveon",
                "Astrem",
                "Auretoskos",
            )
    }

    @Test
    @DisplayName("keeps the two wire codes that are not the obvious ones")
    fun `the CDN language codes are the desktop's`() {
        AppLanguage.entries.map { it.code } shouldBe
            listOf("es", "en", "ca", "eu", "gl", "pt", "val", "fr")
    }

    @Test
    @DisplayName("shows each language's own endonym, untranslated")
    fun `display names are the desktop's Description attributes`() {
        AppLanguage.entries.map { it.displayName } shouldBe
            listOf("Español", "English", "Català", "Euskera", "Galego", "Português", "Valencià", "Français")
    }
}
