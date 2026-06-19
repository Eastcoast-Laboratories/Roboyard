package roboyard.ui.compose

import driftingdroids.model.Board
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.managers.GameHistoryManager
import roboyard.logic.storage.PlatformStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for history and autosave functionality
 */
class HistoryAutosaveTest {
    
    /**
     * Test that GameHistoryEntry records completion correctly
     */
    @Test
    fun testGameHistoryEntryRecordCompletion() {
        val entry = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 0, 0, 0, "12x14", null)
        
        // Record first completion
        val newRecord = entry.recordCompletion(30, 10, 3)
        assertTrue(newRecord, "First completion should be a new record")
        assertEquals(1, entry.completionCount)
        assertEquals(30, entry.playDuration)
        assertEquals(10, entry.movesMade)
        assertEquals(3, entry.starsEarned)
        assertEquals(10, entry.bestMoves)
        assertEquals(30, entry.bestTime)
        
        // Record second completion with better time
        val newRecord2 = entry.recordCompletion(20, 8, 3)
        assertTrue(newRecord2, "Better time should be a new record")
        assertEquals(2, entry.completionCount)
        assertEquals(50, entry.playDuration) // 30 + 20
        assertEquals(8, entry.movesMade)
        assertEquals(8, entry.bestMoves)
        assertEquals(20, entry.bestTime)
    }
    
    /**
     * Test that GameHistoryEntry records hint usage correctly
     */
    @Test
    fun testGameHistoryEntryRecordHintUsed() {
        val entry = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 0, 0, 0, "12x14", null)
        
        // Record hint usage
        entry.recordHintUsed(1)
        assertEquals(1, entry.maxHintUsed)
        assertTrue(entry.isEverUsedHints())
        
        // Record higher hint level
        entry.recordHintUsed(3)
        assertEquals(3, entry.maxHintUsed)
        assertTrue(entry.isEverUsedHints())
    }
    
    /**
     * Test that GameHistoryEntry records solved without hints correctly
     */
    @Test
    fun testGameHistoryEntryRecordSolvedWithoutHints() {
        val entry = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 0, 0, 0, "12x14", null)
        
        // Record solved without hints (optimal)
        entry.recordSolvedWithoutHints(true)
        assertTrue(entry.isSolvedWithoutHints())
        assertTrue(entry.lastSolvedWithoutHints > 0)
        assertTrue(entry.lastPerfectlySolvedWithoutHints > 0)
        
        // Check achievement qualification
        assertTrue(entry.qualifiesForNoHintsAchievement())
        assertTrue(entry.qualifiesForPerfectNoHintsAchievement())
    }
    
    /**
     * Test that GameHistoryEntry achievement qualification works correctly
     */
    @Test
    fun testGameHistoryEntryAchievementQualification() {
        val entry = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 0, 0, 0, "12x14", null)
        
        // Set optimal moves
        entry.optimalMoves = 5
        entry.movesMade = 5
        
        // Without hints
        entry.setSolvedWithoutHints(true)
        assertTrue(entry.qualifiesForNoHintsAchievement())
        assertTrue(entry.qualifiesForPerfectNoHintsAchievement())
        
        // With hints (solvedWithoutHints = false)
        entry.setSolvedWithoutHints(false)
        entry.markEverUsedHints()
        assertFalse(entry.qualifiesForNoHintsAchievement())
        assertFalse(entry.qualifiesForPerfectNoHintsAchievement())
        
        // Test that everUsedHints is tracked separately
        assertTrue(entry.isEverUsedHints())
        
        // Reset to solved without hints
        entry.setSolvedWithoutHints(true)
        assertTrue(entry.qualifiesForNoHintsAchievement())
        assertTrue(entry.qualifiesForPerfectNoHintsAchievement())
    }
    
    /**
     * Test map signature generation
     */
    @Test
    fun testMapSignatureGeneration() {
        val board = Board.createBoardRandom(4)
        
        val wallSig = generateWallSignature(board)
        val posSig = generatePositionSignature(board)
        val mapSig = generateMapSignature(board)
        
        assertNotNull(wallSig)
        assertNotNull(posSig)
        assertNotNull(mapSig)
        assertTrue(mapSig.contains("||"))
        assertEquals(wallSig + "||" + posSig, mapSig)
    }
    
    /**
     * Test that history index file is created on initialization
     */
    @Test
    fun testGameHistoryManagerInitialization() {
        val mockStorage = object : PlatformStorage {
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
        
        GameHistoryManager.initialize(mockStorage)
        assertTrue(mockStorage.fileExists("history_index.json"))
    }
    
    /**
     * Test that history entries are saved and loaded correctly
     */
    @Test
    fun testGameHistoryManagerSaveAndLoad() {
        val mockStorage = object : PlatformStorage {
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
        
        GameHistoryManager.initialize(mockStorage)
        
        val entry = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 30, 10, 5, "12x14", null)
        entry.recordCompletion(30, 10, 3)
        
        val added = GameHistoryManager.addHistoryEntry(mockStorage, entry)
        assertTrue(added)
        
        val entries = GameHistoryManager.getHistoryEntries(mockStorage)
        assertEquals(1, entries.size)
        
        val loadedEntry = entries[0]
        assertEquals("history_0.txt", loadedEntry.getMapPath())
        assertEquals("Test Map", loadedEntry.mapName)
        assertEquals(1, loadedEntry.completionCount)
        assertEquals(10, loadedEntry.movesMade)
        assertEquals(3, loadedEntry.starsEarned)
    }
    
    /**
     * Test that history entries are sorted by timestamp (newest first)
     */
    @Test
    fun testGameHistoryManagerSorting() {
        val mockStorage = object : PlatformStorage {
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
        
        GameHistoryManager.initialize(mockStorage)
        
        val now = System.currentTimeMillis()
        val entry1 = GameHistoryEntry("history_0.txt", "Old Map", now - 10000, 30, 10, 5, "12x14", null)
        val entry2 = GameHistoryEntry("history_1.txt", "New Map", now, 30, 10, 5, "12x14", null)
        
        GameHistoryManager.addHistoryEntry(mockStorage, entry1)
        GameHistoryManager.addHistoryEntry(mockStorage, entry2)
        
        val entries = GameHistoryManager.getHistoryEntries(mockStorage)
        assertEquals(2, entries.size)
        assertEquals("history_1.txt", entries[0].getMapPath()) // Newest first
        assertEquals("history_0.txt", entries[1].getMapPath())
    }
    
    /**
     * Test that history entries with same map signature are updated instead of duplicated
     */
    @Test
    fun testGameHistoryManagerMapSignatureUpdate() {
        val mockStorage = object : PlatformStorage {
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
        
        GameHistoryManager.initialize(mockStorage)
        
        val entry1 = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 30, 10, 5, "12x14", null)
        entry1.mapSignature = "test_signature"
        entry1.recordCompletion(30, 10, 3)
        
        GameHistoryManager.addHistoryEntry(mockStorage, entry1)
        
        val entry2 = GameHistoryEntry("history_1.txt", "Test Map", System.currentTimeMillis(), 30, 8, 3, "12x14", null)
        entry2.mapSignature = "test_signature"
        entry2.recordCompletion(30, 8, 3)
        
        GameHistoryManager.addHistoryEntry(mockStorage, entry2)
        
        val entries = GameHistoryManager.getHistoryEntries(mockStorage)
        assertEquals(1, entries.size) // Should be updated, not duplicated
        assertEquals(2, entries[0].completionCount) // Should have 2 completions
    }
}
