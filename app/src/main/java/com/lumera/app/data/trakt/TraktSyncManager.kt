package com.lumera.app.data.trakt

import android.util.Log
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.WatchHistoryEntity
import com.lumera.app.data.model.WatchlistEntity
import com.lumera.app.data.model.trakt.TraktIds
import com.lumera.app.data.model.trakt.TraktPlaybackItem
import com.lumera.app.data.model.trakt.TraktSyncItem
import com.lumera.app.data.model.trakt.TraktSyncRequest
import com.lumera.app.data.model.trakt.TraktWatchlistItem
import com.lumera.app.data.remote.TraktSyncApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraktSyncManager @Inject constructor(
    private val traktSyncApi: TraktSyncApiService,
    private val traktAuthManager: TraktAuthManager,
    private val dao: AddonDao
) {
    companion object {
        private const val TAG = "TraktSyncManager"
    }

    private val syncMutex = Mutex()

    // Last known activity timestamps from Trakt
    private var lastWatchlistActivity: String? = null
    private var lastPlaybackActivity: String? = null

    // IDs currently being deleted from Trakt — sync skips these to prevent race conditions (Fix 5)
    // Fix #2: Thread-safe set for concurrent access from sync poll and delete operations
    private val pendingDeletes = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Lightweight check: queries /sync/last_activities and only runs
     * syncs for categories whose timestamps have changed.
     */
    suspend fun checkAndSync(): Boolean {
        if (traktAuthManager.getAccessToken() == null) return false

        return withContext(Dispatchers.IO) {
            try {
                val response = traktSyncApi.getLastActivities()
                if (!response.isSuccessful) {
                    Log.w(TAG, "last_activities failed: ${response.code()}")
                    return@withContext false
                }

                val activities = response.body() ?: return@withContext false
                var synced = false

                // Check watchlist changes
                val watchlistTimestamp = activities.watchlist?.updatedAt
                if (watchlistTimestamp != null && watchlistTimestamp != lastWatchlistActivity) {
                    Log.d(TAG, "Watchlist activity changed: $lastWatchlistActivity → $watchlistTimestamp")
                    lastWatchlistActivity = watchlistTimestamp
                    syncWatchlist()
                    synced = true
                }

                // Check playback progress changes (continue watching)
                val moviesPaused = activities.movies?.pausedAt
                val episodesPaused = activities.episodes?.pausedAt
                val playbackTimestamp = listOfNotNull(moviesPaused, episodesPaused).maxOrNull()
                if (playbackTimestamp != null && playbackTimestamp != lastPlaybackActivity) {
                    Log.d(TAG, "Playback activity changed: $lastPlaybackActivity → $playbackTimestamp")
                    lastPlaybackActivity = playbackTimestamp
                    syncPlaybackProgress()
                    synced = true
                }

                synced
            } catch (e: Exception) {
                Log.w(TAG, "Activity check failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Initial sync after first connecting Trakt.
     * Pushes all local items to Trakt, then does a normal sync.
     */
    suspend fun initialSync() = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val localItems = dao.getWatchlistOnce()
                if (localItems.isNotEmpty()) {
                    pushToTrakt(localItems)
                    Log.d(TAG, "Initial push: ${localItems.size} items")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Initial push failed: ${e.message}")
            }
        }
        syncWatchlist()
    }

    /**
     * Periodic watchlist sync with diff.
     * Pulls new items and removes items deleted on Trakt.
     * Does NOT push — local adds are pushed instantly via pushAdd().
     */
    suspend fun syncWatchlist(): Result<Unit> = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (traktAuthManager.getAccessToken() == null) {
                    return@withContext Result.failure(Exception("Not connected to Trakt"))
                }

                // 1. Fetch Trakt watchlist
                val traktItems = fetchAllTraktWatchlist()
                    ?: return@withContext Result.failure(Exception("Failed to fetch Trakt watchlist"))

                // 2. Get local watchlist
                val localItems = dao.getWatchlistOnce()

                // 3. Build lookup sets
                val traktImdbIds = traktItems.mapNotNull { item ->
                    when (item.type) {
                        "movie" -> item.movie?.ids?.imdb
                        "show" -> item.show?.ids?.imdb
                        else -> null
                    }
                }.toSet()

                val localImdbIds = localItems.map { it.id }.toSet()

                // 4. Pull Trakt → local (items on Trakt but not local)
                val toPull = traktItems.filter { item ->
                    val imdbId = when (item.type) {
                        "movie" -> item.movie?.ids?.imdb
                        "show" -> item.show?.ids?.imdb
                        else -> null
                    }
                    imdbId != null && imdbId !in localImdbIds
                }
                if (toPull.isNotEmpty()) {
                    pullFromTrakt(toPull)
                    Log.d(TAG, "Pulled ${toPull.size} items from Trakt")
                }

                // 5. Remove local items no longer on Trakt (deleted externally).
                // Local adds are pushed instantly via pushAdd(), so anything
                // local but missing from Trakt was removed on Trakt's side.
                val toRemove = localItems.filter { it.id !in traktImdbIds }
                for (item in toRemove) {
                    dao.removeFromWatchlist(item.id)
                    Log.d(TAG, "Removed ${item.title} (deleted on Trakt)")
                }

                Log.i(TAG, "Sync complete: pulled=${toPull.size}, removed=${toRemove.size}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Watchlist sync failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Push a single item to Trakt watchlist (called when user adds locally).
     */
    suspend fun pushAdd(item: WatchlistEntity) {
        val token = traktAuthManager.getAccessToken()
        Log.d(TAG, "pushAdd called: id=${item.id}, type=${item.type}, hasToken=${token != null}")
        if (token == null) return
        withContext(Dispatchers.IO) {
            try {
                pushToTrakt(listOf(item))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push watchlist add to Trakt: ${e.message}")
            }
        }
    }

    /**
     * Push a removal to Trakt watchlist (called when user removes locally).
     */
    suspend fun pushRemove(itemId: String, type: String) {
        if (traktAuthManager.getAccessToken() == null) return
        withContext(Dispatchers.IO) {
            try {
                val item = listOf(TraktSyncItem(ids = TraktIds(imdb = itemId)))
                val body = if (type == "movie") TraktSyncRequest(movies = item)
                           else TraktSyncRequest(shows = item)
                val response = traktSyncApi.removeFromWatchlist(body)
                if (!response.isSuccessful) {
                    Log.w(TAG, "Trakt watchlist remove failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push watchlist remove to Trakt: ${e.message}")
            }
        }
    }

    /**
     * Full playback progress sync with diff logic.
     *
     * For each LOCAL in-progress item where scrobbled == true:
     *   - On Trakt playback?        → keep local (more accurate position)
     *   - Not on playback, on watched? → mark as watched locally (finished on other app)
     *   - Not on either?            → user cleared it on Trakt → delete locally
     *
     * For each LOCAL in-progress item where scrobbled == false:
     *   → Never touched Trakt. Leave it alone.
     *
     * For each item on Trakt playback NOT in local:
     *   → Pull to Lumera (watched on other app)
     */
    suspend fun syncPlaybackProgress() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch Trakt playback progress
                val playbackResponse = traktSyncApi.getPlaybackProgress()
                if (!playbackResponse.isSuccessful) {
                    Log.w(TAG, "Playback progress fetch failed: ${playbackResponse.code()}")
                    return@withContext
                }
                val traktPlayback = playbackResponse.body() ?: emptyList()

                // 2. Build lookup: IMDb ID → TraktPlaybackItem
                val traktPlaybackIds = mutableMapOf<String, TraktPlaybackItem>()
                for (item in traktPlayback) {
                    val id = when (item.type) {
                        "movie" -> item.movie?.ids?.imdb
                        "episode" -> {
                            val showImdb = item.show?.ids?.imdb ?: continue
                            val ep = item.episode ?: continue
                            "$showImdb:${ep.season}:${ep.number}"
                        }
                        else -> null
                    }
                    if (id != null) traktPlaybackIds[id] = item
                }

                // 3. Fetch Trakt watched history with episode-level detail (Fix 2)
                // Fix #7: If this fails, we get null — skip removals entirely to prevent data loss
                val watchedData = fetchWatchedData()

                // 4. Process local scrobbled in-progress items
                val scrobbledItems = dao.getScrobbledInProgressItems()
                var removed = 0
                var markedWatched = 0

                for (local in scrobbledItems) {
                    // Fix 5: skip items currently being deleted to prevent race
                    if (local.id in pendingDeletes) continue

                    // Fix 1: strip stream index suffix for lookup
                    val normalizedId = normalizePlaybackId(local.id)

                    val onTraktPlayback = normalizedId in traktPlaybackIds

                    if (onTraktPlayback) {
                        // Still in progress on Trakt — keep local version (more accurate)
                        continue
                    }

                    // Fix #7: If watched data fetch failed, we can't determine if
                    // the item was cleared vs finished. Skip to avoid data loss.
                    if (watchedData == null) continue

                    // Not on Trakt playback. Check if the specific item was watched (Fix 2)
                    val onTraktWatched = if (local.type == "series") {
                        val parts = normalizedId.split(":")
                        if (parts.size >= 3) {
                            val showImdb = parts.dropLast(2).joinToString(":")
                            val season = parts[parts.size - 2].toIntOrNull()
                            val episode = parts[parts.size - 1].toIntOrNull()
                            season != null && episode != null && watchedData.isEpisodeWatched(showImdb, season, episode)
                        } else false
                    } else {
                        watchedData.isMovieWatched(local.id)
                    }

                    if (onTraktWatched) {
                        // Finished on another app → mark as watched locally
                        dao.upsertHistory(local.copy(watched = true))
                        markedWatched++
                        Log.d(TAG, "Marked watched (finished elsewhere): ${local.title}")
                    } else {
                        // Not on playback, not on watched → user cleared it on Trakt
                        dao.deleteHistoryItem(local.id)
                        removed++
                        Log.d(TAG, "Removed (cleared on Trakt): ${local.title}")
                    }
                }

                // 5. Pull new items from Trakt playback that aren't local
                var added = 0
                for ((id, item) in traktPlaybackIds) {
                    // Fix 5: skip items being deleted
                    if (id in pendingDeletes) continue

                    val type = if (item.type == "movie") "movie" else "series"

                    // Don't overwrite existing local progress (check both exact and with stream index)
                    val existing = dao.getHistoryItem(id)
                    if (existing != null) continue
                    if (type == "series") {
                        val hasLocal = dao.getLatestSeriesEpisodeHistory("$id:%") != null
                        if (hasLocal) continue
                    }

                    val title = when (item.type) {
                        "movie" -> item.movie?.title ?: "Unknown"
                        else -> {
                            val ep = item.episode
                            val showTitle = item.show?.title ?: "Unknown"
                            when {
                                ep?.title != null -> ep.title
                                ep?.season != null && ep.number != null -> "S${ep.season}:E${ep.number} - $showTitle"
                                else -> showTitle
                            }
                        }
                    }

                    val estimatedDurationMs = 90 * 60 * 1000L
                    val estimatedPositionMs = ((item.progress / 100f) * estimatedDurationMs).toLong()

                    dao.upsertHistory(
                        WatchHistoryEntity(
                            id = id,
                            title = title,
                            poster = null,
                            position = estimatedPositionMs,
                            duration = estimatedDurationMs,
                            lastWatched = System.currentTimeMillis(),
                            type = type,
                            watched = false,
                            scrobbled = true
                        )
                    )
                    added++
                }

                Log.i(TAG, "Playback sync: added=$added, removed=$removed, markedWatched=$markedWatched")
            } catch (e: Exception) {
                Log.e(TAG, "Playback progress sync failed", e)
            }
        }
    }

    /** Reset stored activity timestamps (e.g., on profile switch). */
    fun resetActivityState() {
        lastWatchlistActivity = null
        lastPlaybackActivity = null
    }

    // ── Internal ──

    /**
     * Fetch the complete Trakt watchlist. Returns null if ANY page fails,
     * so callers never act on incomplete data. (Fix #3)
     */
    private suspend fun fetchAllTraktWatchlist(): List<TraktWatchlistItem>? {
        val allItems = mutableListOf<TraktWatchlistItem>()
        var page = 1

        while (true) {
            val response = traktSyncApi.getWatchlist(page = page, limit = 100)
            if (!response.isSuccessful) {
                Log.w(TAG, "Trakt watchlist fetch failed on page $page: ${response.code()}")
                return null // Fail entirely — never return partial data
            }
            val items = response.body() ?: break
            if (items.isEmpty()) break
            allItems.addAll(items)
            if (items.size < 100) break
            page++
        }

        return allItems
    }

    private suspend fun pushToTrakt(items: List<WatchlistEntity>) {
        val movies = items.filter { it.type == "movie" }
            .map { TraktSyncItem(ids = TraktIds(imdb = it.id)) }
        val shows = items.filter { it.type == "series" }
            .map { TraktSyncItem(ids = TraktIds(imdb = it.id)) }

        val body = TraktSyncRequest(
            movies = movies.ifEmpty { null },
            shows = shows.ifEmpty { null }
        )

        Log.d(TAG, "pushToTrakt: movies=${movies.size}, shows=${shows.size}")
        if (movies.isNotEmpty() || shows.isNotEmpty()) {
            val response = traktSyncApi.addToWatchlist(body)
            Log.d(TAG, "pushToTrakt response: ${response.code()}")
            if (!response.isSuccessful) {
                Log.w(TAG, "Trakt watchlist push failed: ${response.code()} - ${response.errorBody()?.string()}")
            }
        }
    }

    /**
     * Strip stream index suffix from playback ID for Trakt lookup. (Fix 1)
     * "tt123:1:3:0" → "tt123:1:3"  (has stream index)
     * "tt123:1:3"   → "tt123:1:3"  (already normalized)
     * "tt123"       → "tt123"      (movie, no change)
     */
    private fun normalizePlaybackId(id: String): String {
        val parts = id.split(":")
        // Episode with stream index: 4+ parts where last is numeric (stream idx)
        // and second-to-last and third-to-last are also numeric (episode, season)
        if (parts.size >= 4 &&
            parts.last().toIntOrNull() != null &&
            parts[parts.size - 2].toIntOrNull() != null &&
            parts[parts.size - 3].toIntOrNull() != null
        ) {
            return parts.dropLast(1).joinToString(":")
        }
        return id
    }

    /**
     * Watched data from Trakt with episode-level granularity. (Fix 2)
     */
    private class WatchedData(
        val movieIds: Set<String>,
        val episodeMap: Map<String, Set<Pair<Int, Int>>> // showImdbId → set of (season, episode)
    ) {
        fun isMovieWatched(imdbId: String) = imdbId in movieIds
        fun isEpisodeWatched(showImdbId: String, season: Int, episode: Int): Boolean {
            return episodeMap[showImdbId]?.contains(Pair(season, episode)) ?: false
        }
    }

    /**
     * Fetch watched history with episode-level detail.
     * Returns null on failure so callers can skip removal logic. (Fix #7)
     */
    private suspend fun fetchWatchedData(): WatchedData? {
        val movieIds = mutableSetOf<String>()
        val episodeMap = mutableMapOf<String, MutableSet<Pair<Int, Int>>>()
        try {
            val moviesResponse = traktSyncApi.getWatchedMovies()
            if (!moviesResponse.isSuccessful) return null
            moviesResponse.body()?.forEach { it.movie.ids.imdb?.let { id -> movieIds.add(id) } }

            val showsResponse = traktSyncApi.getWatchedShows()
            if (!showsResponse.isSuccessful) return null
            showsResponse.body()?.forEach { show ->
                val showImdb = show.show.ids.imdb ?: return@forEach
                val episodes = episodeMap.getOrPut(showImdb) { mutableSetOf() }
                show.seasons?.forEach { season ->
                    season.episodes?.forEach { ep ->
                        episodes.add(Pair(season.number, ep.number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch watched history", e)
            return null
        }
        return WatchedData(movieIds, episodeMap)
    }

    /**
     * Delete a playback item from Trakt by finding its Trakt playback ID.
     * Called when user clears progress in Lumera.
     */
    /**
     * Delete playback items from Trakt. Uses pendingDeletes to prevent
     * the sync poll from re-adding them during the delete operation. (Fix 5)
     */
    suspend fun deletePlaybackFromTrakt(localId: String) {
        if (traktAuthManager.getAccessToken() == null) return
        val normalizedId = normalizePlaybackId(localId)
        pendingDeletes.add(normalizedId)
        pendingDeletes.add(localId)
        withContext(Dispatchers.IO) {
            try {
                val response = traktSyncApi.getPlaybackProgress()
                if (!response.isSuccessful) return@withContext
                val items = response.body() ?: return@withContext

                for (item in items) {
                    val traktId = when (item.type) {
                        "movie" -> item.movie?.ids?.imdb
                        "episode" -> {
                            val showImdb = item.show?.ids?.imdb ?: continue
                            val ep = item.episode ?: continue
                            "$showImdb:${ep.season}:${ep.number}"
                        }
                        else -> null
                    } ?: continue

                    if (traktId == normalizedId) {
                        val playbackId = item.id ?: continue
                        val deleteResponse = traktSyncApi.deletePlaybackItem(playbackId)
                        Log.d(TAG, "Deleted playback $playbackId from Trakt: ${deleteResponse.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete playback from Trakt: ${e.message}")
            } finally {
                pendingDeletes.remove(normalizedId)
                pendingDeletes.remove(localId)
            }
        }
    }

    private suspend fun pullFromTrakt(items: List<TraktWatchlistItem>) {
        for (item in items) {
            val (id, title, type) = when (item.type) {
                "movie" -> Triple(
                    item.movie?.ids?.imdb ?: continue,
                    item.movie?.title ?: "Unknown",
                    "movie"
                )
                "show" -> Triple(
                    item.show?.ids?.imdb ?: continue,
                    item.show?.title ?: "Unknown",
                    "series"
                )
                else -> continue
            }

            dao.addToWatchlist(
                WatchlistEntity(
                    id = id,
                    type = type,
                    title = title,
                    poster = null,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
