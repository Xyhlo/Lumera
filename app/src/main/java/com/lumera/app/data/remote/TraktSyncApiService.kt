package com.lumera.app.data.remote

import com.lumera.app.data.model.trakt.TraktLastActivities
import com.lumera.app.data.model.trakt.TraktPlaybackItem
import com.lumera.app.data.model.trakt.TraktScrobbleRequest
import com.lumera.app.data.model.trakt.TraktSyncRequest
import com.lumera.app.data.model.trakt.TraktSyncResponse
import com.lumera.app.data.model.trakt.TraktWatchlistItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TraktSyncApiService {

    // ── Activity ──

    @GET("sync/last_activities")
    suspend fun getLastActivities(): Response<TraktLastActivities>

    // ── Watchlist ──

    @GET("sync/watchlist")
    suspend fun getWatchlist(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Response<List<TraktWatchlistItem>>

    @POST("sync/watchlist")
    suspend fun addToWatchlist(
        @Body body: TraktSyncRequest
    ): Response<TraktSyncResponse>

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(
        @Body body: TraktSyncRequest
    ): Response<TraktSyncResponse>

    // ── Playback Progress (Continue Watching) ──

    @GET("sync/playback")
    suspend fun getPlaybackProgress(): Response<List<TraktPlaybackItem>>

    // ── Scrobble ──

    @POST("scrobble/start")
    suspend fun scrobbleStart(
        @Body body: TraktScrobbleRequest
    ): Response<okhttp3.ResponseBody>

    @POST("scrobble/pause")
    suspend fun scrobblePause(
        @Body body: TraktScrobbleRequest
    ): Response<okhttp3.ResponseBody>

    @POST("scrobble/stop")
    suspend fun scrobbleStop(
        @Body body: TraktScrobbleRequest
    ): Response<okhttp3.ResponseBody>
}
