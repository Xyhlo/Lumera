package com.lumera.app.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.WatchlistEntity
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.data.profile.ProfileConfigurationManager
import com.lumera.app.data.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val dao: AddonDao,
    private val repository: AddonRepository,
    private val profileConfigurationManager: ProfileConfigurationManager
) : ViewModel() {

    private val resolveInFlight = mutableSetOf<String>()

    var lastFocusedKey: String? = null

    val movieRowState = androidx.compose.foundation.lazy.LazyListState()
    val seriesRowState = androidx.compose.foundation.lazy.LazyListState()

    val movieItems: StateFlow<List<MetaItem>> = profileConfigurationManager.activeProfileId
        .flatMapLatest { profileId -> dao.getWatchlistByType(profileId, "movie") }
        .map { list -> list.map { it.toMetaItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesItems: StateFlow<List<MetaItem>> = profileConfigurationManager.activeProfileId
        .flatMapLatest { profileId -> dao.getWatchlistByType(profileId, "series") }
        .map { list -> list.map { it.toMetaItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Resolve poster from addons for items missing one (e.g., pulled from Trakt).
     * Updates the DB so the poster persists — the Flow will re-emit automatically.
     */
    fun resolvePosterIfNeeded(item: MetaItem) {
        if (!item.poster.isNullOrBlank()) return
        if (!resolveInFlight.add(item.id)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profileId = profileConfigurationManager.requireActiveProfileId()
                val meta = repository.resolveMetaDetails(item.type, item.id) ?: return@launch
                if (!meta.poster.isNullOrBlank()) {
                    val existing = dao.getWatchlistItem(item.id, profileId) ?: return@launch
                    dao.addToWatchlist(existing.copy(poster = meta.poster))
                }
            } catch (_: Exception) {
            } finally {
                resolveInFlight.remove(item.id)
            }
        }
    }
}

private fun WatchlistEntity.toMetaItem() = MetaItem(
    id = id,
    type = type,
    name = title,
    poster = poster
)
