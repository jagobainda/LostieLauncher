package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.GameInstallationRequest
import dev.jagoba.lostielauncher.model.GameInstallationResult
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.model.GameUninstallOutcome
import dev.jagoba.lostielauncher.model.GameUninstallResult
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.MissingGameId
import dev.jagoba.lostielauncher.model.gameSlug
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
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

    private val background = CoroutineScope(dispatchers.io + SupervisorJob())

    init {
        background.launch {
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

        setPhase(file.gameId, GameInstallationState.Extracting())
        val directory = gameDirectory(file.gameId)
            ?: throw IOException("Invalid game directory: '${file.gameId}' escapes the games root.")
        extract(archive, directory, file.gameId)
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

    override suspend fun uninstall(game: GameTarget): GameUninstallResult = withContext(dispatchers.io) {
        val directory = gameDirectory(gameSlug(game.name))
        if (directory == null) {
            logger.error("Uninstall refused for ${game.name}: its directory escapes the games root.")
            return@withContext GameUninstallResult(GameUninstallOutcome.NOTHING_DELETED)
        }
        val existed = directory.exists()
        var deleted = 0
        var blocking: File? = null
        if (existed) {
            for (entry in directory.walkBottomUp()) {
                if (entry.delete()) {
                    deleted++
                } else if (blocking == null) {
                    blocking = entry
                }
            }
        }
        val location = blocking?.let { GameLocationReference(it.path, it.path) }
        if (location != null && deleted == 0) {
            logger.error("Uninstall deleted nothing of ${game.name}. Blocked at: ${location.token}.")
            return@withContext GameUninstallResult(GameUninstallOutcome.NOTHING_DELETED, location)
        }
        try {
            kirin.unregister(directory)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Kirin unregistration failed for ${game.name}.", error)
        }
        library.removeGame(game.name)
        reloadInstalled()
        when {
            location != null -> {
                logger.error("Uninstall could not delete every file of ${game.name}. Blocked at: ${location.token}.")
                GameUninstallResult(GameUninstallOutcome.FILES_LEFT_BEHIND, location)
            }

            existed -> GameUninstallResult(GameUninstallOutcome.COMPLETED)

            else -> GameUninstallResult(GameUninstallOutcome.FILES_NOT_FOUND)
        }
    }

    private fun gameDirectory(gameId: String): File? =
        File(options.gamesRoot, gameId).takeIf { it.canonicalFile.parentFile == options.gamesRoot.canonicalFile }

    private suspend fun extract(archive: File, directory: File, gameId: String) {
        val temp = File("${directory.path}.tmp")
        temp.deleteRecursively()
        if (!temp.mkdirs()) throw IOException("Could not create '$temp'.")
        hideGamesFromMediaScanner()
        try {
            ZipFile(archive, Charsets.UTF_8).use { zip -> extractEntries(zip, temp, gameId) }
            swap(temp, directory)
        } catch (error: Exception) {
            temp.deleteRecursively()
            throw error
        }
        deleteInBackground(previousInstallations(directory))
    }

    private suspend fun extractEntries(zip: ZipFile, temp: File, gameId: String) {
        val root = temp.toPath().toAbsolutePath().normalize()
        val files = linkedMapOf<File, ZipEntry>()
        val directories = mutableSetOf<Path>()
        for (entry in zip.entries()) {
            val target = root.resolve(entry.name.replace('\\', '/')).normalize()
            if (!target.startsWith(root) || (target == root && !entry.isDirectory)) {
                throw IOException("Zip Slip attempt detected in entry: ${entry.name}")
            }
            if (entry.isDirectory) {
                directories.addWithAncestors(target, root)
            } else {
                files[target.toFile()] = entry
                target.parent?.let { directories.addWithAncestors(it, root) }
            }
        }
        for (directory in directories.sorted()) {
            if (!directory.toFile().mkdir()) throw IOException("Could not create '$directory'.")
        }

        val entryCost = options.archiveEntryCostBytes
        val progress = ExtractionProgress(files.values.sumOf { it.size.coerceAtLeast(0) + entryCost }) {
            setPhase(gameId, GameInstallationState.Extracting(it))
        }
        progress.advance(0)
        val queue = files.entries.sortedByDescending { it.value.size }
        val next = AtomicInteger()
        coroutineScope {
            repeat(options.archiveExtractionWorkers.coerceAtLeast(1)) {
                launch {
                    val buffer = ByteArray(options.archiveBufferSizeBytes)
                    while (true) {
                        ensureActive()
                        val (target, entry) = queue.getOrNull(next.getAndIncrement()) ?: break
                        copyEntry(zip, entry, target, buffer, progress)
                        progress.advance(entryCost)
                    }
                }
            }
        }
        logger.debug("Temp extraction complete: ${files.size} files.")
    }

    private fun copyEntry(
        zip: ZipFile,
        entry: ZipEntry,
        target: File,
        buffer: ByteArray,
        progress: ExtractionProgress,
    ) {
        zip.getInputStream(entry).use { input ->
            target.outputStream().use { output ->
                var filled = 0
                while (true) {
                    val read = input.read(buffer, filled, buffer.size - filled)
                    if (read < 0) break
                    filled += read
                    if (filled == buffer.size) {
                        output.write(buffer, 0, filled)
                        progress.advance(filled.toLong())
                        filled = 0
                    }
                }
                if (filled > 0) {
                    output.write(buffer, 0, filled)
                    progress.advance(filled.toLong())
                }
            }
        }
    }

    private fun MutableSet<Path>.addWithAncestors(directory: Path, root: Path) {
        var current: Path? = directory
        while (current != null && current != root && add(current)) current = current.parent
    }

    private fun swap(temp: File, directory: File) {
        val backup = File("${directory.path}$PREVIOUS_INSTALLATION_SUFFIX${UUID.randomUUID()}")
        val hadExisting = directory.exists()
        if (hadExisting && !directory.renameTo(backup)) throw IOException("Could not move '$directory' aside.")
        if (!temp.renameTo(directory)) {
            if (hadExisting) backup.renameTo(directory)
            throw IOException("Could not move '$temp' into place.")
        }
    }

    private fun previousInstallations(directory: File): List<File> = directory.parentFile
        ?.listFiles { file -> file.name.startsWith("${directory.name}$PREVIOUS_INSTALLATION_SUFFIX") }
        .orEmpty()
        .toList()

    private fun deleteInBackground(directories: List<File>) {
        if (directories.isEmpty()) return
        background.launch {
            for (directory in directories) {
                if (!directory.deleteRecursively() && directory.exists()) {
                    logger.error("Previous installation could not be deleted: ${directory.path}.")
                }
            }
        }
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
            val buffer = ByteArray(options.archiveBufferSizeBytes)
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
        const val PREVIOUS_INSTALLATION_SUFFIX = ".old-"
    }

    private class ExtractionProgress(private val total: Long, private val report: (Int) -> Unit) {
        private val done = AtomicLong()

        @Volatile
        private var lastPercent = -1

        fun advance(units: Long) {
            if (total <= 0) return
            val percent = (done.addAndGet(units) * FULL_PERCENT / total).toInt().coerceIn(0, FULL_PERCENT)
            if (percent <= lastPercent) return
            synchronized(this) {
                if (percent > lastPercent) {
                    lastPercent = percent
                    report(percent)
                }
            }
        }

        private companion object {
            const val FULL_PERCENT = 100
        }
    }
}
