package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadDestination
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.DownloadSnapshot
import kotlinx.coroutines.flow.Flow

interface DownloadManager {
    val downloads: Flow<List<DownloadSnapshot>>

    suspend fun start(request: DownloadRequest): DownloadCommandResult

    suspend fun pause(gameId: String): DownloadCommandResult

    suspend fun resume(gameId: String): DownloadCommandResult

    suspend fun cancel(gameId: String): DownloadCommandResult

    suspend fun purgeStale(knownGameIds: Set<String>): Int

    suspend fun destination(): DownloadDestination
}
