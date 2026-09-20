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

/** Ported declaration for declaration from the desktop's `Utils/VersionUtilsTests.cs`. */
@DisplayName("VersionUtils")
class VersionUtilsTest {
    // ---- isNewerVersion ----

    @ParameterizedTest
    @CsvSource("1.2.0, 1.1.0, true", "1.1.0, 1.2.0, false", "1.0.0, 1.0.0, false")
    fun `compares numerically when both parse`(remote: String, local: String, expected: Boolean) {
        // Arrange & Act
        val result = VersionUtils.isNewerVersion(remote, local)

        // Assert
        result shouldBe expected
    }

    @ParameterizedTest
    @CsvSource("v2.0.0, v1.0.0, true", "V2.0.0, 1.0.0, true")
    fun `strips a leading v prefix before comparing`(remote: String, local: String, expected: Boolean) {
        // Arrange & Act
        val result = VersionUtils.isNewerVersion(remote, local)

        // Assert
        result shouldBe expected
    }

    @Test
    fun `strips a pre-release suffix before comparing`() {
        // Arrange — base version 1.2.0 must beat 1.1.0 even with a -beta suffix.
        // Act
        val result = VersionUtils.isNewerVersion("1.2.0-beta", "1.1.0")

        // Assert
        result.shouldBeTrue()
    }

    @Test
    fun `when the base versions are equal but the pre-release suffix differs, is not newer`() {
        // Arrange — both parse to 1.0.0; the suffix is ignored, so this is not an update.
        // Act
        val result = VersionUtils.isNewerVersion("1.0.0-beta", "1.0.0")

        // Assert
        result.shouldBeFalse()
    }

    @ParameterizedTest
    @CsvSource("alpha, beta", "beta, alpha", "alpha, alpha")
    fun `when either input is unparsable, fails closed`(remote: String, local: String) {
        // Arrange — fail closed: a version that is not numerically comparable never marks an
        // update, which is what keeps a false positive from turning into an automatic downgrade.
        // Act
        val result = VersionUtils.isNewerVersion(remote, local)

        // Assert
        result.shouldBeFalse()
    }

    @Test
    fun `when the base versions parse but the suffix differs only in case, is not newer`() {
        // Arrange — "v1.0-BETA" against "v1.0-beta" never reaches the fail-closed branch: the
        // suffix is cut off first, both sides parse to 1.0, and the numeric comparison answers.
        // Act
        val result = VersionUtils.isNewerVersion("v1.0-BETA", "v1.0-beta")

        // Assert
        result.shouldBeFalse()
    }

    @ParameterizedTest
    @CsvSource("1.0.0, garbage", "garbage, 1.0.0")
    fun `when only one input is unparsable, fails closed`(remote: String, local: String) {
        // Arrange & Act
        val result = VersionUtils.isNewerVersion(remote, local)

        // Assert
        result.shouldBeFalse()
    }

    @ParameterizedTest
    @CsvSource("'', ''", "1.0.0, ''", "'', 1.0.0")
    fun `when either input is empty, fails closed`(remote: String, local: String) {
        // Arrange — the desktop's default version is an empty string; an update must never be
        // marked on empty data.
        // Act
        val result = VersionUtils.isNewerVersion(remote, local)

        // Assert
        result.shouldBeFalse()
    }

    @Test
    fun `when an input is null, fails closed`() {
        // Arrange — the catalogue's deserializer will bind an explicit "version": null even where
        // the field is declared non-nullable, which is why the parameter accepts one.
        // Act
        val result = VersionUtils.isNewerVersion(null, "1.0.0")

        // Assert
        result.shouldBeFalse()
    }

    // ---- formatDisplayVersion ----

    @ParameterizedTest
    @ValueSource(strings = ["v4.0.3", "V4.0.3", "4.0.3"])
    fun `emits exactly one v prefix regardless of the source prefix`(version: String) {
        // Act
        val result = VersionUtils.formatDisplayVersion(version)

        // Assert
        result shouldBe "v4.0.3"
    }

    @Test
    fun `when the source already carries the prefix, does not duplicate it`() {
        // Act
        val result = VersionUtils.formatDisplayVersion("v2.18.0")

        // Assert
        result shouldBe "v2.18.0"
        result.shouldNotStartWith("vv")
    }

    @Test
    fun `collapses an already duplicated prefix`() {
        // Act
        val result = VersionUtils.formatDisplayVersion("vv2.18.0")

        // Assert
        result shouldBe "v2.18.0"
    }

    @Test
    fun `preserves a pre-release suffix`() {
        // Act
        val result = VersionUtils.formatDisplayVersion("v1.2.0-beta")

        // Assert
        result shouldBe "v1.2.0-beta"
    }

    @Test
    fun `trims surrounding whitespace`() {
        // Act
        val result = VersionUtils.formatDisplayVersion("  v2.11.0  ")

        // Assert
        result shouldBe "v2.11.0"
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "   ", "v"])
    fun `when there is no version to show, returns unknown rather than a lone v`(version: String?) {
        // Act
        val result = VersionUtils.formatDisplayVersion(version)

        // Assert
        result shouldBe "unknown"
    }
}
