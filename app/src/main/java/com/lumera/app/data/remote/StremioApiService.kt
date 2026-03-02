package com.lumera.app.data.remote

import com.lumera.app.data.model.stremio.CatalogResponse
import com.lumera.app.data.model.stremio.Manifest
import com.lumera.app.data.model.stremio.MetaResponse
import com.lumera.app.data.model.stremio.SubtitleResponse
import com.lumera.app.data.model.stremio.StreamResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface StremioApiService {

    // Download the addon description (manifest.json)
    // We use @Url so we can pass any link (Cinemeta, Torrentio, etc.)
    @GET
    suspend fun getManifest(@Url url: String): Manifest

    // Get a list of movies (Catalog)
    @GET
    suspend fun getCatalog(@Url url: String): CatalogResponse

    // Get details about a movie/show (meta)
    @GET
    suspend fun getMeta(@Url url: String): MetaResponse

    // Get the video links (streams)
    @GET
    suspend fun getStreams(@Url url: String): StreamResponse

    // Get subtitles from subtitle addons
    @GET
    suspend fun getSubtitles(@Url url: String): SubtitleResponse
}
