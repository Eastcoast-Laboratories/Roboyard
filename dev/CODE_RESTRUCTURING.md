# Code Restructuring: Move Logic Files from UI to Logic Package

## Overview
Many logic files are currently located in `roboyard.ui.components` but contain no UI code. They should be moved to `roboyard.logic` or `roboyard.logic.managers` for better separation of concerns and to prepare for KMP (Kotlin Multiplatform).

## Files to Move

### High Priority (Pure Logic, No Android UI Dependencies)

| Current Path | Target Path | Reason |
|-------------|------------|--------|
| `roboyard.ui.components/GameHistoryManager.java` | `roboyard.logic.managers/GameHistoryManager.java` | Pure history management logic, no UI |
| `roboyard.ui/components/LevelCompletionManager.java` | `roboyard.logic.managers/LevelCompletionManager.java` | Level completion tracking, no UI |
| `roboyard.ui.components/SyncManager.java` | `roboyard.logic.managers/SyncManager.java` | Sync logic, no UI (but has Android network dependencies) |
| `roboyard.ui.components/RoboyardApiClient.java` | `roboyard.logic.network/RoboyardApiClient.java` | API client logic, no UI (but has Android network dependencies) |
| `roboyard.ui.components/DataExportImportManager.java` | `roboyard.logic.managers/DataExportImportManager.java` | Export/import logic, no UI |
| `roboyard.ui.components/FileReadWrite.java` | `roboyard.logic.storage/FileReadWrite.java` | File I/O logic, no UI (but has Android file dependencies) |
| `roboyard.ui.components/MinimapGenerator.java` | `roboyard.logic.graphics/MinimapGenerator.java` | Minimap generation logic, no UI (but has Android Bitmap dependencies) |
| `roboyard.ui.components/PlayGamesManager.java` | `roboyard.logic.managers/PlayGamesManager.java` | Google Play Games integration, no UI (but has Android Play Services dependencies) |

### Medium Priority (Logic with Some Android Dependencies)

| Current Path | Target Path | Reason |
|-------------|------------|--------|
| `roboyard.ui.components/GameStateManager.java` | `roboyard.logic.managers/GameStateManager.java` | Game state logic, but uses Android ViewModel/Activity (may need refactoring for KMP) |
| `roboyard.ui/components/InputManager.java` | `roboyard.logic.input/InputManager.java` | Input handling logic, but uses Android MotionEvent (may need refactoring for KMP) |

### Low Priority (UI-Related or Unclear)

| Current Path | Target Path | Reason |
|-------------|------------|--------|
| `roboyard.ui/components/GameManager.java` | Keep in UI or analyze | Need to check if it's pure logic or UI-related |
| `roboyard.ui.achievements/Achievement.java` | `roboyard.logic.achievements/Achievement.java` | Data class, can be moved to logic |
| `roboyard.ui.achievements/AchievementCategory.java` | `roboyard.logic.achievements/AchievementCategory.java` | Data class, can be moved to logic |
| `roboyard.ui.achievements/AchievementDefinitions.java` | `roboyard.logic.achievements/AchievementDefinitions.java` | Definitions, can be moved to logic |
| `roboyard.ui.achievements/AchievementManager.java` | `roboyard.logic.achievements/AchievementManager.java` | Manager logic, no UI (but has Android SharedPreferences dependencies) |
| `roboyard.ui.achievements/StreakManager.java` | `roboyard.logic.achievements/StreakManager.java` | Streak logic, no UI (but has Android SharedPreferences dependencies) |

### Keep in UI (UI Components)

| File | Reason |
|------|--------|
| `roboyard.ui.components/GameGridView.java` | Custom View (UI) |
| `roboyard.ui.components/GamePiece.java` | Custom View (UI) |
| `roboyard.ui.components/GameScreen.java` | Custom View (UI) |
| `roboyard.ui.components/GridGameView.java` | Custom View (UI) |
| `roboyard.ui.components/RenderManager.java` | Rendering logic (UI) |
| `roboyard.ui.components/WallRenderer.java` | Rendering logic (UI) |
| `roboyard.ui/components/LiveModeToggleButtonAlt.java` | Custom Button (UI) |
| `roboyard.ui.components/LoginDialogHelper.java` | Dialog helper (UI) |
| `roboyard.ui/components/RegisterDialogHelper.java` | Dialog helper (UI) |
| `roboyard.ui.components/AccessibilityUtil.java` | Accessibility helper (UI) |
| `roboyard.ui.achievements/AchievementIconHelper.java` | Icon helper (UI) |
| `roboyard.ui.achievements/AchievementPopup.java` | Popup (UI) |
| `roboyard.ui.animation/RobotAnimationManager.java` | Animation (UI) |
| `roboyard.ui/adapters/LanguageSpinnerAdapter.java` | Adapter (UI) |

## Recommended New Package Structure

```
roboyard.logic/
├── core/ (existing)
│   ├── GameState.kt
│   ├── GameLogic.kt
│   ├── GameHistoryEntry.kt
│   └── ...
├── managers/
│   ├── GameHistoryManager.java
│   ├── LevelCompletionManager.java
│   ├── GameStateManager.java
│   ├── SyncManager.java
│   ├── DataExportImportManager.java
│   ├── PlayGamesManager.java
│   ├── AchievementManager.java
│   └── StreakManager.java
├── storage/
│   └── FileReadWrite.java
├── network/
│   └── RoboyardApiClient.java
├── graphics/
│   └── MinimapGenerator.java
├── input/
│   └── InputManager.java
└── achievements/
    ├── Achievement.java
    ├── AchievementCategory.java
    ├── AchievementDefinitions.java
    ├── AchievementManager.java
    └── StreakManager.java
```

## Migration Strategy

1. **Phase 1**: Move pure logic files (High Priority)
   - GameHistoryManager
   - LevelCompletionManager
   - FileReadWrite
   - MinimapGenerator

2. **Phase 2**: Move files with Android dependencies (Medium Priority)
   - GameStateManager (may need refactoring)
   - SyncManager
   - RoboyardApiClient
   - DataExportImportManager
   - PlayGamesManager

3. **Phase 3**: Move achievement-related files
   - Achievement, AchievementCategory, AchievementDefinitions
   - AchievementManager, StreakManager

4. **Phase 4**: Analyze and decide on remaining files
   - GameManager (analyze first)
   - InputManager (may need refactoring for KMP)

## Notes

- Files with Android-specific dependencies (SharedPreferences, Activity, ViewModel, Bitmap, MotionEvent, HttpURLConnection) cannot be directly shared with iOS via KMP without refactoring.
- For KMP, these dependencies will need to be abstracted through expect/actual declarations or interfaces.
- The restructuring should be done incrementally to avoid breaking the build.
- After each move, run tests to ensure nothing is broken.
