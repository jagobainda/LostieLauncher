package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadResumeMetadata

internal object DownloadResumePolicy {
    fun validator(metadata: DownloadResumeMetadata?): String? {
        val etag = metadata?.etag
        if (etag != null && isStrongEtag(etag)) return etag
        return metadata?.lastModified?.takeIf(String::isNotBlank)
    }

    fun responseAction(statusCode: Int, existingBytes: Long, recordedTotalBytes: Long?): DownloadResponseAction =
        when (statusCode) {
            200 -> if (existingBytes > 0) DownloadResponseAction.RESTART else DownloadResponseAction.WRITE

            206 -> DownloadResponseAction.APPEND

            416 -> if (existingBytes > 0 && existingBytes == recordedTotalBytes) {
                DownloadResponseAction.COMPLETE
            } else {
                DownloadResponseAction.INVALIDATE
            }

            else -> DownloadResponseAction.REJECT
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
