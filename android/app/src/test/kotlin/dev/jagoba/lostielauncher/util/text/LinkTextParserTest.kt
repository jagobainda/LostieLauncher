package dev.jagoba.lostielauncher.util.text

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

/** Ported declaration for declaration from the desktop's `Utils/LinkTextParserTests.cs`. */
@DisplayName("LinkTextParser.parse")
class LinkTextParserTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = [""])
    fun `returns nothing for null or empty text`(text: String?) {
        // Act & Assert
        LinkTextParser.parse(text).shouldBeEmpty()
    }

    @Test
    fun `returns a single plain segment when there are no links`() {
        // Act
        val segments = LinkTextParser.parse("Texto normal sin enlaces.")

        // Assert
        segments.size shouldBe 1
        segments[0].isLink.shouldBeFalse()
        segments[0].text shouldBe "Texto normal sin enlaces."
    }

    @Test
    fun `detects an https url`() {
        // Act
        val segments = LinkTextParser.parse("Mira https://github.com/jagobainda/LostieLauncher para más info")

        // Assert
        segments.size shouldBe 3
        segments[0].text shouldBe "Mira "
        segments[1].isLink.shouldBeTrue()
        segments[1].text shouldBe "https://github.com/jagobainda/LostieLauncher"
        segments[1].url shouldBe "https://github.com/jagobainda/LostieLauncher"
        segments[2].text shouldBe " para más info"
    }

    @Test
    fun `detects a bare domain with a path and normalizes it to https`() {
        // Arrange — the real case from the v0.9.0 news item: no scheme, and a full stop
        // ending the sentence right after the path.
        // Act
        val segments = LinkTextParser.parse(
            "Puedes consultar el changelog completo en github.com/jagobainda/LostieLauncher/releases.",
        )

        // Assert
        segments.size shouldBe 3
        segments[1].isLink.shouldBeTrue()
        segments[1].text shouldBe "github.com/jagobainda/LostieLauncher/releases"
        segments[1].url shouldBe "https://github.com/jagobainda/LostieLauncher/releases"
        segments[2].text shouldBe "."
    }

    @Test
    fun `detects a www-prefixed domain`() {
        // Act
        val segments = LinkTextParser.parse("Visita www.example.org ahora")

        // Assert — the target is the canonical form, which is where the trailing slash comes from.
        segments[1].isLink.shouldBeTrue()
        segments[1].url shouldBe "https://www.example.org/"
    }

    @Test
    fun `trims trailing punctuation`() {
        // Act
        val segments = LinkTextParser.parse("(ver https://example.com/docs), ¿vale?")

        // Assert
        segments.single { it.isLink }.text shouldBe "https://example.com/docs"
        segments.last().text shouldBe "), ¿vale?"
    }

    @Test
    fun `detects several links`() {
        // Act
        val segments = LinkTextParser.parse("Repo: github.com/a/b y web: https://example.com")

        // Assert
        segments.count { it.isLink } shouldBe 2
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            // An unknown TLD is not a link.
            "Guarda el archivo.txt en la carpeta",
            // An email address is not a link.
            "Escríbenos a soporte@example.com si falla",
            // Dotted numbers are not a link.
            "La versión v0.9.0 ya está disponible",
        ],
    )
    fun `ignores what is not a link`(text: String) {
        // Act & Assert
        LinkTextParser.parse(text).forEach { it.isLink.shouldBeFalse() }
    }

    @Test
    fun `ignores http urls`() {
        // Arrange — HTTPS only, the same policy the outbound-link guard enforces.
        // Act
        val segments = LinkTextParser.parse("Antiguo mirror en http://example.com/old sin soporte")

        // Assert
        segments.forEach { it.isLink.shouldBeFalse() }
    }
}
