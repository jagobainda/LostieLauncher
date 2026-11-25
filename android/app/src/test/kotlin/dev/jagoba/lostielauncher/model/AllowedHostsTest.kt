package dev.jagoba.lostielauncher.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("AllowedHosts")
class AllowedHostsTest {
    private val sut = AllowedHosts(
        exact = setOf("github.com", "ericlostie-launcher.jagoba.dev"),
        suffixes = setOf("githubusercontent.com"),
    )

    @ParameterizedTest
    @ValueSource(
        strings = [
            "github.com",
            "GITHUB.COM",
            "ericlostie-launcher.jagoba.dev",
            "release-assets.githubusercontent.com",
            "objects.githubusercontent.com",
            "a.b.githubusercontent.com",
            "RELEASE-ASSETS.GITHUBUSERCONTENT.COM",
        ],
    )
    fun `allows matching exact hosts and subdomains case-insensitively`(host: String) {
        sut.allows(host) shouldBe true
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = [
            "   ",
            "evil.example",
            "api.github.com",
            "githubusercontent.com",
            ".githubusercontent.com",
            "evilgithubusercontent.com",
            "release-assets.githubusercontent.com.evil.example",
            "release-assets.githubusercontent.com.",
            "evil.github.com",
        ],
    )
    fun `rejects hosts outside the allowlist`(host: String?) {
        sut.allows(host) shouldBe false
    }

    @Test
    fun `normalizes a leading dot in configured suffixes`() {
        val withLeadingDot = AllowedHosts(suffixes = setOf(".githubusercontent.com"))

        withLeadingDot.allows("release-assets.githubusercontent.com") shouldBe true
        withLeadingDot.allows("githubusercontent.com") shouldBe false
        withLeadingDot.allows(".githubusercontent.com") shouldBe false
    }
}
