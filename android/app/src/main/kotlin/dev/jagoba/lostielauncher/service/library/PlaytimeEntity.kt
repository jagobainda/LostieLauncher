package dev.jagoba.lostielauncher.service.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playtime")
internal data class PlaytimeEntity(
    @PrimaryKey
    @ColumnInfo(name = "game_id")
    val gameId: String,
    val minutes: Int,
)
