package dev.jagoba.lostielauncher.util.text

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class RichTextTest {
    @Test
    fun `empty text has no runs`() {
        RichText.runs(null, "term", detectLinks = true).shouldBeEmpty()
        RichText.runs("", "term", detectLinks = true).shouldBeEmpty()
    }

    @Test
    fun `plain text without a term is one run`() {
        RichText.runs("Pokémon Añil", "  ", detectLinks = false) shouldContainExactly listOf(
            RichText.Run("Pokémon Añil", null, false),
        )
    }

    @Test
    fun `highlight ignores case and diacritics and trims the term`() {
        RichText.runs("Pokémon Añil", " anil ", detectLinks = false) shouldContainExactly listOf(
            RichText.Run("Pokémon ", null, false),
            RichText.Run("Añil", null, true),
        )
    }

    @Test
    fun `links keep trailing punctuation in the following plain run`() {
        RichText.runs("Visita www.example.com.", null, detectLinks = true) shouldContainExactly listOf(
            RichText.Run("Visita ", null, false),
            RichText.Run("www.example.com", "https://www.example.com/", false),
            RichText.Run(".", null, false),
        )
    }

    @Test
    fun `a highlight inside a link stays part of that link`() {
        RichText.runs("Ver https://example.com/guia", "guia", detectLinks = true) shouldContainExactly listOf(
            RichText.Run("Ver ", null, false),
            RichText.Run("https://example.com/", "https://example.com/guia", false),
            RichText.Run("guia", "https://example.com/guia", true),
        )
    }

    @Test
    fun `links are left as text when detection is off`() {
        RichText.runs("www.example.com", null, detectLinks = false) shouldContainExactly listOf(
            RichText.Run("www.example.com", null, false),
        )
    }
}
