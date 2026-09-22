package dev.jagoba.lostielauncher.model

data class DownloadRequest(val displayName: String, val args: GameDownloadArgs)

data class DownloadSnapshot(
    val gameId: String,
    val displayName: String,
    val version: String,
    val status: DownloadStatus,
    val percent: Double,
    val bytesPerSecond: Double,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val errorMessage: String?,
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    PERMISSION_DENIED,
    CANCELLED,
}

enum class DownloadCommandResult {
    ACCEPTED,
    BUSY,
    NOT_FOUND,
    INVALID_STATE,
}

data class DownloadedFile(
    val gameId: String,
    val displayName: String,
    val version: String,
    val key: String?,
    val path: String,
)

data class DownloadProgress(
    val percent: Double,
    val bytesPerSecond: Double,
    val downloadedBytes: Long,
    val totalBytes: Long?,
)

data class DownloadResumeMetadata(val etag: String?, val lastModified: String?, val totalBytes: Long?)

sealed interface DownloadTransferResult {
    data object Success : DownloadTransferResult

    data object PermissionDenied : DownloadTransferResult

    data class Failed(val message: String) : DownloadTransferResult
}
