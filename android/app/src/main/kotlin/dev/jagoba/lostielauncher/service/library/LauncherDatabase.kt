package dev.jagoba.lostielauncher.service.library

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.jagoba.lostielauncher.service.download.DownloadDao
import dev.jagoba.lostielauncher.service.download.DownloadEntity

@Database(
    entities = [InstalledGameEntity::class, PlaytimeEntity::class, DownloadEntity::class],
    version = 2,
    exportSchema = true,
)
internal abstract class LauncherDatabase : RoomDatabase() {
    abstract fun localLibraryDao(): LocalLibraryDao

    abstract fun downloadDao(): DownloadDao
}
