# Roboyard Code Migration Strategy Document
Preparing for Framework Migration with UI/Logic Separation

1. Files to Move to `logic/`Folder
Location:`app/src/main/java/roboyard/eclabs`
| File                      | Action               |
|---------------------------|----------------------|
|`AbstractGameObject.java`  | Move directly        | Base class for game objects with no UI dependencies
|`Constants.java`           | Move directly        | Contains game-wide constants (no Android imports)
|`GameHistoryEntry.java`    | Move directly        | Pure data class for game history
|`GameLogic.java`           | Move directly        | Core map generation/solver logic
|`GridElement.java`         | Move directly        | Grid cell representation (no rendering logic)
|`IExecutor.java`           | Move directly        | Generic execution interface
|`IGameObject.java`         | Move directly        | Game object lifecycle interface
|`MapGenerator.java`        | Move directly        | Board generation algorithm
|`Move.java`                | Move directly        | Move representation (robot ID, direction, distance)
|`SaveManager.java`         | Move directly        | Save/load logic (decoupled from Android context) |
Location:`app/src/main/java/roboyard/pm/ia/ricochet`
| File                      | Action               |
|---------------------------|----------------------|
|`ERRGameMove.java`         | Move directly        |
|`RRGameMove.java`          | Move directly        |
|`RRGameState.java`         | Move directly        |
|`RRGetMap.java`            | Move directly        |
|`RRPiece.java`             | Move directly        |

2. Files Requiring Refactoring
|`AccessibilityUtil.java`
| - Keep `isScreenReaderActive()` in `logic/` (pure Java logic)
| - Move `announceForAccessibility()` to UI layer (Android TalkBack dependency)
|`FileReadWrite.java`
| - Extract generic I/O methods to`FileOperations`interface in`logic/`
| - Leave Android-specific methods (e.g.,`readPrivateData()) in UI
|`GameHistoryManager.java`
| - Move JSON serialization logic to`logic/`
| - Keep Activity-dependent methods (e.g.,`initialize()) in UI
|`GameManager.java`
| - Extract`GameStateManager`to`logic/`(handles game rules/state)
| - Keep UI interactions (e.g., screen transitions) in original class

3. UI Layer Files (Do Not Move)
| File                       | Reason 
|----------------------------|--------------------------------------------
|`GameButton*.java`          | Directly renders UI elements
|`GameDropdown.java`         | Android Spinner adapter dependency
|`GameMovementInterface.java`| Depends on`GridGameScreen`and Android resources
|`GamePiece.java`            | Uses`MediaPlayer,`RenderManager, and Android colors
|`GameScreen.java`           | Base class for UI screens
|`GridGameScreen.java`       | Extends`GameScreen`(UI placeholder)
|`InputManager.java`         | Handles touch events
|`MainActivity.java`         | Android Activity class
|`RenderManager.java`        | Direct OpenGL ES rendering
|`ScreenLayout.java`         | Manages UI layout coordinates

4. Problematic/Ambiguous Files
| File                      | Issue                               | Solution      |
|---------------------------|-------------------------------------|-------------|
|`MapObjects.java`          | Mixes game object                   | Split into:
|                           | types and rendering paths           | -`MapObjectType.java`(logic: type definitions)
|                           |                                     | -`MapObjectRenderer.java`(UI: rendering logic) |
|`Preferences.java`         | Uses Android `SharedPreferences`    | Refactor:
|                           |                                     | - Create`SettingsManager`interface in`logic/
|                           |                                     | - Implement Android-specific version in UI |
|`UIConstants.java`         | Contains both UI and game constants | Split into:
|                           |                                     | -`UIConstants.java`(UI: colors, dimensions)
|                           |                                     | -`GameConstants.java`(logic: gameplay values) |

5. Files to Delete
| File                             | Reason                         | Verification Needed |
|----------------------------------|--------------------------------|---------------------|
|`GridGameScreen.java`             | Marked as dummy implementation | Confirm unused via call hierarchy analysis |
| Deprecated`GameButton`variants   | Unused legacy UI components    | Run Android Lint check |

6. Target Folder Structure

src/
├── logic/
│   ├── core/          # GameLogic, MapGenerator, IGameObject
│   ├── data/          # GameHistoryEntry, Move, GridElement
│   ├── solver/        # ricochet/* files
│   └── utils/         # Constants, IExecutor, SaveManager
└── ui/
    ├── activities/    # MainActivity, ShareActivity
    ├── fragments/     # ModernGameFragment
    ├── components/    # GameButton*, RenderManager
    └── utils/         # AccessibilityUtil (UI parts)

7. Implementation Steps
    1. Create`logic/`Folder:
       
         mkdir -p app/src/main/java/roboyard/logic/{core,data,solver,utils}
    2. Move Identified Files:
       
        mv app/src/main/java/roboyard/eclabs/GameLogic.java app/src/main/java/roboyard/logic/core/
       # Repeat for other files in Section 1

    3. Refactor Mixed Classes:
        ◦ Use dependency injection for Android context:
          // Example: FileOperations interface
          public interface FileOperations {
              String readFile(String path); // Logic layer
          }

    4. Update Imports:
        ◦ Replace`roboyard.eclabs`with`roboyard.logic.core` where applicable.

    5. Validate via Build:
        ◦ Ensure`logic/`has no dependencies on`android.*` or `javax.microedition.khronos.*`.
        ◦ Run`./gradlew build`to verify no compilation errors.

This strategy ensures a clean separation for framework migration while preserving functionality.
