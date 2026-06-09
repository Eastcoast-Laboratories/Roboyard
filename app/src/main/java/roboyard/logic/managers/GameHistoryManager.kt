package roboyard.logic.managers

import android.app.Application
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import roboyard.logic.core.Constants
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.core.GameState.Companion.parseFromSaveData
import roboyard.logic.storage.FileReadWrite
import roboyard.logic.storage.FileReadWrite.Companion.deletePrivateData
import roboyard.logic.storage.FileReadWrite.Companion.privateDataExists
import roboyard.logic.storage.FileReadWrite.Companion.readPrivateData
import roboyard.logic.storage.FileReadWrite.Companion.writePrivateData
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.w
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Collections
import java.util.Locale

/**
 * Manager class for handling game history entries.
 * Provides methods for saving, loading, and managing history entries.
 */
object GameHistoryManager {
    private const val HISTORY_DIR = "history"
    private const val HISTORY_INDEX_FILE = "history_index.json"

    // Maps are never deleted - kept forever for unique map tracking
    /**
     * Initialize the history directory if it doesn't exist
     */
    @JvmStatic
    fun initialize(context: Context) {
        try {
            // Create empty history index file if it doesn't exist
            if (!privateDataExists(context, HISTORY_INDEX_FILE)) {
                val indexJson = JSONObject()
                indexJson.put("historyEntries", JSONArray())
                writePrivateData(context, HISTORY_INDEX_FILE, indexJson.toString())
                d("Created empty history index file")
            }
        } catch (e: Exception) {
            e("Error initializing history manager: %s", e.message)
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
    fun addHistoryEntry(context: Context, entry: GameHistoryEntry): Boolean {
        try {
            // Load existing entries
            val entries = getHistoryEntries(context)


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
                            d(
                                "[HISTORY_FLOW] addHistoryEntry(existing): recordCompletion called, movesMade=%d, countBefore=%d, countAfter=%d",
                                entry.movesMade, countBefore, existing.completionCount
                            )
                        } else {
                            d(
                                "[HISTORY_FLOW] addHistoryEntry(existing): movesMade=0, skipping recordCompletion, count stays %d",
                                countBefore
                            )
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
                            existing.lastPerfectlySolvedWithoutHints =
                                entry.lastPerfectlySolvedWithoutHints
                        }


                        // Log optimal solution achievement with full hint history
                        val optMoves =
                            if (existing.optimalMoves > 0) existing.optimalMoves else entry.optimalMoves
                        val isOptimal = optMoves > 0 && entry.movesMade == optMoves
                        if (isOptimal) {
                            val neverHints = !existing.isEverUsedHints()
                            d(
                                "[HISTORY] OPTIMAL SOLUTION on completion #%d: map=%s, moves=%d, " +
                                        "everUsedHints=%b, solvedWithoutHints=%b, qualifiesNoHints=%b",
                                existing.completionCount, existing.getMapPath(),
                                entry.movesMade, existing.isEverUsedHints(),
                                existing.isSolvedWithoutHints(), neverHints
                            )
                        }

                        updated = true
                        d(
                            "[HISTORY] Updated existing map (completion #%d): %s, maxHintUsed=%d, everUsedHints=%b",
                            existing.completionCount, existing.getMapPath(),
                            existing.maxHintUsed, existing.isEverUsedHints()
                        )
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
                    d(
                        "[HISTORY_FLOW] addHistoryEntry(new): recordCompletion called on new entry, movesMade=%d, countAfter=%d",
                        entry.movesMade, entry.completionCount
                    )
                } else if (entry.completionCount > 0) {
                    d(
                        "[HISTORY_FLOW] addHistoryEntry(new): completionCount already set to %d (server restore), skipping recordCompletion",
                        entry.completionCount
                    )
                } else {
                    d("[HISTORY_FLOW] addHistoryEntry(new): movesMade=0, new entry added with completionCount=0")
                }
                entries.add(entry)
            }


            // Sort by lastCompletionTimestamp (most recently played first)
            Collections.sort<GameHistoryEntry?>(entries, object : Comparator<GameHistoryEntry?> {
                override fun compare(o1: GameHistoryEntry?, o2: GameHistoryEntry?): Int {
                    val t1 = if ((o1?.lastCompletionTimestamp ?: 0) > 0) o1?.lastCompletionTimestamp ?: 0 else o1?.timestamp ?: 0
                    val t2 = if ((o2?.lastCompletionTimestamp ?: 0) > 0) o2?.lastCompletionTimestamp ?: 0 else o2?.timestamp ?: 0
                    return t2.compareTo(t1)
                }
            })


            // No trimming - maps are kept forever for unique map tracking

            // Save updated index
            val isSaved = saveHistoryIndex(context, entries)

            d("Added history entry: %s", entry.getMapPath())
            return isSaved
        } catch (e: Exception) {
            e("Error adding history entry: %s", e.message)
            return false
        }
    }

    /**
     * Get all history entries
     */
    @JvmStatic
    fun getHistoryEntries(context: Context): MutableList<GameHistoryEntry> {
        val entries: MutableList<GameHistoryEntry> = ArrayList<GameHistoryEntry>()
        var anyMigrated = false
        try {
            val indexJson = readPrivateData(context, HISTORY_INDEX_FILE)
            d(
                "[HISTORY] getHistoryEntries: indexJson=%s",
                if (indexJson != null) "loaded (" + indexJson.length + " chars)" else "null"
            )

            if (indexJson != null && !indexJson.isEmpty()) {
                val entriesArray: JSONArray?


                // Handle both formats: direct array [...] and wrapped object {"historyEntries":[...]}
                if (indexJson.trim { it <= ' ' }.startsWith("{")) {
                    // Wrapped format - extract the array
                    val wrapperObject = JSONObject(indexJson)
                    entriesArray = wrapperObject.getJSONArray("historyEntries")
                } else {
                    // Direct array format
                    entriesArray = JSONArray(indexJson)
                }

                for (i in 0..<entriesArray.length()) {
                    val entryJson = entriesArray.getJSONObject(i)
                    val entry = GameHistoryEntry()


                    // MIGRATION: Remove "history/" prefix from old entries (Android doesn't allow path separators in filenames)
                    var mapPath = entryJson.getString("mapPath")
                    if (mapPath.startsWith("history/")) {
                        mapPath = mapPath.substring(8) // Remove "history/" prefix
                        d("[HISTORY_MIGRATION] Removed 'history/' prefix from mapPath: %s", mapPath)
                        anyMigrated = true
                    }
                    entry.setMapPath(mapPath)
                    entry.mapName = entryJson.optString("mapName", "Unnamed")
                    entry.timestamp = entryJson.getLong("timestamp")
                    entry.playDuration = entryJson.getInt("playDuration")
                    entry.movesMade = entryJson.getInt("movesMade")
                    entry.optimalMoves = entryJson.optInt("optimalMoves", 0)
                    entry.boardSize = entryJson.optString("boardSize", "")
                    entry.previewImagePath = entryJson.optString("previewImagePath", "")


                    // Load difficulty - support both int (new) and string (legacy migration)
                    var difficultyId = Constants.DIFFICULTY_BEGINNER // default
                    if (entryJson.has("difficulty")) {
                        val diffValue = entryJson.get("difficulty")
                        if (diffValue is Int) {
                            difficultyId = diffValue
                        } else if (diffValue is String) {
                            // Migration: convert old string values to int
                            val diffStr = diffValue
                            difficultyId = migrateDifficultyStringToInt(diffStr)
                            d(
                                "[HISTORY_MIGRATION] Converted difficulty '%s' to %d",
                                diffStr,
                                difficultyId
                            )
                        }
                    }
                    entry.difficulty = difficultyId


                    // Load new fields for unique map tracking
                    entry.completionCount = entryJson.optInt("completionCount", 0)
                    entry.lastCompletionTimestamp =
                        entryJson.optLong("lastCompletionTimestamp", entry.timestamp)
                    entry.bestTime = entryJson.optInt("bestTime", entry.playDuration)
                    entry.bestMoves = entryJson.optInt("bestMoves", entry.movesMade)
                    entry.wallSignature = entryJson.optString("wallSignature", null)
                    entry.positionSignature = entryJson.optString("positionSignature", null)
                    entry.mapSignature = entryJson.optString("mapSignature", null)


                    // Load completion timestamps array
                    if (entryJson.has("completionTimestamps")) {
                        val timestamps = entryJson.getJSONArray("completionTimestamps")
                        val completionTimestamps = mutableListOf<Long>()
                        for (j in 0..<timestamps.length()) {
                            completionTimestamps.add(timestamps.getLong(j))
                        }
                        entry.setCompletionTimestamps(completionTimestamps)
                    } else {
                        // Legacy entry - create list with single timestamp
                        val completionTimestamps = mutableListOf<Long>()
                        completionTimestamps.add(entry.timestamp)
                        entry.setCompletionTimestamps(completionTimestamps)
                    }

                    val completionSize = entry.getCompletionTimestamps().size

                    if (entryJson.has("completionMoves")) {
                        val movesArray = entryJson.getJSONArray("completionMoves")
                        val completionMoves: MutableList<Int> = ArrayList<Int>()
                        for (j in 0..<movesArray.length()) {
                            completionMoves.add(movesArray.getInt(j))
                        }
                        entry.setCompletionMoves(completionMoves)
                    } else {
                        val completionMoves: MutableList<Int> = ArrayList<Int>()
                        for (j in 0..<completionSize) {
                            completionMoves.add(entry.movesMade)
                        }
                        entry.setCompletionMoves(completionMoves)
                    }


                    // Load stars earned
                    entry.starsEarned = entryJson.optInt("starsEarned", 0)

                    if (entryJson.has("completionStars")) {
                        val starsArray = entryJson.getJSONArray("completionStars")
                        val completionStars: MutableList<Int> = ArrayList<Int>()
                        for (j in 0..<starsArray.length()) {
                            completionStars.add(starsArray.getInt(j))
                        }
                        entry.setCompletionStars(completionStars)
                    } else {
                        val completionStars: MutableList<Int> = ArrayList<Int>()
                        for (j in 0..<completionSize) {
                            completionStars.add(entry.starsEarned)
                        }
                        entry.setCompletionStars(completionStars)
                    }


                    // Load hint tracking fields
                    entry.maxHintUsed = entryJson.optInt("maxHintUsed", -1)
                    entry.setSolvedWithoutHints(entryJson.optBoolean("solvedWithoutHints", false))
                    entry.setEverUsedHints(entryJson.optBoolean("everUsedHints", false))
                    // Load no-hints timestamp fields (0 = never solved without hints)
                    entry.lastSolvedWithoutHints = entryJson.optLong("lastSolvedWithoutHints", 0)
                    entry.lastPerfectlySolvedWithoutHints =
                        entryJson.optLong("lastPerfectlySolvedWithoutHints", 0)


                    // MIGRATION: If mapSignature is missing, compute it from the saved game file
                    if (entry.mapSignature == null || entry.mapSignature!!.isEmpty()) {
                        computeAndSetMapSignature(context, entry)
                        anyMigrated = true
                    }

                    entries.add(entry)
                }


                // Save index if any entries were migrated (to persist the computed signatures or removed prefixes)
                if (anyMigrated) {
                    saveHistoryIndex(context, entries)
                    d("[HISTORY_MIGRATION] Saved migrated entries (signatures or removed 'history/' prefix) to index")
                }
            }
        } catch (e: Exception) {
            e("Error loading history entries: %s", e.message)
        }
        d("[HISTORY] getHistoryEntries: returning %d entries", entries.size)
        return entries
    }

    /**
     * Compute and set mapSignature for a history entry by loading its saved game file.
     * This is used for migration of old entries that don't have mapSignature stored.
     * @param context The context
     * @param entry The history entry to update
     */
    private fun computeAndSetMapSignature(context: Context, entry: GameHistoryEntry) {
        try {
            // Read the save data from the history file
            var historyFile = File(entry.getMapPath())
            if (!historyFile.isAbsolute()) {
                historyFile = context.getFileStreamPath(entry.getMapPath())
            }
            if (!historyFile.exists()) {
                w(
                    "[HISTORY_MIGRATION] Cannot compute mapSignature: file not found: %s",
                    entry.getMapPath()
                )
                return
            }

            val saveData = StringBuilder()
            FileInputStream(historyFile).use { fis ->
                BufferedReader(InputStreamReader(fis)).use { reader ->
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        saveData.append(line).append("\n")
                    }
                }
            }

            // Parse the save data to get the GameState
            val application = context.applicationContext as? Application
            val state = parseFromSaveData(saveData.toString(), application)
            if (state != null) {
                // Compute signatures from the loaded state
                val wallSig = state.generateWallSignature()
                val posSig = state.generatePositionSignature()
                val mapSig = state.generateMapSignature()

                entry.wallSignature = wallSig
                entry.positionSignature = posSig
                entry.mapSignature = mapSig

                d("[HISTORY_MIGRATION] Computed mapSignature for '%s': %s", entry.mapName, mapSig)
            } else {
                w("[HISTORY_MIGRATION] Failed to parse GameState for: %s", entry.getMapPath())
            }
        } catch (e: Exception) {
            e(e, "[HISTORY_MIGRATION] Error computing mapSignature for: %s", entry.getMapPath())
        }
    }

    /**
     * Delete a history entry
     */
    @JvmStatic
    fun deleteHistoryEntry(context: Context, entry: GameHistoryEntry) {
        try {
            // Load existing entries
            val entries = getHistoryEntries(context)


            // Remove the entry
            var removed = false
            for (i in entries.indices) {
                if (entries.get(i).getMapPath() == entry.getMapPath()) {
                    entries.removeAt(i)
                    removed = true
                    break
                }
            }

            if (removed) {
                // Delete the files
                deleteHistoryFiles(context, entry)


                // Save updated index
                saveHistoryIndex(context, entries)

                d("Deleted history entry: %s", entry.getMapPath())
            }
        } catch (e: Exception) {
            e("Error deleting history entry: %s", e.message)
        }
    }

    /**
     * Delete history files for an entry
     */
    private fun deleteHistoryFiles(context: Context, entry: GameHistoryEntry) {
        try {
            // Delete map file
            deletePrivateData(context, entry.getMapPath())


            // Delete preview image if it exists
            if (entry.previewImagePath != null) {
                FileReadWrite.deletePrivateData(context, entry.previewImagePath!!)
            }
        } catch (e: Exception) {
            e("Error deleting history files: %s", e.message)
        }
    }

    /**
     * Save the history index file
     * 
     * @return
     */
    @JvmStatic
    fun saveHistoryIndex(context: Context?, entries: MutableList<GameHistoryEntry>): Boolean {
        try {
            val root = JSONObject()
            val entriesArray = JSONArray()

            for (entry in entries) {
                val entryJson = JSONObject()
                entryJson.put("mapPath", entry.getMapPath())
                entryJson.put("mapName", entry.mapName)
                entryJson.put("timestamp", entry.timestamp)
                entryJson.put("playDuration", entry.playDuration)
                entryJson.put("movesMade", entry.movesMade)
                entryJson.put("optimalMoves", entry.optimalMoves)
                entryJson.put("boardSize", entry.boardSize)
                entryJson.put("previewImagePath", entry.previewImagePath)


                // Save new fields for unique map tracking
                entryJson.put("completionCount", entry.completionCount)
                entryJson.put("lastCompletionTimestamp", entry.lastCompletionTimestamp)
                entryJson.put("bestTime", entry.bestTime)
                entryJson.put("bestMoves", entry.bestMoves)
                if (entry.wallSignature != null) {
                    entryJson.put("wallSignature", entry.wallSignature)
                }
                if (entry.positionSignature != null) {
                    entryJson.put("positionSignature", entry.positionSignature)
                }
                if (entry.mapSignature != null) {
                    entryJson.put("mapSignature", entry.mapSignature)
                }


                // Save completion timestamps array
                val timestamps = JSONArray()
                if (entry.getCompletionTimestamps() != null) {
                    for (ts in entry.getCompletionTimestamps()) {
                        timestamps.put(ts)
                    }
                }
                entryJson.put("completionTimestamps", timestamps)

                val completionMoves = JSONArray()
                if (entry.getCompletionMoves() != null) {
                    for (moves in entry.getCompletionMoves()) {
                        completionMoves.put(moves)
                    }
                }
                entryJson.put("completionMoves", completionMoves)

                val completionStars = JSONArray()
                if (entry.getCompletionStars() != null) {
                    for (stars in entry.getCompletionStars()) {
                        completionStars.put(stars)
                    }
                }
                entryJson.put("completionStars", completionStars)


                // Save stars earned
                entryJson.put("starsEarned", entry.starsEarned)


                // Save hint tracking fields
                entryJson.put("maxHintUsed", entry.maxHintUsed)
                entryJson.put("solvedWithoutHints", entry.isSolvedWithoutHints())
                entryJson.put("everUsedHints", entry.isEverUsedHints())
                entryJson.put("lastSolvedWithoutHints", entry.lastSolvedWithoutHints)
                entryJson.put(
                    "lastPerfectlySolvedWithoutHints",
                    entry.lastPerfectlySolvedWithoutHints
                )
                // Save difficulty as int ID (0-3), not localized string
                entryJson.put("difficulty", entry.difficulty)

                entriesArray.put(entryJson)
            }

            root.put("historyEntries", entriesArray)

            val isSaved = writePrivateData(context, HISTORY_INDEX_FILE, root.toString())

            d("Saved history index with %d entries", entries.size)
            return isSaved
        } catch (e: Exception) {
            e("Error saving history index: %s", e.message)
            return false
        }
    }

    /**
     * Get the next available history index
     * @param context the context
     * @return the next available index
     */
    @JvmStatic
    fun getNextHistoryIndex(context: Context): Int {
        val entries = getHistoryEntries(context)

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
    fun getHistoryIndex(context: Context, mapPath: String?): Int {
        val entries = getHistoryEntries(context)
        for (i in entries.indices) {
            if (entries.get(i).getMapPath() == mapPath) {
                return i
            }
        }
        return -1
    }

    /**
     * Konvertiert einen History-Index in einen Dateipfad
     */
    fun indexToPath(index: Int): String {
        return "history_" + index + ".txt"
    }


    /**
     * Delete a history entry by path
     * @param context The context
     * @param mapPath The map path to delete
     * @return true if the history entry was deleted successfully
     */
    @JvmStatic
    fun deleteHistoryEntry(context: Context, mapPath: String): Boolean {
        try {
            d("[HISTORY_DELETE] Attempting to delete history entry: %s", mapPath)


            // Initialize if needed
            initialize(context)


            // Extract file name from path if it's a full path
            var fileName: String? = mapPath
            if (mapPath.contains("/")) {
                fileName = mapPath.substring(mapPath.lastIndexOf("/") + 1)
            }


            // Load existing history entries
            val historyEntries = getHistoryEntries(context)
            if (historyEntries == null) {
                e("[HISTORY_DELETE] Failed to load history entries")
                return false
            }

            d(
                "[HISTORY_DELETE] Loaded %d history entries, looking for: '%s' (fileName: '%s')",
                historyEntries.size,
                mapPath,
                fileName
            )
            for (i in historyEntries.indices) {
                d("[HISTORY_DELETE] Entry %d: mapPath='%s'", i, historyEntries.get(i).getMapPath())
            }


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
                    d("Found entry to delete: %s", entry.mapName)
                    break
                }
            }

            if (entryToDelete == null) {
                e("History entry not found for path: %s", mapPath)
                return false
            }


            // Remove the entry from the list
            historyEntries.remove(entryToDelete)


            // Delete the actual file - try both the given path and the path from the entry
            var fileDeleted = false


            // Try with the mapPath parameter
            val historyFile = File(mapPath)
            if (historyFile.exists()) {
                fileDeleted = historyFile.delete()
                if (fileDeleted) {
                    d("Deleted history file at path: %s", mapPath)
                } else {
                    e("Failed to delete history file: %s", mapPath)
                }
            }


            // If that didn't work, try with the entry's path
            if (!fileDeleted && entryToDelete.getMapPath() != mapPath) {
                val entryFile = File(entryToDelete.getMapPath())
                if (entryFile.exists()) {
                    fileDeleted = entryFile.delete()
                    if (fileDeleted) {
                        d("Deleted history file at entry path: %s", entryToDelete.getMapPath())
                    } else {
                        e(
                            "Failed to delete history file at entry path: %s",
                            entryToDelete.getMapPath()
                        )
                    }
                }
            }


            // Try with the file in the history directory
            if (!fileDeleted) {
                val historyDir = File(context.filesDir, HISTORY_DIR)
                val fileInHistoryDir = File(historyDir, fileName)
                if (fileInHistoryDir.exists()) {
                    fileDeleted = fileInHistoryDir.delete()
                    if (fileDeleted) {
                        d(
                            "Deleted history file in history directory: %s",
                            fileInHistoryDir.getAbsolutePath()
                        )
                    } else {
                        e(
                            "Failed to delete history file in history directory: %s",
                            fileInHistoryDir.getAbsolutePath()
                        )
                    }
                }
            }


            // Update the history index regardless of file deletion success
            val indexUpdated = saveHistoryIndex(context, historyEntries)
            if (!indexUpdated) {
                e("Failed to update history index after deletion")
                return false
            }

            d("Successfully deleted history entry: %s", mapPath)
            return true
        } catch (e: Exception) {
            e("Error deleting history entry: %s - %s", mapPath, e.message)
            e.printStackTrace()
            return false
        }
    }

    // ========== Unique Map Tracking Methods ==========
    /**
     * Check if a map with the given signature is being completed for the first time.
     * @param activity The activity context
     * @param mapSignature The unique map signature to check
     * @return true if this map has never been completed before
     */
    @JvmStatic
    fun isFirstCompletion(context: Context, mapSignature: String?): Boolean {
        if (mapSignature == null || mapSignature.isEmpty()) {
            return true // No signature = treat as new
        }
        val existing = findByMapSignature(context, mapSignature)
        // An entry is created on the first move (before completion), so we check
        // completionCount == 0 to distinguish "started but not yet completed" from "already completed before".
        return existing == null || existing.completionCount == 0
    }

    /**
     * Find a history entry by its map signature.
     * @param context The context
     * @param mapSignature The unique map signature to find
     * @return The matching entry, or null if not found
     */
    @JvmStatic
    fun findByMapSignature(context: Context, mapSignature: String?): GameHistoryEntry? {
        if (mapSignature == null || mapSignature.isEmpty()) {
            return null
        }
        val entries = getHistoryEntries(context)
        for (entry in entries) {
            if (mapSignature == entry.mapSignature) {
                return entry
            }
        }
        return null
    }

    /**
     * Find all history entries with the same wall signature (same walls, different positions).
     * @param context The context
     * @param wallSignature The wall signature to match
     * @return List of entries with matching wall layout
     */
    @JvmStatic
    fun findByWallSignature(
        context: Context,
        wallSignature: String?
    ): MutableList<GameHistoryEntry?> {
        val result: MutableList<GameHistoryEntry?> = ArrayList<GameHistoryEntry?>()
        if (wallSignature == null || wallSignature.isEmpty()) {
            return result
        }
        val entries = getHistoryEntries(context)
        for (entry in entries) {
            if (wallSignature == entry.wallSignature) {
                result.add(entry)
            }
        }
        return result
    }

    /**
     * Get the total count of unique maps completed.
     * @param context The context
     * @return Number of unique maps in history
     */
    fun getUniqueMapCount(context: Context): Int {
        return getHistoryEntries(context).size
    }

    /**
     * Get the total count of unique completed levels from history.
     * Only entries with map names like "Level N" or matching level file paths are counted.
     * @param context The context
     * @return Number of unique completed levels in history
     */
    @JvmStatic
    fun getUniqueCompletedLevelCount(context: Context): Int {
        val entries = getHistoryEntries(context)
        val uniqueLevelKeys: MutableSet<String?> = HashSet<String?>()

        for (entry in entries) {
            val levelKey = extractLevelKey(entry)
            if (levelKey != null) {
                uniqueLevelKeys.add(levelKey)
            }
        }
        d(
            "[GAME_HISTORY][ACHIEVEMENTS][LEVEL] getUniqueCompletedLevelCount: Found %d unique levels",
            uniqueLevelKeys.size
        )
        return uniqueLevelKeys.size
    }

    /**
     * Get the total count of unique completed levels that earned at least three stars.
     * @param context The context
     * @return Number of unique 3-star levels in history
     */
    @JvmStatic
    fun getUniqueThreeStarLevelCount(context: Context): Int {
        val entries = getHistoryEntries(context)
        val uniqueLevelKeys: MutableSet<String?> = HashSet<String?>()

        for (entry in entries) {
            if (entry.starsEarned < 3) {
                continue
            }

            val levelKey = extractLevelKey(entry)
            if (levelKey != null) {
                uniqueLevelKeys.add(levelKey)
            }
        }

        d(
            "[GAME_HISTORY][ACHIEVEMENTS][LEVEL] getUniqueThreeStarLevelCount: Found %d unique 3-star levels",
            uniqueLevelKeys.size
        )
        return uniqueLevelKeys.size
    }

    private fun extractLevelKey(entry: GameHistoryEntry): String? {
        val mapName = entry.mapName
        if (mapName != null && mapName.matches("(?i)Level \\d+".toRegex())) {
            val id =
                mapName.trim { it <= ' ' }.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1].toInt()

            val levelKey = if (id >= 141) "custom_level_" + id else "level_" + id
            // Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] extractLevelKey: Found level key for entry: %s", levelKey);
            return levelKey
        }

        val mapPath = entry.getMapPath()
        if (mapPath != null) {
            val base = if (mapPath.contains("/"))
                mapPath.substring(mapPath.lastIndexOf('/') + 1)
            else
                mapPath
            if (base.startsWith("level_") || base.startsWith("custom_level_")) {
                // Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] extractLevelKey: Found level key for entry: %s", base);
                return if (base.endsWith(".txt")) base.substring(0, base.length - 4) else base
            }
        }
        // Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] extractLevelKey: No level key found for entry: %s", entry);
        return null
    }

    /**
     * Get the completion count for a specific map.
     * @param activity The activity context
     * @param mapSignature The map signature to check
     * @return Number of times this map was completed, or 0 if never
     */
    fun getCompletionCount(context: Context, mapSignature: String?): Int {
        val entry = findByMapSignature(context, mapSignature)
        return if (entry != null) entry.completionCount else 0
    }

    /**
     * Migrate old string difficulty values to int IDs.
     * Supports both English and German localized strings.
     * @param difficultyStr The old string difficulty value
     * @return The corresponding difficulty ID (0-3)
     */
    private fun migrateDifficultyStringToInt(difficultyStr: String?): Int {
        if (difficultyStr == null || difficultyStr.isEmpty()) {
            return Constants.DIFFICULTY_BEGINNER
        }

        val lower = difficultyStr.lowercase(Locale.getDefault()).trim { it <= ' ' }


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
        if (lower.substring(0, 3) == "anf") {
            return Constants.DIFFICULTY_BEGINNER
        } else if (lower.substring(0, 3) == "for") {
            return Constants.DIFFICULTY_ADVANCED
        } else if (lower.substring(0, 3) == "ver") {
            return Constants.DIFFICULTY_INSANE
        } else if (lower.substring(0, 3) == "unm") {
            return Constants.DIFFICULTY_IMPOSSIBLE
        }


        // Default fallback
        w(
            "[HISTORY_MIGRATION] Unknown difficulty string: '%s', defaulting to BEGINNER",
            difficultyStr
        )
        return Constants.DIFFICULTY_BEGINNER
    }
}
