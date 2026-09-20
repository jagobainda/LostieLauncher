package dev.jagoba.lostielauncher.util.net

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * The HTTPS-only guard every outbound link passes through, ported from
 * `UrlLauncher.TryGetHttpsUri` in the desktop's `Utils/UrlLauncher.cs`.
 *
 * It is a security property, not a Windows detail: the desktop refuses to hand
 * the shell anything but an absolute HTTPS URL, because a `file://` or a
 * `javascript:` string reaching `Process.Start` is code execution from a CDN
 * payload. The same string arriving at an Android `Intent` deserves the same
 * treatment, so the check survives the port and the shelling out does not —
 * that half lands with the screens that need it.
 */
object HttpsUrls {
    private const val HTTPS_SCHEME = "https"
    private const val HTTPS_DEFAULT_PORT = 443

    /**
     * The ASCII characters .NET's `Uri` percent-encodes and `java.net.URI`
     * refuses outright, mapped to what .NET writes for each.
     *
     * Measured on both runtimes rather than read off a grammar. `[` and `]` are
     * in the list although .NET leaves them alone in a path, because the strict
     * parse runs first: an IPv6 authority such as `https://[::1]/a` never
     * reaches this table, and a bracket in a path is the only thing that does.
     */
    private val FORBIDDEN_BY_THE_STRICT_PARSER = mapOf(
        ' ' to "%20", '"' to "%22", '<' to "%3C", '>' to "%3E", '\\' to "%5C",
        '^' to "%5E", '`' to "%60", '{' to "%7B", '|' to "%7C", '}' to "%7D",
        '[' to "%5B", ']' to "%5D",
    )

    /** A `%` that does not begin a valid escape. .NET writes it as `%25`; the strict parser rejects it. */
    private val MALFORMED_ESCAPE = Regex("%(?![0-9A-Fa-f]{2})")

    private const val LAST_CONTROL = '\u001F'

    /**
     * [url] as an absolute HTTPS URI, or `null` when it is blank, relative, or
     * carries any other scheme.
     *
     * The scheme is compared case-insensitively. The desktop compares it with
     * `==`, but .NET lowercases a scheme while parsing and `java.net.URI` keeps
     * it as written, so `HTTPS://…` has to be accepted here for the two sides
     * to agree.
     *
     * **A URL is not rejected for being badly punctuated.** `java.net.URI` is
     * stricter than `Uri.TryCreate`: it throws on a space, on `| ^ { } [ ]` and
     * on a malformed `%`, all of which .NET accepts and percent-encodes. Left
     * alone, that difference would render a link as dead text on Android and as
     * a working link on the desktop, from the same news item. So a string the
     * strict parser refuses is escaped the way .NET escapes it and parsed again.
     *
     * The strict parse runs **first**, so every URL that already worked is
     * unchanged, and the fallback only ever widens what is accepted. What it
     * does not widen is the guard itself: the result still has to be absolute
     * and still has to be HTTPS, which is the whole security property.
     */
    fun parseOrNull(url: String?): URI? {
        if (url.isNullOrBlank()) return null

        val parsed = parseOrNullStrict(url) ?: parseOrNullStrict(escapeAsDotNetWould(url)) ?: return null

        if (!parsed.isAbsolute) return null
        return if (HTTPS_SCHEME.equals(parsed.scheme, ignoreCase = true)) parsed else null
    }

    private fun parseOrNullStrict(url: String): URI? = try {
        URI(url)
    } catch (_: URISyntaxException) {
        null
    }

    private fun escapeAsDotNetWould(url: String): String = buildString(url.length) {
        for (character in MALFORMED_ESCAPE.replace(url, "%25")) {
            val escape = FORBIDDEN_BY_THE_STRICT_PARSER[character]
            when {
                escape != null -> append(escape)
                character <= LAST_CONTROL -> append("%%%02X".format(Locale.ROOT, character.code))
                else -> append(character)
            }
        }
    }

    /**
     * The canonical form of [uri], matching what .NET's `Uri.AbsoluteUri`
     * produces for an HTTPS URL: a lowercase scheme and host, the default port
     * dropped, and an empty path written as `/`.
     *
     * That last rule is the one with a consequence — `www.example.org` becomes
     * `https://www.example.org/` — and it is why this exists at all rather than
     * the URI being handed on as written.
     *
     * Two parts of .NET's normalization are still not reproduced, and both are
     * cosmetic rather than behavioural — the target resolves to the same
     * resource either way, and every consumer of this is an Android `Intent`:
     * **dot segments are not resolved** (`/../a` stays), and **non-ASCII
     * characters are not percent-encoded** (`/camión` stays, where .NET writes
     * `/cami%C3%B3n`). What [parseOrNull] escapes on its fallback path *is*
     * preserved here, because the raw components are what this reads.
     */
    fun canonicalUrl(uri: URI): String = buildString {
        append(uri.scheme.lowercase(Locale.ROOT)).append("://")
        uri.userInfo?.let { append(it).append('@') }
        append(uri.host.orEmpty().lowercase(Locale.ROOT))
        if (uri.port != -1 && uri.port != HTTPS_DEFAULT_PORT) append(':').append(uri.port)
        append(uri.rawPath.orEmpty().ifEmpty { "/" })
        uri.rawQuery?.let { append('?').append(it) }
        uri.rawFragment?.let { append('#').append(it) }
    }
}
