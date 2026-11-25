package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.KirinOptions
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

    private fun createSut(
        ioDispatcher: CoroutineDispatcher = dispatcher,
        maxLibraryFileBytes: Long = KirinOptions.DEFAULT_MAX_LIBRARY_FILE_BYTES,
    ) = KirinGameLibrary(
        KirinOptions(
            directory.toFile(),
            libraryFile,
            "Game.exe",
            "auto",
            "",
            "",
            "",
            "",
            maxLibraryFileBytes = maxLibraryFileBytes,
        ),
        TestDispatcherProvider(ioDispatcher),
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
    fun `refreshes launch options for an existing path and preserves other fields`() = runTest(dispatcher) {
        val path = gameDirectory.absolutePath
        val old = entry("Old", path, "executableName" to "Old.exe", "runtimeMode" to "compat", "icon" to "cover.png")
        libraryFile.parentFile?.mkdirs()
        libraryFile.writeText(JsonArray(listOf(old)).toString())
        val sut = createSut()

        sut.register("Demo", gameDirectory)

        readLibrary() shouldBe JsonArray(
            listOf(entry("Demo", path, "executableName" to "Game.exe", "runtimeMode" to "auto", "icon" to "cover.png")),
        )
    }

    @Test
    fun `concurrent registrations and removals preserve every change`() = runTest(dispatcher) {
        val added = (1..16).map { directory.resolve("new-$it").toFile() }
        val removed = (1..16).map { directory.resolve("old-$it").toFile() }
        val untouched = entry("Other", "/storage/other", "runtimeMode" to "compat")
        libraryFile.parentFile?.mkdirs()
        libraryFile.writeText(JsonArray(removed.map { entry(it.name, it.absolutePath) } + untouched).toString())

        Executors.newFixedThreadPool(4).asCoroutineDispatcher().use { ioDispatcher ->
            val sut = createSut(ioDispatcher)
            coroutineScope {
                for (index in added.indices) {
                    launch { sut.register(added[index].name, added[index]) }
                    launch { sut.unregister(removed[index]) }
                }
            }
        }

        val expected = added.map {
            entry(it.name, it.absolutePath, "executableName" to "Game.exe", "runtimeMode" to "auto")
        }
        (readLibrary() as JsonArray) shouldContainExactlyInAnyOrder (expected + untouched)
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

    @Test
    fun `unregister removes only the game entry`() = runTest(dispatcher) {
        val other = entry("Other", "/storage/other")
        libraryFile.parentFile?.mkdirs()
        libraryFile.writeText(JsonArray(listOf(other, entry("Demo", gameDirectory.absolutePath))).toString())
        val sut = createSut()

        sut.unregister(gameDirectory)

        readLibrary() shouldBe JsonArray(listOf(other))
    }

    @Test
    fun `unregister does not create a missing library`() = runTest(dispatcher) {
        val sut = createSut()

        sut.unregister(gameDirectory)

        libraryFile.exists() shouldBe false
    }

    @Test
    fun `rejects an oversized library file without modifying it`() = runTest(dispatcher) {
        val oversized = "[" + " ".repeat(64) + "]"
        libraryFile.parentFile?.mkdirs()
        libraryFile.writeText(oversized)
        val sut = createSut(maxLibraryFileBytes = 32)

        shouldThrow<IOException> { sut.register("Demo", gameDirectory) }
        shouldThrow<IOException> { sut.unregister(gameDirectory) }

        libraryFile.readText() shouldBe oversized
        File(libraryFile.parentFile, "${libraryFile.name}.tmp").exists() shouldBe false
    }
}
