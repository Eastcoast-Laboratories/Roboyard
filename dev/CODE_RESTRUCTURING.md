# Code Restructuring: Strict UI vs. Logic Separation

## Overview
Logic and data classes belong in `roboyard.logic.*`, UI/rendering classes in `roboyard.ui.*`. The package split is now complete:

- `roboyard.logic.*` — game/business logic and data. Some files still carry Android *infrastructure* dependencies (`Context`, `SharedPreferences`, `Activity`, `ViewModel`, `Bitmap`); acceptable for now but must be abstracted via `expect`/`actual` or interfaces before KMP sharing.
- `roboyard.ui.*` — everything that draws, animates, inflates layouts, handles dialogs, or extends a `View`/`Fragment`/`Activity`.

## Done
- **Deleted** the dead legacy game-loop cluster (was self-referential, never instantiated): `GameManager`, `GameScreen`, `GridGameView`, `GamePiece`, `InputManager`, `RenderManager` (all `ui.components`) and `IGameObject` (`logic.core`).
- **Moved** the achievement logic classes to `roboyard.logic.achievements`: `Achievement`, `AchievementCategory`, `AchievementDefinitions`, `AchievementManager`, `StreakManager`.
- **Moved** `GameStateManager` from `ui.components` to `roboyard.logic.managers`.

No further package moves are pending.

## Current logic package structure
```
roboyard.logic/
├── core/           (Kotlin: GameState, GameLogic, GameHistoryEntry, Constants, Wall*, ...)
├── solver/         (Kotlin: RR*/ERR*/Solver* classes)
├── managers/       (Java: GameStateManager, GameHistoryManager, SyncManager,
│                    DataExportImportManager; Kotlin: LevelCompletionManager,
│                    PlayGamesManager)
├── network/        (Java: RoboyardApiClient)
├── storage/        (Kotlin: FileReadWrite)
├── graphics/       (Kotlin: MinimapGenerator)
└── achievements/   (Java: AchievementManager; Kotlin: Achievement,
                     AchievementCategory, AchievementDefinitions, StreakManager)
```

## Files that stay in UI (confirmed UI)

| File | Reason |
|------|--------|
| `roboyard.ui.components/GameGridView.java` | Custom `View` (active game board). |
| `roboyard.ui.components/WallRenderer.java` | Rendering (`Canvas`). |
| `roboyard.ui.components/LiveModeToggleButtonAlt.java` | Custom button. |
| `roboyard.ui.components/LoginDialogHelper.java` | Dialog helper. |
| `roboyard.ui.components/RegisterDialogHelper.java` | Dialog helper. |
| `roboyard.ui.components/AccessibilityUtil.java` | Accessibility helper. |
| `roboyard.ui.achievements/AchievementIconHelper.java` | `Bitmap`/`Canvas`/`Drawable` rendering. |
| `roboyard.ui.achievements/AchievementPopup.java` | `View`/animation popup. |
| `roboyard.ui.animation/RobotAnimationManager.java` | Animation. |
| `roboyard.ui.adapters/LanguageSpinnerAdapter.java` | `Adapter` (UI). |

## Java → Kotlin conversion candidates
The `logic.core` and `logic.solver` packages are already fully Kotlin. The remaining Java files in `roboyard.logic` should be converted to Kotlin for KMP.

> In Android Studio the **Project** tool window groups files by **package path**, not by file name. Switch to the *Project* view (not *Android* view) and expand `app/src/main/java/roboyard/logic/...` to find them under the packages listed below.

| Package (path in Android Studio) | File | Android deps to abstract before KMP |
|----------------------------------|------|--------------------------------------|
| `roboyard.logic.managers` | `GameStateManager.java` | `AndroidViewModel`, `Activity`, `LiveData`, `GameGridView`, `RobotAnimationManager` |
| `roboyard.logic.managers` | `GameHistoryManager.java` | `Activity`, file I/O |
| `roboyard.logic.managers` | `SyncManager.java` | `Context`, network |
| `roboyard.logic.managers` | `DataExportImportManager.java` | `Context`, file I/O |
| `roboyard.logic.network`  | `RoboyardApiClient.java` | `HttpURLConnection` |
| `roboyard.logic.achievements` | `AchievementManager.java` | `Activity`, `SharedPreferences` |

### Suggested conversion order
1. [ ] - Network/file/history infra still pending Java → Kotlin: `RoboyardApiClient`, `DataExportImportManager`, `SyncManager`, `GameHistoryManager`.
2. [ ] - `Activity`-dependent managers still pending Java → Kotlin: `AchievementManager` and `GameStateManager` — abstract the `Activity`/ViewModel/UI dependencies during the conversion.

## KMP notes
- Files depending on `SharedPreferences`, `Activity`, `ViewModel`, `LiveData`, `Bitmap` or `HttpURLConnection` cannot be shared with iOS as-is.
- Abstract these via `expect`/`actual` declarations or platform interfaces when KMP work begins.
