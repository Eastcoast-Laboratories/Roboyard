package roboyard.logic.managers

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.StreakManager
import roboyard.logic.platform.PlatformInfo
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.util.RLog

/**
 * Manages export and import of all app data as JSON.
 * Includes: preferences, achievements, streaks, level completion, wall storage, save games.
 * Shared implementation used by Android and Desktop (Compose).
 */
class DataExportImportManager(private val storage: PlatformStorage) {
    private val log = RLog.tag(TAG)

    /**
     * Export all app data to a JSON string.
     * @return JSON string containing all app data
     */
    fun exportAllData(): String? {
        try {
            val root = JsonObject()

            // Add metadata
            val metadata = JsonObject()
            metadata.addProperty("version", 1)
            metadata.addProperty("exportTime", System.currentTimeMillis())
            metadata.addProperty("appVersion", PlatformInfo.getAppVersionName())
            root.add("metadata", metadata)

            // Export all SharedPreferences
            val prefsData = JsonObject()
            for (prefsName in PREFS_NAMES) {
                val prefsJson = exportSharedPreferences(prefsName)
                if (prefsJson.size() > 0) {
                    prefsData.add(prefsName, prefsJson)
                }
            }
            root.add("preferences", prefsData)

            // Export save games
            root.add("saveGames", exportFilesInDir(SAVES_DIRECTORY))

            // Export game history: "history" dir plus root-level history_*.txt files
            // (GameHistoryManager stores history_N.txt in the storage root)
            val gameHistory = exportFilesInDir(HISTORY_DIRECTORY)
            for (name in storage.listFiles(HISTORY_FILE_PREFIX, HISTORY_FILE_SUFFIX)) {
                addFileEntry(gameHistory, name, storage.readFile(name))
            }
            if (storage.fileExists(HISTORY_INDEX_FILE)) {
                addFileEntry(
                    gameHistory, HISTORY_INDEX_FILE,
                    storage.readFile(HISTORY_INDEX_FILE)
                )
            }
            root.add("gameHistory", gameHistory)

            log.d("Exported all data successfully")
            return root.toString()
        } catch (e: Exception) {
            log.e(e, "Error exporting data")
            return null
        }
    }

    /** Export all files inside a private-storage directory to a JSON array. */
    private fun exportFilesInDir(dirName: String): JsonArray {
        val files = JsonArray()
        for (name in storage.listFilesInDir(dirName)) {
            addFileEntry(files, name, storage.readFile("$dirName/$name"))
        }
        return files
    }

    private fun addFileEntry(target: JsonArray, filename: String, content: String) {
        if (content.isEmpty()) return
        val entry = JsonObject()
        entry.addProperty("filename", filename)
        entry.addProperty("content", content)
        target.add(entry)
    }

    /**
     * Export a single SharedPreferences file to JSON.
     * Exports all known entries including those with null values.
     */
    private fun exportSharedPreferences(prefsName: String?): JsonObject {
        val result = JsonObject()
        // Note: PlatformStorage doesn't expose getAll(), so we export known keys
        val allEntries = getKnownPrefsEntries(prefsName)

        for ((key, value) in allEntries) {
            when (value) {
                null -> result.add(key, JsonNull.INSTANCE)
                is Boolean -> result.addProperty(key, value)
                is Int -> result.addProperty(key, value)
                is Long -> result.addProperty(key, value)
                is Float -> result.addProperty(key, value)
                is String -> result.addProperty(key, value)
                is Set<*> -> {
                    val jsonArray = JsonArray()
                    for (item in value) jsonArray.add(item?.toString())
                    result.add(key, jsonArray)
                }
            }
        }

        log.d("Exported %d entries from %s", allEntries.size, prefsName)
        return result
    }

    /**
     * Import all app data from a JSON string.
     * @param jsonData JSON string containing app data
     * @return true if import was successful, false otherwise
     */
    fun importAllData(jsonData: String): Boolean {
        try {
            val root = JsonParser.parseString(jsonData).asJsonObject

            // Check version compatibility
            val metadata = if (root.has("metadata") && root.get("metadata").isJsonObject)
                root.getAsJsonObject("metadata") else null
            if (metadata != null) {
                log.d("Importing data version %d",
                    if (metadata.has("version")) metadata.get("version").asInt else 1)
            }

            // Import SharedPreferences
            if (root.has("preferences") && root.get("preferences").isJsonObject) {
                val prefsData = root.getAsJsonObject("preferences")
                for (prefsName in PREFS_NAMES) {
                    if (prefsData.has(prefsName) && prefsData.get(prefsName).isJsonObject) {
                        importSharedPreferences(prefsName, prefsData.getAsJsonObject(prefsName))
                    }
                }
            }

            // Import save games
            if (root.has("saveGames") && root.get("saveGames").isJsonArray) {
                importFiles(root.getAsJsonArray("saveGames"), SAVES_DIRECTORY + "/")
            }

            // Import game history (entries may carry a "history/" prefix or be root-level files)
            if (root.has("gameHistory") && root.get("gameHistory").isJsonArray) {
                importFiles(root.getAsJsonArray("gameHistory"), null)
            }

            log.d("Imported all data successfully")
            return true
        } catch (e: Exception) {
            log.e(e, "Error importing data")
            return false
        }
    }

    /**
     * Import a single SharedPreferences file from JSON.
     */
    private fun importSharedPreferences(prefsName: String?, prefsJson: JsonObject) {
        // Clear existing data in storage
        clearPrefsStorage(prefsName)

        var importedCount = 0
        for ((key, element) in prefsJson.entrySet()) {
            try {
                when {
                    element.isJsonNull -> continue
                    element is JsonPrimitive && element.isBoolean -> {
                        storage.putBoolean(key, element.asBoolean)
                        importedCount++
                    }
                    element is JsonPrimitive && element.isNumber -> {
                        val num = element.asNumber
                        // For roboyard_streaks, certain fields must be stored as Long, not Integer
                        if ("roboyard_streaks" == prefsName &&
                            (key == "last_login_date" || key == "last_streak_date" || key == "last_popup_date")
                        ) {
                            storage.putLong(key, num.toLong())
                        } else if (num.toDouble() == num.toLong().toDouble()) {
                            storage.putLong(key, num.toLong())
                        } else {
                            storage.putString(key, num.toString())
                        }
                        importedCount++
                    }
                    element is JsonPrimitive && element.isString -> {
                        storage.putString(key, element.asString)
                        importedCount++
                    }
                    element.isJsonArray -> {
                        // StringSet stored as JSON string (PlatformStorage has no set type)
                        storage.putString(key, element.toString())
                        importedCount++
                    }
                }
            } catch (e: Exception) {
                log.e(e, "Error importing preference: %s", key)
            }
        }

        log.d("Imported %d entries to %s", importedCount, prefsName)
    }

    /** Import files from a JSON array of {filename, content} entries. */
    private fun importFiles(files: JsonArray, dirPrefix: String?) {
        for (i in 0 until files.size()) {
            val entry = files.get(i).asJsonObject
            val filename = entry.get("filename").asString
            val content = entry.get("content").asString
            val path = if (dirPrefix != null) dirPrefix + filename else filename
            try {
                storage.writeFile(path, content)
                log.d("Imported file: %s", path)
            } catch (e: Exception) {
                log.e(e, "Error importing file: %s", path)
            }
        }
    }

    /**
     * Get known preference entries for export (PlatformStorage has no getAll())
     */
    private fun getKnownPrefsEntries(prefsName: String?): Map<String, Any?> {
        val entries = mutableMapOf<String, Any?>()
        when (prefsName) {
            "RoboYard" -> {
                entries["robot_count"] = storage.getInt("robot_count", 1)
                entries["target_colors"] = storage.getInt("target_colors", 1)
                entries["sound_enabled"] = storage.getBoolean("sound_enabled", true)
                entries["difficulty"] = storage.getInt("difficulty", 0)
                entries["boardSizeX"] = storage.getInt("boardSizeX", 12)
                entries["boardSizeY"] = storage.getInt("boardSizeY", 14)
                entries["generate_new_map"] = storage.getBoolean("generate_new_map", true)
            }
            "roboyard_achievements" -> {
                // Achievement keys are dynamic, export what we can
                entries["total_stars"] = storage.getInt("total_stars", 0)
            }
            "roboyard_streaks" -> {
                entries["current_streak"] = storage.getInt("current_streak", 0)
                entries["last_login_date"] = storage.getLong("last_login_date", 0)
                entries["last_streak_date"] = storage.getLong("last_streak_date", 0)
            }
            "level_completion_prefs" -> {
                entries["total_solved"] = storage.getInt("total_solved", 0)
            }
        }
        return entries
    }

    /**
     * Clear preference storage for a given prefs name
     */
    private fun clearPrefsStorage(prefsName: String?) {
        when (prefsName) {
            "RoboYard" -> {
                storage.remove("robot_count")
                storage.remove("target_colors")
                storage.remove("sound_enabled")
                storage.remove("difficulty")
                storage.remove("boardSizeX")
                storage.remove("boardSizeY")
                storage.remove("generate_new_map")
            }
            "roboyard_achievements", "roboyard_streaks", "level_completion_prefs" -> {
                storage.clear()
            }
        }
    }

    /**
     * Reset only account-bound progress data for logout.
     * Clears: achievements, streaks, level completion, history, saves.
     * Keeps: app preferences (RoboYard), wall storage (WallStoragePrefs), UI prefs (RoboyardUIPrefs).
     */
    fun resetProgressData() {
        log.d("[LOGOUT][RESET] Starting progress data reset")

        val progressPrefs = arrayOf(
            "roboyard_achievements",  // Achievement data
            "roboyard_streaks",  // Streak data
            "level_completion_prefs" // Level completion data
        )
        for (prefsName in progressPrefs) {
            clearPrefsStorage(prefsName)
            log.d("[LOGOUT][RESET] Cleared storage: %s", prefsName)
        }

        // Reset in-memory singleton state
        AchievementManager.getInstance(storage).resetAll()
        log.d("[ACHIEVEMENT][RESET] Achievement manager reset")

        StreakManager.getInstance(storage, null).resetStreak()
        log.d("[STREAK][RESET] Streak manager reset")

        LevelCompletionManager.getInstance(storage).resetAll()
        log.d("[LEVEL][RESET] Level completion manager reset")

        deleteSaveGames()
        deleteGameHistory()

        log.d("[LOGOUT][RESET] Progress data reset complete")
    }

    /**
     * Reset all app data (clear all preferences and files).
     */
    fun resetAllData() {
        for (prefsName in PREFS_NAMES) {
            clearPrefsStorage(prefsName)
        }

        // Reset level completion data (stars and solved status)
        LevelCompletionManager.getInstance(storage).resetAll()

        deleteSaveGames()
        deleteGameHistory()

        log.d("Reset all data")
    }

    private fun deleteSaveGames() {
        for (name in storage.listFilesInDir(SAVES_DIRECTORY)) {
            storage.deleteFile("$SAVES_DIRECTORY/$name")
        }
        log.d("[SAVE][RESET] Deleted saves directory")
    }

    private fun deleteGameHistory() {
        for (name in storage.listFilesInDir(HISTORY_DIRECTORY)) {
            storage.deleteFile("$HISTORY_DIRECTORY/$name")
        }
        // Root-level history_N.txt files written by GameHistoryManager
        for (name in storage.listFiles(HISTORY_FILE_PREFIX, HISTORY_FILE_SUFFIX)) {
            storage.deleteFile(name)
        }
        storage.deleteFile(HISTORY_INDEX_FILE)
        log.d("[HISTORY][RESET] Deleted history files")
    }

    companion object {
        private const val TAG = "DataExportImport"

        // SharedPreferences names used in the app
        private val PREFS_NAMES = arrayOf(
            "RoboYard",  // Main preferences (Preferences)
            "roboyard_achievements",  // Achievement data
            "roboyard_streaks",  // Streak data
            "level_completion_prefs",  // Level completion data
            "WallStoragePrefs",  // Wall storage
            "RoboyardUIPrefs" // UI mode
        )

        private const val SAVES_DIRECTORY = "saves"
        private const val HISTORY_DIRECTORY = "history"
        private const val HISTORY_FILE_PREFIX = "history_"
        private const val HISTORY_FILE_SUFFIX = ".txt"
        private const val HISTORY_INDEX_FILE = "history_index.json"
    }
}
