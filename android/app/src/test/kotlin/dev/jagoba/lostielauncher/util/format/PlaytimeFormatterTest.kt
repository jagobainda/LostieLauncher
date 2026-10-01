package dev.jagoba.lostielauncher.util.format

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("PlaytimeFormatter.format")
class PlaytimeFormatterTest {
    @ParameterizedTest
    @CsvSource("0, ''", "-5, ''", "-2147483648, ''")
    fun `when the total is zero or negative, returns empty`(minutes: Int, expected: String) {
        val text = PlaytimeFormatter.format(minutes)

        text shouldBe expected
    }

    @ParameterizedTest
    @CsvSource("1, 1 min", "45, 45 min", "59, 59 min")
    fun `when below one hour, returns minutes only`(minutes: Int, expected: String) {
        val text = PlaytimeFormatter.format(minutes)

        text shouldBe expected
    }

    @ParameterizedTest
    @CsvSource("60, 1 h", "120, 2 h")
    fun `when the total is whole hours, omits the minutes`(minutes: Int, expected: String) {
        val text = PlaytimeFormatter.format(minutes)

        text shouldBe expected
    }

    @ParameterizedTest
    @CsvSource("75, 1 h 15 min", "125, 2 h 5 min", "150, 2 h 30 min")
    fun `when the total is hours and minutes, returns both`(minutes: Int, expected: String) {
        val text = PlaytimeFormatter.format(minutes)

        text shouldBe expected
    }
}
