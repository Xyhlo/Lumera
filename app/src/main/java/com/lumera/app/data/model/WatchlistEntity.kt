package com.lumera.app.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index

@Immutable
@Entity(
    tableName = "watchlist",
    primaryKeys = ["id", "profileId"],
    indices = [Index(value = ["profileId"])]
)
data class WatchlistEntity(
    val id: String,                   // IMDb or addon ID (e.g., "tt0111161")
    val profileId: Int,               // Owning profile id (per-profile scoping)
    val type: String,                 // "movie" or "series"
    val title: String,
    val poster: String?,
    val addedAt: Long                 // System.currentTimeMillis() when bookmarked
)
