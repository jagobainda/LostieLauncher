package dev.jagoba.lostielauncher.core.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.DownloadTransferOptions
import dev.jagoba.lostielauncher.service.download.AndroidDownloadForegroundInfoFactory
import dev.jagoba.lostielauncher.service.download.DefaultDownloadFileStore
import dev.jagoba.lostielauncher.service.download.DefaultDownloadManager
import dev.jagoba.lostielauncher.service.download.DownloadFileStore
import dev.jagoba.lostielauncher.service.download.DownloadForegroundInfoFactory
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import dev.jagoba.lostielauncher.service.download.DownloadWorkScheduler
import dev.jagoba.lostielauncher.service.download.DownloadedFileHandoff
import dev.jagoba.lostielauncher.service.download.InstallDownloadedFileHandoff
import dev.jagoba.lostielauncher.service.download.MonotonicTimeSource
import dev.jagoba.lostielauncher.service.download.OkHttpDownloadTransfer
import dev.jagoba.lostielauncher.service.download.WorkManagerDownloadScheduler
import dev.jagoba.lostielauncher.util.download.DownloadCachePolicy
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
internal object DownloadModule {
    private const val CACHE_DIRECTORY_NAME = "downloads"
    private const val BUFFER_SIZE_BYTES = 64 * 1024
    private const val MAXIMUM_ATTEMPTS = 3
    private const val FINALIZATION_MAXIMUM_ATTEMPTS = 3

    @Provides
    @Singleton
    fun provideOptions(): DownloadTransferOptions = DownloadTransferOptions(
        cacheDirectoryName = CACHE_DIRECTORY_NAME,
        cacheMaxAge = DownloadCachePolicy.DEFAULT_MAX_AGE,
        bufferSizeBytes = BUFFER_SIZE_BYTES,
        maximumAttempts = MAXIMUM_ATTEMPTS,
        retryBaseDelay = 1.seconds,
        inactivityTimeout = 60.seconds,
        progressSampleInterval = 500.milliseconds,
        finalizationMaximumAttempts = FINALIZATION_MAXIMUM_ATTEMPTS,
        finalizationBaseDelay = 500.milliseconds,
    )

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideMonotonicTimeSource(): MonotonicTimeSource = MonotonicTimeSource(System::nanoTime)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DownloadBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindManager(impl: DefaultDownloadManager): DownloadManager

    @Binds
    @Singleton
    internal abstract fun bindScheduler(impl: WorkManagerDownloadScheduler): DownloadWorkScheduler

    @Binds
    @Singleton
    internal abstract fun bindFileStore(impl: DefaultDownloadFileStore): DownloadFileStore

    @Binds
    @Singleton
    internal abstract fun bindTransfer(impl: OkHttpDownloadTransfer): DownloadTransfer

    @Binds
    @Singleton
    internal abstract fun bindHandoff(impl: InstallDownloadedFileHandoff): DownloadedFileHandoff

    @Binds
    @Singleton
    internal abstract fun bindForegroundInfoFactory(
        impl: AndroidDownloadForegroundInfoFactory,
    ): DownloadForegroundInfoFactory
}
