package dev.jagoba.lostielauncher.util.net

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

object HttpsUrls {
    private const val HTTPS_SCHEME = "https"
    private const val HTTPS_DEFAULT_PORT = 443

    private val FORBIDDEN_BY_THE_STRICT_PARSER = mapOf(
        ' ' to "%20", '"' to "%22", '<' to "%3C", '>' to "%3E", '\\' to "%5C",
        '^' to "%5E", '`' to "%60", '{' to "%7B", '|' to "%7C", '}' to "%7D",
        '[' to "%5B", ']' to "%5D",
    )

    private val MALFORMED_ESCAPE = Regex("%(?![0-9A-Fa-f]{2})")

    private const val LAST_CONTROL = '\u001F'

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
