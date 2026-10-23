package dev.jagoba.lostielauncher.service.library

import dev.jagoba.lostielauncher.model.LocalGame

interface LocalLibraryStore {
    suspend fun getGames(): List<LocalGame>

    suspend fun registerGame(game: LocalGame)

    suspend fun removeGame(gameName: String)
}
