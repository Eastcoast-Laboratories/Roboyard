package roboyard.logic.managers

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import roboyard.logic.core.LevelCompletionData
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.w

/**
 * Manages level completion data, including saving and loading statistics.
 */
class LevelCompletionManager private constructor(context: Context) {
    private var completionDataMap: MutableMap<Int?, LevelCompletionData>?
    private val context: Context

    init {
        this.context = context.getApplicationContext()
        this.completionDataMap = HashMap<Int?, LevelCompletionData>()
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

        d(
            "[LEVEL_COMPLETION] Saving data for level %d - Stars: %d, Moves: %d, Time: %d, Squares: %d",
            levelId, data.getStars(), data.movesNeeded, data.timeNeeded, data.squaresSurpassed
        )


        // Check if we already have data for this level
        if (completionDataMap!!.containsKey(levelId)) {
            val existingData = completionDataMap!!.get(levelId)

            d(
                "[LEVEL_COMPLETION] Existing data - Stars: %d, Moves: %d, Time: %d, Squares: %d",
                existingData!!.getStars(), existingData.movesNeeded,
                existingData.timeNeeded, existingData.squaresSurpassed
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
                d(
                    "[LEVEL_COMPLETION] Stars improved from %d to %d - updating stars and related metrics",
                    existingData.getStars(), data.getStars()
                )

                // If stars have improved, update stars and related metrics
                existingData.setStars(data.getStars())
                existingData.movesNeeded = data.movesNeeded
                existingData.timeNeeded = data.timeNeeded
                existingData.robotsUsed = data.robotsUsed
                existingData.squaresSurpassed = data.squaresSurpassed
            } else {
                d(
                    "[LEVEL_COMPLETION] Stars not improved (%d vs %d) - not updating stars",
                    data.getStars(), existingData.getStars()
                )
            }

            // Only update robotsUsed if stars are at least the same
            if (data.isCompleted() && starsAtLeastSame) {
                existingData.setCompleted(true)
                existingData.robotsUsed = data.robotsUsed
                // TODO: robots used is nowhere displayed, maybe an achievement later
            }

            // Always update moves if it's lower (better) than existing value
            if (data.isCompleted() && (existingData.movesNeeded == 0 || data.movesNeeded < existingData.movesNeeded)) {
                d(
                    "[LEVEL_COMPLETION] Moves improved from %d to %d",
                    existingData.movesNeeded, data.movesNeeded
                )
                existingData.movesNeeded = data.movesNeeded
            }

            // Always update time if it's lower (faster) than existing value
            if (data.isCompleted() && (existingData.timeNeeded == 0L || data.timeNeeded < existingData.timeNeeded)) {
                d(
                    "[LEVEL_COMPLETION] Time improved from %d to %d",
                    existingData.timeNeeded, data.timeNeeded
                )
                existingData.timeNeeded = data.timeNeeded
            }

            // Always update squares if it's more (better) than existing value
            if (data.isCompleted() && data.squaresSurpassed > existingData.squaresSurpassed) {
                d(
                    "[LEVEL_COMPLETION] Squares improved from %d to %d",
                    existingData.squaresSurpassed, data.squaresSurpassed
                )
                existingData.squaresSurpassed = data.squaresSurpassed
            }


            // Use the updated existing data
            completionDataMap!!.put(levelId, existingData)
        } else {
            // No existing data, just add the new data directly
            d("[LEVEL_COMPLETION] No existing data for level %d, adding new data", levelId)
            completionDataMap!!.put(levelId, data)
        }


        // Save changes to SharedPreferences
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
        d("Checking if level %d is completed: %s", levelId, completed)
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
            d("Total stars earned across all levels: %d", totalStars)
            return totalStars
        }

    /**
     * Load all completion data from SharedPreferences
     */
    private fun loadCompletionData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(COMPLETION_DATA_KEY, null)

        d("Loading completion data, found JSON: %s", if (json != null) "yes" else "no")

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
                    d("Loaded completion data for %d levels", completionDataMap!!.size)


                    // Debug output to show what was loaded
                    for (entry in completionDataMap!!.entries) {
                        d(
                            "Loaded level %d: completed=%s, stars=%d",
                            entry.key, entry.value.isCompleted(), entry.value.getStars()
                        )
                    }
                } else {
                    w("Loaded data was null despite having JSON")
                }
            } catch (e: Exception) {
                // More detailed error handling with UI feedback
                e(e, "Error loading level completion data")
                Toast.makeText(context, "Error loading level data: " + e.message, Toast.LENGTH_LONG)
                    .show()
                // Create an empty map as fallback
                completionDataMap = HashMap<Int?, LevelCompletionData>()
            }
        } else {
            d("No completion data found in SharedPreferences")
        }
    }

    var lastPlayedLevel: Int
        /**
         * Get the last played level ID
         * @return The last played level ID, or 1 if none was set
         */
        get() {
            val prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            return prefs.getInt(LAST_PLAYED_LEVEL_KEY, 1)
        }
        /**
         * Set the last played level ID
         * @param levelId The level ID that was last played
         */
        set(levelId) {
            val prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            prefs.edit().putInt(LAST_PLAYED_LEVEL_KEY, levelId)
                .apply()
            d("[LEVEL_COMPLETION] Set last played level to %d", levelId)
        }

    /**
     * Reset all level completion data
     */
    fun resetAll() {
        d("[LEVEL_COMPLETION] Resetting all level completion data")
        completionDataMap!!.clear()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        d("[LEVEL_COMPLETION] All level data reset successfully")
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
        d("[LEVEL_COMPLETION] Unlocking 1 star per level for first %d levels", numLevels)

        for (levelId in 1..numLevels) {
            val data = getLevelCompletionData(levelId)
            data!!.setCompleted(true)
            data.setStars(1)
            data.optimalMoves = 1
            data.timeNeeded = 1
            data.squaresSurpassed = 0
        }

        saveCompletionData()
        d(
            "[LEVEL_COMPLETION] 1 star per level unlocked for first %d levels successfully",
            numLevels
        )
    }

    /**
     * Save all completion data to SharedPreferences
     */
    private fun saveCompletionData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        try {
            val gson = Gson()
            val json = gson.toJson(completionDataMap)
            d("Saving completion data JSON: %s", json)
            editor.putString(COMPLETION_DATA_KEY, json)
            val success = editor.commit() // Use commit() instead of apply() to get immediate result
            d("Saved completion data for %d levels, success: %s", completionDataMap!!.size, success)
        } catch (e: Exception) {
            e(e, "Error saving level completion data")
        }
    }

    companion object {
        private const val PREFS_NAME = "level_completion_prefs"
        private const val COMPLETION_DATA_KEY = "completion_data"
        private const val LAST_PLAYED_LEVEL_KEY = "last_played_level"

        private var instance: LevelCompletionManager? = null

        @Synchronized
        fun getInstance(context: Context): LevelCompletionManager {
            if (instance == null) {
                instance = LevelCompletionManager(context)
            }
            return instance!!
        }
    }
}
