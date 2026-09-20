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

/**
 * Android-only. The desktop has no counterpart because it does not need one:
 * these are `System.Version`'s own rules, and .NET ships them. Here they are
 * hand-written, so the rules a naive port would get wrong are pinned directly —
 * the component count, the `-1` for an absent component, and .NET's integer
 * grammar. `spec/09-utilities.md` names the first two as edge cases that matter.
 */
@DisplayName("BaseVersion")
class BaseVersionTest {
    @Test
    fun `an absent component ranks below an explicit zero`() {
        // Arrange — the rule behind "1.2 is lower than 1.2.0": .NET stores an unwritten
        // component as -1, so the two are not the same release and 1.2.0 wins.
        val short = BaseVersion.parse("1.2").shouldNotBeNull()
        val long = BaseVersion.parse("1.2.0").shouldNotBeNull()

        // Act & Assert
        short shouldBeLessThan long
        long shouldBeGreaterThan short
    }

    @ParameterizedTest
    @ValueSource(strings = ["1.2", "1.2.3", "1.2.3.4"])
    fun `accepts two to four components`(value: String) {
        // Act & Assert
        BaseVersion.parse(value).shouldNotBeNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "1.2.3.4.5", "", "1.", ".1", "1..2", "1.2.x"])
    fun `rejects anything that is not two to four numeric components`(value: String) {
        // Act & Assert
        BaseVersion.parse(value).shouldBeNull()
    }

    @ParameterizedTest
    @CsvSource("' 1 . 2 ', 1, 2", "+1.+2, 1, 2", "-0.5, 0, 5")
    fun `reproduces dotNET's integer grammar for a component`(value: String, major: Int, minor: Int) {
        // Arrange — .NET parses each component with NumberStyles.Integer, which allows
        // surrounding whitespace and a leading sign, and only then rejects a negative result.
        // Reproduced because this type's job is equivalence, not improvement.
        // Act
        val parsed = BaseVersion.parse(value).shouldNotBeNull()

        // Assert
        parsed.major shouldBe major
        parsed.minor shouldBe minor
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1.0", "1.-1", "1.99999999999"])
    fun `rejects a negative or overflowing component`(value: String) {
        // Act & Assert
        BaseVersion.parse(value).shouldBeNull()
    }
}
