package dev.jagoba.lostielauncher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.KirinOptions
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object KirinModule {
    private const val SHARED_STORAGE_ROOT = "/storage/emulated/0"

    @Provides
    @Singleton
    fun provideKirinOptions(): KirinOptions = KirinOptions(
        gamesRoot = File(SHARED_STORAGE_ROOT, "LostieLauncher"),
        libraryFile = File(SHARED_STORAGE_ROOT, "Kirin/game-library.json"),
        executableName = "Game.exe",
        runtimeMode = "auto",
    )
}
