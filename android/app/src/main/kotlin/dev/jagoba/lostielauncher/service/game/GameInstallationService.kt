package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallResult
import dev.jagoba.lostielauncher.model.InstalledGameResult
import dev.jagoba.lostielauncher.model.InstalledGamesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GameInstallationService {
    val installedGames: StateFlow<InstalledGamesState>

    suspend fun install(request: GameInstallationRequest): GameInstallationResult

    fun observeInstallation(gameId: String): Flow<GameInstallationState>

    suspend fun uninstall(game: GameTarget): GameUninstallResult

    suspend fun findInstalled(game: GameTarget): InstalledGameResult
}
