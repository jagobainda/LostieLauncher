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

@DisplayName("HttpsUrls")
class HttpsUrlsTest {
    @Test
    fun `accepts an absolute https url`() {
        val uri = HttpsUrls.parseOrNull("https://github.com/jagobainda/LostieLauncher")

        uri.shouldNotBeNull()
        uri.scheme shouldBe "https"
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "http://example.com",
            "file:///C:/Windows/System32/calc.exe",
            "ftp://example.com/file",
            "javascript:alert(1)",
            "cmd://whatever",
        ],
    )
    fun `rejects every other scheme`(url: String) {
        HttpsUrls.parseOrNull(url).shouldBeNull()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(
        strings = [
            "",
            "   ",
            "github.com/jagobainda",
            "not a uri at all",
        ],
    )
    fun `rejects null, empty or malformed input`(url: String?) {
        HttpsUrls.parseOrNull(url).shouldBeNull()
    }

    @Test
    fun `accepts an uppercase scheme, which dotNET lowercases while parsing`() {
        val uri = HttpsUrls.parseOrNull("HTTPS://example.com/docs")

        uri.shouldNotBeNull()
        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com/docs"
    }

    @Test
    fun `writes an empty path as a slash`() {
        val uri = HttpsUrls.parseOrNull("https://www.example.org").shouldNotBeNull()

        HttpsUrls.canonicalUrl(uri) shouldBe "https://www.example.org/"
    }

    @Test
    fun `drops the default port and lowercases the host`() {
        val uri = HttpsUrls.parseOrNull("https://Example.COM:443/a?q=1#top").shouldNotBeNull()

        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com/a?q=1#top"
    }

    @Test
    fun `keeps a non-default port`() {
        val uri = HttpsUrls.parseOrNull("https://example.com:8443/a").shouldNotBeNull()

        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com:8443/a"
    }

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
        delimiter = ';',
    )
    fun `accepts what the desktop accepts, escaping what the strict parser refuses`(url: String, expected: String) {
        val uri = HttpsUrls.parseOrNull(url).shouldNotBeNull()

        HttpsUrls.canonicalUrl(uri) shouldBe expected
    }

    @Test
    fun `escaping does not widen the guard itself`() {
        HttpsUrls.parseOrNull("javascript:alert(1 2)").shouldBeNull()
        HttpsUrls.parseOrNull("file:///C:/Program Files/calc.exe").shouldBeNull()
        HttpsUrls.parseOrNull("not a uri at all").shouldBeNull()
    }

    @Test
    fun `an IPv6 authority still parses, because the strict attempt comes first`() {
        val uri = HttpsUrls.parseOrNull("https://[::1]/a").shouldNotBeNull()

        HttpsUrls.canonicalUrl(uri) shouldBe "https://[::1]/a"
    }

    @Test
    fun `an already-escaped url is left alone`() {
        val uri = HttpsUrls.parseOrNull("https://example.com/a%20b").shouldNotBeNull()

        HttpsUrls.canonicalUrl(uri) shouldBe "https://example.com/a%20b"
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        value = [
            "https://example.com/path[0]     ; https://example.com/path%5B0%5D",
            "https://example.com/camión      ; https://example.com/camión",
            "https://example.com/../a        ; https://example.com/../a",
        ],
        delimiter = ';',
    )
    fun `pins the three places the canonical form still differs from the desktop`(url: String, expected: String) {
        val uri = HttpsUrls.parseOrNull(url).shouldNotBeNull()

        HttpsUrls.canonicalUrl(uri) shouldBe expected
    }
}
