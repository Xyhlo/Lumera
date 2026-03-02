package com.lumera.app.data.model.stremio

import com.google.gson.JsonElement // <--- We need this import to handle dynamic types
import com.google.gson.annotations.SerializedName

// Tells us what the addon can do (stream, catalog, meta)
data class Manifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,

    // FIX: Changed from List<String> to List<JsonElement>
    // This allows it to accept both Cinemeta (Strings) and Torrentio (Objects)
    val resources: List<JsonElement>,

    val types: List<String>,
    val catalogs: List<CatalogManifest> = emptyList(),
    val logo: String? = null
)

data class CatalogManifest(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<CatalogExtra> = emptyList()
)

data class CatalogExtra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String>? = null
)