package roboyard.eclabs.ui;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import roboyard.eclabs.Constants;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.FileReadWrite;
import roboyard.eclabs.MainActivity;
import roboyard.eclabs.MapGenerator;
import roboyard.eclabs.MapObjects;
import roboyard.eclabs.R;
import roboyard.eclabs.util.SolverManager;
import roboyard.eclabs.util.BoardSizeManager;
import roboyard.eclabs.util.BrailleSpinner;
import roboyard.eclabs.util.DifficultyManager;
import roboyard.eclabs.util.SolutionAnimator;
import roboyard.eclabs.util.UIModeManager;
import roboyard.pm.ia.GameSolution;
import roboyard.eclabs.GameLogic;
import roboyard.pm.ia.IGameMove;
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
    private MutableLiveData<Integer> squaresMoved = new MutableLiveData<>(0); 
    private MutableLiveData<Boolean> isGameComplete = new MutableLiveData<>(false);
    
    // Move history for undo functionality
    private ArrayList<GameState> stateHistory = new ArrayList<>();
    private ArrayList<Integer> squaresMovedHistory = new ArrayList<>();
    
    // Game settings
    private MutableLiveData<Boolean> soundEnabled = new MutableLiveData<>(true);
    
    // Solver
    private SolverManager solver;
    private Context context;
    
    // Minimap
    private String currentMapName = "";
    private long startTime = 0;
    private Bitmap minimap = null;
    
    // Board size manager
    private BoardSizeManager boardSizeManager;
    
    // Difficulty manager
    private DifficultyManager difficultyManager;
    
    // UI mode manager
    private UIModeManager uiModeManager;
    
    public GameStateManager(Application application) {
        super(application);
        // Initialize solver
        solver = new SolverManager();
        
        // Set solver listener to handle results
        solver.setListener(new SolverManager.SolverListener() {
            @Override
            public void onSolverFinished(boolean success, int solutionMoves, int numSolutions) {
                // Process on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (success) {
                        GameSolution solution = solver.getCurrentSolution();
                        onSolutionCalculationCompleted(solution);
                    } else {
                        onSolutionCalculationFailed("No solution found");
                    }
                });
            }
            
            @Override
            public void onSolverCancelled() {
                // Process on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    onSolutionCalculationFailed("Solver was cancelled");
                });
            }
        });
        
        context = application.getApplicationContext();
        
        // Initialize board size manager
        boardSizeManager = BoardSizeManager.getInstance(context);
        
        // Initialize difficulty manager
        difficultyManager = DifficultyManager.getInstance(context);
        
        // Initialize UI mode manager
        uiModeManager = UIModeManager.getInstance(context);
    }
    
    /**
     * Start a new random game
     */
    public void startNewGame() {
        Timber.d("GameStateManager: startNewGame() called");
        
        // Get board dimensions from BoardSizeManager
        int width = boardSizeManager.getBoardWidth();
        int height = boardSizeManager.getBoardHeight();
        
        // Create a new random game state
        GameState newState = GameState.createRandom(width, height, difficultyManager.getDifficulty());
        Timber.d("GameStateManager: Created new random GameState");
        
        currentState.setValue(newState);
        Timber.d("GameStateManager: Set currentState LiveData value with new state");
        
        moveCount.setValue(0);
        Timber.d("GameStateManager: Reset moveCount to 0");
        
        isGameComplete.setValue(false);
        Timber.d("GameStateManager: Reset isGameComplete to false");
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        solver.initialize(gridElements);
    }
    
    /**
     * Start a new random game with the modern UI
     * This is similar to startNewGame() but ensures the game is displayed in the modern UI
     */
    public void startModernGame() {
        Timber.d("GameStateManager: startModernGame() called");
        
        // Get board dimensions from BoardSizeManager
        int width = boardSizeManager.getBoardWidth();
        int height = boardSizeManager.getBoardHeight();
        
        Timber.d("[BOARD_SIZE_DEBUG] GameStateManager.startModernGame - Retrieved board size from BoardSizeManager: %dx%d", width, height);
        Timber.d("[BOARD_SIZE_DEBUG] GameStateManager.startModernGame - Current MainActivity board size: %dx%d", 
                MainActivity.boardSizeX, MainActivity.boardSizeY);
        
        // Create a new random game state
        Timber.d("[BOARD_SIZE_DEBUG] GameStateManager.startModernGame - Before calling createRandom with size: %dx%d", width, height);
        GameState newState = GameState.createRandom(width, height, difficultyManager.getDifficulty());
        Timber.d("GameStateManager: Created new random GameState for modern UI");
        
        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this);
        
        // Verify the dimensions of the created game state
        if (newState != null) {
            Timber.d("[BOARD_SIZE_DEBUG] GameStateManager.startModernGame - Created GameState has dimensions: %dx%d", 
                    newState.getWidth(), newState.getHeight());
            
            // Check if dimensions match what was requested
            if (newState.getWidth() != width || newState.getHeight() != height) {
                Timber.w("[BOARD_SIZE_DEBUG] GameStateManager.startModernGame - WARNING: Created game dimensions don't match requested dimensions!");
            }
        }
        
        // Set a flag to indicate this is a modern UI game
        newState.setLevelName("Modern UI Game");
        
        // Store initial robot positions for reset functionality
        newState.storeInitialRobotPositions();
        
        // Reset the state history and move counts
        stateHistory.clear();
        squaresMovedHistory.clear();
        currentState.setValue(newState);
        moveCount.setValue(0);
        squaresMoved.setValue(0);
        isGameComplete.setValue(false);
        
        Timber.d("GameStateManager: Reset move counts and state history");
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        solver.initialize(gridElements);
        
        // Set UI mode to modern
        uiModeManager.setUIMode(UIModeManager.MODE_MODERN);
    }
    
    /**
     * Load a specific level
     * @param levelId Level ID to load
     */
    public void loadLevel(int levelId) {
        // Load level from assets
        GameState newState = GameState.loadLevel(getApplication(), levelId);
        newState.setLevelId(levelId);
        
        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this);
        
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        solver.initialize(gridElements);
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
                // Set reference to this GameStateManager in the new state
                newState.setGameStateManager(this);
                
                currentState.setValue(newState);
                moveCount.setValue(newState.getMoveCount());
                isGameComplete.setValue(newState.isComplete());
                
                // Initialize solver with grid elements
                ArrayList<GridElement> gridElements = newState.getGridElements();
                solver.initialize(gridElements);
            }
        }
    }
    
    /**
     * Load a history entry
     * @param historyId History entry ID to load
     */
    public void loadHistoryEntry(int historyId) {
        // TODO: Implement history entry loading
        GameState newState = GameState.createRandom(boardSizeManager.getBoardWidth(), boardSizeManager.getBoardHeight(), difficultyManager.getDifficulty());
        newState.setLevelId(historyId);
        
        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this);
        
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        solver.initialize(gridElements);
    }
    
    /**
     * Load a game from a history entry
     * @param mapPath Path to the history entry file
     */
    public void loadHistoryEntry(String mapPath) {
        Timber.d("Loading history entry: %s", mapPath);
        
        try {
            // Load game state from file
            File historyFile = new File(mapPath);
            if (!historyFile.exists()) {
                Timber.e("History file does not exist: %s", mapPath);
                return;
            }
            
            // TODO: Implement proper loading from history file
            // For now, just start a new game with current settings
            startModernGame();
            
            Timber.d("Loaded history entry");
        } catch (Exception e) {
            Timber.e(e, "Error loading history entry: %s", mapPath);
        }
    }
    
    /**
     * Save the current game to a slot
     * @param saveId The save slot ID
     * @return true if the game was saved successfully, false otherwise
     */
    public boolean saveGame(int saveId) {
        Timber.d("Saving game to slot %d", saveId);
        
        // Get the current game state
        GameState gameState = currentState.getValue();
        if (gameState == null) {
            Timber.e("Cannot save game: No valid GameState available");
            // Try to debug why the current state is null
            Timber.d("Current state: %s", currentState == null ? "null MutableLiveData" : "MutableLiveData exists but value is null");
            Timber.d("Has saved state history: %s", stateHistory.isEmpty() ? "no" : "yes, with " + stateHistory.size() + " entries");
            return false;
        }
        
        try {
            // Create save directory if it doesn't exist
            File saveDir = new File(getContext().getFilesDir(), Constants.SAVE_DIRECTORY);
            if (!saveDir.exists()) {
                if (!saveDir.mkdirs()) {
                    Timber.e("Failed to create save directory");
                    return false;
                }
            }
            
            // Create save file
            String fileName = Constants.SAVE_FILENAME_PREFIX + saveId + Constants.SAVE_FILENAME_EXTENSION;
            File saveFile = new File(saveDir, fileName);
            
            // Serialize game state to JSON
            String saveData = gameState.serialize();
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                fos.write(saveData.getBytes());
                fos.flush();
                Timber.d("Game saved successfully to slot %d", saveId);
                return true;
            }
        } catch (IOException e) {
            Timber.e(e, "Error saving game to slot %d", saveId);
            return false;
        }
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
                    
                    // Get how many squares were moved and update counter
                    int squaresMovedInThisMove = state.getLastSquaresMoved();
                    addSquaresMoved(squaresMovedInThisMove);
                    
                    // Create a copy of the state for history
                    GameState stateCopy = null;
                    try {
                        // Serialize and deserialize for deep copy
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(state);
                        oos.flush();
                        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        stateCopy = (GameState) ois.readObject();
                        ois.close();
                    } catch (Exception e) {
                        Timber.e(e, "Error creating a deep copy of GameState for history");
                        stateCopy = state; // Fallback to reference copy if deep copy fails
                    }
                    
                    // Add current state to history
                    stateHistory.add(stateCopy);
                    squaresMovedHistory.add(squaresMoved.getValue());
                    
                    // Check if game is complete
                    boolean isComplete = state.checkCompletion();
                    if (isComplete) {
                        // Set the game as complete - this will trigger the observers
                        // in ModernGameFragment that show the toast notification
                        setGameComplete(true);
                        Timber.d("Game completed! Moves: %d, Squares moved: %d", 
                            moveCount.getValue(), squaresMoved.getValue());
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
        
        try {
            // Initialize the solver with the current game state
            solver.initialize(state.getGridElements());
            
            // Run the solver synchronously for hints
            solver.run();
            
            // Check if a solution was found
            if (solver.getNumDifferentSolutionsFound() > 0) {
                // Get the solution
                GameSolution solution = solver.getCurrentSolution();
                if (solution != null && solution.getMoves() != null && !solution.getMoves().isEmpty()) {
                    return solution.getMoves().get(0);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error getting hint");
        }
        
        return null;
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
        Timber.d("Navigating to save screen, saveMode=%s", saveMode);
        // Check if we have a valid state before navigation
        GameState currentGameState = currentState.getValue();
        Timber.d("Current state before navigation: %s", currentGameState != null ? "valid" : "null");
        
        if (currentGameState == null) {
            Timber.e("Cannot navigate to save screen: No valid GameState available");
            // Show a toast notification
            Toast.makeText(context, "No game available to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // We should only proceed if the context is a FragmentActivity
        if (!(context instanceof androidx.fragment.app.FragmentActivity)) {
            Timber.e("Cannot navigate to save screen: context is not a FragmentActivity");
            // Toast to inform the user
            if (context != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Cannot navigate to save screen", Toast.LENGTH_SHORT).show();
                });
            }
            return;
        }
        
        // Get the current activity context
        androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;
        
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
     * Get the context from application
     * @return Application context
     */
    private Context getContext() {
        return getApplication().getApplicationContext();
    }
    
    /**
     * Getters for LiveData to observe
     */
    public LiveData<GameState> getCurrentState() { return currentState; }
    public LiveData<Integer> getMoveCount() { return moveCount; }
    public LiveData<Integer> getSquaresMoved() { return squaresMoved; }
    public LiveData<Boolean> isGameComplete() { return isGameComplete; }
    public LiveData<Boolean> getSoundEnabled() { return soundEnabled; }
    
    /**
     * Get the board width from BoardSizeManager
     * @return Current board width
     */
    public int getBoardWidth() {
        return boardSizeManager.getBoardWidth();
    }
    
    /**
     * Get the board height from BoardSizeManager
     * @return Current board height
     */
    public int getBoardHeight() {
        return boardSizeManager.getBoardHeight();
    }
    
    /**
     * Set the board width in BoardSizeManager
     * @param width New board width
     */
    public void setBoardWidth(int width) {
        // Use setBoardSize with current height
        boardSizeManager.setBoardSize(width, getBoardHeight());
        Timber.d("[BOARD_SIZE_DEBUG] GameStateManager.setBoardWidth() - Set width to %d", width);
    }
    
    /**
     * Set the board height in BoardSizeManager
     * @param height New board height
     */
    public void setBoardHeight(int height) {
        // Use setBoardSize with current width
        boardSizeManager.setBoardSize(getBoardWidth(), height);
        Timber.d("[BOARD_SIZE_DEBUG] GameStateManager.setBoardHeight() - Set height to %d", height);
    }
    
    /**
     * Set both board width and height in BoardSizeManager
     * @param width New board width
     * @param height New board height
     */
    public void setBoardSize(int width, int height) {
        boardSizeManager.setBoardSize(width, height);
    }
    
    /**
     * Setters for game settings
     */
    public void setSoundEnabled(boolean enabled) { this.soundEnabled.setValue(enabled); }
    
    /**
     * Increment the move count
     */
    public void incrementMoveCount() {
        Integer currentCount = moveCount.getValue();
        if (currentCount != null) {
            moveCount.setValue(currentCount + 1);
        }
    }
    
    /**
     * Undo the last move if possible
     * @return true if a move was undone, false otherwise
     */
    public boolean undoLastMove() {
        // Check if there's anything to undo
        if (stateHistory.isEmpty()) {
            return false;
        }
        
        // Get the previous state and restore it
        GameState previousState = stateHistory.remove(stateHistory.size() - 1);
        if (previousState != null) {
            // Also restore the squares moved count
            if (!squaresMovedHistory.isEmpty()) {
                squaresMoved.setValue(squaresMovedHistory.remove(squaresMovedHistory.size() - 1));
            }
            
            // Restore the state
            currentState.setValue(previousState);
            
            // Decrement move count
            int moves = moveCount.getValue();
            moveCount.setValue(Math.max(0, moves - 1));
            
            // Reset game complete flag if it was set
            if (isGameComplete.getValue()) {
                isGameComplete.setValue(false);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Set the move count
     * @param count New move count
     */
    public void setMoveCount(int count) {
        moveCount.setValue(count);
    }
    
    /**
     * Add squares moved to the counter
     * @param squares Number of squares moved
     */
    public void addSquaresMoved(int squares) {
        Integer current = squaresMoved.getValue();
        if (current != null) {
            squaresMoved.setValue(current + squares);
        }
    }
    
    /**
     * Reset squares moved counter
     */
    public void resetSquaresMoved() {
        squaresMoved.setValue(0);
        squaresMovedHistory.clear();
    }
    
    /**
     * Set the squares moved count
     * @param squares Number of squares moved
     */
    public void setSquaresMoved(int squares) {
        squaresMoved.setValue(squares);
    }
    
    /**
     * Set whether the game is complete
     * @param complete Whether the game is complete
     */
    public void setGameComplete(boolean complete) {
        Timber.d("GameStateManager: setGameComplete(%s)", complete);
        if (complete) {
            // If game is newly completed, show toast notification
            if (Boolean.FALSE.equals(isGameComplete.getValue())) {
                // Show toast on the main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        Toast.makeText(context, 
                            "Target reached in " + moveCount.getValue() + " moves and " + 
                            squaresMoved.getValue() + " squares moved!", 
                            Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Timber.e(e, "Error showing completion toast");
                    }
                });
            }
        }
        isGameComplete.setValue(complete);
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
                paint.setColor(Color.RED);
                canvas.drawCircle((left + right) / 2, (top + bottom) / 2, scale / 2, paint);
            } else if (element.getType().startsWith("target_")) {
                // Draw targets
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(Color.RED);
                canvas.drawRect(left, top, right, bottom, paint);
                paint.setStyle(Paint.Style.FILL);
            } else if (element.getType().equals("mv")) {
                // Draw walls
                paint.setColor(Color.DKGRAY);
                canvas.drawRect(left + scale/3, top, right - scale/3, bottom, paint);
            } else if (element.getType().equals("mh")) {
                // Draw walls
                paint.setColor(Color.DKGRAY);
                canvas.drawRect(left, top + scale/3, right, bottom - scale/3, paint);
            }
        }
        
        return bitmap;
    }

    /**
     * Get the current difficulty level from DifficultyManager
     * @return Current difficulty level
     */
    public int getDifficulty() {
        return difficultyManager.getDifficulty();
    }
    
    /**
     * Get a string representation of the current difficulty level
     * @return String representation of the current difficulty level
     */
    public String getDifficultyString() {
        return difficultyManager.getDifficultyString();
    }
    
    /**
     * Set the difficulty level in DifficultyManager
     * @param difficulty New difficulty level
     */
    public void setDifficulty(int difficulty) {
        difficultyManager.setDifficulty(difficulty);
    }

    /**
     * Save the current map for later use
     * @return true if the map was saved successfully, false otherwise
     */
    public boolean saveCurrentMap() {
        GameState state = currentState.getValue();
        if (state == null) {
            return false;
        }
        
        try {
            // Create a timestamp for the map name
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String mapName = "custom_map_" + timestamp;
            
            // Get the save directory
            File appDir = new File(context.getFilesDir(), "maps");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            // Create the map file
            File mapFile = new File(appDir, mapName + ".map");
            
            // Write the current state to the map file
            FileWriter writer = new FileWriter(mapFile);
            
            // Write metadata
            writer.write("#MAPNAME:" + mapName + ";TIME:" + System.currentTimeMillis() + ";\n");
            
            // Write board dimensions
            writer.write(state.getWidth() + " " + state.getHeight() + "\n");
            
            // Write the grid elements (walls, robots, targets)
            ArrayList<GridElement> elements = state.getGridElements();
            for (GridElement element : elements) {
                // Format: type x y [color]
                StringBuilder line = new StringBuilder();
                
                // Add type code based on element type
                String type = element.getType();
                if (type.startsWith("robot_")) {
                    line.append("rb");
                } else if (type.startsWith("target_")) {
                    line.append("tg");
                } else if (type.equals("mv")) {
                    line.append("mv");
                } else if (type.equals("mh")) {
                    line.append("mh");
                } else {
                    continue; // Skip unknown elements
                }
                
                // Add position
                line.append(" ").append(element.getX())
                    .append(" ").append(element.getY());
                
                // Add color for robots and targets
                if (type.startsWith("robot_") || type.startsWith("target_")) {
                    // Extract color from type (e.g., "robot_red" -> "red")
                    String color = type.substring(type.indexOf("_") + 1);
                    int colorCode = 0; // Default to black
                    
                    // Convert color string to code
                    if (color.equals("red")) {
                        colorCode = 1;
                    } else if (color.equals("green")) {
                        colorCode = 2;
                    } else if (color.equals("blue")) {
                        colorCode = 3;
                    } else if (color.equals("yellow")) {
                        colorCode = 4;
                    }
                    
                    line.append(" ").append(colorCode);
                }
                
                // Write the line
                writer.write(line.toString() + "\n");
            }
            
            writer.close();
            
            // Show a toast notification
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Map saved as " + mapName, Toast.LENGTH_SHORT).show();
                });
            }
            
            Timber.d("GameStateManager: Map saved as %s", mapName);
            return true;
        } catch (IOException e) {
            Timber.e(e, "Error saving map");
            return false;
        }
    }

    /**
     * Reset robots to their starting positions without changing the map
     * This keeps the same map but resets robot positions and move counters
     */
    public void resetRobots() {
        GameState currentGameState = currentState.getValue();
        if (currentGameState == null) {
            return;
        }
        
        try {
            // Create a copy of the current state for reset
            GameState resetState = null;
            try {
                // Serialize and deserialize for deep copy
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(currentGameState);
                oos.flush();
                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bis);
                resetState = (GameState) ois.readObject();
                ois.close();
            } catch (Exception e) {
                Timber.e(e, "Error creating a deep copy of GameState for reset");
                return; // Can't proceed without a valid copy
            }
            
            // Reset robot positions by moving them back to original positions
            resetState.resetRobotPositions();
            
            // Reset counters
            moveCount.setValue(0);
            squaresMoved.setValue(0);
            isGameComplete.setValue(false);
            
            // Clear history
            stateHistory.clear();
            squaresMovedHistory.clear();
            
            // Update state
            currentState.setValue(resetState);
            
            // Show a toast notification
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Robots reset to starting positions", Toast.LENGTH_SHORT).show();
                });
            }
            
            Timber.d("GameStateManager: Robots reset to starting positions");
        } catch (Exception e) {
            Timber.e(e, "Error resetting robots");
        }
    }

    /**
     * Asynchronously calculates the solution for the current game state
     * and returns the result via callback
     * 
     * @param callback The callback to receive the solution when it's ready
     */
    public void calculateSolutionAsync(final SolutionCallback callback) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Show progress indicator
        if (callback != null) {
            callback.onSolutionCalculationStarted();
        }
        
        executor.execute(() -> {
            // Initialize the solver with the current game state
            GameState state = currentState.getValue();
            if (state == null) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onSolutionCalculationFailed("No active game state"));
                }
                return;
            }
            
            try {
                // Store the callback for later use
                this.solutionCallback = callback;
                
                // Initialize solver with current state
                solver.initialize(state.getGridElements());
                
                // Run solver in a background thread
                Thread solverThread = new Thread(solver, "solver-thread");
                solverThread.start();
                
                // The listener will handle completion
            } catch (Exception e) {
                Timber.e(e, "Error calculating solution");
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onSolutionCalculationFailed("Error: " + e.getMessage()));
                }
            }
        });
    }
    
    // Field to store the current solution callback
    private SolutionCallback solutionCallback;
    
    /**
     * Called when the solution calculation starts
     */
    private void onSolutionCalculationStarted() {
        if (solutionCallback != null) {
            solutionCallback.onSolutionCalculationStarted();
        }
    }
    
    /**
     * Called when the solution calculation completes successfully
     * @param solution The calculated solution
     */
    private void onSolutionCalculationCompleted(GameSolution solution) {
        if (solutionCallback != null) {
            solutionCallback.onSolutionCalculationCompleted(solution);
            solutionCallback = null; // Clear the reference
        }
    }
    
    /**
     * Called when the solution calculation fails
     * @param errorMessage The error message
     */
    private void onSolutionCalculationFailed(String errorMessage) {
        if (solutionCallback != null) {
            solutionCallback.onSolutionCalculationFailed(errorMessage);
            solutionCallback = null; // Clear the reference
        }
    }
    
    /**
     * Cancel any running solver operation
     */
    public void cancelSolver() {
        solver.cancel();
    }
    
    /**
     * Callback interface for solution calculation
     */
    public interface SolutionCallback {
        /**
         * Called when the solution calculation starts
         */
        void onSolutionCalculationStarted();
        
        /**
         * Called when the solution calculation completes successfully
         * @param solution The calculated solution
         */
        void onSolutionCalculationCompleted(GameSolution solution);
        
        /**
         * Called when the solution calculation fails
         * @param errorMessage The error message
         */
        void onSolutionCalculationFailed(String errorMessage);
    }
    
    /**
     * Shows a spinner animation while a solution is being calculated.
     * This method can be used by any UI implementation to show a standardized
     * loading indicator during solver processing.
     * 
     * @param callback A callback that will receive the Braille spinner character
     *                 updates for display
     * @param show True to show the spinner, false to hide it
     * @return The BrailleSpinner instance (so the caller can store and stop it later)
     */
    public BrailleSpinner showSpinner(BrailleSpinner.SpinnerListener callback, boolean show) {
        if (show) {
            BrailleSpinner spinner = new BrailleSpinner();
            spinner.setSpinnerListener(callback);
            spinner.start();
            return spinner;
        } else if (callback != null) {
            // Just send an empty character to hide it
            callback.onSpinnerUpdate("");
        }
        return null;
    }
    
    /**
     * Animate a solution by moving robots according to the solution steps.
     * This implementation uses the utility SolutionAnimator and provides
     * a standard way to display solution animations in any UI.
     * 
     * @param solution The solution to animate
     * @param listener The listener to receive animation events
     * @return The SolutionAnimator instance (so the caller can store and control it)
     */
    public SolutionAnimator animateSolution(GameSolution solution, SolutionAnimator.AnimationListener listener) {
        if (solution == null || solution.getMoves().isEmpty()) {
            Timber.w("Cannot animate null or empty solution");
            if (listener != null) {
                listener.onAnimationComplete();
            }
            return null;
        }
        
        Timber.d("Animating solution with %d moves", solution.getMoves().size());
        
        // Create and configure animator
        SolutionAnimator animator = new SolutionAnimator();
        animator.setAnimationListener(listener);
        animator.animateSolution(solution);
        return animator;
    }
}
