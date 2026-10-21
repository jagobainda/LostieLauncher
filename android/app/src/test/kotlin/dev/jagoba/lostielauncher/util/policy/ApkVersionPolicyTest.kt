package dev.jagoba.lostielauncher.util.policy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("ApkVersionPolicy")
class ApkVersionPolicyTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "0.4.0, true",
        "0.4.0-beta8, true",
        "v0.4.1, true",
        "1.0, true",
        "0.3.5, false",
        "0.3.9-beta1, false",
        "not-a-version, false",
        "'', false",
    )
    fun `supports the minimum version and later, ignoring a pre-release suffix`(version: String, supported: Boolean) {
        ApkVersionPolicy.isSupported(version, MINIMUM) shouldBe supported
    }

    @Test
    fun `an unparseable minimum supports nothing`() {
        ApkVersionPolicy.isSupported("9.9.9", "latest") shouldBe false
    }

    @ParameterizedTest(name = "release {0}, installed {1} -> {2}")
    @CsvSource(
        "0.4.2, , true",
        "0.4.2, 0.3.5, true",
        "0.4.2, 0.4.0, true",
        "0.4.2, 0.4.2, false",
        "0.4.0, 0.4.2, false",
        "0.3.9, , false",
    )
    fun `offers a release at the minimum or later that is newer than the installed one`(
        release: String,
        installed: String?,
        offered: Boolean,
    ) {
        ApkVersionPolicy.isOffered(release, installed, MINIMUM) shouldBe offered
    }

    @ParameterizedTest(name = "release {0}, installed {1} -> {2}")
    @CsvSource(
        "0.0.1, , true",
        "1.2.0, 1.1.9, true",
        "1.2.0, 1.2.0-beta1, false",
        "1.2.0, 1.2.0, false",
        "1.1.0, 1.2.0, false",
        "not-a-version, 1.2.0, false",
    )
    fun `without a minimum, offers any release newer than the installed one`(
        release: String,
        installed: String?,
        offered: Boolean,
    ) {
        ApkVersionPolicy.isOffered(release, installed, null) shouldBe offered
    }

    private companion object {
        const val MINIMUM = "0.4.0"
    }
}
