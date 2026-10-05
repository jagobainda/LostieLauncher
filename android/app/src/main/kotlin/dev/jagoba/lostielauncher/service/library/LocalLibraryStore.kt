package dev.jagoba.lostielauncher.service.library

import dev.jagoba.lostielauncher.model.LocalGame
import java.util.UUID

interface LocalLibraryStore {
    suspend fun getGames(): List<LocalGame>

    suspend fun registerGame(game: LocalGame)

    suspend fun removeGame(gameName: String)

    suspend fun addPlaytime(gameId: UUID, minutes: Int)

    suspend fun getPlaytimes(): Map<UUID, Int>
}
