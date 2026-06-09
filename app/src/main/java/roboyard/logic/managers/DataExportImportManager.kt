package roboyard.logic.managers

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import roboyard.eclabs.BuildConfig
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.StreakManager
import roboyard.logic.storage.FileReadWrite.Companion.loadAbsoluteData
import timber.log.Timber.Forest.tag
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages export and import of all app data as JSON.
 * Includes: preferences, achievements, streaks, level completion, wall storage, save games.
 */
class DataExportImportManager(context: Context) {
    private val context: Context

    init {
        this.context = context.getApplicationContext()
    }

    /**
     * Export all app data to a JSON string.
     * @return JSON string containing all app data
     */
    fun exportAllData(): String? {
        try {
            val root = JSONObject()


            // Add metadata
            val metadata = JSONObject()
            metadata.put("version", 1)
            metadata.put("exportTime", System.currentTimeMillis())
            metadata.put("appVersion", BuildConfig.VERSION_NAME)
            root.put("metadata", metadata)


            // Export all SharedPreferences
            val prefsData = JSONObject()
            for (prefsName in PREFS_NAMES) {
                val prefsJson = exportSharedPreferences(prefsName)
                if (prefsJson.length() > 0) {
                    prefsData.put(prefsName, prefsJson)
                }
            }
            root.put("preferences", prefsData)


            // Export save games
            val saveGames = exportSaveGames()
            root.put("saveGames", saveGames)


            // Export game history
            val gameHistory = exportGameHistory()
            root.put("gameHistory", gameHistory)

            tag(TAG).d("Exported all data successfully")
            return root.toString(2) // Pretty print with 2-space indent
        } catch (e: JSONException) {
            tag(TAG).e(e, "Error exporting data")
            return null
        }
    }

    /**
     * Export a single SharedPreferences file to JSON.
     * Exports all entries including those with null values.
     */
    @Throws(JSONException::class)
    private fun exportSharedPreferences(prefsName: String?): JSONObject {
        val result = JSONObject()
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val allEntries = prefs.getAll()

        for (entry in allEntries.entries) {
            val key: String = entry.key!!
            val value: Any? = entry.value

            if (value == null) {
                result.put(key, JSONObject.NULL)
            } else if (value is Boolean) {
                result.put(key, value)
            } else if (value is Int) {
                result.put(key, value)
            } else if (value is Long) {
                result.put(key, value)
            } else if (value is Float) {
                result.put(key, value)
            } else if (value is String) {
                result.put(key, value)
            } else if (value is MutableSet<*>) {
                // Handle StringSet
                val jsonArray = JSONArray()
                for (item in value) {
                    jsonArray.put(item)
                }
                result.put(key, jsonArray)
            }
        }

        tag(TAG).d("Exported %d entries from %s", allEntries.size, prefsName)
        return result
    }

    /**
     * Export all save games to a JSON array.
     */
    @Throws(JSONException::class)
    private fun exportSaveGames(): JSONArray {
        val saveGames = JSONArray()

        val savesDir = File(context.getFilesDir(), SAVES_DIRECTORY)
        if (savesDir.exists() && savesDir.isDirectory()) {
            val files = savesDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile() && file.getName().endsWith(".dat")) {
                        try {
                            val content = loadAbsoluteData(file.getAbsolutePath())
                            if (content != null && !content.isEmpty()) {
                                val saveGame = JSONObject()
                                saveGame.put("filename", file.getName())
                                saveGame.put("content", content)
                                saveGame.put("lastModified", file.lastModified())
                                saveGames.put(saveGame)
                            }
                        } catch (e: Exception) {
                            tag(TAG).e(e, "Error reading save file: %s", file.getName())
                        }
                    }
                }
            }
        }

        return saveGames
    }

    /**
     * Export game history to a JSON array.
     */
    @Throws(JSONException::class)
    private fun exportGameHistory(): JSONArray {
        val history = JSONArray()

        val historyDir = File(context.getFilesDir(), "history")
        if (historyDir.exists() && historyDir.isDirectory()) {
            val files = historyDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile()) {
                        try {
                            val content = loadAbsoluteData(file.getAbsolutePath())
                            if (content != null && !content.isEmpty()) {
                                val historyEntry = JSONObject()
                                historyEntry.put("filename", file.getName())
                                historyEntry.put("content", content)
                                historyEntry.put("lastModified", file.lastModified())
                                history.put(historyEntry)
                            }
                        } catch (e: Exception) {
                            tag(TAG).e(e, "Error reading history file: %s", file.getName())
                        }
                    }
                }
            }
        }

        return history
    }

    /**
     * Import all app data from a JSON string.
     * @param jsonData JSON string containing app data
     * @return true if import was successful, false otherwise
     */
    fun importAllData(jsonData: String): Boolean {
        try {
            val root = JSONObject(jsonData)


            // Check version compatibility
            val metadata = root.optJSONObject("metadata")
            if (metadata != null) {
                val version = metadata.optInt("version", 1)
                tag(TAG).d("Importing data version %d", version)
            }


            // Import SharedPreferences
            val prefsData = root.optJSONObject("preferences")
            if (prefsData != null) {
                for (prefsName in PREFS_NAMES) {
                    val prefsJson = prefsData.optJSONObject(prefsName)
                    if (prefsJson != null) {
                        importSharedPreferences(prefsName, prefsJson)
                    }
                }
            }


            // Import save games
            val saveGames = root.optJSONArray("saveGames")
            if (saveGames != null) {
                importSaveGames(saveGames)
            }


            // Import game history
            val gameHistory = root.optJSONArray("gameHistory")
            if (gameHistory != null) {
                importGameHistory(gameHistory)
            }

            tag(TAG).d("Imported all data successfully")
            return true
        } catch (e: JSONException) {
            tag(TAG).e(e, "Error importing data")
            return false
        }
    }

    /**
     * Import a single SharedPreferences file from JSON.
     */
    private fun importSharedPreferences(prefsName: String?, prefsJson: JSONObject) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val editor = prefs.edit()


        // Clear existing data
        editor.clear()


        // Import all entries
        val keys = prefsJson.keys()
        var importedCount = 0
        while (keys.hasNext()) {
            val key = keys.next()
            try {
                val value = prefsJson.get(key)

                if (value === JSONObject.NULL) {
                    // Skip null values
                    continue
                } else if (value is Boolean) {
                    editor.putBoolean(key, value)
                    importedCount++
                } else if (value is Int) {
                    // For roboyard_streaks, certain fields must be stored as Long, not Integer
                    // to avoid ClassCastException when reading with getLong()
                    if ("roboyard_streaks" == prefsName &&
                        (key == "last_login_date" ||
                                key == "last_streak_date" ||
                                key == "last_popup_date")
                    ) {
                        editor.putLong(key, value.toLong())
                    } else {
                        editor.putInt(key, value)
                    }
                    importedCount++
                } else if (value is Long) {
                    editor.putLong(key, value)
                    importedCount++
                } else if (value is Double) {
                    // JSON stores floats as doubles
                    editor.putFloat(key, value.toFloat())
                    importedCount++
                } else if (value is String) {
                    editor.putString(key, value)
                    importedCount++
                } else if (value is JSONArray) {
                    // Handle StringSet
                    val jsonArray = value
                    val stringSet: MutableSet<String?> = HashSet<String?>()
                    for (i in 0..<jsonArray.length()) {
                        stringSet.add(jsonArray.getString(i))
                    }
                    editor.putStringSet(key, stringSet)
                    importedCount++
                }
            } catch (e: JSONException) {
                tag(TAG).e(e, "Error importing preference: %s", key)
            }
        }

        editor.apply()
        tag(TAG).d("Imported %d entries to %s", importedCount, prefsName)
    }

    /**
     * Import save games from a JSON array.
     */
    @Throws(JSONException::class)
    private fun importSaveGames(saveGames: JSONArray) {
        val savesDir = File(context.getFilesDir(), SAVES_DIRECTORY)
        if (!savesDir.exists()) {
            savesDir.mkdirs()
        }

        for (i in 0..<saveGames.length()) {
            val saveGame = saveGames.getJSONObject(i)
            val filename = saveGame.getString("filename")
            val content = saveGame.getString("content")

            try {
                val saveFile = File(savesDir, filename)
                val fos = FileOutputStream(saveFile)
                fos.write(content.toByteArray())
                fos.close()
                tag(TAG).d("Imported save game: %s", filename)
            } catch (e: IOException) {
                tag(TAG).e(e, "Error importing save game: %s", filename)
            }
        }
    }

    /**
     * Import game history from a JSON array.
     */
    @Throws(JSONException::class)
    private fun importGameHistory(gameHistory: JSONArray) {
        val historyDir = File(context.getFilesDir(), "history")
        if (!historyDir.exists()) {
            historyDir.mkdirs()
        }

        for (i in 0..<gameHistory.length()) {
            val historyEntry = gameHistory.getJSONObject(i)
            val filename = historyEntry.getString("filename")
            val content = historyEntry.getString("content")

            try {
                val historyFile = File(historyDir, filename)
                val fos = FileOutputStream(historyFile)
                fos.write(content.toByteArray())
                fos.close()
                tag(TAG).d("Imported history entry: %s", filename)
            } catch (e: IOException) {
                tag(TAG).e(e, "Error importing history entry: %s", filename)
            }
        }
    }

    /**
     * Reset only account-bound progress data for logout.
     * Clears: achievements, streaks, level completion, history, saves.
     * Keeps: app preferences (RoboYard), wall storage (WallStoragePrefs), UI prefs (RoboyardUIPrefs).
     */
    fun resetProgressData() {
        tag(TAG).d("[LOGOUT][RESET] Starting progress data reset")


        // SharedPreferences to clear (only progress-related)
        val progressPrefs = arrayOf<String?>(
            "roboyard_achievements",  // Achievement data
            "roboyard_streaks",  // Streak data
            "level_completion_prefs" // Level completion data
        )

        for (prefsName in progressPrefs) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            tag(TAG).d("[LOGOUT][RESET] Cleared SharedPreferences: %s", prefsName)
        }


        // Reset in-memory singleton state
        AchievementManager.getInstance(context).resetAll()
        tag(TAG).d("[ACHIEVEMENT][RESET] Achievement manager reset")

        StreakManager.getInstance(context).resetStreak()
        tag(TAG).d("[STREAK][RESET] Streak manager reset")

        LevelCompletionManager.getInstance(context).resetAll()
        tag(TAG).d("[LEVEL][RESET] Level completion manager reset")


        // Delete save games
        val savesDir = File(context.getFilesDir(), SAVES_DIRECTORY)
        if (savesDir.exists()) {
            deleteDirectory(savesDir)
            tag(TAG).d("[SAVE][RESET] Deleted saves directory")
        }


        // Delete game history
        val historyDir = File(context.getFilesDir(), "history")
        if (historyDir.exists()) {
            deleteDirectory(historyDir)
            tag(TAG).d("[HISTORY][RESET] Deleted history directory")
        }


        // Delete history index file
        val historyIndexFile = File(context.getFilesDir(), "history_index.json")
        if (historyIndexFile.exists()) {
            historyIndexFile.delete()
            tag(TAG).d("[HISTORY][RESET] Deleted history_index.json")
        }

        tag(TAG).d("[LOGOUT][RESET] Progress data reset complete")
    }

    /**
     * Reset all app data (clear all preferences and files).
     */
    fun resetAllData() {
        // Clear all SharedPreferences
        for (prefsName in PREFS_NAMES) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }


        // Reset level completion data (stars and solved status)
        val levelCompletionManager = LevelCompletionManager.getInstance(context)
        levelCompletionManager.resetAll()


        // Delete save games
        val savesDir = File(context.getFilesDir(), SAVES_DIRECTORY)
        if (savesDir.exists()) {
            deleteDirectory(savesDir)
        }


        // Delete game history
        val historyDir = File(context.getFilesDir(), "history")
        if (historyDir.exists()) {
            deleteDirectory(historyDir)
        }


        // Delete history index file
        val historyIndexFile = File(context.getFilesDir(), "history_index.json")
        if (historyIndexFile.exists()) {
            historyIndexFile.delete()
        }

        tag(TAG).d("Reset all data")
    }

    /**
     * Delete a directory and all its contents.
     */
    private fun deleteDirectory(dir: File) {
        if (dir.isDirectory()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file)
                    } else {
                        file.delete()
                    }
                }
            }
        }
        dir.delete()
    }

    companion object {
        private const val TAG = "DataExportImport"

        // SharedPreferences names used in the app
        private val PREFS_NAMES = arrayOf<String?>(
            "RoboYard",  // Main preferences (Preferences.java)
            "roboyard_achievements",  // Achievement data (AchievementManager.java)
            "roboyard_streaks",  // Streak data (StreakManager.java)
            "level_completion_prefs",  // Level completion data (LevelCompletionManager.java)
            "WallStoragePrefs",  // Wall storage (WallStorage.java)
            "RoboyardUIPrefs" // UI mode (UIModeManager.java)
        )

        private const val SAVES_DIRECTORY = "saves"
    }
}
