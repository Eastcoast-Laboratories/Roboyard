# Dice Button Implementation for Random Game

## Feature Request
When `generateNewMapEachTime == "no"`, add a dice button in the random game screen:
- Position: Left of the map name in the game screen (only in random modenot in level mode)
- Function: Generates a new map once (one-time override of `generateNewMapEachTime`)
- Behavior: Same as "New Game" button, but additionally sets `Preferences.generateNewMapEachTime` to true for only the next game

## Current Implementation Analysis

### Where Maps Are Generated
1. **GameState.createRandom()** - Creates random game state
   - Calls `MapGenerator.getGeneratedGameMap()`
   - Checks `Preferences.generateNewMapEachTime`
   - If false: Uses stored walls from `WallStorage`
   - If true: Generates completely new map

2. **MapGenerator.getGeneratedGameMap()** - Generates map
   - Checks `Preferences.generateNewMapEachTime`
   - If false and walls stored: Preserves walls, regenerates robots/targets
   - If true: Generates completely new map

3. **WallStorage** - Stores wall configuration
   - `hasStoredWalls()` - Check if walls are stored
   - `storeWalls(data)` - Store walls
   - `applyWallsToElements(data)` - Apply stored walls

### How to Implement Dice Button

#### Option A: Add Override Flag (Recommended)
1. Add static flag to `MapGenerator`:
   ```java
   public static boolean forceGenerateNewMapOnce = false;
   ```

2. Modify `getGeneratedGameMap()`:
   ```java
   boolean preserveWalls = !Preferences.generateNewMapEachTime && 
                          !forceGenerateNewMapOnce && 
                          wallStorage.hasStoredWalls();
   
   // Reset the flag after use
   if (forceGenerateNewMapOnce) {
       forceGenerateNewMapOnce = false;
   }
   ```

3. Call from dice button:
   ```java
   MapGenerator.forceGenerateNewMapOnce = true;
   gameStateManager.startModernGame();
   ```


### UI Implementation

#### Where to Add Button
- **ModernGameFragment** - Game screen
- Position: Next to map name (top of game screen)
- Only visible when `Preferences.generateNewMapEachTime == false`

#### Button Properties
- Icon: Dice icon (🎲)
- Size: Small (similar to other control buttons)
- Color: Match game theme (blue/green)
- Text: no Text, just icon (accesibility text: "generate a new map")

#### Implementation Steps

1. **Find ModernGameFragment layout**
   - Locate the map name TextView
   - Add dice button next to it

2. **Add button visibility logic**
   ```java
   diceButton.setVisibility(
       Preferences.generateNewMapEachTime ? View.GONE : View.VISIBLE
   );
   ```

3. **Add click listener**
   ```java
   diceButton.setOnClickListener(v -> {
       MapGenerator.forceGenerateNewMapOnce = true;
       gameStateManager.startModernGame();
       Timber.d("[DICE_BUTTON] New map generated with dice button");
   });
   ```

4. **Update on preference change**
   - Listen to preference changes
   - Update button visibility when `generateNewMapEachTime` changes

### unit tests :
- [ ] Dice button only visible when `generateNewMapEachTime == false`
- [ ] Clicking dice button generates new map (walls change), verified in logs
- [ ] Button doesn't affect subsequent games (only one-time override), verified in logs
- [ ] button not visible in level games
- [ ] No crashes or errors

### Files to Modify
1. `MapGenerator.java` - Add override flag
2. `ModernGameFragment.java` - Add button and logic
3. `fragment_modern_game.xml` (or equivalent) - Add button UI
4. `strings.xml` - Add button label for accessibility in all languages
