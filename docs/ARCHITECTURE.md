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
