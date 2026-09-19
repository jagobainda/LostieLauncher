package dev.jagoba.lostielauncher.model

import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Ported from the desktop's `Models/GameInfoTests.cs`.
 *
 * Only the two derived members this port keeps on the model are here.
 * `DownloadSpeedText` and `PlaytimeText` are derived from transient download
 * and playtime state, which this model deliberately does not carry — they
 * arrive with port plan steps 08 and 06 respectively — and the desktop's
 * `PropertyChanged` cases have no counterpart at all, because the model is
 * immutable.
 */
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

    // ---- formattedSize ----

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
        // Arrange — a locale whose decimal separator is a comma. The desktop
        // pins the invariant culture here so the CDN's numbers never render
        // differently on a Spanish machine; the port has to do the same.
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
        try {
            // Act / Assert
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
        // 0.5889 GB is the real size of the first catalogue entry.
        createGame(sizeGb = 0.5889).formattedSize shouldBe "603 MB"
    }

    // ---- gameId ----

    @Test
    fun `slugs a name with capitals and spaces to lowercase kebab case`() {
        createGame(name = "Eric Lostie 2").gameId shouldBe "eric-lostie-2"
    }

    @Test
    fun `strips accents and symbols without leaving a dangling dash`() {
        // Desktop BUG-048: a leading punctuation mark and a trailing '+' each
        // become a separator run, and neither may survive as a dash.
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
        // Arrange — Turkish lowercases 'I' to a dotless 'i', which is not a
        // match for [a-z] and would silently change the slug of any game with
        // an uppercase I in its name.
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
        // The desktop's "null name" case has no counterpart: the name is
        // non-nullable here, and an explicit null in the payload never reaches
        // the model. What survives is the degenerate name.
        createGame(name = "+++").gameId shouldBe ""
    }
}
