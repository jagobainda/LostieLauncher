package dev.jagoba.lostielauncher.core.di

import android.os.Environment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.LocalLibraryOptions
import dev.jagoba.lostielauncher.service.game.AndroidKirinShortcut
import dev.jagoba.lostielauncher.service.game.KirinShortcut
import dev.jagoba.lostielauncher.service.library.FileLocalLibraryStore
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class KirinBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindShortcut(impl: AndroidKirinShortcut): KirinShortcut

    @Binds
    @Singleton
    internal abstract fun bindLocalLibraryStore(impl: FileLocalLibraryStore): LocalLibraryStore
}

@Module
@InstallIn(SingletonComponent::class)
internal object KirinModule {
    @Provides
    @Singleton
    fun provideKirinOptions(): KirinOptions {
        val sharedStorageRoot = Environment.getExternalStorageDirectory()
        return KirinOptions(
            gamesRoot = File(sharedStorageRoot, "LostieLauncher"),
            libraryFile = File(sharedStorageRoot, "Kirin/game-library.json"),
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

    @Provides
    @Singleton
    internal fun provideLocalLibraryOptions(kirin: KirinOptions): LocalLibraryOptions =
        LocalLibraryOptions(File(kirin.gamesRoot, "local_games.json"))
}
