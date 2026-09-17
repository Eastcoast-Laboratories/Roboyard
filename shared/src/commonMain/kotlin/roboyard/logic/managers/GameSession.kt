package roboyard.logic.managers

import driftingdroids.model.TimeProvider
import kotlin.concurrent.Volatile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import roboyard.logic.core.calculateStars as sharedCalculateStars
import roboyard.logic.core.saveLevelCompletion
import roboyard.logic.platform.requestGc
import roboyard.logic.solver.ERRGameMove
import roboyard.logic.solver.RRGameMove
import roboyard.logic.solver.RRPiece
import roboyard.logic.solver.SolverDD
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.ui.StringProvider
import roboyard.logic.ui.UiNotifier
import roboyard.logic.ui.getStringProvider
import roboyard.logic.util.RLog

/**
 * Platform-independent game session manager.
 * Holds the complete game logic that was previously implemented in the
 * Android-only GameStateManager ViewModel: game state, movement, undo,
 * solver lifecycle, hints, save/load, history, timers, completion,
 * difficulty validation, map regeneration, live move counter and the
 * precomputation cache.
 *
 * Platform-specific concerns (animations, toasts, achievements, sync,
 * navigation, Android UI) are exposed through injectable hooks so Android
 * and Compose can attach their own behavior while sharing all logic.
 */
class GameSession(
    private val storage: PlatformStorage,
    private val scope: CoroutineScope,
    private val stringProvider: StringProvider = getStringProvider()
) {
    private val log = RLog.tag("GameSession")

    // ── Platform hooks ────────────────────────────────────────────────────

    /** Shows UI messages (Toast/Snackbar). Set by the platform. */
    var uiNotifier: UiNotifier? = null

    /** Called when a session-scoped achievement should be unlocked (e.g. view_1_hour). */
    var achievementUnlocker: ((String) -> Unit)? = null

    /** Called ~1.5s after game completion so the platform can upload history (SyncManager). */
    var historyUploadTrigger: (() -> Unit)? = null

    /**
     * Formats the live move counter text. Android uses plurals resources;
     * the default implementation uses the shared StringProvider.
     */
    var liveCounterFormatter: ((remainingMoves: Int) -> String)? = null

    /**
     * When set, robot moves are delegated to this animator instead of applying
     * the position immediately. The animator must invoke onComplete when done.
     * Signature: (robot, fromX, fromY, toX, toY, onComplete).
     */
    var moveAnimator: ((GameElement, Int, Int, Int, Int, () -> Unit) -> Unit)? = null

    /**
     * Called after a reverse-move undo so the platform can update visuals
     * (GameGridView.applyPathSegmentUndoVisual + selectRobotByColor on Android).
     * Signature: (removedPathEntry, undoneRobotColor).
     */
    var onReverseMoveUndo: ((IntArray?, Int) -> Unit)? = null

    /**
     * Called after a move animation completes so the platform can run
     * visual side effects (GameGridView.handleRobotMovementEffects on Android).
     * Signature: (state, robot, oldX, oldY).
     */
    var onRobotMoveCompleted: ((GameState, GameElement, Int, Int) -> Unit)? = null

    /** Reads a file by absolute path (history entries outside app storage). */
    var externalFileReader: ((String) -> String?)? = null

    /**
     * Called when map regeneration gave up after max attempts and the last
     * (difficulty-violating) map was accepted — UI may show a warning dialog.
     * Signature: (attempts, moveCount of accepted solution or null).
     */
    var onMapFallbackAccepted: ((Int, Int?) -> Unit)? = null

    // ── Observable state (mirrors Android LiveData) ───────────────────────

    private val _currentState = MutableStateFlow<GameState?>(null)
    val currentState = _currentState

    /**
     * Monotonic revision counter, bumped whenever the GameState object is
     * (re-)assigned or mutated in place. StateFlow conflates identical
     * instances, so observers that render the state must collect this flow.
     * (Android LiveData re-emits on setValue(sameInstance); this preserves
     * that behavior for Compose.)
     */
    private val _stateRevision = MutableStateFlow(0L)
    val stateRevision = _stateRevision

    /** Incremented on every new game / level / loaded state — UI key for per-game resets. */
    private val _gameCounter = MutableStateFlow(0)
    val gameCounter = _gameCounter

    /** Assign a (possibly mutated) state and notify observers. */
    private fun emitState(state: GameState?) {
        _currentState.value = state
        _stateRevision.value = _stateRevision.value + 1
    }

    private val _moveCount = MutableStateFlow(0)
    val moveCount = _moveCount

    private val _squaresMoved = MutableStateFlow(0)
    val squaresMoved = _squaresMoved

    private val _isGameComplete = MutableStateFlow(false)
    val isGameComplete = _isGameComplete

    /** Carries color of wrong robot that just landed on a target, -1 = none. */
    private val _wrongRobotAtTarget = MutableStateFlow(-1)
    val wrongRobotAtTarget = _wrongRobotAtTarget

    private val _isSolverRunning = MutableStateFlow(false)
    val isSolverRunning = _isSolverRunning

    private val _newGameLoadedEvent = MutableStateFlow(false)
    val newGameLoadedEvent = _newGameLoadedEvent

    private val _liveMoveCounterText = MutableStateFlow("")
    val liveMoveCounterText = _liveMoveCounterText

    private val _liveSolverCalculating = MutableStateFlow(false)
    val liveSolverCalculating = _liveSolverCalculating

    private val _liveMoveCounterDeviation = MutableStateFlow(0)
    val liveMoveCounterDeviation = _liveMoveCounterDeviation

    // ── Move history for undo ─────────────────────────────────────────────

    private val stateHistory = ArrayList<GameState?>()
    private val squaresMovedHistory = ArrayList<Int?>()

    /** Path history for visual robot paths: [robotColor, fromX, fromY, toX, toY] */
    val pathHistory: ArrayList<IntArray> = ArrayList()

    /** Robot starting positions: map robot color to starting position [x,y] */
    val robotStartingPositions: HashMap<Int?, IntArray?> = HashMap()

    // ── Game settings ─────────────────────────────────────────────────────

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled = _soundEnabled

    // ── Solver lifecycle ──────────────────────────────────────────────────

    private var solver: SolverDD? = null
    private var solverInitialized = false
    private var solverJob: Job? = null
    private var predefinedSolution: String? = null
    private var predefinedNumMoves: Int = 0

    /** Solutions from the solver or loaded from a save file. */
    private var solverSolutionList: List<driftingdroids.model.Solution>? = null

    var levelName: String? = ""
        private set
    private var startTime: Long = 0

    // Solution state — backed by a StateFlow so UIs can observe it
    private val _currentSolution = MutableStateFlow<GameSolution?>(null)
    val solutionFlow = _currentSolution
    var currentSolution: GameSolution?
        get() = _currentSolution.value
        private set(value) {
            _currentSolution.value = value
        }

    /** The current solution step (hint number, 0-indexed). */
    var currentSolutionStep: Int = 0
        private set

    /** Solutions loaded from save file for re-saving. */
    private var loadedSolutions: MutableList<GameSolution?>? = null

    /** Remembered robot order from last known solution (survives solver restarts). */
    private val preCompRobotOrder: MutableList<Int?> = ArrayList()

    // Track hint usage and unique robots used for level completion statistics
    private var hintsShown = 0
    private val robotsUsed: MutableSet<Int?> = HashSet()

    private var isResetting = false

    private val _solverRestartCountFlow = MutableStateFlow(0)
    /** Observable mirror of [solverRestartCount] for Compose UI (keep-map button, restart counter). */
    val solverRestartCountFlow: StateFlow<Int> = _solverRestartCountFlow

    /** Number of times the solver has been restarted. */
    var solverRestartCount: Int = 0
        private set(value) {
            field = value
            _solverRestartCountFlow.value = value
        }

    private val _lastSolutionMinMovesFlow = MutableStateFlow(0)
    /** Observable mirror of [lastSolutionMinMoves] for Compose UI (restart counter info). */
    val lastSolutionMinMovesFlow: StateFlow<Int> = _lastSolutionMinMovesFlow

    /** Minimum moves from the last found solution, or 0 if none yet. */
    var lastSolutionMinMoves: Int = 0
        private set(value) {
            field = value
            _lastSolutionMinMovesFlow.value = value
        }

    // UI timer tracking (survives fragment/screen recreation)
    var uiTimerElapsedMs: Long = 0
        private set
    private var uiTimerWasRunning = false

    private var lastMoveTime: Long = 0

    /** True if a new game was just loaded (timer should reset). */
    var isNewGameLoaded: Boolean = false
        private set
    private var solutionWasAccepted = false

    // Game history tracking variables
    private var gameStartTime: Long = 0
    private var totalPlayTime = 0
    private var isHistorySaved = false
    private var isViewTimeAchievementChecked = false
    private var isCompletionRecorded = false

    /** Robot colors that already triggered the "wrong robot on target" toast this game. */
    private val wrongRobotToastShownColors: MutableSet<Int?> = HashSet()

    /** Collision info from the last movement (read by the game grid view). */
    var lastMoveHitWall: Boolean = false
        private set
    private var lastMoveHitRobot = false

    /** The robot that was hit in the last movement, or null. */
    var lastMoveHitRobotElement: GameElement? = null
        private set

    /** True if the current game was loaded from a savegame (skips difficulty validation). */
    var isLoadedFromSave: Boolean = false
        private set

    /** True if the current game was loaded from history (back = previous history entry). */
    var isLoadedFromHistory: Boolean = false
        private set

    private var currentHistoryPath: String? = null

    // Live move counter feature
    private var liveSolverJob: Job? = null
    private var liveSolver: SolverDD? = null
    private var liveMoveCounterEnabled = false

    // Pre-computation cache for next possible moves (copy-on-write map: single writer)
    @Volatile
    private var nextMovesCache: Map<String, Int> = emptyMap()
    private var preComputeJob: Job? = null

    @Volatile
    private var preComputeRunning = false

    @Volatile
    private var preComputeCancelled = false

    // Difficulty validation / regeneration
    private var validateDifficulty = true
    private var regenerationCount = 0
    private var allowRegeneration = true
    private var keepCurrentMapDespiteDifficulty = false

    private var solutionCallback: SolutionCallback? = null

    // ── Game lifecycle ────────────────────────────────────────────────────

    /** Start a new random game. */
    open fun startNewGame() {
        log.d("startNewGame() called")
        startGame()
    }

    /** Start a new game. */
    fun startGame() {
        log.d("startGame() called")

        // Reset loaded game flags - new games should use current difficulty settings
        isLoadedFromSave = false
        isLoadedFromHistory = false
        currentHistoryPath = null
        log.d("[NEW_GAME] Reset isLoadedFromSave, isLoadedFromHistory and currentHistoryPath, using current difficulty settings")

        // Reset any existing solver state to ensure a clean calculation for the new game
        resetSolverInitialization()
        cancelSolver() // Cancel any running solver process
        _isSolverRunning.value = false // Reset immediately to avoid race condition with calculateSolutionAsync guard

        // Clear any existing solution to prevent it from being reused
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null
        preCompRobotOrder.clear()
        resetSolverRestartCount()
        resetLastSolutionMinMoves()

        // Reset regeneration counter
        regenerationCount = 0
        keepCurrentMapDespiteDifficulty = false

        // Reset UI timer for the new game
        resetUiTimer()

        // Reset history tracking so a new history entry is created for this game
        resetGameTimer()
        startGameTimer()

        // New game starts with no moves — clear robot trail history
        pathHistory.clear()

        // Create a new valid game (will regenerate if solution is too simple)
        createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight)

        // Record start time
        startTime = TimeProvider.currentTimeMillis()

        log.d("startGame() complete")
    }

    /**
     * Start a level game.
     * @param levelId Level ID to load
     */
    /**
     * Loads the GameState for a level. Custom levels (custom_level_N.txt in
     * private storage, written by the level editor) take precedence over
     * bundled asset levels. Throws if neither source provides the level.
     */
    private fun loadLevelState(levelId: Int): GameState {
        val customFile = "custom_level_$levelId.txt"
        try {
            if (storage.fileExists(customFile)) {
                val content = storage.readFile(customFile)
                if (content.isNotEmpty()) {
                    return GameState.parseLevel(content, levelId)
                }
            }
        } catch (e: Exception) {
            log.e(e, "[LEVEL_LOAD] Failed to load custom level %d from %s", levelId, customFile)
            throw RuntimeException("Failed to load level $levelId", e)
        }
        return GameState.loadLevel(levelId)
    }

    fun startLevelGame(levelId: Int) {
        log.d("startLevelGame() called with levelId: %d", levelId)

        // If solver is already running, don't create a new game state to avoid mismatch
        if (_isSolverRunning.value) {
            log.d("[SOLUTION_SOLVER] startLevelGame: Solver already running, not creating new game state")
            return
        }

        // Reset any existing solver state to ensure a clean calculation for the new level
        resetSolverInitialization()
        cancelSolver()
        _isSolverRunning.value = false

        // Clear any existing solution to prevent it from being reused
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null
        preCompRobotOrder.clear()

        // A new level starts with no moves — drop the previous game's path
        // history so UIs do not redraw stale robot trails (Android clears it
        // via GameGridView.clearRobotPaths on every level change)
        pathHistory.clear()

        // Load level: custom level file in private storage takes precedence over
        // bundled assets (custom_level_N.txt is written by the level editor).
        val state = loadLevelState(levelId)
        state.levelId = levelId
        state.levelName = "Level " + levelId

        // Save last played level for scroll position in level selection
        LevelCompletionManager.getInstance(storage).lastPlayedLevel = levelId

        // Set reference to this session in the new state
        state.setGameStateManager(this)

        // Set the current state
        emitState(state)
        _gameCounter.value = _gameCounter.value + 1
        this.levelName = "Level-" + levelId

        // Reset move counts and history
        setMoveCount(0)
        resetSquaresMoved()
        setGameComplete(false)
        stateHistory.clear()
        squaresMovedHistory.clear()
        clearNextMovesCache()

        // Disable live-move-mode for level games (only available in random games)
        setLiveMoveCounterEnabled(false)

        // Initialize the solver with the grid elements from the loaded level
        val gridElements = state.gridElements
        log.d(
            "[SOLUTION_SOLVER] Initializing solver with %d grid elements from level %d",
            gridElements.size, levelId
        )
        initializeSolver(gridElements)

        // Check if level has a predefined solution (for complex levels like 140)
        if (state.hasPredefinedSolution()) {
            log.d(
                "[SOLUTION_SOLVER] Level %d has predefined solution with %d moves",
                levelId, state.predefinedNumMoves
            )
            setPredefinedSolution(state.predefinedSolution, state.predefinedNumMoves)
        }

        // Start calculating the solution automatically
        calculateSolutionAsync(null)

        // Reset UI timer for the new level
        resetUiTimer()

        // Record start time
        startTime = TimeProvider.currentTimeMillis()

        log.d("startLevelGame() complete for level %d", levelId)

        resetStatistics()
    }

    /**
     * Load a specific level.
     * @param levelId Level ID to load
     */
    open fun loadLevel(levelId: Int) {
        val newState = GameState.loadLevel(levelId)
        newState.levelId = levelId

        newState.setGameStateManager(this)

        emitState(newState)
        _gameCounter.value = _gameCounter.value + 1
        _moveCount.value = 0
        _isGameComplete.value = false

        resetGameTimer()
        startGameTimer()

        val gridElements = newState.gridElements
        initializeSolver(gridElements)
    }

    /**
     * Load a saved game.
     * @param saveId Save slot ID
     */
    fun loadGame(saveId: Int) {
        if (saveId >= 0) {
            val newState = GameState.loadSavedGame(storage, saveId)
            if (newState != null) {
                isLoadedFromSave = true
                isLoadedFromHistory = false

                log.d(
                    "[LOAD_GAME] Loading saved game with difficulty: %d (current settings difficulty: %d)",
                    newState.difficulty, Preferences.difficulty
                )

                applyLoadedGameState(newState)
                log.d("Successfully loaded game from slot %d", saveId)
            } else {
                log.e("Failed to load game state from slot %d", saveId)
            }
        }
    }

    /**
     * Apply a loaded GameState to the current game.
     * Common logic used by saved games, history entries and deep links.
     * @return true if successful, false otherwise
     */
    fun applyLoadedGameState(newState: GameState): Boolean {
        // Mark that a new game was loaded - timer should reset
        isNewGameLoaded = true
        _newGameLoadedEvent.value = true
        log.d("[TIMER] New game loaded - timer will reset")
        log.d("[HINT_SYSTEM] New game loaded event triggered")

        newState.setGameStateManager(this)

        log.d("[GAME_LOAD] Analyzing loaded game data")
        log.d("[GAME_LOAD] Board size: %d x %d", newState.width, newState.height)
        log.d("[GAME_LOAD] Map name: %s", newState.levelName)

        // Store saved solutions string for later use (after reset)
        val savedSolutionsStr = newState.savedSolutions
        if (!savedSolutionsStr.isNullOrEmpty()) {
            log.d("[SOLUTIONS_SAVE_LOAD] Found saved solutions in metadata: %s", savedSolutionsStr)
        }

        // Log all game elements
        var robotCount = 0
        var targetCount = 0
        for (element in newState.gameElements) {
            when (element.type) {
                GameElement.TYPE_ROBOT -> robotCount++
                GameElement.TYPE_TARGET -> targetCount++
            }
        }
        log.d(
            "[GAME_LOAD] Element summary - Robots: %d, Targets: %d",
            robotCount, targetCount
        )

        this.levelName = newState.levelName
        log.d("[MAPNAME] GameSession - Set currentMapName to: %s", this.levelName)

        // Check for targets in the board data (cellType and targetColors)
        var boardTargetCount = 0
        for (y in 0..<newState.height) {
            for (x in 0..<newState.width) {
                if (newState.getCellType(x, y) == Constants.TYPE_TARGET) {
                    boardTargetCount++
                }
            }
        }
        log.d("[GAME_LOAD] Board target count: %d", boardTargetCount)

        // Recreate target GameElements if only present in board data
        if (targetCount == 0 && boardTargetCount > 0) {
            log.w(
                "[GAME_LOAD] No targets found in gameElements but %d targets found in board data. Recreating targets.",
                boardTargetCount
            )
            for (y in 0..<newState.height) {
                for (x in 0..<newState.width) {
                    if (newState.getCellType(x, y) == Constants.TYPE_TARGET) {
                        val color = newState.getTargetColor(x, y)
                        val target = GameElement(GameElement.TYPE_TARGET, x, y)
                        target.color = color
                        newState.gameElements.add(target)
                    }
                }
            }
        }

        if (targetCount == 0 && boardTargetCount == 0) {
            val errorMessage = "No targets found in loaded game data"
            log.e("[GAME_LOAD] %s", errorMessage)
            throw IllegalStateException(errorMessage)
        }

        // Adjust robotCount to match the actual number of targets
        val actualTargetCount = max(targetCount, boardTargetCount)
        val previousRobotCount = newState.getRobotCount()
        if (actualTargetCount > 0 && actualTargetCount != previousRobotCount) {
            newState.setRobotCount(actualTargetCount)
            log.d(
                "[GAME_LOAD] Adjusted robotCount from %d to %d to match actual target count",
                previousRobotCount, actualTargetCount
            )
        }

        newState.resetRobotPositions()

        val syncedTargets = newState.synchronizeTargets()
        if (syncedTargets > 0) {
            log.d("[GAME_LOAD] Synchronized %d targets after loading", syncedTargets)
        }

        emitState(newState)
        _gameCounter.value = _gameCounter.value + 1
        _moveCount.value = newState.moveCount
        _isGameComplete.value = newState.isComplete

        log.d(
            "[BOARD_SIZE_DEBUG] GameState board size: %dx%d",
            newState.width, newState.height
        )

        // Store walls from loaded game in WallStorage if generateNewMapEachTime is off
        if (!Preferences.generateNewMapEachTime) {
            val gridElements = buildGridElements(newState)
            getInstance().storeWallsForBoardSize(
                gridElements.filterNotNull().toMutableList(), newState.width, newState.height
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
        resetSolverInitialization()

        // Reset history tracking so this replay is treated as a new game session
        resetGameTimer()

        // Initialize solver with grid elements from the loaded map
        initializeSolverForState(newState)

        // NOW deserialize and set solutions AFTER reset and initialization
        if (!savedSolutionsStr.isNullOrEmpty()) {
            val loaded = deserializeSolutions(savedSolutionsStr)
            if (!loaded.isNullOrEmpty()) {
                this.currentSolution = loaded[0]
                this.currentSolutionStep = 0
                log.d(
                    "[SOLUTIONS_SAVE_LOAD] Loaded %d solutions from save, using first solution with %d moves",
                    loaded.size, currentSolution!!.moves.size
                )

                this.loadedSolutions = loaded

                // Convert solution to predefined format for the solver path
                val solutionStr = StringBuilder()
                for (move in currentSolution!!.moves) {
                    if (solutionStr.isNotEmpty()) {
                        solutionStr.append(" ")
                    }
                    val rrMove = move as RRGameMove?
                    val color = rrMove!!.color
                    val direction = rrMove.direction

                    solutionStr.append(color)
                    when (direction) {
                        1 -> solutionStr.append("U")
                        2 -> solutionStr.append("R")
                        4 -> solutionStr.append("D")
                        8 -> solutionStr.append("L")
                        else -> {
                            log.e("[SOLUTIONS_SAVE_LOAD] unknown direction %d in solution", direction)
                            solutionStr.append(direction)
                        }
                    }
                }
                setPredefinedSolution(solutionStr.toString(), currentSolution!!.moves.size)
            }
        }

        // Only start solver if no solution was loaded from save file
        if (currentSolution == null || currentSolution!!.moves.isEmpty()) {
            log.d("[GAME_LOAD] No saved solution found, starting solver for loaded map")
            calculateSolutionAsync(null)
        } else {
            log.d(
                "[SOLUTIONS_SAVE_LOAD] Using loaded solution with %d moves, skipping solver calculation",
                currentSolution!!.moves.size
            )
            solutionWasAccepted = true
            _isSolverRunning.value = false
            solutionCallback?.onSolutionCalculationCompleted(currentSolution)
        }

        return true
    }

    /** Initialize the solver with grid elements from a game state. */
    private fun initializeSolverForState(state: GameState) {
        val gridElements = buildGridElements(state)
        initializeSolver(gridElements)
        log.d(
            "[SOLVER_INIT] Initialized solver with %d grid elements including robots and targets",
            gridElements.size
        )
    }

    /**
     * Load a game from a history entry.
     * @param mapPath Path to the history entry file (relative to app storage or absolute)
     */
    fun loadHistoryEntry(mapPath: String) {
        log.d("Loading history entry: %s", mapPath)

        try {
            val absolute = isAbsolutePath(mapPath)
            if (absolute) {
                val reader = externalFileReader
                if (reader == null) {
                    log.e("[HISTORY_LOAD] No externalFileReader for absolute path: %s", mapPath)
                    return
                }
                val saveData = reader(mapPath)
                if (saveData == null) {
                    log.e("History file does not exist: %s", mapPath)
                    return
                }
                parseHistorySaveData(saveData, mapPath)
            } else {
                if (!storage.fileExists(mapPath)) {
                    log.e("History file does not exist: %s", storage.getFilePath(mapPath))
                    return
                }
                val saveData = storage.readFile(mapPath)
                log.d(
                    "[HISTORY_LOAD] Read %d characters from history file: %s",
                    saveData.length, storage.getFilePath(mapPath)
                )
                parseHistorySaveData(saveData, mapPath)
            }
        } catch (e: Exception) {
            log.e(e, "Error loading history entry: %s", mapPath)
        }
    }

    private fun parseHistorySaveData(saveData: String, mapPath: String) {
        val newState = parseFromSaveData(saveData)
        if (newState != null) {
            isLoadedFromSave = true
            isLoadedFromHistory = true
            currentHistoryPath = mapPath

            log.d("[HISTORY_LOAD] Loaded history entry with difficulty: %d", newState.difficulty)
            applyLoadedGameState(newState)
            log.d("[HISTORY_LOAD] Successfully loaded history entry: %s", mapPath)
        } else {
            log.e("[HISTORY_LOAD] Failed to parse game state from history file: %s", mapPath)
        }
    }

    private fun isAbsolutePath(path: String): Boolean {
        return path.startsWith("/") || Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(path)
    }

    /**
     * Save the current game to a slot.
     * @param saveId The save slot ID (0 = autosave)
     * @param isAutoSave true if this is a system autosave, false for manual saves
     * @return true if the game was saved successfully
     */
    fun saveGame(saveId: Int, isAutoSave: Boolean = false): Boolean {
        // Slot 0 is reserved for auto-save only - prevent manual saves unless no autosave exists yet
        if (saveId == 0 && !isAutoSave) {
            val autoSaveFileName =
                Constants.SAVE_DIRECTORY + "/" + Constants.SAVE_FILENAME_PREFIX + 0 + Constants.SAVE_FILENAME_EXTENSION
            if (storage.fileExists(autoSaveFileName)) {
                log.e("[SAVE_PROTECTION] Attempted manual save to slot 0 (auto-save slot) - blocked (autosave already exists)")
                return false
            }
            log.d("[SAVE_PROTECTION] Allowing manual save to slot 0 - no autosave exists yet")
        }

        log.d("Saving game to slot %d (autosave: %b)", saveId, isAutoSave)

        val gameState = _currentState.value
        if (gameState == null) {
            log.e("Cannot save game: No valid GameState available")
            return false
        }

        try {
            val fileName =
                Constants.SAVE_DIRECTORY + "/" + Constants.SAVE_FILENAME_PREFIX + saveId + Constants.SAVE_FILENAME_EXTENSION

            val saveData = gameState.serialize()
            val enhancedSaveData = StringBuilder(saveData)

            // Add metadata tags if not already present
            if (!saveData.contains("DIFFICULTY:")) {
                val difficultyLevel = Preferences.difficulty
                if (saveData.indexOf("\n") > 0) {
                    val difficultyTag = "DIFFICULTY:" + difficultyLevel + ";"
                    enhancedSaveData.insert(saveData.indexOf(";", 0) + 1, difficultyTag)
                    log.d("[SAVEDATA] Added difficulty tag: %s", difficultyTag)
                }
            }

            if (!saveData.contains("SIZE:")) {
                val sizeTag = "SIZE:" + gameState.width + "," + gameState.height + ";"
                if (enhancedSaveData.indexOf("\n") > 0) {
                    enhancedSaveData.insert(enhancedSaveData.indexOf(";", 0) + 1, sizeTag)
                    log.d("[SAVEDATA] Added size tag: %s", sizeTag)
                }
            }

            if (!saveData.contains("SOLVED:")) {
                val solvedTag = "SOLVED:" + gameState.isComplete + ";"
                if (enhancedSaveData.indexOf("\n") > 0) {
                    enhancedSaveData.insert(enhancedSaveData.indexOf(";", 0) + 1, solvedTag)
                    log.d("[SAVEDATA] Added solved tag: %s", solvedTag)
                }
            }

            if (!enhancedSaveData.toString().contains("MAX_HINT_USED:")) {
                val hintTag = "MAX_HINT_USED:" + gameState.maxHintUsedThisSession + ";"
                enhancedSaveData.insert(enhancedSaveData.indexOf(";", 0) + 1, hintTag)
                log.d("[SAVEDATA] Added hint tag: %s", hintTag)
            }

            if (!enhancedSaveData.toString().contains("MOVES:")) {
                val movesTag = "MOVES:" + gameState.moveCount + ";"
                enhancedSaveData.insert(enhancedSaveData.indexOf(";", 0) + 1, movesTag)
                log.d("[SAVEDATA] Added moves tag: %s", movesTag)
            }

            // Add map signature for history lookup (Base64 to avoid delimiter collision)
            if (!enhancedSaveData.toString().contains("MAP_SIG:")) {
                val mapSig = gameState.generateMapSignature()
                if (!mapSig.isNullOrEmpty()) {
                    @OptIn(ExperimentalEncodingApi::class)
                    val encoded = Base64.encode(mapSig.encodeToByteArray())
                    val sigTag = "MAP_SIG:" + encoded + ";"
                    enhancedSaveData.insert(enhancedSaveData.indexOf(";", 0) + 1, sigTag)
                    log.d("[SAVEDATA] Added map signature tag (base64-encoded)")
                }
            }

            // Add all solver solutions if available
            if (!enhancedSaveData.toString().contains("SOLUTIONS:")) {
                val solutionsTag = serializeAllSolutions()
                if (!solutionsTag.isNullOrEmpty()) {
                    enhancedSaveData.insert(enhancedSaveData.indexOf(";", 0) + 1, solutionsTag)
                    log.d("[SAVEDATA] Added solutions tag")
                }
            }

            // If this is an autosave (slot 0), store settings metadata for quick comparison
            if (saveId == 0) {
                saveAutosaveMetadata(gameState)
            }

            val success = storage.writeFile(fileName, enhancedSaveData.toString())
            if (!success) {
                log.e("Error saving game to %s", fileName)
                return false
            }
            log.d("Game saved to %s", fileName)

            // VERIFICATION: Read back the save file and check for targets
            val savedContent = storage.readFile(fileName)
            if (savedContent.isEmpty() || !validateSaveContainsTargets(savedContent, fileName)) {
                log.e("[SAVE_VERIFICATION] FATAL ERROR: Save file validation failed - no targets found")
                storage.deleteFile(fileName)
                throw IllegalStateException("Save file validation failed: No targets found in saved game")
            }
            return true
        } catch (e: IllegalStateException) {
            log.e("[SAVE_ERROR] %s", e.message)
            throw RuntimeException("FATAL: " + e.message, e)
        } catch (e: Exception) {
            log.e("Unexpected error saving game: %s", e.message)
            return false
        }
    }

    /** Validates that save data contains at least one target (all games MUST have targets). */
    private fun validateSaveContainsTargets(saveData: String, fileName: String): Boolean {
        log.d("[SAVE_VERIFICATION] Validating save file: %s", fileName)

        if (saveData.contains("TARGET_SECTION:")) {
            return true
        }
        if (saveData.matches("(?s).*\\|T-?\\d+@\\d+,\\d+;.*".toRegex())) {
            return true
        }
        if (saveData.matches("(?s).*(?:^|\\n|;)t[rgbyms]\\d+,\\d+;.*".toRegex())) {
            return true
        }
        if (saveData.matches("(?s).*target_(red|green|blue|yellow)\\d+,\\d+;.*".toRegex())) {
            return true
        }
        val lines = saveData.split("\n")
        for (line in lines) {
            if (line.contains(Constants.TYPE_TARGET.toString() + ":")) {
                return true
            }
        }
        log.e("[SAVE_VERIFICATION] NO TARGETS FOUND IN SAVE DATA")
        return false
    }

    /**
     * Serialize all solver solutions to a compact string format for saving.
     * Format: SOLUTIONS:0U,1R,0D|0U,2L,1D|...
     */
    private fun serializeAllSolutions(): String? {
        try {
            if (!loadedSolutions.isNullOrEmpty()) {
                return serializeGameSolutions(loadedSolutions)
            }

            val solutions = solverSolutionList
            if (solutions.isNullOrEmpty()) {
                return null
            }

            val sb = StringBuilder("SOLUTIONS:")
            var first = true

            for (i in solutions.indices) {
                val gameSolution = getSolverSolution(i) ?: continue
                if (gameSolution.moves.isEmpty()) continue

                if (!first) sb.append("|")
                first = false
                appendSerializedMoves(sb, gameSolution, false)
            }

            sb.append(";")
            return sb.toString()
        } catch (e: Exception) {
            log.e(e, "[SOLUTIONS_SAVE_LOAD] Error serializing solutions: %s", e.message)
            return null
        }
    }

    /** Serialize GameSolution objects directly (used for re-saving loaded games). */
    private fun serializeGameSolutions(gameSolutions: MutableList<GameSolution?>?): String? {
        try {
            if (gameSolutions.isNullOrEmpty()) {
                return null
            }

            val sb = StringBuilder("SOLUTIONS:")
            var first = true

            for (gameSolution in gameSolutions) {
                if (gameSolution == null || gameSolution.moves.isEmpty()) continue

                if (!first) sb.append("|")
                first = false
                appendSerializedMoves(sb, gameSolution, true)
            }

            sb.append(";")
            return sb.toString()
        } catch (e: Exception) {
            log.e(e, "[SOLUTIONS_SAVE_LOAD] Error serializing game solutions: %s", e.message)
            return null
        }
    }

    /** Encode each move as colorDirection (0U, 1R, ...). */
    private fun appendSerializedMoves(sb: StringBuilder, gameSolution: GameSolution, unknownAsQuestion: Boolean) {
        var firstMove = true
        for (move in gameSolution.moves) {
            if (!firstMove) sb.append(",")
            firstMove = false

            val rrMove = move as RRGameMove?
            sb.append(rrMove!!.color)
            when (rrMove.direction) {
                1 -> sb.append("U")
                2 -> sb.append("R")
                4 -> sb.append("D")
                8 -> sb.append("L")
                else -> {
                    if (unknownAsQuestion) {
                        sb.append("?")
                    } else {
                        log.e("[SOLUTIONS_SAVE_LOAD] unknown direction %d", rrMove.direction)
                        sb.append(rrMove.direction)
                    }
                }
            }
        }
    }

    /** Deserialize solutions from save data (SOLUTIONS: tag value). */
    private fun deserializeSolutions(solutionsString: String?): MutableList<GameSolution?>? {
        try {
            if (solutionsString.isNullOrEmpty()) {
                return null
            }

            val solutions: MutableList<GameSolution?> = ArrayList()
            val solutionStrings =
                solutionsString.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }

            for (solutionStr in solutionStrings) {
                if (solutionStr.isEmpty()) continue

                val solution = GameSolution()
                val moves = solutionStr.split(",")

                for (moveStr in moves) {
                    if (moveStr.length < 2) continue

                    val color = moveStr.substring(0, moveStr.length - 1).toInt()
                    val direction: ERRGameMove = when (moveStr[moveStr.length - 1]) {
                        'U' -> ERRGameMove.UP
                        'R' -> ERRGameMove.RIGHT
                        'D' -> ERRGameMove.DOWN
                        'L' -> ERRGameMove.LEFT
                        else -> ERRGameMove.NOMOVE
                    }

                    val piece = RRPiece(0, 0, color, color)
                    solution.addMove(RRGameMove(piece, direction))
                }

                solutions.add(solution)
            }

            log.d("[SOLUTIONS_SAVE_LOAD] Deserialized %d solutions from save data", solutions.size)
            return solutions
        } catch (e: Exception) {
            log.e(e, "[SOLUTIONS_SAVE_LOAD] Error deserializing solutions: %s", e.message)
            return null
        }
    }

    // ── Touch / movement ──────────────────────────────────────────────────

    /**
     * Handle a touch on the game grid.
     * @param action touch action; use [ACTION_UP] for release
     */
    fun handleGridTouch(x: Int, y: Int, action: Int): Boolean {
        log.d("[TOUCH] Handle grid touch at (%d,%d) with action %d", x, y, action)

        val state = _currentState.value
        if (state != null) {
            val selectedRobot = state.getSelectedRobot()

            if (action == ACTION_UP) {
                val touchedRobot = state.getRobotAt(x, y)

                if (touchedRobot != null) {
                    log.d("[TOUCH] Selecting robot at (%d,%d)", x, y)
                    state.setSelectedRobot(touchedRobot)

                    if (touchedRobot.color >= 0) {
                        robotsUsed.add(touchedRobot.color)
                    }

                    emitState(state)
                } else if (selectedRobot != null) {
                    val robotX = selectedRobot.x
                    val robotY = selectedRobot.y

                    var dx = 0
                    var dy = 0

                    if (robotX == x) {
                        dy = if (y > robotY) 1 else -1
                    } else if (robotY == y) {
                        dx = if (x > robotX) 1 else -1
                    } else {
                        // Diagonal tap - determine primary direction
                        val deltaX = x - robotX
                        val deltaY = y - robotY

                        if (abs(deltaX) > abs(deltaY)) {
                            dx = if (deltaX > 0) 1 else -1
                        } else {
                            dy = if (deltaY > 0) 1 else -1
                        }
                    }

                    if (dx != 0 || dy != 0) {
                        log.d("[TOUCH] Moving robot in direction dx=%d, dy=%d", dx, dy)
                        moveRobotInDirection(dx, dy)
                    }
                }
            }
        }
        return true
    }

    /**
     * Move the selected robot in a specific direction until it hits an obstacle.
     * @return True if the robot moved, false otherwise
     */
    fun moveRobotInDirection(dx: Int, dy: Int): Boolean {
        // Check if move cooldown is active
        val currentTime = TimeProvider.currentTimeMillis()
        if (currentTime - lastMoveTime < MOVE_COOLDOWN_MS) {
            log.d(
                "[MOVE_COOLDOWN] Move blocked: %dms remaining",
                MOVE_COOLDOWN_MS - (currentTime - lastMoveTime)
            )
            return false
        }

        // Cancel any running pre-computation immediately on robot move
        cancelPreComputation()

        val state = _currentState.value
        if (state == null || state.getSelectedRobot() == null) {
            return false
        }

        if (isResetting) {
            return false
        }

        val robot = state.getSelectedRobot()!!

        // Check if the robot is moving back to its last position (reverse move = undo)
        // Only if game is not already complete - after goal, reverse moves are forward moves
        if (!_isGameComplete.value && pathHistory.isNotEmpty()) {
            val lastPath = pathHistory[pathHistory.size - 1]
            val lastColor = lastPath[0]
            val lastFromX = lastPath[1]
            val lastFromY = lastPath[2]
            val lastToX = lastPath[3]
            val lastToY = lastPath[4]
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
                // If the robot would land exactly on its previous position, treat as undo
                if (endX2 == lastFromX && endY2 == lastFromY) {
                    log.d(
                        "[ROBOTS][UNDO] Reverse move detected for robot %d: (%d,%d)->(%d,%d), triggering undo",
                        robot.color, lastToX, lastToY, lastFromX, lastFromY
                    )
                    val undoneRobotColor = robot.color
                    val removedPath = removeLastPathFromHistory()
                    val undone = undoLastMove()
                    if (undone) {
                        onReverseMoveUndo?.invoke(removedPath, undoneRobotColor)
                    }
                    return undone
                }
            }
        }

        // Advance preCompRobotOrder if the moved robot matches the expected next color
        if (preCompRobotOrder.isNotEmpty() && preCompRobotOrder[0] == robot.color) {
            preCompRobotOrder.removeAt(0)
        }
        val startX = robot.x
        val startY = robot.y

        var endX = startX
        var endY = startY

        if (dx != 0) {
            robot.directionX = dx
        }

        var hitWall = false
        var hitRobot = false

        // Snapshot the current state for undo before making any changes
        val stateBeforeMove = snapshotState(state)
        stateHistory.add(stateBeforeMove)
        squaresMovedHistory.add(_squaresMoved.value)

        var hitRobotElement: GameElement? = null

        if (dx != 0) {
            val step = if (dx > 0) 1 else -1
            var i = startX + step
            while (i >= 0 && i < state.width) {
                if (state.canRobotMoveTo(robot, i, startY)) {
                    endX = i
                } else {
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

        if (dy != 0) {
            val step = if (dy > 0) 1 else -1
            var i = startY + step
            while (i >= 0 && i < state.height) {
                if (state.canRobotMoveTo(robot, startX, i)) {
                    endY = i
                } else {
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

        this.lastMoveHitWall = hitWall
        this.lastMoveHitRobot = hitRobot
        this.lastMoveHitRobotElement = hitRobotElement

        val distanceMoved = abs(endX - startX) + abs(endY - startY)

        if (distanceMoved > 0) {
            val originalX = startX
            val originalY = startY
            val targetX = endX
            val targetY = endY

            log.d(
                "[ROBOT][HINT_SYSTEM] Movement INITIATED: Robot %d moving from (%d,%d) to (%d,%d)",
                robot.color, originalX, originalY, targetX, targetY
            )

            val wasFirstMove = pathHistory.isEmpty()
            if (!_isGameComplete.value) {
                _squaresMoved.value += distanceMoved
                _moveCount.value += 1
                state.moveCount = _moveCount.value

                // Save history immediately on first move (before threshold)
                if (wasFirstMove && !isHistorySaved) {
                    log.d("[HISTORY] First move detected, saving history immediately (async)")
                    isHistorySaved = true
                    scope.launch(Dispatchers.Default) {
                        try {
                            saveToHistory()
                        } catch (e: Exception) {
                            log.e(e, "[HISTORY] Error saving history on first move")
                        }
                    }
                }
            } else {
                log.d("[GAME_COMPLETE] Game already completed, not incrementing move/squares counters")
            }

            // Track the robot and direction for hint verification
            var directionConstant = 0
            if (dx > 0) directionConstant = 2 // RIGHT
            else if (dx < 0) directionConstant = 8 // LEFT
            else if (dy < 0) directionConstant = 1 // UP
            else if (dy > 0) directionConstant = 4 // DOWN

            state.lastMovedRobot = robot
            state.lastMoveDirection = directionConstant

            // Completion callback applied after animation or immediately
            val completionCallback = {
                robot.x = targetX
                robot.y = targetY

                if (!_isGameComplete.value && state.areAllRobotsAtTargets()) {
                    setGameComplete(true)
                } else if (!_isGameComplete.value && state.isRobotOnWrongTarget(robot)) {
                    _wrongRobotAtTarget.value = robot.color
                    _wrongRobotAtTarget.value = -1
                }

                emitState(state)

                triggerLiveSolver()
            }

            val animator = moveAnimator
            if (animator != null) {
                // Animated path: the platform animates and calls onComplete when done
                val oldX = robot.x
                val oldY = robot.y
                animator(robot, originalX, originalY, targetX, targetY) {
                    completionCallback()
                    onRobotMoveCompleted?.invoke(state, robot, oldX, oldY)
                    if (robot.color >= 0) {
                        robotsUsed.add(robot.color)
                    }
                }
            } else {
                // Immediate mode - update position without animation
                robot.x = targetX
                robot.y = targetY

                if (!_isGameComplete.value && state.areAllRobotsAtTargets()) {
                    setGameComplete(true)
                } else if (!_isGameComplete.value && state.isRobotOnWrongTarget(robot)) {
                    _wrongRobotAtTarget.value = robot.color
                    _wrongRobotAtTarget.value = -1
                }

                emitState(state)

                triggerLiveSolver()
            }

            lastMoveTime = TimeProvider.currentTimeMillis()

            return true
        }

        return false
    }

    /** Deep-copy a GameState for the undo history. */
    private fun snapshotState(state: GameState): GameState {
        val stateBeforeMove = GameState(state.width, state.height)

        for (y in 0..<state.height) {
            for (x in 0..<state.width) {
                val cellType = state.getCellType(x, y)
                stateBeforeMove.setCellType(x, y, cellType)

                if (cellType == Constants.TYPE_TARGET) {
                    val targetColor = state.getTargetColor(x, y)
                    stateBeforeMove.setTargetColor(x, y, targetColor)
                }
            }
        }

        val elements = state.gameElements
        for (element in elements) {
            if (element.type == GameElement.TYPE_ROBOT) {
                stateBeforeMove.addRobot(element.x, element.y, element.color)
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

        stateBeforeMove.levelId = state.levelId
        stateBeforeMove.levelName = state.levelName
        stateBeforeMove.moveCount = state.moveCount
        stateBeforeMove.setCompleted(state.isComplete)
        stateBeforeMove.setRobotCount(state.getRobotCount())
        stateBeforeMove.setTargetColors(state.getTargetColors())
        stateBeforeMove.difficulty = state.difficulty

        for (i in 0..<state.hintCount) {
            stateBeforeMove.incrementHintCount()
        }

        stateBeforeMove.uniqueMapId = state.uniqueMapId

        if (state.initialRobotPositions != null) {
            stateBeforeMove.initialRobotPositions = HashMap()
            for (entry in state.initialRobotPositions!!.entries) {
                stateBeforeMove.initialRobotPositions!![entry.key] =
                    intArrayOf(entry.value!![0], entry.value!![1])
            }
        }

        return stateBeforeMove
    }

    /**
     * Move the selected robot using a Board direction constant
     * (Constants.NORTH/EAST/SOUTH/WEST). Convenience wrapper for UIs.
     */
    fun moveRobot(direction: Int): Boolean {
        val dx: Int
        val dy: Int
        when (direction) {
            Constants.NORTH -> { dx = 0; dy = -1 }
            Constants.SOUTH -> { dx = 0; dy = 1 }
            Constants.EAST -> { dx = 1; dy = 0 }
            Constants.WEST -> { dx = -1; dy = 0 }
            else -> return false
        }
        return moveRobotInDirection(dx, dy)
    }

    /**
     * Select the robot with the given color index (0=red/pink, 1=green,
     * 2=blue, 3=yellow, 4=silver — same order as Board.robotPositions).
     * @return the selected robot element, or null if not found
     */
    fun selectRobotByColor(color: Int): GameElement? {
        val state = _currentState.value ?: return null
        val robot = state.gameElements.firstOrNull {
            it.type == GameElement.TYPE_ROBOT && it.color == color
        } ?: return null
        state.setSelectedRobot(robot)
        if (color >= 0) {
            robotsUsed.add(color)
        }
        emitState(state)
        return robot
    }

    /**
     * Find and select a robot matching the color of the first target —
     * Android GameFragment.selectRobotWithTargetColor, called when the game
     * screen is shown (onViewCreated/onResume), independent of accessibility mode.
     * @return true if a matching robot was found and selected
     */
    fun selectRobotWithTargetColor(): Boolean {
        val state = _currentState.value ?: return false
        val target = state.gameElements.firstOrNull { it.type == GameElement.TYPE_TARGET }
            ?: return false
        val robot = state.gameElements.firstOrNull {
            it.isRobot && it.color == target.color
        } ?: return false
        state.setSelectedRobot(robot)
        emitState(state)
        return true
    }

    /** The currently selected robot element, or null. */
    fun getSelectedRobot(): GameElement? {
        return _currentState.value?.getSelectedRobot()
    }

    /** True if at least one move can be undone. */
    fun canUndo(): Boolean = stateHistory.isNotEmpty()

    /**
     * Get a hint for the next move.
     * @return The next move according to the solver, or null if no solution exists
     */
    val hint: IGameMove?
        get() {
            if (currentSolution != null && currentSolutionStep < currentSolution!!.moves.size) {
                hintsShown++
                val move = currentSolution!!.moves[currentSolutionStep]
                incrementSolutionStep()
                return move
            }
            return null
        }

    /**
     * Record that a hint was shown to the player (completion stats +
     * GameState session tracking). UI hint systems call this directly.
     */
    fun recordHintShown(hintIndex: Int) {
        hintsShown++
        _currentState.value?.recordHintUsed(hintIndex)
    }

    // ── Path / starting position tracking ─────────────────────────────────

    fun addPathToHistory(color: Int, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        pathHistory.add(intArrayOf(color, fromX, fromY, toX, toY))
    }

    fun removeLastPathFromHistory(): IntArray? {
        if (pathHistory.isEmpty()) return null
        return pathHistory.removeAt(pathHistory.size - 1)
    }

    fun clearPathHistory() {
        pathHistory.clear()
    }

    fun setRobotStartingPosition(color: Int, x: Int, y: Int) {
        robotStartingPositions[color] = intArrayOf(x, y)
    }

    fun clearRobotStartingPositions() {
        robotStartingPositions.clear()
    }

    // ── Settings ──────────────────────────────────────────────────────────

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        Preferences.setSoundEnabled(enabled)
    }

    // ── Undo ──────────────────────────────────────────────────────────────

    /**
     * Undo the last move if possible.
     * @return true if a move was undone, false otherwise
     */
    fun undoLastMove(): Boolean {
        if (stateHistory.isEmpty()) {
            log.d("[ROBOTS] undoLastMove: No history to undo, stateHistory is empty")
            return false
        }

        val previousState = stateHistory.removeAt(stateHistory.size - 1)

        if (previousState != null) {
            if (squaresMovedHistory.isNotEmpty()) {
                val previousSquaresMoved =
                    squaresMovedHistory.removeAt(squaresMovedHistory.size - 1)!!
                _squaresMoved.value = previousSquaresMoved
            }

            emitState(previousState)

            _moveCount.value = max(0, _moveCount.value - 1)

            if (_isGameComplete.value) {
                _isGameComplete.value = false
            }

            // Re-trigger live solver so the display updates instead of disappearing
            triggerLiveSolver()

            return true
        } else {
            log.e("[ROBOTS] undoLastMove: Previous state was null, this should not happen")
        }

        return false
    }

    // ── Counters ──────────────────────────────────────────────────────────

    fun setMoveCount(count: Int) {
        _moveCount.value = count
    }

    fun resetSquaresMoved() {
        _squaresMoved.value = 0
        squaresMovedHistory.clear()
    }

    fun setSquaresMoved(squares: Int) {
        _squaresMoved.value = squares
    }

    // ── Completion ────────────────────────────────────────────────────────

    /**
     * Set whether the game is complete.
     * Handles level completion data, history updates and upload triggers.
     */
    fun setGameComplete(complete: Boolean) {
        val state = _currentState.value
        if (state != null) {
            // Guard: prevent duplicate completion triggers for LEVEL games only
            if (complete && state.levelId > 0 && _isGameComplete.value) {
                log.d(
                    "[HISTORY_SYNC] setGameComplete(true) called but already complete for level %d, ignoring duplicate",
                    state.levelId
                )
                return
            }
            log.d("Setting game complete: %s for level %d", complete, state.levelId)
            state.setCompleted(complete)
            _isGameComplete.value = complete

            // Save level completion data if this is a level game
            if (complete && state.levelId > 0) {
                val manager = LevelCompletionManager.getInstance(storage)
                val starsBefore = manager.totalStars
                val data = saveLevelCompletionData(state)

                if (data != null) {
                    manager.saveLevelCompletionData(data)
                    log.d("Saved level completion data: %s", data)

                    // Update history entries with stars/moves before upload
                    val finalLevelId = state.levelId
                    val finalStars = data.getStars()
                    val finalMoves = state.moveCount

                    scope.launch(Dispatchers.Default) {
                        try {
                            delay(500)
                            val allEntries = GameHistoryManager.getHistoryEntries(storage)
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
                                }
                            }
                            if (updatedCount > 0) {
                                GameHistoryManager.saveHistoryIndex(storage, allEntries)
                                log.d(
                                    "[HISTORY_SYNC] Updated and persisted %d level history entries with stars+moves",
                                    updatedCount
                                )
                            }
                        } catch (e: Exception) {
                            log.e(e, "[HISTORY_SYNC] Error updating level history entries with stars")
                        }
                    }
                }

                val starsAfter = manager.totalStars
                if (starsBefore < LEVEL_EDITOR_UNLOCK_STARS && starsAfter >= LEVEL_EDITOR_UNLOCK_STARS) {
                    val message = stringProvider.getString("level_editor_unlocked")
                    if (message != null) {
                        uiNotifier?.showMessage(message)
                    }
                    log.d("[LEVEL_EDITOR] Level Editor unlocked at %d stars!", starsAfter)
                }
            }

            // Auto-upload history after ANY game completion (level or random)
            if (complete) {
                triggerHistoryUpload(state.levelId)
            }
        }
    }

    /**
     * Trigger automatic history upload after game completion.
     * Waits briefly for saveToHistory to persist, then notifies the platform hook.
     */
    private fun triggerHistoryUpload(levelId: Int) {
        val gameType = if (levelId > 0) "level " + levelId else "random game"
        log.d("[HISTORY_SYNC] Triggering automatic history upload after %s completion", gameType)

        val trigger = historyUploadTrigger ?: return

        scope.launch(Dispatchers.Default) {
            try {
                delay(1500)
                trigger()
            } catch (e: Exception) {
                log.e(e, "[HISTORY_SYNC] Exception during upload after %s", gameType)
            }
        }
    }

    /**
     * Save level completion data when a level is completed.
     * @return The prepared LevelCompletionData object
     */
    private fun saveLevelCompletionData(state: GameState): LevelCompletionData? {
        val levelId = state.levelId
        if (levelId <= 0) {
            return null // Not a level game
        }

        val manager = LevelCompletionManager.getInstance(storage)

        var optimalMoves = 0
        if (currentSolution != null) {
            optimalMoves = currentSolution!!.moves.size
        }

        val playerMoves = _moveCount.value
        val starCount = sharedCalculateStars(playerMoves, optimalMoves, hintsShown)

        saveLevelCompletion(
            manager,
            levelId,
            playerMoves,
            hintsShown,
            optimalMoves,
            starCount,
            _squaresMoved.value,
            TimeProvider.currentTimeMillis() - startTime,
            robotsUsed.size
        )

        val data = manager.getLevelCompletionData(levelId)
        return data
    }

    /**
     * Calculate star rating based on player performance (delegates to shared logic).
     */
    fun calculateStars(playerMoves: Int, optimalMoves: Int, hintsUsed: Int): Int {
        return sharedCalculateStars(playerMoves, optimalMoves, hintsUsed)
    }

    /** Total number of stars earned across all levels. */
    val totalStars: Int
        get() = LevelCompletionManager.getInstance(storage).totalStars

    // ── UI timer ──────────────────────────────────────────────────────────

    fun saveUiTimerElapsed(elapsedMs: Long) {
        this.uiTimerElapsedMs = elapsedMs
        this.uiTimerWasRunning = true
    }

    fun wasUiTimerRunning(): Boolean {
        return uiTimerWasRunning
    }

    fun resetUiTimer() {
        this.uiTimerElapsedMs = 0
        this.uiTimerWasRunning = false
        this.isNewGameLoaded = false
    }

    fun pauseTimer() {
        // Timer pause is handled by the platform screen
    }

    fun resumeTimer() {
        // Timer resume is handled by the platform screen
    }

    fun clearNewGameLoadedFlag() {
        this.isNewGameLoaded = false
    }

    fun solutionWasAccepted(): Boolean {
        return solutionWasAccepted
    }

    fun clearSolutionAcceptedFlag() {
        this.solutionWasAccepted = false
    }

    // ── Difficulty ────────────────────────────────────────────────────────

    /** True if the current game is a level game (not a random game). */
    val isInLevelGame: Boolean
        get() {
            val state = _currentState.value
            return state != null && state.levelId > 0
        }

    /** The current level ID, or -1 if not in a level game. */
    val currentLevelId: Int
        get() {
            val state = _currentState.value
            return if (state != null) state.levelId else -1
        }

    var difficulty: Int
        get() = Preferences.difficulty
        set(difficulty) {
            Preferences.setDifficulty(difficulty)
        }

    /**
     * Calculate difficulty based on level number.
     * Levels 1-35: Beginner, 36-70: Advanced, 71-105: Insane, 106-140: Impossible.
     */
    private fun calculateDifficultyForLevel(levelId: Int): Int {
        return if (levelId <= 35) {
            Constants.DIFFICULTY_BEGINNER
        } else if (levelId <= 70) {
            Constants.DIFFICULTY_ADVANCED
        } else if (levelId <= 105) {
            Constants.DIFFICULTY_INSANE
        } else {
            Constants.DIFFICULTY_IMPOSSIBLE
        }
    }

    /**
     * Effective difficulty: level-based for level games, saved for savegames,
     * preferences for random games.
     */
    val effectiveDifficulty: Int
        get() {
            val state = _currentState.value
            if (state != null && state.levelId > 0) {
                return calculateDifficultyForLevel(state.levelId)
            } else if (isLoadedFromSave) {
                return state!!.difficulty
            } else {
                return Preferences.difficulty
            }
        }

    val localizedDifficultyString: String
        get() {
            val key = when (this.effectiveDifficulty) {
                Constants.DIFFICULTY_BEGINNER -> "difficulty_beginner"
                Constants.DIFFICULTY_ADVANCED -> "difficulty_advanced"
                Constants.DIFFICULTY_INSANE -> "difficulty_insane"
                Constants.DIFFICULTY_IMPOSSIBLE -> "difficulty_impossible"
                else -> "difficulty_unknown"
            }
            return stringProvider.getString(key) ?: key
        }

    // ── History navigation ────────────────────────────────────────────────

    fun clearLoadedFromHistoryFlag() {
        isLoadedFromHistory = false
        currentHistoryPath = null
    }

    /** Filtered history entries (level games excluded, matching history tab display). */
    private fun filteredHistoryEntries(): MutableList<GameHistoryEntry> {
        val allEntries = GameHistoryManager.getHistoryEntries(storage)
        val filtered = ArrayList<GameHistoryEntry>()
        for (entry in allEntries) {
            val mapName = entry.mapName
            if (mapName == null || !mapName.matches("(?i)^Level\\s+\\d+.*".toRegex())) {
                filtered.add(entry)
            }
        }
        return filtered
    }

    private fun currentHistoryIndex(filteredEntries: List<GameHistoryEntry>): Int {
        for (i in filteredEntries.indices) {
            if (filteredEntries[i].getMapPath() == currentHistoryPath) {
                return i
            }
        }
        return -1
    }

    /** Load the previous history entry (if one exists). */
    fun loadPreviousHistoryEntry(): Boolean {
        if (currentHistoryPath == null) {
            return false
        }
        try {
            val filteredEntries = filteredHistoryEntries()
            if (filteredEntries.isEmpty()) return false

            val currentIndex = currentHistoryIndex(filteredEntries)
            if (currentIndex <= 0) return false

            loadHistoryEntry(filteredEntries[currentIndex - 1].getMapPath())
            return true
        } catch (e: Exception) {
            log.e(e, "[HISTORY_NAV] Error loading previous history entry")
            return false
        }
    }

    /** Load the next history entry (if one exists). */
    fun loadNextHistoryEntry(): Boolean {
        if (currentHistoryPath == null) {
            return false
        }
        try {
            val filteredEntries = filteredHistoryEntries()
            if (filteredEntries.isEmpty()) return false

            val currentIndex = currentHistoryIndex(filteredEntries)
            if (currentIndex == -1 || currentIndex == filteredEntries.size - 1) return false

            loadHistoryEntry(filteredEntries[currentIndex + 1].getMapPath())
            return true
        } catch (e: Exception) {
            log.e(e, "[HISTORY_NAV] Error loading next history entry")
            return false
        }
    }

    /** Check if there is a next history entry available. */
    fun hasNextHistoryEntry(): Boolean {
        if (currentHistoryPath == null) {
            return false
        }
        try {
            val filteredEntries = filteredHistoryEntries()
            if (filteredEntries.isEmpty()) return false

            val currentIndex = currentHistoryIndex(filteredEntries)
            return currentIndex != -1 && currentIndex < filteredEntries.size - 1
        } catch (e: Exception) {
            log.e(e, "[HISTORY_NAV] Error checking for next history entry")
            return false
        }
    }

    // ── Game timer / history tracking ─────────────────────────────────────

    /** Start the game timer for history tracking. */
    fun startGameTimer() {
        gameStartTime = TimeProvider.currentTimeMillis()
        isHistorySaved = false
        isViewTimeAchievementChecked = false
        isCompletionRecorded = false
        wrongRobotToastShownColors.clear()
        log.d("[HISTORY] Game timer started")
    }

    /** Update the game timer and check if game should be saved to history. */
    fun updateGameTimer() {
        if (!isHistorySaved && _currentState.value != null) {
            val elapsedSeconds =
                ((TimeProvider.currentTimeMillis() - gameStartTime) / 1000).toInt()
            totalPlayTime = elapsedSeconds

            if (totalPlayTime >= HISTORY_SAVE_THRESHOLD) {
                log.d(
                    "[HISTORY] Threshold reached (%d seconds), saving game to history",
                    HISTORY_SAVE_THRESHOLD
                )
                saveToHistory()
                isHistorySaved = true
            }

            // Check for "Don't give up" achievement (view_1_hour)
            if (!isViewTimeAchievementChecked && totalPlayTime >= 3600) {
                if (achievementUnlocker != null && !isViewTimeAchievementChecked) {
                    achievementUnlocker!!.invoke("view_1_hour")
                    log.d(
                        "[ACHIEVEMENT] Unlocking view_1_hour ('Don't give up') - played for %d seconds",
                        totalPlayTime
                    )
                }
                isViewTimeAchievementChecked = true
            }
        }
    }

    /**
     * Save to history immediately, bypassing the time threshold.
     * @param reason Short description for logging
     */
    fun saveToHistoryNow(reason: String?) {
        if (!isHistorySaved) {
            log.d("[HISTORY] Immediate save triggered by: %s", reason)
            saveToHistory()
            isHistorySaved = true
        } else {
            updateHintTrackingInHistory()
        }
    }

    /** Update hint tracking in the existing history entry for the current map. */
    private fun updateHintTrackingInHistory() {
        try {
            val gameState = _currentState.value ?: return

            val mapSig = gameState.generateMapSignature()
            if (mapSig.isNullOrEmpty()) return

            val allEntries = GameHistoryManager.getHistoryEntries(storage)
            var existing: GameHistoryEntry? = null
            for (e in allEntries) {
                if (mapSig == e.mapSignature) {
                    existing = e
                    break
                }
            }

            if (existing != null) {
                val maxHint = gameState.maxHintUsedThisSession

                if (!existing.hasUsedHints() && maxHint >= 0) {
                    existing.recordHintUsed(maxHint)
                }

                // Update move count if game is completed (once per session)
                if (_isGameComplete.value && !isCompletionRecorded) {
                    val actualMoveCount = gameState.moveCount

                    val optMoves =
                        if (currentSolution != null) currentSolution!!.moves.size else 0
                    if (optMoves > 0) {
                        existing.optimalMoves = optMoves
                    }

                    val levelIdForStars = gameState.levelId
                    val currentAttemptStars: Int
                    if (levelIdForStars > 0) {
                        var stars = calculateStars(actualMoveCount, optMoves, hintsShown)
                        if (stars < 1 && levelIdForStars <= Constants.MIN_STAR_GUARANTEE_LEVEL) {
                            stars = 1
                        }
                        currentAttemptStars = stars
                    } else {
                        currentAttemptStars = existing.starsEarned
                    }
                    existing.recordCompletion(
                        ((TimeProvider.currentTimeMillis() - gameStartTime) / 1000).toInt(),
                        actualMoveCount,
                        currentAttemptStars
                    )
                    isCompletionRecorded = true

                    // If completed without hints, record the no-hints timestamp
                    if (maxHint < 0 && actualMoveCount > 0) {
                        val isOptimal = optMoves > 0 && actualMoveCount == optMoves
                        existing.recordSolvedWithoutHints(isOptimal)
                    }
                }

                if (maxHint >= 0) {
                    existing.markEverUsedHints()
                }

                GameHistoryManager.saveHistoryIndex(storage, allEntries)
            }
        } catch (e: Exception) {
            log.e("[HISTORY] Error updating hint tracking: %s", e.message)
        }
    }

    /** Save the current game state to history. */
    private fun saveToHistory() {
        try {
            val gameState = _currentState.value
            if (gameState == null) {
                log.e("[HISTORY] Cannot save to history: no current game state")
                return
            }

            GameHistoryManager.initialize(storage)

            val historyIndex = GameHistoryManager.getNextHistoryIndex(storage)
            val historyFileName = "history_" + historyIndex + ".txt"
            val historyPath = historyFileName

            var mapName: String? = gameState.levelName

            if (mapName == null || mapName.isEmpty() || "XXXXX" == mapName) {
                val levelId = gameState.levelId
                mapName = if (levelId > 0) {
                    "Level " + levelId
                } else {
                    "Random Map #" + historyIndex
                }
            }

            val saveData = gameState.serialize()
            storage.writeFile(historyPath, saveData)

            val boardWidth = gameState.width
            val boardHeight = gameState.height
            val boardSize =
                if (boardWidth > 0 && boardHeight > 0) boardWidth.toString() + "x" + boardHeight else ""

            val previewImagePath = historyFileName + "_preview.txt"

            var optimalMovesCount = 0
            val currentSolution = this.currentSolution
            if (currentSolution != null) {
                optimalMovesCount = currentSolution.moves.size
            }

            // Only save actual move count if game is completed
            val actualMoveCount = if (_isGameComplete.value) gameState.moveCount else 0
            val entry = GameHistoryEntry(
                historyPath,
                mapName,
                gameStartTime,
                totalPlayTime,
                actualMoveCount,
                optimalMovesCount,
                boardSize,
                previewImagePath
            )

            entry.difficulty = this.effectiveDifficulty

            val wallSig = gameState.generateWallSignature()
            val posSig = gameState.generatePositionSignature()
            val mapSig = gameState.generateMapSignature()
            entry.wallSignature = wallSig
            entry.positionSignature = posSig
            entry.mapSignature = mapSig

            // Stars earned for THIS completion (level games only)
            val levelId = gameState.levelId
            if (levelId > 0 && _isGameComplete.value) {
                val optMovesForStars =
                    if (currentSolution != null) currentSolution.moves.size else 0
                var currentAttemptStars =
                    calculateStars(actualMoveCount, optMovesForStars, hintsShown)
                if (currentAttemptStars < 1 && levelId <= Constants.MIN_STAR_GUARANTEE_LEVEL) {
                    currentAttemptStars = 1
                }
                entry.starsEarned = currentAttemptStars
            }

            // Hint tracking
            val maxHintUsed = gameState.maxHintUsedThisSession
            entry.maxHintUsed = maxHintUsed
            val noHintsThisSession = maxHintUsed < 0
            entry.setSolvedWithoutHints(noHintsThisSession)
            if (maxHintUsed >= 0) {
                entry.markEverUsedHints()
            }
            if (noHintsThisSession && actualMoveCount > 0) {
                val optMoves =
                    if (currentSolution != null) currentSolution.moves.size else 0
                val isOptimal = optMoves > 0 && actualMoveCount == optMoves
                entry.recordSolvedWithoutHints(isOptimal)
            }

            GameHistoryManager.addHistoryEntry(storage, entry)

            if (actualMoveCount > 0) {
                isCompletionRecorded = true
            }

            log.d("[HISTORY] Game saved to history: %s (Map name: '%s')", historyPath, mapName)
        } catch (e: Exception) {
            log.e("[HISTORY] Error saving game to history: %s", e.message)
        }
    }

    // ── Solver ────────────────────────────────────────────────────────────

    /** Initialize the solver with grid elements (replaces SolverManager.initialize). */
    fun initializeSolver(gridElements: ArrayList<GridElement?>) {
        val newSolver = SolverDD()
        newSolver.init(ArrayList(gridElements.filterNotNull()))
        solver = newSolver
        solverInitialized = true
    }

    /** Reset solver initialization state (replaces SolverManager.resetInitialization). */
    fun resetSolverInitialization() {
        solverInitialized = false
        solver = null
        solverSolutionList = null
    }

    /** Set a predefined solution so the solver does not need to run. */
    fun setPredefinedSolution(solution: String?, numMoves: Int) {
        predefinedSolution = solution
        predefinedNumMoves = numMoves
    }

    fun hasPredefinedSolution(): Boolean {
        return !predefinedSolution.isNullOrEmpty()
    }

    /** True if the solver reports a one-move solution. */
    fun isSolution01(): Boolean {
        return solver?.isSolution01() == true
    }

    /** Number of solutions found by the solver. */
    fun getSolverSolutionCount(): Int {
        return solverSolutionList?.size ?: 0
    }

    /** Get the i-th solver solution converted to a GameSolution. */
    fun getSolverSolution(index: Int): GameSolution? {
        return solver?.getSolution(index)
    }

    /**
     * Asynchronously calculates the solution for the current game state.
     * @param callback The callback to receive the solution when it's ready
     */
    fun calculateSolutionAsync(callback: SolutionCallback?) {
        if (_isSolverRunning.value) {
            log.d("[SOLUTION_SOLVER][calculateSolutionAsync] Solver already running, ignoring duplicate request")
            return
        }

        solverRestartCount++
        log.d("[SOLUTION_SOLVER][calculateSolutionAsync] Solver restart count: %d", solverRestartCount)

        this.solutionCallback = callback

        val state = _currentState.value
        if (state == null) {
            log.d("[SOLUTION_SOLVER][calculateSolutionAsync] Current state is null")
            onSolutionCalculationFailed("No game state available")
            return
        }

        val elements = state.gridElements
        log.d("[SOLUTION_SOLVER][calculateSolutionAsync] Current map has %d elements", elements.size)

        _isSolverRunning.value = true

        onSolutionCalculationStarted()

        try {
            // Cancel any previous solver task
            if (solverJob?.isActive == true) {
                log.d("[SOLUTION_SOLVER] Cancelling previous solver task before starting new one")
                solver?.cancel()
                solverJob?.cancel()
            }

            val capturedElements = ArrayList<GridElement?>(elements)

            solverJob = scope.launch(Dispatchers.Default) {
                try {
                    // Memory gate: wait for GC to reclaim previous solver's memory
                    val memInfo = TimeProvider.getRuntimeMemoryInfo()
                    val minFreeBytes = memInfo.maxMemory / 10 // 10% of heap must be free
                    for (attempt in 0..4) {
                        val info = TimeProvider.getRuntimeMemoryInfo()
                        val freeBytes = info.maxMemory - info.totalMemory + info.freeMemory
                        if (freeBytes >= minFreeBytes) break
                        log.d(
                            "[SOLUTION_SOLVER] Memory gate: waiting for GC, free=%dMB need=%dMB attempt=%d",
                            freeBytes shr 20, minFreeBytes shr 20, attempt
                        )
                        requestGc()
                        delay(200)
                    }
                    log.d("[SOLUTION_SOLVER][calculateSolutionAsync] Initializing and running solver on background thread")
                    initializeSolver(capturedElements)
                    runSolver()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e, "[SOLUTION_SOLVER] Error running solver")
                    scope.launch {
                        onSolutionCalculationFailed("Error: " + e.message)
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e, "[SOLUTION_SOLVER] Error initializing solver")
            onSolutionCalculationFailed("Error: " + e.message)
        }
    }

    /**
     * Execute the solver and dispatch the result to the listener path.
     * Runs inside the solver coroutine (Dispatchers.Default).
     */
    private fun runSolver() {
        if (!solverInitialized) {
            log.w("[SOLUTION_SOLVER] Solver not initialized, cannot run")
            scope.launch { onSolverCancelled() }
            return
        }

        try {
            // Check if we have a predefined solution - use it instead of running the solver
            if (hasPredefinedSolution()) {
                val parsed = parsePredefinedSolution(predefinedSolution)
                if (parsed != null) {
                    val moveCount = parsed.moves.size
                    solverSolutionList = null
                    currentSolverSolution = parsed
                    scope.launch { onSolverFinished(true, moveCount, 1) }
                    return
                } else {
                    log.w("[SOLUTION_SOLVER] Failed to parse predefined solution, falling back to solver")
                }
            }

            val s = solver
            if (s == null) {
                scope.launch { onSolverCancelled() }
                return
            }
            s.run()

            val status = s.getSolverStatus()
            if (status != null && status.isFinished) {
                val numSolutions = s.getSolutionList()?.size ?: 0
                solverSolutionList = s.getSolutionList()

                if (numSolutions > 0) {
                    val solution = s.getSolution(0)
                    currentSolverSolution = solution
                    val moveCount = solution?.moves?.size ?: 0
                    scope.launch { onSolverFinished(true, moveCount, numSolutions) }
                } else {
                    scope.launch { onSolverFinished(false, 0, 0) }
                }
            } else {
                scope.launch { onSolverCancelled() }
            }
        } catch (e: Exception) {
            log.e(e, "[SOLUTION_SOLVER] Exception in solver processing: %s", e.message)
            scope.launch { onSolverCancelled() }
        }
    }

    /** The solution returned by the last finished solver run. */
    private var currentSolverSolution: GameSolution? = null

    /**
     * Parse a predefined solution string into a GameSolution.
     * Format: "gE gN gE gS ..." — [robot color letter][direction letter].
     * Faithful port of SolverManager.parsePredefinedSolution.
     */
    private fun parsePredefinedSolution(solutionStr: String?): GameSolution? {
        if (solutionStr.isNullOrEmpty()) {
            return null
        }

        val solution = GameSolution()
        val moves = solutionStr.trim().split("\\s+".toRegex())

        for (move in moves) {
            if (move.length != 2) {
                log.w("[SOLUTION_SOLVER] Invalid move format: %s", move)
                continue
            }

            val robotChar = move[0]
            val dirChar = move[1]

            val robotColor: Int = when (robotChar) {
                'r' -> 0
                'g' -> 1
                'b' -> 2
                'y' -> 3
                else -> {
                    log.w("[SOLUTION_SOLVER] Unknown robot color: %c", robotChar)
                    continue
                }
            }

            val direction: ERRGameMove = when (dirChar) {
                'N' -> ERRGameMove.UP
                'S' -> ERRGameMove.DOWN
                'E' -> ERRGameMove.RIGHT
                'W' -> ERRGameMove.LEFT
                else -> {
                    log.w("[SOLUTION_SOLVER] Unknown direction: %c", dirChar)
                    continue
                }
            }

            val piece = RRPiece(0, 0, robotColor, robotColor)
            solution.addMove(RRGameMove(piece, direction))
        }

        return solution
    }

    /** Called when the solution calculation starts. */
    private fun onSolutionCalculationStarted() {
        log.d("[SOLUTION_SOLVER] onSolutionCalculationStarted")
        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null

        solutionCallback?.onSolutionCalculationStarted()
    }

    /**
     * Called when the solution calculation completes successfully.
     * Stores the solution, runs map-regeneration validation and notifies the callback.
     */
    private fun onSolutionCalculationCompleted(solution: GameSolution?) {
        val state = _currentState.value
        val isLevelMode = (state != null && state.levelId > 0)

        val moveCount = if (solution != null) solution.moves.size else 0
        if (moveCount > 0) {
            val minRequiredMoves = this.minimumRequiredMoves
            val maxRequiredMoves = this.maximumRequiredMoves

            // Skip validation for level games, loaded savegames, or when regeneration is disabled
            if (!isLevelMode && !isLoadedFromSave && allowRegeneration && regenerationCount < MAX_AUTO_REGENERATIONS) {
                val isTooEasy = moveCount < minRequiredMoves
                val isTooHard = moveCount > maxRequiredMoves

                if (keepCurrentMapDespiteDifficulty && (isTooEasy || isTooHard)) {
                    log.d(
                        "[SOLUTION_SOLVER][MOVES][KEEP_MAP_ENFORCER] Solution has only %d moves, but current map was manually kept",
                        moveCount
                    )
                } else {
                    if (isTooEasy || isTooHard) {
                        log.d(
                            "[MAP_VALIDATION][DISCARD] Map discarded: %s (%d moves), regenerating (attempt %d/%d)",
                            if (isTooEasy) "too easy" else "too hard",
                            moveCount,
                            regenerationCount + 1,
                            MAX_AUTO_REGENERATIONS
                        )
                        regenerationCount++

                        resetSolverInitialization()
                        solver?.cancel()

                        scope.launch {
                            delay(100)
                            createValidGame(
                                Preferences.boardSizeWidth, Preferences.boardSizeHeight
                            )
                        }
                        return
                    }
                }
            } else if (regenerationCount >= MAX_AUTO_REGENERATIONS) {
                log.d(
                    "[MAP_VALIDATION][ACCEPT] Map accepted: reached maximum regeneration attempts (%d)",
                    MAX_AUTO_REGENERATIONS
                )
                onMapFallbackAccepted?.invoke(MAX_AUTO_REGENERATIONS, moveCount)
                regenerationCount = 0
            } else if (!allowRegeneration) {
                log.d("[MAP_VALIDATION][ACCEPT] Map accepted: regeneration disabled")
            }
        } else {
            // moveCount==0: solver hit memory/depth limit or puzzle is unsolvable
            if (!isLevelMode && !isLoadedFromSave && allowRegeneration && regenerationCount < MAX_AUTO_REGENERATIONS) {
                log.d(
                    "[MAP_VALIDATION][DISCARD] Map discarded: no solution found, trying new map (regen %d/%d)",
                    regenerationCount + 1, MAX_AUTO_REGENERATIONS
                )
                regenerationCount++
                resetSolverInitialization()
                solver?.cancel()
                scope.launch {
                    delay(100)
                    createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight)
                }
                return
            }
            log.w("[SOLUTION_SOLVER][MOVES] onSolutionCalculationCompleted: No solution found, accepting puzzle")
        }

        // Store the solution for later use with getHint()
        currentSolution = solution
        currentSolutionStep = 0
        updatePreCompRobotOrder(solution)

        _isSolverRunning.value = false

        solutionWasAccepted = true

        regenerationCount = 0

        if (solutionCallback != null) {
            solutionCallback!!.onSolutionCalculationCompleted(solution)
            solutionCallback = null
        }

        lastSolutionMinMoves =
            if (solution != null) solution.moves.size else 0
    }

    /** Called when the solution calculation fails. */
    private fun onSolutionCalculationFailed(errorMessage: String?) {
        log.d("[SOLUTION_SOLVER] onSolutionCalculationFailed: %s", errorMessage)

        currentSolution = null
        currentSolutionStep = 0
        loadedSolutions = null

        _isSolverRunning.value = false

        if (solutionCallback != null) {
            solutionCallback!!.onSolutionCalculationFailed(errorMessage)
            solutionCallback = null
        }
    }

    /**
     * SolverManager.SolverListener equivalent: called when the solver finishes.
     */
    fun onSolverFinished(success: Boolean, solutionMoves: Int, numSolutions: Int) {
        log.d(
            "[SOLUTION_SOLVER][DIAGNOSTIC] onSolverFinished called: success=%b, moves=%d, solutions=%d",
            success, solutionMoves, numSolutions
        )

        if (success && numSolutions > 0) {
            try {
                val solution = currentSolverSolution
                if (solution != null) {
                    onSolutionCalculationCompleted(solution)
                } else {
                    onSolutionCalculationFailed("No valid solution found")
                }
            } catch (e: Exception) {
                log.e(e, "[SOLUTION_SOLVER][DIAGNOSTIC] Error getting solution: %s", e.message)
                onSolutionCalculationFailed("Error: " + e.message)
            }
        } else {
            onSolutionCalculationFailed("No solution found")
        }
    }

    /**
     * SolverManager.SolverListener equivalent: called when the solver is cancelled.
     */
    fun onSolverCancelled() {
        log.d("[SOLUTION_SOLVER][DIAGNOSTIC] onSolverCancelled called")
        onSolutionCalculationFailed("Solver was cancelled")
    }

    /** Cancel any running solver operation. */
    fun cancelSolver() {
        log.d("[SOLUTION_SOLVER] cancelSolver called")
        solver?.cancel()
        solverJob?.cancel()
        _isSolverRunning.value = false
        onSolutionCalculationFailed("Solver was cancelled")
    }

    /** Stop all map regeneration (e.g., when user leaves game screen). */
    fun stopRegeneration() {
        allowRegeneration = false
        log.d("[SOLUTION_SOLVER] Map regeneration disabled")
    }

    /** Resume map regeneration (e.g., when user enters game screen). */
    fun resumeRegeneration() {
        allowRegeneration = true
        log.d("[SOLUTION_SOLVER] Map regeneration enabled")
    }

    /** Keep the current map even if it does not satisfy the active difficulty limits. */
    fun keepCurrentMapDespiteDifficulty() {
        keepCurrentMapDespiteDifficulty = true
        log.d("[DIFFICULTY_ENFORCER] Current map will be kept despite difficulty limits")
    }

    fun incrementSolutionStep() {
        currentSolutionStep++
    }

    fun resetSolutionStep() {
        currentSolutionStep = 0
    }

    /** Callback interface for solution calculation. */
    interface SolutionCallback {
        fun onSolutionCalculationStarted()
        fun onSolutionCalculationCompleted(solution: GameSolution?)
        fun onSolutionCalculationFailed(errorMessage: String?)
    }

    /** Reset statistics for a new game. */
    private fun resetStatistics() {
        hintsShown = 0
        robotsUsed.clear()
        startTime = TimeProvider.currentTimeMillis()
        _moveCount.value = 0
        _squaresMoved.value = 0
        _isGameComplete.value = false
    }

    private val minimumRequiredMoves: Int
        get() = Preferences.minSolutionMoves

    private val maximumRequiredMoves: Int
        get() = Preferences.maxSolutionMoves

    /**
     * Creates a valid game with at least the minimum required moves.
     * @param width  Width of the board
     * @param height Height of the board
     */
    private fun createValidGame(width: Int, height: Int) {
        log.d("createValidGame() called")

        if (keepCurrentMapDespiteDifficulty) {
            log.d("[KEEP_MAP_ENFORCER] createValidGame() blocked - user chose to keep current map")
            return
        }

        val wallStorage = getInstance()
        wallStorage.updateCurrentBoardSize()

        val newState = createRandom()

        emitState(newState)
        _gameCounter.value = _gameCounter.value + 1
        _moveCount.value = 0
        _isGameComplete.value = false

        if (!Preferences.generateNewMapEachTime) {
            wallStorage.storeWalls(newState.gridElements.filterNotNull().toMutableList())
        }

        stateHistory.clear()
        squaresMovedHistory.clear()
        clearNextMovesCache()

        val gridElements = newState.gridElements
        resetSolverInitialization()
        initializeSolver(gridElements)

        // Quick check for trivial puzzles before starting expensive solver
        if (validateDifficulty && isTrivialPuzzle(newState)) {
            log.d("[TRIVIAL_CHECK] Detected trivial puzzle, regenerating without running solver")
            createValidGame(width, height)
            return
        }

        startTime = TimeProvider.currentTimeMillis()

        if (validateDifficulty) {
            calculateSolutionAsync(DifficultyValidationCallback(width, height))
        } else {
            validateDifficulty = true
            calculateSolutionAsync(null)
        }
    }

    /**
     * Quick check if puzzle is trivial (already solved or only 1 move needed).
     * Prevents expensive solver runs for very simple puzzles.
     */
    private fun isTrivialPuzzle(state: GameState): Boolean {
        if (state.isComplete) {
            log.d("[TRIVIAL_CHECK] Puzzle is already solved")
            return true
        }

        // Check if any robot can reach its target in one move
        for (robot in state.gameElements) {
            if (robot.type != GameElement.TYPE_ROBOT) continue

            val robotX = robot.x
            val robotY = robot.y
            val robotColor = robot.color

            for (target in state.gameElements) {
                if (target.type != GameElement.TYPE_TARGET) continue
                if (target.color != robotColor) continue

                val targetX = target.x
                val targetY = target.y

                if (robotX == targetX || robotY == targetY) {
                    var pathClear = true

                    if (robotX == targetX) {
                        val startY = min(robotY, targetY)
                        val endY = max(robotY, targetY)
                        for (y in startY..endY) {
                            if (y > startY && state.getCellType(
                                    robotX, y
                                ) == Constants.TYPE_HORIZONTAL_WALL
                            ) {
                                pathClear = false
                                break
                            }
                        }
                    } else {
                        val startX = min(robotX, targetX)
                        val endX = max(robotX, targetX)
                        for (x in startX..endX) {
                            if (x > startX && state.getCellType(
                                    x, robotY
                                ) == Constants.TYPE_VERTICAL_WALL
                            ) {
                                pathClear = false
                                break
                            }
                        }
                    }

                    if (pathClear) {
                        log.d(
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

    /** Callback to validate puzzle difficulty and regenerate if needed. */
    private inner class DifficultyValidationCallback(
        private val width: Int,
        private val height: Int
    ) : SolutionCallback {
        private var attemptCount = 0

        override fun onSolutionCalculationStarted() {
            log.d("DifficultyValidationCallback: Calculation started, attempt %d", attemptCount + 1)
        }

        override fun onSolutionCalculationCompleted(solution: GameSolution?) {
            attemptCount++
            val moveCount = if (solution != null) solution.moves.size else 0
            val requiredMoves: Int = this@GameSession.minimumRequiredMoves
            val maxMoves: Int = this@GameSession.maximumRequiredMoves

            if (keepCurrentMapDespiteDifficulty) {
                log.d(
                    "[DifficultyValidationCallback][KEEP_MAP_ENFORCER] Skipping difficulty validation - map was manually kept (moves=%d)",
                    moveCount
                )
            } else {
                if (this@GameSession.isSolution01()) {
                    log.d("[DifficultyValidationCallback]: Puzzle too easy (1 move), generating new one")
                    createValidGame(width, height)
                    return
                }

                if (moveCount == 0 && attemptCount < MAX_ATTEMPTS) {
                    log.d(
                        "[DifficultyValidationCallback]: Solver found no solution, trying new map (attempt %d/%d)",
                        attemptCount, MAX_ATTEMPTS
                    )
                    createValidGame(width, height)
                    return
                } else if (moveCount == 0) {
                    log.w(
                        "[DifficultyValidationCallback]: No solution after %d attempts, accepting puzzle",
                        attemptCount
                    )
                    onMapFallbackAccepted?.invoke(attemptCount, null)
                    validateDifficulty = true
                    solutionWasAccepted = true
                    _isSolverRunning.value = false
                    return
                }

                if (moveCount < requiredMoves && attemptCount < MAX_ATTEMPTS) {
                    log.d(
                        "[MAP_VALIDATION][DISCARD] Map discarded: too easy (%d moves < %d required), generating new one (attempt %d/%d)",
                        moveCount, requiredMoves, attemptCount, MAX_ATTEMPTS
                    )
                    createValidGame(width, height)
                    return
                } else if (moveCount > maxMoves && attemptCount < MAX_ATTEMPTS) {
                    log.d(
                        "[MAP_VALIDATION][DISCARD] Map discarded: too hard (%d moves > %d max), generating new one (attempt %d/%d)",
                        moveCount, maxMoves, attemptCount, MAX_ATTEMPTS
                    )
                    createValidGame(width, height)
                    return
                }
            }

            // Accepted despite violating difficulty limits (attempts exhausted)
            if (!keepCurrentMapDespiteDifficulty && attemptCount >= MAX_ATTEMPTS &&
                (moveCount < requiredMoves || moveCount > maxMoves)
            ) {
                onMapFallbackAccepted?.invoke(attemptCount, moveCount)
            }

            log.d(
                "[DifficultyValidationCallback] Accepted puzzle with %d moves after %d attempts",
                moveCount, attemptCount
            )
            validateDifficulty = true
            currentSolution = solution
            currentSolutionStep = 0
            updatePreCompRobotOrder(solution)

            solutionWasAccepted = true

            _isSolverRunning.value = false
        }

        override fun onSolutionCalculationFailed(errorMessage: String?) {
            log.w("DifficultyValidationCallback: Solution calculation failed: %s", errorMessage)
            validateDifficulty = true
            _isSolverRunning.value = false
        }
    }

    /**
     * Reset the game to its initial state (Soft Reset).
     * Preserves the current board, resets robot positions and counters.
     */
    fun resetGame() {
        isResetting = true

        val currentGameState = _currentState.value

        if (currentGameState != null) {
            currentGameState.resetRobotPositions()

            _moveCount.value = 0
            _squaresMoved.value = 0
            _isGameComplete.value = false

            for (element in currentGameState.gameElements) {
                if (element.isRobot) {
                    element.isSelected = false
                }
            }

            robotsUsed.clear()

            emitState(currentGameState)
        }

        isResetting = false
    }

    /** Reset all move counts and game history. */
    fun resetMoveCountsAndHistory() {
        log.d("[RESET_GAME] Resetting all move counts and game history")
        setMoveCount(0)
        resetSquaresMoved()
        setGameComplete(false)
        stateHistory.clear()
        squaresMovedHistory.clear()
        pathHistory.clear()
        clearNextMovesCache()
        isCompletionRecorded = false
    }

    fun resetSolverRestartCount() {
        solverRestartCount = 0
    }

    fun resetLastSolutionMinMoves() {
        lastSolutionMinMoves = 0
    }

    /** Reset the game timer (not the UI timer - call resetUiTimer for that). */
    private fun resetGameTimer() {
        gameStartTime = TimeProvider.currentTimeMillis()
        totalPlayTime = 0
        isHistorySaved = false
        isCompletionRecorded = false
        wrongRobotToastShownColors.clear()
    }

    /** Check if the wrong-robot-on-target toast was already shown for this color. */
    fun hasWrongRobotToastBeenShownFor(robotColor: Int): Boolean {
        return wrongRobotToastShownColors.contains(robotColor)
    }

    /** Mark the wrong-robot-on-target toast as shown for this robot color. */
    fun markWrongRobotToastShownFor(robotColor: Int) {
        wrongRobotToastShownColors.add(robotColor)
    }

    /**
     * Set the current game state.
     * Used by deep link functionality to load a state from external data.
     */
    fun setGameState(state: GameState?) {
        if (state == null) {
            log.e("[DEEPLINK] Cannot set null game state")
            return
        }

        log.d("[DEEPLINK] Setting game state from deep link")

        isLoadedFromSave = true

        resetSolverInitialization()
        cancelSolver()
        _isSolverRunning.value = false

        resetUiTimer()

        if (state.levelId == -1 && (state.levelName == null || state.levelName == "XXXXX")) {
            log.w(
                "[RANDOM_STATE_VALIDATION] setGameState called with a random state! State: levelId=%d, levelName=%s",
                state.levelId, state.levelName
            )
        }

        resetMoveCountsAndHistory()

        this.levelName = state.levelName

        state.setGameStateManager(this)

        emitState(state)
        _gameCounter.value = _gameCounter.value + 1

        _moveCount.value = state.moveCount

        resetGameTimer()
        startGameTimer()

        calculateSolutionAsync(null)
    }

    // ── Live move counter ─────────────────────────────────────────────────

    fun isLiveMoveCounterEnabled(): Boolean {
        return liveMoveCounterEnabled
    }

    fun setLiveMoveCounterEnabled(enabled: Boolean) {
        this.liveMoveCounterEnabled = enabled
        Preferences.setLiveMoveCounterEnabled(enabled)
        log.d("[LIVE_SOLVER] Live move counter %s (persisted)", if (enabled) "enabled" else "disabled")
        if (!enabled) {
            _liveMoveCounterText.value = ""
            _liveSolverCalculating.value = false
            liveSolver?.cancel()
            liveSolverJob?.cancel()
        }
    }

    /**
     * Trigger the live solver to calculate optimal moves from the current robot positions.
     * Called after each player move when the live move counter is enabled.
     */
    fun triggerLiveSolver() {
        if (!liveMoveCounterEnabled) return

        val state = _currentState.value ?: return

        if (_isGameComplete.value) {
            _liveMoveCounterText.value = ""
            _liveSolverCalculating.value = false
            return
        }

        _liveSolverCalculating.value = true

        if (lastSolutionMinMoves == 0 && currentSolution != null) {
            lastSolutionMinMoves = currentSolution!!.moves.size
        }

        // Check pre-computation cache first
        val stateHash = computeStateHash(state)
        val cachedResult = nextMovesCache[stateHash]
        if (cachedResult != null) {
            val currentMoves = _moveCount.value
            val optimal = lastSolutionMinMoves
            val deviation = if (optimal > 0) (currentMoves + cachedResult) - optimal else 0
            _liveMoveCounterDeviation.value = deviation
            _liveMoveCounterText.value = formatLiveCounterText(cachedResult, deviation)
            _liveSolverCalculating.value = false
            // Pre-compute next moves from this new position
            preComputeNextMoves(state, null)
            return
        }

        val gridElements = buildGridElements(state)

        // Cancel previous live solve and start a new one
        liveSolver?.cancel()
        liveSolverJob?.cancel()
        val newSolver = SolverDD()
        liveSolver = newSolver

        liveSolverJob = scope.launch(Dispatchers.Default) {
            try {
                newSolver.init(ArrayList(gridElements.filterNotNull()))
                newSolver.run()

                val status = newSolver.getSolverStatus()
                if (status != null && status.isFinished) {
                    val numSolutions = newSolver.getSolutionList()?.size ?: 0
                    if (numSolutions > 0) {
                        val solution = newSolver.getSolution(0)
                        var moves = solution?.moves?.size ?: 0
                        if (newSolver.isSolution01()) {
                            moves = 1
                        }
                        scope.launch {
                            _liveSolverCalculating.value = false
                            val currentMoves = _moveCount.value
                            val optimal = lastSolutionMinMoves
                            val deviation =
                                if (optimal > 0) (currentMoves + moves) - optimal else 0
                            _liveMoveCounterDeviation.value = deviation
                            _liveMoveCounterText.value =
                                formatLiveCounterText(moves, deviation)
                            nextMovesCache = nextMovesCache + (stateHash to moves)
                            preComputeNextMoves(state, solution)
                        }
                        return@launch
                    }
                }
                scope.launch {
                    _liveSolverCalculating.value = false
                    _liveMoveCounterText.value = "?"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e, "[LIVE_SOLVER] Error during live solve: %s", e.message)
                scope.launch {
                    _liveSolverCalculating.value = false
                    _liveMoveCounterText.value = "?"
                }
            }
        }
    }

    /** Format the live counter text: "{n} moves from here Δ+d" */
    private fun formatLiveCounterText(remainingMoves: Int, deviation: Int): String {
        val base = liveCounterFormatter?.invoke(remainingMoves)
            ?: stringProvider.getString("live_move_counter_optimal", remainingMoves)
            ?: "$remainingMoves moves from here"
        val deviationStr =
            if (deviation != 0) " Δ" + (if (deviation >= 0) "+" else "") + deviation else ""
        return base + deviationStr
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

    /** Robot colors ordered by solution priority (unique, in order of appearance). */
    private fun getSolutionRobotOrder(solution: GameSolution?): MutableList<Int?> {
        val order: MutableList<Int?> = ArrayList()
        if (solution != null) {
            val moves = solution.moves
            val startStep = if (solution === currentSolution) currentSolutionStep else 0
            for (i in startStep..<moves.size) {
                val move = moves[i]
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

    /** Update preCompRobotOrder from a solution's move list. */
    private fun updatePreCompRobotOrder(solution: GameSolution?) {
        preCompRobotOrder.clear()
        if (solution != null) {
            for (move in solution.moves) {
                if (move is RRGameMove) {
                    val color = move.color
                    if (!preCompRobotOrder.contains(color)) {
                        preCompRobotOrder.add(color)
                    }
                }
            }
        }
    }

    /**
     * Pre-compute optimal moves for all possible next states (robots × 4 directions).
     * Runs sequentially on a background coroutine; checks preComputeCancelled
     * before each solve so a robot move can abort the batch.
     */
    private fun preComputeNextMoves(state: GameState, liveSolution: GameSolution?) {
        if (!liveMoveCounterEnabled) return
        if (preComputeRunning) {
            log.d("[PRECOMP_SOLUTION] Skipping — previous pre-computation still running")
            return
        }

        val robots: MutableList<GameElement> = ArrayList()
        val nonRobots: MutableList<GameElement> = ArrayList()
        for (element in state.gameElements) {
            if (element.type == GameElement.TYPE_ROBOT) {
                robots.add(element)
            } else {
                nonRobots.add(element)
            }
        }

        // Sort robots: solution-next robots first
        val priorityColors: MutableList<Int?>
        if (preCompRobotOrder.isNotEmpty()) {
            priorityColors = ArrayList(preCompRobotOrder)
        } else if (liveSolution != null) {
            priorityColors = getSolutionRobotOrder(liveSolution)
        } else {
            priorityColors = ArrayList()
        }
        if (priorityColors.isNotEmpty()) {
            robots.sortWith { a, b ->
                val idxA = priorityColors.indexOf(a.color)
                val idxB = priorityColors.indexOf(b.color)
                when {
                    idxA >= 0 && idxB >= 0 -> idxA - idxB
                    idxA >= 0 -> -1
                    idxB >= 0 -> 1
                    else -> 0
                }
            }
        }

        val width = state.width
        val height = state.height
        val directions = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)
        )
        val dirNames = arrayOf("E", "W", "S", "N")

        preComputeRunning = true
        preComputeCancelled = false
        log.d(
            "[PRECOMP_SOLUTION] Starting sequential pre-computation for %d robots × 4 directions",
            robots.size
        )

        preComputeJob = scope.launch(Dispatchers.Default) {
            try {
                var computed = 0
                var skipped = 0
                outer@ for (robot in robots) {
                    for (d in 0..3) {
                        if (preComputeCancelled) {
                            log.d(
                                "[PRECOMP_SOLUTION] Cancelled after %d computed, %d skipped",
                                computed, skipped
                            )
                            break@outer
                        }

                        val dx = directions[d][0]
                        val dy = directions[d][1]

                        // Simulate the slide
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

                        if (newX == robot.x && newY == robot.y) {
                            skipped++
                            continue
                        }

                        val sb = StringBuilder()
                        for (r in robots) {
                            sb.append(robotColorShort(r.color)).append(':')
                            if (r === robot) {
                                sb.append(newX).append(',').append(newY)
                            } else {
                                sb.append(r.x).append(',').append(r.y)
                            }
                            sb.append(';')
                        }
                        val hypotheticalHash = sb.toString()

                        if (nextMovesCache.containsKey(hypotheticalHash)) {
                            skipped++
                            continue
                        }

                        val gridElements = ArrayList<GridElement?>()
                        for (r in robots) {
                            val rType = "robot_" + getColorName(r.color, false)
                            if (r === robot) {
                                gridElements.add(GridElement(newX, newY, rType))
                            } else {
                                gridElements.add(GridElement(r.x, r.y, rType))
                            }
                        }
                        for (nr in nonRobots) {
                            var ge: GridElement? = null
                            when (nr.type) {
                                GameElement.TYPE_TARGET -> ge = GridElement(
                                    nr.x, nr.y,
                                    "target_" + getColorName(nr.color, false)
                                )

                                GameElement.TYPE_HORIZONTAL_WALL -> ge =
                                    GridElement(nr.x, nr.y, "mh")

                                GameElement.TYPE_VERTICAL_WALL -> ge =
                                    GridElement(nr.x, nr.y, "mv")
                            }
                            if (ge != null) gridElements.add(ge)
                        }

                        // Solve with timeout using cooperative cancellation
                        val preCompSolver = SolverDD()
                        preCompSolver.init(ArrayList(gridElements.filterNotNull()))
                        val solveJob = async(Dispatchers.Default) { preCompSolver.run() }
                        val completed = withTimeoutOrNull(
                            Constants.PRECOMP_SOLVER_TIMEOUT_SECONDS * 1000L
                        ) {
                            solveJob.join()
                            true
                        }
                        if (completed == null) {
                            preCompSolver.cancel()
                            solveJob.join()
                        }

                        if (preComputeCancelled) {
                            log.d("[PRECOMP_SOLUTION] Cancelled after solve, %d computed so far", computed)
                            break@outer
                        }

                        val status = preCompSolver.getSolverStatus()
                        if (status != null && status.isFinished) {
                            val numSolutions = preCompSolver.getSolutionList()?.size ?: 0
                            if (numSolutions > 0) {
                                val solution = preCompSolver.getSolution(0)
                                var moves = solution?.moves?.size ?: 0
                                if (preCompSolver.isSolution01()) moves = 1
                                nextMovesCache = nextMovesCache + (hypotheticalHash to moves)
                                computed++
                            }
                        }
                    }
                }
                log.d(
                    "[PRECOMP_SOLUTION] Finished: %d computed, %d skipped, cache size: %d",
                    computed, skipped, nextMovesCache.size
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e, "[PRECOMP_SOLUTION] Error during pre-computation")
            } finally {
                preComputeRunning = false
            }
        }
    }

    /** Cancel any running pre-computation (called when a robot move starts). */
    private fun cancelPreComputation() {
        if (preComputeRunning) {
            preComputeCancelled = true
            preComputeJob?.cancel()
            preComputeRunning = false
        }
    }

    /** Clear the pre-computation cache (call on new game / reset). */
    fun clearNextMovesCache() {
        cancelPreComputation()
        nextMovesCache = emptyMap()
    }

    // ── Autosave metadata ─────────────────────────────────────────────────

    /** Save autosave metadata so the Play button can check if settings still match. */
    private fun saveAutosaveMetadata(state: GameState) {
        var targets = 0
        for (el in state.gameElements) {
            if (el.type == GameElement.TYPE_TARGET) targets++
        }
        storage.putInt(AUTOSAVE_META_BOARD_W, state.width)
        storage.putInt(AUTOSAVE_META_BOARD_H, state.height)
        storage.putInt(AUTOSAVE_META_TARGET_COUNT, targets)
        log.d("[AUTOSAVE_META] Saved metadata: %dx%d, %d targets", state.width, state.height, targets)
    }

    /**
     * Check if current settings match the autosave metadata.
     * @return true if settings match (autosave can be resumed)
     */
    fun autosaveSettingsMatch(): Boolean {
        val savedW = storage.getInt(AUTOSAVE_META_BOARD_W, -1)
        val savedH = storage.getInt(AUTOSAVE_META_BOARD_H, -1)
        val savedTargets = storage.getInt(AUTOSAVE_META_TARGET_COUNT, -1)

        if (savedW == -1) {
            return false
        }

        return savedW == Preferences.boardSizeWidth && savedH == Preferences.boardSizeHeight &&
                savedTargets == Preferences.targetColors
    }

    /** Clear autosave metadata (called when autosave is deleted). */
    fun clearAutosaveMetadata() {
        storage.remove(AUTOSAVE_META_BOARD_W)
        storage.remove(AUTOSAVE_META_BOARD_H)
        storage.remove(AUTOSAVE_META_TARGET_COUNT)
        log.d("[AUTOSAVE_META] Metadata cleared")
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    /** Cancel all running solver work (replaces Android ViewModel.onCleared). */
    fun cleanup() {
        liveSolver?.cancel()
        liveSolverJob?.cancel()
        solver?.cancel()
        solverJob?.cancel()
        preComputeCancelled = true
        preComputeJob?.cancel()
    }

    // ── Companion ─────────────────────────────────────────────────────────

    companion object {
        /** Touch action for release (matches MotionEvent.ACTION_UP). */
        const val ACTION_UP: Int = 1

        private const val MAX_ATTEMPTS = 999
        private const val MAX_AUTO_REGENERATIONS = 999

        /** Move cooldown to prevent multiple moves within a short window. */
        private const val MOVE_COOLDOWN_MS: Long = 400

        /** Seconds of play before the game is saved to history. */
        private const val HISTORY_SAVE_THRESHOLD = 30

        /** Stars needed to unlock the level editor. */
        private const val LEVEL_EDITOR_UNLOCK_STARS = 140

        /** Autosave metadata keys stored in PlatformStorage. */
        private const val AUTOSAVE_META_BOARD_W = "autosave_board_w"
        private const val AUTOSAVE_META_BOARD_H = "autosave_board_h"
        private const val AUTOSAVE_META_TARGET_COUNT = "autosave_target_count"

        /**
         * Extract metadata from save data.
         * Delegates to GameState.extractMetadataFromSaveData (DRY).
         */
        @JvmStatic
        fun extractMetadataFromSaveData(saveData: String?): MutableMap<String?, String?> {
            return GameState.extractMetadataFromSaveData(saveData)
        }

        /** Short color letter for log/hash: r=red/pink, g=green, b=blue, y=yellow. */
        private fun robotColorShort(colorId: Int): String {
            when (colorId) {
                Constants.COLOR_PINK -> return "r"
                Constants.COLOR_GREEN -> return "g"
                Constants.COLOR_BLUE -> return "b"
                Constants.COLOR_YELLOW -> return "y"
                else -> return colorId.toString()
            }
        }
    }
}
