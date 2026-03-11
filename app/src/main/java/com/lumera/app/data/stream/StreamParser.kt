package com.lumera.app.data.stream

import com.lumera.app.data.model.ParsedStreamInfo
import com.lumera.app.data.model.StreamQuality
import com.lumera.app.data.model.stremio.Stream

object StreamParser {

    private val seedPatterns = listOf(
        Regex("""👤\s*(\d[\d,.]*)"""),
        Regex("""(?i)\bseeds?[:\s]+(\d[\d,.]*)"""),
        Regex("""(?i)\bpeers?[:\s]+(\d[\d,.]*)"""),
        Regex("""(?i)\bS[:\s]*(\d[\d,.]*)""")
    )

    fun parse(stream: Stream): ParsedStreamInfo {
        val text = combinedText(stream)
        return ParsedStreamInfo(
            quality = StreamQuality.fromString(text),
            sizeBytes = stream.behaviorHints?.videoSize,
            seeds = extractSeeds(text)
        )
    }

    fun combinedText(stream: Stream): String {
        return listOfNotNull(
            stream.name,
            stream.title,
            stream.description,
            stream.behaviorHints?.filename
        ).joinToString(" ")
    }

    fun extractSeeds(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        for (pattern in seedPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val raw = match.groupValues[1].replace(",", "").replace(".", "")
                return raw.toIntOrNull()
            }
        }
        return null
    }
}
