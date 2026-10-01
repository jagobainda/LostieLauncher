package dev.jagoba.lostielauncher.model

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("SpecialVersionConfig.parse")
class SpecialVersionConfigTest {
    private val validGuid = "11111111-2222-3333-4444-555555555555"

    private fun validContent(overrideKey: String? = null, overrideValue: String = ""): String {
        val values = linkedMapOf(
            "sha256" to "abc",
            "tipo" to "beta",
            "juego-principal" to validGuid,
            "vers" to "1.2.3",
            "archivo" to "game.zip",
        )
        if (overrideKey != null) values[overrideKey] = overrideValue
        return values.entries.joinToString("\n") { "${it.key}=${it.value}" }
    }

    @Test
    fun `reads every required field`() {
        val config = SpecialVersionConfig.parse(validContent())

        config.shouldNotBeNull()
        config.sha256 shouldBe "abc"
        config.type shouldBe "beta"
        config.mainGameId shouldBe UUID.fromString(validGuid)
        config.version shouldBe "1.2.3"
        config.fileName shouldBe "game.zip"
    }

    @Test
    fun `accepts carriage returns and stray whitespace around each field`() {
        val content = "  sha256 = abc  \n tipo=beta\njuego-principal=$validGuid\nvers=1.0.0\narchivo=g.zip\n"

        val config = SpecialVersionConfig.parse(content)

        config.shouldNotBeNull()
        config.sha256 shouldBe "abc"
        config.type shouldBe "beta"
    }

    @Test
    fun `matches keys regardless of case`() {
        val content = "SHA256=abc\nTipo=beta\nJUEGO-PRINCIPAL=$validGuid\nVers=2.0.0\nArchivo=g.zip"

        val config = SpecialVersionConfig.parse(content)

        config.shouldNotBeNull()
        config.version shouldBe "2.0.0"
    }

    @ParameterizedTest
    @ValueSource(strings = ["sha256", "tipo", "juego-principal", "vers", "archivo"])
    fun `refuses the file when a required key is missing`(missingKey: String) {
        val content = validContent()
            .lines()
            .filterNot { it.startsWith("$missingKey=") }
            .joinToString("\n")

        SpecialVersionConfig.parse(content).shouldBeNull()
    }

    @Test
    fun `refuses the file when the main game is not a GUID`() {
        SpecialVersionConfig.parse(validContent("juego-principal", "not-a-guid")).shouldBeNull()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "11111111-2222-3333-4444-555555555555",
            "11111111222233334444555555555555",
            "{11111111-2222-3333-4444-555555555555}",
            "(11111111-2222-3333-4444-555555555555)",
            "11111111-2222-3333-4444-555555555555 ",
            "11111111-2222-3333-4444-5555555555AB",
        ],
    )
    fun `accepts every main-game form the desktop accepts`(written: String) {
        SpecialVersionConfig.parse(validContent("juego-principal", written)).shouldNotBeNull()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "1-2-3-4-5",
            "{11111111222233334444555555555555}",
            "{11111111-2222-3333-4444-555555555555",
            "11111111-2222-3333-4444-555555555555}",
            "{ 11111111-2222-3333-4444-555555555555 }",
            "1111111122223333444455555555-5-5-5-5",
            "11111111-2222-3333-4444-55555555555G",
            "0x11111111222233334444555555555555",
            "not-a-guid",
        ],
    )
    fun `refuses every main-game form the desktop refuses`(written: String) {
        SpecialVersionConfig.parse(validContent("juego-principal", written)).shouldBeNull()
    }

    @Test
    fun `reads a wrapped main game as the same id as a bare one`() {
        val wrapped = SpecialVersionConfig.parse(validContent("juego-principal", "{$validGuid}"))
        val compact = SpecialVersionConfig.parse(validContent("juego-principal", validGuid.replace("-", "")))

        wrapped.shouldNotBeNull().mainGameId shouldBe UUID.fromString(validGuid)
        compact.shouldNotBeNull().mainGameId shouldBe UUID.fromString(validGuid)
    }

    @Test
    fun `skips comments, blank lines and lines with no separator`() {
        val content = "# this is a comment\n\nsha256=abc\ntipo=beta\n" +
            "juego-principal=$validGuid\nvers=1.0.0\narchivo=g.zip\nbroken-line"

        SpecialVersionConfig.parse(content).shouldNotBeNull()
    }

    @Test
    fun `refuses an empty file`() {
        SpecialVersionConfig.parse("").shouldBeNull()
    }

    @Test
    fun `lets the last occurrence of a repeated key win`() {
        val content = validContent() + "\ntipo=halloween"

        val config = SpecialVersionConfig.parse(content)

        config.shouldNotBeNull()
        config.type shouldBe "halloween"
    }

    @Test
    fun `keeps a value that itself contains an equals sign`() {
        val content = validContent("archivo", "g=1.zip")

        val config = SpecialVersionConfig.parse(content)

        config.shouldNotBeNull()
        config.fileName shouldBe "g=1.zip"
    }
}
