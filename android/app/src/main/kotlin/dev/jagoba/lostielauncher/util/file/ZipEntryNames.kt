package dev.jagoba.lostielauncher.util.file

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.util.zip.CRC32
import java.util.zip.ZipEntry

internal object ZipEntryNames {
    val LEGACY_CHARSET: Charset by lazy { Charset.forName("IBM850") }

    private const val FIELD_HEADER_SIZE = 4
    private const val FIELD_SIZE_OFFSET = 2
    private const val UNSIGNED_SHORT_MASK = 0xFFFF

    private const val UNICODE_PATH_ID: Short = 0x7075
    private const val UNICODE_PATH_VERSION: Byte = 1
    private const val UNICODE_PATH_CHECKSUM_OFFSET = 1
    private const val UNICODE_PATH_NAME_OFFSET = 5

    fun resolve(entry: ZipEntry): String = unicodePath(entry) ?: entry.name

    private fun unicodePath(entry: ZipEntry): String? {
        val extra = entry.extra ?: return null
        val fields = ByteBuffer.wrap(extra).order(ByteOrder.LITTLE_ENDIAN)
        var field = 0
        while (field + FIELD_HEADER_SIZE <= extra.size) {
            val data = field + FIELD_HEADER_SIZE
            val end = data + (fields.getShort(field + FIELD_SIZE_OFFSET).toInt() and UNSIGNED_SHORT_MASK)
            if (end > extra.size) return null
            if (fields.getShort(field) == UNICODE_PATH_ID) return decodeUnicodePath(fields, data, end, entry.name)
            field = end
        }
        return null
    }

    private fun decodeUnicodePath(fields: ByteBuffer, start: Int, end: Int, headerName: String): String? {
        val name = start + UNICODE_PATH_NAME_OFFSET
        if (name >= end || fields.get(start) != UNICODE_PATH_VERSION) return null
        val headerChecksum = CRC32().apply { update(headerName.toByteArray(LEGACY_CHARSET)) }.value.toInt()
        if (fields.getInt(start + UNICODE_PATH_CHECKSUM_OFFSET) != headerChecksum) return null
        return try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(fields.array(), name, end - name)).toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }
}
