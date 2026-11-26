package dev.jagoba.lostielauncher.util.policy

import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.ui.viewmodel.testGame
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GameIdentityMatcher")
class GameIdentityMatcherTest {
    private val firstId = UUID.randomUUID()
    private val secondId = UUID.randomUUID()

    @Test
    fun `matching IDs win despite renamed game`() {
        GameIdentityMatcher.matches(
            testGame(firstId, "New Name"),
            LocalGame(firstId, "Old Name", "v1", null),
        ) shouldBe true
    }

    @Test
    fun `conflicting IDs never match by name`() {
        GameIdentityMatcher.matches(
            testGame(firstId, "Same"),
            LocalGame(secondId, "Same", "v1", null),
        ) shouldBe false
    }

    @Test
    fun `missing ID permits case insensitive name match`() {
        GameIdentityMatcher.matches(
            testGame(null, "SAME"),
            LocalGame(secondId, "same", "v1", null),
        ) shouldBe true
        GameIdentityMatcher.matches(
            testGame(firstId, "Same"),
            LocalGame(UUID(0, 0), "same", "v1", null),
        ) shouldBe true
    }

    @Test
    fun `ID lookup precedes earlier name fallback`() {
        val idlessRemote = testGame(null, "Same")
        val identifiedRemote = testGame(firstId, "Renamed")
        val local = LocalGame(firstId, "Same", "v1", null)
        GameIdentityMatcher.findRemote(local, listOf(idlessRemote, identifiedRemote)) shouldBe identifiedRemote

        val idlessLocal = LocalGame(UUID(0, 0), "Same", "v1", null)
        val identifiedLocal = LocalGame(firstId, "Renamed", "v1", null)
        GameIdentityMatcher.findLocal(testGame(firstId, "Same"), listOf(idlessLocal, identifiedLocal)) shouldBe
            identifiedLocal
    }
}
