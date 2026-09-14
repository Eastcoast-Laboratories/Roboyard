# Plan: Migrate remaining app/ logic to shared/ (DRY)

## Status: IN PROGRESS — Phase 6 (Android-First Alignment)

## Principle: Android app is the source of truth

The Android app is the main app with all features working. The ComposeApp must look and behave exactly like the Android app at the end. When in doubt, the Android app's logic wins.

## Phase 6 Progress

### Step 6.1: Extract shared GameController — ✅ DONE
- Created `shared/commonMain/.../GameController.kt` with:
  - `moveRobotWithCooldown()` — 400ms move cooldown (matches Android MOVE_COOLDOWN_MS)
  - `undoLastMove()` — undo via pathHistory (matches Android GameStateManager)
  - `isUndoMove()` — detects reverse moves as undo (matches Android logic)
  - `addPathToHistory()`, `removeLastPathFromHistory()`, `clearPathHistory()`
  - `canRobotMoveTo()` — wall/robot collision check
  - `reset()` — clear state for new game

### Step 6.2: Replace ComposeApp Board-based logic — ✅ DONE
- `isBoardSolved` in `BoardUtils.kt` updated to match Android `areAllRobotsAtTargets()`:
  - Checks ALL robots against ALL targets (not just goal robot)
  - Multi-color targets (robotNumber < 0) match any robot
  - Game complete when enough robots at targets (min of robotCount and goals.size)
- `GameScreen.kt` updated:
  - All `moveRobotOnBoard` calls replaced with `gameController.moveRobotWithCooldown`
  - Undo button uses `gameController.undoLastMove()` instead of boardHistory
  - `gameController.isGameComplete` synced with `gameWon` state
  - `gameController.reset()` called on Retry

### Step 6.3: Solver alignment — ✅ VERIFIED (no change needed)
- Android `SolverDD` uses `SolverIDDFS` internally (same as ComposeApp)
- Both apps use the same DriftingDroids solver algorithm
- No behavioral difference — SolverDD is just a wrapper for GameState→Board conversion

### Step 6.4: Save format alignment — ⏳ PENDING (larger refactor)
- Android uses `GameState.serialize()` (mapData-based format)
- ComposeApp uses custom format (Board-based)
- Aligning requires either:
  - Converting between Board and GameState for serialization, or
  - Making ComposeApp use GameState internally (major refactor)
- Current: each app can save/load its own format
- TODO: align save formats for cross-app compatibility

### Step 6.5: Remaining behavioral differences — ⏳ PENDING
- Sound/haptics
- UI layout/appearance details
- Hint system details

## What was migrated (Phases 1-5, completed)

| File | From | To | Notes |
|------|------|----|-------|
| `GameState.kt` | `app/` | `shared/commonMain` | Timber→RLog, Context→PlatformStorage, ResourceLoader for levels |
| `GameStateManagerCore.kt` | `app/` | `shared/commonMain` | 1:1 move, 0 Android deps |
| `StreakManager.kt` | `app/` | `shared/commonMain` | Context→PlatformStorage, Timber→RLog, AchievementCallback interface, DateUtils expect/actual |
| `AchievementManager.kt` | `app/` | `shared/commonMain` | Context→PlatformStorage, Toast→UiNotifier, Resources→StringProvider, BuildConfig→PlatformInfo, org.json→Gson, AchievementSyncClient/PlayGamesClient/StreakDataProvider interfaces |
| `GameUtils.kt` | (new) | `shared/commonMain` | formatTime, formatElapsedTime, buildGameWinMessage — extracted from GameScreen.kt |
| `BoardUtils.kt` | (new) | `shared/commonMain` | isBoardSolved, moveRobotOnBoard — extracted from GameScreen.kt (**NOTE: these are ComposeApp versions, need to be replaced with Android versions**)
| `BoardSerializer.kt` | (new) | `shared/commonMain` | serializeBoard, deserializeBoard — extracted from GameScreen.kt (**NOTE: ComposeApp format, need to align with Android save format**)

## Problem identified after Phase 5

The extracted functions (`BoardUtils.kt`, `BoardSerializer.kt`) came from the **ComposeApp**, not the Android app. This violates the "Android app is source of truth" principle:

| Function | ComposeApp version (current) | Android version (correct) |
|----------|------------------------------|---------------------------|
| `isBoardSolved` | Checks only goal robot on goal position | `GameState.checkCompletion()` → `areAllRobotsAtTargets()` — checks ALL robots against ALL matching targets |
| `moveRobotOnBoard` | Simple slide until wall/robot, no undo | `moveRobotInDirection()` with 400ms cooldown, pathHistory undo logic, `canRobotMoveTo()` |
| `serializeBoard` | Custom format `board:W,H;hX,Y;...` | Android save format with `GameState` serialization |
| Solver | `SolverIDDFS` (DriftingDroids) | `SolverDD` (shared/logic/solver/) |

## What was skipped (and why)

| File | Reason |
|------|--------|
| `MinimapGenerator.kt` | Different graphics APIs: Android Bitmap/Canvas vs Compose DrawScope |
| `DataExportImportManager.kt` | Only used in Android app, not in ComposeApp |
| `RoboyardApiClient.kt` | Only used in Android app, uses HttpURLConnection (would need ktor for KMP) |
| `SyncManager.kt` | Only used in Android app, depends on RoboyardApiClient |

## New infrastructure created in shared

- `PlatformInfo` (expect/actual) — OS version, language tag, app version, Play Games flag
- `DateUtils` (expect/actual) — timezone offset, ISO date formatting/parsing
- `AchievementCallback` — interface for streak→achievement notifications
- `AchievementSyncClient` — interface for server sync abstraction
- `StreakDataProvider` — interface for streak data access
- `PlayGamesClient` — interface for Play Games Services

## Android wrappers created in app/

- `AchievementManagerFactory` — Context-based factory, Android StringProvider, Android UiNotifier
- `StreakManagerFactory` — Context-based factory

---

## Phase 6: Android-First Alignment (NEW — in progress)

**Goal:** The ComposeApp must use the same game logic as the Android app. The Android app's `GameStateManager` has 118 functions, only 14 are Android-specific. The other 104 are pure game logic that should be shared.

### Architecture decision: Adapt, don't discard

The ComposeApp can be adapted — no need to discard it:
- **UI Screens stay:** 11 Compose screens (MainMenuScreen, GameScreen, etc.) are Compose-specific and can remain
- **Logic layer changes:** ComposeApp switches from `Board`-direct manipulation to `GameState`-based logic via a shared `GameController`
- **Solver changes:** ComposeApp switches from `SolverIDDFS` to `SolverDD`

### Phase 6 Steps

#### Step 6.1: Extract shared GameController from GameStateManager
- Extract the 104 platform-independent functions from `GameStateManager.kt` into `shared/commonMain/.../GameController.kt`
- Keep the 14 Android-specific functions in the Android `GameStateManager` wrapper
- GameController uses `GameState` (already in shared), not `Board` directly
- Includes: moveRobotInDirection (with cooldown, undo, pathHistory), checkCompletion, saveGame, loadGame, startNewGame, startLevelGame, etc.

#### Step 6.2: Replace ComposeApp Board-based logic with GameState-based
- `GameScreen.kt`: replace `Board` state with `GameState` state
- Replace `moveRobotOnBoard` calls with `GameController.moveRobotInDirection`
- Replace `isBoardSolved` calls with `GameController.checkCompletion`
- Replace `SolverIDDFS` with `SolverDD`
- Replace `serializeBoard`/`deserializeBoard` with Android save format

#### Step 6.3: Remove ComposeApp-specific duplicates
- Delete `BoardUtils.kt` (replaced by GameController)
- Delete `BoardSerializer.kt` (replaced by Android save format)
- Update `GameUtils.kt` if needed to match Android behavior

#### Step 6.4: Verify behavior parity
- Run Android smoke tests
- Run ComposeApp desktop tests
- Run PyAutoGUI tests (random_game_test.py, history_info_button_headed_test.py)
- Manual: compare behavior between Android and ComposeApp

## Verification

- `./gradlew :app:assembleDebug` — Android app still builds
- `./gradlew :shared:desktopTest` — shared tests pass
- `./gradlew :composeApp:desktopTest` — ComposeApp tests pass
- `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"` — smoke tests pass
- `cd composeApp && python3 random_game_test.py` — PyAutoGUI random game test passes
- `cd composeApp && python3 history_info_button_headed_test.py` — PyAutoGUI history test passes

## Rule: No functionality lost

After each phase, the Android app must keep all functionality:
- Level selection, completion data, stars, unlocks
- History entries, save/load slots
- Achievements, streaks
- Solver, hints, optimal moves
- Random game generation
- Settings, language switching
- Debug settings
