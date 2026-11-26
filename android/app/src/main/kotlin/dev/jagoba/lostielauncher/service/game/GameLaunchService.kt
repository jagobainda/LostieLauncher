package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameRunningSignal
import dev.jagoba.lostielauncher.model.GameTarget

interface GameLaunchService {
    suspend fun launch(game: GameTarget): GameLaunchResult

    suspend fun runningSignal(game: GameTarget): GameRunningSignal
}
