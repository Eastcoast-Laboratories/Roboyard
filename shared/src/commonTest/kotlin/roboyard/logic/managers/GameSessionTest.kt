package roboyard.logic.managers

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import driftingdroids.model.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import roboyard.logic.core.Constants
import roboyard.logic.core.GameElement
import roboyard.logic.core.GameState
import roboyard.logic.core.Preferences
import roboyard.logic.storage.PlatformStorage

/**
 * Unit tests for the shared GameSession (platform-independent game logic
 * extracted from the Android GameStateManager).
 */
class GameSessionTest {

    private class InMemoryStorage : PlatformStorage {
        private val strings = mutableMapOf<String, String>()
        private val ints = mutableMapOf<String, Int>()
        private val longs = mutableMapOf<String, Long>()
        private val booleans = mutableMapOf<String, Boolean>()
        val files = mutableMapOf<String, String>()

        override fun getString(key: String, defaultValue: String?): String? = strings[key] ?: defaultValue
        override fun putString(key: String, value: String) { strings[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
        override fun putInt(key: String, value: Int) { ints[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
        override fun putLong(key: String, value: Long) { longs[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
        override fun remove(key: String) {
            strings.remove(key); ints.remove(key); longs.remove(key); booleans.remove(key)
        }
        override fun clear() {
            strings.clear(); ints.clear(); longs.clear(); booleans.clear(); files.clear()
        }
        override fun readFile(fileName: String): String = files[fileName] ?: ""
        override fun writeFile(fileName: String, content: String): Boolean {
            files[fileName] = content
            return true
        }
        override fun fileExists(fileName: String): Boolean = files.containsKey(fileName)
        override fun deleteFile(fileName: String): Boolean = files.remove(fileName) != null
        override fun getFilePath(fileName: String): String = fileName
        override fun hasSavedGames(): Boolean = false
        override fun readBitmap(fileName: String): Any? = null
        override fun writeBitmap(fileName: String, bitmap: Any?): Boolean = true
    }

    private lateinit var storage: InMemoryStorage
    private lateinit var scope: CoroutineScope
    private lateinit var session: GameSession

    @BeforeTest
    fun setup() {
        storage = InMemoryStorage()
        Preferences.initialize(storage)
        scope = CoroutineScope(Dispatchers.Default + Job())
        session = GameSession(storage, scope)
    }

    /**
     * Build a deterministic 8x8 state: red robot at (1,1), red target at (6,1).
     * The robot slides freely along row 1.
     */
    private fun testState(): GameState {
        val state = GameState(8, 8)
        state.addRobot(1, 1, 0)
        state.addTarget(6, 1, 0)
        state.storeInitialRobotPositions()
        return state
    }

    @Test
    fun test_setGameState_exposesStateViaFlow() {
        val state = testState()
        session.setGameState(state)

        assertNotNull(session.currentState.value)
        assertEquals(8, session.currentState.value!!.width)
        assertEquals(0, session.moveCount.value)
        assertFalse(session.isGameComplete.value)
    }

    /**
     * Loading a different game (history entry / savegame path) must drop the
     * previous game's robot trails and advance the game counter, so UIs bound
     * to the counter rebuild their trails from the emptied history.
     */
    @Test
    fun test_applyLoadedGameState_clearsPathHistoryAndAdvancesGameCounter() {
        session.setGameState(testState())
        session.addPathToHistory(0, 1, 1, 7, 1)
        session.addPathToHistory(0, 7, 1, 7, 7)
        val counterBefore = session.gameCounter.value

        session.applyLoadedGameState(testState())

        assertTrue(session.pathHistory.isEmpty(), "trails of the replaced game must not survive a load")
        assertEquals(counterBefore + 1, session.gameCounter.value)
    }

    /**
     * Map difficulty decision: inside the regeneration limit only the
     * configured range is accepted; past the limit generation continues until
     * the map has at least min(FALLBACK_MIN_SOLUTION_MOVES, configured
     * minimum) optimal moves.
     */
    @Test
    fun test_evaluateGeneratedMap_fallbackRequiresMinimumMoves() {
        val within = 0
        val pastLimit = 100_000
        val fb = GameSession.FALLBACK_MIN_SOLUTION_MOVES
        fun verdict(moves: Int, min: Int, max: Int, discarded: Int, trivial: Boolean = false) =
            GameSession.evaluateGeneratedMap(moves, trivial, min, max, discarded)

        assertEquals(GameSession.Companion.MapVerdict.ACCEPT, verdict(22, 20, 99, within))
        assertEquals(GameSession.Companion.MapVerdict.DISCARD, verdict(12, 20, 99, within))
        assertEquals(GameSession.Companion.MapVerdict.DISCARD, verdict(0, 20, 99, within))

        // Past the limit: below the fallback minimum keeps generating, at or
        // above it the map is accepted as fallback
        assertEquals(GameSession.Companion.MapVerdict.DISCARD_BELOW_FALLBACK, verdict(2, 20, 99, pastLimit))
        assertEquals(GameSession.Companion.MapVerdict.DISCARD_BELOW_FALLBACK, verdict(fb - 1, 20, 99, pastLimit))
        assertEquals(GameSession.Companion.MapVerdict.DISCARD_BELOW_FALLBACK, verdict(0, 20, 99, pastLimit))
        assertEquals(GameSession.Companion.MapVerdict.ACCEPT_FALLBACK, verdict(fb, 20, 99, pastLimit))
        assertEquals(GameSession.Companion.MapVerdict.ACCEPT, verdict(20, 20, 99, pastLimit))

        // Too hard past the limit is accepted as fallback (any maximum)
        assertEquals(GameSession.Companion.MapVerdict.ACCEPT_FALLBACK, verdict(fb + 1, 5, 8, pastLimit))
        // Configured minimum below the fallback minimum caps it (6 here)
        assertEquals(GameSession.Companion.MapVerdict.ACCEPT_FALLBACK, verdict(9, 6, 8, pastLimit))
        assertEquals(GameSession.Companion.MapVerdict.DISCARD_BELOW_FALLBACK, verdict(5, 6, 8, pastLimit))
        // A trivial one-move solution is never accepted by generation
        assertEquals(GameSession.Companion.MapVerdict.DISCARD_BELOW_FALLBACK, verdict(1, 1, 99, pastLimit, trivial = true))
    }

    @Test
    fun test_moveRobotInDirection_slidesUntilBoundary() {
        val state = testState()
        session.setGameState(state)

        val robot = state.getRobotAt(1, 1)
        assertNotNull(robot)
        state.setSelectedRobot(robot)

        val moved = session.moveRobotInDirection(1, 0)
        assertTrue(moved)
        assertEquals(7, robot!!.x, "robot should slide to the right edge")
        assertEquals(1, session.moveCount.value)
    }

    @Test
    fun test_moveRobotInDirection_requiresSelectedRobot() {
        session.setGameState(testState())
        val moved = session.moveRobotInDirection(1, 0)
        assertFalse(moved, "no robot selected - move must be rejected")
        assertEquals(0, session.moveCount.value)
    }

    @Test
    fun test_undoLastMove_restoresPreviousPosition() {
        val state = testState()
        session.setGameState(state)
        val robot = state.getRobotAt(1, 1)!!
        state.setSelectedRobot(robot)

        session.moveRobotInDirection(1, 0)
        assertEquals(7, robot.x)

        // undo restores the state snapshot taken before the move
        val undone = session.undoLastMove()
        assertTrue(undone)
        assertEquals(0, session.moveCount.value)

        val restoredRobot = session.currentState.value!!.getRobotAt(1, 1)
        assertNotNull(restoredRobot, "robot should be back at its start position")
    }

    @Test
    fun test_undoLastMove_emptyHistoryReturnsFalse() {
        session.setGameState(testState())
        assertFalse(session.undoLastMove())
    }

    @Test
    fun test_setGameComplete_marksState() {
        val state = testState()
        session.setGameState(state)

        session.setGameComplete(true)
        assertTrue(session.isGameComplete.value)
        assertTrue(state.isComplete)
    }

    @Test
    fun test_saveGame_writesSaveFile() {
        val state = testState()
        session.setGameState(state)

        val saved = session.saveGame(1, false)
        assertTrue(saved, "saveGame should succeed for slot 1")
        assertTrue(storage.fileExists("saves/save_1.dat"), "save file must be written via PlatformStorage")
        assertTrue(storage.readFile("saves/save_1.dat").isNotEmpty())
    }

    @Test
    fun test_saveGame_slot0_blockedForManualSaveWhenAutosaveExists() {
        val state = testState()
        session.setGameState(state)

        // Existing autosave blocks manual writes to slot 0
        storage.writeFile("saves/save_0.dat", "existing-autosave")
        assertFalse(session.saveGame(0, false), "manual save to slot 0 must be blocked")

        // Autosave itself is always allowed
        assertTrue(session.saveGame(0, true))
    }

    @Test
    fun test_handleGridTouch_selectsRobot() {
        val state = testState()
        session.setGameState(state)

        session.handleGridTouch(1, 1, GameSession.ACTION_UP)
        val selected = session.currentState.value!!.getSelectedRobot()
        assertNotNull(selected)
        assertEquals(1, selected!!.x)
        assertEquals(1, selected.y)
    }

    @Test
    fun test_effectiveDifficulty_matchesPreferencesForRandomGame() {
        Preferences.setDifficulty(2)
        session.setGameState(testState())
        // setGameState marks isLoadedFromSave=true -> uses state difficulty
        val stored = session.currentState.value!!.difficulty
        assertEquals(stored, session.effectiveDifficulty)
    }

    @Test
    fun test_extractMetadataFromSaveData_delegatesToGameState() {
        val saveData = "#BOARD:8x8;DIFFICULTY:2;MOVES:5;\nrest"
        val meta = GameSession.extractMetadataFromSaveData(saveData)
        assertEquals("2", meta["DIFFICULTY"])
        assertEquals("5", meta["MOVES"])
    }

    /**
     * Regression: when the solver finishes and the generated map is discarded
     * (too easy/too hard/no solution), onSolutionCalculationCompleted used to
     * return early WITHOUT clearing isSolverRunning. The delayed regeneration
     * then hit the "already running" guard and the solver never ran again —
     * the UI stayed on "AI calculating solution" forever.
     */
    @Test
    fun test_isSolverRunning_clearedAfterMapDiscard() {
        val savedMin = Preferences.minSolutionMoves
        val savedMax = Preferences.maxSolutionMoves
        try {
            Preferences.boardSizeWidth = 8
            Preferences.boardSizeHeight = 8
            // Every generated map violates the difficulty bounds -> discard path
            Preferences.minSolutionMoves = 999
            Preferences.maxSolutionMoves = 999

            // Saw a solver start, then the flag cleared again (thread-safe via
            // StateFlow because collect runs on the session scope's dispatcher)
            val sawCleared = MutableStateFlow(false)
            val collectJob = scope.launch {
                var sawRunning = false
                session.isSolverRunning.collect { running ->
                    if (running) {
                        sawRunning = true
                    } else if (sawRunning) {
                        sawCleared.value = true
                    }
                }
            }

            session.startGame()

            val deadline = TimeProvider.currentTimeMillis() + 60_000
            while (!sawCleared.value && TimeProvider.currentTimeMillis() < deadline) {
                runBlocking { delay(10) }
            }
            collectJob.cancel()

            assertTrue(
                sawCleared.value,
                "isSolverRunning was never cleared after a map discard — solver stays stuck on 'AI calculating solution'"
            )
        } finally {
            Preferences.minSolutionMoves = savedMin
            Preferences.maxSolutionMoves = savedMax
        }
    }

    @Test
    fun test_gameElapsedMs_gracePeriodAfterMapStart() {
        // No game started yet -> report "past grace" so buttons keep their
        // long-press protection.
        assertTrue(
            session.gameElapsedMs() >= Constants.BUTTON_COOLDOWN_GRACE_MS,
            "no game running: elapsed must be past the cooldown grace"
        )

        Preferences.boardSizeWidth = 8
        Preferences.boardSizeHeight = 8
        session.startGame()

        val elapsed = session.gameElapsedMs()
        assertTrue(
            elapsed in 0 until Constants.BUTTON_COOLDOWN_GRACE_MS,
            "fresh map must be inside the ${Constants.BUTTON_COOLDOWN_GRACE_MS}ms click grace, was ${elapsed}ms"
        )
    }
}
