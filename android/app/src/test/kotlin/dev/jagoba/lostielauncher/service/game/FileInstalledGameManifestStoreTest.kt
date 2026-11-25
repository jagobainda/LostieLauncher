package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.InstalledGameFileDigest
import dev.jagoba.lostielauncher.model.InstalledGameManifest
import dev.jagoba.lostielauncher.model.InstalledGameManifestLookup
import dev.jagoba.lostielauncher.model.InstalledGameManifestOptions
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.UUID
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("FileInstalledGameManifestStore")
class FileInstalledGameManifestStoreTest {
    @TempDir
    lateinit var directory: Path

    private val logger = mockk<Logger>(relaxed = true)
    private val manifestsDir get() = directory.resolve("manifests").toFile()
    private val gameId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val sampleDigest = "a".repeat(64)

    @Test
    fun `writes and reads a game integrity manifest with normalized lowercase hashes`() = runTest {
        val sut = createSut()
        val manifest = InstalledGameManifest(
            gameId = "demo",
            id = gameId,
            name = "Demo",
            version = "v1.0.0",
            variant = "special",
            files = mapOf(
                "Data/Scripts.rxdata" to InstalledGameFileDigest(12L, sampleDigest.uppercase()),
                "Game.exe" to InstalledGameFileDigest(4L, sampleDigest),
            ),
        )

        sut.write(manifest)

        sut.read("demo") shouldBe InstalledGameManifestLookup.Found(
            manifest.copy(
                files = mapOf(
                    "Data/Scripts.rxdata" to InstalledGameFileDigest(12L, sampleDigest),
                    "Game.exe" to InstalledGameFileDigest(4L, sampleDigest),
                ),
            ),
        )
    }

    @Test
    fun `returns not found when no manifest exists for a game`() = runTest {
        val sut = createSut()

        sut.read("demo") shouldBe InstalledGameManifestLookup.NotFound
        sut.readAll().shouldBeEmpty()
    }

    @Test
    fun `removes an existing manifest file`() = runTest {
        val sut = createSut()
        sut.write(manifest("demo"))

        sut.remove("demo")

        sut.read("demo") shouldBe InstalledGameManifestLookup.NotFound
        File(manifestsDir, "demo.json").exists() shouldBe false
    }

    @Test
    fun `reads all valid manifests and skips corrupted or temporary files`() = runTest {
        val sut = createSut()
        val alpha = manifest("alpha")
        val beta = manifest("beta")
        sut.write(beta)
        sut.write(alpha)
        File(manifestsDir, "broken.json").writeText("{not json")
        File(manifestsDir, "alpha.json.tmp").writeText("partial")

        sut.readAll() shouldBe listOf(alpha, beta)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "not json",
            """{"gameId":"other","id":"11111111-2222-3333-4444-555555555555","name":"Demo","version":"v1","files":{}}""",
            """{"gameId":"demo","id":"not-a-uuid","name":"Demo","version":"v1","files":{}}""",
            """{"gameId":"demo","id":"11111111-2222-3333-4444-555555555555","name":"Demo","version":"v1","files":{"Game.exe":{"size":-1,"sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}}}""",
            """{"gameId":"demo","id":"11111111-2222-3333-4444-555555555555","name":"Demo","version":"v1","files":{"Game.exe":{"size":1,"sha256":"short"}}}""",
            """{"gameId":"demo","id":"11111111-2222-3333-4444-555555555555","name":"Demo","version":"v1","files":{"../escape.txt":{"size":1,"sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}}}""",
        ],
    )
    fun `reports a corrupted manifest when its file is malformed or fails validation`(content: String) = runTest {
        manifestsDir.mkdirs()
        File(manifestsDir, "demo.json").writeText(content)
        val sut = createSut()

        sut.read("demo") shouldBe InstalledGameManifestLookup.Corrupted
    }

    @Test
    fun `refuses to write a manifest with an escaping game id`() = runTest {
        val sut = createSut()

        shouldThrow<IOException> { sut.write(manifest("../escape")) }
    }

    private fun TestScope.createSut() = FileInstalledGameManifestStore(
        options = InstalledGameManifestOptions(manifestsDir),
        json = Json { ignoreUnknownKeys = true },
        logger = logger,
        dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )

    private fun manifest(gameId: String) = InstalledGameManifest(
        gameId = gameId,
        id = this.gameId,
        name = gameId.replaceFirstChar(Char::uppercaseChar),
        version = "v1.0.0",
        variant = null,
        files = mapOf("Game.exe" to InstalledGameFileDigest(3L, sampleDigest)),
    )
}
