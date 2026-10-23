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

@DisplayName("SearchMatcher")
class SearchMatcherTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "   "])
    fun `with an empty or whitespace term, returns nothing`(term: String?) {
        val matches = SearchMatcher.findMatches("¿Cómo descargo un juego?", term)

        matches.shouldBeEmpty()
    }

    @Test
    fun `with empty text, returns nothing`() {
        val matches = SearchMatcher.findMatches(null, "juego")

        matches.shouldBeEmpty()
    }

    @Test
    fun `is case insensitive`() {
        val matches = SearchMatcher.findMatches("Descargar el JUEGO", "juego")

        matches shouldContainExactly listOf(SearchMatcher.Match(13, 5))
    }

    @Test
    fun `is accent insensitive`() {
        val matches = SearchMatcher.findMatches("instalación en español", "instalacion")

        matches shouldContainExactly listOf(SearchMatcher.Match(0, "instalación".length))
    }

    @Test
    fun `returns every occurrence`() {
        val matches = SearchMatcher.findMatches("juego tras juego", "juego")

        matches shouldContainExactly listOf(SearchMatcher.Match(0, 5), SearchMatcher.Match(11, 5))
    }

    @Test
    fun `contains reports a match`() {
        SearchMatcher.contains("¿Dónde se instalan los juegos?", "donde").shouldBeTrue()
    }

    @Test
    fun `contains reports the absence of a match`() {
        SearchMatcher.contains("¿Dónde se instalan los juegos?", "biblioteca").shouldBeFalse()
    }

    @Test
    fun `matches text written with decomposed accents`() {
        val decomposed = "instalacio\u0301n en espan\u0303ol"

        val matches = SearchMatcher.findMatches(decomposed, "instalación")

        matches shouldContainExactly listOf(SearchMatcher.Match(0, 12))
    }

    @Test
    fun `a term that folds away entirely matches nothing`() {
        val matches = SearchMatcher.findMatches("instalación", "\u0301")

        matches.shouldBeEmpty()
    }

    @Test
    fun `matches do not overlap`() {
        val matches = SearchMatcher.findMatches("aaaa", "aa")

        matches shouldContainExactly listOf(SearchMatcher.Match(0, 2), SearchMatcher.Match(2, 2))
    }

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
        val text = "La instal·lació ha acabat"

        val matches = SearchMatcher.findMatches(text, term)

        matches shouldContainExactly listOf(SearchMatcher.Match(start, length))
    }

    @Test
    fun `keeps filtering through every keystroke of a geminated word`() {
        val text = "La instal·lació ha acabat"
        val typed = "instal·lacio"

        for (length in 1..typed.length) {
            SearchMatcher.contains(text, typed.take(length)).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @CsvSource("l·a, la", "L·a, la", "l·A, LA", "instal·acio, instalacio", "ll·a, lla")
    fun `contracts a middle dot after an l whatever follows it`(text: String, term: String) {
        SearchMatcher.contains(text, term).shouldBeTrue()
    }

    @Test
    fun `contracts a middle dot at the very end of the text`() {
        SearchMatcher.contains("instal·", "instal").shouldBeTrue()
    }

    @ParameterizedTest
    @CsvSource("l\u0301·a, l\u0301a", "l\u00AD·a, l\u00ADa", "l\u200D·a, l\u200Da")
    fun `does not contract when something sits between the l and the dot`(text: String, term: String) {
        SearchMatcher.contains(text, term).shouldBeFalse()
    }

    @Test
    fun `finds a geminated l in a string the app actually ships`() {
        val question = "On s'instal·len els jocs i com canvio la carpeta?"

        SearchMatcher.contains(question, "installen").shouldBeTrue()
        SearchMatcher.contains(question, "instal·len").shouldBeTrue()
    }

    @ParameterizedTest
    @CsvSource("l·L, lL", "L·l, Ll", "L·L, LL")
    fun `the contraction ignores the case of the l on either side of the dot`(geminated: String, flattened: String) {
        SearchMatcher.contains(geminated, flattened).shouldBeTrue()
    }

    @ParameterizedTest
    @CsvSource("a·b, ab", "n·n, nn", "l··l, ll", "l·, ll", "·l, ll")
    fun `a middle dot anywhere else keeps its weight`(text: String, term: String) {
        SearchMatcher.contains(text, term).shouldBeFalse()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "insta\u00ADlacion",
            "insta\u200Clacion", "insta\u200Dlacion", "insta\u200Blacion",
            "insta\uFEFFlacion",
            "insta\u0001lacion",
        ],
    )
    fun `ignores what the collation gives no weight to`(text: String) {
        SearchMatcher.contains(text, "instalacion").shouldBeTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["insta\tlacion", "insta\nlacion", "insta\rlacion"])
    fun `does not ignore a control character that collates as whitespace`(text: String) {
        SearchMatcher.contains(text, "instalacion").shouldBeFalse()
    }

    @Test
    fun `a match reaches over a trailing combining mark`() {
        val matches = SearchMatcher.findMatches("cafe\u0301", "cafe")

        matches shouldContainExactly listOf(SearchMatcher.Match(0, 5))
    }

    @ParameterizedTest(name = "U+{0} spans {1}")
    @CsvSource(
        "200C, 6",
        "200D, 6",
        "00AD, 5",
        "FEFF, 5",
        "200B, 5",
        "2060, 5",
    )
    fun `a match reaches over a trailing joiner but not over other ignorables`(hex: String, length: Int) {
        val text = "insta" + hex.toInt(16).toChar() + "lacion"

        val matches = SearchMatcher.findMatches(text, "insta")

        matches shouldContainExactly listOf(SearchMatcher.Match(0, length))
    }

    @Test
    fun `a match never reaches backwards over a dropped character`() {
        val matches = SearchMatcher.findMatches("insta\u00ADlacion", "lacion")

        matches shouldContainExactly listOf(SearchMatcher.Match(6, 6))
    }

    @Test
    fun `finds every geminated word in a string, scanning forward`() {
        val matches = SearchMatcher.findMatches("instal·lar i desinstal·lar", "installar")

        matches shouldContainExactly listOf(SearchMatcher.Match(0, 10), SearchMatcher.Match(16, 10))
    }

    @Test
    fun `a repeated geminated l contracts every time`() {
        SearchMatcher.findMatches("l·l·l", "lll") shouldContainExactly
            listOf(SearchMatcher.Match(0, 5))
        SearchMatcher.findMatches("l·l·l", "ll") shouldContainExactly
            listOf(SearchMatcher.Match(0, 3))
    }
}
