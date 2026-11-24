package dev.jagoba.lostielauncher.service.download

import android.annotation.SuppressLint
import dev.jagoba.lostielauncher.model.DownloadCacheEntry
import dev.jagoba.lostielauncher.model.DownloadDestination
import dev.jagoba.lostielauncher.model.DownloadTransferOptions
import dev.jagoba.lostielauncher.model.GameDownloadArgs
import dev.jagoba.lostielauncher.service.storage.StorageLocations
import dev.jagoba.lostielauncher.util.download.DownloadCachePolicy
import dev.jagoba.lostielauncher.util.download.DownloadPathUtils
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

internal interface DownloadFileStore {
    fun destinationFor(args: GameDownloadArgs): File

    fun isManagedPath(destinationPath: String): Boolean

    fun relocatePath(destinationPath: String): String

    fun deleteArtifacts(destinationPath: String): Int

    fun purgeStale(
        knownGameIds: Set<String>,
        now: Instant,
        protectedDestinationPaths: Set<String> = emptySet(),
        retainedArchivePaths: Set<String> = emptySet(),
    ): Int

    fun hasArtifacts(destinationPath: String): Boolean

    fun hasArchive(destinationPath: String): Boolean

    fun hasResumablePartial(destinationPath: String): Boolean

    fun destination(): DownloadDestination
}

@Singleton
internal class DefaultDownloadFileStore @Inject constructor(
    storageLocations: StorageLocations,
    private val options: DownloadTransferOptions,
) : DownloadFileStore {
    private val downloadsRoot = storageLocations.downloadsRoot
    private val downloadsDirectory = File(downloadsRoot, options.cacheDirectoryName)
    private val downloadsDirectoryPath = downloadsDirectory.toPath().toAbsolutePath().normalize()
    private val legacyDownloadsRoots = storageLocations.legacyDownloadsRoots

    override fun destinationFor(args: GameDownloadArgs): File =
        File(downloadsDirectory, DownloadPathUtils.getZipFileName(args))

    override fun isManagedPath(destinationPath: String): Boolean = runCatching {
        File(destinationPath).toPath().toAbsolutePath().normalize().parent == downloadsDirectoryPath
    }.getOrDefault(false)

    override fun relocatePath(destinationPath: String): String =
        File(downloadsDirectory, File(destinationPath).name).absolutePath

    @SuppressLint("UsableSpace")
    override fun destination(): DownloadDestination {
        val freeBytes = runCatching {
            generateSequence(downloadsDirectory, File::getParentFile).firstOrNull(File::exists)?.usableSpace
        }.getOrNull()?.takeIf { it > 0 }
        return DownloadDestination(downloadsDirectory.absolutePath, freeBytes)
    }

    override fun deleteArtifacts(destinationPath: String): Int =
        artifactsOf(destinationPath).count { it.exists() && it.delete() }

    override fun purgeStale(
        knownGameIds: Set<String>,
        now: Instant,
        protectedDestinationPaths: Set<String>,
        retainedArchivePaths: Set<String>,
    ): Int {
        val protectedNames = protectedDestinationPaths
            .flatMap(::artifactsOf)
            .mapTo(mutableSetOf()) { it.name.lowercase(Locale.ROOT) }
        val retainedArchives = retainedArchivePaths
            .mapTo(mutableSetOf()) { File(it).name.lowercase(Locale.ROOT) }
        val files = downloadsDirectory.listFiles()?.filter(File::isFile).orEmpty()
        val entries = files.map { file ->
            DownloadCacheEntry(file.name, Instant.ofEpochMilli(file.lastModified()))
        }
        val staleNames = DownloadCachePolicy.selectStaleFiles(entries, knownGameIds, now, options.cacheMaxAge).toSet()
        val purgedCurrent = entries.count { entry ->
            if (entry.fileName !in staleNames) return@count false
            val lower = entry.fileName.lowercase(Locale.ROOT)
            val keptByTransfer = lower in protectedNames
            val keptByRecentArchive = lower in retainedArchives &&
                Duration.between(entry.lastWriteTime, now) <= options.cacheMaxAge
            !keptByTransfer && !keptByRecentArchive && File(downloadsDirectory, entry.fileName).delete()
        }
        return purgedCurrent + purgeLegacyCaches(now)
    }

    private fun purgeLegacyCaches(now: Instant): Int {
        val currentRootPath = downloadsRoot.toPath().toAbsolutePath().normalize()
        var deleted = 0
        for (legacyRoot in legacyDownloadsRoots) {
            if (legacyRoot.toPath().toAbsolutePath().normalize() == currentRootPath) continue
            val legacyCache = File(legacyRoot, options.cacheDirectoryName)
            val files = legacyCache.listFiles()?.filter(File::isFile).orEmpty()
            val entries = files.map { file ->
                DownloadCacheEntry(file.name, Instant.ofEpochMilli(file.lastModified()))
            }
            val managedNames = DownloadCachePolicy.selectStaleFiles(entries, emptySet(), now, Duration.ZERO).toSet()
            deleted += entries.count { entry ->
                entry.fileName in managedNames && File(legacyCache, entry.fileName).delete()
            }
            legacyCache.delete()
            legacyRoot.delete()
        }
        return deleted
    }

    override fun hasArtifacts(destinationPath: String): Boolean = artifactsOf(destinationPath).any(File::exists)

    override fun hasArchive(destinationPath: String): Boolean = File(destinationPath).isFile

    override fun hasResumablePartial(destinationPath: String): Boolean {
        val (_, part, metadata) = artifactsOf(destinationPath)
        return part.exists() && metadata.exists()
    }

    private fun artifactsOf(destinationPath: String): List<File> {
        val partPath = DownloadPathUtils.getPartFilePath(destinationPath)
        return listOf(File(destinationPath), File(partPath), File(DownloadPathUtils.getMetaFilePath(partPath)))
    }
}
