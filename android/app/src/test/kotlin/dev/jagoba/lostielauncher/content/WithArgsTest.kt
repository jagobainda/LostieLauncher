package dev.jagoba.lostielauncher.content

import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("String.withArgs")
class WithArgsTest {
    // ---- what the catalogue actually asks of it ----

    @Test
    @DisplayName("substitutes a single placeholder")
    fun `replaces {0}`() {
        // Arrange / Act / Assert
        "Uninstall {0}?".withArgs("Pokémon Lostie") shouldBe "Uninstall Pokémon Lostie?"
    }

    @Test
    @DisplayName("substitutes two placeholders in their own positions")
    fun `replaces {0} and {1} independently`() {
        // Arrange — UninstallErrorMessage takes the game name and then the
        // blocking path, and both appear once.
        val template = "{0} left this behind:\n\n{1}\n\nOpen it?"

        // Act
        val message = template.withArgs("Lostie", """C:\Games\Lostie\data""")

        // Assert
        message shouldBe "Lostie left this behind:\n\nC:\\Games\\Lostie\\data\n\nOpen it?"
    }

    @Test
    @DisplayName("keeps the catalogue's own multi-line strings intact")
    fun `works against a real catalogue string`() {
        // Arrange — DownloadDirNotUsableMessage is the two-argument one, and
        // the second argument is itself a localized step name.
        val strings = stringsFor(AppLanguage.ENG)

        // Act
        val message = strings.downloadDirNotUsableMessage
            .withArgs("/storage/emulated/0/Games", strings.downloadDirStepWrite)

        // Assert
        message shouldContain "/storage/emulated/0/Games"
        message shouldContain strings.downloadDirStepWrite
        message shouldContain "\n"
    }

    // ---- the edges, and why each one behaves as it does ----

    @Test
    @DisplayName("inserts an argument verbatim, without rescanning it")
    fun `an argument containing a placeholder is not substituted again`() {
        // Arrange — a game name or a filesystem path is user-influenced data.
        // A second pass over it would let "{1}" inside an argument pull in
        // another argument, which is the small, avoidable injection this single
        // pass rules out.
        val template = "{0} and {1}"

        // Act
        val message = template.withArgs("{1}", "second")

        // Assert
        message shouldBe "{1} and second"
    }

    @Test
    @DisplayName("leaves a placeholder with no matching argument alone")
    fun `a missing argument does not throw`() {
        // Arrange / Act — a dialog with one argument too few must still open.
        // `string.Format` on the desktop would throw; here the placeholder is
        // left visible, which is a bug a tester can see and not a crash.
        val message = "{0} and {1}".withArgs("only one")

        // Assert
        message shouldBe "only one and {1}"
    }

    @Test
    @DisplayName("leaves anything that is not a numbered placeholder alone")
    fun `braces that are not placeholders survive`() {
        // Arrange / Act / Assert — no catalogue string does this today, but
        // nothing stops one from doing it later, and a formatter that ate them
        // would be a surprise at that point rather than here.
        "{} {a} {01x} {0}".withArgs("x") shouldBe "{} {a} {01x} x"
    }

    @Test
    @DisplayName("returns the string untouched when there is nothing to substitute")
    fun `no arguments means no work`() {
        // Arrange / Act / Assert — the overwhelmingly common case: 106 of the
        // 114 keys carry no placeholder at all.
        val text = "Ajustes"
        text.withArgs() shouldBe text
    }

    @Test
    @DisplayName("renders a null argument as an empty string")
    fun `a null argument does not print the word null`() {
        // Arrange / Act / Assert — the alternative is the literal text "null"
        // appearing in a dialog, which is the worse of the two failures.
        "[{0}]".withArgs(null) shouldBe "[]"
    }
}
