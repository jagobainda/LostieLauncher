package dev.jagoba.lostielauncher.content

import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.lang.reflect.Method
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("Strings")
class StringsTest {
    // ---- the catalogue is complete ----

    @Test
    @DisplayName("declares the 114 keys ported from the desktop")
    fun `has the expected number of keys`() {
        // Arrange / Act — the desktop's IStrings has 118. Four were dropped
        // with the Windows-only surfaces they name, which the test below
        // pins down by name; this one only guards the count, so that a key
        // quietly disappearing is caught even if someone edits that list too.
        val keys = KEYS

        // Assert
        keys.size shouldBe 114
    }

    @Test
    @DisplayName("has dropped the four keys whose surfaces do not exist on Android")
    fun `the Windows-only keys are gone from every language`() {
        // Arrange — spec/10-windows-only.md, entries 2 and 3 and the closing
        // table: "Start with Windows" has no Android counterpart, and neither
        // do "Start minimized" or the two tray menu items. Dropping a setting
        // means dropping its key in all eight languages, not leaving a dead one
        // behind, and half-removing one is the failure mode named there.
        val dropped = setOf(
            "settingsStartWithWindows",
            "settingsStartMinimized",
            "trayOpen",
            "trayExit",
        )

        // Act / Assert
        KEYS.intersect(dropped).shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("has a non-blank value for every key")
    fun `every key is filled in`(language: AppLanguage) {
        // Arrange — on the desktop a missing key is a compile error, and it is
        // here too. What the compiler cannot catch is a key filled in with an
        // empty string, or left in another language because someone copied an
        // implementation to start a new one.
        val strings = stringsFor(language)

        // Act
        val blank = KEY_GETTERS.filter { it.valueIn(strings).isBlank() }.map { it.keyName() }

        // Assert
        blank.shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("resolves to its own implementation")
    fun `each language resolves to a distinct catalogue`(language: AppLanguage) {
        // Arrange / Act — `stringsFor` is a ten-line `when`, which is exactly
        // the shape a copy-paste hides in: the wrong arm simply shows another
        // language's text.
        val strings = stringsFor(language)

        // Assert
        AppLanguage.entries
            .filter { it != language }
            .forEach { other -> (stringsFor(other) === strings) shouldBe false }
    }

    // ---- placeholders ----

    @Test
    @DisplayName("keeps placeholder count and order identical in all eight languages")
    fun `placeholders do not drift between languages`() {
        // Arrange — spec/05-localization.md names the eight keys that carry a
        // placeholder and records that there is no drift in any of them. Drift
        // here is not cosmetic: the arguments are positional, so a translation
        // that swapped {0} and {1} would put a filesystem path where a game
        // name belongs, in one language only.
        val reference = AppLanguage.Default

        // Act / Assert
        KEY_GETTERS.forEach { getter ->
            val expected = placeholdersOf(getter.valueIn(stringsFor(reference)))
            AppLanguage.entries.forEach { language ->
                val actual = placeholdersOf(getter.valueIn(stringsFor(language)))
                withClue(getter.keyName(), language) { actual shouldContainExactly expected }
            }
        }
    }

    @Test
    @DisplayName("carries a placeholder in exactly the eight keys the specification names")
    fun `the keys with placeholders are the expected ones`() {
        // Arrange / Act
        val withPlaceholders = KEY_GETTERS
            .filter { placeholdersOf(it.valueIn(stringsFor(AppLanguage.Default))).isNotEmpty() }
            .map { it.keyName() }
            .toSet()

        // Assert
        withPlaceholders shouldBe setOf(
            "uninstallConfirmMessage",
            "uninstallErrorMessage",
            "uninstallBlockedMessage",
            "uninstallGameRunningMessage",
            "uninstallMaybeRunningMessage",
            "updateAvailableMessage",
            "oneDriveWarningMessage",
            "downloadDirNotUsableMessage",
        )
    }

    // ---- the one key that is not a translation ----

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("holds the same repository URL in every language, over HTTPS")
    fun `the repository url does not vary`(language: AppLanguage) {
        // Arrange / Act — it lives in the catalogue because the welcome dialog
        // reads its link target through the same interface as its text, not
        // because it varies. The HTTPS check is the one security property
        // spec/10-windows-only.md says survives the port for every outbound
        // link.
        val url = stringsFor(language).repositoryUrl

        // Assert
        url shouldBe "https://github.com/jagobainda/LostieLauncher"
    }

    private companion object {
        /**
         * The catalogue's keys, read off the interface with Java reflection —
         * `kotlin-reflect` is not on the test classpath and is not worth adding
         * for this. Kotlin compiles `val titleHome` to `getTitleHome()`, so the
         * key name is the getter with `get` stripped and the first letter
         * lowered.
         */
        val KEY_GETTERS: List<Method> = Strings::class.java.methods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 }
            .sortedBy { it.name }

        val KEYS: Set<String> = KEY_GETTERS.map { it.keyName() }.toSet()

        fun Method.keyName(): String = name.removePrefix("get").replaceFirstChar { it.lowercase() }

        fun Method.valueIn(strings: Strings): String = invoke(strings) as String

        val PLACEHOLDER = Regex("""\{(\d+)}""")

        fun placeholdersOf(text: String): List<String> = PLACEHOLDER.findAll(text).map { it.value }.toList()

        /** Names the failing key and language, which a bare `shouldBe` would not. */
        inline fun withClue(key: String, language: AppLanguage, block: () -> Unit) {
            try {
                block()
            } catch (failure: AssertionError) {
                throw AssertionError("$key in $language: ${failure.message}", failure)
            }
        }
    }
}
