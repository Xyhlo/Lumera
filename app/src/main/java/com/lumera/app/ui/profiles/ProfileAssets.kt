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
    private const val CUSTOM_PREFIX = "custom:"

    val AVATAR_MAP = mapOf(
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
        "avatar_anime_21" to R.drawable.avatar_anime_21,
        "avatar_anime_22" to R.drawable.avatar_anime_22,
        "avatar_anime_23" to R.drawable.avatar_anime_23,
        "avatar_anime_24" to R.drawable.avatar_anime_24,
        "avatar_anime_25" to R.drawable.avatar_anime_25,
        "avatar_anime_26" to R.drawable.avatar_anime_26,
        "avatar_anime_27" to R.drawable.avatar_anime_27,
        "avatar_anime_28" to R.drawable.avatar_anime_28,
        "avatar_anime_29" to R.drawable.avatar_anime_29,
        "avatar_anime_30" to R.drawable.avatar_anime_30,
        "avatar_anime_31" to R.drawable.avatar_anime_31,
        "avatar_anime_32" to R.drawable.avatar_anime_32,
        "avatar_anime_33" to R.drawable.avatar_anime_33,
        "avatar_anime_34" to R.drawable.avatar_anime_34,
        "avatar_anime_35" to R.drawable.avatar_anime_35,
        "avatar_anime_36" to R.drawable.avatar_anime_36,
        "avatar_anime_37" to R.drawable.avatar_anime_37,
        "avatar_anime_38" to R.drawable.avatar_anime_38,
        "avatar_anime_39" to R.drawable.avatar_anime_39,
        "avatar_anime_40" to R.drawable.avatar_anime_40,
        "avatar_anime_41" to R.drawable.avatar_anime_41,
        "avatar_anime_42" to R.drawable.avatar_anime_42,
        "avatar_anime_43" to R.drawable.avatar_anime_43,
        "avatar_anime_44" to R.drawable.avatar_anime_44,
        "avatar_anime_45" to R.drawable.avatar_anime_45,
        "avatar_anime_46" to R.drawable.avatar_anime_46,
        "avatar_anime_47" to R.drawable.avatar_anime_47,
        "avatar_anime_48" to R.drawable.avatar_anime_48,
        "avatar_anime_49" to R.drawable.avatar_anime_49,
        "avatar_anime_50" to R.drawable.avatar_anime_50,
        "avatar_anime_51" to R.drawable.avatar_anime_51,
        "avatar_anime_52" to R.drawable.avatar_anime_52,
        "avatar_anime_53" to R.drawable.avatar_anime_53,
        "avatar_anime_54" to R.drawable.avatar_anime_54,
        "avatar_anime_55" to R.drawable.avatar_anime_55,
        "avatar_anime_56" to R.drawable.avatar_anime_56,
        "avatar_anime_57" to R.drawable.avatar_anime_57,
        "avatar_anime_58" to R.drawable.avatar_anime_58,
        "avatar_anime_59" to R.drawable.avatar_anime_59,
        "avatar_anime_60" to R.drawable.avatar_anime_60,
        "avatar_anime_61" to R.drawable.avatar_anime_61,
        "avatar_anime_62" to R.drawable.avatar_anime_62,
        "avatar_anime_63" to R.drawable.avatar_anime_63,
        "avatar_anime_64" to R.drawable.avatar_anime_64,
        "avatar_anime_65" to R.drawable.avatar_anime_65,
        "avatar_anime_66" to R.drawable.avatar_anime_66,
        "avatar_anime_67" to R.drawable.avatar_anime_67,
        "avatar_anime_68" to R.drawable.avatar_anime_68,
        "avatar_anime_69" to R.drawable.avatar_anime_69,
        "avatar_anime_70" to R.drawable.avatar_anime_70,
        "avatar_anime_71" to R.drawable.avatar_anime_71,
        "avatar_anime_72" to R.drawable.avatar_anime_72,
        "avatar_anime_73" to R.drawable.avatar_anime_73,
        "avatar_anime_74" to R.drawable.avatar_anime_74,
        "avatar_anime_75" to R.drawable.avatar_anime_75,
        "avatar_anime_76" to R.drawable.avatar_anime_76,
        "avatar_anime_77" to R.drawable.avatar_anime_77,
        "avatar_anime_78" to R.drawable.avatar_anime_78,
        "avatar_anime_79" to R.drawable.avatar_anime_79,
        "avatar_anime_80" to R.drawable.avatar_anime_80
    )

    val AVATAR_CATEGORIES = listOf(
        AvatarCategory("Anime", listOf(
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
            AvatarInfo("avatar_anime_20", R.drawable.avatar_anime_20, "Mikasa"),
            AvatarInfo("avatar_anime_21", R.drawable.avatar_anime_21, "Lelouch"),
            AvatarInfo("avatar_anime_22", R.drawable.avatar_anime_22, "Edward"),
            AvatarInfo("avatar_anime_23", R.drawable.avatar_anime_23, "Gintoki"),
            AvatarInfo("avatar_anime_24", R.drawable.avatar_anime_24, "Ichigo"),
            AvatarInfo("avatar_anime_25", R.drawable.avatar_anime_25, "Kaneki"),
            AvatarInfo("avatar_anime_26", R.drawable.avatar_anime_26, "Loid"),
            AvatarInfo("avatar_anime_27", R.drawable.avatar_anime_27, "Trunks"),
            AvatarInfo("avatar_anime_28", R.drawable.avatar_anime_28, "Meliodas"),
            AvatarInfo("avatar_anime_29", R.drawable.avatar_anime_29, "Asta"),
            AvatarInfo("avatar_anime_30", R.drawable.avatar_anime_30, "Itadori"),
            AvatarInfo("avatar_anime_31", R.drawable.avatar_anime_31, "Genos"),
            AvatarInfo("avatar_anime_32", R.drawable.avatar_anime_32, "Mugen"),
            AvatarInfo("avatar_anime_33", R.drawable.avatar_anime_33, "Archer"),
            AvatarInfo("avatar_anime_34", R.drawable.avatar_anime_34, "Yusuke"),
            AvatarInfo("avatar_anime_35", R.drawable.avatar_anime_35, "Kenshin"),
            AvatarInfo("avatar_anime_36", R.drawable.avatar_anime_36, "Inuyasha"),
            AvatarInfo("avatar_anime_37", R.drawable.avatar_anime_37, "Megumi"),
            AvatarInfo("avatar_anime_38", R.drawable.avatar_anime_38, "Giyu"),
            AvatarInfo("avatar_anime_39", R.drawable.avatar_anime_39, "Jinwoo"),
            AvatarInfo("avatar_anime_40", R.drawable.avatar_anime_40, "Senku"),
            AvatarInfo("avatar_anime_41", R.drawable.avatar_anime_41, "Hinata"),
            AvatarInfo("avatar_anime_42", R.drawable.avatar_anime_42, "Nami"),
            AvatarInfo("avatar_anime_43", R.drawable.avatar_anime_43, "Yor"),
            AvatarInfo("avatar_anime_44", R.drawable.avatar_anime_44, "Nezuko"),
            AvatarInfo("avatar_anime_45", R.drawable.avatar_anime_45, "Zero Two"),
            AvatarInfo("avatar_anime_46", R.drawable.avatar_anime_46, "Erza"),
            AvatarInfo("avatar_anime_47", R.drawable.avatar_anime_47, "Rem"),
            AvatarInfo("avatar_anime_48", R.drawable.avatar_anime_48, "Maki"),
            AvatarInfo("avatar_anime_49", R.drawable.avatar_anime_49, "Asuna"),
            AvatarInfo("avatar_anime_50", R.drawable.avatar_anime_50, "Ochaco"),
            AvatarInfo("avatar_anime_51", R.drawable.avatar_anime_51, "Rukia"),
            AvatarInfo("avatar_anime_52", R.drawable.avatar_anime_52, "Robin"),
            AvatarInfo("avatar_anime_53", R.drawable.avatar_anime_53, "Sakura"),
            AvatarInfo("avatar_anime_54", R.drawable.avatar_anime_54, "Bulma"),
            AvatarInfo("avatar_anime_55", R.drawable.avatar_anime_55, "Android 18"),
            AvatarInfo("avatar_anime_56", R.drawable.avatar_anime_56, "Winry"),
            AvatarInfo("avatar_anime_57", R.drawable.avatar_anime_57, "Anya"),
            AvatarInfo("avatar_anime_58", R.drawable.avatar_anime_58, "Power"),
            AvatarInfo("avatar_anime_59", R.drawable.avatar_anime_59, "Makima"),
            AvatarInfo("avatar_anime_60", R.drawable.avatar_anime_60, "Nobara"),
            AvatarInfo("avatar_anime_61", R.drawable.avatar_anime_61, "Mitsuri"),
            AvatarInfo("avatar_anime_62", R.drawable.avatar_anime_62, "Shinobu"),
            AvatarInfo("avatar_anime_63", R.drawable.avatar_anime_63, "Tohru"),
            AvatarInfo("avatar_anime_64", R.drawable.avatar_anime_64, "Violet"),
            AvatarInfo("avatar_anime_65", R.drawable.avatar_anime_65, "Saber"),
            AvatarInfo("avatar_anime_66", R.drawable.avatar_anime_66, "Emilia"),
            AvatarInfo("avatar_anime_67", R.drawable.avatar_anime_67, "Faye"),
            AvatarInfo("avatar_anime_68", R.drawable.avatar_anime_68, "Lucy"),
            AvatarInfo("avatar_anime_69", R.drawable.avatar_anime_69, "Kagome"),
            AvatarInfo("avatar_anime_70", R.drawable.avatar_anime_70, "Usagi"),
            AvatarInfo("avatar_anime_71", R.drawable.avatar_anime_71, "Rei"),
            AvatarInfo("avatar_anime_72", R.drawable.avatar_anime_72, "Asuka"),
            AvatarInfo("avatar_anime_73", R.drawable.avatar_anime_73, "Tsunade"),
            AvatarInfo("avatar_anime_74", R.drawable.avatar_anime_74, "Historia"),
            AvatarInfo("avatar_anime_75", R.drawable.avatar_anime_75, "Hancock"),
            AvatarInfo("avatar_anime_76", R.drawable.avatar_anime_76, "Misa"),
            AvatarInfo("avatar_anime_77", R.drawable.avatar_anime_77, "Chika"),
            AvatarInfo("avatar_anime_78", R.drawable.avatar_anime_78, "Mai"),
            AvatarInfo("avatar_anime_79", R.drawable.avatar_anime_79, "Aqua"),
            AvatarInfo("avatar_anime_80", R.drawable.avatar_anime_80, "Esdeath")
        ))
    )

    fun isCustomAvatar(avatarRef: String): Boolean {
        return avatarRef.startsWith(CUSTOM_PREFIX)
    }

    private fun getCustomAvatarPath(avatarRef: String): String {
        return avatarRef.removePrefix(CUSTOM_PREFIX)
    }

    private fun getCustomAvatarFile(avatarRef: String): File? {
        val path = getCustomAvatarPath(avatarRef)
        val file = File(path)
        return if (file.exists()) file else null
    }

    private fun getAvatarRes(name: String): Int {
        if (isCustomAvatar(name)) {
            return R.drawable.avatar_anime_01
        }
        return AVATAR_MAP[name] ?: R.drawable.avatar_anime_01
    }

    fun getAvatarSource(avatarRef: String): Any {
        return if (isCustomAvatar(avatarRef)) {
            getCustomAvatarFile(avatarRef) ?: R.drawable.avatar_anime_01
        } else {
            getAvatarRes(avatarRef)
        }
    }
}
