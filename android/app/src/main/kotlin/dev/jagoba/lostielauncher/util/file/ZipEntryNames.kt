package dev.jagoba.lostielauncher.util.file

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.util.zip.CRC32
import java.util.zip.ZipEntry

internal object ZipEntryNames {
    /**
     * Charset to open the archive with. `ZipFile` applies it only to names stored without the UTF-8 flag.
     * The game archives are packed on Western European Windows, whose OEM code page is 850 rather than
     * the ZIP default 437. Android does not guarantee this charset, so it is resolved on first use: a device
     * without it fails that installation instead of the initialization of this object.
     */
    val LEGACY_CHARSET: Charset by lazy { Charset.forName("IBM850") }

    private const val FIELD_HEADER_SIZE = 4
    private const val FIELD_SIZE_OFFSET = 2
    private const val UNSIGNED_SHORT_MASK = 0xFFFF

    private const val UNICODE_PATH_ID: Short = 0x7075
    private const val UNICODE_PATH_VERSION: Byte = 1
    private const val UNICODE_PATH_CHECKSUM_OFFSET = 1
    private const val UNICODE_PATH_NAME_OFFSET = 5

    /**
     * Name of an [entry] read with [LEGACY_CHARSET]. The Info-ZIP Unicode Path field wins over the header
     * name, as on desktop. Unlike desktop, the field is ignored when its checksum shows that the header name
     * changed after the field was written, as the ZIP specification asks.
     */
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
