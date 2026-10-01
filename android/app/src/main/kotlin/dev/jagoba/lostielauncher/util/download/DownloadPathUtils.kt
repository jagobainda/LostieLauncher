package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.GameDownloadArgs
import java.security.MessageDigest

object DownloadPathUtils {
    private const val PART_EXTENSION = ".part"
    private const val META_EXTENSION = ".meta"

    private const val TOKEN_BYTES = 8

    private const val HEX_DIGITS = "0123456789abcdef"
    private const val NIBBLE_BITS = 4
    private const val LOW_NIBBLE = 0x0F
    private const val BYTE_MASK = 0xFF

    fun getZipFileName(args: GameDownloadArgs): String = "${args.gameId}.${computeToken(args.version, args.key)}.zip"

    fun getPartFilePath(finalPath: String): String = finalPath + PART_EXTENSION

    fun getMetaFilePath(partPath: String): String = partPath + META_EXTENSION

    fun computeToken(version: String, key: String?): String {
        val raw = "$version|${key.orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))

        return buildString(TOKEN_BYTES * 2) {
            for (index in 0 until TOKEN_BYTES) {
                val byte = digest[index].toInt() and BYTE_MASK
                append(HEX_DIGITS[byte ushr NIBBLE_BITS])
                append(HEX_DIGITS[byte and LOW_NIBBLE])
            }
        }
    }
}
