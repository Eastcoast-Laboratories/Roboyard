package roboyard.eclabs;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.app.Activity;

import androidx.core.content.res.ResourcesCompat;

import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import timber.log.Timber;

/**
 * Represents a button to save or load a game.
 */
public class GameButtonGotoSavedGame extends GameButtonGoto {

    private final int defaultImageUp;
    private final int defaultImageDown;
    private final String mapPath;
    private final int buttonNumber;
    private final int parentButtonX;
    private final int parentButtonY;
    private final int parentButtonSize;
    private ScreenLayout layout;
    private Context context;
    private boolean isSaveMode;

    public GameButtonGotoSavedGame(Context context, int x, int y, int w, int h, int imageUp, int imageDown, String mapPath, int buttonNumber,
                                  int parentButtonX, int parentButtonY, int parentButtonSize) {
        super(x, y, w, h, imageUp, imageDown, 4); // 4 is the target screen for GridGameScreen
        this.defaultImageUp = imageUp;
        this.defaultImageDown = imageDown;
        this.mapPath = mapPath;
        this.buttonNumber = buttonNumber;
        this.parentButtonX = parentButtonX;
        this.parentButtonY = parentButtonY;
        this.parentButtonSize = parentButtonSize;
        this.context = context;
        this.isSaveMode = false; // Default to load mode
    }

    /**
     * Handles the click event of the button.
     * @param gameManager The GameManager instance.
     */
    @Override
    public void onClick(GameManager gameManager) {
        GridGameScreen gameScreen = (GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME);
        
        // Check if we're in save mode
        if (isSaveMode || (gameScreen != null && gameScreen.isRandomGame())) {
            // Screen to save or overwrite a savegame
            // Timber.d(" Saving game to slot: " + mapPath);
            ArrayList<GridElement> gridElements = gameScreen.getGridElements();
            
            // Build save data with additional fields
            StringBuilder saveData = new StringBuilder();
            
            // Add board name
            saveData.append("name:").append(gameScreen.mapName).append(";");
            
            // Add number of optimal moves if available
            int numMoves = gameScreen.solutionMoves;
            if (numMoves > 0) {
                saveData.append("num_moves:").append(numMoves).append(";");
            }
            
            // Add solution if available 
            if (gameScreen.solution != null) {
                StringBuilder solutionStr = new StringBuilder("solution:");
// TODO: add the solution to the share string
//                for (IGameMove move : gameScreen.solution.getMoves()) {
//                    if (move instanceof RRGameMove) {
//                        RRGameMove rrMove = (RRGameMove) move;
//                        solutionStr.append(rrMove.getColor()).append("_")
//                                 .append(rrMove.getX()).append(",")
//                                 .append(rrMove.getY()).append(";");
//                    }
//                }
                saveData.append(solutionStr);
            }

            // Add the grid elements data
            saveData.append(MapObjects.createStringFromList(gridElements, false));
            
            try {
                // Write save data directly (will overwrite if file exists)
                FileReadWrite.writePrivateData(gameManager.getActivity(), mapPath, saveData.toString());
                // Timber.d(" wrote " + saveData.length() + " bytes to " + mapPath);
                
                // Add to saved games list if needed
                addMapsSaved(gameManager);

                // Refresh save game screen buttons
                SaveGameScreen saveGameScreen = (SaveGameScreen) gameManager.getScreens().get(Constants.SCREEN_SAVE_GAMES);
                if (saveGameScreen != null) {
                    SaveGameScreen.clearCachesForMap(mapPath);
                    saveGameScreen.createButtons();
                    // Timber.d(" Refreshed save game screen buttons");
                }
                
                // Keep track of the game screen and explicitly set it as previous
                gameManager.setPreviousScreen(gameManager.getScreens().get(Constants.SCREEN_GAME));
                gameManager.setGameScreen(Constants.SCREEN_SAVE_GAMES);
            } catch (Exception e) {
                // Timber.d(" Error saving game: " + e.getMessage());
            }
        } else {
            // Screen to select a savegame
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if (saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                super.onClick(gameManager);
                gameScreen.setSavedGame(mapPath);
                // disable the savegame button in the gamescreen
                gameScreen.buttonSaveSetEnabled(false);
            }
        }
    }

    /**
     * Handles the click event of the button.
     * @deprecated Use onClick(GameManager) instead
     */
    @Deprecated
    public void onClick() {
        // Deprecated method, use onClick(GameManager) instead
        // This is kept for backward compatibility
    }

    /**
     * Set whether this button is in save mode
     * @param saveMode true for save mode, false for load mode
     */
    public void setSaveMode(boolean saveMode) {
        this.isSaveMode = saveMode;
    }

    @Override
    public void create() {
        super.create();
        // Check if save file exists and create minimap if it does
        Activity activity = (Activity)context;
        String saveData = FileReadWrite.readPrivateData(activity, mapPath);
        if (saveData != null && !saveData.isEmpty()) {
            String minimapPath = createMiniMap(saveData, context);
            if (minimapPath != null) {
                try {
                    // Load minimap as button image
                    Bitmap bitmap = BitmapFactory.decodeFile(minimapPath);
                    Drawable minimapDrawable = new BitmapDrawable(context.getResources(), bitmap);
                    this.setImageUp(minimapDrawable);
                    this.setImageDown(minimapDrawable); // Use same image for down state
                    this.setEnabled(true);
                } catch (Exception e) {
                    Timber.e(e, "Failed to load minimap as button image");
                }
            }
        } else {
            // nothing to do on empty slots
        }
    }

    @Override
    public void update(GameManager gameManager) {
        if (layout == null) {
            // Initialize screen layout on first update when we have gameManager
            layout = new ScreenLayout(
                gameManager.getScreenWidth(),
                gameManager.getScreenHeight(),
                gameManager.getActivity().getResources().getDisplayMetrics().density
            );
        }
        
        // Check if we're in save mode
        SaveGameScreen saveScreen = (SaveGameScreen) gameManager.getCurrentScreen();
        if (saveScreen != null && saveScreen.isSaveMode()) {
            // In save mode, enable all buttons
            this.setEnabled(true);
            // Set default images if no save data exists
            this.setImageUp(gameManager.getRenderManager().getDrawable(R.drawable.bt_start_up_saved));
            this.setImageDown(gameManager.getRenderManager().getDrawable(R.drawable.bt_start_down_saved));
        }
        
        super.update(gameManager);
    }

    /**
     * Create a mini map of a savegame
     * @param mapData The savegame data.
     * @param context The context.
     * @return The mini map as a png file.
     */
    public static String createMiniMap(String mapData, Context context) {
        try {
            // Parse map data
            ArrayList<GridElement> gridElements = MapObjects.extractDataFromString(mapData);
            
            // Create a bitmap for the minimap
            int minimapSize = 200; // Size of the minimap in pixels
            float gridSpace = minimapSize / Math.max(MainActivity.getBoardWidth(), MainActivity.getBoardHeight());
            
            Bitmap bitmap = Bitmap.createBitmap(
                (int)(MainActivity.getBoardWidth() * gridSpace), 
                (int)(MainActivity.getBoardHeight() * gridSpace), 
                Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);  // White background

            // Load all drawables
            Map<String, Drawable> drawables = new HashMap<>();
            drawables.put("grid_tiles", ResourcesCompat.getDrawable(context.getResources(), R.drawable.grid_tiles, null));
            drawables.put("roboyard", ResourcesCompat.getDrawable(context.getResources(), R.drawable.roboyard, null));
            drawables.put("mh", ResourcesCompat.getDrawable(context.getResources(), R.drawable.mh, null));
            drawables.put("mv", ResourcesCompat.getDrawable(context.getResources(), R.drawable.mv, null));
            drawables.put("robot_green", ResourcesCompat.getDrawable(context.getResources(), R.drawable.robot_green_right, null));
            drawables.put("robot_red", ResourcesCompat.getDrawable(context.getResources(), R.drawable.robot_red_right, null));
            drawables.put("robot_yellow", ResourcesCompat.getDrawable(context.getResources(), R.drawable.robot_yellow_right, null));
            drawables.put("robot_blue", ResourcesCompat.getDrawable(context.getResources(), R.drawable.robot_blue_right, null));
            drawables.put("target_red", ResourcesCompat.getDrawable(context.getResources(), R.drawable.cr, null));
            drawables.put("target_blue", ResourcesCompat.getDrawable(context.getResources(), R.drawable.cb, null));
            drawables.put("target_green", ResourcesCompat.getDrawable(context.getResources(), R.drawable.cv, null));
            drawables.put("target_yellow", ResourcesCompat.getDrawable(context.getResources(), R.drawable.cj, null));
            drawables.put("target_multi", ResourcesCompat.getDrawable(context.getResources(), R.drawable.cm, null));

            // Draw grid tiles
            Drawable fullGrid = drawables.get("grid_tiles");
            for(int x = 0; x < MainActivity.getBoardWidth(); x++) {
                for(int y = 0; y < MainActivity.getBoardHeight(); y++) {
                    fullGrid.setBounds(
                        (int)(x * gridSpace),
                        (int)(y * gridSpace),
                        (int)((x + 1) * gridSpace),
                        (int)((y + 1) * gridSpace)
                    );
                    fullGrid.draw(canvas);
                }
            }

            // Draw Roboyard logo in center
            drawables.get("roboyard").setBounds(
                (int)((MainActivity.getBoardWidth()/2 - 1)*gridSpace),
                (int)((MainActivity.getBoardHeight()/2 - 1)*gridSpace),
                (int)((MainActivity.getBoardWidth()/2 + 1)*gridSpace),
                (int)((MainActivity.getBoardHeight()/2 + 1)*gridSpace)
            );
            drawables.get("roboyard").draw(canvas);

            // Draw targets
            for (GridElement element : gridElements) {
                if (element.getType().startsWith("target_")) {
                    drawables.get(element.getType()).setBounds(
                        (int)(element.getX() * gridSpace),
                        (int)(element.getY() * gridSpace),
                        (int)((element.getX() + 1) * gridSpace),
                        (int)((element.getY() + 1) * gridSpace)
                    );
                    drawables.get(element.getType()).draw(canvas);
                }
            }

            // Calculate wall dimensions
            int pixel = Math.max(1, (int)(gridSpace / 45));
            int stretchWall = 12 * pixel;
            int offsetWall = 5 * pixel;
            int wallThickness = 2 * pixel;

            // Draw walls and robots
            for (GridElement element : gridElements) {
                if (element.getType().equals("mh")) {
                    drawables.get("mh").setBounds(
                        (int)(element.getX() * gridSpace - stretchWall),
                        (int)(element.getY() * gridSpace - stretchWall + offsetWall),
                        (int)((element.getX() + 1) * gridSpace + stretchWall),
                        (int)(element.getY() * gridSpace + wallThickness + offsetWall)
                    );
                    drawables.get("mh").draw(canvas);
                } else if (element.getType().equals("mv")) {
                    drawables.get("mv").setBounds(
                        (int)(element.getX() * gridSpace - stretchWall + offsetWall),
                        (int)(element.getY() * gridSpace - stretchWall),
                        (int)(element.getX() * gridSpace + wallThickness + offsetWall),
                        (int)((element.getY() + 1) * gridSpace + stretchWall)
                    );
                    drawables.get("mv").draw(canvas);
                } else if (element.getType().startsWith("robot_")) {
                    drawables.get(element.getType()).setBounds(
                        (int)(element.getX() * gridSpace),
                        (int)(element.getY() * gridSpace),
                        (int)((element.getX() + 1) * gridSpace),
                        (int)((element.getY() + 1) * gridSpace)
                    );
                    drawables.get(element.getType()).draw(canvas);
                }
            }

            // Save bitmap to temporary file
            File outputDir = context.getCacheDir();
            File outputFile = File.createTempFile("minimap_", ".png", outputDir);
            FileOutputStream fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Timber.e(e, "Failed to create minimap");
            return null;
        }
    }

    /**
     * Adds the saved game to the list of maps saved.
     * @param gameManager The GameManager instance.
     */
    public void addMapsSaved(GameManager gameManager) {
        if (mapPath.length() > 0) {
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if (!saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                FileReadWrite.appendPrivateData(gameManager.getActivity(), "mapsSaved.txt", mapPath + "\n");
            }
        }
    }

    /**
     * Get the map path associated with this button
     * @return the map path
     */
    public String getMapPath() {
        return mapPath;
    }

    /**
     * Get the button number associated with this button
     * @return the button number
     */
    public int getButtonNumber() {
        return buttonNumber;
    }

    /**
     * Get the parent button X position associated with this button
     * @return the parent button X position
     */
    public int getParentButtonX() {
        return parentButtonX;
    }

    /**
     * Get the parent button Y position associated with this button
     * @return the parent button Y position
     */
    public int getParentButtonY() {
        return parentButtonY;
    }

    /**
     * Get the parent button size associated with this button
     * @return the parent button size
     */
    public int getParentButtonSize() {
        return parentButtonSize;
    }

    /**
     * Get whether this button is in save mode
     * @return true if in save mode, false otherwise
     */
    public boolean isSaveMode() {
        return isSaveMode;
    }

    /**
     * Get the map name associated with this button
     * @return the map name
     */
    public String getMapName() {
        return mapPath;
    }
    
    /**
     * Get the slot ID for this button
     * @return the slot ID
     */
    public int getSlotId() {
        return buttonNumber;
    }
}
