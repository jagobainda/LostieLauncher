package dev.jagoba.lostielauncher.model

import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("GameInfo")
class GameInfoTest {
    private fun createGame(name: String = "Demo") = GameInfo(
        id = null,
        name = name,
        version = "v1.0.0",
        sizeGb = 1.0,
        description = "",
        pageUrl = "",
        logoUrl = null,
        relativePath = "",
        sha256 = "",
    )

    @Test
    fun `slugs a name with capitals and spaces to lowercase kebab case`() {
        createGame(name = "Eric Lostie 2").gameId shouldBe "eric-lostie-2"
    }

    @Test
    fun `strips accents and symbols without leaving a dangling dash`() {
        createGame(name = "!Hola! Mundo+").gameId shouldBe "hola-mundo"
    }

    @ParameterizedTest
    @CsvSource(
        "'+++', ''",
        "'-Trailing-', 'trailing'",
        "'Inner  Spaces', 'inner-spaces'",
    )
    fun `trims dangling dashes and collapses separator runs`(name: String, expected: String) {
        createGame(name = name).gameId shouldBe expected
    }

    @Test
    fun `slugs the accented catalogue name the way the desktop does`() {
        createGame(name = "Pokémon Añil").gameId shouldBe "pok-mon-a-il"
    }

    @Test
    fun `lowercases the name the same way in every locale`() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        try {
            createGame(name = "IBERIA").gameId shouldBe "iberia"
        } finally {
            Locale.setDefault(original)
        }
    }
}
