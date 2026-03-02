package com.lumera.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addons")
data class AddonEntity(
    @PrimaryKey val transportUrl: String,

    val id: String,
    val name: String,
    val version: String,
    val description: String?,
    val iconUrl: String?,
    val isTrusted: Boolean = false,
    val isEnabled: Boolean = true,
    val nickname: String? = null,

    // Store supported catalogs as JSON string
    val catalogsJson: String = "[]",

    val sortOrder: Int = 999
)