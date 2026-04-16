# Lumera Bug Fix Plan

## Issue 1: Player Pause/Resume Freezing

### Problem
When playing a show, clicking pause and then trying to resume sometimes works and sometimes freezes, requiring the user to back out and click play again.

### Root Cause
Located in `ExoPlayerBackend.kt` (line 512-526), the `play()` function has a problematic recovery mechanism:

```kotlin
override fun play() {
    if (released) return
    val player = exoPlayer ?: return
    val wasPaused = !player.playWhenReady
    player.play()
    if (wasPaused && player.playbackState == Player.STATE_READY) {
        // Force a codec flush on resume to prevent indefinite buffering on
        // certain MKV files. Seeking to current position + 1ms ensures
        // ExoPlayer doesn't optimise the seek away, while CLOSEST_SYNC
        // snaps to the nearest keyframe (no visible skip).
        val pos = player.currentPosition.coerceAtLeast(0L)
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        player.seekTo(pos + 1L)
    }
}
```

**Problems identified:**
1. **Race condition**: The `seekTo(pos + 1L)` is called immediately after `player.play()`. If the player hasn't fully transitioned to the playing state, this seek can cause the player to enter an inconsistent state, especially with network streams or torrent streams that may still be buffering.
2. **No error handling**: If the seek fails or the player is in STATE_BUFFERING, there's no fallback mechanism.
3. **State mismatch**: The `playWhenReady` flag and actual playback state can be out of sync, causing the seek to be applied at the wrong time.
4. **Torrent streams**: For torrent streams, the seek can fail if the required piece hasn't been downloaded yet.

### Files to Fix
- `app/src/main/java/com/lumera/app/ui/player/base/ExoPlayerBackend.kt` (line 512-526)

### Solution
```kotlin
override fun play() {
    if (released) return
    val player = exoPlayer ?: return
    
    // If already playing, do nothing
    if (player.playWhenReady && player.playbackState == Player.STATE_READY) return
    
    player.play()
    
    // Only apply codec flush seek when player is fully ready and stable
    if (player.playbackState == Player.STATE_READY && !isTorrentStream) {
        // Delay the seek slightly to ensure player state is stable after play()
        scope.launch {
            delay(100) // Small delay to let player stabilize
            val currentPlayer = exoPlayer ?: return@launch
            if (currentPlayer.playbackState == Player.STATE_READY) {
                val pos = currentPlayer.currentPosition.coerceAtLeast(0L)
                val savedSeekParams = currentPlayer.seekParameters
                try {
                    currentPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    currentPlayer.seekTo(pos + 1L)
                } finally {
                    currentPlayer.setSeekParameters(savedSeekParams)
                }
            }
        }
    }
}
```

**Additional improvements:**
- Add a `resumeRetryCount` to prevent infinite loops if the resume keeps failing
- Add logging to track when resume fails for debugging
- Consider adding a timeout mechanism to detect and recover from stuck states

---

## Issue 2: Watchlist and Watch History Are Global (Not Per-Profile)

### Problem
Profile watchlists and watch history are shared across all users. Each user should have their own taste separated from other users.

### Root Cause
The database tables `watchlist` and `watch_history` do not have a `profileId` column. All DAO queries operate on these tables without any profile filtering:

**WatchlistEntity.kt** (line 9-15):
```kotlin
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val poster: String?,
    val addedAt: Long
)
```

**WatchHistoryEntity.kt** (line 9-21):
```kotlin
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val poster: String?,
    val background: String? = null,
    val logo: String? = null,
    val position: Long,
    val duration: Long,
    val lastWatched: Long,
    val type: String,
    val watched: Boolean = false,
    val scrobbled: Boolean = false
)
```

**AddonDao.kt** queries (lines 79-80, 210-211):
```kotlin
@Query("SELECT * FROM watch_history ORDER BY lastWatched DESC")
fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

@Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
fun getWatchlist(): Flow<List<WatchlistEntity>>
```

### Files to Fix
1. `app/src/main/java/com/lumera/app/data/model/WatchlistEntity.kt` - Add `profileId` field
2. `app/src/main/java/com/lumera/app/data/model/WatchHistoryEntity.kt` - Add `profileId` field
3. `app/src/main/java/com/lumera/app/data/local/AddonDao.kt` - Update all queries to filter by `profileId`
4. `app/src/main/java/com/lumera/app/data/local/LumeraDatabase.kt` - Add migration for new columns
5. `app/src/main/java/com/lumera/app/ui/player/PlayerViewModel.kt` - Pass `profileId` when saving progress
6. `app/src/main/java/com/lumera/app/ui/watchlist/WatchlistViewModel.kt` - Pass `profileId` when querying
7. `app/src/main/java/com/lumera/app/ui/home/HomeViewModel.kt` - Pass `profileId` when loading history
8. `app/src/main/java/com/lumera/app/data/profile/ProfileConfigurationManager.kt` - Update snapshot save/restore
9. `app/src/main/java/com/lumera/app/ui/details/DetailsViewModel.kt` - Pass `profileId` for watchlist operations
10. `app/src/main/java/com/lumera/app/MainActivity.kt` - Pass `profileId` to all relevant ViewModels

### Solution

#### Step 1: Add `profileId` to entities

**WatchlistEntity.kt**:
```kotlin
@Entity(tableName = "watchlist", indices = [Index(value = ["profileId"])])
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val profileId: Int,  // NEW
    val type: String,
    val title: String,
    val poster: String?,
    val addedAt: Long
)
```

**WatchHistoryEntity.kt**:
```kotlin
@Entity(tableName = "watch_history", indices = [Index(value = ["profileId"])])
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val profileId: Int,  // NEW
    val title: String,
    val poster: String?,
    val background: String? = null,
    val logo: String? = null,
    val position: Long,
    val duration: Long,
    val lastWatched: Long,
    val type: String,
    val watched: Boolean = false,
    val scrobbled: Boolean = false
)
```

#### Step 2: Update DAO queries

**AddonDao.kt** - All watchlist queries:
```kotlin
@Query("SELECT * FROM watchlist WHERE profileId = :profileId ORDER BY addedAt DESC")
fun getWatchlist(profileId: Int): Flow<List<WatchlistEntity>>

@Query("SELECT * FROM watchlist WHERE profileId = :profileId AND type = :type ORDER BY addedAt DESC")
fun getWatchlistByType(profileId: Int, type: String): Flow<List<WatchlistEntity>>

@Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id AND profileId = :profileId)")
suspend fun isInWatchlist(id: String, profileId: Int): Boolean

@Query("DELETE FROM watchlist WHERE id = :id AND profileId = :profileId")
suspend fun removeFromWatchlist(id: String, profileId: Int)
```

**AddonDao.kt** - All watch history queries:
```kotlin
@Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatched DESC")
fun getWatchHistory(profileId: Int): Flow<List<WatchHistoryEntity>>

@Query("SELECT * FROM watch_history WHERE profileId = :profileId")
suspend fun getAllWatchHistoryOnce(profileId: Int): List<WatchHistoryEntity>

@Query("SELECT * FROM watch_history WHERE id = :id AND profileId = :profileId")
suspend fun getHistoryItem(id: String, profileId: Int): WatchHistoryEntity?
```

#### Step 3: Add database migration

**LumeraDatabase.kt** - Increment version and add migration:
```kotlin
@Database(
    entities = [...],
    version = 44,  // Increment from 43
    exportSchema = true
)
abstract class LumeraDatabase : RoomDatabase() {
    // Add migration
    private val MIGRATION_43_44 = object : Migration(43, 44) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE watchlist ADD COLUMN profileId INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE watch_history ADD COLUMN profileId INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_watchlist_profileId ON watchlist(profileId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_watch_history_profileId ON watch_history(profileId)")
        }
    }
}
```

#### Step 4: Update ViewModels to pass profileId

All ViewModels that interact with watchlist/watch history need to receive and use the current profile ID. The `MainViewModel` already tracks the active profile, so this can be passed down through the UI layer.

---

## Issue 3: Main Menu Lag (Especially Settings Tab and Scrolling)

### Problem
The main menu lags sometimes, especially in the settings tab and when scrolling through the main menu.

### Root Cause
Multiple performance issues identified:

#### 3a. Settings Screen - Multiple Animation Layers
**SettingsScreen.kt** (lines 105-119):
```kotlin
val rowStartPadding by animateDpAsState(...)
val colTopPadding by animateDpAsState(...)
val contentTopPadding by animateDpAsState(...)
val colStartPadding by animateDpAsState(...)
```
Four simultaneous `animateDpAsState` calls trigger recomposition on every frame during transitions.

#### 3b. Settings Screen - LazyColumn Not Used
The settings sidebar uses a regular `Column` with `forEach` instead of a `LazyColumn`, causing all items to be composed even when off-screen.

#### 3c. NavDrawer - Gradient and Animation Overhead
**NavDrawer.kt** (lines 108-163):
- Complex gradient brushes are recomposed on every focus change
- `AnimatedVisibility` with fade transitions for the expansion shadow
- `animateDpAsState` and `animateFloatAsState` for width and alpha

#### 3d. Home Screen - Heavy Compose Recompositions
The home screen likely has similar issues with:
- Multiple `StateFlow` collections triggering frequent recompositions
- Image loading without proper caching
- No `derivedStateOf` usage for expensive computations

### Files to Fix
1. `app/src/main/java/com/lumera/app/ui/settings/SettingsScreen.kt`
2. `app/src/main/java/com/lumera/app/ui/navigation/NavDrawer.kt`
3. `app/src/main/java/com/lumera/app/ui/home/HomeScreen.kt`
4. `app/src/main/java/com/lumera/app/ui/settings/SettingsSubScreens.kt`

### Solutions

#### 3a. Optimize Settings Screen Animations

**Combine animations into a single state class**:
```kotlin
data class SettingsLayoutState(
    val rowStartPadding: Dp,
    val colTopPadding: Dp,
    val contentTopPadding: Dp,
    val colStartPadding: Dp
)

val layoutState by animateStateAsState(
    targetValue = if (isTopNav) SettingsLayoutState(0.dp, 70.dp, 70.dp, 50.dp)
    else SettingsLayoutState(80.dp, 40.dp, 40.dp, 32.dp),
    animationSpec = tween(300),
    label = "SettingsLayout"
)
```

#### 3b. Use LazyColumn for Settings Sidebar

Replace the `Column` with `forEach` to a `LazyColumn`:
```kotlin
LazyColumn(
    modifier = Modifier
        .focusRequester(sidebarListRequester)
        .padding(top = 0.dp, bottom = 10.dp),
    contentPadding = PaddingValues(vertical = 8.dp)
) {
    items(SettingsSection.entries, key = { it.name }) { section ->
        // ... SettingsSidebarItem
    }
}
```

#### 3c. Optimize NavDrawer

- **Memoize gradient brushes** using `remember`:
```kotlin
val staticMaskGradient = remember(backgroundColor) {
    Brush.horizontalGradient(...)
}
```

- **Reduce animation complexity**: Replace `AnimatedVisibility` with simple alpha animation
- **Use `derivedStateOf`** for `showStaticMask` calculation

#### 3d. General Performance Improvements

- Add `key()` to all list items for stable identity
- Use `rememberUpdatedState` for lambdas passed to composables
- Implement `derivedStateOf` for expensive state computations
- Add `LaunchedEffect` debouncing for rapid state changes
- Use `snapshotFlow` to convert State to Flow where appropriate

---

## Implementation Priority

1. **Issue 1 (Player Pause/Resume)** - HIGH PRIORITY, smallest fix, immediate user impact
2. **Issue 2 (Per-Profile Data)** - HIGH PRIORITY, requires database migration, affects data integrity
3. **Issue 3 (Menu Lag)** - MEDIUM PRIORITY, multiple files, incremental improvements

## Testing Recommendations

### Issue 1 Testing
- Test pause/resume with regular HTTP streams
- Test pause/resume with HLS streams
- Test pause/resume with torrent streams
- Test pause/resume with MKV files
- Test rapid pause/resume cycles

### Issue 2 Testing
- Create two profiles, add different items to watchlist, verify separation
- Watch content on profile A, switch to profile B, verify history is separate
- Test profile deletion cleans up associated data
- Test database migration from version 43 to 44

### Issue 3 Testing
- Measure frame rate during settings navigation
- Test scrolling performance with 100+ items
- Profile recomposition counts using Android Studio Compose Inspector
- Test on low-end devices for regression
