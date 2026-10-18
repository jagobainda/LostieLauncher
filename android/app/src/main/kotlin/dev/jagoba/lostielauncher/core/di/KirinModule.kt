package dev.jagoba.lostielauncher.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.service.game.AndroidKirinShortcut
import dev.jagoba.lostielauncher.service.game.KirinShortcut
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class KirinBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindShortcut(impl: AndroidKirinShortcut): KirinShortcut
}

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
        packageName = "com.gmax.kirin",
        launchActivity = "com.gmax.kirin.LibraryActivity",
        launchAction = "com.gmax.kirin.action.LAUNCH_GAME_SHORTCUT",
        gamePathExtra = "shortcut_game_path",
        archiveBufferSizeBytes = 64 * 1024,
        archiveExtractionWorkers = 4,
        archiveEntryCostBytes = 64 * 1024,
    )
}
