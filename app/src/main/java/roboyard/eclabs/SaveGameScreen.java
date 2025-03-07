package roboyard.eclabs;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Screen for saving and loading games (screen 9)
 */
public class SaveGameScreen extends GameScreen {
    private float ratioW;
    private float ratioH;
    private int ts;
    private int iconSize;
    private int[] buttonPositionsX;
    private int[] buttonPositionsY;
    private String[] mapUniqueString;
    private String[] mapUniqueColor;
    private int backButtonX;
    private int backButtonY;
    private int saveTabButtonX;
    private int loadTabButtonX;
    private int historyTabButtonX;
    private int tabButtonsY;
    private int inactiveTabOffset;
    private int loadOrSaveX;
    private int loadOrSaveY;
    private int tabButtonWidth;
    private int tabButtonHeight;
    private boolean saveMode = false;
    private boolean loadMode = true; 
    private boolean historyMode = false;
    private int historyButtonSize;
    boolean dontAutoSwitchTabs = false;
    private boolean skipModeSwitch = false;

    // Cache for unique string to color mapping to avoid repeated SHA-256 hashing
    private static Map<String, String> colorCache = new HashMap<>();
    
    // Cache for save data to avoid repeated file reads
    private static Map<String, String> saveDataCache = new HashMap<>();
    
    // Cache for map unique strings to avoid recomputing
    private static Map<String, String> mapUniqueStringCache = new HashMap<>();

    public SaveGameScreen(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void create() {
        // Load button images and initialize
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_down_saved_used);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_up_saved_used);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_up_saved);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_down_saved);
        gameManager.getRenderManager().loadImage(R.drawable.share);
        saveMode = false; 
        init();
    }

    /**
     * Initialize the screen
     */
    private void init() {
        
        // Calculate ratios for different screen sizes
        ratioW = gameManager.getScreenWidth() / 1080.0f;
        ratioH = gameManager.getScreenHeight() / 1920.0f;
        
        // Set default mode
        saveMode = false;
        loadMode = true;
        historyMode = false;
        
        // Calculate button positions
        calculateButtonPositions();
        
        // Load saved maps
        loadSavedMaps();
        
        // Create buttons
        createButtons();
    }
    
    /**
     * Calculate button positions based on screen size
     */
    private void calculateButtonPositions() {
        // Button positions and dimensions
        int hs2 = this.gameManager.getScreenHeight() / 2;
        ts = hs2 / 10;
        iconSize = 144;
        
        // Calculate positions for back button
        backButtonX = (int) (50 * ratioW);
        backButtonY = (int) (1650 * ratioH);
        
        // Calculate positions for tab buttons
        tabButtonWidth = (int) (160 * ratioW);
        tabButtonHeight = (int) (100 * ratioH);
        saveTabButtonX = (int) (20 * ratioW);
        loadTabButtonX = (int) (180 * ratioW);
        historyTabButtonX = (int) (340 * ratioW);
        tabButtonsY = (int) (-10 * ratioH);
        inactiveTabOffset = (int) (5 * ratioW);

        // Calculate positions for save slots
        int numCols = 5;
        int numRows = 7;
        int startX = (int) (44 * ratioW);
        int startY = (int) (400 * ratioH);
        int stepX = (int) (210 * ratioW);
        int stepY = (int) (200 * ratioH);
        
        buttonPositionsX = new int[numCols * numRows];
        buttonPositionsY = new int[numCols * numRows];
        
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                int index = row * numCols + col;
                buttonPositionsX[index] = (int) (startX + col * stepX );
                buttonPositionsY[index] = startY + (row - 1) * stepY;
            }
        }
        
        // the position of the text "Load map" or "Select slot to save map"
        loadOrSaveX = (int) ((555) * ratioW);
        loadOrSaveY = (int) ((55) * ratioH);

        historyButtonSize  = 155;
    }
    
    /**
     * Load saved maps
     */
    private void loadSavedMaps() {
        Timber.d(" loading saved maps");
        int cols = 5; // Number of columns
        int rows = 7; // Number of rows
        
        ArrayList<GridElement> gridElements = new ArrayList<>(100);  // Pre-allocate with reasonable size
        
        mapUniqueString = new String[cols * rows];
        mapUniqueColor = new String[cols * rows];
        for (int i = 0; i < cols * rows; i++) {
            String mapPath = getMapPath(i);
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if(saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                // Check map unique string cache first
                String uniqueString = mapUniqueStringCache.get(mapPath);
                if (uniqueString == null) {
                    // Check save data cache first
                    String saveData = saveDataCache.get(mapPath);
                    if (saveData == null) {
                        saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
                        saveDataCache.put(mapPath, saveData);
                    }
                    
                    gridElements.clear();  // Reuse ArrayList to avoid allocation
                    gridElements = MapObjects.extractDataFromString(saveData);
                    uniqueString = MapObjects.createStringFromList(gridElements, true);
                    mapUniqueStringCache.put(mapPath, uniqueString);
                }
                mapUniqueString[i] = uniqueString;
                
                // Use cached color if available, otherwise generate and cache it
                String cachedColor = colorCache.get(uniqueString);
                if (cachedColor != null) {
                    mapUniqueColor[i] = cachedColor;
                } else {
                    mapUniqueColor[i] = MapObjects.generateHexColorFromString(uniqueString);
                    colorCache.put(uniqueString, mapUniqueColor[i]);
                }
            } else {
                mapUniqueString[i] = "";
                mapUniqueColor[i] = "#000000";
            }
        }
        Timber.d(" finished loading saved maps");
    }

    public static void clearCachesForMap(String mapPath) {
        // Clear save data cache
        saveDataCache.remove(mapPath);
        
        // Get and remove the unique string from cache
        String uniqueString = mapUniqueStringCache.get(mapPath);
        mapUniqueStringCache.remove(mapPath);
        
        // If we had a unique string, also remove its color from cache
        if (uniqueString != null) {
            colorCache.remove(uniqueString);
        }
        
        // Clear minimap cache in GameButtonGotoSavedGame
        GameButtonGotoSavedGame.clearMinimapCache(mapPath);
    }
    
    /**
     * Create buttons for the screen
     */
    public void createButtons() {
        // Clear existing buttons
        instances.clear();
        
        final SaveGameScreen thisScreen = this;
        
        // Create tab buttons
        GameButtonTab savesTabButton = new GameButtonTab(
            saveTabButtonX,
                tabButtonsY - (saveMode?0:inactiveTabOffset),
            tabButtonWidth, 
            tabButtonHeight, 
            "Save", 
            saveMode,
            new Runnable() {
                @Override
                public void run() {
                    thisScreen.showSavesTab();
                }
            }
        );
        savesTabButton.create();
        instances.add(savesTabButton);
        
        GameButtonTab loadTabButton = new GameButtonTab(
            loadTabButtonX,
                tabButtonsY - (loadMode?0:inactiveTabOffset),
            tabButtonWidth, 
            tabButtonHeight, 
            "Load", 
            loadMode,
            new Runnable() {
                @Override
                public void run() {
                    thisScreen.showLoadTab();
                    dontAutoSwitchTabs = true;
                }
            }
        );
        loadTabButton.create();
        instances.add(loadTabButton);
        
        GameButtonTab historyTabButton = new GameButtonTab(
            historyTabButtonX,
                tabButtonsY - (historyMode?0:inactiveTabOffset),
            (int)(tabButtonWidth* 1.2),
            tabButtonHeight, 
            "History", 
            historyMode,
            new Runnable() {
                @Override
                public void run() {
                    thisScreen.showHistoryTab();
                }
            }
        );
        historyTabButton.create();
        instances.add(historyTabButton);

        // Add back button
        instances.add(new GameButtonGotoBack(
            backButtonX, 
            backButtonY, 
            (int)(iconSize * Math.min(ratioW, ratioH)), 
            (int)(iconSize * Math.min(ratioW, ratioH)), 
            R.drawable.bt_back_up, 
            R.drawable.bt_back_down));
            
        if (historyMode) {
            // Create history buttons
            createHistoryButtons();
        } else {
            // Create save/load buttons
            createSaveButtons(iconSize);
        }
    }
    
    /**
     * Create buttons for history mode
     */
    private void createHistoryButtons() {
        // Initialize the history manager
        GameHistoryManager.initialize(gameManager.getActivity());
        
        // Get all history entries
        List<GameHistoryEntry> historyEntries = GameHistoryManager.getHistoryEntries(gameManager.getActivity());
        
        if (historyEntries.isEmpty()) {
            // We'll show a message in the draw method
            return;
        }
        
        // Use minimum of width/height ratio to maintain circular shape
        float buttonRatio = Math.min(ratioW, ratioH);
        int buttonSize = (int)(historyButtonSize * buttonRatio);

        // Calculate positions for history entries (vertical list)
        int startY = (int) (120 * ratioH);  // Start below the tabs
        int stepY = buttonSize + (int) (-5 * ratioH);  // Vertical spacing
        int startX = (int) (55 * ratioW);   // Left margin
        
        // Create buttons for each history entry
        for (int i = 0; i < historyEntries.size(); i++) {
            GameHistoryEntry entry = historyEntries.get(i);
            int y = startY + (i * stepY);
            
            // Skip if we're going to render off-screen
            if (y > gameManager.getScreenHeight() - buttonSize) {
                break;
            }
            
            // Create button for loading the history entry
            GameButtonGotoHistoryGame historyButton = new GameButtonGotoHistoryGame(
                startX,
                y,
                buttonSize,
                buttonSize,
                entry,
                gameManager.getActivity()
            );
            historyButton.create();
            instances.add(historyButton);
            
            // Explicitly load the minimap
            historyButton.loadMinimap(); // TODO: does not show
            
            // Add action buttons (delete, promote, share)
            
            // Delete button
            int actionButtonSize = buttonSize / 2;
            int actionX = startX + buttonSize + (int) (350 * ratioW);
            
            GameButtonHistoryAction deleteButton = new GameButtonHistoryAction(
                actionX,
                y,
                actionButtonSize,
                GameButtonHistoryAction.ACTION_DELETE,
                i,
                entry
            );
            deleteButton.create();
            instances.add(deleteButton);
            
            // Promote button
            GameButtonHistoryAction promoteButton = new GameButtonHistoryAction(
                actionX + actionButtonSize + 10,
                y,
                actionButtonSize,
                GameButtonHistoryAction.ACTION_PROMOTE,
                i,
                entry
            );
            promoteButton.create();
            instances.add(promoteButton);
            
            // Share button
            GameButtonHistoryAction shareButton = new GameButtonHistoryAction(
                actionX + (actionButtonSize + 10) * 2,
                y,
                actionButtonSize,
                GameButtonHistoryAction.ACTION_SHARE,
                i,
                entry
            );
            shareButton.create();
            instances.add(shareButton);
        }
    }
    
    /**
     * Create buttons for save/load mode
     */
    private void createSaveButtons(int iconsize) {
        String mapPath = "";
        SaveManager saver = new SaveManager(gameManager.getActivity());

        // Use minimum of width/height ratio to maintain circular shape
        float buttonRatio = Math.min(ratioW, ratioH);
        int buttonSize = (int)(iconsize * buttonRatio);
        Boolean hasSavegame;

        // Create buttons for each save slot
        for (int i = 0; i < buttonPositionsX.length; i++) {
            mapPath = getMapPath(i);
            hasSavegame = (mapUniqueString[i].length() > 0);

            GameButtonGotoSavedGame saveButton;
            // each save slot
            saveButton = new GameButtonGotoSavedGame(
                gameManager.getActivity(),
                buttonPositionsX[i],
                buttonPositionsY[i],
                buttonSize,
                buttonSize,
                saver.getButtonSaved(mapPath, true),
                saver.getButtonSaved(mapPath, false),
                mapPath,
                i,
                buttonPositionsX[i],
                buttonPositionsY[i],
                buttonSize
            );
            saveButton.create();
            instances.add(saveButton);

            // Set save mode based on current screen mode
            saveButton.setSaveMode(saveMode);

            if(hasSavegame && isSaveMode() || !hasSavegame && isLoadMode()) {
                saveButton.setEnabled(false);
            }
            // Add share button for this save slot
            String saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
            if (saveData != null && !saveData.isEmpty()) {
                GameButtonShareMap shareButton = new GameButtonShareMap(
                    0, 0,  // x,y will be set in update()
                    buttonSize/4, buttonSize/4, // smaller size
                    R.drawable.share,
                    R.drawable.share,
                    mapPath,
                    buttonPositionsX[i],
                    buttonPositionsY[i],
                    buttonSize
                );
                shareButton.create();
                instances.add(shareButton);
            }
        }
    }

    /**
     * Get the file path for a specific level in the screen.
     *
     * @param levelInScreen The level index in the screen.
     * @return The file path for the level.
     */
    public static String getMapPath(int levelInScreen) {
        return "map_" + levelInScreen + ".txt";
    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }

    @Override
    public void draw(RenderManager renderManager) {
        // Draw background
        renderManager.setColor(Color.parseColor("#cccccc"));
        renderManager.paintScreen();
        
        // Draw all game elements
        super.draw(renderManager);
        
        // Draw screen title based on current mode
        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize(ts/2);
        
        if (!historyMode) {
            // Draw save slots
            for (int i = 0; i < buttonPositionsX.length; i++) {

                renderManager.setColor(Color.parseColor(mapUniqueColor[i]));
                int moveleft=16;

                if (mapUniqueString[i].length() > 0){
                    // unicode string with 7 filled squares
                    String bar = "\u2588\u2588\u2588\u2588\u2588\u2588\u2588";
                    for (int j = 0; j < 1; j++) {
                        renderManager.drawText(buttonPositionsX[i] - moveleft, (int)(buttonPositionsY[i] + (-11 + j * 22)*ratioH), bar);
                    }

                }

                renderManager.setTextSize((int) (0.37 * ts));
                renderManager.setColor(Color.parseColor("#000000"));
                renderManager.drawText(buttonPositionsX[i] - moveleft + 1 + (i<10? 8 : 0), buttonPositionsY[i] - 5, (i+1) + ". " + mapUniqueString[i]);
            }
        }else{
            // Draw history message if no history entries
            List<GameHistoryEntry> historyEntries = GameHistoryManager.getHistoryEntries(gameManager.getActivity());
            if (historyEntries.isEmpty()) {
                renderManager.setTextSize((int) (0.4 * ts));
                renderManager.setColor(Color.BLACK);
                renderManager.drawText((int) (20 * ratioW), (int) (120 * ratioH), 
                        "No history entries yet. Play a game for at least one minute to create one.");
            }
        }
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if (gameManager.getInputManager().backOccurred()) {
            gameManager.setGameScreen(gameManager.getPreviousScreenKey());
        }
        
        // If we should skip mode switching, don't do any automatic mode changes
        if (skipModeSwitch) {
            // Just update the buttons with the current mode
            updateButtonModes();
            return;
        }
        
        // Check if we're coming from a game screen and ensure we're in the correct mode
        int previousScreenKey = gameManager.getPreviousScreenKey();
        GameScreen previousScreen = gameManager.getScreens().get(previousScreenKey);
        
        if (previousScreen instanceof GridGameScreen && 
            gameManager.getCurrentScreen() == this) {
            GridGameScreen gameScreen = (GridGameScreen) previousScreen;
            
            // If coming from a random game AND we haven't already saved a game, go to save mode
            if(!dontAutoSwitchTabs){
                if (gameScreen.isRandomGame()) {
                    if (!saveMode) {
                        Timber.d("Coming from random game screen, switching to save mode");
                        showSavesTab();
                    }
                } else if(!isLoadMode()){
                    // Otherwise go to load mode
                    if (!loadMode) {
                        Timber.d("Coming from game screen, switching to load mode");
                        showLoadTab();
                    }
                }
            }

        }
        
        // Update button modes
        updateButtonModes();
    }
    
    /**
     * Update the save mode on all save buttons
     */
    private void updateButtonModes() {
        // Update save mode on all save buttons
        for (IGameObject element : instances) {
            if (element instanceof GameButtonGotoSavedGame) {
                GameButtonGotoSavedGame saveButton = (GameButtonGotoSavedGame) element;
                saveButton.setSaveMode(saveMode);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Check if the screen is in save mode
     * @return true if in save mode, false otherwise
     */
    public boolean isSaveMode() {
        return saveMode;
    }
    
    /**
     * Check if the screen is in load mode
     * @return true if in load mode, false otherwise
     */
    public boolean isLoadMode() {
        return loadMode;
    }
    
    /**
     * Check if the screen is in history mode
     * @return true if in history mode, false otherwise
     */
    public boolean isHistoryMode() {
        return historyMode;
    }

    /**
     * Sets whether the screen is in save mode.
     * @param saveMode true for save mode, false for load mode
     */
    public void setSaveMode(boolean saveMode) {
        if (this.saveMode != saveMode) {
            this.saveMode = saveMode;
            if (saveMode) {
                // If entering save mode, ensure other modes are off
                this.loadMode = false;
                this.historyMode = false;
            } else if (!this.loadMode && !this.historyMode) {
                // If turning off save mode and no other mode is active, default to load mode
                this.loadMode = true;
            }
            // Recreate buttons when mode changes
            createButtons();
        }
    }

    /**
     * Set the screen to history mode
     */
    public void setHistoryMode(boolean historyMode) {
        if (this.historyMode != historyMode) {
            this.historyMode = historyMode;
            
            if (historyMode) {
                // If entering history mode, ensure other modes are off
                this.saveMode = false;
                this.loadMode = false;
            } else if (!this.saveMode && !this.loadMode) {
                // If turning off history mode and no other mode is active, default to load mode
                this.loadMode = true;
            }
            
            // Clear all existing buttons to prevent color preservation
            instances.clear(); // TODO: does not work
            
            // Recreate all buttons
            createButtons();
        }
    }
    
    /**
     * Show the saves tab
     */
    public void showSavesTab() {
        saveMode = true;
        loadMode = false;
        historyMode = false;
        
        // Reset the skip flag when explicitly changing modes
        skipModeSwitch = false;
        
        // Update button states
        createButtons();
    }
    
    /**
     * Show the load tab
     */
    public void showLoadTab() {
        saveMode = false;
        loadMode = true;
        historyMode = false;
        
        // Reset the skip flag when explicitly changing modes
        skipModeSwitch = false;
        
        // Update button states
        createButtons();
    }
    
    /**
     * Show the history tab
     */
    public void showHistoryTab() {
        saveMode = false;
        loadMode = false;
        historyMode = true;
        
        // Update button states
        createButtons();
    }
    
    /**
     * Force refresh the screen buttons
     */
    public void refreshScreen() {
        Timber.d("Refreshing SaveGameScreen");
        // Clear instances and recreate all buttons
        instances.clear();
        createButtons();
    }
    
    /**
     * Refresh a specific save slot button
     * @param slotNumber The slot number to refresh
     */
    public void refreshSaveSlot(int slotNumber) {
        // Find and refresh the specific save slot button
        for (IGameObject element : instances) {
            if (element instanceof GameButtonGotoSavedGame) {
                GameButtonGotoSavedGame saveButton = (GameButtonGotoSavedGame) element;
                if (saveButton.getButtonNumber() == slotNumber) {
                    saveButton.create();
                    break;
                }
            }
        }
        Timber.d("Refreshed save slot button: %d", slotNumber);
    }
    
    /**
     * Set the skipModeSwitch flag
     * @param skip true to skip automatic mode switching, false otherwise
     */
    public void setSkipModeSwitch(boolean skip) {
        skipModeSwitch = skip;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
