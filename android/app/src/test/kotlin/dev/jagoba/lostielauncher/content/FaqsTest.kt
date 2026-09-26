package dev.jagoba.lostielauncher.content

import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("Faqs")
class FaqsTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("has the desktop's six entries, all filled in")
    fun `every language has six complete entries`(language: AppLanguage) {
        val entries = faqsFor(language)

        val incomplete = entries.filter { it.question.isBlank() || it.answer.isBlank() }

        entries.size shouldBe 6
        incomplete.shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("resolves to its own list")
    fun `each language resolves to a distinct list`(language: AppLanguage) {
        val entries = faqsFor(language)

        AppLanguage.entries
            .filter { it != language }
            .forEach { other -> (faqsFor(other) === entries) shouldBe false }
    }

    @Test
    @DisplayName("keeps the entries in the desktop's order in every language")
    fun `the entries are in a stable order across languages`() {
        val english = faqsFor(AppLanguage.ENG)

        english[0].question shouldBe "How do I download a game?"
        english[4].question shouldBe "What does offline mode mean?"
        english[5].question shouldBe "I found a bug, where do I report it?"
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("never points to the download directory setting Android does not have")
    fun `no answer names the removed download directory setting`(language: AppLanguage) {
        val setting = stringsFor(language).settingsDownloadDir

        faqsFor(language).filter { it.answer.contains(setting, ignoreCase = true) }.shouldBeEmpty()
    }
}
