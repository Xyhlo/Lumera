package com.lumera.app.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.lumera.app.domain.AddonSubtitle
import com.lumera.app.domain.episodePlaybackId
import com.lumera.app.domain.episodeStreamId
import com.lumera.app.domain.episodeDisplayTitle
import com.lumera.app.data.model.stremio.MetaVideo
import com.lumera.app.data.model.stremio.Stream
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.lumera.app.R
import com.lumera.app.ui.home.FocusPivotSpec
import com.lumera.app.data.tmdb.TmdbCastInfo
import com.lumera.app.data.tmdb.TmdbCompanyInfo
import com.lumera.app.data.tmdb.TmdbMetaPreview
import com.lumera.app.data.tmdb.TmdbVideoInfo

@Composable
fun DetailsScreen(
    type: String,
    id: String,
    addonBaseUrl: String? = null,
    resumePlaybackHint: String? = null,
    autoSelectSource: Boolean = false,
    rememberSourceSelection: Boolean = true,
    onPlayClick: (String, String, String, String, String, String, Stream, List<AddonSubtitle>, List<Stream>, List<MetaVideo>) -> Unit,
    onNavigateToDetails: (type: String, id: String) -> Unit = { _, _ -> },
    viewModel: DetailsViewModel = hiltViewModel(key = "details_${type}_${id}")
) {
    LaunchedEffect(type, id) { viewModel.loadDetails(type, id, addonBaseUrl) }

    val state by viewModel.state.collectAsState()
    val movie = state.meta
    val streamId = state.resolvedId ?: movie?.id ?: id // Resolved IMDb ID for stream/subtitle requests
    // Check contentKey to prevent stale content from the previous item flashing for one frame.
    // contentKey is set when meta loads and matches "$type:$id" of the navigation params.
    val isCurrentMovie = movie != null && !state.isLoading && state.contentKey == "$type:$id"
    val showMovieContent = isCurrentMovie
    val sidebarState = if (isCurrentMovie) state.sidebarState else SidebarState.Closed

    val accentColor = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val lifecycleOwner = LocalLifecycleOwner.current

    var pendingPlaybackId by remember(type, id) { mutableStateOf(id) }
    var pendingPlaybackType by remember(type, id) { mutableStateOf(type) }
    var pendingPlaybackTitle by remember(type, id) { mutableStateOf("") }
    val autoPlayStream = state.autoPlayStream
    val addonSubtitles = state.addonSubtitles
    val availableStreams = state.availableStreams

    LaunchedEffect(autoPlayStream) {
        val stream = autoPlayStream ?: return@LaunchedEffect
        viewModel.commitClearProgress()
        val urlToPlay = resolvePlayableUrl(stream)
        if (!urlToPlay.isNullOrEmpty()) {
            val playbackId = pendingPlaybackId.ifBlank { movie?.id ?: id }
            val playbackType = pendingPlaybackType.ifBlank { movie?.type ?: type }
            val playbackTitle = pendingPlaybackTitle.ifBlank { movie?.name ?: "" }
            onPlayClick(
                urlToPlay,
                playbackId,
                playbackType,
                playbackTitle,
                movie?.name ?: "",
                movie?.logo ?: "",
                stream,
                addonSubtitles,
                availableStreams,
                movie?.videos ?: emptyList()
            )
        }
        viewModel.consumeAutoPlayStream()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshResumeState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.commitClearProgress()
        }
    }

    BackHandler(enabled = sidebarState !is SidebarState.Closed) {
        viewModel.goBackInSidebar()
    }

    val firstButtonFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val tmdbPending = state.tmdbEnabled && state.tmdbLoading
    val contentReady = showMovieContent && !tmdbPending

    // Track whether focus is inside the hero area (any button).
    // While hero has focus, suppress vertical pivot scrolling (viewport stays fixed,
    // just like the hero carousel on the home screen).
    var heroHasFocus by remember { mutableStateOf(false) }

    // Vertical pivot for smooth row-to-row scrolling (matches home screen SimpleLayout)
    val density = LocalDensity.current
    @OptIn(ExperimentalFoundationApi::class)
    val verticalPivot = remember(density) {
        val pivotPx = with(density) { 71.dp.toPx() }
        FocusPivotSpec(
            customOffset = pivotPx,
            skipScrollProvider = { heroHasFocus },
            stiffnessProvider = { Spring.StiffnessLow }
        )
    }

    // When focus returns to the hero from a row, animate back to the top
    LaunchedEffect(heroHasFocus) {
        if (heroHasFocus && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(contentReady) {
        if (contentReady) {
            kotlinx.coroutines.delay(200)
            runCatching { firstButtonFocusRequester.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        if (!showMovieContent || tmdbPending) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentColor)
        }
        if (showMovieContent && !tmdbPending) {
            val currentMovie = requireNotNull(movie)
            val bgImage = currentMovie.background ?: currentMovie.poster
            AsyncImage(
                model = bgImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.6f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to bg,
                                0.1f to bg.copy(alpha = 0.95f),
                                0.2f to bg.copy(alpha = 0.85f),
                                0.3f to bg.copy(alpha = 0.72f),
                                0.4f to bg.copy(alpha = 0.58f),
                                0.55f to bg.copy(alpha = 0.38f),
                                0.7f to bg.copy(alpha = 0.20f),
                                0.85f to bg.copy(alpha = 0.08f),
                                1.0f to Color.Transparent
                            ),
                            startX = 0f,
                            endX = 1500f
                        )
                    )
            )
            com.lumera.app.ui.components.NoiseOverlay()

            val enrichment = state.tmdbEnrichment
            val hasEnrichment = enrichment != null

            @OptIn(ExperimentalFoundationApi::class)
            CompositionLocalProvider(LocalBringIntoViewSpec provides verticalPivot) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
            // ── Hero item (fixed height, scroll-suppressed) ──
            item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(start = 48.dp, end = 48.dp, top = 60.dp, bottom = 24.dp)
                    .onFocusChanged { heroHasFocus = it.hasFocus },
                verticalArrangement = Arrangement.Bottom
            ) {
                val titleStyle = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    lineHeight = 34.sp
                )

                if (!currentMovie.logo.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = currentMovie.logo,
                        contentDescription = currentMovie.name,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomStart,
                        modifier = Modifier
                            .widthIn(max = 450.dp)
                            .heightIn(max = 90.dp),
                        error = {
                            Text(
                                text = currentMovie.name,
                                style = titleStyle,
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = true
                            )
                        }
                    )
                } else {
                    Text(
                        text = currentMovie.name,
                        style = titleStyle,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val typeLabel = currentMovie.type.replaceFirstChar { it.uppercase() }
                val genreLabel = currentMovie.genres
                    ?.firstOrNull()
                    ?.replaceFirstChar { it.uppercase() }
                    ?: "Unknown"
                val yearLabel = extractPrimaryYear(currentMovie.releaseInfo)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = textColor.copy(alpha = 0.95f)
                    )
                    MetaDot(textColor)
                    Text(
                        text = genreLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = textColor.copy(alpha = 0.95f)
                    )
                    MetaDot(textColor)
                    Text(
                        text = yearLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = textColor.copy(alpha = 0.95f)
                    )

                    currentMovie.imdbRating?.takeIf { it.isNotBlank() }?.let { rating ->
                        Spacer(modifier = Modifier.width(4.dp))
                        ImdbBadge()
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = textColor.copy(alpha = 0.95f)
                        )
                    }
                }

                // Age rating + runtime from TMDB
                val ageRating = enrichment?.ageRating
                val runtimeMin = enrichment?.runtimeMinutes
                if (ageRating != null || runtimeMin != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ageRating?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = textColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        runtimeMin?.let {
                            val hours = it / 60
                            val mins = it % 60
                            val display = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            Text(
                                text = display,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = currentMovie.description ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(32.dp))

                val firstEpisode = remember(currentMovie.id, currentMovie.videos) {
                    findFirstEpisode(currentMovie.videos)
                }
                val hintedResumePlaybackId = remember(type, id, resumePlaybackHint) {
                    when (type) {
                        "series" -> resumePlaybackHint?.takeIf { playbackIdBelongsToSeries(id, it) }
                        else -> resumePlaybackHint?.takeIf { it == id }
                    }
                }
                val resumePlaybackId = if (state.progressCleared) null
                    else hintedResumePlaybackId ?: state.resumePlaybackId
                val resumeEpisode = remember(currentMovie.id, currentMovie.videos, resumePlaybackId) {
                    if (type == "series") {
                        resolveEpisodeForPlaybackId(currentMovie.id, currentMovie.videos, resumePlaybackId)
                    } else {
                        null
                    }
                }
                val parsedResumeSeasonEpisode = remember(resumePlaybackId) {
                    parseSeasonEpisodeFromPlaybackId(resumePlaybackId)
                }
                val firstEpisodeSeason = firstEpisode?.season?.takeIf { it > 0 } ?: 1
                val firstEpisodeNumber = firstEpisode?.episode?.takeIf { it > 0 } ?: 1

                // No onNavigateDown — Compose's default DOWN navigation
                // handles hero→row transitions reliably after disposal/recomposition.

                if (type == "series") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val playLabel = if (resumePlaybackId != null) {
                            val resumeSeason = resumeEpisode?.season?.takeIf { it > 0 } ?: parsedResumeSeasonEpisode?.first
                            val resumeNumber = resumeEpisode?.episode?.takeIf { it > 0 } ?: parsedResumeSeasonEpisode?.second
                            if (resumeSeason != null && resumeNumber != null) {
                                "Resume S${resumeSeason} E${resumeNumber}"
                            } else {
                                "Resume"
                            }
                        } else {
                            "Play S${firstEpisodeSeason} E${firstEpisodeNumber}"
                        }

                        VoidActionButton(
                            label = playLabel,
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.focusRequester(firstButtonFocusRequester),
                            onClick = {
                                val ep = resumeEpisode ?: firstEpisode ?: return@VoidActionButton
                                val trackId = resumePlaybackId ?: episodePlaybackId(streamId, ep)
                                val epStreamId = episodeStreamId(streamId, ep)
                                val epTitle = when {
                                    resumePlaybackId != null && resumeEpisode != null -> episodeDisplayTitle(resumeEpisode)
                                    resumePlaybackId != null && parsedResumeSeasonEpisode != null ->
                                        "S${parsedResumeSeasonEpisode.first}:E${parsedResumeSeasonEpisode.second} - ${currentMovie.name}"
                                    resumePlaybackId != null -> currentMovie.name
                                    else -> episodeDisplayTitle(ep)
                                }
                                pendingPlaybackId = trackId
                                pendingPlaybackType = type
                                pendingPlaybackTitle = epTitle
                                viewModel.loadStreams(type, epStreamId, epTitle, sourceSelectionId = trackId, autoSelectSource = autoSelectSource, rememberSourceSelection = rememberSourceSelection)
                            }
                        )

                        VoidActionButton(
                            label = "More Episodes",
                            icon = Icons.Default.List,
                            onClick = { viewModel.openEpisodes() }
                        )

                        if (resumePlaybackId != null || state.progressCleared) {
                            VoidActionButton(
                                label = if (state.progressCleared) "Undo" else "Clear Progress",
                                icon = if (state.progressCleared) Icons.Default.Refresh else Icons.Default.Close,
                                onClick = {
                                    if (state.progressCleared) {
                                        viewModel.undoClearProgress()
                                    } else {
                                        viewModel.clearProgress()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoidActionButton(
                            label = if (resumePlaybackId != null) "Resume" else "Play Movie",
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.focusRequester(firstButtonFocusRequester),
                            onClick = {
                                pendingPlaybackId = streamId
                                pendingPlaybackType = type
                                pendingPlaybackTitle = currentMovie.name
                                viewModel.loadStreams(type, streamId, currentMovie.name, autoSelectSource = autoSelectSource, rememberSourceSelection = rememberSourceSelection)
                            }
                        )

                        if (resumePlaybackId != null || state.progressCleared) {
                            VoidActionButton(
                                label = if (state.progressCleared) "Undo" else "Clear Progress",
                                icon = if (state.progressCleared) Icons.Default.Refresh else Icons.Default.Close,
                                onClick = {
                                    if (state.progressCleared) {
                                        viewModel.undoClearProgress()
                                    } else {
                                        viewModel.clearProgress()
                                    }
                                }
                            )
                        }

                        // Works around a Compose focus-tree bug where requestFocus()
                        // silently fails when a container has a single focusable child.
                        if (resumePlaybackId == null && !state.progressCleared) {
                            Spacer(modifier = Modifier
                                .size(0.dp)
                                .onFocusChanged {
                                    if (it.isFocused) firstButtonFocusRequester.requestFocus()
                                }
                                .focusable()
                            )
                        }
                    }
                }
            }
            } // hero item

            // ── TMDB Enrichment Sections ──
            if (hasEnrichment) {
                val castMembers = enrichment?.castMembers.orEmpty()
                val directorMembers = enrichment?.directorMembers.orEmpty()
                val writerMembers = enrichment?.writerMembers.orEmpty()
                val companies = enrichment?.productionCompanies.orEmpty()
                val networks = enrichment?.networks.orEmpty()
                val tmdbVideos = state.tmdbVideos
                val tmdbRecommendations = state.tmdbRecommendations
                val tmdbCollection = state.tmdbCollection

                val leadingCrew = directorMembers + writerMembers
                if (castMembers.isNotEmpty() || leadingCrew.isNotEmpty()) {
                    item(key = "tmdb_cast") {
                        val title = if (leadingCrew.isNotEmpty() && castMembers.isNotEmpty()) "Director & Cast"
                            else if (leadingCrew.isNotEmpty()) "Director"
                            else "Cast"
                        Column(modifier = Modifier.padding(top = 28.dp)) {
                            SectionHeader(title, textColor, Modifier.padding(start = 48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            CastRow(leadingCrew, castMembers, accentColor, textColor)
                        }
                    }
                }

                if (tmdbVideos.isNotEmpty()) {
                    item(key = "tmdb_trailers") {
                        Column(modifier = Modifier.padding(top = 28.dp)) {
                            SectionHeader("Trailers", textColor, Modifier.padding(start = 48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            TrailerRow(tmdbVideos, accentColor, textColor)
                        }
                    }
                }

                val networkCompanies = networks.map { TmdbCompanyInfo(name = it.name, logo = it.logo, tmdbId = it.tmdbId) }
                val isTvShow = type == "series"

                // TV shows: Networks first, then Production. Movies: Production first, then Networks.
                val firstStudios = if (isTvShow) networkCompanies else companies
                val firstLabel = if (isTvShow) "Network" else "Production"
                val secondStudios = if (isTvShow) companies else networkCompanies
                val secondLabel = if (isTvShow) "Production" else "Network"

                if (firstStudios.isNotEmpty()) {
                    item(key = "tmdb_studios_first") {
                        Column(modifier = Modifier.padding(top = 28.dp)) {
                            SectionHeader(firstLabel, textColor, Modifier.padding(start = 48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            StudioRow(firstStudios, textColor, accentColor)
                        }
                    }
                }

                if (secondStudios.isNotEmpty()) {
                    item(key = "tmdb_studios_second") {
                        Column(modifier = Modifier.padding(top = 28.dp)) {
                            SectionHeader(secondLabel, textColor, Modifier.padding(start = 48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            StudioRow(secondStudios, textColor, accentColor)
                        }
                    }
                }

                if (tmdbRecommendations.isNotEmpty()) {
                    item(key = "tmdb_recs") {
                        Column(modifier = Modifier.padding(top = 28.dp)) {
                            SectionHeader("More Like This", textColor, Modifier.padding(start = 48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            RecommendationRow(tmdbRecommendations, accentColor, onNavigateToDetails)
                        }
                    }
                }

                val collectionName = state.tmdbCollectionName
                if (tmdbCollection.isNotEmpty() && collectionName != null) {
                    item(key = "tmdb_collection") {
                        Column(modifier = Modifier.padding(top = 28.dp)) {
                            SectionHeader(collectionName, textColor, Modifier.padding(start = 48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            RecommendationRow(tmdbCollection, accentColor, onNavigateToDetails)
                        }
                    }
                }

                item(key = "tmdb_spacer") { Spacer(modifier = Modifier.height(48.dp)) }
            }
            } // LazyColumn
            } // CompositionLocalProvider verticalPivot
        }

        GlassSidebar(
            state = sidebarState,
            onDismiss = { viewModel.closeSidebar() },
            onBack = { viewModel.goBackInSidebar() },
            onEpisodeSelected = { episode ->
                val trackId = episodePlaybackId(streamId, episode)
                val epStreamId = episodeStreamId(streamId, episode)
                val epTitle = episodeDisplayTitle(episode)
                pendingPlaybackId = trackId
                pendingPlaybackType = type
                pendingPlaybackTitle = epTitle
                viewModel.loadStreams(type, epStreamId, epTitle, sourceSelectionId = trackId, autoSelectSource = autoSelectSource, rememberSourceSelection = rememberSourceSelection)
            },
            onSourceSelected = { stream ->
                viewModel.closeSidebar()
                val playbackId = pendingPlaybackId.ifBlank { movie?.id ?: id }
                val urlToPlay = resolvePlayableUrl(stream)
                if (!urlToPlay.isNullOrEmpty()) {
                    val playbackType = pendingPlaybackType.ifBlank { movie?.type ?: type }
                    val playbackTitle = pendingPlaybackTitle.ifBlank { movie?.name ?: "" }
                    onPlayClick(
                        urlToPlay,
                        playbackId,
                        playbackType,
                        playbackTitle,
                        movie?.name ?: "",
                        movie?.logo ?: "",
                        stream,
                        addonSubtitles,
                        availableStreams,
                        movie?.videos ?: emptyList()
                    )
                }
            }
        )

        // Centered loading spinner for auto-resolve paths (remembered source, auto-select)
        if (state.isLoadingStreams && sidebarState is SidebarState.Closed) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        }
    }
}

@Composable
private fun VoidActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "btnScale")
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .height(40.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.07f))
            .border(
                if (isFocused) 2.dp else 1.dp,
                if (isFocused) accentColor else Color.White.copy(0.15f),
                RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label.uppercase(),
            color = if (isFocused) accentColor else Color.White.copy(0.8f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetaDot(textColor: Color) {
    Text(
        text = ".",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = textColor.copy(alpha = 0.55f)
    )
}

@Composable
private fun ImdbBadge() {
    Image(
        painter = painterResource(id = R.drawable.imdb_logo),
        contentDescription = "IMDb",
        modifier = Modifier.height(20.dp)
    )
}

// ── TMDB Section Components ──

@Composable
private fun SectionHeader(title: String, textColor: Color, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = textColor.copy(alpha = 0.9f),
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CastRow(leadingCrew: List<TmdbCastInfo>, cast: List<TmdbCastInfo>, accentColor: Color, textColor: Color) {
    val rowState = rememberLazyListState()
    val density = LocalDensity.current
    val startPad = 48.dp
    val paddingPx = remember(density) { with(density) { startPad.toPx() } }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val endPadding = (screenWidth - startPad - 96.dp).coerceAtLeast(120.dp)

    val pivotSpec = remember(paddingPx) {
        FocusPivotSpec(
            customOffset = paddingPx,
            stiffnessProvider = { Spring.StiffnessLow }
        )
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(start = startPad, end = endPadding)
        ) {
            // Leading crew (directors, writers)
            itemsIndexed(leadingCrew, key = { i, it -> "crew_${it.tmdbId ?: i}" }) { index, member ->
                Box(modifier = Modifier.onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft && index == 0) true else false
                }) {
                    CastCard(member, accentColor, textColor)
                }
            }

            // Vertical divider between crew and cast
            if (leadingCrew.isNotEmpty() && cast.isNotEmpty()) {
                item(key = "cast_divider") {
                    Box(
                        modifier = Modifier.height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(72.dp)
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // Regular cast
            itemsIndexed(cast.take(20), key = { i, it -> "cast_${it.tmdbId ?: i}" }) { index, member ->
                val isFirstOverall = leadingCrew.isEmpty() && index == 0
                Box(modifier = Modifier.onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft && isFirstOverall) true else false
                }) {
                    CastCard(member, accentColor, textColor)
                }
            }
        }
    }
}


@Composable
private fun CastCard(member: TmdbCastInfo, accentColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "castScale")

    // Wider focusable area (96dp) with 80dp visual content centered within.
    // The extra width replaces LazyRow spacing and shifts focus centers so
    // Compose's geometric search picks the correct item when navigating
    // between rows with different card widths (cast 80dp vs trailer 190dp).
    Box(
        modifier = modifier
            .width(96.dp)
            .height(110.dp)
            .scale(scale)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        color = if (isFocused) accentColor else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                if (member.photo != null) {
                    AsyncImage(
                        model = member.photo,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (isFocused) Color.White else textColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            member.character?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = textColor.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrailerRow(videos: List<TmdbVideoInfo>, accentColor: Color, textColor: Color) {
    val rowState = rememberLazyListState()
    val density = LocalDensity.current
    val startPad = 48.dp
    val paddingPx = remember(density) { with(density) { startPad.toPx() } }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val endPadding = (screenWidth - startPad - 190.dp).coerceAtLeast(120.dp)

    val pivotSpec = remember(paddingPx) {
        FocusPivotSpec(
            customOffset = paddingPx,
            stiffnessProvider = { Spring.StiffnessLow }
        )
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = startPad, end = endPadding)
        ) {
            itemsIndexed(videos.take(6), key = { _, it -> it.key }) { index, video ->
                Box(modifier = Modifier.onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft && index == 0) true else false
                }) {
                    TrailerCard(video, accentColor, textColor)
                }
            }
        }
    }
}

@Composable
private fun TrailerCard(video: TmdbVideoInfo, accentColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "trailerScale")

    Column(
        modifier = modifier
            .width(190.dp)
            .scale(scale)
            .focusable(interactionSource = interactionSource)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(107.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(0.06f))
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) accentColor else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = video.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
            )
            // Play icon overlay
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(0.9f),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.5f))
                    .padding(4.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (isFocused) Color.White else textColor.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StudioRow(studios: List<TmdbCompanyInfo>, textColor: Color, accentColor: Color) {
    val rowState = rememberLazyListState()
    val density = LocalDensity.current
    val startPad = 48.dp
    val paddingPx = remember(density) { with(density) { startPad.toPx() } }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val endPadding = (screenWidth - startPad - 140.dp).coerceAtLeast(120.dp)

    val pivotSpec = remember(paddingPx) {
        FocusPivotSpec(
            customOffset = paddingPx,
            stiffnessProvider = { Spring.StiffnessLow }
        )
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = startPad, end = endPadding)
        ) {
            items(studios, key = { "${it.tmdbId}:${it.name}" }) { studio ->
                StudioChip(studio, textColor, accentColor)
            }
        }
    }
}

@Composable
private fun StudioChip(studio: TmdbCompanyInfo, textColor: Color, accentColor: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "studioScale")

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) accentColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        if (studio.logo != null) {
            AsyncImage(
                model = studio.logo,
                contentDescription = studio.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = studio.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isFocused) accentColor else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecommendationRow(items: List<TmdbMetaPreview>, accentColor: Color, onItemClick: (type: String, id: String) -> Unit = { _, _ -> }) {
    val rowState = rememberLazyListState()
    val density = LocalDensity.current
    val startPad = 48.dp
    val paddingPx = remember(density) { with(density) { startPad.toPx() } }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val endPadding = (screenWidth - startPad - 120.dp).coerceAtLeast(120.dp)

    val pivotSpec = remember(paddingPx) {
        FocusPivotSpec(
            customOffset = paddingPx,
            stiffnessProvider = { Spring.StiffnessLow }
        )
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = startPad, end = endPadding)
        ) {
            itemsIndexed(items, key = { _, it -> it.tmdbId }) { index, item ->
                Box(modifier = Modifier.onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft && index == 0) true else false
                }) {
                    RecommendationCard(item, accentColor, onClick = {
                        val stremioType = if (item.type == "tv") "series" else item.type
                        onItemClick(stremioType, "tmdb:${item.tmdbId}")
                    })
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(item: TmdbMetaPreview, accentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "recScale")

    Box(
        modifier = modifier
            .width(120.dp)
            .height(180.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.06f))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) accentColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
    ) {
        if (item.poster != null) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

private fun extractPrimaryYear(releaseInfo: String?): String {
    if (releaseInfo.isNullOrBlank()) return "----"
    return Regex("\\d{4}").find(releaseInfo)?.value ?: releaseInfo.take(4)
}

private fun findFirstEpisode(videos: List<MetaVideo>?): MetaVideo? {
    if (videos.isNullOrEmpty()) return null

    val numbered = videos.filter { it.season > 0 && it.episode > 0 }
    val candidates = if (numbered.isNotEmpty()) numbered else videos
    return candidates.minWithOrNull(
        compareBy<MetaVideo>({ if (it.season > 0) it.season else Int.MAX_VALUE })
            .thenBy { if (it.episode > 0) it.episode else Int.MAX_VALUE }
            .thenBy { it.title }
    )
}


private fun resolveEpisodeForPlaybackId(
    seriesId: String,
    videos: List<MetaVideo>?,
    playbackId: String?
): MetaVideo? {
    val targetId = playbackId ?: return null
    val episodeList = videos ?: return null
    // Exact match (new format: seriesId:season:episode)
    episodeList.firstOrNull { episodePlaybackId(seriesId, it) == targetId }?.let { return it }
    // Fallback: match by season/episode numbers (handles old-format entries)
    val parsed = parseSeasonEpisodeFromPlaybackId(targetId) ?: return null
    return episodeList.firstOrNull { it.season == parsed.first && it.episode == parsed.second }
}

private fun parseSeasonEpisodeFromPlaybackId(playbackId: String?): Pair<Int, Int>? {
    val id = playbackId ?: return null
    val parts = id.split(":")
    if (parts.size < 3) return null
    val season = parts[parts.lastIndex - 1].toIntOrNull() ?: return null
    val episode = parts.last().toIntOrNull() ?: return null
    if (season <= 0 || episode <= 0) return null
    return season to episode
}

private fun playbackIdBelongsToSeries(seriesId: String, playbackId: String): Boolean {
    val parts = playbackId.split(":")
    if (parts.size < 3) return playbackId == seriesId
    val season = parts[parts.lastIndex - 1].toIntOrNull()
    val episode = parts.last().toIntOrNull()
    if (season == null || episode == null) return playbackId == seriesId
    return parts.dropLast(2).joinToString(":") == seriesId
}

private val TORRENT_TRACKERS = listOf(
    // HTTP trackers (TCP — work even when UDP is blocked)
    "http://tracker.opentrackr.org:1337/announce",
    "http://tracker.openbittorrent.com:80/announce",
    "http://tracker1.bt.moack.co.kr:80/announce",
    "http://tracker.gbitt.info:80/announce",
    // UDP trackers (fallback)
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://open.stealth.si:80/announce",
    "udp://tracker.openbittorrent.com:6969/announce",
    "udp://exodus.desync.com:6969/announce"
)

private fun resolvePlayableUrl(stream: com.lumera.app.data.model.stremio.Stream): String? {
    if (!stream.url.isNullOrEmpty()) return stream.url
    if (!stream.infoHash.isNullOrEmpty()) {
        val trackerParams = TORRENT_TRACKERS.joinToString("") {
            "&tr=${java.net.URLEncoder.encode(it, "UTF-8")}"
        }
        return "magnet:?xt=urn:btih:${stream.infoHash}&dn=Video${trackerParams}"
    }
    return null
}
