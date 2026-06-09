package roboyard.logic.managers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.MotionEvent
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation.findNavController
import roboyard.eclabs.R
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.core.Constants
import roboyard.logic.core.GameElement
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.core.GameLogic.Companion.getColorName
import roboyard.logic.core.GameSolution
import roboyard.logic.core.GameState
import roboyard.logic.core.GameState.Companion.createRandom
import roboyard.logic.core.GameState.Companion.parseFromSaveData
import roboyard.logic.core.GridElement
import roboyard.logic.core.IGameMove
import roboyard.logic.core.LevelCompletionData
import roboyard.logic.core.Preferences
import roboyard.logic.core.WallStorage.Companion.getInstance
import roboyard.logic.managers.GameHistoryManager.addHistoryEntry
import roboyard.logic.managers.GameHistoryManager.findByMapSignature
import roboyard.logic.managers.GameHistoryManager.getHistoryEntries
import roboyard.logic.managers.GameHistoryManager.getNextHistoryIndex
import roboyard.logic.managers.GameHistoryManager.initialize
import roboyard.logic.managers.GameHistoryManager.saveHistoryIndex
import roboyard.logic.managers.SyncManager.HistoryUploadCallback
import roboyard.logic.solver.ERRGameMove
import roboyard.logic.solver.RRGameMove
import roboyard.logic.solver.RRPiece
import roboyard.logic.solver.SolverDD
import roboyard.logic.solver.SolverDD.isSolution01
import roboyard.logic.storage.FileReadWrite.Companion.writePrivateData
import roboyard.ui.RoboyardApplication
import roboyard.ui.activities.MainActivity
import roboyard.ui.animation.RobotAnimationManager
import roboyard.ui.components.GameGridView
import roboyard.ui.util.LiveSolverManager
import roboyard.ui.util.LiveSolverManager.LiveSolverListener
import roboyard.ui.util.SolutionAnimator
import roboyard.ui.util.SolverManager
import roboyard.ui.util.SolverManager.SolverListener
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.w
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Central state manager for the game.
 * Handles game state, navigation, and communication between fragments.
 */
open class GameStateManager(application: Application) : AndroidViewModel(application),
    SolverListener {
    // Minimum required moves for each difficulty level (as per documentation)
    private var validateDifficulty = true
    private var regenerationCount = 0
    private var allowRegeneration = true // Flag to stop regeneration when leaving game
    private var keepCurrentMapDespiteDifficulty =
        false // Manual override from the UI to keep the current map

    // Game state
    private val currentState = MutableLiveData<GameState?>()
    private val moveCount = MutableLiveData<Int?>(0)
    private val squaresMoved = MutableLiveData<Int?>(0)
    private val isGameComplete = MutableLiveData<Boolean?>(false)
    private val wrongRobotAtTarget =
        MutableLiveData<Int?>(-1) // carries color of wrong robot that just landed on a target, -1 = none

    // Move history for undo functionality
    private val stateHistory = ArrayList<GameState?>()
    private val squaresMovedHistory = ArrayList<Int?>()

    /**
     * Get the path history for robot movements
     * @return ArrayList of path entries [robotColor, fromX, fromY, toX, toY]
     */
    // Path history for visual robot paths: each entry stores [robotColor, fromX, fromY, toX, toY]
    @JvmField
    val pathHistory: ArrayList<IntArray> = ArrayList<IntArray>()

    /**
     * Get robot starting positions
     * @return HashMap of robot color to starting position [x,y]
     */
    // Robot starting positions: Map robot color to starting position [x,y]
    @JvmField
    val robotStartingPositions: HashMap<Int?, IntArray?> = HashMap<Int?, IntArray?>()

    // Game settings
    private val soundEnabled = MutableLiveData<Boolean?>(true)
    private val isSolverRunning = MutableLiveData<Boolean?>(false)

    // Solver
    private val solver: SolverManager? = null
    private val solverExecutor: ExecutorService =
        Executors.newSingleThreadExecutor() // Single persistent executor

    @Volatile
    private var solverFuture: Future<*>? = null // Track current solver task for cancellation
    private val context: Context?

    /**
     * Get the level name
     * 
     * @return The current level name
     */
    // Minimap
    var levelName: String? = ""
        private set
    private var startTime: Long = 0
    private val minimap: Bitmap? = null

    // Solution state
    var currentSolution: GameSolution? = null
        private set

    /**
     * Get the current solution step (hint number)
     * 
     * @return The current solution step (0-indexed)
     */
    var currentSolutionStep: Int = 0
        private set
    private var loadedSolutions: MutableList<GameSolution?>? =
        null // Solutions loaded from save file for re-saving

    // Pre-computation: remembered robot order from last known solution.
    // Survives solver restarts so preComputeNextMoves can prioritize correctly.
    private val preCompRobotOrder: MutableList<Int?> = ArrayList<Int?>()

    // Track hint usage for level completion statistics
    private var hintsShown = 0

    // Track unique robots used for level completion statistics
    private val robotsUsed: MutableSet<Int?> = HashSet<Int?>()

    // Robot animation manager
    private val robotAnimationManager: RobotAnimationManager?

    // Animation settings - made slower for more visible effect (minDuration = 100f,) 
    private val animationsEnabled = true

    /**
     * Get acceleration duration for animations
     * 
     * @return Acceleration duration in milliseconds
     */
    @JvmField
    val accelerationDuration: Float = 300f // Reduced from 300f for faster animations

    /**
     * Get maximum animation speed
     * 
     * @return Maximum speed for animations
     */
    @JvmField
    val maxSpeed: Float = 1500f // Higher speed but not extreme

    /**
     * Get deceleration duration for animations
     * 
     * @return Deceleration duration in milliseconds
     */
    @JvmField
    val decelerationDuration: Float = 50f // Reduced from 400f for faster animations

    /**
     * Get the current animation frame delay in milliseconds
     * 
     * @return Current animation frame delay
     */
    @JvmField
    val animationFrameDelay: Long =
        25 // Animation frame delay in ms (default Android is ~16ms = 60fps)

    private var isResetting = false
    private var gameGridView: GameGridView? = null

    /**
     * Gets the current solver restart count
     * 
     * @return The number of times the solver has been restarted
     */
    // Track solver restart count and last solution minimums for UI display
    var solverRestartCount: Int = 0
        private set

    /**
     * Gets the minimum moves from the last found solution
     * 
     * @return The minimum moves from the last solution, or 0 if no solution found yet
     */
    var lastSolutionMinMoves: Int = 0
        private set

    /**
     * Get the saved UI timer elapsed time
     * @return elapsed time in milliseconds, or 0 if no timer was running
     */
    // UI timer tracking (survives fragment recreation)
    var uiTimerElapsedMs: Long = 0
        private set
    private var uiTimerWasRunning = false

    private var lastMoveTime: Long = 0

    /**
     * Check if a new game was just loaded (timer should reset)
     * @return true if a new game was loaded
     */
    var isNewGameLoaded: Boolean =
        false // Flag to indicate if a new game was just loaded (timer should reset)
        private set
    private val newGameLoadedEvent =
        MutableLiveData<Boolean?>(false) // LiveData to trigger observer when new game is loaded
    private var solutionWasAccepted =
        false // Flag to signal Fragment that the solution was accepted

    // Game history tracking variables
    private var gameStartTime: Long = 0
    private var totalPlayTime = 0
    private var isHistorySaved = false
    private var isViewTimeAchievementChecked = false // prevents double-checking per session
    private var isCompletionRecorded =
        false // prevents double-counting completions per game session
    private val wrongRobotToastShownColors: MutableSet<Int?> =
        HashSet<Int?>() // robot colors that already triggered the "wrong robot on target" toast this game

    // Reference to the current activity - will be updated by getActivity() and setActivity() methods
    private var activityRef: WeakReference<Activity?>? = null

    /**
     * Get collision info from the last movement (DRY - avoid recalculating in GameGridView).
     * @return true if the last movement hit a wall
     */
    // Last movement collision info (set by moveRobotInDirection, read by GameGridView)
    var lastMoveHitWall: Boolean = false
        private set
    private var lastMoveHitRobot = false

    /**
     * Get the robot that was hit in the last movement (DRY - avoid recalculating in GameGridView).
     * @return the robot element that was hit, or null if no robot was hit
     */
    var lastMoveHitRobotElement: GameElement? = null
        private set

    /**
     * Check if the current game was loaded from a savegame
     * @return true if loaded from savegame, false otherwise
     */
    // Store the difficulty level from a deep link
    // Flag to indicate if the current game was loaded from a savegame
    // When true, skip min/max moves validation to allow playing saved games regardless of current difficulty settings
    var isLoadedFromSave: Boolean = false
        private set

    /**
     * Check if the current game was loaded from history
     * @return true if loaded from history, false otherwise
     */
    // Flag to indicate if the current game was loaded from history
    // When true, back button should navigate to previous history entry instead of undo
    var isLoadedFromHistory: Boolean = false
        private set

    // Track the current history entry path for navigation
    private var currentHistoryPath: String? = null

    // Live move counter feature
    private var liveSolverManager: LiveSolverManager? = null
    private var liveMoveCounterEnabled = false
    private val liveMoveCounterText = MutableLiveData<String?>("")
    private val liveSolverCalculating = MutableLiveData<Boolean?>(false)
    private val liveMoveCounterDeviation = MutableLiveData<Int?>(0)

    // Pre-computation cache for next possible moves (sequential, one solver at a time)
    private val nextMovesCache = ConcurrentHashMap<String?, Int?>()
    private var preComputeExecutor: ExecutorService? = null

    @Volatile
    private var preComputeRunning = false

    @Volatile
    private var preComputeCancelled = false

    // Hint button reset timer to avoid race condition when loading games from history
    private var hintButtonResetRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val solverManager: SolverManager
        /**
         * Get the solver manager instance, initializing it if necessary
         * 
         * @return The solver manager instance
         */
        get() {
            d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.getSolverManager(): Getting SolverManager singleton instance")
            val solverManager = SolverManager.getInstance()

            // Always set this GameStateManager as the listener.
            // The SolverManager is a singleton that survives Activity recreation,
            // so it may hold a stale listener from a destroyed GameStateManager.
            if (solverManager.getListener() !== this) {
                d(
                    "[SOLUTION_SOLVER][DEEPLINK_DIAG] GameStateManager replacing SolverListener (old=%s, new=%s)",
                    solverManager.getListener(),
                    this
                )
                solverManager.setListener(this)
            }
            return solverManager
        }

    /**
     * Start a new random game
     */
    open fun startNewGame() {
        d("GameStateManager: startNewGame() called")

        startGame()
    }

    /**
     * Start a new game
     */
    fun startGame() {
        d("GameStateManager: startGame() called")

        // Reset loaded game flags - new games should use current difficulty settings
        isLoadedFromSave = false
        isLoadedFromHistory = false
        currentHistoryPath = null
        d("[NEW_GAME] Reset isLoadedFromSave, isLoadedFromHistory and currentHistoryPath, using current difficulty settings")

        // Reset any existing solver state to ensure a clean calculation for the new game
        val solverManager = this.solverManager
        solverManager.resetInitialization()
        solverManager.cancelSolver() // Cancel any running solver process
        isSolverRunning.setValue(false) // Reset immediately to avoid race condition with calculateSolutionAsync guard

        // Clear any existing solution to prevent it from being reused
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null
        preCompRobotOrder.clear()
        resetSolverRestartCount()
        resetLastSolutionMinMoves()

        // Reset regeneration counter
        regenerationCount = 0
        keepCurrentMapDespiteDifficulty = false // Reset when starting a completely new game

        // Reset UI timer for the new game
        resetUiTimer()

        // Reset history tracking so a new history entry is created for this game
        resetGameTimer()
        startGameTimer()

        // Create a new valid game (will regenerate if solution is too simple)
        createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight)

        // Record start time
        startTime = System.currentTimeMillis()

        d("GameStateManager: startGame() complete")
    }

    /**
     * Start a level game
     * 
     * @param levelId Level ID to load
     */
    fun startLevelGame(levelId: Int) {
        d("GameStateManager: startLevelGame() called with levelId: %d", levelId)

        // If solver is already running, don't create a new game state to avoid mismatch
        if (java.lang.Boolean.TRUE == isSolverRunning.getValue()) {
            d("[SOLUTION_SOLVER] startLevelGame: Solver already running, not creating new game state")
            return
        }

        // Reset any existing solver state to ensure a clean calculation for the new level
        val solverManager = this.solverManager
        solverManager.resetInitialization()
        solverManager.cancelSolver() // Cancel any running solver process
        isSolverRunning.setValue(false) // Reset immediately to avoid race condition with calculateSolutionAsync guard

        // Clear any existing solution to prevent it from being reused
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null
        preCompRobotOrder.clear()

        // Load level from assets
        val state = GameState.loadLevel(getApplication<Application?>()!!, levelId)
        state.levelId = levelId
        state.levelName = "Level " + levelId

        // Save last played level for scroll position in level selection
        LevelCompletionManager.getInstance(getApplication<Application?>()!!).lastPlayedLevel =
            levelId

        // Set reference to this GameStateManager in the new state
        state.setGameStateManager(this)

        // Set the current state
        currentState.setValue(state)
        this.levelName = "Level-" + levelId

        // Reset move counts and history
        setMoveCount(0)
        resetSquaresMoved() // reset squares moved count
        setGameComplete(false)
        stateHistory.clear()
        squaresMovedHistory.clear()
        clearNextMovesCache()


        // Disable live-move-mode for level games (only available in random games)
        setLiveMoveCounterEnabled(false)

        // Initialize the solver with the grid elements from the loaded level
        val gridElements = state.gridElements
        d(
            "[SOLUTION_SOLVER] Initializing solver with %d grid elements from level %d",
            gridElements.size, levelId
        )
        this.solverManager.initialize(gridElements)


        // Check if level has a predefined solution (for complex levels like 140)
        if (state.hasPredefinedSolution()) {
            d(
                "[SOLUTION_SOLVER] Level %d has predefined solution with %d moves",
                levelId, state.predefinedNumMoves
            )
            this.solverManager.setPredefinedSolution(
                state.predefinedSolution,
                state.predefinedNumMoves
            )
        }

        // Start calculating the solution automatically
        calculateSolutionAsync(null)

        // Reset UI timer for the new level
        resetUiTimer()

        // Record start time
        startTime = System.currentTimeMillis()

        d("GameStateManager: startLevelGame() complete for level %d", levelId)

        resetStatistics()
    }

    /**
     * Load a specific level
     * 
     * @param levelId Level ID to load
     */
    open fun loadLevel(levelId: Int) {
        // Load level from assets
        val newState = GameState.loadLevel(getApplication<Application?>()!!, levelId)
        newState.levelId = levelId

        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this)

        currentState.setValue(newState)
        moveCount.setValue(0)
        isGameComplete.setValue(false)

        // Reset history tracking so a new history entry is created for this game
        resetGameTimer()
        startGameTimer()

        // Initialize the solver with grid elements
        val gridElements = newState.gridElements
        this.solverManager.initialize(gridElements)
    }

    /**
     * Load a saved game
     * 
     * @param saveId Save slot ID
     */
    fun loadGame(saveId: Int) {
        if (saveId >= 0) {
            // Load saved game using the original method
            val newState = GameState.loadSavedGame(getApplication<Application?>()!!, saveId)
            if (newState != null) {
                // Set flag to skip min/max moves validation for loaded games
                isLoadedFromSave = true
                // Ensure this is NOT treated as a history game
                isLoadedFromHistory = false

                d(
                    "[LOAD_GAME] Loading saved game with difficulty: %d (current settings difficulty: %d)",
                    newState.difficulty, Preferences.difficulty
                )

                // Apply the loaded game state using the shared method
                applyLoadedGameState(newState)
                d("Successfully loaded game from slot %d", saveId)
            } else {
                e("Failed to load game state from slot %d", saveId)
            }
        }
    }

    /**
     * Apply a loaded GameState to the current game
     * This method contains the common logic used by both saved games and deep links
     * 
     * @param newState The new GameState to apply
     * @return true if successful, false otherwise
     */
    fun applyLoadedGameState(newState: GameState): Boolean {
        // Mark that a new game was loaded - timer should reset
        isNewGameLoaded = true
        newGameLoadedEvent.setValue(true) // Trigger observer
        d("[TIMER] New game loaded - timer will reset")
        d("[HINT_SYSTEM] New game loaded event triggered")


        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this)

        // Analyze the loaded data and show all elements loaded in the log
        d("[GAME_LOAD] Analyzing loaded game data")
        d("[GAME_LOAD] Board size: %d x %d", newState.width, newState.height)
        d("[GAME_LOAD] Map name: %s", newState.levelName)


        // Store saved solutions string for later use (after reset)
        val savedSolutionsStr = newState.savedSolutions
        if (savedSolutionsStr != null && !savedSolutionsStr.isEmpty()) {
            d("[SOLUTIONS_SAVE_LOAD] Found saved solutions in metadata: %s", savedSolutionsStr)
        }

        // Log all game elements
        var robotCount = 0
        var targetCount = 0
        var horizontalWallCount = 0
        var verticalWallCount = 0
        for (element in newState.gameElements) {
            when (element.type) {
                GameElement.TYPE_ROBOT -> {
                    robotCount++
                    d(
                        "[GAME_LOAD] Found robot at (%d,%d) with color %d",
                        element.x, element.y, element.color
                    )
                }

                GameElement.TYPE_TARGET -> {
                    targetCount++
                    d(
                        "[GAME_LOAD] Found target at (%d,%d) with color %d",
                        element.x, element.y, element.color
                    )
                }

                GameElement.TYPE_HORIZONTAL_WALL -> horizontalWallCount++
                GameElement.TYPE_VERTICAL_WALL -> verticalWallCount++
            }
        }

        // Log summary of elements
        d(
            "[GAME_LOAD] Element summary - Robots: %d, Targets: %d, Horizontal walls: %d, Vertical walls: %d",
            robotCount, targetCount, horizontalWallCount, verticalWallCount
        )

        // Store the map name from the loaded state
        this.levelName = newState.levelName
        d("[MAPNAME] GameStateManager - Set currentMapName to: %s", this.levelName)

        // Check for targets in the board data (cellType and targetColors)
        var boardTargetCount = 0
        for (y in 0..<newState.height) {
            for (x in 0..<newState.width) {
                if (newState.getCellType(x, y) == Constants.TYPE_TARGET) {
                    boardTargetCount++
                    val targetColor = newState.getTargetColor(x, y)
                    d("[GAME_LOAD] Found board target at (%d,%d) with color %d", x, y, targetColor)
                }
            }
        }
        d("[GAME_LOAD] Board target count: %d", boardTargetCount)

        // If there are no targets in gameElements but there are targets in the board data,
        // we need to recreate the GameElements for the targets
        if (targetCount == 0 && boardTargetCount > 0) {
            w(
                "[GAME_LOAD] No targets found in gameElements but %d targets found in board data. Recreating targets.",
                boardTargetCount
            )
            // Recreate target elements based on board data
            for (y in 0..<newState.height) {
                for (x in 0..<newState.width) {
                    if (newState.getCellType(x, y) == Constants.TYPE_TARGET) {
                        val color = newState.getTargetColor(x, y)
                        val target = GameElement(GameElement.TYPE_TARGET, x, y)
                        target.color = color
                        newState.gameElements.add(target)
                        d(
                            "[GAME_LOAD] Recreated target element at (%d,%d) with color %d",
                            x,
                            y,
                            color
                        )
                    }
                }
            }
        }

        // If there are no targets at all, throw an exception
        if (targetCount == 0 && boardTargetCount == 0) {
            val errorMessage = "No targets found in loaded game data"
            val t = Throwable()
            e(t, "[GAME_LOAD] %s", errorMessage)
            throw IllegalStateException(errorMessage)
        }

        // Adjust robotCount to match the actual number of targets in this game.
        // This is important for levels and external maps that may have fewer targets
        // than the settings value (Preferences.robotCount).
        val actualTargetCount = max(targetCount, boardTargetCount)
        val previousRobotCount = newState.getRobotCount()
        if (actualTargetCount > 0 && actualTargetCount != previousRobotCount) {
            newState.setRobotCount(actualTargetCount)
            d(
                "[GAME_LOAD] Adjusted robotCount from %d to %d to match actual target count",
                previousRobotCount, actualTargetCount
            )
        }

        // Ensure robots are reset to their initial positions
        newState.resetRobotPositions()
        d("[GAME_LOAD] Robots reset to initial positions after loading")

        // Synchronize targets to ensure board array matches gameElements
        val syncedTargets = newState.synchronizeTargets()
        if (syncedTargets > 0) {
            d("[GAME_LOAD] Synchronized %d targets after loading", syncedTargets)
        }

        // Update LiveData values
        currentState.setValue(newState)
        moveCount.setValue(newState.moveCount)
        isGameComplete.setValue(newState.isComplete)

        // Update board size globals for UI and other components
        MainActivity.boardSizeX = newState.width
        MainActivity.boardSizeY = newState.height
        d(
            "[BOARD_SIZE_DEBUG] Updated MainActivity board size to: %dx%d",
            newState.width,
            newState.height
        )

        // Store walls from loaded game in WallStorage if generateNewMapEachTime is off
        if (!Preferences.generateNewMapEachTime) {
            val gridElements = buildGridElements(newState)
            getInstance().storeWallsForBoardSize(
                gridElements, newState.width, newState.height
            )
            d(
                "[GAME_LOAD] Stored walls from loaded savegame for board size %dx%d",
                newState.width, newState.height
            )
        }

        // Clear old game data and force solver re-initialization with new map
        stateHistory.clear()
        squaresMovedHistory.clear()
        clearNextMovesCache()
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null
        preCompRobotOrder.clear()
        this.solverManager.resetInitialization()
        d("[GAME_LOAD] Cleared old game data and reset solver for new map")

        // Reset history tracking so this replay is treated as a new game session
        resetGameTimer()
        d("[HISTORY] Reset game timer for loaded history entry - new completion session")

        // Initialize solver with grid elements from the loaded map
        initializeSolverForState(newState)


        // NOW deserialize and set solutions AFTER reset and initialization
        if (savedSolutionsStr != null && !savedSolutionsStr.isEmpty()) {
            d(
                "[SOLUTIONS_SAVE_LOAD] Deserializing saved solutions after reset: %s",
                savedSolutionsStr
            )
            val loadedSolutions = deserializeSolutions(savedSolutionsStr)
            if (loadedSolutions != null && !loadedSolutions.isEmpty()) {
                // Use the first solution as the current solution
                this.currentSolution = loadedSolutions.get(0)
                this.currentSolutionStep = 0
                d(
                    "[SOLUTIONS_SAVE_LOAD] Loaded %d solutions from save, using first solution with %d moves",
                    loadedSolutions.size,
                    if (currentSolution!!.moves != null) currentSolution!!.moves.size else 0
                )


                // Set predefined solution in SolverManager so solver doesn't need to run
                val solverManager = this.solverManager
                if (solverManager != null && currentSolution!!.moves != null) {
                    // Store the loaded solutions in GameStateManager for re-saving
                    this.loadedSolutions = loadedSolutions
                    d(
                        "[SOLUTIONS_SAVE_LOAD] Stored %d solutions in GameStateManager for re-saving",
                        loadedSolutions.size
                    )
                    // Convert solution to string format for predefined solution
                    // Use the same format as serializeAllSolutions: colorDirection (e.g., 0U,1R,0D,1L)
                    val solutionStr = StringBuilder()
                    for (move in currentSolution!!.moves) {
                        if (solutionStr.length > 0) {
                            solutionStr.append(" ")
                        }
                        // Cast to RRGameMove to access methods
                        val rrMove = move as RRGameMove?
                        val color = rrMove!!.color
                        val direction = rrMove.direction


                        // Append color as digit
                        solutionStr.append(color)


                        // Append direction using same codes as serializeAllSolutions
                        d("[SOLUTIONS_SAVE_LOAD] interpreting color and binary direction " + color + "-" + direction + " in solution")
                        when (direction) {
                            1 -> solutionStr.append("U")
                            2 -> solutionStr.append("R")
                            4 -> solutionStr.append("D")
                            8 -> solutionStr.append("L")
                            else -> {
                                e("[SOLUTIONS_SAVE_LOAD] unknown direction " + direction + " in solution")
                                solutionStr.append(direction)
                            }
                        }
                    }
                    solverManager.setPredefinedSolution(
                        solutionStr.toString(),
                        currentSolution!!.moves.size
                    )
                    d(
                        "[SOLUTIONS_SAVE_LOAD] Set predefined solution in SolverManager: %s",
                        solutionStr.toString()
                    )
                }
            }
        }


        // Only start solver if no solution was loaded from save file
        if (currentSolution == null || currentSolution!!.moves == null || currentSolution!!.moves.isEmpty()) {
            d("[GAME_LOAD] No saved solution found, starting solver for loaded map")
            calculateSolutionAsync(null)
        } else {
            d(
                "[SOLUTIONS_SAVE_LOAD] Using loaded solution with %d moves, skipping solver calculation",
                currentSolution!!.moves.size
            )
            // Signal Fragment that solution was accepted (hides hint container)
            solutionWasAccepted = true
            // Mark solver as not running since we're using a pre-loaded solution
            isSolverRunning.setValue(false)
            // Notify that solution is ready
            if (solutionCallback != null) {
                solutionCallback!!.onSolutionCalculationCompleted(currentSolution)
            }
        }

        return true
    }

    /**
     * Initialize the solver with grid elements from a game state
     * 
     * @param state The game state to initialize the solver with
     */
    private fun initializeSolverForState(state: GameState) {
        // Create a GridElements list that properly includes the targets
        val gridElements = ArrayList<GridElement?>()

        // Convert GameElements to GridElements for the solver
        for (element in state.gameElements) {
            var gridElement: GridElement? = null
            when (element.type) {
                GameElement.TYPE_ROBOT -> {
                    val robotType = "robot_" + getColorName(element.color, false)
                    gridElement = GridElement(element.x, element.y, robotType)
                    d(
                        "[SOLVER_INIT] Added robot GridElement: %s at (%d,%d)",
                        robotType,
                        element.x,
                        element.y
                    )
                }

                GameElement.TYPE_TARGET -> {
                    // Get the raw color value from the element
                    val targetColorId = element.color
                    val colorName = getColorName(targetColorId, false)
                    val targetType = "target_" + colorName


                    // Detailed logging for target conversion
                    d(
                        "[POSSIBLE_UNREACHEABLE_CODE][SOLUTION_SOLVER_TARGET] Converting GameElement target with color ID %d (%s) to GridElement type '%s'",
                        targetColorId,
                        (if (targetColorId == Constants.COLOR_MULTI) "MULTI" else colorName),
                        targetType
                    )


                    // Create GridElement with the correct type
                    gridElement = GridElement(element.x, element.y, targetType)


                    // More verbose logging for multi-color targets
                    if (targetColorId == Constants.COLOR_MULTI) {
                        d(
                            "[POSSIBLE_UNREACHEABLE_CODE][SOLUTION_SOLVER_TARGET] Created multi-color target GridElement: %s at (%d,%d)",
                            targetType, element.x, element.y
                        )
                    } else {
                        d(
                            "[POSSIBLE_UNREACHEABLE_CODE][SOLVER_INIT] Added target GridElement: %s at (%d,%d)",
                            targetType,
                            element.x,
                            element.y
                        )
                    }
                }

                GameElement.TYPE_HORIZONTAL_WALL -> gridElement =
                    GridElement(element.x, element.y, "mh")

                GameElement.TYPE_VERTICAL_WALL -> gridElement =
                    GridElement(element.x, element.y, "mv")
            }

            if (gridElement != null) {
                gridElements.add(gridElement)
            }
        }

        // Initialize solver with our properly constructed grid elements
        this.solverManager.initialize(gridElements)
        d(
            "[SOLVER_INIT] Initialized solver with %d grid elements including robots and targets",
            gridElements.size
        )
    }

    /**
     * Load a history entry
     * 
     * @param historyId History entry ID to load
     */
    fun loadHistoryEntry(historyId: Int) {
        // TODO: Implement history entry loading
        val newState = createRandom()
        newState.levelId = historyId

        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this)

        currentState.setValue(newState)
        moveCount.setValue(0)
        isGameComplete.setValue(false)

        // Initialize the solver with grid elements
        val gridElements = newState.gridElements
        this.solverManager.initialize(gridElements)
    }

    /**
     * Load a game from a history entry
     * 
     * @param mapPath Path to the history entry file
     */
    fun loadHistoryEntry(mapPath: String) {
        d("Loading history entry: %s", mapPath)

        try {
            // Resolve relative path to absolute
            var historyFile = File(mapPath)
            if (!historyFile.isAbsolute()) {
                historyFile = getApplication<Application?>()!!.getFileStreamPath(mapPath)
                d("[HISTORY_LOAD] Resolved relative path to: %s", historyFile.getAbsolutePath())
            }
            if (!historyFile.exists()) {
                e("History file does not exist: %s", historyFile.getAbsolutePath())
                return
            }

            // Read the save data from the history file
            val saveData = StringBuilder()
            FileInputStream(historyFile).use { fis ->
                BufferedReader(InputStreamReader(fis)).use { reader ->
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        saveData.append(line).append("\n")
                    }
                }
            }
            d(
                "[HISTORY_LOAD] Read %d characters from history file: %s",
                saveData.length,
                historyFile.getAbsolutePath()
            )

            // Parse the save data into a GameState
            val newState = parseFromSaveData(saveData.toString(), getApplication<Application?>())
            if (newState != null) {
                // Set flag to skip min/max moves validation for loaded games
                isLoadedFromSave = true
                // Set flag to indicate this is a history game (not a savegame)
                isLoadedFromHistory = true
                // Track the current history path for navigation
                currentHistoryPath = mapPath

                d("[HISTORY_LOAD] Loaded history entry with difficulty: %d", newState.difficulty)

                // Apply the loaded game state using the shared method
                applyLoadedGameState(newState)
                d("[HISTORY_LOAD] Successfully loaded history entry: %s", mapPath)
            } else {
                e("[HISTORY_LOAD] Failed to parse game state from history file: %s", mapPath)
            }
        } catch (e: Exception) {
            e(e, "Error loading history entry: %s", mapPath)
        }
    }

    /**
     * Save the current game to a slot
     * 
     * @param saveId The save slot ID
     * @param isAutoSave true if this is a system autosave, false for manual saves
     * @return true if the game was saved successfully, false otherwise
     */
    /**
     * Save the current game to a slot
     * 
     * @param saveId The save slot ID
     * @return true if the game was saved successfully, false otherwise
     */
    @JvmOverloads
    fun saveGame(saveId: Int, isAutoSave: Boolean = false): Boolean {
        // Slot 0 is reserved for auto-save only - prevent manual saves unless no autosave exists yet
        if (saveId == 0 && !isAutoSave) {
            val autoSaveDir = File(getContext()!!.getFilesDir(), Constants.SAVE_DIRECTORY)
            val autoSaveFileName =
                Constants.SAVE_FILENAME_PREFIX + 0 + Constants.SAVE_FILENAME_EXTENSION
            val autoSaveFile = File(autoSaveDir, autoSaveFileName)
            if (autoSaveFile.exists()) {
                e("[SAVE_PROTECTION] Attempted manual save to slot 0 (auto-save slot) - blocked (autosave already exists)")
                return false
            }
            d("[SAVE_PROTECTION] Allowing manual save to slot 0 - no autosave exists yet")
        }

        d("Saving game to slot %d (autosave: %b)", saveId, isAutoSave)

        // Get the current game state
        val gameState = currentState.getValue()
        if (gameState == null) {
            e("Cannot save game: No valid GameState available")
            // Try to debug why the current state is null
            d(
                "Current state: %s",
                if (currentState == null) "null MutableLiveData" else "MutableLiveData exists but value is null"
            )
            d(
                "Has saved state history: %s",
                if (stateHistory.isEmpty()) "no" else "yes, with " + stateHistory.size + " entries"
            )
            return false
        }

        try {
            // Create save directory if it doesn't exist
            val saveDir = File(getContext()!!.getFilesDir(), Constants.SAVE_DIRECTORY)
            if (!saveDir.exists()) {
                if (!saveDir.mkdirs()) {
                    e("Failed to create save directory")
                    return false
                }
            }

            // Create save file
            val fileName =
                Constants.SAVE_FILENAME_PREFIX + saveId + Constants.SAVE_FILENAME_EXTENSION
            val saveFile = File(saveDir, fileName)

            // Serialize game state to JSON
            val saveData = gameState.serialize()

            // Add additional important metadata if not already present
            val enhancedSaveData = StringBuilder(saveData)

            // Check if we need to add a DIFFICULTY tag - find first line end
            if (!saveData.contains("DIFFICULTY:")) {
                val difficultyLevel = Preferences.difficulty // Get current difficulty setting
                val endOfFirstLine = saveData.indexOf("\n")
                if (endOfFirstLine > 0) {
                    val difficultyTag = "DIFFICULTY:" + difficultyLevel + ";"
                    // Insert right after first semicolon
                    val insertPos = saveData.indexOf(";", 0) + 1
                    enhancedSaveData.insert(insertPos, difficultyTag)
                    d("[SAVEDATA] Added difficulty tag: %s", difficultyTag)
                }
            }

            // Add board size if not already present
            if (!saveData.contains("SIZE:")) {
                val width = gameState.width
                val height = gameState.height
                val endOfFirstLine = enhancedSaveData.indexOf("\n")
                if (endOfFirstLine > 0) {
                    val sizeTag = "SIZE:" + width + "," + height + ";"
                    // Insert right after first semicolon and any other tags we've added
                    val insertPos = enhancedSaveData.indexOf(";", 0) + 1
                    enhancedSaveData.insert(insertPos, sizeTag)
                    d("[SAVEDATA] Added size tag: %s", sizeTag)
                }
            }

            // Add completion status if not already present
            if (!saveData.contains("SOLVED:")) {
                val solved = gameState.isComplete
                val solvedTag = "SOLVED:" + solved + ";"
                val endOfFirstLine = enhancedSaveData.indexOf("\n")
                if (endOfFirstLine > 0) {
                    // Insert right after first semicolon and any other tags we've added
                    val insertPos = enhancedSaveData.indexOf(";", 0) + 1
                    enhancedSaveData.insert(insertPos, solvedTag)
                    d("[SAVEDATA] Added solved tag: %s", solvedTag)
                }
            }

            // Add hint tracking metadata (DRY with history)
            if (!enhancedSaveData.toString().contains("MAX_HINT_USED:")) {
                val maxHintUsed = gameState.maxHintUsedThisSession
                val hintTag = "MAX_HINT_USED:" + maxHintUsed + ";"
                val insertPos = enhancedSaveData.indexOf(";", 0) + 1
                enhancedSaveData.insert(insertPos, hintTag)
                d("[SAVEDATA] Added hint tag: %s", hintTag)
            }


            // Add move count if not already present
            if (!enhancedSaveData.toString().contains("MOVES:")) {
                val moves = gameState.moveCount
                val movesTag = "MOVES:" + moves + ";"
                val insertPos = enhancedSaveData.indexOf(";", 0) + 1
                enhancedSaveData.insert(insertPos, movesTag)
                d("[SAVEDATA] Added moves tag: %s", movesTag)
            }


            // Add map signature for history lookup (DRY with history)
            // Base64-encode to avoid semicolons in signature colliding with header delimiter
            if (!enhancedSaveData.toString().contains("MAP_SIG:")) {
                val mapSig = gameState.generateMapSignature()
                if (mapSig != null && !mapSig.isEmpty()) {
                    val encoded = Base64.encodeToString(
                        mapSig.toByteArray(StandardCharsets.UTF_8),
                        Base64.NO_WRAP
                    )
                    val sigTag = "MAP_SIG:" + encoded + ";"
                    val insertPos = enhancedSaveData.indexOf(";", 0) + 1
                    enhancedSaveData.insert(insertPos, sigTag)
                    d("[SAVEDATA] Added map signature tag (base64-encoded)")
                }
            }


            // Add all solver solutions if available
            if (!enhancedSaveData.toString().contains("SOLUTIONS:")) {
                val solutionsTag = serializeAllSolutions()
                if (solutionsTag != null && !solutionsTag.isEmpty()) {
                    val insertPos = enhancedSaveData.indexOf(";", 0) + 1
                    enhancedSaveData.insert(insertPos, solutionsTag)
                    // Count solutions: extract value between SOLUTIONS: and ;
                    val solutionsValue =
                        solutionsTag.substring("SOLUTIONS:".length, solutionsTag.length - 1)
                    var solutionCount = 0
                    if (!solutionsValue.isEmpty()) {
                        val parts =
                            solutionsValue.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        for (part in parts) {
                            if (!part.trim { it <= ' ' }.isEmpty()) {
                                solutionCount++
                            }
                        }
                    }
                    d("[SAVEDATA] Added solutions tag with %d solutions", solutionCount)
                }
            }

            // If this is an autosave (slot 0), store settings metadata for quick comparison
            if (saveId == 0) {
                saveAutosaveMetadata(gameState)
            }

            // Write the save data to the file
            try {
                FileOutputStream(saveFile).use { fos ->
                    fos.write(enhancedSaveData.toString().toByteArray())
                    d("Game saved to %s", saveFile.getAbsolutePath())

                    // VERIFICATION: Read back the save file and check for targets
                    val savedContent = readSaveFileContent(saveFile)
                    if (savedContent == null || !validateSaveContainsTargets(
                            savedContent,
                            saveFile
                        )
                    ) {
                        // This is a fatal error - delete the corrupt save file
                        e("[SAVE_VERIFICATION] FATAL ERROR: Save file validation failed - no targets found")
                        e("[SAVE_VERIFICATION] FATAL: Game state information before throw:")
                        e(
                            "[SAVE_VERIFICATION] Width: %d, Height: %d",
                            gameState.width,
                            gameState.height
                        )
                        var targetCount = 0
                        for (y in 0..<gameState.height) {
                            for (x in 0..<gameState.width) {
                                if (gameState.getCellType(x, y) == Constants.TYPE_TARGET) {
                                    targetCount++
                                    e(
                                        "[SAVE_VERIFICATION] Target found at (%d,%d) with color %d",
                                        x, y, gameState.getTargetColor(x, y)
                                    )
                                }
                            }
                        }
                        val t = Throwable()
                        e(
                            t,
                            "[SAVE_VERIFICATION] Total targets in CURRENT game state: %d",
                            targetCount
                        )
                        e(
                            "[SAVE_VERIFICATION] Save file content before deletion (first 200 chars): %s",
                            if (savedContent!!.length > 200) savedContent.substring(
                                0,
                                200
                            ) + "..." else savedContent
                        )
                        saveFile.delete()
                        throw IllegalStateException("Save file validation failed: No targets found in saved game")
                    }
                    return true
                }
            } catch (e: IOException) {
                e("Error saving game: %s", e.message)
                return false
            }
        } catch (e: IllegalStateException) {
            // Log the detailed error for debugging
            e("[SAVE_ERROR] %s", e.message)
            // Rethrow as this is a fatal error that should never occur
            throw RuntimeException("FATAL: " + e.message, e)
        } catch (e: Exception) {
            e("Unexpected error saving game: %s", e.message)
            return false
        }
    }

    /**
     * Validates that a save file contains at least one target.
     * In Roboyard, all games MUST have targets - this is a fatal requirement.
     * 
     * @param saveData The save data string to validate
     * @param saveFile The save file reference (for logging)
     * @return true if the save contains targets, false otherwise
     */
    private fun validateSaveContainsTargets(saveData: String, saveFile: File): Boolean {
        d("[SAVE_VERIFICATION] Validating save file: %s", saveFile.getName())

        // Check for dedicated TARGET_SECTION section (legacy format)
        if (saveData.contains("TARGET_SECTION:")) {
            d("[SAVE_VERIFICATION] Save file contains TARGET_SECTION entries")
            return true
        }

        // First-line format: |T<color>@<x>,<y>; (e.g. |T3@4,13; or |T-1@4,13; for multi)
        if (saveData.matches("(?s).*\\|T-?\\d+@\\d+,\\d+;.*".toRegex())) {
            d("[SAVE_VERIFICATION] Save file contains targets (T@x,y format)")
            return true
        }

        // Compact format: t<color_letter><x>,<y>; (e.g. ty4,13; tb2,11; tm3,4; for multi)
        if (saveData.matches("(?s).*(?:^|\\n|;)t[rgbyms]\\d+,\\d+;.*".toRegex())) {
            d("[SAVE_VERIFICATION] Save file contains targets (compact t<color> format)")
            return true
        }

        // Legacy format: target_<color><x>,<y>;
        if (saveData.matches("(?s).*target_(red|green|blue|yellow)\\d+,\\d+;.*".toRegex())) {
            d("[SAVE_VERIFICATION] Save file contains targets (legacy target_color format)")
            return true
        }

        // Check for target cell types in board data (cell type 2)
        val lines = saveData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            if (line.contains(Constants.TYPE_TARGET.toString() + ":")) {
                d("[SAVE_VERIFICATION] Save file contains target cell types in board data")
                return true
            }
        }

        // Log the full save data for diagnostics when no targets are found
        e("[SAVE_VERIFICATION] NO TARGETS FOUND IN SAVE DATA:")
        val logLines = saveData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in 0..<min(logLines.size, 50)) { // Limit to 50 lines
            e("[SAVE_VERIFICATION] Line %d: %s", i, logLines[i])
        }

        return false
    }

    /**
     * Reads the content of a save file.
     * 
     * @param saveFile The save file to read
     * @return The content of the save file as a string, or null if reading fails
     */
    private fun readSaveFileContent(saveFile: File?): String? {
        try {
            FileInputStream(saveFile).use { fis ->
                BufferedReader(InputStreamReader(fis)).use { reader ->
                    val content = StringBuilder()
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        content.append(line).append("\n")
                    }
                    return content.toString()
                }
            }
        } catch (e: IOException) {
            e("Error reading save file: %s", e.message)
            return null
        }
    }

    /**
     * Serialize all solver solutions to a compact string format for saving
     * Format: SOLUTIONS:0U,1R,0D|0U,2L,1D|...
     * Each solution separated by |, each move as colorDirection (0U = robot 0 UP)
     * Directions: U=UP, R=RIGHT, D=DOWN, L=LEFT
     * 
     * @return Solutions tag string or null if no solutions available
     */
    private fun serializeAllSolutions(): String? {
        try {
            // First check if we have loaded solutions from a save file
            if (loadedSolutions != null && !loadedSolutions!!.isEmpty()) {
                d(
                    "[SOLUTIONS_SAVE_LOAD] Using %d loaded solutions for re-saving",
                    loadedSolutions!!.size
                )
                return serializeGameSolutions(loadedSolutions)
            }

            val solverManager = this.solverManager
            if (solverManager == null) {
                d("[SOLUTIONS_SAVE_LOAD] SolverManager is null")
                return null
            }

            val solutions = solverManager.getSolutionList()
            if (solutions == null) {
                d("[SOLUTIONS_SAVE_LOAD] Solution list is null")
                return null
            }
            if (solutions.isEmpty()) {
                d("[SOLUTIONS_SAVE_LOAD] Solution list is empty (size=0)")
                return null
            }

            d("[SOLUTIONS_SAVE_LOAD] Found %d solutions from solver", solutions.size)

            val sb = StringBuilder("SOLUTIONS:")
            var first = true
            var serializedCount = 0

            for (i in solutions.indices) {
                val gameSolution = solverManager.getSolution(i)
                if (gameSolution == null) {
                    d("[SOLUTIONS_SAVE_LOAD] Solution %d: GameSolution is null", i)
                    continue
                }
                if (gameSolution.moves == null) {
                    d("[SOLUTIONS_SAVE_LOAD] Solution %d: Moves list is null", i)
                    continue
                }
                if (gameSolution.moves.isEmpty()) {
                    d("[SOLUTIONS_SAVE_LOAD] Solution %d: Moves list is empty", i)
                    continue
                }

                d("[SOLUTIONS_SAVE_LOAD] Solution %d: Has %d moves", i, gameSolution.moves.size)

                if (!first) {
                    sb.append("|")
                }
                first = false


                // Encode each move as colorDirection
                var firstMove = true
                for (move in gameSolution.moves) {
                    if (!firstMove) {
                        sb.append(",")
                    }
                    firstMove = false


                    // Cast to RRGameMove to access methods
                    val rrMove = move as RRGameMove?
                    val color = rrMove!!.color
                    val direction = rrMove.direction

                    sb.append(color)
                    d("[SOLUTIONS_SAVE_LOAD] serializing direction " + direction)
                    when (direction) {
                        1 -> sb.append("U")
                        2 -> sb.append("R")
                        4 -> sb.append("D")
                        8 -> sb.append("L")
                        else -> {
                            e("[SOLUTIONS_SAVE_LOAD] unknown direction " + direction)
                            sb.append(direction)
                        }
                    }
                }

                serializedCount++
            }

            sb.append(";")

            d("[SOLUTIONS_SAVE_LOAD] Serialized %d solutions", serializedCount)
            return sb.toString()
        } catch (e: Exception) {
            e(e, "[SOLUTIONS_SAVE_LOAD] Error serializing solutions: %s", e.message)
            return null
        }
    }

    /**
     * Serialize GameSolution objects directly (used for re-saving loaded games)
     * @param gameSolutions List of GameSolution objects to serialize
     * @return Solutions tag string or null if no solutions available
     */
    private fun serializeGameSolutions(gameSolutions: MutableList<GameSolution?>?): String? {
        try {
            if (gameSolutions == null || gameSolutions.isEmpty()) {
                d("[SOLUTIONS_SAVE_LOAD] No game solutions to serialize")
                return null
            }

            val sb = StringBuilder("SOLUTIONS:")
            var first = true
            var serializedCount = 0

            for (gameSolution in gameSolutions) {
                if (gameSolution == null || gameSolution.moves == null || gameSolution.moves.isEmpty()) {
                    d("[SOLUTIONS_SAVE_LOAD] Skipping empty solution")
                    continue
                }

                if (!first) {
                    sb.append("|")
                }
                first = false


                // Encode each move as colorDirection
                var firstMove = true
                for (move in gameSolution.moves) {
                    if (!firstMove) {
                        sb.append(",")
                    }
                    firstMove = false


                    // Cast to RRGameMove to access methods
                    val rrMove = move as RRGameMove?
                    val color = rrMove!!.color
                    val direction = rrMove.direction

                    sb.append(color)
                    when (direction) {
                        1 -> sb.append("U")
                        2 -> sb.append("R")
                        4 -> sb.append("D")
                        8 -> sb.append("L")
                        else -> sb.append("?")
                    }
                }

                serializedCount++
            }

            sb.append(";")

            d("[SOLUTIONS_SAVE_LOAD] Serialized %d game solutions", serializedCount)
            return sb.toString()
        } catch (e: Exception) {
            e(e, "[SOLUTIONS_SAVE_LOAD] Error serializing game solutions: %s", e.message)
            return null
        }
    }

    /**
     * Deserialize solutions from save data and use them instead of running solver
     * 
     * @param solutionsString The SOLUTIONS: tag value from save metadata
     * @return List of GameSolution objects or null if parsing failed
     */
    private fun deserializeSolutions(solutionsString: String?): MutableList<GameSolution?>? {
        try {
            if (solutionsString == null || solutionsString.isEmpty()) {
                return null
            }

            val solutions: MutableList<GameSolution?> = ArrayList<GameSolution?>()
            val solutionStrings =
                solutionsString.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (solutionStr in solutionStrings) {
                if (solutionStr.isEmpty()) {
                    continue
                }

                val solution = GameSolution()
                val moves =
                    solutionStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                for (moveStr in moves) {
                    if (moveStr.length < 2) {
                        continue
                    }


                    // Parse color (all chars except last)
                    val color = moveStr.substring(0, moveStr.length - 1).toInt()


                    // Parse direction (last char)
                    val dirChar = moveStr.get(moveStr.length - 1)
                    val direction: ERRGameMove?
                    when (dirChar) {
                        'U' -> direction = ERRGameMove.UP
                        'R' -> direction = ERRGameMove.RIGHT
                        'D' -> direction = ERRGameMove.DOWN
                        'L' -> direction = ERRGameMove.LEFT
                        else -> direction = ERRGameMove.NOMOVE
                    }


                    // Create robot piece (position 0,0 is placeholder, color is what matters)
                    val piece = RRPiece(0, 0, color, color)
                    solution.addMove(RRGameMove(piece, direction))
                }

                solutions.add(solution)
            }

            d("[SOLUTIONS_SAVE_LOAD] Deserialized %d solutions from save data", solutions.size)
            return solutions
        } catch (e: Exception) {
            e(e, "[SOLUTIONS_SAVE_LOAD] Error deserializing solutions: %s", e.message)
            return null
        }
    }


    /**
     * Handle a touch on the game grid
     * 
     * @param x      Grid X coordinate
     * @param y      Grid Y coordinate
     * @param action Touch action type
     * @return
     */
    fun handleGridTouch(x: Int, y: Int, action: Int): Boolean {
        d("[TOUCH] Handle grid touch at (%d,%d) with action %d", x, y, action)

        val state = getCurrentState().getValue()
        if (state != null) {
            // Get the currently selected robot
            val selectedRobot = state.getSelectedRobot()

            if (action == MotionEvent.ACTION_UP) {
                // Check if the user tapped on a robot - if so, select it
                val touchedRobot = state.getRobotAt(x, y)

                if (touchedRobot != null) {
                    // User tapped on a robot - select it
                    d("[TOUCH] Selecting robot at (%d,%d)", x, y)
                    state.setSelectedRobot(touchedRobot)

                    // Add used robot to the set
                    if (touchedRobot.color >= 0) {
                        robotsUsed.add(touchedRobot.color)
                    }

                    currentState.setValue(state)
                } else if (selectedRobot != null) {
                    // User tapped on an empty space - try to move the selected robot
                    val robotX = selectedRobot.x
                    val robotY = selectedRobot.y

                    // Determine movement direction
                    var dx = 0
                    var dy = 0

                    if (robotX == x) {
                        // Moving vertically
                        dy = if (y > robotY) 1 else -1
                    } else if (robotY == y) {
                        // Moving horizontally
                        dx = if (x > robotX) 1 else -1
                    } else {
                        // Diagonal tap - determine primary direction
                        val deltaX = x - robotX
                        val deltaY = y - robotY

                        if (abs(deltaX) > abs(deltaY)) {
                            // Horizontal movement takes priority
                            dx = if (deltaX > 0) 1 else -1
                            dy = 0
                        } else {
                            // Vertical movement takes priority
                            dx = 0
                            dy = if (deltaY > 0) 1 else -1
                        }
                    }

                    // Move the robot using the animation system
                    if (dx != 0 || dy != 0) {
                        d("[TOUCH] Moving robot in direction dx=%d, dy=%d", dx, dy)
                        moveRobotInDirection(dx, dy)
                    }
                }
            }
        }
        return true
    }

    /**
     * Move the selected robot in a specific direction until it hits an obstacle
     * This method can be called by both touch interactions and accessibility controls
     * 
     * @param dx Horizontal direction (-1 = left, 0 = no movement, 1 = right)
     * @param dy Vertical direction (-1 = up, 0 = no movement, 1 = down)
     * @return True if the robot moved, false otherwise
     */
    fun moveRobotInDirection(dx: Int, dy: Int): Boolean {
        // Check if move cooldown is active
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMoveTime < MOVE_COOLDOWN_MS) {
            d(
                "[MOVE_COOLDOWN] Move blocked: %dms remaining",
                MOVE_COOLDOWN_MS - (currentTime - lastMoveTime)
            )
            return false
        }


        // Cancel any running pre-computation immediately on robot move
        cancelPreComputation()

        val state = getCurrentState().getValue()
        if (state == null || state.getSelectedRobot() == null) {
            return false
        }

        // If game is resetting, ignore movement commands
        if (isResetting) {
            return false
        }

        val robot = state.getSelectedRobot()

        // Check if the robot is moving back to its last position in the path history (reverse move = undo)
        // But only if game is not already complete - after goal, reverse moves are treated as normal forward moves
        d(
            "[ROBOTS][UNDO] moveRobotInDirection: robot=%d at (%d,%d), dx=%d dy=%d, pathHistory.size=%d, gameComplete=%b",
            robot!!.color,
            robot.x,
            robot.y,
            dx,
            dy,
            pathHistory.size,
            isGameComplete.getValue()
        )
        if (!isGameComplete.getValue()!! && !pathHistory.isEmpty()) {
            val lastPath = pathHistory.get(pathHistory.size - 1)
            val lastColor = lastPath[0]
            val lastFromX = lastPath[1]
            val lastFromY = lastPath[2]
            val lastToX = lastPath[3]
            val lastToY = lastPath[4]
            d(
                "[ROBOTS][UNDO] lastPath: color=%d from=(%d,%d) to=(%d,%d)",
                lastColor, lastFromX, lastFromY, lastToX, lastToY
            )
            // Only applies to the same robot
            if (lastColor == robot.color && robot.x == lastToX && robot.y == lastToY) {
                // Determine which direction the robot would slide to
                val startX2 = robot.x
                val startY2 = robot.y
                var endX2 = startX2
                var endY2 = startY2
                if (dx != 0) {
                    val step = if (dx > 0) 1 else -1
                    var i = startX2 + step
                    while (i >= 0 && i < state.width) {
                        if (state.canRobotMoveTo(robot, i, startY2)) {
                            endX2 = i
                        } else {
                            break
                        }
                        i += step
                    }
                }
                if (dy != 0) {
                    val step = if (dy > 0) 1 else -1
                    var i = startY2 + step
                    while (i >= 0 && i < state.height) {
                        if (state.canRobotMoveTo(robot, startX2, i)) {
                            endY2 = i
                        } else {
                            break
                        }
                        i += step
                    }
                }
                d(
                    "[ROBOTS][UNDO] Would slide to (%d,%d), lastFrom=(%d,%d), match=%b",
                    endX2, endY2, lastFromX, lastFromY, (endX2 == lastFromX && endY2 == lastFromY)
                )
                // If the robot would land exactly on its previous position, treat as undo
                if (endX2 == lastFromX && endY2 == lastFromY) {
                    d(
                        "[ROBOTS][UNDO] Reverse move detected for robot %d: (%d,%d)->(%d,%d), triggering undo",
                        robot.color, lastToX, lastToY, lastFromX, lastFromY
                    )
                    val undoneRobotColor = robot.color
                    // Remove the path history entry first, then undo the game state
                    val removedPath = removeLastPathFromHistory()
                    val undone = undoLastMove()
                    d(
                        "[ROBOTS][UNDO] undoLastMove returned: %b, gameGridView=%s",
                        undone, if (gameGridView != null) "non-null" else "null"
                    )
                    if (undone) {
                        if (gameGridView != null) {
                            // Apply visual undo without touching pathHistory again (already removed above)
                            gameGridView!!.applyPathSegmentUndoVisual(removedPath)
                            // Re-select the robot with full visual treatment (grow animation etc.)
                            gameGridView!!.selectRobotByColor(undoneRobotColor)
                            d(
                                "[ROBOTS][UNDO] Re-selected robot %d after reverse-move undo",
                                undoneRobotColor
                            )
                            gameGridView!!.invalidate()
                        }
                    }
                    return undone
                }
            }
        }

        // Advance preCompRobotOrder: if the moved robot matches the first expected color, remove it
        if (!preCompRobotOrder.isEmpty() && preCompRobotOrder.get(0) == robot.color) {
            preCompRobotOrder.removeAt(0)
            d(
                "[PRECOMP_SOLUTION] Advanced preCompRobotOrder after %s move, remaining first: %s",
                robotColorShort(robot.color),
                if (preCompRobotOrder.isEmpty()) "none" else Companion.robotColorShort(
                    preCompRobotOrder.get(0)!!
                )
            )
        }
        val startX = robot.x
        val startY = robot.y

        // Initialize end position to the current position (in case no movement is possible)
        var endX = startX
        var endY = startY

        // Update the robot's direction if moving horizontally
        if (dx != 0) {
            robot.directionX = dx // Set facing direction
        }

        // Flags to determine which sound to play
        var hitWall = false
        var hitRobot = false

        // Before making a move, push the current state to history for undo functionality
        // We don't need a deep copy - we'll save the state before making any changes
        d(
            "[ROBOTS] Saving current state to history before move. History size before: %d",
            stateHistory.size
        )

        // Create a snapshot of the current state with all elements
        val stateBeforeMove = GameState(state.width, state.height)

        // Copy the board data (walls, targets, etc.)
        for (y in 0..<state.height) {
            for (x in 0..<state.width) {
                val cellType = state.getCellType(x, y)
                stateBeforeMove.setCellType(x, y, cellType)

                // Also copy target colors if this is a target
                if (cellType == Constants.TYPE_TARGET) {
                    val targetColor = state.getTargetColor(x, y)
                    stateBeforeMove.setTargetColor(x, y, targetColor)
                }
            }
        }

        // Copy all game elements including robots, walls, and targets
        val elements = state.gameElements
        if (elements != null) {
            for (element in elements) {
                // Add the element based on its type
                if (element.type == GameElement.TYPE_ROBOT) {
                    stateBeforeMove.addRobot(element.x, element.y, element.color)

                    // Find the newly added robot and set its direction
                    for (newElement in stateBeforeMove.gameElements) {
                        if (newElement.type == GameElement.TYPE_ROBOT && newElement.color == element.color && newElement.x == element.x && newElement.y == element.y) {
                            newElement.directionX = element.directionX
                            break
                        }
                    }
                } else if (element.type == GameElement.TYPE_HORIZONTAL_WALL) {
                    stateBeforeMove.addHorizontalWall(element.x, element.y)
                } else if (element.type == GameElement.TYPE_VERTICAL_WALL) {
                    stateBeforeMove.addVerticalWall(element.x, element.y)
                } else if (element.type == GameElement.TYPE_TARGET) {
                    stateBeforeMove.addTarget(element.x, element.y, element.color)
                }
            }
        }

        // Copy other game state variables that have setters
        stateBeforeMove.levelId = state.levelId
        stateBeforeMove.levelName = state.levelName
        stateBeforeMove.moveCount = state.moveCount
        stateBeforeMove.setCompleted(state.isComplete)
        stateBeforeMove.setRobotCount(state.getRobotCount())
        stateBeforeMove.setTargetColors(state.getTargetColors())
        stateBeforeMove.difficulty = state.difficulty

        // Copy the hint count
        for (i in 0..<state.hintCount) {
            stateBeforeMove.incrementHintCount()
        }

        stateBeforeMove.uniqueMapId = state.uniqueMapId

        // Store initial robot positions for robot reset functionality
        if (state.initialRobotPositions != null) {
            stateBeforeMove.initialRobotPositions = HashMap<Int?, IntArray?>()
            for (entry in state.initialRobotPositions!!.entries) {
                val positionCopy = intArrayOf(entry.value!![0], entry.value!![1])
                stateBeforeMove.initialRobotPositions!!.put(entry.key, positionCopy)
            }
        }

        // Save the complete state for undo
        stateHistory.add(stateBeforeMove)
        squaresMovedHistory.add(getSquaresMoved().getValue())
        d("[ROBOTS] Saved complete state to history. History size now: %d", stateHistory.size)

        // Reset collision info
        var hitRobotElement: GameElement? = null


        // Check for movement in X direction
        if (dx != 0) {
            val step = if (dx > 0) 1 else -1
            var i = startX + step
            while (i >= 0 && i < state.width) {
                if (state.canRobotMoveTo(robot, i, startY)) {
                    endX = i
                } else {
                    // Found an obstacle
                    val robotAtPosition = state.getRobotAt(i, startY)
                    if (robotAtPosition != null) {
                        hitRobot = true
                        hitRobotElement = robotAtPosition
                    } else {
                        hitWall = true
                    }
                    break
                }
                i += step
            }
        }

        // Check for movement in Y direction
        if (dy != 0) {
            val step = if (dy > 0) 1 else -1
            var i = startY + step
            while (i >= 0 && i < state.height) {
                if (state.canRobotMoveTo(robot, startX, i)) {
                    endY = i
                } else {
                    // Found an obstacle
                    val robotAtPosition = state.getRobotAt(startX, i)
                    if (robotAtPosition != null) {
                        hitRobot = true
                        hitRobotElement = robotAtPosition
                    } else {
                        hitWall = true
                    }
                    break
                }
                i += step
            }
        }


        // Store collision info for GameGridView to use (DRY - avoid recalculating)
        this.lastMoveHitWall = hitWall
        this.lastMoveHitRobot = hitRobot
        this.lastMoveHitRobotElement = hitRobotElement

        // Calculate the distance moved
        val distanceMoved = abs(endX - startX) + abs(endY - startY)

        if (distanceMoved > 0) {
            // Create a copy of the current position
            val originalX = startX
            val originalY = startY
            val targetX = endX
            val targetY = endY

            // Log the movement initiation
            d(
                "[ROBOT][HINT_SYSTEM] Movement INITIATED: Robot %d moving from (%d,%d) to (%d,%d)",
                robot.color, originalX, originalY, targetX, targetY
            )

            // Only increment counters if game is not already complete
            // Once goal is reached, stop counting moves and squares
            val wasFirstMove = pathHistory.isEmpty()
            if (!isGameComplete.getValue()!!) {
                setSquaresMoved(getSquaresMoved().getValue()!! + distanceMoved)

                // Increment move count
                // The value in this GameStateManager (UI)
                setMoveCount(getMoveCount().getValue()!! + 1)

                // Also update the move count in the GameState object itself (logic)
                // This ensures state.getMoveCount() returns the correct value
                state.moveCount = getMoveCount().getValue()!!

                // Save history immediately on first move (before threshold)
                // Move count is 0 here since game is not complete yet
                if (wasFirstMove && !isHistorySaved) {
                    d("[HISTORY] First move detected, saving history immediately (async)")
                    isHistorySaved = true
                    // Run on background thread to prevent ANR
                    Thread(Runnable {
                        try {
                            saveToHistory()
                        } catch (e: Exception) {
                            e(e, "[HISTORY] Error saving history on first move")
                        }
                    }).start()
                }
            } else {
                d("[GAME_COMPLETE] Game already completed, not incrementing move/squares counters")
            }

            // Track the robot and direction for hint verification
            var directionConstant = 0
            if (dx > 0) directionConstant = 2 // RIGHT
            else if (dx < 0) directionConstant = 8 // LEFT 
            else if (dy < 0) directionConstant = 1 // UP
            else if (dy > 0) directionConstant = 4 // DOWN


            // Store the last moved robot and direction in the GameState
            state.lastMovedRobot = robot
            state.lastMoveDirection = directionConstant

            d(
                "[HINT_SYSTEM] Robot moved: color=%d, direction=%d",
                robot.color,
                directionConstant
            )
            d("[HINT_SYSTEM] Updated moveCount in GameState to %d", state.moveCount)

            // Create completion callback for when animation finishes
            val completionCallback = Runnable {
                // Update the robot's actual position after animation completes
                robot.x = targetX
                robot.y = targetY

                // Check for game completion after animation - but only if not already complete
                // This prevents triggering goal event twice
                if (!isGameComplete.getValue()!! && state.areAllRobotsAtTargets()) {
                    setGameComplete(true)
                } else if (!isGameComplete.getValue()!! && state.isRobotOnWrongTarget(robot)) {
                    wrongRobotAtTarget.setValue(robot.color)
                    wrongRobotAtTarget.setValue(-1)
                }

                // Notify observers that the state has changed
                val currentStateLiveData = getCurrentState()
                if (currentStateLiveData is MutableLiveData<*>) {
                    (currentStateLiveData as MutableLiveData<GameState?>).setValue(state)
                }

                // Trigger live solver after move completes
                triggerLiveSolver()
            }

            // Queue this movement for animation
            if (animationsEnabled && robotAnimationManager != null) {
                d(
                    "[ANIM] Attempting to queue robot animation with manager=%s",
                    if (robotAnimationManager != null) "active" else "null"
                )

                // Make absolutely sure the GameGridView is connected
                if (gameGridView != null && robotAnimationManager.getGameGridView() == null) {
                    d("[ANIM] Fixing GameGridView connection to animation manager")
                    robotAnimationManager.setGameGridView(gameGridView)
                }

                // Capture current position for path tracking
                val oldX = robot.x
                val oldY = robot.y

                // Create enhanced completion callback that tracks paths
                val enhancedCallback = Runnable {
                    // First run the original completion callback
                    completionCallback.run()

                    // Then update the path tracking in GameGridView
                    if (gameGridView != null) {
                        gameGridView!!.handleRobotMovementEffects(state, robot, oldX, oldY)
                    }

                    // Add this robot to the used set
                    if (robot.color >= 0) {
                        robotsUsed.add(robot.color)
                    }
                }

                // Queue the animation with the enhanced callback
                robotAnimationManager.queueRobotMove(
                    robot,
                    originalX,
                    originalY,
                    targetX,
                    targetY,
                    enhancedCallback
                )
            } else {
                // Immediate mode - update position without animation
                d(
                    "[ANIM] Animations disabled or manager null (enabled=%b, manager=%s), moving robot immediately",
                    animationsEnabled, if (robotAnimationManager != null) "exists" else "null"
                )
                robot.x = targetX
                robot.y = targetY

                // Check for game completion - but only if not already complete
                // This prevents triggering goal event twice
                if (!isGameComplete.getValue()!! && state.areAllRobotsAtTargets()) {
                    setGameComplete(true)
                } else if (!isGameComplete.getValue()!! && state.isRobotOnWrongTarget(robot)) {
                    wrongRobotAtTarget.setValue(robot.color)
                    wrongRobotAtTarget.setValue(-1)
                }

                // Notify observers
                val currentStateLiveData = getCurrentState()
                if (currentStateLiveData is MutableLiveData<*>) {
                    (currentStateLiveData as MutableLiveData<GameState?>).setValue(state)
                }

                // Trigger live solver after move completes (immediate mode)
                triggerLiveSolver()
            }


            // Update the last move time to enforce cooldown
            lastMoveTime = System.currentTimeMillis()
            d("[MOVE_COOLDOWN] Move completed, cooldown activated for 2 seconds")

            return true
        }

        return false
    }


    val hint: IGameMove?
        /**
         * Get a hint for the next move
         * 
         * @return The next move according to the solver, or null if no solution exists
         */
        get() {
            if (currentSolution != null && currentSolution!!.moves != null && currentSolutionStep < currentSolution!!.moves.size) {
                // Increment hint counter for level completion statistics

                hintsShown++

                val move = currentSolution!!.moves.get(currentSolutionStep)
                incrementSolutionStep()
                return move
            }
            return null
        }


    /**
     * Navigate to main menu
     */
    fun navigateToMainMenu() {
        d("GameStateManager: navigateToMainMenu() called")
        // Use the NavController to navigate to the main menu fragment
        val navController = findNavController(
            (context as FragmentActivity?)!!, R.id.nav_host_fragment
        )
        navController.navigate(R.id.actionGlobalMainMenu)
    }

    /**
     * Navigate to settings screen
     */
    fun navigateToSettings() {
        d("GameStateManager: navigateToSettings() called")
        // Use the NavController to navigate to the settings fragment
        val navController = findNavController(
            (context as FragmentActivity?)!!, R.id.nav_host_fragment
        )
        navController.navigate(R.id.actionGlobalSettings)
    }


    /**
     * Get the context from application
     * 
     * @return Application context
     */
    private fun getContext(): Context? {
        return getApplication<Application?>()!!.getApplicationContext()
    }

    /**
     * Getters for LiveData to observe
     */
    open fun getCurrentState(): LiveData<GameState?> {
        return currentState
    }

    fun getMoveCount(): LiveData<Int?> {
        return moveCount
    }

    fun getSquaresMoved(): LiveData<Int?> {
        return squaresMoved
    }

    fun isGameComplete(): LiveData<Boolean?> {
        return isGameComplete
    }

    fun isSolverRunning(): LiveData<Boolean?> {
        return isSolverRunning
    }

    fun getNewGameLoadedEvent(): LiveData<Boolean?> {
        return newGameLoadedEvent
    }

    /**
     * Add a path entry to the history
     */
    fun addPathToHistory(color: Int, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        pathHistory.add(intArrayOf(color, fromX, fromY, toX, toY))
    }

    /**
     * Remove the last path entry (for undo)
     */
    fun removeLastPathFromHistory(): IntArray? {
        if (pathHistory.isEmpty()) return null
        return pathHistory.removeAt(pathHistory.size - 1)
    }

    /**
     * Clear path history
     */
    fun clearPathHistory() {
        pathHistory.clear()
    }

    /**
     * Store a robot's starting position
     */
    fun setRobotStartingPosition(color: Int, x: Int, y: Int) {
        robotStartingPositions.put(color, intArrayOf(x, y))
    }

    /**
     * Clear all robot starting positions
     */
    fun clearRobotStartingPositions() {
        robotStartingPositions.clear()
    }

    /**
     * Setters for game settings
     */
    fun setSoundEnabled(enabled: Boolean) {
        this.soundEnabled.setValue(enabled)
        // Also update the static Preferences value to ensure consistency
        Preferences.setSoundEnabled(enabled)
    }


    /**
     * Undo the last move if possible
     * 
     * @return true if a move was undone, false otherwise
     */
    fun undoLastMove(): Boolean {
        // Check if there's anything to undo
        if (stateHistory.isEmpty()) {
            d("[ROBOTS] undoLastMove: No history to undo, stateHistory is empty")
            return false
        }

        // Get the previous state and restore it
        val previousState = stateHistory.removeAt(stateHistory.size - 1)
        d(
            "[ROBOTS] undoLastMove: Removed previous state from history, remaining history size: %d",
            stateHistory.size
        )

        if (previousState != null) {
            // Also restore the squares moved count
            if (!squaresMovedHistory.isEmpty()) {
                val previousSquaresMoved =
                    squaresMovedHistory.removeAt(squaresMovedHistory.size - 1)!!
                squaresMoved.setValue(previousSquaresMoved)
                d("[ROBOTS] undoLastMove: Restored squares moved to: %d", previousSquaresMoved)
            } else {
                d("[ROBOTS] undoLastMove: No squares moved history to restore")
            }

            // Restore the state
            currentState.setValue(previousState)
            d("[ROBOTS] undoLastMove: Restored previous game state")

            // Decrement move count
            val moves: Int = moveCount.getValue()!!
            moveCount.setValue(max(0, moves - 1))
            d(
                "[ROBOTS][HINT_SYSTEM] undoLastMove: Decremented move count to: %d",
                max(0, moves - 1)
            )

            // Reset game complete flag if it was set
            if (isGameComplete.getValue()) {
                isGameComplete.setValue(false)
                d("[ROBOTS] undoLastMove: Reset game complete flag")
            }

            // Re-trigger live solver so the display updates instead of disappearing
            triggerLiveSolver()

            return true
        } else {
            e("[ROBOTS] undoLastMove: Previous state was null, this should not happen")
        }

        return false
    }

    /**
     * Set the move count
     * 
     * @param count New move count
     */
    fun setMoveCount(count: Int) {
        moveCount.setValue(count)
    }


    /**
     * Reset squares moved counter
     */
    fun resetSquaresMoved() {
        squaresMoved.setValue(0)
        squaresMovedHistory.clear()
    }

    /**
     * Set the squares moved count
     * 
     * @param squares Number of squares moved
     */
    fun setSquaresMoved(squares: Int) {
        squaresMoved.setValue(squares)
    }

    /**
     * Set whether the game is complete
     * 
     * @param complete Whether the game is complete
     */
    fun setGameComplete(complete: Boolean) {
        val state = currentState.getValue()
        if (state != null) {
            // Guard: prevent duplicate completion triggers for LEVEL games only (not random games)
            // Random games (levelId <= 0) can be completed multiple times
            if (complete && state.levelId > 0 && java.lang.Boolean.TRUE == isGameComplete.getValue()) {
                d(
                    "[HISTORY_SYNC] setGameComplete(true) called but already complete for level %d, ignoring duplicate",
                    state.levelId
                )
                return
            }
            d("Setting game complete: %s for level %d", complete, state.levelId)
            state.setCompleted(complete)
            isGameComplete.setValue(complete)

            // Save level completion data if this is a level game
            if (complete && state.levelId > 0) {
                d(
                    "[SAVE] [STARS] Game completed, saving level completion data for level %d",
                    state.levelId
                )
                val manager = LevelCompletionManager.getInstance(context!!)
                val starsBefore = manager.totalStars
                val data = saveLevelCompletionData(state)

                // Now save the prepared data
                if (data != null) {
                    manager.saveLevelCompletionData(data)
                    d("Saved level completion data: %s", data)


                    // Update history entries with stars/moves before upload
                    val finalLevelId = state.levelId
                    val finalStars = data.getStars()
                    val finalMoves = state.moveCount

                    val currentActivity = this.activity
                    if (currentActivity != null) {
                        Thread(Runnable {
                            try {
                                Thread.sleep(500)
                                val allEntries = getHistoryEntries(currentActivity)
                                var updatedCount = 0
                                val levelName = "Level " + finalLevelId
                                for (entry in allEntries) {
                                    if (entry.mapName != null && entry.mapName == levelName) {
                                        entry.starsEarned = finalStars
                                        if (finalMoves > 0 && (entry.movesMade == 0 || finalMoves < entry.movesMade)) {
                                            entry.movesMade = finalMoves
                                        }
                                        if (entry.completionCount == 0) {
                                            entry.recordCompletion(0, finalMoves, finalStars)
                                        }
                                        updatedCount++
                                        d(
                                            "[HISTORY_SYNC] Set stars=%d, moves=%d for history entry '%s'",
                                            finalStars,
                                            entry.movesMade,
                                            entry.mapName
                                        )
                                    }
                                }
                                if (updatedCount > 0) {
                                    saveHistoryIndex(currentActivity, allEntries)
                                    d(
                                        "[HISTORY_SYNC] Updated and persisted %d level history entries with stars+moves",
                                        updatedCount
                                    )
                                }
                            } catch (e: Exception) {
                                e(
                                    e,
                                    "[HISTORY_SYNC] Error updating level history entries with stars"
                                )
                            }
                        }).start()
                    }
                }

                val starsAfter = manager.totalStars
                d("[LEVEL_EDITOR] Stars before=%d, after=%d", starsBefore, starsAfter)
                if (starsBefore < 140 && starsAfter >= 140) {
                    Handler(Looper.getMainLooper()).post(Runnable {
                        Toast.makeText(
                            context,
                            R.string.level_editor_unlocked,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    )
                    d("[LEVEL_EDITOR] Level Editor unlocked at %d stars!", starsAfter)
                }
            }


            // Auto-upload history after ANY game completion (level or random)
            if (complete) {
                triggerHistoryUpload(state.levelId)
            }
        }
    }

    /**
     * Trigger automatic history upload after game completion (level or random).
     * Waits briefly for saveToHistory to persist, then uploads all history entries.
     * @param levelId The level ID (>0 for levels, <=0 for random games)
     */
    private fun triggerHistoryUpload(levelId: Int) {
        val currentActivity = this.activity
        if (currentActivity == null) {
            e("[HISTORY_SYNC] Activity is null, cannot upload history")
            return
        }

        val gameType = if (levelId > 0) "level " + levelId else "random game"
        d("[HISTORY_SYNC] Triggering automatic history upload after %s completion", gameType)

        Thread(Runnable {
            try {
                // Wait to ensure saveToHistory() has been called AND persisted
                Thread.sleep(1500)

                d("[HISTORY_SYNC] Starting upload for %s...", gameType)
                SyncManager.getInstance(context!!)
                    .uploadHistory(currentActivity, object : HistoryUploadCallback {
                        override fun onSuccess(syncedCount: Int) {
                            d(
                                "[HISTORY_SYNC] Upload callback: success with %d entries synced after %s",
                                syncedCount,
                                gameType
                            )
                        }

                        override fun onError(error: String?) {
                            e(
                                "[HISTORY_SYNC] Upload callback: error after %s - %s",
                                gameType,
                                error
                            )
                        }
                    })
            } catch (e: Exception) {
                e(e, "[HISTORY_SYNC] Exception during upload after %s", gameType)
            }
        }).start()
    }

    /**
     * Save level completion data when a level is completed
     * 
     * @param state The completed game state
     * @return The prepared LevelCompletionData object (does not save it)
     */
    private fun saveLevelCompletionData(state: GameState): LevelCompletionData? {
        val levelId = state.levelId
        if (levelId <= 0) {
            d("Not saving completion data - not a level game (levelId=%d)", levelId)
            return null // Not a level game
        }

        d("Preparing completion data for level %d", levelId)

        // Get the level completion manager
        val manager = LevelCompletionManager.getInstance(context!!)

        // Get or create completion data for this level
        val data = manager.getLevelCompletionData(levelId)

        // Update completion data
        data!!.setCompleted(true)
        data.hintsShown = hintsShown
        data.timeNeeded = System.currentTimeMillis() - startTime
        data.movesNeeded = (if (moveCount.getValue() != null) moveCount.getValue() else 0)!!
        data.robotsUsed = robotsUsed.size
        data.squaresSurpassed =
            (if (squaresMoved.getValue() != null) squaresMoved.getValue() else 0)!!

        // Set optimal moves if we have a solution
        var optimalMoves = 0
        if (currentSolution != null && currentSolution!!.moves != null) {
            optimalMoves = currentSolution!!.moves.size
            data.optimalMoves = optimalMoves
        }

        // Calculate stars based on the criteria
        val playerMoves: Int = (if (moveCount.getValue() != null) moveCount.getValue() else 0)!!
        var starCount = calculateStars(playerMoves, optimalMoves, hintsShown)
        // For beginner levels (1-10), always earn at least 1 star
        if (starCount < 1 && levelId <= Constants.MIN_STAR_GUARANTEE_LEVEL) {
            starCount = 1
        }
        data.setStars(starCount)

        d(
            "[STARS] gameStateManager: Level %d completed with %d moves (optimal: %d), %d hints, earned %d stars",
            levelId, playerMoves, optimalMoves, hintsShown, starCount
        )

        // Return the prepared data without saving it
        d("Prepared level completion data: %s", data)
        return data
    }

    /**
     * Calculate star rating based on player performance
     * 
     * 
     * Star allocation rules:
     * - 4 stars: Hyper-optimal solution (better than solver's optimal solution)
     * - 3 stars: Optimal solution (same as solver) with no hints
     * - 2 stars: One move more than optimal with no hints, OR optimal with one hint
     * - 1 star: Optimal solution with two hints, OR two moves more than optimal with no hints
     * - 0 stars: All other cases
     * 
     * @param playerMoves  Number of moves used by player
     * @param optimalMoves Optimal number of moves from solver
     * @param hintsUsed    Number of hints used
     * @return Number of stars earned (0-4)
     */
    fun calculateStars(playerMoves: Int, optimalMoves: Int, hintsUsed: Int): Int {
        if (optimalMoves <= 0) {
            d("[stars] No optimal solution available")
            return 0 // No optimal solution available
        }

        // Calculate stars based on the rules
        if (playerMoves < optimalMoves) {
            // hyper-Optimal solution (better than solver's solution)
            d("[stars] hyper-optimal solution! 4 stars")
            return 4
        } else if (playerMoves == optimalMoves && hintsUsed == 0) {
            // Optimal solution (or better) and no hints
            d("[stars] optimal solution! 3 stars")
            return 3
        } else if ((playerMoves == optimalMoves + 1 && hintsUsed == 0) ||
            (playerMoves == optimalMoves && hintsUsed == 1)
        ) {
            // One move more than optimal with no hints OR optimal with one hint
            return 2
        } else if ((playerMoves == optimalMoves && hintsUsed == 2) ||
            (playerMoves == optimalMoves + 2 && hintsUsed == 0)
        ) {
            // Optimal with two hints OR two moves more than optimal with no hints
            return 1
        } else {
            // All other cases
            return 0
        }
    }

    val totalStars: Int
        /**
         * Get the total number of stars earned across all levels
         * 
         * @return Total number of stars
         */
        get() {
            val manager = LevelCompletionManager.getInstance(context!!)
            return manager.totalStars
        }


    /**
     * Save the current UI timer elapsed time (called from fragment onPause/stopTimer)
     * @param elapsedMs elapsed time in milliseconds
     */
    fun saveUiTimerElapsed(elapsedMs: Long) {
        this.uiTimerElapsedMs = elapsedMs
        this.uiTimerWasRunning = true
        d("[TIMER] Saved UI timer elapsed: %d ms", elapsedMs)
    }

    /**
     * Check if a UI timer was running before fragment recreation
     * @return true if timer was running
     */
    fun wasUiTimerRunning(): Boolean {
        return uiTimerWasRunning
    }

    /**
     * Reset the UI timer state (called when starting a new game)
     */
    fun resetUiTimer() {
        this.uiTimerElapsedMs = 0
        this.uiTimerWasRunning = false
        this.isNewGameLoaded = false
        d("[TIMER] Reset UI timer state")
    }

    /**
     * Pause the UI timer (called when app goes to background)
     * This is a no-op in GameStateManager - the actual timer is in GameFragment
     * GameFragment will call saveUiTimerElapsed() before pausing
     */
    fun pauseTimer() {
        // Timer pause is handled by GameFragment
        // This method exists for MainActivity lifecycle compatibility
        d("[TIMER] pauseTimer() called (handled by GameFragment)")
    }

    /**
     * Resume the UI timer (called when app comes to foreground)
     * This is a no-op in GameStateManager - the actual timer is in GameFragment
     * GameFragment will restore the timer using wasUiTimerRunning() and getUiTimerElapsedMs()
     */
    fun resumeTimer() {
        // Timer resume is handled by GameFragment
        // This method exists for MainActivity lifecycle compatibility
        d("[TIMER] resumeTimer() called (handled by GameFragment)")
    }

    /**
     * Clear the new game loaded flag (called after timer is reset)
     */
    fun clearNewGameLoadedFlag() {
        this.isNewGameLoaded = false
        d("[TIMER] Cleared new game loaded flag")
    }

    /**
     * Check if the solution was accepted
     * @return true if the solution was accepted
     */
    fun solutionWasAccepted(): Boolean {
        return solutionWasAccepted
    }

    /**
     * Clear the solution accepted flag (called in Fragment)
     */
    fun clearSolutionAcceptedFlag() {
        this.solutionWasAccepted = false
        d("[TIMER] Cleared timer reset flag")
    }

    val isInLevelGame: Boolean
        /**
         * Check if the current game is a level game (not a random game)
         * @return true if playing a level
         */
        get() {
            val state = currentState.getValue()
            return state != null && state.levelId > 0
        }

    val currentLevelId: Int
        /**
         * Get the current level ID
         * @return level ID, or -1 if not in a level game
         */
        get() {
            val state = currentState.getValue()
            return if (state != null) state.levelId else -1
        }

    open var difficulty: Int
        /**
         * Get the current difficulty level from Preferences
         * 
         * @return Current difficulty level
         */
        get() = Preferences.difficulty
        /**
         * Set the difficulty level in Preferences
         * 
         * @param difficulty New difficulty level
         */
        set(difficulty) {
            Preferences.difficulty = difficulty
        }

    /**
     * Calculate difficulty based on level number.
     * Levels 1-35: Beginner
     * Levels 36-70: Advanced
     * Levels 71-105: Insane
     * Levels 106-140: Impossible
     * 
     * @param levelId The level ID
     * @return The difficulty constant
     */
    private fun calculateDifficultyForLevel(levelId: Int): Int {
        if (levelId <= 35) {
            return Constants.DIFFICULTY_BEGINNER
        } else if (levelId <= 70) {
            return Constants.DIFFICULTY_ADVANCED
        } else if (levelId <= 105) {
            return Constants.DIFFICULTY_INSANE
        } else {
            return Constants.DIFFICULTY_IMPOSSIBLE
        }
    }

    val effectiveDifficulty: Int
        /**
         * Get the effective difficulty for the current game.
         * For level games: returns difficulty based on level number
         * For loaded savegames: returns difficulty stored in the savegame
         * For random games: returns difficulty from preferences
         * 
         * @return The effective difficulty level
         */
        get() {
            val state = currentState.getValue()
            if (state != null && state.levelId > 0) {
                // Level game: calculate difficulty based on level
                return calculateDifficultyForLevel(state.levelId)
            } else if (isLoadedFromSave) {
                // Loaded savegame: use difficulty from the GameState
                val difficulty = state!!.difficulty
                d(
                    "[DIFFICULTY] Using difficulty from loaded savegame: %d (isLoadedFromSave=%s)",
                    difficulty, isLoadedFromSave
                )
                return difficulty
            } else {
                // Random game: use difficulty from preferences
                d(
                    "[DIFFICULTY] Using difficulty from preferences: %d",
                    Preferences.difficulty
                )
                return Preferences.difficulty
            }
        }

    /**
     * Clear the loaded from history flag
     * Used when loading a history entry from a random game context
     */
    fun clearLoadedFromHistoryFlag() {
        isLoadedFromHistory = false
        currentHistoryPath = null
        d("[HISTORY_NAV] Cleared loaded from history flag")
    }

    /**
     * Load the previous history entry (if one exists)
     * Uses filtered entries (excluding level games) to match history tab display
     * @return true if successful, false otherwise
     */
    fun loadPreviousHistoryEntry(): Boolean {
        if (currentHistoryPath == null) {
            d("[HISTORY_NAV] No current history path, cannot load previous entry")
            return false
        }

        val activity = this.activity
        if (activity == null) {
            e("[HISTORY_NAV] Activity is null, cannot load previous history entry")
            return false
        }

        try {
            val allEntries = getHistoryEntries(activity)
            if (allEntries.isEmpty()) {
                d("[HISTORY_NAV] No history entries found")
                return false
            }

            // Filter out level games (matching SaveGameFragment filtering)
            val filteredEntries: MutableList<GameHistoryEntry> = ArrayList<GameHistoryEntry>()
            for (entry in allEntries) {
                val mapName = entry.mapName
                if (mapName == null || !mapName.matches("(?i)^Level\\s+\\d+.*".toRegex())) {
                    filteredEntries.add(entry)
                }
            }

            if (filteredEntries.isEmpty()) {
                d("[HISTORY_NAV] No filtered history entries found")
                return false
            }

            // Find the index of the current entry in filtered list
            var currentIndex = -1
            for (i in filteredEntries.indices) {
                if (filteredEntries.get(i).getMapPath() == currentHistoryPath) {
                    currentIndex = i
                    break
                }
            }

            if (currentIndex == -1) {
                d("[HISTORY_NAV] Current history entry not found in filtered list")
                return false
            }

            // Check if there's a previous entry
            if (currentIndex == 0) {
                d("[HISTORY_NAV] Already at first history entry, no previous entry")
                return false
            }

            // Load the previous entry
            val previousEntry = filteredEntries.get(currentIndex - 1)
            d("[HISTORY_NAV] Loading previous history entry: %s", previousEntry.getMapPath())
            loadHistoryEntry(previousEntry.getMapPath())
            return true
        } catch (e: Exception) {
            e(e, "[HISTORY_NAV] Error loading previous history entry")
            return false
        }
    }

    /**
     * Load the next history entry (if one exists)
     * Uses filtered entries (excluding level games) to match history tab display
     * @return true if successful, false otherwise
     */
    fun loadNextHistoryEntry(): Boolean {
        if (currentHistoryPath == null) {
            d("[HISTORY_NAV] No current history path, cannot load next entry")
            return false
        }

        val activity = this.activity
        if (activity == null) {
            e("[HISTORY_NAV] Activity is null, cannot load next history entry")
            return false
        }

        try {
            val allEntries = getHistoryEntries(activity)
            if (allEntries.isEmpty()) {
                d("[HISTORY_NAV] No history entries found")
                return false
            }

            // Filter out level games (matching SaveGameFragment filtering)
            val filteredEntries: MutableList<GameHistoryEntry> = ArrayList<GameHistoryEntry>()
            for (entry in allEntries) {
                val mapName = entry.mapName
                if (mapName == null || !mapName.matches("(?i)^Level\\s+\\d+.*".toRegex())) {
                    filteredEntries.add(entry)
                }
            }

            if (filteredEntries.isEmpty()) {
                d("[HISTORY_NAV] No filtered history entries found")
                return false
            }

            // Find the index of the current entry in filtered list
            var currentIndex = -1
            for (i in filteredEntries.indices) {
                if (filteredEntries.get(i).getMapPath() == currentHistoryPath) {
                    currentIndex = i
                    break
                }
            }

            if (currentIndex == -1) {
                d("[HISTORY_NAV] Current history entry not found in filtered list")
                return false
            }

            // Check if there's a next entry
            if (currentIndex == filteredEntries.size - 1) {
                d("[HISTORY_NAV] Already at last history entry, no next entry")
                return false
            }

            // Load the next entry
            val nextEntry = filteredEntries.get(currentIndex + 1)
            d("[HISTORY_NAV] Loading next history entry: %s", nextEntry.getMapPath())
            loadHistoryEntry(nextEntry.getMapPath())
            return true
        } catch (e: Exception) {
            e(e, "[HISTORY_NAV] Error loading next history entry")
            return false
        }
    }

    /**
     * Check if there is a next history entry available
     * Uses filtered entries (excluding level games) to match history tab display
     * @return true if next entry exists, false otherwise
     */
    fun hasNextHistoryEntry(): Boolean {
        if (currentHistoryPath == null) {
            return false
        }

        val activity = this.activity
        if (activity == null) {
            return false
        }

        try {
            val allEntries = getHistoryEntries(activity)
            if (allEntries.isEmpty()) {
                return false
            }

            // Filter out level games (matching SaveGameFragment filtering)
            val filteredEntries: MutableList<GameHistoryEntry?> = ArrayList<GameHistoryEntry?>()
            for (entry in allEntries) {
                val mapName = entry.mapName
                if (mapName == null || !mapName.matches("(?i)^Level\\s+\\d+.*".toRegex())) {
                    filteredEntries.add(entry)
                }
            }

            if (filteredEntries.isEmpty()) {
                return false
            }

            // Find the index of the current entry in filtered list
            var currentIndex = -1
            for (i in filteredEntries.indices) {
                if (filteredEntries.get(i)!!.getMapPath() == currentHistoryPath) {
                    currentIndex = i
                    break
                }
            }

            return currentIndex != -1 && currentIndex < filteredEntries.size - 1
        } catch (e: Exception) {
            e(e, "[HISTORY_NAV] Error checking for next history entry")
            return false
        }
    }


    val localizedDifficultyString: String
        /**
         * Get a string representation of the current difficulty level
         * 
         * @return String representation of the current difficulty level
         */
        get() {
            // Verwende den bereits lokalisierten Anwendungskontext
            val localizedContext = RoboyardApplication.getAppContext()

            val difficulty = this.effectiveDifficulty

            d(
                "[DIFFICULTY] getLocalizedDifficultyString() called, using effective difficulty level %d",
                difficulty
            )

            when (difficulty) {
                Constants.DIFFICULTY_BEGINNER -> return localizedContext.getString(
                    R.string.difficulty_beginner
                )

                Constants.DIFFICULTY_ADVANCED -> return localizedContext.getString(
                    R.string.difficulty_advanced
                )

                Constants.DIFFICULTY_INSANE -> return localizedContext.getString(
                    R.string.difficulty_insane
                )

                Constants.DIFFICULTY_IMPOSSIBLE -> return localizedContext.getString(
                    R.string.difficulty_impossible
                )

                else -> return localizedContext.getString(R.string.difficulty_unknown)
            }
        }

    /**
     * Start the game timer for history tracking
     */
    fun startGameTimer() {
        gameStartTime = System.currentTimeMillis()
        isHistorySaved = false
        isViewTimeAchievementChecked = false // Reset achievement check for new game
        isCompletionRecorded = false
        wrongRobotToastShownColors.clear()
        d("[HISTORY] Game timer started")
    }

    /**
     * Update the game timer and check if game should be saved to history
     */
    fun updateGameTimer() {
        if (!isHistorySaved && currentState.getValue() != null) {
            val elapsedSeconds = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()
            totalPlayTime = elapsedSeconds

            // Save to history after threshold time of play
            if (totalPlayTime >= HISTORY_SAVE_THRESHOLD) {
                d(
                    "[HISTORY] Threshold reached (%d seconds), saving game to history",
                    HISTORY_SAVE_THRESHOLD
                )
                saveToHistory()
                isHistorySaved = true
            }

            // Check for "Don't give up" achievement (view_1_hour)
            // 5 seconds for testing, should be 3600 in production
            if (!isViewTimeAchievementChecked && totalPlayTime >= 3600) {
                checkViewTimeAchievement()
                isViewTimeAchievementChecked = true
            }
        }
    }

    /**
     * Check and unlock the "Don't give up" achievement if conditions are met
     */
    private fun checkViewTimeAchievement() {
        if (context == null) return

        val achievementManager = AchievementManager.getInstance(context)
        if (!achievementManager.isUnlocked("view_1_hour")) {
            d(
                "[ACHIEVEMENT] Unlocking view_1_hour ('Don't give up') - played for %d seconds",
                totalPlayTime
            )
            achievementManager.unlock("view_1_hour")
        }
    }

    /**
     * Save to history immediately, bypassing the time threshold.
     * Called when a hint is shown, live move counter is activated, or map is completed.
     * @param reason Short description for logging (e.g. "hint_shown", "live_move", "completed")
     */
    fun saveToHistoryNow(reason: String?) {
        d(
            "[HISTORY_FLOW] saveToHistoryNow(%s): isHistorySaved=%b, isComplete=%b, moveCount=%s",
            reason, isHistorySaved, isGameComplete.getValue(),
            moveCount.getValue()
        )
        if (!isHistorySaved) {
            d("[HISTORY] Immediate save triggered by: %s", reason)
            saveToHistory()
            isHistorySaved = true
        } else {
            // Already saved - just update hint tracking in existing entry
            d("[HISTORY] Already saved, updating hint tracking for: %s", reason)
            updateHintTrackingInHistory()
        }
    }

    /**
     * Update hint tracking in the existing history entry for the current map.
     * Called when hint status changes after the initial history save.
     * Also updates move count if game is completed after hints were shown.
     */
    private fun updateHintTrackingInHistory() {
        try {
            val activity = this.activity
            val gameState = currentState.getValue()
            if (activity == null || gameState == null) return

            val mapSig = gameState.generateMapSignature()
            d(
                "[HISTORY] updateHintTrackingInHistory: mapSig=%s, isComplete=%b",
                mapSig, isGameComplete.getValue()
            )

            if (mapSig == null || mapSig.isEmpty()) return

            // Load the full list once - we will modify it in-place and save it back
            val allEntries = getHistoryEntries(activity)
            var existing: GameHistoryEntry? = null
            for (e in allEntries) {
                if (mapSig == e.mapSignature) {
                    existing = e
                    break
                }
            }
            if (existing == null) {
                d("[MAPSIG] updateHintTracking: NOT FOUND. Searching for: %s", mapSig)
                for (e in allEntries) {
                    d("[MAPSIG] updateHintTracking: stored entry mapSig=%s", e.mapSignature)
                }
            }
            d(
                "[HISTORY] findByMapSignature result: %s",
                if (existing != null) existing.mapName else "null - NOT FOUND"
            )

            if (existing != null) {
                val maxHint = gameState.maxHintUsedThisSession

                // Update hint tracking if hints were used
                if (!existing.hasUsedHints() && maxHint >= 0) {
                    existing.recordHintUsed(maxHint)
                    d("[HISTORY] Updated hint tracking in existing entry: maxHintUsed=%d", maxHint)
                }

                // Update move count if game is completed (important when hints were shown before completion)
                // Guard: only record completion once per game session
                if (java.lang.Boolean.TRUE == isGameComplete.getValue() && !isCompletionRecorded) {
                    val actualMoveCount = gameState.moveCount
                    val countBefore = existing.completionCount

                    // [STARS_PER_COMPLETION] Calculate current attempt's stars for level games
                    // and pass them to recordCompletion. Without this, the 2-arg overload
                    // falls back to existing.starsEarned which holds the BEST stars from
                    // LevelCompletionManager, not the current attempt's stars.
                    val optMoves =
                        if (currentSolution != null && currentSolution!!.moves != null) currentSolution!!.moves.size else 0
                    if (optMoves > 0) {
                        existing.optimalMoves = optMoves
                        d("[HISTORY] updateHintTracking: setOptimalMoves=%d", optMoves)
                    }

                    val levelIdForStars = gameState.levelId
                    var currentAttemptStars: Int
                    if (levelIdForStars > 0) {
                        currentAttemptStars = calculateStars(actualMoveCount, optMoves, hintsShown)
                        if (currentAttemptStars < 1 && levelIdForStars <= Constants.MIN_STAR_GUARANTEE_LEVEL) {
                            currentAttemptStars = 1
                        }
                    } else {
                        currentAttemptStars = existing.starsEarned
                    }
                    existing.recordCompletion(
                        ((System.currentTimeMillis() - gameStartTime) / 1000).toInt(),
                        actualMoveCount,
                        currentAttemptStars
                    )
                    isCompletionRecorded = true
                    d(
                        "[HISTORY_FLOW][STARS_PER_COMPLETION] updateHintTracking: recordCompletion called, countBefore=%d, countAfter=%d, moveCount=%d, stars=%d",
                        countBefore, existing.completionCount, actualMoveCount, currentAttemptStars
                    )

                    // If completed without hints, record the no-hints timestamp (never overwritten by later hint usage)
                    if (maxHint < 0 && actualMoveCount > 0) {
                        val isOptimal = optMoves > 0 && actualMoveCount == optMoves
                        existing.recordSolvedWithoutHints(isOptimal)
                        d(
                            "[HISTORY] updateHintTracking: recordSolvedWithoutHints isOptimal=%b, moves=%d, optimal=%d",
                            isOptimal,
                            actualMoveCount,
                            optMoves
                        )
                    }
                } else if (java.lang.Boolean.TRUE == isGameComplete.getValue()) {
                    d("[HISTORY_FLOW] updateHintTracking: completion already recorded this session, skipping")
                }

                // Mark everUsedHints if hints were used
                if (maxHint >= 0) {
                    existing.markEverUsedHints()
                }

                // Save the same list we modified (not a freshly-read copy from disk)
                saveHistoryIndex(activity, allEntries)
                d(
                    "[HISTORY] Saved updated history entry: completionCount=%d, maxHintUsed=%d, everUsedHints=%b",
                    existing.completionCount, maxHint, existing.isEverUsedHints()
                )
            }
        } catch (e: Exception) {
            e("[HISTORY] Error updating hint tracking: %s", e.message)
        }
    }

    /**
     * Save the current game state to history
     */
    private fun saveToHistory() {
        try {
            val gameState = currentState.getValue()
            if (gameState == null) {
                e("[HISTORY] Cannot save to history: no current game state")
                return
            }

            // Get activity from weak reference to avoid memory leaks
            val activity = this.activity
            if (activity == null) {
                e("[HISTORY] Cannot save to history: no activity")
                return
            }

            // Initialize GameHistoryManager if needed
            initialize(activity)

            // Get next available history index
            val historyIndex = getNextHistoryIndex(activity)
            val historyFileName = "history_" + historyIndex + ".txt"
            val historyPath = historyFileName

            // Get a proper map name directly from the game state
            var mapName: String? = null

            // Get level name from the game state
            if (gameState != null) {
                mapName = gameState.levelName
                d("[HISTORY] Retrieved level name from game state: %s", mapName)

                // If level name is not set properly, use level ID
                if (mapName == null || mapName.isEmpty() || "XXXXX" == mapName) {
                    val levelId = gameState.levelId
                    if (levelId > 0) {
                        mapName = "Level " + levelId
                        d("[HISTORY] Using level ID to generate map name: %s", mapName)
                    } else {
                        // Random game
                        mapName = "Random Map #" + historyIndex
                        d("[HISTORY] Using fallback random map name: %s", mapName)
                    }
                }
            } else {
                // No game state available
                mapName = "Game " + historyIndex
                e("[HISTORY] ERROR: No game state available, using default name: %s", mapName)
            }

            // Serialize the game state using the same format as save games
            val saveData = gameState.serialize()
            writePrivateData(activity, historyPath, saveData)

            // Get actual board dimensions from game state
            val boardWidth = gameState.width
            val boardHeight = gameState.height
            val boardSize =
                if (boardWidth > 0 && boardHeight > 0) boardWidth.toString() + "x" + boardHeight else ""

            // Preview image path (flat filename, no directory separator) - kept for compatibility but not used for minimap generation
            val previewImagePath = historyFileName + "_preview.txt"

            // Get optimal moves from current solution if available
            var optimalMovesCount = 0
            val currentSolution = this.currentSolution
            if (currentSolution != null && currentSolution.moves != null) {
                optimalMovesCount = currentSolution.moves.size
            }

            // Create history entry with available information
            // Only save actual move count if game is completed
            // For intermediate saves (hints, live move counter), use 0
            val actualMoveCount = if (isGameComplete.getValue()) gameState.moveCount else 0
            val entry = GameHistoryEntry(
                historyPath,
                mapName,
                gameStartTime,  // timestamp = when game started, not when first move was made
                totalPlayTime,
                actualMoveCount,
                optimalMovesCount,
                boardSize,
                previewImagePath
            )
            d(
                "[HISTORY] Saving to history: moveCount=%d (gameComplete=%b), optimalMoves=%d",
                actualMoveCount, isGameComplete.getValue(), optimalMovesCount
            )


            // Set difficulty as int ID (0-3), not localized string
            entry.difficulty = this.effectiveDifficulty

            // Set map signatures for unique map tracking
            val wallSig = gameState.generateWallSignature()
            val posSig = gameState.generatePositionSignature()
            val mapSig = gameState.generateMapSignature()
            entry.wallSignature = wallSig
            entry.positionSignature = posSig
            entry.mapSignature = mapSig
            d("[MAPSIG] saveToHistory: wallSig=%s", wallSig)
            d("[MAPSIG] saveToHistory: posSig=%s", posSig)
            d("[MAPSIG] saveToHistory: mapSig=%s", mapSig)


            // Set stars earned for THIS completion (if this is a level game)
            // [STARS_PER_COMPLETION] Use the CURRENT attempt's star count, not the best from
            // LevelCompletionManager (which only stores the highest stars ever earned). This
            // ensures the history entry's completionStars array reflects each attempt accurately.
            val levelId = gameState.levelId
            if (levelId > 0 && isGameComplete.getValue()) {
                val optMovesForStars =
                    if (currentSolution != null && currentSolution.moves != null) currentSolution.moves.size else 0
                var currentAttemptStars =
                    calculateStars(actualMoveCount, optMovesForStars, hintsShown)
                // For beginner levels (1-10), always earn at least 1 star (matches saveLevelCompletionData)
                if (currentAttemptStars < 1 && levelId <= Constants.MIN_STAR_GUARANTEE_LEVEL) {
                    currentAttemptStars = 1
                }
                entry.starsEarned = currentAttemptStars
                d(
                    "[HISTORY][STARS_PER_COMPLETION] Set starsEarned=%d for level %d (moves=%d, optimal=%d, hints=%d)",
                    currentAttemptStars, levelId, actualMoveCount, optMovesForStars, hintsShown
                )
            }

            // Set hint tracking - record if hints were used during this session
            val maxHintUsed = gameState.maxHintUsedThisSession
            entry.maxHintUsed = maxHintUsed
            // Mark as solved without hints only if no hints were used
            val noHintsThisSession = maxHintUsed < 0
            entry.setSolvedWithoutHints(noHintsThisSession)
            // Mark everUsedHints if hints were used in this session
            if (maxHintUsed >= 0) {
                entry.markEverUsedHints()
            }
            // If completed without hints, record the timestamp (never overwritten by later hint usage)
            if (noHintsThisSession && actualMoveCount > 0) {
                val optMoves =
                    if (currentSolution != null && currentSolution.moves != null) currentSolution.moves.size else 0
                val isOptimal = optMoves > 0 && actualMoveCount == optMoves
                entry.recordSolvedWithoutHints(isOptimal)
                d(
                    "[HISTORY] recordSolvedWithoutHints: isOptimal=%b, moves=%d, optimal=%d",
                    isOptimal, actualMoveCount, optMoves
                )
            }
            d(
                "[HISTORY] Hint tracking: maxHintUsed=%d, solvedWithoutHints=%b, everUsedHints=%b, lastSolvedWithoutHints=%d",
                maxHintUsed,
                entry.isSolvedWithoutHints(),
                entry.isEverUsedHints(),
                entry.lastSolvedWithoutHints
            )

            // Add entry to history index
            addHistoryEntry(activity, entry)

            // Verify the entry was stored with the correct mapSignature
            val stored = findByMapSignature(activity, mapSig)
            d(
                "[MAPSIG] saveToHistory: after addHistoryEntry, findByMapSignature('%s') = %s",
                mapSig,
                if (stored != null) "FOUND (completionCount=" + stored.completionCount + ")" else "NOT FOUND"
            )

            // If game was complete when saved, mark completion as recorded to prevent double-counting
            if (actualMoveCount > 0) {
                isCompletionRecorded = true
                d(
                    "[HISTORY_FLOW] saveToHistory: completion recorded via addHistoryEntry (movesMade=%d)",
                    actualMoveCount
                )
            }

            d("[HISTORY] Game saved to history: %s (Map name: '%s')", historyPath, mapName)
        } catch (e: Exception) {
            e("[HISTORY] Error saving game to history: %s", e.message)
        }
    }

    /**
     * Asynchronously calculates the solution for the current game state
     * and returns the result via callback
     * 
     * @param callback The callback to receive the solution when it's ready
     */
    fun calculateSolutionAsync(callback: SolutionCallback?) {
        // Don't start a new calculation if one is already running
        if (java.lang.Boolean.TRUE == isSolverRunning.getValue()) {
            d("[SOLUTION_SOLVER][calculateSolutionAsync] Solver already running, ignoring duplicate request")
            return
        }

        // Increment solver restart count
        solverRestartCount++
        d("[SOLUTION_SOLVER][calculateSolutionAsync] Solver restart count: %d", solverRestartCount)

        this.solutionCallback = callback
        d("[SOLUTION_SOLVER][calculateSolutionAsync] Stored callback: %s", callback)

        val state = currentState.getValue()
        if (state == null) {
            d("[SOLUTION_SOLVER][calculateSolutionAsync] Current state is null")
            onSolutionCalculationFailed("No game state available")
            return
        }

        // Log the current game state details
        val elements = state.gridElements
        d("[SOLUTION_SOLVER][calculateSolutionAsync] Current GameState hash: %d", state.hashCode())
        d("[SOLUTION_SOLVER][calculateSolutionAsync] Current map has %d elements", elements.size)

        // Log robot positions
        val robots = state.robots
        for (robot in robots) {
            d(
                "[SOLUTION_SOLVER][calculateSolutionAsync] Robot ID %d (color %d) at position (%d, %d)",
                robot.color, robot.color, robot.x, robot.y
            )
        }

        // Log target position
        val target = state.target
        if (target != null) {
            d(
                "[SOLUTION_SOLVER][calculateSolutionAsync] Target for robot Color %d (color %d) at position (%d, %d)",
                target.color, target.color, target.x, target.y
            )
        } else {
            d("[SOLUTION_SOLVER][calculateSolutionAsync] No target found in current game state")
        }

        // Set solver running state
        isSolverRunning.setValue(true)

        // Signal that calculation has started
        onSolutionCalculationStarted()

        try {
            // Cancel any previous solver task - the single-thread executor ensures
            // the new task won't start until the old one finishes/is interrupted
            if (solverFuture != null && !solverFuture!!.isDone()) {
                d("[SOLUTION_SOLVER] Cancelling previous solver task before starting new one")
                this.solverManager.cancelSolver()
                solverFuture!!.cancel(true)
            }


            // Capture elements for the background thread
            val capturedElements = ArrayList<GridElement?>(elements)


            // Submit new solver task to the persistent single-thread executor
            // Both initialization and solving run on background thread to avoid Main-Thread OOM
            solverFuture = solverExecutor.submit(Runnable {
                try {
                    // Memory gate: wait for GC to reclaim previous solver's memory
                    val rt = Runtime.getRuntime()
                    val minFreeBytes = rt.maxMemory() / 10 // 10% of heap must be free
                    for (attempt in 0..4) {
                        val freeBytes = rt.maxMemory() - rt.totalMemory() + rt.freeMemory()
                        if (freeBytes >= minFreeBytes) break
                        d(
                            "[SOLUTION_SOLVER] Memory gate: waiting for GC, free=%dMB need=%dMB attempt=%d",
                            freeBytes shr 20, minFreeBytes shr 20, attempt
                        )
                        System.gc()
                        Thread.sleep(200)
                    }
                    d("[SOLUTION_SOLVER][calculateSolutionAsync] Initializing and running solver on background thread")
                    val manager = this.solverManager
                    manager.initialize(capturedElements)
                    SolverManager.ensureUniqueInvocationId()
                    d(
                        "[SOLUTION_SOLVER][calculateSolutionAsync] Using solver manager with counter: %d",
                        SolverManager.getCurrentSolverInvocationId()
                    )
                    manager.run()
                } catch (e: Exception) {
                    e(e, "[SOLUTION_SOLVER] Error running solver")
                    Handler(Looper.getMainLooper()).post(Runnable {
                        onSolutionCalculationFailed("Error: " + e.message)
                    })
                }
            })
        } catch (e: Exception) {
            e(e, "[SOLUTION_SOLVER] Error initializing solver")
            onSolutionCalculationFailed("Error: " + e.message)
        }
    }

    /**
     * Called when the solution calculation starts
     */
    private fun onSolutionCalculationStarted() {
        d("[SOLUTION_SOLVER] onSolutionCalculationStarted")
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null

        // Notify callback if provided
        if (solutionCallback != null) {
            d(
                "[SOLUTION_SOLVER] onSolutionCalculationStarted: Notifying callback: %s",
                solutionCallback
            )
            solutionCallback!!.onSolutionCalculationStarted()
        } else {
            w("[SOLUTION_SOLVER] onSolutionCalculationStarted: No callback to notify")
        }
    }

    /**
     * Called when the solution calculation completes successfully
     * This is the SolverManager.Listener implementation that's called by SolverManager when the solver finishes.
     * This method stores the solution and notifies any registered callback.
     * 
     * @param solution The calculated solution
     */
    private fun onSolutionCalculationCompleted(solution: GameSolution?) {
        d("[SOLUTION_SOLVER] onSolutionCalculationCompleted: solution=%s", solution)

        // Check if we're in level mode (needed for regeneration logic)
        val state = currentState.getValue()
        val isLevelMode = (state != null && state.levelId > 0)


        // Add more detailed logging about the solution
        val moveCount = if (solution != null && solution.moves != null) solution.moves.size else 0
        if (moveCount > 0) {
            d(
                "[SOLUTION_SOLVER][MOVES] onSolutionCalculationCompleted: Found solution with %d moves",
                solution!!.moves.size
            )
            // If solution requires fewer moves than required minimum and we're not in level mode,
            // automatically start a new game because this one is too easy
            val minRequiredMoves = this.minimumRequiredMoves
            val maxRequiredMoves = this.maximumRequiredMoves

            // BEGINNER MODE: Check if solution is too easy (below minimum) or too hard (above maximum)
            // Skip validation for loaded savegames - they should be playable regardless of current difficulty settings
            // Also skip if regeneration is disabled (e.g., when user left the game screen)
            if (!isLevelMode && !isLoadedFromSave && allowRegeneration && regenerationCount < MAX_AUTO_REGENERATIONS) {
                val isTooEasy = moveCount < minRequiredMoves
                val isTooHard = moveCount > maxRequiredMoves

                if (keepCurrentMapDespiteDifficulty && (isTooEasy || isTooHard)) {
                    d(
                        "[SOLUTION_SOLVER][MOVES][KEEP_MAP_ENFORCER] Solution has only %d moves (minimum required: %d), but current map was manually kept",
                        moveCount,
                        minRequiredMoves
                    )
                } else {
                    if (isTooEasy) {
                        // Regenerate if puzzle is too easy (map rejected/discarded)
                        d(
                            "[SOLUTION_SOLVER][MOVES] Solution has only %d moves (minimum required: %d), regenerating (attempt %d/%d)",
                            moveCount,
                            minRequiredMoves,
                            regenerationCount + 1,
                            MAX_AUTO_REGENERATIONS
                        )
                        regenerationCount++

                        // Force reset the solver state before starting a new game
                        val solverManager = this.solverManager
                        solverManager.resetInitialization()
                        solverManager.cancelSolver() // Cancel any running solver process

                        // Create a new game after a short delay to ensure the solver is fully reset
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            createValidGame(
                                Preferences.boardSizeWidth, Preferences.boardSizeHeight
                            )
                        }, 100)
                        return
                    } else if (isTooHard) {
                        // Regenerate if puzzle is too hard for current difficulty mode (map rejected/discarded)
                        w(
                            "[SOLUTION_SOLVER][MOVES] %s mode - Solution has %d moves (maximum allowed: %d), regenerating (attempt %d/%d)",
                            this.localizedDifficultyString,
                            moveCount,
                            maxRequiredMoves,
                            regenerationCount + 1,
                            MAX_AUTO_REGENERATIONS
                        )
                        regenerationCount++

                        // Force reset the solver state before starting a new game
                        val solverManager = this.solverManager
                        solverManager.resetInitialization()
                        solverManager.cancelSolver() // Cancel any running solver process

                        // Create a new game after a short delay to ensure the solver is fully reset
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            validateDifficulty = true // Make sure difficulty is checked
                            createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight)
                        }, 100)
                        return
                    }
                }
            } else if (regenerationCount >= MAX_AUTO_REGENERATIONS) {
                d(
                    "[SOLUTION_SOLVER][MOVES] Reached maximum regeneration attempts (%d). Accepting current game.",
                    MAX_AUTO_REGENERATIONS
                )
                regenerationCount = 0 // Reset for next time
            } else if (!allowRegeneration) {
                d("[SOLUTION_SOLVER][MOVES] Regeneration disabled (user left game screen), accepting current solution")
            }
        } else {
            // moveCount==0: solver hit memory/depth limit or puzzle is unsolvable.
            // Try a new map if regeneration is allowed; otherwise accept as-is.
            if (!isLevelMode && !isLoadedFromSave && allowRegeneration && regenerationCount < MAX_AUTO_REGENERATIONS) {
                d(
                    "[SOLUTION_SOLVER][MOVES] No solution found (memory/depth limit), trying new map (regen %d/%d)",
                    regenerationCount + 1, MAX_AUTO_REGENERATIONS
                )
                regenerationCount++
                val solverManager = this.solverManager
                solverManager.resetInitialization()
                solverManager.cancelSolver()
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight)
                }, 100)
                return
            }
            w("[SOLUTION_SOLVER][MOVES] onSolutionCalculationCompleted: No solution found, accepting puzzle (regen exhausted or disabled)")
        }

        // Store the solution for later use with getHint()
        currentSolution = solution
        currentSolutionStep = 0
        updatePreCompRobotOrder(solution)

        // Update solver status
        isSolverRunning.setValue(false)


        // Set flag to signal Fragment that solution was found and accepted
        solutionWasAccepted = true
        if (regenerationCount > 0) {
            d(
                "[SOLUTION_SOLVER][TIMER] New map accepted after %d regenerations, signaling Fragment to reset timer",
                regenerationCount
            )
        } else {
            d("[SOLUTION_SOLVER] Solution found for loaded game, signaling Fragment")
        }


        // Reset regeneration count when map is accepted
        regenerationCount = 0

        // Notify the callback if provided
        if (solutionCallback != null) {
            d(
                "[SOLUTION_SOLVER] onSolutionCalculationCompleted: Notifying callback: %s",
                solutionCallback
            )
            solutionCallback!!.onSolutionCalculationCompleted(solution)
            solutionCallback = null // Clear callback after use
        } else {
            d("[SOLUTION_SOLVER] onSolutionCalculationCompleted: No callback provided")
        }

        // Store the minimum moves from this solution for display
        lastSolutionMinMoves =
            if (solution != null && solution.moves != null) solution.moves.size else 0
        d(
            "[SOLUTION_SOLVER][MOVES] onSolutionCalculationCompleted: Found solution with %d moves after %d regeneration(s)",
            lastSolutionMinMoves,
            regenerationCount
        )
    }

    /**
     * Called when the solution calculation fails
     * 
     * @param errorMessage The error message
     */
    private fun onSolutionCalculationFailed(errorMessage: String?) {
        d("[SOLUTION_SOLVER] onSolutionCalculationFailed: %s", errorMessage)

        // Clear any partial solution
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null

        // Update solver status
        isSolverRunning.setValue(false)

        // Notify the callback if provided
        if (solutionCallback != null) {
            d(
                "[SOLUTION_SOLVER] onSolutionCalculationFailed: Notifying callback: %s",
                solutionCallback
            )
            solutionCallback!!.onSolutionCalculationFailed(errorMessage)
            solutionCallback = null // Clear callback after use
        } else {
            d("[SOLUTION_SOLVER] onSolutionCalculationFailed: No callback provided")
        }
    }

    /**
     * Cancel any running solver operation
     */
    fun cancelSolver() {
        d("[SOLUTION_SOLVER] cancelSolver called")
        this.solverManager.cancelSolver()
        // Cancel the current solver task (interrupts the thread)
        if (solverFuture != null && !solverFuture!!.isDone()) {
            d("[SOLUTION_SOLVER] Cancelling solver future")
            solverFuture!!.cancel(true)
        }
        isSolverRunning.setValue(false)
        // Call the failure handler for normal cancellation
        onSolutionCalculationFailed("Solver was cancelled")
    }

    /**
     * Stop all map regeneration (e.g., when user leaves game screen)
     */
    fun stopRegeneration() {
        allowRegeneration = false
        d("[SOLUTION_SOLVER] Map regeneration disabled")
    }

    /**
     * Resume map regeneration (e.g., when user enters game screen)
     */
    fun resumeRegeneration() {
        allowRegeneration = true
        d("[SOLUTION_SOLVER] Map regeneration enabled")
    }

    /**
     * Keep the current map even if it does not satisfy the active difficulty limits.
     * This is triggered manually from the UI.
     */
    fun keepCurrentMapDespiteDifficulty() {
        keepCurrentMapDespiteDifficulty = true
        d("[DIFFICULTY_ENFORCER] Current map will be kept despite difficulty limits")
    }

    /**
     * Get the current solution
     * 
     * @return The current solution or null if none is available
     */
    fun getWrongRobotAtTarget(): LiveData<Int?> {
        return wrongRobotAtTarget
    }

    /**
     * Increment the solution step counter
     */
    fun incrementSolutionStep() {
        currentSolutionStep++
    }

    /**
     * Reset the solution step counter to show hints from the beginning
     */
    fun resetSolutionStep() {
        currentSolutionStep = 0
    }

    // Field to store the current solution callback
    private var solutionCallback: SolutionCallback? = null

    /**
     * Callback interface for solution calculation
     */
    interface SolutionCallback {
        /**
         * Called when the solution calculation starts
         */
        fun onSolutionCalculationStarted()

        /**
         * Called when the solution calculation completes successfully
         * 
         * @param solution The calculated solution
         */
        fun onSolutionCalculationCompleted(solution: GameSolution?)

        /**
         * Called when the solution calculation fails
         * 
         * @param errorMessage The error message
         */
        fun onSolutionCalculationFailed(errorMessage: String?)
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
    fun animateSolution(
        solution: GameSolution?,
        listener: SolutionAnimator.AnimationListener?
    ): SolutionAnimator? {
        if (solution == null || solution.moves.isEmpty()) {
            w("Cannot animate null or empty solution")
            if (listener != null) {
                listener.onAnimationComplete()
            }
            return null
        }

        d("Animating solution with %d moves", solution.moves.size)

        // Create and configure animator
        val animator = SolutionAnimator()
        animator.setAnimationListener(listener)
        animator.animateSolution(solution)
        return animator
    }

    /**
     * Reset statistics for a new game
     */
    private fun resetStatistics() {
        hintsShown = 0
        robotsUsed.clear()
        startTime = System.currentTimeMillis()
        moveCount.setValue(0)
        squaresMoved.setValue(0)
        isGameComplete.setValue(false)
    }

    private val minimumRequiredMoves: Int
        /**
         * Gets the minimum required moves based on current difficulty setting
         * Uses configurable preferences if available, otherwise falls back to defaults
         * 
         * @return minimum number of moves required for current difficulty
         */
        get() {
            val minMoves = Preferences.minSolutionMoves
            d(
                "[SOLUTION_SOLVER][MOVES] Minimum required moves from preferences: %d",
                minMoves
            )
            return minMoves
        }

    private val maximumRequiredMoves: Int
        get() {
            val maxMoves = Preferences.maxSolutionMoves
            d(
                "[SOLUTION_SOLVER][MOVES] Maximum required moves from preferences: %d",
                maxMoves
            )
            return maxMoves
        }

    /**
     * Creates a valid game with at least MIN_REQUIRED_MOVES difficulty
     * 
     * @param width  Width of the board
     * @param height Height of the board
     */
    private fun createValidGame(width: Int, height: Int) {
        d("GameStateManager: createValidGame() called")

        // If the user has clicked keep-map, do not create a new map
        if (keepCurrentMapDespiteDifficulty) {
            d("[KEEP_MAP_ENFORCER] createValidGame() blocked - user chose to keep current map")
            return
        }

        // Update WallStorage with current board size to ensure we're using the right storage
        val wallStorage = getInstance()
        wallStorage.updateCurrentBoardSize()

        // Create a new random game state using static Preferences
        val newState = createRandom()
        d(
            "GameStateManager: Created new random GameState with robotCount=%d, targetColors=%d",
            Preferences.robotCount,
            Preferences.targetColors
        )

        // DEBUG: Analyze all game elements in the newly created state
        d("[DEBUG_ROBOTS] Starting debug of newly created GameState (createValidGame)")
        var robotCount = 0
        for (element in newState.gameElements) {
            if (element.isRobot) {
                robotCount++
                d(
                    "[DEBUG_ROBOTS] Robot #%d at (%d,%d) with color %d (colorName: %s)",
                    robotCount, element.x, element.y, element.color,
                    getColorName(element.color, true)
                )
            }
        }
        d(
            "[DEBUG_ROBOTS] Total robots in new GameState: %d (should be %d)",
            robotCount, Constants.NUM_ROBOTS
        )

        // Set the game state
        currentState.setValue(newState)
        moveCount.setValue(0)
        isGameComplete.setValue(false)

        // Store walls for future use if we're not generating new maps each time
        if (!Preferences.generateNewMapEachTime) {
            wallStorage.storeWalls(newState.gridElements)
            d("[WALL STORAGE] Stored walls for future use after creating new game")
        }

        // Clear the state history
        stateHistory.clear()
        squaresMovedHistory.clear()
        clearNextMovesCache()

        // Initialize the solver with grid elements from the new state
        val gridElements = newState.gridElements
        this.solverManager.resetInitialization()
        this.solverManager.initialize(gridElements)

        // Quick check for trivial puzzles (1 move or already solved) before starting expensive solver
        if (validateDifficulty && isTrivialPuzzle(newState)) {
            d("[TRIVIAL_CHECK] Detected trivial puzzle, regenerating without running solver")
            createValidGame(width, height)
            return
        }

        startTime = System.currentTimeMillis()

        // Start calculating the solution, but use our internal validation callback
        if (validateDifficulty) {
            // Temporarily set validateDifficulty to false to prevent infinite recursion

            // Calculate solution with our own callback to validate difficulty

            d("[calculateSolutionAsync] GameStateManager: Validating puzzle difficulty...")
            calculateSolutionAsync(DifficultyValidationCallback(width, height))
        } else {
            // only debug
            d("[calculateSolutionAsync] GameStateManager: Not validating puzzle difficulty")
            // Regular game initialization, don't validate difficulty
            validateDifficulty = true // Reset for next time
            calculateSolutionAsync(null)
        }
    }

    /**
     * Quick check if puzzle is trivial (already solved or only 1 move needed)
     * This prevents occasionally OutOfMemoryError in solver for very simple puzzles
     * this is a duplicate of isSolution01() but that function did not always work
     */
    private fun isTrivialPuzzle(state: GameState): Boolean {
        // Check if puzzle is already solved
        if (state.isComplete) {
            d("[TRIVIAL_CHECK] Puzzle is already solved")
            return true
        }


        // Check if any robot can reach its target in one move
        // This is a quick heuristic check - not perfect but catches most trivial cases
        for (robot in state.gameElements) {
            if (robot.type != GameElement.TYPE_ROBOT) continue

            val robotX = robot.x
            val robotY = robot.y
            val robotColor = robot.color


            // Find matching target
            for (target in state.gameElements) {
                if (target.type != GameElement.TYPE_TARGET) continue
                if (target.color != robotColor) continue

                val targetX = target.x
                val targetY = target.y


                // Check if robot can reach target in one move (same row or column)
                if (robotX == targetX || robotY == targetY) {
                    // Check if path is clear (no walls blocking)
                    var pathClear = true

                    if (robotX == targetX) {
                        // Vertical movement
                        val startY = min(robotY, targetY)
                        val endY = max(robotY, targetY)
                        for (y in startY..endY) {
                            // Check for horizontal walls blocking vertical movement
                            if (y > startY && state.getCellType(
                                    robotX,
                                    y
                                ) == Constants.TYPE_HORIZONTAL_WALL
                            ) {
                                pathClear = false
                                break
                            }
                        }
                    } else {
                        // Horizontal movement
                        val startX = min(robotX, targetX)
                        val endX = max(robotX, targetX)
                        for (x in startX..endX) {
                            // Check for vertical walls blocking horizontal movement
                            if (x > startX && state.getCellType(
                                    x,
                                    robotY
                                ) == Constants.TYPE_VERTICAL_WALL
                            ) {
                                pathClear = false
                                break
                            }
                        }
                    }

                    if (pathClear) {
                        d(
                            "[TRIVIAL_CHECK] Robot color %d can reach target in 1 move from (%d,%d) to (%d,%d)",
                            robotColor, robotX, robotY, targetX, targetY
                        )
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Callback to validate puzzle difficulty and regenerate if needed
     */
    private inner class DifficultyValidationCallback(
        private val width: Int,
        private val height: Int
    ) : SolutionCallback {
        private var attemptCount = 0

        override fun onSolutionCalculationStarted() {
            d("DifficultyValidationCallback: Calculation started, attempt %d", attemptCount + 1)
        }

        override fun onSolutionCalculationCompleted(solution: GameSolution?) {
            attemptCount++
            val moveCount =
                if (solution != null && solution.moves != null) solution.moves.size else 0
            val requiredMoves: Int = this.minimumRequiredMoves
            val maxMoves: Int = this.maximumRequiredMoves

            // If user manually chose to keep the current map, skip all difficulty validation
            if (keepCurrentMapDespiteDifficulty) {
                d(
                    "[DifficultyValidationCallback][KEEP_MAP_ENFORCER] Skipping difficulty validation - map was manually kept (moves=%d)",
                    moveCount
                )
                // Fall through to acceptance below
            } else {
                // if only one move is needed, then the puzzle is too easy
                if (this.solverManager.isSolution01()) {
                    d("[DifficultyValidationCallback]: Puzzle too easy (1 move), generating new one")
                    createValidGame(width, height)
                    return
                }

                d(
                    "[DifficultyValidationCallback]: Found solution with %d moves (minimum required: %d, maximum required: %d)",
                    moveCount, requiredMoves, maxMoves
                )

                // moveCount==0 means solver couldn't find a solution (memory/depth limit).
                // Try a new map instead of accepting an unsolved puzzle.
                if (moveCount == 0 && attemptCount < MAX_ATTEMPTS) {
                    d(
                        "[DifficultyValidationCallback]: Solver found no solution (memory/depth limit), trying new map (attempt %d/%d)",
                        attemptCount,
                        MAX_ATTEMPTS
                    )
                    createValidGame(width, height)
                    return
                } else if (moveCount == 0) {
                    // Exhausted attempts - accept puzzle as-is to avoid infinite loop
                    w(
                        "[DifficultyValidationCallback]: No solution after %d attempts, accepting puzzle",
                        attemptCount
                    )
                    validateDifficulty = true
                    solutionWasAccepted = true
                    isSolverRunning.setValue(false)
                    return
                }

                if (moveCount < requiredMoves && attemptCount < MAX_ATTEMPTS) {
                    // Puzzle too easy, generate a new one
                    d(
                        "[DifficultyValidationCallback]: Puzzle too easy (%d moves), generating new one",
                        moveCount
                    )
                    createValidGame(width, height)
                    return
                } else if (moveCount > maxMoves && attemptCount < MAX_ATTEMPTS) {
                    // Puzzle too hard, generate a new one
                    d(
                        "[DifficultyValidationCallback]: Puzzle too hard (%d moves), generating new one",
                        moveCount
                    )
                    createValidGame(width, height)
                    return
                }
            }

            // Puzzle is good enough, kept by user, or we've tried too many times
            d(
                "[DifficultyValidationCallback][calculateSolutionAsync] Accepted puzzle with %d moves after %d attempts",
                moveCount, attemptCount
            )
            validateDifficulty = true // Reset validation flag
            // Store the solution
            currentSolution = solution
            currentSolutionStep = 0
            updatePreCompRobotOrder(solution)


            // Set flag to signal Fragment that solution was accepted
            solutionWasAccepted = true

            isSolverRunning.setValue(false)
            // Signal to UI that solution was accepted and hint container should be hidden
            d("[SOLUTION][ACCEPTED][calculateSolutionAsync] Solution accepted, notifying UI to hide hint container")
        }

        override fun onSolutionCalculationFailed(errorMessage: String?) {
            w("DifficultyValidationCallback: Solution calculation failed: %s", errorMessage)
            // Just accept the current puzzle even if we couldn't solve it
            validateDifficulty = true
            isSolverRunning.setValue(false)
        }

        companion object {
            private const val MAX_ATTEMPTS = 999
        }
    }

    override fun onSolverFinished(success: Boolean, solutionMoves: Int, numSolutions: Int) {
        d(
            "[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.onSolverFinished called: success=%b, moves=%d, solutions=%d",
            success, solutionMoves, numSolutions
        )

        // Process on main thread to ensure thread safety with UI updates
        Handler(Looper.getMainLooper()).post(Runnable {
            if (success && numSolutions > 0) {
                // Get the solution from the solver manager
                try {
                    val solution = this.solverManager.getCurrentSolution()
                    if (solution != null) {
                        d(
                            "[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager found solution with %d moves",
                            if (solution.moves != null) solution.moves.size else 0
                        )
                        // Forward to the regular solution handling
                        onSolutionCalculationCompleted(solution)
                    } else {
                        e("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager received null solution despite success=true")
                        onSolutionCalculationFailed("No valid solution found")
                    }
                } catch (e: Exception) {
                    e(
                        e,
                        "[SOLUTION_SOLVER][DIAGNOSTIC] Error getting solution from solver: %s",
                        e.message
                    )
                    onSolutionCalculationFailed("Error: " + e.message)
                }
            } else {
                d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager - No solution found")
                onSolutionCalculationFailed("No solution found")
            }
        })
    }

    override fun onSolverCancelled() {
        d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.onSolverCancelled called")

        // Process on main thread to ensure thread safety with UI updates
        Handler(Looper.getMainLooper()).post(Runnable {
            // Check if map regeneration is enabled
            if (Preferences.generateNewMapEachTime) {
                d("[SOLUTION_SOLVER] generateNewMapEachTime enabled - discarding unsolvable map and generating new one")
                // TODO: Implement map regeneration retry loop when solver is cancelled
                // For now, just notify user that solver was cancelled
            } else {
                d("[SOLUTION_SOLVER] generateNewMapEachTime disabled - showing error to user")
            }
            onSolutionCalculationFailed("Solver was cancelled")
        })
    }


    /**
     * Reset the game to its initial state (Soft Reset)
     * - Preserves the current board/map layout
     * - Resets robot positions to their starting positions
     * - Clears move counters and selection states
     * - Keeps the same target and wall configurations
     * - Perfect for when a player wants to try the same puzzle again
     */
    fun resetGame() {
        isResetting = true

        // Cancel all animations first
        if (robotAnimationManager != null) {
            robotAnimationManager.cancelAllAnimations()
        }

        // Get the current game state
        val currentGameState = currentState.getValue()

        if (currentGameState != null) {
            // Reset robot positions
            currentGameState.resetRobotPositions()

            // Reset game statistics
            moveCount.setValue(0)
            squaresMoved.setValue(0)
            isGameComplete.setValue(false)

            // Reset robot selection
            for (element in currentGameState.gameElements) {
                if (element.isRobot) {
                    element.isSelected = false
                }
            }

            // REMOVED: resetRobots(); - This was causing infinite recursion

            // Reset the robotsUsed tracking for statistics
            robotsUsed.clear()

            // Notify that the game state has changed
            currentState.setValue(currentGameState)
        }

        isResetting = false
    }

    /**
     * Check if animations are enabled
     * 
     * @return True if animations are enabled
     */
    fun areAnimationsEnabled(): Boolean {
        return animationsEnabled
    }

    /**
     * Set the GameGridView for rendering
     * 
     * @param gameGridView The game grid view
     */
    fun setGameGridView(gameGridView: GameGridView?) {
        this.gameGridView = gameGridView

        // Connect the grid view to the animation manager
        if (robotAnimationManager != null) {
            robotAnimationManager.setGameGridView(gameGridView)
        }
    }

    /**
     * Reset all move counts and game history
     * This resets the move counter, squares moved counter, game completion status,
     * and clears both state history and squares moved history.
     */
    fun resetMoveCountsAndHistory() {
        d("[RESET_GAME] Resetting all move counts and game history")
        // Reset move counts and history
        setMoveCount(0)
        resetSquaresMoved() // reset squares moved count
        setGameComplete(false)
        stateHistory.clear()
        squaresMovedHistory.clear()
        clearNextMovesCache()
        isCompletionRecorded = false
    }

    /**
     * reset the solver restart count
     */
    fun resetSolverRestartCount() {
        solverRestartCount = 0
    }

    /**
     * reset last solution min moves
     */
    fun resetLastSolutionMinMoves() {
        lastSolutionMinMoves = 0
    }

    /** reset the game timer but not the UI Timer, then you need to call resetUITimer() to reset the UI Timer  */
    private fun resetGameTimer() {
        gameStartTime = System.currentTimeMillis()
        totalPlayTime = 0
        isHistorySaved = false
        isCompletionRecorded = false
        wrongRobotToastShownColors.clear()
    }

    /**
     * Check if the wrong-robot-on-target toast has already been shown for this robot color this game.
     * Used by GameFragment to ensure the toast appears only once per robot color per game.
     */
    fun hasWrongRobotToastBeenShownFor(robotColor: Int): Boolean {
        return wrongRobotToastShownColors.contains(robotColor)
    }

    /**
     * Mark the wrong-robot-on-target toast as shown for this robot color in this game.
     */
    fun markWrongRobotToastShownFor(robotColor: Int) {
        wrongRobotToastShownColors.add(robotColor)
    }

    private var activity: Activity?
        /**
         * Get the current activity
         * 
         * @return The current activity or null if none is available
         */
        get() {
            if (activityRef != null) {
                return activityRef!!.get()
            }
            return null
        }
        /**
         * Set the current activity reference
         * 
         * @param activity Current activity
         */
        set(activity) {
            if (activity != null) {
                this.activityRef = WeakReference<Activity?>(activity)
                d("[HISTORY] Activity reference updated in GameStateManager")
            }
        }

    /**
     * Set the current game state
     * Used by deep link functionality to load a state from external data
     * 
     * @param state The game state to set
     */
    fun setGameState(state: GameState?) {
        if (state == null) {
            e("[DEEPLINK] Cannot set null game state")
            return
        }

        d("[DEEPLINK] Setting game state from deep link")

        // Skip difficulty validation for externally loaded maps (deeplinks)
        isLoadedFromSave = true
        d("[DEEPLINK] Set isLoadedFromSave=true to skip difficulty move validation")

        // Reset the solver singleton so it picks up the new map instead of the old one
        val solverManager = this.solverManager
        solverManager.resetInitialization()
        solverManager.cancelSolver()
        isSolverRunning.setValue(false) // Reset immediately to avoid race condition with calculateSolutionAsync guard
        d("[DEEPLINK] Reset SolverManager for new deeplink map")

        // Reset UI timer for the new deep link game
        resetUiTimer()

        // --- VALIDATION LOGGING [RANDOM_STATE_VALIDATION] ---
        // Check if the state is a random state (i.e., not from a level, save, or deep link)
        // If so, log a warning. This helps ensure validation is not bypassed.
        if (state.levelId == -1 && (state.levelName == null || state.levelName == "XXXXX")) {
            w(
                "[RANDOM_STATE_VALIDATION] setGameState called with a random state! This may bypass difficulty validation. State: levelId=%d, levelName=%s",
                state.levelId,
                state.levelName
            )
        }

        // --- END VALIDATION LOGGING ---

        // Clear history and reset counters
        resetMoveCountsAndHistory()

        // Set the current map name
        this.levelName = state.levelName
        d("[MAPNAME] GameStateManager.setGameState - Set currentMapName to: %s", this.levelName)

        // Set the connection back to this manager
        state.setGameStateManager(this)

        // Update the current state
        currentState.setValue(state)

        // Update the move count
        moveCount.setValue(state.moveCount)

        // Reset game timer
        resetGameTimer()
        startGameTimer()

        // Start the solver in the background
        calculateSolutionAsync(null)
    }


    // --- Live Move Counter Feature ---
    fun getLiveMoveCounterText(): LiveData<String?> {
        return liveMoveCounterText
    }

    fun isLiveSolverCalculating(): LiveData<Boolean?> {
        return liveSolverCalculating
    }

    fun getLiveMoveCounterDeviation(): LiveData<Int?> {
        return liveMoveCounterDeviation
    }

    fun isLiveMoveCounterEnabled(): Boolean {
        return liveMoveCounterEnabled
    }

    fun setLiveMoveCounterEnabled(enabled: Boolean) {
        this.liveMoveCounterEnabled = enabled
        Preferences.setLiveMoveCounterEnabled(enabled)
        d("[LIVE_SOLVER] Live move counter %s (persisted)", if (enabled) "enabled" else "disabled")
        if (!enabled) {
            liveMoveCounterText.setValue("")
            liveSolverCalculating.setValue(false)
            if (liveSolverManager != null) {
                liveSolverManager!!.cancel()
            }
        }
    }

    /**
     * Trigger the live solver to calculate optimal moves from the current robot positions.
     * Called after each player move when the live move counter is enabled.
     */
    fun triggerLiveSolver() {
        if (!liveMoveCounterEnabled) return

        val state = currentState.getValue()
        if (state == null) return

        // Don't solve if game is already complete
        if (java.lang.Boolean.TRUE == isGameComplete.getValue()) {
            liveMoveCounterText.setValue("")
            liveSolverCalculating.setValue(false)
            return
        }

        // Lazy-init the live solver manager
        if (liveSolverManager == null) {
            liveSolverManager = LiveSolverManager()
        }

        liveSolverCalculating.setValue(true)

        // Ensure lastSolutionMinMoves is up-to-date from currentSolution
        if (lastSolutionMinMoves == 0 && currentSolution != null && currentSolution!!.moves != null) {
            lastSolutionMinMoves = currentSolution!!.moves.size
            d(
                "[LIVE_SOLVER] Updated lastSolutionMinMoves from currentSolution: %d",
                lastSolutionMinMoves
            )
        }

        // Check pre-computation cache first
        val stateHash = computeStateHash(state)
        val cachedResult = nextMovesCache.get(stateHash)
        if (cachedResult != null) {
            d("[PRECOMP_SOLUTION] Cache HIT for state %s → %d moves", stateHash, cachedResult)
            val currentMoves: Int =
                (if (moveCount.getValue() != null) moveCount.getValue() else 0)!!
            val optimal = lastSolutionMinMoves
            val deviation = if (optimal > 0) (currentMoves + cachedResult) - optimal else 0
            val deviationStr =
                if (optimal > 0) " \u0394" + (if (deviation >= 0) "+" else "") + deviation else ""
            val text = RoboyardApplication.getAppContext().getResources().getQuantityString(
                R.plurals.live_move_counter_optimal_plural,
                cachedResult,
                cachedResult
            ) + deviationStr
            liveMoveCounterDeviation.setValue(deviation)
            liveMoveCounterText.setValue(text) // has an observer on the live move counter text in GameFragment.java
            liveSolverCalculating.setValue(false)
            d(
                "[PRECOMP_SOLUTION] Used pre-computed result: %d remaining, %d current, %d optimal, Δ%+d",
                cachedResult,
                currentMoves,
                optimal,
                deviation
            )
            // Pre-compute next moves from this new position
            preComputeNextMoves(state, null)
            return
        }
        d(
            "[PRECOMP_SOLUTION] Cache MISS — no pre-computation available for state %s (cache size: %d)",
            stateHash,
            nextMovesCache.size
        )

        val gridElements = buildGridElements(state)

        d("[LIVE_SOLVER] Triggering live solve with %d elements", gridElements.size)

        liveSolverManager!!.solveAsync(gridElements, object : LiveSolverListener {
            override fun onLiveSolverFinished(remainingMoves: Int, liveSolution: GameSolution?) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    liveSolverCalculating.setValue(false)
                    val currentMoves: Int =
                        (if (moveCount.getValue() != null) moveCount.getValue() else 0)!!
                    val optimal = lastSolutionMinMoves
                    val deviation =
                        if (optimal > 0) (currentMoves + remainingMoves) - optimal else 0
                    val deviationStr =
                        if (optimal > 0) " \u0394" + (if (deviation >= 0) "+" else "") + deviation else ""
                    val text = RoboyardApplication.getAppContext().getResources().getQuantityString(
                        R.plurals.live_move_counter_optimal_plural,
                        remainingMoves,
                        remainingMoves
                    ) + deviationStr
                    liveMoveCounterDeviation.setValue(deviation)
                    liveMoveCounterText.setValue(text)
                    d(
                        "[LIVE_SOLVER] Result: %d remaining, %d current, %d optimal, Δ%+d",
                        remainingMoves,
                        currentMoves,
                        optimal,
                        deviation
                    )
                    // Cache this result and pre-compute next moves
                    nextMovesCache.put(stateHash, remainingMoves)
                    preComputeNextMoves(state, liveSolution)
                })
            }

            override fun onLiveSolverFailed() {
                Handler(Looper.getMainLooper()).post(Runnable {
                    liveSolverCalculating.setValue(false)
                    liveMoveCounterText.setValue("?")
                    d("[LIVE_SOLVER] No solution found from current position")
                })
            }
        })
    }


    /**
     * Build GridElement list from a GameState for the solver.
     */
    private fun buildGridElements(state: GameState): ArrayList<GridElement?> {
        val gridElements = ArrayList<GridElement?>()
        for (element in state.gameElements) {
            var gridElement: GridElement? = null
            when (element.type) {
                GameElement.TYPE_ROBOT -> {
                    val robotType = "robot_" + getColorName(element.color, false)
                    gridElement = GridElement(element.x, element.y, robotType)
                }

                GameElement.TYPE_TARGET -> {
                    val targetType = "target_" + getColorName(element.color, false)
                    gridElement = GridElement(element.x, element.y, targetType)
                }

                GameElement.TYPE_HORIZONTAL_WALL -> gridElement =
                    GridElement(element.x, element.y, "mh")

                GameElement.TYPE_VERTICAL_WALL -> gridElement =
                    GridElement(element.x, element.y, "mv")
            }
            if (gridElement != null) {
                gridElements.add(gridElement)
            }
        }
        return gridElements
    }

    /**
     * Compute a hash string from robot positions in the given state.
     * Used as cache key for pre-computation.
     * Format: r:4,5;g:3,2;b:12,17;y:10,1;
     */
    private fun computeStateHash(state: GameState): String {
        val sb = StringBuilder()
        for (element in state.gameElements) {
            if (element.type == GameElement.TYPE_ROBOT) {
                sb.append(robotColorShort(element.color)).append(':')
                    .append(element.x).append(',')
                    .append(element.y).append(';')
            }
        }
        return sb.toString()
    }

    /**
     * Get robot colors ordered by solution priority.
     * Returns colors of robots from remaining solution moves (unique, in order of appearance).
     * The first robot in the returned list is the one the solver expects the player to move next.
     */
    private fun getSolutionRobotOrder(solution: GameSolution?): MutableList<Int?> {
        val order: MutableList<Int?> = ArrayList<Int?>()
        if (solution != null && solution.moves != null) {
            val moves: MutableList<IGameMove?> = solution.moves
            val startStep = if (solution == currentSolution) currentSolutionStep else 0
            for (i in startStep..<moves.size) {
                val move = moves.get(i)
                if (move is RRGameMove) {
                    val color = move.color
                    if (!order.contains(color)) {
                        order.add(color)
                    }
                }
            }
        }
        return order
    }

    /**
     * Update preCompRobotOrder from a solution's move list.
     * Extracts unique robot colors in order of first appearance.
     */
    private fun updatePreCompRobotOrder(solution: GameSolution?) {
        preCompRobotOrder.clear()
        if (solution != null && solution.moves != null) {
            for (move in solution.moves) {
                if (move is RRGameMove) {
                    val color = move.color
                    if (!preCompRobotOrder.contains(color)) {
                        preCompRobotOrder.add(color)
                    }
                }
            }
        }
        val sb = StringBuilder()
        for (c in preCompRobotOrder) sb.append(Companion.robotColorShort(c!!))
        d("[PRECOMP_SOLUTION] Updated preCompRobotOrder: %s", sb)
    }

    /**
     * Pre-compute optimal moves for all possible next states (4 robots × 4 directions).
     * Runs SEQUENTIALLY — one solver at a time on a single background thread.
     * Checks preComputeCancelled before each solve so a robot move can abort the batch.
     * Results are cached in nextMovesCache for instant lookup.
     */
    private fun preComputeNextMoves(state: GameState, liveSolution: GameSolution?) {
        if (!liveMoveCounterEnabled) return
        if (preComputeRunning) {
            d("[PRECOMP_SOLUTION] Skipping — previous pre-computation still running")
            return
        }

        if (preComputeExecutor == null || preComputeExecutor!!.isShutdown()) {
            preComputeExecutor = Executors.newSingleThreadExecutor(ThreadFactory { r: Runnable? ->
                val t = Thread(r, "precompute-solver")
                t.setDaemon(true)
                t.setPriority(Thread.MIN_PRIORITY)
                t
            })
        }

        // Collect robots and non-robot elements (walls, targets) from current state
        val robots: MutableList<GameElement> = ArrayList<GameElement>()
        val nonRobots: MutableList<GameElement> = ArrayList<GameElement>()
        for (element in state.gameElements) {
            if (element.type == GameElement.TYPE_ROBOT) {
                robots.add(element)
            } else {
                nonRobots.add(element)
            }
        }

        // Sort robots: solution-next robots first, then rest
        // Primary: preCompRobotOrder (survives solver restarts, advanced on each user move)
        // Fallback: liveSolution from live-solver (fresh solve for current position)
        val priorityColors: MutableList<Int?>?
        if (!preCompRobotOrder.isEmpty()) {
            priorityColors = ArrayList<Int?>(preCompRobotOrder)
            d("[PRECOMP_SOLUTION] Using preCompRobotOrder for sorting")
        } else if (liveSolution != null) {
            priorityColors = getSolutionRobotOrder(liveSolution)
            d("[PRECOMP_SOLUTION] Using liveSolution for sorting (preCompRobotOrder empty)")
        } else {
            priorityColors = ArrayList<Int?>()
        }
        if (!priorityColors.isEmpty()) {
            robots.sort(Comparator { a: GameElement?, b: GameElement? ->
                val idxA = priorityColors.indexOf(a!!.color)
                val idxB = priorityColors.indexOf(b!!.color)
                // Robots in priority list come first, in their solution order
                if (idxA >= 0 && idxB >= 0) return@sort idxA - idxB
                if (idxA >= 0) return@sort -1
                if (idxB >= 0) return@sort 1
                0
            })
            val orderLog = StringBuilder()
            for (r in robots) {
                orderLog.append(robotColorShort(r.color))
            }
            d("[PRECOMP_SOLUTION] Robot order (solution-prioritized): %s", orderLog)
        }

        val width = state.width
        val height = state.height
        val directions = arrayOf<IntArray?>(
            intArrayOf(1, 0),
            intArrayOf(-1, 0),
            intArrayOf(0, 1),
            intArrayOf(0, -1)
        )
        val dirNames = arrayOf<String?>("E", "W", "S", "N")

        preComputeRunning = true
        preComputeCancelled = false
        d(
            "[PRECOMP_SOLUTION] Starting sequential pre-computation for %d robots × 4 directions",
            robots.size
        )

        preComputeExecutor!!.submit(Runnable {
            try {
                var computed = 0
                var skipped = 0
                for (robot in robots) {
                    for (d in 0..3) {
                        // Check cancellation / thread interruption before each solve
                        if (preComputeCancelled || Thread.currentThread().isInterrupted()) {
                            d(
                                "[PRECOMP_SOLUTION] Cancelled after %d computed, %d skipped",
                                computed,
                                skipped
                            )
                            return@submit
                        }

                        val dx = directions[d]!![0]
                        val dy = directions[d]!![1]

                        // Simulate the slide: move robot until it hits wall/robot/boundary
                        var newX = robot.x
                        var newY = robot.y
                        if (dx != 0) {
                            val step = if (dx > 0) 1 else -1
                            var i = newX + step
                            while (i >= 0 && i < width) {
                                if (state.canRobotMoveTo(robot, i, newY)) {
                                    newX = i
                                } else {
                                    break
                                }
                                i += step
                            }
                        }
                        if (dy != 0) {
                            val step = if (dy > 0) 1 else -1
                            var i = newY + step
                            while (i >= 0 && i < height) {
                                if (state.canRobotMoveTo(robot, newX, i)) {
                                    newY = i
                                } else {
                                    break
                                }
                                i += step
                            }
                        }

                        // Skip if robot didn't move
                        if (newX == robot.x && newY == robot.y) {
                            skipped++
                            continue
                        }

                        // Compute hash for the hypothetical state
                        val sb = StringBuilder()
                        for (r in robots) {
                            sb.append(robotColorShort(r.color)).append(':')
                            if (r == robot) {
                                sb.append(newX).append(',').append(newY)
                            } else {
                                sb.append(r.x).append(',').append(r.y)
                            }
                            sb.append(';')
                        }
                        val hypotheticalHash = sb.toString()

                        // Skip if already cached
                        if (nextMovesCache.containsKey(hypotheticalHash)) {
                            skipped++
                            continue
                        }

                        // Build grid elements for the hypothetical state
                        val gridElements = ArrayList<GridElement?>()
                        for (r in robots) {
                            val rType = "robot_" + getColorName(r.color, false)
                            if (r == robot) {
                                gridElements.add(GridElement(newX, newY, rType))
                            } else {
                                gridElements.add(GridElement(r.x, r.y, rType))
                            }
                        }
                        for (nr in nonRobots) {
                            var ge: GridElement? = null
                            when (nr.type) {
                                GameElement.TYPE_TARGET -> ge = GridElement(
                                    nr.x,
                                    nr.y,
                                    "target_" + getColorName(nr.color, false)
                                )

                                GameElement.TYPE_HORIZONTAL_WALL -> ge =
                                    GridElement(nr.x, nr.y, "mh")

                                GameElement.TYPE_VERTICAL_WALL -> ge = GridElement(nr.x, nr.y, "mv")
                            }
                            if (ge != null) gridElements.add(ge)
                        }

                        val colorLetter: String = robotColorShort(robot.color)
                        d(
                            "[PRECOMP_SOLUTION] [%d/%d] Solving: %s%s (%d,%d)→(%d,%d)...",
                            computed + skipped + 1, robots.size * 4,
                            colorLetter, dirNames[d], robot.x, robot.y, newX, newY
                        )
                        val solveStart = System.currentTimeMillis()

                        // Solve with some minutes timeout using a sub-executor
                        val solver = SolverDD()
                        solver.init(gridElements)
                        val solverThread = Executors.newSingleThreadExecutor()
                        val solverFuture = solverThread.submit(Runnable { solver.run() })
                        var solverCompleted = false
                        try {
                            solverFuture.get(
                                Constants.PRECOMP_SOLVER_TIMEOUT_SECONDS.toLong(),
                                TimeUnit.SECONDS
                            )
                            solverCompleted = true
                        } catch (te: TimeoutException) {
                            solverFuture.cancel(true)
                            val elapsed = System.currentTimeMillis() - solveStart
                            w(
                                "[PRECOMP_SOLUTION] [%d/%d] TIMEOUT after %dms: %s%s (%d,%d)→(%d,%d)",
                                computed + skipped + 1, robots.size * 4,
                                elapsed, colorLetter, dirNames[d], robot.x, robot.y, newX, newY
                            )
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                        } catch (ee: ExecutionException) {
                            e(ee, "[PRECOMP_SOLUTION] Solver execution error")
                        } finally {
                            solverThread.shutdownNow()
                        }

                        val solveElapsed = System.currentTimeMillis() - solveStart

                        // Check cancellation / thread interruption after solve completes
                        if (preComputeCancelled || Thread.currentThread().isInterrupted()) {
                            d(
                                "[PRECOMP_SOLUTION] Cancelled after solve (%s%s, %dms), %d computed so far",
                                colorLetter, dirNames[d], solveElapsed, computed
                            )
                            return@submit
                        }

                        if (solverCompleted && solver.getSolverStatus()!!.isFinished) {
                            val numSolutions =
                                if (solver.getSolutionList() != null) solver.getSolutionList()!!.size else 0
                            if (numSolutions > 0) {
                                val solution = solver.getSolution(0)
                                var moves =
                                    if (solution != null && solution.moves != null) solution.moves.size else 0
                                if (solver.isSolution01()) moves = 1
                                nextMovesCache.put(hypotheticalHash, moves)
                                computed++
                                d(
                                    "[PRECOMP_SOLUTION] [%d/%d] Solved in %dms: %s%s (%d,%d)→(%d,%d) = %d moves",
                                    computed + skipped,
                                    robots.size * 4,
                                    solveElapsed,
                                    colorLetter,
                                    dirNames[d],
                                    robot.x,
                                    robot.y,
                                    newX,
                                    newY,
                                    moves
                                )
                            } else {
                                d(
                                    "[PRECOMP_SOLUTION] [%d/%d] No solution in %dms: %s%s (%d,%d)→(%d,%d)",
                                    computed + skipped + 1,
                                    robots.size * 4,
                                    solveElapsed,
                                    colorLetter,
                                    dirNames[d],
                                    robot.x,
                                    robot.y,
                                    newX,
                                    newY
                                )
                            }
                        } else if (solverCompleted) {
                            d(
                                "[PRECOMP_SOLUTION] [%d/%d] Solver not finished in %dms: %s%s (%d,%d)→(%d,%d)",
                                computed + skipped + 1, robots.size * 4,
                                solveElapsed, colorLetter, dirNames[d], robot.x, robot.y, newX, newY
                            )
                        }
                    }
                }
                d(
                    "[PRECOMP_SOLUTION] Finished: %d computed, %d skipped, cache size: %d",
                    computed,
                    skipped,
                    nextMovesCache.size
                )
            } catch (e: Exception) {
                e(e, "[PRECOMP_SOLUTION] Error during pre-computation")
            } finally {
                preComputeRunning = false
            }
        })
    }

    /**
     * Cancel any running pre-computation. Called when a robot move starts
     * so the solver is not running in parallel with the live solver.
     */
    private fun cancelPreComputation() {
        if (preComputeRunning) {
            preComputeCancelled = true
            d("[PRECOMP_SOLUTION] Cancellation requested — shutting down executor")
            if (preComputeExecutor != null) {
                preComputeExecutor!!.shutdownNow()
                preComputeExecutor = null
            }
            preComputeRunning = false
        }
    }

    /**
     * Clear the pre-computation cache. Call on new game / reset.
     */
    fun clearNextMovesCache() {
        cancelPreComputation()
        nextMovesCache.clear()
        d("[PRECOMP_SOLUTION] Cache cleared")
    }

    init {
        // We'll use lazy initialization for solver now - do not create it here
        context = application.getApplicationContext()

        // Initialize robotAnimationManager
        robotAnimationManager = RobotAnimationManager(this)
    }

    /**
     * Save metadata about the autosave so the Play button can quickly check
     * if current settings still match the autosaved game.
     */
    private fun saveAutosaveMetadata(state: GameState) {
        val prefs = getApplication<Application?>()!!.getSharedPreferences(
            AUTOSAVE_META_PREFS,
            Context.MODE_PRIVATE
        )
        // Count targets in the state
        var targets = 0
        for (el in state.gameElements) {
            if (el.type == GameElement.TYPE_TARGET) targets++
        }
        prefs.edit()
            .putInt(AUTOSAVE_META_BOARD_W, state.width)
            .putInt(AUTOSAVE_META_BOARD_H, state.height)
            .putInt(AUTOSAVE_META_TARGET_COUNT, targets)
            .apply()
        d("[AUTOSAVE_META] Saved metadata: %dx%d, %d targets", state.width, state.height, targets)
    }

    /**
     * Check if current settings match the autosave metadata.
     * @return true if settings match (autosave can be resumed), false if settings changed
     */
    fun autosaveSettingsMatch(): Boolean {
        val prefs = getApplication<Application?>()!!.getSharedPreferences(
            AUTOSAVE_META_PREFS,
            Context.MODE_PRIVATE
        )
        val savedW = prefs.getInt(AUTOSAVE_META_BOARD_W, -1)
        val savedH = prefs.getInt(AUTOSAVE_META_BOARD_H, -1)
        val savedTargets = prefs.getInt(AUTOSAVE_META_TARGET_COUNT, -1)

        if (savedW == -1) {
            d("[AUTOSAVE_META] No metadata found, settings don't match")
            return false
        }

        val match =
            savedW == Preferences.boardSizeWidth && savedH == Preferences.boardSizeHeight && savedTargets == Preferences.targetColors

        d(
            "[AUTOSAVE_META] Settings match: %s (saved: %dx%d/%d targets, current: %dx%d/%d targets)",
            match, savedW, savedH, savedTargets,
            Preferences.boardSizeWidth, Preferences.boardSizeHeight, Preferences.targetColors
        )
        return match
    }

    /**
     * Clear autosave metadata (called when autosave is deleted).
     */
    fun clearAutosaveMetadata() {
        getApplication<Application?>()!!.getSharedPreferences(
            AUTOSAVE_META_PREFS,
            Context.MODE_PRIVATE
        )
            .edit().clear().apply()
        d("[AUTOSAVE_META] Metadata cleared")
    }

    /**
     * Schedule hint button reset after a delay to avoid race condition when loading games.
     * Called when a new game is loaded from history, save, or level.
     * @param hintButton The ToggleButton to reset
     */
    fun scheduleHintButtonReset(hintButton: ToggleButton?) {
        if (hintButton == null) return

        d(
            "[HINT_SYSTEM] New game loaded - scheduling hint button reset in %d ms",
            HINT_BUTTON_RESET_DELAY_MS
        )


        // Cancel any pending reset
        if (hintButtonResetRunnable != null) {
            mainHandler.removeCallbacks(hintButtonResetRunnable!!)
        }


        // Schedule hint button reset after delay to avoid race condition
        hintButtonResetRunnable = Runnable? {
            if (hintButton != null && hintButton.isChecked()) {
                d("[HINT_SYSTEM] Executing delayed hint button reset")
                hintButton.setChecked(false)
            }
        }
        mainHandler.postDelayed(hintButtonResetRunnable!!, HINT_BUTTON_RESET_DELAY_MS)
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel any pending hint button reset
        if (hintButtonResetRunnable != null) {
            mainHandler.removeCallbacks(hintButtonResetRunnable!!)
        }
        if (liveSolverManager != null) {
            liveSolverManager!!.shutdown()
        }
        if (preComputeExecutor != null) {
            preComputeCancelled = true
            preComputeExecutor!!.shutdownNow()
        }
    }

    companion object {
        private const val MAX_AUTO_REGENERATIONS = 999

        // Move cooldown to prevent multiple moves within
        private const val MOVE_COOLDOWN_MS: Long = 400 // milliseconds
        private const val HISTORY_SAVE_THRESHOLD = 30 // seconds threshold for saving to history

        private const val HINT_BUTTON_RESET_DELAY_MS: Long =
            1000 // close hint container after some milliseconds

        /**
         * Extract metadata from save data
         * 
         * @param saveData The save data string
         * @return A map containing the metadata or null if no metadata was found
         */
        @JvmStatic
        fun extractMetadataFromSaveData(saveData: String?): MutableMap<String?, String?> {
            val metadata: MutableMap<String?, String?> = HashMap<String?, String?>()

            // Check if the save data has a metadata line
            if (saveData != null && saveData.startsWith("#")) {
                // Extract the first line
                val endOfFirstLine = saveData.indexOf('\n')
                if (endOfFirstLine > 0) {
                    val metadataLine = saveData.substring(1, endOfFirstLine)

                    // Parse metadata entries (MAPNAME:name;TIME:seconds;MOVES:count;SOLUTIONS:0U,1R|0D,1L;)
                    // Note: SOLUTIONS can contain | separators, so we need to handle it specially
                    val entries = metadataLine.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    for (entry in entries) {
                        val colonPos = entry.indexOf(":")
                        if (colonPos > 0) {
                            val key = entry.substring(0, colonPos)
                            val value = entry.substring(colonPos + 1)
                            metadata.put(key, value)
                        }
                    }
                }
            }

            return metadata
        }

        /**
         * Short color letter for log/hash: r=red/pink, g=green, b=blue, y=yellow, s=silver.
         */
        private fun robotColorShort(colorId: Int): String {
            when (colorId) {
                Constants.COLOR_PINK -> return "r"
                Constants.COLOR_GREEN -> return "g"
                Constants.COLOR_BLUE -> return "b"
                Constants.COLOR_YELLOW -> return "y"
                else -> return colorId.toString()
            }
        }

        // ── Autosave metadata for settings comparison ─────────────────────
        private const val AUTOSAVE_META_PREFS = "AutosaveMetadata"
        private const val AUTOSAVE_META_BOARD_W = "autosave_board_w"
        private const val AUTOSAVE_META_BOARD_H = "autosave_board_h"
        private const val AUTOSAVE_META_TARGET_COUNT = "autosave_target_count"
    }
}
