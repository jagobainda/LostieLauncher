package dev.jagoba.lostielauncher.model

import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("GameInfo")
class GameInfoTest {
    private fun createGame(name: String = "Demo", sizeGb: Double = 1.0) = GameInfo(
        id = null,
        name = name,
        version = "v1.0.0",
        sizeGb = sizeGb,
        description = "",
        pageUrl = "",
        logoUrl = null,
        relativePath = "",
        sha256 = "",
    )

    @Test
    fun `reports a size of one gigabyte or more in gigabytes`() {
        createGame(sizeGb = 1.5).formattedSize shouldBe "1.5 GB"
    }

    @Test
    fun `reports a size below one gigabyte in megabytes`() {
        createGame(sizeGb = 0.25).formattedSize shouldBe "256 MB"
    }

    @Test
    fun `formats the size with a dot whatever the device locale is`() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
        try {
            createGame(sizeGb = 1.5).formattedSize shouldBe "1.5 GB"
            createGame(sizeGb = 1.0).formattedSize shouldBe "1 GB"
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `drops the trailing zero of a whole number of gigabytes`() {
        createGame(sizeGb = 1.0).formattedSize shouldBe "1 GB"
    }

    @Test
    fun `converts gigabytes to megabytes with a 1024 divisor, not 1000`() {
        createGame(sizeGb = 0.5889).formattedSize shouldBe "603 MB"
    }

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

    @Test
    fun `returns an empty slug for a name that is all separators`() {
        createGame(name = "+++").gameId shouldBe ""
    }
}
