package roboyard.eclabs;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

        // Calculate positions for autosave button and back button
        autosaveButtonX = (int) (55 * ratioW);
        autosaveButtonY = (int) ((45 + ts) * ratioH);
        backButtonX = (int) (814 * ratioW);
        backButtonY = (int) (1650 * ratioH);

        System.out.println("DEBUG: loading saved maps");
        
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
        System.out.println("DEBUG: finished loading saved maps");
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
        init();
        ArrayList<GameButtonGotoSavedGame> aRemove = new ArrayList<>();
        for (Object currentObject : this.instances) {
            if (currentObject.getClass() == GameButtonGotoSavedGame.class) {
                aRemove.add((GameButtonGotoSavedGame) currentObject);
            }
        }
        for (GameButtonGotoSavedGame p : aRemove) {
            this.instances.remove(p);
        }

        String mapPath = "";
        SaveManager saver = new SaveManager(gameManager.getActivity());

        // Create buttons for each save slot
        for (int i = 0; i < buttonPositionsX.length; i++) {
            mapPath = getMapPath(i);
            if (i == 0) {
                this.instances.add(new GameButtonGotoSavedGame(autosaveButtonX, autosaveButtonY, iconSize * ratioH, iconSize * ratioW, saver.getButtonAutoSaved(mapPath, true), saver.getButtonAutoSaved(mapPath, false), 4, mapPath));
            } else {
                // System.out.println("DEBUG: Creating button for save slot " + i);
                this.instances.add(new GameButtonGotoSavedGame(buttonPositionsX[i], buttonPositionsY[i], iconSize * ratioH, iconSize * ratioW, saver.getButtonSaved(mapPath, true), saver.getButtonSaved(mapPath, false), 4, mapPath));
            }
        }

        // Add back button
        this.instances.add(new GameButtonGotoBack(backButtonX, backButtonY, (int) (222 * ratioH), (int) (222 * ratioW), R.drawable.bt_back_up, R.drawable.bt_back_down));
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
        
        // Show different text based on save/load mode
        GridGameScreen gameScreen = (GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME);
        if (gameScreen != null && gameScreen.isRandomGame()) {
            renderManager.drawText((int) (20 * ratioW), (int) (55 * ratioH), "Select slot to save map");
        } else {
            renderManager.drawText((int) (20 * ratioW), (int) (55 * ratioH), "Load map");
            //+ ((gameScreen.isRandomGame() == true) ? " (random)" : "keins") + "!" + ((gameScreen != null) ? " (gamescreen ist nicht null)" : ""));
        }

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
                        renderManager.drawText(buttonPositionsX[i] - moveleft, buttonPositionsY[i] - 5 + j * 15, bar);
                    }

                }

                renderManager.setColor(Color.parseColor("#000000"));
                renderManager.drawText(buttonPositionsX[i] - moveleft + 1 + (i<10? 8 : 0), buttonPositionsY[i] - 5, i + ". " + mapUniqueString[i]);
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
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
