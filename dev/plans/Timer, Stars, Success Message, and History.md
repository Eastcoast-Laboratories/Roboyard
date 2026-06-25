# Timer, Stars, Success Message, and History Info Button Implementation Plan

Implement timer functionality matching Main Game, correct star calculation, success message with retry button, and add i-button to history screen for detailed entry information.

## Current State Analysis

### Main Game Timer Implementation (GameFragment.java)
- `timerTextView` displays elapsed time in mm:ss or hh:mm:ss format
- `timerRunnable` updates every 500ms using Handler
- `startTimer()`, `stopTimer()`, `resetAndStartTimer()` methods
- `elapsedTimeBeforePause` for resume after pause
- GameStateManager has `startGameTimer()`, `updateGameTimer()`, `resetGameTimer()`
- Timer integrates with history via `totalPlayTime` (saved in GameHistoryEntry)

### Main Game Star Calculation (StarRating.kt, GameHistoryEntry.kt)
- `calculateStars(playerMoves, optimalMoves, hintsUsed)`:
  - 3 stars: playerMoves == optimalMoves
  - 2 stars: playerMoves <= optimalMoves + 1
  - 1 star: level completed (any moves)
- `GameHistoryEntry.recordCompletion(time, moves, stars)`:
  - Stores stars in `completionStars` list
  - Updates `starsEarned` if new stars are better
- For beginner levels (1-10): always earn at least 1 star

### Main Game Hint Tracking and No-Hints Achievement Logic (GameHistoryEntry.kt)
- **Permanent Hint Tracking**:
  - `everUsedHints`: Boolean flag, permanently true if hints were EVER used in ANY session
  - `recordHintUsed(hint)`: Sets `everUsedHints = true` and tracks `maxHintUsed`
  - `markEverUsedHints()`: Explicitly sets `everUsedHints = true`
  - Once `everUsedHints` is true, it CANNOT be reset to false
- **No-Hints Achievement Logic**:
  - `solvedWithoutHints`: Boolean flag, tracks if current session was solved without hints
  - `recordSolvedWithoutHints(optimal)`: Sets `solvedWithoutHints = true` and records timestamp
  - `qualifiesForNoHintsAchievement()`: Returns `solvedWithoutHints` AND NOT `everUsedHints`
  - **CRITICAL**: If `everUsedHints` is true, `qualifiesForNoHintsAchievement()` ALWAYS returns false
  - Even if a later session is solved without hints, the achievement is permanently disqualified
- **Perfect No-Hints Achievement**:
  - `qualifiesForPerfectNoHintsAchievement()`: Requires `solvedWithoutHints` AND `movesMade <= optimalMoves` AND NOT `everUsedHints`
  - Same permanent disqualification rule applies
- **Implementation Rule**:
  - When hint is shown: Call `recordHintUsed()` → `everUsedHints = true` (permanent)
  - When game completes without hints: Call `recordSolvedWithoutHints()` → `solvedWithoutHints = true`
  - Achievement check: `qualifiesForNoHintsAchievement()` returns true ONLY if no hints were EVER used in ANY session

### Main Game Success Message (GameFragment.java)
- Level completion: Shows "Level Complete" with stars (★ ★ ★)
- Random game completion: Shows "Random Game Complete" with moves
- Perfect solution: Shows "Perfect Solution" message
- Shows Next Level button (level games) or New Random Game button (random games)
- **NO retry button for current game** (needs to be added)

### Main Game i-Button Implementation (SaveGameFragment.java, BaseGameFragment.java)
- `infoButton` in HistoryViewHolder shows popup with detailed info
- `showMapInfoPopup(entry.getHistoryEntry())` displays dialog
- `buildMapInfoPopupMessage()` shows:
  - completionCount
  - firstStarted (timestamp)
  - lastPlayed (lastCompletionTimestamp)
  - all completions with timestamps, stars, moves
  - bestTime
  - bestMoves
  - optimalMoves
  - hint tracking info

### ComposeApp Current State
- GameScreen has timer variables (`elapsedTime`, `timerRunning`) but displays hardcoded "00:00"
- Timer runs every second via LaunchedEffect
- Timer NOT integrated with history saving
- **Star calculation NOT implemented** (hardcoded or missing)
- **Success message NOT implemented** (no completion dialog)
- SaveLoadScreen does not exist (needs to be created)
- No i-button or history info popup

## Implementation Plan

### 1. Fix Timer Display in GameScreen
- Replace hardcoded "00:00" with formatted `elapsedTime`
- Format time as mm:ss (< 100 minutes) or hh:mm:ss (>= 100 minutes)
- Match Main Game format exactly

### 2. Integrate Timer with History Saving
- Use `totalPlayTime` in `saveToHistory()` function
- Ensure `totalPlayTime` is calculated from `elapsedTime` or `gameStartTime`
- Match Main Game logic: `totalPlayTime = (System.currentTimeMillis() - gameStartTime) / 1000`

### 3. Implement Star Calculation in GameScreen
- Use `calculateStars(playerMoves, optimalMoves, hintsUsed)` from StarRating.kt
- Calculate stars on game completion
- Store stars in GameHistoryEntry via `recordCompletion(time, moves, stars)`
- For beginner levels (1-10): always earn at least 1 star
- Display stars in success message and history info popup

### 4. Implement Success Message with Retry Button
- Create completion dialog showing:
  - "Level Complete" with stars (★ ★ ★) for level games
  - "Random Game Complete" with moves for random games
  - "Perfect Solution" message if moves == optimalMoves
- Add buttons:
  - "Next Level" (level games) or "New Random Game" (random games)
  - **"Retry" button to reset current game and try again**
- Match Main Game layout and styling

### 5. Create SaveLoadScreen
- Create new Compose screen for Save/Load/History
- Implement tab system (Save slots, History entries)
- Display history entries with minimap preview
- Add delete button for history entries
- Add load button to replay history entries

### 6. Implement i-Button in SaveLoadScreen
- Add info button to each history entry
- Implement `showMapInfoPopup()` function
- Implement `buildMapInfoPopupMessage()` function
- Display all information matching Main Game:
  - Completions count
  - First started timestamp
  - Last played timestamp
  - All completions list with stars and moves
  - Best time
  - Best moves
  - Optimal moves
  - Hint tracking info

### 7. Navigation Integration
- Add SaveLoadScreen to navigation graph
- Add button in GameScreen to navigate to SaveLoadScreen
- Handle navigation back to game after loading history entry

## Files to Modify/Create

### Modify
- `composeApp/src/commonMain/kotlin/roboyard/ui/compose/GameScreen.kt`
  - Fix timer display in GameInfoCard
  - Integrate timer with history saving
  - Implement star calculation on completion
  - Implement success message with retry button

### Create
- `composeApp/src/commonMain/kotlin/roboyard/ui/compose/SaveLoadScreen.kt`
  - New screen for Save/Load/History
  - History entry list with i-button
  - Info popup dialog

## Testing
- Test timer display matches Main Game format
- Test timer counts correctly and stops on game completion
- Test timer is saved to history correctly
- Test star calculation matches Main Game logic
- Test stars are displayed in success message
- Test stars are displayed in history info popup
- Test retry button resets current game correctly
- Test i-button shows all information matching Main Game
- Test loading history entries from SaveLoadScreen
