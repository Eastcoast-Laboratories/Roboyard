# Copilot Prompt: Create iOS Port of Roboyard Android App

## Goal
Create an iOS app that looks and behaves exactly like the Android Roboyard puzzle game. Maximize code reuse (DRY).

## 1. Kotlin Multiplatform (KMP)
- Convert Java to Kotlin (automated) and share logic between Android and iOS.

### Stack
- **Shared Logic**: Kotlin (converted from Java) in shared module
- **UI**: Compose Multiplatform (iOS) + Jetpack Compose (Android)
- **Navigation**: Compose Navigation
- **State**: Kotlin Flow + Compose State
- **Persistence**: Multiplatform Settings
- **Networking**: Ktor
- **Accessibility**: VoiceOver (iOS) + TalkBack (Android)

## What to Port (Shared Module) - Checklist (sorted by size)

### roboyard.logic.core (22 files)
- [x] GameState.java (103K)
- [x] GameLogic.java (60K)
- [x] Preferences.java (47K)
- [x] WallPatternGenerator.java (25K)
- [x] WallStorage.java (14K)
- [x] GameHistoryEntry.java (15K)
- [x] MapGenerator.java (11K)
- [x] GameElement.java (5.4K)
- [x] MapObjects.java (5.9K)
- [x] LevelFormatParser.java (5.1K)
- [x] Constants.java (5.0K)
- [x] GameMove.java (5.0K)
- [x] WallModel.java (4.0K)
- [x] LevelCompletionData.java (2.9K)
- [x] GridElement.java (1.8K)
- [x] Wall.java (1.8K)
- [x] IGameObject.java (681)
- [x] AGameState.java (571)
- [x] GameSolution.java (709)
- [x] Move.java (501)
- [x] WallType.java (143)
- [x] IGameMove.java (92)

### roboyard.logic.solver (10 files)
- [x] RRGetMap.java (29K)
- [x] SolverDD.java (11K)
- [x] GameLevelSolver.java (3.3K)
- [x] RRPiece.java (1.5K)
- [x] RRGameState.java (1.8K)
- [x] RRGameMove.java (1.2K)
- [x] ISolver.java (1.1K)
- [x] SolverStatus.java (781)
- [x] ERRGameMove.java (628)

### driftingdroids (separate package)
- [ ] All DriftingDroids solver files

## What to Rebuild (Platform-Specific)
- **UI**: All screens (MainMenu, GameScreen, LevelSelection, Settings, SaveLoadHistory, LevelEditor, Achievements, Credits)
- **Platform APIs**: File storage, audio, accessibility

## Key Features
- 140 built-in levels (copy from `app/src/main/assets/Maps/`)
- Random map generation (4 difficulty levels)
- AI solver for hints and optimal solutions
- Save/load/history system
- Level editor with 10 generators
- 6 languages (en, de, fr, es, zh, ko)
- Full accessibility (VoiceOver/TalkBack)

## Success Criteria
- All 140 levels playable with identical solutions
- AI solver matches Android performance
- Save/load/history works identically
- Level editor can create/share levels
- Full accessibility support
- Pixel-perfect visual match

## Reference Files
- `/app/src/main/java/roboyard/logic/core/` - Core game logic
- `/app/src/main/java/roboyard/logic/solver/` - AI solver
- `/app/src/main/assets/Maps/` - Level data
- `/docs/ARCHITECTURE.md` - Architecture
