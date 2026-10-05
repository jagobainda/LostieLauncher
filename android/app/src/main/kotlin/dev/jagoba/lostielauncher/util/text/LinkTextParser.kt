package dev.jagoba.lostielauncher.util.text

import dev.jagoba.lostielauncher.util.net.HttpsUrls

object LinkTextParser {
    data class Segment(val text: String, val url: String?) {
        val isLink: Boolean get() = url != null
    }

    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '"', '\'')

    private const val HTTPS_PREFIX = "https://"

    private const val WORD = """\p{L}\p{Mn}\p{Nd}\p{Pc}"""

    private const val NON_SPACE = """[^\s\x{85}\p{Z}]"""

    private val LINK_REGEX = Regex(
        """https://$NON_SPACE+|(?<![$WORD@./-])(?:www\.[$WORD-]+(?:\.[$WORD-]+)+|""" +
            """(?:[$WORD-]+\.)+(?:com|net|org|es|eu|io|gg|dev|app|me|co|tv|info)(?![$WORD]))(?:/$NON_SPACE*)?""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String?): List<Segment> {
        if (text.isNullOrEmpty()) return emptyList()

        val segments = mutableListOf<Segment>()
        var lastIndex = 0

        for (match in LINK_REGEX.findAll(text)) {
            val candidate = match.value.trimEnd(*TRAILING_PUNCTUATION)
            if (candidate.isEmpty()) continue

            val normalized = if (candidate.startsWith(HTTPS_PREFIX, ignoreCase = true)) {
                candidate
            } else {
                HTTPS_PREFIX + candidate
            }
            val uri = HttpsUrls.parseOrNull(normalized) ?: continue

            val start = match.range.first
            if (start > lastIndex) segments += Segment(text.substring(lastIndex, start), null)

            segments += Segment(candidate, HttpsUrls.canonicalUrl(uri))
            lastIndex = start + candidate.length
        }

        if (lastIndex < text.length) segments += Segment(text.substring(lastIndex), null)

        return segments
    }
}
