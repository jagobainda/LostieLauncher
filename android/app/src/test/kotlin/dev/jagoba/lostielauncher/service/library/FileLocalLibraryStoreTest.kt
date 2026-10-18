package dev.jagoba.lostielauncher.service.library

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.LocalLibraryOptions
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Path
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("FileLocalLibraryStore")
class FileLocalLibraryStoreTest {
    @TempDir
    lateinit var directory: Path

    private val logger = mockk<Logger>(relaxed = true)
    private val registryFile get() = directory.resolve("LostieLauncher/local_games.json").toFile()

    @Test
    fun `returns an empty registry when no game has been installed`() = runTest {
        val sut = createSut()

        sut.getGames().shouldBeEmpty()
    }

    @Test
    fun `registers a game`() = runTest {
        val sut = createSut()
        val game = game("Test Game", "v1.0.0")

        sut.registerGame(game)

        sut.getGames() shouldBe listOf(game)
    }

    @Test
    fun `reads the games registered by a previous installation of the app`() = runTest {
        val game = game("Test Game", "v1.0.0")
        createSut().registerGame(game)

        createSut().getGames() shouldBe listOf(game)
    }

    @Test
    fun `replaces a registered game with the same name ignoring case`() = runTest {
        val sut = createSut()
        sut.registerGame(game("Test Game", "v1.0.0"))
        val replacement = game("test game", "v1.1.0")

        sut.registerGame(replacement)

        sut.getGames() shouldBe listOf(replacement)
    }

    @Test
    fun `keeps the first stored game when different names share an id`() = runTest {
        val id = UUID.randomUUID()
        writeEntries(entry(id.toString(), "Original", "v1.0.0"), entry(id.toString(), "Renamed", "v2.0.0"))
        val sut = createSut()

        sut.getGames() shouldBe listOf(game("Original", "v1.0.0", id))
        verify(exactly = 1) { logger.info(match { it.contains("Skipping duplicate local game entry") }) }
    }

    @Test
    fun `removes a registered game ignoring case`() = runTest {
        val sut = createSut()
        sut.registerGame(game("Game A"))
        sut.registerGame(game("Game B"))

        sut.removeGame("game a")

        sut.getGames().map { it.name } shouldBe listOf("Game B")
    }

    @Test
    fun `removing a missing game is a no op`() = runTest {
        val sut = createSut()

        sut.removeGame("Missing")

        sut.getGames().shouldBeEmpty()
        registryFile.exists() shouldBe false
    }

    @Test
    fun `persists every concurrent game registration`() = runTest {
        val sut = createSut()

        List(100) { index -> async { sut.registerGame(game("Game $index")) } }.awaitAll()

        sut.getGames() shouldHaveSize 100
    }

    @Test
    fun `skips an invalid stored game id and logs it`() = runTest {
        val good = game("Good")
        writeEntries(entry("not-a-guid", "Bad", "1"), entry(good.id.toString(), good.name, good.version))
        val sut = createSut()

        sut.getGames() shouldBe listOf(good)
        verify(exactly = 1) { logger.error(match { it.contains("invalid local game id") }, any()) }
    }

    @Test
    fun `degrades to an empty registry when the file cannot be read`() = runTest {
        writeRegistry("not json")
        val sut = createSut()

        sut.getGames().shouldBeEmpty()
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `swallows and logs a failed registration`() = runTest {
        writeRegistry("not json")
        val sut = createSut()

        sut.registerGame(game("Test"))

        verify(exactly = 1) { logger.error(any(), any()) }
    }

    private fun TestScope.createSut() = FileLocalLibraryStore(
        options = LocalLibraryOptions(registryFile),
        json = Json { ignoreUnknownKeys = true },
        logger = logger,
        dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )

    private fun game(name: String, version: String = "v1.0.0", id: UUID = UUID.randomUUID()) = LocalGame(
        id = id,
        name = name,
        version = version,
        type = null,
    )

    private fun entry(id: String, name: String, version: String) =
        """{"id":"$id","name":"$name","version":"$version"}"""

    private fun writeEntries(vararg entries: String) = writeRegistry("[${entries.joinToString(",")}]")

    private fun writeRegistry(content: String) {
        registryFile.parentFile?.mkdirs()
        registryFile.writeText(content)
    }
}
