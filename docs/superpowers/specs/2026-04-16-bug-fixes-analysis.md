# Lumera Bug Fixes Analysis

Analysis of three reported issues with root causes, affected files, and proposed solutions.

---

## Issue 1: Player Pause/Resume Intermittent Freeze

### Symptom
User pauses playback, tries to resume — sometimes works, sometimes freezes. Must back out and restart playback.

### Root Cause
**Race condition in `togglePlayPause()`** — reads stale UI state before the async ExoPlayer listener has updated it.

**Primary offender:** `app/src/main/java/com/lumera/app/ui/player/base/PlayerBackend.kt:18-20`

```kotlin
fun togglePlayPause() {
    if (uiState.value.playWhenReady) pause() else play()
}
```

### How the Freeze Happens
1. User presses pause at t=0ms. `togglePlayPause()` reads `playWhenReady = true`, calls `pause()`
2. ExoPlayer begins async state transition
3. `onPlayWhenReadyChanged` listener fires at t=~50ms, updates `_uiState` to `playWhenReady = false`
4. **If user presses play at t=45ms** (before listener fires), `togglePlayPause()` reads stale `playWhenReady = true` and calls `pause()` again instead of `play()`
5. Player appears frozen — UI shows paused but user pressed play

A secondary issue is the codec-flush seek in `ExoPlayerBackend.play()` at lines 517-524, which dispatches `seekTo(pos + 1L)` immediately after `play()`. If called twice rapidly, two seeks queue up and the player can stall on buffering.

### Affected Files
| File | Lines | Role |
|------|-------|------|
| `app/src/main/java/com/lumera/app/ui/player/base/PlayerBackend.kt` | 18-20 | `togglePlayPause()` uses stale state |
| `app/src/main/java/com/lumera/app/ui/player/base/ExoPlayerBackend.kt` | 517-524 | Codec-flush seek on resume can race |
| `app/src/main/java/com/lumera/app/ui/player/base/BasePlayerScaffold.kt` | 535-632, 688-692 | No debounce on DPAD_CENTER key |

### Proposed Fix
1. **Read directly from ExoPlayer, not from `_uiState`:**
   ```kotlin
   fun togglePlayPause() {
       val player = exoPlayer ?: return
       if (player.playWhenReady) pause() else play()
   }
   ```
   Move `togglePlayPause()` into `ExoPlayerBackend` (or expose a way to read live player state) since `PlayerBackend.kt` only has access to `uiState`.

2. **Add a mutex/lock around play/pause to prevent concurrent calls:**
   ```kotlin
   private val playPauseLock = Any()
   fun togglePlayPause() = synchronized(playPauseLock) {
       val player = exoPlayer ?: return
       if (player.playWhenReady) player.pause() else player.play()
   }
   ```

3. **Debounce DPAD_CENTER in `BasePlayerScaffold.kt`** — ignore events within 250ms of the previous one:
   ```kotlin
   var lastToggleMs = remember { mutableStateOf(0L) }
   // In key handler:
   val now = System.currentTimeMillis()
   if (now - lastToggleMs.value < 250L) return@onPreviewKeyEvent true
   lastToggleMs.value = now
   playbackController.togglePlayPause()
   ```

4. **Only apply codec-flush seek if paused for >1 second** (avoid double-seek on rapid toggles):
   ```kotlin
   val pausedDurationMs = System.currentTimeMillis() - pausedAtMs
   if (wasPaused && pausedDurationMs > 1000L && player.playbackState == Player.STATE_READY) {
       // existing seek logic
   }
   ```

---

## Issue 2: Watch History & Watchlist Are Global (Not Per-Profile)

### Symptom
Every profile sees the same watch history and watchlist instead of their own.

### Root Cause
**`WatchHistoryEntity` and `WatchlistEntity` have no `profileId` field** — they're stored in a single global table. The app uses a snapshot-based "swap everything on profile switch" approach, which breaks when Trakt sync runs (Trakt sync operates on the global table and mixes data across profiles).

### Affected Files
| File | Issue |
|------|-------|
| `app/src/main/java/com/lumera/app/data/model/WatchHistoryEntity.kt` | No `profileId` column |
| `app/src/main/java/com/lumera/app/data/model/WatchlistEntity.kt` | No `profileId` column |
| `app/src/main/java/com/lumera/app/data/local/AddonDao.kt` (lines 79-111, 210-258) | Queries have no `WHERE profileId = ?` filter |
| `app/src/main/java/com/lumera/app/data/local/LumeraDatabase.kt` | Schema version needs bump + migration |
| `app/src/main/java/com/lumera/app/data/trakt/TraktSyncManager.kt` (lines 131-188, 318-453) | Syncs write to global table |
| `app/src/main/java/com/lumera/app/data/profile/ProfileConfigurationManager.kt` (lines 87-106) | `replaceRuntimeState()` clears all profiles' data, merges by timestamp |

### Proposed Fix (Option A — Add `profileId` Foreign Key, Recommended)

1. **Add `profileId: Int` to both entities:**
   ```kotlin
   @Entity(
       tableName = "watch_history",
       primaryKeys = ["id", "profileId"],
       foreignKeys = [ForeignKey(
           entity = ProfileEntity::class,
           parentColumns = ["id"],
           childColumns = ["profileId"],
           onDelete = ForeignKey.CASCADE
       )],
       indices = [Index("profileId")]
   )
   data class WatchHistoryEntity(
       val id: String,
       val profileId: Int,
       // ...existing fields
   )
   ```
   Same change for `WatchlistEntity`.

2. **Update all DAO queries to filter by `profileId`:**
   ```kotlin
   @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatched DESC")
   fun getWatchHistory(profileId: Int): Flow<List<WatchHistoryEntity>>

   @Query("SELECT * FROM watchlist WHERE profileId = :profileId ORDER BY addedAt DESC")
   fun getWatchlist(profileId: Int): Flow<List<WatchlistEntity>>
   ```
   Every query that touches these tables needs `profileId` parameter.

3. **Bump database schema version** in `LumeraDatabase.kt` and add a migration:
   - Add `profileId INTEGER NOT NULL DEFAULT 0` columns
   - Assign existing rows to the first profile (or the last-active one) during migration
   - Update primary key constraint

4. **Inject current `profileId` into Trakt sync:**
   `TraktSyncManager.kt` already has access to `traktAuthManager.activeProfileId()` — pass it through every DAO call.

5. **Update `ProfileConfigurationManager.replaceRuntimeState()`** to only operate on the current profile's rows (use `deleteWatchHistoryForProfile(profileId)` instead of `clearWatchHistory()`).

6. **Remove watch history from `ProfileRuntimeSnapshot`** — it no longer needs to be snapshotted since each profile's data lives in the same table, queried by `profileId`.

### Why Not Option B (Separate Database Per Profile)
More invasive, more code, harder to add cross-profile features later (e.g., "shared watchlist"). Option A is cleaner.

---

## Issue 3: Main Menu & Settings UI Lag

### Symptom
Main menu lags during scroll. Settings tab is worst.

### Root Causes (Ranked by Impact)

#### A. `NoiseOverlay` draws nested tile loop on every recomposition
**File:** `app/src/main/java/com/lumera/app/ui/components/NoiseOverlay.kt:42-62`

Canvas runs a double `for` loop drawing 64+ tile images on every recomposition. The NavDrawer recomposes frequently during scroll, so this runs many times per second.

**Fix:** Wrap bitmap creation in `remember`, only recompute tile count when size changes.

#### B. `ProfileAvatarItem` rebuilds Coil `ImageRequest` on every recomposition
**File:** `app/src/main/java/com/lumera/app/ui/navigation/NavDrawer.kt:487-491`

```kotlin
AsyncImage(
    model = ImageRequest.Builder(context)  // new builder every recomposition
        .data(avatarSource)
        .size(100, 100)
        .crossfade(true)
        .build()
)
```

**Fix:** Memoize with `remember(avatarSource) { ... }`.

#### C. Too many `animate*AsState` calls per item
**Files:**
- `app/src/main/java/com/lumera/app/ui/navigation/NavDrawer.kt:297-323` (SidebarItem — 8 animations)
- `app/src/main/java/com/lumera/app/ui/navigation/NavDrawer.kt:409-442` (ProfileAvatarItem — 6 animations)
- `app/src/main/java/com/lumera/app/ui/settings/SettingsScreen.kt:335-341` (SettingsSidebarItem — 3 per item × 8 items)
- `app/src/main/java/com/lumera/app/ui/settings/SettingsSubScreens.kt:607-812` (many toggle/option rows)

Each `animate*AsState` is a separate state subscriber. With 8+ items × multiple animations, that's 64+ concurrent animation observers.

**Fix:** Combine into a single data class state holder computed via `remember(inputs) { ... }` or `derivedStateOf { ... }`. Use `.graphicsLayer { scaleX = s; scaleY = s }` instead of `.scale(s)` modifier (avoids extra layer).

#### D. `SettingsScreen` has a 300ms `LaunchedEffect` delay on every tab change
**File:** `app/src/main/java/com/lumera/app/ui/settings/SettingsScreen.kt:81-84`

```kotlin
LaunchedEffect(selectedSection) {
    delay(300)
    displayedSection = selectedSection
}
```

Combined with `AnimatedContent` fade transitions (lines 246-307), switching settings tabs triggers a chained 300ms+ animation storm. This is the single biggest contributor to settings-tab lag.

**Fix:** Remove the delay. Let `AnimatedContent` handle transitions — it already does.

#### E. `FocusRequester` created inside LazyColumn item lambda
**File:** `app/src/main/java/com/lumera/app/ui/settings/DashboardEditorScreen.kt:387-460`

```kotlin
itemsIndexed(items) { _, item ->
    val itemRequester = remember { FocusRequester() }  // new each reorder
}
```

Focus requesters get garbage-collected and recreated when list items reorder, breaking D-pad navigation and wasting work.

**Fix:** Keep a `Map<stableKey, FocusRequester>` outside the lambda:
```kotlin
val requesters = remember { mutableMapOf<String, FocusRequester>() }
itemsIndexed(items, key = { _, item -> item.key }) { _, item ->
    val itemRequester = requesters.getOrPut(item.key) { FocusRequester() }
}
```

#### F. Flow collections using `collectAsState()` instead of `collectAsStateWithLifecycle()`
**Files:**
- `app/src/main/java/com/lumera/app/ui/settings/IntegrationsScreen.kt:68,77`
- Various settings subscreens

`collectAsState()` keeps collecting even when the screen is off-screen. On TV, pausing sync when the settings screen isn't visible is a straight win.

**Fix:** Replace with `collectAsStateWithLifecycle()`.

### Priority Order
1. Remove the 300ms delay in `SettingsScreen` (biggest win, 5 min)
2. Fix `NoiseOverlay` Canvas loop (big win, 10 min)
3. Memoize `ImageRequest` in `ProfileAvatarItem` (medium win, 5 min)
4. Collapse `animate*AsState` into single state holders for `SidebarItem`, `ProfileAvatarItem`, `SettingsSidebarItem` (medium win, 30 min total)
5. Fix `FocusRequester` creation in `DashboardEditorScreen` (correctness + perf, 15 min)
6. Switch to `collectAsStateWithLifecycle()` everywhere (small win, 20 min)

---

## Summary Table

| Issue | Primary File | Severity | Est. Fix Time |
|-------|--------------|----------|---------------|
| Player freeze | `PlayerBackend.kt:18-20` | High (data-loss adjacent) | 30 min |
| Global watchlist/history | `WatchHistoryEntity.kt`, `WatchlistEntity.kt`, `AddonDao.kt` | Critical (data integrity) | 2-3 hours (+ migration) |
| NavDrawer/Settings lag | `NoiseOverlay.kt`, `SettingsScreen.kt:81`, `NavDrawer.kt:487` | Medium (UX) | 1-2 hours |

### Recommended Order
1. **Player freeze** — small, isolated, quick win
2. **UI lag** — improves everyday use immediately
3. **Per-profile data** — largest change, requires DB migration and careful testing (back up user data first)
