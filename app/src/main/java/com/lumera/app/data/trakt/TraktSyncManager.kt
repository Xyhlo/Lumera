package com.lumera.app.data.trakt

import android.util.Log
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.WatchlistEntity
import com.lumera.app.data.model.trakt.TraktIds
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

    // Last known watchlist activity timestamp from Trakt.
    // When this changes, we know the watchlist was modified externally.
    private var lastWatchlistActivity: String? = null

    /**
     * Lightweight check: queries /sync/last_activities and only runs a full
     * sync if the watchlist timestamp has changed since we last checked.
     * Returns true if a sync was triggered.
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
                val currentTimestamp = activities.watchlist?.updatedAt

                if (currentTimestamp != null && currentTimestamp != lastWatchlistActivity) {
                    Log.d(TAG, "Watchlist activity changed: $lastWatchlistActivity → $currentTimestamp")
                    lastWatchlistActivity = currentTimestamp
                    syncWatchlist()
                    true
                } else {
                    false
                }
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

    /** Reset stored activity timestamp (e.g., on profile switch). */
    fun resetActivityState() {
        lastWatchlistActivity = null
    }

    // ── Internal ──

    private suspend fun fetchAllTraktWatchlist(): List<TraktWatchlistItem>? {
        val allItems = mutableListOf<TraktWatchlistItem>()
        var page = 1

        while (true) {
            val response = traktSyncApi.getWatchlist(page = page, limit = 100)
            if (!response.isSuccessful) {
                Log.w(TAG, "Trakt watchlist fetch failed: ${response.code()}")
                return if (allItems.isNotEmpty()) allItems else null
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
