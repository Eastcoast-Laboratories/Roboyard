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

        // Clean solve: flag + timestamp recording (mirrors GameSession usage)
        entry.setSolvedWithoutHints(true)
        entry.recordSolvedWithoutHints(true)
        assertTrue(entry.qualifiesForNoHintsAchievement())
        assertTrue(entry.qualifiesForPerfectNoHintsAchievement())

        // Hints used AFTER a recorded clean solve must not revoke qualification
        // (chronological rule: only hints before the first clean solve disqualify)
        entry.setSolvedWithoutHints(false)
        entry.markEverUsedHints()
        assertTrue(entry.isEverUsedHints())
        assertTrue(entry.qualifiesForNoHintsAchievement())
        assertTrue(entry.qualifiesForPerfectNoHintsAchievement())

        // Once everUsedHints is set, recordSolvedWithoutHints must not set new timestamps
        val tsBefore = entry.lastSolvedWithoutHints
        val perfectTsBefore = entry.lastPerfectlySolvedWithoutHints
        entry.recordSolvedWithoutHints(true)
        assertEquals(tsBefore, entry.lastSolvedWithoutHints)
        assertEquals(perfectTsBefore, entry.lastPerfectlySolvedWithoutHints)

        // Hints before the first clean solve permanently disqualify
        val entry2 = GameHistoryEntry("history_1.txt", "Test Map", System.currentTimeMillis(), 0, 0, 0, "12x14", null)
        entry2.optimalMoves = 5
        entry2.movesMade = 5
        entry2.markEverUsedHints()
        entry2.recordSolvedWithoutHints(true)
        assertFalse(entry2.qualifiesForNoHintsAchievement())
        assertFalse(entry2.qualifiesForPerfectNoHintsAchievement())
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
    
    /**
     * Test findByMapSignature function
     */
    @Test
    fun testFindByMapSignature() {
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
        
        val entry1 = GameHistoryEntry("history_0.txt", "Map A", System.currentTimeMillis(), 30, 10, 5, "12x14", null)
        entry1.mapSignature = "signature_a"
        
        val entry2 = GameHistoryEntry("history_1.txt", "Map B", System.currentTimeMillis(), 30, 10, 5, "12x14", null)
        entry2.mapSignature = "signature_b"
        
        GameHistoryManager.addHistoryEntry(mockStorage, entry1)
        GameHistoryManager.addHistoryEntry(mockStorage, entry2)
        
        // Find by existing signature
        val found = GameHistoryManager.findByMapSignature(mockStorage, "signature_a")
        assertNotNull(found)
        assertEquals("Map A", found?.mapName)
        
        // Find by non-existing signature
        val notFound = GameHistoryManager.findByMapSignature(mockStorage, "signature_c")
        assertEquals(null, notFound)
        
        // Find by null signature
        val nullSig = GameHistoryManager.findByMapSignature(mockStorage, null)
        assertEquals(null, nullSig)
        
        // Find by empty signature
        val emptySig = GameHistoryManager.findByMapSignature(mockStorage, "")
        assertEquals(null, emptySig)
    }
    
    /**
     * Test hint tracking updates across multiple sessions
     */
    @Test
    fun testHintTrackingAcrossSessions() {
        val entry = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 0, 0, 0, "12x14", null)
        
        // Session 1: No hints used
        entry.setSolvedWithoutHints(true)
        entry.recordSolvedWithoutHints(true)
        entry.recordCompletion(30, 10, 3)
        assertTrue(entry.qualifiesForNoHintsAchievement())
        assertFalse(entry.isEverUsedHints())

        // Session 2: Hints used (simulating a later session)
        entry.recordHintUsed(2)
        entry.markEverUsedHints()
        entry.setSolvedWithoutHints(false) // Explicitly set to false when hints are used
        entry.recordCompletion(40, 15, 2)

        // Qualification persists: a clean solve was already recorded in session 1
        assertTrue(entry.qualifiesForNoHintsAchievement())
        assertTrue(entry.isEverUsedHints())
        assertEquals(2, entry.maxHintUsed)
        
        // But completion count should be 2 (both sessions recorded)
        assertEquals(2, entry.completionCount)
    }
    
    /**
     * Test that solvedWithoutHints is preserved even if hints are used later
     */
    @Test
    fun testSolvedWithoutHintsPreservation() {
        val entry = GameHistoryEntry("history_0.txt", "Test Map", System.currentTimeMillis(), 0, 0, 0, "12x14", null)
        
        // First completion without hints
        entry.setSolvedWithoutHints(true)
        entry.recordSolvedWithoutHints(true)
        entry.recordCompletion(30, 10, 3)
        
        val firstSolvedTimestamp = entry.lastSolvedWithoutHints
        assertTrue(firstSolvedTimestamp > 0)
        
        // Later session with hints
        entry.recordHintUsed(1)
        entry.markEverUsedHints()
        entry.recordCompletion(40, 15, 2)
        
        // solvedWithoutHints flag should be preserved
        assertTrue(entry.isSolvedWithoutHints())
        
        // Timestamp should not be overwritten
        assertEquals(firstSolvedTimestamp, entry.lastSolvedWithoutHints)
        
        // But everUsedHints should be true
        assertTrue(entry.isEverUsedHints())
    }
}
