package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.GameDownloadArgs
import java.security.MessageDigest

object DownloadPathUtils {
    internal const val ZIP_EXTENSION = ".zip"
    internal const val PART_EXTENSION = ".part"
    internal const val META_EXTENSION = ".meta"

    private const val TOKEN_BYTES = 8

    fun getZipFileName(args: GameDownloadArgs): String =
        "${args.gameId}.${computeToken(args.version, args.key)}$ZIP_EXTENSION"

    fun getPartFilePath(finalPath: String): String = finalPath + PART_EXTENSION

    fun getMetaFilePath(partPath: String): String = partPath + META_EXTENSION

    fun computeToken(version: String, key: String?): String {
        val raw = "$version|${key.orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.toHexString(endIndex = TOKEN_BYTES)
    }
}
