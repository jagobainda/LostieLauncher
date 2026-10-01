package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.LocalGame
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.Test

class GameIdentityMatcherTest {
    private val firstId = UUID.randomUUID()
    private val secondId = UUID.randomUUID()

    @Test
    fun `matching IDs win despite renamed game`() {
        GameIdentityMatcher.matches(game(firstId, "New Name"), LocalGame(firstId, "Old Name", "v1", null)) shouldBe true
    }

    @Test
    fun `conflicting IDs never match by name`() {
        GameIdentityMatcher.matches(game(firstId, "Same"), LocalGame(secondId, "Same", "v1", null)) shouldBe false
    }

    @Test
    fun `missing ID permits case insensitive name match`() {
        GameIdentityMatcher.matches(game(null, "SAME"), LocalGame(secondId, "same", "v1", null)) shouldBe true
        GameIdentityMatcher.matches(game(firstId, "Same"), LocalGame(UUID(0, 0), "same", "v1", null)) shouldBe true
    }

    @Test
    fun `ID lookup precedes earlier name fallback`() {
        val idlessRemote = game(null, "Same")
        val identifiedRemote = game(firstId, "Renamed")
        val local = LocalGame(firstId, "Same", "v1", null)
        GameIdentityMatcher.findRemote(local, listOf(idlessRemote, identifiedRemote)) shouldBe identifiedRemote

        val idlessLocal = LocalGame(UUID(0, 0), "Same", "v1", null)
        val identifiedLocal = LocalGame(firstId, "Renamed", "v1", null)
        GameIdentityMatcher.findLocal(game(firstId, "Same"), listOf(idlessLocal, identifiedLocal)) shouldBe
            identifiedLocal
    }

    private fun game(id: UUID?, name: String): GameInfo =
        GameInfo(id, name, "v1", 1.0, "", "", null, "/game.zip", "a".repeat(64))
}
