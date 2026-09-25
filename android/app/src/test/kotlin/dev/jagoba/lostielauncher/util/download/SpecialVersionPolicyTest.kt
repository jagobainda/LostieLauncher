package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.SpecialVersionConfig
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.Test

class SpecialVersionPolicyTest {
    private val gameId = UUID.randomUUID()
    private val valid = SpecialVersionConfig("a".repeat(64), "special", gameId, "v2", "game.zip")

    @Test
    fun `key must match five groups of four alphanumeric characters`() {
        SpecialVersionPolicy.isValidKey("ABCD-1234-EFGH-5678-IJKL") shouldBe true
        SpecialVersionPolicy.isValidKey("bad") shouldBe false
    }

    @Test
    fun `archive hash version and target must all be valid`() {
        SpecialVersionPolicy.isValidConfig(valid) shouldBe true
        SpecialVersionPolicy.isValidConfig(valid.copy(fileName = "../game.zip")) shouldBe false
        SpecialVersionPolicy.isValidConfig(valid.copy(sha256 = "bad")) shouldBe false
        SpecialVersionPolicy.isValidConfig(valid.copy(version = " ")) shouldBe false
        SpecialVersionPolicy.matchesGame(valid, UUID.randomUUID()) shouldBe false
    }
}
