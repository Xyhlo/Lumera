package com.lumera.app.data.trakt

import android.util.Log
import com.lumera.app.data.model.trakt.TraktIds
import com.lumera.app.data.model.trakt.TraktScrobbleEpisode
import com.lumera.app.data.model.trakt.TraktScrobbleMovie
import com.lumera.app.data.model.trakt.TraktScrobbleRequest
import com.lumera.app.data.model.trakt.TraktScrobbleShow
import com.lumera.app.data.remote.TraktSyncApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Trakt scrobble events (start/pause/stop) during playback.
 *
 * Scrobble flow:
 * - start: user begins or resumes playback
 * - pause: user pauses
 * - stop:  user exits the player — if progress >= threshold, Trakt auto-marks as watched
 *
 * The playback ID format is:
 * - Movies: "tt1234567"
 * - Episodes: "tt1234567:1:3:0" (imdbId:season:episode:streamIndex)
 */
@Singleton
class TraktScrobbleManager @Inject constructor(
    private val traktSyncApi: TraktSyncApiService,
    private val traktAuthManager: TraktAuthManager
) {
    companion object {
        private const val TAG = "TraktScrobble"
        private const val MIN_SCROBBLE_INTERVAL_MS = 5_000L
    }

    private var lastScrobbleTimeMs = 0L
    private var activePlaybackId: String? = null

    /**
     * Called when playback starts or resumes.
     */
    suspend fun scrobbleStart(playbackId: String, mediaType: String, positionMs: Long, durationMs: Long) {
        if (!shouldScrobble()) return
        val progress = calculateProgress(positionMs, durationMs)
        val request = buildRequest(playbackId, mediaType, progress) ?: return

        activePlaybackId = playbackId
        withContext(Dispatchers.IO) {
            try {
                val response = traktSyncApi.scrobbleStart(request)
                Log.d(TAG, "start: ${response.code()} progress=${"%.1f".format(progress)}%")
            } catch (e: Exception) {
                Log.w(TAG, "start failed: ${e.message}")
            }
        }
    }

    /**
     * Called when playback is paused.
     */
    suspend fun scrobblePause(playbackId: String, mediaType: String, positionMs: Long, durationMs: Long) {
        if (!shouldScrobble()) return
        if (!isDebouncedOk()) return
        val progress = calculateProgress(positionMs, durationMs)
        val request = buildRequest(playbackId, mediaType, progress) ?: return

        withContext(Dispatchers.IO) {
            try {
                val response = traktSyncApi.scrobblePause(request)
                Log.d(TAG, "pause: ${response.code()} progress=${"%.1f".format(progress)}%")
            } catch (e: Exception) {
                Log.w(TAG, "pause failed: ${e.message}")
            }
        }
    }

    /**
     * Called when playback stops (user exits player).
     * If progress >= threshold, Trakt automatically marks the item as watched.
     */
    suspend fun scrobbleStop(playbackId: String, mediaType: String, positionMs: Long, durationMs: Long) {
        if (!shouldScrobble()) return
        val progress = calculateProgress(positionMs, durationMs)
        val request = buildRequest(playbackId, mediaType, progress) ?: return

        activePlaybackId = null
        withContext(Dispatchers.IO) {
            try {
                val response = traktSyncApi.scrobbleStop(request)
                Log.d(TAG, "stop: ${response.code()} progress=${"%.1f".format(progress)}%")
            } catch (e: Exception) {
                Log.w(TAG, "stop failed: ${e.message}")
            }
        }
    }

    // ── Internal ──

    private fun shouldScrobble(): Boolean = traktAuthManager.getAccessToken() != null

    private fun isDebouncedOk(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastScrobbleTimeMs < MIN_SCROBBLE_INTERVAL_MS) return false
        lastScrobbleTimeMs = now
        return true
    }

    private fun calculateProgress(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return ((positionMs.toFloat() / durationMs.toFloat()) * 100f).coerceIn(0f, 100f)
    }

    /**
     * Build the scrobble request based on playback ID format.
     * Movies: TraktScrobbleMovie with IMDb ID
     * Episodes: TraktScrobbleShow + TraktScrobbleEpisode with season/episode numbers
     */
    private fun buildRequest(playbackId: String, mediaType: String, progress: Float): TraktScrobbleRequest? {
        if (mediaType == "series") {
            // Episode format: "tt1234567:1:3:0" → imdbId, season, episode, streamIdx
            val parts = playbackId.split(":")
            if (parts.size < 4) {
                Log.w(TAG, "Invalid episode playback ID: $playbackId")
                return null
            }
            val imdbId = parts.dropLast(3).joinToString(":") // handles IDs with colons
            val season = parts[parts.size - 3].toIntOrNull() ?: return null
            val episode = parts[parts.size - 2].toIntOrNull() ?: return null

            return TraktScrobbleRequest(
                show = TraktScrobbleShow(ids = TraktIds(imdb = imdbId)),
                episode = TraktScrobbleEpisode(season = season, number = episode),
                progress = progress
            )
        } else {
            // Movie format: "tt1234567"
            return TraktScrobbleRequest(
                movie = TraktScrobbleMovie(ids = TraktIds(imdb = playbackId)),
                progress = progress
            )
        }
    }
}
