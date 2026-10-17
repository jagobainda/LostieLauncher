package dev.jagoba.lostielauncher.ui

import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.content.withArgs
import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("withArgs, called from outside content/")
class WithArgsResolutionTest {
    @Test
    @DisplayName("substitutes, rather than silently resolving to the standard library")
    fun `a placeholder message is substituted from another package`() {
        val message = stringsFor(AppLanguage.ENG).uninstallConfirmMessage

        val rendered = message.withArgs("Lostie")

        rendered shouldNotContain "{0}"
        rendered shouldBe
            "Are you sure you want to uninstall Lostie? " +
            "Your saved games and playtime record will not be lost."
    }

    @Test
    @DisplayName("substitutes every one of the five keys that take arguments")
    fun `no placeholder survives in any of the five`() {
        val arguments = arrayOf("a", "b")

        AppLanguage.entries.forEach { language ->
            val strings = stringsFor(language)
            listOf(
                strings.uninstallConfirmMessage,
                strings.uninstallErrorMessage,
                strings.uninstallBlockedMessage,
                strings.uninstallGameRunningMessage,
                strings.uninstallMaybeRunningMessage,
            ).forEach { template ->
                template.withArgs(*arguments) shouldNotContain Regex("""\{\d}""")
            }
        }
    }
}
