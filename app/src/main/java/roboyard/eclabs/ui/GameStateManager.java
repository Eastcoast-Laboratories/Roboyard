package roboyard.eclabs.ui;

import android.app.Application;
import android.content.Context;
import android.view.MotionEvent;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.Constants;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;

import roboyard.eclabs.R;

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
     * Save current game
     * @param saveId Save slot ID
     * @return True if save was successful
     */
    public boolean saveGame(int saveId) {
        GameState state = currentState.getValue();
        if (state != null) {
            return state.saveToFile(getApplication(), saveId);
        }
        return false;
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
                navController.navigate(R.id.actionGameCanvasToSaveGame, args);
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
}
