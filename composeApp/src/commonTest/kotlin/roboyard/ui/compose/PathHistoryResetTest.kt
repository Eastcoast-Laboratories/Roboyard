package roboyard.ui.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import roboyard.logic.managers.GameSession
import roboyard.logic.storage.PlatformStorage
import kotlin.test.assertTrue

/**
 * Regression test: robot trails must not leak into the next game.
 * Android clears both the view paths and `GameStateManager.pathHistory`
 * via `GameGridView.clearRobotPaths()` on every level change / new game.
 * The Compose UI rebuilds trails from `session.pathHistory` on each new
 * game epoch, so the session must clear it when a game starts — otherwise
 * the previous level's trails reappear on the new board.
 *
 * Run: ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.PathHistoryResetTest"
 */
class PathHistoryResetTest {

    private fun mockStorage() = object : PlatformStorage {
        private val files = mutableMapOf<String, String>()
        private val prefs = mutableMapOf<String, Any>()

        override fun readFile(fileName: String): String = files[fileName] ?: ""
        override fun writeFile(fileName: String, content: String): Boolean {
            files[fileName] = content
            return true
        }
        override fun fileExists(fileName: String): Boolean = files.containsKey(fileName)
        override fun deleteFile(fileName: String): Boolean {
            files.remove(fileName)
            return true
        }
        override fun hasSavedGames(): Boolean = files.keys.any { it.startsWith("saves/") }
        override fun getString(key: String, defaultValue: String?): String? = prefs[key] as? String ?: defaultValue
        override fun putString(key: String, value: String) { prefs[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = prefs[key] as? Int ?: defaultValue
        override fun putInt(key: String, value: Int) { prefs[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = prefs[key] as? Long ?: defaultValue
        override fun putLong(key: String, value: Long) { prefs[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs[key] as? Boolean ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { prefs[key] = value }
        override fun remove(key: String) { prefs.remove(key) }
        override fun clear() { prefs.clear() }
        override fun getFilePath(fileName: String): String = fileName
        override fun readBitmap(fileName: String): Any? = null
        override fun writeBitmap(fileName: String, bitmap: Any?): Boolean = false
    }

    @Test
    fun testStartLevelGameClearsPathHistory() {
        val session = GameSession(mockStorage(), CoroutineScope(Dispatchers.Default))
        session.pathHistory.add(intArrayOf(0, 1, 1, 1, 5))
        session.startLevelGame(1)
        assertTrue(session.pathHistory.isEmpty(), "startLevelGame must clear pathHistory")
    }

    @Test
    fun testResetMoveCountsAndHistoryClearsPathHistory() {
        val session = GameSession(mockStorage(), CoroutineScope(Dispatchers.Default))
        session.pathHistory.add(intArrayOf(0, 1, 1, 1, 5))
        session.resetMoveCountsAndHistory()
        assertTrue(session.pathHistory.isEmpty(), "resetMoveCountsAndHistory must clear pathHistory")
    }
}
