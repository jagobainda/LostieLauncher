package dev.jagoba.lostielauncher.service.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY created_at_epoch_millis, game_id")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE game_id = :gameId")
    suspend fun get(gameId: String): DownloadEntity?

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN (:statuses)")
    suspend fun countWithStatuses(statuses: List<String>): Int

    @Query("SELECT * FROM downloads WHERE status IN (:statuses)")
    suspend fun getWithStatuses(statuses: List<String>): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query(
        """
        UPDATE downloads
        SET work_id = :workId, status = :status, error_message = NULL
        WHERE game_id = :gameId AND status = :expectedStatus
        """,
    )
    suspend fun replaceWork(gameId: String, expectedStatus: String, workId: String, status: String): Int

    @Query(
        """
        UPDATE downloads
        SET status = :status,
            error_message = :errorMessage,
            bytes_per_second = 0,
            key = CASE WHEN :clearSensitiveData THEN NULL ELSE key END,
            url = CASE WHEN :clearSensitiveData THEN '' ELSE url END
        WHERE game_id = :gameId
        """,
    )
    suspend fun setStatus(
        gameId: String,
        status: String,
        errorMessage: String? = null,
        clearSensitiveData: Boolean = false,
    )

    @Query(
        """
        UPDATE downloads
        SET destination_path = :destinationPath,
            status = :status,
            percent = 0,
            bytes_per_second = 0,
            downloaded_bytes = 0,
            total_bytes = NULL,
            error_message = NULL
        WHERE game_id = :gameId AND work_id = :workId AND status IN (:expectedStatuses)
        """,
    )
    suspend fun relocate(
        gameId: String,
        workId: String,
        expectedStatuses: List<String>,
        destinationPath: String,
        status: String,
    ): Int

    @Query(
        """
        UPDATE downloads
        SET status = :status, bytes_per_second = 0
        WHERE game_id = :gameId AND work_id = :workId AND status IN (:expectedStatuses)
        """,
    )
    suspend fun setWorkerStatus(gameId: String, workId: String, status: String, expectedStatuses: List<String>): Int

    @Query(
        """
        UPDATE downloads
        SET percent = :percent,
            bytes_per_second = :bytesPerSecond,
            downloaded_bytes = :downloadedBytes,
            total_bytes = :totalBytes,
            error_message = NULL
        WHERE game_id = :gameId AND work_id = :workId AND status = :expectedStatus
        """,
    )
    suspend fun setProgress(
        gameId: String,
        workId: String,
        expectedStatus: String,
        percent: Double,
        bytesPerSecond: Double,
        downloadedBytes: Long,
        totalBytes: Long?,
    ): Int

    @Query(
        """
        UPDATE downloads
        SET status = :status,
            bytes_per_second = 0,
            error_message = :errorMessage,
            key = CASE WHEN :clearSensitiveData THEN NULL ELSE key END,
            url = CASE WHEN :clearSensitiveData THEN '' ELSE url END
        WHERE game_id = :gameId AND work_id = :workId AND status = :expectedStatus
        """,
    )
    suspend fun finish(
        gameId: String,
        workId: String,
        expectedStatus: String,
        status: String,
        errorMessage: String?,
        clearSensitiveData: Boolean = false,
    ): Int

    @Query(
        """
        UPDATE downloads
        SET status = :status,
            percent = 100,
            bytes_per_second = 0,
            error_message = NULL
        WHERE game_id = :gameId AND work_id = :workId AND status = :expectedStatus
        """,
    )
    suspend fun complete(gameId: String, workId: String, expectedStatus: String, status: String): Int

    @Query("DELETE FROM downloads WHERE game_id IN (:gameIds)")
    suspend fun deleteByGameIds(gameIds: List<String>): Int
}
