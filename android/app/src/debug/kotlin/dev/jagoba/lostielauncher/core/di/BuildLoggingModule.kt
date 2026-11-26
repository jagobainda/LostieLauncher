package dev.jagoba.lostielauncher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.log.FileLogger
import dev.jagoba.lostielauncher.util.log.AndroidLogger
import dev.jagoba.lostielauncher.util.log.BuildLoggers
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BuildLoggingModule {
    @Provides
    @Singleton
    internal fun provideLogger(fileLogger: FileLogger): Logger = BuildLoggers.debug(AndroidLogger(), fileLogger)
}
