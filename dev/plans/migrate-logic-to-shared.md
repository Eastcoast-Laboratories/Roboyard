# Plan: Migrate remaining app/ logic to shared/ (DRY)

## Status: COMPLETED

## What was migrated

| File | From | To | Notes |
|------|------|----|-------|
| `GameState.kt` | `app/` | `shared/commonMain` | Timber→RLog, Context→PlatformStorage, ResourceLoader for levels |
| `GameStateManagerCore.kt` | `app/` | `shared/commonMain` | 1:1 move, 0 Android deps |
| `StreakManager.kt` | `app/` | `shared/commonMain` | Context→PlatformStorage, Timber→RLog, AchievementCallback interface, DateUtils expect/actual |
| `AchievementManager.kt` | `app/` | `shared/commonMain` | Context→PlatformStorage, Toast→UiNotifier, Resources→StringProvider, BuildConfig→PlatformInfo, org.json→Gson, AchievementSyncClient/PlayGamesClient/StreakDataProvider interfaces |
| `GameUtils.kt` | (new) | `shared/commonMain` | formatTime, formatElapsedTime, buildGameWinMessage — extracted from GameScreen.kt |
| `BoardUtils.kt` | (new) | `shared/commonMain` | isBoardSolved, moveRobotOnBoard — extracted from GameScreen.kt |
| `BoardSerializer.kt` | (new) | `shared/commonMain` | serializeBoard, deserializeBoard — extracted from GameScreen.kt |

## What was skipped (and why)

| File | Reason |
|------|--------|
| `MinimapGenerator.kt` | Different graphics APIs: Android Bitmap/Canvas vs Compose DrawScope |
| `DataExportImportManager.kt` | Only used in Android app, not in ComposeApp |
| `RoboyardApiClient.kt` | Only used in Android app, uses HttpURLConnection (would need ktor for KMP) |
| `SyncManager.kt` | Only used in Android app, depends on RoboyardApiClient |
| `GameStateManager.kt` | 5072 lines, Android-only (LiveData, ViewModel, MotionEvent, Navigation), not used by ComposeApp |

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

## Verification

- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL
- `./gradlew :shared:desktopTest` — BUILD SUCCESSFUL
- `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"` — 59 tests, 0 failures
- `./gradlew :composeApp:desktopTest` — 23/25 pass (2 pre-existing failures in HistoryAutosaveTest, unrelated to migration)
