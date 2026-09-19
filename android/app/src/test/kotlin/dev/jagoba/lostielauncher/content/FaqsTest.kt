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
        // Arrange — six on the desktop, in `Content/Faqs.cs`. The FAQ screen
        // filters this list and shows an empty-state message when nothing
        // matches, so a language short an entry would look like a search that
        // found nothing rather than like a bug.
        val entries = faqsFor(language)

        // Act
        val incomplete = entries.filter { it.question.isBlank() || it.answer.isBlank() }

        // Assert
        entries.size shouldBe 6
        incomplete.shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("resolves to its own list")
    fun `each language resolves to a distinct list`(language: AppLanguage) {
        // Arrange / Act — the same copy-paste risk as `stringsFor`: a wrong arm
        // in the `when` shows another language's answers.
        val entries = faqsFor(language)

        // Assert
        AppLanguage.entries
            .filter { it != language }
            .forEach { other -> (faqsFor(other) === entries) shouldBe false }
    }

    @Test
    @DisplayName("keeps the entries in the desktop's order in every language")
    fun `the entries are in a stable order across languages`() {
        // Arrange — the order is content, not incidental: it runs from "how do
        // I download a game" to "where do I report a bug", and the screen never
        // reorders it. The check that it holds across languages is that the
        // English list — the one a reviewer can read — sits where it should.
        val english = faqsFor(AppLanguage.ENG)

        // Act / Assert
        english[0].question shouldBe "How do I download a game?"
        english[4].question shouldBe "What does offline mode mean?"
        english[5].question shouldBe "I found a bug, where do I report it?"
    }
}
