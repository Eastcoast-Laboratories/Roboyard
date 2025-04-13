# Game History Feature Concept

This document outlines a system to automatically save games that have been played for at least a minute to a history section. This will replace the current autosave functionality with a more comprehensive history system.

## Conceptual Overview

1. **Game History Storage**: Create a dedicated directory for history games, separate from manual saves (done)
2. **Automatic Saving**: Track gameplay time and automatically save games after 1 minute of play (done)
3. **History UI**: Add a new "History" tab or section in the save game screen (done)
4. **Metadata**: Store additional metadata with each history entry (date/time, duration, moves made) (done)
5. **Management**: Allow users to delete history entries, share them via link or promote them to permanent saves (done)

## Implementation Details

### 1. Data Structure

Create a `GameHistoryEntry` class to store history information: (done)

```java
public class GameHistoryEntry {
    private String mapPath;          // Path to the saved game file
    private long timestamp;          // When the game was saved
    private int playDuration;        // In seconds
    private int movesMade;           // Number of moves made
    private String boardSize;        // Board dimensions
    private String previewImagePath; // Path to a thumbnail image
    
    // Constructor, getters, setters
}
```

### 2. Storage Implementation

1. Create a dedicated directory for history entries (`history_maps/`) (done)
2. Use a JSON file (`history_index.json`) to store the list of history entries and their metadata (done)
3. Save the actual game state in individual files using the same format as current save games (done)
4. Use the same string concept for map data as in the existing save games (done)
5. Create mini preview images for each history entry with createMiniMap() (done)

Example history index file structure:
```json
{
  "historyEntries": [
    {
      "mapPath": "history_0.txt",
      "name": "uniquestring from gamemanager",
      "timestamp": 1709366400000,
      "playDuration": 180,
      "movesMade": 12,
      "num_moves": 8,
      "boardSize": "16x16",
      "previewImagePath": "history_0_preview.png"
    },
    {
      "mapPath": "history_1.txt",
      "name": "uniquestring from gamemanager",
      "timestamp": 1709366700000,
      "playDuration": 240,
      "movesMade": 18,
      "num_moves": 10,
      "boardSize": "18x22",
      "previewImagePath": "history_1_preview.png"
    }
  ]
}
```

### 3. Time Tracking in GridGameScreen

Add to the `GridGameScreen` class: (done)

```java
private long gameStartTime;
private int totalPlayTime = 0;
private boolean isHistorySaved = false;
private static final int HISTORY_SAVE_THRESHOLD = 600; // 1 minute in seconds (threshhold for saving to history)

// Call this when game starts or loads
private void startGameTimer() {
    gameStartTime = System.currentTimeMillis();
    isHistorySaved = false;
}

// Call this in the update loop
private void updateGameTimer() {
    if (isGameActive && !isHistorySaved) {
        int elapsedSeconds = (int)((System.currentTimeMillis() - gameStartTime) / 1000);
        totalPlayTime = elapsedSeconds;
        
        // Save to history after 1 minute of play
        if (totalPlayTime >= HISTORY_SAVE_THRESHOLD) {
            saveToHistory();
            isHistorySaved = true;
        }
    }
}

// Save the current game state to history
private void saveToHistory() {
    try {
        // Get next available history index
        int historyIndex = getNextHistoryIndex();
        String historyPath = "history_" + historyIndex + ".txt";
        
        // Build save data using the same format as regular saves
        StringBuilder saveData = new StringBuilder();
        
        // Add board name
        saveData.append("name:").append(gameScreen.mapName).append(";");
        
        // Add timestamp
        saveData.append("timestamp:").append(System.currentTimeMillis()).append(";");
        
        // Add play duration
        saveData.append("duration:").append(totalPlayTime).append(";");
        
        // Add number of moves the player made
        saveData.append("moves:").append(nbCoups).append(";");
        
        // Add number of optimal moves if available
        int numMoves = gameScreen.solutionMoves;
        if (numMoves > 0) {
            saveData.append("num_moves:").append(numMoves).append(";");
        }
        // Add board size
        saveData.append("board:").append(MainActivity.getBoardWidth()).append(",")
               .append(MainActivity.getBoardHeight()).append(";");
        
        // Add the grid elements data using the existing method
        saveData.append(MapObjects.createStringFromList(gridElements, false));
        
        // Write save data
        FileReadWrite.writePrivateData(gameManager.getActivity(), 
                                      "history/" + historyPath, 
                                      saveData.toString());
        
        // Create and save history entry metadata
        GameHistoryEntry entry = new GameHistoryEntry(
            historyPath,
            System.currentTimeMillis(),
            totalPlayTime,
            nbCoups,
            MainActivity.getBoardWidth() + "x" + MainActivity.getBoardHeight(),
            "history/" + historyPath + "_preview.png"
        );
        
        // Save preview image
        saveHistoryPreviewImage(entry.getPreviewImagePath());
        
        // Add entry to history index
        GameHistoryManager.addHistoryEntry(gameManager.getActivity(), entry);
        
        Timber.d("Game saved to history: %s", historyPath);
    } catch (Exception e) {
        Timber.e("Error saving game to history: %s", e.getMessage());
    }
}
```

### 4. UI Implementation

#### 4.1. Modified SaveGameScreen

Update the `SaveGameScreen` class to include a history tab: (done)

```java
private boolean isHistoryMode = false;
private Button saveTabButton;
private Button historyTabButton;

@Override
public void create() {
    // ... existing initialization code ...
    
    // Add tab buttons at the top
    saveTabButton = new GameButtonGeneral(
        layout.x(270), layout.y(100), 
        layout.x(250), layout.y(80),
        R.drawable.tab_saves_active, R.drawable.tab_saves_inactive,
        new SetSaveMode());
    
    historyTabButton = new GameButtonGeneral(
        layout.x(540), layout.y(100), 
        layout.x(250), layout.y(80),
        R.drawable.tab_history_inactive, R.drawable.tab_history_active,
        new SetHistoryMode());
    
    this.instances.add(saveTabButton);
    this.instances.add(historyTabButton);
    
    // Initialize in save mode
    isHistoryMode = false;
    updateTabButtons();
    
    // Create appropriate buttons based on mode
    createButtons();
}

private void updateTabButtons() {
    if (isHistoryMode) {
        saveTabButton.setImages(R.drawable.tab_saves_inactive, R.drawable.tab_saves_inactive);
        historyTabButton.setImages(R.drawable.tab_history_active, R.drawable.tab_history_active);
    } else {
        saveTabButton.setImages(R.drawable.tab_saves_active, R.drawable.tab_saves_active);
        historyTabButton.setImages(R.drawable.tab_history_inactive, R.drawable.tab_history_inactive);
    }
}

@Override
public void createButtons() {
    // Clear existing buttons
    clearButtons();
    
    if (isHistoryMode) {
        createHistoryButtons();
    } else {
        createSaveButtons();
    }
    
    // Add back button (common to both modes)
    addBackButton();
}

private void createHistoryButtons() {
    // Get history entries from GameHistoryManager
    List<GameHistoryEntry> historyEntries = 
        GameHistoryManager.getHistoryEntries(gameManager.getActivity());
    
    // Sort by timestamp (newest first)
    Collections.sort(historyEntries, (a, b) -> 
        Long.compare(b.getTimestamp(), a.getTimestamp()));
    
    // Create buttons for each history entry
    int buttonSize = layout.x(144);
    int spacing = layout.x(20);
    int buttonsPerRow = 3;
    
    for (int i = 0; i < historyEntries.size(); i++) {
        GameHistoryEntry entry = historyEntries.get(i);
        
        // Calculate position
        int row = i / buttonsPerRow;
        int col = i % buttonsPerRow;
        int x = layout.x(90) + col * (buttonSize + spacing);
        int y = layout.y(200) + row * (buttonSize + spacing * 3);
        
        // Create history button
        GameButtonHistoryGame historyButton = new GameButtonHistoryGame(
            gameManager.getActivity(),
            x, y, buttonSize, buttonSize,
            R.drawable.bt_start_up_history, R.drawable.bt_start_down_history,
            entry.getMapPath(), i, entry
        );
        
        this.instances.add(historyButton);
        
        // Add metadata text below button
        addHistoryMetadataText(entry, x, y + buttonSize + spacing);
        
        // Add action buttons
        addHistoryActionButtons(entry, x, y + buttonSize + spacing * 2);
    }
}

private void addHistoryMetadataText(GameHistoryEntry entry, int x, int y) {
    // Format date/time
    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
    String dateTime = sdf.format(new Date(entry.getTimestamp()));
    
    // Format duration
    String duration = formatDuration(entry.getPlayDuration());
    
    // Create text elements
    TextElement dateText = new TextElement(x, y, dateTime);
    TextElement durationText = new TextElement(x, y + layout.y(20), 
                                             duration + " played");
    TextElement boardSizeText = new TextElement(x, y + layout.y(40), 
                                              entry.getBoardSize() + " board");
    TextElement movesText = new TextElement(x, y + layout.y(60), 
                                          entry.getMovesMade() + " moves");
    
    this.instances.add(dateText);
    this.instances.add(durationText);
    this.instances.add(boardSizeText);
    this.instances.add(movesText);
}

private void addHistoryActionButtons(GameHistoryEntry entry, int x, int y) {
    int actionButtonWidth = layout.x(70);
    int actionButtonHeight = layout.y(40);
    int spacing = layout.x(10);
    
    // Load button
    GameButtonHistoryAction loadButton = new GameButtonHistoryAction(
        x, y, actionButtonWidth, actionButtonHeight,
        R.drawable.bt_load_up, R.drawable.bt_load_down,
        new LoadHistoryAction(entry)
    );
    
    // Save button (promote to permanent save)
    GameButtonHistoryAction saveButton = new GameButtonHistoryAction(
        x + actionButtonWidth + spacing, y, 
        actionButtonWidth, actionButtonHeight,
        R.drawable.bt_save_up, R.drawable.bt_save_down,
        new SaveHistoryAction(entry)
    );
    
    // Delete button
    GameButtonHistoryAction deleteButton = new GameButtonHistoryAction(
        x + (actionButtonWidth + spacing) * 2, y, 
        actionButtonWidth, actionButtonHeight,
        R.drawable.bt_delete_up, R.drawable.bt_delete_down,
        new DeleteHistoryAction(entry)
    );
    
    this.instances.add(loadButton);
    this.instances.add(saveButton);
    this.instances.add(deleteButton);
}

#### 4.2. History Entry UI

Each history entry should display: (done)
- Minimap preview (similar to current save buttons) (done)
- Date/time of the save (done)
- Play duration (done)
- Board size (done)
- Number of moves made (done)
- Options to: (done)
  - Load the game (done)
  - Delete from history (done)
  - Promote to permanent save (done)
  - Share via link (done)

## UI Mockup

```
+--------------------------------------+
|  [SAVES]    [HISTORY]                |
+--------------------------------------+
|                                      |
|  +--------+  +--------+  +--------+  |
|  |        |  |        |  |        |  |
|  |  Map1  |  |  Map2  |  |  Map3  |  |
|  |        |  |        |  |        |  |
|  +--------+  +--------+  +--------+  |
|  Mar 2, 9:30 Mar 1, 15:12 Feb 28, 20:45 |
|  5m played   12m played  3m played   |
|  16x16 board 18x22 board 14x14 board |
|  24 moves    37 moves    15 moves    |
|                                      |
|  [Load] [Save] [Delete]              |
|                                      |
+--------------------------------------+
|           [BACK TO MENU]             |
+--------------------------------------+

## Implementation Order

The implementation will proceed in the following order:

1. **Core Data Structure and Storage (Phase 1)** (done)
   - Create the `GameHistoryEntry` class (done)
   - Implement the `GameHistoryManager` for handling history entries (done)
   - Add methods to FileReadWrite for handling history files (done)
   - Implement JSON serialization/deserialization for history metadata (done)

2. **Time Tracking and Auto-Saving (Phase 2)** (done)
   - time tracking is already implemented into GridGameScreen - use that timer (done)
   - Implement automatic saving after 1 minute of gameplay (done)
   - add a stop of the timer, when the user reached the target or pressed the back button or the next button. Then update the history save entry with the new time and moves data. (done)
   - Create minimap previews for history entries (done)
   - Test saving and loading of history entries (done)

3. **UI Implementation (Phase 3)** (done)
   - Create UI assets for tabs and buttons (done)
   - Modify SaveGameScreen to include history tab (done)
   - Implement history entry display with metadata (done)
   - Add action buttons (Load, Save, Delete, Share) (done)

4. **Integration and Testing (Phase 4)** (done)
   - Integrate with existing save game functionality (done)
   - Implement error handling and edge cases (done)
   - Test across different devices and scenarios (done)

## UI Assets Needed

The following UI assets will need to be created: (done)

1. **Tab Buttons** (done)
   - `tab_saves_active.png` - Active state for Saves tab (done)
   - `tab_saves_inactive.png` - Inactive state for Saves tab (done)
   - `tab_history_active.png` - Active state for History tab (done)
   - `tab_history_inactive.png` - Inactive state for History tab (done)

2. **History Entry Buttons** (done)
   - `bt_start_up_history.png` - Normal state for history entry button (done)
   - `bt_start_down_history.png` - Pressed state for history entry button (done)

3. **Action Buttons** (done)
   - `bt_load_up.png` / `bt_load_down.png` - Load button states (done)
   - `bt_save_up.png` / `bt_save_down.png` - Save button states (promote to permanent save) (done)
   - `bt_delete_up.png` / `bt_delete_down.png` - Delete button states (done)
   - `bt_share_up.png` / `bt_share_down.png` - Share button states (done)

4. **Icons and Indicators** (done)
   - `icon_clock.png` - Icon for play duration (done)
   - `icon_moves.png` - Icon for moves made (done)
   - `icon_optimal.png` - Icon for optimal moves (done)
   - `icon_board.png` - Icon for board size (done)

These assets should follow the existing visual style of the app to maintain consistency. The UI implementation can use placeholder assets initially until the final designs are created.

## Benefits

1. **Gameplay Recovery**: Players can recover any game they've invested time in (done)
2. **Progress Tracking**: Players can see their recent gameplay history (done)
3. **Improved UX**: No need to manually save games that might be important (done)
4. **Organization**: Clear separation between intentional saves and automatic history (done)

## Technical Considerations

1. **Storage Limits**: Implement a maximum number of history entries (e.g., 20) to prevent excessive storage use (done)
2. **Performance**: Ensure history saving happens in a background thread to avoid UI stuttering (done)
3. **Compatibility**: Make sure the history feature works with existing save game functionality (done)
4. **Error Handling**: Implement robust error handling for cases where history saving fails (done)

## Current Status (March 3, 2025)

The Game History feature has been fully implemented with all planned functionality. The implementation includes:

1. **Automatic History Saving**: Games are automatically saved to history after 10 seconds of gameplay (reduced from 60 seconds for testing)
2. **History UI**: A dedicated history tab in the save game screen shows all saved history entries
3. **History Management**: Users can load, delete, or promote history entries to permanent saves
4. **Metadata Display**: Each history entry shows date/time, play duration, board size, and moves made
5. **Error Handling**: Robust error handling ensures history saving works reliably

The only remaining issue that was fixed was related to file paths in Android's private storage, which doesn't support path separators in filenames. This has been resolved by using proper file path handling.
