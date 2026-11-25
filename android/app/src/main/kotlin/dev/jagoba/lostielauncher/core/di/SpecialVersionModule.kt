package dev.jagoba.lostielauncher.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.DownloadOptions
import dev.jagoba.lostielauncher.model.SpecialVersionOptions
import dev.jagoba.lostielauncher.service.cdn.CdnSpecialVersionService
import dev.jagoba.lostielauncher.service.cdn.SpecialVersionService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpecialVersionBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindSpecialVersionService(impl: CdnSpecialVersionService): SpecialVersionService
}

@Module
@InstallIn(SingletonComponent::class)
internal object SpecialVersionOptionsModule {
    @Provides
    internal fun provideSpecialVersionOptions(downloads: DownloadOptions): SpecialVersionOptions =
        SpecialVersionOptions(
            baseUrl = downloads.baseUrl,
            configFileName = "game.config",
            maxConfigBytes = SpecialVersionOptions.DEFAULT_MAX_CONFIG_BYTES,
        )
}
