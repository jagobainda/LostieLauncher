package dev.jagoba.lostielauncher.service.library

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.LocalLibraryOptions
import dev.jagoba.lostielauncher.model.MissingGameId
import dev.jagoba.lostielauncher.util.file.AtomicFileWriter
import dev.jagoba.lostielauncher.util.log.Logger
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
internal class FileLocalLibraryStore @Inject constructor(
    private val options: LocalLibraryOptions,
    private val json: Json,
    private val logger: Logger,
    private val dispatchers: DispatcherProvider,
) : LocalLibraryStore {
    private val gamesGate = Mutex()

    override suspend fun getGames(): List<LocalGame> = withContext(dispatchers.io) {
        gamesGate.withLock {
            try {
                deduplicateGames(readEntries().mapNotNull(::toLocalGame))
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
                writeEntries(readEntries().filterNot { it.name.toNameKey() == game.name.toNameKey() } + game.toEntry())
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
                if (options.registryFile.exists()) {
                    writeEntries(readEntries().filterNot { it.name.toNameKey() == gameName.toNameKey() })
                }
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error("Could not remove local game '$gameName'", cause)
            }
        }
    }

    private fun readEntries(): List<LocalGameEntry> =
        if (options.registryFile.exists()) json.decodeFromString(options.registryFile.readText()) else emptyList()

    private fun writeEntries(entries: List<LocalGameEntry>) =
        AtomicFileWriter.writeText(options.registryFile, json.encodeToString(entries))

    private fun toLocalGame(entry: LocalGameEntry): LocalGame? = runCatching {
        LocalGame(
            id = UUID.fromString(entry.id),
            name = entry.name,
            version = entry.version,
            type = entry.variant,
        )
    }.onFailure {
        logger.error("Ignoring an invalid local game id '${entry.id}'", it)
    }.getOrNull()

    private fun deduplicateGames(games: List<LocalGame>): List<LocalGame> {
        val seenIds = mutableSetOf<UUID>()
        val seenNames = mutableSetOf<String>()
        return games.filter { game ->
            val isUnique = if (game.id != MissingGameId) seenIds.add(game.id) else seenNames.add(game.name.toNameKey())
            if (!isUnique) logger.info("Skipping duplicate local game entry: '${game.name}' (id: ${game.id})")
            isUnique
        }
    }

    private fun LocalGame.toEntry() = LocalGameEntry(
        id = id.toString(),
        name = name,
        version = version,
        variant = type,
    )

    private fun String.toNameKey(): String = lowercase(Locale.ROOT)
}

@Serializable
private data class LocalGameEntry(val id: String, val name: String, val version: String, val variant: String? = null)
