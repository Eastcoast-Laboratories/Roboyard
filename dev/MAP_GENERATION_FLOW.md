# Map Generation Flow Analysis

## Overview

This document describes the complete map generation and difficulty validation flow in Roboyard, including the dual-path validation system that was discovered during the keep-map button implementation.

## Map Generation Flow

### 1. Initial Game Start (`startGame()`)

```
startGame()
├── Reset game state
├── Reset regeneration counter
├── Reset keepCurrentMapDespiteDifficulty flag
└── createValidGame(boardWidth, boardHeight)
```

### 2. Core Map Generation (`createValidGame()`)

```
createValidGame(width, height)
├── 🚫 EARLY RETURN if keepCurrentMapDespiteDifficulty == true
├── Create new random GameState
├── Initialize solver with grid elements
├── Quick trivial puzzle check (isTrivialPuzzle)
│   └── If trivial → createValidGame() (recursive)
└── Start solver with DifficultyValidationCallback
    └── calculateSolutionAsync(new DifficultyValidationCallback(width, height))
```

### 3. Solver Execution

```
SolverManager.solve()
├── Find solution using DriftingDroids solver
├── onSolverFinished(success, solution)
└── GameStateManager.onSolverFinished()
    └── onSolutionCalculationCompleted(solution)
```

## Dual Difficulty Validation Paths

### Path 1: Main Validation (`onSolutionCalculationCompleted`)

**Location**: `GameStateManager.java` ~line 2981

```
onSolutionCalculationCompleted(solution)
├── Check if level mode / loaded savegame
├── Get move count from solution
├── ✅ CHECKS keepCurrentMapDespiteDifficulty flag
├── If too easy OR too hard AND keepCurrentMapDespiteDifficulty == false:
│   ├── Log regeneration reason
│   ├── Increment regenerationCount
│   ├── Reset solver state
│   └── postDelayed(() -> createValidGame(width, height), 100)
├── Store solution (currentSolution, currentSolutionStep)
├── Set solutionWasAccepted = true
└── 🔄 CALL solutionCallback.onSolutionCalculationCompleted(solution)
```

### Path 2: Callback Validation (`DifficultyValidationCallback`)

**Location**: `GameStateManager.java` ~line 3458

```
DifficultyValidationCallback.onSolutionCalculationCompleted(solution)
├── Increment attemptCount
├── Get move count and difficulty bounds
├── ✅ NOW CHECKS keepCurrentMapDespiteDifficulty flag (FIXED)
├── If keepCurrentMapDespiteDifficulty == true:
│   └── Skip all validation → accept puzzle
└── Else perform validation:
    ├── Check isSolution01() (1-move puzzle)
    │   └── If too easy → createValidGame(width, height)
    ├── Check moveCount == 0 (no solution)
    │   └── If no solution → createValidGame(width, height)
    ├── Check moveCount < requiredMoves (too easy)
    │   └── If too easy → createValidGame(width, height)
    ├── Check moveCount > maxMoves (too hard)
    │   └── If too hard → createValidGame(width, height)
    └── If all checks pass → accept puzzle
```

## Race Condition Analysis

### The Problem

```
Timeline:
T1: User starts game → createValidGame() → Solver starts for Map A
T2: Solver finds Map A too easy → schedules regeneration via postDelayed
T3: User clicks keep-map button → sets keepCurrentMapDespiteDifficulty = true
T4: postDelayed executes → createValidGame() creates Map B → starts new solver
T5: Solver for Map B finds solution → dual validation runs
```

### The Fix

1. **DifficultyValidationCallback**: Now respects `keepCurrentMapDespiteDifficulty` flag
2. **createValidGame()**: Early return when flag is set, blocking scheduled regenerations

## Is This Design Sensible?

### Problems with Current Design

1. **Dual Validation**: Two separate difficulty validation paths create complexity and potential inconsistencies
2. **Race Conditions**: Asynchronous `postDelayed` regenerations can conflict with user actions
3. **Recursive Calls**: `createValidGame()` can call itself recursively via trivial puzzle check
4. **State Management**: Multiple flags (`keepCurrentMapDespiteDifficulty`, `validateDifficulty`, `allowRegeneration`) create complex state interactions

### Alternative Approaches

#### Option 1: Single Validation Path
```java
// Only validate in one place, eliminate DifficultyValidationCallback
onSolutionCalculationCompleted(solution) {
    if (shouldRegenerate(solution)) {
        regenerateImmediately(); // No postDelayed
    } else {
        acceptSolution();
    }
}
```

#### Option 2: Command Pattern for Regeneration
```java
interface RegenerationCommand {
    boolean shouldExecute();
    void execute();
}

class KeepMapOverride implements RegenerationCommand {
    boolean shouldExecute() { return !keepCurrentMapDespiteDifficulty; }
    void execute() { createValidGame(); }
}
```

#### Option 3: State Machine
```java
enum MapGenerationState {
    GENERATING, VALIDATING, ACCEPTED, USER_OVERRIDE
}
```

### Recommendation

The dual-path design adds unnecessary complexity. Consider refactoring to:

1. **Single validation point** - Move all difficulty logic to `onSolutionCalculationCompleted()`
2. **Immediate regeneration** - Remove `postDelayed` and regenerate synchronously
3. **Clearer state management** - Use a single flag or state machine
4. **Better separation of concerns** - Separate map generation from difficulty validation

## Keep-Map Button Flow (Fixed)

```
User clicks keep-map button
├── gameStateManager.keepCurrentMapDespiteDifficulty = true
├── Solver continues running to completion
├── onSolutionCalculationCompleted() sees flag → skips regeneration
├── DifficultyValidationCallback sees flag → skips validation
└── Current map is accepted with full solution available
```

## Key Files

- `GameStateManager.java` - Main logic, both validation paths
- `GameFragment.java` - Keep-map button implementation
- `SolverManager.java` - Solver execution and callbacks
- `Constants.java` - Difficulty thresholds and limits

## Debug Tags for Analysis

- `[SOLUTION_SOLVER][MOVES]` - Main validation path
- `[DifficultyValidationCallback]` - Callback validation path
- `[KEEP_MAP_ENFORCER]` - Keep-map flag checks
- `[TRIVIAL_CHECK]` - Trivial puzzle detection
