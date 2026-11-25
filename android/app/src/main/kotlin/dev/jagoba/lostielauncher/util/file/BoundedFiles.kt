package dev.jagoba.lostielauncher.util.file

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

internal object BoundedFiles {
    fun readText(file: File, maxBytes: Long): String {
        require(maxBytes >= 0L) { "maxBytes must not be negative." }
        if (file.length() > maxBytes) {
            throw IOException("File '${file.name}' exceeds the $maxBytes-byte limit.")
        }
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        file.inputStream().use { input ->
            while (true) {
                val remaining = maxBytes - total
                val toRead = if (remaining < buffer.size) remaining.toInt() + 1 else buffer.size
                val read = input.read(buffer, 0, toRead)
                if (read < 0) break
                total += read
                if (total > maxBytes) {
                    throw IOException("File '${file.name}' exceeds the $maxBytes-byte limit.")
                }
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray().toString(Charsets.UTF_8)
    }
}
