# Roboyard Test Suite

This document describes all tests in the Roboyard project: what they test, how to run them, their tags, and their current status.

---

## How to Run Tests

### All Unit Tests (fast, no device required)
```bash
cd /var/www/Roboyard
./gradlew testDebugUnitTest
```

### Smoke Test only (run before every commit)
```bash
./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"
```

### All Instrumented/E2E Tests (requires connected device or emulator)
```bash
./gradlew connectedDebugAndroidTest
```

### Single Instrumented Test
```bash
adb shell am instrument -w \
  -e class roboyard.eclabs.ui.SaveLoadE2ETest \
  de.z11.roboyard.test/androidx.test.runner.AndroidJUnitRunner
```

### Filter by Tag (manual – grep for tag in file headers)
```bash
grep -rl "Tags:.*save" app/src/androidTest/
grep -rl "Tags:.*achievement" app/src/androidTest/
```

---

## Instrumented/E2E Tests (`app/src/androidTest/`)

These tests require a connected Android device or emulator.

| Class                         | Status    | Tests | Description                                                                                                                                                                                                        | Tags                                                                          |
| ----------------------------- | --------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| `AutoLoginPersistenceTest`    | ✅ Passing | 1     | Tests that auth token persists across app restarts. Registers new user, verifies token is saved, simulates app restart by clearing singleton, confirms token still valid and server accepts it.                    | auth, login, token-persistence, server-sync, auto-login                       |
| `HintAutoModeE2ETest`         | ✅ Passing | 3     | Tests all 3 hint auto-move modes: Manual (user moves robots), Full-Auto (robot moves automatically when hint shown), Semi-Auto (robot moves when next-hint button clicked). Uses Settings UI to change modes.      | hint-system, auto-move, settings, preferences, ui-navigation                  |
| `HistorySyncStarsTest`        | ⏳ Manual  | 1     | Verifies that stars are correctly synced to server after level completion. Uses logcat analysis via TestHelper to verify sync process. Requires manual level completion during test execution.                     | history-sync, stars, server-sync, logcat-analysis, manual-test                |

---

## Unit Tests (`app/src/test/`)

These tests run on the JVM without an Android device or emulator.

| Class                         | Status    | Tests | Description                                                                                                                                                                                                        | Tags                                                                          |
| ----------------------------- | --------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| `RoboyardSmokeTest`           | ✅ Passing | 67    | Comprehensive smoke test for all core game logic, data classes, and utility functions. Covers GameElement, GameMove, GridElement, MapObjects, Achievements, Constants, Save Data Parsing, Streak/Sync, Timestamps. | smoke-test, game-logic, data-classes, achievements, save-data, streak, sync   |
| `GameStateSaveLoadTest`       | ✅ Passing | 7     | Serialization of GameState to compact level format. Verifies robot positions (rr/rg/rb/ry), target positions (tr/tg/tb/ty), move counts, map name, board dimensions in metadata header.                            | save, load, serialization, compact-format, metadata, robots, targets          |
| `LevelFormatParserTest`       | ✅ Passing | 7     | Parsing and serialization of level format entries: basic entries, comments (#), line breaks, empty content, legacy format (mh/mv/target_color/robot_color), compact format.                                        | level-format, parsing, serialization, comments, legacy-format, compact-format |
| `MapSignatureTest`            | ✅ Passing | ~5    | Wall signatures, position signatures, and full map signatures for unique map tracking and achievement progress.                                                                                                    | map-signature, walls, positions, unique-map-tracking, achievements            |
| `MoveCountSyncTest`           | ✅ Passing | ~3    | MoveCount synchronization between GameState and history entries.                                                                                                                                                   | move-count, game-state, history, synchronization                              |
| `GameHistoryTest`             | ✅ Passing | ~19   | GameHistoryEntry unique map tracking, hint tracking, qualifiesForNoHints (updated: later hints no longer revoke qualification), recordSolvedWithoutHints(), optimalMoves persistence through intermediate-save path, bestMoves/movesMade tracking. | game-history, unique-map-tracking, map-completions, history-entry, no-hints, optimal-moves |
| `SyncManagerTest`             | ✅ Passing | ~8    | Save data parsing, map name extraction, streak/sync logic without Android context. Note: JSON tests are in androidTest (org.json not available in JVM).                                                            | sync, parsing, save-data, streak, timestamp, url-construction                 |
| `AchievementPopupMarginTest`  | ✅ Passing | 4     | Linear interpolation for AchievementPopup topMargin across screen sizes and orientations.                                                                                                                          | achievement-popup, ui-layout, responsive-design, interpolation                |
| `AchievementPopupPaddingTest` | ✅ Passing | 5     | Horizontal padding scaling via linear interpolation across screen widths with min/max clamping.                                                                                                                    | achievement-popup, ui-layout, responsive-design, padding, interpolation       |
| `SpecialTargetSaveLoadTest`   | ✅ Passing | 5     | Silver robot+target, multi-color target (COLOR_MULTI=-1) serialize as 'ts'/'tm', 2 targets with 1 robot, board targetColors array, synchronizeTargets preserves silver color.                                      | save, load, serialization, silver, multi-color, target, robot, edge-cases     |
| `DeepLinkBoardParsingTest`    | ✅ Passing | 10    | Deep-link compact format parsing: board:W,H; entry extraction, 12x12 board parsing (not 8x8), robot/target/wall counts, small/large board sizes, compact format detection.                                         | deep-link, board-size, compact-format, parsing, unit-test                     |
| `NoHintsQualificationTest`    | ✅ Passing | 11    | qualifiesForNoHintsAchievement() regression: after solving optimally without hints, later hint usage must NOT revoke qualification. Tests lastSolvedWithoutHints + lastPerfectlySolvedWithoutHints timestamps.       | no-hints, achievement, history, qualification, regression, unit-test          |
| `MultipleTargetsSolverTest`   | ✅ Passing | 3     | Multi-target solver support: single goal baseline, 2 goals (green+yellow robots verified at target positions), 3 goals (green+yellow+blue). Tests Board.activeGoals and SolverIDDFS multi-goal check.               | multi-target, solver, tdd, driftingdroids                                    |
| `LevelCompletionDataTest`    | ✅ Passing | 22    | LevelCompletionData save/update logic: stars only improve, optimalMoves not overwritten with 0, hintsShown always updated, direct hint field sync (maxHintUsed, everUsedHints, solvedWithoutHints, timestamps), hintsShown derived from maxHintUsed, restore from server with metadata, full round-trip (save→upload→download→restore). | level-completion, stars, optimal-moves, hints, restore, sync, round-trip      |

**Total unit tests: ~162 in 14 files, all passing.**

---

## Instrumented Tests (`app/src/androidTest/`)

These tests require a connected Android device or emulator.

### UI / E2E Tests (`roboyard.eclabs.ui`)

| Class                         | Status                   | Tests | Description                                                                                                        | Tags                                                                  |
| ----------------------------- | ------------------------ | ----- | ------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------- |
| [e] `UISmokeTest`         | ✅ Passing                | 1     | Fast UI smoke test (~28s): Settings, Random Game, Achievements, Save/Load, History tab, Level Selection, Debug Settings. Sets min=2/max=20 moves for instant map generation. | e2e, smoke-test, settings, navigation, espresso                       |
| [e] `SaveLoadE2ETest`         | ✅ Passing                | 1     | Save a random game to slot 1, verify no red cross (❌) indicator on the saved slot, load it, verify map ID matches. | save, load, serialization, target-validation, game-state, random-game |
| [e] `SaveLoadCombinationsE2ETest` | ✅ Passing                | 8     | Save in all edge-case combos: fresh, after hints, live-move-counter, after moves, hints+moves, load→re-save, main-menu-load→save, level-game→save. Verifies no ❌ red cross. | save, load, target-validation, hints, move-counter, level-game, edge-cases |
| [e] `Level1E2ETest`           | ✅ Passing                | 3     | Complete Level 1 using solver solution. Verifies level completion, achievements, star rating.                      | e2e, level-game, achievement, solver                                  |
| [e] `Level1FastE2ETest`       | ❌ Failing                | 1     | Complete Level 1 in under 10s — tests speedrun achievements.                                                       | e2e, level-game, achievement, solver, fast, speedrun, timer           |
| [e] `Level1SlowE2ETest`       | ❌ Failing                | 1     | Complete Level 1 in over 30s — verifies only `level_1_complete` is unlocked (no speedrun).                         | e2e, level-game, achievement, slow, timer                             |
| [e] `Level1WrongE2ETest`      | ❌ Failing                | 1     | Execute wrong moves — verifies level is NOT completed.                                                             | e2e, level-game, negative-test, wrong-moves                           |
| [e] `Level3E2ETest`           | ❌ Failing                | 1     | Complete levels 1–3 using solver. Tests level completion and achievements.                                         | e2e, level-game, achievement, solver                                  |
| [e] `Level10E2ETest`          | ❌ Failing                | 1     | Complete levels 1–10 to unlock `3_star_10_levels` achievement.                                                     | e2e, level-game, achievement, 3-star, solver                          |
| [e] `Level11With2StarsE2ETest` | ✅ Passing                | 1     | Complete 11 levels where Level 1 gets only 2 stars. Verifies `3_star_10_levels` is NOT unlocked.                   | e2e, level-game, achievement, 3-star, negative-test                   |
| [e] [long] `Level140E2ETest`  | ⏳ Not tested             | 1     | Complete all 140 levels. Unlocks `3_star_10_levels`, `3_star_50_levels`, `3_star_all_levels`. Long-running.        | e2e, level-game, achievement, 3-star, solver, all-levels              |
| `Level111DebugTest`       | ❌ Failing                | 1     | Debug test for Level 111 solver issue (board dimensions).                                                          | debug, level-game, solver, board-dimensions                           |
| [e] `GameModeMemoryE2ETest`   | ❌ Failing                | 1     | After completing Level 1 and navigating to Achievements, "Next Level" button still visible. No auto-regeneration.  | e2e, game-mode, level-game, achievements, navigation, next-level      |
| [e] `HistoryCompletionE2ETest` | ❌ Failing (2/5)          | 5     | Random game completion → history entry shows "Completions: 1". Also verifies that after moving all 4 robots and saving to slot 1, the save-slot info popup resolves the matching history entry for the same map. | e2e, history, random-game, completion-tracking, save-slot, map-signature, espresso              |
| [e] `HistorySyncE2ETest`      | ⚠️ Requires network      | 1     | Full sync round-trip: register → play random game → auto-upload → verify server → reset all data → re-login → download → verify restored fields (optimalMoves, maxHintUsed, everUsedHints, movesMade, bestTime, bestMoves, completionCount, completionTimestamps). Includes timezone validation. | e2e, history, sync, register, login, upload, download, round-trip, timezone     |
| [e] `HistoryReplayUpdateE2ETest` | ✅ Passing                | 1     | Play random game with extra moves, replay from history with optimal moves → bestMoves updated, completionCount=2. Proves history entries are correctly updated on replay. | e2e, history, random-game, replay, bestMoves, completion-tracking     |
| `HistoryReplayImprovementE2ETest` | ✅ Passing                | 1     | Tests that replaying a map with improved solution updates existing entry (not duplicate). Creates entry with 8 moves, simulates replay with 6 moves → bestMoves=6, completionCount=2, historySize=1. Validates mapSignature matching and migration of old entries without signature. | e2e, history, replay, improvement, map-signature, migration, deduplication     |
| [e] `LevelScreenMinimapE2ETest` | ✅ Passing                | 3     | After completing Level 1, level selection shows minimap + info-button; info popup shows "Completions: 1"; boardSize not hardcoded 16x16. | e2e, level-screen, minimap, info-button, history, level-game, solver, espresso |
| `HistoryStressTest`       | ✅ Passing                | 1     | Creates 60 history entries (3x 20) and monitors memory usage. Confirms memory is NOT an issue (only 1.2-2% usage). | e2e, history, memory, stress-test, debug-settings, espresso           |
| [e] `RandomGameHistoryMinimapTest` | ✅ Passing                | 1     | Start random game, make move, verify history minimap is displayed correctly (not dummy blue placeholder). Proves minimaps work for real games. | e2e, history, random-game, minimap, espresso                          |
| [e] `LiveMoveCounterE2ETest`  | ❌ Failing                | 4     | Live Move Counter: hint system regression, eye toggle, live counter after move.                                    | e2e, hint-system, live-move-counter, level-game, regression           |
| [e] `LiveMoveCounterGermanTest` | ✅ Passing              | 1     | Live Move Counter with German locale: verifies correct German UI strings.                                          | e2e, hint-system, live-move-counter, locale, german, espresso         |
| [e] `ResetHintE2ETest`        | ✅ Passing                | 1     | Reset button hides hint container and unchecks hint button.                                                        | e2e, hint-system, reset, live-move-counter, level-game                |
| [e] `ReverseMoveUndoBugTest`  | ❌ Failing                | 1     | Regression: after reverse-move undo, subsequent robot moves still work.                                            | regression, undo, reverse-move, level-game, bug-fix                   |
| [e] [long] `RandomGame11E2ETest` | ⏳ Not tested          | 1     | 11 random games testing `perfect_random_games_10` and `no_hints_streak_random_10` achievements. Long-running.      | e2e, random-game, achievement, hint-system, streak                    |
| [e] `TimerPersistenceE2ETest` | ✅ Passing                | 1     | Timer persists when navigating to menu and back.                                                                   | e2e, timer, navigation, persistence, random-game                      |
| [e] `LoadGameDifficultyTest`  | ❌ Failing                | 2     | Loading a saved game bypasses min/max move validation and shows the saved difficulty.                              | e2e, save-load, difficulty, random-game, settings                     |
| [e] `DeepLinkBoardSizeE2ETest` | ✅ Passing                | 5     | Deep-link with board:12,12; loads correctly as 12x12 (not 8x8). Verifies board dimensions, robot movement beyond 8x8, target accessibility, element loading. | e2e, deep-link, board-size, compact-format, ui-movement, espresso     |
| `AchievementsFragmentTest` | ❌ Failing                | 7     | Achievements screen navigation, list display, back button, filter/tab switching.                                   | achievements, ui, fragment, navigation, espresso                      |
| `AlternativeLayoutTest`   | ❌ Failing                | 1     | Enable alt layout in debug settings, verify correct widget types in game screen.                                   | ui, settings, debug-settings, alternative-layout, espresso            |
| `BackgroundSoundSettingsTest` | ❌ Failing                | 2     | Sound settings SeekBar display and persistence to Preferences.                                                     | ui, settings, sound, seekbar, espresso                                |
| [e] `DebugSettingsNavigationTest` | ✅ Passing                | 1     | Main Menu → Settings → 3s longpress → Debug Settings → Back.                                                       | ui, navigation, debug-settings, espresso                              |
| [e] `LevelEditorDebugTest`    | ❌ Failing (1/3 passes)   | 3     | Access Level Editor via Debug Settings; level files load correctly.                                                | ui, level-editor, debug-settings, navigation, espresso                |
| [e] `LevelEditorExportTest`   | ⚠️ Requires manual setup | 2     | Level Editor "Save to Sourcecode" feature. Requires `python3 dev/scripts/level_receiver.py` running on host.       | ui, level-editor, export, sourcecode, espresso                        |
| [e] `BoardSizeResetLevel2E2ETest` | ✅ Passing            | 1     | Board size should not reset on undo after Next Level. Completes Level 1 (10x10), clicks Next Level to Level 2 (10x12), makes move, undos - verifies board stays 10x12 (not reset to 10x10). Tests nothing, but you can look at it ⚠️ Requires manual confirmation | e2e, level-game, board-size, undo, grid-scaling, bug-reproduction     |
| `LevelEditorParsingTest`  | ❌ Failing                | 7     | Level file parsing: board:W,H; mhX,Y; mvX,Y; format, outer wall positions.                                         | level-editor, parsing, walls, board-dimensions, instrumented          |
| `PreComputationTest`          | ✅ Passing                | 4     | Pre-computation cache of next possible moves on a 19x19 map with known 21-move solution.                           | solver, pre-computation, moves-cache, instrumented                    |
| [e] [long] `ImpossibleDifficultyNewGameTest` | ⏳ Not tested | 1     | Multi-target mode (2 robots, 2 targets) + Impossible difficulty. Plays 25 consecutive games with 20s wait. Visible E2E test. Verifies no OOM crash. | e2e, multi-target, impossible, solver, oom, new-game, memory, stress-test |
| [e] `LevelSelectionLandscapeTest` | ❌ Failing            | 4     | Landscape orientation tests for level selection. Verifies: (1) level selection loads in landscape without crash, (2) scroll_up_arrow null check prevents crash, (3) card binding works correctly, (4) starting a level in landscape mode. Tests orientation handling and layout robustness. | e2e, landscape, level-selection, orientation, ui, null-safety |
| [e] `HintAutoModeE2ETest`     | ❌ Failing (1/3)          | 3     | Hint auto-move modes: full-auto, semi-auto, manual. testFullAutoMode fails.                                        | e2e, hint-system, auto-move, level-game, espresso                     |
| `AutoLoginPersistenceTest` | ✅ Passing               | 1     | Auto-login token persistence across app restart.                                                                   | auth, login, persistence, instrumented                                |
| `BackgroundSoundServiceTest` | ✅ Passing              | 2     | Background sound service start/stop and volume control.                                                            | sound, service, background, instrumented                              |
| `DebugHistoryTest`         | ❌ Failing                | 1     | Debug history creation and memory stats.                                                                           | debug, history, memory, instrumented                                  |
| `HistoryMinimapTest`       | ✅ Passing                | 1     | History minimap bitmap generation unit test.                                                                       | history, minimap, bitmap, instrumented                                |
| `HistoryPaginationTest`    | ❌ Failing                | 1     | Pagination with 200 history entries.                                                                               | history, pagination, stress-test, instrumented                        |
| `HistorySyncStarsTest`     | ✅ Passing                | 1     | History sync star rating round-trip.                                                                               | history, sync, stars, instrumented                                    |
| [e] [long] `PerfectRandom5E2ETest` | ❌ Failing           | 1     | 5 perfect random games unlock `perfect_random_games_5` achievement.                                                | e2e, random-game, achievement, perfect, espresso                      |
| `TestHelper`                  | N/A                      | —     | **Global test helper class with common methods.** Use in all new tests: startNewSessionWithEmptyStorage(), startAndWait8sForPopupClose(), startRandomGame(), startLevelGame(), openDebugScreen(), openLevelEditorThroughDebug(), openSettingsAndScrollDown(), navigateToSaveLoadScreen(), navigateToHistoryTab(), closeAchievementPopupIfPresent(). | test-helper, espresso, navigation, setup, common-methods              |

### Non-UI Instrumented Tests

| Class                    | Status              | Tests | Description                                                                                   | Tags                                                          |
| ------------------------ | ------------------- | ----- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `GameHistoryManagerTest` | ❌ Failing (1/15)    | 15    | History entry creation, retrieval, deletion, path handling. testMaxHistoryEntriesEnforced fails. | game-history, history-manager, history-entry, instrumented    |
| `WallSerializationTest`  | ❌ Failing           | 13    | Outer boundary wall serialization/deserialization (right/bottom edges at grid+1).             | walls, serialization, deserialization, boundary, instrumented |
| `MoveCountDebugTest`     | ✅ Passing           | 1     | Debug: play random level with optimal + 5 extra moves, verify move count in history.          | debug, move-count, history, solver, random-game               |
| `RoboyardApiClientTest`  | ⚠️ Requires network | 7     | Integration tests for user registration API against real `roboyard.z11.de`. Requires network. | api, network, registration, sync, integration-test            |
| `ShareParsingTest`       | ❌ Failing (1/4)     | 4     | Share URL parsing: compact metadata format (||R, |T, mv/mh in # line), SIZE field, multicolor targets, real save file round-trip, null/empty handling. testParseRealSaveFile fails. | share, parsing, save-data, compact-format, instrumented       |

### Achievement Tests (`roboyard.eclabs.achievements`)

| Class                             | Status              | Tests | Description                                                                            | Tags                                                        |
| --------------------------------- | ------------------- | ----- | -------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `AchievementManagerTest`          | ❌ Failing (3/31)    | 31    | Achievement unlocking, storage, and event handlers. 3 tests fail (allAchievementsLoaded, noHintsRandom10, noHintsRandomGames). | achievements, achievement-manager, unlock, storage, events  |
| `AchievementPopupTest`            | ✅ Passing           | 7     | AchievementPopup and unlock listener integration.                                      | ui, achievements, achievement-popup, unlock-listener        |
| `AchievementPopupCloseButtonTest` | ✅ Passing           | 2     | X close button appears when tapping popup and can be clicked.                          | ui, achievements, achievement-popup, close-button, espresso |
| `AchievementCounterTest`          | ✅ Passing           | 3     | Achievement counters increment correctly and unlock at the right threshold.            | achievements, counters, unlock-logic, level-completion      |
| `AchievementCallCountTest`        | ✅ Passing           | 3     | `onLevelCompleted` not called multiple times; achievements not double-counted.         | achievements, level-completion, call-count, regression      |
| `AchievementDebugTest`            | ✅ Passing           | 3     | Reproduce early unlock issue after Level 5.                                            | debug, achievements, unlock-timing, level-5, regression     |
| `AchievementResetTest`            | ✅ Passing           | 2     | Achievement counters reset correctly; no carry-over between tests.                     | achievements, reset, counters, test-isolation               |
| `AchievementSyncTest`             | ⚠️ Requires network | 5     | Achievement sync API structure, sync after unlock, sync only when logged in.           | achievements, sync, api, network, login                     |
| `GimmeFiveAchievementTest`        | ❌ Failing (4/7)     | 7     | `gimme_five` achievement: all robots touch each other at least once. 4 tests fail.     | achievements, gimme-five, robot-collision, game-logic       |
| `SameWallsAchievementTest`        | ✅ Passing           | 3     | `same_walls` achievement: detect when two maps share the same wall configuration.      | achievements, same-walls, wall-signature, instrumented      |
| `StreakManagerTest`               | ✅ Passing           | 7     | StreakManager daily login tracking, streak increments/resets, achievement integration. | streak, daily-login, achievement-manager, instrumented      |
| `ThreeStarAchievementTest`        | ❌ Failing (5/6)     | 6     | `3_star_hard_level` achievement: complete levels with 5+ optimal moves with 3 stars. 5 tests fail. | achievements, 3-star, hard-level, solver, level-data        |

### Data / Generator Tests (`roboyard.eclabs.data`)

| Class                        | Status                           | Tests | Description                                                                                                                                     | Tags                                                                 |
| ---------------------------- | -------------------------------- | ----- | ----------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| `LevelSolutionGeneratorTest` | ⚠️ Manual / data generation only | 2     | Solves all 140 levels, generates optimal move data for `LevelSolutionData.java`. Output in logcat tag `LEVEL_SOLUTION_DATA`. Run manually only. | solver, level-data, solution-generation, all-levels, data-generation |
| `LevelSolutionData`          | N/A                              | —     | Stub class with optimal move counts per level. Populated by `LevelSolutionGeneratorTest`.                                                       | —                                                                    |

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ Passing | Test passes consistently |
| ⚠️ Requires setup | Test needs external setup (network, running server, etc.) |
| ❌ Failing | Test is broken and needs fixing |
| 🚫 Obsolete | Test is no longer relevant and should be removed |

---

## Test Categories by Tag

Use these tags to find tests relevant to a specific feature:

| Feature | Relevant Tags | Example command |
|---------|--------------|-----------------|
| Save/Load | `save`, `save-load`, `serialization` | `grep -rl "Tags:.*save" app/src/` |
| Achievements | `achievements`, `achievement-manager` | `grep -rl "Tags:.*achievement" app/src/` |
| Level Game | `level-game`, `e2e`, `solver` | `grep -rl "Tags:.*level-game" app/src/` |
| Random Game | `random-game` | `grep -rl "Tags:.*random-game" app/src/` |
| Hint System | `hint-system` | `grep -rl "Tags:.*hint-system" app/src/` |
| Walls | `walls`, `serialization` | `grep -rl "Tags:.*walls" app/src/` |
| History | `history`, `game-history` | `grep -rl "Tags:.*history" app/src/` |
| Streak | `streak`, `daily-login` | `grep -rl "Tags:.*streak" app/src/` |
| UI Navigation | `navigation`, `espresso` | `grep -rl "Tags:.*navigation" app/src/` |
| Regression | `regression`, `bug-fix` | `grep -rl "Tags:.*regression" app/src/` |
| Debug | `debug` | `grep -rl "Tags:.*debug" app/src/` |
| Network/API | `api`, `network`, `sync` | `grep -rl "Tags:.*api" app/src/` |

---

## Notes

- **`LevelEditorExportTest`**: Requires a running Python receiver: `python3 dev/scripts/level_receiver.py`. Skip in CI unless setup is available.
- **`RoboyardApiClientTest`** and **`AchievementSyncTest`**: Require network access to `roboyard.z11.de`. Skip in CI without network.
- **`Level140E2ETest`**, **`RandomGame11E2ETest`**, **`ImpossibleDifficultyNewGameTest`**, and **`PerfectRandom5E2ETest`**: Very long-running (minutes). Run only for release validation.
- **`LevelSolutionGeneratorTest`**: Not a regression test — only run to regenerate `LevelSolutionData.java` stub data.
- **`Level111DebugTest`**: Debug-only test for investigating a specific solver edge case. Not a regression test.
- **`MoveCountDebugTest`** and **`AchievementDebugTest`**: Debug tests for past issues, kept for reference.
