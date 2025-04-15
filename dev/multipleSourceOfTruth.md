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

This dual representation creates a challenge: the game needs to keep both data structures synchronized. When any game element (robot, target, wall) is added, moved, or removed, it must be updated in both:
    • The gameElements list (for rendering)
    • The board array (for collision detection)

Refactoring of the codebase might introduce performance issues. Here's why:
1. Different access patterns:
    ◦ The board array provides O(1) access to determine what's at a specific coordinate
    ◦ Using gameElements for the same task would require iterating through the list (O(n) operation)
    ◦ This would severely impact performance for frequent operations like collision detection
2. Game logic dependencies:
    ◦ Many critical game functions (movement, collision detection) are optimized for grid-based lookups
    ◦ They depend on the board array's direct indexing for performance
    
# Current Issues:
Game elements (robots, targets, and walls) are managed inconsistently across several components:
1. **GameStateManager**: Manages game elements when creating new games via the "New Game" button
2. **GameLogic**: Manages game elements when preserving a game state or resetting robots
3. **WallStorage**: Stores and manages wall configurations for preserving walls across game changes, but is used inconsistently.

This leads to bugs where elements may be displayed correctly on screen but not properly
recognized by the solver, or where elements are out of sync between different game
components.

## Proposed Solution

Create a dedicated `GameElementManager` service in the `roboyard.logic.core` package that centralizes all game element operations (walls, robots, targets).

## Implementation Details

### 1. Create New GameElementManager Class

```java
package roboyard.logic.core;

import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Central game element management service that handles all element operations.
 * This provides a single source of truth for managing all game elements across the app.
 */
public class GameElementManager {
    private static final String TAG = "GameElementManager";
    private static GameElementManager instance;
    
    // Store references to different types of elements
    private final ArrayList<GridElement> robots = new ArrayList<>();
    private final ArrayList<GridElement> targets = new ArrayList<>();
    private final ArrayList<GridElement> walls = new ArrayList<>();
    
    // Private constructor for singleton pattern
    private GameElementManager() {}
    
    /**
     * Get the singleton instance
     */
    public static GameElementManager getInstance() {
        if (instance == null) {
            instance = new GameElementManager();
        }
        return instance;
    }
    
    /**
     * Synchronize all game elements between gameElements list and board array
     * 
     * @param elements Current grid elements
     * @param preserveElements Whether to use stored elements or regenerate them
     * @param generateNewMap Whether a completely new map should be generated
     * @return Updated grid elements with proper configuration
     */
    public ArrayList<GridElement> synchronizeGameElements(ArrayList<GridElement> elements, 
                                                        boolean preserveElements,
                                                        boolean generateNewMap) {
        Timber.tag(TAG).d("Synchronizing game elements: preserveElements=%b, generateNewMap=%b", 
                        preserveElements, generateNewMap);
        
        // Update storage with current board size
        updateStorageWithCurrentBoardSize();
        
        ArrayList<GridElement> result = new ArrayList<>();
        
        if (preserveElements && !generateNewMap && hasStoredElements()) {
            // Categorize elements by type
            categorizeElements(elements);
            
            // Apply stored elements where appropriate
            result = applyStoredElements(removeAllElements(elements));
            
            Timber.tag(TAG).d("Applied stored elements to game state");
        } else if (!generateNewMap && hasStoredElements()) {
            // This case is for "New Game" but preserving the element layout
            result = applyStoredElements(removeAllElements(elements));
            
            Timber.tag(TAG).d("Applied stored elements to new game");
        } else {
            // Store elements for future use if we're not generating new maps each time
            if (!Preferences.generateNewMapEachTime) {
                storeElements(elements);
                result = new ArrayList<>(elements);
                Timber.tag(TAG).d("Stored elements for future use");
            } else {
                result = new ArrayList<>(elements);
            }
        }
        
        // Ensure board array is synchronized with gameElements
        synchronizeBoardArray(result);
        
        return result;
    }
    
    /**
     * Synchronize the board array with the gameElements list
     * This ensures both data structures represent the same game state
     * 
     * @param elements Current grid elements
     */
    private void synchronizeBoardArray(ArrayList<GridElement> elements) {
        // Get reference to the board array (this would access the appropriate singleton)
        int[][] board = GameBoard.getInstance().getBoard();
        
        // First clear the board
        clearBoard(board);
        
        // Then repopulate it with all elements
        for (GridElement element : elements) {
            int x = element.getX();
            int y = element.getY();
            String type = element.getType();
            
            // Map element type to board cell type and update board
            int cellType = mapElementTypeToCellType(type);
            if (cellType != -1 && x >= 0 && y >= 0 && x < board.length && y < board[0].length) {
                board[x][y] = cellType;
                Timber.tag(TAG).v("Set board[%d][%d] = %d (from %s)", x, y, cellType, type);
            }
        }
        
        // Handle special edge cases like board borders
        addBoardBorders(board);
    }
    
    /**
     * Move a robot and ensure both data structures are updated
     * 
     * @param robotId ID of the robot to move
     * @param newX New X coordinate
     * @param newY New Y coordinate
     * @param gameElements Current game elements list
     * @return Updated game elements list
     */
    public ArrayList<GridElement> moveRobot(int robotId, int newX, int newY, ArrayList<GridElement> gameElements) {
        // Find the robot in the gameElements list
        GridElement robotElement = null;
        for (GridElement element : gameElements) {
            if (element.getType().startsWith("robot_") && element.getId() == robotId) {
                robotElement = element;
                break;
            }
        }
        
        if (robotElement == null) {
            Timber.tag(TAG).e("Robot with ID %d not found", robotId);
            return gameElements;
        }
        
        // Get current position for board array update
        int oldX = robotElement.getX();
        int oldY = robotElement.getY();
        
        // Update the robot position in gameElements
        robotElement.setX(newX);
        robotElement.setY(newY);
        
        // Update the board array
        int[][] board = GameBoard.getInstance().getBoard();
        if (oldX >= 0 && oldY >= 0 && oldX < board.length && oldY < board[0].length) {
            board[oldX][oldY] = Constants.CELL_EMPTY; // Clear old position
        }
        if (newX >= 0 && newY >= 0 && newX < board.length && newY < board[0].length) {
            board[newX][newY] = Constants.CELL_ROBOT; // Set new position
        }
        
        Timber.tag(TAG).d("Moved robot %d from (%d,%d) to (%d,%d)", robotId, oldX, oldY, newX, newY);
        
        return gameElements;
    }
    
    /**
     * Add a new element to the game and ensure both data structures are updated
     * 
     * @param type Element type
     * @param x X coordinate
     * @param y Y coordinate
     * @param gameElements Current game elements list
     * @return Updated game elements list
     */
    public ArrayList<GridElement> addElement(String type, int x, int y, ArrayList<GridElement> gameElements) {
        // Create new element
        GridElement newElement = createGridElement(type, x, y);
        
        // Add to gameElements list
        gameElements.add(newElement);
        
        // Update board array
        int[][] board = GameBoard.getInstance().getBoard();
        if (x >= 0 && y >= 0 && x < board.length && y < board[0].length) {
            board[x][y] = mapElementTypeToCellType(type);
        }
        
        Timber.tag(TAG).d("Added %s element at (%d,%d)", type, x, y);
        
        return gameElements;
    }
    
    // Helper methods
    
    /**
     * Remove all elements of specific types from a list
     */
    private ArrayList<GridElement> removeAllElements(ArrayList<GridElement> elements) {
        ArrayList<GridElement> result = new ArrayList<>();
        for (GridElement element : elements) {
            // Keep only elements we want to preserve (this could be configurable)
            if (!element.getType().startsWith("robot_") && 
                !element.getType().startsWith("target_") && 
                !element.getType().equals("mh") && 
                !element.getType().equals("mv")) {
                result.add(element);
            }
        }
        return result;
    }
    
    /**
     * Categorize elements by type for easier management
     */
    private void categorizeElements(ArrayList<GridElement> elements) {
        robots.clear();
        targets.clear();
        walls.clear();
        
        for (GridElement element : elements) {
            String type = element.getType();
            if (type.startsWith("robot_")) {
                robots.add(element);
            } else if (type.startsWith("target_")) {
                targets.add(element);
            } else if (type.equals("mh") || type.equals("mv")) {
                walls.add(element);
            }
        }
    }
    
    /**
     * Apply stored elements to a base set of elements
     */
    private ArrayList<GridElement> applyStoredElements(ArrayList<GridElement> baseElements) {
        ArrayList<GridElement> result = new ArrayList<>(baseElements);
        result.addAll(robots);
        result.addAll(targets);
        result.addAll(walls);
        return result;
    }
    
    /**
     * Store current elements for future use
     */
    private void storeElements(ArrayList<GridElement> elements) {
        categorizeElements(elements);
    }
    
    /**
     * Check if we have stored elements
     */
    private boolean hasStoredElements() {
        return !robots.isEmpty() || !targets.isEmpty() || !walls.isEmpty();
    }
    
    /**
     * Map element type to board cell type
     */
    private int mapElementTypeToCellType(String type) {
        if (type.startsWith("robot_")) {
            return Constants.CELL_ROBOT;
        } else if (type.startsWith("target_")) {
            return Constants.CELL_TARGET;
        } else if (type.equals("mh") || type.equals("mv")) {
            return Constants.CELL_WALL;
        } else {
            return Constants.CELL_EMPTY;
        }
    }
    
    /**
     * Create a new grid element of the specified type
     */
    private GridElement createGridElement(String type, int x, int y) {
        // Implementation depends on GridElement constructor
        // This is a simplified example
        return new GridElement(type, x, y);
    }
    
    /**
     * Update storage with current board size
     */
    private void updateStorageWithCurrentBoardSize() {
        // This would update any size-dependent storage
    }
    
    /**
     * Clear the board array
     */
    private void clearBoard(int[][] board) {
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                board[x][y] = Constants.CELL_EMPTY;
            }
        }
    }
    
    /**
     * Add borders to the board
     */
    private void addBoardBorders(int[][] board) {
        int width = board.length;
        int height = board[0].length;
        
        // Set borders as walls
        for (int x = 0; x < width; x++) {
            // Top and bottom borders
            if (x >= 0 && x < width) {
                board[x][0] = Constants.CELL_WALL;
                board[x][height-1] = Constants.CELL_WALL;
            }
        }
        
        for (int y = 0; y < height; y++) {
            // Left and right borders
            if (y >= 0 && y < height) {
                board[0][y] = Constants.CELL_WALL;
                board[width-1][y] = Constants.CELL_WALL;
            }
        }
    }
}
```

### 2. Update GameStateManager

Replace the element handling in `createValidGame()` with a call to `GameElementManager`:

```java
// Create a new random game state using static Preferences
GameState newState = GameState.createRandom();

// Apply element configuration using centralized element manager
boolean preserveElements = !Preferences.generateNewMapEachTime;
boolean generateNewMap = true; // This is a new game initialization
ArrayList<GridElement> elements = newState.getGridElements();
elements = GameElementManager.getInstance().synchronizeGameElements(elements, preserveElements, generateNewMap);
newState.setGridElements(elements);
```

### 3. Update GameLogic

Replace the element handling in `generateGameMap()` with a call to `GameElementManager`:

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
    
    // Apply element configuration using centralized element manager
    boolean preserveElements = false;
    boolean generateNewMap = true;
    existingMap = GameElementManager.getInstance().synchronizeGameElements(existingMap, preserveElements, generateNewMap);
    
    return existingMap;
} else {
    // We have an existing map and might need to preserve elements
    ArrayList<GridElement> data;
    
    // Apply element configuration using centralized element manager
    boolean generateNewMap = false;
    data = GameElementManager.getInstance().synchronizeGameElements(existingMap, preserveElements, generateNewMap);
    
    return data;
}
```

### 4. Update Robot Movement Handling

Replace direct modifications of robot positions with `GameElementManager` calls:

```java
// Instead of directly modifying the robot position
// robot.setX(newX);
// robot.setY(newY);
// board[oldX][oldY] = EMPTY;
// board[newX][newY] = ROBOT;

// Use the centralized GameElementManager
ArrayList<GridElement> updatedElements = GameElementManager.getInstance().moveRobot(
    robot.getId(), newX, newY, gameState.getGridElements());
gameState.setGridElements(updatedElements);
```

## Benefits

1. **Single Source of Truth**: All game element management is centralized in one place
2. **Improved Synchronization**: Elements are always kept in sync between gameElements and board
3. **Consistent Behavior**: Element handling behaves consistently across different code paths
4. **Improved Debugging**: Easier to debug element-related issues with centralized logging
5. **Better Abstraction**: Game logic doesn't need to know about multiple data structures
6. **Enhanced Testability**: Easier to write unit tests for element management logic

## Implementation Plan

1. Create the new `GameElementManager` class in `roboyard.logic.core`
2. Create or adapt a `GameBoard` class to represent the board array as a singleton
3. Update `GameStateManager` to use `GameElementManager`
4. Update `GameLogic` to use `GameElementManager`
5. Update robot movement logic to use `GameElementManager`
6. adapt `applyWallsToElements` in `WallStorage`
7. adapt the save/load/history system
8. Test all code paths to verify consistent element behavior

## Future Enhancements

- Add support for undo/redo operations via element state history
- Implement serialization/deserialization of game elements for save/load functionality
- adapt the minimap generation in the save/load/history system
- Create specific element sub-managers (RobotManager, TargetManager, WallManager) that operate under the main GameElementManager
- Add advanced debugging features like
 - move the `generateAsciiMap` board state visualization from `RRGetMap` here
- Implement board state validation to catch inconsistencies early
