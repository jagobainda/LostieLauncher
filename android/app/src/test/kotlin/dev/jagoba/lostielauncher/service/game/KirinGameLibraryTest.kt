package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.KirinOptions
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("KirinGameLibrary")
class KirinGameLibraryTest {
    @TempDir
    lateinit var directory: Path

    private val dispatcher = StandardTestDispatcher()
    private val libraryFile get() = directory.resolve("Kirin/game-library.json").toFile()
    private val gameDirectory get() = directory.resolve("LostieLauncher/demo").toFile()

    private fun createSut() = KirinGameLibrary(
        KirinOptions(directory.toFile(), libraryFile, "Game.exe", "auto"),
        TestDispatcherProvider(dispatcher),
    )

    private fun entry(title: String, path: String, vararg extra: Pair<String, String>) = JsonObject(
        mapOf("title" to JsonPrimitive(title), "path" to JsonPrimitive(path)) +
            extra.associate { (key, value) -> key to JsonPrimitive(value) },
    )

    private fun readLibrary(): JsonElement = Json.parseToJsonElement(libraryFile.readText())

    @Test
    fun `creates the library with the game entry when the file does not exist`() = runTest(dispatcher) {
        val sut = createSut()

        sut.register("Demo", gameDirectory)

        readLibrary() shouldBe JsonArray(
            listOf(entry("Demo", gameDirectory.absolutePath, "executableName" to "Game.exe", "runtimeMode" to "auto")),
        )
    }

    @Test
    fun `appends the game and keeps unrelated entries`() = runTest(dispatcher) {
        val other = entry("Other", "/storage/other", "runtimeMode" to "compat")
        libraryFile.parentFile?.mkdirs()
        libraryFile.writeText(JsonArray(listOf(other)).toString())
        val sut = createSut()

        sut.register("Demo", gameDirectory)

        val entries = readLibrary() as JsonArray
        entries.size shouldBe 2
        entries[0] shouldBe other
    }

    @Test
    fun `updates the title of an existing entry and preserves its settings`() = runTest(dispatcher) {
        libraryFile.parentFile?.mkdirs()
        libraryFile.writeText(
            JsonArray(listOf(entry("Old", gameDirectory.absolutePath, "runtimeMode" to "compat"))).toString(),
        )
        val sut = createSut()

        sut.register("Demo", gameDirectory)

        readLibrary() shouldBe JsonArray(listOf(entry("Demo", gameDirectory.absolutePath, "runtimeMode" to "compat")))
    }

    @Test
    fun `refuses to overwrite a library that is not a JSON array`() = runTest(dispatcher) {
        libraryFile.parentFile?.mkdirs()
        libraryFile.writeText("{}")
        val sut = createSut()

        shouldThrow<IOException> { sut.register("Demo", gameDirectory) }

        libraryFile.readText() shouldBe "{}"
        File(libraryFile.parentFile, "${libraryFile.name}.tmp").exists() shouldBe false
    }
}
