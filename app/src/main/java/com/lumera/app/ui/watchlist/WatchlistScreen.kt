package com.lumera.app.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.ui.home.DpadRepeatGate
import com.lumera.app.ui.home.InfiniteLoopRow
import com.lumera.app.ui.home.UpKeyDebouncer

@Composable
fun WatchlistScreen(
    currentProfile: ProfileEntity?,
    entryRequester: FocusRequester,
    drawerRequester: FocusRequester,
    onMovieClick: (MetaItem) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val movies by viewModel.movieItems.collectAsState()
    val series by viewModel.seriesItems.collectAsState()

    val upKeyDebouncer = remember { UpKeyDebouncer() }
    val dpadRepeatGate = remember { DpadRepeatGate() }

    var lastFocusedKey by remember { mutableStateOf<String?>(null) }

    // Resolve missing posters (e.g., items pulled from Trakt)
    LaunchedEffect(movies) { movies.forEach { viewModel.resolvePosterIfNeeded(it) } }
    LaunchedEffect(series) { series.forEach { viewModel.resolvePosterIfNeeded(it) } }

    val isTopNav = currentProfile?.navPosition == "top"
    val startPadding = if (isTopNav) 50.dp else 120.dp
    val topPadding = if (isTopNav) 24.dp else 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (movies.isEmpty() && series.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Your watchlist is empty",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Watchlist",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(start = startPadding)
                )

                if (movies.isNotEmpty()) {
                    InfiniteLoopRow(
                        startPadding = startPadding,
                        isTopNav = isTopNav,
                        rowIndex = 0,
                        title = "Movies",
                        items = movies,
                        onMovieClick = onMovieClick,
                        onViewMore = {},
                        onFocused = { _: MetaItem?, key: String ->
                            lastFocusedKey = key
                        },
                        entryRequester = entryRequester,
                        drawerRequester = drawerRequester,
                        locallyFocusedItemId = if (lastFocusedKey?.startsWith("0_") == true) lastFocusedKey else null,
                        isGlobalFocusPresent = lastFocusedKey != null,
                        isFirstRow = true,
                        isInfiniteLoopEnabled = false,
                        upKeyDebouncer = upKeyDebouncer,
                        repeatGate = dpadRepeatGate
                    )
                }

                if (series.isNotEmpty()) {
                    InfiniteLoopRow(
                        startPadding = startPadding,
                        isTopNav = isTopNav,
                        rowIndex = 1,
                        title = "Series",
                        items = series,
                        onMovieClick = onMovieClick,
                        onViewMore = {},
                        onFocused = { _: MetaItem?, key: String ->
                            lastFocusedKey = key
                        },
                        entryRequester = entryRequester,
                        drawerRequester = drawerRequester,
                        locallyFocusedItemId = if (lastFocusedKey?.startsWith("1_") == true) lastFocusedKey else null,
                        isGlobalFocusPresent = lastFocusedKey != null,
                        isFirstRow = movies.isEmpty(),
                        isInfiniteLoopEnabled = false,
                        upKeyDebouncer = upKeyDebouncer,
                        repeatGate = dpadRepeatGate
                    )
                }
            }
        }
    }
}
