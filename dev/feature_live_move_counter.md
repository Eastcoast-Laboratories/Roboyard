# Feature: Live Move Counter (Remaining Moves from Current Position)

## Overview
A toggleable feature in the hint box that shows, after each player move, how many optimal moves remain from the **current** robot positions. This requires running a new solver instance after each move with the current board state.

## User Experience
1. Player opens the hint box (toggle button ON)
2. A new toggle/checkbox appears inside the current hint box with only accessibiliity text: **"Show remaining moves"** (default: OFF) appearence is an eye, if on open if off, closed
   1. - the toggle only appears in the hint box, if the last pre-hint is shown where it sais "The A.I. found a solution in X moves" (pre_hint_exact_solution) 
   2. - the toggle is hidden in all other hint cases
3. When enabled, after each player move:
   - A background solver runs with the **current** robot positions
   - The hint box displays: `"X moves from here"` (or localized equivalent)
   - While calculating: the toggle is blinking, no additional accessibiliity messages here 
4. All other hint functionality (pre-hints, step-by-step hints, hint navigation) continues to work based on the **original** solution from the starting position
5. The Reset button still resets to the original positions, and hints still reference the original solution

## Architecture

### Key Principle: Two Separate Solutions
- **`currentSolution`** (existing): Solution from the **original** starting position. Used for all hint display, hint navigation, pre-hints, and reset. **Never overwritten** by the live solver.
- **`liveSolution`** (new): Solution from the **current** robot positions. Only used to display the remaining move count. Recalculated after each player move when the feature is enabled.

### Data Flow
```
Player makes a move
  → if liveMoveCounterEnabled:
      → Create a snapshot of current robot positions + walls + target
      → Initialize a NEW SolverManager instance (or reuse with new state)
      → Run solver async on background thread
      → On completion: display "Optimal: X moves from here" in hint box
      → On failure/timeout: display "No solution found" or hide
  → Normal hint checking (checkIfMoveMatchesHint) continues as before
```

### Implementation Plan

#### 1. GameStateManager Changes
- Add `private GameSolution liveSolution;` field
- Add `private boolean liveMoveCounterEnabled = false;` field
- Add `setLiveMoveCounterEnabled(boolean)` / `isLiveMoveCounterEnabled()` methods
- Add `private MutableLiveData<String> liveMoveCounterText = new MutableLiveData<>("")`
- Add `getLiveMoveCounterText()` LiveData getter for UI observation
- After each move (in the move handling code), if enabled:
  - Create GridElements from current state
  - Run a **separate** solver instance (not the main one)
  - On completion: update `liveMoveCounterText` with result
  - Important: Do NOT touch `currentSolution`

#### 2. Solver Considerations
- The existing `SolverManager` is a singleton. For the live counter, we need either:
  - **Option A**: A second `SolverManager` instance dedicated to live solving
  - **Option B**: Queue the live solve after the main solve completes, and cancel any pending live solve when a new move is made
- **Recommended: Option A** — Create a `LiveSolverManager` or allow `SolverManager` to be instantiated (not singleton) for this purpose
- The live solver should have a **timeout** (e.g., 3-5 seconds) since we don't need it to find the absolute optimal solution for very complex boards
- Cancel any running live solver when:
  - Player makes another move (new solve needed)
  - Player resets the board
  - Player disables the feature
  - Player leaves the game

#### 3. ModernGameFragment UI Changes
- Add a `ToggleButton` in the hint container area as an eye with the accessibility text: "Show remaining moves"
- Observe `liveMoveCounterText` LiveData
- When text is non-empty and feature is enabled, display it instead of the current hint text
- The live counter text should be in a dark-green font-color to distinct it from the main hint text
- Persist the toggle state in SharedPreferences so it remembers the user's preference

#### 4. String Resources
Add to `strings.xml`:
```xml
<string name="live_move_counter_label_a11y">Show remaining moves</string>
<string name="live_move_counter_optimal">Optimal: %d moves from here</string>
```

### Edge Cases
- **No solution possible**: blink permanently)
- **Solver timeout**: Show "?" or "Calculating..." with a spinner
- **Rapid moves**: Cancel previous live solver before starting new one
- **Reset button**: Clear live counter, don't re-solve until next move
- **Game completion**: Hide the live counter

### Performance
- The solver is already designed to run async on a background thread
- Cancel-on-new-move prevents accumulation of solver threads
- Timeout prevents hanging on impossible/very-hard positions
- The live solver only needs the move count, not the full move sequence (optimization opportunity)

### Files to Modify
1. **`GameStateManager.java`** — Add liveSolution field, LiveData, enable/disable, trigger after moves
2. **`SolverManager.java`** — Either make non-singleton or create a LiveSolverManager
3. **`ModernGameFragment.java`** — Add UI toggle, observe LiveData, display counter
4. **`fragment_modern_game.xml`** — Add toggle button in hint area
5. **`strings.xml`** — Add new string resources
6. **`SharedPreferences`** — Persist toggle state
