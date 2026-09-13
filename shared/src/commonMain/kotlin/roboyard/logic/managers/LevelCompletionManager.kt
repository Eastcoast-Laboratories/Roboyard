package roboyard.logic.managers

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import roboyard.logic.core.LevelCompletionData
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.UiNotifier
import roboyard.logic.util.RLog

/**
 * Manager for level completion data.
 * Stores and retrieves level completion information including stars, moves, time, etc.
 * Uses Gson for serialization to maintain data compatibility with existing Android app data.
 */
class LevelCompletionManager private constructor(
    private val storage: PlatformStorage
) {
    private var completionDataMap: MutableMap<Int, LevelCompletionData> = HashMap()
    private var uiNotifier: UiNotifier? = null
    private val log = RLog.tag("LevelCompletionManager")
    private val gson = Gson()

    init {
        loadCompletionData()
    }

    /**
     * Set the UI notifier for displaying messages
     */
    fun setUiNotifier(notifier: UiNotifier?) {
        this.uiNotifier = notifier
    }

    /**
     * Get completion data for a specific level
     * @param levelId The level ID
     * @return The completion data, or a new empty data object if none exists
     */
    fun getLevelCompletionData(levelId: Int): LevelCompletionData? {
        if (!completionDataMap.containsKey(levelId)) {
            completionDataMap.put(levelId, LevelCompletionData(levelId))
        }
        return completionDataMap.get(levelId)
    }

    /**
     * Save completion data for a level, only updating values that are better than existing ones
     * @param data The completion data to save
     */
    fun saveLevelCompletionData(data: LevelCompletionData) {
        val levelId = data.levelId

        log.d(
            "[LEVEL_COMPLETION] Saving data for level $levelId - Stars: ${data.getStars()}, Moves: ${data.movesNeeded}, Time: ${data.timeNeeded}, Squares: ${data.squaresSurpassed}"
        )

        // Check if we already have data for this level
        if (completionDataMap.containsKey(levelId)) {
            val existingData = completionDataMap.get(levelId) ?: return

            log.d(
                "[LEVEL_COMPLETION] Existing data - Stars: ${existingData.getStars()}, Moves: ${existingData.movesNeeded}, Time: ${existingData.timeNeeded}, Squares: ${existingData.squaresSurpassed}"
            )

            // Only update stars if new value is greater
            val starsImproved = data.getStars() > existingData.getStars()
            val starsAtLeastSame = data.getStars() >= existingData.getStars()

            // Always update hints shown (relevant for achievement tracking)
            existingData.hintsShown = data.hintsShown
            // Only update optimal moves if new value is valid (> 0), to avoid overwriting with 0
            if (data.optimalMoves > 0) {
                existingData.optimalMoves = data.optimalMoves
            }

            if (starsImproved) {
                log.d(
                    "[LEVEL_COMPLETION] Stars improved from ${existingData.getStars()} to ${data.getStars()} - updating stars and related metrics"
                )
                // If stars have improved, update stars and related metrics
                existingData.setStars(data.getStars())
                existingData.movesNeeded = data.movesNeeded
                existingData.timeNeeded = data.timeNeeded
                existingData.robotsUsed = data.robotsUsed
                existingData.squaresSurpassed = data.squaresSurpassed
            } else {
                log.d(
                    "[LEVEL_COMPLETION] Stars not improved (${data.getStars()} vs ${existingData.getStars()}) - not updating stars"
                )
            }

            // Only update robotsUsed if stars are at least the same
            if (data.isCompleted() && starsAtLeastSame) {
                existingData.setCompleted(true)
                existingData.robotsUsed = data.robotsUsed
            }

            // Always update moves if it's lower (better) than existing value
            if (data.isCompleted() && (existingData.movesNeeded == 0 || data.movesNeeded < existingData.movesNeeded)) {
                log.d(
                    "[LEVEL_COMPLETION] Moves improved from ${existingData.movesNeeded} to ${data.movesNeeded}"
                )
                existingData.movesNeeded = data.movesNeeded
            }

            // Always update time if it's lower (faster) than existing value
            if (data.isCompleted() && (existingData.timeNeeded == 0L || data.timeNeeded < existingData.timeNeeded)) {
                log.d(
                    "[LEVEL_COMPLETION] Time improved from ${existingData.timeNeeded} to ${data.timeNeeded}"
                )
                existingData.timeNeeded = data.timeNeeded
            }

            // Always update squares if it's more (better) than existing value
            if (data.isCompleted() && data.squaresSurpassed > existingData.squaresSurpassed) {
                log.d(
                    "[LEVEL_COMPLETION] Squares improved from ${existingData.squaresSurpassed} to ${data.squaresSurpassed}"
                )
                existingData.squaresSurpassed = data.squaresSurpassed
            }

            // Use the updated existing data
            completionDataMap.put(levelId, existingData)
        } else {
            // No existing data, just add the new data directly
            log.d("[LEVEL_COMPLETION] No existing data for level $levelId, adding new data")
            completionDataMap.put(levelId, data)
        }

        // Save changes to storage
        saveCompletionData()
    }

    /**
     * Check if a level has been completed
     * @param levelId The level ID to check
     * @return true if the level is completed, false otherwise
     */
    fun isLevelCompleted(levelId: Int): Boolean {
        val data = getLevelCompletionData(levelId)
        val completed = data?.isCompleted() ?: false
        log.d("Checking if level $levelId is completed: $completed")
        return completed
    }

    val totalStars: Int
        /**
         * Get the total number of stars earned across all levels
         * @return The total number of stars
         */
        get() {
            var totalStars = 0
            for (data in completionDataMap.values) {
                if (data.isCompleted()) {
                    totalStars += data.getStars()
                }
            }
            log.d("Total stars earned across all levels: $totalStars")
            return totalStars
        }

    /**
     * Load all completion data from storage using Gson for full data preservation.
     * Compatible with the JSON format used by the Android app version.
     */
    private fun loadCompletionData() {
        val json = storage.getString(COMPLETION_DATA_KEY, null)

        log.d("Loading completion data, found JSON: ${if (json != null) "yes" else "no"}")

        if (json != null) {
            try {
                // Use runtime type token to avoid ProGuard issues and match app version format
                val mapType = object : TypeToken<HashMap<Int, LevelCompletionData>>() {}.type
                val loadedData: MutableMap<Int, LevelCompletionData>? = gson.fromJson(json, mapType)

                if (loadedData != null) {
                    completionDataMap = loadedData
                    log.d("Loaded completion data for ${completionDataMap.size} levels")

                    // Debug output to show what was loaded
                    for (entry in completionDataMap.entries) {
                        log.d(
                            "Loaded level ${entry.key}: completed=${entry.value.isCompleted()}, stars=${entry.value.getStars()}"
                        )
                    }
                } else {
                    log.w("Loaded data was null despite having JSON")
                }
            } catch (e: Exception) {
                log.e("Error loading level completion data: ${e.message}")
                uiNotifier?.showMessage("Error loading level data: ${e.message}")
                // Create an empty map as fallback
                completionDataMap = HashMap()
            }
        } else {
            log.d("No completion data found in storage")
        }
    }

    var lastPlayedLevel: Int
        /**
         * Get the last played level ID
         * @return The last played level ID, or 1 if none was set
         */
        get() {
            return storage.getInt(LAST_PLAYED_LEVEL_KEY, 1)
        }
        /**
         * Set the last played level ID
         * @param levelId The level ID that was last played
         */
        set(levelId) {
            storage.putInt(LAST_PLAYED_LEVEL_KEY, levelId)
            log.d("[LEVEL_COMPLETION] Set last played level to $levelId")
        }

    /**
     * Reset all level completion data
     */
    fun resetAll() {
        log.d("[LEVEL_COMPLETION] Resetting all level completion data")
        completionDataMap.clear()
        storage.clear()
        log.d("[LEVEL_COMPLETION] All level data reset successfully")
    }

    /**
     * Unlock 1 star per level for all levels except level 139 (used for debug/level design editor unlock)
     */
    fun unlockAllStars() {
        unlockStars(139)
    }

    /**
     * Unlock 1 star per level for the first N levels
     * @param numLevels Number of levels to unlock (1 star per level)
     */
    fun unlockStars(numLevels: Int) {
        log.d("[LEVEL_COMPLETION] Unlocking 1 star per level for first $numLevels levels")

        for (levelId in 1..numLevels) {
            val data = getLevelCompletionData(levelId) ?: continue
            data.setCompleted(true)
            data.setStars(1)
            data.optimalMoves = 1
            data.timeNeeded = 1
            data.squaresSurpassed = 0
        }

        saveCompletionData()
        log.d(
            "[LEVEL_COMPLETION] 1 star per level unlocked for first $numLevels levels successfully"
        )
    }

    /**
     * Save all completion data to storage using Gson.
     * Format is compatible with the Android app version.
     */
    private fun saveCompletionData() {
        try {
            val json = gson.toJson(completionDataMap)
            storage.putString(COMPLETION_DATA_KEY, json)
            log.d("Saved completion data for ${completionDataMap.size} levels")
        } catch (e: Exception) {
            log.e("Error saving level completion data: ${e.message}")
        }
    }

    companion object {
        private const val COMPLETION_DATA_KEY = "completion_data"
        private const val LAST_PLAYED_LEVEL_KEY = "last_played_level"

        private var instance: LevelCompletionManager? = null

        /**
         * Get the singleton instance using the default platform storage.
         * On desktop, this uses DesktopStorage via getPlatformStorage().
         * On Android, use getInstance(storage) with AndroidStorage.getInstance(context) instead.
         */
        @Synchronized
        fun getInstance(): LevelCompletionManager {
            if (instance == null) {
                instance = LevelCompletionManager(getPlatformStorage())
            }
            return instance!!
        }

        /**
         * Get the singleton instance with an explicit platform storage.
         * Use this on Android: getInstance(AndroidStorage.getInstance(context))
         * @param storage The platform storage to use
         * @return The singleton LevelCompletionManager instance
         */
        @JvmStatic
        @Synchronized
        fun getInstance(storage: PlatformStorage): LevelCompletionManager {
            if (instance == null) {
                instance = LevelCompletionManager(storage)
            }
            return instance!!
        }
    }
}
