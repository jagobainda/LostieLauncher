package dev.jagoba.lostielauncher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.BuildConfig
import dev.jagoba.lostielauncher.model.AppVersion
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import kotlin.time.Duration.Companion.minutes

@Module
@InstallIn(SingletonComponent::class)
internal object PresentationModule {
    @Provides
    internal fun provideHomeRefreshOptions(): HomeRefreshOptions = HomeRefreshOptions(2.minutes)

    @Provides
    internal fun provideAppVersion(): AppVersion = AppVersion(BuildConfig.VERSION_NAME)
}
