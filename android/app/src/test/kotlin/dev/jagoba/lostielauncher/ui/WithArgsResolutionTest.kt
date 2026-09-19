package dev.jagoba.lostielauncher.ui

import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.content.withArgs
import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * That a placeholder message substituted from outside `content/` really is
 * substituted.
 *
 * This test is in `ui/` on purpose, because `content/` is the one package where
 * the bug it guards against cannot happen. The catalogue's substitution used to
 * be called `format`, and `kotlin.text` declares `String.format(vararg Any?)`
 * and is imported into every file by default: from any other package, a call
 * that omitted the explicit import resolved to the standard library's, which
 * looks for `%s`, finds none, and hands back the template with `{0}` still in
 * it. No warning and no error — the first sign would have been a user reading
 * "uninstall {0}?" in a dialog.
 *
 * Renaming it to `withArgs` is what actually fixes that, since the standard
 * library has nothing of that name to lose to. This is the regression test, and
 * it is only meaningful from here: the eight messages that take arguments are
 * all rendered from `ui/`, by port plan steps 13 and 14.
 */
@DisplayName("withArgs, called from outside content/")
class WithArgsResolutionTest {
    @Test
    @DisplayName("substitutes, rather than silently resolving to the standard library")
    fun `a placeholder message is substituted from another package`() {
        // Arrange — the shortest of the eight, so the assertion reads.
        val message = stringsFor(AppLanguage.ENG).uninstallConfirmMessage

        // Act
        val rendered = message.withArgs("Lostie")

        // Assert — the second is the one that matters: `shouldContain` alone
        // would still pass against the untouched template.
        rendered shouldNotContain "{0}"
        rendered shouldBe
            "Are you sure you want to uninstall Lostie? " +
            "Your saved games and playtime record will not be lost."
    }

    @Test
    @DisplayName("substitutes every one of the eight keys that take arguments")
    fun `no placeholder survives in any of the eight`() {
        // Arrange — spec/05-localization.md names them, and each is checked in
        // all eight languages: a resolution that went wrong would go wrong
        // everywhere, but a *translation* carrying the wrong placeholder count
        // would not, and this catches both.
        val arguments = arrayOf("a", "b")

        // Act / Assert
        AppLanguage.entries.forEach { language ->
            val strings = stringsFor(language)
            listOf(
                strings.uninstallConfirmMessage,
                strings.uninstallErrorMessage,
                strings.uninstallBlockedMessage,
                strings.uninstallGameRunningMessage,
                strings.uninstallMaybeRunningMessage,
                strings.updateAvailableMessage,
                strings.oneDriveWarningMessage,
                strings.downloadDirNotUsableMessage,
            ).forEach { template ->
                template.withArgs(*arguments) shouldNotContain Regex("""\{\d}""")
            }
        }
    }
}
