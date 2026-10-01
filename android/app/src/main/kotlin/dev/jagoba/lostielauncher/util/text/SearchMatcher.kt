package dev.jagoba.lostielauncher.util.text

import java.text.Normalizer

object SearchMatcher {
    data class Match(val start: Int, val length: Int)

    private const val MIDDLE_DOT = '·'
    private const val NEXT_LINE = 0x85
    private const val ZERO_WIDTH_NON_JOINER = 0x200C
    private const val ZERO_WIDTH_JOINER = 0x200D

    fun contains(text: String?, term: String?): Boolean = findMatches(text, term).isNotEmpty()

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

    private fun isIgnorable(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.NON_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt(), Character.FORMAT.toInt() -> true
        Character.CONTROL.toInt() -> codePoint !in 0x09..0x0D && codePoint != NEXT_LINE
        else -> false
    }

    private fun isContractedMiddleDot(text: String, index: Int, codePoint: Int): Boolean =
        codePoint == MIDDLE_DOT.code && index > 0 && isContractingL(text[index - 1])

    private fun isContractingL(character: Char): Boolean = character == 'l' || character == 'L'
}
