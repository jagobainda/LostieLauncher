package dev.jagoba.lostielauncher.service.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
internal abstract class LocalLibraryDao {
    @Query("SELECT * FROM installed_games ORDER BY row_id")
    abstract suspend fun getGames(): List<InstalledGameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGame(game: InstalledGameEntity)

    @Query("DELETE FROM installed_games WHERE name_key = :nameKey")
    abstract suspend fun deleteGame(nameKey: String)

    @Query("SELECT * FROM playtime")
    abstract suspend fun getPlaytimes(): List<PlaytimeEntity>

    @Query("UPDATE playtime SET minutes = minutes + :minutes WHERE game_id = :gameId")
    protected abstract suspend fun incrementPlaytime(gameId: String, minutes: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPlaytime(playtime: PlaytimeEntity): Long

    @Transaction
    open suspend fun addPlaytime(gameId: String, minutes: Int) {
        if (incrementPlaytime(gameId, minutes) != 0) return
        if (insertPlaytime(PlaytimeEntity(gameId, minutes)) == -1L) incrementPlaytime(gameId, minutes)
    }
}
