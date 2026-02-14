# Roboyard Code Migration Strategy Document
Completing the UI/Logic Package Separation

## Current State (Feb 2026)
The migration is **partially complete**. Some files have been moved to `roboyard.logic.*` and `roboyard.ui.*`,
but many remain in the legacy `roboyard.eclabs.*` package. This document tracks the remaining migration.

### Already Migrated
- `roboyard.logic.core` — GameLogic, GameState, Constants, GridElement, etc. (15 files)
- `roboyard.logic.solver` — RRGameMove, RRPiece, RRGameState, etc. (5 files)
- `roboyard.ui.activities` — MainActivity (1 file)
- `roboyard.ui.animation` — RobotAnimationManager (1 file)
- `roboyard.ui.components` — GameGridView, GameStateManager, GamePiece, etc. (11 files)

### Still in Legacy `roboyard.eclabs.*`
- `roboyard.eclabs` root — 10 files
- `roboyard.eclabs.ui` — 21 files (Fragments, Activities, UI helpers)
- `roboyard.eclabs.achievements` — 7 files
- `roboyard.eclabs.util` — 7 files
- `roboyard.eclabs.solver` — 4 files
- `roboyard.pm.ia` — 4 files (legacy interfaces)

## Important Migration Rule
**When moving any file, the package declaration inside the file must be updated!**
A file physically moved to `roboyard/ui/fragments/` must have `package roboyard.ui.fragments;`.
All imports referencing the old package must be updated across the entire codebase.

## Migration Plan

### Phase 1: Move `eclabs/ui/` Fragments → `ui/fragments/`
| File | Target |
|------|--------|
| `AchievementsFragment.java` | `roboyard.ui.fragments` |
| `BaseGameFragment.java` | `roboyard.ui.fragments` |
| `CreditsFragment.java` | `roboyard.ui.fragments` |
| `DebugSettingsFragment.java` | `roboyard.ui.fragments` |
| `HelpFragment.java` | `roboyard.ui.fragments` |
| `LevelDesignEditorFragment.java` | `roboyard.ui.fragments` |
| `LevelSelectionFragment.java` | `roboyard.ui.fragments` |
| `MainMenuFragment.java` | `roboyard.ui.fragments` |
| `ModernGameFragment.java` | `roboyard.ui.fragments` |
| `SaveGameFragment.java` | `roboyard.ui.fragments` |
| `SettingsFragment.java` | `roboyard.ui.fragments` |

### Phase 2: Move `eclabs/ui/` Activity → `ui/activities/`
| File | Target |
|------|--------|
| `MainFragmentActivity.java` | `roboyard.ui.activities` |

### Phase 3: Move `eclabs/ui/` non-Android data classes → `logic/core/`
| File | Target | Reason |
|------|--------|--------|
| `GameElement.java` | `roboyard.logic.core` | No Android deps, data model |
| `GameMove.java` | `roboyard.logic.core` | No Android deps, data model |
| `LevelCompletionData.java` | `roboyard.logic.core` | No Android deps, data model |

### Phase 4: Move `eclabs/ui/` UI components → `ui/components/`
| File | Target |
|------|--------|
| `LevelCompletionManager.java` | `roboyard.ui.components` |
| `GameSurfaceView.java` | `roboyard.ui.components` |
| `MinimapGenerator.java` | `roboyard.ui.components` |
| `UIModeManager.java` | `roboyard.ui.components` |
| `LoginDialogHelper.java` | `roboyard.ui.components` |
| `RegisterDialogHelper.java` | `roboyard.ui.components` |

### Phase 5: Move `eclabs/achievements/` → `ui/achievements/`
| File | Target |
|------|--------|
| All 7 achievement files | `roboyard.ui.achievements` |

### Phase 6: Move `eclabs/util/` → `ui/util/`
| File | Target |
|------|--------|
| All 7 util files | `roboyard.ui.util` |

### Phase 7: Move `eclabs/solver/` → `logic/solver/`
| File | Target |
|------|--------|
| `GameLevelSolver.java` | `roboyard.logic.solver` |
| `ISolver.java` | `roboyard.logic.solver` |
| `SolverDD.java` | `roboyard.logic.solver` |
| `SolverStatus.java` | `roboyard.logic.solver` |

### Phase 8: Move `eclabs/` root files
| File | Target | Reason |
|------|--------|--------|
| `MapObjects.java` | `roboyard.logic.core` | 0 Android deps, pure data parsing |
| `ShareActivity.java` | `roboyard.ui.activities` | Activity |
| `RoboyardApplication.java` | `roboyard.ui` | Application class |
| `GameManager.java` | `roboyard.ui.components` | Has Android dep |
| `FileReadWrite.java` | `roboyard.ui.components` | Has Android deps |
| `GameHistoryManager.java` | `roboyard.ui.components` | Has Android dep |
| `DataExportImportManager.java` | `roboyard.ui.components` | Has Android deps |
| `PlayGamesManager.java` | `roboyard.ui.components` | Has Android deps |
| `RoboyardApiClient.java` | `roboyard.ui.components` | Has Android deps |
| `SyncManager.java` | `roboyard.ui.components` | Has Android deps |

### Phase 9: Move `pm/ia/` → `logic/core/`
| File | Target | Reason |
|------|--------|--------|
| `AGameState.java` | `roboyard.logic.core` | Abstract game state |
| ~~`AWorld.java`~~ | ~~deleted~~ | ~~Unused, no references~~ |
| `GameSolution.java` | `roboyard.logic.core` | Solution model |
| `IGameMove.java` | `roboyard.logic.core` | Move interface |

## Target Folder Structure

```
app/src/main/java/roboyard/
├── logic/
│   ├── core/          # GameLogic, GameState, MapGenerator, GridElement, GameElement,
│   │                  # GameMove, MapObjects, IGameObject, AGameState, IGameMove, etc.
│   └── solver/        # RRGameMove, SolverDD, ISolver, GameLevelSolver, etc.
├── ui/
│   ├── RoboyardApplication.java
│   ├── activities/    # MainActivity, MainFragmentActivity, ShareActivity
│   ├── achievements/  # Achievement*, StreakManager
│   ├── animation/     # RobotAnimationManager
│   ├── components/    # GameGridView, GameStateManager, RenderManager, GameManager,
│   │                  # FileReadWrite, MinimapGenerator, etc.
│   ├── fragments/     # ModernGameFragment, SettingsFragment, all *Fragment.java
│   └── util/          # SolverManager, SoundManager, SolutionAnimator, FontScaleUtil, etc.
└── (eclabs/ — EMPTY after migration, to be deleted)
```

## Implementation
Each phase: `git mv` files → update `package` declaration → update all imports → build → test
