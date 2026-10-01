package dev.jagoba.lostielauncher.content

import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("String.withArgs")
class WithArgsTest {

    @Test
    @DisplayName("substitutes a single placeholder")
    fun `replaces {0}`() {
        "Uninstall {0}?".withArgs("Pokémon Lostie") shouldBe "Uninstall Pokémon Lostie?"
    }

    @Test
    @DisplayName("substitutes two placeholders in their own positions")
    fun `replaces {0} and {1} independently`() {
        val template = "{0} left this behind:\n\n{1}\n\nOpen it?"

        val message = template.withArgs("Lostie", """C:\Games\Lostie\data""")

        message shouldBe "Lostie left this behind:\n\nC:\\Games\\Lostie\\data\n\nOpen it?"
    }

    @Test
    @DisplayName("keeps the catalogue's own multi-line strings intact")
    fun `works against a real catalogue string`() {
        val strings = stringsFor(AppLanguage.ENG)

        val message = strings.uninstallErrorMessage.withArgs("Lostie", "/storage/emulated/0/Games/Lostie")

        message shouldContain "Lostie"
        message shouldContain "/storage/emulated/0/Games/Lostie"
        message shouldContain "\n"
    }

    @Test
    @DisplayName("inserts an argument verbatim, without rescanning it")
    fun `an argument containing a placeholder is not substituted again`() {
        val template = "{0} and {1}"

        val message = template.withArgs("{1}", "second")

        message shouldBe "{1} and second"
    }

    @Test
    @DisplayName("leaves a placeholder with no matching argument alone")
    fun `a missing argument does not throw`() {
        val message = "{0} and {1}".withArgs("only one")

        message shouldBe "only one and {1}"
    }

    @Test
    @DisplayName("leaves anything that is not a numbered placeholder alone")
    fun `braces that are not placeholders survive`() {
        "{} {a} {01x} {0}".withArgs("x") shouldBe "{} {a} {01x} x"
    }

    @Test
    @DisplayName("returns the string untouched when there is nothing to substitute")
    fun `no arguments means no work`() {
        val text = "Ajustes"
        text.withArgs() shouldBe text
    }

    @Test
    @DisplayName("renders a null argument as an empty string")
    fun `a null argument does not print the word null`() {
        "[{0}]".withArgs(null) shouldBe "[]"
    }
}
