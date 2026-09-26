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
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

internal interface DownloadFileStore {
    fun destinationFor(args: GameDownloadArgs): File

    fun deleteArtifacts(destinationPath: String): Int

    fun purgeStale(knownGameIds: Set<String>, now: Instant): Int

    fun hasArtifacts(destinationPath: String): Boolean

    fun hasResumablePartial(destinationPath: String): Boolean

    fun destination(): DownloadDestination
}

@Singleton
internal class DefaultDownloadFileStore @Inject constructor(
    storageLocations: StorageLocations,
    private val options: DownloadTransferOptions,
) : DownloadFileStore {
    private val downloadsDirectory = File(storageLocations.gamesRoot, options.cacheDirectoryName)

    override fun destinationFor(args: GameDownloadArgs): File =
        File(downloadsDirectory, DownloadPathUtils.getZipFileName(args))

    @SuppressLint("UsableSpace")
    override fun destination(): DownloadDestination {
        val freeBytes = runCatching {
            generateSequence(downloadsDirectory, File::getParentFile).firstOrNull(File::exists)?.usableSpace
        }.getOrNull()?.takeIf { it > 0 }
        return DownloadDestination(downloadsDirectory.absolutePath, freeBytes)
    }

    override fun deleteArtifacts(destinationPath: String): Int {
        val partPath = DownloadPathUtils.getPartFilePath(destinationPath)
        return listOf(
            File(destinationPath),
            File(partPath),
            File(DownloadPathUtils.getMetaFilePath(partPath)),
        ).count { it.exists() && it.delete() }
    }

    override fun purgeStale(knownGameIds: Set<String>, now: Instant): Int {
        val files = downloadsDirectory.listFiles()?.filter(File::isFile).orEmpty()
        val entries = files.map { file ->
            DownloadCacheEntry(file.name, Instant.ofEpochMilli(file.lastModified()))
        }
        val staleNames = DownloadCachePolicy.selectStaleFiles(entries, knownGameIds, now, options.cacheMaxAge)
        return staleNames.count { fileName -> File(downloadsDirectory, fileName).delete() }
    }

    override fun hasArtifacts(destinationPath: String): Boolean {
        val partPath = DownloadPathUtils.getPartFilePath(destinationPath)
        return listOf(
            File(destinationPath),
            File(partPath),
            File(DownloadPathUtils.getMetaFilePath(partPath)),
        ).any(File::exists)
    }

    override fun hasResumablePartial(destinationPath: String): Boolean {
        val partPath = DownloadPathUtils.getPartFilePath(destinationPath)
        return File(partPath).exists() && File(DownloadPathUtils.getMetaFilePath(partPath)).exists()
    }
}
