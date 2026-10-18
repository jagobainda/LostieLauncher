package dev.jagoba.lostielauncher.service.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal abstract class LocalLibraryDao {
    @Query("SELECT * FROM installed_games ORDER BY row_id")
    abstract suspend fun getGames(): List<InstalledGameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGame(game: InstalledGameEntity)

    @Query("DELETE FROM installed_games WHERE name_key = :nameKey")
    abstract suspend fun deleteGame(nameKey: String)
}
