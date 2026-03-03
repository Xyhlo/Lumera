package com.lumera.app.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val poster: String?,
    val position: Long,
    val duration: Long,
    val lastWatched: Long,
    val type: String
) {
    fun progress(): Float {
        if (duration == 0L) return 0f
        return position.toFloat() / duration.toFloat()
    }
}