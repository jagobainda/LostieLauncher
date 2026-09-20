package dev.jagoba.lostielauncher.util.net

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Ported declaration for declaration from the desktop's `Utils/UrlLauncherTests.cs`, which
 * only ever covered `TryGetHttpsUri` — the shelling out has no test there either.
 */
@DisplayName("HttpsUrls")
class HttpsUrlsTest {
    @Test
    fun `accepts an absolute https url`() {
        // Act
        val uri = HttpsUrls.parseOrNull("https://github.com/jagobainda/LostieLauncher")

        // Assert
        uri.shouldNotBeNull()
        uri.scheme shouldBe "https"
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            // Plain HTTP is rejected: only HTTPS is allowed out.
            "http://example.com",
            // The local-executable vector the desktop guard exists for.
            "file:///C:/Windows/System32/calc.exe",
            "ftp://example.com/file",
            "javascript:alert(1)",
            "cmd://whatever",
        ],
    )
    fun `rejects every other scheme`(url: String) {
        // Act & Assert — nothing but HTTPS may ever reach the platform.
        HttpsUrls.parseOrNull(url).shouldBeNull()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(
        strings = [
            "",
            "   ",
            // Relative, no scheme.
            "github.com/jagobainda",
            "not a uri at all",
        ],
    )
    fun `rejects null, empty or malformed input`(url: String?) {
        // Act & Assert
        HttpsUrls.parseOrNull(url).shouldBeNull()
    }

    // ---- Android-only: the canonical form the desktop gets from Uri.AbsoluteUri ----

    @Test
    fun `accepts an uppercase scheme, which dotNET lowercases while parsing`() {
        // Arrange — java.net.URI keeps the scheme as written, so the comparison has to ignore
        // case for the two sides to agree on this input.
        // Act
        val uri = HttpsUrls.parseOrNull("HTTPS://example.com/docs")

        // Assert
        uri.shouldNotBeNull()
        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com/docs"
    }

    @Test
    fun `writes an empty path as a slash`() {
        // Arrange — this is the rule with a visible consequence: it is why a bare domain in a
        // news item renders with a trailing slash in its target.
        val uri = HttpsUrls.parseOrNull("https://www.example.org").shouldNotBeNull()

        // Act & Assert
        HttpsUrls.canonicalUrl(uri) shouldBe "https://www.example.org/"
    }

    @Test
    fun `drops the default port and lowercases the host`() {
        // Arrange
        val uri = HttpsUrls.parseOrNull("https://Example.COM:443/a?q=1#top").shouldNotBeNull()

        // Act & Assert
        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com/a?q=1#top"
    }

    @Test
    fun `keeps a non-default port`() {
        // Arrange
        val uri = HttpsUrls.parseOrNull("https://example.com:8443/a").shouldNotBeNull()

        // Act & Assert
        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com:8443/a"
    }

    // ---- Android-only: URLs java.net.URI refuses and .NET accepts ----

    @ParameterizedTest
    @CsvSource(
        value = [
            "https://example.com/a b            ; https://example.com/a%20b",
            "https://example.com/a|b            ; https://example.com/a%7Cb",
            "https://example.com/a^b            ; https://example.com/a%5Eb",
            "https://example.com/a{b}c          ; https://example.com/a%7Bb%7Dc",
            "https://example.com/a[b]c          ; https://example.com/a%5Bb%5Dc",
            "https://example.com/a%zzb          ; https://example.com/a%25zzb",
            "https://example.com/a%2            ; https://example.com/a%252",
            "https://example.com/a\"b           ; https://example.com/a%22b",
            "https://example.com/a<b>c          ; https://example.com/a%3Cb%3Ec",
            "https://example.com/a`b            ; https://example.com/a%60b",
        ],
        // Not the default comma, and not '|' either: both occur inside the data.
        delimiter = ';',
    )
    fun `accepts what the desktop accepts, escaping what the strict parser refuses`(url: String, expected: String) {
        // Arrange — every one of these is accepted by Uri.TryCreate and rejected by
        // java.net.URI. Rejecting them would render a link as dead text on Android and as a
        // working link on the desktop, from the same news item. The expected forms are what
        // .NET's AbsoluteUri produces, measured on both runtimes.
        // Act
        val uri = HttpsUrls.parseOrNull(url).shouldNotBeNull()

        // Assert
        HttpsUrls.canonicalUrl(uri) shouldBe expected
    }

    @Test
    fun `escaping does not widen the guard itself`() {
        // Arrange — the fallback must not turn a rejected scheme into an accepted URL. These
        // only reach it because the strict parser chokes on the space.
        // Act & Assert
        HttpsUrls.parseOrNull("javascript:alert(1 2)").shouldBeNull()
        HttpsUrls.parseOrNull("file:///C:/Program Files/calc.exe").shouldBeNull()
        HttpsUrls.parseOrNull("not a uri at all").shouldBeNull()
    }

    @Test
    fun `an IPv6 authority still parses, because the strict attempt comes first`() {
        // Arrange — the escape table turns square brackets into %5B/%5D, which would wreck
        // this. It never sees it: java.net.URI parses an IPv6 literal on its own.
        val uri = HttpsUrls.parseOrNull("https://[::1]/a").shouldNotBeNull()

        // Act & Assert
        HttpsUrls.canonicalUrl(uri) shouldBe "https://[::1]/a"
    }

    @Test
    fun `an already-escaped url is left alone`() {
        // Arrange — the malformed-escape rule must not double-encode a valid one.
        val uri = HttpsUrls.parseOrNull("https://example.com/a%20b").shouldNotBeNull()

        // Act & Assert
        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com/a%20b"
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        value = [
            // .NET leaves brackets in a path alone; here they arrive through the escape table.
            "https://example.com/path[0]     ; https://example.com/path%5B0%5D",
            // .NET percent-encodes non-ASCII; java.net.URI keeps it, and so does this.
            "https://example.com/camión      ; https://example.com/camión",
            // .NET resolves dot segments; java.net.URI does not normalise.
            "https://example.com/../a        ; https://example.com/../a",
        ],
        delimiter = ';',
    )
    fun `pins the three places the canonical form still differs from the desktop`(url: String, expected: String) {
        // Arrange — a differential over the whole shipped corpus plus these cases found exactly
        // three disagreements with .NET, all of them here and all of them in the target rather
        // than in whether the text is a link at all. Each resolves to the same resource, and
        // every consumer is an Android Intent. They are pinned so that closing one — or letting
        // a fourth appear — has to be a decision somebody made.
        val uri = HttpsUrls.parseOrNull(url).shouldNotBeNull()

        // Act & Assert
        HttpsUrls.canonicalUrl(uri) shouldBe expected
    }
}
