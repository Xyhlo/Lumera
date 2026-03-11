package com.lumera.app.data.stream

import com.lumera.app.data.model.StreamQuality
import com.lumera.app.data.model.stremio.Stream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamSortingService @Inject constructor() {

    fun sortAndFilter(
        streams: List<Stream>,
        enabledQualities: Set<StreamQuality>,
        excludePhrases: List<String>,
        addonSortOrders: Map<String, Int>
    ): List<Stream> {
        val lowerPhrases = excludePhrases
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        return streams
            .filter { stream ->
                val info = StreamParser.parse(stream)
                // Keep streams whose quality is in the enabled set
                info.quality in enabledQualities
            }
            .filter { stream ->
                if (lowerPhrases.isEmpty()) return@filter true
                val text = StreamParser.combinedText(stream).lowercase()
                lowerPhrases.none { phrase -> text.contains(phrase) }
            }
            .sortedWith(buildComparator(addonSortOrders))
    }

    private fun buildComparator(addonSortOrders: Map<String, Int>): Comparator<Stream> {
        return compareByDescending<Stream> {
            StreamParser.parse(it).quality.sortOrder
        }.thenByDescending {
            StreamParser.parse(it).sizeBytes ?: 0L
        }.thenByDescending {
            StreamParser.parse(it).seeds ?: 0
        }.thenBy {
            addonSortOrders[it.addonTransportUrl] ?: Int.MAX_VALUE
        }
    }

    companion object {
        fun parseEnabledQualities(qualitiesString: String): Set<StreamQuality> {
            if (qualitiesString.isBlank()) return StreamQuality.entries.toSet()
            return qualitiesString.split(",")
                .mapNotNull { StreamQuality.fromKey(it.trim()) }
                .toSet()
        }

        fun parseExcludePhrases(phrasesString: String): List<String> {
            if (phrasesString.isBlank()) return emptyList()
            return phrasesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
