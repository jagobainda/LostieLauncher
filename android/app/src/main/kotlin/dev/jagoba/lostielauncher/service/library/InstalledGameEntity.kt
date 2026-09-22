package dev.jagoba.lostielauncher.service.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installed_games",
    indices = [Index(value = ["name_key"], unique = true)],
)
internal data class InstalledGameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    val rowId: Long = 0,
    @ColumnInfo(name = "name_key")
    val nameKey: String,
    @ColumnInfo(name = "game_id")
    val gameId: String,
    val name: String,
    val version: String,
    val variant: String?,
)
