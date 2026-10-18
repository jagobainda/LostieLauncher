package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.OpenGameLocationResult

interface GameLocationService {
    suspend fun openBlockingLocation(location: GameLocationReference): OpenGameLocationResult
}
