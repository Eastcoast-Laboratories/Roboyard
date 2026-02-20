# Concept: Hot/Cold Mode (Heiß-Kalt-Modus)

## Overview
Enhance the existing **Live Move Counter** feature into a full Hot/Cold mode that shows the player
how close they are to the optimal solution — in real-time, move by move.
Independent of the solver/hint system. Persists across games and app restarts.

## What Already Exists

### Live Move Counter (implemented)
- **Toggle button:** `ToggleButton liveMoveCounterToggle` (eye icon) in `GameFragment`
- **State:** `liveMoveCounterEnabled` boolean in `GameStateManager`
- **Solver:** `LiveSolverManager` runs a separate solver instance for current position
- **Display:** Shows `"%d moves from here"` in `statusTextView` (dark green)
- **Blink animation:** live-move-toggle icon blinks while solver calculates
- **LiveData:** `liveMoveCounterText` and `liveSolverCalculating` observables
- **Trigger:** `triggerLiveSolver()` called after each move when enabled
- **String:** `live_move_counter_optimal` = `"%1$d moves from here"`
- **Optimal moves** are already shown in the big ***Optimal moves button***

## Display Format - fixed [x]

keep it like it is
### Toggle State
- Save to SharedPreferences: `live_move_counter_enabled` (boolean)
- Restore in `GameFragment.onViewCreated()` from SharedPreferences
- Update SharedPreferences in `setLiveMoveCounterEnabled()`
- Survives: Back button, new game, app restart, fragment recreation

### What's NOT Yet Implemented
- ~~No background pre-computation of next possible moves~~ → **done**
- ~~color coding~~ → **done**

## The 4 Key Values

| Value | Meaning | Source |
|-------|---------|--------|
| **Optimal** | Minimum moves from start position | Main solver (already computed) |
| **Moves** | Player's moves so far | `moveCount` LiveData |
| **Remaining** | Optimal moves from current position | `LiveSolverManager` (already computed) |
| **Deviation (Δ)** | How far off optimal: (Moves + Remaining) - Optimal | Calculated |


## Color Coding

The **deviation** (+2) should be color-coded:
- **Green (+0):** On an optimal path
- **Yellow (+1 to +2):** Slightly off
- **Red (+3+):** Significantly off

## Background Pre-computation [implemented]

After each player move, solve all possible next states in background:
- 4 robots × 4 directions = max 16 possible next states
- Cache results in `ConcurrentHashMap<String, Integer>` (state hash → optimal moves)
- When player makes their next move, look up cached result → instant display
- Clear cache on new game / reset

### Execution Rules
- **Sequential only:** All pre-computations run on a `SingleThreadExecutor` — never two solvers in parallel
- **Cancel on move:** When the player moves a robot, all pending pre-computations are cancelled immediately via `preComputeCancelled` flag
- **Cache lookup first:** On each move, check if the result was already pre-computed; if yes, use it instantly; if not, run the live solver normally
- **Pre-compute after solve:** Only after the current position is solved (either from cache or live solver), start pre-computing the next possible positions
- **Logging:** All pre-computation events use the `[PRECOMP_SOLUTION]` tag:
  - `[PRECOMP_SOLUTION] Starting sequential pre-computation for N robots × 4 directions`
  - `[PRECOMP_SOLUTION] Solving: robot X dir → (x,y)...`
  - `[PRECOMP_SOLUTION] Solved: robot X dir → (x,y) = N moves`
  - `[PRECOMP_SOLUTION] Finished: N computed, N skipped, cache size: N`
  - `[PRECOMP_SOLUTION] Cache HIT for state ...`
  - `[PRECOMP_SOLUTION] Cache MISS — no pre-computation available for state ...`
  - `[PRECOMP_SOLUTION] Used pre-computed result: ...`
  - `[PRECOMP_SOLUTION] Cancelled after N computed, N skipped`
  - `[PRECOMP_SOLUTION] Cancellation requested`
  - `[PRECOMP_SOLUTION] Cache cleared`

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

### Phase 3: Background Pre-computation [implemented]
- After each move, solve all 16 possible next positions sequentially (one at a time)
- Cache results for instant Remaining display
- Cancel all pending pre-computations when player moves a robot
- See "Execution Rules" above for details

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
