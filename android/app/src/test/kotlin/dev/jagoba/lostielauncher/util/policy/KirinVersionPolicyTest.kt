package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.model.KirinStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("KirinVersionPolicy")
class KirinVersionPolicyTest {
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

    private companion object {
        const val MINIMUM = "0.4.0"
        const val LATEST = "0.4.2"
    }
}
