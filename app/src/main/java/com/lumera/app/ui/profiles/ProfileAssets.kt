package com.lumera.app.ui.profiles

import com.lumera.app.R
import java.io.File

data class AvatarInfo(
    val key: String,
    val resId: Int,
    val displayName: String
)

data class AvatarCategory(
    val label: String,
    val avatars: List<AvatarInfo>
)

object ProfileAssets {
    // Prefix for custom uploaded avatars
    private const val CUSTOM_PREFIX = "custom:"

    // We map the "Safe String" to the "Unsafe ID"
    val AVATAR_MAP = mapOf(
        // Original 16 abstract avatars
        "avatar_1" to R.drawable.avatar_1,
        "avatar_2" to R.drawable.avatar_2,
        "avatar_3" to R.drawable.avatar_3,
        "avatar_4" to R.drawable.avatar_4,
        "avatar_5" to R.drawable.avatar_5,
        "avatar_6" to R.drawable.avatar_6,
        "avatar_7" to R.drawable.avatar_7,
        "avatar_8" to R.drawable.avatar_8,
        "avatar_9" to R.drawable.avatar_9,
        "avatar_10" to R.drawable.avatar_10,
        "avatar_11" to R.drawable.avatar_11,
        "avatar_12" to R.drawable.avatar_12,
        "avatar_13" to R.drawable.avatar_13,
        "avatar_14" to R.drawable.avatar_14,
        "avatar_15" to R.drawable.avatar_15,
        "avatar_16" to R.drawable.avatar_16,
        // Anime avatars
        "avatar_anime_01" to R.drawable.avatar_anime_01,
        "avatar_anime_02" to R.drawable.avatar_anime_02,
        "avatar_anime_03" to R.drawable.avatar_anime_03,
        "avatar_anime_04" to R.drawable.avatar_anime_04,
        "avatar_anime_05" to R.drawable.avatar_anime_05,
        "avatar_anime_06" to R.drawable.avatar_anime_06,
        "avatar_anime_07" to R.drawable.avatar_anime_07,
        "avatar_anime_08" to R.drawable.avatar_anime_08,
        "avatar_anime_09" to R.drawable.avatar_anime_09,
        "avatar_anime_10" to R.drawable.avatar_anime_10,
        "avatar_anime_11" to R.drawable.avatar_anime_11,
        "avatar_anime_12" to R.drawable.avatar_anime_12,
        "avatar_anime_13" to R.drawable.avatar_anime_13,
        "avatar_anime_14" to R.drawable.avatar_anime_14,
        "avatar_anime_15" to R.drawable.avatar_anime_15,
        "avatar_anime_16" to R.drawable.avatar_anime_16,
        "avatar_anime_17" to R.drawable.avatar_anime_17,
        "avatar_anime_18" to R.drawable.avatar_anime_18,
        "avatar_anime_19" to R.drawable.avatar_anime_19,
        "avatar_anime_20" to R.drawable.avatar_anime_20,
        // Video game avatars
        "avatar_game_01" to R.drawable.avatar_game_01,
        "avatar_game_02" to R.drawable.avatar_game_02,
        "avatar_game_03" to R.drawable.avatar_game_03,
        "avatar_game_04" to R.drawable.avatar_game_04,
        "avatar_game_05" to R.drawable.avatar_game_05,
        "avatar_game_06" to R.drawable.avatar_game_06,
        "avatar_game_07" to R.drawable.avatar_game_07,
        "avatar_game_08" to R.drawable.avatar_game_08,
        "avatar_game_09" to R.drawable.avatar_game_09,
        "avatar_game_10" to R.drawable.avatar_game_10,
        "avatar_game_11" to R.drawable.avatar_game_11,
        "avatar_game_12" to R.drawable.avatar_game_12,
        "avatar_game_13" to R.drawable.avatar_game_13,
        "avatar_game_14" to R.drawable.avatar_game_14,
        "avatar_game_15" to R.drawable.avatar_game_15,
        "avatar_game_16" to R.drawable.avatar_game_16,
        "avatar_game_17" to R.drawable.avatar_game_17,
        "avatar_game_18" to R.drawable.avatar_game_18,
        "avatar_game_19" to R.drawable.avatar_game_19,
        "avatar_game_20" to R.drawable.avatar_game_20,
        // Movies & TV avatars
        "avatar_movie_01" to R.drawable.avatar_movie_01,
        "avatar_movie_02" to R.drawable.avatar_movie_02,
        "avatar_movie_03" to R.drawable.avatar_movie_03,
        "avatar_movie_04" to R.drawable.avatar_movie_04,
        "avatar_movie_05" to R.drawable.avatar_movie_05,
        "avatar_movie_06" to R.drawable.avatar_movie_06,
        "avatar_movie_07" to R.drawable.avatar_movie_07,
        "avatar_movie_08" to R.drawable.avatar_movie_08,
        "avatar_movie_09" to R.drawable.avatar_movie_09,
        "avatar_movie_10" to R.drawable.avatar_movie_10,
        "avatar_movie_11" to R.drawable.avatar_movie_11,
        "avatar_movie_12" to R.drawable.avatar_movie_12,
        "avatar_movie_13" to R.drawable.avatar_movie_13,
        "avatar_movie_14" to R.drawable.avatar_movie_14,
        "avatar_movie_15" to R.drawable.avatar_movie_15,
        "avatar_movie_16" to R.drawable.avatar_movie_16,
        "avatar_movie_17" to R.drawable.avatar_movie_17,
        "avatar_movie_18" to R.drawable.avatar_movie_18,
        "avatar_movie_19" to R.drawable.avatar_movie_19,
        "avatar_movie_20" to R.drawable.avatar_movie_20,
        // Superhero avatars
        "avatar_hero_01" to R.drawable.avatar_hero_01,
        "avatar_hero_02" to R.drawable.avatar_hero_02,
        "avatar_hero_03" to R.drawable.avatar_hero_03,
        "avatar_hero_04" to R.drawable.avatar_hero_04,
        "avatar_hero_05" to R.drawable.avatar_hero_05,
        "avatar_hero_06" to R.drawable.avatar_hero_06,
        "avatar_hero_07" to R.drawable.avatar_hero_07,
        "avatar_hero_08" to R.drawable.avatar_hero_08,
        "avatar_hero_09" to R.drawable.avatar_hero_09,
        "avatar_hero_10" to R.drawable.avatar_hero_10,
        "avatar_hero_11" to R.drawable.avatar_hero_11,
        "avatar_hero_12" to R.drawable.avatar_hero_12,
        "avatar_hero_13" to R.drawable.avatar_hero_13,
        "avatar_hero_14" to R.drawable.avatar_hero_14,
        "avatar_hero_15" to R.drawable.avatar_hero_15,
        "avatar_hero_16" to R.drawable.avatar_hero_16,
        "avatar_hero_17" to R.drawable.avatar_hero_17,
        "avatar_hero_18" to R.drawable.avatar_hero_18,
        "avatar_hero_19" to R.drawable.avatar_hero_19,
        "avatar_hero_20" to R.drawable.avatar_hero_20
    )

    val AVATAR_CATEGORIES = listOf(
        AvatarCategory(
            label = "Anime",
            avatars = listOf(
                AvatarInfo("avatar_anime_01", R.drawable.avatar_anime_01, "Goku"),
                AvatarInfo("avatar_anime_02", R.drawable.avatar_anime_02, "Naruto"),
                AvatarInfo("avatar_anime_03", R.drawable.avatar_anime_03, "Luffy"),
                AvatarInfo("avatar_anime_04", R.drawable.avatar_anime_04, "Levi"),
                AvatarInfo("avatar_anime_05", R.drawable.avatar_anime_05, "Gojo"),
                AvatarInfo("avatar_anime_06", R.drawable.avatar_anime_06, "Tanjiro"),
                AvatarInfo("avatar_anime_07", R.drawable.avatar_anime_07, "Spike"),
                AvatarInfo("avatar_anime_08", R.drawable.avatar_anime_08, "Vegeta"),
                AvatarInfo("avatar_anime_09", R.drawable.avatar_anime_09, "Kakashi"),
                AvatarInfo("avatar_anime_10", R.drawable.avatar_anime_10, "Itachi"),
                AvatarInfo("avatar_anime_11", R.drawable.avatar_anime_11, "Zoro"),
                AvatarInfo("avatar_anime_12", R.drawable.avatar_anime_12, "Saitama"),
                AvatarInfo("avatar_anime_13", R.drawable.avatar_anime_13, "Light"),
                AvatarInfo("avatar_anime_14", R.drawable.avatar_anime_14, "Eren"),
                AvatarInfo("avatar_anime_15", R.drawable.avatar_anime_15, "Killua"),
                AvatarInfo("avatar_anime_16", R.drawable.avatar_anime_16, "Gon"),
                AvatarInfo("avatar_anime_17", R.drawable.avatar_anime_17, "Deku"),
                AvatarInfo("avatar_anime_18", R.drawable.avatar_anime_18, "Todoroki"),
                AvatarInfo("avatar_anime_19", R.drawable.avatar_anime_19, "Sasuke"),
                AvatarInfo("avatar_anime_20", R.drawable.avatar_anime_20, "Mikasa")
            )
        ),
        AvatarCategory(
            label = "Video Games",
            avatars = listOf(
                AvatarInfo("avatar_game_01", R.drawable.avatar_game_01, "Master Chief"),
                AvatarInfo("avatar_game_02", R.drawable.avatar_game_02, "Kratos"),
                AvatarInfo("avatar_game_03", R.drawable.avatar_game_03, "Mario"),
                AvatarInfo("avatar_game_04", R.drawable.avatar_game_04, "Link"),
                AvatarInfo("avatar_game_05", R.drawable.avatar_game_05, "Cloud"),
                AvatarInfo("avatar_game_06", R.drawable.avatar_game_06, "Geralt"),
                AvatarInfo("avatar_game_07", R.drawable.avatar_game_07, "Arthur Morgan"),
                AvatarInfo("avatar_game_08", R.drawable.avatar_game_08, "Solid Snake"),
                AvatarInfo("avatar_game_09", R.drawable.avatar_game_09, "Doom Slayer"),
                AvatarInfo("avatar_game_10", R.drawable.avatar_game_10, "Shepard"),
                AvatarInfo("avatar_game_11", R.drawable.avatar_game_11, "Aloy"),
                AvatarInfo("avatar_game_12", R.drawable.avatar_game_12, "Ellie"),
                AvatarInfo("avatar_game_13", R.drawable.avatar_game_13, "Jin Sakai"),
                AvatarInfo("avatar_game_14", R.drawable.avatar_game_14, "Kirby"),
                AvatarInfo("avatar_game_15", R.drawable.avatar_game_15, "Pikachu"),
                AvatarInfo("avatar_game_16", R.drawable.avatar_game_16, "Sonic"),
                AvatarInfo("avatar_game_17", R.drawable.avatar_game_17, "Samus"),
                AvatarInfo("avatar_game_18", R.drawable.avatar_game_18, "Mega Man"),
                AvatarInfo("avatar_game_19", R.drawable.avatar_game_19, "Pac-Man"),
                AvatarInfo("avatar_game_20", R.drawable.avatar_game_20, "Lara Croft")
            )
        ),
        AvatarCategory(
            label = "Movies & TV",
            avatars = listOf(
                AvatarInfo("avatar_movie_01", R.drawable.avatar_movie_01, "Darth Vader"),
                AvatarInfo("avatar_movie_02", R.drawable.avatar_movie_02, "Batman"),
                AvatarInfo("avatar_movie_03", R.drawable.avatar_movie_03, "Iron Man"),
                AvatarInfo("avatar_movie_04", R.drawable.avatar_movie_04, "Mandalorian"),
                AvatarInfo("avatar_movie_05", R.drawable.avatar_movie_05, "John Wick"),
                AvatarInfo("avatar_movie_06", R.drawable.avatar_movie_06, "Gandalf"),
                AvatarInfo("avatar_movie_07", R.drawable.avatar_movie_07, "Jack Sparrow"),
                AvatarInfo("avatar_movie_08", R.drawable.avatar_movie_08, "Neo"),
                AvatarInfo("avatar_movie_09", R.drawable.avatar_movie_09, "Joker"),
                AvatarInfo("avatar_movie_10", R.drawable.avatar_movie_10, "Thanos"),
                AvatarInfo("avatar_movie_11", R.drawable.avatar_movie_11, "Yoda"),
                AvatarInfo("avatar_movie_12", R.drawable.avatar_movie_12, "Wolverine"),
                AvatarInfo("avatar_movie_13", R.drawable.avatar_movie_13, "Deadpool"),
                AvatarInfo("avatar_movie_14", R.drawable.avatar_movie_14, "Walter White"),
                AvatarInfo("avatar_movie_15", R.drawable.avatar_movie_15, "Eleven"),
                AvatarInfo("avatar_movie_16", R.drawable.avatar_movie_16, "Tyrion"),
                AvatarInfo("avatar_movie_17", R.drawable.avatar_movie_17, "Witcher"),
                AvatarInfo("avatar_movie_18", R.drawable.avatar_movie_18, "Baby Groot"),
                AvatarInfo("avatar_movie_19", R.drawable.avatar_movie_19, "Venom"),
                AvatarInfo("avatar_movie_20", R.drawable.avatar_movie_20, "Rick Sanchez")
            )
        ),
        AvatarCategory(
            label = "Superheroes",
            avatars = listOf(
                AvatarInfo("avatar_hero_01", R.drawable.avatar_hero_01, "Spider-Man"),
                AvatarInfo("avatar_hero_02", R.drawable.avatar_hero_02, "Superman"),
                AvatarInfo("avatar_hero_03", R.drawable.avatar_hero_03, "Wonder Woman"),
                AvatarInfo("avatar_hero_04", R.drawable.avatar_hero_04, "Black Panther"),
                AvatarInfo("avatar_hero_05", R.drawable.avatar_hero_05, "Thor"),
                AvatarInfo("avatar_hero_06", R.drawable.avatar_hero_06, "Captain America"),
                AvatarInfo("avatar_hero_07", R.drawable.avatar_hero_07, "Flash"),
                AvatarInfo("avatar_hero_08", R.drawable.avatar_hero_08, "Aquaman"),
                AvatarInfo("avatar_hero_09", R.drawable.avatar_hero_09, "Green Lantern"),
                AvatarInfo("avatar_hero_10", R.drawable.avatar_hero_10, "Doctor Strange"),
                AvatarInfo("avatar_hero_11", R.drawable.avatar_hero_11, "Scarlet Witch"),
                AvatarInfo("avatar_hero_12", R.drawable.avatar_hero_12, "Hulk"),
                AvatarInfo("avatar_hero_13", R.drawable.avatar_hero_13, "Black Widow"),
                AvatarInfo("avatar_hero_14", R.drawable.avatar_hero_14, "Nightwing"),
                AvatarInfo("avatar_hero_15", R.drawable.avatar_hero_15, "Raven"),
                AvatarInfo("avatar_hero_16", R.drawable.avatar_hero_16, "Cyborg"),
                AvatarInfo("avatar_hero_17", R.drawable.avatar_hero_17, "Shazam"),
                AvatarInfo("avatar_hero_18", R.drawable.avatar_hero_18, "Ant-Man"),
                AvatarInfo("avatar_hero_19", R.drawable.avatar_hero_19, "Vision"),
                AvatarInfo("avatar_hero_20", R.drawable.avatar_hero_20, "Hawkeye")
            )
        ),
        AvatarCategory(
            label = "Abstract",
            avatars = listOf(
                AvatarInfo("avatar_1", R.drawable.avatar_1, "Abstract 1"),
                AvatarInfo("avatar_2", R.drawable.avatar_2, "Abstract 2"),
                AvatarInfo("avatar_3", R.drawable.avatar_3, "Abstract 3"),
                AvatarInfo("avatar_4", R.drawable.avatar_4, "Abstract 4"),
                AvatarInfo("avatar_5", R.drawable.avatar_5, "Abstract 5"),
                AvatarInfo("avatar_6", R.drawable.avatar_6, "Abstract 6"),
                AvatarInfo("avatar_7", R.drawable.avatar_7, "Abstract 7"),
                AvatarInfo("avatar_8", R.drawable.avatar_8, "Abstract 8"),
                AvatarInfo("avatar_9", R.drawable.avatar_9, "Abstract 9"),
                AvatarInfo("avatar_10", R.drawable.avatar_10, "Abstract 10"),
                AvatarInfo("avatar_11", R.drawable.avatar_11, "Abstract 11"),
                AvatarInfo("avatar_12", R.drawable.avatar_12, "Abstract 12"),
                AvatarInfo("avatar_13", R.drawable.avatar_13, "Abstract 13"),
                AvatarInfo("avatar_14", R.drawable.avatar_14, "Abstract 14"),
                AvatarInfo("avatar_15", R.drawable.avatar_15, "Abstract 15"),
                AvatarInfo("avatar_16", R.drawable.avatar_16, "Abstract 16")
            )
        )
    )

    /**
     * Check if the avatar reference is a custom uploaded avatar.
     */
    fun isCustomAvatar(avatarRef: String): Boolean {
        return avatarRef.startsWith(CUSTOM_PREFIX)
    }

    /**
     * Get the file path for a custom avatar (strips the "custom:" prefix).
     */
    private fun getCustomAvatarPath(avatarRef: String): String {
        return avatarRef.removePrefix(CUSTOM_PREFIX)
    }

    /**
     * Get the File object for a custom avatar. Returns null if file doesn't exist.
     */
    private fun getCustomAvatarFile(avatarRef: String): File? {
        val path = getCustomAvatarPath(avatarRef)
        val file = File(path)
        return if (file.exists()) file else null
    }

    /**
     * The Magic Function: String -> Int (for built-in avatars only)
     * For custom avatars, use isCustomAvatar() and getCustomAvatarPath() instead.
     */
    private fun getAvatarRes(name: String): Int {
        // If it's a custom avatar, return fallback (callers should check isCustomAvatar first)
        if (isCustomAvatar(name)) {
            return R.drawable.avatar_1
        }
        return AVATAR_MAP[name] ?: R.drawable.avatar_1 // Fallback to avatar_1 if missing
    }

    /**
     * Get avatar source for AsyncImage - returns either Int (resource ID) or File (for custom).
     * Callers should use this for loading avatars:
     *
     * val source = ProfileAssets.getAvatarSource(avatarRef)
     * AsyncImage(
     *     model = ImageRequest.Builder(context)
     *         .data(source)
     *         .build(),
     *     ...
     * )
     */
    fun getAvatarSource(avatarRef: String): Any {
        return if (isCustomAvatar(avatarRef)) {
            getCustomAvatarFile(avatarRef) ?: R.drawable.avatar_1
        } else {
            getAvatarRes(avatarRef)
        }
    }
}
