package com.lumera.app.domain

import com.lumera.app.data.model.stremio.MetaVideo

fun episodePlaybackId(seriesId: String, episode: MetaVideo): String {
    return if (episode.id.contains(":")) {
        episode.id
    } else {
        "$seriesId:${episode.season}:${episode.episode}"
    }
}

fun episodeDisplayTitle(episode: MetaVideo): String {
    val season = episode.season.takeIf { it > 0 } ?: 1
    val number = episode.episode.takeIf { it > 0 } ?: 1
    return "S${season}:E${number} - ${episode.title}"
}

fun findNextEpisode(
    seriesId: String,
    currentPlaybackId: String,
    episodes: List<MetaVideo>
): MetaVideo? {
    if (episodes.isEmpty()) return null
    // Only consider regular episodes (season > 0 and episode > 0)
    val regular = episodes.filter { it.season > 0 && it.episode > 0 }
    if (regular.isEmpty()) return null
    val sorted = regular.sortedWith(
        compareBy<MetaVideo> { it.season }
            .thenBy { it.episode }
    )
    val currentIndex = sorted.indexOfFirst {
        episodePlaybackId(seriesId, it) == currentPlaybackId
    }
    if (currentIndex < 0 || currentIndex >= sorted.lastIndex) return null
    return sorted[currentIndex + 1]
}
