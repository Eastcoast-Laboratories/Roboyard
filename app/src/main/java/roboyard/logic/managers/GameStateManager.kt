package roboyard.logic.managers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.ToggleButton
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation.findNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import roboyard.eclabs.R
import roboyard.logic.achievements.AchievementManagerFactory
import roboyard.logic.core.GameElement
import roboyard.logic.core.GameSolution
import roboyard.logic.core.GameState
import roboyard.logic.core.GameState.Companion.createRandom
import roboyard.logic.core.IGameMove
import roboyard.logic.managers.SyncManager.HistoryUploadCallback
import roboyard.logic.network.ApiClientProvider
import roboyard.logic.ui.UiNotifier
import roboyard.platform.AndroidStorage
import roboyard.ui.RoboyardApplication
import roboyard.ui.animation.RobotAnimationManager
import roboyard.ui.components.GameGridView
import roboyard.ui.util.SolutionAnimator
import roboyard.ui.util.SolverManager
import roboyard.ui.util.SolverManager.SolverListener
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.w
import java.io.File
import java.lang.ref.WeakReference

/**
 * Central state manager for the game (Android façade).
 *
 * All platform-independent game logic lives in the shared [GameSession]
 * (movement, undo, solver orchestration, hints, save/load, history, timers,
 * difficulty validation, map regeneration, live move counter). This class only
 * keeps Android-specific concerns: LiveData exposure for fragments, View-based
 * robot animations via [RobotAnimationManager], [GameGridView] wiring,
 * navigation, achievements via [AchievementManagerFactory], history upload via
 * [SyncManager], and the legacy [SolverManager] singleton bridge.
 *
 * Public API is unchanged so all callers (fragments, tests) keep working.
 */
open class GameStateManager(application: Application) : AndroidViewModel(application),
    SolverListener {

    private val context: Context? = application.applicationContext

    /** Shared game session holding all platform-independent logic. */
    private val session = GameSession(AndroidStorage.getInstance(application), viewModelScope)

    // Robot animation manager (Android View animations)
    private val robotAnimationManager: RobotAnimationManager = RobotAnimationManager(this)

    // Animation settings - made slower for more visible effect (minDuration = 100f,)
    private val animationsEnabled = true

    /** Acceleration duration for animations in milliseconds */
    @JvmField
    val accelerationDuration: Float = 300f

    /** Maximum animation speed */
    @JvmField
    val maxSpeed: Float = 1500f

    /** Deceleration duration for animations in milliseconds */
    @JvmField
    val decelerationDuration: Float = 50f

    /** Animation frame delay in milliseconds (default Android is ~16ms = 60fps) */
    @JvmField
    val animationFrameDelay: Long = 25

    private var gameGridView: GameGridView? = null

    // Reference to the current activity - will be updated by setActivity()
    private var activityRef: WeakReference<Activity?>? = null

    // Hint button reset timer to avoid race condition when loading games from history
    private var hintButtonResetJob: Job? = null

    // Legacy singleton; only kept so cancelSolver() also stops a SolverManager job
    private val solverManager: SolverManager = SolverManager.getInstance()

    /**
     * LiveData mirror of the session's current state. StateFlow conflates
     * identical instances, so this mirrors [GameSession.stateRevision] instead:
     * every revision bump re-emits the current GameState, exactly like the old
     * MutableLiveData.setValue(sameInstance) behavior fragments rely on.
     */
    private val currentState = MutableLiveData<GameState?>()

    @Suppress("UNCHECKED_CAST")
    private val moveCountLiveData = session.moveCount.asLiveData() as LiveData<Int?>

    @Suppress("UNCHECKED_CAST")
    private val squaresMovedLiveData = session.squaresMoved.asLiveData() as LiveData<Int?>

    @Suppress("UNCHECKED_CAST")
    private val isGameCompleteLiveData = session.isGameComplete.asLiveData() as LiveData<Boolean?>

    @Suppress("UNCHECKED_CAST")
    private val isSolverRunningLiveData = session.isSolverRunning.asLiveData() as LiveData<Boolean?>

    @Suppress("UNCHECKED_CAST")
    private val wrongRobotAtTargetLiveData =
        session.wrongRobotAtTarget.asLiveData() as LiveData<Int?>

    @Suppress("UNCHECKED_CAST")
    private val newGameLoadedEventLiveData =
        session.newGameLoadedEvent.asLiveData() as LiveData<Boolean?>

    @Suppress("UNCHECKED_CAST")
    private val liveMoveCounterTextLiveData =
        session.liveMoveCounterText.asLiveData() as LiveData<String?>

    @Suppress("UNCHECKED_CAST")
    private val liveSolverCalculatingLiveData =
        session.liveSolverCalculating.asLiveData() as LiveData<Boolean?>

    @Suppress("UNCHECKED_CAST")
    private val liveMoveCounterDeviationLiveData =
        session.liveMoveCounterDeviation.asLiveData() as LiveData<Int?>

    init {
        // Wire platform hooks into the shared session.

        // Robot movement animation via RobotAnimationManager; without it the
        // position is applied immediately (same as the old immediate mode).
        session.moveAnimator = { robot, fromX, fromY, toX, toY, onComplete ->
            if (animationsEnabled) {
                if (gameGridView != null && robotAnimationManager.getGameGridView() == null) {
                    d("[ANIM] Fixing GameGridView connection to animation manager")
                    robotAnimationManager.setGameGridView(gameGridView)
                }
                robotAnimationManager.queueRobotMove(
                    robot, fromX, fromY, toX, toY
                ) { onComplete() }
            } else {
                onComplete()
            }
        }

        // Path tracking visual effects after a move animation completes
        session.onRobotMoveCompleted = { state, robot, oldX, oldY ->
            gameGridView?.handleRobotMovementEffects(state, robot, oldX, oldY)
        }

        // Visual undo after a reverse move: restore path segment + re-select robot
        session.onReverseMoveUndo = { removedPath, undoneRobotColor ->
            gameGridView?.let { gridView ->
                gridView.applyPathSegmentUndoVisual(removedPath)
                gridView.selectRobotByColor(undoneRobotColor)
                gridView.invalidate()
            }
        }

        // History files may live outside PlatformStorage (absolute paths)
        session.externalFileReader = { path ->
            try {
                val file = File(path)
                if (file.exists()) file.readText() else null
            } catch (ex: Exception) {
                e(ex, "[HISTORY_LOAD] Failed to read history file: %s", path)
                null
            }
        }

        // Session-scoped achievements (e.g. view_1_hour)
        session.achievementUnlocker = { key ->
            context?.let { AchievementManagerFactory.getInstance(it).unlock(key) }
        }

        // Automatic history upload after game completion
        session.historyUploadTrigger = {
            triggerHistoryUpload(session.currentState.value?.levelId ?: 0)
        }

        // Live move counter uses Android plurals resources
        session.liveCounterFormatter = { remainingMoves ->
            RoboyardApplication.getAppContext().resources.getQuantityString(
                R.plurals.live_move_counter_optimal_plural,
                remainingMoves,
                remainingMoves
            )
        }

        // Mirror session state revisions into the currentState LiveData so
        // in-place mutations re-emit like the old MutableLiveData did.
        viewModelScope.launch {
            session.stateRevision.collect {
                currentState.value = session.currentState.value
            }
        }
    }

    /**
     * Start a new random game
     */
    open fun startNewGame() {
        d("GameStateManager: startNewGame() called")
        session.startNewGame()
    }

    /**
     * Start a new game
     */
    fun startGame() {
        d("GameStateManager: startGame() called")
        session.startGame()
    }

    /**
     * Start a level game
     * @param levelId Level ID to load
     */
    fun startLevelGame(levelId: Int) {
        d("GameStateManager: startLevelGame() called with levelId: %d", levelId)
        session.startLevelGame(levelId)
    }

    /**
     * Load a specific level
     * @param levelId Level ID to load
     */
    open fun loadLevel(levelId: Int) {
        session.loadLevel(levelId)
    }

    /**
     * Load a saved game
     * @param saveId Save slot ID
     */
    fun loadGame(saveId: Int) {
        session.loadGame(saveId)
    }

    /**
     * Apply a loaded GameState to the current game.
     * @return true if successful, false otherwise
     */
    fun applyLoadedGameState(newState: GameState): Boolean {
        return session.applyLoadedGameState(newState)
    }

    /**
     * Legacy history-entry loading by id (unused stub kept for API parity).
     */
    fun loadHistoryEntry(historyId: Int) {
        // TODO: Implement history entry loading by id (stub preserved from original)
        val newState = createRandom()
        newState.levelId = historyId
        session.setGameState(newState)
    }

    /**
     * Load a game from a history entry
     * @param mapPath Path to the history entry file
     */
    fun loadHistoryEntry(mapPath: String) {
        session.loadHistoryEntry(mapPath)
    }

    /**
     * Save the current game to a slot
     * @param saveId The save slot ID
     * @param isAutoSave true if this is a system autosave, false for manual saves
     * @return true if the game was saved successfully, false otherwise
     */
    @JvmOverloads
    fun saveGame(saveId: Int, isAutoSave: Boolean = false): Boolean {
        return session.saveGame(saveId, isAutoSave)
    }

    /**
     * Handle a touch event on the game grid
     */
    fun handleGridTouch(x: Int, y: Int, action: Int): Boolean {
        return session.handleGridTouch(x, y, action)
    }

    /**
     * Move the selected robot in the given direction
     * @return true if the robot moved, false otherwise
     */
    fun moveRobotInDirection(dx: Int, dy: Int): Boolean {
        return session.moveRobotInDirection(dx, dy)
    }

    /**
     * Get a hint for the next move
     * @return The next move according to the solver, or null if no solution exists
     */
    val hint: IGameMove?
        get() = session.hint

    // ── Navigation (Android-specific) ──────────────────────────────────────

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

    // ── LiveData accessors (mirroring session StateFlows) ──────────────────

    open fun getCurrentState(): LiveData<GameState?> {
        return currentState
    }

    fun getMoveCount(): LiveData<Int?> {
        return moveCountLiveData
    }

    fun getSquaresMoved(): LiveData<Int?> {
        return squaresMovedLiveData
    }

    fun isGameComplete(): LiveData<Boolean?> {
        return isGameCompleteLiveData
    }

    fun isSolverRunning(): LiveData<Boolean?> {
        return isSolverRunningLiveData
    }

    fun getNewGameLoadedEvent(): LiveData<Boolean?> {
        return newGameLoadedEventLiveData
    }

    fun getWrongRobotAtTarget(): LiveData<Int?> {
        return wrongRobotAtTargetLiveData
    }

    fun getLiveMoveCounterText(): LiveData<String?> {
        return liveMoveCounterTextLiveData
    }

    fun isLiveSolverCalculating(): LiveData<Boolean?> {
        return liveSolverCalculatingLiveData
    }

    fun getLiveMoveCounterDeviation(): LiveData<Int?> {
        return liveMoveCounterDeviationLiveData
    }

    // ── Path / position tracking ───────────────────────────────────────────

    /**
     * Path history for visual robot paths: [robotColor, fromX, fromY, toX, toY].
     * Same instance as the session's list, so mutations stay in sync.
     */
    @JvmField
    val pathHistory: ArrayList<IntArray> = session.pathHistory

    /**
     * Robot starting positions: robot color to starting position [x,y].
     * Same instance as the session's map.
     */
    @JvmField
    val robotStartingPositions: HashMap<Int?, IntArray?> = session.robotStartingPositions

    fun addPathToHistory(color: Int, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        session.addPathToHistory(color, fromX, fromY, toX, toY)
    }

    fun removeLastPathFromHistory(): IntArray? {
        return session.removeLastPathFromHistory()
    }

    fun clearPathHistory() {
        session.clearPathHistory()
    }

    fun setRobotStartingPosition(color: Int, x: Int, y: Int) {
        session.setRobotStartingPosition(color, x, y)
    }

    fun clearRobotStartingPositions() {
        session.clearRobotStartingPositions()
    }

    // ── Undo / counters ────────────────────────────────────────────────────

    fun undoLastMove(): Boolean {
        return session.undoLastMove()
    }

    fun setMoveCount(count: Int) {
        session.setMoveCount(count)
    }

    fun resetSquaresMoved() {
        session.resetSquaresMoved()
    }

    fun setSquaresMoved(squares: Int) {
        session.setSquaresMoved(squares)
    }

    fun setGameComplete(complete: Boolean) {
        session.setGameComplete(complete)
    }

    fun setSoundEnabled(enabled: Boolean) {
        session.setSoundEnabled(enabled)
    }

    // ── History ────────────────────────────────────────────────────────────

    fun saveToHistoryNow(reason: String?) {
        session.saveToHistoryNow(reason)
    }

    fun loadPreviousHistoryEntry(): Boolean {
        return session.loadPreviousHistoryEntry()
    }

    fun loadNextHistoryEntry(): Boolean {
        return session.loadNextHistoryEntry()
    }

    fun hasNextHistoryEntry(): Boolean {
        return session.hasNextHistoryEntry()
    }

    fun clearLoadedFromHistoryFlag() {
        session.clearLoadedFromHistoryFlag()
    }

    // ── Level completion / stars ───────────────────────────────────────────

    fun calculateStars(playerMoves: Int, optimalMoves: Int, hintsUsed: Int): Int {
        return session.calculateStars(playerMoves, optimalMoves, hintsUsed)
    }

    val totalStars: Int
        get() = session.totalStars

    // ── Timers ─────────────────────────────────────────────────────────────

    fun startGameTimer() {
        session.startGameTimer()
    }

    fun updateGameTimer() {
        session.updateGameTimer()
    }

    fun pauseTimer() {
        session.pauseTimer()
    }

    fun resumeTimer() {
        session.resumeTimer()
    }

    fun saveUiTimerElapsed(elapsedMs: Long) {
        session.saveUiTimerElapsed(elapsedMs)
    }

    fun wasUiTimerRunning(): Boolean {
        return session.wasUiTimerRunning()
    }

    fun resetUiTimer() {
        session.resetUiTimer()
    }

    fun clearNewGameLoadedFlag() {
        session.clearNewGameLoadedFlag()
    }

    fun solutionWasAccepted(): Boolean {
        return session.solutionWasAccepted()
    }

    fun clearSolutionAcceptedFlag() {
        session.clearSolutionAcceptedFlag()
    }

    // ── Game info properties ───────────────────────────────────────────────

    val isInLevelGame: Boolean
        get() = session.isInLevelGame

    val currentLevelId: Int
        get() = session.currentLevelId

    open var difficulty: Int
        get() = session.difficulty
        set(difficulty) {
            session.difficulty = difficulty
        }

    val effectiveDifficulty: Int
        get() = session.effectiveDifficulty

    val localizedDifficultyString: String
        get() = session.localizedDifficultyString

    val levelName: String?
        get() = session.levelName

    /**
     * Check if a new game was just loaded (timer should reset)
     */
    val isNewGameLoaded: Boolean
        get() = session.isNewGameLoaded

    /** Collision info from the last movement (read by GameGridView). */
    val lastMoveHitWall: Boolean
        get() = session.lastMoveHitWall

    /** The robot element that was hit in the last movement, or null. */
    val lastMoveHitRobotElement: GameElement?
        get() = session.lastMoveHitRobotElement

    /** True if the current game was loaded from a savegame. */
    val isLoadedFromSave: Boolean
        get() = session.isLoadedFromSave

    /** True if the current game was loaded from history. */
    val isLoadedFromHistory: Boolean
        get() = session.isLoadedFromHistory

    val currentSolution: GameSolution?
        get() = session.currentSolution

    /** The current solution step (hint number, 0-indexed). */
    val currentSolutionStep: Int
        get() = session.currentSolutionStep

    /** The number of times the solver has been restarted. */
    val solverRestartCount: Int
        get() = session.solverRestartCount

    /** Minimum moves from the last found solution, or 0 if none yet. */
    val lastSolutionMinMoves: Int
        get() = session.lastSolutionMinMoves

    /** Elapsed UI timer time in milliseconds, or 0 if no timer was running. */
    val uiTimerElapsedMs: Long
        get() = session.uiTimerElapsedMs

    // ── Solver orchestration ───────────────────────────────────────────────

    fun calculateSolutionAsync(callback: SolutionCallback?) {
        session.calculateSolutionAsync(callback)
    }

    fun cancelSolver() {
        d("[SOLUTION_SOLVER] cancelSolver called")
        solverManager.cancelSolver()
        session.cancelSolver()
    }

    fun stopRegeneration() {
        session.stopRegeneration()
    }

    fun resumeRegeneration() {
        session.resumeRegeneration()
    }

    fun keepCurrentMapDespiteDifficulty() {
        session.keepCurrentMapDespiteDifficulty()
    }

    fun incrementSolutionStep() {
        session.incrementSolutionStep()
    }

    fun resetSolutionStep() {
        session.resetSolutionStep()
    }

    /**
     * Callback interface for solution calculation.
     * Kept as a nested type for API compatibility; identical to the session's.
     */
    interface SolutionCallback : GameSession.SolutionCallback

    /**
     * Animate a solution using the Android SolutionAnimator.
     */
    fun animateSolution(
        solution: GameSolution?,
        listener: SolutionAnimator.AnimationListener?
    ): SolutionAnimator? {
        if (solution == null || solution.moves.isEmpty()) {
            w("Cannot animate null or empty solution")
            listener?.onAnimationComplete()
            return null
        }

        d("Animating solution with %d moves", solution.moves.size)

        val animator = SolutionAnimator()
        animator.setAnimationListener(listener)
        animator.animateSolution(solution)
        return animator
    }

    override fun onSolverFinished(success: Boolean, solutionMoves: Int, numSolutions: Int) {
        d(
            "[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.onSolverFinished called: success=%b, moves=%d, solutions=%d",
            success, solutionMoves, numSolutions
        )
        session.onSolverFinished(success, solutionMoves, numSolutions)
    }

    override fun onSolverCancelled() {
        d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.onSolverCancelled called")
        session.onSolverCancelled()
    }

    // ── Reset / lifecycle ──────────────────────────────────────────────────

    /**
     * Reset the game to its initial state (Soft Reset).
     * Preserves the board layout, resets robot positions and counters.
     */
    fun resetGame() {
        robotAnimationManager.cancelAllAnimations()
        session.resetGame()
    }

    fun areAnimationsEnabled(): Boolean {
        return animationsEnabled
    }

    /**
     * Set the GameGridView for rendering
     * @param gameGridView The game grid view
     */
    fun setGameGridView(gameGridView: GameGridView?) {
        this.gameGridView = gameGridView
        robotAnimationManager.setGameGridView(gameGridView)
    }

    /**
     * Reset all move counts and game history.
     */
    fun resetMoveCountsAndHistory() {
        session.resetMoveCountsAndHistory()
    }

    /** reset the solver restart count */
    fun resetSolverRestartCount() {
        session.resetSolverRestartCount()
    }

    /** reset last solution min moves */
    fun resetLastSolutionMinMoves() {
        session.resetLastSolutionMinMoves()
    }

    /**
     * Check if the wrong-robot-on-target toast has already been shown for this
     * robot color this game (toast appears only once per robot color per game).
     */
    fun hasWrongRobotToastBeenShownFor(robotColor: Int): Boolean {
        return session.hasWrongRobotToastBeenShownFor(robotColor)
    }

    /**
     * Mark the wrong-robot-on-target toast as shown for this robot color.
     */
    fun markWrongRobotToastShownFor(robotColor: Int) {
        session.markWrongRobotToastShownFor(robotColor)
    }

    // ── Activity / context ─────────────────────────────────────────────────

    var activity: Activity?
        /**
         * Get the current activity
         * @return The current activity or null if none is available
         */
        get() = activityRef?.get()

        /**
         * Set the current activity reference
         * @param activity Current activity
         */
        set(activity) {
            if (activity != null) {
                this.activityRef = WeakReference<Activity?>(activity)
                d("[HISTORY] Activity reference updated in GameStateManager")
            }
        }

    /**
     * Set the UI notifier for platform-agnostic message display (KMP compatibility)
     * @param notifier The UiNotifier instance, or null to clear
     */
    fun setUiNotifier(notifier: UiNotifier?) {
        session.uiNotifier = notifier
    }

    /**
     * Get the current UI notifier
     * @return The current UiNotifier or null
     */
    fun getUiNotifier(): UiNotifier? {
        return session.uiNotifier
    }

    /**
     * Set the current game state.
     * Used by deep link functionality to load a state from external data.
     * @param state The game state to set
     */
    fun setGameState(state: GameState?) {
        if (state == null) {
            e("[DEEPLINK] Cannot set null game state")
            return
        }
        d("[DEEPLINK] Setting game state from deep link")
        session.setGameState(state)
    }

    // ── Live move counter ──────────────────────────────────────────────────

    fun isLiveMoveCounterEnabled(): Boolean {
        return session.isLiveMoveCounterEnabled()
    }

    fun setLiveMoveCounterEnabled(enabled: Boolean) {
        session.setLiveMoveCounterEnabled(enabled)
    }

    fun triggerLiveSolver() {
        session.triggerLiveSolver()
    }

    fun clearNextMovesCache() {
        session.clearNextMovesCache()
    }

    // ── Autosave metadata ──────────────────────────────────────────────────

    fun autosaveSettingsMatch(): Boolean {
        return session.autosaveSettingsMatch()
    }

    fun clearAutosaveMetadata() {
        session.clearAutosaveMetadata()
    }

    // ── Android-only helpers ───────────────────────────────────────────────

    /**
     * Get the context from application
     * @return Application context
     */
    private fun getContext(): Context? {
        return getApplication<Application>()!!.getApplicationContext()
    }

    /**
     * Trigger an automatic history upload ~1.5s after game completion.
     */
    private fun triggerHistoryUpload(levelId: Int) {
        val currentContext = this.context
        if (currentContext == null) {
            e("[HISTORY_SYNC] Context is null, cannot upload history")
            return
        }

        val gameType = if (levelId > 0) "level $levelId" else "random game"
        d("[HISTORY_SYNC] Triggering automatic history upload after %s completion", gameType)

        Thread {
            try {
                // Wait to ensure saveToHistory() has been called AND persisted
                Thread.sleep(1500)

                d("[HISTORY_SYNC] Starting upload for %s...", gameType)
                ApiClientProvider.sync(currentContext)
                    .uploadHistory(object : HistoryUploadCallback {
                        override fun onSuccess(syncedCount: Int) {
                            d(
                                "[HISTORY_SYNC] Upload callback: success with %d entries synced after %s",
                                syncedCount, gameType
                            )
                        }

                        override fun onError(error: String?) {
                            e("[HISTORY_SYNC] Upload callback: error after %s - %s", gameType, error)
                        }
                    })
            } catch (ex: Exception) {
                e(ex, "[HISTORY_SYNC] Exception during upload after %s", gameType)
            }
        }.start()
    }

    /**
     * Schedule a delayed hint button reset to avoid race conditions when a new
     * game is loaded from history.
     */
    fun scheduleHintButtonReset(hintButton: ToggleButton?) {
        if (hintButton == null) return

        d(
            "[HINT_SYSTEM] New game loaded - scheduling hint button reset in %d ms",
            HINT_BUTTON_RESET_DELAY_MS
        )

        hintButtonResetJob?.cancel()

        hintButtonResetJob = viewModelScope.launch {
            delay(HINT_BUTTON_RESET_DELAY_MS)
            if (hintButton.isChecked()) {
                d("[HINT_SYSTEM] Executing delayed hint button reset")
                hintButton.setChecked(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        hintButtonResetJob?.cancel()
        session.cleanup()
    }

    companion object {
        private const val HINT_BUTTON_RESET_DELAY_MS: Long =
            1000 // close hint container after some milliseconds

        /**
         * Extract metadata from save data
         * @param saveData The save data string
         * @return A map containing the metadata or null if no metadata was found
         */
        @JvmStatic
        fun extractMetadataFromSaveData(saveData: String?): MutableMap<String?, String?> {
            return GameSession.extractMetadataFromSaveData(saveData)
        }
    }
}
