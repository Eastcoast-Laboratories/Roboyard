# GameScreen Functionality Restoration Plan

## Objective
Restore complete game functionality in the Compose Multiplatform GameScreen to match the fragment-app branch behavior.

## Current Status
- **Visuals**: Pixel-perfect match with fragment-app (completed)
- **Functionality**: Partially working - robots can be moved via drag, but core game logic is missing

## Completed Tasks
- [x] Win-Detection: Recognize when goal robot reaches target + Feedback/Win overlay
- [x] squaresMoved counter updates on every move
- [x] New-Game button wired (onNewGame callback to App)
- [x] Win overlay with "Solved!" message and Menu/New Game buttons

## Pending Tasks
- [ ] Random-Game-Erzeugung an fragment-app angleichen (Boardgroesse/Preferences/GameLogic)
- [ ] Level-Laden verifizieren (Ziele, Roboter, Win korrekt)

## Detailed Implementation Plan

### 1. Random-Game-Erzeugung (Priority: High)
**Current State**: Uses `Board.createBoardRandom(4)` (16x16 standard quadrants, 4 robots)
**Target State**: Use GameLogic.generateGameMap with Preferences (boardSizeWidth/Height, robotCount, targetColors)

**Steps**:
1. Initialize Preferences in Compose App entry point (Main.kt)
2. Create GameLogic instance with current Preferences
3. Replace `Board.createBoardRandom(4)` with GameLogic-based generation
4. Convert GridElement list to Board (use Board.createBoardFreestyle + setWall/addGoal/setRobots)
5. Test random game generation with different board sizes

**Files to Modify**:
- `composeApp/desktopMain/kotlin/roboyard/Main.kt` - Add Preferences initialization
- `composeApp/src/commonMain/kotlin/roboyard/ui/compose/App.kt` - Replace random game generation
- `shared/src/commonMain/kotlin/roboyard/logic/core/` - Verify GameLogic integration

### 2. Level-Laden verifizieren (Priority: Medium)
**Current State**: Uses LevelLoader.loadLevel(levelId) which parses level files
**Target State**: Verify that levels load correctly with goals, robots, and win detection works

**Steps**:
1. Test level loading with a few sample levels (e.g., level 1, level 140)
2. Verify goals are set correctly (goal.robotNumber matches robot index)
3. Verify robot positions match level file
4. Test win detection on loaded levels
5. Verify LevelLoader parses color chars correctly (p/g/b/y/s to robot indices)

**Files to Verify**:
- `shared/src/commonMain/kotlin/roboyard/logic/core/LevelLoader.kt`
- `shared/src/commonMain/resources/Maps/level_*.txt`

## Code Changes Made So Far

### GameScreen.kt
- Added `onNewGame` callback parameter
- Added `gameWon` state variable
- Wrapped content in Box for win overlay
- Updated `onRobotMove` to calculate `squaresMoved` distance
- Added win check via `isSolved(newBoard)` after each move
- Added win overlay with "Solved!" message and Menu/New Game buttons
- Added `isSolved(board)` helper function
- Wired "New Game" button to `onNewGame` callback

### App.kt
- Updated GameScreen call to include `onNewGame` callback
- `onNewGame` creates new random board with `Board.createBoardRandom(4)`

## Next Steps
1. Implement Preferences initialization in Main.kt
2. Replace random game generation with GameLogic-based approach
3. Test level loading and win detection
4. Verify all game mechanics work correctly
