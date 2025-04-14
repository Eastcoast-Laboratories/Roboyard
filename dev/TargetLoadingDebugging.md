# Target Loading Debugging Plan for Roboyard

## Problem Statement

Targets are not being loaded correctly from saved games. This causes gameplay issues since targets are essential game elements that robots need to reach.

## Debugging Approach

### 1. Add Comprehensive Logging for Target Loading

```java
// In GameState.parseFromSaveData
Timber.d("[TARGET LOADING] Starting to parse save data with %d lines", lines.length);

// When processing targets section
Timber.d("[TARGET LOADING] Processing TARGETS section, found %d entries", targetLines.length);

// For each target being processed
Timber.d("[TARGET LOADING] Adding target: (%d,%d) with color %d", x, y, color);

// After all targets are processed
Timber.d("[TARGET LOADING] Finished loading targets, total count: %d", targetCount);
```

### 2. Add Verification Steps

```java
// In GameState.addTarget method
Timber.d("[TARGET LOADING] Before adding target at (%d,%d) with color %d", x, y, color);
// Check if coordinates are valid
if (x < 0 || y < 0 || x >= width || y >= height) {
    Timber.e("[TARGET LOADING] Invalid target coordinates: (%d,%d) with color %d", x, y, color);
    return; // Skip invalid targets
}
// Call the implementation
addTargetImpl(x, y, color);
Timber.d("[TARGET LOADING] After adding target, board cell type is %d", getCellType(x, y));
```

### 3. Inspect addTarget Method

The current implementation of `addTarget` might have issues:

```java
public void addTarget(int x, int y, int color) {
    GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
    target.setColor(color);
    gameElements.add(target);  // Adds to gameElements list
    setCellType(x, y, Constants.TYPE_TARGET);  // Updates board array
    setTargetColor(x, y, color);  // Updates targetColors array
}
```

Potential issues:
- gameElements might not be properly initialized
- setCellType might silently fail for some coordinates
- setTargetColor might silently fail for some coordinates

### 4. Check Serialization & Deserialization

Compare the target serialization with deserialization:

```java
// SERIALIZATION (check in exportToSaveData)
Timber.d("[TARGET LOADING] Serializing targets, count: %d", targetCount);
// For each target being serialized
Timber.d("[TARGET LOADING] Serializing target: (%d,%d) with color %d", x, y, color);

// DESERIALIZATION (check in parseFromSaveData)
Timber.d("[TARGET LOADING] Deserializing target line: %s", line);
```

### 5. Add Runtime Validations

In both GameState and GameStateManager, add runtime validations:

```java
// In GameState.validate() method (create if needed)
public boolean validate() {
    int targetsInGameElements = 0;
    int targetsInBoard = 0;
    
    // Count targets in gameElements
    for (GameElement element : gameElements) {
        if (element.getType() == GameElement.TYPE_TARGET) {
            targetsInGameElements++;
        }
    }
    
    // Count targets in board array
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            if (board[y][x] == Constants.TYPE_TARGET) {
                targetsInBoard++;
            }
        }
    }
    
    boolean valid = (targetsInGameElements == targetsInBoard);
    Timber.d("[TARGET VALIDATION] Game state validation: %s (gameElements: %d, board: %d)",
            valid ? "PASSED" : "FAILED", targetsInGameElements, targetsInBoard);
    return valid;
}
```

## Implementation Plan

1. Add the logging statements to track target loading
2. Add the validation checks to identify inconsistencies
3. Test with various saved games to trace the issue
4. Once the root cause is identified, fix the specific method(s) causing targets to be lost

## Expected Outcomes

- Identify exactly where targets are being lost during the loading process
- Determine if the issue is in serialization, deserialization, or both
- Develop a targeted fix for the specific component causing the problem
