# Refactoring Plan: Separation of UI and Logic Concerns

## Current Issues

The current implementation has several architectural concerns:

1. **Blurred Responsibility Boundaries**: Movement execution logic is mixed with solution management
2. **Tight Coupling**: UI components (robot movement) are coupled with solution management
3. **Redundant Systems**: Multiple systems trying to execute the same moves

## Architectural Principles

### Proper Separation of Concerns

1. **UI Layer**: Should handle all visual representation and animation
   - Robot movement and positioning
   - Visual feedback and animations
   - User interaction

2. **Logic Layer**: Should handle game state and solution computation
   - Solving algorithms
   - Solution representation
   - Game state management

## Key Components

### Existing Components to Keep

1. **GameStateManager**
   - Central manager coordinating game state and solver
   - Correctly positioned as the intermediary between UI and logic

2. **GameState**
   - Representation of board, robots, and targets
   - Pure data structure without UI concerns

3. **ISolver and SolverDD**
   - Solver implementations that generate solutions
   - Pure logic with no UI dependencies

4. **GameMove**
   - Representation of a single move
   - Should be a pure data object without execution logic

### Components to Refactor or Remove

1. **GameMoveManager**
   - Currently tries to handle both solution representation AND movement execution
   - Should be removed or refactored to only handle solution representation

## Implementation Plan

### 1. Remove GameMoveManager

Since GameMoveManager is trying to handle UI concerns (robot movement), we should remove it entirely.

### 2. Enhance the Solver Component

The solver is pure logic and should remain isolated from UI concerns:

```java
// SolverManager (already exists)
// - Handles solver algorithm selection
// - Manages the solving process
// - Returns solutions without UI knowledge

// GameSolution (already exists)
// - Pure data structure for solutions
// - Contains moves but doesn't execute them
```

### 3. Consider a Solution Interpreter Utility

If needed, create a utility class that only translates solution formats:

```java
// SolutionInterpreter (potential new utility)
// - Converts solution format to game-specific format
// - Does NOT execute moves
// - Pure data transformation, no UI logic
```

### 4. Keep Movement Logic in UI Layer

The UI layer should handle all movement logic:

```java
// GridGameScreen
// - Continues to handle the doMovesInMemory method
// - Handles robot movement visualization
// - Handles UI state during movement

// GamePiece
// - Handles individual robot movement mechanics
// - Manages animation and position updates
```

### 5. Define Clean Interfaces Between Logic and UI

Establish clear interaction points between logic and UI:

```java
// When solving is complete:
// 1. SolverManager notifies via listener
// 2. UI receives solution data structure
// 3. UI is responsible for visualization and movement
```

## Implementation Steps

1. **Delete GameMoveManager class** or refactor it to only handle solution representation
   
2. **Update GridGameScreen**:
   - Remove GameMoveManager field and initialization
   - Ensure doMovesInMemory only reads from solution data structures
   - Keep movement logic contained within GridGameScreen and GamePiece
   
3. **Enhance debug logging** to help trace movement issues in both GamePiece and GridGameScreen

## Benefits

1. **Cleaner Architecture**: Clear separation between UI and logic
2. **Easier Maintenance**: Changes to UI don't affect logic and vice versa
3. **Better Testability**: Logic can be tested independently of UI
4. **Reduced Bugs**: Fewer unexpected interactions between components
