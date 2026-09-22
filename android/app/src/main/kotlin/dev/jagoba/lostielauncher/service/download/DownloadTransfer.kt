package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadTransferResult

internal data class DownloadTransferRequest(val url: String, val destinationPath: String)

internal interface DownloadTransfer {
    suspend fun download(
        request: DownloadTransferRequest,
        onProgress: suspend (DownloadProgress) -> Unit,
    ): DownloadTransferResult
}

internal fun interface MonotonicTimeSource {
    fun readNanos(): Long
}
