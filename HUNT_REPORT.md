# Issue Hunt Report — Lumera Codebase

**Date:** 2026-04-16
**Codebase:** Kotlin/Jetpack Compose Android TV Streaming App
**Files Scanned:** ~111 Kotlin source files
**Hunt Categories:** UI/UX Performance, Code Errors, Dead Code, Architecture

---

## Critical Issues (must fix)

| # | Category | File:Line | Description | Impact |
|---|----------|-----------|-------------|--------|
| C1 | Architecture | `MainActivity.kt:1-2214` | God Activity — 2,214 lines handling navigation, player lifecycle, torrent progress, Trakt sync, splash screen, update dialogs, profile switching, and 10+ responsibilities | Impossible to unit test; every change risks breaking unrelated features |
| C2 | Architecture | `BasePlayerScaffold.kt:1-3790` | 3,790-line composable containing entire player UI: controls, source picker, subtitle picker, audio picker, episode list, seek overlay, autoplay, torrent progress | Zero testability; any change risks breaking unrelated player features |
| C3 | Code Error | `HomeViewModel.kt:597-621` | Room Flow `collect` calls started in `loadScreen()` run for entire ViewModel lifetime. When `loadScreen("home")` is called after `invalidate()`, new collectors are created while old ones may still be active | Multiple collectors update state simultaneously, causing redundant work, stale data, and potential state inconsistency |
| C4 | Code Error | `PlayerViewModel.kt:33` | `saveProgress()` uses `NonCancellable` context — any database exception (full DB, corruption) crashes the app with unhandled exception, losing watch progress | App crash during playback exit; progress data lost |

## High Priority Issues

| # | Category | File:Line | Description | Impact |
|---|----------|-----------|-------------|--------|
| H1 | UI/UX | `Theme.kt:32-51` | 5 simultaneous `animateColorAsState` calls at root of composition tree — every theme change triggers full-app recomposition cascade | Visible jank during theme switch on low-end TV hardware |
| H2 | UI/UX | `HomeScreen.kt:97` | Monolithic `collectAsState()` on entire `HomeState` data class — any field change triggers full HomeScreen recomposition | Every state update (loading, rows, history, enrichedMeta, watchedIds) causes full recomposition |
| H3 | UI/UX | `BasePlayerScaffold.kt:183-186` | 4 independent `collectAsState()` calls on hot flows — `uiState` likely emits position updates multiple times per second | Excessive recompositions during playback |
| H4 | UI/UX | `BasePlayerScaffold.kt:298-307, 474-485` | Two competing `LaunchedEffect` blocks both request focus when `showControls` changes — `runCatching` swallows failures silently | Unpredictable focus behavior on TV; focus lands on wrong element |
| H5 | UI/UX | `HomeScreen.kt:1305` | `CinematicBackground` image request missing `.allowHardware(true)` — 1920x1080 software bitmap uses ~8MB per image | Peak 16MB during crossfade transitions; significant on 192-256MB heap TV devices |
| H6 | Code Error | `DetailsViewModel.kt:614-674` | `prefetchedStreams` race condition — `prefetchStreamsJob` can be cancelled and replaced between `join()` and null check, causing stale or null streams | Streams may fail to load or show wrong results after rapid navigation |
| H7 | Code Error | `HomeViewModel.kt:99-111` | Mutable collections (`loadingMoreRows`, `metadataRequestsInFlight`, `pendingRowItems`) accessed from multiple coroutines without synchronization | `ConcurrentModificationException` possible if `invalidate()` called during background work |
| H8 | Code Error | `AddonsScreen.kt:270` | `MainScope().launch` creates coroutine tied to app lifecycle, not composable lifecycle — calls `requestFocus()` on potentially disposed FocusRequester | Potential crash or undefined behavior on navigation away from Addons screen |
| H9 | Code Error | `TorrentService.kt:136` | Silent exception in progress polling loop (`catch (_: Exception) {}`) — if TorrServer crashes, loop continues indefinitely polling dead server | User sees frozen progress with no error message; CPU wasted |
| H10 | Code Error | `HomeViewModel.kt:175, 751` | Silent failures in `loadMoreItems` and `openHub` — exceptions swallowed, no error UI shown | User sees row stop loading or loading spinner forever with no feedback |
| H11 | Architecture | `DetailsViewModel.kt:334-415` | 80-line `computeAndStoreNextUp()` business logic in ViewModel — should be a domain use case | Not independently testable; not reusable for background sync |
| H12 | Architecture | `DetailsViewModel.kt:417-512` | `loadTmdbEnrichment()` handles TMDB ID resolution, parallel fetching, collection fetching, metadata merging — all in ViewModel | Complex data orchestration mixed with UI state management |
| H13 | Architecture | `PlayerScreen.kt:210-257` | `persistAndBack` lambda computes completion ratios, determines scrobble stop vs pause, decides mark-as-completed — all in UI layer | Business logic (90% threshold, 30-second rule) in composable, not testable |
| H14 | Architecture | `SettingsViewModel.kt:17-240` | 30+ nearly identical `updateXxx()` methods — each follows same pattern of fetch-profile, copy-property, insert-profile | Boilerplate; each new profile property requires another method |
| H15 | Architecture | `ProfileConfigurationManager.kt` | Five responsibilities: SharedPreferences management, JSON snapshot file I/O, auth coordination, runtime snapshot capture, default profile factory | Five reasons to change; should be split into 4 separate classes |
| H16 | Architecture | `PlayerBackendFactory.kt` + `PlayerScreen.kt:81` | `ExoPlayerBackend` created with `new` instead of through Hilt — cannot receive injected dependencies | Harder to test; prevents DI benefits |
| H17 | Architecture | `BasePlayerScaffold.kt:103-105` imports from `ui.details`; `DetailsScreen.kt:109` imports from `ui.home` | Cross-feature dependency chain: `player → details → home` — shared utilities in wrong packages | Inappropriate coupling; prevents independent feature development |
| H18 | Architecture | 5 separate `Gson()` instances across `ProfileConfigurationManager`, `AppUpdateManager`, `YouTubeExtractor`, `AddonRepository`, `StremioAuthService` | No consistent Gson configuration; wasted resources | Inconsistent JSON serialization; potential subtle bugs |

## Medium Priority Issues

| # | Category | File:Line | Description | Impact |
|---|----------|-----------|-------------|--------|
| M1 | UI/UX | `HeroCarousel.kt:360-377` | `animateDpAsState` called inside `forEachIndexed` loop — 10+ simultaneous animation instances for carousel indicators | Wasted memory and CPU; only one animates at a time |
| M2 | UI/UX | `NavDrawer.kt:304-330` | `SidebarItem` runs 5 concurrent `animate*AsState` calls, `ProfileAvatarItem` runs 6 — with 7 nav items = 77 simultaneous animation states | Jank during drawer expansion on low-end hardware |
| M3 | UI/UX | `InfiniteLoopRow.kt:353`, `HubRow.kt:162` | Lazy list keys include positional `$index` — insertion/removal causes cascading recomposition of all following items | Full row recomposition on any item change |
| M4 | UI/UX | `BasePlayerScaffold.kt:443-451` | Auto-hide `LaunchedEffect` has 9 keys — any state change cancels and restarts the timer | Controls may hide prematurely or stay visible indefinitely |
| M5 | UI/UX | `HomeScreen.kt:850-875` | `resolveLatestPreviewItem` performs deep sequence traversal across all mixed rows on every `mixedRows` or `enrichedMeta` change | O(N) main-thread scan triggered by TMDB enrichment arrival |
| M6 | UI/UX | `LumeraLandscapeCard.kt:148-161`, `BasePlayerScaffold.kt:1194-1207`, `DetailsScreen.kt:297-312` | Static gradient brushes created every recomposition — 15+ cards × 8 color stops = 120+ allocations per recomposition | Unnecessary GC pressure during scrolling |
| M7 | UI/UX | `NoiseOverlay.kt:46-59` | Canvas tile loop draws ~135 `drawImage` calls per frame on 1080p display — appears on hero, details, and nav drawer | Expensive draw loop running multiple times per frame |
| M8 | UI/UX | `DashboardEditorScreen.kt:525-1135` | 12+ instances of `LaunchedEffect(Unit) { delay(100); focusRequester.requestFocus() }` — generic focus restoration with timing hack | Focus lands on wrong element after dialogs on slower devices |
| M9 | UI/UX | `BasePlayerScaffold.kt:1253-1255`, line 1032 | Player control buttons at 36dp, "Next Episode" button at 40dp — below 48dp minimum | Too small for pointer input (phone remote apps) |
| M10 | Code Error | `ProfileScreen.kt:196,199,203` | `activeProfileForOptions!!` captured in lambdas that execute later — potentially after recomposition sets it to null | Crash if user triggers action during dialog dismissal |
| M11 | Code Error | `TorrentService.kt:34-36` | Static callback fields (`onStreamReady`, `onStreamError`, `onStreamProgress`) hold references to Activity/ViewModel | Activity/ViewModel leak if service outlives UI |
| M12 | Code Error | `ExoPlayerBackend.kt:103` | `onMagnetSourceSelected` callback not cleared in `release()` | Callback reference leak |
| M13 | Code Error | `TorrServerApi.kt:25-44` | `addTorrent` has no try-catch for `JsonSyntaxException` if response is invalid JSON | Unhandled exception propagates to caller |
| M14 | Code Error | `YouTubeExtractor.kt:412-421` | `performRequest` has no try-catch — `IOException` from network failure propagates unhandled | Crash on network failure |
| M15 | Code Error | `HomeViewModel.kt:568-711` | `loadJob?.cancel()` is cooperative — Flow collectors may keep emitting after cancellation until next checkpoint | Stale data could appear in UI during rapid navigation |
| M16 | Architecture | `SettingsSubScreens.kt:1-1892` | 1,892-line file mixing settings screens, reusable components, static data (43-item language list), and sidebar content | Four distinct concerns in one file |
| M17 | Architecture | `DashboardEditorScreen.kt:1-1683` | 1,683-line dashboard editor with tab management, catalog config, hero settings, hub management, and inline dialogs | Should be split into sub-package with separate files |
| M18 | Architecture | `DetailsScreen.kt:1-1365` | 1,365-line details screen with hero, cast rows, episode sidebar, dialogs, TMDB display, focus management | Too many concerns in one composable |
| M19 | Architecture | `HomeScreen.kt:1-1281` | 1,281-line file mixing CinematicLayout, SimpleLayout, data transformation, and focus resolution utilities | Two complete layout implementations + utilities in one file |
| M20 | Architecture | `DetailsScreen.kt:377`, `HomeScreen.kt:1343`, `HeroCarousel.kt:385`, +12 more files | `replaceFirstChar { it.uppercase() }` duplicated 25+ times across data layer, ViewModels, and 10+ UI files | Should be a single extension function |
| M21 | Architecture | `NavDrawer.kt`, `TopNavigationBar.kt`, `DetailsScreen.kt`, +7 files | `DpadRepeatGate` and `FocusPivotSpec` placed in `ui.home` but imported by 7 different feature modules | Inappropriate dependency on home feature |
| M22 | Architecture | `ui.addons` package | `VoidButton`, `VoidDialog`, `VoidInput` placed in `ui.addons` but imported by 10+ files from settings, profiles, home, and MainActivity | Generic components in wrong package |
| M23 | Architecture | 6 different SharedPreferences instances across `ProfileConfigurationManager`, `AppUpdateManager`, `TraktAuthManager`, `PlaybackTrackSelectionStore`, `SourceSelectionStore`, `StremioAuthManager` | No unified configuration abstraction; no single source of truth | Scattered preferences; hard to manage |
| M24 | Architecture | `TraktSyncManager.kt:1-755` | 755-line class handling playback sync, watchlist sync, watched sync, next-up computation, push/pull operations | Five distinct sync responsibilities |
| M25 | Architecture | 1,541 hardcoded dp values across UI files; hardcoded color values duplicated across files | No design tokens or dimension resources | Responsive design and theme changes difficult |

## Low Priority Issues

| # | Category | File:Line | Description | Impact |
|---|----------|-----------|-------------|--------|
| L1 | Dead Code | `TraktSyncManager.kt:617,625` | `markPendingDelete()` and `unmarkPendingDelete()` — defined but never called anywhere | Dead functions; `pendingDeletes` set never populated |
| L2 | Dead Code | `FocusPivotSpec.kt:53-59` | `scrollAnimationSpec` — deprecated with `HIDDEN` level, deliberately unreachable | Dead override |
| L3 | Dead Code | `PlayerBackendType.kt:1-5` | Enum with single value `EXOPLAYER` — no discrimination, factory `when` has one branch | Structural dead code |
| L4 | Dead Code | `PlayerBackendFactory.kt:12-21` | `when` expression with single branch — could be direct instantiation | Unnecessary indirection |
| L5 | Dead Code | `BasePlayerScaffold.kt:229-232` | `autoplayThresholdMode == "introdb"` branch — `"introdb"` is never set anywhere in codebase | Unreachable branch |
| L6 | Dead Code | `PlaybackSettings.kt:56-57` | `autoplayNextEpisode` and `autoSelectSource` — stored in profile but `ExoPlayerBackend` never reads them (`autoplayNextEpisode` IS used in PlayerScreen, but `autoSelectSource` is not) | `autoSelectSource` is dead; `autoplayNextEpisode` partially used |
| L7 | Dead Code | 78 wildcard imports across 27 files (e.g., `animation.*`, `foundation.*`, `layout.*`, `material3.*`, `runtime.*`) | Unnecessary imports pulling in unused symbols | Code smell; slower IDE performance |
| L8 | Dead Code | `GlassSidebar.kt:425` | HACK comment for DropdownMenu `containerColor` workaround | Should revisit with newer Compose M3 |
| L9 | Code Error | `DetailsViewModel.kt:177,267` | `nextUp!!` — protected by prior null check but fragile pattern | Crash risk if guard logic is modified |
| L10 | Code Error | `ExoPlayerBackend.kt:830-832` | `release()` not idempotent — `frameRateManager?.restoreOriginalMode()` called on every release | Minor display mode glitch on double-release |
| L11 | UI/UX | `HeroCarousel.kt:114-127` | Auto-scroll `while(true)` loop captures `currentIndex` from outer scope — stale value after effect restart | Carousel may auto-advance at wrong times |
| L12 | UI/UX | `HeroCarousel.kt:229-236`, `HomeScreen.kt:1301-1306` | 1920x1080 hardware bitmaps consume significant GPU memory; crossfade holds two simultaneously | GPU memory pressure on budget TV devices |
| L13 | UI/UX | `DetailsScreen.kt:134` | `LaunchedEffect(type, id)` missing `addonBaseUrl` as key — if URL changes with same type/id, effect won't re-execute | Stale addon URL causes stream/subtitle load failures |
| L14 | UI/UX | `SettingsScreen.kt:245-292` | Focus restored to sidebar item, not to specific element within sub-screen | Disorienting on TV when returning from deep settings |
| L15 | UI/UX | `HeroCarousel.kt:366-376` | Indicator dots only 4dp tall — essentially invisible on TV from 10 feet | Poor visual feedback |

---

## Dead Code Summary

| Category | Count | Details |
|----------|-------|---------|
| Dead Functions | 2 | `markPendingDelete`, `unmarkPendingDelete` in TraktSyncManager |
| Deprecated Override | 1 | `scrollAnimationSpec` in FocusPivotSpec (HIDDEN level) |
| Single-Value Enum | 1 | `PlayerBackendType` with only `EXOPLAYER` |
| Unreachable Branch | 1 | `autoplayThresholdMode == "introdb"` never set |
| Unused Parameter | 1 | `PlaybackSettings.autoSelectSource` not consumed by backend |
| Wildcard Imports | 27+ files | 78 wildcard imports across the codebase |
| HACK Comments | 1 | GlassSidebar DropdownMenu workaround |

---

## Architectural Concerns

### God Classes (8 files over 500 lines)
1. `BasePlayerScaffold.kt` — 3,790 lines (CRITICAL)
2. `MainActivity.kt` — 2,214 lines (CRITICAL)
3. `SettingsSubScreens.kt` — 1,892 lines (HIGH)
4. `DashboardEditorScreen.kt` — 1,683 lines (HIGH)
5. `DetailsScreen.kt` — 1,365 lines (HIGH)
6. `HomeScreen.kt` — 1,281 lines (HIGH)
7. `DetailsViewModel.kt` — 782 lines (HIGH)
8. `ExoPlayerBackend.kt` — 1,856 lines (MEDIUM)

### Dependency Direction Violations
- `ui.player.base` → imports from `ui.details` (GlassSidebar, GlassSidebarScaffold)
- `ui.details` → imports from `ui.home` (DpadRepeatGate, FocusPivotSpec)
- `ui.settings` → imports from `ui.addons` (VoidDialog) and `ui.details` (FilterDropdown)
- Shared utilities (`VoidButton`, `DpadRepeatGate`, `FocusPivotSpec`) placed in feature packages instead of `ui.components`

### Business Logic in Wrong Layer
- `DetailsViewModel`: next-up computation (80 lines), TMDB enrichment orchestration
- `PlayerScreen`: completion ratio logic, scrobble decision-making
- `HomeViewModel`: metadata merging, TMDB enrichment per-item, scroll position memory
- `AddonRepository`: catalog title formatting (presentation concern in data layer)

---

## Recommendations

### Immediate (fix this week)
1. **C4**: Wrap `dao.upsertHistory()` in try-catch in `PlayerViewModel.saveProgress()` — prevent app crash on database failure
2. **C3**: Cancel Flow collectors when switching away from "home" screen in `HomeViewModel.loadScreen()` — use a dedicated `homeCollectorJob` that gets cancelled
3. **H8**: Replace `MainScope().launch` with `LocalLifecycleOwner.lifecycleScope.launch` or a `rememberCoroutineScope()` in `AddonsScreen`
4. **H4**: Consolidate competing focus `LaunchedEffect` blocks in `BasePlayerScaffold` into a single effect with clear priority ordering
5. **L1**: Delete `markPendingDelete`/`unmarkPendingDelete` in TraktSyncManager — dead code

### Short-term (fix this month)
6. **H5**: Add `.allowHardware(true)` to `CinematicBackground` image request in `HomeScreen.kt`
7. **H6**: Fix `prefetchedStreams` race condition in `DetailsViewModel` — use a snapshot pattern or atomic reference
8. **H7**: Use `Mutex` or `ConcurrentHashMap` for mutable collections in `HomeViewModel` accessed from multiple coroutines
9. **H1**: Replace 5 individual `animateColorAsState` in `Theme.kt` with a single animated theme state or reduce to essential animations
10. **H2**: Split `HomeState` into multiple smaller StateFlows or use `derivedStateOf` for expensive derived values
11. **L3/L4**: Replace `PlayerBackendType` enum with direct instantiation or add a second backend type
12. **L6**: Delete `PlaybackSettings.autoSelectSource` or implement the auto-select logic

### Medium-term (next quarter)
13. **C1/C2**: Break up `MainActivity` and `BasePlayerScaffold` into smaller, focused components. Start by extracting player controls, source picker, and episode list into separate composables
14. **H11/H12/H13/H14**: Extract business logic from ViewModels into domain use cases: `ComputeNextUpUseCase`, `TmdbEnrichmentUseCase`, `PlaybackCompletionService`
15. **H15**: Split `ProfileConfigurationManager` into `ProfileSwitcher`, `ProfileSnapshotStore`, `ProfileAuthCoordinator`, `DefaultProfileFactory`
16. **H16**: Provide `ExoPlayerBackend` through Hilt or an assisted injection factory
17. **H17**: Move shared utilities (`DpadRepeatGate`, `FocusPivotSpec`, `VoidButton`, etc.) to `ui.components` or `ui.tv` package
18. **H18**: Provide a single DI-managed `Gson` instance with consistent configuration
19. **M3**: Remove `$index` from lazy list keys in `InfiniteLoopRow` and `HubRow` — use stable IDs only
20. **M6/M7**: Memoize static gradient brushes with `remember`; consider replacing `NoiseOverlay` draw loop with a pre-rendered tileable bitmap

### Long-term (ongoing)
21. **M23**: Create a centralized `PreferencesRepository` or `AppConfig` layer to replace 6 scattered SharedPreferences instances
22. **M25**: Extract design tokens (dimensions, colors, timing) into a `ui.theme` or `ui.design` module
23. **M20**: Create extension functions for duplicated patterns (`String.capitalizeFirst()`, rating formatting)
24. **L7**: Run Android Studio's "Optimize Imports" on all 27 files with wildcard imports
25. **M16/M17/M18/M19**: Split large files into smaller, focused files following the single responsibility principle
