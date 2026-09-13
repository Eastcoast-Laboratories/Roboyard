# KMP Manager Migration — Fix Duplicate-Class Crash

**Date:** 2026-09-14
**Problem:** Android app crashed with `NoSuchMethodError: No static method initialize(Landroid/content/Context;)V` in `GameHistoryManager` when logging in (SyncManager → downloadHistory).

## Root Cause

Two classes with the same fully qualified name `roboyard.logic.managers.GameHistoryManager` existed in both `app/` (old `Context`-based API) and `shared/commonMain/` (new `PlatformStorage`-based KMP API). The same applied to `LevelCompletionManager`.

Commit `76e1ae5c` added a Gradle classpath-shadowing hack in `app/build.gradle` to make the app version win at compile time, but both classes still ended up in the APK's dex files. At runtime, the shared version was loaded (in `classes5.dex`), which didn't have `initialize(Context)`, causing the crash.

## Fix

Completed the KMP migration by making the shared versions functionally equivalent to the app versions, migrating all callers, then removing the app duplicates.

### Changes

#### Shared module — `shared/src/commonMain/kotlin/roboyard/logic/managers/GameHistoryManager.kt`
- Added `@JvmStatic` annotations to all public methods (required for Java callers).
- Added missing methods that the app version had:
  - `findByWallSignature(storage, wallSignature)` — same-walls achievement support.
  - `getUniqueMapCount(storage)` — unique map count.
  - `getUniqueCompletedLevelCount(storage)` — unique completed level count with level-key extraction.
  - `getUniqueThreeStarLevelCount(storage)` — unique 3-star level count.
  - `getCompletionCount(storage, mapSignature)` — completion count for a specific map.
  - `deleteHistoryEntry(storage, entry)` — delete by entry object (overload).
  - `extractLevelKey(entry)` — private helper for level-key extraction.
- Improved `migrateDifficultyStringToInt` to support both English and German localized difficulty strings (Anfänger, Fortgeschritten, verrückt, Unmöglich).
- Changed `HistoryEntryData.difficulty` field to `String?` for backward compatibility with old JSON formats where difficulty was stored as a string.
- Added `parseHistoryIndex` helper to handle both wrapped-object format (`{"historyEntries": [...]}`) and legacy direct-array format (`[...]`).

#### Shared module — `shared/src/commonMain/kotlin/roboyard/logic/managers/LevelCompletionManager.kt`
- Rewrote to use Gson for serialization (compatible with the Android app's existing data format).
- Changed from `object` to `class` with a `PlatformStorage` constructor parameter.
- Added `setUiNotifier(notifier)` for UI notification support.
- Added `getInstance(storage)` overload with `@JvmStatic` for Android callers.
- Kept `getInstance()` (no-arg) for ComposeApp/desktop use via `getPlatformStorage()`.
- Full data preservation: stars, moves, time, robot count, squares surpassed, optimal moves, hints shown, completed flag.
- Preserved all existing behavior: `saveLevelCompletionData` only updates better values, `resetAll`, `unlockAllStars`, `unlockStars(n)`, `lastPlayedLevel` getter/setter.

#### App module — migrated all callers
All `GameHistoryManager.X(context)` and `LevelCompletionManager.getInstance(context)` calls were changed to `GameHistoryManager.X(AndroidStorage.getInstance(context))` and `LevelCompletionManager.getInstance(AndroidStorage.getInstance(context))` respectively.

Files migrated:
- `app/src/main/java/roboyard/logic/managers/SyncManager.kt` (crash site — line 464)
- `app/src/main/java/roboyard/logic/managers/GameStateManager.kt`
- `app/src/main/java/roboyard/logic/achievements/AchievementManager.kt`
- `app/src/main/java/roboyard/logic/managers/DataExportImportManager.kt`
- `app/src/main/java/roboyard/ui/fragments/GameFragment.java`
- `app/src/main/java/roboyard/ui/fragments/LevelSelectionFragment.java`
- `app/src/main/java/roboyard/ui/fragments/SaveGameFragment.java`
- `app/src/main/java/roboyard/ui/fragments/DebugSettingsFragment.java`
- `app/src/main/java/roboyard/ui/fragments/LevelDesignEditorFragment.java`
- 15 Android test files under `app/src/androidTest/`

#### App module — deleted duplicate files
- Deleted `app/src/main/java/roboyard/logic/managers/GameHistoryManager.kt`
- Deleted `app/src/main/java/roboyard/logic/managers/LevelCompletionManager.kt`

#### App module — removed classpath-shadowing hack
- Removed the `afterEvaluate { tasks.withType(JavaCompile) ... }` block from `app/build.gradle` that manipulated the javac classpath to shadow the shared classes. This was a compile-time-only workaround that didn't fix the runtime duplicate-class problem.

## Post-Install Fixes (after first testing)

Two issues were found during testing and fixed:

### Fix A: LevelCompletionManager data loss (levels all locked, 0 stars)

**Root cause:** The old app version (before commit `8c1e21af`) stored completion data in a SharedPreferences file named `level_completion_prefs`. Commit `8c1e21af` migrated `LevelCompletionManager` to use `AndroidStorage` (which uses `roboyard_prefs`), but did NOT migrate the old data. The user's completion data was still in `level_completion_prefs`, but the new code read from the empty `roboyard_prefs`.

**Fix:** Added a one-time migration in `AndroidStorage.getInstance()` that copies all data from `level_completion_prefs` to `roboyard_prefs`, then clears the old file. This runs transparently on first `AndroidStorage` access.

File changed: `app/src/main/java/roboyard/platform/AndroidStorage.kt`

### Fix B: GameHistoryManager difficulty "Unknown difficulty string: '0'"

**Root cause:** The old app stored difficulty as an int (0-3) in the history JSON. My `migrateDifficultyStringToInt` function only handled localized string values ("beginner", "Anfänger", etc.) and didn't recognize numeric strings like "0".

**Fix:** Added numeric string handling at the top of `migrateDifficultyStringToInt` — if the string parses as an int, it's used directly as the difficulty ID.

File changed: `shared/src/commonMain/kotlin/roboyard/logic/managers/GameHistoryManager.kt`

## Verification

- `./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL** (clean build).
- `./gradlew :shared:desktopTest` — **BUILD SUCCESSFUL** (all tests pass).
- `./gradlew :composeApp:desktopTest` — 2 pre-existing failures (same before and after changes, unrelated to this migration).
- `./gradlew :app:compileDebugAndroidTestJavaWithJavac` — 7 pre-existing errors (same before and after changes, unrelated to this migration: `RRGameMove.move`, `Preferences.initialize`, `GameState.difficulty` private access).
- Verified no `GameHistoryManager.class` or `LevelCompletionManager.class` in app module's compiled output.
- `checkDebugDuplicateClasses` task passes.
- APK contains only one class definition per FQN (from the shared module).

## What was preserved

All Android functionality is preserved:
- History initialization, add/update/delete entries.
- Completion counting, unique map/level/3-star tracking.
- Wall-signature and map-signature lookup.
- History index management (next index, lookup, save).
- History file and preview-image deletion.
- Old JSON format loading and difficulty migration (English + German).
- Level completion data: stars, moves, time, robot count, squares surpassed, optimal moves, hints, completed flag.
- UI notifier support.
- `getInstance(context)` compatibility via `getInstance(AndroidStorage.getInstance(context))`.
- `@JvmStatic` annotations for all Java callers.
- ComposeApp's `getInstance()` no-arg path still works via `getPlatformStorage()`.
