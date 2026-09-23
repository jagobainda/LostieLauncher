package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.GamePlaySession
import dev.jagoba.lostielauncher.model.PlaySessionResult

/** Isolates game-session accounting from the persisted playtime ledger. */
interface PlaySessionService {
    suspend fun recordCompletedSession(session: GamePlaySession): PlaySessionResult
}
