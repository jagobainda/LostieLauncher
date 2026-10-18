package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("ArchiveGameInstallationService")
class ArchiveGameInstallationServiceTest {
    @TempDir
    lateinit var directory: Path

    private val dispatcher = StandardTestDispatcher()
    private val logger = mockk<Logger>(relaxed = true)
    private val library = mockk<LocalLibraryStore>(relaxed = true)
    private val kirin = mockk<KirinGameLibrary>(relaxed = true)
    private val gameId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val installedGame = LocalGame(gameId, "Demo", "v1", null)
    private val gamesRoot get() = directory.resolve("LostieLauncher").toFile()
    private val gameDirectory get() = File(gamesRoot, "demo")
    private val archive get() = directory.resolve("downloads/demo.zip").toFile()

    private fun createSut() = ArchiveGameInstallationService(
        PendingGameInstallationService(logger),
        library,
        kirin,
        KirinOptions(
            gamesRoot,
            directory.resolve("Kirin/game-library.json").toFile(),
            "Game.exe",
            "auto",
            "",
            "",
            "",
            "",
        ),
        TestDispatcherProvider(dispatcher),
        logger,
    )

    private fun writeArchive(vararg entries: Pair<String, String>): String {
        archive.parentFile?.mkdirs()
        ZipOutputStream(archive.outputStream()).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(archive.readBytes()).joinToString("") { "%02x".format(it) }
    }

    private fun request(hash: String?) = GameInstallationRequest(
        DownloadedFile("demo", "Demo", "v1", null, archive.path),
        gameId,
        hash,
        null,
    )

    @Test
    fun `extracts a verified archive, registers it in Kirin and the local library, and deletes the archive`() =
        runTest(dispatcher) {
            val hash = writeArchive("Game.exe" to "exe", "Data/Scripts.rxdata" to "data")
            coEvery { library.getGames() } returns listOf(installedGame)
            val sut = createSut()

            sut.install(request(hash.uppercase())) shouldBe GameInstallationResult.Installed(installedGame)

            File(gameDirectory, "Game.exe").readText() shouldBe "exe"
            File(gameDirectory, "Data/Scripts.rxdata").readText() shouldBe "data"
            archive.exists() shouldBe false
            File("${gameDirectory.path}.tmp").exists() shouldBe false
            coVerify(exactly = 1) { kirin.register("Demo", gameDirectory) }
            coVerify(exactly = 1) { library.registerGame(installedGame) }
            sut.installedGames.value shouldBe InstalledGamesState.Available(listOf(installedGame))
            sut.observeInstallation("demo").first() shouldBe
                GameInstallationState.Finished(GameInstallationResult.Installed(installedGame))
        }

    @Test
    fun `replaces the files of a previous installation`() = runTest(dispatcher) {
        File(gameDirectory, "Obsolete.txt").apply { parentFile?.mkdirs() }.writeText("old")
        val hash = writeArchive("Game.exe" to "new")
        val sut = createSut()

        sut.install(request(hash))

        File(gameDirectory, "Game.exe").readText() shouldBe "new"
        File(gameDirectory, "Obsolete.txt").exists() shouldBe false
        File("${gameDirectory.path}.old").exists() shouldBe false
    }

    @Test
    fun `reports a missing archive`() = runTest(dispatcher) {
        val sut = createSut()

        sut.install(request("a".repeat(64))) shouldBe GameInstallationResult.MissingArchive
    }

    @Test
    fun `refuses an archive without a well-formed hash and deletes it`() = runTest(dispatcher) {
        writeArchive("Game.exe" to "exe")
        val sut = createSut()

        sut.install(request(null)) shouldBe GameInstallationResult.InvalidHash

        archive.exists() shouldBe false
        gameDirectory.exists() shouldBe false
    }

    @Test
    fun `refuses an archive whose hash does not match and deletes it`() = runTest(dispatcher) {
        writeArchive("Game.exe" to "exe")
        val sut = createSut()

        sut.install(request("a".repeat(64))) shouldBe GameInstallationResult.HashMismatch

        archive.exists() shouldBe false
        gameDirectory.exists() shouldBe false
    }

    @Test
    fun `rejects an entry that escapes the game directory and keeps the archive`() = runTest(dispatcher) {
        val hash = writeArchive("Game.exe" to "exe", "../escaped.txt" to "evil")
        val sut = createSut()

        sut.install(request(hash)) shouldBe GameInstallationResult.ExtractionFailed

        File(gamesRoot, "escaped.txt").exists() shouldBe false
        gameDirectory.exists() shouldBe false
        File("${gameDirectory.path}.tmp").exists() shouldBe false
        archive.exists() shouldBe true
        coVerify(exactly = 0) { kirin.register(any(), any()) }
    }

    @Test
    fun `reports a registry failure without registering the game locally when Kirin cannot be updated`() =
        runTest(dispatcher) {
            val hash = writeArchive("Game.exe" to "exe")
            coEvery { kirin.register(any(), any()) } throws IOException("denied")
            val sut = createSut()

            sut.install(request(hash)) shouldBe GameInstallationResult.RegistryFailed

            coVerify(exactly = 0) { library.registerGame(any()) }
        }
}
