package dev.jagoba.lostielauncher.service.download

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.settings.AppearanceStore

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface DownloadWorkerEntryPoint {
    fun runner(): DownloadWorkerRunner

    fun foregroundInfoFactory(): DownloadForegroundInfoFactory

    fun appearanceStore(): AppearanceStore
}
