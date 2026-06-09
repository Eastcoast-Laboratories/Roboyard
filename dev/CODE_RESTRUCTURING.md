# Code Restructuring: Strict UI vs. Logic Separation

## Overview
Several files still located under `roboyard.ui` contain no Android UI code and belong in `roboyard.logic`. The goal is a strict separation:

- `roboyard.logic.*` — pure game/business logic and data, no Android `View`/`Canvas`/`Activity` rendering. Some files still carry Android *infrastructure* dependencies (`Context`, `SharedPreferences`, `Activity`, `Bitmap`); these are acceptable in `logic` for now but must be abstracted via `expect`/`actual` or interfaces before KMP sharing.
- `roboyard.ui.*` — everything that draws, animates, inflates layouts, handles dialogs, or extends a `View`/`Fragment`/`Activity`.

This prepares the codebase for Kotlin Multiplatform (KMP).

## Current logic package structure
```
roboyard.logic/
├── core/        (GameState.kt, GameLogic.kt, GameHistoryEntry.kt, Constants, ...)
├── solver/      (RR*/ERR* solver classes)
├── managers/    (GameHistoryManager, LevelCompletionManager, SyncManager,
│                 DataExportImportManager, PlayGamesManager)
├── network/     (RoboyardApiClient)
├── storage/     (FileReadWrite)
└── graphics/    (MinimapGenerator)
```

## Files still to move (non-UI living under `roboyard.ui`)

| Current Path | Target Path | Android deps | Notes |
|-------------|------------|--------------|-------|
| `roboyard.ui.components/GameStateManager.java` | `roboyard.logic.managers/GameStateManager.java` | `Activity`, `ViewModel`, `LiveData` | Core game-state orchestration. Largest dependency surface; the Android lifecycle/ViewModel parts must be abstracted before the move is clean for KMP. Move last. |
| `roboyard.ui.components/InputManager.java` | `roboyard.logic.input/InputManager.java` | none | Pure touch-state holder (coordinates + boolean flags), no Android imports. Coupled only to the legacy `GameScreen`/`GameManager` loop, so the move is trivial but only worthwhile together with a decision on that legacy rendering stack. |
| `roboyard.ui.achievements/Achievement.java` | `roboyard.logic.achievements/Achievement.java` | none | Pure data class. |
| `roboyard.ui.achievements/AchievementCategory.java` | `roboyard.logic.achievements/AchievementCategory.java` | `Context` (string lookup only) | Data/enum with localized labels. |
| `roboyard.ui.achievements/AchievementDefinitions.java` | `roboyard.logic.achievements/AchievementDefinitions.java` | none | Static definitions. |
| `roboyard.ui.achievements/AchievementManager.java` | `roboyard.logic.achievements/AchievementManager.java` | `Activity`, `SharedPreferences` | Achievement tracking/persistence logic. |
| `roboyard.ui.achievements/StreakManager.java` | `roboyard.logic.achievements/StreakManager.java` | `Context`, `SharedPreferences` | Streak tracking/persistence logic. |

## Files that stay in UI (confirmed UI)

| File | Reason |
|------|--------|
| `roboyard.ui.components/GameManager.java` | Orchestrates `GameScreen`/`RenderManager`/`InputManager` and exposes `draw()` — screen/render controller, not pure logic. |
| `roboyard.ui.components/GameScreen.java` | Screen base for the legacy render loop. |
| `roboyard.ui.components/GameGridView.java` | Custom `View`. |
| `roboyard.ui.components/GridGameView.java` | Custom `View`. |
| `roboyard.ui.components/GamePiece.java` | Drawable piece (UI). |
| `roboyard.ui.components/RenderManager.java` | Rendering (`Canvas`). |
| `roboyard.ui.components/WallRenderer.java` | Rendering (`Canvas`). |
| `roboyard.ui.components/LiveModeToggleButtonAlt.java` | Custom button. |
| `roboyard.ui.components/LoginDialogHelper.java` | Dialog helper. |
| `roboyard.ui.components/RegisterDialogHelper.java` | Dialog helper. |
| `roboyard.ui.components/AccessibilityUtil.java` | Accessibility helper. |
| `roboyard.ui.achievements/AchievementIconHelper.java` | `Bitmap`/`Canvas`/`Drawable` rendering. |
| `roboyard.ui.achievements/AchievementPopup.java` | `View`/animation popup. |
| `roboyard.ui.animation/RobotAnimationManager.java` | Animation. |
| `roboyard.ui.adapters/LanguageSpinnerAdapter.java` | `Adapter` (UI). |

## Target structure after remaining moves
```
roboyard.logic/
├── core/
├── solver/
├── managers/        + GameStateManager
├── network/
├── storage/
├── graphics/
├── input/           InputManager
└── achievements/    Achievement, AchievementCategory, AchievementDefinitions,
                     AchievementManager, StreakManager
```

## Migration order
1. **Achievements** — move the five non-UI achievement files into `roboyard.logic.achievements`. `Achievement`, `AchievementCategory` and `AchievementDefinitions` are trivial; `AchievementManager` and `StreakManager` carry only `Context`/`SharedPreferences`.
2. **InputManager** — move into `roboyard.logic.input` (decide whether to keep the legacy `GameScreen`/`GameManager` loop alive first).
3. **GameStateManager** — move last; abstract the `Activity`/`ViewModel`/`LiveData` dependencies before or during the move.

After each move: update package declarations + imports, then build (`./gradlew assembleDebug`) and run smoke tests.

## KMP notes
- Files depending on `SharedPreferences`, `Activity`, `ViewModel`, `LiveData` or `Bitmap` cannot be shared with iOS as-is.
- Abstract these via `expect`/`actual` declarations or platform interfaces when KMP work begins.
