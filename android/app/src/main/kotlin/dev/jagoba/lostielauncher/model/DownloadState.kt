package dev.jagoba.lostielauncher.model

data class DownloadRequest(
    val displayName: String,
    val args: GameDownloadArgs,
    val expectedSha256: String,
    val variant: String? = null,
    val requiresUnmeteredNetwork: Boolean = false,
)

data class DownloadDestination(val path: String, val freeBytes: Long?)

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
    INSTALLING,
    PAUSED,
    COMPLETED,
    FAILED,
    INSTALLATION_FAILED,
    PERMISSION_DENIED,
    CANCELLED,
    ;

    val isTransferring: Boolean
        get() = this == QUEUED || this == DOWNLOADING

    val isActive: Boolean
        get() = isTransferring || this == INSTALLING

    val isOngoing: Boolean
        get() = isActive || this == PAUSED || this == INSTALLATION_FAILED

    val isFinished: Boolean
        get() = this == COMPLETED || this == CANCELLED

    companion object {
        val TransferringNames: List<String> = entries.filter { it.isTransferring }.map { it.name }
        val ActiveNames: List<String> = entries.filter { it.isActive }.map { it.name }
        val OngoingNames: List<String> = entries.filter { it.isOngoing }.map { it.name }
        val PurgeableNames: List<String> = entries.filterNot { it.isOngoing }.map { it.name }
    }
}

enum class DownloadCommandResult {
    ACCEPTED,
    BUSY,
    NOT_FOUND,
    INVALID_STATE,
}

val List<DownloadSnapshot>.hasActiveDownload: Boolean
    get() = any { it.status.isActive }

data class DownloadedFile(
    val gameId: String,
    val displayName: String,
    val version: String,
    val key: String?,
    val path: String,
    val expectedSha256: String? = null,
    val variant: String? = null,
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
