package roboyard.eclabs.ui;

import android.app.Application;
import android.content.Context;
import android.view.MotionEvent;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.Constants;
import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;

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
    
    public GameStateManager(Application application) {
        super(application);
        // Initialize solver
        solver = new SolverDD();
    }
    
    /**
     * Start a new random game
     */
    public void startNewGame() {
        // Create a new random game state
        GameState newState = GameState.createRandom(boardSize.getValue(), difficulty.getValue());
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Initialize solver with new state
        solver.init(newState.getMapData());
    }
    
    /**
     * Load a specific level
     * @param levelId Level ID to load
     */
    public void loadLevel(int levelId) {
        // Load level from assets
        GameState newState = GameState.loadLevel(getApplication(), levelId);
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);
        
        // Initialize solver with new state
        solver.init(newState.getMapData());
    }
    
    /**
     * Load a saved game
     * @param saveId Save slot ID
     */
    public void loadGame(int saveId) {
        // Load game from storage
        GameState newState = GameState.loadSavedGame(getApplication(), saveId);
        if (newState != null) {
            currentState.setValue(newState);
            moveCount.setValue(newState.getMoveCount());
            isGameComplete.setValue(newState.isComplete());
            
            // Initialize solver with new state
            solver.init(newState.getMapData());
        }
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
                    moveCount.setValue(moveCount.getValue() + 1);
                    
                    // Check if game is complete
                    if (state.checkCompletion()) {
                        isGameComplete.setValue(true);
                    }
                    
                    currentState.setValue(state);
                }
            }
        }
    }
    
    /**
     * Get a hint for the current state
     * @return Next move hint
     */
    public GameMove getHint() {
        if (solver != null && currentState.getValue() != null) {
            return solver.getNextMove(currentState.getValue().getMapData());
        }
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
     * Get the current context
     */
    public Context getContext() {
        return getApplication().getApplicationContext();
    }
}
