package com.lumera.app.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "series_next_up")
data class SeriesNextUpEntity(
    @PrimaryKey val seriesId: String,   // IMDb ID (e.g., "tt1190634")
    val title: String,                  // Show name
    val poster: String?,
    val nextSeason: Int,
    val nextEpisode: Int,
    val nextEpisodeTitle: String?,
    val nextReleased: String? = null,   // Air date ISO string (e.g., "2025-07-10"), null = already aired
    val isComplete: Boolean = false,    // All episodes watched — don't show in continue watching
    val updatedAt: Long
)
