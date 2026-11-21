package dev.jagoba.lostielauncher.service.download

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadEntity::class],
    version = 2,
    exportSchema = true,
)
internal abstract class LauncherDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN expected_sha256 TEXT")
        db.execSQL("ALTER TABLE downloads ADD COLUMN variant TEXT")
    }
}
