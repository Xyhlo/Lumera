package com.lumera.app.ui.player.base

import java.util.Locale

/**
 * Shared language normalization used by both the subtitle picker UI and
 * the preferred-language auto-selection in ExoPlayerBackend.
 *
 * Converts any addon / ISO-639-2 / ISO-639-3 / alias language tag into a
 * canonical ISO-639-1 two-letter key so that e.g. "eng", "en", "english"
 * all resolve to "en".
 */

internal val iso3ToIso2LanguageMap: Map<String, String> by lazy {
    buildMap {
        Locale.getISOLanguages().forEach { iso2 ->
            val normalizedIso2 = iso2.lowercase(Locale.ROOT)
            val locale = runCatching {
                Locale.Builder().setLanguage(normalizedIso2).build()
            }.getOrNull() ?: return@forEach
            runCatching { locale.isO3Language.lowercase(Locale.ROOT) }
                .getOrNull()
                ?.let { iso3 -> put(iso3, normalizedIso2) }
        }

        // Common legacy ISO-639-2 bibliographic codes still returned by some subtitle addons.
        put("fre", "fr")
        put("ger", "de")
        put("dut", "nl")
        put("gre", "el")
        put("rum", "ro")
        put("slo", "sk")
        put("alb", "sq")
        put("arm", "hy")
        put("baq", "eu")
        put("cze", "cs")
        put("chi", "zh")
        put("per", "fa")
        put("tib", "bo")
        put("wel", "cy")
        put("mac", "mk")
        put("ice", "is")
    }
}

internal val languageNameToIso2Map: Map<String, String> by lazy {
    buildMap {
        Locale.getISOLanguages().forEach { iso2 ->
            val normalizedIso2 = iso2.lowercase(Locale.ROOT)
            val locale = runCatching {
                Locale.Builder().setLanguage(normalizedIso2).build()
            }.getOrNull() ?: return@forEach
            val names = listOf(
                locale.getDisplayLanguage(Locale.ENGLISH),
                locale.getDisplayLanguage(locale)
            )
            names.forEach { name ->
                normalizedLanguageNameKey(name)?.let { key ->
                    putIfAbsent(key, normalizedIso2)
                }
            }
        }
    }
}

internal val addonLanguageAliasToIso2 = mapOf(
    // Common addon/non-ISO language tags.
    "pob" to "pt", // Portuguese (Brazil)
    "ptbr" to "pt",
    "ptpt" to "pt",
    "pb" to "pt",
    "spn" to "es", // Spanish
    "spl" to "es", // Spanish (LATAM variants in some addons)
    "esp" to "es",
    "esl" to "es",
    "zht" to "zh", // Chinese (traditional variants)
    "zhs" to "zh", // Chinese (simplified variants)
    "zhe" to "zh",
    "cht" to "zh",
    "chs" to "zh",
    "zho" to "zh",
    "zhcn" to "zh",
    "zhtw" to "zh",
    "zhhk" to "zh",
    "zhhans" to "zh",
    "zhhant" to "zh",
    "scc" to "sr", // Legacy Serbian code
    "scr" to "hr", // Legacy Croatian code
    "may" to "ms", // Legacy Malay code
    "bur" to "my", // Legacy Burmese code
    "geo" to "ka", // Legacy Georgian code
    "mao" to "mi", // Legacy Maori code
    "mol" to "ro"  // Legacy Moldavian code
)

/**
 * Normalizes any language string (ISO-639-1, ISO-639-2, ISO-639-3, addon
 * alias, full language name) into a canonical ISO-639-1 two-letter key.
 * Returns "und" for null, blank, or unrecognizable input.
 */
internal fun normalizeLanguageToIso2(language: String?): String {
    val normalized = language
        ?.trim()
        ?.replace('_', '-')
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.isNotEmpty() }
        ?: return "und"

    val primarySubtag = normalized.substringBefore('-').takeIf { it.isNotBlank() } ?: return "und"
    val compactPrimarySubtag = primarySubtag.filter { it in 'a'..'z' }
    if (compactPrimarySubtag == "und") return "und"

    addonLanguageAliasToIso2[normalized]?.let { return it }
    addonLanguageAliasToIso2[primarySubtag]?.let { return it }
    addonLanguageAliasToIso2[compactPrimarySubtag]?.let { return it }

    // Group all Chinese addon variants into one bucket (zht, zhs, zh-cn, zh.tw, etc.).
    if (compactPrimarySubtag.startsWith("zh")) return "zh"

    // Collapse locale variants and normalize aliases:
    // en-US / eng / english -> en
    if (compactPrimarySubtag.length == 2) return compactPrimarySubtag

    iso3ToIso2LanguageMap[compactPrimarySubtag]?.let { return it }

    normalizedLanguageNameKey(primarySubtag)
        ?.let { key ->
            languageNameToIso2Map[key]
                ?: key.substringBefore(' ').takeIf { it != key }?.let { firstWord ->
                    languageNameToIso2Map[firstWord]
                }
        }
        ?.let { return it }

    return when {
        compactPrimarySubtag.length >= 2 -> compactPrimarySubtag
        else -> "und"
    }
}

internal fun normalizedLanguageNameKey(rawValue: String?): String? {
    val value = rawValue
        ?.lowercase(Locale.ROOT)
        ?.replace(Regex("[^\\p{L}]"), " ")
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
    return value?.takeIf { it.isNotEmpty() }
}
