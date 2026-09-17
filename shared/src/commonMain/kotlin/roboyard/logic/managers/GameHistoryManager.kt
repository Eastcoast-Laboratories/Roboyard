package roboyard.logic.managers

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import roboyard.logic.core.Constants
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.util.RLog

/**
 * Serializable data for a single history entry
 */
data class HistoryEntryData(
    val mapPath: String,
    val mapName: String,
    val timestamp: Long,
    val playDuration: Int,
    val movesMade: Int,
    val optimalMoves: Int,
    val boardSize: String,
    val previewImagePath: String,
    val completionCount: Int,
    val lastCompletionTimestamp: Long,
    val bestTime: Int,
    val bestMoves: Int,
    val wallSignature: String?,
    val positionSignature: String?,
    val mapSignature: String?,
    val completionTimestamps: List<Long>,
    val completionMoves: List<Int>,
    val completionStars: List<Int>,
    val starsEarned: Int,
    val maxHintUsed: Int,
    val solvedWithoutHints: Boolean,
    val everUsedHints: Boolean,
    val lastSolvedWithoutHints: Long,
    val lastPerfectlySolvedWithoutHints: Long,
    val difficulty: String?
)

/**
 * Serializable wrapper for history index
 */
data class HistoryIndex(
    val historyEntries: List<HistoryEntryData>
)

/**
 * Manager class for handling game history entries.
 * Provides methods for saving, loading, and managing history entries.
 * Migrated from Android-specific version to use PlatformStorage for cross-platform support.
 */
object GameHistoryManager {
    private const val HISTORY_INDEX_FILE = "history_index.json"
    private val log = RLog.tag("GameHistoryManager")
    private val gson = Gson()

    /**
     * Parse history index JSON, handling both wrapped object format and legacy direct array format.
     * Wrapped: {"historyEntries": [...]}
     * Legacy: [...]
     */
    private fun parseHistoryIndex(indexJson: String): HistoryIndex {
        val trimmed = indexJson.trim()
        return if (trimmed.startsWith("{")) {
            gson.fromJson(indexJson, HistoryIndex::class.java) ?: HistoryIndex(emptyList())
        } else {
            // Legacy direct array format
            val type = object : TypeToken<List<HistoryEntryData>>() {}.type
            val list: List<HistoryEntryData> = gson.fromJson(indexJson, type) ?: emptyList()
            HistoryIndex(list)
        }
    }

    /**
     * Initialize the history index file if it doesn't exist
     */
    @JvmStatic
    fun initialize(storage: PlatformStorage) {
        try {
            // Create empty history index file if it doesn't exist
            if (!storage.fileExists(HISTORY_INDEX_FILE)) {
                val index = HistoryIndex(emptyList())
                val indexJson = gson.toJson(index)
                storage.writeFile(HISTORY_INDEX_FILE, indexJson)
                log.d("Created empty history index file")
            }
        } catch (e: Exception) {
            log.e("Error initializing history manager: ${e.message}")
        }
    }

    /**
     * Add a new history entry or update existing one if map already exists.
     * Maps are identified by their mapSignature (unique combination of walls + positions).
     * Entries are never deleted - only updated with new completion data.
     * 
     * @return true if entry was added/updated successfully
     */
    @JvmStatic
    fun addHistoryEntry(storage: PlatformStorage, entry: GameHistoryEntry): Boolean {
        try {
            // Load existing entries
            val entries = getHistoryEntries(storage)

            // Check if we already have an entry with the same mapSignature
            var updated = false
            val newMapSignature = entry.mapSignature

            if (newMapSignature != null && !newMapSignature.isEmpty()) {
                for (i in entries.indices) {
                    val existing = entries.get(i)
                    if (newMapSignature == existing.mapSignature) {
                        // Same map found - only record completion if moves > 0 (game was actually played)
                        // Don't record completion for intermediate saves (e.g., when hints are shown)
                        val countBefore = existing.completionCount
                        if (entry.movesMade > 0) {
                            existing.recordCompletion(
                                entry.playDuration,
                                entry.movesMade,
                                entry.starsEarned
                            )
                            log.d("[HISTORY_FLOW] addHistoryEntry(existing): recordCompletion called, movesMade=${entry.movesMade}, countBefore=$countBefore, countAfter=${existing.completionCount}")
                        } else {
                            log.d("[HISTORY_FLOW] addHistoryEntry(existing): movesMade=0, skipping recordCompletion, count stays $countBefore")
                        }
                        if (entry.optimalMoves > 0) {
                            existing.optimalMoves = entry.optimalMoves
                        }
                        // Merge hint tracking - once hints used, permanently marked
                        // Update maxHintUsed to the higher value (more hints = worse)
                        if (entry.maxHintUsed > existing.maxHintUsed) {
                            existing.maxHintUsed = entry.maxHintUsed
                        }
                        // everUsedHints is cumulative: true if hints used in ANY attempt
                        if (entry.maxHintUsed >= 0 || entry.isEverUsedHints()) {
                            existing.markEverUsedHints()
                        }
                        // lastSolvedWithoutHints / lastPerfectlySolvedWithoutHints:
                        // Only update if new entry has a more recent no-hints solve
                        if (entry.lastSolvedWithoutHints > existing.lastSolvedWithoutHints) {
                            existing.lastSolvedWithoutHints = entry.lastSolvedWithoutHints
                        }
                        if (entry.lastPerfectlySolvedWithoutHints > existing.lastPerfectlySolvedWithoutHints) {
                            existing.lastPerfectlySolvedWithoutHints = entry.lastPerfectlySolvedWithoutHints
                        }

                        // Log optimal solution achievement with full hint history
                        val optMoves = if (existing.optimalMoves > 0) existing.optimalMoves else entry.optimalMoves
                        val isOptimal = optMoves > 0 && entry.movesMade == optMoves
                        if (isOptimal) {
                            val neverHints = !existing.isEverUsedHints()
                            log.d("[HISTORY] OPTIMAL SOLUTION on completion #${existing.completionCount}: map=${existing.getMapPath()}, moves=${entry.movesMade}, everUsedHints=${existing.isEverUsedHints()}, solvedWithoutHints=${existing.isSolvedWithoutHints()}, qualifiesNoHints=$neverHints")
                        }

                        updated = true
                        log.d("[HISTORY] Updated existing map (completion #${existing.completionCount}): ${existing.getMapPath()}, maxHintUsed=${existing.maxHintUsed}, everUsedHints=${existing.isEverUsedHints()}")
                        break
                    }
                }
            }

            // Fallback: check by mapName (legacy entries)
            if (!updated) {
                for (i in entries.indices) {
                    if (entries.get(i).mapName == entry.mapName) {
                        val existing = entries.get(i)
                        // Only record completion if moves > 0 (game was actually played)
                        if (entry.movesMade > 0) {
                            existing.recordCompletion(
                                entry.playDuration,
                                entry.movesMade,
                                entry.starsEarned
                            )
                        }
                        updated = true
                        break
                    }
                }
            }

            // Add new entry if not updated
            if (!updated) {
                // If game was completed (movesMade > 0) AND completionCount is still 0 (not pre-set from server restore),
                // record the completion on the new entry
                if (entry.movesMade > 0 && entry.completionCount == 0) {
                    entry.recordCompletion(entry.playDuration, entry.movesMade, entry.starsEarned)
                    log.d("[HISTORY_FLOW] addHistoryEntry(new): recordCompletion called on new entry, movesMade=${entry.movesMade}, countAfter=${entry.completionCount}")
                } else if (entry.completionCount > 0) {
                    log.d("[HISTORY_FLOW] addHistoryEntry(new): completionCount already set to ${entry.completionCount} (server restore), skipping recordCompletion")
                } else {
                    log.d("[HISTORY_FLOW] addHistoryEntry(new): movesMade=0, new entry added with completionCount=0")
                }
                entries.add(entry)
            }

            // Sort by lastCompletionTimestamp (most recently played first)
            entries.sortByDescending { 
                if (it.lastCompletionTimestamp > 0) it.lastCompletionTimestamp else it.timestamp 
            }

            // No trimming - maps are kept forever for unique map tracking

            // Save updated index
            val isSaved = saveHistoryIndex(storage, entries)
            log.d("[HISTORY] saveHistoryIndex returned: $isSaved for ${entry.getMapPath()}")

            log.d("Added history entry: ${entry.getMapPath()}")
            return isSaved
        } catch (e: Exception) {
            log.e("Error adding history entry: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Get all history entries
     */
    @JvmStatic
    fun getHistoryEntries(storage: PlatformStorage): MutableList<GameHistoryEntry> {
        val entries: MutableList<GameHistoryEntry> = mutableListOf()
        try {
            val indexJson = storage.readFile(HISTORY_INDEX_FILE)
            log.d("[HISTORY] getHistoryEntries: indexJson=${if (indexJson != null) "loaded (${indexJson.length} chars)" else "null"}")

            if (indexJson != null && !indexJson.isEmpty()) {
                val index = parseHistoryIndex(indexJson)
                
                for (entryData in index.historyEntries) {
                    val entry = GameHistoryEntry()
                    
                    // MIGRATION: Remove "history/" prefix from old entries
                    var mapPath = entryData.mapPath
                    if (mapPath.startsWith("history/")) {
                        mapPath = mapPath.substring(8)
                        log.d("[HISTORY_MIGRATION] Removed 'history/' prefix from mapPath: $mapPath")
                    }
                    entry.setMapPath(mapPath)
                    entry.mapName = entryData.mapName
                    entry.timestamp = entryData.timestamp
                    entry.playDuration = entryData.playDuration
                    entry.movesMade = entryData.movesMade
                    entry.optimalMoves = entryData.optimalMoves
                    entry.boardSize = entryData.boardSize
                    entry.previewImagePath = entryData.previewImagePath
                    entry.difficulty = migrateDifficultyStringToInt(entryData.difficulty)
                    entry.completionCount = entryData.completionCount
                    entry.lastCompletionTimestamp = entryData.lastCompletionTimestamp
                    entry.bestTime = entryData.bestTime
                    entry.bestMoves = entryData.bestMoves
                    entry.wallSignature = entryData.wallSignature
                    entry.positionSignature = entryData.positionSignature
                    entry.mapSignature = entryData.mapSignature
                    entry.setCompletionTimestamps(entryData.completionTimestamps)
                    entry.setCompletionMoves(entryData.completionMoves)
                    entry.setCompletionStars(entryData.completionStars)
                    entry.starsEarned = entryData.starsEarned
                    entry.maxHintUsed = entryData.maxHintUsed
                    entry.setSolvedWithoutHints(entryData.solvedWithoutHints)
                    entry.setEverUsedHints(entryData.everUsedHints)
                    entry.lastSolvedWithoutHints = entryData.lastSolvedWithoutHints
                    entry.lastPerfectlySolvedWithoutHints = entryData.lastPerfectlySolvedWithoutHints

                    entries.add(entry)
                }
            }
        } catch (e: Exception) {
            log.e("Error loading history entries: ${e.message}")
        }
        log.d("[HISTORY] getHistoryEntries: returning ${entries.size} entries")
        return entries
    }

    /**
     * Save the history index file
     * 
     * @return true if saved successfully
     */
    @JvmStatic
    fun saveHistoryIndex(storage: PlatformStorage, entries: MutableList<GameHistoryEntry>): Boolean {
        try {
            val entryDataList = entries.map { entry ->
                HistoryEntryData(
                    mapPath = entry.getMapPath(),
                    mapName = entry.mapName ?: "",
                    timestamp = entry.timestamp,
                    playDuration = entry.playDuration,
                    movesMade = entry.movesMade,
                    optimalMoves = entry.optimalMoves,
                    boardSize = entry.boardSize ?: "",
                    previewImagePath = entry.previewImagePath ?: "",
                    completionCount = entry.completionCount,
                    lastCompletionTimestamp = entry.lastCompletionTimestamp,
                    bestTime = entry.bestTime,
                    bestMoves = entry.bestMoves,
                    wallSignature = entry.wallSignature,
                    positionSignature = entry.positionSignature,
                    mapSignature = entry.mapSignature,
                    completionTimestamps = entry.getCompletionTimestamps(),
                    completionMoves = entry.getCompletionMoves(),
                    completionStars = entry.getCompletionStars(),
                    starsEarned = entry.starsEarned,
                    maxHintUsed = entry.maxHintUsed,
                    solvedWithoutHints = entry.isSolvedWithoutHints(),
                    everUsedHints = entry.isEverUsedHints(),
                    lastSolvedWithoutHints = entry.lastSolvedWithoutHints,
                    lastPerfectlySolvedWithoutHints = entry.lastPerfectlySolvedWithoutHints,
                    difficulty = entry.difficulty.toString()
                )
            }
            
            val index = HistoryIndex(entryDataList)
            val indexJson = gson.toJson(index)
            
            val isSaved = storage.writeFile(HISTORY_INDEX_FILE, indexJson)

            log.d("Saved history index with ${entries.size} entries")
            return isSaved
        } catch (e: Exception) {
            log.e("Error saving history index: ${e.message}")
            return false
        }
    }

    /**
     * Get the next available history index
     * @param storage the platform storage
     * @return the next available index
     */
    @JvmStatic
    fun getNextHistoryIndex(storage: PlatformStorage): Int {
        val entries = getHistoryEntries(storage)

        if (entries.isEmpty()) {
            return 0 // Start with index 0 if no entries exist
        } else {
            // Get the highest index and add 1
            var maxIndex = 0
            for (entry in entries) {
                if (entry.getHistoryIndex() > maxIndex) {
                    maxIndex = entry.getHistoryIndex()
                }
            }
            return maxIndex + 1
        }
    }

    /**
     * Find the index of a history entry by map path
     */
    @JvmStatic
    fun getHistoryIndex(storage: PlatformStorage, mapPath: String?): Int {
        val entries = getHistoryEntries(storage)
        for (i in entries.indices) {
            if (entries.get(i).getMapPath() == mapPath) {
                return i
            }
        }
        return -1
    }

    /**
     * Find a history entry by map signature
     * @param storage The platform storage
     * @param mapSignature The map signature to search for
     * @return The history entry if found, null otherwise
     */
    @JvmStatic
    fun findByMapSignature(storage: PlatformStorage, mapSignature: String?): GameHistoryEntry? {
        if (mapSignature == null || mapSignature.isEmpty()) {
            return null
        }
        
        val entries = getHistoryEntries(storage)
        for (entry in entries) {
            if (entry.mapSignature == mapSignature) {
                return entry
            }
        }
        return null
    }

    /**
     * Convert a history index to a file path
     */
    @JvmStatic
    fun indexToPath(index: Int): String {
        return "history_$index.txt"
    }

    /**
     * Delete a history entry by path
     * @param storage The platform storage
     * @param mapPath The map path to delete
     * @return true if the history entry was deleted successfully
     */
    @JvmStatic
    fun deleteHistoryEntry(storage: PlatformStorage, mapPath: String): Boolean {
        try {
            log.d("[HISTORY_DELETE] Attempting to delete history entry: $mapPath")

            // Initialize if needed
            initialize(storage)

            // Extract file name from path if it's a full path
            var fileName: String? = mapPath
            if (mapPath.contains("/")) {
                fileName = mapPath.substring(mapPath.lastIndexOf("/") + 1)
            }

            // Load existing history entries
            val historyEntries = getHistoryEntries(storage)
            if (historyEntries.isEmpty()) {
                log.e("[HISTORY_DELETE] Failed to load history entries")
                return false
            }

            log.d("[HISTORY_DELETE] Loaded ${historyEntries.size} history entries, looking for: '$mapPath' (fileName: '$fileName')")

            // Find the entry to delete
            var entryToDelete: GameHistoryEntry? = null
            for (entry in historyEntries) {
                val entryPath = entry.getMapPath()
                var entryFileName = entryPath
                if (entryPath.contains("/")) {
                    entryFileName = entryPath.substring(entryPath.lastIndexOf("/") + 1)
                }

                // Match by either full path or just filename
                if (entryPath == mapPath || entryFileName == fileName) {
                    entryToDelete = entry
                    log.d("Found entry to delete: ${entry.mapName}")
                    break
                }
            }

            if (entryToDelete == null) {
                log.e("History entry not found for path: $mapPath")
                return false
            }

            // Remove the entry from the list
            historyEntries.remove(entryToDelete)

            // Delete the actual file
            val fileDeleted = storage.deleteFile(mapPath)
            if (!fileDeleted) {
                log.e("Failed to delete history file: $mapPath")
            }

            // Update the history index regardless of file deletion success
            val indexUpdated = saveHistoryIndex(storage, historyEntries)
            if (!indexUpdated) {
                log.e("Failed to update history index after deletion")
                return false
            }

            log.d("Successfully deleted history entry: $mapPath")
            return true
        } catch (e: Exception) {
            log.e("Error deleting history entry: $mapPath - ${e.message}")
            return false
        }
    }

    // ========== Unique Map Tracking Methods ==========
    /**
     * Check if a map with the given signature is being completed for the first time.
     * @param storage The platform storage
     * @param mapSignature The unique map signature to check
     * @return true if this map has never been completed before
     */
    @JvmStatic
    fun isFirstCompletion(storage: PlatformStorage, mapSignature: String?): Boolean {
        if (mapSignature == null || mapSignature.isEmpty()) {
            return true // No signature = treat as new
        }
        val existing = findByMapSignature(storage, mapSignature)
        // An entry is created on the first move (before completion), so we check
        // completionCount == 0 to distinguish "started but not yet completed" from "already completed before".
        return existing == null || existing.completionCount == 0
    }

    /**
     * Find all history entries with the same wall signature (same walls, different positions).
     * @param storage The platform storage
     * @param wallSignature The wall signature to match
     * @return List of entries with matching wall layout
     */
    @JvmStatic
    fun findByWallSignature(
        storage: PlatformStorage,
        wallSignature: String?
    ): MutableList<GameHistoryEntry> {
        val result: MutableList<GameHistoryEntry> = ArrayList()
        if (wallSignature == null || wallSignature.isEmpty()) {
            return result
        }
        val entries = getHistoryEntries(storage)
        for (entry in entries) {
            if (wallSignature == entry.wallSignature) {
                result.add(entry)
            }
        }
        return result
    }

    /**
     * Get the total count of unique maps completed.
     * @param storage The platform storage
     * @return Number of unique maps in history
     */
    @JvmStatic
    fun getUniqueMapCount(storage: PlatformStorage): Int {
        return getHistoryEntries(storage).size
    }

    /**
     * Get the total count of unique completed levels from history.
     * Only entries with map names like "Level N" or matching level file paths are counted.
     * @param storage The platform storage
     * @return Number of unique completed levels in history
     */
    @JvmStatic
    fun getUniqueCompletedLevelCount(storage: PlatformStorage): Int {
        val entries = getHistoryEntries(storage)
        val uniqueLevelKeys: MutableSet<String> = HashSet()

        for (entry in entries) {
            val levelKey = extractLevelKey(entry)
            if (levelKey != null) {
                uniqueLevelKeys.add(levelKey)
            }
        }
        log.d(
            "[GAME_HISTORY][ACHIEVEMENTS][LEVEL] getUniqueCompletedLevelCount: Found ${uniqueLevelKeys.size} unique levels"
        )
        return uniqueLevelKeys.size
    }

    /**
     * Get the total count of unique completed levels that earned at least three stars.
     * @param storage The platform storage
     * @return Number of unique 3-star levels in history
     */
    @JvmStatic
    fun getUniqueThreeStarLevelCount(storage: PlatformStorage): Int {
        val entries = getHistoryEntries(storage)
        val uniqueLevelKeys: MutableSet<String> = HashSet()

        for (entry in entries) {
            if (entry.starsEarned < 3) {
                continue
            }

            val levelKey = extractLevelKey(entry)
            if (levelKey != null) {
                uniqueLevelKeys.add(levelKey)
            }
        }

        log.d(
            "[GAME_HISTORY][ACHIEVEMENTS][LEVEL] getUniqueThreeStarLevelCount: Found ${uniqueLevelKeys.size} unique 3-star levels"
        )
        return uniqueLevelKeys.size
    }

    /**
     * Extract a unique level key from a history entry.
     * Handles both "Level N" map names and level file paths.
     * @param entry The history entry
     * @return The level key, or null if not a level
     */
    /**
     * Extracts a normalized level key (e.g. "level_1", "custom_level_141") from a
     * history entry. Prioritizes mapPath parsing to handle corrupt server data
     * where mapNames are 5-letter codes; falls back to mapName "Level N".
     * Matches Android LevelSelectionFragment.extractLevelKey.
     */
    @JvmStatic
    fun extractLevelKey(entry: GameHistoryEntry): String? {
        val mapPath = entry.getMapPath()
        if (mapPath != null) {
            val base = if (mapPath.contains("/"))
                mapPath.substring(mapPath.lastIndexOf('/') + 1)
            else
                mapPath
            if (base.startsWith("level_") || base.startsWith("custom_level_")) {
                return if (base.endsWith(".txt")) base.substring(0, base.length - 4) else base
            }
        }

        val mapName = entry.mapName
        if (mapName != null && mapName.matches("(?i)Level \\d+".toRegex())) {
            val id =
                mapName.trim { it <= ' ' }.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1].toInt()
            return if (id >= 141) "custom_level_$id" else "level_$id"
        }
        return null
    }

    /**
     * Loads history entries mapped by normalized level key (e.g. "level_1").
     * When duplicates exist, the entry with the most completions wins.
     * Matches Android LevelSelectionFragment.loadHistoryByMapName.
     */
    @JvmStatic
    fun getHistoryByLevelKey(storage: PlatformStorage): Map<String, GameHistoryEntry> {
        val result = HashMap<String, GameHistoryEntry>()
        try {
            val entries = getHistoryEntries(storage)
            for (entry in entries) {
                val key = extractLevelKey(entry) ?: continue
                val existing = result[key]
                if (existing == null || entry.completionCount >= existing.completionCount) {
                    result[key] = entry
                }
            }
        } catch (e: Exception) {
            log.e(e, "[LEVEL_SELECTION] Error loading history entries for minimap display")
        }
        return result
    }

    /**
     * Get the completion count for a specific map.
     * @param storage The platform storage
     * @param mapSignature The map signature to check
     * @return Number of times this map was completed, or 0 if never
     */
    @JvmStatic
    fun getCompletionCount(storage: PlatformStorage, mapSignature: String?): Int {
        val entry = findByMapSignature(storage, mapSignature)
        return if (entry != null) entry.completionCount else 0
    }

    /**
     * Delete a history entry by entry object.
     * @param storage The platform storage
     * @param entry The history entry to delete
     */
    @JvmStatic
    fun deleteHistoryEntry(storage: PlatformStorage, entry: GameHistoryEntry) {
        try {
            val entries = getHistoryEntries(storage)

            var removed = false
            for (i in entries.indices) {
                if (entries[i].getMapPath() == entry.getMapPath()) {
                    entries.removeAt(i)
                    removed = true
                    break
                }
            }

            if (removed) {
                storage.deleteFile(entry.getMapPath())
                if (entry.previewImagePath != null) {
                    storage.deleteFile(entry.previewImagePath!!)
                }
                saveHistoryIndex(storage, entries)
                log.d("Deleted history entry: ${entry.getMapPath()}")
            }
        } catch (e: Exception) {
            log.e("Error deleting history entry: ${e.message}")
        }
    }

    /**
     * Migrate old difficulty values to int IDs.
     * Handles three formats:
     * - Numeric string ("0", "1", "2", "3") — already an int, just parse it
     * - English strings ("beginner", "intermediate", "advanced", "impossible")
     * - German strings ("Anfänger", "Fortgeschritten", "verrückt", "Unmöglich")
     * @param difficultyStr The old difficulty value (may be numeric or localized string)
     * @return The corresponding difficulty ID (0-3)
     */
    private fun migrateDifficultyStringToInt(difficultyStr: String?): Int {
        if (difficultyStr == null || difficultyStr.isEmpty()) {
            return Constants.DIFFICULTY_BEGINNER
        }

        val lower = difficultyStr.lowercase().trim { it <= ' ' }

        // Numeric string (already an int ID, just parse it)
        val num = lower.toIntOrNull()
        if (num != null) {
            return when (num) {
                Constants.DIFFICULTY_BEGINNER,
                Constants.DIFFICULTY_ADVANCED,
                Constants.DIFFICULTY_INSANE,
                Constants.DIFFICULTY_IMPOSSIBLE -> num
                else -> {
                    log.w("[HISTORY_MIGRATION] Unknown difficulty int: '$difficultyStr', defaulting to BEGINNER")
                    Constants.DIFFICULTY_BEGINNER
                }
            }
        }

        // English strings
        if (lower.contains("beginner") || lower.contains("easy")) {
            return Constants.DIFFICULTY_BEGINNER
        } else if (lower.contains("intermediate") || lower.contains("advanced") || lower.contains("medium")) {
            return Constants.DIFFICULTY_ADVANCED
        } else if (lower.contains("insane") || lower.contains("hard")) {
            return Constants.DIFFICULTY_INSANE
        } else if (lower.contains("impossible") || lower.contains("expert")) {
            return Constants.DIFFICULTY_IMPOSSIBLE
        }

        // German strings (Anfänger, Fortgeschritten, verrückt, Unmöglich)
        if (lower.length >= 3) {
            val prefix = lower.substring(0, 3)
            if (prefix == "anf") {
                return Constants.DIFFICULTY_BEGINNER
            } else if (prefix == "for") {
                return Constants.DIFFICULTY_ADVANCED
            } else if (prefix == "ver") {
                return Constants.DIFFICULTY_INSANE
            } else if (prefix == "unm") {
                return Constants.DIFFICULTY_IMPOSSIBLE
            }
        }

        log.w("[HISTORY_MIGRATION] Unknown difficulty string: '$difficultyStr', defaulting to BEGINNER")
        return Constants.DIFFICULTY_BEGINNER
    }
}
