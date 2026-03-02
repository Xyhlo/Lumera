package com.lumera.app.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String, // The Movie ID (e.g. "tt00000")
    val title: String,
    val poster: String?,
    val position: Long,    // Where we stopped (in milliseconds)
    val duration: Long,    // Total length
    val lastWatched: Long, // Timestamp (to sort by "Recent")
    val type: String       // "movie" or "series"
) {
    // Helper to calculate progress percentage (0.0 to 1.0)
    fun progress(): Float {
        if (duration == 0L) return 0f
        return position.toFloat() / duration.toFloat()
    }
}