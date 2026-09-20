package dev.jagoba.lostielauncher.util.text

import dev.jagoba.lostielauncher.util.net.HttpsUrls

/**
 * Splits a block of CDN-authored text into plain and link segments, ported from
 * the desktop's `Utils/LinkTextParser.cs`.
 *
 * News bodies and FAQ answers are plain strings on the wire with no markup, and
 * the people writing them type `github.com/...` as often as a full URL. This is
 * what turns either into something tappable without trusting the payload: a
 * candidate only becomes a link if it survives [HttpsUrls.parseOrNull], so an
 * `http://` mirror in a news item renders as text and nothing else.
 *
 * Two consequences worth keeping in mind when rendering: trailing sentence
 * punctuation stays in the following **plain** segment rather than being
 * swallowed into the link, and the displayed text of a link is the candidate as
 * it was written, which is not always its target.
 */
object LinkTextParser {
    /** One run of text, a link when [url] is set. */
    data class Segment(val text: String, val url: String?) {
        val isLink: Boolean get() = url != null
    }

    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '"', '\'')

    private const val HTTPS_PREFIX = "https://"

    /**
     * The desktop's pattern, character for character, with one addition: the
     * leading `(?U)`.
     *
     * .NET's `\w` and `\b` are Unicode-aware by default and Java's are ASCII-only
     * until `UNICODE_CHARACTER_CLASS` is switched on. Without it the look-behind
     * would not see the accented letter in `Escríbenos a soporte@…` as a word
     * character, and the guard that keeps this from firing inside an email
     * address would come apart on exactly the Spanish text the CDN serves.
     */
    private val LINK_REGEX = Regex(
        """(?U)https://\S+|(?<![\w@./-])(?:www\.[\w-]+(?:\.[\w-]+)+|""" +
            """(?:[\w-]+\.)+(?:com|net|org|es|eu|io|gg|dev|app|me|co|tv|info)\b)(?:/\S*)?""",
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
