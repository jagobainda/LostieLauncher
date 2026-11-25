package dev.jagoba.lostielauncher.util.file

import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class FileDigestResult(val size: Long, val sha256: String)

internal object FileDigests {
    val SHA256_FORMAT = Regex("[0-9a-fA-F]{64}")

    fun newSha256(): MessageDigest = MessageDigest.getInstance("SHA-256")

    fun toHex(digest: MessageDigest): String =
        digest.digest().joinToString("") { String.format(Locale.ROOT, "%02x", it) }

    suspend fun sha256(file: File, bufferSizeBytes: Int): FileDigestResult {
        val digest = newSha256()
        var size = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(bufferSizeBytes)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                size += read
            }
        }
        return FileDigestResult(size, toHex(digest))
    }
}
