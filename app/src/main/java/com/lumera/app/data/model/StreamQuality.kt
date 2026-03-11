package com.lumera.app.data.model

enum class StreamQuality(val sortOrder: Int) {
    UHD_4K(5),
    FHD_1080P(4),
    HD_720P(3),
    SD_480P(2),
    CAM(1),
    UNKNOWN(0);

    companion object {
        private val pattern4K = Regex("""(?i)\b(2160p|4k|uhd)\b""")
        private val pattern1080 = Regex("""(?i)\b(1080p|fhd)\b""")
        private val pattern720 = Regex("""(?i)\b720p\b""")
        private val patternHD = Regex("""(?i)\bHD\b""")
        private val patternSD = Regex("""(?i)\b(480p|sd|dvd|dvdrip)\b""")
        private val patternCAM = Regex("""(?i)\b(cam|camrip|ts|telesync|hdts|hdcam|telecine|tc)\b""")

        fun fromString(input: String): StreamQuality {
            return when {
                pattern4K.containsMatchIn(input) -> UHD_4K
                pattern1080.containsMatchIn(input) -> FHD_1080P
                pattern720.containsMatchIn(input) -> HD_720P
                patternSD.containsMatchIn(input) -> SD_480P
                patternCAM.containsMatchIn(input) -> CAM
                patternHD.containsMatchIn(input) -> HD_720P
                else -> UNKNOWN
            }
        }

        fun fromKey(key: String): StreamQuality? = when (key) {
            "4k" -> UHD_4K
            "1080p" -> FHD_1080P
            "720p" -> HD_720P
            "sd" -> SD_480P
            "cam" -> CAM
            "unknown" -> UNKNOWN
            else -> null
        }

        fun toKey(quality: StreamQuality): String = when (quality) {
            UHD_4K -> "4k"
            FHD_1080P -> "1080p"
            HD_720P -> "720p"
            SD_480P -> "sd"
            CAM -> "cam"
            UNKNOWN -> "unknown"
        }
    }
}
