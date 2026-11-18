package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadResumeMetadata

internal object DownloadResumePolicy {
    fun validator(metadata: DownloadResumeMetadata?): String? {
        val etag = metadata?.etag
        if (etag != null && isStrongEtag(etag)) return etag
        return metadata?.lastModified?.takeIf(String::isNotBlank)
    }

    fun responseAction(
        statusCode: Int,
        existingBytes: Long,
        recordedTotalBytes: Long?,
        contentRange: String?,
    ): DownloadResponseAction = when (statusCode) {
        200 -> if (existingBytes > 0) DownloadResponseAction.RESTART else DownloadResponseAction.WRITE

        206 -> if (existingBytes > 0 && matchesResumeRange(contentRange, existingBytes, recordedTotalBytes)) {
            DownloadResponseAction.APPEND
        } else {
            DownloadResponseAction.INVALIDATE
        }

        416 -> if (existingBytes > 0 && existingBytes == recordedTotalBytes) {
            DownloadResponseAction.COMPLETE
        } else {
            DownloadResponseAction.INVALIDATE
        }

        else -> DownloadResponseAction.REJECT
    }

    private val byteRange = Regex("""bytes ([0-9]+)-([0-9]+)/(\*|[0-9]+)""", RegexOption.IGNORE_CASE)

    private fun matchesResumeRange(header: String?, offset: Long, recordedTotal: Long?): Boolean {
        val parts = header?.let(byteRange::matchEntire)?.groupValues ?: return false
        val end = parts[2].toLongOrNull() ?: return false
        val total = if (parts[3] == "*") recordedTotal else parts[3].toLongOrNull() ?: return false
        return parts[1].toLongOrNull() == offset && end >= offset && (total == null || end == total - 1)
    }

    private fun isStrongEtag(value: String): Boolean = value.length >= 2 &&
        value.first() == '"' &&
        value.last() == '"' &&
        !value.startsWith("W/", ignoreCase = true)
}

internal enum class DownloadResponseAction {
    WRITE,
    APPEND,
    RESTART,
    COMPLETE,
    INVALIDATE,
    REJECT,
}
