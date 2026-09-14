package roboyard.logic.managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import roboyard.logic.core.GameState

/**
 * Platform-agnostic core of GameStateManager.
 * Contains only the game logic state without Android dependencies.
 * 
 * For KMP: This class can be shared with iOS/Desktop.
 * Android-specific wrapper (GameStateManager) handles LiveData for UI compatibility.
 */
class GameStateManagerCore {
    // Core game state using StateFlow (KMP-compatible)
    private val _currentState = MutableStateFlow<GameState?>(null)
    val currentState: StateFlow<GameState?> = _currentState
    
    private val _moveCount = MutableStateFlow(0)
    val moveCount: StateFlow<Int> = _moveCount
    
    private val _squaresMoved = MutableStateFlow(0)
    val squaresMoved: StateFlow<Int> = _squaresMoved
    
    private val _isGameComplete = MutableStateFlow(false)
    val isGameComplete: StateFlow<Boolean> = _isGameComplete
    
    private val _wrongRobotAtTarget = MutableStateFlow(-1)
    val wrongRobotAtTarget: StateFlow<Int> = _wrongRobotAtTarget
    
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled
    
    private val _isSolverRunning = MutableStateFlow(false)
    val isSolverRunning: StateFlow<Boolean> = _isSolverRunning
    
    // Move history for undo functionality
    private val stateHistory = ArrayList<GameState?>()
    
    fun updateState(state: GameState?) {
        _currentState.value = state
    }
    
    fun updateMoveCount(count: Int) {
        _moveCount.value = count
    }
    
    fun updateSquaresMoved(squares: Int) {
        _squaresMoved.value = squares
    }
    
    fun updateGameComplete(complete: Boolean) {
        _isGameComplete.value = complete
    }
    
    fun updateWrongRobotAtTarget(color: Int) {
        _wrongRobotAtTarget.value = color
    }
    
    fun updateSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
    }
    
    fun updateSolverRunning(running: Boolean) {
        _isSolverRunning.value = running
    }
    
    fun addToHistory(state: GameState?) {
        stateHistory.add(state)
    }
    
    fun getHistorySize(): Int = stateHistory.size
    
    fun clearHistory() {
        stateHistory.clear()
    }
    
    fun undo(): GameState? {
        if (stateHistory.size > 1) {
            stateHistory.removeAt(stateHistory.size - 1)
            return stateHistory.lastOrNull()
        }
        return null
    }
}
