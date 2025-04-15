# Concept

## Problem Description

Currently, there are two separate data structures for game elements due to their different purposes:
1. gameElements (List):
    ◦ A collection of all dynamic objects in the game (robots, targets)
    ◦ Used primarily for rendering and user interactions
    ◦ Contains detailed information about each element (position, type, color)
    ◦ Needed for the UI to draw the game elements on screen
    ◦ Also used by the game logic to track robots and their movements
2. board (2D array):
    ◦ A grid representation of the game board's cell types
    ◦ Used for fast spatial lookups and collision detection
    ◦ Stores only the type of cell at each position (empty, wall, target, robot)
    ◦ Provides a way to quickly check what exists at a specific coordinate
    ◦ Essential for movement logic to determine valid moves

This dual representation creates a challenge: the game needs to keep both data structures synchronized. When a target is added, it must be added to both:
    • The gameElements list (for rendering)
    • The board array (for collision detection)

refactoring of the codebase might introduce performance issues. Here's why:
1. Different access patterns:
    ◦ The board array provides O(1) access to determine what's at a specific coordinate
    ◦ Using gameElements for the same task would require iterating through the list (O(n) operation)
    ◦ This would severely impact performance for frequent operations like collision detection
2. Game logic dependencies:
    ◦ Many critical game functions (movement, collision detection) are optimized for grid-based lookups
    ◦ They depend on the board array's direct indexing for performance
    
# idea:
1. **GameStateManager**: Handles walls when creating new games via the "New Game" button
2. **GameLogic**: Handles walls when preserving a game state or resetting robots
3. **WallStorage**: Stores and retrieves walls, but is used inconsistently

This leads to bugs where walls may be displayed correctly on screen but not properly recognized by the solver.

## Proposed Solution

Create a dedicated `WallManager` service in the `roboyard.logic.core` package that centralizes all wall-related operations.

## Implementation Details

### 1. Create New WallManager Class

```java
package roboyard.logic.core;

import java.util.ArrayList;
import timber.log.Timber;

/**
 * Central wall management service that handles all wall-related operations.
 * This provides a single source of truth for wall management across the app.
 */
public class WallManager {
    private static final String TAG = "WallManager";
    private static WallManager instance;
    
    // Private constructor for singleton pattern
    private WallManager() {}
    
    /**
     * Get the singleton instance
     */
    public static WallManager getInstance() {
        if (instance == null) {
            instance = new WallManager();
        }
        return instance;
    }
    
    /**
     * Apply appropriate wall configuration to game elements
     * 
     * @param elements Grid elements to process
     * @param preserveWalls Whether to use stored walls or keep existing walls
     * @param generateNewMap Whether a completely new map should be generated
     * @return Updated grid elements with proper wall configuration
     */
    public ArrayList<GridElement> applyWallsToGameState(ArrayList<GridElement> elements, 
                                                       boolean preserveWalls,
                                                       boolean generateNewMap) {
        WallStorage wallStorage = WallStorage.getInstance();
        
        // Update with current board size
        wallStorage.updateCurrentBoardSize();
        
        ArrayList<GridElement> result = new ArrayList<>(elements);
        
        if (preserveWalls && !generateNewMap && wallStorage.hasStoredWalls()) {
            // Remove existing walls from the elements
            ArrayList<GridElement> nonWallElements = removeWalls(result);
            
            // Apply the stored walls to the non-wall elements
            result = wallStorage.applyWallsToElements(nonWallElements);
            
            Timber.tag(TAG).d("Applied stored walls to game state");
        } else if (!generateNewMap && wallStorage.hasStoredWalls()) {
            // This case is for "New Game" but preserving the wall layout
            // Remove existing walls from the elements
            ArrayList<GridElement> nonWallElements = removeWalls(result);
            
            // Apply the stored walls to the non-wall elements
            result = wallStorage.applyWallsToElements(nonWallElements);
            
            Timber.tag(TAG).d("Applied stored walls to new game");
        } else {
            // Store walls for future use if we're not generating new maps each time
            if (!Preferences.generateNewMapEachTime) {
                wallStorage.storeWalls(result);
                Timber.tag(TAG).d("Stored walls for future use");
            }
        }
        
        return result;
    }
    
    /**
     * Remove wall elements from a list of grid elements
     * 
     * @param elements List of grid elements
     * @return List with wall elements removed
     */
    private ArrayList<GridElement> removeWalls(ArrayList<GridElement> elements) {
        ArrayList<GridElement> nonWallElements = new ArrayList<>();
        for (GridElement element : elements) {
            if (!element.getType().equals("mh") && !element.getType().equals("mv")) {
                nonWallElements.add(element);
            }
        }
        return nonWallElements;
    }
}
```

### 2. Update GameStateManager

Replace the wall handling in `createValidGame()` with a call to `WallManager`:

```java
// Create a new random game state using static Preferences
GameState newState = GameState.createRandom();

// Apply wall configuration using centralized wall manager
boolean preserveWalls = !Preferences.generateNewMapEachTime;
boolean generateNewMap = true; // This is a new game initialization
ArrayList<GridElement> elements = newState.getGridElements();
elements = WallManager.getInstance().applyWallsToGameState(elements, preserveWalls, generateNewMap);
newState.setGridElements(elements);
```

### 3. Update GameLogic

Replace the wall handling in `generateGameMap()` with a call to `WallManager`:

```java
if (existingMap == null) {
    // Generate a new map based on board size
    if (boardWidth <= 8 || boardHeight <= 8) {
        // For small boards, use the simplified generation algorithm
        existingMap = generateSimpleGameMap3(null);
    } else {
        // For larger boards, use the standard generation algorithm
        existingMap = generateStandardGameMap();
    }
    
    // Apply wall configuration using centralized wall manager
    boolean preserveWalls = false;
    boolean generateNewMap = true;
    existingMap = WallManager.getInstance().applyWallsToGameState(existingMap, preserveWalls, generateNewMap);
    
    return existingMap;
} else {
    // We have an existing map and might need to preserve walls
    ArrayList<GridElement> data;
    
    // Apply wall configuration using centralized wall manager
    boolean generateNewMap = false;
    data = WallManager.getInstance().applyWallsToGameState(existingMap, preserveWalls, generateNewMap);
    
    // Add game elements (robots and targets) to the map
    data = addGameElementsToGameMap(data, null, null);
    
    return data;
}
```

## Benefits

1. **Single Source of Truth**: All wall management logic is centralized in one place
2. **Improved Maintainability**: Changes to wall handling only need to be made in one location
3. **Consistent Behavior**: Wall handling behaves consistently across different code paths
4. **Architecture Alignment**: Follows the refactoring plan to separate logic and UI concerns
5. **Enhanced Debugging**: Easier to debug wall-related issues with centralized logging

## Implementation Plan

1. Create the new `WallManager` class in `roboyard.logic.core`
2. Update `GameStateManager` to use `WallManager`
3. Update `GameLogic` to use `WallManager`
4. Ensure `WallStorage` is only used via `WallManager`
5. Test all code paths to verify consistent wall behavior

## Future Enhancements

- Consider incorporating wall generation logic into `WallManager` as well
- Add unit tests for wall management functionality
- Potentially extract wall types and constants to a dedicated `WallConstants` class
