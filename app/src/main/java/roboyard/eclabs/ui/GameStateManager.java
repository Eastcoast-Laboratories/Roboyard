package roboyard.eclabs.ui;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboyard.eclabs.Constants;
import roboyard.eclabs.FileReadWrite;
import roboyard.eclabs.GameManager;
import roboyard.eclabs.GameScreen;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.GridGameScreen;
import roboyard.eclabs.MapObjects;
import roboyard.eclabs.MainActivity;
import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;

import roboyard.eclabs.R;
import roboyard.eclabs.MapObjects;

import timber.log.Timber;

/**
 * Central state manager for the game.
 * Handles game state, navigation, and communication between fragments.
 * This replaces the previous GameManager with a more Android-native approach.
 */
public class GameStateManager extends AndroidViewModel {
    
    // Game state
    private MutableLiveData<GameState> currentState = new MutableLiveData<>();
    private MutableLiveData<Integer> moveCount = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> isGameComplete = new MutableLiveData<>(false);
    
    // Game settings
    private MutableLiveData<Integer> difficulty = new MutableLiveData<>(Constants.DIFFICULTY_NORMAL);
    private MutableLiveData<Integer> boardSize = new MutableLiveData<>(Constants.BOARD_SIZE_MEDIUM);
    private MutableLiveData<Boolean> soundEnabled = new MutableLiveData<>(true);
    
    // Solver
    private ISolver solver;
    private Context context;
    
    // Minimap
    private String currentMapName = "";
    private long startTime = 0;
    private Bitmap minimap = null;
    
    public GameStateManager(Application application) {
        super(application);
        // Initialize solver
        solver = new SolverDD();
        context = application.getApplicationContext();
    }
    
    /**
     * Start a new random game
     */
    public void startNewGame() {
        Timber.d("GameStateManager: startNewGame() called");
        
        // Create a new random game state
        GameState newState = GameState.createRandom(boardSize.getValue(), difficulty.getValue());
        Timber.d("GameStateManager: Created new random GameState");
        
        currentState.setValue(newState);
        Timber.d("GameStateManager: Set currentState LiveData value with new state");
        
        moveCount.setValue(0);
        Timber.d("GameStateManager: Reset moveCount to 0");
        
        isGameComplete.setValue(false);
        Timber.d("GameStateManager: Reset isGameComplete to false");
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        solver.init(gridElements);
    }
    
    /**
     * Load a specific level
     * @param levelId Level ID to load
     */
    public void loadLevel(int levelId) {
        // Load level from assets
        GameState newState = GameState.loadLevel(getApplication(), levelId);
        newState.setLevelId(levelId);
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        solver.init(gridElements);
    }
    
    /**
     * Load a saved game
     * @param saveId Save slot ID
     */
    public void loadGame(int saveId) {
        if (saveId >= 0) {
            // Load saved game using the original method
            GameState newState = GameState.loadSavedGame(getApplication(), saveId);
            if (newState != null) {
                currentState.setValue(newState);
                moveCount.setValue(newState.getMoveCount());
                isGameComplete.setValue(newState.isComplete());
                
                // Initialize solver with grid elements
                ArrayList<GridElement> gridElements = newState.getGridElements();
                solver.init(gridElements);
            }
        }
    }
    
    /**
     * Load a history entry
     * @param historyId History entry ID to load
     */
    public void loadHistoryEntry(int historyId) {
        // TODO: Implement history entry loading
        GameState newState = GameState.createRandom(boardSize.getValue(), difficulty.getValue());
        newState.setLevelId(historyId);
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        solver.init(gridElements);
    }
    
    /**
     * Load a history entry by its path
     * @param mapPath Path to the history entry map file
     */
    public void loadHistoryEntry(String mapPath) {
        Timber.d("Loading history entry: %s", mapPath);
        
        // Get the GameManager if available
        GameManager gameManager = getGameManager();
        if (gameManager == null) {
            Timber.e("Cannot load history entry: GameManager is null");
            return;
        }
        
        // Get the current screen
        GameScreen currentScreen = gameManager.getCurrentScreen();
        
        // If it's already a GridGameScreen, use it; otherwise switch to the game screen
        if (currentScreen instanceof GridGameScreen) {
            GridGameScreen gridScreen = (GridGameScreen) currentScreen;
            // Load the saved game
            gridScreen.setSavedGame(mapPath);
            Timber.d("Loaded history entry into existing GridGameScreen");
        } else {
            // First switch to the game screen
            gameManager.setGameScreen(Constants.SCREEN_GAME);
            
            // Then get the game screen and load the saved game
            GridGameScreen gridScreen = (GridGameScreen) gameManager.getCurrentScreen();
            gridScreen.setSavedGame(mapPath);
            Timber.d("Loaded history entry into GridGameScreen");
        }
    }
    
    /**
     * Save the current game state to a file
     * @param saveId Save slot ID
     * @return True if save was successful
     */
    public boolean saveGame(int saveId) {
        Timber.d("Attempting to save game to slot %d", saveId);
        
        // First try to get the current GridGameScreen instance from the legacy UI
        GridGameScreen gameScreen = null;
        GameManager gameManager = getGameManager();
        
        if (gameManager != null && gameManager.getCurrentScreen() instanceof GridGameScreen) {
            gameScreen = (GridGameScreen) gameManager.getCurrentScreen();
            Timber.d("Retrieved GridGameScreen from GameManager");
        } else {
            // Try with MainActivity
            MainActivity mainActivity = getMainActivity();
            if (mainActivity != null && mainActivity.getGameManager() != null 
                    && mainActivity.getGameManager().getCurrentScreen() instanceof GridGameScreen) {
                gameScreen = (GridGameScreen) mainActivity.getGameManager().getCurrentScreen();
                Timber.d("Retrieved GridGameScreen from MainActivity");
            }
        }
        
        // If we found a game screen, use it to save
        if (gameScreen != null) {
            // Use the GridGameScreen's data to save the game
            Timber.d("Using GridGameScreen data to save game to slot %d", saveId);
            String saveData = createSaveDataFromGridGameScreen(gameScreen);
            
            // Determine filename based on slot ID
            String filename;
            if (saveId == 0) {
                filename = Constants.AUTO_SAVE_FILENAME;
            } else {
                filename = Constants.SAVE_FILENAME_PREFIX + saveId + Constants.SAVE_FILENAME_EXTENSION;
            }
            
            // Save the data to file using FileReadWrite utility
            try {
                boolean success = FileReadWrite.writeStringToFile(
                        (Activity) context, saveData, Constants.SAVE_DIRECTORY, filename);
                
                if (success) {
                    Timber.d("Game saved successfully to slot %d", saveId);
                    return true;
                } else {
                    Timber.e("Failed to save game to slot %d", saveId);
                    return false;
                }
            } catch (Exception e) {
                Timber.e("Error saving game to slot %d: %s", saveId, e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            Timber.e("Cannot find GridGameScreen to save from");
        }
        
        // Fallback to using GameState if GridGameScreen is not available
        GameState state = currentState.getValue();
        if (state != null) {
            Timber.d("Falling back to using GameState to save game");
            return state.saveToFile(getApplication(), saveId);
        }
        
        Timber.e("Failed to save game: No valid GameState or GridGameScreen available");
        return false;
    }
    
    /**
     * Create save data from the current game state
     * @param gameScreen The current GridGameScreen
     * @return The save data as a string
     */
    private String createSaveDataFromGridGameScreen(GridGameScreen gameScreen) {
        StringBuilder saveData = new StringBuilder();
        
        // Add metadata as a comment line
        String mapName = gameScreen.getMapName();
        int timeCount = gameScreen.getTimeCount();
        int moveCount = gameScreen.getMoveCount();
        
        // Format: #MAPNAME:name;TIME:seconds;MOVES:count;
        saveData.append("#MAPNAME:").append(mapName).append(";");
        saveData.append("TIME:").append(timeCount).append(";");
        saveData.append("MOVES:").append(moveCount).append(";\n");
        
        // Add the map data
        List<GridElement> gridElements = gameScreen.getGridElements();
        saveData.append(MapObjects.createDataString(gridElements));
        
        return saveData.toString();
    }
    
    /**
     * Extract metadata from save data
     * @param saveData The save data string
     * @return A map containing the metadata or null if no metadata was found
     */
    public static Map<String, String> extractMetadataFromSaveData(String saveData) {
        Map<String, String> metadata = new HashMap<>();
        
        // Check if the save data has a metadata line
        if (saveData != null && saveData.startsWith("#")) {
            // Extract the first line
            int endOfFirstLine = saveData.indexOf('\n');
            if (endOfFirstLine > 0) {
                String metadataLine = saveData.substring(1, endOfFirstLine);
                
                // Parse metadata entries (MAPNAME:name;TIME:seconds;MOVES:count;)
                String[] entries = metadataLine.split(";");
                for (String entry : entries) {
                    String[] keyValue = entry.split(":");
                    if (keyValue.length == 2) {
                        metadata.put(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
        
        return metadata;
    }
    
    /**
     * Handle a touch on the game grid
     * @param x Grid X coordinate
     * @param y Grid Y coordinate
     * @param action Touch action type
     */
    public void handleGridTouch(int x, int y, int action) {
        GameState state = currentState.getValue();
        if (state != null && action == MotionEvent.ACTION_UP) {
            // Find if a robot was touched
            GameElement touchedRobot = state.getRobotAt(x, y);
            if (touchedRobot != null) {
                // Robot was touched, update selection
                state.setSelectedRobot(touchedRobot);
                currentState.setValue(state);
            } else if (state.getSelectedRobot() != null) {
                // Try to move selected robot to this location
                boolean moved = state.moveRobotTo(state.getSelectedRobot(), x, y);
                if (moved) {
                    // Update move count
                    incrementMoveCount();
                    
                    // Check if game is complete
                    if (state.checkCompletion()) {
                        setGameComplete(true);
                    }
                    
                    currentState.setValue(state);
                }
            }
        }
    }
    
    /**
     * Handle deep link to a specific level
     * @param levelId Level ID to load
     */
    public void handleDeepLink(int levelId) {
        loadLevel(levelId);
    }
    
    /**
     * Get a hint for the next move
     * @return The next move according to the solver, or null if no solution exists
     */
    public IGameMove getHint() {
        GameState state = currentState.getValue();
        if (state == null) return null;
        
        // Run the solver if it hasn't already run
        solver.run();
        
        // Get the first solution
        GameSolution solution = solver.getSolution(0);
        if (solution == null || solution.getMoves() == null || solution.getMoves().isEmpty()) {
            return null;
        }
        
        // Get the first move
        return solution.getMoves().get(0);
    }
    
    /**
     * Navigate to specific level screen (beginner, intermediate, advanced, expert)
     * @param screenId Screen ID from Constants (SCREEN_LEVEL_BEGINNER, etc)
     */
    public void navigateToLevelScreen(int screenId) {
        // Store the selected level screen for the GamePlayFragment to use
        MutableLiveData<Integer> levelScreen = new MutableLiveData<>(screenId);
        
        // The GamePlayFragment will check this value and load the appropriate level screen
        // This corresponds to the Constants.SCREEN_LEVEL_BEGINNER etc. values
        switch (screenId) {
            case Constants.SCREEN_LEVEL_BEGINNER:
                // Load first beginner level (level 1)
                loadLevel(1);
                break;
            case Constants.SCREEN_LEVEL_INTERMEDIATE:
                // Load first intermediate level (level 36)
                loadLevel(36);
                break;
            case Constants.SCREEN_LEVEL_ADVANCED:
                // Load first advanced level (level 71)
                loadLevel(71);
                break;
            case Constants.SCREEN_LEVEL_EXPERT:
                // Load first expert level (level 106)
                loadLevel(106);
                break;
            default:
                // Default to beginner level 1
                loadLevel(1);
                break;
        }
    }
    
    /**
     * Navigate to main menu
     */
    public void navigateToMainMenu() {
        Timber.d("GameStateManager: navigateToMainMenu() called");
        // Use the NavController to navigate to the main menu fragment
        NavController navController = Navigation.findNavController(
                androidx.fragment.app.FragmentActivity.class.cast(context), R.id.nav_host_fragment);
        navController.navigate(R.id.actionGlobalMainMenu);
    }
    
    /**
     * Navigate to settings screen
     */
    public void navigateToSettings() {
        Timber.d("GameStateManager: navigateToSettings() called");
        // Use the NavController to navigate to the settings fragment
        NavController navController = Navigation.findNavController(
                androidx.fragment.app.FragmentActivity.class.cast(context), R.id.nav_host_fragment);
        navController.navigate(R.id.actionGlobalSettings);
    }
    
    /**
     * Navigate to save screen
     * @param saveMode True for save mode, false for load mode
     */
    public void navigateToSaveScreen(boolean saveMode) {
        Timber.d("GameStateManager: navigateToSaveScreen() called with saveMode=%s", saveMode);
        
        // Get the current fragment that is calling this method
        androidx.fragment.app.FragmentActivity activity = androidx.fragment.app.FragmentActivity.class.cast(context);
        
        // Check if we're on the main thread
        if (android.os.Looper.getMainLooper().getThread() == Thread.currentThread()) {
            // We're on the main thread, safe to navigate
            performNavigation(activity, saveMode);
        } else {
            // We're on a background thread, post navigation to main thread
            Timber.d("GameStateManager: Posting navigation to main thread");
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Timber.d("GameStateManager: Now on main thread, proceeding with navigation");
                performNavigation(activity, saveMode);
            });
        }
    }
    
    /**
     * Perform the actual navigation on the main thread
     */
    private void performNavigation(androidx.fragment.app.FragmentActivity activity, boolean saveMode) {
        try {
            // Create a bundle to pass the save mode parameter
            android.os.Bundle args = new android.os.Bundle();
            args.putBoolean("saveMode", saveMode);
            
            // Find the NavController for the current fragment
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(
                    activity, R.id.nav_host_fragment);
            
            // Get the current destination ID
            int currentDestId = navController.getCurrentDestination().getId();
            Timber.d("Current destination ID: %d", currentDestId);
            
            // Choose the appropriate action based on the current fragment
            if (currentDestId == R.id.gameCanvasFragment) {
                Timber.d("Navigating from GameCanvasFragment to SaveGame");
                navController.navigate(R.id.actionGameToSaveGame, args);
            } else if (currentDestId == R.id.gamePlayFragment) {
                Timber.d("Navigating from GamePlayFragment to SaveGame");
                navController.navigate(R.id.actionGamePlayToSaveGame, args);
            } else {
                // For other fragments, use the global action
                Timber.d("Using global action to navigate to SaveGame");
                navController.navigate(R.id.actionGlobalSaveGame, args);
            }
        } catch (Exception e) {
            // Log any navigation errors
            Timber.e(e, "Navigation error");
        }
    }
    
    /**
     * Update the context to a valid activity context for fragment navigation
     * @param activityContext The activity context
     */
    public void updateContext(Context activityContext) {
        if (activityContext instanceof androidx.fragment.app.FragmentActivity) {
            Timber.d("GameStateManager: Updating context to activity: %s", activityContext.getClass().getSimpleName());
            this.context = activityContext;
        }
    }
    
    /**
     * Helper method to get the MainActivity instance
     */
    private MainActivity getMainActivity() {
        Context context = getApplication();
        if (context instanceof MainActivity) {
            return (MainActivity) context;
        }
        return null;
    }
    
    /**
     * Get the GameManager instance from MainActivity
     * Note: This will only work if the application context is MainActivity
     * @return GameManager instance or null if not available
     */
    public GameManager getGameManager() {
        if (context instanceof MainActivity) {
            return ((MainActivity) context).getGameManager();
        } else if (context != null) {
            // Try to get the Application Context in case we have a wrapped context
            Context appContext = context.getApplicationContext();
            if (appContext instanceof MainActivity) {
                return ((MainActivity) appContext).getGameManager();
            }
            
            // If we have an activity context, try to cast it directly
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (activity instanceof MainActivity) {
                    return ((MainActivity) activity).getGameManager();
                }
            }
        }
        
        Timber.w("Cannot get GameManager: application context is not MainActivity");
        return null;
    }
    
    /**
     * Getters for LiveData to observe
     */
    public LiveData<GameState> getCurrentState() { return currentState; }
    public LiveData<Integer> getMoveCount() { return moveCount; }
    public LiveData<Boolean> isGameComplete() { return isGameComplete; }
    public LiveData<Integer> getDifficulty() { return difficulty; }
    public LiveData<Integer> getBoardSize() { return boardSize; }
    public LiveData<Boolean> getSoundEnabled() { return soundEnabled; }
    
    /**
     * Setters for game settings
     */
    public void setDifficulty(int difficulty) { this.difficulty.setValue(difficulty); }
    public void setBoardSize(int boardSize) { this.boardSize.setValue(boardSize); }
    public void setSoundEnabled(boolean enabled) { this.soundEnabled.setValue(enabled); }
    
    /**
     * Increment the move count
     */
    public void incrementMoveCount() {
        Integer currentCount = moveCount.getValue();
        if (currentCount != null) {
            moveCount.setValue(currentCount + 1);
        } else {
            moveCount.setValue(1);
        }
    }
    
    /**
     * Set the move count
     * @param count New move count
     */
    public void setMoveCount(int count) {
        moveCount.setValue(count);
    }
    
    /**
     * Set whether the game is complete
     * @param complete Whether the game is complete
     */
    public void setGameComplete(boolean complete) {
        isGameComplete.setValue(complete);
    }
    
    /**
     * Get the current context
     */
    public Context getContext() {
        return getApplication().getApplicationContext();
    }

    /**
     * Load a saved game from a specific slot
     * @param context The context
     * @param slotId The save slot ID
     * @return True if successful, false otherwise
     */
    public boolean loadSavedGame(Context context, int slotId) {
        try {
            // Get save file path
            String savePath = FileReadWrite.getSaveGamePath((Activity) context, slotId);
            String saveData = FileReadWrite.loadAbsoluteData(savePath);
            
            if (saveData != null && !saveData.isEmpty()) {
                // Extract metadata
                Map<String, String> metadata = extractMetadataFromSaveData(saveData);
                
                // Store metadata for access by other methods
                if (metadata != null) {
                    if (metadata.containsKey("MAPNAME")) {
                        this.currentMapName = metadata.get("MAPNAME");
                    }
                    
                    if (metadata.containsKey("TIME")) {
                        try {
                            this.startTime = Long.parseLong(metadata.get("TIME"));
                        } catch (NumberFormatException e) {
                            Timber.e("Invalid time format: %s", metadata.get("TIME"));
                        }
                    }
                }
                
                // Create minimap for this save
                try {
                    this.minimap = createMinimapFromSaveData(context, saveData);
                } catch (Exception e) {
                    Timber.e(e, "Error creating minimap for slot %d", slotId);
                }
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Timber.e(e, "Error loading saved game from slot %d", slotId);
            return false;
        }
    }
    
    /**
     * Get the level name
     * @return The current level name
     */
    public String getLevelName() {
        return currentMapName;
    }
    
    /**
     * Get the start time
     * @return The start time as a long value
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Create a minimap from save data
     * @param context The context
     * @param saveData The save data
     * @return The minimap bitmap
     */
    private Bitmap createMinimapFromSaveData(Context context, String saveData) {
        // Skip metadata line if present
        if (saveData.startsWith("#")) {
            int newlineIndex = saveData.indexOf('\n');
            if (newlineIndex >= 0) {
                saveData = saveData.substring(newlineIndex + 1);
            }
        }
        
        // Extract grid elements
        List<GridElement> elements = MapObjects.extractDataFromString(saveData, true);
        
        // Create a simple minimap
        return createMinimap(context, elements, 100, 100);
    }
    
    /**
     * Get a minimap for the current game state
     * @param context The context
     * @param width The minimap width
     * @param height The minimap height
     * @return The minimap bitmap
     */
    public Bitmap getMiniMap(Context context, int width, int height) {
        if (minimap != null) {
            return minimap;
        }
        
        // If no minimap is available, create a placeholder
        Bitmap placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(placeholder);
        canvas.drawColor(Color.LTGRAY);
        return placeholder;
    }
    
    /**
     * Create a minimap from grid elements
     * @param context The context
     * @param elements The grid elements
     * @param width The minimap width
     * @param height The minimap height
     * @return The minimap bitmap
     */
    private Bitmap createMinimap(Context context, List<GridElement> elements, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        
        // If no elements, return empty bitmap
        if (elements == null || elements.isEmpty()) {
            return bitmap;
        }
        
        // Determine grid bounds
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;
        
        for (GridElement element : elements) {
            int x = element.getX();
            int y = element.getY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        
        // Calculate scaling
        float gridWidth = maxX - minX + 2; // Add padding
        float gridHeight = maxY - minY + 2;
        float scaleX = width / gridWidth;
        float scaleY = height / gridHeight;
        float scale = Math.min(scaleX, scaleY);
        
        // Draw elements
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        for (GridElement element : elements) {
            int x = element.getX() - minX + 1; // Add padding
            int y = element.getY() - minY + 1;
            float left = x * scale;
            float top = y * scale;
            float right = (x + 1) * scale;
            float bottom = (y + 1) * scale;
            
            if (element.getType().startsWith("robot_")) {
                // Draw robots
                if (element.getType().contains("red")) {
                    paint.setColor(Color.RED);
                } else if (element.getType().contains("blue")) {
                    paint.setColor(Color.BLUE);
                } else if (element.getType().contains("green")) {
                    paint.setColor(Color.GREEN);
                } else if (element.getType().contains("yellow")) {
                    paint.setColor(Color.YELLOW);
                } else {
                    paint.setColor(Color.GRAY);
                }
                canvas.drawCircle((left + right) / 2, (top + bottom) / 2, scale / 2, paint);
            } else if (element.getType().startsWith("target_")) {
                // Draw targets
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                if (element.getType().contains("red")) {
                    paint.setColor(Color.RED);
                } else if (element.getType().contains("blue")) {
                    paint.setColor(Color.BLUE);
                } else if (element.getType().contains("green")) {
                    paint.setColor(Color.GREEN);
                } else if (element.getType().contains("yellow")) {
                    paint.setColor(Color.YELLOW);
                } else {
                    paint.setColor(Color.GRAY);
                }
                canvas.drawRect(left, top, right, bottom, paint);
                paint.setStyle(Paint.Style.FILL);
            } else if (element.getType().startsWith("m")) {
                // Draw walls
                paint.setColor(Color.DKGRAY);
                if (element.getType().equals("mh")) { // Horizontal wall
                    canvas.drawRect(left, top + scale/3, right, bottom - scale/3, paint);
                } else if (element.getType().equals("mv")) { // Vertical wall
                    canvas.drawRect(left + scale/3, top, right - scale/3, bottom, paint);
                }
            }
        }
        
        return bitmap;
    }
}
