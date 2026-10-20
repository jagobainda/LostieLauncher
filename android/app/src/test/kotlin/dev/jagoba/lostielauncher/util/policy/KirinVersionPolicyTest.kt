package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.model.KirinStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("KirinVersionPolicy")
class KirinVersionPolicyTest {
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
        KirinVersionPolicy.isSupported(version, MINIMUM) shouldBe supported
    }

    @Test
    fun `an unparseable minimum supports nothing`() {
        KirinVersionPolicy.isSupported("9.9.9", "latest") shouldBe false
    }

    @Test
    fun `a missing package is missing`() {
        KirinVersionPolicy.status(null, MINIMUM, LATEST) shouldBe KirinStatus.Missing(MINIMUM)
    }

    @Test
    fun `an installation below the minimum is outdated and keeps both versions`() {
        KirinVersionPolicy.status("0.3.5", MINIMUM, LATEST) shouldBe KirinStatus.Outdated("0.3.5", MINIMUM)
    }

    @Test
    fun `an installation at the minimum or later is advised to update to a newer release`() {
        KirinVersionPolicy.status("0.4.0-beta8", MINIMUM, LATEST) shouldBe
            KirinStatus.UpdateRecommended("0.4.0-beta8", LATEST)
    }

    @ParameterizedTest(name = "{0}, latest {1}")
    @CsvSource("0.4.2, 0.4.2", "0.5.0, 0.4.2", "0.4.0-beta8, 0.4.0", "0.4.0, ")
    fun `an installation at the latest release, or with no release known, is ready`(
        installed: String,
        latest: String?,
    ) {
        KirinVersionPolicy.status(installed, MINIMUM, latest) shouldBe KirinStatus.Ready
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
        KirinVersionPolicy.isOffered(release, installed, MINIMUM) shouldBe offered
    }

    private companion object {
        const val MINIMUM = "0.4.0"
        const val LATEST = "0.4.2"
    }
}
