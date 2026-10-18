package dev.jagoba.lostielauncher.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.service.download.DownloadDao
import dev.jagoba.lostielauncher.service.library.LauncherDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    private const val DATABASE_NAME = "launcher.db"

    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS downloads (
                    game_id TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    version TEXT NOT NULL,
                    key TEXT,
                    url TEXT NOT NULL,
                    destination_path TEXT NOT NULL,
                    work_id TEXT NOT NULL,
                    status TEXT NOT NULL,
                    percent REAL NOT NULL,
                    bytes_per_second REAL NOT NULL,
                    downloaded_bytes INTEGER NOT NULL,
                    total_bytes INTEGER,
                    error_message TEXT,
                    created_at_epoch_millis INTEGER NOT NULL,
                    PRIMARY KEY(game_id)
                )
                """.trimIndent(),
            )
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS playtime")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS installed_games")
        }
    }

    @Provides
    @Singleton
    internal fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase =
        Room.databaseBuilder(context, LauncherDatabase::class.java, DATABASE_NAME)
            .addMigrations(migration1To2, migration2To3, migration3To4)
            .build()

    @Provides
    @Singleton
    internal fun provideDownloadDao(database: LauncherDatabase): DownloadDao = database.downloadDao()
}
