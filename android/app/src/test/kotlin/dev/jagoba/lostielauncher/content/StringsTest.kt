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

    @Test
    @DisplayName("declares the 114 keys ported from the desktop and the Android-only key")
    fun `has the expected number of keys`() {
        val keys = KEYS

        keys.size shouldBe 115
    }

    @Test
    @DisplayName("has dropped the four keys whose surfaces do not exist on Android")
    fun `the Windows-only keys are gone from every language`() {
        val dropped = setOf(
            "settingsStartWithWindows",
            "settingsStartMinimized",
            "trayOpen",
            "trayExit",
        )

        KEYS.intersect(dropped).shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("has a non-blank value for every key")
    fun `every key is filled in`(language: AppLanguage) {
        val strings = stringsFor(language)

        val blank = KEY_GETTERS.filter { it.valueIn(strings).isBlank() }.map { it.keyName() }

        blank.shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("resolves to its own implementation")
    fun `each language resolves to a distinct catalogue`(language: AppLanguage) {
        val strings = stringsFor(language)

        AppLanguage.entries
            .filter { it != language }
            .forEach { other -> (stringsFor(other) === strings) shouldBe false }
    }

    @Test
    @DisplayName("keeps placeholder count and order identical in all eight languages")
    fun `placeholders do not drift between languages`() {
        val reference = AppLanguage.Default

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
        val withPlaceholders = KEY_GETTERS
            .filter { placeholdersOf(it.valueIn(stringsFor(AppLanguage.Default))).isNotEmpty() }
            .map { it.keyName() }
            .toSet()

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

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    @DisplayName("holds the same repository URL in every language, over HTTPS")
    fun `the repository url does not vary`(language: AppLanguage) {
        val url = stringsFor(language).repositoryUrl

        url shouldBe "https://github.com/jagobainda/LostieLauncher"
    }

    private companion object {
        val KEY_GETTERS: List<Method> = Strings::class.java.methods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 }
            .sortedBy { it.name }

        val KEYS: Set<String> = KEY_GETTERS.map { it.keyName() }.toSet()

        fun Method.keyName(): String = name.removePrefix("get").replaceFirstChar { it.lowercase() }

        fun Method.valueIn(strings: Strings): String = invoke(strings) as String

        val PLACEHOLDER = Regex("""\{(\d+)}""")

        fun placeholdersOf(text: String): List<String> = PLACEHOLDER.findAll(text).map { it.value }.toList()

        inline fun withClue(key: String, language: AppLanguage, block: () -> Unit) {
            try {
                block()
            } catch (failure: AssertionError) {
                throw AssertionError("$key in $language: ${failure.message}", failure)
            }
        }
    }
}
