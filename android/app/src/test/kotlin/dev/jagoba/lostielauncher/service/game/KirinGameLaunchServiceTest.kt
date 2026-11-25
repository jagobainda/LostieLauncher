package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.GameLaunchResult
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.InstalledGameFileDigest
import dev.jagoba.lostielauncher.model.InstalledGameManifest
import dev.jagoba.lostielauncher.model.InstalledGameManifestLookup
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.util.file.FileDigests
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.nio.file.Path
import java.util.UUID
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
    private val kirinSetup = mockk<KirinSetupService> {
        coEvery { isOfficialKirinInstalled() } returns true
    }
    private val manifests = mockk<InstalledGameManifestStore> {
        coEvery { read(any()) } returns InstalledGameManifestLookup.NotFound
    }
    private val game = GameTarget(null, "Pokémon Añil")
    private val gameDirectory get() = directory.resolve("pok-mon-a-il").toFile()

    private fun createSut() = KirinGameLaunchService(
        PendingGameLaunchService(logger),
        shortcut,
        kirinSetup,
        manifests,
        KirinOptions(directory.toFile(), File(""), "", "", "", "", "", ""),
        TestDispatcherProvider(dispatcher),
        logger,
    )

    @Test
    fun `opens the Kirin shortcut for a legacy game directory without a manifest`() = runTest(dispatcher) {
        gameDirectory.mkdirs()

        createSut().launch(game) shouldBe GameLaunchResult.Launched

        verify(exactly = 1) { shortcut.open(gameDirectory) }
    }

    @Test
    fun `opens the Kirin shortcut when every manifest file matches and ignores extra runtime files`() =
        runTest(dispatcher) {
            val exe = File(gameDirectory, "Game.exe").apply {
                parentFile?.mkdirs()
                writeText("original")
            }
            val digest = FileDigests.sha256(exe, 4 * 1024)
            File(gameDirectory, "Save/Game.rxdata").apply {
                parentFile?.mkdirs()
                writeText("save-data")
            }
            coEvery { manifests.read("pok-mon-a-il") } returns InstalledGameManifestLookup.Found(
                InstalledGameManifest(
                    gameId = "pok-mon-a-il",
                    id = UUID.randomUUID(),
                    name = game.name,
                    version = "v1.0.0",
                    variant = null,
                    files = mapOf("Game.exe" to InstalledGameFileDigest(digest.size, digest.sha256)),
                ),
            )

            createSut().launch(game) shouldBe GameLaunchResult.Launched

            verify(exactly = 1) { shortcut.open(gameDirectory) }
        }

    @Test
    fun `refuses to launch a game when a file recorded in the manifest was modified`() = runTest(dispatcher) {
        val exe = File(gameDirectory, "Game.exe").apply {
            parentFile?.mkdirs()
            writeText("original")
        }
        val digest = FileDigests.sha256(exe, 4 * 1024)
        exe.writeText("tampered")
        coEvery { manifests.read("pok-mon-a-il") } returns InstalledGameManifestLookup.Found(
            InstalledGameManifest(
                gameId = "pok-mon-a-il",
                id = UUID.randomUUID(),
                name = game.name,
                version = "v1.0.0",
                variant = null,
                files = mapOf("Game.exe" to InstalledGameFileDigest(digest.size, digest.sha256)),
            ),
        )

        createSut().launch(game) shouldBe GameLaunchResult.IntegrityMismatch

        verify(exactly = 0) { shortcut.open(any()) }
    }

    @Test
    fun `refuses to launch a game when its internal manifest is unreadable`() = runTest(dispatcher) {
        gameDirectory.mkdirs()
        coEvery { manifests.read("pok-mon-a-il") } returns InstalledGameManifestLookup.Corrupted

        createSut().launch(game) shouldBe GameLaunchResult.IntegrityMismatch

        verify(exactly = 0) { shortcut.open(any()) }
    }

    @Test
    fun `reports a missing game directory without opening Kirin`() = runTest(dispatcher) {
        createSut().launch(game) shouldBe GameLaunchResult.GameNotFound

        verify(exactly = 0) { shortcut.open(any()) }
    }

    @Test
    fun `refuses to hand off a game if the installed signer has changed`() = runTest(dispatcher) {
        gameDirectory.mkdirs()
        coEvery { kirinSetup.isOfficialKirinInstalled() } returns false

        createSut().launch(game) shouldBe GameLaunchResult.LaunchFailed

        verify(exactly = 0) { shortcut.open(any()) }
    }

    @Test
    fun `reports a failed launch when Kirin cannot be opened`() = runTest(dispatcher) {
        gameDirectory.mkdirs()
        every { shortcut.open(any()) } throws IllegalStateException("No activity")

        createSut().launch(game) shouldBe GameLaunchResult.LaunchFailed
    }
}
