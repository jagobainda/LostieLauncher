package dev.jagoba.lostielauncher.service.game

import app.cash.turbine.test
import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadedFile
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.GameUninstallResult
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val noMediaMarker get() = File(gamesRoot, ".nomedia")

    private fun createSut(entryCostBytes: Long = 0) = ArchiveGameInstallationService(
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
            archiveBufferSizeBytes = 64 * 1024,
            archiveExtractionWorkers = 4,
            archiveEntryCostBytes = entryCostBytes,
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
    fun `reports byte progress within a large entry and finishes without going back to indeterminate`() =
        runTest(dispatcher) {
            val first = "a".repeat(1024 * 1024)
            val second = "b".repeat(3 * 1024 * 1024)
            val hash = writeArchive("Data/first.bin" to first, "Data/second.bin" to second)
            val sut = createSut()

            sut.observeInstallation("demo").test {
                awaitItem() shouldBe GameInstallationState.NotStarted
                sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)
                awaitItem() shouldBe GameInstallationState.VerifyingIntegrity
                awaitItem() shouldBe GameInstallationState.Extracting()
                awaitItem() shouldBe GameInstallationState.Extracting(0)
                var previous = 0
                var updates = 0
                while (previous < 100) {
                    val phase = awaitItem() as GameInstallationState.Extracting
                    val percent = requireNotNull(phase.percent)
                    (percent > previous) shouldBe true
                    if (updates == 0) (percent < 25) shouldBe true
                    previous = percent
                    updates++
                }
                (updates <= 100) shouldBe true
                awaitItem() shouldBe GameInstallationState.Finished(GameInstallationResult.Installed(installedGame))
            }
            File(gameDirectory, "Data/first.bin").readText() shouldBe first
            File(gameDirectory, "Data/second.bin").readText() shouldBe second
        }

    @Test
    fun `keeps extraction indeterminate for an archive containing only empty files`() = runTest(dispatcher) {
        val hash = writeArchive("Data/empty.bin" to "")
        val sut = createSut()

        sut.observeInstallation("demo").test {
            awaitItem() shouldBe GameInstallationState.NotStarted
            sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)
            awaitItem() shouldBe GameInstallationState.VerifyingIntegrity
            awaitItem() shouldBe GameInstallationState.Extracting()
            awaitItem() shouldBe GameInstallationState.Finished(GameInstallationResult.Installed(installedGame))
        }
        File(gameDirectory, "Data/empty.bin").length() shouldBe 0L
    }

    @Test
    fun `rejects an unsafe archive before extracting any file and keeps an existing installation`() =
        runTest(dispatcher) {
            File(gameDirectory, "Game.exe").apply { parentFile?.mkdirs() }.writeText("old")
            val hash = writeArchive("Game.exe" to "new", "../escaped.txt" to "bad")
            val sut = createSut()

            sut.observeInstallation("demo").test {
                awaitItem() shouldBe GameInstallationState.NotStarted
                sut.install(request(hash)) shouldBe GameInstallationResult.ExtractionFailed
                awaitItem() shouldBe GameInstallationState.VerifyingIntegrity
                awaitItem() shouldBe GameInstallationState.Extracting()
                awaitItem() shouldBe GameInstallationState.Finished(GameInstallationResult.ExtractionFailed)
            }
            File(gameDirectory, "Game.exe").readText() shouldBe "old"
            File(gameDirectory, "escaped.txt").exists() shouldBe false
            File("${gameDirectory.path}.tmp").exists() shouldBe false
        }

    @Test
    fun `counts a fixed cost per file so an archive of empty files still reports progress`() = runTest(dispatcher) {
        val hash = writeArchive("a.bin" to "", "b.bin" to "", "c.bin" to "", "d.bin" to "")
        val sut = createSut(entryCostBytes = 1)

        sut.observeInstallation("demo").test {
            awaitItem() shouldBe GameInstallationState.NotStarted
            sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)
            awaitItem() shouldBe GameInstallationState.VerifyingIntegrity
            awaitItem() shouldBe GameInstallationState.Extracting()
            awaitItem() shouldBe GameInstallationState.Extracting(0)
            awaitItem() shouldBe GameInstallationState.Extracting(25)
            awaitItem() shouldBe GameInstallationState.Extracting(50)
            awaitItem() shouldBe GameInstallationState.Extracting(75)
            awaitItem() shouldBe GameInstallationState.Extracting(100)
            awaitItem() shouldBe GameInstallationState.Finished(GameInstallationResult.Installed(installedGame))
        }
    }

    @Test
    fun `creates every directory in the archive, including empty ones and unlisted parents`() = runTest(dispatcher) {
        val hash = writeArchive("Save/" to "", "Audio/BGM/Theme.ogg" to "ogg")
        val sut = createSut()

        sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)

        File(gameDirectory, "Save").isDirectory shouldBe true
        File(gameDirectory, "Audio/BGM/Theme.ogg").readText() shouldBe "ogg"
    }

    @Test
    fun `hides the games root from the media scanner when installing`() = runTest(dispatcher) {
        val hash = writeArchive("Game.exe" to "exe", "Graphics/Pictures/title.png" to "png")
        val sut = createSut()

        sut.install(request(hash))

        noMediaMarker.isFile shouldBe true
    }

    @Test
    fun `hides an existing games root from the media scanner on startup`() = runTest(dispatcher) {
        gamesRoot.mkdirs()

        createSut()
        advanceUntilIdle()

        noMediaMarker.isFile shouldBe true
    }

    @Test
    fun `deletes leftover previous installations on startup`() = runTest(dispatcher) {
        File(gamesRoot, "demo.old-leftover/Game.exe").apply { parentFile?.mkdirs() }.writeText("exe")
        gameDirectory.mkdirs()

        createSut()
        advanceUntilIdle()

        gamesRoot.list().orEmpty().sorted() shouldBe listOf(".nomedia", "demo")
    }

    @Test
    fun `does not create the games root on startup when nothing is installed`() = runTest(dispatcher) {
        createSut()
        advanceUntilIdle()

        gamesRoot.exists() shouldBe false
    }

    @Test
    fun `replaces the files of a previous installation`() = runTest(dispatcher) {
        File(gameDirectory, "Obsolete.txt").apply { parentFile?.mkdirs() }.writeText("old")
        val hash = writeArchive("Game.exe" to "new")
        val sut = createSut()

        sut.install(request(hash))
        advanceUntilIdle()

        File(gameDirectory, "Game.exe").readText() shouldBe "new"
        File(gameDirectory, "Obsolete.txt").exists() shouldBe false
        gamesRoot.list().orEmpty().sorted() shouldBe listOf(".nomedia", "demo")
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

    @Test
    fun `uninstall deletes the game directory and both registrations`() = runTest(dispatcher) {
        File(gameDirectory, "Data/Scripts.rxdata").apply { parentFile?.mkdirs() }.writeText("data")
        coEvery { library.getGames() } returns emptyList()
        val sut = createSut()

        sut.uninstall(GameTarget(gameId, "Demo")) shouldBe GameUninstallResult(GameUninstallOutcome.COMPLETED)
        advanceUntilIdle()

        gamesRoot.list().orEmpty().sorted() shouldBe listOf(".nomedia")
        coVerify(exactly = 1) { kirin.unregister(gameDirectory) }
        coVerify(exactly = 1) { library.removeGame("Demo") }
        sut.installedGames.value shouldBe InstalledGamesState.Available(emptyList())
    }

    @Test
    fun `uninstall of a missing directory still cleans up the registrations`() = runTest(dispatcher) {
        val sut = createSut()

        sut.uninstall(GameTarget(gameId, "Demo")) shouldBe GameUninstallResult(GameUninstallOutcome.FILES_NOT_FOUND)

        coVerify(exactly = 1) { kirin.unregister(gameDirectory) }
        coVerify(exactly = 1) { library.removeGame("Demo") }
    }

    @Test
    fun `uninstall removes the local record even when Kirin cannot be updated`() = runTest(dispatcher) {
        gameDirectory.mkdirs()
        coEvery { kirin.unregister(any()) } throws IOException("denied")
        val sut = createSut()

        sut.uninstall(GameTarget(gameId, "Demo")) shouldBe GameUninstallResult(GameUninstallOutcome.COMPLETED)

        coVerify(exactly = 1) { library.removeGame("Demo") }
    }

    @Test
    fun `uninstall refuses a name whose directory would be the games root`() = runTest(dispatcher) {
        File(gamesRoot, "other/Game.exe").apply { parentFile?.mkdirs() }.writeText("exe")
        val sut = createSut()

        sut.uninstall(GameTarget(null, "!!!")) shouldBe GameUninstallResult(GameUninstallOutcome.NOTHING_DELETED)

        File(gamesRoot, "other/Game.exe").exists() shouldBe true
        coVerify(exactly = 0) { library.removeGame(any()) }
    }
}
