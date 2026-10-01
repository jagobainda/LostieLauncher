package dev.jagoba.lostielauncher.service.library

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RoomLocalLibraryStore")
class RoomLocalLibraryStoreTest {
    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `returns an empty registry when no game has been installed`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())

        sut.getGames().shouldBeEmpty()
    }

    @Test
    fun `registers a game`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())
        val game = game("Test Game", "v1.0.0")

        sut.registerGame(game)

        sut.getGames() shouldBe listOf(game)
    }

    @Test
    fun `replaces a registered game with the same name ignoring case`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())
        sut.registerGame(game("Test Game", "v1.0.0"))
        val replacement = game("test game", "v1.1.0")

        sut.registerGame(replacement)

        sut.getGames() shouldBe listOf(replacement)
    }

    @Test
    fun `keeps the first stored game when different names share an id`() = runTest {
        val id = UUID.randomUUID()
        val first = game("Original", "v1.0.0", id)
        val duplicate = game("Renamed", "v2.0.0", id)
        val sut = createSut(FakeLocalLibraryDao(listOf(first.toEntity(1), duplicate.toEntity(2))))

        sut.getGames() shouldBe listOf(first)
        verify(exactly = 1) { logger.info(match { it.contains("Skipping duplicate local game entry") }) }
    }

    @Test
    fun `removes a registered game ignoring case`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())
        sut.registerGame(game("Game A"))
        sut.registerGame(game("Game B"))

        sut.removeGame("game a")

        sut.getGames().map { it.name } shouldBe listOf("Game B")
    }

    @Test
    fun `removing a missing game is a no op`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())

        sut.removeGame("Missing")

        sut.getGames().shouldBeEmpty()
    }

    @Test
    fun `returns empty playtimes before any session is recorded`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())

        sut.getPlaytimes() shouldBe emptyMap()
    }

    @Test
    fun `ignores playtime for the empty id`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())

        sut.addPlaytime(UUID(0, 0), 30)

        sut.getPlaytimes() shouldBe emptyMap()
    }

    @Test
    fun `accumulates playtime for one game`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())
        val id = UUID.randomUUID()

        sut.addPlaytime(id, 30)
        sut.addPlaytime(id, 15)

        sut.getPlaytimes() shouldBe mapOf(id to 45)
    }

    @Test
    fun `does not lose concurrent playtime updates`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())
        val id = UUID.randomUUID()

        List(100) { async { sut.addPlaytime(id, 1) } }.awaitAll()

        sut.getPlaytimes() shouldBe mapOf(id to 100)
    }

    @Test
    fun `persists every concurrent game registration`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())

        List(100) { index -> async { sut.registerGame(game("Game $index")) } }.awaitAll()

        sut.getGames() shouldHaveSize 100
    }

    @Test
    fun `allows reads to interleave with writes without losing playtime`() = runTest {
        val sut = createSut(FakeLocalLibraryDao())
        val id = UUID.randomUUID()

        val writes = List(100) { async { sut.addPlaytime(id, 1) } }
        val reads = List(25) { async { sut.getPlaytimes() } }
        (writes + reads).awaitAll()

        sut.getPlaytimes() shouldBe mapOf(id to 100)
    }

    @Test
    fun `skips an invalid stored game id and logs it`() = runTest {
        val dao = FakeLocalLibraryDao(
            initialGames = listOf(
                InstalledGameEntity(
                    nameKey = "bad",
                    gameId = "not-a-guid",
                    name = "Bad",
                    version = "1",
                    variant = null,
                ),
                game("Good").toEntity(2),
            ),
        )
        val sut = createSut(dao)

        sut.getGames().map { it.name } shouldBe listOf("Good")
        verify(exactly = 1) { logger.error(match { it.contains("invalid local game id") }, any()) }
    }

    @Test
    fun `degrades to an empty registry when the database cannot be read`() = runTest {
        val sut = createSut(FailingLocalLibraryDao())

        sut.getGames().shouldBeEmpty()
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `swallows and logs a failed registration`() = runTest {
        val sut = createSut(FailingLocalLibraryDao())

        sut.registerGame(game("Test"))

        verify(exactly = 1) { logger.error(any(), any()) }
    }

    private fun TestScope.createSut(dao: LocalLibraryDao) = RoomLocalLibraryStore(
        dao = dao,
        logger = logger,
        dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )

    private fun game(name: String, version: String = "v1.0.0", id: UUID = UUID.randomUUID()) = LocalGame(
        id = id,
        name = name,
        version = version,
        type = null,
    )

    private fun LocalGame.toEntity(rowId: Long = 1) = InstalledGameEntity(
        rowId = rowId,
        nameKey = name.lowercase(),
        gameId = id.toString(),
        name = name,
        version = version,
        variant = type,
    )

    private class FakeLocalLibraryDao(initialGames: List<InstalledGameEntity> = emptyList()) : LocalLibraryDao() {
        private val gate = Mutex()
        private val games = initialGames.associateByTo(linkedMapOf()) { it.nameKey }
        private val playtimes = linkedMapOf<String, Int>()
        private var nextRowId = (initialGames.maxOfOrNull { it.rowId } ?: 0) + 1

        override suspend fun getGames(): List<InstalledGameEntity> = gate.withLock {
            games.values.sortedBy { it.rowId }
        }

        override suspend fun insertGame(game: InstalledGameEntity) {
            gate.withLock {
                games[game.nameKey] = game.copy(rowId = nextRowId++)
            }
        }

        override suspend fun deleteGame(nameKey: String) {
            gate.withLock { games.remove(nameKey) }
        }

        override suspend fun getPlaytimes(): List<PlaytimeEntity> = gate.withLock {
            playtimes.map { PlaytimeEntity(it.key, it.value) }
        }

        override suspend fun incrementPlaytime(gameId: String, minutes: Int): Int = gate.withLock {
            val current = playtimes[gameId] ?: return@withLock 0
            playtimes[gameId] = current + minutes
            1
        }

        override suspend fun insertPlaytime(playtime: PlaytimeEntity): Long = gate.withLock {
            if (playtime.gameId in playtimes) return@withLock -1
            playtimes[playtime.gameId] = playtime.minutes
            1
        }
    }

    private class FailingLocalLibraryDao : LocalLibraryDao() {
        override suspend fun getGames(): List<InstalledGameEntity> = throw IOException("database unavailable")

        override suspend fun insertGame(game: InstalledGameEntity): Unit = throw IOException("database unavailable")

        override suspend fun deleteGame(nameKey: String): Unit = throw IOException("database unavailable")

        override suspend fun getPlaytimes(): List<PlaytimeEntity> = throw IOException("database unavailable")

        override suspend fun incrementPlaytime(gameId: String, minutes: Int): Int =
            throw IOException("database unavailable")

        override suspend fun insertPlaytime(playtime: PlaytimeEntity): Long = throw IOException("database unavailable")
    }
}
