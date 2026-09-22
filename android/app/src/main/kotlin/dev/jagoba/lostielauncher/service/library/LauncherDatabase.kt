package dev.jagoba.lostielauncher.service.library

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [InstalledGameEntity::class, PlaytimeEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class LauncherDatabase : RoomDatabase() {
    abstract fun localLibraryDao(): LocalLibraryDao
}
