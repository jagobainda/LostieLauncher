package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.GameDownloadArgs
import java.security.MessageDigest

/**
 * Names the files a download writes, ported from the desktop's
 * `Utils/DownloadPathUtils.cs`.
 *
 * All four are pure string derivations, and that is the point: the component
 * that writes the transfer and the one that later cleans the cache have to
 * derive the *same* names from the same inputs, or a cleanup misses the sidecar
 * it was supposed to remove. They take and return names, not handles, so they
 * hold whatever the Android storage model turns out to be — the directory is
 * the caller's, here as on the desktop.
 */
object DownloadPathUtils {
    private const val PART_EXTENSION = ".part"
    private const val META_EXTENSION = ".meta"

    /** Eight of the digest's bytes, which is the 16 hex characters of a token. */
    private const val TOKEN_BYTES = 8

    private const val HEX_DIGITS = "0123456789abcdef"
    private const val NIBBLE_BITS = 4
    private const val LOW_NIBBLE = 0x0F
    private const val BYTE_MASK = 0xFF

    /** `<gameId>.<token>.zip` — the cached archive's name. */
    fun getZipFileName(args: GameDownloadArgs): String = "${args.gameId}.${computeToken(args.version, args.key)}.zip"

    /** The partial file a resumable transfer writes into. */
    fun getPartFilePath(finalPath: String): String = finalPath + PART_EXTENSION

    /** The resume metadata beside a partial file. Takes the **part** path, not the final one. */
    fun getMetaFilePath(partPath: String): String = partPath + META_EXTENSION

    /**
     * The first eight bytes of the SHA-256 of `"<version>|<key>"`, lowercase
     * hex.
     *
     * It is what stops two downloads of the same game from colliding in the
     * cache: a new version, or the keyed special build of a version already
     * being fetched, hashes differently and therefore resumes into its own
     * partial file. An absent [key] hashes as an empty string, so the standard
     * build and a keyed one never share a token.
     */
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
