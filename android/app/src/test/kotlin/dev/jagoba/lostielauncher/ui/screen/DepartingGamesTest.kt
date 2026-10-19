package dev.jagoba.lostielauncher.ui.screen

import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.ui.viewmodel.InstalledGameUiState
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.Test

class DepartingGamesTest {
    private val first = row("First")
    private val second = row("Second")
    private val third = row("Third")

    @Test
    fun `an uninstalled game keeps its place until its animation ends`() {
        val uninstalling = second.copy(isUninstalling = true)
        withDepartingGames(listOf(first, uninstalling, third), listOf(first, third)) shouldBe
            listOf(first, uninstalling, third)
    }

    @Test
    fun `the only game stays so the list does not turn into the empty state at once`() {
        val uninstalling = first.copy(isUninstalling = true)
        withDepartingGames(listOf(uninstalling), emptyList()) shouldBe listOf(uninstalling)
    }

    @Test
    fun `a game that leaves without uninstalling is dropped straight away`() {
        withDepartingGames(listOf(first, second), listOf(first)) shouldBe listOf(first)
    }

    private fun row(name: String) = InstalledGameUiState(
        game = LocalGame(UUID.randomUUID(), name, "v1.0", null),
        remote = null,
        hasUpdate = false,
        isUpdating = false,
        isUninstalling = false,
    )
}
