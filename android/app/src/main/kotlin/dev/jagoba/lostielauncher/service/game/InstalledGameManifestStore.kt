package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.InstalledGameFileDigest
import dev.jagoba.lostielauncher.model.InstalledGameManifest
import dev.jagoba.lostielauncher.model.InstalledGameManifestLookup
import dev.jagoba.lostielauncher.model.InstalledGameManifestOptions
import dev.jagoba.lostielauncher.util.file.AtomicFileWriter
import dev.jagoba.lostielauncher.util.file.FileDigests
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal interface InstalledGameManifestStore {
    suspend fun read(gameId: String): InstalledGameManifestLookup

    suspend fun readAll(): List<InstalledGameManifest>

    suspend fun write(manifest: InstalledGameManifest)

    suspend fun remove(gameId: String)
}

@Singleton
internal class FileInstalledGameManifestStore @Inject constructor(
    private val options: InstalledGameManifestOptions,
    private val json: Json,
    private val logger: Logger,
    private val dispatchers: DispatcherProvider,
) : InstalledGameManifestStore {
    private val gate = Mutex()

    override suspend fun read(gameId: String): InstalledGameManifestLookup = withContext(dispatchers.io) {
        gate.withLock {
            val file = manifestFile(gameId)
            if (file == null || !file.exists()) return@withLock InstalledGameManifestLookup.NotFound
            try {
                val manifest = decode(file, expectedGameId = gameId)
                if (manifest != null) {
                    InstalledGameManifestLookup.Found(manifest)
                } else {
                    logger.error("Invalid game integrity manifest for $gameId.")
                    InstalledGameManifestLookup.Corrupted
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not read game integrity manifest for $gameId.", error)
                InstalledGameManifestLookup.Corrupted
            }
        }
    }

    override suspend fun readAll(): List<InstalledGameManifest> = withContext(dispatchers.io) {
        gate.withLock {
            if (!options.directory.isDirectory) return@withLock emptyList()
            val files = options.directory
                .listFiles { file -> file.isFile && file.name.endsWith(MANIFEST_EXTENSION) }
                .orEmpty()
                .sortedBy(File::getName)
            files.mapNotNull { file ->
                val expectedGameId = file.name.removeSuffix(MANIFEST_EXTENSION)
                try {
                    decode(file, expectedGameId).also { decoded ->
                        if (decoded == null) logger.error("Ignoring invalid game integrity manifest '${file.name}'.")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.error("Could not read game integrity manifest '${file.name}'.", error)
                    null
                }
            }
        }
    }

    override suspend fun write(manifest: InstalledGameManifest) = withContext(dispatchers.io) {
        gate.withLock {
            val file = manifestFile(manifest.gameId)
                ?: throw IOException("Invalid manifest id: '${manifest.gameId}'.")
            val payload = ManifestPayload(
                gameId = manifest.gameId,
                id = manifest.id.toString(),
                name = manifest.name,
                version = manifest.version,
                variant = manifest.variant,
                files = manifest.files.toSortedMap().mapValues { (_, entry) ->
                    ManifestFileEntry(entry.size, entry.sha256)
                },
            )
            AtomicFileWriter.writeText(file, json.encodeToString(payload))
        }
    }

    override suspend fun remove(gameId: String) = withContext(dispatchers.io) {
        gate.withLock {
            val file = manifestFile(gameId) ?: return@withLock
            if (file.exists() && !file.delete()) {
                logger.error("Could not delete game integrity manifest for $gameId.")
            }
        }
    }

    private fun manifestFile(gameId: String): File? {
        if (gameId.isBlank() || '/' in gameId || '\\' in gameId) return null
        val file = File(options.directory, "$gameId$MANIFEST_EXTENSION")
        return file.takeIf { it.canonicalFile.parentFile == options.directory.canonicalFile }
    }

    private fun decode(file: File, expectedGameId: String): InstalledGameManifest? {
        val payload = json.decodeFromString<ManifestPayload>(file.readText())
        if (payload.gameId != expectedGameId || payload.name.isBlank()) return null
        val id = runCatching { UUID.fromString(payload.id) }.getOrNull() ?: return null
        val files = buildMap(payload.files.size) {
            for ((path, entry) in payload.files) {
                if (!isValidRelativePath(path)) return null
                if (entry.size < 0L || !FileDigests.SHA256_FORMAT.matches(entry.sha256)) return null
                put(path, InstalledGameFileDigest(entry.size, entry.sha256.lowercase(Locale.ROOT)))
            }
        }
        return InstalledGameManifest(
            gameId = payload.gameId,
            id = id,
            name = payload.name,
            version = payload.version,
            variant = payload.variant,
            files = files,
        )
    }

    private fun isValidRelativePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || '\\' in path) return false
        val segments = path.split('/')
        return segments.none { it.isEmpty() || it == "." || it == ".." }
    }

    private companion object {
        const val MANIFEST_EXTENSION = ".json"
    }
}

@Serializable
private data class ManifestPayload(
    val gameId: String,
    val id: String,
    val name: String,
    val version: String,
    val variant: String? = null,
    val files: Map<String, ManifestFileEntry>,
)

@Serializable
private data class ManifestFileEntry(val size: Long, val sha256: String)
