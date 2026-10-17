package dev.jagoba.lostielauncher.core.di

import android.content.Context
import android.os.Environment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.storage.FixedStorageLocations
import dev.jagoba.lostielauncher.service.storage.StorageLocations
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StorageModule {
    private const val GAMES_DIRECTORY = "games"
    private const val LOGS_DIRECTORY = "logs"

    @Provides
    @Singleton
    fun provideStorageLocations(@ApplicationContext context: Context): StorageLocations {
        val external = context.getExternalFilesDir(null)
            ?.takeIf { Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED }
        val gameStorage = external ?: context.filesDir
        return FixedStorageLocations(
            gamesRoot = File(gameStorage, GAMES_DIRECTORY),
            logsDirectory = File(context.noBackupFilesDir, LOGS_DIRECTORY),
        )
    }
}
