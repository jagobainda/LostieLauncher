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
import dev.jagoba.lostielauncher.model.InstalledGameFileDigest
import dev.jagoba.lostielauncher.model.InstalledGameManifest
import dev.jagoba.lostielauncher.model.InstalledGameManifestLookup
import dev.jagoba.lostielauncher.model.InstalledGameManifestOptions
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.service.download.MonotonicTimeSource
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.service.storage.StorageAccess
import dev.jagoba.lostielauncher.util.file.FileDigests
import dev.jagoba.lostielauncher.util.file.LEGACY_ZIP_CHARSET
import dev.jagoba.lostielauncher.util.file.unicodePathField
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ArchiveGameInstallationService")
class ArchiveGameInstallationServiceTest {
    @TempDir
    lateinit var directory: Path

    private val dispatcher = StandardTestDispatcher()
    private val logger = mockk<Logger>(relaxed = true)
    private val library = mockk<LocalLibraryStore>(relaxed = true)
    private val manifests = mockk<InstalledGameManifestStore>(relaxed = true)
    private val kirin = mockk<KirinGameLibrary>(relaxed = true)
    private val storageAccess = mockk<StorageAccess> { every { isGranted() } returns true }
    private val gameId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val installedGame = LocalGame(gameId, "Demo", "v1", null)
    private val gamesRoot get() = directory.resolve("LostieLauncher").toFile()
    private val gameDirectory get() = File(gamesRoot, "demo")
    private val manifestsDirectory get() = directory.resolve("no_backup/manifests").toFile()
    private val archive get() = directory.resolve("downloads/demo.zip").toFile()
    private val noMediaMarker get() = File(gamesRoot, ".nomedia")

    private fun createSut(
        bufferSizeBytes: Int = 64 * 1024,
        entryCostBytes: Long = 0,
        extractionWorkers: Int = 4,
        progressInterval: Duration = Duration.ZERO,
        timeSource: MonotonicTimeSource = MonotonicTimeSource { 0L },
        manifestStore: InstalledGameManifestStore = manifests,
        maxEntries: Int = KirinOptions.DEFAULT_ARCHIVE_MAX_ENTRIES,
        maxUncompressedBytes: Long = KirinOptions.DEFAULT_ARCHIVE_MAX_UNCOMPRESSED_BYTES,
        maxCompressionRatio: Int = KirinOptions.DEFAULT_ARCHIVE_MAX_COMPRESSION_RATIO,
        compressionRatioThresholdBytes: Long = KirinOptions.DEFAULT_ARCHIVE_COMPRESSION_RATIO_THRESHOLD_BYTES,
    ) = ArchiveGameInstallationService(
        PendingGameInstallationService(logger),
        library,
        manifestStore,
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
            archiveBufferSizeBytes = bufferSizeBytes,
            archiveExtractionWorkers = extractionWorkers,
            archiveEntryCostBytes = entryCostBytes,
            archiveProgressInterval = progressInterval,
            archiveMaxEntries = maxEntries,
            archiveMaxUncompressedBytes = maxUncompressedBytes,
            archiveMaxCompressionRatio = maxCompressionRatio,
            archiveCompressionRatioThresholdBytes = compressionRatioThresholdBytes,
        ),
        storageAccess,
        timeSource,
        TestDispatcherProvider(dispatcher),
        logger,
    )

    private fun writeArchive(
        vararg entries: Pair<String, String>,
        charset: Charset = Charsets.UTF_8,
        unicodePaths: Map<String, String> = emptyMap(),
    ): String {
        archive.parentFile?.mkdirs()
        ZipOutputStream(archive.outputStream(), charset).use { zip ->
            for ((name, content) in entries) {
                val entry = ZipEntry(name)
                unicodePaths[name]?.let { entry.extra = unicodePathField(name, it.toByteArray()) }
                zip.putNextEntry(entry)
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
    fun `samples extraction progress at the configured interval while keeping initial zero and final hundred`() =
        runTest(dispatcher) {
            val entries = (1..8).map { "file-$it.bin" to "" }.toTypedArray()
            val hash = writeArchive(*entries)
            var nowMillis = -100L
            val sut = createSut(
                entryCostBytes = 1,
                extractionWorkers = 1,
                progressInterval = 500.milliseconds,
                timeSource = MonotonicTimeSource {
                    nowMillis += 100L
                    nowMillis.milliseconds.inWholeNanoseconds
                },
            )

            sut.observeInstallation("demo").test {
                awaitItem() shouldBe GameInstallationState.NotStarted
                sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)
                awaitItem() shouldBe GameInstallationState.VerifyingIntegrity
                awaitItem() shouldBe GameInstallationState.Extracting()
                awaitItem() shouldBe GameInstallationState.Extracting(0)
                awaitItem() shouldBe GameInstallationState.Extracting(62)
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

    @ParameterizedTest
    @CsvSource(
        "IBM850, Trovão Distorsión.mp3,",
        "UTF-8,  Trovão Distorsión.mp3,",
        "IBM850, Trov_o Distorsi_n.mp3, Audio/BGM/Trovão Distorsión.mp3",
    )
    fun `extracts an accented name from the legacy code page, the UTF-8 flag or the Unicode Path field`(
        charset: String,
        headerName: String,
        unicodePath: String?,
    ) = runTest(dispatcher) {
        val entryName = "Audio/BGM/$headerName"
        val hash = writeArchive(
            entryName to "bgm",
            charset = Charset.forName(charset),
            unicodePaths = unicodePath?.let { mapOf(entryName to it) }.orEmpty(),
        )
        val sut = createSut()

        sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)

        File(gameDirectory, "Audio/BGM").list().orEmpty().toList() shouldBe listOf("Trovão Distorsión.mp3")
    }

    @Test
    fun `rejects a Unicode Path field that escapes the game directory`() = runTest(dispatcher) {
        val hash = writeArchive(
            "Data/safe.txt" to "evil",
            charset = LEGACY_ZIP_CHARSET,
            unicodePaths = mapOf("Data/safe.txt" to "../escaped.txt"),
        )
        val sut = createSut()

        sut.install(request(hash)) shouldBe GameInstallationResult.ExtractionFailed

        File(gamesRoot, "escaped.txt").exists() shouldBe false
        gameDirectory.exists() shouldBe false
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
    fun `reports missing shared storage access without extracting and keeps the archive`() = runTest(dispatcher) {
        val hash = writeArchive("Game.exe" to "exe")
        every { storageAccess.isGranted() } returns false
        val sut = createSut()

        sut.install(request(hash)) shouldBe GameInstallationResult.StorageAccessDenied

        gameDirectory.exists() shouldBe false
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
    fun `cancels integrity verification promptly without deleting the archive or modifying an existing installation`() =
        runTest(dispatcher) {
            File(gameDirectory, "Game.exe").apply { parentFile?.mkdirs() }.writeText("old")
            writeArchive("Game.exe" to "new")
            var installJob: Job? = null
            every { storageAccess.isGranted() } answers {
                installJob?.cancel()
                true
            }
            val sut = createSut()

            val deferred = async { sut.install(request("a".repeat(64))) }.also { installJob = it }
            val error = runCatching { deferred.await() }.exceptionOrNull()

            (error is CancellationException) shouldBe true
            archive.exists() shouldBe true
            File(gameDirectory, "Game.exe").readText() shouldBe "old"
            sut.observeInstallation("demo").first() shouldBe GameInstallationState.VerifyingIntegrity
            coVerify(exactly = 0) { kirin.register(any(), any()) }
            coVerify(exactly = 0) { library.registerGame(any()) }
        }

    @Test
    fun `cancels extraction inside a large entry without finishing the copy and cleans up the temp directory`() =
        runTest(dispatcher) {
            File(gameDirectory, "Game.exe").apply { parentFile?.mkdirs() }.writeText("old")
            val hash = writeArchive("Data/large.bin" to "x".repeat(512 * 1024))
            var installJob: Job? = null
            var progressChecks = 0
            val sut = createSut(
                bufferSizeBytes = 8 * 1024,
                extractionWorkers = 1,
                timeSource = MonotonicTimeSource {
                    progressChecks++
                    if (progressChecks == 2) installJob?.cancel()
                    0L
                },
            )

            val deferred = async { sut.install(request(hash)) }.also { installJob = it }
            val error = runCatching { deferred.await() }.exceptionOrNull()

            (error is CancellationException) shouldBe true
            progressChecks shouldBe 2
            File("${gameDirectory.path}.tmp").exists() shouldBe false
            File(gameDirectory, "Game.exe").readText() shouldBe "old"
            archive.exists() shouldBe true
            coVerify(exactly = 0) { kirin.register(any(), any()) }
            coVerify(exactly = 0) { library.registerGame(any()) }
        }

    @Test
    fun `cancels before swapping directories when cancelled at the end of extraction`() = runTest(dispatcher) {
        File(gameDirectory, "Game.exe").apply { parentFile?.mkdirs() }.writeText("old")
        val hash = writeArchive("Game.exe" to "new")
        var installJob: Job? = null
        var progressChecks = 0
        val sut = createSut(
            extractionWorkers = 1,
            timeSource = MonotonicTimeSource {
                progressChecks++
                if (progressChecks == 2) installJob?.cancel()
                0L
            },
        )

        val deferred = async { sut.install(request(hash)) }.also { installJob = it }
        val error = runCatching { deferred.await() }.exceptionOrNull()

        (error is CancellationException) shouldBe true
        File("${gameDirectory.path}.tmp").exists() shouldBe false
        File(gameDirectory, "Game.exe").readText() shouldBe "old"
        archive.exists() shouldBe true
        coVerify(exactly = 0) { kirin.register(any(), any()) }
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
        coVerify(exactly = 0) { manifests.remove(any()) }
        coVerify(exactly = 0) { library.removeGame(any()) }
    }

    @Test
    fun `writes an internal integrity manifest with file digests during installation and removes it on uninstall`() =
        runTest(dispatcher) {
            val store = FileInstalledGameManifestStore(
                InstalledGameManifestOptions(manifestsDirectory),
                Json { ignoreUnknownKeys = true },
                logger,
                TestDispatcherProvider(dispatcher),
            )
            val hash = writeArchive("Game.exe" to "exe", "Data/Scripts.rxdata" to "data")
            coEvery { library.getGames() } returns listOf(installedGame)
            val sut = createSut(manifestStore = store)

            sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)

            val expectedExe = FileDigests.sha256(File(gameDirectory, "Game.exe"), 4 * 1024)
            val expectedData = FileDigests.sha256(File(gameDirectory, "Data/Scripts.rxdata"), 4 * 1024)
            store.read("demo") shouldBe InstalledGameManifestLookup.Found(
                InstalledGameManifest(
                    gameId = "demo",
                    id = gameId,
                    name = "Demo",
                    version = "v1",
                    variant = null,
                    files = mapOf(
                        "Data/Scripts.rxdata" to InstalledGameFileDigest(expectedData.size, expectedData.sha256),
                        "Game.exe" to InstalledGameFileDigest(expectedExe.size, expectedExe.sha256),
                    ),
                ),
            )

            coEvery { library.getGames() } returns emptyList()
            sut.uninstall(GameTarget(gameId, "Demo")) shouldBe GameUninstallResult(GameUninstallOutcome.COMPLETED)
            store.read("demo") shouldBe InstalledGameManifestLookup.NotFound
        }

    @Test
    fun `reports a registry failure without registering locally when writing the manifest fails`() =
        runTest(dispatcher) {
            val hash = writeArchive("Game.exe" to "exe")
            coEvery { manifests.write(any()) } throws IOException("disk full")
            val sut = createSut()

            sut.install(request(hash)) shouldBe GameInstallationResult.RegistryFailed

            coVerify(exactly = 0) { library.registerGame(any()) }
        }

    @Test
    fun `prefers internal manifest metadata over tampered version and variant in the shared registry`() =
        runTest(dispatcher) {
            val legacy = LocalGame(UUID.randomUUID(), "Legacy", "v1.0", null)
            coEvery { library.getGames() } returns listOf(LocalGame(gameId, "Demo", "999", "fake"), legacy)
            coEvery { manifests.readAll() } returns listOf(
                InstalledGameManifest("demo", gameId, "Demo", "v1", null, emptyMap()),
            )
            val sut = createSut()
            advanceUntilIdle()

            sut.installedGames.value shouldBe InstalledGamesState.Available(listOf(installedGame, legacy))
        }

    @Test
    fun `rejects an archive with too many entries without replacing an existing installation`() = runTest(dispatcher) {
        File(gameDirectory, "Game.exe").apply { parentFile?.mkdirs() }.writeText("old")
        val hash = writeArchive("a.txt" to "1", "b.txt" to "2", "c.txt" to "3")
        val sut = createSut(maxEntries = 2)

        sut.install(request(hash)) shouldBe GameInstallationResult.ExtractionFailed

        File(gameDirectory, "Game.exe").readText() shouldBe "old"
        File("${gameDirectory.path}.tmp").exists() shouldBe false
        coVerify(exactly = 0) { kirin.register(any(), any()) }
    }

    @Test
    fun `rejects an archive whose uncompressed size exceeds the limit`() = runTest(dispatcher) {
        File(gameDirectory, "Game.exe").apply { parentFile?.mkdirs() }.writeText("old")
        val hash = writeArchive("Data/big.bin" to "x".repeat(256))
        val sut = createSut(maxUncompressedBytes = 128)

        sut.install(request(hash)) shouldBe GameInstallationResult.ExtractionFailed

        File(gameDirectory, "Game.exe").readText() shouldBe "old"
        File("${gameDirectory.path}.tmp").exists() shouldBe false
        coVerify(exactly = 0) { kirin.register(any(), any()) }
    }

    @Test
    fun `rejects an archive whose actual extracted bytes exceed the limit even with a spoofed entry size`() =
        runTest(dispatcher) {
            writeArchive("Data/big.bin" to "x".repeat(256))
            val bytes = archive.readBytes()
            val centralDirOffset = findCentralDirectorySignature(bytes)
            for (offset in 24..27) bytes[centralDirOffset + offset] = 0
            archive.writeBytes(bytes)
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            val sut = createSut(bufferSizeBytes = 64, extractionWorkers = 1, maxUncompressedBytes = 128)

            sut.install(request(hash)) shouldBe GameInstallationResult.ExtractionFailed

            gameDirectory.exists() shouldBe false
            File("${gameDirectory.path}.tmp").exists() shouldBe false
            coVerify(exactly = 0) { kirin.register(any(), any()) }
        }

    @Test
    fun `rejects a high compression ratio archive once uncompressed size exceeds the threshold`() =
        runTest(dispatcher) {
            val hash = writeArchive("Data/zeros.bin" to "0".repeat(256 * 1024))
            val sut = createSut(
                maxCompressionRatio = 20,
                compressionRatioThresholdBytes = 64L * 1024,
            )

            sut.install(request(hash)) shouldBe GameInstallationResult.ExtractionFailed

            gameDirectory.exists() shouldBe false
            File("${gameDirectory.path}.tmp").exists() shouldBe false
            coVerify(exactly = 0) { kirin.register(any(), any()) }
        }

    @Test
    fun `allows a highly compressible archive below the compression ratio threshold`() = runTest(dispatcher) {
        val hash = writeArchive("Data/zeros.bin" to "0".repeat(32 * 1024))
        val sut = createSut(
            maxCompressionRatio = 20,
            compressionRatioThresholdBytes = 64L * 1024,
        )

        sut.install(request(hash)) shouldBe GameInstallationResult.Installed(installedGame)

        File(gameDirectory, "Data/zeros.bin").length() shouldBe 32L * 1024
    }

    private fun findCentralDirectorySignature(bytes: ByteArray): Int {
        for (i in 0..bytes.size - 4) {
            if (bytes[i] == 0x50.toByte() &&
                bytes[i + 1] == 0x4b.toByte() &&
                bytes[i + 2] == 0x01.toByte() &&
                bytes[i + 3] == 0x02.toByte()
            ) {
                return i
            }
        }
        error("Central directory header not found")
    }
}
