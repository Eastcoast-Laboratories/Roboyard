package roboyard.eclabs.ui.mock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import roboyard.logic.core.GameState;
import roboyard.ui.components.GameStateManager;

/**
 * Mock implementation of GameStateManager for testing.
 * Provides simplified behavior of the real GameStateManager to facilitate testing
 * of UI components that depend on it.
 */
public class MockGameStateManager extends GameStateManager {
    
    private final MutableLiveData<GameState> currentState = new MutableLiveData<>();
    private final MutableLiveData<Integer> boardSize = new MutableLiveData<>(16);
    private final MutableLiveData<Integer> difficulty = new MutableLiveData<>(1);
    
    public MockGameStateManager() {
        super(null);
        // Initialize with a default state
        currentState.setValue(GameState.createRandom(16, 1));
    }
    
    @Override
    public LiveData<GameState> getCurrentState() {
        return currentState;
    }
    
    @Override
    public void startNewGame() {
        GameState newState = GameState.createRandom(boardSize.getValue(), difficulty.getValue());
        currentState.setValue(newState);
    }
    
    @Override
    public void loadLevel(int level) {
        // Simplified level loading for testing
        GameState newState = GameState.createRandom(boardSize.getValue(), difficulty.getValue());
        newState.setLevelId(level);
        currentState.setValue(newState);
    }
    
    @Override
    public LiveData<Integer> getBoardSize() {
        return boardSize;
    }
    
    @Override
    public LiveData<Integer> getDifficulty() {
        return difficulty;
    }
    
    public void setBoardSize(int size) {
        boardSize.setValue(size);
    }
    
    public void setDifficulty(int level) {
        difficulty.setValue(level);
    }
    
    public void setCurrentState(GameState state) {
        currentState.setValue(state);
    }
}
