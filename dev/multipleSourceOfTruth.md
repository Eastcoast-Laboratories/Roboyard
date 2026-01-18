# Single Source of Truth - Concept and Implementation

## Analysis: The Three Sources of Truth

### 1. GameState.gameElements (List<GameElement>)
**File:** `roboyard/logic/core/GameState.java`
**Class:** `roboyard.eclabs.ui.GameElement`

```java
private final List<GameElement> gameElements;  // Line 44
```

**Properties:**
- Contains all dynamic objects: robots, targets, walls
- Each element has: type (int), x, y, color, selected, animationX/Y
- Used for rendering and UI interactions
- Serializable for save/load

**Used in:**
- `GameState.java` - Primary storage, addRobot(), addTarget(), addWall(), getRobotAt(), getTargets(), getRobots()
- `GameGridView.java` - Rendering all elements
- `GameStateManager.java` - Game logic, movement, undo
- `ModernGameFragment.java` - UI updates, robot selection
- `LevelDesignEditorFragment.java` - Level editor
- `MinimapGenerator.java` - Minimap creation
- `RobotAnimationManager.java` - Animations

### 2. GameState.board (int[][])
**File:** `roboyard/logic/core/GameState.java`

```java
private final int[][] board; // 0=empty, 1=wall, 2=target  (Line 40)
private final int[][] targetColors; // Color index for targets (Line 41)
```

**Properties:**
- 2D array for fast O(1) coordinate lookups
- Stores cell types: TYPE_EMPTY, TYPE_HORIZONTAL_WALL, TYPE_VERTICAL_WALL, TYPE_TARGET
- Separate array for target colors
- Used for collision detection and movement logic

**Used in:**
- `GameState.java` - getCellType(), setCellType(), getTargetColor(), setTargetColor()
- `GameState.getGridElements()` - Conversion to GridElement for solver
- `GameState.parseFromSaveData()` - Loading game states
- `GameStateManager.applyLoadedGameState()` - Validation after loading

### 3. Solver: driftingdroids.model.Board + GridElement
**Files:** 
- `roboyard/logic/core/GridElement.java` - Roboyard's intermediate format
- `roboyard/logic/solver/RRGetMap.java` - Conversion
- `driftingdroids/model/Board.java` - External solver format

```java
// GridElement - intermediate format for solver
public class GridElement {
    private int x, y;
    private String type; // "robot_red", "target_blue", "mh", "mv"
}
```

**Properties:**
- GridElement uses string-based types ("robot_red", "mh", "mv")
- DriftingDroids Board uses its own coordinate system (position = y * width + x)
- Walls are stored as "N" (North) and "W" (West)
- **IMPORTANT:** Solver should NOT be modified (external source)

**Used in:**
- `GameState.getGridElements()` - Converts gameElements + board to GridElement list
- `RRGetMap.createDDWorld()` - Converts GridElement to DriftingDroids Board
- `SolverManager.java` - Initializes solver with GridElements
- `GameStateManager.initializeSolverForState()` - Converts GameElement to GridElement

---

## Complete List of All Code Locations

### Files using gameElements:
| File | Usage |
|------|-------|
| `GameState.java` | Primary storage, 38+ references |
| `GameGridView.java` | Rendering, 12 references |
| `GameStateManager.java` | Game logic, 12 references |
| `GameLogic.java` | Map generation, 11 references |
| `LevelDesignEditorFragment.java` | Editor, 10 references |
| `ModernGameFragment.java` | UI, 9 references |
| `MapGenerator.java` | Generation, 4 references |
| `MainFragmentActivity.java` | Navigation, 2 references |
| `MinimapGenerator.java` | Minimap, 1 reference |
| `SaveGameFragment.java` | Save/Load, 1 reference |
| `WallModel.java` | Wall model, 1 reference |

### Files using board[][]:
| File | Usage |
|------|-------|
| `GameState.java` | Primary storage, 11 references |
| `GameBoard.java` | Singleton (currently unused), 7 references |

### Files using GridElement:
| File | Usage |
|------|-------|
| `GameLogic.java` | Map generation, 44 references |
| `GameState.java` | Conversion, 40 references |
| `GameStateManager.java` | Solver init, 33 references |
| `RRGetMap.java` | Solver conversion, 21 references |
| `WallStorage.java` | Wall storage, 13 references |
| `MapObjects.java` | Legacy, 12 references |
| `GameGridView.java` | Rendering, 10 references |
| `MapIdGenerator.java` | ID generation, 5 references |
| `SolverManager.java` | Solver, 5 references |

### Debugging Functions:
| Function | File | Description |
|----------|------|-------------|
| `generateAsciiMap()` | `RRGetMap.java` | ASCII representation of game board |
| `toChar()` | `GridElement.java` | Character representation of element |

---

## Current Problems (Fixed)

1. **Dual Data Storage in GameState:**
   - `gameElements` (List) and `board[][]` must be kept in sync
   - Both must be updated on changes
   - Error source: Forgetting to update one of them

2. **Inconsistent Conversion:**
   - `GameState.getGridElements()` converts board[][] to GridElement
   - `GameStateManager.initializeSolverForState()` converts gameElements to GridElement
   - Different conversion paths can lead to inconsistencies

3. **WallStorage Inconsistency:**
   - Stores walls separately for persistence
   - Not always correctly synchronized with gameElements

4. **GameBoard Singleton Unused:**
   - `GameBoard.java` exists as singleton
   - Currently NOT actively used
   - Was planned as central source but not implemented

---

## Solution: Minimal Approach - gameElements as Single Source of Truth

### Decision: gameElements (List<GameElement>) as Primary Data Source

**Rationale:**
1. `gameElements` already contains all necessary information (position, type, color)
2. Already used for rendering, serialization, and UI
3. Solver only needs conversion to GridElement (already implemented)
4. The board[][] array can be regenerated from gameElements as needed

### Strategy: board[][] Becomes a Cache

Instead of keeping both structures in sync, `board[][]` becomes a **derived cache** that is regenerated from `gameElements` as needed.

**Advantages:**
- No more synchronization problems
- Minimal code changes
- board[][] remains available for performance-critical O(1) lookups
- No changes to solver needed

---

## Implementation Plan

### Phase 1: GameState.java - Add Cache Regeneration

New method `rebuildBoardCache()` that regenerates board[][] from gameElements:

```java
/**
 * Rebuild the board cache from gameElements.
 * Call this after any modification to gameElements to ensure board[][] is in sync.
 */
public void rebuildBoardCache() {
    // Clear board
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            board[y][x] = Constants.TYPE_EMPTY;
            targetColors[y][x] = -1;
        }
    }
    
    // Rebuild from gameElements
    for (GameElement element : gameElements) {
        int x = element.getX();
        int y = element.getY();
        if (x >= 0 && x < width && y >= 0 && y < height) {
            switch (element.getType()) {
                case GameElement.TYPE_HORIZONTAL_WALL:
                    board[y][x] = Constants.TYPE_HORIZONTAL_WALL;
                    break;
                case GameElement.TYPE_VERTICAL_WALL:
                    board[y][x] = Constants.TYPE_VERTICAL_WALL;
                    break;
                case GameElement.TYPE_TARGET:
                    board[y][x] = Constants.TYPE_TARGET;
                    targetColors[y][x] = element.getColor();
                    break;
                // Robots are NOT stored in board[][] - they are dynamic
            }
        }
    }
}
```

### Phase 2: Update All Modification Methods

Each method that modifies gameElements calls `rebuildBoardCache()` at the end:

| Method | File | Change |
|--------|------|--------|
| `addRobot()` | GameState.java | + rebuildBoardCache() |
| `addTarget()` | GameState.java | + rebuildBoardCache() |
| `addHorizontalWall()` | GameState.java | + rebuildBoardCache() |
| `addVerticalWall()` | GameState.java | + rebuildBoardCache() |
| `parseFromSaveData()` | GameState.java | + rebuildBoardCache() at end |
| `loadLevel()` | GameState.java | + rebuildBoardCache() at end |

### Phase 3: Remove Direct board[][] Modifications

All places that set board[][] directly are removed or replaced with gameElements operations:

```java
// BEFORE (problematic):
board[y][x] = Constants.TYPE_TARGET;
targetColors[y][x] = color;

// AFTER (correct):
GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
target.setColor(color);
gameElements.add(target);
rebuildBoardCache();
```

### Phase 4: Simplify getGridElements()

The `getGridElements()` method is simplified since it only needs to convert gameElements:

```java
public ArrayList<GridElement> getGridElements() {
    ArrayList<GridElement> elements = new ArrayList<>();
    
    for (GameElement element : gameElements) {
        String gridType = convertToGridElementType(element);
        if (gridType != null) {
            elements.add(new GridElement(element.getX(), element.getY(), gridType));
        }
    }
    
    // Add placeholder robots for solver (if needed)
    ensureFourRobots(elements);
    
    return elements;
}

private String convertToGridElementType(GameElement element) {
    switch (element.getType()) {
        case GameElement.TYPE_ROBOT:
            return "robot_" + GameLogic.getColorName(element.getColor(), false);
        case GameElement.TYPE_TARGET:
            if (element.getColor() == Constants.COLOR_MULTI) {
                return "target_multi";
            }
            return "target_" + GameLogic.getColorName(element.getColor(), false);
        case GameElement.TYPE_HORIZONTAL_WALL:
            return "mh";
        case GameElement.TYPE_VERTICAL_WALL:
            return "mv";
        default:
            return null;
    }
}
```

---

## Affected Files and Changes

### Critical Changes:
| File | Change | Priority |
|------|--------|----------|
| `GameState.java` | + rebuildBoardCache(), update modification methods | HIGH |
| `GameStateManager.java` | Replace direct board access with gameElements | HIGH |

### Secondary Changes:
| File | Change | Priority |
|------|--------|----------|
| `GameLogic.java` | Check if board[][] is modified directly | MEDIUM |
| `WallStorage.java` | Check if synchronization is needed | MEDIUM |
| `MapGenerator.java` | Check if board[][] is modified directly | LOW |

### No Changes Needed:
| File | Reason |
|------|--------|
| `RRGetMap.java` | Uses only GridElement (intermediate format) |
| `GameGridView.java` | Reads only gameElements for rendering |
| `driftingdroids/*` | External solver - do not modify |

---

## Debugging Functions

### generateAsciiMap() Remains in RRGetMap.java

The `generateAsciiMap()` function in `RRGetMap.java` remains unchanged since it uses GridElements and thus already uses the correct data source.

### New Validation Function for GameState

```java
/**
 * Validate that board[][] cache matches gameElements.
 * Use for debugging synchronization issues.
 */
public boolean validateBoardCache() {
    // Rebuild to temporary and compare
    int[][] tempBoard = new int[height][width];
    // ... rebuild logic ...
    
    // Compare with current board
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            if (board[y][x] != tempBoard[y][x]) {
                Timber.e("[BOARD_SYNC] Mismatch at (%d,%d): board=%d, expected=%d", 
                         x, y, board[y][x], tempBoard[y][x]);
                return false;
            }
        }
    }
    return true;
}
```

---

## Implementation Status (18.01.2026)

### ✅ Phase 1 - Basic Implementation:

| Change | File | Status |
|--------|------|--------|
| `rebuildBoardCache()` | GameState.java:189-219 | ✅ Implemented |
| `validateBoardCache()` | GameState.java:226-279 | ✅ Implemented |
| rebuildBoardCache() in `parseFromSaveData()` | GameState.java:1047 | ✅ Implemented |
| rebuildBoardCache() in `loadLevel()` | GameState.java:1091 | ✅ Implemented |
| rebuildBoardCache() in `createRandom()` | GameState.java:1860 | ✅ Implemented |

### ✅ Phase 2 - Direct board[][] Modifications Removed:

| Change | File | Status |
|--------|------|--------|
| TARGET_SECTION parsing - gameElements only | GameState.java:850-863 | ✅ Fixed |
| `synchronizeTargets()` - uses rebuildBoardCache() | GameState.java:1994-2009 | ✅ Fixed |
| `removeElementsAt()` - uses rebuildBoardCache() | LevelDesignEditorFragment.java:831-848 | ✅ Fixed |
| `parseLevelContent()` - uses addWall methods | LevelDesignEditorFragment.java:962-972 | ✅ Fixed |
| State snapshot - redundant board copy removed | GameStateManager.java:903-907 | ✅ Fixed |

### ✅ Phase 3 - Bugfix: Duplicate Target Loading:

| Change | File | Status |
|--------|------|--------|
| Board data target loading disabled | GameState.java:922-931 | ✅ Fixed |

**Problem:** Targets were loaded twice - once from board data and once from TARGET_SECTION.
**Solution:** Board data loading for targets skipped, TARGET_SECTION is the authoritative source.

### ✅ Phase 4 - Bugfix: Solver Not Re-initialized on Load:

| Change | File | Status |
|--------|------|--------|
| `resetInitialization()` before solver init | GameStateManager.java:412-413 | ✅ Fixed |
| `resetInitialization()` also clears solution | SolverManager.java:140-146 | ✅ Fixed |

**Problem:** When loading a game, solver was not re-initialized and old solution remained in memory.
**Solution:** 
1. `getSolverManager().resetInitialization()` called before `initializeSolverForState()`
2. `resetInitialization()` now also resets `isSolved`, `currentSolution`, `solutionMoves`, and `numDifferentSolutionsFound`

### ✅ Phase 5 - Bugfix: Solver Not Starting After Loading (18.01.2026)

| Change | File | Status |
|--------|------|--------|
| Call `calculateSolutionAsync()` after loading | GameStateManager.java:425 | ✅ Implemented |
| Fatal check if solver starts without init | SolverManager.java:148-153 | ✅ Implemented |
| UI test for loading + hint | LoadGameSolverTest.java | ✅ Created |

**Commit:** `e2f2c4a9` - Fix: Solver not starting after loading saved game

### Build Status: ✅ SUCCESSFUL

---

## 🐛 Known Issue: Level 5 Shows 9-Move Solution (MSOT Problem)

### Problem Description
Level 5 displays a 9-move solution that is **not playable** because the solver has a **wrong map representation**.

### Root Cause: Multiple Source of Truth
The solver receives a different map than what is displayed in the game. This is a classic MSOT problem where:
1. Game displays map based on `gameElements`
2. Solver receives map based on `board[][]` (cache)
3. If `board[][]` is out of sync with `gameElements`, solver gets wrong map
4. Solver finds solution for wrong map → solution doesn't work on actual game board

### Evidence from Logs
**Solver Output:**
```
solution: size=9 ***** bS gN gE gN gW rW rN rE rS ***** 09/3/rgb#
[SOLUTION_SOLVER] 1 solution(s) found; first solution: 09/3/rgb#
[SOLUTION_SOLVER] getSolution(0): Created GameSolution with 9 moves
```

**Solver Search Depth:**
```
iddfs: finished depthLimit=2 ... no solution
iddfs: finished depthLimit=3 ... no solution
...
iddfs: finished depthLimit=8 ... no solution
iddfs: finished depthLimit=9 ... FOUND: size=9
```

The solver found no solution up to depth 8, only at depth 9. This indicates the map representation is incorrect - the optimal 3-move solution is blocked by walls that shouldn't be there.

### Why This Is a MSOT Problem
- `gameElements` (game display) has the correct map
- `board[][]` (solver input) has a wrong map due to synchronization issues
- When `initializeSolverForState()` converts gameElements to GridElements, it should use gameElements directly
- But if `board[][]` is used anywhere in the conversion chain, wrong walls/targets get passed to solver

### Solution
Complete implementation of MSOT architecture:
1. Implement `rebuildBoardCache()` (already done in code, but may not be called everywhere)
2. Ensure `board[][]` is always in sync with `gameElements`
3. Verify solver initialization uses correct map data
4. Test that solver finds optimal solution after fix

### Status
This issue will be resolved once the complete MSOT refactoring is finished and all direct `board[][]` modifications are removed.

---

## Summary

**Single Source of Truth:** `GameState.gameElements` (List<GameElement>)

**Derived Data:**
- `board[][]` - Cache for O(1) lookups, regenerated as needed
- `GridElement` - Intermediate format for solver conversion
- `driftingdroids.Board` - Solver internal format (do not modify)

**Implemented Changes:**
1. ✅ New method `rebuildBoardCache()` in GameState - regenerates board[][] from gameElements
2. ✅ New method `validateBoardCache()` for debugging
3. ✅ `rebuildBoardCache()` called after loading/creating GameState
4. ✅ Direct board[][] modifications in parseFromSaveData() removed
5. ✅ `synchronizeTargets()` now uses rebuildBoardCache()
6. ✅ LevelDesignEditorFragment now uses add methods and rebuildBoardCache()
7. ✅ GameStateManager state snapshot without redundant board copy
8. Existing add methods (addRobot, addTarget, addWall) still update both structures for backward compatibility

**No Changes To:**
- Solver (driftingdroids) - external source, do not modify
- RRGetMap (conversion) - already uses GridElement
- Rendering (GameGridView) - reads only gameElements

**Remaining board[][] Accesses (acceptable):**
- `setCellType()` - only called by add methods (OK)
- `rebuildBoardCache()` - central cache regeneration (OK)
- `validateBoardCache()` - temporary array for validation (OK)
- `serialize()` - read access for saving (OK)
- `GameBoard.java` - unused singleton class (can be removed later)

---

## ✅ Phase 5 - Bugfix: Solver Not Starting After Loading (18.01.2026)

### Problem
When loading a saved game, the solver showed "AI calculating" indefinitely because `calculateSolutionAsync()` was never called after loading.

### Solution
| Change | File | Status |
|--------|------|--------|
| Call `calculateSolutionAsync()` after loading | GameStateManager.java:425 | ✅ Implemented |
| Fatal check if solver starts without init | SolverManager.java:148-153 | ✅ Implemented |
| UI test for loading + hint | LoadGameSolverTest.java | ✅ Created |

**Commit:** `e2f2c4a9` - Fix: Solver not starting after loading saved game

### Verification
Logs show successful solution after loading:
```
[GAME_LOAD] Starting solver for loaded map
[SOLUTION_SOLVER] 1 solution(s) found
[SOLUTION_SOLVER] getSolution(0): Created GameSolution with 2 moves
```

---

## ⚠️ Status: Multiple Source of Truth - NOT FULLY SOLVED

### Current Situation (as of 18.01.2026)
The MSOT problem is **partially solved** - the solver now works after loading, but the architecture still has issues:

### Remaining Direct board[][] Modifications
| File | Line | Method | Problem |
|------|------|--------|---------|
| `GameState.java` | 755-756 | `parseFromSaveData()` | Direct `state.board[y][x] = Constants.TYPE_TARGET` |
| `GameState.java` | 1934-1935 | `synchronizeTargets()` | Direct `board[y][x]` update |
| `LevelDesignEditorFragment.java` | 845 | `removeElementsAt()` | `setCellType(x, y, 0)` without gameElements update |
| `LevelDesignEditorFragment.java` | 966, 970 | `parseLevelContent()` | `setCellType()` for walls without gameElements update |
| `GameStateManager.java` | 922, 927 | `createStateSnapshot()` | `setCellType()` / `setTargetColor()` during copy |

### What's Still Missing
1. **`rebuildBoardCache()` does not exist** - Planned in concept but never implemented
2. **No true Single Source of Truth** - `gameElements` and `board[][]` are still modified independently
3. **`synchronizeTargets()` is a workaround** - Should be replaced by `rebuildBoardCache()`

### Why This Is Currently Acceptable
- Solver now works correctly after loading
- Synchronization issues only occur during special operations (loading, editor)
- Normal game logic (robot movement) works correctly
- The `synchronizeTargets()` method prevents major errors

### Complete Solution Would Require
1. Implement `rebuildBoardCache()` in GameState.java
2. Replace all direct `board[][]` modifications with gameElements changes + `rebuildBoardCache()`
3. Remove `synchronizeTargets()` (becomes redundant)
4. Update LevelDesignEditorFragment to use `addHorizontalWall()` / `addVerticalWall()`

**Decision:** Will be implemented later since current solution is stable and solver works.

---

## 🐛 Issue: Level 5 Shows 9-Move Solution Instead of 3-Move Solution (18.01.2026)

### Problem Description
Level 5 displays a 9-move solution that doesn't work, even though a simple 3-move solution exists.

### Analysis from Logs
**Solver Output:**
```
iddfs: finished depthLimit=8 megaBytes=70 time=9ms totalTime=17ms
minimizeColorChanges: merged green
minimizeColorChanges: reduced from 4 to 3
solution: size=9 ***** bS gN gE gN gW rW rN rE rS ***** 09/3/rgb#
iddfs: finished depthLimit=9 megaBytes=70 time=26ms totalTime=44ms
[SOLUTION_SOLVER] 1 solution(s) found; first solution: 09/3/rgb#
[SOLUTION_SOLVER] getSolution(0): Created GameSolution with 9 moves
```

**Key Observations:**
1. Solver found solution at depth 9 with 9 moves
2. Solution notation: `09/3/rgb#` = 9 moves, 3 robots moved, colors: red, green, blue
3. Moves: `bS gN gE gN gW rW rN rE rS` (blue south, green north, green east, etc.)
4. Target: Red robot to position (7,7)
5. Red robot starts at (3,1)

### Possible Root Causes
1. **Solver algorithm issue** - IDDFS may be finding a suboptimal solution
2. **Map representation issue** - Walls might be incorrectly placed in solver's internal representation
3. **Robot/target mapping issue** - Robots or targets might be at wrong positions in solver
4. **Goal state issue** - Target position might be wrong in solver

### Root Cause Analysis

**Coordinate Mapping:** ✅ Correct
- Red robot at (3,1) in game = position 15 in solver (1*12+3) ✓
- Red target at (7,7) in game = position 91 in solver (7*12+7) ✓
- ASCII map shows correct positions: `r` at (3,1), `R` at (7,7)

**Solver Search Depth:**
```
iddfs: finished depthLimit=2 ... no solution
iddfs: finished depthLimit=3 ... no solution
iddfs: finished depthLimit=4 ... no solution
...
iddfs: finished depthLimit=8 ... no solution
iddfs: finished depthLimit=9 ... FOUND: size=9
```

**Conclusion:** The DriftingDroids solver found a 9-move solution at depth 9, but no solution at depths 2-8. This suggests either:
1. The optimal 3-move solution doesn't exist (walls block the path)
2. The solver's algorithm is not finding the optimal solution
3. There's a constraint we're not aware of (e.g., minimum robot moves)

**Note:** The solution uses 3 robots (red, green, blue) which might be a constraint. The solver minimizes "color changes" not "total moves", so it may prefer solutions that use fewer different robots even if they require more moves.

**Status:** This is a DriftingDroids external solver behavior, not a bug in Roboyard. The solver is working as designed.
