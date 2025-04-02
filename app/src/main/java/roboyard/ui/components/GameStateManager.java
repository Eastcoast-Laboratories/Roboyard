package roboyard.ui.components;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import roboyard.logic.core.Constants;
import roboyard.eclabs.FileReadWrite;
import roboyard.eclabs.ui.GameElement;
import roboyard.logic.core.GameState;
import roboyard.eclabs.ui.LevelCompletionData;
import roboyard.eclabs.ui.LevelCompletionManager;
import roboyard.eclabs.MapObjects;
import roboyard.eclabs.R;
import roboyard.eclabs.util.SolverManager;
import roboyard.eclabs.util.BrailleSpinner;
import roboyard.eclabs.util.SolutionAnimator;
import roboyard.logic.core.GridElement;
import roboyard.logic.core.Preferences;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import timber.log.Timber;

/**
 * Central state manager for the game.
 * Handles game state, navigation, and communication between fragments.
 * This replaces the previous GameManager with a more Android-native approach.
 */
public class GameStateManager extends AndroidViewModel implements SolverManager.SolverListener {
    
        
    // Minimum required moves for each difficulty level (as per documentation)
    private static final int MIN_MOVES_BEGINNER = 4;      // 4-6 moves
    private static final int MAX_MOVES_BEGINNER = 6;    // 4-6 moves
    private static final int MIN_MOVES_ADVANCED = 6;    // 6-8 moves
    private static final int MAX_MOVES_ADVANCED = 8;    // 6-8 moves
    private static final int MIN_MOVES_INSANE = 10;     // 10+ moves
    private static final int MIN_MOVES_IMPOSSIBLE = 17; // 17+ moves
    
    private boolean validateDifficulty = true;
    private int regenerationCount = 0;
    private static final int MAX_AUTO_REGENERATIONS = 999;
    
    // Game state
    private final MutableLiveData<GameState> currentState = new MutableLiveData<>();
    private final MutableLiveData<Integer> moveCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> squaresMoved = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isGameComplete = new MutableLiveData<>(false);
    
    // Move history for undo functionality
    private final ArrayList<GameState> stateHistory = new ArrayList<>();
    private final ArrayList<Integer> squaresMovedHistory = new ArrayList<>();
    
    // Game settings
    private final MutableLiveData<Boolean> soundEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isSolverRunning = new MutableLiveData<>(false);
    
    // Solver
    private SolverManager solver;
    private Context context;
    
    // Minimap
    private String currentMapName = "";
    private long startTime = 0;
    private Bitmap minimap = null;
    
    // Solution state
    private GameSolution currentSolution = null;
    private int currentSolutionStep = 0;
    
    // Track hint usage for level completion statistics
    private int hintsShown = 0;
    // Track unique robots used for level completion statistics
    private final Set<Integer> robotsUsed = new HashSet<>();
    
    public GameStateManager(Application application) {
        super(application);
        // We'll use lazy initialization for solver now - do not create it here
        
        context = application.getApplicationContext();
        
    }
    
    /**
     * Get the solver manager instance using the singleton pattern
     */
    private SolverManager getSolverManager() {
        Timber.d("[SOLUTION SOLVER] GameStateManager.getSolverManager(): Getting SolverManager singleton instance");
        SolverManager solverManager = SolverManager.getInstance();
        
        // Set solver listener if not already set
        if (solverManager.getListener() == null) {
            solverManager.setListener(this);
        }
        return solverManager;
    }
    
    /**
     * Start a new random game
     */
    public void startNewGame() {
        Timber.d("GameStateManager: startNewGame() called");

        startModernGame();
    }
    
    /**
     * Start a new modern game
     */
    public void startModernGame() {
        Timber.d("GameStateManager: startModernGame() called");
        
        // Reset any existing solver state to ensure a clean calculation for the new game
        SolverManager solverManager = getSolverManager();
        solverManager.resetInitialization();
        solverManager.cancelSolver(); // Cancel any running solver process
        
        // Clear any existing solution to prevent it from being reused
        currentSolution = null;
        currentSolutionStep = 0;
        
        // Reset regeneration counter
        regenerationCount = 0;
        
        // Create a new valid game (will regenerate if solution is too simple)
        createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight);
        
        // Record start time
        startTime = System.currentTimeMillis();
        
        Timber.d("GameStateManager: startModernGame() complete");
    }
    
    /**
     * Start a level game with the modern UI
     * @param levelId Level ID to load
     */
    public void startLevelGame(int levelId) {
        Timber.d("GameStateManager: startLevelGame() called with levelId: %d", levelId);
        
        // If solver is already running, don't create a new game state to avoid mismatch
        if (Boolean.TRUE.equals(isSolverRunning.getValue())) {
            Timber.d("[SOLUTION SOLVER] startLevelGame: Solver already running, not creating new game state");
            return;
        }
        
        // Reset any existing solver state to ensure a clean calculation for the new level
        SolverManager solverManager = getSolverManager();
        solverManager.resetInitialization();
        solverManager.cancelSolver(); // Cancel any running solver process
        
        // Clear any existing solution to prevent it from being reused
        currentSolution = null;
        currentSolutionStep = 0;
        
        // Load level from assets
        GameState state = GameState.loadLevel(getApplication(), levelId);
        state.setLevelId(levelId);
        state.setLevelName("Level " + levelId);
        
        // Set reference to this GameStateManager in the new state
        state.setGameStateManager(this);
        
        // Set the current state
        currentState.setValue(state);
        currentMapName = "Level-" + levelId;
        
        // Reset move counts and history
        setMoveCount(0);
        setSquaresMoved(0);
        setGameComplete(false);
        stateHistory.clear();
        squaresMovedHistory.clear();
        
        // Initialize the solver with the grid elements from the loaded level
        ArrayList<GridElement> gridElements = state.getGridElements();
        Timber.d("[SOLUTION SOLVER] Initializing solver with %d grid elements from level %d", 
                gridElements.size(), levelId);
        getSolverManager().initialize(gridElements);
        
        // Start calculating the solution automatically
        calculateSolutionAsync(null);
        
        // Record start time
        startTime = System.currentTimeMillis();
        
        Timber.d("GameStateManager: startLevelGame() complete for level %d", levelId);
        
        resetStatistics();
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
        getSolverManager().initialize(gridElements);
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
                
                // Ensure robots are reset to their initial positions
                newState.resetRobotPositions();
                Timber.d("GameStateManager: Robots reset to initial positions after loading saved game");
                
                currentState.setValue(newState);
                moveCount.setValue(newState.getMoveCount());
                isGameComplete.setValue(newState.isComplete());
                
                // Initialize solver with grid elements
                ArrayList<GridElement> gridElements = newState.getGridElements();
                getSolverManager().initialize(gridElements);
            }
        }
    }
    
    /**
     * Load a history entry
     * @param historyId History entry ID to load
     */
    public void loadHistoryEntry(int historyId) {
        // TODO: Implement history entry loading
        GameState newState = GameState.createRandom();
        newState.setLevelId(historyId);
        
        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this);
        
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        getSolverManager().initialize(gridElements);
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
     *
     * @param x      Grid X coordinate
     * @param y      Grid Y coordinate
     * @param action Touch action type
     * @return
     */
    public boolean handleGridTouch(int x, int y, int action) {
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
                    
                    // Track which robot was used (for level completion statistics)
                    GameElement robot = state.getSelectedRobot();
                    if (robot != null && robot.getColor() >= 0) {
                        robotsUsed.add(robot.getColor());
                    }
                    
                    // Store a copy of the state in history
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
        return true;
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
        if (currentSolution != null && currentSolution.getMoves() != null && 
            currentSolutionStep < currentSolution.getMoves().size()) {
            
            // Increment hint counter for level completion statistics
            hintsShown++;
            
            IGameMove move = currentSolution.getMoves().get(currentSolutionStep);
            incrementSolutionStep();
            return move;
        }
        return null;
    }
    
    /**
     * Check if a valid solution is available
     * @return true if a solution is available, false otherwise
     */
    public boolean hasSolution() {
        return currentSolution != null && currentSolution.getMoves() != null && 
               currentSolutionStep < currentSolution.getMoves().size();
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
                (androidx.fragment.app.FragmentActivity) context, R.id.nav_host_fragment);
        navController.navigate(R.id.actionGlobalMainMenu);
    }
    
    /**
     * Navigate to settings screen
     */
    public void navigateToSettings() {
        Timber.d("GameStateManager: navigateToSettings() called");
        // Use the NavController to navigate to the settings fragment
        NavController navController = Navigation.findNavController(
                (androidx.fragment.app.FragmentActivity) context, R.id.nav_host_fragment);
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
        if (!(context instanceof androidx.fragment.app.FragmentActivity activity)) {
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
    public LiveData<Boolean> isSolverRunning() { return isSolverRunning; }

    /**
     * Setters for game settings
     */
    public void setSoundEnabled(boolean enabled) { 
        this.soundEnabled.setValue(enabled); 
        // Also update the static Preferences value to ensure consistency
        roboyard.logic.core.Preferences.setSoundEnabled(enabled);
    }
    
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
            Timber.d("[ROBOTS] undoLastMove: No history to undo, stateHistory is empty");
            return false;
        }
        
        // Get the previous state and restore it
        GameState previousState = stateHistory.remove(stateHistory.size() - 1);
        Timber.d("[ROBOTS] undoLastMove: Removed previous state from history, remaining history size: %d", stateHistory.size());
        
        if (previousState != null) {
            // Also restore the squares moved count
            if (!squaresMovedHistory.isEmpty()) {
                int previousSquaresMoved = squaresMovedHistory.remove(squaresMovedHistory.size() - 1);
                squaresMoved.setValue(previousSquaresMoved);
                Timber.d("[ROBOTS] undoLastMove: Restored squares moved to: %d", previousSquaresMoved);
            } else {
                Timber.d("[ROBOTS] undoLastMove: No squares moved history to restore");
            }
            
            // Restore the state
            currentState.setValue(previousState);
            Timber.d("[ROBOTS] undoLastMove: Restored previous game state");
            
            // Decrement move count
            int moves = moveCount.getValue();
            moveCount.setValue(Math.max(0, moves - 1));
            Timber.d("[ROBOTS] undoLastMove: Decremented move count to: %d", Math.max(0, moves - 1));
            
            // Reset game complete flag if it was set
            if (isGameComplete.getValue()) {
                isGameComplete.setValue(false);
                Timber.d("[ROBOTS] undoLastMove: Reset game complete flag");
            }
            
            return true;
        } else {
            Timber.e("[ROBOTS] undoLastMove: Previous state was null, this should not happen");
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
        GameState state = currentState.getValue();
        if (state != null) {
            Timber.d("Setting game complete: %s for level %d", complete, state.getLevelId());
            state.setCompleted(complete);
            isGameComplete.setValue(complete);
            
            // Save level completion data if this is a level game
            if (complete && state.getLevelId() > 0) {
                Timber.d("[SAVE] [STARS] Game completed, saving level completion data for level %d", state.getLevelId());
                LevelCompletionData data = saveLevelCompletionData(state);
                
                // Now save the prepared data
                if (data != null) {
                    LevelCompletionManager manager = LevelCompletionManager.getInstance(context);
                    manager.saveLevelCompletionData(data);
                    Timber.d("Saved level completion data: %s", data);
                }
                
                // Show a toast to indicate the level was completed
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Level " + state.getLevelId() + " completed!", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
    
    /**
     * Save level completion data when a level is completed
     * @param state The completed game state
     * @return The prepared LevelCompletionData object (does not save it)
     */
    private LevelCompletionData saveLevelCompletionData(GameState state) {
        int levelId = state.getLevelId();
        if (levelId <= 0) {
            Timber.d("Not saving completion data - not a level game (levelId=%d)", levelId);
            return null; // Not a level game
        }
        
        Timber.d("Preparing completion data for level %d", levelId);
        
        // Get the level completion manager
        LevelCompletionManager manager = LevelCompletionManager.getInstance(context);
        
        // Get or create completion data for this level
        LevelCompletionData data = manager.getLevelCompletionData(levelId);
        
        // Update completion data
        data.setCompleted(true);
        data.setHintsShown(hintsShown);
        data.setTimeNeeded(System.currentTimeMillis() - startTime);
        data.setMovesNeeded(moveCount.getValue() != null ? moveCount.getValue() : 0);
        data.setRobotsUsed(robotsUsed.size());
        data.setSquaresSurpassed(squaresMoved.getValue() != null ? squaresMoved.getValue() : 0);
        
        // Set optimal moves if we have a solution
        int optimalMoves = 0;
        if (currentSolution != null && currentSolution.getMoves() != null) {
            optimalMoves = currentSolution.getMoves().size();
            data.setOptimalMoves(optimalMoves);
        }
        
        // Calculate stars based on the criteria
        int playerMoves = moveCount.getValue() != null ? moveCount.getValue() : 0;
        int starCount = calculateStars(playerMoves, optimalMoves, hintsShown);
        data.setStars(starCount);
        
        Timber.d("[STARS] gameStateManager: Level %d completed with %d moves (optimal: %d), %d hints, earned %d stars", 
                levelId, playerMoves, optimalMoves, hintsShown, starCount);
        
        // Return the prepared data without saving it
        Timber.d("Prepared level completion data: %s", data);
        return data;
    }
    
    /**
     * Calculate star rating based on player performance
     * 
     * Star allocation rules:
     * - 4 stars: Hyper-optimal solution (better than solver's optimal solution)
     * - 3 stars: Optimal solution (same as solver) with no hints
     * - 2 stars: One move more than optimal with no hints, OR optimal with one hint
     * - 1 star: Optimal solution with two hints, OR two moves more than optimal with no hints
     * - 0 stars: All other cases
     * 
     * @param playerMoves Number of moves used by player
     * @param optimalMoves Optimal number of moves from solver
     * @param hintsUsed Number of hints used
     * @return Number of stars earned (0-4)
     */
    public int calculateStars(int playerMoves, int optimalMoves, int hintsUsed) {
        if (optimalMoves <= 0) {
        	Timber.d("[stars] No optimal solution available");
            return 0; // No optimal solution available
        }
        
        // Calculate stars based on the rules
        if (playerMoves < optimalMoves) {
            // hyper-Optimal solution (better than solver's solution)
            Timber.d("[stars] hyper-optimal solution! 4 stars");
            return 4;
        } else if (playerMoves == optimalMoves && hintsUsed == 0) {
            // Optimal solution (or better) and no hints
            Timber.d("[stars] optimal solution! 3 stars");
			return 3;
        } else if ((playerMoves == optimalMoves + 1 && hintsUsed == 0) || 
                  (playerMoves == optimalMoves && hintsUsed == 1)) {
            // One move more than optimal with no hints OR optimal with one hint
            return 2;
        } else if ((playerMoves == optimalMoves && hintsUsed == 2) || 
                  (playerMoves == optimalMoves + 2 && hintsUsed == 0)) {
            // Optimal with two hints OR two moves more than optimal with no hints
            return 1;
        } else {
            // All other cases
            return 0;
        }
    }
    
    /**
     * Get the total number of stars earned across all levels
     * @return Total number of stars
     */
    public int getTotalStars() {
        LevelCompletionManager manager = LevelCompletionManager.getInstance(context);
        return manager.getTotalStars();
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
     * Get the current difficulty level from Preferences
     * @return Current difficulty level
     */
    public int getDifficulty() {
        return Preferences.difficulty;
    }
    
    /**
     * Get a string representation of the current difficulty level
     * @return String representation of the current difficulty level
     */
    public String getDifficultyString() {
        switch (getDifficulty()) {
            case Constants.DIFFICULTY_BEGINNER:
                return "Beginner";
            case Constants.DIFFICULTY_INTERMEDIATE:
                return "Intermediate";
            case Constants.DIFFICULTY_INSANE:
                return "Insane";
            case Constants.DIFFICULTY_IMPOSSIBLE:
                return "Impossible";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Set the difficulty level in Preferences
     * @param difficulty New difficulty level
     */
    public void setDifficulty(int difficulty) {
        Preferences.difficulty = difficulty;
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
                writer.write(line + "\n");
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
        Timber.d("[ROBOTS] resetRobots: Attempting to reset robots to starting positions");
        
        GameState currentGameState = currentState.getValue();
        if (currentGameState == null) {
            Timber.e("[ROBOTS] resetRobots: Current game state is null, cannot reset robots");
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
                Timber.d("[ROBOTS] resetRobots: Successfully created deep copy of current game state");
            } catch (Exception e) {
                Timber.e(e, "[ROBOTS] resetRobots: Error creating a deep copy of GameState for reset");
                return; // Can't proceed without a valid copy
            }
            
            // Reset robot positions by moving them back to original positions
            Timber.d("[ROBOTS] resetRobots: Calling resetRobotPositions on the game state");
            resetState.resetRobotPositions();
            
            // Reset counters
            moveCount.setValue(0);
            squaresMoved.setValue(0);
            isGameComplete.setValue(false);
            Timber.d("[ROBOTS] resetRobots: Reset move count, squares moved, and game complete flag");
            
            // Clear history
            int historySize = stateHistory.size();
            stateHistory.clear();
            squaresMovedHistory.clear();
            Timber.d("[ROBOTS] resetRobots: Cleared state history (previous size: %d)", historySize);
            
            // Update state
            currentState.setValue(resetState);
            Timber.d("[ROBOTS] resetRobots: Updated current state with reset state");
            
            // Show a toast notification
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Robots reset to starting positions", Toast.LENGTH_SHORT).show();
                });
            }
            
            Timber.d("[ROBOTS] resetRobots: Robots successfully reset to starting positions");
        } catch (Exception e) {
            Timber.e(e, "[ROBOTS] resetRobots: Error resetting robots");
        }
    }
    
    /**
     * Asynchronously calculates the solution for the current game state
     * and returns the result via callback
     * 
     * @param callback The callback to receive the solution when it's ready
     */
    public void calculateSolutionAsync(final SolutionCallback callback) {
        // Don't start a new calculation if one is already running
        if (Boolean.TRUE.equals(isSolverRunning.getValue())) {
            Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Solver already running, ignoring duplicate request");
            return;
        }
        
        // Store the callback
        this.solutionCallback = callback;
        Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Stored callback: %s", callback);
        
        GameState state = currentState.getValue();
        if (state == null) {
            Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Current state is null");
            onSolutionCalculationFailed("No game state available");
            return;
        }
        
        // Log the current game state details
        ArrayList<GridElement> elements = state.getGridElements();
        Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Current GameState hash: %d", state.hashCode());
        Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Current map has %d elements", elements.size());
        
        // Log robot positions
        List<GameElement> robots = state.getRobots();
        for (GameElement robot : robots) {
            Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Robot ID %d (color %d) at position (%d, %d)", 
                  robot.getColor(), robot.getColor(), robot.getX(), robot.getY());
        }
        
        // Log target position
        GameElement target = state.getTarget();
        if (target != null) {
            Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Target for robot Color %d (color %d) at position (%d, %d)",
                  target.getColor(), target.getColor(), target.getX(), target.getY());
        } else {
            Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: No target found in current game state");
        }
        
        // Set solver running state
        isSolverRunning.setValue(true);
        
        // Signal that calculation has started
        onSolutionCalculationStarted();
        
        try {
            Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Initializing solver with current game state");
            getSolverManager().initialize(elements);
            
            // Run the solver on a background thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Timber.d("[SOLUTION SOLVER] calculateSolutionAsync: Running solver on background thread");
                    getSolverManager().run();
                    // Note: The solver will call the listener methods (onSolverFinished)
                    // when it completes, so we don't need to do anything more here
                } catch (Exception e) {
                    Timber.e(e, "[SOLUTION SOLVER] Error running solver");
                    // Handle on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        onSolutionCalculationFailed("Error: " + e.getMessage());
                    });
                }
            });
            executor.shutdown();
        } catch (Exception e) {
            Timber.e(e, "[SOLUTION SOLVER] Error initializing solver");
            onSolutionCalculationFailed("Error: " + e.getMessage());
        }
    }

    /**
     * Called when the solution calculation starts
     */
    private void onSolutionCalculationStarted() {
        Timber.d("[SOLUTION SOLVER] onSolutionCalculationStarted");
        currentSolution = null;
        currentSolutionStep = 0;
        
        // Notify callback if provided
        if (solutionCallback != null) {
            Timber.d("[SOLUTION SOLVER] onSolutionCalculationStarted: Notifying callback: %s", solutionCallback);
            solutionCallback.onSolutionCalculationStarted();
        } else {
            Timber.w("[SOLUTION SOLVER] onSolutionCalculationStarted: No callback to notify");
        }
    }

    /**
     * Called when the solution calculation completes successfully
     * This is the SolverManager.Listener implementation that's called by SolverManager when the solver finishes.
     * This method stores the solution and notifies any registered callback.
     * 
     * @param solution The calculated solution
     */
    private void onSolutionCalculationCompleted(GameSolution solution) {
        Timber.d("[SOLUTION SOLVER] onSolutionCalculationCompleted: solution=%s", solution);
        
        // Add more detailed logging about the solution
        int moveCount = solution != null && solution.getMoves() != null ? solution.getMoves().size() : 0;
        if (moveCount > 0) {
            Timber.d("[SOLUTION SOLVER][MOVES] onSolutionCalculationCompleted: Found solution with %d moves", solution.getMoves().size());
            // If solution requires fewer moves than required minimum and we're not in level mode,
            // automatically start a new game because this one is too easy
            GameState state = currentState.getValue();
            boolean isLevelMode = (state != null && state.getLevelId() > 0);
            int minRequiredMoves = getMinimumRequiredMoves();
            
            if (moveCount < minRequiredMoves && !isLevelMode && regenerationCount < MAX_AUTO_REGENERATIONS) {
                Timber.d("[SOLUTION SOLVER][MOVES] Solution has only %d moves (minimum required: %d), starting new game (regeneration %d/%d)", 
                        moveCount, minRequiredMoves, regenerationCount + 1, MAX_AUTO_REGENERATIONS);
                regenerationCount++;
                
                // Force reset the solver state before starting a new game
                SolverManager solverManager = getSolverManager();
                solverManager.resetInitialization();
                solverManager.cancelSolver(); // Cancel any running solver process
                
                // Create a new game after a short delay to ensure the solver is fully reset
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight);
                }, 100);
                return;
            } else if (regenerationCount >= MAX_AUTO_REGENERATIONS) {
                Timber.d("[SOLUTION SOLVER][MOVES] Reached maximum regeneration attempts (%d). Accepting current game.", MAX_AUTO_REGENERATIONS);
                regenerationCount = 0; // Reset for next time
            }
        } else {
            Timber.w("[SOLUTION SOLVER][MOVES] onSolutionCalculationCompleted: Solution or moves is null!");
        }
        
        // Store the solution for later use with getHint()
        currentSolution = solution;
        currentSolutionStep = 0;
        
        // Update solver status
        isSolverRunning.setValue(false);
        
        // Notify the callback if provided
        if (solutionCallback != null) {
            Timber.d("[SOLUTION SOLVER] onSolutionCalculationCompleted: Notifying callback: %s", solutionCallback);
            solutionCallback.onSolutionCalculationCompleted(solution);
            solutionCallback = null; // Clear callback after use
        } else {
            Timber.d("[SOLUTION SOLVER] onSolutionCalculationCompleted: No callback provided");
        }
    }
    
    /**
     * Called when the solution calculation fails
     * @param errorMessage The error message
     */
    private void onSolutionCalculationFailed(String errorMessage) {
        Timber.d("[SOLUTION SOLVER] onSolutionCalculationFailed: %s", errorMessage);
        
        // Clear any partial solution
        currentSolution = null;
        currentSolutionStep = 0;
        
        // Update solver status
        isSolverRunning.setValue(false);
        
        // Notify the callback if provided
        if (solutionCallback != null) {
            Timber.d("[SOLUTION SOLVER] onSolutionCalculationFailed: Notifying callback: %s", solutionCallback);
            solutionCallback.onSolutionCalculationFailed(errorMessage);
            solutionCallback = null; // Clear callback after use
        } else {
            Timber.d("[SOLUTION SOLVER] onSolutionCalculationFailed: No callback provided");
        }
    }

    /**
     * Cancel any running solver operation
     */
    public void cancelSolver() {
        Timber.d("[SOLUTION SOLVER] cancelSolver called");
        if (Boolean.TRUE.equals(isSolverRunning.getValue())) {
            getSolverManager().cancelSolver();
            // The solver will call onSolverCancelled() via the listener
        }
    }
    
    /**
     * Get the current solution
     * @return The current solution or null if none is available
     */
    public GameSolution getCurrentSolution() {
        return currentSolution;
    }
    
    /**
     * Get the current solution step (hint number)
     * @return The current solution step (0-indexed)
     */
    public int getCurrentSolutionStep() {
        return currentSolutionStep;
    }
    
    /**
     * Increment the solution step counter
     */
    public void incrementSolutionStep() {
        currentSolutionStep++;
    }
    
    /**
     * Reset the solution step counter to show hints from the beginning
     */
    public void resetSolutionStep() {
        currentSolutionStep = 0;
    }
    
    // Field to store the current solution callback
    private SolutionCallback solutionCallback;
    
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
    
    /**
     * Reset statistics for a new game
     */
    private void resetStatistics() {
        hintsShown = 0;
        robotsUsed.clear();
        startTime = System.currentTimeMillis();
        moveCount.setValue(0);
        squaresMoved.setValue(0);
        isGameComplete.setValue(false);
    }
    
    /**
     * Gets the minimum required moves based on current difficulty setting
     * @return minimum number of moves required for current difficulty
     */
    private int getMinimumRequiredMoves() {
        int difficulty = Preferences.difficulty;
        int minMoves = 0;
        switch (difficulty) {
            case Constants.DIFFICULTY_INTERMEDIATE:
                minMoves = MIN_MOVES_ADVANCED;
                break;
            case Constants.DIFFICULTY_INSANE:
                minMoves = MIN_MOVES_INSANE;
                break;
            case Constants.DIFFICULTY_IMPOSSIBLE:
                minMoves = MIN_MOVES_IMPOSSIBLE;
                break;
            default:
                minMoves = MIN_MOVES_BEGINNER; // Default to beginner if unknown difficulty
        }
        Timber.d("[SOLUTION SOLVER][MOVES] Minimum required moves for difficulty %d: %d", difficulty, minMoves);
        return minMoves;
    }

    private int getMaximumRequiredMoves() {
        int difficulty = Preferences.difficulty;
        int maxMoves = 9999;
        switch (difficulty) {
            case Constants.DIFFICULTY_INTERMEDIATE:
                maxMoves = MAX_MOVES_ADVANCED;
                break;
            case Constants.DIFFICULTY_INSANE:
            case Constants.DIFFICULTY_IMPOSSIBLE:
                break;
            default:
                maxMoves = MAX_MOVES_BEGINNER;
        }
        Timber.d("[SOLUTION SOLVER][MOVES] Maximum required moves for difficulty %d: %d", difficulty, maxMoves);
        return maxMoves;
    }
    
    /**
     * Creates a valid game with at least MIN_REQUIRED_MOVES difficulty
     * @param width Width of the board
     * @param height Height of the board
     */
    private void createValidGame(int width, int height) {
        Timber.d("GameStateManager: createValidGame() called");
        
        // Create a new random game state using static Preferences
        GameState newState = GameState.createRandom();
        Timber.d("GameStateManager: Created new random GameState with robotCount=%d, targetColors=%d", 
                Preferences.robotCount, 
                Preferences.targetColors);
        
        // Set the game state
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Clear the state history
        stateHistory.clear();
        squaresMovedHistory.clear();
        
        // Initialize the solver with grid elements from the new state
        ArrayList<GridElement> gridElements = newState.getGridElements();
        getSolverManager().resetInitialization();
        getSolverManager().initialize(gridElements);
        
        startTime = System.currentTimeMillis();
        
        // Start calculating the solution, but use our internal validation callback
        if (validateDifficulty) {
            // Temporarily set validateDifficulty to false to prevent infinite recursion
            // if all generated puzzles are too easy
            validateDifficulty = false;
            
            // Calculate solution with our own callback to validate difficulty
            Timber.d("GameStateManager: Validating puzzle difficulty...");
            calculateSolutionAsync(new DifficultyValidationCallback(width, height));
        } else {
            // Regular game initialization, don't validate difficulty
            validateDifficulty = true; // Reset for next time
            calculateSolutionAsync(null);
        }
    }
    
    /**
     * Callback to validate puzzle difficulty and regenerate if needed
     */
    private class DifficultyValidationCallback implements SolutionCallback {
        private final int width;
        private final int height;
        private int attemptCount = 0;
        private static final int MAX_ATTEMPTS = 999;
        
        public DifficultyValidationCallback(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        @Override
        public void onSolutionCalculationStarted() {
            Timber.d("DifficultyValidationCallback: Calculation started, attempt %d", attemptCount + 1);
        }
        
        @Override
        public void onSolutionCalculationCompleted(GameSolution solution) {
            attemptCount++;
            int moveCount = solution != null && solution.getMoves() != null ? solution.getMoves().size() : 0;
            int requiredMoves = getMinimumRequiredMoves();
            int maxMoves = getMaximumRequiredMoves();
            
            Timber.d("[DifficultyValidationCallback]: Found solution with %d moves (minimum required: %d, maximum required: %d)",
                    moveCount, requiredMoves, maxMoves);
            
            if (moveCount < requiredMoves && attemptCount < MAX_ATTEMPTS) {
                // Puzzle too easy, generate a new one
                Timber.d("[DifficultyValidationCallback]: Puzzle too easy (%d moves), generating new one", moveCount);
                createValidGame(width, height);
            } else if (moveCount > maxMoves && attemptCount < MAX_ATTEMPTS) {
                // Puzzle too hard, generate a new one
                Timber.d("[DifficultyValidationCallback]: Puzzle too hard (%d moves), generating new one", moveCount);
                createValidGame(width, height);
            } else {
                // Puzzle is good enough or we've tried too many times
                Timber.d("[DifficultyValidationCallback]: Accepted puzzle with %d moves after %d attempts",
                        moveCount, attemptCount);
                validateDifficulty = true; // Reset validation flag
                // Store the solution
                currentSolution = solution;
                currentSolutionStep = 0;
                isSolverRunning.setValue(false);
            }
        }
        
        @Override
        public void onSolutionCalculationFailed(String errorMessage) {
            Timber.w("DifficultyValidationCallback: Solution calculation failed: %s", errorMessage);
            // Just accept the current puzzle even if we couldn't solve it
            validateDifficulty = true;
            isSolverRunning.setValue(false);
        }
    }
    
    @Override
    public void onSolverFinished(boolean success, int solutionMoves, int numSolutions) {
        // Process on main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (success) {
                GameSolution solution = getSolverManager().getCurrentSolution();
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
}
