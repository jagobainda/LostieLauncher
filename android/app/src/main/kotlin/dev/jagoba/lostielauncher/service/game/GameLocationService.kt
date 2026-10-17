package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.GameLocation
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.OpenGameLocationResult

interface GameLocationService {
    suspend fun helpAvailability(game: GameTarget): GameHelpAvailability

    suspend fun open(game: GameTarget, location: GameLocation): OpenGameLocationResult

    suspend fun openBlockingLocation(location: GameLocationReference): OpenGameLocationResult
}
