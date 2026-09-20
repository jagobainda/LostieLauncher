package dev.jagoba.lostielauncher.util.text

import java.text.Normalizer

/**
 * Case- and accent-insensitive substring search, ported from the desktop's
 * `Utils/SearchMatcher.cs`.
 *
 * It backs the FAQ filter and the match highlighting on a FAQ card, which is
 * why it returns **positions and lengths** rather than a boolean: the caller
 * slices the original text with them.
 *
 * ## Why the length is returned rather than assumed
 *
 * The desktop compares with `CompareOptions.IgnoreCase | IgnoreNonSpace` under
 * the invariant culture, so a match can span a different number of characters
 * than the term does: searching `instalacion` inside `instalación` matches
 * eleven characters against an eleven-character term, but the eleventh in the
 * text is a precomposed `ó`. Anything that assumed `term.length` would
 * highlight the wrong slice as soon as a text used decomposed accents.
 *
 * ## How the same answer is reached here
 *
 * .NET's collation-aware `IndexOf` has no JVM counterpart that a unit test can
 * reach — `android.icu.text.StringSearch` exists, but only on a device, and
 * tests here never run on one. So each side is folded instead: every code point
 * is decomposed (NFD), whatever the collation gives no weight to is dropped,
 * and what remains is lowercased with [Character.toLowerCase], which is
 * locale-independent the way the invariant culture is. Every folded character
 * remembers which original character produced it, so a match found in folded
 * space maps back to a range in the original.
 *
 * ## What the collation ignores, and what it does not
 *
 * The rules below were measured against `CompareInfo.Compare` on .NET 10 across
 * every BMP code point, not inferred. What is dropped here:
 *
 * - **Non-spacing marks**, which is what `IgnoreNonSpace` is named after.
 * - **Format and enclosing-mark characters**, every one of which the collation
 *   ignores — a soft hyphen or a zero-width joiner pasted into a CDN string
 *   must not stop a search from matching.
 * - **Control characters, except the six that collate as whitespace**
 *   (`U+0009`–`U+000D` and `U+0085`). Those carry weight and are left alone.
 * - **The middle dot of an `l·l`**, and only there. See below.
 *
 * Two places where this is deliberately not the root collation, both confirmed
 * absent from the eight shipped catalogues and the captured CDN payloads:
 *
 * - **Expansions are not reproduced.** `æ` collates as `ae` under ICU and does
 *   not fold to it here.
 * - **A handful of ignorables outside these categories are not reproduced**:
 *   the collation also ignores six modifier letters, twenty-six other letters
 *   and two punctuation marks, all in scripts the launcher does not ship, plus
 *   thirty-three spacing combining marks. Those last are why the rule is not
 *   simply "drop every mark": most spacing combining marks — Devanagari vowel
 *   signs among them — *do* carry weight, so dropping the category wholesale
 *   would be worse than this gap. In the other direction, the collation gives
 *   weight to 439 of the 1066 non-spacing marks, so dropping them all is
 *   slightly over-broad; every mark that occurs in a Latin-script language is
 *   ignorable, which is the corpus this has.
 *
 * ## The `ela geminada`
 *
 * Catalan and Valencian write a geminated l as `l·l`, and the invariant
 * collation contracts it away so that `instal·lació` and `installació` compare
 * equal. This is a contraction and **not** an ignorable character: `a·b` does
 * not match `ab`.
 *
 * The condition is narrower than the name suggests and was swept rather than
 * assumed. Over every ordered pair of a 39-character alphabet, the dot is
 * dropped in exactly 39 of the 1521 — **the ones whose left neighbour is `l` or
 * `L`, whatever follows**, including nothing at all: `l·a` equals `la` and `l·`
 * equals `l`. A leading `·l` keeps its dot, and so does the second dot of
 * `l··l`, whose left neighbour is a dot. The neighbour is read literally, not
 * through the folding: `l` + a combining acute + `·` does not contract, and
 * neither does `ł`.
 *
 * Requiring an `l` on *both* sides looks right and is wrong in the one place it
 * shows: the FAQ filter runs on every keystroke, so a Catalan user typing the
 * word passes through `instal·`, and a both-sides rule blanks the list for that
 * keystroke while the desktop keeps it. The two languages carry the sequence in
 * 38 interface strings and 14 FAQ entries.
 */
object SearchMatcher {
    /** A match, as a range over the **original** text. */
    data class Match(val start: Int, val length: Int)

    private const val MIDDLE_DOT = '·'
    private const val NEXT_LINE = 0x85
    private const val ZERO_WIDTH_NON_JOINER = 0x200C
    private const val ZERO_WIDTH_JOINER = 0x200D

    fun contains(text: String?, term: String?): Boolean = findMatches(text, term).isNotEmpty()

    /**
     * Every non-overlapping occurrence of [term] in [text], scanning forward
     * from the end of each match.
     *
     * Empty text or a blank term yields nothing, as does a term that folds away
     * entirely — the desktop's loop leaves on a zero-length match for the same
     * reason.
     */
    fun findMatches(text: String?, term: String?): List<Match> {
        if (text.isNullOrEmpty() || term.isNullOrBlank()) return emptyList()

        val foldedText = fold(text)
        val foldedTerm = fold(term).value
        if (foldedTerm.isEmpty()) return emptyList()

        val matches = mutableListOf<Match>()
        var cursor = 0

        while (cursor < foldedText.value.length) {
            val index = foldedText.value.indexOf(foldedTerm, cursor)
            if (index < 0) break

            val end = index + foldedTerm.length
            val start = foldedText.starts[index]
            val stop = extendOverGraphemeExtenders(text, foldedText.ends[end - 1])
            matches += Match(start, stop - start)
            cursor = end
        }

        return matches
    }

    /**
     * The folded form of a string, and for each folded character the range of
     * the original character it came from.
     *
     * Both ends are needed, and the second is not "the start of the next one".
     * A match that ends just before something the collation dropped must not
     * swallow it wholesale: `instal` inside `instal·lació` is six characters on
     * the desktop, not seven. Reading the end off the **last** folded character
     * of the match keeps the dropped middle dot outside it, and still spans a
     * dropped mark that falls *inside* the match.
     */
    private class Folded(val value: String, val starts: IntArray, val ends: IntArray)

    private fun fold(text: String): Folded {
        val folded = StringBuilder(text.length)
        val starts = ArrayList<Int>(text.length)
        val ends = ArrayList<Int>(text.length)

        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val width = Character.charCount(codePoint)
            if (!isContractedMiddleDot(text, index, codePoint)) {
                appendFolded(String(Character.toChars(codePoint)), index, index + width, folded, starts, ends)
            }
            index += width
        }

        return Folded(folded.toString(), starts.toIntArray(), ends.toIntArray())
    }

    private fun appendFolded(
        source: String,
        start: Int,
        end: Int,
        folded: StringBuilder,
        starts: MutableList<Int>,
        ends: MutableList<Int>,
    ) {
        val decomposed = Normalizer.normalize(source, Normalizer.Form.NFD)

        var index = 0
        while (index < decomposed.length) {
            val codePoint = decomposed.codePointAt(index)
            index += Character.charCount(codePoint)
            if (isIgnorable(codePoint)) continue

            for (character in Character.toChars(Character.toLowerCase(codePoint))) {
                folded.append(character)
                starts += start
                ends += end
            }
        }
    }

    /**
     * Walks [from] forward over characters that belong to the **same grapheme**
     * as the one before them, which is how far a match reaches.
     *
     * The desktop's trailing boundary is not "stop at the last weighted
     * character" and not "swallow everything ignorable" — it is exactly this,
     * measured character by character against `CompareInfo.IndexOf`. Searching
     * `cafe` in `cafe` + a combining acute matches **five** characters, because
     * the accent is part of the final `e`; searching `insta` in `insta` + a soft
     * hyphen + `lacion` matches **five**, because a soft hyphen is its own
     * grapheme however ignorable it is. Joiners go with the preceding grapheme,
     * so `ZWNJ` and `ZWJ` are in and every other ignorable format character,
     * control and word joiner is out.
     *
     * Getting it wrong is not abstract: the lengths this returns are what a FAQ
     * card highlights, so an off-by-one here paints one character too many or
     * leaves an accent unpainted.
     */
    private fun extendOverGraphemeExtenders(text: String, from: Int): Int {
        var index = from
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (!isGraphemeExtending(codePoint)) break
            index += Character.charCount(codePoint)
        }
        return index
    }

    private fun isGraphemeExtending(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.NON_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt() -> true
        else -> codePoint == ZERO_WIDTH_NON_JOINER || codePoint == ZERO_WIDTH_JOINER
    }

    /** Whether the collation gives [codePoint] no weight at all. */
    private fun isIgnorable(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.NON_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt(), Character.FORMAT.toInt() -> true

        // Tab, the line breaks and NEL collate as whitespace; every other control is ignored.
        Character.CONTROL.toInt() -> codePoint !in 0x09..0x0D && codePoint != NEXT_LINE

        else -> false
    }

    /**
     * Whether the character at [index] is a middle dot the collation contracts
     * away — which is every one written straight after an `l`.
     *
     * There is deliberately **no condition on what follows**: the sweep in this
     * file's header found the dot dropped after `l` and nowhere else, whatever
     * came next and even at the end of the string. The left neighbour is read
     * from the original text, because the contraction does not see through the
     * folding either: an ignorable character between the `l` and the dot blocks
     * it on the desktop too.
     */
    private fun isContractedMiddleDot(text: String, index: Int, codePoint: Int): Boolean =
        codePoint == MIDDLE_DOT.code && index > 0 && isContractingL(text[index - 1])

    /** The plain letter only — `ł` and the rest of the l-lookalikes do not contract. */
    private fun isContractingL(character: Char): Boolean = character == 'l' || character == 'L'
}
