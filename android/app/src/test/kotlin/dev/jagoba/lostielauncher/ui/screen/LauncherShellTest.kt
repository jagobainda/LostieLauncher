package dev.jagoba.lostielauncher.ui.screen

import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.LauncherSection
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("LauncherShell")
class LauncherShellTest {
    @ParameterizedTest
    @EnumSource(AppLanguage::class)
    fun `maps each section to its localized title`(language: AppLanguage) {
        val strings = stringsFor(language)

        LauncherSection.HOME.label(strings) shouldBe strings.titleHome
        LauncherSection.GAMES.label(strings) shouldBe strings.titleGames
        LauncherSection.LIBRARY.label(strings) shouldBe strings.titleLibrary
        LauncherSection.FAQS.label(strings) shouldBe strings.titleFaqs
        LauncherSection.SETTINGS.label(strings) shouldBe strings.titleSettings
    }
}
