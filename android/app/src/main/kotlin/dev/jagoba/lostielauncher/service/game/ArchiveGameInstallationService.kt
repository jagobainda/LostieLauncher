package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.MissingGameId
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
internal class ArchiveGameInstallationService @Inject constructor(
    pending: PendingGameInstallationService,
    private val library: LocalLibraryStore,
    private val kirin: KirinGameLibrary,
    private val options: KirinOptions,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : GameInstallationService by pending {
    private val installed = MutableStateFlow<InstalledGamesState>(InstalledGamesState.NotSupportedYet)
    private val phases = MutableStateFlow<Map<String, GameInstallationState>>(emptyMap())

    override val installedGames: StateFlow<InstalledGamesState> = installed.asStateFlow()

    init {
        CoroutineScope(dispatchers.io + SupervisorJob()).launch {
            hideGamesFromMediaScanner()
            reloadInstalled()
        }
    }

    override fun observeInstallation(gameId: String): Flow<GameInstallationState> =
        phases.map { it[gameId] ?: GameInstallationState.NotStarted }.distinctUntilChanged()

    override suspend fun install(request: GameInstallationRequest): GameInstallationResult {
        val gameId = request.file.gameId
        val result = try {
            withContext(dispatchers.io) { installArchive(request) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Game installation failed for $gameId.", error)
            GameInstallationResult.ExtractionFailed
        }
        setPhase(gameId, GameInstallationState.Finished(result))
        return result
    }

    private suspend fun installArchive(request: GameInstallationRequest): GameInstallationResult {
        val file = request.file
        val archive = File(file.path)
        if (!archive.isFile) {
            logger.error("Downloaded archive not found for ${file.gameId}: ${file.path}.")
            return GameInstallationResult.MissingArchive
        }

        setPhase(file.gameId, GameInstallationState.VerifyingIntegrity)
        val expectedHash = request.expectedSha256
        if (expectedHash == null || !SHA256_FORMAT.matches(expectedHash)) {
            logger.error("Integrity verification refused for ${file.gameId}: no valid SHA-256 available.")
            deleteArchive(archive)
            return GameInstallationResult.InvalidHash
        }
        if (!sha256(archive).equals(expectedHash, ignoreCase = true)) {
            logger.error("Hash mismatch for ${file.gameId}. Expected: $expectedHash")
            deleteArchive(archive)
            return GameInstallationResult.HashMismatch
        }

        setPhase(file.gameId, GameInstallationState.Extracting)
        val directory = File(options.gamesRoot, file.gameId)
        extract(archive, directory)
        deleteArchive(archive)

        val game = LocalGame(request.catalogueId ?: MissingGameId, file.displayName, file.version, request.variant)
        try {
            kirin.register(game.name, directory)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Kirin registration failed for ${file.gameId}.", error)
            return GameInstallationResult.RegistryFailed
        }
        library.registerGame(game)
        reloadInstalled()
        logger.info("Game installed: ${file.gameId} ${file.version}${request.variant?.let { " ($it)" }.orEmpty()}.")
        return GameInstallationResult.Installed(game)
    }

    private fun extract(archive: File, directory: File) {
        if (directory.canonicalFile.parentFile != options.gamesRoot.canonicalFile) {
            throw IOException("Invalid game directory: '$directory' escapes the games root.")
        }
        val temp = File("${directory.path}.tmp")
        val backup = File("${directory.path}.old")
        temp.deleteRecursively()
        backup.deleteRecursively()
        if (!temp.mkdirs()) throw IOException("Could not create '$temp'.")
        hideGamesFromMediaScanner()
        try {
            val root = temp.canonicalPath + File.separator
            var entryCount = 0
            ZipFile(archive, Charsets.UTF_8).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    val target = File(temp, entry.name.replace('\\', '/')).canonicalFile
                    if (!target.path.startsWith(root)) {
                        throw IOException("Zip Slip attempt detected in entry: ${entry.name}")
                    }
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input -> target.outputStream().use(input::copyTo) }
                    entryCount++
                }
            }
            logger.debug("Temp extraction complete: $entryCount files.")
            swap(temp, backup, directory)
        } catch (error: Exception) {
            temp.deleteRecursively()
            throw error
        }
    }

    private fun swap(temp: File, backup: File, directory: File) {
        val hadExisting = directory.exists()
        if (hadExisting && !directory.renameTo(backup)) throw IOException("Could not move '$directory' aside.")
        if (!temp.renameTo(directory)) {
            if (hadExisting) backup.renameTo(directory)
            throw IOException("Could not move '$temp' into place.")
        }
        if (hadExisting) backup.deleteRecursively()
    }

    private fun hideGamesFromMediaScanner() {
        if (!options.gamesRoot.isDirectory) return
        val marker = File(options.gamesRoot, NO_MEDIA_FILE_NAME)
        try {
            marker.createNewFile()
        } catch (error: IOException) {
            logger.error("Could not create '$marker'; game images may appear in the gallery.", error)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { String.format(Locale.ROOT, "%02x", it) }
    }

    private fun deleteArchive(archive: File) {
        if (!archive.delete()) logger.error("Downloaded archive could not be deleted: ${archive.path}.")
    }

    private suspend fun reloadInstalled() {
        installed.value = InstalledGamesState.Available(library.getGames())
    }

    private fun setPhase(gameId: String, state: GameInstallationState) {
        phases.update { it + (gameId to state) }
    }

    private companion object {
        val SHA256_FORMAT = Regex("[0-9a-fA-F]{64}")
        const val NO_MEDIA_FILE_NAME = ".nomedia"
    }
}
