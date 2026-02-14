# Concept: Hot/Cold Mode (Heiß-Kalt-Modus)

## Overview
Enhance the existing **Live Move Counter** feature into a full Hot/Cold mode that shows the player
how close they are to the optimal solution — in real-time, move by move.
Independent of the solver/hint system. Persists across games and app restarts.

## What Already Exists

### Live Move Counter (implemented)
- **Toggle button:** `ToggleButton liveMoveCounterToggle` (eye icon) in `ModernGameFragment`
- **State:** `liveMoveCounterEnabled` boolean in `GameStateManager`
- **Solver:** `LiveSolverManager` runs a separate solver instance for current position
- **Display:** Shows `"%d moves from here"` in `statusTextView` (dark green)
- **Blink animation:** Eye icon blinks while solver calculates
- **LiveData:** `liveMoveCounterText` and `liveSolverCalculating` observables
- **Trigger:** `triggerLiveSolver()` called after each move when enabled
- **String:** `live_move_counter_optimal` = `"%1$d moves from here"`
- **Optimal moves** are already shown in the big ***Optimal moves button***

### What's NOT Yet Implemented
- Toggle state is **not persisted** (resets on Back / new game)
- Only shows remaining moves (R), not deviation (Δ)
- No background pre-computation of next possible moves

## Problems to Fix
1. Toggle resets when navigating away — needs SharedPreferences persistence
2. Display text (`"X moves from here"`) — needs to add the delta Y like: `X moves from here (Δ+Y)`, so player can see if they're on track

## The 4 Key Values

| Value | Meaning | Source |
|-------|---------|--------|
| **Optimal** | Minimum moves from start position | Main solver (already computed) |
| **Moves** | Player's moves so far | `moveCount` LiveData |
| **Remaining** | Optimal moves from current position | `LiveSolverManager` (already computed) |
| **Deviation (Δ)** | How far off optimal: (Moves + Remaining) - Optimal | Calculated |

## Display Format

Short, word-based, but compact. Three lines or one line depending on space:

**One-line format (in statusTextView):**
```
Optimal: 6 · Moves: 3 · Remaining: 5 · +2
```

**Compact alternative:**
```
Optimal 6 | Moves 3 | Left 5 | +2 over
```

**Minimal (if space is tight):**
```
Opt 6 | You 3 | Left 5 | +2
```

The **deviation** (+2) should be color-coded:
- **Green (+0):** On an optimal path
- **Yellow (+1 to +2):** Slightly off
- **Red (+3+):** Significantly off

While solver is calculating remaining:
- Show the solver's search depth counting up in matte/blinking style
- e.g. `Left ⏳4...` → `Left ⏳5...` → `Left 6` (solid when done)

## Persistence

### Toggle State
- Save to SharedPreferences: `live_move_counter_enabled` (boolean)
- Restore in `ModernGameFragment.onViewCreated()` from SharedPreferences
- Update SharedPreferences in `setLiveMoveCounterEnabled()`
- Survives: Back button, new game, app restart, fragment recreation

## Background Pre-computation (Future Enhancement)

After each player move, solve all possible next states in background:
- 4 robots × 4 directions = max 16 possible next states
- Cache results in `HashMap<String, Integer>` (state hash → optimal moves)
- When player makes their next move, look up cached result → instant display
- Clear cache on new game / reset

## Relationship to Hint Mode

| Feature | Hot/Cold Mode | Hint Mode |
|---------|--------------|-----------|
| Shows direction | Indirectly (deviation goes up/down) | Directly (shows next move) |
| Spoiler level | Low (just numbers) | High (shows exact solution) |
| Learning effect | High (player discovers path) | Low (player follows instructions) |
| Requires thinking | Yes | No |

Keep both. Hot/Cold is the "soft" helper, Hints is the "hard" helper.
In a way, Hot/Cold could make hints less necessary — but not fully replace them.

### Hot/Cold Color Feedback
- **Green (Δ=0):** Player is on an optimal path
- **Yellow (Δ=1-2):** Slightly off optimal
- **Red (Δ=3+):** Significantly off optimal
- **Pulsing green:** Move just improved Δ (getting warmer)
- **Pulsing red:** Move just worsened Δ (getting colder)

## Implementation Phases

### Phase 1: Persist Toggle + Show All 4 Values
- Store toggle in SharedPreferences (`live_move_counter_enabled`)
- Restore on fragment creation
- Display: `Optimal X | Moves Y | Left Z | +N`
- Use existing `lastSolutionMinMoves` for Optimal value
- Use existing `LiveSolverManager` result for Remaining value
- Calculate Deviation = (Moves + Remaining) - Optimal
- Color-code the deviation value

### Phase 3: Background Pre-computation
- After each move, solve all 16 possible next positions
- Cache results for instant Remaining display
- Use thread pool for parallel solving

### Phase 4: Hot/Cold Pulse Animation
- Track previous Δ value
- Pulse green when Δ decreases (warmer)
- Pulse red when Δ increases (colder)

## Technical Notes
- `LiveSolverManager` already handles async solving — extend for batch
- `GameStateManager.lastSolutionMinMoves` already stores optimal from start
- `GameStateManager.triggerLiveSolver()` already builds GridElements from current state
- SharedPreferences key: `live_move_counter_enabled` (consistent with existing naming)
- Trail undo already implemented via `pathHistory` + `undoLastPathSegment()`
