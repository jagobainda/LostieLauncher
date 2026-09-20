package dev.jagoba.lostielauncher.util.text

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

/** Ported declaration for declaration from the desktop's `Utils/SearchMatcherTests.cs`. */
@DisplayName("SearchMatcher")
class SearchMatcherTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "   "])
    fun `with an empty or whitespace term, returns nothing`(term: String?) {
        // Act
        val matches = SearchMatcher.findMatches("¿Cómo descargo un juego?", term)

        // Assert
        matches.shouldBeEmpty()
    }

    @Test
    fun `with empty text, returns nothing`() {
        // Act
        val matches = SearchMatcher.findMatches(null, "juego")

        // Assert
        matches.shouldBeEmpty()
    }

    @Test
    fun `is case insensitive`() {
        // Act
        val matches = SearchMatcher.findMatches("Descargar el JUEGO", "juego")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(13, 5))
    }

    @Test
    fun `is accent insensitive`() {
        // Arrange — and the match is as long as the text it covers, not as long as the term:
        // that is why a match carries its own length.
        // Act
        val matches = SearchMatcher.findMatches("instalación en español", "instalacion")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(0, "instalación".length))
    }

    @Test
    fun `returns every occurrence`() {
        // Act
        val matches = SearchMatcher.findMatches("juego tras juego", "juego")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(0, 5), SearchMatcher.Match(11, 5))
    }

    @Test
    fun `contains reports a match`() {
        // Act & Assert
        SearchMatcher.contains("¿Dónde se instalan los juegos?", "donde").shouldBeTrue()
    }

    @Test
    fun `contains reports the absence of a match`() {
        // Act & Assert
        SearchMatcher.contains("¿Dónde se instalan los juegos?", "biblioteca").shouldBeFalse()
    }

    // ---- Android-only: the folding this port had to write by hand ----

    @Test
    fun `matches text written with decomposed accents`() {
        // Arrange — the same word in NFD. The desktop gets this from the collation for free;
        // here the folding is hand-written, so the case is pinned rather than assumed.
        val decomposed = "instalacio\u0301n en espan\u0303ol"

        // Act
        val matches = SearchMatcher.findMatches(decomposed, "instalación")

        // Assert — twelve characters of text carry the eleven-character word.
        matches shouldContainExactly listOf(SearchMatcher.Match(0, 12))
    }

    @Test
    fun `a term that folds away entirely matches nothing`() {
        // Arrange — a lone combining mark is not blank, so it reaches the search, and the
        // desktop's loop leaves on the zero-length match it produces. Same answer here.
        // Act
        val matches = SearchMatcher.findMatches("instalación", "\u0301")

        // Assert
        matches.shouldBeEmpty()
    }

    @Test
    fun `matches do not overlap`() {
        // Arrange — scanning resumes at the end of each match, so the middle "aa" is not a third.
        // Act
        val matches = SearchMatcher.findMatches("aaaa", "aa")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(0, 2), SearchMatcher.Match(2, 2))
    }

    // ---- The ela geminada: a middle dot after an l is contracted away ----

    @ParameterizedTest
    @CsvSource(
        "installació, 3, 12",
        "installacio, 3, 12",
        "instal·lacio, 3, 12",
        "instal, 3, 6",
        "instal·, 3, 6",
        "ll, 8, 3",
    )
    fun `finds a geminated l written without the middle dot`(term: String, start: Int, length: Int) {
        // Arrange — the expected ranges are what .NET's CompareInfo.IndexOf returns for the same
        // inputs, measured rather than reasoned about. Two of them span one character more than
        // the term, because the dot is inside the match and the caller has to highlight it.
        val text = "La instal·lació ha acabat"

        // Act
        val matches = SearchMatcher.findMatches(text, term)

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(start, length))
    }

    @Test
    fun `keeps filtering through every keystroke of a geminated word`() {
        // Arrange — the FAQ filter runs on each keystroke, so every prefix of the word has to
        // keep matching. The prefix ending in the middle dot is the one a both-sides rule
        // drops, and the list would blank for exactly that keystroke.
        val text = "La instal·lació ha acabat"
        val typed = "instal·lacio"

        // Act & Assert
        for (length in 1..typed.length) {
            SearchMatcher.contains(text, typed.take(length)).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @CsvSource("l·a, la", "L·a, la", "l·A, LA", "instal·acio, instalacio", "ll·a, lla")
    fun `contracts a middle dot after an l whatever follows it`(text: String, term: String) {
        // Arrange — the condition is the left neighbour alone. Swept over every ordered pair of
        // a 39-character alphabet: the dot is dropped in 39 of 1521, which is exactly the rows
        // whose left neighbour is an l, and in none of the other 1482.
        // Act & Assert
        SearchMatcher.contains(text, term).shouldBeTrue()
    }

    @Test
    fun `contracts a middle dot at the very end of the text`() {
        // Arrange — "l·" equals "l"; there is no right-hand side to require.
        // Act & Assert
        SearchMatcher.contains("instal·", "instal").shouldBeTrue()
    }

    @ParameterizedTest
    @CsvSource("l\u0301·a, l\u0301a", "l\u00AD·a, l\u00ADa", "l\u200D·a, l\u200Da")
    fun `does not contract when something sits between the l and the dot`(text: String, term: String) {
        // Arrange — the contraction reads the literal preceding character, not the collation's
        // view of it, so an accent or an ignorable in between blocks it on the desktop too.
        // Act & Assert
        SearchMatcher.contains(text, term).shouldBeFalse()
    }

    @Test
    fun `finds a geminated l in a string the app actually ships`() {
        // Arrange — a Catalan FAQ question, verbatim. It is half the point of the contraction:
        // the FAQ filter is what this backs, and a user types "installen".
        val question = "On s'instal·len els jocs i com canvio la carpeta?"

        // Act & Assert
        SearchMatcher.contains(question, "installen").shouldBeTrue()
        SearchMatcher.contains(question, "instal·len").shouldBeTrue()
    }

    @ParameterizedTest
    @CsvSource("l·L, lL", "L·l, Ll", "L·L, LL")
    fun `the contraction ignores the case of the l on either side of the dot`(geminated: String, flattened: String) {
        // Act & Assert
        SearchMatcher.contains(geminated, flattened).shouldBeTrue()
    }

    @ParameterizedTest
    @CsvSource("a·b, ab", "n·n, nn", "l··l, ll", "l·, ll", "·l, ll")
    fun `a middle dot anywhere else keeps its weight`(text: String, term: String) {
        // Arrange — this is a contraction, not an ignorable character. .NET compares every
        // one of these as different. The first two are what an unconditional drop would have
        // matched; the rest pin the edges — the second dot of "l··l" follows a dot, not an l.
        // Act & Assert
        SearchMatcher.contains(text, term).shouldBeFalse()
    }

    // ---- Other characters the collation gives no weight to ----

    @ParameterizedTest
    @ValueSource(
        strings = [
            // Soft hyphen, the one that turns up in pasted copy.
            "insta\u00ADlacion",
            // Zero-width non-joiner, zero-width joiner, zero-width space.
            "insta\u200Clacion", "insta\u200Dlacion", "insta\u200Blacion",
            // Byte-order mark used as a zero-width no-break space.
            "insta\uFEFFlacion",
            // A control character that is not whitespace.
            "insta\u0001lacion",
        ],
    )
    fun `ignores what the collation gives no weight to`(text: String) {
        // Act & Assert
        SearchMatcher.contains(text, "instalacion").shouldBeTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["insta\tlacion", "insta\nlacion", "insta\rlacion"])
    fun `does not ignore a control character that collates as whitespace`(text: String) {
        // Arrange — tab and the line breaks carry weight, so they break a word the way a space
        // does. Dropping the whole Control category would have swallowed them.
        // Act & Assert
        SearchMatcher.contains(text, "instalacion").shouldBeFalse()
    }

    // ---- Where a match stops when the next character was dropped ----

    @Test
    fun `a match reaches over a trailing combining mark`() {
        // Arrange — the accent belongs to the final "e", so .NET matches five characters
        // against a four-character term. Highlighting four would leave the accent unpainted.
        // Act
        val matches = SearchMatcher.findMatches("cafe\u0301", "cafe")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(0, 5))
    }

    @ParameterizedTest(name = "U+{0} spans {1}")
    @CsvSource(
        // Zero-width non-joiner and zero-width joiner.
        "200C, 6",
        "200D, 6",
        // Soft hyphen, byte-order mark, zero-width space, word joiner.
        "00AD, 5",
        "FEFF, 5",
        "200B, 5",
        "2060, 5",
    )
    fun `a match reaches over a trailing joiner but not over other ignorables`(hex: String, length: Int) {
        // Arrange — all six are ignorable, and the boundary still treats them differently: a
        // joiner goes with the grapheme before it, a soft hyphen or a byte-order mark is its
        // own. Measured against CompareInfo.IndexOf, one character at a time. The code points
        // are written as hex rather than as literals so the test file stays readable.
        val text = "insta" + hex.toInt(16).toChar() + "lacion"

        // Act
        val matches = SearchMatcher.findMatches(text, "insta")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(0, length))
    }

    @Test
    fun `a match never reaches backwards over a dropped character`() {
        // Arrange — the leading side has no equivalent rule: a match starts at its own first
        // character, whatever precedes it.
        // Act
        val matches = SearchMatcher.findMatches("insta\u00ADlacion", "lacion")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(6, 6))
    }

    @Test
    fun `finds every geminated word in a string, scanning forward`() {
        // Arrange — two contractions in one string, which is the shape of a real FAQ answer.
        // Act
        val matches = SearchMatcher.findMatches("instal·lar i desinstal·lar", "installar")

        // Assert
        matches shouldContainExactly listOf(SearchMatcher.Match(0, 10), SearchMatcher.Match(16, 10))
    }

    @Test
    fun `a repeated geminated l contracts every time`() {
        // Act & Assert — each middle dot flanked by l disappears on its own.
        SearchMatcher.findMatches("l·l·l", "lll") shouldContainExactly
            listOf(SearchMatcher.Match(0, 5))
        SearchMatcher.findMatches("l·l·l", "ll") shouldContainExactly
            listOf(SearchMatcher.Match(0, 3))
    }
}
