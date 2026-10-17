package dev.jagoba.lostielauncher.util.version

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotStartWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("VersionUtils")
class VersionUtilsTest {

    @ParameterizedTest
    @CsvSource("1.2.0, 1.1.0, true", "1.1.0, 1.2.0, false", "1.0.0, 1.0.0, false")
    fun `compares numerically when both parse`(remote: String, local: String, expected: Boolean) {
        val result = VersionUtils.isNewerVersion(remote, local)

        result shouldBe expected
    }

    @ParameterizedTest
    @CsvSource("v2.0.0, v1.0.0, true", "V2.0.0, 1.0.0, true")
    fun `strips a leading v prefix before comparing`(remote: String, local: String, expected: Boolean) {
        val result = VersionUtils.isNewerVersion(remote, local)

        result shouldBe expected
    }

    @Test
    fun `strips a pre-release suffix before comparing`() {
        val result = VersionUtils.isNewerVersion("1.2.0-beta", "1.1.0")

        result.shouldBeTrue()
    }

    @Test
    fun `when the base versions are equal but the pre-release suffix differs, is not newer`() {
        val result = VersionUtils.isNewerVersion("1.0.0-beta", "1.0.0")

        result.shouldBeFalse()
    }

    @ParameterizedTest
    @CsvSource("alpha, beta", "beta, alpha", "alpha, alpha")
    fun `when either input is unparsable, fails closed`(remote: String, local: String) {
        val result = VersionUtils.isNewerVersion(remote, local)

        result.shouldBeFalse()
    }

    @Test
    fun `when the base versions parse but the suffix differs only in case, is not newer`() {
        val result = VersionUtils.isNewerVersion("v1.0-BETA", "v1.0-beta")

        result.shouldBeFalse()
    }

    @ParameterizedTest
    @CsvSource("1.0.0, garbage", "garbage, 1.0.0")
    fun `when only one input is unparsable, fails closed`(remote: String, local: String) {
        val result = VersionUtils.isNewerVersion(remote, local)

        result.shouldBeFalse()
    }

    @ParameterizedTest
    @CsvSource("'', ''", "1.0.0, ''", "'', 1.0.0")
    fun `when either input is empty, fails closed`(remote: String, local: String) {
        val result = VersionUtils.isNewerVersion(remote, local)

        result.shouldBeFalse()
    }

    @Test
    fun `when an input is null, fails closed`() {
        val result = VersionUtils.isNewerVersion(null, "1.0.0")

        result.shouldBeFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["v4.0.3", "V4.0.3", "4.0.3"])
    fun `emits exactly one v prefix regardless of the source prefix`(version: String) {
        val result = VersionUtils.formatDisplayVersion(version)

        result shouldBe "v4.0.3"
    }

    @Test
    fun `when the source already carries the prefix, does not duplicate it`() {
        val result = VersionUtils.formatDisplayVersion("v2.18.0")

        result shouldBe "v2.18.0"
        result.shouldNotStartWith("vv")
    }

    @Test
    fun `collapses an already duplicated prefix`() {
        val result = VersionUtils.formatDisplayVersion("vv2.18.0")

        result shouldBe "v2.18.0"
    }

    @Test
    fun `preserves a pre-release suffix`() {
        val result = VersionUtils.formatDisplayVersion("v1.2.0-beta")

        result shouldBe "v1.2.0-beta"
    }

    @Test
    fun `trims surrounding whitespace`() {
        val result = VersionUtils.formatDisplayVersion("  v2.11.0  ")

        result shouldBe "v2.11.0"
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "   ", "v"])
    fun `when there is no version to show, returns unknown rather than a lone v`(version: String?) {
        val result = VersionUtils.formatDisplayVersion(version)

        result shouldBe "unknown"
    }
}
