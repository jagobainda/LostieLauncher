package dev.jagoba.lostielauncher.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.LogOptions
import dev.jagoba.lostielauncher.service.log.FileLogger
import dev.jagoba.lostielauncher.util.log.LogMaintenance
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoggingBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindLogMaintenance(impl: FileLogger): LogMaintenance
}

@Module
@InstallIn(SingletonComponent::class)
internal object LoggingModule {
    private const val MAX_LOG_FILE_BYTES = 10L * 1024 * 1024
    private const val LOG_RETENTION_MONTHS = 6

    @Provides
    @Singleton
    internal fun provideLogOptions(): LogOptions = LogOptions(
        maxFileSizeBytes = MAX_LOG_FILE_BYTES,
        retentionMonths = LOG_RETENTION_MONTHS,
        zoneId = ZoneId.systemDefault(),
    )
}
