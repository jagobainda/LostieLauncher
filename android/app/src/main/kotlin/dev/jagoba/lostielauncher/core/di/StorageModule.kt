package dev.jagoba.lostielauncher.core.di

import android.content.Context
import android.os.Environment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.storage.AndroidStorageAccess
import dev.jagoba.lostielauncher.service.storage.FixedStorageLocations
import dev.jagoba.lostielauncher.service.storage.StorageAccess
import dev.jagoba.lostielauncher.service.storage.StorageLocations
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StorageBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindStorageAccess(impl: AndroidStorageAccess): StorageAccess
}

@Module
@InstallIn(SingletonComponent::class)
internal object StorageModule {
    private const val GAMES_DIRECTORY = "games"
    private const val LOGS_DIRECTORY = "logs"

    @Provides
    @Singleton
    fun provideStorageLocations(@ApplicationContext context: Context): StorageLocations {
        val downloadsRoot = File(context.noBackupFilesDir, GAMES_DIRECTORY)
        val external = context.getExternalFilesDir(null)
            ?.takeIf { Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED }
        val legacyDownloadsRoots = listOfNotNull(
            external?.let { File(it, GAMES_DIRECTORY) },
            File(context.filesDir, GAMES_DIRECTORY),
        ).filter { it != downloadsRoot }
        return FixedStorageLocations(
            downloadsRoot = downloadsRoot,
            logsDirectory = File(context.noBackupFilesDir, LOGS_DIRECTORY),
            legacyDownloadsRoots = legacyDownloadsRoots,
        )
    }
}
