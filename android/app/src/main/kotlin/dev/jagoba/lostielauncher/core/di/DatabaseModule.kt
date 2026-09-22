package dev.jagoba.lostielauncher.core.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.library.LauncherDatabase
import dev.jagoba.lostielauncher.service.library.LocalLibraryDao
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import dev.jagoba.lostielauncher.service.library.RoomLocalLibraryStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    private const val DATABASE_NAME = "launcher.db"

    @Provides
    @Singleton
    internal fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase =
        Room.databaseBuilder(context, LauncherDatabase::class.java, DATABASE_NAME).build()

    @Provides
    @Singleton
    internal fun provideLocalLibraryDao(database: LauncherDatabase): LocalLibraryDao = database.localLibraryDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindLocalLibraryStore(impl: RoomLocalLibraryStore): LocalLibraryStore
}
