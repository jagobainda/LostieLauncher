package dev.jagoba.lostielauncher.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.ExternalLinkOptions
import dev.jagoba.lostielauncher.service.link.AndroidExternalLinkService
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ExternalLinkBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindExternalLinkService(impl: AndroidExternalLinkService): ExternalLinkService
}

@Module
@InstallIn(SingletonComponent::class)
internal object ExternalLinkOptionsModule {
    @Provides
    internal fun provideExternalLinkOptions(): ExternalLinkOptions = ExternalLinkOptions(
        mapOf(
            ExternalLink.GITHUB to "https://github.com/jagobainda/LostieLauncher",
            ExternalLink.TWITCH to "https://www.twitch.tv/ericlostie",
            ExternalLink.YOUTUBE to "https://www.youtube.com/@EricLostie",
            ExternalLink.TWITTER to "https://x.com/Eric_Lostie",
        ),
    )
}
