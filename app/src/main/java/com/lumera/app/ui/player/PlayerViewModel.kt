package com.lumera.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.WatchHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val dao: AddonDao
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
            val isCompleted = completionRatio >= 0.98 || remaining <= 30_000L

            if (isCompleted) {
                dao.deleteHistoryItem(id)
                return@launch
            }

            val entry = WatchHistoryEntity(
                id = id,
                title = title,
                poster = poster,
                position = safePosition,
                duration = safeDuration,
                lastWatched = System.currentTimeMillis(),
                type = type.ifBlank { "movie" }
            )
            dao.upsertHistory(entry)
        }
    }

    fun markCompleted(id: String) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            dao.deleteHistoryItem(id)
        }
    }

    suspend fun getResumePosition(id: String): Long {
        return withContext(Dispatchers.IO) {
            val item = dao.getHistoryItem(id)
            item?.position?.takeIf { it > 0 } ?: 0L
        }
    }
}
