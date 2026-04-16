# Profile Redesign & Cyberpunk Theme

## Summary

Three changes to the Lumera Android TV app:
1. Remove the "Welcome to Lumera" intro splash (WelcomeView)
2. Combine the Name and Avatar wizard steps into a single screen with 80+ categorized character avatars
3. Add a new "Cyberpunk" built-in theme

## 1. Remove Welcome Splash

**Current:** `WelcomeView` (ProfileScreen.kt lines 124-155) shows "WELCOME TO LUMERA" on first launch before profile selection.

**Change:** Remove WelcomeView entirely. First-time users go straight to the profile selector (which will show the "Add Profile" card since no profiles exist yet). Clicking "Add Profile" enters the wizard.

## 2. Combined Name + Avatar Screen

**Current flow:** 4 steps — Name → Avatar → Theme → Done
**New flow:** 2 steps — Name+Avatar → Theme → Done

### Layout (top to bottom)
- **Top section:** Large circular avatar preview (120dp) on the left, name input field on the right. Avatar preview updates in real-time as user navigates avatars below.
- **Avatar rows:** Categorized horizontal rows, vertically scrollable between categories. Each row scrolls horizontally with D-pad.

### Avatar Categories & Characters (80+ total)

**Anime (20 avatars):**
Goku, Naruto, Luffy, Levi Ackerman, Gojo Satoru, Tanjiro, Spike Spiegel, Vegeta, Kakashi, Itachi, Zoro, Saitama, Light Yagami, Eren Yeager, Killua, Gon, Deku, Todoroki, Sasuke, Mikasa

**Video Games (20 avatars):**
Master Chief, Kratos, Mario, Link, Cloud Strife, Geralt, Arthur Morgan, Solid Snake, Doom Slayer, Commander Shepard, Aloy, Ellie (TLOU), Jin Sakai, Kirby, Pikachu, Sonic, Samus, Mega Man, Pac-Man, Lara Croft

**Movies & TV (20 avatars):**
Darth Vader, Batman, Iron Man, The Mandalorian, John Wick, Gandalf, Jack Sparrow, Neo, Joker, Thanos, Yoda, Wolverine, Deadpool, Walter White, Eleven (Stranger Things), Tyrion Lannister, The Witcher, Baby Groot, Venom, Rick Sanchez

**Superheroes (20 avatars):**
Spider-Man, Superman, Wonder Woman, Black Panther, Thor, Captain America, Flash, Aquaman, Green Lantern, Doctor Strange, Scarlet Witch, Hulk, Black Widow, Nightwing, Raven, Cyborg, Shazam, Ant-Man, Vision, Hawkeye

### Avatar Assets
- Format: WebP, ~100-150KB each, 280x280px
- Storage: `res/drawable/avatar_anime_01.webp` through `avatar_hero_20.webp`
- Naming convention: `avatar_{category}_{nn}` where category is `anime`, `game`, `movie`, `hero`
- Total: 80 new avatars + keep existing 16 generic avatars in an "Abstract" category

### Avatar Map Update
- `ProfileAssets.kt` AVATAR_MAP expands from 16 entries to 96 entries
- Add `AVATAR_CATEGORIES` ordered map: category label → list of avatar keys
- Display name metadata per avatar for accessibility

### Navigation (TV D-pad)
- Screen opens with name field focused
- Down arrow moves to first avatar row
- Left/Right scrolls within a row
- Up/Down moves between rows (and back to name field from top row)
- Enter/Center selects avatar and proceeds to next step
- "Upload Your Own" button at the end of each row or as a floating action

## 3. Cyberpunk Theme

**Added to DefaultThemes.kt as the 9th built-in theme:**

| Slot | Value | Hex |
|------|-------|-----|
| primaryColor | Neon Cyan | 0xFF00E5FF |
| backgroundColor | Deep Dark Blue | 0xFF0A0E1A |
| surfaceColor | Dark Navy | 0xFF111833 |
| textColor | Ice White | 0xFFE0F7FA |
| textMutedColor | Soft Cyan | 0xFF4DD0E1 |
| errorColor | Neon Red | 0xFFFF1744 |

- id: "cyberpunk"
- name: "Cyberpunk"
- category: "colorful"
- isBuiltIn: true

## Files to Modify

| File | Change |
|------|--------|
| `ProfileScreen.kt` | Remove WelcomeView, combine WizardNameStep + WizardAvatarStep into WizardNameAvatarStep, update wizard state machine (3 states instead of 4) |
| `ProfileAssets.kt` | Expand AVATAR_MAP, add AVATAR_CATEGORIES, add display names |
| `ProfileViewModel.kt` | Update wizard step logic for 2-step flow |
| `DefaultThemes.kt` | Add CYBERPUNK theme constant |
| `res/drawable/` | Add 80 new avatar WebP files |

## Out of Scope
- No changes to theme system architecture (stays 6-color)
- No changes to theme editor, theme manager, or profile entity model
- No changes to card components, navigation, or player
- Existing 16 avatars retained in "Abstract" category
