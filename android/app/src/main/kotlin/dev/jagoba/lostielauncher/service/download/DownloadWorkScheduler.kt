package dev.jagoba.lostielauncher.service.download

import java.util.UUID

internal interface DownloadWorkScheduler {
    fun nextId(): UUID

    fun enqueue(gameId: String, workId: UUID)

    suspend fun cancel(workId: UUID)
}
