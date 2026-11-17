package dev.jagoba.lostielauncher.util.file

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.zip.CRC32

internal val LEGACY_ZIP_CHARSET: Charset = Charset.forName("IBM850")

internal fun unicodePathField(
    headerName: String,
    unicodeName: ByteArray,
    version: Byte = 1,
    checksum: Int = CRC32().apply { update(headerName.toByteArray(LEGACY_ZIP_CHARSET)) }.value.toInt(),
): ByteArray {
    val size = UNICODE_PATH_PREFIX_SIZE + unicodeName.size
    return ByteBuffer.allocate(EXTRA_FIELD_HEADER_SIZE + size)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putShort(UNICODE_PATH_ID)
        .putShort(size.toShort())
        .put(version)
        .putInt(checksum)
        .put(unicodeName)
        .array()
}

private const val UNICODE_PATH_ID: Short = 0x7075
private const val EXTRA_FIELD_HEADER_SIZE = 4
private const val UNICODE_PATH_PREFIX_SIZE = 5
