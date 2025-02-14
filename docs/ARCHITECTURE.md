# Roboyard Architecture

This document describes the high-level architecture of Roboyard.

## Overview

Roboyard is an Android puzzle game where players guide robots through mazes. The game is built using Java and the Android SDK.

## Core Components

### 1. Game Management (`GameManager.java`)
- Central controller for the game
- Manages screen transitions
- Handles game state
- Coordinates between components

### 2. Screen System
Screens are managed through a screen ID system defined in `Constants.java`:
- Main Menu (0)
- Settings (2)
- Credits (3)
- Game Screen (4)
- Level Selection (5-8)
- Save/Load Screen (9)

See [screens.md](screens.md) for detailed screen documentation.

### 3. Game Logic

#### Grid System (`GridGameScreen.java`)
- Manages the game board
- Handles robot movement
- Collision detection
- Level completion checks

#### Movement System (`GameMovementInterface.java`)
- Processes user input
- Validates moves
- Updates robot positions
- Handles movement animations

#### Save System
- Autosave functionality
- Manual save slots
- Save state persistence
- Level progress tracking

### 4. Resource Management

#### Render Manager
- Handles graphics rendering
- Manages textures and sprites
- Screen transitions
- UI element drawing

#### Input Manager
- Touch input processing
- Gesture recognition
- Button interactions
- Back button handling

#### Asset Management
- Level data loading
- Image resource management
- Sound effects
- Game state serialization

## AI Components (`roboyard.pm.ia`)

The `roboyard.pm.ia` package contains the artificial intelligence (IA - Intelligence Artificielle) components used for solving the game and generating solutions. It consists of:

### Core Components
- `AGameState`: Abstract base class for representing game states
- `AWorld`: Abstract base class for the game world
- `GameSolution`: Class that holds a sequence of moves that solve a puzzle
- `IEndCondition`: Interface for defining win conditions
- `IGameMove`: Interface for representing game moves

### Ricochet Robots Implementation (`ricochet/`)
The `ricochet/` subpackage contains the concrete implementation for the Ricochet Robots game:
- `RRGameState`: Represents a state in the Ricochet Robots game
- `RRGameMove`: Represents a move in the game (robot movement)
- `RRWorld`: Represents the game board and its rules
- `RREndCondition`: Defines when a puzzle is solved
- `RRPiece`: Represents a robot piece on the board
- `RRGridCell`: Represents a cell on the game board
- `RRGetMap`: Helper class for loading game maps

The AI system is used by the solver (`SolverDD` class) to:
1. Find solutions to puzzles
2. Validate that generated random puzzles are solvable
3. Calculate the minimum number of moves needed
4. Generate hints and solutions for players

## DriftingDroids Integration (`driftingdroids.model`)

The game integrates the DriftingDroids Ricochet Robots solver (Copyright 2011-2014 Michael Henke, GPL v3) as a high-performance puzzle solver. The integration works as follows:

### Core Components
- `Board`: Core game board representation with efficient bit operations
- `Solver` (with `SolverBFS` and `SolverIDDFS` implementations): Advanced pathfinding algorithms
- `Solution` and `Move`: Represents solutions and individual moves
- `KeyDepthMap` and variants: Optimized data structures for state space exploration

### Integration Architecture
The integration between Roboyard's own AI system (`roboyard.pm.ia`) and DriftingDroids is managed through a bridge design pattern:

1. Bridge Component (`roboyard.eclabs.solver.SolverDD`)
   - Implements Roboyard's `ISolver` interface
   - Creates and manages the DriftingDroids `Board` and `Solver` instances
   - Translates between Roboyard's game elements and DriftingDroids' board representation
   - Converts DriftingDroids `Solution` objects into Roboyard's `GameSolution` format

2. Game Logic Layer (`roboyard.eclabs`)
   - Uses `SolverDD` through the `ISolver` interface
   - Handles user interaction and game state management
   - Remains independent of the actual solver implementation

3. AI Layer (`roboyard.pm.ia`)
   - Provides game-specific classes (`RRGameMove`, `RRPiece`, etc.)
   - Includes `RRGetMap` to convert game elements to DriftingDroids format
   - Defines the solution format (`GameSolution`) used by the game

4. Solver Layer (`driftingdroids.model`)
   - Provides the core solving algorithms
   - Works with its own optimized board representation
   - Returns solutions in its native format

### Data Flow
1. Game creates grid elements (`GridElement`) to represent the game state
2. `SolverDD.init()` uses `RRGetMap` to convert these to a DriftingDroids `Board`
3. DriftingDroids solver finds solutions
4. `SolverDD.getSolution()` converts DriftingDroids moves to Roboyard's format
5. Game displays the solution to the player

This architecture allows Roboyard to:
1. Use the efficient DriftingDroids solver while maintaining its own game logic
2. Easily swap out the solver implementation if needed
3. Keep the game code independent of the solver details
4. Support future puzzle types by implementing new solver bridges

The DriftingDroids solver is particularly used for:
- Validating randomly generated puzzles
- Finding optimal (minimum moves) solutions
- Supporting the hint system
- Calculating puzzle difficulty

## Data Flow

1. User Input
   ```
   Input Manager → Game Manager → Current Screen → Game Logic → Render Manager
   ```

2. Game State Changes
   ```
   Game Logic → Game Manager → Screen Update → Render Manager
   ```

3. Screen Transitions
   ```
   Game Manager → Previous Screen Cleanup → New Screen Init → Render Manager
   ```

## File Structure

```
app/src/main/
├── java/roboyard/eclabs/
│   ├── GameManager.java         # Game management
│   ├── GridGameScreen.java      # Main game screen
│   ├── Constants.java           # Game constants
│   ├── SaveGameScreen.java      # Save/load system
│   └── solver/                  # Game solver
│       ├── GameLevelSolver.java # Solver core
│       ├── ISolver.java         # Solver interface
│       ├── SolverDD.java        # Solver implementation of the driftingDdroids Solver
│       └── SolverStatus.java    # Solver status
├── res/
│   ├── drawable/                # Game graphics
│   └── raw/                     # Raw assets (e.g. audio background)
└── assets/
    └── Maps/                    # Level data
```

## Dependencies

- Android SDK (min API 21)
- Java standard libraries
- No external game engines

## Performance Considerations

### Memory Management
- Texture pooling
- Resource caching
- State management
- Garbage collection optimization

### Rendering
- Sprite batching
- View recycling
- Layout optimization
- Frame rate management

### Storage
- Efficient save formats
- Data compression
- Cache management
- State serialization

## Security

### Save Data
- Local storage only
- No sensitive data
- Basic file validation
- Error recovery

### User Data
- No personal information
- No network access
- No external storage
- No permissions required

## Future Considerations

### Planned Features
- Additional level packs
- New game mechanics
- Performance improvements
- UI/UX enhancements

### Technical Debt
- Code organization
- Documentation updates
- Testing coverage
- Resource optimization

## Testing Strategy

### Unit Tests
- Game logic
- Movement validation
- State management
- Save/load system

### Integration Tests
- Screen transitions
- Game flow
- State persistence
- User interactions

### Manual Testing
- Gameplay testing
- Performance testing
- Device compatibility
- User experience
