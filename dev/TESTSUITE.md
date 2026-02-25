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

## Unit Tests (`app/src/test/`)

These tests run on the JVM without an Android device or emulator.

| Class                         | Status    | Tests | Description                                                                                                                                                                                                        | Tags                                                                          |
| ----------------------------- | --------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| `RoboyardSmokeTest`           | ✅ Passing | 67    | Comprehensive smoke test for all core game logic, data classes, and utility functions. Covers GameElement, GameMove, GridElement, MapObjects, Achievements, Constants, Save Data Parsing, Streak/Sync, Timestamps. | smoke-test, game-logic, data-classes, achievements, save-data, streak, sync   |
| `GameStateSaveLoadTest`       | ✅ Passing | 7     | Serialization of GameState to compact level format. Verifies robot positions (rr/rg/rb/ry), target positions (tr/tg/tb/ty), move counts, map name, board dimensions in metadata header.                            | save, load, serialization, compact-format, metadata, robots, targets          |
| `LevelFormatParserTest`       | ✅ Passing | 7     | Parsing and serialization of level format entries: basic entries, comments (#), line breaks, empty content, legacy format (mh/mv/target_color/robot_color), compact format.                                        | level-format, parsing, serialization, comments, legacy-format, compact-format |
| `MapSignatureTest`            | ✅ Passing | ~5    | Wall signatures, position signatures, and full map signatures for unique map tracking and achievement progress.                                                                                                    | map-signature, walls, positions, unique-map-tracking, achievements            |
| `MoveCountSyncTest`           | ✅ Passing | ~3    | MoveCount synchronization between GameState and history entries.                                                                                                                                                   | move-count, game-state, history, synchronization                              |
| `GameHistoryTest`             | ✅ Passing | ~5    | GameHistoryEntry unique map tracking fields and methods for tracking map completions.                                                                                                                              | game-history, unique-map-tracking, map-completions, history-entry             |
| `SyncManagerTest`             | ✅ Passing | ~8    | Save data parsing, map name extraction, streak/sync logic without Android context. Note: JSON tests are in androidTest (org.json not available in JVM).                                                            | sync, parsing, save-data, streak, timestamp, url-construction                 |
| `AchievementPopupMarginTest`  | ✅ Passing | 4     | Linear interpolation for AchievementPopup topMargin across screen sizes and orientations.                                                                                                                          | achievement-popup, ui-layout, responsive-design, interpolation                |
| `AchievementPopupPaddingTest` | ✅ Passing | 5     | Horizontal padding scaling via linear interpolation across screen widths with min/max clamping.                                                                                                                    | achievement-popup, ui-layout, responsive-design, padding, interpolation       |

**Total unit tests: ~111 in 9 files, all passing.**

---

## Instrumented Tests (`app/src/androidTest/`)

These tests require a connected Android device or emulator.

### UI / E2E Tests (`roboyard.eclabs.ui`)

| Class                         | Status                   | Tests | Description                                                                                                        | Tags                                                                  |
| ----------------------------- | ------------------------ | ----- | ------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------- |
| `SaveLoadE2ETest`             | ✅ Passing                | 1     | Save a random game to slot 1, verify no red cross (❌) indicator on the saved slot, load it, verify map ID matches. | save, load, serialization, target-validation, game-state, random-game |
| `SaveLoadCombinationsE2ETest` | ✅ Passing                | 8     | Save in all edge-case combos: fresh, after hints, live-move-counter, after moves, hints+moves, load→re-save, main-menu-load→save, level-game→save. Verifies no ❌ red cross. | save, load, target-validation, hints, move-counter, level-game, edge-cases |
| `Level1E2ETest`               | ✅ Passing                | 3     | Complete Level 1 using solver solution. Verifies level completion, achievements, star rating.                      | e2e, level-game, achievement, solver                                  |
| `Level1FastE2ETest`           | ✅ Passing                | 1     | Complete Level 1 in under 10s — tests speedrun achievements.                                                       | e2e, level-game, achievement, solver, fast, speedrun, timer           |
| `Level1SlowE2ETest`           | ✅ Passing                | 1     | Complete Level 1 in over 30s — verifies only `level_1_complete` is unlocked (no speedrun).                         | e2e, level-game, achievement, slow, timer                             |
| `Level1WrongE2ETest`          | ✅ Passing                | 1     | Execute wrong moves — verifies level is NOT completed.                                                             | e2e, level-game, negative-test, wrong-moves                           |
| `Level3E2ETest`               | ✅ Passing                | 1     | Complete levels 1–3 using solver. Tests level completion and achievements.                                         | e2e, level-game, achievement, solver                                  |
| `Level10E2ETest`              | ✅ Passing                | 1     | Complete levels 1–10 to unlock `3_star_10_levels` achievement.                                                     | e2e, level-game, achievement, 3-star, solver                          |
| `Level11With2StarsE2ETest`    | ✅ Passing                | 1     | Complete 11 levels where Level 1 gets only 2 stars. Verifies `3_star_10_levels` is NOT unlocked.                   | e2e, level-game, achievement, 3-star, negative-test                   |
| `Level140E2ETest`             | ✅ Passing                | 1     | Complete all 140 levels. Unlocks `3_star_10_levels`, `3_star_50_levels`, `3_star_all_levels`. Long-running.        | e2e, level-game, achievement, 3-star, solver, all-levels              |
| `Level111DebugTest`           | ✅ Passing                | 1     | Debug test for Level 111 solver issue (board dimensions).                                                          | debug, level-game, solver, board-dimensions                           |
| `GameModeMemoryE2ETest`       | ✅ Passing                | 1     | After completing Level 1 and navigating to Achievements, "Next Level" button still visible. No auto-regeneration.  | e2e, game-mode, level-game, achievements, navigation, next-level      |
| `HistoryCompletionE2ETest`    | ✅ Passing                | 4     | Random game completion → history entry shows "Completions: 1".                                                     | e2e, history, random-game, completion-tracking, espresso              |
| `LevelScreenMinimapE2ETest`   | ✅ Passing                | 3     | After completing Level 1, level selection shows minimap + info-button; info popup shows "Completions: 1"; boardSize not hardcoded 16x16. | e2e, level-screen, minimap, info-button, history, level-game, solver, espresso |
| `LiveMoveCounterE2ETest`      | ✅ Passing                | 4     | Live Move Counter: hint system regression, eye toggle, live counter after move.                                    | e2e, hint-system, live-move-counter, level-game, regression           |
| `ResetHintE2ETest`            | ✅ Passing                | 1     | Reset button hides hint container and unchecks hint button.                                                        | e2e, hint-system, reset, live-move-counter, level-game                |
| `ReverseMoveUndoBugTest`      | ✅ Passing                | 1     | Regression: after reverse-move undo, subsequent robot moves still work.                                            | regression, undo, reverse-move, level-game, bug-fix                   |
| `RandomGame11E2ETest`         | ✅ Passing                | 1     | 11 random games testing `perfect_random_games_10` and `no_hints_streak_random_10` achievements. Long-running.      | e2e, random-game, achievement, hint-system, streak                    |
| `TimerPersistenceE2ETest`     | ✅ Passing                | 1     | Timer persists when navigating to menu and back.                                                                   | e2e, timer, navigation, persistence, random-game                      |
| `LoadGameDifficultyTest`      | ✅ Passing                | 2     | Loading a saved game bypasses min/max move validation and shows the saved difficulty.                              | e2e, save-load, difficulty, random-game, settings                     |
| `AchievementsFragmentTest`    | ✅ Passing                | 7     | Achievements screen navigation, list display, back button, filter/tab switching.                                   | achievements, ui, fragment, navigation, espresso                      |
| `AlternativeLayoutTest`       | ✅ Passing                | 1     | Enable alt layout in debug settings, verify correct widget types in game screen.                                   | ui, settings, debug-settings, alternative-layout, espresso            |
| `BackgroundSoundSettingsTest` | ✅ Passing                | 2     | Sound settings SeekBar display and persistence to Preferences.                                                     | ui, settings, sound, seekbar, espresso                                |
| `DebugSettingsNavigationTest` | ✅ Passing                | 1     | Main Menu → Settings → 3s longpress → Debug Settings → Back.                                                       | ui, navigation, debug-settings, espresso                              |
| `LevelEditorDebugTest`        | ✅ Passing                | 3     | Access Level Editor via Debug Settings; level files load correctly.                                                | ui, level-editor, debug-settings, navigation, espresso                |
| `LevelEditorExportTest`       | ⚠️ Requires manual setup | 2     | Level Editor "Save to Sourcecode" feature. Requires `python3 dev/scripts/level_receiver.py` running on host.       | ui, level-editor, export, sourcecode, espresso                        |
| `LevelEditorParsingTest`      | ✅ Passing                | 7     | Level file parsing: board:W,H; mhX,Y; mvX,Y; format, outer wall positions.                                         | level-editor, parsing, walls, board-dimensions, instrumented          |
| `PreComputationTest`          | ✅ Passing                | 4     | Pre-computation cache of next possible moves on a 19x19 map with known 21-move solution.                           | solver, pre-computation, moves-cache, instrumented                    |
| `TestHelper`                  | N/A                      | —     | Shared test utilities: close achievement/streak popups. Not a test class itself.                                   | test-helper, espresso, achievement-popup, streak-popup                |

### Non-UI Instrumented Tests

| Class                    | Status              | Tests | Description                                                                                   | Tags                                                          |
| ------------------------ | ------------------- | ----- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `GameHistoryManagerTest` | ✅ Passing           | 15    | History entry creation, retrieval, deletion, path handling.                                   | game-history, history-manager, history-entry, instrumented    |
| `WallSerializationTest`  | ✅ Passing           | 13    | Outer boundary wall serialization/deserialization (right/bottom edges at grid+1).             | walls, serialization, deserialization, boundary, instrumented |
| `MoveCountDebugTest`     | ✅ Passing           | 1     | Debug: play random level with optimal + 5 extra moves, verify move count in history.          | debug, move-count, history, solver, random-game               |
| `RoboyardApiClientTest`  | ⚠️ Requires network | 7     | Integration tests for user registration API against real `roboyard.z11.de`. Requires network. | api, network, registration, sync, integration-test            |

### Achievement Tests (`roboyard.eclabs.achievements`)

| Class                             | Status              | Tests | Description                                                                            | Tags                                                        |
| --------------------------------- | ------------------- | ----- | -------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `AchievementManagerTest`          | ✅ Passing           | 31    | Achievement unlocking, storage, and event handlers.                                    | achievements, achievement-manager, unlock, storage, events  |
| `AchievementPopupTest`            | ✅ Passing           | 7     | AchievementPopup and unlock listener integration.                                      | ui, achievements, achievement-popup, unlock-listener        |
| `AchievementPopupCloseButtonTest` | ✅ Passing           | 2     | X close button appears when tapping popup and can be clicked.                          | ui, achievements, achievement-popup, close-button, espresso |
| `AchievementCounterTest`          | ✅ Passing           | 3     | Achievement counters increment correctly and unlock at the right threshold.            | achievements, counters, unlock-logic, level-completion      |
| `AchievementCallCountTest`        | ✅ Passing           | 3     | `onLevelCompleted` not called multiple times; achievements not double-counted.         | achievements, level-completion, call-count, regression      |
| `AchievementDebugTest`            | ✅ Passing           | 3     | Reproduce early unlock issue after Level 5.                                            | debug, achievements, unlock-timing, level-5, regression     |
| `AchievementResetTest`            | ✅ Passing           | 2     | Achievement counters reset correctly; no carry-over between tests.                     | achievements, reset, counters, test-isolation               |
| `AchievementSyncTest`             | ⚠️ Requires network | 5     | Achievement sync API structure, sync after unlock, sync only when logged in.           | achievements, sync, api, network, login                     |
| `GimmeFiveAchievementTest`        | ✅ Passing           | 7     | `gimme_five` achievement: all robots touch each other at least once.                   | achievements, gimme-five, robot-collision, game-logic       |
| `StreakManagerTest`               | ✅ Passing           | 7     | StreakManager daily login tracking, streak increments/resets, achievement integration. | streak, daily-login, achievement-manager, instrumented      |
| `ThreeStarAchievementTest`        | ✅ Passing           | 6     | `3_star_hard_level` achievement: complete levels with 5+ optimal moves with 3 stars.   | achievements, 3-star, hard-level, solver, level-data        |

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
- **`Level140E2ETest`** and **`RandomGame11E2ETest`**: Very long-running (minutes). Run only for release validation.
- **`LevelSolutionGeneratorTest`**: Not a regression test — only run to regenerate `LevelSolutionData.java` stub data.
- **`Level111DebugTest`**: Debug-only test for investigating a specific solver edge case. Not a regression test.
- **`MoveCountDebugTest`** and **`AchievementDebugTest`**: Debug tests for past issues, kept for reference.
