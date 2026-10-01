package dev.jagoba.lostielauncher.util.version

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("BaseVersion")
class BaseVersionTest {
    @Test
    fun `an absent component ranks below an explicit zero`() {
        val short = BaseVersion.parse("1.2").shouldNotBeNull()
        val long = BaseVersion.parse("1.2.0").shouldNotBeNull()

        short shouldBeLessThan long
        long shouldBeGreaterThan short
    }

    @ParameterizedTest
    @ValueSource(strings = ["1.2", "1.2.3", "1.2.3.4"])
    fun `accepts two to four components`(value: String) {
        BaseVersion.parse(value).shouldNotBeNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "1.2.3.4.5", "", "1.", ".1", "1..2", "1.2.x"])
    fun `rejects anything that is not two to four numeric components`(value: String) {
        BaseVersion.parse(value).shouldBeNull()
    }

    @ParameterizedTest
    @CsvSource("' 1 . 2 ', 1, 2", "+1.+2, 1, 2", "-0.5, 0, 5")
    fun `reproduces dotNET's integer grammar for a component`(value: String, major: Int, minor: Int) {
        val parsed = BaseVersion.parse(value).shouldNotBeNull()

        parsed.major shouldBe major
        parsed.minor shouldBe minor
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1.0", "1.-1", "1.99999999999"])
    fun `rejects a negative or overflowing component`(value: String) {
        BaseVersion.parse(value).shouldBeNull()
    }
}
