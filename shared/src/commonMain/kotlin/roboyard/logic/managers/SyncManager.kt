package roboyard.logic.managers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import roboyard.logic.core.Constants
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.core.LevelCompletionData
import roboyard.logic.network.NetworkMonitor
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.network.RoboyardApiClient.ApiCallback
import roboyard.logic.network.optBoolean
import roboyard.logic.network.optInt
import roboyard.logic.network.optJsonArray
import roboyard.logic.network.optLong
import roboyard.logic.network.optString
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.util.RLog

/**
 * Platform-specific hooks needed by the shared SyncManager.
 */
class SyncHooks(
    /** Names of files inside the save-games directory (e.g. "save_0.dat"). */
    val listSaveFileNames: () -> List<String>,
    /** Read a bundled map asset like "level_12.txt" — used to overwrite corrupt server data. */
    val readMapAsset: (String) -> String?,
    /** Parse an ISO-8601 timestamp (e.g. "2026-03-09T08:26:56+00:00") to epoch millis. */
    val parseIsoTimestamp: (String) -> Long,
    /** Trigger an achievement upload (AchievementManager.syncToServer on the platform). */
    val syncAchievementsToServer: () -> Unit
)

/**
 * Central sync manager for uploading/downloading save games and history to/from server.
 * Handles bidirectional sync on login and periodic uploads.
 * Shared implementation used by Android and Desktop (Compose).
 */
class SyncManager private constructor(
    private val storage: PlatformStorage,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: RoboyardApiClient,
    private val hooks: SyncHooks
) {
    private var lastSyncTimestamp: Long = 0
    private val log = RLog.tag(TAG)

    /** Check if network is available. */
    val isNetworkAvailable: Boolean
        get() = networkMonitor.isNetworkAvailable()

    /**
     * Sync on app resume: upload all local data if online and logged in.
     * Called from Activity.onResume() to catch offline-to-online transitions.
     * Throttled to avoid excessive syncs.
     */
    fun syncOnResume() {
        if (!apiClient.isLoggedIn) {
            log.d("[AUTO_SYNC] Not logged in, skipping auto-sync")
            return
        }

        if (!this.isNetworkAvailable) {
            log.d("[AUTO_SYNC] No network, skipping auto-sync")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastSyncTimestamp < MIN_SYNC_INTERVAL_MS) {
            log.d("[AUTO_SYNC] Throttled - last sync was %d ms ago", now - lastSyncTimestamp)
            return
        }

        lastSyncTimestamp = now
        log.d("[AUTO_SYNC] Starting auto-sync on resume")

        // Upload achievements (includes streak data)
        hooks.syncAchievementsToServer()

        // Upload save games and history
        uploadSaveGames()
        uploadHistory()
    }

    // ========== SAVE GAMES ==========

    /** Upload all local save games to server. */
    fun uploadSaveGames() {
        if (!apiClient.isLoggedIn) {
            log.d("[SAVE_SYNC] Not logged in, skipping save game upload")
            return
        }

        try {
            val savesArray = JsonArray()
            for (name in hooks.listSaveFileNames()) {
                if (!name.startsWith(Constants.SAVE_FILENAME_PREFIX)) continue

                try {
                    // Extract slot ID from filename (save_X.dat)
                    val idStr = name
                        .replace(Constants.SAVE_FILENAME_PREFIX, "")
                        .replace(Constants.SAVE_FILENAME_EXTENSION, "")
                    val slotId = idStr.toInt()

                    // Read save data
                    val saveData = storage.readFile(Constants.SAVE_DIRECTORY + "/" + name)
                    if (saveData.isEmpty()) continue

                    val saveJson = JsonObject()
                    saveJson.addProperty("slot_id", slotId)
                    saveJson.addProperty("save_data", saveData)
                    saveJson.addProperty("map_name", extractMapName(saveData))
                    saveJson.addProperty("board_width", extractBoardWidth(saveData))
                    saveJson.addProperty("board_height", extractBoardHeight(saveData))
                    saveJson.addProperty("is_solved", saveData.contains("SOLVED:true"))

                    savesArray.add(saveJson)
                } catch (e: Exception) {
                    log.e(e, "[SAVE_SYNC] Error reading save file: %s", name)
                }
            }

            if (savesArray.size() == 0) {
                log.d("[SAVE_SYNC] No save games to upload")
                return
            }

            log.d("[SAVE_SYNC] Uploading %d save games to server", savesArray.size())
            apiClient.syncSaveGames(savesArray, object : ApiCallback<Int?> {
                override fun onSuccess(syncedCount: Int?) {
                    log.d("[SAVE_SYNC] Upload complete: %d save games synced", syncedCount)
                }

                override fun onError(error: String?) {
                    log.e("[SAVE_SYNC] Upload failed: %s", error)
                }
            })
        } catch (e: Exception) {
            log.e(e, "[SAVE_SYNC] Error uploading save games")
        }
    }

    /** Download save games from server and write to local storage. */
    fun downloadSaveGames(callback: ApiCallback<Int?>?) {
        if (!apiClient.isLoggedIn) {
            log.d("[SAVE_SYNC] Not logged in, skipping save game download")
            callback?.onError("Not logged in")
            return
        }

        apiClient.fetchSaveGames(object : ApiCallback<JsonArray?> {
            override fun onSuccess(saves: JsonArray?) {
                var restoredCount = 0

                try {
                    for (i in 0 until (saves?.size() ?: 0)) {
                        val save = saves!!.get(i).asJsonObject
                        val slotId = save.get("slot_id").asInt
                        val saveData = save.get("save_data").asString

                        val fileName = Constants.SAVE_DIRECTORY + "/" +
                            Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION

                        // Only download if local file doesn't exist (don't overwrite local saves)
                        if (!storage.fileExists(fileName)) {
                            if (storage.writeFile(fileName, saveData)) {
                                restoredCount++
                                log.d("[SAVE_SYNC] Restored save game slot %d from server", slotId)
                            }
                        } else {
                            log.d("[SAVE_SYNC] Skipping slot %d - local save exists", slotId)
                        }
                    }

                    log.d("[SAVE_SYNC] Download complete: %d save games restored", restoredCount)
                } catch (e: Exception) {
                    log.e(e, "[SAVE_SYNC] Error restoring save games")
                    callback?.onError("Error restoring saves: " + e.message)
                    return
                }

                callback?.onSuccess(restoredCount)
            }

            override fun onError(error: String?) {
                log.e("[SAVE_SYNC] Download failed: %s", error)
                callback?.onError(error)
            }
        })
    }

    // ========== GAME HISTORY ==========

    /** Callback interface for history upload completion. */
    interface HistoryUploadCallback {
        fun onSuccess(syncedCount: Int)
        fun onError(error: String?)
    }

    /** Upload all local history entries to server with callback. */
    @JvmOverloads
    fun uploadHistory(callback: HistoryUploadCallback? = null) {
        try {
            val entries = GameHistoryManager.getHistoryEntries(storage)
            uploadHistory(entries, callback)
        } catch (e: Exception) {
            log.e(e, "[HISTORY_SYNC] Error loading history entries for upload")
            callback?.onError("Failed to load history entries: " + e.message)
        }
    }

    /**
     * Upload specific history entries to server with callback.
     * This overload accepts pre-loaded entries to avoid race conditions with disk I/O.
     */
    fun uploadHistory(entries: MutableList<GameHistoryEntry>?, callback: HistoryUploadCallback?) {
        if (!apiClient.isLoggedIn) {
            log.d("[HISTORY_SYNC] Not logged in, skipping history upload")
            callback?.onError("Not logged in")
            return
        }

        try {
            if (entries.isNullOrEmpty()) {
                log.d("[HISTORY_SYNC] No history entries to upload")
                callback?.onSuccess(0)
                return
            }

            val historyArray = JsonArray()

            for (entry in entries) {
                // Only upload entries that were actually played (has stars or moves)
                // to prevent overwriting server data with empty entries
                if (entry.starsEarned == 0 && entry.movesMade == 0) {
                    log.d(
                        "[HISTORY_SYNC] Skipping unplayed entry: %s (stars=0, moves=0)",
                        entry.mapName
                    )
                    continue
                }

                // Read the actual map data from the history file
                val mapData = readHistoryFileData(entry)
                if (mapData.isNullOrEmpty()) continue

                val historyJson = JsonObject()
                historyJson.addProperty("map_name", entry.mapName)
                historyJson.addProperty("save_data", mapData)
                historyJson.addProperty("board_width", extractBoardWidthFromSize(entry.boardSize))
                historyJson.addProperty("board_height", extractBoardHeightFromSize(entry.boardSize))
                historyJson.addProperty("move_count", entry.movesMade)
                historyJson.addProperty("optimal_moves", entry.optimalMoves)
                historyJson.addProperty("max_hint_used", entry.maxHintUsed)
                historyJson.addProperty("ever_used_hints", entry.isEverUsedHints())
                historyJson.addProperty("solved_without_hints", entry.isSolvedWithoutHints())
                historyJson.addProperty("last_solved_without_hints", entry.lastSolvedWithoutHints)
                historyJson.addProperty(
                    "last_perfectly_solved_without_hints",
                    entry.lastPerfectlySolvedWithoutHints
                )
                historyJson.addProperty("is_solved", entry.movesMade > 0)
                historyJson.addProperty("play_time_seconds", entry.playDuration)
                historyJson.addProperty("stars_earned", entry.starsEarned)
                // CRITICAL: Send played_at in UTC timezone to prevent timezone offset issues
                val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                utcFormat.timeZone = TimeZone.getTimeZone("UTC")
                historyJson.addProperty("played_at", utcFormat.format(Date(entry.timestamp)))
                historyJson.addProperty("best_time", entry.bestTime)
                historyJson.addProperty("best_moves", entry.bestMoves)
                historyJson.addProperty("completion_count", entry.completionCount)
                historyJson.addProperty("last_completion_timestamp", entry.lastCompletionTimestamp)
                val tsArray = JsonArray()
                entry.getCompletionTimestamps()?.forEach { tsArray.add(it) }
                historyJson.add("completion_timestamps", tsArray)
                // [HISTORY_SYNC] Upload per-completion moves and stars arrays so receiving
                // devices can show the correct values for each completion (otherwise arrays
                // get out of sync with timestamps after multi-device play)
                val movesArray = JsonArray()
                entry.getCompletionMoves()?.forEach { movesArray.add(it) }
                historyJson.add("completion_moves", movesArray)
                val starsArray = JsonArray()
                entry.getCompletionStars()?.forEach { starsArray.add(it) }
                historyJson.add("completion_stars", starsArray)

                // Log timestamps with human-readable format for debugging timezone issues
                val playedAtStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    .format(Date(entry.timestamp))
                val lastCompletionStr = if (entry.lastCompletionTimestamp > 0)
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        .format(Date(entry.lastCompletionTimestamp))
                else
                    "never"
                log.d(
                    "[HISTORY_SYNC] Uploading: %s (moves=%d, optimal=%d, maxHint=%d, everHints=%b, stars=%d)",
                    entry.mapName, entry.movesMade, entry.optimalMoves,
                    entry.maxHintUsed, entry.isEverUsedHints(), entry.starsEarned
                )
                log.d(
                    "[HISTORY_SYNC_TIME] Upload timestamps - played_at='%s' (millis=%d), last_completion='%s' (millis=%d)",
                    playedAtStr, entry.timestamp, lastCompletionStr, entry.lastCompletionTimestamp
                )

                historyArray.add(historyJson)
            }

            if (historyArray.size() == 0) {
                log.d("[HISTORY_SYNC] No valid history entries to upload")
                return
            }

            log.d("[HISTORY_SYNC] Uploading %d history entries to server", historyArray.size())
            apiClient.syncHistory(historyArray, object : ApiCallback<JsonObject?> {
                override fun onSuccess(response: JsonObject?) {
                    val syncedCount = response?.optInt("synced_count") ?: 0
                    val skippedCount = response?.optInt("skipped_count") ?: 0
                    val totalEntries = response?.optInt("total_entries") ?: 0

                    log.d(
                        "[HISTORY_SYNC] ✓ Upload complete: synced=%d, skipped=%d, total=%d",
                        syncedCount, skippedCount, totalEntries
                    )

                    // All skipped = data already in sync on server, treat as success
                    if (syncedCount == 0 && totalEntries > 0) {
                        log.d(
                            "[HISTORY_SYNC] All %d entries already in sync (no changes needed)",
                            skippedCount
                        )
                    }

                    callback?.onSuccess(syncedCount)
                }

                override fun onError(error: String?) {
                    log.e("[HISTORY_SYNC] ✗ Upload failed: %s", error)

                    // If unauthorized, try to re-login once and retry
                    if (error != null && error.lowercase(Locale.getDefault()).contains("unauthorized")) {
                        log.d("[HISTORY_SYNC] Attempting auto re-login after 401...")
                        apiClient.attemptReLogin(object : ApiCallback<Boolean?> {
                            override fun onSuccess(reLoginSuccess: Boolean?) {
                                if (reLoginSuccess == true) {
                                    log.d("[HISTORY_SYNC] Re-login successful, retrying upload...")
                                    // Retry the upload with the same entries
                                    uploadHistory(entries, callback)
                                } else {
                                    log.e("[HISTORY_SYNC] Re-login failed, user needs to login manually")
                                    callback?.onError("Not logged in - please login again")
                                }
                            }

                            override fun onError(reLoginError: String?) {
                                log.e("[HISTORY_SYNC] Re-login error: %s", reLoginError)
                                callback?.onError("Not logged in - please login again")
                            }
                        })
                    } else {
                        // Not an auth error, just pass it through
                        callback?.onError(error)
                    }
                }
            })
        } catch (e: Exception) {
            log.e(e, "[HISTORY_SYNC] Error uploading history")
        }
    }

    /** Download history entries from server and write to local storage. */
    fun downloadHistory(callback: ApiCallback<Int?>?) {
        log.d("[HISTORY_SYNC] downloadHistory called")
        if (!apiClient.isLoggedIn) {
            log.d("[HISTORY_SYNC] Not logged in, skipping history download")
            callback?.onError("Not logged in")
            return
        }

        log.d("[HISTORY_SYNC] Logged in, fetching history from server")
        apiClient.fetchHistory(object : ApiCallback<JsonArray?> {
            override fun onSuccess(history: JsonArray?) {
                var restoredCount = 0

                try {
                    GameHistoryManager.initialize(storage)
                    val existingEntries = GameHistoryManager.getHistoryEntries(storage)

                    for (i in 0 until (history?.size() ?: 0)) {
                        val entry = history!!.get(i).asJsonObject
                        val mapName = entry.optString("map_name") ?: "Unnamed"
                        val saveData = entry.get("save_data").asString

                        // VALIDATION: Fix corrupt mapPath values where mapName is "Level X" but mapPath doesn't match
                        // This prevents corrupt data from server from causing incorrect minimaps
                        var expectedMapPath: String? = null
                        if (mapName.matches("Level \\d+".toRegex())) {
                            expectedMapPath = "level_" + mapName.substring(6) + ".txt"
                        }

                        // Check if we already have this entry locally (by map name)
                        var exists = false
                        for (existing in existingEntries) {
                            if (existing.mapName == mapName) {
                                exists = true
                                break
                            }
                        }

                        // For built-in levels, always overwrite the file with correct data from assets
                        // to fix corrupt server data. For custom levels, only create if not exists.
                        if (!exists || expectedMapPath != null) {
                            // Save the map data to a history file
                            // For built-in levels (Level X), use the expected mapPath (level_X.txt)
                            // For custom levels, use the standard history path (history_X.txt)
                            val historyPath: String
                            if (expectedMapPath != null) {
                                historyPath = expectedMapPath
                                log.d(
                                    "[HISTORY_SYNC] Using expected mapPath for built-in level: %s",
                                    historyPath
                                )
                            } else {
                                val nextIndex = GameHistoryManager.getNextHistoryIndex(storage)
                                historyPath = GameHistoryManager.indexToPath(nextIndex)
                            }

                            // For built-in levels, ALWAYS load the correct level data from assets instead of using corrupt server saveData
                            // This ensures corrupt files are overwritten on sync
                            var dataToWrite = saveData
                            if (expectedMapPath != null) {
                                try {
                                    val assetData = hooks.readMapAsset(expectedMapPath)
                                    if (assetData != null) {
                                        dataToWrite = assetData
                                        log.d(
                                            "[HISTORY_SYNC] Loaded level data from assets for %s instead of server data",
                                            expectedMapPath
                                        )
                                    }
                                } catch (e: Exception) {
                                    log.w(
                                        "[HISTORY_SYNC] Failed to load level data from assets for %s (%s), using server data",
                                        expectedMapPath, e.message
                                    )
                                }
                            }

                            storage.writeFile(historyPath, dataToWrite)

                            // Create a history entry
                            val historyEntry = GameHistoryEntry()
                            historyEntry.setMapPath(historyPath)
                            historyEntry.mapName = mapName
                            historyEntry.timestamp = parseTimestamp(entry.optString("played_at"))
                            historyEntry.playDuration = entry.optInt("play_time_seconds", 0)
                            historyEntry.movesMade = entry.optInt("move_count", 0)
                            historyEntry.optimalMoves = entry.optInt("optimal_moves", 0)
                            historyEntry.maxHintUsed = entry.optInt("max_hint_used", -1)
                            historyEntry.setEverUsedHints(entry.optBoolean("ever_used_hints"))
                            historyEntry.setSolvedWithoutHints(entry.optBoolean("solved_without_hints"))
                            historyEntry.lastSolvedWithoutHints =
                                entry.optLong("last_solved_without_hints", 0)
                            historyEntry.lastPerfectlySolvedWithoutHints =
                                entry.optLong("last_perfectly_solved_without_hints", 0)
                            historyEntry.boardSize = entry.optInt("board_width", 12)
                                .toString() + "x" + entry.optInt("board_height", 12)
                            historyEntry.previewImagePath = ""
                            historyEntry.starsEarned = entry.optInt("stars_earned", 0)
                            historyEntry.bestTime = entry.optInt("best_time", 0)
                            historyEntry.bestMoves = entry.optInt("best_moves", 0)
                            historyEntry.completionCount = entry.optInt("completion_count", 0)
                            historyEntry.lastCompletionTimestamp =
                                entry.optLong("last_completion_timestamp", 0)
                            val tsArrayDl = entry.optJsonArray("completion_timestamps")
                            if (tsArrayDl != null) {
                                val timestamps = mutableListOf<Long>()
                                for (j in 0 until tsArrayDl.size()) {
                                    timestamps.add(tsArrayDl.get(j).asLong)
                                }
                                historyEntry.setCompletionTimestamps(timestamps)
                            }
                            // [HISTORY_SYNC] Restore per-completion moves and stars arrays
                            // to keep them aligned with completion_timestamps
                            val movesArrayDl = entry.optJsonArray("completion_moves")
                            if (movesArrayDl != null) {
                                val movesList = mutableListOf<Int>()
                                for (j in 0 until movesArrayDl.size()) {
                                    movesList.add(movesArrayDl.get(j).asInt)
                                }
                                historyEntry.setCompletionMoves(movesList)
                            }
                            val starsArrayDl = entry.optJsonArray("completion_stars")
                            if (starsArrayDl != null) {
                                val starsList = mutableListOf<Int>()
                                for (j in 0 until starsArrayDl.size()) {
                                    starsList.add(starsArrayDl.get(j).asInt)
                                }
                                historyEntry.setCompletionStars(starsList)
                            }

                            GameHistoryManager.addHistoryEntry(storage, historyEntry)
                            restoredCount++

                            // Log timestamps with human-readable format for debugging timezone issues
                            val downloadedPlayedAt = entry.optString("played_at") ?: "null"
                            val parsedTimestampStr =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                    .format(Date(historyEntry.timestamp))
                            val lastCompletionStrDl = if (historyEntry.lastCompletionTimestamp > 0)
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                    .format(Date(historyEntry.lastCompletionTimestamp))
                            else
                                "never"
                            log.d("[HISTORY_SYNC] Restored history entry: %s", mapName)
                            log.d(
                                "[HISTORY_SYNC_TIME] Download timestamps - server_played_at='%s', parsed_timestamp='%s' (millis=%d), last_completion='%s' (millis=%d)",
                                downloadedPlayedAt,
                                parsedTimestampStr,
                                historyEntry.timestamp,
                                lastCompletionStrDl,
                                historyEntry.lastCompletionTimestamp
                            )
                        }
                    }

                    log.d(
                        "[HISTORY_SYNC] Download complete: %d history entries restored",
                        restoredCount
                    )

                    // Restore level stars from ALL history entries to LevelCompletionManager
                    restoreLevelStarsFromHistory(history ?: JsonArray())
                } catch (e: Exception) {
                    log.e(e, "[HISTORY_SYNC] Error restoring history")
                    callback?.onError("Error restoring history: " + e.message)
                    return
                }

                callback?.onSuccess(restoredCount)
            }

            override fun onError(error: String?) {
                log.e("[HISTORY_SYNC] Download failed: %s", error)
                callback?.onError(error)
            }
        })
    }

    // ========== FULL SYNC ON LOGIN ==========

    /**
     * Perform full sync after login: download everything from server, then upload local data.
     */
    fun fullSyncOnLogin(callback: ApiCallback<String?>?) {
        log.d("[FULL_SYNC] Starting full sync after login")

        // Step 1: Download save games
        downloadSaveGames(object : ApiCallback<Int?> {
            override fun onSuccess(savesRestored: Int?) {
                log.d("[FULL_SYNC] Save games downloaded: %d restored", savesRestored)

                // Step 2: Download history
                downloadHistory(object : ApiCallback<Int?> {
                    override fun onSuccess(historyRestored: Int?) {
                        log.d("[FULL_SYNC] History downloaded: %d restored", historyRestored)

                        // Count level entries separately
                        var levelsRestored = 0
                        try {
                            val allEntries = GameHistoryManager.getHistoryEntries(storage)
                            for (entry in allEntries) {
                                if (entry.mapName != null && entry.mapName!!.startsWith("Level ")) {
                                    levelsRestored++
                                }
                            }
                        } catch (e: Exception) {
                            log.e(e, "[FULL_SYNC] Error counting level entries")
                        }

                        val randomMapsRestored = (historyRestored ?: 0) - levelsRestored

                        // Step 3: Upload local data to server
                        uploadSaveGames()
                        uploadHistory()

                        var summary =
                            savesRestored.toString() + " saves, " + levelsRestored + " levels restored"
                        if (randomMapsRestored > 0) {
                            summary += ", " + randomMapsRestored + " random maps"
                        }
                        log.d("[FULL_SYNC] Full sync complete: %s", summary)
                        callback?.onSuccess(summary)
                    }

                    override fun onError(error: String?) {
                        log.e("[FULL_SYNC] History download failed: %s", error)
                        // Still try to upload local data
                        uploadSaveGames()
                        uploadHistory()
                        callback?.onSuccess(savesRestored.toString() + " saves restored (history failed)")
                    }
                })
            }

            override fun onError(error: String?) {
                log.e("[FULL_SYNC] Save game download failed: %s", error)
                // Still try history
                downloadHistory(object : ApiCallback<Int?> {
                    override fun onSuccess(historyRestored: Int?) {
                        uploadSaveGames()
                        uploadHistory()
                        callback?.onSuccess(historyRestored.toString() + " history entries restored (saves failed)")
                    }

                    override fun onError(histError: String?) {
                        callback?.onError("Sync failed: saves=" + error + ", history=" + histError)
                    }
                })
            }
        })
    }

    // ========== HELPER METHODS ==========

    private fun readHistoryFileData(entry: GameHistoryEntry): String? {
        return try {
            storage.readFile(entry.getMapPath())
        } catch (e: Exception) {
            log.e(e, "[HISTORY_SYNC] Error reading history file: %s", entry.getMapPath())
            null
        }
    }

    private fun parseTimestamp(isoTimestamp: String?): Long {
        if (isoTimestamp.isNullOrEmpty()) {
            return System.currentTimeMillis()
        }
        return try {
            val millis = hooks.parseIsoTimestamp(isoTimestamp)
            val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            utcFormat.timeZone = TimeZone.getTimeZone("UTC")
            val localFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            log.d(
                "[HISTORY_SYNC_TIME] parseTimestamp: input='%s' → millis=%d → UTC='%s', local='%s'",
                isoTimestamp, millis,
                utcFormat.format(Date(millis)),
                localFormat.format(Date(millis))
            )
            millis
        } catch (e: Exception) {
            log.e(e, "[HISTORY_SYNC_TIME] Failed to parse timestamp: %s", isoTimestamp)
            System.currentTimeMillis()
        }
    }

    private fun extractMapName(saveData: String): String? {
        // Try to extract NAME:xxx; from save data
        val nameStart = saveData.indexOf("NAME:")
        if (nameStart >= 0) {
            val nameEnd = saveData.indexOf(";", nameStart)
            if (nameEnd > nameStart) {
                return saveData.substring(nameStart + 5, nameEnd)
            }
        }
        return null
    }

    private fun extractBoardWidth(saveData: String): Int =
        extractSizeComponent(saveData, 0, 12)

    private fun extractBoardHeight(saveData: String): Int =
        extractSizeComponent(saveData, 1, 12)

    private fun extractSizeComponent(saveData: String, index: Int, defaultValue: Int): Int {
        val sizeStart = saveData.indexOf("SIZE:")
        if (sizeStart >= 0) {
            val sizeEnd = saveData.indexOf(";", sizeStart)
            if (sizeEnd > sizeStart) {
                val sizeStr = saveData.substring(sizeStart + 5, sizeEnd)
                val parts = sizeStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                if (parts.size > index) {
                    return try {
                        parts[index].trim().toInt()
                    } catch (e: NumberFormatException) {
                        defaultValue
                    }
                }
            }
        }
        return defaultValue
    }

    /**
     * Restore level stars from downloaded history entries to LevelCompletionManager.
     * Parses "Level X" from map_name and sets stars in the level completion data.
     * Only updates if the downloaded stars are better than what's already stored.
     */
    private fun restoreLevelStarsFromHistory(history: JsonArray) {
        try {
            val lcm = LevelCompletionManager.getInstance(storage)
            var restoredLevels = 0

            for (i in 0 until history.size()) {
                val entry = history.get(i).asJsonObject
                val mapName = entry.optString("map_name") ?: ""
                val stars = entry.optInt("stars_earned", 0)
                val moves = entry.optInt("move_count", 0)

                if (!mapName.startsWith("Level ") || stars <= 0) continue

                // Parse level number from "Level X"
                val levelId: Int = try {
                    mapName.substring(6).trim().toInt()
                } catch (e: NumberFormatException) {
                    continue
                }

                val optimalMoves = entry.optInt("optimal_moves", 0)
                val maxHintUsed = entry.optInt("max_hint_used", -1)
                val hintsShown = if (maxHintUsed >= 0) maxHintUsed + 1 else 0

                val existing = lcm.getLevelCompletionData(levelId)
                // Update if stars improved OR if we have new optimal_moves data for existing level
                val starsImproved = stars > existing!!.getStars()
                val hasNewMetadata = optimalMoves > 0 && existing.optimalMoves == 0

                if (starsImproved || hasNewMetadata) {
                    val data = LevelCompletionData(levelId)
                    data.setCompleted(true)
                    data.setStars(stars)
                    if (moves > 0) {
                        data.movesNeeded = moves
                    }
                    if (optimalMoves > 0) {
                        data.optimalMoves = optimalMoves
                    }
                    data.hintsShown = hintsShown
                    lcm.saveLevelCompletionData(data)
                    restoredLevels++
                    log.d(
                        "[HISTORY_SYNC] Restored level %d stars: %d (moves=%d, optimal=%d, maxHint=%d)",
                        levelId, stars, moves, optimalMoves, maxHintUsed
                    )
                }
            }

            log.d("[HISTORY_SYNC] Level stars restoration complete: %d levels updated", restoredLevels)
        } catch (e: Exception) {
            log.e(e, "[HISTORY_SYNC] Error restoring level stars from history")
        }
    }

    private fun extractBoardWidthFromSize(boardSize: String?): Int {
        if (boardSize != null && boardSize.contains("x")) {
            return try {
                boardSize.split("x".toRegex()).dropLastWhile { it.isEmpty() }[0].trim().toInt()
            } catch (e: NumberFormatException) {
                12
            }
        }
        return 12
    }

    private fun extractBoardHeightFromSize(boardSize: String?): Int {
        if (boardSize != null && boardSize.contains("x")) {
            return try {
                boardSize.split("x".toRegex()).dropLastWhile { it.isEmpty() }[1].trim().toInt()
            } catch (e: NumberFormatException) {
                12
            }
        }
        return 12
    }

    companion object {
        private const val TAG = "SyncManager"
        private const val MIN_SYNC_INTERVAL_MS: Long = 60000 // 1 minute between auto-syncs

        @Volatile
        private var instance: SyncManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(
            storage: PlatformStorage,
            networkMonitor: NetworkMonitor,
            apiClient: RoboyardApiClient,
            hooks: SyncHooks
        ): SyncManager {
            if (instance == null) {
                instance = SyncManager(storage, networkMonitor, apiClient, hooks)
            }
            return instance!!
        }

        /** Returns the already-initialized instance, or null if not yet created. */
        @JvmStatic
        fun getExistingInstance(): SyncManager? = instance

        /** Test hook: reset the singleton. */
        @JvmStatic
        fun resetInstance() {
            instance = null
        }
    }
}
