---
agent: devin-local
session: gentle-step
created: 2026-09-16T10:55:56Z
---
# Megaplan: Full Android → ComposeApp Feature Parity

Port every remaining Android-app feature into the ComposeApp — including the Phase-6 GameState refactor, all missing screens (Achievements, Level Editor, Debug Settings), desktop sound, and the full online feature set (login, sync, sharing) tested against the local Laravel community app at http://127.0.0.1:8000.

## Context & Current State

**Decision (confirmed with user):** Full parity including the GameState refactor (Phase 6), complete online features, both missing screens, and real desktop sound. Online features are developed and tested against the **local Laravel app** (`/var/www/roboyard.z11`, started via `./run.sh -d=r` → http://127.0.0.1:8000, MySQL in Docker, phpMyAdmin :8081). Local DB is disposable per `/var/www/roboyard.z11/AGENTS.md`; **never** test against production roboyard.z11.de.

**Already done** (see `dev/plans/migrate-logic-to-shared.md`): movement + undo + path rendering via `GameController`, full hint system (`HintManager`), settings (`SettingsManager`, 21 prefs), localization (9 languages, `strings.json`), map-generation validation/fallback (`MapGenerationValidator`), tap-to-move, autosave/history basics, stars/level-completion (`StarRating.kt`, `LevelCompletionManager`), timer, win detection.

**Source of truth:** `app/` (Android). ComposeApp must look and behave identically. No Android functionality may be lost.

## Gap Analysis (verified against code)

| Area | Android | ComposeApp | Status |
|------|---------|-----------|--------|
| Game state | `GameState` + `GameStateManager` (5073 lines, ~118 fns) | `Board` direct + `GameController` (215 lines) | **Architecture differs — refactor** |
| Achievements | `AchievementsFragment` (301) + popups + streaks | Stub (37 lines, TODO) | **Missing** |
| Level editor | `LevelDesignEditorFragment` (2141) | Truncated stub (20), not navigable | **Missing** |
| Debug settings | `DebugSettingsFragment` (797), long-press on Settings title | Truncated stub (16), not navigable | **Missing** |
| Save/Load | `SaveGameFragment` (2307): metadata, minimaps, sort/filter, pagination, share | `SaveLoadScreen` (479): basic slots + history | **Partial** |
| Level selection | `LevelSelectionFragment` (1316): progress, scroll-to-last, zoom, custom levels, editor button | `LevelSelectionScreen` (244): grid + stars, progress bar hardcoded 0 | **Partial** |
| Main menu | `MainMenuFragment` (420): streak popup, profile/login, editor button, load-game state | `MainMenuScreen` (215): static buttons | **Partial** |
| Help | Localized strings, 7 sections | Hardcoded English, 4 sections | **Partial** |
| Credits | Real version, clickable links, update nudge | Hardcoded "v1.9", dead links | **Partial** |
| Sound | 40 per-robot mp3s + bg music (`SoundService`) | `SoundManager` wired, desktop = no-op | **Missing playback** |
| Online | `RoboyardApiClient` (940), `SyncManager` (932), login dialogs, share | All desktop stubs return null/false | **Missing** |
| Live solver | `LiveSolverManager` + live move counter + eye-blink | Not present | **Missing** |
| Accessibility | TalkBack detect + 3-row controls + announcements | Dead flag `accessibilityControlsVisible` | **Missing** |
| Animations | `RobotAnimationManager`, `SolutionAnimator`, long-press progress | Scale + hint slide only | **Partial** |

---

## Phase 0 — Shared Game-State Architecture (GameState refactor)

**Goal:** Move the ~104 platform-independent functions of `app/.../GameStateManager.kt` into shared; ComposeApp runs on `GameState` like Android.

### 0.1 Observable abstraction
- `GameStateManager` uses `LiveData`; shared needs a KMP replacement → `kotlinx.coroutines.flow.StateFlow` (coroutines 1.9.0 already a shared dependency).
- New `shared/.../managers/GameSession.kt` (extends today's `GameController` concept): holds `MutableStateFlow<GameState?>`, `moveCount`, `squaresMoved`, `isGameComplete`, `isSolverRunning`, `wrongRobotAtTarget`, `newGameLoaded` events.

### 0.2 Port function groups from `GameStateManager.kt` → shared `GameSession`
- **Movement:** `moveRobotInDirection` (400ms cooldown, collision info incl. *which* robot was hit — needed for per-robot sounds), `isUndoMove`, `undoLastMove`, pathHistory add/remove/clear
- **Completion:** `areAllRobotsAtTargets`, `checkCompletion`, wrong-robot-at-target tracking (`getWrongRobotAtTarget`, `markWrongRobotToastShownFor`, `hasWrongRobotToastBeenShownFor`), `isTrivialPuzzle`
- **Lifecycle:** `startNewGame`, `startLevelGame`, `loadGame`, `resetGame`, `applyLoadedGameState`, `createValidGame` (integrated with existing `MapGenerationValidator`)
- **Save/Load:** `saveGame`, `loadGame`, `readSaveFileContent`, `extractMetadataFromSaveData`, `validateSaveContainsTargets`, autosave metadata (`saveAutosaveMetadata`, `clearAutosaveMetadata`, `autosaveSettingsMatch`) — **Android save format** (replaces `BoardUtils.serializeBoardToMainGameFormat` where formats diverge; verify byte-compat against real Android save files)
- **History:** `saveToHistory`/`saveToHistoryNow`, `loadHistoryEntry`, `loadNext/PreviousHistoryEntry`, `hasNextHistoryEntry`, `updateHintTrackingInHistory` (mostly already in `GameHistoryManager` — consolidate call sites)
- **Solver orchestration:** `calculateSolutionAsync` (coroutines), `initializeSolverForState`, `cancelSolver`, `cancelPreComputation`, `solutionWasAccepted`, `serializeAllSolutions`/`deserializeSolutions`, `preComputeNextMoves`, `clearNextMovesCache`, `getSolutionRobotOrder`, `increment/resetSolutionStep`, `computeStateHash`
- **Live counter:** `getLiveMoveCounterText`, `getLiveMoveCounterDeviation`, `isLiveMoveCounterEnabled`/`setLiveMoveCounterEnabled`, `triggerLiveSolver`, `onLiveSolverFinished/Failed` — port `LiveSolverManager` to shared coroutines (`SolverDD` is already shared)
- **Timer:** `startGameTimer`, `pauseTimer`, `resumeTimer`, `resetGameTimer`, `updateGameTimer`, `wasUiTimerRunning`, `saveUiTimerElapsed`, `resetUiTimer`
- **Misc:** `getLocalizedDifficultyString`, `robotColorShort`, `checkViewTimeAchievement`, `getLastSolutionMinMoves`/`resetLastSolutionMinMoves`, `resetSolverRestartCount`, `setRobotStartingPosition`/`clearRobotStartingPositions`, `triggerHistoryUpload` (stub until Phase 4), `animateSolution` (drives `SolutionAnimator` port)

### 0.3 Keep Android-side (thin wrapper)
`GameStateManager.kt` remains an `AndroidViewModel` façade: exposes the same `LiveData` (mirroring StateFlow via `asLiveData()`), keeps `Context`/`Activity`/`GameGridView`/`findNavController`/`Base64`-`Bitmap`/navigation functions (the ~14 Android-specific ones), delegates all logic to `GameSession`. Android behavior must not change.

### 0.4 Rewire `GameScreen.kt`
- Replace `board: Board` param with `GameState`; `App.kt` creates `GameState` from `MapGenerator.generatedGameMap` (GridElements → GameState directly, not via Board)
- Rendering: `BoardCanvas` iterates `GameState.gridElements` (or keeps a derived `Board` purely as render model — decide by which yields less churn; solver conversion `GameState→Board` via shared `SolverDD`-style converter)
- All movement/undo/completion/save/history calls route through `GameSession`
- Retire `ComposeGameState`, shrink `BoardUtils.kt` to conversion helpers only

**Verification Phase 0:** `:app:assembleDebug`, `RoboyardSmokeTest` (59), `:shared:desktopTest`, `:composeApp:desktopTest`, `composeApp:compileKotlinDesktop`, PyAutoGUI `random_game_test.py` + `history_info_button_headed_test.py`.

---

## Phase 1 — GameScreen Feature Parity

Reference: `GameFragment.java` (4705 lines). Missing pieces to port:

### 1.1 Live move counter (`LiveSolverManager`, `getLiveMoveCounterText/Deviation`)
- Toggle button on game screen; when enabled, a separate solver continuously solves the *current* board state and shows remaining-moves/deviation with coloring (`getDeviationColor`)
- Shared coroutine port of `LiveSolverManager` (single-thread executor → `Dispatchers.Default` + Job cancel)

### 1.2 Hint auto-move (`Preferences.hintAutoMoveEnabled` + `hintAutoMoveMode`)
- Port `executeHintAutoMove`, `checkIfMoveMatchesHint`, `scheduleHintButtonReset` semantics: manual / semi-auto / full-auto modes; on matching user move or hint display, robot moves automatically with 1s auto-advance delay (partially implemented — verify against Android)
- Extend `HintManager` where logic is shareable

### 1.3 Robot movement animation + collision sounds
- Port `RobotAnimationManager` semantics: smooth slide along path (Compose `Animatable` offset per robot), collision info from `GameSession` (`hitWall`, `hitRobot` + ids)
- Per-robot sounds: `playSound("hit_robot", attackerId, targetId)` → `robot_X_hits_robot_Y` resources (Phase 3 provides playback; wiring now)

### 1.4 Long-press circular-progress buttons (1.2s cooldown)
- Android: `startCircularProgressAnimation`/`stopCircularProgressAnimation`/`fadeOutCircularProgressAnimation`
- Compose: `combinedClickable(onLongClick)` + `CircularProgressIndicator` overlay

### 1.5 Missing buttons/indicators
- **"Keep Map"** button (regenerate solution for same map — solver restart)
- **Layout direction toggle** (`toggleLayoutDirection`, landscape alternative layout)
- **Unique map ID / level name text** (`updateUniqueMapIdText`: `level_id_text` for levels, map name for named maps, `unique_map_id` fallback)
- **Difficulty text** (`updateDifficulty` via `getLocalizedDifficultyString`, 0.7× relative size)
- **Wrong-robot-at-target** notification (`wrongRobotAtTarget` + toast shown-once-per-color)
- **Hint cancel** while solver runs (`hint_cancel_button`, `cancelSolver`)
- **Back-to-previous-level dialog** + `loadPreviousHistoryEntry`/`startPreviousLevel` history navigation

### 1.6 Accessibility mode
- `Preferences.accessibilityMode` (TalkBack detection doesn't exist on desktop — platform check returns false; the *setting* drives visibility)
- Port 3-row controls (`accessibility_controls.xml`): Announce/Select-Robot row, N/S/W/E direction row, selected-robot + robot-goal labels
- `cycleThroughRobots`, `announceGameStart`, `announcePossibleMoves` → desktop renders announcements as visible status text (no TTS); Compose `semantics`/`contentDescription` on all elements for potential screen readers

### 1.7 Achievements & streaks on completion
- `AchievementManager`/`StreakManager` are already in shared but **never invoked** on desktop — wire into `GameScreen` win flow exactly as Android:
  - `isFirstCompletion` check **before** `saveToHistoryNow` (race-condition order matters)
  - `qualifiesForNoHints` (session-based on first completion, history-based on repeat)
  - `onRandomGameCompleted(...)` / `onLevelCompleted(...)` with achievement-guard dedup (levelId+1s rule)
  - `checkAndUnlockStreakAchievements` on game-screen entry; `onNewGameStarted`, `onHintUsed`
- Compose `AchievementPopup` equivalent (port `AchievementPopup.java` visuals incl. streak popup hiding next-level button)

### 1.8 Completion side-effect parity
- `shouldHandleCompletionSideEffects` guard, `completionHandledThisSession`
- Perfect-solution / "less than X moves" / "no solution" completion messages (exact string keys)
- Reset→Retry text swap; Next-Level vs New-Random-Game button switch; accessibility controls hide on random-game completion

**Verification Phase 1:** new shared unit tests per group + Compose UI tests + PyAutoGUI regression.

---

## Phase 2 — Remaining Screens

### 2.1 `AchievementsScreen` (port `AchievementsFragment` + `fragment_achievements.xml`)
- Progress text (X/Y unlocked), scrollable list grouped by `AchievementCategory` headers
- Items: icon (`AchievementIconHelper` equivalent via compose resources), name, description, unlock state, "new" badge (`isNewAchievement`)
- Streak section: current + longest login streak labels (`achievement_login_streak_day(s)_label`)
- `user_profile_button` behavior (login state → Phase 4)

### 2.2 `LevelSelectionScreen` (port `LevelSelectionFragment`, 1316 lines)
- Real progress: filled fraction + "completed / 140" count (currently hardcoded `0f`/`0`)
- Scroll-to-last-played on open; `bg_level_card_last_played` highlight
- Tap zoom-in animation (`animateLevelZoom`), scroll fade effect, scroll-up arrow
- Custom levels (id ≥ `CUSTOM_LEVEL_START_ID`=141) section + header items
- Level-editor button (visibility rule `updateLevelEditorButtonVisibility`)
- `loadHistoryByMapName` for per-level history display

### 2.3 `SaveLoadScreen` (port `SaveGameFragment`, 2307 lines)
- Slot metadata: map name, board size, difficulty, moves, date (`extractSaveMetadata`, `SaveDataMetadata`)
- Minimap preview per slot/history entry (reuse `MinimapGenerator`)
- Sort + filter spinners, pagination (top+bottom controls, `page_info_text`)
- Share: `shareViaUrl` (build share URL → clipboard + browser), `shareToAccount` (Phase 4)
- Save-slot info popup (`buildMapInfoPopupMessage` semantics), overwrite behavior, empty-slot save

### 2.4 `MainMenuScreen` (port `MainMenuFragment`, 420 lines)
- Daily streak popup (`maybeShowDailyStreakPopup`)
- Functional profile button (login avatar/state — Phase 4; placeholder until then)
- Level-editor entry button
- `checkAndUpdateLoadGameButton` (enabled-state)
- Achievements loading spinner while achievements load

### 2.5 `HelpScreen` — full localization
- All 7 Android sections via `StringProvider`: goal, movement (3 pts), controls (4 pts), accessibility, tips (3 pts), star system (5 rules + hint penalty), level editor
- Laravel-style formatting (heading 18sp / paragraph 14sp, line spacing 1.6)

### 2.6 `CreditsScreen`
- Version from `PlatformInfo` (`version_format` with name+code), clickable links via new `expect fun openUrl(url)` (desktop: `java.awt.Desktop.browse`; android: Intent), update nudge (`showUpdateNudgeForCredits`)

---

## Phase 3 — Desktop Sound

- `DesktopSoundManager` real implementation: MP3 playback via **JLayer** (`javazoom:jlayer`, add to `shared desktopMain` deps) — alternative: WAV conversion + `javax.sound` (decide at implementation time)
- Copy `app/src/main/res/raw/*.mp3` (40 files + `singing_bowls_in_a_forest`) → compose resources
- API surface already exists (`playSound(id)`, `stopAll`, `setBackgroundVolume`, `pause/resumeBackground`, `isSoundEnabled`, `setSoundEnabled`); extend interface for `playSound(type, attacker, target)` per-robot collision
- Background music: loop + `Preferences.backgroundSoundVolume`; pause on window unfocus, resume on focus (desktop `Main.kt` window listeners)
- Android `SoundService` remains unchanged

---

## Phase 4 — Online Features (tested against local app)

**Local server:** `cd /var/www/roboyard.z11 && ./run.sh -d=r` → Laravel at `http://127.0.0.1:8000`, disposable Docker MySQL, phpMyAdmin at :8081. Never touch production.

### 4.0 Configurable API base URL (prerequisite)
- `RoboyardApiClient.BASE_URL` is hardcoded to `https://roboyard.z11.de` → make configurable:
  - shared `ApiConfig`/`PlatformInfo` hook reading `System.getProperty("roboyard.api.baseurl")` on desktop (default = prod URL)
  - debug-settings field (Phase 6) to switch at runtime
  - Android `network_security_config` needs cleartext allowance only if Android is ever pointed at localhost — desktop needs none
- All endpoint paths verified against local `/var/www/roboyard.z11/routes/api.php`:
  - `POST /api/mobile/login|register|verify-token|logout`, `GET /api/mobile/profile`
  - `POST /api/mobile/maps` (share), `POST/GET /api/mobile/achievements(/sync)`, `POST/GET /api/mobile/saves(/sync)`, `POST/GET /api/mobile/history(/sync)`
  - Web: `POST /auto-login`, `GET /share_map`, `GET /maps/{id}`

### 4.1 HTTP layer
- `expect/actual` minimal HTTP client in shared (`get/post` with headers+body); desktop actual = `java.net.HttpURLConnection` (JVM — no ktor needed yet); iOS actual stubbed/ktor later
- Gson already in shared for JSON

### 4.2 Port `RoboyardApiClient` → shared `commonMain`
- All endpoints listed above + `attemptReLogin`, `tryReLoginOrLogout`, `buildAutoLoginUrl`
- `ver=1` protocol: client sends `ver=API_VERSION`; `needs_update: true` response → `onNeedsUpdate` (desktop: dialog "app needs update", navigate to menu); `latest_app_version` in verify-token response drives update nudge
- Token persistence via `PlatformStorage`

### 4.3 Desktop `AuthManager` impl + Compose login/register dialogs
- Port `LoginDialogHelper`/`RegisterDialogHelper` logic to Compose dialogs
- Profile button: logged-in → `buildAutoLoginUrl("{base}/profile")` + `openUrl`; logged-out → login dialog → optional open-after-login

### 4.4 Port `SyncManager` → shared
- Achievement/history/savegame up+down sync; wire triggers (app start, completion, history write) matching Android call sites

### 4.5 Sharing
- Share map: logged-out → URL → clipboard + `openUrl` to `share_map?data=...`; logged-in → `shareMap` API + auto-login open
- `buildMapDataForShare`/`parseSaveDataForShare` → shared

### 4.6 Deep links on desktop
- `DeepLinkHandler` parse logic is pure string/`LevelFormatParser` work → move to `commonMain`
- Desktop entry points: "open map from link" input in Load screen + startup `args` in `Main.kt`; `ver>1` → needs-update message
- `DataExportImportManager` → shared core + desktop file dialogs (export/import all data)

### 4.7 Local test matrix (against http://127.0.0.1:8000)
- Register test account → login → verify-token → logout (fresh local DB is disposable)
- Share map (logged-out URL flow + logged-in API flow, duplicate-map case)
- Achievement/save/history sync up and down
- `needs_update` path: set `roboyard.latest_app_version` config on local server above app version → expect update nudge/dialog
- `auto-login` URL opens local profile in browser

**Explicitly Android-only forever:** `PlayGamesManager` (Google Play Services), OS intent-filters, logcat viewer (desktop equivalent: log-file viewer in Debug screen).

---

## Phase 5 — Level Editor (port `LevelDesignEditorFragment`, 2141 lines)

- New `LevelDesignEditorScreen` + shared `LevelEditorController` (edit state, grid mutations)
- Edit modes (`setupEditModeRadioGroup`): add/remove horizontal+vertical walls, targets, robots, erase; color radio buttons
- Tools: border walls, center carrée add/remove, wall-pattern generator (`WallPatternGenerator` → shared), board-size change, `trimContent`, `shiftContent`
- Load sources: existing level spinner (140 levels + custom), savegame spinner, last random map, `importAsciiMap` dialog
- Outputs: `generateLevelText`, `showLevelText`, save to custom levels (id ≥ 141), `shareLevelOnline` (Phase 4, local server), `playCurrentMap` → GameScreen, `saveToSourcecode` (desktop: file write)
- Navigation: main-menu `level_editor_button` + level-selection editor button (Phase 2)
- `createMinimapFromPath/FromString` → reuse shared `MinimapGenerator`

## Phase 6 — Debug Settings (port `DebugSettingsFragment`, 797 lines)

- `DebugSettingsScreen` with: streak simulator (`simulateDailyLogins`), achievement test buttons (`addAchievementButtons`/`showAchievementSelector`), dummy history entries, memory stats, unlock-stars (`LevelCompletionManager.unlockAllStars/unlockStars`), level buttons, history test buttons, hint-auto-move settings, streak test mode, `checkReceiverReachable`, **API base-URL switch** (local ↔ prod, from 4.0)
- Entry gesture identical to Android: **long-press on Settings title** (`setupDebugView`/`debugGestureDetector` → Compose `combinedClickable(onLongClick)`)

---

## Cross-Cutting (every phase)

- **Strings:** add all new keys to `strings.json` for all 9 languages — copy translations from `app/src/main/res/values-*/strings.xml`; escape rules per AGENTS
- **Tests:** shared unit tests per ported manager; Compose UI tests per screen (follow `ComposeAppUiSmokeTest` pattern with `testTag`s + `assertIsDisplayed`); document each in `dev/TESTSUITE.md`
- **Logging:** `[TAG]` prefixes matching existing conventions; `prompts_log.md` entry per user prompt
- **Commits:** after each phase, full `git status`/`git diff --stat`/diff review → propose message (never commit)

## Acceptance Criteria

- Every Android feature above works identically in ComposeApp (desktop)
- `GameState` is the single game-state model; Android unchanged (`assembleDebug` + 59 smoke tests green)
- `:shared:desktopTest`, `:composeApp:desktopTest`, `compileKotlinDesktop` green; iOS source sets still compile
- Online features verified end-to-end against local Laravel app (register→login→sync→share→auto-login)
- Visual parity verified side-by-side per screen

## Risks

- **Phase 0 refactor** is the riskiest item — mitigate by keeping `GameStateManager` as façade (Android never breaks) and rewiring GameScreen incrementally (state → movement → save → solver → live features), not big-bang
- **MP3 on desktop:** JLayer adds a dep; alternative WAV conversion decided at implementation time
- **Save-format compatibility:** Android `GameState` serialization vs current `serializeBoardToMainGameFormat` — verify round-trip with real Android save files before switching
- **Online features** touch the local server only; no production calls without explicit user approval

## Suggested Execution Order

0 → 1 → 3 (sound, independent) → 2 (screens) → 4 (online, incl. base-URL switch) → 5 (editor, needs 4 for share) → 6 (debug) → final audit. Each phase ends in a commit proposal.
