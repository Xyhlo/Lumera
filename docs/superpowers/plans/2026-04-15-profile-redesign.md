# Profile Redesign & Cyberpunk Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the welcome splash, combine name+avatar into one wizard screen with 80+ categorized character avatars, and add a Cyberpunk theme.

**Architecture:** Modify the existing 4-step wizard (Name → Avatar → Theme → Done) to a 2-step wizard (Name+Avatar → Theme → Done). The combined screen shows a name input at the top with a large avatar preview, and categorized horizontal avatar rows below. Avatar assets are WebP drawables added to `res/drawable/`. The Cyberpunk theme is added as the 9th entry in `DefaultThemes`.

**Tech Stack:** Kotlin, Jetpack Compose for TV, Material 3, Coil (AsyncImage), Room database

---

### Task 1: Add Cyberpunk Theme to DefaultThemes

**Goal:** Add a new "Cyberpunk" built-in theme with neon cyan/dark blue palette.

**Files:**
- Modify: `app/src/main/java/com/lumera/app/ui/theme/DefaultThemes.kt`

**Acceptance Criteria:**
- [ ] Cyberpunk theme exists with id "cyberpunk", category "colorful"
- [ ] Theme appears in the `ALL` list
- [ ] `getById("cyberpunk")` returns the Cyberpunk theme

**Verify:** Build the project: `./gradlew assembleDebug` → BUILD SUCCESSFUL

**Steps:**

- [ ] **Step 1: Add CYBERPUNK constant to DefaultThemes.kt**

In `app/src/main/java/com/lumera/app/ui/theme/DefaultThemes.kt`, add the new theme constant after `SLATE` and before `ALL`:

```kotlin
val CYBERPUNK = ThemeEntity(
    id = "cyberpunk",
    name = "Cyberpunk",
    primaryColor = 0xFF00E5FF,      // Neon Cyan
    backgroundColor = 0xFF0A0E1A,   // Deep Dark Blue
    surfaceColor = 0xFF111833,      // Dark Navy
    textColor = 0xFFE0F7FA,         // Ice White
    textMutedColor = 0xFF4DD0E1,    // Soft Cyan
    errorColor = 0xFFFF1744,        // Neon Red
    isBuiltIn = true,
    category = "colorful"
)
```

- [ ] **Step 2: Update the ALL list to include CYBERPUNK**

Change the `ALL` list to:

```kotlin
val ALL = listOf(VOID, NEON, OCEAN, SUNSET, EMERALD, AMBER, CRIMSON, SLATE, CYBERPUNK)
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lumera/app/ui/theme/DefaultThemes.kt
git commit -m "feat: add Cyberpunk built-in theme"
```

---

### Task 2: Add 80 Character Avatar Assets

**Goal:** Add 80 character avatar WebP images to the drawable resources, organized by category naming convention.

**Files:**
- Create: `app/src/main/res/drawable/avatar_anime_01.webp` through `avatar_anime_20.webp`
- Create: `app/src/main/res/drawable/avatar_game_01.webp` through `avatar_game_20.webp`
- Create: `app/src/main/res/drawable/avatar_movie_01.webp` through `avatar_movie_20.webp`
- Create: `app/src/main/res/drawable/avatar_hero_01.webp` through `avatar_hero_20.webp`

**Acceptance Criteria:**
- [ ] 80 WebP files exist in `res/drawable/` with correct naming convention
- [ ] Each image is approximately 280x280px, under 200KB
- [ ] Files are valid WebP format that Android can decode

**Verify:** `ls app/src/main/res/drawable/avatar_anime_*.webp | wc -l` → 20 (repeat for game, movie, hero)

**Steps:**

- [ ] **Step 1: Source/generate 80 character avatar images**

The user needs to provide or generate 80 character avatar images. These should be:
- 280x280px WebP format
- Circular-friendly compositions (character face/bust, centered)
- Consistent art style within categories

**Anime (avatar_anime_01 through avatar_anime_20):**
01=Goku, 02=Naruto, 03=Luffy, 04=Levi, 05=Gojo, 06=Tanjiro, 07=Spike, 08=Vegeta, 09=Kakashi, 10=Itachi, 11=Zoro, 12=Saitama, 13=Light, 14=Eren, 15=Killua, 16=Gon, 17=Deku, 18=Todoroki, 19=Sasuke, 20=Mikasa

**Video Games (avatar_game_01 through avatar_game_20):**
01=Master Chief, 02=Kratos, 03=Mario, 04=Link, 05=Cloud, 06=Geralt, 07=Arthur Morgan, 08=Solid Snake, 09=Doom Slayer, 10=Shepard, 11=Aloy, 12=Ellie, 13=Jin Sakai, 14=Kirby, 15=Pikachu, 16=Sonic, 17=Samus, 18=Mega Man, 19=Pac-Man, 20=Lara Croft

**Movies & TV (avatar_movie_01 through avatar_movie_20):**
01=Darth Vader, 02=Batman, 03=Iron Man, 04=Mandalorian, 05=John Wick, 06=Gandalf, 07=Jack Sparrow, 08=Neo, 09=Joker, 10=Thanos, 11=Yoda, 12=Wolverine, 13=Deadpool, 14=Walter White, 15=Eleven, 16=Tyrion, 17=Witcher, 18=Baby Groot, 19=Venom, 20=Rick Sanchez

**Superheroes (avatar_hero_01 through avatar_hero_20):**
01=Spider-Man, 02=Superman, 03=Wonder Woman, 04=Black Panther, 05=Thor, 06=Captain America, 07=Flash, 08=Aquaman, 09=Green Lantern, 10=Doctor Strange, 11=Scarlet Witch, 12=Hulk, 13=Black Widow, 14=Nightwing, 15=Raven, 16=Cyborg, 17=Shazam, 18=Ant-Man, 19=Vision, 20=Hawkeye

- [ ] **Step 2: Place all 80 files in the drawable directory**

Copy all WebP files to `app/src/main/res/drawable/`

- [ ] **Step 3: Verify file count and format**

```bash
ls app/src/main/res/drawable/avatar_anime_*.webp | wc -l  # → 20
ls app/src/main/res/drawable/avatar_game_*.webp | wc -l   # → 20
ls app/src/main/res/drawable/avatar_movie_*.webp | wc -l  # → 20
ls app/src/main/res/drawable/avatar_hero_*.webp | wc -l   # → 20
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable/avatar_anime_*.webp
git add app/src/main/res/drawable/avatar_game_*.webp
git add app/src/main/res/drawable/avatar_movie_*.webp
git add app/src/main/res/drawable/avatar_hero_*.webp
git commit -m "feat: add 80 character avatar images (anime, games, movies, heroes)"
```

---

### Task 3: Expand ProfileAssets with Categorized Avatar Map

**Goal:** Update `ProfileAssets.kt` to include all 96 avatars (16 original + 80 new) organized by category with display names.

**Files:**
- Modify: `app/src/main/java/com/lumera/app/ui/profiles/ProfileAssets.kt`

**Acceptance Criteria:**
- [ ] `AVATAR_MAP` contains all 96 avatar key → resource ID mappings
- [ ] `AVATAR_CATEGORIES` provides ordered categories with their avatar keys and display names
- [ ] Existing avatar references ("avatar_1" through "avatar_16") still resolve correctly
- [ ] `getAvatarSource()` works for all new avatar keys

**Verify:** Build the project: `./gradlew assembleDebug` → BUILD SUCCESSFUL

**Steps:**

- [ ] **Step 1: Add category data structure and expand AVATAR_MAP**

Replace the contents of `app/src/main/java/com/lumera/app/ui/profiles/ProfileAssets.kt` with:

```kotlin
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
        // Original abstract avatars
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
        // Anime
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
        // Video Games
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
        // Movies & TV
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
        // Superheroes
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
            AvatarInfo("avatar_anime_20", R.drawable.avatar_anime_20, "Mikasa")
        )),
        AvatarCategory("Video Games", listOf(
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
        )),
        AvatarCategory("Movies & TV", listOf(
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
        )),
        AvatarCategory("Superheroes", listOf(
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
        )),
        AvatarCategory("Abstract", listOf(
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
            return R.drawable.avatar_1
        }
        return AVATAR_MAP[name] ?: R.drawable.avatar_1
    }

    fun getAvatarSource(avatarRef: String): Any {
        return if (isCustomAvatar(avatarRef)) {
            getCustomAvatarFile(avatarRef) ?: R.drawable.avatar_1
        } else {
            getAvatarRes(avatarRef)
        }
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lumera/app/ui/profiles/ProfileAssets.kt
git commit -m "feat: expand ProfileAssets with 80 categorized character avatars"
```

---

### Task 4: Update ProfileViewModel for 2-Step Wizard

**Goal:** Change the wizard from 4 steps (0=selector, 1=name, 2=avatar, 3=theme) to 3 steps (0=selector, 1=name+avatar, 2=theme). Add a method that sets both name and avatar simultaneously.

**Files:**
- Modify: `app/src/main/java/com/lumera/app/ui/profiles/ProfileViewModel.kt`

**Acceptance Criteria:**
- [ ] `setWizardNameAndAvatar(name, avatarKey)` sets both values and advances to step 2 (theme)
- [ ] `startWizard()` still starts at step 1
- [ ] `goBackStep()` still works correctly
- [ ] Old `setWizardName()` and `setWizardAvatar()` are removed

**Verify:** Build the project: `./gradlew assembleDebug` → BUILD SUCCESSFUL

**Steps:**

- [ ] **Step 1: Replace setWizardName and setWizardAvatar with setWizardNameAndAvatar**

In `app/src/main/java/com/lumera/app/ui/profiles/ProfileViewModel.kt`, replace these two methods:

```kotlin
fun setWizardName(name: String) {
    tempName = name
    _wizardStep.value = 2
}

fun setWizardAvatar(avatarKey: String) {
    tempAvatarRef = avatarKey
    _wizardStep.value = 3
}
```

With this single method:

```kotlin
fun setWizardNameAndAvatar(name: String, avatarKey: String) {
    tempName = name
    tempAvatarRef = avatarKey
    _wizardStep.value = 2
}
```

- [ ] **Step 2: Update setWizardTheme to call finishWizard (no change needed)**

`setWizardTheme` already calls `finishWizard()` — no change needed. Theme is now step 2 instead of step 3, but the logic is the same.

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lumera/app/ui/profiles/ProfileViewModel.kt
git commit -m "feat: update ProfileViewModel for 2-step wizard (name+avatar combined)"
```

---

### Task 5: Build Combined WizardNameAvatarStep and Update ProfileScreen

**Goal:** Replace `WelcomeView`, `WizardNameStep`, and `WizardAvatarStep` with a single `WizardNameAvatarStep` composable. Update the wizard state machine in `ProfileScreen` to use the 2-step flow.

**Files:**
- Modify: `app/src/main/java/com/lumera/app/ui/profiles/ProfileScreen.kt`

**Acceptance Criteria:**
- [ ] `WelcomeView` composable is removed
- [ ] `WizardNameStep` and `WizardAvatarStep` composables are removed
- [ ] New `WizardNameAvatarStep` shows name input + avatar preview at top, categorized avatar rows below
- [ ] First-time users (no profiles) go straight to profile selector showing only "Add Profile" card
- [ ] Wizard step 1 = WizardNameAvatarStep, step 2 = WizardThemeStep
- [ ] D-pad navigation works: name field → avatar rows (vertical), avatars within row (horizontal)
- [ ] Selected avatar updates preview in real-time
- [ ] "Upload Your Own" button accessible at end of last avatar row
- [ ] Category labels show above each row

**Verify:** Build and run on Android TV emulator. Create a new profile — should show combined name+avatar screen, then theme screen.

**Steps:**

- [ ] **Step 1: Update the wizard state machine in ProfileScreen**

In `ProfileScreen` composable (around line 81-116), replace the `when (step)` block:

```kotlin
when (step) {
    0 -> {
        ProfileSelectorView(
            profiles = profiles,
            onSelect = onProfileSelected,
            onAdd = { viewModel.startWizard() },
            onEdit = { viewModel.startEditWizard(it) },
            onDelete = { viewModel.deleteProfile(it.id) },
            viewModel = viewModel
        )
    }
    1 -> WizardNameAvatarStep(
        initialName = viewModel.tempName,
        initialAvatar = viewModel.tempAvatarRef,
        onNext = { name, avatarKey -> viewModel.setWizardNameAndAvatar(name, avatarKey) },
        onCancel = { viewModel.cancelWizard() }
    )
    2 -> WizardThemeStep(
        onFinish = { viewModel.setWizardTheme(it) },
        onBack = { viewModel.goBackStep() }
    )
}
```

Note: The `WelcomeView` case is removed — when `profiles.isEmpty()`, the `ProfileSelectorView` will just show the "Add Profile" card (it already handles this via the existing `if (profiles.size < 6)` check). But we need to auto-start the wizard when there are no profiles.

- [ ] **Step 2: Auto-start wizard for first-time users**

In the `ProfileScreen` composable, add a `LaunchedEffect` before the `Box` to auto-start the wizard when there are no profiles and the wizard isn't already running:

```kotlin
LaunchedEffect(profiles.isEmpty(), wizardStep) {
    if (profiles.isEmpty() && wizardStep == 0) {
        viewModel.startWizard()
    }
}
```

- [ ] **Step 3: Remove WelcomeView, WizardNameStep, and WizardAvatarStep composables**

Delete the `WelcomeView` composable (lines 124-155), the `WizardNameStep` composable (lines 651-682), and the `WizardAvatarStep` composable (lines 685-765). Keep the `UploadAvatarButton` composable as it will be reused.

- [ ] **Step 4: Create the WizardNameAvatarStep composable**

Add this new composable in place of the deleted ones:

```kotlin
@Composable
fun WizardNameAvatarStep(
    initialName: String,
    initialAvatar: String,
    onNext: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedAvatar by remember { mutableStateOf(initialAvatar) }
    val nameFocusRequester = remember { FocusRequester() }
    val categories = ProfileAssets.AVATAR_CATEGORIES
    var showUploadDialog by remember { mutableStateOf(false) }

    val categoryFocusRequesters = remember(categories.size) {
        categories.map { cat ->
            List(cat.avatars.size) { FocusRequester() }
        }
    }
    val uploadButtonRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        nameFocusRequester.requestFocus()
    }
    BackHandler { onCancel() }

    val context = LocalContext.current
    val avatarSource = ProfileAssets.getAvatarSource(selectedAvatar)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Top section: Avatar preview + Name input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Large avatar preview
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarSource)
                        .size(300, 300)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(24.dp))

            // Name input
            Column {
                Text(
                    if (initialName.isEmpty()) "What should we call you?" else "Update your name",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.width(350.dp)) {
                    VoidInput(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Enter Name",
                        modifier = Modifier.focusRequester(nameFocusRequester),
                        onDone = {
                            if (name.isNotEmpty()) {
                                // Move focus to first avatar
                                categoryFocusRequesters.firstOrNull()?.firstOrNull()?.requestFocus()
                            }
                        }
                    )
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF1A1A1A))
        )

        Spacer(Modifier.height(16.dp))

        // Avatar category rows
        val scrollState = rememberLazyListState()
        androidx.compose.foundation.lazy.LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            categories.forEachIndexed { catIndex, category ->
                item(key = category.label) {
                    Column {
                        Text(
                            text = category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF9AA0A6),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        val rowState = rememberLazyListState()
                        val horizontalRepeatGate = remember {
                            DpadRepeatGate(horizontalRepeatIntervalMs = PROFILE_HORIZONTAL_REPEAT_INTERVAL_MS)
                        }

                        androidx.compose.foundation.lazy.LazyRow(
                            state = rowState,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                                ) {
                                    horizontalRepeatGate.shouldConsume(event)
                                } else {
                                    false
                                }
                            }
                        ) {
                            items(category.avatars.size) { avatarIndex ->
                                val avatar = category.avatars[avatarIndex]
                                AvatarGridItem(
                                    resId = avatar.resId,
                                    onClick = {
                                        selectedAvatar = avatar.key
                                        if (name.isNotEmpty()) {
                                            onNext(name, avatar.key)
                                        }
                                    },
                                    focusRequester = categoryFocusRequesters[catIndex][avatarIndex],
                                    modifier = Modifier.size(80.dp),
                                    onFocused = { selectedAvatar = avatar.key }
                                )
                            }
                        }
                    }
                }
            }

            // Upload button at the bottom
            item(key = "upload") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    UploadAvatarButton(
                        onClick = { showUploadDialog = true },
                        focusRequester = uploadButtonRequester
                    )
                }
            }
        }

        // Bottom hint
        Text(
            text = "← → Navigate  |  ↑ ↓ Switch Category  |  Enter Select",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF444444),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (showUploadDialog) {
        AvatarUploadDialog(
            onDismissRequest = { showUploadDialog = false },
            onAvatarReceived = { avatarPath ->
                showUploadDialog = false
                selectedAvatar = avatarPath
                if (name.isNotEmpty()) {
                    onNext(name, avatarPath)
                }
            }
        )
    }
}
```

- [ ] **Step 5: Add missing import for TextAlign**

Add to the imports at the top of `ProfileScreen.kt`:

```kotlin
import androidx.compose.ui.text.style.TextAlign
```

Then replace the inline `androidx.compose.ui.text.style.TextAlign.Center` with just `TextAlign.Center` in the bottom hint Text.

Also add these imports if not already present:

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
```

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Test on emulator**

1. Fresh install → should auto-start wizard (no welcome splash)
2. Combined screen shows: avatar preview + name input at top, categorized rows below
3. Type name → press Down → navigate avatar rows
4. Focus avatar → preview updates in real-time
5. Click avatar with name filled → advances to theme step
6. Click avatar without name → stays on screen (name required)
7. Theme step → select theme → profile created
8. Edit existing profile → same combined screen with pre-filled values

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lumera/app/ui/profiles/ProfileScreen.kt
git commit -m "feat: combined name+avatar wizard step, remove welcome splash"
```
