package dev.jagoba.lostielauncher.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.download.DownloadDao
import dev.jagoba.lostielauncher.service.download.LauncherDatabase
import dev.jagoba.lostielauncher.service.download.MIGRATION_1_2
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    private const val DATABASE_NAME = "launcher.db"

    @Provides
    @Singleton
    internal fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase =
        Room.databaseBuilder(context, LauncherDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    internal fun provideDownloadDao(database: LauncherDatabase): DownloadDao = database.downloadDao()
}
