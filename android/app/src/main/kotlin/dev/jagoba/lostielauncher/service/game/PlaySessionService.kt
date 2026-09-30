package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GamePlaySession
import dev.jagoba.lostielauncher.model.PlaySessionResult

interface PlaySessionService {
    suspend fun recordCompletedSession(session: GamePlaySession): PlaySessionResult
}
