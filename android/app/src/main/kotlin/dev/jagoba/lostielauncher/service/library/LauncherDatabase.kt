package dev.jagoba.lostielauncher.service.library

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.jagoba.lostielauncher.service.download.DownloadDao
import dev.jagoba.lostielauncher.service.download.DownloadEntity

@Database(
    entities = [DownloadEntity::class],
    version = 4,
    exportSchema = true,
)
internal abstract class LauncherDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
