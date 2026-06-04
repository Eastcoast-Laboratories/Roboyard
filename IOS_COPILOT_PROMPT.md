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
- [ ] Preferences.java (47K)
- [ ] WallPatternGenerator.java (25K)
- [ ] WallStorage.java (14K)
- [ ] GameHistoryEntry.java (15K)
- [ ] MapGenerator.java (11K)
- [ ] GameElement.java (5.4K)
- [ ] MapObjects.java (5.9K)
- [ ] LevelFormatParser.java (5.1K)
- [ ] Constants.java (5.0K)
- [ ] GameMove.java (5.0K)
- [ ] WallModel.java (4.0K)
- [ ] LevelCompletionData.java (2.9K)
- [ ] GridElement.java (1.8K)
- [ ] Wall.java (1.8K)
- [ ] IGameObject.java (681)
- [ ] AGameState.java (571)
- [ ] GameSolution.java (709)
- [ ] Move.java (501)
- [ ] WallType.java (143)
- [ ] IGameMove.java (92)

### roboyard.logic.solver (10 files)
- [ ] RRGetMap.java (29K)
- [ ] SolverDD.java (11K)
- [ ] GameLevelSolver.java (3.3K)
- [ ] RRPiece.java (1.5K)
- [ ] RRGameState.java (1.8K)
- [ ] RRGameMove.java (1.2K)
- [ ] ISolver.java (1.1K)
- [ ] SolverStatus.java (781)
- [ ] ERRGameMove.java (628)

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
