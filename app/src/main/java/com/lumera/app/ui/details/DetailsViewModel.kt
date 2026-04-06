package com.lumera.app.ui.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.data.model.stremio.MetaVideo
import com.lumera.app.data.model.stremio.Stream
import com.lumera.app.data.model.StreamQuality
import com.lumera.app.data.player.PlaybackTrackSelectionStore
import com.lumera.app.data.player.SourceSelectionStore
import com.lumera.app.data.profile.ProfileConfigurationManager
import com.lumera.app.data.repository.AddonRepository
import com.lumera.app.data.repository.SubtitleRepository
import com.lumera.app.data.stream.StreamSortingService
import com.lumera.app.data.tmdb.TmdbEnrichment
import com.lumera.app.data.tmdb.TmdbEpisodeEnrichment
import com.lumera.app.data.tmdb.TmdbMetaPreview
import com.lumera.app.data.tmdb.TmdbMetadataService
import com.lumera.app.data.tmdb.TmdbService
import com.lumera.app.data.tmdb.TmdbVideoInfo
import com.lumera.app.domain.AddonSubtitle
import com.lumera.app.data.trakt.TraktSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import com.lumera.app.data.model.WatchHistoryEntity
import com.lumera.app.data.model.WatchlistEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val dao: AddonDao,
    private val sourceSelectionStore: SourceSelectionStore,
    private val playbackTrackSelectionStore: PlaybackTrackSelectionStore,
    private val repository: AddonRepository,
    private val subtitleRepository: SubtitleRepository,
    private val profileConfigurationManager: ProfileConfigurationManager,
    private val streamSortingService: StreamSortingService,
    private val tmdbService: TmdbService,
    private val tmdbMetadataService: TmdbMetadataService,
    private val traktSyncManager: TraktSyncManager
) : ViewModel() {

    /** Per-episode watch progress for the episodes sidebar. */
    data class EpisodeProgress(
        val progress: Float,  // 0.0–1.0
        val watched: Boolean
    )

    data class DetailsState(
        val meta: MetaItem? = null,
        val resolvedId: String? = null, // IMDb ID resolved from tmdb: prefixes, used for stream/subtitle fetching
        val contentKey: String? = null, // Tracks which item this state belongs to
        val isLoading: Boolean = true,
        val isLoadingStreams: Boolean = false,
        val resumePlaybackId: String? = null,
        val autoPlayStream: Stream? = null,
        val addonSubtitles: List<AddonSubtitle> = emptyList(),
        val availableStreams: List<Stream> = emptyList(),
        val sidebarState: SidebarState = SidebarState.Closed,
        val progressCleared: Boolean = false,
        val episodeProgressMap: Map<String, EpisodeProgress> = emptyMap(), // "S1:E3" → progress
        val episodeEnrichmentMap: Map<String, TmdbEpisodeEnrichment> = emptyMap(), // "S1:E3" → TMDB data
        // TMDB enrichment
        val tmdbEnabled: Boolean = false,
        val tmdbLoading: Boolean = false,
        val tmdbEnrichment: TmdbEnrichment? = null,
        val tmdbRecommendations: List<TmdbMetaPreview> = emptyList(),
        val tmdbVideos: List<TmdbVideoInfo> = emptyList(),
        val tmdbCollection: List<TmdbMetaPreview> = emptyList(),
        val tmdbCollectionName: String? = null
    )

    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state

    /** Reactive watchlist status — emits true/false as the current item's watchlist state changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val isInWatchlist: StateFlow<Boolean> = _state
        .map { it.resolvedId ?: it.meta?.id }
        .flatMapLatest { id -> if (id != null) dao.isInWatchlistFlow(id) else flowOf(false) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    private var loadDetailsJob: Job? = null
    private var loadStreamsJob: Job? = null
    private var tmdbEnrichmentJob: Job? = null
    private var loadRequestVersion: Long = 0L
    private var loadedContentKey: String? = null

    // Stash for undo: holds deleted history entries until the user leaves the screen
    private var clearedHistoryStash: List<WatchHistoryEntity> = emptyList()
    private var clearedResumeId: String? = null

    fun loadDetails(type: String, id: String, addonBaseUrl: String? = null) {
        val requestKey = "$type:$id"

        // Keep current details when reopening the same item (e.g., returning from player).
        if (
            loadedContentKey == requestKey &&
            _state.value.meta != null &&
            !_state.value.isLoading
        ) {
            refreshResumeStateIfNeeded(_state.value.meta)
            if (_state.value.sidebarState !is SidebarState.Closed) {
                _state.value = _state.value.copy(sidebarState = SidebarState.Closed)
            }
            return
        }

        loadDetailsJob?.cancel()
        loadRequestVersion += 1
        val requestVersion = loadRequestVersion

        // Reset immediately so previous movie details never flash for a new item.
        _state.value = DetailsState(
            isLoading = true,
            resumePlaybackId = null,
            autoPlayStream = null,
            addonSubtitles = emptyList(),
            availableStreams = emptyList(),
            sidebarState = SidebarState.Closed
        )

        loadDetailsJob = viewModelScope.launch {
            try {
                // Resolve tmdb: IDs to IMDb IDs via TMDB API so all addons work consistently
                val isTmdbEnabled = profileConfigurationManager.getLastActiveProfileId()
                    ?.let { dao.getProfileById(it) }?.tmdbEnabled == true
                val resolvedId = if (isTmdbEnabled && id.startsWith("tmdb:", ignoreCase = true)) {
                    val tmdbNumericId = id.substringAfter(':').substringBefore(':').toIntOrNull()
                    val mediaType = tmdbService.normalizeMediaType(type)
                    tmdbNumericId?.let { tmdbService.tmdbToImdb(it, mediaType) } ?: id
                } else id

                val details = repository.resolveMetaDetails(type, resolvedId, addonBaseUrl)
                    ?: throw Exception("No meta found")
                if (requestVersion != loadRequestVersion) return@launch
                loadedContentKey = requestKey
                // Use resolved ID for streams — guarantees IMDb format for stream addons
                val streamFetchId = if (details.id.startsWith("tt")) details.id else resolvedId
                val resumePlaybackId = if (details.type == "series") {
                    dao.getLatestSeriesEpisodeHistory("${streamFetchId}:%")?.id
                } else {
                    dao.getHistoryItem(streamFetchId)?.id
                }
                // Build per-episode progress map for the episodes sidebar
                val episodeProgressMap = if (details.type == "series") {
                    buildEpisodeProgressMap(streamFetchId)
                } else emptyMap()

                _state.value = _state.value.copy(
                    meta = details,
                    resolvedId = streamFetchId,
                    contentKey = requestKey,
                    isLoading = false,
                    resumePlaybackId = resumePlaybackId,
                    episodeProgressMap = episodeProgressMap,
                    autoPlayStream = null,
                    addonSubtitles = emptyList(),
                    availableStreams = emptyList(),
                    tmdbEnrichment = null,
                    tmdbRecommendations = emptyList(),
                    tmdbVideos = emptyList(),
                    tmdbCollection = emptyList(),
                    tmdbCollectionName = null
                )
                // Fire TMDB enrichment in background (non-blocking)
                loadTmdbEnrichment(details.type, streamFetchId, requestKey)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (requestVersion != loadRequestVersion) return@launch
                loadedContentKey = null
                _state.value = _state.value.copy(
                    meta = null,
                    isLoading = false,
                    resumePlaybackId = null,
                    autoPlayStream = null,
                    addonSubtitles = emptyList(),
                    availableStreams = emptyList()
                )
            }
        }
    }

    private fun refreshResumeStateIfNeeded(meta: MetaItem?) {
        if (meta == null) {
            if (_state.value.resumePlaybackId != null) {
                _state.value = _state.value.copy(resumePlaybackId = null)
            }
            return
        }

        viewModelScope.launch {
            val resumePlaybackId = if (meta.type == "series") {
                dao.getLatestSeriesEpisodeHistory("${meta.id}:%")?.id
            } else {
                dao.getHistoryItem(meta.id)?.id
            }
            if (_state.value.meta?.id == meta.id && _state.value.meta?.type == meta.type) {
                val episodeProgressMap = if (meta.type == "series") {
                    buildEpisodeProgressMap(meta.id)
                } else emptyMap()
                _state.value = _state.value.copy(
                    resumePlaybackId = resumePlaybackId,
                    autoPlayStream = null,
                    episodeProgressMap = episodeProgressMap
                )
            }
        }
    }

    fun refreshResumeState() {
        refreshResumeStateIfNeeded(_state.value.meta)
    }

    /**
     * Build a map of "S{season}:E{episode}" → EpisodeProgress from watch history.
     * Checks both with and without stream index suffix.
     */
    private suspend fun buildEpisodeProgressMap(seriesId: String): Map<String, EpisodeProgress> {
        val historyItems = dao.getSeriesEpisodeHistory("$seriesId:%")
        if (historyItems.isEmpty()) return emptyMap()

        val map = mutableMapOf<String, EpisodeProgress>()
        for (item in historyItems) {
            val parts = item.id.split(":")
            if (parts.size < 3) continue
            // Extract season and episode from the playback ID
            val hasStreamIndex = parts.size >= 4 && parts.last().toIntOrNull() != null
            val season = parts[parts.size - if (hasStreamIndex) 3 else 2].toIntOrNull() ?: continue
            val episode = parts[parts.size - if (hasStreamIndex) 2 else 1].toIntOrNull() ?: continue
            val key = "S${season}:E${episode}"

            // Keep the most recent entry if there are duplicates
            val existing = map[key]
            if (existing == null || (!existing.watched && item.watched)) {
                map[key] = EpisodeProgress(
                    progress = item.progress(),
                    watched = item.watched
                )
            }
        }
        return map
    }

    private fun loadTmdbEnrichment(type: String, videoId: String, contentKey: String) {
        tmdbEnrichmentJob?.cancel()
        tmdbEnrichmentJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if TMDB is enabled for the active profile
                val profileId = profileConfigurationManager.getLastActiveProfileId()
                val profile = profileId?.let { dao.getProfileById(it) }
                if (profile?.tmdbEnabled != true) {
                    _state.value = _state.value.copy(tmdbEnabled = false, tmdbLoading = false)
                    return@launch
                }

                _state.value = _state.value.copy(tmdbEnabled = true, tmdbLoading = true)

                val language = profile.tmdbLanguage.ifBlank { null } ?: "en"
                val mediaType = tmdbService.normalizeMediaType(type)

                // Resolve TMDB ID — if unresolvable (e.g. Kitsu IDs), stop loading and show addon data
                val tmdbId = tmdbService.ensureTmdbId(videoId, mediaType)
                if (tmdbId == null) {
                    _state.value = _state.value.copy(tmdbLoading = false)
                    return@launch
                }

                // Fetch enrichment, recommendations, and videos in parallel
                val enrichmentDeferred = async { tmdbMetadataService.fetchEnrichment(tmdbId, mediaType, language) }
                val recommendationsDeferred = async { tmdbMetadataService.fetchRecommendations(tmdbId, mediaType, language) }
                val videosDeferred = async { tmdbMetadataService.fetchVideos(tmdbId, mediaType, language) }

                val enrichment = enrichmentDeferred.await()
                val recommendations = recommendationsDeferred.await()
                val videos = videosDeferred.await()

                // Fetch collection if available (movies only)
                val collection = if (enrichment?.collectionId != null) {
                    tmdbMetadataService.fetchCollection(enrichment.collectionId, language)
                } else emptyList()

                // Only update if we're still showing the same content
                if (_state.value.contentKey != contentKey) return@launch

                // Apply enrichment — overlay TMDB data onto existing metadata where it adds value
                val currentMeta = _state.value.meta
                val enrichedMeta = if (currentMeta != null && enrichment != null) {
                    currentMeta.copy(
                        // Localized title
                        name = enrichment.localizedTitle ?: currentMeta.name,
                        // Localized description
                        description = enrichment.description ?: currentMeta.description,
                        // Better images
                        logo = enrichment.logo ?: currentMeta.logo,
                        background = enrichment.backdrop ?: currentMeta.background,
                        poster = enrichment.poster ?: currentMeta.poster,
                        // Localized genres
                        genres = enrichment.genres.ifEmpty { currentMeta.genres },
                        // Release info
                        releaseInfo = enrichment.releaseInfo ?: currentMeta.releaseInfo,
                        // Rating from TMDB
                        imdbRating = enrichment.rating?.let {
                            String.format("%.1f", it)
                        } ?: currentMeta.imdbRating,
                        // Runtime
                        runtime = enrichment.runtimeMinutes?.let { "${it}m" } ?: currentMeta.runtime
                    )
                } else currentMeta

                // Fetch per-episode enrichment for series (synopsis, runtime, thumbnails)
                val episodeEnrichmentMap = if (mediaType == "tv" && tmdbId != null) {
                    val seasons = enrichedMeta?.videos
                        ?.filter { it.season > 0 }
                        ?.map { it.season }
                        ?.distinct() ?: emptyList()
                    if (seasons.isNotEmpty()) {
                        val raw = tmdbMetadataService.fetchEpisodeEnrichment(tmdbId, seasons, language)
                        raw.mapKeys { (key, _) -> "S${key.first}:E${key.second}" }
                    } else emptyMap()
                } else emptyMap()

                _state.value = _state.value.copy(
                    meta = enrichedMeta,
                    tmdbLoading = false,
                    tmdbEnrichment = enrichment,
                    tmdbRecommendations = recommendations,
                    tmdbVideos = videos,
                    tmdbCollection = collection,
                    tmdbCollectionName = enrichment?.collectionName,
                    episodeEnrichmentMap = episodeEnrichmentMap
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DetailsViewModel", "TMDB enrichment failed: ${e.message}")
                _state.value = _state.value.copy(tmdbLoading = false)
            }
        }
    }

    // ── Mark episode watched/unwatched ──

    fun toggleEpisodeWatched(episode: MetaVideo) {
        val meta = _state.value.meta ?: return
        val streamId = _state.value.resolvedId ?: meta.id
        val key = "S${episode.season}:E${episode.episode}"
        val currentProgress = _state.value.episodeProgressMap[key]
        val isCurrentlyWatched = currentProgress?.watched ?: false

        viewModelScope.launch(Dispatchers.IO) {
            if (isCurrentlyWatched) {
                // Unmark: remove watched entry from history
                val playbackId = "$streamId:${episode.season}:${episode.episode}"
                dao.deleteHistoryItem(playbackId)
                // Also try with stream index variants
                dao.getSeriesEpisodeHistory("$playbackId:%").forEach {
                    dao.deleteHistoryItem(it.id)
                }
                traktSyncManager.pushEpisodeUnwatched(streamId, episode.season, episode.episode)
            } else {
                // Mark as watched: create a watched history entry
                val playbackId = "$streamId:${episode.season}:${episode.episode}"
                dao.upsertHistory(
                    WatchHistoryEntity(
                        id = playbackId,
                        title = episode.title.takeIf { it.isNotBlank() && it != "Episode" }
                            ?: "S${episode.season}:E${episode.episode} - ${meta.name}",
                        poster = meta.poster,
                        position = 0L,
                        duration = 0L,
                        lastWatched = System.currentTimeMillis(),
                        type = "series",
                        watched = true,
                        scrobbled = true
                    )
                )
                traktSyncManager.pushEpisodeWatched(streamId, episode.season, episode.episode)
            }

            // Refresh the progress map
            val updatedMap = buildEpisodeProgressMap(streamId)
            _state.value = _state.value.copy(episodeProgressMap = updatedMap)
        }
    }

    // 1. Open Episodes (Series)
    fun openEpisodes() {
        val videos = _state.value.meta?.videos ?: emptyList()
        _state.value = _state.value.copy(
            autoPlayStream = null,
            addonSubtitles = emptyList(),
            availableStreams = emptyList(),
            sidebarState = SidebarState.Episodes(videos)
        )
    }

    // 2. Open Sources (Movie OR Specific Episode)
    fun loadStreams(
        type: String,
        id: String,
        displayTitle: String,
        sourceSelectionId: String = id,
        forceSourcePicker: Boolean = false,
        autoSelectSource: Boolean = false,
        rememberSourceSelection: Boolean = true
    ) {
        loadStreamsJob?.cancel()
        loadStreamsJob = viewModelScope.launch {
            // Show immediate loading feedback:
            // - Show sources sidebar when user needs to pick manually
            // - Centered spinner when auto-resolve is expected (auto-select or remembered source)
            val hasRemembered = rememberSourceSelection && sourceSelectionStore.hasRememberedSelection(sourceSelectionId)
            val showSidebar = forceSourcePicker || (!autoSelectSource && !hasRemembered)

            _state.value = _state.value.copy(
                autoPlayStream = null,
                addonSubtitles = emptyList(),
                availableStreams = emptyList(),
                isLoadingStreams = true,
                sidebarState = if (showSidebar) SidebarState.Sources(displayTitle, null)
                               else SidebarState.Closed
            )

            try {
                val streamsDeferred = async { repository.getStreams(type, id) }
                val subtitlesDeferred = async { subtitleRepository.getSubtitles(type, id) }

                val rawStreams = streamsDeferred.await()
                val addonSubtitles = subtitlesDeferred.await()

                // Read sorting preferences from the active profile
                val activeProfileId = profileConfigurationManager.getLastActiveProfileId()
                val profile = activeProfileId?.let { dao.getProfileById(it) }

                val streams = if (profile?.sourceSortingEnabled != false) {
                    val enabledQualities = StreamSortingService.parseEnabledQualities(profile?.sourceEnabledQualities ?: "4k,1080p,720p,unknown")
                    val excludePhrases = StreamSortingService.parseExcludePhrases(profile?.sourceExcludePhrases ?: "")
                    val addonSortOrders = dao.getAllAddons().firstOrNull()
                        ?.associate { it.transportUrl to it.sortOrder } ?: emptyMap()
                    val excludedFormats = StreamSortingService.parseExcludedFormats(profile?.sourceExcludedFormats ?: "")
                    streamSortingService.sortAndFilter(rawStreams, enabledQualities, excludePhrases, addonSortOrders, profile?.sourceSortPrimary ?: "quality", profile?.sourceMaxSizeGb ?: 0, excludedFormats)
                } else rawStreams

                val preferredStream = if (forceSourcePicker || !rememberSourceSelection) {
                    null
                } else {
                    sourceSelectionStore.findPreferredStream(sourceSelectionId, streams)
                }

                if (preferredStream != null) {
                    _state.value = _state.value.copy(
                        isLoadingStreams = false,
                        sidebarState = SidebarState.Closed,
                        autoPlayStream = preferredStream,
                        addonSubtitles = addonSubtitles,
                        availableStreams = streams
                    )
                    return@launch
                }

                // Auto-select first playable source when enabled
                if (autoSelectSource && !forceSourcePicker) {
                    val firstPlayable = streams.firstOrNull {
                        !it.url.isNullOrBlank() || !it.infoHash.isNullOrBlank()
                    }
                    if (firstPlayable != null) {
                        _state.value = _state.value.copy(
                            isLoadingStreams = false,
                            sidebarState = SidebarState.Closed,
                            autoPlayStream = firstPlayable,
                            addonSubtitles = addonSubtitles,
                            availableStreams = streams
                        )
                        return@launch
                    }
                }

                // Update sidebar with results
                _state.value = _state.value.copy(
                    isLoadingStreams = false,
                    autoPlayStream = null,
                    addonSubtitles = addonSubtitles,
                    availableStreams = streams,
                    sidebarState = SidebarState.Sources(displayTitle, streams)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingStreams = false,
                    autoPlayStream = null,
                    addonSubtitles = emptyList(),
                    availableStreams = emptyList(),
                    sidebarState = SidebarState.Sources(displayTitle, emptyList())
                )
            }
        }
    }

    fun consumeAutoPlayStream() {
        if (_state.value.autoPlayStream == null) return
        _state.value = _state.value.copy(autoPlayStream = null)
    }

    // --- Clear Progress (with undo) ---

    fun clearProgress() {
        val meta = _state.value.meta ?: return
        val currentResumeId = _state.value.resumePlaybackId ?: return
        clearedResumeId = currentResumeId

        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            clearedHistoryStash = if (meta.type == "series") {
                dao.getSeriesEpisodeHistory("${meta.id}:%")
            } else {
                listOfNotNull(dao.getHistoryItem(meta.id))
            }

            // Mark scrobbled items as pending delete so the sync poll doesn't re-add them
            val scrobbledIds = clearedHistoryStash.filter { it.scrobbled }.map { it.id }
            if (scrobbledIds.isNotEmpty()) {
                traktSyncManager.markPendingDelete(scrobbledIds)
            }

            if (meta.type == "series") {
                dao.deleteSeriesHistory("${meta.id}:%")
                sourceSelectionStore.clearSelectionsForPrefix(meta.id)
                playbackTrackSelectionStore.clearSelectionsForPrefix(meta.id)
            } else {
                dao.deleteHistoryItem(meta.id)
                sourceSelectionStore.clearSelection(meta.id)
                playbackTrackSelectionStore.clearSelection(meta.id)
            }

            profileConfigurationManager.saveActiveRuntimeState()

            _state.value = _state.value.copy(
                resumePlaybackId = null,
                progressCleared = true
            )
        }
    }

    fun undoClearProgress() {
        if (clearedHistoryStash.isEmpty()) return
        val stash = clearedHistoryStash
        val resumeId = clearedResumeId

        // Unmark from pending deletes so sync poll can see them again
        val scrobbledIds = stash.filter { it.scrobbled }.map { it.id }
        if (scrobbledIds.isNotEmpty()) {
            traktSyncManager.unmarkPendingDelete(scrobbledIds)
        }

        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            dao.upsertHistoryItems(stash)
            profileConfigurationManager.saveActiveRuntimeState()
            clearedHistoryStash = emptyList()
            clearedResumeId = null

            _state.value = _state.value.copy(
                resumePlaybackId = resumeId,
                progressCleared = false
            )
        }
    }

    /** Drop the undo stash so progress deletion becomes permanent. (Fix 4: Trakt deletion deferred to here) */
    fun commitClearProgress() {
        val stash = clearedHistoryStash
        clearedHistoryStash = emptyList()
        clearedResumeId = null
        if (_state.value.progressCleared) {
            _state.value = _state.value.copy(progressCleared = false)
        }
        // Now that the user can no longer undo, delete from Trakt
        if (stash.any { it.scrobbled }) {
            viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                for (item in stash) {
                    if (item.scrobbled) {
                        traktSyncManager.deletePlaybackFromTrakt(item.id)
                    }
                }
            }
        }
    }

    // --- Watchlist toggle ---

    fun toggleWatchlist() {
        val meta = _state.value.meta ?: return
        val itemId = _state.value.resolvedId ?: meta.id
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.isInWatchlist(itemId)) {
                dao.removeFromWatchlist(itemId)
                traktSyncManager.pushRemove(itemId, meta.type)
            } else {
                val entity = WatchlistEntity(
                    id = itemId,
                    type = meta.type,
                    title = meta.name,
                    poster = meta.poster,
                    addedAt = System.currentTimeMillis()
                )
                dao.addToWatchlist(entity)
                traktSyncManager.pushAdd(entity)
            }
        }
    }

    // 3. Close Logic
    fun closeSidebar() {
        loadStreamsJob?.cancel()
        loadStreamsJob = null
        _state.value = _state.value.copy(
            isLoadingStreams = false,
            autoPlayStream = null,
            availableStreams = emptyList(),
            sidebarState = SidebarState.Closed
        )
    }

    // 4. Back Button Logic (Drill Up)
    fun goBackInSidebar() {
        val currentState = _state.value.sidebarState

        // If viewing Sources for a Series, go back to Episode List
        if (currentState is SidebarState.Sources && _state.value.meta?.type == "series") {
            openEpisodes()
        } else {
            closeSidebar()
        }
    }
}
