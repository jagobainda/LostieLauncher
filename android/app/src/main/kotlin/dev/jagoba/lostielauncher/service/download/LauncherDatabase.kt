package dev.jagoba.lostielauncher.service.download

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class LauncherDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
