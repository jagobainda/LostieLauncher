package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.LocalGame
import java.util.UUID

object GameIdentityMatcher {
    private val missingId = UUID(0, 0)

    fun findRemote(local: LocalGame, catalogue: List<GameInfo>): GameInfo? {
        local.id.takeUnless { it == missingId }?.let { id ->
            catalogue.firstOrNull { it.id == id }?.let { return it }
        }
        return catalogue.firstOrNull { matches(it, local) }
    }

    fun findLocal(remote: GameInfo, installed: List<LocalGame>): LocalGame? {
        remote.id?.takeUnless { it == missingId }?.let { id ->
            installed.firstOrNull { it.id == id }?.let { return it }
        }
        return installed.firstOrNull { matches(remote, it) }
    }

    fun matches(remote: GameInfo, local: LocalGame): Boolean {
        val remoteId = remote.id?.takeUnless { it == missingId }
        val localId = local.id.takeUnless { it == missingId }
        if (remoteId != null && localId != null) return remoteId == localId
        return remote.name.equals(local.name, ignoreCase = true)
    }
}
