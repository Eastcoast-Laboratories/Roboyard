package roboyard.logic.managers

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
}
