# Roboyard Code Migration Strategy Document
Preparing for Framework Migration with UI/Logic Separation

## Important Migration Note
**When moving any file, the package declaration inside the file must be updated!**
A file physically moved to `roboyard/logic/core/` must have its package declaration changed from `package roboyard.eclabs;` to `package roboyard.logic.core;`.

## 1. Remaining Files to Move to `logic/` Folder
Location:`app/src/main/java/roboyard/eclabs`
| File                      | Action               | Target Package       | Description
|---------------------------|----------------------|----------------------|------------
|`AbstractGameObject.java`  | (done)               | `roboyard.logic.core`| Base class for game objects with no UI dependencies
|`IGameObject.java`         | (done)               | `roboyard.logic.core`| Game object lifecycle interface
|`MapGenerator.java`        | (done)               | `roboyard.logic.core`| Board generation algorithm
|`Move.java`                | (done)               | `roboyard.logic.core`| Move representation (robot ID, direction, distance)
|`IExecutor.java`           | (done)               | `roboyard.logic.core`| Generic execution interface

## 2. UI Layer Files (Move to UI Package)
| File                       | Action                | Target Package           | Description
|----------------------------|----------------------|---------------------------|------------
|`RenderManager.java`        | Move to UI components | `roboyard.ui.components` | Direct OpenGL ES rendering
|`ScreenLayout.java`         | Move to UI components | `roboyard.ui.components` | Manages UI layout coordinates
|`GameButton*.java`          | Move to UI components | `roboyard.ui.components` | Directly renders UI elements

already done
| File                       | Action                | Target Package           | Description
|----------------------------|----------------------|---------------------------|------------
|`MainActivity.java`         | (done) UI components | `roboyard.ui.activities` | Android Activity class
|`InputManager.java`         | (done) UI components | `roboyard.ui.components` | Handles touch events
|`GamePiece.java`         | (done) UI components | `roboyard.ui.components` | Migrated successfully|
|`GameScreen.java`         | (done) UI components | `roboyard.ui.components` | Migrated successfully|
|`GridGameScreen.java`         | (done) UI components | `roboyard.ui.components` | Migrated successfully|
|`GameDropdown.java`         | (done)  UI components | `roboyard.ui.components` | Android Spinner adapter dependency
|`GameMovementInterface.java`| (done) UI components | `roboyard.ui.components` | Migrated successfully|


## 4. Files to Delete
| File                             | Reason                         | Verification Needed |
|----------------------------------|--------------------------------|---------------------|
|`GridGameScreen.java`             | Marked as dummy implementation | Confirm unused via call hierarchy analysis |
| Deprecated`GameButton`variants   | Unused legacy UI components    | Run Android Lint check |

## 5. Files Requiring Refactoring
| File                      | Action                               | Target Package            | Description
|---------------------------|--------------------------------------|---------------------------|------------
|`AccessibilityUtil.java`   | Split UI/Logic functionality         | `roboyard.logic.utils`    | Keep `isScreenReaderActive()` in logic layer
|                           |                                      | `roboyard.ui.utils`       | Move `announceForAccessibility()` to UI layer
|`FileReadWrite.java`       | Extract interface, move implementation| `roboyard.logic.utils`   | Extract generic I/O methods to interface
|                           |                                      | `roboyard.ui.utils`       | Keep Android-specific methods in UI
|`GameHistoryManager.java`  | Extract logic part                   | `roboyard.logic.core`     | Move JSON serialization logic to logic layer
|                           |                                      |                           | Keep Activity-dependent methods in UI
|`GameManager.java`         | Extract core logic                   | `roboyard.logic.core`     | Extract `GameStateManager` to handle game rules/state
|                           |                                      |                           | Keep UI interactions (screen transitions) in original

## 6. Problematic/Ambiguous Files
| File                      | Issue                               | Solution                                | Description 
|---------------------------|-------------------------------------|-----------------------------------------|-------------------------------
|`MapObjects.java`          | Mixes game object                   | Create two copies                       | Contains both data models 
|                           | types and rendering paths           | - In `roboyard.logic.core` (logic only) | and rendering code 
|                           |                                     | - In `roboyard.ui.components` (UI only) |  
|`Preferences.java`         | Uses Android `SharedPreferences`    | Create                                  | Handles app settings 
|                           |                                     | - Interface in `roboyard.logic.utils`   | with Android dependencies 
|                           |                                     | - Implementation in `roboyard.ui.utils` |  
|`UIConstants.java`         | Contains both UI and game constants | Create two copies | Mix of logical and UI constants 
|                           |                                     | - `GameConstants.java` in `roboyard.logic.utils` 
|                           |                                     | - `UIConstants.java` in `roboyard.ui.utils` 

## 6. Target Folder Structure

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

## 7. Implementation Steps
    1. Create `logic/` and `ui/` folders:
       ```
       mkdir -p app/src/main/java/roboyard/logic/{core,data,solver,utils}
       mkdir -p app/src/main/java/roboyard/ui/{activities,fragments,components,utils}
       ```
       
    2. Move Identified Files and Update Package Declarations:
       ```
       # Example for correctly moving a file
       # 1. Move file with git mv
       git mv app/src/main/java/roboyard/eclabs/AbstractGameObject.java app/src/main/java/roboyard/logic/core/
       
       # 2. Update package declaration in file
       sed -i "1s/package [^;]*/package roboyard.logic.core/" app/src/main/java/roboyard/logic/core/AbstractGameObject.java
       ```

    3. Refactor Mixed Classes:
        ◦ Duplicate files with different implementations for UI and logic
        ◦ Use interface extraction for shared functionality
        ◦ Example:
          ```java
          // In logic/utils/FileOperations.java
          public interface FileOperations {
              String readFile(String path); // Logic layer
          }
          
          // In ui/utils/FileOperationsImpl.java
          public class FileOperationsImpl implements FileOperations {
              Context context; // Android dependency only in UI layer
              
              public String readFile(String path) {
                  // Android implementation
              }
          }
          ```

    4. Update Imports in All Files:
        ◦ After each move, all imports must be updated
        ◦ Example: Replace `import roboyard.eclabs.Constants;` with `import roboyard.logic.core.Constants;`
        ◦ The script `update_imports.sh` can be used to automate this

    5. Validate via Build after each step:
        ◦ Ensure `logic/` has no dependencies on `android.*` or `javax.microedition.khronos.*`
        ◦ Run `./gradlew build` to verify no compilation errors
        ◦ Handle duplicate class issues by ensuring each class exists in only one package

## 8. Handling Duplicate Classes
When identical or similar classes exist in multiple packages (e.g., `GameGridView` in both `roboyard.logic.core.ui` and `roboyard.ui.components`), use one of these approaches:

1. **Interface Extraction**: Create a common interface in the logic layer with both implementations implementing it
2. **Delegation**: Have one implementation delegate to the other
3. **Unification**: Choose one location and update all references

This strategy ensures a clean separation between UI and logic components while avoiding compilation errors from duplicate class declarations.
