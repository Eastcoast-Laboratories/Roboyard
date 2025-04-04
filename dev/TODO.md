# TODO
- show a big 90% transparent blue robot in the background of the whole app
- walls on menu are not tiled 
- add sound effects for buttons
- add sound effect background for game


# Difficulty
- Beginner
  - show any puzzles with solutions with at least 4-6 moves (max 6 not always works )
  - goals are always in corners with two walls
- Advanced
  - solutions with at least 6-8 moves
  - keep initial playing field when starting the next game
  - keep playing field when loading a saved game
  - three lines allowed in the same row/column
  - no multi-color target
- Insane mode
  - five lines allowed in the same row/column
- Impossible mode
  - five lines allowed in the same row/column


# Accessibility-Verbesserungen
- TalkBack-Unterstützung sprache einstellbar machen (Englisch ermöglichen).  
1. Add a preference setting to customize the order of information in TalkBack announcements
2. Create custom button sounds to provide audio feedback for different actions
3. Add a high-contrast mode for better visibility
4. get rid of the accessibility texts of the walls and the logo in the menu


# Sonstige Features & Fixes
- Rückgängig-Funktion ("Back"-Methode) reparieren:  
  - im Moment Geht nur so viele Schritte zurück, wieviel verschiedene Roboter bereits bewegt wurden, also pro roboter auswahl geht ein back schritt verloren im moment.  


# achievements
- add achievement system

# levels 
- the back button at the bottom does not work
- add a "retry" button to the level game screen

# current
- winning a level: this overwrites the old saved stars and data, so you can "loose" stars this way if you play a level again with less stars

- auto save to history after 60 seconds (2s for now) seems to work, but nothing shown in the histroy tab

- new map each time is not working. if set, there is generated a new map anyway, all the walls should stay the same, just new robot and target positions on "new game"

- @ModernGameFragment.java#L622-624 hintbutton.setenabled(false) hat keinen effect


- for debugging: add a skip button to levels, which will just move the robot to its target in one move
- multi-color target is gone

- Beginner
  - show any puzzles with solutions with at least 4-6 moves (max 6 not always works )

- when loading a map, the mapname is not shown
- when saving a game and directly load it again, then save again and the next time it loads, it has no target. strangely, the solver still works, as if the target were there where it was before saving.

- the same: if oyu start the app and directly load the auto save game, it has no target

- In den Leveln, wenn man die schon mal gespielt hat, dann die optimale Zahl gleich anzeigen

- Die Maps die verworfen wurden zählen beim generieren, also einen counter bei "AI calculating silution..."

- im start menu schon die Mini-Maps cachen

- Beim HintText einen kleinen Pfeil links für zurück

- Wenn Map ration sehr gross, dann die Buttons unten alle kleiner und nur mit Icons statt text und alle in eine Reihe anstatt 2

- optimale Zahl  Margin rechts grösser

## Implemented Features

### Wall Preservation Across Game Changes

Implemented a feature that preserves wall configurations when starting a new game or resetting robots. This feature works with the existing `generateNewMapEachTime` preference in the settings.

**Implementation Details:**

1. Created a new `WallStorage` class in the `roboyard.logic.core` package to manage wall configurations:
   - Provides methods to store, retrieve, and apply wall configurations
   - Uses a singleton pattern for global access

2. Modified `GameLogic.generateGameMap()` to check the `generateNewMapEachTime` preference:
   - If `generateNewMapEachTime` is false and walls are stored, preserves the walls and only regenerates robots and targets
   - If `generateNewMapEachTime` is true or no walls are stored, generates a completely new map

3. Updated `MapGenerator.getGeneratedGameMap()` to integrate with the wall storage:
   - Preserves walls when `generateNewMapEachTime` is false
   - Stores walls for future use when generating a new map

4. Enhanced `GameStateManager` to store walls when resetting robots or creating a new game:
   - Modified `resetRobots()` to store the current walls before resetting
   - Modified `createValidGame()` to store walls before creating a new game state

**Testing:**

- Verified that walls are preserved when resetting robots with `generateNewMapEachTime` set to false
- Confirmed that a new map is generated when `generateNewMapEachTime` is set to true
- Tested that the feature works correctly with different board sizes and difficulty levels

## Concept: Preserving Walls Across Game Changes

### Overview
Implement a feature to preserve the wall configuration when starting a new game or resetting robots, based on a setting in the preferences.

### Implementation Details

1. **Add a Preference Setting**
   - Add a new boolean preference in `Preferences.java`: `KEY_PRESERVE_WALLS`
   - Default value: `false` (for backward compatibility)
   - Add UI toggle in settings screen

2. **Wall Storage Mechanism**
   - Create a static `WallStorage` class to store the current wall configuration
   - Methods:
     - `storeWalls(GameState state)`: Extract and store only wall elements
     - `hasStoredWalls()`: Check if walls are stored
     - `applyWallsToNewState(GameState state)`: Apply stored walls to a new game state
     - `clearStoredWalls()`: Clear the stored walls

3. **Modify Game State Creation**
   - Update `GameStateManager.createValidGame()` to check if walls should be preserved
   - If `Preferences.preserveWalls` is true and `WallStorage.hasStoredWalls()` is true:
     - Create a new game state with random robots and targets
     - Apply the stored walls using `WallStorage.applyWallsToNewState()`
   - Otherwise, create a completely new random game state

4. **Integration Points**
   - When `startNewGame()` or `startModernGame()` is called, store walls from current state if available
   - When `resetRobots()` is called, preserve the wall configuration
   - Add a button in the UI to manually reset the wall storage

5. **Solver Integration**
   - Ensure the solver is properly reinitialized with the new game state after applying stored walls
   - Validate that the new game state with preserved walls is solvable

6. **Edge Cases to Handle**
   - First game launch (no stored walls)
   - Changing board size (invalidate stored walls if dimensions change)
   - Invalid wall configurations (fallback to random generation)
   - Ensure difficulty validation still works with preserved walls

### Code Changes Required

1. Add new preference in `Preferences.java`
2. Create new `WallStorage.java` class
3. Modify `GameStateManager.createValidGame()` method
4. Update `GameStateManager.resetRobots()` method
5. Add UI toggle in settings screen
6. Add wall reset functionality in game UI

### Testing Strategy

1. Verify walls are preserved when starting a new game with the setting enabled
2. Verify robot positions are reset correctly while walls remain unchanged
3. Test with different board sizes and difficulty levels
4. Ensure solver still works correctly with preserved walls
5. Verify disabling the setting returns to normal random generation

## Improved Play Store Description

### deutsch
Roboyard - Roboter-Puzzle-Herausforderung

Inspiriert vom klassischen Brettspiel Ricochet Robots, fordert Roboyard dein logisches Denken heraus! Steuere Roboter durch ein Labyrinth voller Hindernisse und finde den optimalen Weg zum Ziel.
🕹️ SPIELPRINZIP:

🔹 Roboter bewegen sich nur in geraden Linien und stoppen erst an Hindernissen oder anderen Robotern.
🔹 Plane strategisch und finde den kürzesten Weg zum Ziel!
🔹 Kannst du jede Herausforderung mit der optimalen Lösung meistern?
🚀 FEATURES:

✅ Einzigartiger KI-Lösungsalgorithmus – Lass dir Hinweise geben oder sieh die beste Lösung!
✅ Verschiedene Schwierigkeitsstufen – Von entspannt bis knifflig.
✅ Spielfelder speichern & teilen – Teile deine eigenen Herausforderungen mit Freunden!
✅ Trainiere dein logisches Denken & räumliches Vorstellungsvermögen.
✅ Vollständige Barrierefreiheit – Unterstützt TalkBack und den Android Accessibility Mode für blinde Spieler.
🎯 PERFEKT FÜR:

✔️ Puzzle-Fans und Logikrätsel-Liebhaber
✔️ Fans von Ricochet Robots und ähnlichen Denkspielen
✔️ Alle, die ihr Gehirn herausfordern wollen – egal ob für kurze oder lange Sessions

🔹 Komplett kostenlos, werbefrei & Open Source!
🔹 Entwickle deine Problemlösungsfähigkeiten und habe Spaß dabei!

🖥️ Quellcode & weitere Infos: https://roboyard.z11.de/

### english

Roboyard – The Ultimate Robot Puzzle Challenge!

Inspired by the classic board game Ricochet Robots, Roboyard challenges your logical thinking! Guide robots through a maze filled with obstacles and find the optimal path to the goal.

🕹️ GAMEPLAY:

🔹 Robots move in straight lines and stop only when hitting a hedge or another robot.
🔹 Plan ahead and find the shortest path to the target!
🔹 Can you master each challenge with the perfect solution?

🚀 FEATURES:

✅ Unique AI-powered solution algorithm – Get hints or watch the optimal solution!
✅ Multiple difficulty levels – From beginner-friendly to mind-bending.
✅ Save & share custom game boards – Challenge your friends!
✅ Sharpen your logical thinking & spatial awareness.
✅ Fully accessible for blind players – Includes TalkBack and Android Accessibility Mode support.

🎯 PERFECT FOR:

✔️ Puzzle enthusiasts and logic lovers
✔️ Fans of Ricochet Robots and similar brain teasers
✔️ Anyone looking for a fun mental challenge – whether for quick sessions or deep thinking

🔹 Completely free, ad-free & open source!
🔹 Train your problem-solving skills while having fun!

🖥️ Source code & more info: https://roboyard.z11.de/