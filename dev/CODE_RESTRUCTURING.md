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
├── managers/       (Java: GameStateManager, 1 SyncManager,
│                    DataExportImportManager; Kotlin: LevelCompletionManager,
│                    PlayGamesManager)
├── network/        (Kotlin: RoboyardApiClient)
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

### Suggested conversion order
1. [ ] - Last remaining Java → Kotlin: `GameStateManager` (see strategy below).

### GameStateManager conversion strategy
`GameStateManager` is the central hub: ~4600 lines, ~230 methods, ~370 external
references across 13 files (184 in `GameFragment.java`, 68 in `GameGridView.java`).

**Decision: convert in one pass with the AS converter, then fix compile errors
iteratively. Do NOT pre-extract/abstract the UI parts first** — that is a separate,
later KMP step. For the Kotlin conversion the class may stay an `AndroidViewModel`
with `LiveData`.

Steps:
1. [ ] - In Android Studio: *Convert Java File to Kotlin File* on `GameStateManager.java`.
2. [ ] - Run `./gradlew assembleDebug` and fix compile errors iteratively (same known
   classes as previous conversions — see checklist below). Expect many follow-up errors
   in the Java callers, especially `GameFragment.java` and `GameGridView.java`.
3. [ ] - **Runtime-test afterwards** (start game, save/load, history, robot animations):
   the auto-converter can introduce subtle nullability/`LiveData`-generics behaviour bugs
   that still compile.

### Known auto-converter pitfalls (checklist)
These recurred in every prior conversion:
- Java getter `getX()` → access Kotlin property directly as `.x`.
- `static` members → `companion object`/`object` + `@JvmStatic` (else Java callers break,
  e.g. `getInstance`, `saveHistoryIndex`).
- `return@methodName` artifact → use the correct lambda label, e.g. `return@Runnable`.
- Remove spurious `import java.lang.Long` (collides with Kotlin `Long`; breaks `Long.compare`).
- Interface overrides must match the exact nullability of the signature
  (e.g. `onSuccess(result: T?)`).
- Collections: `ArrayList<Long>()` → `mutableListOf<Long>()`; match target signatures
  (`List<Long>` not `MutableList<Long?>`); iterate nullable map values with `.filterNotNull()`.
- Run `./gradlew assembleDebug` after each file and fix errors one by one.
- Do NOT abstract Android dependencies here — that is a later, separate KMP step.

## KMP notes
- Files depending on `SharedPreferences`, `Activity`, `ViewModel`, `LiveData`, `Bitmap` or `HttpURLConnection` cannot be shared with iOS as-is.
- Abstract these via `expect`/`actual` declarations or platform interfaces when KMP work begins.
