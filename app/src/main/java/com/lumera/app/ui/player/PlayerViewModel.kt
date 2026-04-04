package com.lumera.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.WatchHistoryEntity
import com.lumera.app.data.profile.ProfileConfigurationManager
import com.lumera.app.data.trakt.TraktScrobbleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val dao: AddonDao,
    private val profileConfigurationManager: ProfileConfigurationManager,
    private val traktScrobbleManager: TraktScrobbleManager
) : ViewModel() {

    fun saveProgress(
        id: String,
        type: String,
        title: String,
        poster: String?,
        position: Long,
        duration: Long?
    ) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            if (id.startsWith("trailer_")) return@launch
            val safePosition = position.coerceAtLeast(0L)
            if (safePosition < 5_000L) return@launch

            val existing = dao.getHistoryItem(id)
            val safeDuration = (duration ?: existing?.duration ?: safePosition)
                .coerceAtLeast(safePosition)

            val remaining = safeDuration - safePosition
            val completionRatio = if (safeDuration > 0L) safePosition.toDouble() / safeDuration.toDouble() else 0.0

            // Read configurable threshold from active profile (default 85%)
            val profileId = profileConfigurationManager.getLastActiveProfileId()
            val threshold = profileId?.let { dao.getProfileById(it) }?.watchedThreshold ?: 85
            val isCompleted = completionRatio >= (threshold / 100.0) || remaining <= 30_000L

            val entry = WatchHistoryEntity(
                id = id,
                title = title,
                poster = poster,
                position = safePosition,
                duration = safeDuration,
                lastWatched = System.currentTimeMillis(),
                type = type.ifBlank { "movie" },
                watched = isCompleted
            )
            dao.upsertHistory(entry)
        }
    }

    fun markCompleted(id: String) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            val existing = dao.getHistoryItem(id)
            if (existing != null) {
                dao.upsertHistory(existing.copy(watched = true, lastWatched = System.currentTimeMillis()))
            }
        }
    }

    // ── Trakt Scrobbling ──

    fun scrobbleStart(id: String, type: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            traktScrobbleManager.scrobbleStart(id, type, positionMs, durationMs)
        }
    }

    fun scrobblePause(id: String, type: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            traktScrobbleManager.scrobblePause(id, type, positionMs, durationMs)
        }
    }

    fun scrobbleStop(id: String, type: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            traktScrobbleManager.scrobbleStop(id, type, positionMs, durationMs)
        }
    }

    suspend fun getResumePosition(id: String): Long {
        return withContext(Dispatchers.IO) {
            val item = dao.getHistoryItem(id)
            item?.position?.takeIf { it > 0 } ?: 0L
        }
    }
}
