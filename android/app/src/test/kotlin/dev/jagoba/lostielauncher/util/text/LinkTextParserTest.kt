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

@DisplayName("LinkTextParser.parse")
class LinkTextParserTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = [""])
    fun `returns nothing for null or empty text`(text: String?) {
        LinkTextParser.parse(text).shouldBeEmpty()
    }

    @Test
    fun `returns a single plain segment when there are no links`() {
        val segments = LinkTextParser.parse("Texto normal sin enlaces.")

        segments.size shouldBe 1
        segments[0].isLink.shouldBeFalse()
        segments[0].text shouldBe "Texto normal sin enlaces."
    }

    @Test
    fun `detects an https url`() {
        val segments = LinkTextParser.parse("Mira https://github.com/jagobainda/LostieLauncher para más info")

        segments.size shouldBe 3
        segments[0].text shouldBe "Mira "
        segments[1].isLink.shouldBeTrue()
        segments[1].text shouldBe "https://github.com/jagobainda/LostieLauncher"
        segments[1].url shouldBe "https://github.com/jagobainda/LostieLauncher"
        segments[2].text shouldBe " para más info"
    }

    @Test
    fun `detects a bare domain with a path and normalizes it to https`() {
        val segments = LinkTextParser.parse(
            "Puedes consultar el changelog completo en github.com/jagobainda/LostieLauncher/releases.",
        )

        segments.size shouldBe 3
        segments[1].isLink.shouldBeTrue()
        segments[1].text shouldBe "github.com/jagobainda/LostieLauncher/releases"
        segments[1].url shouldBe "https://github.com/jagobainda/LostieLauncher/releases"
        segments[2].text shouldBe "."
    }

    @Test
    fun `detects a www-prefixed domain`() {
        val segments = LinkTextParser.parse("Visita www.example.org ahora")

        segments[1].isLink.shouldBeTrue()
        segments[1].url shouldBe "https://www.example.org/"
    }

    @Test
    fun `trims trailing punctuation`() {
        val segments = LinkTextParser.parse("(ver https://example.com/docs), ¿vale?")

        segments.single { it.isLink }.text shouldBe "https://example.com/docs"
        segments.last().text shouldBe "), ¿vale?"
    }

    @Test
    fun `detects several links`() {
        val segments = LinkTextParser.parse("Repo: github.com/a/b y web: https://example.com")

        segments.count { it.isLink } shouldBe 2
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "Guarda el archivo.txt en la carpeta",
            "Escríbenos a soporte@example.com si falla",
            "La versión v0.9.0 ya está disponible",
        ],
    )
    fun `ignores what is not a link`(text: String) {
        LinkTextParser.parse(text).forEach { it.isLink.shouldBeFalse() }
    }

    @Test
    fun `ignores http urls`() {
        val segments = LinkTextParser.parse("Antiguo mirror en http://example.com/old sin soporte")

        segments.forEach { it.isLink.shouldBeFalse() }
    }
}
