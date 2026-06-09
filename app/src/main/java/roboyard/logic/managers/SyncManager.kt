package roboyard.logic.managers

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.core.Constants
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.core.LevelCompletionData
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.network.RoboyardApiClient.ApiCallback
import roboyard.logic.storage.FileReadWrite.Companion.readPrivateData
import roboyard.logic.storage.FileReadWrite.Companion.writePrivateData
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.w
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.Scanner
import java.util.TimeZone

/**
 * Central sync manager for uploading/downloading save games and history to/from server.
 * Handles bidirectional sync on login and periodic uploads.
 */
class SyncManager private constructor(context: Context) {
    private val context: Context
    private var lastSyncTimestamp: Long = 0

    init {
        this.context = context.getApplicationContext()
    }

    val isNetworkAvailable: Boolean
        /**
         * Check if network is available.
         */
        get() {
            val cm =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (cm == null) return false
            val activeNetwork = cm.getActiveNetworkInfo()
            return activeNetwork != null && activeNetwork.isConnected()
        }

    /**
     * Sync on app resume: upload all local data if online and logged in.
     * Called from Activity.onResume() to catch offline-to-online transitions.
     * Throttled to avoid excessive syncs.
     */
    fun syncOnResume(context: Context? = null) {
        val apiClient = RoboyardApiClient.getInstance(this.context)
        if (!apiClient.isLoggedIn) {
            d("[AUTO_SYNC] Not logged in, skipping auto-sync")
            return
        }

        if (!this.isNetworkAvailable) {
            d("[AUTO_SYNC] No network, skipping auto-sync")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastSyncTimestamp < MIN_SYNC_INTERVAL_MS) {
            d("[AUTO_SYNC] Throttled - last sync was %d ms ago", now - lastSyncTimestamp)
            return
        }

        lastSyncTimestamp = now
        d("[AUTO_SYNC] Starting auto-sync on resume")


        // Upload achievements (includes streak data)
        AchievementManager.getInstance(this.context).syncToServer()


        // Upload save games and history
        uploadSaveGames()
        uploadHistory()
    }

    // ========== SAVE GAMES ==========
    /**
     * Upload all local save games to server.
     */
    fun uploadSaveGames() {
        val apiClient = RoboyardApiClient.getInstance(context)
        if (!apiClient.isLoggedIn) {
            d("[SAVE_SYNC] Not logged in, skipping save game upload")
            return
        }

        try {
            val saveDir = File(context.getFilesDir(), Constants.SAVE_DIRECTORY)
            if (!saveDir.exists()) {
                d("[SAVE_SYNC] No save directory, nothing to upload")
                return
            }

            val savesArray = JSONArray()
            val saveFiles = saveDir.listFiles()
            if (saveFiles == null) return

            for (saveFile in saveFiles) {
                if (!saveFile.getName().startsWith(Constants.SAVE_FILENAME_PREFIX)) continue

                try {
                    // Extract slot ID from filename (save_X.dat)
                    val name = saveFile.getName()
                    val idStr = name.replace(Constants.SAVE_FILENAME_PREFIX, "")
                        .replace(Constants.SAVE_FILENAME_EXTENSION, "")
                    val slotId = idStr.toInt()


                    // Read save data
                    val saveData = readFileContent(saveFile)
                    if (saveData == null || saveData.isEmpty()) continue

                    val saveJson = JSONObject()
                    saveJson.put("slot_id", slotId)
                    saveJson.put("save_data", saveData)
                    saveJson.put("map_name", extractMapName(saveData))
                    saveJson.put("board_width", extractBoardWidth(saveData))
                    saveJson.put("board_height", extractBoardHeight(saveData))
                    saveJson.put("is_solved", saveData.contains("SOLVED:true"))

                    savesArray.put(saveJson)
                } catch (e: Exception) {
                    e(e, "[SAVE_SYNC] Error reading save file: %s", saveFile.getName())
                }
            }

            if (savesArray.length() == 0) {
                d("[SAVE_SYNC] No save games to upload")
                return
            }

            d("[SAVE_SYNC] Uploading %d save games to server", savesArray.length())
            apiClient.syncSaveGames(savesArray, object : ApiCallback<Int?> {
                override fun onSuccess(syncedCount: Int?) {
                    d("[SAVE_SYNC] Upload complete: %d save games synced", syncedCount)
                }

                override fun onError(error: String?) {
                    e("[SAVE_SYNC] Upload failed: %s", error)
                }
            })
        } catch (e: Exception) {
            e(e, "[SAVE_SYNC] Error uploading save games")
        }
    }

    /**
     * Download save games from server and write to local storage.
     */
    fun downloadSaveGames(callback: ApiCallback<Int?>?) {
        val apiClient = RoboyardApiClient.getInstance(context)
        if (!apiClient.isLoggedIn) {
            d("[SAVE_SYNC] Not logged in, skipping save game download")
            if (callback != null) callback.onError("Not logged in")
            return
        }

        apiClient.fetchSaveGames(object : ApiCallback<JSONArray?> {
            override fun onSuccess(saves: JSONArray?) {
                var restoredCount = 0

                try {
                    val saveDir = File(context.getFilesDir(), Constants.SAVE_DIRECTORY)
                    if (!saveDir.exists()) {
                        saveDir.mkdirs()
                    }

                    for (i in 0..<(saves?.length() ?: 0)) {
                        val save = saves!!.getJSONObject(i)
                        val slotId = save.getInt("slot_id")
                        val saveData = save.getString("save_data")

                        val fileName =
                            Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION
                        val saveFile = File(saveDir, fileName)


                        // Only download if local file doesn't exist (don't overwrite local saves)
                        if (!saveFile.exists()) {
                            FileOutputStream(saveFile).use { fos ->
                                fos.write(saveData.toByteArray(StandardCharsets.UTF_8))
                                restoredCount++
                                d("[SAVE_SYNC] Restored save game slot %d from server", slotId)
                            }
                        } else {
                            d("[SAVE_SYNC] Skipping slot %d - local save exists", slotId)
                        }
                    }

                    d("[SAVE_SYNC] Download complete: %d save games restored", restoredCount)
                } catch (e: JSONException) {
                    e(e, "[SAVE_SYNC] Error restoring save games")
                    if (callback != null) callback.onError("Error restoring saves: " + e.message)
                    return
                } catch (e: IOException) {
                    e(e, "[SAVE_SYNC] Error restoring save games")
                    if (callback != null) callback.onError("Error restoring saves: " + e.message)
                    return
                }

                if (callback != null) callback.onSuccess(restoredCount)
            }

            override fun onError(error: String?) {
                e("[SAVE_SYNC] Download failed: %s", error)
                if (callback != null) callback.onError(error)
            }
        })
    }

    // ========== GAME HISTORY ==========
    /**
     * Callback interface for history upload completion.
     */
    interface HistoryUploadCallback {
        fun onSuccess(syncedCount: Int)
        fun onError(error: String?)
    }

    /**
     * Upload all local history entries to server with callback.
     */
    /**
     * Upload all local history entries to server.
     */
    @JvmOverloads
    fun uploadHistory(context: Context? = null, callback: HistoryUploadCallback? = null) {
        try {
            val entries = GameHistoryManager.getHistoryEntries(this.context)
            uploadHistory(this.context, entries, callback)
        } catch (e: Exception) {
            e(e, "[HISTORY_SYNC] Error loading history entries for upload")
            if (callback != null) {
                callback.onError("Failed to load history entries: " + e.message)
            }
        }
    }

    /**
     * Upload specific history entries to server with callback.
     * This overload accepts pre-loaded entries to avoid race conditions with disk I/O.
     */
    fun uploadHistory(
        context: Context?,
        entries: MutableList<GameHistoryEntry>?,
        callback: HistoryUploadCallback?
    ) {
        val apiClient = RoboyardApiClient.getInstance(this.context)
        if (!apiClient.isLoggedIn) {
            d("[HISTORY_SYNC] Not logged in, skipping history upload")
            if (callback != null) callback.onError("Not logged in")
            return
        }

        try {
            if (entries == null || entries.isEmpty()) {
                d("[HISTORY_SYNC] No history entries to upload")
                if (callback != null) callback.onSuccess(0)
                return
            }

            val historyArray = JSONArray()

            for (entry in entries) {
                // Only upload entries that were actually played (has stars or moves)
                // to prevent overwriting server data with empty entries
                if (entry.starsEarned == 0 && entry.movesMade == 0) {
                    d(
                        "[HISTORY_SYNC] Skipping unplayed entry: %s (stars=0, moves=0)",
                        entry.mapName
                    )
                    continue
                }


                // Read the actual map data from the history file
                val mapData = readHistoryFileData(this.context, entry)
                if (mapData == null || mapData.isEmpty()) continue

                val historyJson = JSONObject()
                historyJson.put("map_name", entry.mapName)
                historyJson.put("save_data", mapData)
                historyJson.put("board_width", extractBoardWidthFromSize(entry.boardSize))
                historyJson.put("board_height", extractBoardHeightFromSize(entry.boardSize))
                historyJson.put("move_count", entry.movesMade)
                historyJson.put("optimal_moves", entry.optimalMoves)
                historyJson.put("max_hint_used", entry.maxHintUsed)
                historyJson.put("ever_used_hints", entry.isEverUsedHints())
                historyJson.put("solved_without_hints", entry.isSolvedWithoutHints())
                historyJson.put("last_solved_without_hints", entry.lastSolvedWithoutHints)
                historyJson.put(
                    "last_perfectly_solved_without_hints",
                    entry.lastPerfectlySolvedWithoutHints
                )
                historyJson.put("is_solved", entry.movesMade > 0)
                historyJson.put("play_time_seconds", entry.playDuration)
                historyJson.put("stars_earned", entry.starsEarned)
                // CRITICAL: Send played_at in UTC timezone to prevent timezone offset issues
                val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                historyJson.put("played_at", utcFormat.format(Date(entry.timestamp)))
                historyJson.put("best_time", entry.bestTime)
                historyJson.put("best_moves", entry.bestMoves)
                historyJson.put("completion_count", entry.completionCount)
                historyJson.put("last_completion_timestamp", entry.lastCompletionTimestamp)
                val tsArray = JSONArray()
                if (entry.getCompletionTimestamps() != null) {
                    for (ts in entry.getCompletionTimestamps()) {
                        tsArray.put(ts)
                    }
                }
                historyJson.put("completion_timestamps", tsArray)
                // [HISTORY_SYNC] Upload per-completion moves and stars arrays so receiving
                // devices can show the correct values for each completion (otherwise arrays
                // get out of sync with timestamps after multi-device play)
                val movesArray = JSONArray()
                if (entry.getCompletionMoves() != null) {
                    for (moves in entry.getCompletionMoves()) {
                        movesArray.put(moves)
                    }
                }
                historyJson.put("completion_moves", movesArray)
                val starsArray = JSONArray()
                if (entry.getCompletionStars() != null) {
                    for (stars in entry.getCompletionStars()) {
                        starsArray.put(stars)
                    }
                }
                historyJson.put("completion_stars", starsArray)


                // Log timestamps with human-readable format for debugging timezone issues
                val playedAtStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(
                    Date(entry.timestamp)
                )
                val lastCompletionStr = if (entry.lastCompletionTimestamp > 0)
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                    ).format(Date(entry.lastCompletionTimestamp))
                else
                    "never"
                d(
                    "[HISTORY_SYNC] Uploading: %s (moves=%d, optimal=%d, maxHint=%d, everHints=%b, stars=%d)",
                    entry.mapName, entry.movesMade, entry.optimalMoves,
                    entry.maxHintUsed, entry.isEverUsedHints(), entry.starsEarned
                )
                d(
                    "[HISTORY_SYNC_TIME] Upload timestamps - played_at='%s' (millis=%d), last_completion='%s' (millis=%d)",
                    playedAtStr, entry.timestamp, lastCompletionStr, entry.lastCompletionTimestamp
                )

                historyArray.put(historyJson)
            }

            if (historyArray.length() == 0) {
                d("[HISTORY_SYNC] No valid history entries to upload")
                return
            }

            d("[HISTORY_SYNC] Uploading %d history entries to server", historyArray.length())
            apiClient.syncHistory(historyArray, object : ApiCallback<JSONObject?> {
                override fun onSuccess(response: JSONObject?) {
                    val syncedCount = response?.optInt("synced_count", 0) ?: 0
                    val skippedCount = response?.optInt("skipped_count", 0) ?: 0
                    val totalEntries = response?.optInt("total_entries", 0) ?: 0

                    d(
                        "[HISTORY_SYNC] ✓ Upload complete: synced=%d, skipped=%d, total=%d",
                        syncedCount, skippedCount, totalEntries
                    )


                    // All skipped = data already in sync on server, treat as success
                    if (syncedCount == 0 && totalEntries > 0) {
                        d(
                            "[HISTORY_SYNC] All %d entries already in sync (no changes needed)",
                            skippedCount
                        )
                    }

                    if (callback != null) {
                        callback.onSuccess(syncedCount)
                    }
                }

                override fun onError(error: String?) {
                    e("[HISTORY_SYNC] ✗ Upload failed: %s", error)


                    // If unauthorized, try to re-login once and retry
                    if (error != null && error.lowercase(Locale.getDefault())
                            .contains("unauthorized")
                    ) {
                        d("[HISTORY_SYNC] Attempting auto re-login after 401...")
                        apiClient.attemptReLogin(object : ApiCallback<Boolean?> {
                            override fun onSuccess(reLoginSuccess: Boolean?) {
                                if (reLoginSuccess == true) {
                                    d("[HISTORY_SYNC] Re-login successful, retrying upload...")
                                    // Retry the upload with the same entries
                                    uploadHistory(context, entries, callback)
                                } else {
                                    e("[HISTORY_SYNC] Re-login failed, user needs to login manually")
                                    if (callback != null) {
                                        callback.onError("Not logged in - please login again")
                                    }
                                }
                            }

                            override fun onError(reLoginError: String?) {
                                e("[HISTORY_SYNC] Re-login error: %s", reLoginError)
                                if (callback != null) {
                                    callback.onError("Not logged in - please login again")
                                }
                            }
                        })
                    } else {
                        // Not an auth error, just pass it through
                        if (callback != null) {
                            callback.onError(error)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e(e, "[HISTORY_SYNC] Error uploading history")
        }
    }

    /**
     * Download history entries from server and write to local storage.
     */
    fun downloadHistory(context: Context, callback: ApiCallback<Int?>?) {
        d("[HISTORY_SYNC] downloadHistory called")
        val apiClient = RoboyardApiClient.getInstance(this.context)
        if (!apiClient.isLoggedIn) {
            d("[HISTORY_SYNC] Not logged in, skipping history download")
            if (callback != null) callback.onError("Not logged in")
            return
        }

        d("[HISTORY_SYNC] Logged in, fetching history from server")
        apiClient.fetchHistory(object : ApiCallback<JSONArray?> {
            override fun onSuccess(history: JSONArray?) {
                var restoredCount = 0

                try {
                    GameHistoryManager.initialize(this@SyncManager.context)
                    val existingEntries = GameHistoryManager.getHistoryEntries(this@SyncManager.context)

                    for (i in 0..<(history?.length() ?: 0)) {
                        val entry = history!!.getJSONObject(i)
                        val mapName = entry.optString("map_name", "Unnamed")
                        val saveData = entry.getString("save_data")

                        // VALIDATION: Fix corrupt mapPath values where mapName is "Level X" but mapPath doesn't match
                        // This prevents corrupt data from server from causing incorrect minimaps
                        var expectedMapPath: String? = null
                        if (mapName != null && mapName.matches("Level \\d+".toRegex())) {
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
                            val historyPath: String?
                            if (expectedMapPath != null) {
                                historyPath = expectedMapPath
                                d(
                                    "[HISTORY_SYNC] Using expected mapPath for built-in level: %s",
                                    historyPath
                                )
                            } else {
                                val nextIndex = GameHistoryManager.getNextHistoryIndex(this@SyncManager.context)
                                historyPath = GameHistoryManager.indexToPath(nextIndex)
                            }

                            // For built-in levels, ALWAYS load the correct level data from assets instead of using corrupt server saveData
                            // This ensures corrupt files are overwritten on sync
                            var dataToWrite = saveData
                            if (expectedMapPath != null) {
                                try {
                                    val `is` = this@SyncManager.context.assets.open("Maps/" + expectedMapPath)
                                    dataToWrite = Scanner(`is`).useDelimiter("\\A").next()
                                    `is`.close()
                                    d(
                                        "[HISTORY_SYNC] Loaded level data from assets for %s instead of server data",
                                        expectedMapPath
                                    )
                                } catch (e: Exception) {
                                    w(
                                        e,
                                        "[HISTORY_SYNC] Failed to load level data from assets for %s, using server data",
                                        expectedMapPath
                                    )
                                }
                            }

                            writePrivateData(this@SyncManager.context, historyPath, dataToWrite)

                            // Create a history entry
                            val historyEntry = GameHistoryEntry()
                            historyEntry.setMapPath(historyPath)
                            historyEntry.mapName = mapName
                            historyEntry.timestamp =
                                parseTimestamp(entry.optString("played_at", null))
                            historyEntry.playDuration = entry.optInt("play_time_seconds", 0)
                            historyEntry.movesMade = entry.optInt("move_count", 0)
                            historyEntry.optimalMoves = entry.optInt("optimal_moves", 0)
                            historyEntry.maxHintUsed = entry.optInt("max_hint_used", -1)
                            historyEntry.setEverUsedHints(
                                entry.optBoolean(
                                    "ever_used_hints",
                                    false
                                )
                            )
                            historyEntry.setSolvedWithoutHints(
                                entry.optBoolean(
                                    "solved_without_hints",
                                    false
                                )
                            )
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
                            val tsArray = entry.optJSONArray("completion_timestamps")
                            if (tsArray != null) {
                                val timestamps = mutableListOf<Long>()
                                for (j in 0..<tsArray.length()) {
                                    timestamps.add(tsArray.optLong(j, 0))
                                }
                                historyEntry.setCompletionTimestamps(timestamps)
                            }
                            // [HISTORY_SYNC] Restore per-completion moves and stars arrays
                            // to keep them aligned with completion_timestamps
                            val movesArrayDl = entry.optJSONArray("completion_moves")
                            if (movesArrayDl != null) {
                                val movesList = mutableListOf<Int>()
                                for (j in 0..<movesArrayDl.length()) {
                                    movesList.add(movesArrayDl.optInt(j, 0))
                                }
                                historyEntry.setCompletionMoves(movesList)
                            }
                            val starsArrayDl = entry.optJSONArray("completion_stars")
                            if (starsArrayDl != null) {
                                val starsList = mutableListOf<Int>()
                                for (j in 0..<starsArrayDl.length()) {
                                    starsList.add(starsArrayDl.optInt(j, 0))
                                }
                                historyEntry.setCompletionStars(starsList)
                            }

                            GameHistoryManager.addHistoryEntry(this@SyncManager.context, historyEntry)
                            restoredCount++


                            // Log timestamps with human-readable format for debugging timezone issues
                            val downloadedPlayedAt = entry.optString("played_at", "null")
                            val parsedTimestampStr =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(
                                    Date(historyEntry.timestamp)
                                )
                            val lastCompletionStr = if (historyEntry.lastCompletionTimestamp > 0)
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(
                                    Date(
                                        historyEntry.lastCompletionTimestamp
                                    )
                                )
                            else
                                "never"
                            d("[HISTORY_SYNC] Restored history entry: %s", mapName)
                            d(
                                "[HISTORY_SYNC_TIME] Download timestamps - server_played_at='%s', parsed_timestamp='%s' (millis=%d), last_completion='%s' (millis=%d)",
                                downloadedPlayedAt,
                                parsedTimestampStr,
                                historyEntry.timestamp,
                                lastCompletionStr,
                                historyEntry.lastCompletionTimestamp
                            )
                        }
                    }

                    d(
                        "[HISTORY_SYNC] Download complete: %d history entries restored",
                        restoredCount
                    )


                    // Restore level stars from ALL history entries to LevelCompletionManager
                    restoreLevelStarsFromHistory(history ?: JSONArray())
                } catch (e: Exception) {
                    e(e, "[HISTORY_SYNC] Error restoring history")
                    if (callback != null) callback.onError("Error restoring history: " + e.message)
                    return
                }

                if (callback != null) callback.onSuccess(restoredCount)
            }

            override fun onError(error: String?) {
                e("[HISTORY_SYNC] Download failed: %s", error)
                if (callback != null) callback.onError(error)
            }
        })
    }

    // ========== FULL SYNC ON LOGIN ==========
    /**
     * Perform full sync after login: download everything from server, then upload local data.
     */
    fun fullSyncOnLogin(context: Context, callback: ApiCallback<String?>?) {
        d("[FULL_SYNC] Starting full sync after login")


        // Step 1: Download save games
        downloadSaveGames(object : ApiCallback<Int?> {
            override fun onSuccess(savesRestored: Int?) {
                d("[FULL_SYNC] Save games downloaded: %d restored", savesRestored)


                // Step 2: Download history
                downloadHistory(this@SyncManager.context, object : ApiCallback<Int?> {
                    override fun onSuccess(historyRestored: Int?) {
                        d("[FULL_SYNC] History downloaded: %d restored", historyRestored)


                        // Count level entries separately
                        var levelsRestored = 0
                        try {
                            val allEntries = GameHistoryManager.getHistoryEntries(this@SyncManager.context)
                            for (entry in allEntries) {
                                if (entry.mapName != null && entry.mapName!!.startsWith("Level ")) {
                                    levelsRestored++
                                }
                            }
                        } catch (e: Exception) {
                            e(e, "[FULL_SYNC] Error counting level entries")
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
                        d("[FULL_SYNC] Full sync complete: %s", summary)
                        if (callback != null) callback.onSuccess(summary)
                    }

                    override fun onError(error: String?) {
                        e("[FULL_SYNC] History download failed: %s", error)
                        // Still try to upload local data
                        uploadSaveGames()
                        uploadHistory()
                        if (callback != null) callback.onSuccess(savesRestored.toString() + " saves restored (history failed)")
                    }
                })
            }

            override fun onError(error: String?) {
                e("[FULL_SYNC] Save game download failed: %s", error)
                // Still try history
                downloadHistory(this@SyncManager.context, object : ApiCallback<Int?> {
                    override fun onSuccess(historyRestored: Int?) {
                        uploadSaveGames()
                        uploadHistory()
                        if (callback != null) callback.onSuccess(historyRestored.toString() + " history entries restored (saves failed)")
                    }

                    override fun onError(histError: String?) {
                        if (callback != null) callback.onError("Sync failed: saves=" + error + ", history=" + histError)
                    }
                })
            }
        })
    }

    // ========== HELPER METHODS ==========
    private fun readFileContent(file: File): String? {
        try {
            BufferedReader(
                InputStreamReader(
                    FileInputStream(file),
                    StandardCharsets.UTF_8
                )
            ).use { reader ->
                val content = StringBuilder()
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    if (content.length > 0) content.append("\n")
                    content.append(line)
                }
                return content.toString()
            }
        } catch (e: IOException) {
            e(e, "[SYNC] Error reading file: %s", file.getName())
            return null
        }
    }

    private fun readHistoryFileData(context: Context?, entry: GameHistoryEntry): String? {
        try {
            return readPrivateData(this.context, entry.getMapPath())
        } catch (e: Exception) {
            e(e, "[HISTORY_SYNC] Error reading history file: %s", entry.getMapPath())
            return null
        }
    }

    private fun parseTimestamp(isoTimestamp: String?): Long {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return System.currentTimeMillis()
        }
        try {
            // IMPORTANT: Server sends ISO 8601 with UTC timezone (e.g., "2026-03-09T08:26:56+00:00")
            // Use java.time API (Android API 26+) for correct ISO 8601 parsing
            // OffsetDateTime.parse() correctly handles timezone offset and converts to UTC
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val odt = OffsetDateTime.parse(isoTimestamp)
                val millis = odt.toInstant().toEpochMilli()


                // For logging: show both UTC and local time to verify correct parsing
                val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                val localFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                d(
                    "[HISTORY_SYNC_TIME] parseTimestamp: input='%s' → millis=%d → UTC='%s', local='%s'",
                    isoTimestamp, millis,
                    utcFormat.format(Date(millis)),
                    localFormat.format(Date(millis))
                )
                return millis
            } else {
                // Fallback for older Android versions: SimpleDateFormat (less reliable)
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                val date = sdf.parse(isoTimestamp)
                return if (date != null) date.getTime() else System.currentTimeMillis()
            }
        } catch (e: Exception) {
            e(e, "[HISTORY_SYNC_TIME] Failed to parse timestamp: %s", isoTimestamp)
            return System.currentTimeMillis()
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

    private fun extractBoardWidth(saveData: String): Int {
        return extractSizeComponent(saveData, 0, 12)
    }

    private fun extractBoardHeight(saveData: String): Int {
        return extractSizeComponent(saveData, 1, 12)
    }

    private fun extractSizeComponent(saveData: String, index: Int, defaultValue: Int): Int {
        val sizeStart = saveData.indexOf("SIZE:")
        if (sizeStart >= 0) {
            val sizeEnd = saveData.indexOf(";", sizeStart)
            if (sizeEnd > sizeStart) {
                val sizeStr = saveData.substring(sizeStart + 5, sizeEnd)
                val parts: Array<String?> =
                    sizeStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size > index) {
                    try {
                        return parts[index]!!.trim { it <= ' ' }.toInt()
                    } catch (e: NumberFormatException) {
                        return defaultValue
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
    private fun restoreLevelStarsFromHistory(history: JSONArray) {
        try {
            val lcm = LevelCompletionManager.getInstance(this.context)
            var restoredLevels = 0

            for (i in 0..<history.length()) {
                val entry = history.getJSONObject(i)
                val mapName = entry.optString("map_name", "")
                val stars = entry.optInt("stars_earned", 0)
                val moves = entry.optInt("move_count", 0)
                val isSolved = entry.optBoolean("is_solved", false)

                if (!mapName.startsWith("Level ") || stars <= 0) continue


                // Parse level number from "Level X"
                val levelId: Int
                try {
                    levelId = mapName.substring(6).trim { it <= ' ' }.toInt()
                } catch (e: NumberFormatException) {
                    continue
                }

                val optimalMoves = entry.optInt("optimal_moves", 0)
                val maxHintUsed = entry.optInt("max_hint_used", -1)
                val hintsShown = if (maxHintUsed >= 0) maxHintUsed + 1 else 0

                val existing = lcm.getLevelCompletionData(levelId)
                // Update if stars improved OR if we have new optimal_moves data for existing level
                val starsImproved = stars > existing!!.getStars()
                val hasNewMetadata = (optimalMoves > 0 && existing.optimalMoves == 0)

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
                    d(
                        "[HISTORY_SYNC] Restored level %d stars: %d (moves=%d, optimal=%d, maxHint=%d)",
                        levelId, stars, moves, optimalMoves, maxHintUsed
                    )
                }
            }

            d("[HISTORY_SYNC] Level stars restoration complete: %d levels updated", restoredLevels)
        } catch (e: Exception) {
            e(e, "[HISTORY_SYNC] Error restoring level stars from history")
        }
    }

    private fun extractBoardWidthFromSize(boardSize: String?): Int {
        if (boardSize != null && boardSize.contains("x")) {
            try {
                return boardSize.split("x".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].trim { it <= ' ' }.toInt()
            } catch (e: NumberFormatException) {
                return 12
            }
        }
        return 12
    }

    private fun extractBoardHeightFromSize(boardSize: String?): Int {
        if (boardSize != null && boardSize.contains("x")) {
            try {
                return boardSize.split("x".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1].trim { it <= ' ' }.toInt()
            } catch (e: NumberFormatException) {
                return 12
            }
        }
        return 12
    }

    companion object {
        private var instance: SyncManager? = null
        private const val MIN_SYNC_INTERVAL_MS: Long = 60000 // 1 minute between auto-syncs

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): SyncManager {
            if (instance == null) {
                instance = SyncManager(context)
            }
            return instance!!
        }
    }
}
