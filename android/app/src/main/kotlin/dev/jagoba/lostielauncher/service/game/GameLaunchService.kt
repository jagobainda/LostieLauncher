package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GameActivityState
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameTarget
import kotlinx.coroutines.flow.StateFlow

/** Isolates launching a game and determining whether it is still active. */
interface GameLaunchService {
    val activeSessions: StateFlow<GameActivityState>

    suspend fun launch(game: GameTarget): GameLaunchResult

    suspend fun runningSignal(game: GameTarget): GameRunningSignal
}
