package dev.jagoba.lostielauncher.service.library

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.util.log.Logger
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
internal class RoomLocalLibraryStore @Inject constructor(
    private val dao: LocalLibraryDao,
    private val logger: Logger,
    private val dispatchers: DispatcherProvider,
) : LocalLibraryStore {
    private val gamesGate = Mutex()
    private val playtimeGate = Mutex()

    override suspend fun getGames(): List<LocalGame> = withContext(dispatchers.io) {
        gamesGate.withLock {
            try {
                deduplicateGames(dao.getGames().mapNotNull(::toLocalGame))
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error("Could not read the local game registry", cause)
                emptyList()
            }
        }
    }

    override suspend fun registerGame(game: LocalGame) = withContext(dispatchers.io) {
        gamesGate.withLock {
            try {
                dao.insertGame(game.toEntity())
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error("Could not register local game '${game.name}'", cause)
            }
        }
    }

    override suspend fun removeGame(gameName: String) = withContext(dispatchers.io) {
        gamesGate.withLock {
            try {
                dao.deleteGame(gameName.toNameKey())
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error("Could not remove local game '$gameName'", cause)
            }
        }
    }

    override suspend fun addPlaytime(gameId: UUID, minutes: Int) = withContext(dispatchers.io) {
        if (gameId == EmptyUuid) return@withContext
        playtimeGate.withLock {
            try {
                dao.addPlaytime(gameId.toString(), minutes)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error("Could not add playtime for game '$gameId'", cause)
            }
        }
    }

    override suspend fun getPlaytimes(): Map<UUID, Int> = withContext(dispatchers.io) {
        playtimeGate.withLock {
            try {
                dao.getPlaytimes().mapNotNull { entity ->
                    runCatching { UUID.fromString(entity.gameId) }
                        .onFailure { logger.error("Ignoring an invalid playtime game id '${entity.gameId}'", it) }
                        .getOrNull()
                        ?.let { it to entity.minutes }
                }.toMap()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error("Could not read playtimes", cause)
                emptyMap()
            }
        }
    }

    private fun toLocalGame(entity: InstalledGameEntity): LocalGame? = runCatching {
        LocalGame(
            id = UUID.fromString(entity.gameId),
            name = entity.name,
            version = entity.version,
            type = entity.variant,
        )
    }.onFailure {
        logger.error("Ignoring an invalid local game id '${entity.gameId}'", it)
    }.getOrNull()

    private fun deduplicateGames(games: List<LocalGame>): List<LocalGame> {
        val seenIds = mutableSetOf<UUID>()
        val seenNames = mutableSetOf<String>()
        return games.filter { game ->
            val isUnique = if (game.id != EmptyUuid) seenIds.add(game.id) else seenNames.add(game.name.toNameKey())
            if (!isUnique) logger.info("Skipping duplicate local game entry: '${game.name}' (id: ${game.id})")
            isUnique
        }
    }

    private fun LocalGame.toEntity() = InstalledGameEntity(
        nameKey = name.toNameKey(),
        gameId = id.toString(),
        name = name,
        version = version,
        variant = type,
    )

    private fun String.toNameKey(): String = lowercase(Locale.ROOT)

    private companion object {
        val EmptyUuid = UUID(0, 0)
    }
}
