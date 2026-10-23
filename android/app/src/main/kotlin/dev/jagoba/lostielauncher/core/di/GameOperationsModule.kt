package dev.jagoba.lostielauncher.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.game.ArchiveGameInstallationService
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.game.GameLaunchService
import dev.jagoba.lostielauncher.service.game.GameLocationService
import dev.jagoba.lostielauncher.service.game.KirinGameLaunchService
import dev.jagoba.lostielauncher.service.game.PendingGameLocationService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GameOperationsModule {
    @Binds
    @Singleton
    internal abstract fun bindInstallation(impl: ArchiveGameInstallationService): GameInstallationService

    @Binds
    @Singleton
    internal abstract fun bindLaunch(impl: KirinGameLaunchService): GameLaunchService

    @Binds
    @Singleton
    internal abstract fun bindLocation(impl: PendingGameLocationService): GameLocationService
}
