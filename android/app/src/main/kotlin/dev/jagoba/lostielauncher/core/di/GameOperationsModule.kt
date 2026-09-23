package dev.jagoba.lostielauncher.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.game.GameLaunchService
import dev.jagoba.lostielauncher.service.game.GameLocationService
import dev.jagoba.lostielauncher.service.game.PendingGameInstallationService
import dev.jagoba.lostielauncher.service.game.PendingGameLaunchService
import dev.jagoba.lostielauncher.service.game.PendingGameLocationService
import dev.jagoba.lostielauncher.service.game.PendingPlaySessionService
import dev.jagoba.lostielauncher.service.game.PlaySessionService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GameOperationsModule {
    @Binds
    @Singleton
    internal abstract fun bindInstallation(impl: PendingGameInstallationService): GameInstallationService

    @Binds
    @Singleton
    internal abstract fun bindLaunch(impl: PendingGameLaunchService): GameLaunchService

    @Binds
    @Singleton
    internal abstract fun bindPlaySession(impl: PendingPlaySessionService): PlaySessionService

    @Binds
    @Singleton
    internal abstract fun bindLocation(impl: PendingGameLocationService): GameLocationService
}
