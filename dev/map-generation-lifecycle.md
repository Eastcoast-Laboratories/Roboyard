# Map Generation Lifecycle Concept

## Current State (IST)

### Flow when user presses "Play" (MainMenuFragment)
1. Check if autosave (slot 0) exists
2. **If autosave exists** → load it, resume game (same map, same positions, same progress)
3. **If no autosave** → call `gameStateManager.startModernGame()` → `createValidGame()`

### Flow inside `createValidGame()` / `MapGenerator.getGeneratedGameMap()`
1. Check `Preferences.generateNewMapEachTime` and `forceGenerateNewMapOnce` (dice button)
2. **If `generateNewMapEachTime == true` OR `forceNewMap == true`** → generate completely new map (walls + robots + targets)
3. **If `generateNewMapEachTime == false` AND stored walls exist** → reuse stored walls, only regenerate robot/target positions
4. Store walls in `WallStorage` (keyed by board size `WxH`)

### Problems with current approach
- Autosave always loads the FULL game state (map + positions + progress) — even when user just wants to play a new round on the same map
- When `generateNewMapEachTime == false` ("keep map"), pressing Play always resumes the old game from autosave, never generates new positions
- Board size change clears WallStorage but autosave still has old board size
- Loading a savegame with different board size doesn't update preferences
- Difficulty change doesn't trigger new map generation if autosave exists
- Robot count / target count changes don't trigger new map generation if autosave exists

---

## Proposed Concept (SOLL)

### Key Principle
**Autosave = "resume interrupted game". New game = respect current settings.**

### Scenarios

#### 1. First app start ever (no autosave, no stored walls)
- Generate new map with current settings (board size, difficulty, robot count, target count)
- Store walls in WallStorage
- Autosave after generation

#### 2. Press "Play" with existing autosave
- **If no settings changed since autosave** → load autosave, resume game
- **If settings changed** (board size, difficulty, robot count, target count) → discard autosave, start new game with new settings
- Detection: store a "settings hash" in autosave metadata (boardW, boardH, difficulty, robotCount, targetCount)

#### 3. Press "Play" with `generateNewMapEachTime == true`
- Always generate completely new map (walls + robots + targets)
- Autosave is only used to resume if game was interrupted (app killed mid-game)
- If autosave settings hash matches current settings → load autosave (resume)
- If autosave settings hash differs → discard, generate new

#### 4. Press "Play" with `generateNewMapEachTime == false` ("keep map")
- **If autosave exists AND settings match** → load autosave (resume same game)
- **If autosave exists AND settings differ** → discard autosave, generate new map, store walls
- **If no autosave BUT stored walls exist for current board size** → reuse walls, generate new robot/target positions
- **If no autosave AND no stored walls** → generate completely new map, store walls

#### 5. Dice button (force new map)
- Always generate completely new map regardless of settings
- Clear stored walls for current board size
- Works the same whether `generateNewMapEachTime` is true or false

#### 6. Board size (ratio) changed in settings
- Clear stored walls for OLD board size
- Clear autosave (it has wrong board size)
- Next "Play" generates new map with new board size

#### 7. Difficulty changed in settings
- Clear autosave (difficulty affects wall generation)
- Clear stored walls (wall density depends on difficulty)
- Next "Play" generates new map

#### 8. Robot count changed in settings
- **If `generateNewMapEachTime == false`** → keep walls, only regenerate robot/target positions
- Clear autosave (robot positions are different)
- Next "Play" reuses walls but places new robots

#### 9. Target count changed in settings
- Same as robot count: keep walls, regenerate positions
- Clear autosave

#### 10. Load a savegame (from load menu)
- Load the full game state (map + positions + progress)
- **Do NOT change preferences** to match savegame's board size
- If savegame has different board size → game plays with savegame's board size
- Autosave slot is updated with loaded game

#### 11. Game completed → "Next Game"
- **If `generateNewMapEachTime == true`** → generate completely new map
- **If `generateNewMapEachTime == false`** → reuse walls, new robot/target positions
- Delete autosave (game is complete)

#### 12. Reset button during game
- Reset robot positions to initial positions (same map, same starting positions)
- Autosave is updated

---

## Settings Hash

To detect whether settings changed since autosave, store a hash:

```
settingsHash = boardW + "x" + boardH + "_d" + difficulty + "_r" + robotCount + "_t" + targetCount
```

Store this in the autosave file metadata or as a separate SharedPreference key.

---

## Implementation Steps

1. Add `settingsHash` computation to `Preferences`
2. Store `settingsHash` in autosave metadata
3. On "Play": compare current `settingsHash` with autosave's hash
4. On settings change: clear autosave if relevant settings changed
5. Separate "wall generation" from "element placement" more clearly
6. Ensure `WallStorage` is cleared on board size AND difficulty changes
