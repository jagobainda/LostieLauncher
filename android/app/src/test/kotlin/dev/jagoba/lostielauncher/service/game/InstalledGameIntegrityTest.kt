package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.InstalledGameFileDigest
import dev.jagoba.lostielauncher.model.InstalledGameManifest
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.MissingGameId
import dev.jagoba.lostielauncher.util.file.FileDigests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("InstalledGameIntegrity")
class InstalledGameIntegrityTest {
    @TempDir
    lateinit var directory: Path

    private val gameDir get() = directory.resolve("demo").toFile()
    private val gameId = UUID.fromString("11111111-2222-3333-4444-555555555555")

    @Test
    fun `accepts a game directory whose manifest files match and ignores extra runtime files`() = runTest {
        writeFile("Game.exe", "exe-content")
        writeFile("Data/Scripts.rxdata", "scripts")
        val manifest = createManifest("Game.exe", "Data/Scripts.rxdata")
        writeFile("Save/Game.rxdata", "player-save")

        InstalledGameIntegrity.verify(gameDir, manifest, BUFFER_SIZE) shouldBe InstalledGameIntegrityResult.Valid
    }

    @Test
    fun `rejects a modified file even when its length stays the same`() = runTest {
        writeFile("Game.exe", "abcd")
        val manifest = createManifest("Game.exe")
        writeFile("Game.exe", "abce")

        val result = InstalledGameIntegrity.verify(gameDir, manifest, BUFFER_SIZE)

        result.shouldBeInstanceOf<InstalledGameIntegrityResult.Mismatch>()
    }

    @Test
    fun `rejects a file whose length changed`() = runTest {
        writeFile("Data/asset.txt", "original")
        val manifest = createManifest("Data/asset.txt")
        writeFile("Data/asset.txt", "tampered-longer")

        val result = InstalledGameIntegrity.verify(gameDir, manifest, BUFFER_SIZE)

        result.shouldBeInstanceOf<InstalledGameIntegrityResult.Mismatch>()
    }

    @Test
    fun `rejects a missing manifest file`() = runTest {
        writeFile("Game.exe", "exe")
        val manifest = createManifest("Game.exe")
        File(gameDir, "Game.exe").delete()

        val result = InstalledGameIntegrity.verify(gameDir, manifest, BUFFER_SIZE)

        result.shouldBeInstanceOf<InstalledGameIntegrityResult.Mismatch>()
    }

    @Test
    fun `overrides tampered version and variant in the shared registry using the internal manifest`() {
        val manifest = InstalledGameManifest("demo", gameId, "Demo", "v1.0.0", null, emptyMap())
        val tampered = LocalGame(gameId, "Demo", "999", "fake")
        var mismatches = 0

        val reconciled = InstalledGameIntegrity.reconcileInstalledGames(
            registryGames = listOf(tampered),
            manifests = listOf(manifest),
            directoryExists = { true },
            onMismatch = { _, _ -> mismatches++ },
        )

        reconciled shouldBe listOf(LocalGame(gameId, "Demo", "v1.0.0", null))
        mismatches shouldBe 1
    }

    @Test
    fun `matches a renamed shared registry entry by catalogue id and deduplicates duplicates`() {
        val manifest = InstalledGameManifest("demo", gameId, "Demo", "v1.0.0", null, emptyMap())
        val renamed = LocalGame(gameId, "Forged Name", "999", "fake")
        val duplicate = LocalGame(MissingGameId, "Demo", "999", null)

        val reconciled = InstalledGameIntegrity.reconcileInstalledGames(
            registryGames = listOf(renamed, duplicate),
            manifests = listOf(manifest),
            directoryExists = { true },
        )

        reconciled shouldBe listOf(LocalGame(gameId, "Demo", "v1.0.0", null))
    }

    @Test
    fun `keeps legacy games without a manifest and restores manifest-backed games missing from the registry`() {
        val legacy = LocalGame(UUID.randomUUID(), "Legacy Game", "v1.0.0", null)
        val present = InstalledGameManifest("demo", gameId, "Demo", "v1.0.0", null, emptyMap())
        val deleted = InstalledGameManifest("gone", UUID.randomUUID(), "Gone", "v1.0.0", null, emptyMap())

        val reconciled = InstalledGameIntegrity.reconcileInstalledGames(
            registryGames = listOf(legacy),
            manifests = listOf(present, deleted),
            directoryExists = { it == "demo" },
        )

        reconciled shouldBe listOf(legacy, present.game)
    }

    private fun writeFile(relativePath: String, content: String): File = File(gameDir, relativePath).apply {
        parentFile?.mkdirs()
        writeText(content)
    }

    private suspend fun createManifest(vararg paths: String): InstalledGameManifest {
        val digests = paths.associateWith { path ->
            val digest = FileDigests.sha256(File(gameDir, path), BUFFER_SIZE)
            InstalledGameFileDigest(digest.size, digest.sha256)
        }
        return InstalledGameManifest("demo", gameId, "Demo", "v1.0.0", null, digests)
    }

    private companion object {
        const val BUFFER_SIZE = 4 * 1024
    }
}
