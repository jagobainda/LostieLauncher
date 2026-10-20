package dev.jagoba.lostielauncher.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.ExternalLinkOptions
import dev.jagoba.lostielauncher.model.TwitchLiveOptions
import dev.jagoba.lostielauncher.service.link.AndroidExternalLinkService
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.link.PreviewTwitchLiveService
import dev.jagoba.lostielauncher.service.link.TwitchLiveService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ExternalLinkBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindExternalLinkService(impl: AndroidExternalLinkService): ExternalLinkService

    @Binds
    @Singleton
    internal abstract fun bindTwitchLiveService(impl: PreviewTwitchLiveService): TwitchLiveService
}

@Module
@InstallIn(SingletonComponent::class)
internal object ExternalLinkOptionsModule {
    private const val TWITCH_CHANNEL = "ericlostie"

    @Provides
    internal fun provideExternalLinkOptions(): ExternalLinkOptions = ExternalLinkOptions(
        mapOf(
            ExternalLink.GITHUB to "https://github.com/jagobainda/LostieLauncher",
            ExternalLink.TWITCH to "https://www.twitch.tv/$TWITCH_CHANNEL",
            ExternalLink.YOUTUBE to "https://www.youtube.com/@EricLostie",
            ExternalLink.TWITTER to "https://x.com/Eric_Lostie",
        ),
    )

    @Provides
    internal fun provideTwitchLiveOptions(): TwitchLiveOptions =
        TwitchLiveOptions("https://static-cdn.jtvnw.net/previews-ttv/live_user_$TWITCH_CHANNEL-80x45.jpg")
}
