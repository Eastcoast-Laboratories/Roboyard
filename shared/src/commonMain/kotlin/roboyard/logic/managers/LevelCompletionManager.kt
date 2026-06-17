package roboyard.logic.managers

import roboyard.logic.core.LevelCompletionData
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.storage.getPlatformStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manager for level completion data.
 * Stores and retrieves level completion information including stars, moves, time, etc.
 */
class LevelCompletionManager private constructor() {
    private var completionDataMap: MutableMap<Int?, LevelCompletionData>? = HashMap()
    private val storage: PlatformStorage = getPlatformStorage()

    init {
        loadCompletionData()
    }

    /**
     * Get completion data for a specific level
     * @param levelId The level ID
     * @return The completion data, or a new empty data object if none exists
     */
    fun getLevelCompletionData(levelId: Int): LevelCompletionData? {
        if (!completionDataMap!!.containsKey(levelId)) {
            completionDataMap!!.put(levelId, LevelCompletionData(levelId))
        }
        return completionDataMap!!.get(levelId)
    }

    /**
     * Save completion data for a level, only updating values that are better than existing ones
     * @param data The completion data to save
     */
    fun saveLevelCompletionData(data: LevelCompletionData) {
        val levelId = data.levelId

        // Check if we already have data for this level
        if (completionDataMap!!.containsKey(levelId)) {
            val existingData = completionDataMap!!.get(levelId)

            // Only update stars if new value is greater
            val starsImproved = data.getStars() > existingData!!.getStars()
            val starsAtLeastSame = data.getStars() >= existingData.getStars()

            // Always update hints shown (relevant for achievement tracking)
            existingData!!.hintsShown = data.hintsShown
            // Only update optimal moves if new value is valid (> 0), to avoid overwriting with 0
            if (data.optimalMoves > 0) {
                existingData.optimalMoves = data.optimalMoves
            }

            if (starsImproved) {
                // If stars have improved, update stars and related metrics
                existingData.setStars(data.getStars())
                existingData.movesNeeded = data.movesNeeded
                existingData.timeNeeded = data.timeNeeded
                existingData.robotsUsed = data.robotsUsed
                existingData.squaresSurpassed = data.squaresSurpassed
            }

            // Only update robotsUsed if stars are at least the same
            if (data.isCompleted() && starsAtLeastSame) {
                existingData.setCompleted(true)
                existingData.robotsUsed = data.robotsUsed
            }

            // Always update moves if it's lower (better) than existing value
            if (data.isCompleted() && (existingData.movesNeeded == 0 || data.movesNeeded < existingData.movesNeeded)) {
                existingData.movesNeeded = data.movesNeeded
            }

            // Always update time if it's lower (faster) than existing value
            if (data.isCompleted() && (existingData.timeNeeded == 0L || data.timeNeeded < existingData.timeNeeded)) {
                existingData.timeNeeded = data.timeNeeded
            }

            // Always update squares if it's more (better) than existing value
            if (data.isCompleted() && data.squaresSurpassed > existingData.squaresSurpassed) {
                existingData.squaresSurpassed = data.squaresSurpassed
            }

            // Use the updated existing data
            completionDataMap!!.put(levelId, existingData)
        } else {
            // No existing data, just add the new data directly
            completionDataMap!!.put(levelId, data)
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
        val completed = data!!.isCompleted()
        return completed
    }

    val totalStars: Int
        /**
         * Get the total number of stars earned across all levels
         * @return The total number of stars
         */
        get() {
            var totalStars = 0
            for (data in completionDataMap!!.values) {
                if (data.isCompleted()) {
                    totalStars += data.getStars()
                }
            }
            return totalStars
        }

    /**
     * Load all completion data from storage
     */
    private fun loadCompletionData() {
        val json = storage.getString(COMPLETION_DATA_KEY, null)

        if (json != null) {
            try {
                val gson = Gson()
                // Use Runtime Type to avoid ProGuard issues
                val mapType =
                    object : TypeToken<HashMap<Int?, LevelCompletionData?>?>() {}.getType()
                val loadedData =
                    gson.fromJson<MutableMap<Int?, LevelCompletionData>?>(json, mapType)

                if (loadedData != null) {
                    completionDataMap = loadedData
                } else {
                    // Create an empty map as fallback
                    completionDataMap = HashMap<Int?, LevelCompletionData>()
                }
            } catch (e: Exception) {
                // Create an empty map as fallback
                completionDataMap = HashMap<Int?, LevelCompletionData>()
            }
        } else {
            // No completion data found in storage
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
        }

    /**
     * Reset all level completion data
     */
    fun resetAll() {
        completionDataMap!!.clear()
        storage.clear()
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
        for (levelId in 1..numLevels) {
            val data = getLevelCompletionData(levelId)
            data!!.setCompleted(true)
            data.setStars(1)
            data.optimalMoves = 1
            data.timeNeeded = 1
            data.squaresSurpassed = 0
        }

        saveCompletionData()
    }

    /**
     * Save all completion data to storage
     */
    private fun saveCompletionData() {
        try {
            val gson = Gson()
            val json = gson.toJson(completionDataMap)
            storage.putString(COMPLETION_DATA_KEY, json)
        } catch (e: Exception) {
            // Error saving level completion data
        }
    }

    companion object {
        private const val COMPLETION_DATA_KEY = "completion_data"
        private const val LAST_PLAYED_LEVEL_KEY = "last_played_level"

        private var instance: LevelCompletionManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): LevelCompletionManager {
            if (instance == null) {
                instance = LevelCompletionManager()
            }
            return instance!!
        }
    }
}
