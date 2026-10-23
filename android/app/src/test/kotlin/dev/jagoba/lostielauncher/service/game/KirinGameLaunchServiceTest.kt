package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("KirinGameLaunchService")
class KirinGameLaunchServiceTest {
    @TempDir
    lateinit var directory: Path

    private val dispatcher = StandardTestDispatcher()
    private val logger = mockk<Logger>(relaxed = true)
    private val shortcut = mockk<KirinShortcut>(relaxed = true)
    private val game = GameTarget(null, "Pokémon Añil")
    private val gameDirectory get() = directory.resolve("pok-mon-a-il").toFile()

    private fun createSut() = KirinGameLaunchService(
        PendingGameLaunchService(logger),
        shortcut,
        KirinOptions(directory.toFile(), File(""), "", "", "", "", "", ""),
        TestDispatcherProvider(dispatcher),
        logger,
    )

    @Test
    fun `opens the Kirin shortcut for the installed game directory`() = runTest(dispatcher) {
        gameDirectory.mkdirs()

        createSut().launch(game) shouldBe GameLaunchResult.Launched

        verify(exactly = 1) { shortcut.open(gameDirectory) }
    }

    @Test
    fun `reports a missing game directory without opening Kirin`() = runTest(dispatcher) {
        createSut().launch(game) shouldBe GameLaunchResult.GameNotFound

        verify(exactly = 0) { shortcut.open(any()) }
    }

    @Test
    fun `reports a failed launch when Kirin cannot be opened`() = runTest(dispatcher) {
        gameDirectory.mkdirs()
        every { shortcut.open(any()) } throws IllegalStateException("No activity")

        createSut().launch(game) shouldBe GameLaunchResult.LaunchFailed
    }
}
