package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.InstalledGameManifest
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.MissingGameId
import dev.jagoba.lostielauncher.model.gameSlug
import dev.jagoba.lostielauncher.util.file.FileDigests
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal sealed interface InstalledGameIntegrityResult {
    data object Valid : InstalledGameIntegrityResult

    data class Mismatch(val reason: String) : InstalledGameIntegrityResult
}

internal object InstalledGameIntegrity {
    suspend fun verify(
        directory: File,
        manifest: InstalledGameManifest,
        bufferSizeBytes: Int,
    ): InstalledGameIntegrityResult {
        val root = directory.toPath().toAbsolutePath().normalize()
        val canonicalRoot = try {
            directory.canonicalFile.toPath()
        } catch (error: IOException) {
            return InstalledGameIntegrityResult.Mismatch("could not resolve game directory: ${error.message}")
        }
        for ((relativePath, expected) in manifest.files) {
            currentCoroutineContext().ensureActive()
            val target = root.resolve(relativePath).normalize()
            if (!target.startsWith(root) || target == root) {
                return InstalledGameIntegrityResult.Mismatch("invalid manifest entry '$relativePath'")
            }
            val file = target.toFile()
            if (!file.isFile) {
                return InstalledGameIntegrityResult.Mismatch("missing file '$relativePath'")
            }
            val canonicalFile = try {
                file.canonicalFile.toPath()
            } catch (error: IOException) {
                return InstalledGameIntegrityResult.Mismatch("could not resolve '$relativePath': ${error.message}")
            }
            if (!canonicalFile.startsWith(canonicalRoot)) {
                return InstalledGameIntegrityResult.Mismatch("file '$relativePath' escapes game directory")
            }
            if (file.length() != expected.size) {
                return InstalledGameIntegrityResult.Mismatch("size mismatch for '$relativePath'")
            }
            val actual = try {
                FileDigests.sha256(file, bufferSizeBytes)
            } catch (error: IOException) {
                return InstalledGameIntegrityResult.Mismatch("could not read '$relativePath': ${error.message}")
            }
            if (actual.size != expected.size || !actual.sha256.equals(expected.sha256, ignoreCase = true)) {
                return InstalledGameIntegrityResult.Mismatch("digest mismatch for '$relativePath'")
            }
        }
        return InstalledGameIntegrityResult.Valid
    }

    fun reconcileInstalledGames(
        registryGames: List<LocalGame>,
        manifests: List<InstalledGameManifest>,
        directoryExists: (String) -> Boolean,
        onMismatch: (manifest: InstalledGameManifest, registryEntry: LocalGame) -> Unit = { _, _ -> },
    ): List<LocalGame> {
        val bySlug = manifests.associateBy { it.gameId }
        val byId = manifests.filter { it.id != MissingGameId }.associateBy { it.id }
        val matchedManifestIds = mutableSetOf<String>()
        val result = mutableListOf<LocalGame>()
        val seenIds = mutableSetOf<UUID>()
        val seenKeys = mutableSetOf<String>()

        fun addUnique(game: LocalGame) {
            val key = gameSlug(game.name).ifEmpty { game.name.lowercase(Locale.ROOT) }
            val idUnique = game.id == MissingGameId || seenIds.add(game.id)
            val keyUnique = seenKeys.add(key)
            if (idUnique && keyUnique) result += game
        }

        for (entry in registryGames) {
            val manifest = bySlug[gameSlug(entry.name)]
                ?: entry.id.takeUnless { it == MissingGameId }?.let(byId::get)
            if (manifest != null) {
                matchedManifestIds += manifest.gameId
                if (entry != manifest.game) onMismatch(manifest, entry)
                addUnique(manifest.game)
            } else {
                addUnique(entry)
            }
        }

        for (manifest in manifests) {
            if (manifest.gameId !in matchedManifestIds && directoryExists(manifest.gameId)) {
                addUnique(manifest.game)
            }
        }

        return result
    }
}
