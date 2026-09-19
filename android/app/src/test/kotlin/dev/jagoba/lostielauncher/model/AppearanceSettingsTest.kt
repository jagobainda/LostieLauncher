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
    // ---- defaults ----

    @Test
    @DisplayName("defaults to Volcarona and Spanish, as the desktop does")
    fun `the defaults are the desktop's`() {
        // Arrange / Act / Assert — `AppSettings` in the desktop initialises
        // Theme to Volcarona and Language to Esp, and spec/04-data-model.md
        // records the same two as the fallbacks.
        AppTheme.Default shouldBe AppTheme.Volcarona
        AppLanguage.Default shouldBe AppLanguage.ESP
        Appearance() shouldBe Appearance(AppTheme.Volcarona, AppLanguage.ESP)
    }

    // ---- round-tripping through storage ----

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppTheme::class)
    fun `every theme round-trips through its persisted name`(theme: AppTheme) {
        // Arrange / Act / Assert — the stored form is the member name. A theme
        // whose name did not resolve would silently become Volcarona on the
        // next launch, which looks like the setting not saving.
        AppTheme.fromNameOrDefault(theme.name) shouldBe theme
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    fun `every language round-trips through its persisted name`(language: AppLanguage) {
        // Arrange / Act / Assert
        AppLanguage.fromNameOrDefault(language.name) shouldBe language
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = ["", "volcarona", "Pikachu", "0", " Zoroark "])
    fun `an unrecognised theme name falls back to the default`(stored: String?) {
        // Arrange / Act / Assert — spec/04-data-model.md: a Theme that is not a
        // defined value logs and falls back to Volcarona. Note the case: the
        // match is exact, so `"volcarona"` is not Volcarona. That is deliberate
        // — a loose match would quietly accept a value nothing ever wrote and
        // hide whatever wrote it.
        AppTheme.fromNameOrDefault(stored) shouldBe AppTheme.Volcarona
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = ["", "es", "Esp", "SPANISH", "0"])
    fun `an unrecognised language name falls back to the default`(stored: String?) {
        // Arrange / Act / Assert — `"es"` is the wire code, not the member
        // name, and `"Esp"` is the desktop's spelling of the member. Neither is
        // what this side stores, and accepting either would mean two formats in
        // the file.
        AppLanguage.fromNameOrDefault(stored) shouldBe AppLanguage.ESP
    }

    // ---- the parts the rest of the port depends on ----

    @Test
    @DisplayName("keeps the desktop's member order, because the pickers list them in it")
    fun `member order matches the desktop`() {
        // Arrange / Act / Assert — spec/05-localization.md and
        // spec/06-design-tokens.md both list them in this order, and the
        // Settings pickers show them in it on both sides.
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
        // Arrange / Act / Assert — Spanish is `es`, not `esp`, and Valencian is
        // `val`, a three-letter code among two-letter ones. They resolve the
        // remote home content, so getting one wrong shows the wrong language's
        // news with no other symptom.
        AppLanguage.entries.map { it.code } shouldBe
            listOf("es", "en", "ca", "eu", "gl", "pt", "val", "fr")
    }

    @Test
    @DisplayName("shows each language's own endonym, untranslated")
    fun `display names are the desktop's Description attributes`() {
        // Arrange / Act / Assert — the Settings picker shows all eight at once,
        // so they are deliberately not localized: a reader has to be able to
        // find their own in a language they cannot read.
        AppLanguage.entries.map { it.displayName } shouldBe
            listOf("Español", "English", "Català", "Euskera", "Galego", "Português", "Valencià", "Français")
    }
}
