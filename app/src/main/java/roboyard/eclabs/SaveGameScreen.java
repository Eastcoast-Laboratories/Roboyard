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
    private int autosaveButtonX;
    private int autosaveButtonY;
    private int backButtonX;
    private int backButtonY;
    private int saveTabButtonX;
    private int saveTabButtonY;
    private int historyTabButtonX;
    private int historyTabButtonY;
    private int loadOrSaveX;
    private int loadOrSaveY;
    private int tabButtonWidth;
    private int tabButtonHeight;
    private boolean saveMode = false;
    private boolean historyMode = false; // Flag to indicate if we're in history view mode

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
        saveMode = false; // Start in load mode
        init();
        createButtons();
    }

    /**
     * calculate Button Positions and load all saved maps to create a unique string for each from the mapElements
     */
    public void init() {
        // Button positions and dimensions
        ratioW = ((float) gameManager.getScreenWidth()) / ((float) 1080);
        ratioH = ((float) gameManager.getScreenHeight()) / ((float) 1920);
        int hs2 = this.gameManager.getScreenHeight() / 2;
        ts = hs2 / 10;
        iconSize = 144;

        // Use minimum of width/height ratio to maintain circular shape
        float buttonRatio = Math.min(ratioW, ratioH);
        int buttonSize = (int)(iconSize * buttonRatio);

        // Calculate button positions for horizontal and vertical layout
        int stepX = 211; // Horizontal step
        int stepY = 222; // Vertical step
        int cols = 5; // Number of columns
        int rows = 7; // Number of rows

        buttonPositionsX = new int[cols * rows];
        buttonPositionsY = new int[cols * rows];

        for (int i = 0; i < cols * rows; i++) {
            int col = i % cols;
            int row = (i / cols) % rows;
            buttonPositionsX[i] = (int) ((55 + (stepX * col)) * ratioW);
            buttonPositionsY[i] = (int) ((45 + ts + (stepY * row)) * ratioH);
        }

        // the position of the text "Load map" or "Select slot to save map"
        loadOrSaveX = (int) ((470) * ratioW);
        loadOrSaveY = (int) ((38) * ratioH);

        // Calculate positions for autosave button and back button
        autosaveButtonX = (int) (55 * ratioW);
        autosaveButtonY = (int) ((45 + ts) * ratioH);
        backButtonX = (int) (814 * ratioW);
        backButtonY = (int) (1650 * ratioH);
        
        // Calculate positions for tab buttons
        tabButtonWidth = (int) (200 * ratioW);
        tabButtonHeight = (int) (80 * ratioH);
        saveTabButtonX = (int) (20 * ratioW);
        saveTabButtonY = (int) (0 * ratioH);
        historyTabButtonX = (int) (240 * ratioW);
        historyTabButtonY = (int) (0 * ratioH);


        Timber.d(" loading saved maps");
        
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
    }
    
    /**
     * Create buttons for saving and loading games.
     */
    public void createButtons() {
        int iconsize = 144;

        init();
        
        // Clear all existing buttons
        ArrayList<IGameObject> objectsToRemove = new ArrayList<>();
        for (IGameObject currentObject : instances) {
            if (currentObject instanceof IGameObject) {
                objectsToRemove.add((IGameObject) currentObject);
            }
        }
        for (IGameObject obj : objectsToRemove) {
            instances.remove(obj);
        }
        
        // Add tab buttons
        final SaveGameScreen thisScreen = this;
        
        // Saves tab button
        GameButtonTab savesTabButton = new GameButtonTab(
            saveTabButtonX, 
            saveTabButtonY, 
            tabButtonWidth, 
            tabButtonHeight, 
            "Saves", 
            !historyMode,
            new Runnable() {
                @Override
                public void run() {
                    thisScreen.showSavesTab();
                }
            }
        );
        savesTabButton.create();
        instances.add(savesTabButton);
        
        // History tab button
        GameButtonTab historyTabButton = new GameButtonTab(
            historyTabButtonX, 
            historyTabButtonY, 
            tabButtonWidth, 
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
            createHistoryButtons(iconsize);
        } else {
            // Create save/load buttons
            createSaveButtons(iconsize);
        }
    }
    
    /**
     * Create buttons for history mode
     */
    private void createHistoryButtons(int iconsize) {
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
        int buttonSize = (int)(iconsize * buttonRatio);

        // Calculate positions for history entries (vertical list)
        int startY = (int) (120 * ratioH);  // Start below the tabs
        int stepY = buttonSize + (int) (20 * ratioH);  // Vertical spacing
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
                i
            );
            historyButton.create();
            instances.add(historyButton);
            
            // Add action buttons (delete, promote, share)
            
            // Delete button
            int actionButtonSize = buttonSize / 3;
            int actionX = startX + buttonSize + (int) (150 * ratioW);
            
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

        // Create buttons for each save slot
        for (int i = 0; i < buttonPositionsX.length; i++) {
            mapPath = getMapPath(i);
            GameButtonGotoSavedGame saveButton;
            if (i == 0) {
                saveButton = new GameButtonGotoSavedGame(
                    gameManager.getActivity(),
                    autosaveButtonX, 
                    autosaveButtonY,
                    buttonSize,
                    buttonSize, 
                    saver.getButtonAutoSaved(mapPath, true), 
                    saver.getButtonAutoSaved(mapPath, false), 
                    mapPath,
                    i,
                    autosaveButtonX,
                    autosaveButtonY,
                    buttonSize
                );
            } else {
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
            }
            saveButton.create();
            instances.add(saveButton);

            // Add share button for this save slot
            String saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
            if (saveData != null && !saveData.isEmpty()) {
                GameButtonShareMap shareButton = new GameButtonShareMap(
                    0, 0,  // x,y will be set in update()
                    buttonSize/4, buttonSize/4, // smaller size
                    R.drawable.share,
                    R.drawable.share,
                    mapPath,
                    i == 0 ? autosaveButtonX : buttonPositionsX[i],
                    i == 0 ? autosaveButtonY : buttonPositionsY[i],
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
        // Draw background and text
        renderManager.setColor(Color.parseColor("#cccccc"));
        renderManager.paintScreen();
        renderManager.setColor(Color.BLACK);

        renderManager.setTextSize((int) (0.4 * ts));
        
        // Show different text based on current mode
        if (historyMode) {
            renderManager.drawText((int) (loadOrSaveX), (int) (loadOrSaveY * ratioH), "Game History");
        } else {
            GridGameScreen gameScreen = (GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME);
            if (gameScreen != null && gameScreen.isRandomGame()) {
                renderManager.drawText((int) (loadOrSaveX), (int) (loadOrSaveY * ratioH), "Select slot to save map");
            } else {
                renderManager.drawText((int) (loadOrSaveX), (int) (loadOrSaveY * ratioH), "Load map");
            }
        }

        if (!historyMode) {
            // Draw save slots
            for (int i = 0; i < buttonPositionsX.length; i++) {
                if (i == 0) {
                    renderManager.drawText((int) (20 * ratioW), (int) ((42 + ts) * ratioH)-5, "Autosave");
                } else {
                    renderManager.setColor(Color.parseColor(mapUniqueColor[i]));
                    int moveleft=16;

                    if (mapUniqueString[i].length() > 0){
                        // unicode string with 7 filled squares
                        String bar = "\u2588\u2588\u2588\u2588\u2588\u2588\u2588";
                        for (int j = 0; j < 8; j++) {
                            renderManager.drawText(buttonPositionsX[i] - moveleft, (int)(buttonPositionsY[i] + (-11 + j * 22)*ratioH), bar);
                        }

                    }

                    renderManager.setTextSize((int) (0.37 * ts));
                    renderManager.setColor(Color.parseColor("#000000"));
                    renderManager.drawText(buttonPositionsX[i] - moveleft + 1 + (i<10? 8 : 0), buttonPositionsY[i] - 5, i + ". " + mapUniqueString[i]);
                }
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

        super.draw(renderManager);
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if (gameManager.getInputManager().backOccurred()) {
            gameManager.setGameScreen(gameManager.getPreviousScreenKey());
        }
        
        // Check if we're coming from a game screen and ensure we're in load mode
        int previousScreenKey = gameManager.getPreviousScreenKey();
        GameScreen previousScreen = gameManager.getScreens().get(previousScreenKey);
        
        if (previousScreen instanceof GridGameScreen && 
            gameManager.getCurrentScreen() == this) {
            // Set to load mode if we're coming from a game screen
            if (saveMode) {
                Timber.d("Coming from game screen, ensuring we're in load mode");
                setSaveMode(false);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Returns whether the screen is in save mode.
     * @return true if in save mode, false if in load mode
     */
    public boolean isSaveMode() {
        return saveMode;
    }

    /**
     * Sets whether the screen is in save mode.
     * @param saveMode true for save mode, false for load mode
     */
    public void setSaveMode(boolean saveMode) {
        if (this.saveMode != saveMode) {
            this.saveMode = saveMode;
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
            
            // Clear all existing buttons to prevent color preservation
            instances.clear(); // TODO: does not work
            
            // Recreate all buttons
            createButtons();
        }
    }
    
    /**
     * Switch to saves tab
     */
    public void showSavesTab() {
        setHistoryMode(false);
    }
    
    /**
     * Switch to history tab
     */
    public void showHistoryTab() {
        setHistoryMode(true);
    }
    
    /**
     * Force refresh the screen buttons
     */
    public void refreshScreen() {
        Timber.d("Refreshing SaveGameScreen");
        createButtons();
    }
}
