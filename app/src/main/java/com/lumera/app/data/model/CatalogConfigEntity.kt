package com.lumera.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_configs")
data class CatalogConfigEntity(
    @PrimaryKey val uniqueId: String, // transportUrl + type + id

    val transportUrl: String,
    val addonName: String,
    val catalogType: String,
    val catalogId: String,
    val catalogName: String? = null,  // Human-readable name from manifest (e.g. "Popular")

    val customTitle: String? = null,

    // --- INDEPENDENT VISIBILITY ---
    val showInHome: Boolean = false,
    val showInMovies: Boolean = false,
    val showInSeries: Boolean = false,

    // --- INDEPENDENT ORDERING ---
    val homeOrder: Int = 999,
    val moviesOrder: Int = 999,
    val seriesOrder: Int = 999,

    // --- LAYOUT SETTINGS (Grid View) ---
    val isInfiniteLoopEnabled: Boolean = false,  // Default: Standard linear scrolling (Grid View)
    val visibleItemCount: Int = 15,              // Only used when Grid View is enabled
    val isInfiniteScrollingEnabled: Boolean = true  // Only used when Grid View is enabled; loops at View More
)