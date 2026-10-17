package dev.jagoba.lostielauncher.service.download

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.jagoba.lostielauncher.model.DownloadSnapshot
import dev.jagoba.lostielauncher.model.DownloadStatus

@Entity(tableName = "downloads")
internal data class DownloadEntity(
    @PrimaryKey
    @ColumnInfo(name = "game_id")
    val gameId: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val version: String,
    val key: String?,
    val url: String,
    @ColumnInfo(name = "destination_path")
    val destinationPath: String,
    @ColumnInfo(name = "work_id")
    val workId: String,
    val status: String,
    val percent: Double,
    @ColumnInfo(name = "bytes_per_second")
    val bytesPerSecond: Double,
    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long,
    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long?,
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
) {
    fun toSnapshot(): DownloadSnapshot = DownloadSnapshot(
        gameId = gameId,
        displayName = displayName,
        version = version,
        status = status.toDownloadStatus(),
        percent = percent,
        bytesPerSecond = bytesPerSecond,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        errorMessage = errorMessage,
    )
}

internal fun String.toDownloadStatus(): DownloadStatus =
    DownloadStatus.entries.firstOrNull { it.name == this } ?: DownloadStatus.FAILED
