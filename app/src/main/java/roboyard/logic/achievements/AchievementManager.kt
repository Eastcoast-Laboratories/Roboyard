package roboyard.logic.achievements

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import roboyard.eclabs.BuildConfig
import roboyard.eclabs.R
import roboyard.logic.achievements.AchievementDefinitions.all
import roboyard.logic.achievements.AchievementDefinitions.getPlayGamesResourceKey
import roboyard.logic.core.Preferences
import roboyard.logic.managers.GameHistoryManager.findByWallSignature
import roboyard.logic.managers.GameHistoryManager.getUniqueCompletedLevelCount
import roboyard.logic.managers.GameHistoryManager.getUniqueThreeStarLevelCount
import roboyard.logic.managers.PlayGamesManager
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.network.RoboyardApiClient.AchievementFetchResult
import roboyard.logic.network.RoboyardApiClient.AchievementSyncResult
import roboyard.logic.network.RoboyardApiClient.ApiCallback
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.w
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

/**
 * Manages achievement unlocking, storage, and retrieval.
 */
class AchievementManager private constructor(context: Context) {
    private val context: Context
    private val prefs: SharedPreferences
    private val achievements: MutableMap<String?, Achievement?>?
    private var unlockListener: AchievementUnlockListener? = null
    private var currentActivity: WeakReference<Activity?>? = null

    // Counters for tracking progress
    private var levelsCompleted = 0
    private var perfectSolutions = 0
    private var threeStarLevels = 0
    private var threeStarHardLevels = 0
    private var impossibleModeGames = 0
    private var impossibleModeStreak = 0
    private var perfectRandomGames = 0
    private var perfectRandomGamesStreak = 0
    private var noHintRandomGames = 0
    private var noHintRandomGamesTotal = 0
    private var dailyLoginStreak = 0
    private var speedrunRandomGamesUnder30s = 0
    private var sameWallsMaxPositions = 0

    // Game session tracking
    private var hintUsedInCurrentGame = false

    // Robot touch tracking for gimme_five achievement
    // Stores pairs of robots that have touched each other (e.g., "0-1" means robot 0 touched robot 1)
    private val robotTouchPairs: MutableSet<String?> = HashSet<String?>()
    private var currentGameRobotCount = 0

    interface AchievementUnlockListener {
        fun onAchievementUnlocked(achievement: Achievement?)
    }

    fun setUnlockListener(listener: AchievementUnlockListener?) {
        this.unlockListener = listener
    }

    fun setCurrentActivity(activity: Activity?) {
        this.currentActivity = WeakReference<Activity?>(activity)
        // Show any pending update nudge now that we have an activity
        if (activity != null && pendingNudgeVersion != null) {
            showUpdateNudgeInternal(activity, pendingNudgeVersion)
            pendingNudgeVersion = null
        }
    }

    /**
     * Show update nudge on credits page - always shows, no cooldown.
     * Should be called when opening the credits/about screen.
     */
    fun showUpdateNudgeForCredits(activity: Activity?) {
        d(
            "[UPDATE_NUDGE_CREDITS] Called, activity=%s",
            if (activity != null) activity.javaClass.getSimpleName() else "null"
        )
        val latestAppVersion = prefs.getString(KEY_PENDING_NUDGE_VERSION, null)
        d("[UPDATE_NUDGE_CREDITS] Pending version from prefs: %s", latestAppVersion)
        if (latestAppVersion == null) {
            d("[UPDATE_NUDGE_CREDITS] No pending version stored, checking fallback...")
            return
        }
        val current = BuildConfig.VERSION_NAME
        d("[UPDATE_NUDGE_CREDITS] Comparing: current=%s, latest=%s", current, latestAppVersion)
        if (compareVersions(current, latestAppVersion) >= 0) {
            d("[UPDATE_NUDGE_CREDITS] App is up to date, not showing nudge")
            return  // up to date
        }
        i("[UPDATE_NUDGE_CREDITS] Showing nudge for version %s", latestAppVersion)
        showUpdateNudgeInternal(activity!!, latestAppVersion)
    }

    private fun showUpdateNudgeInternal(activity: Activity, version: String?) {
        val message = activity.getString(R.string.update_available_nudge, version)
        activity.runOnUiThread(Runnable {
            try {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                d("[UPDATE_NUDGE] Showed nudge: version=%s", version)
            } catch (e: Exception) {
                e(e, "[UPDATE_NUDGE] Failed to show toast")
            }
        })
    }

    /**
     * Progress snapshot for a counter-based achievement.
     * current == required means the achievement can be / is unlocked.
     */
    class AchievementProgress(@JvmField val current: Int, @JvmField val required: Int) {
        fun hasProgress(): Boolean {
            return required > 1
        }

        val isComplete: Boolean
            get() = current >= required
    }

    /**
     * Returns progress for counter-based achievements, or null for binary achievements.
     * This is the central method for progress display and auto-unlock at 100%.
     */
    fun getProgress(achievementId: String): AchievementProgress? {
        val uniqueCompletedLevels = this.uniqueCompletedLevelCount
        val uniqueThreeStarLevels = this.uniqueThreeStarLevelCount
        when (achievementId) {
            "level_10_complete" -> return AchievementProgress(uniqueCompletedLevels, 10)
            "level_50_complete" -> return AchievementProgress(uniqueCompletedLevels, 50)
            "level_140_complete" -> return AchievementProgress(uniqueCompletedLevels, 140)
            "perfect_solutions_5" -> return AchievementProgress(perfectSolutions, 5)
            "perfect_solutions_10" -> return AchievementProgress(perfectSolutions, 10)
            "perfect_solutions_50" -> return AchievementProgress(perfectSolutions, 50)
            "3_star_10_levels" -> return AchievementProgress(uniqueThreeStarLevels, 10)
            "3_star_50_levels" -> return AchievementProgress(uniqueThreeStarLevels, 50)
            "3_star_all_levels" -> return AchievementProgress(uniqueThreeStarLevels, 140)
            "3_star_10_hard_levels" -> return AchievementProgress(threeStarHardLevels, 10)
            "impossible_mode_5" -> return AchievementProgress(impossibleModeGames, 5)
            "impossible_mode_10" -> return AchievementProgress(impossibleModeGames, 10)
            "impossible_mode_streak_5" -> return AchievementProgress(impossibleModeStreak, 5)
            "impossible_mode_streak_10" -> return AchievementProgress(impossibleModeStreak, 10)
            "perfect_random_games_5" -> return AchievementProgress(perfectRandomGames, 5)
            "perfect_random_games_10" -> return AchievementProgress(perfectRandomGames, 10)
            "perfect_random_games_20" -> return AchievementProgress(perfectRandomGames, 20)
            "perfect_random_games_streak_5" -> return AchievementProgress(
                perfectRandomGamesStreak,
                5
            )

            "perfect_random_games_streak_10" -> return AchievementProgress(
                perfectRandomGamesStreak,
                10
            )

            "perfect_random_games_streak_20" -> return AchievementProgress(
                perfectRandomGamesStreak,
                20
            )

            "no_hints_random_10" -> return AchievementProgress(noHintRandomGamesTotal, 10)
            "no_hints_random_50" -> return AchievementProgress(noHintRandomGamesTotal, 50)
            "no_hints_streak_random_10" -> return AchievementProgress(noHintRandomGames, 10)
            "no_hints_streak_random_50" -> return AchievementProgress(noHintRandomGames, 50)
            "daily_login_7" -> return AchievementProgress(dailyLoginStreak, 7)
            "daily_login_30" -> return AchievementProgress(dailyLoginStreak, 30)
            "speedrun_random_5_games_under_30s" -> return AchievementProgress(
                speedrunRandomGamesUnder30s,
                5
            )

            "same_walls_2" -> return AchievementProgress(sameWallsMaxPositions, 2)
            "same_walls_10" -> return AchievementProgress(sameWallsMaxPositions, 5)
            "same_walls_100" -> return AchievementProgress(sameWallsMaxPositions, 10)
            else -> return null
        }
    }

    /**
     * Unlock an achievement if its counter-based progress is complete.
     * Uses getProgress() as the single source of truth for required thresholds.
     * @return true if newly unlocked
     */
    fun unlockIfComplete(achievementId: String): Boolean {
        val progress = getProgress(achievementId)
        if (progress != null && progress.isComplete) {
            return unlock(achievementId)
        }
        return false
    }


    /**
     * Migrate orphaned SharedPreferences keys written by a previous bug in syncFromServer().
     * The bug wrote keys as "id_unlocked" / "id_timestamp" instead of "unlocked_id" / "timestamp_id".
     * This migrates those keys to the correct format and removes the orphaned ones.
     */
    private fun migrateOrphanedSyncKeys() {
        val migrated = prefs.getBoolean("orphaned_keys_migrated", false)
        if (migrated) return

        val editor = prefs.edit()
        var migratedCount = 0

        for (id in achievements!!.keys) {
            // Check for orphaned keys: id + "_unlocked" (wrong format)
            val wrongUnlockedKey = id + "_unlocked"
            val wrongTimestampKey = id + "_timestamp"
            val correctUnlockedKey = KEY_PREFIX_UNLOCKED + id
            val correctTimestampKey = KEY_PREFIX_TIMESTAMP + id

            if (prefs.contains(wrongUnlockedKey)) {
                val unlocked = prefs.getBoolean(wrongUnlockedKey, false)
                val timestamp = prefs.getLong(wrongTimestampKey, 0)


                // Only migrate if the correct key doesn't already have a value
                if (unlocked && !prefs.getBoolean(correctUnlockedKey, false)) {
                    editor.putBoolean(correctUnlockedKey, true)
                    editor.putLong(correctTimestampKey, timestamp)
                    migratedCount++
                }


                // Remove orphaned keys
                editor.remove(wrongUnlockedKey)
                editor.remove(wrongTimestampKey)
            }
        }

        editor.putBoolean("orphaned_keys_migrated", true)
        editor.apply()

        if (migratedCount > 0) {
            d("[ACHIEVEMENTS] Migrated %d orphaned sync keys to correct format", migratedCount)
        }
    }

    private fun loadState() {
        // One-time migration: fix orphaned keys from buggy syncFromServer() 
        // that wrote "id_unlocked" instead of "unlocked_id"
        migrateOrphanedSyncKeys()


        // Load unlock status for all achievements
        for (achievement in achievements!!.values.filterNotNull()) {
            val unlocked = prefs.getBoolean(KEY_PREFIX_UNLOCKED + achievement.id, false)
            val timestamp = prefs.getLong(KEY_PREFIX_TIMESTAMP + achievement.id, 0)
            achievement.setUnlocked(unlocked)
            achievement.unlockedTimestamp = timestamp
        }


        // Load counters
        levelsCompleted = prefs.getInt(KEY_COUNTER_PREFIX + "levels_completed", 0)
        perfectSolutions = prefs.getInt(KEY_COUNTER_PREFIX + "perfect_solutions", 0)
        threeStarLevels = prefs.getInt(KEY_COUNTER_PREFIX + "three_star_levels", 0)
        threeStarHardLevels = prefs.getInt(KEY_COUNTER_PREFIX + "three_star_hard_levels", 0)
        impossibleModeGames = prefs.getInt(KEY_COUNTER_PREFIX + "impossible_mode_games", 0)
        impossibleModeStreak = prefs.getInt(KEY_COUNTER_PREFIX + "impossible_mode_streak", 0)
        perfectRandomGames = prefs.getInt(KEY_COUNTER_PREFIX + "perfect_random_games", 0)
        perfectRandomGamesStreak =
            prefs.getInt(KEY_COUNTER_PREFIX + "perfect_random_games_streak", 0)
        noHintRandomGames = prefs.getInt(KEY_COUNTER_PREFIX + "no_hint_random_games", 0)
        noHintRandomGamesTotal = prefs.getInt(KEY_COUNTER_PREFIX + "no_hint_random_games_total", 0)
        dailyLoginStreak = prefs.getInt(KEY_COUNTER_PREFIX + "daily_login_streak", 0)
        speedrunRandomGamesUnder30s = prefs.getInt(KEY_COUNTER_PREFIX + "speedrun_random_30s", 0)
        sameWallsMaxPositions = prefs.getInt(KEY_COUNTER_PREFIX + "same_walls_max_positions", 0)

        d(
            "[ACHIEVEMENTS] Loaded state: %d achievements, %d unlocked",
            achievements.size, this.unlockedCount
        )
    }

    private fun saveCounter(key: String?, value: Int) {
        prefs.edit().putInt(KEY_COUNTER_PREFIX + key, value).apply()
    }

    /**
     * Unlock an achievement by ID.
     * @return true if newly unlocked, false if already unlocked
     */
    fun unlock(achievementId: String): Boolean {
        val achievement = achievements!!.get(achievementId)
        if (achievement == null) {
            w("[ACHIEVEMENTS] Unknown achievement: %s", achievementId)
            return false
        }

        if (achievement.isUnlocked()) {
            return false // Already unlocked
        }

        achievement.setUnlocked(true)
        val timestamp = System.currentTimeMillis()
        achievement.unlockedTimestamp = timestamp


        // Save to SharedPreferences
        prefs.edit()
            .putBoolean(KEY_PREFIX_UNLOCKED + achievementId, true)
            .putLong(KEY_PREFIX_TIMESTAMP + achievementId, timestamp)
            .apply()

        d("[ACHIEVEMENTS] Unlocked: %s", achievementId)


        // Sync to Google Play Games if enabled
        syncToPlayGames(achievementId)


        // Sync to roboyard.z11.de server
        syncAfterUnlock()


        // Notify listener
        if (unlockListener != null) {
            unlockListener!!.onAchievementUnlocked(achievement)
        }

        return true
    }

    /**
     * Map local achievement ID to Google Play Games achievement ID.
     * Shared between Play and F-Droid flavors.
     * Uses the mapping defined in AchievementDefinitions.
     * 
     * @param localId The local achievement ID
     * @return The Google Play Games achievement ID, or null if not found
     */
    fun getPlayGamesAchievementId(localId: String?): String? {
        try {
            val resourceKey = getPlayGamesResourceKey(localId)
            if (resourceKey == null) {
                w("[ACHIEVEMENTS] Unknown achievement ID: %s", localId)
                return null
            }

            val resId = context.getResources()
                .getIdentifier(resourceKey, "string", context.getPackageName())
            if (resId == 0) {
                w("[ACHIEVEMENTS] String resource not found: %s", resourceKey)
                return null
            }

            return context.getString(resId)
        } catch (e: Exception) {
            e(e, "[ACHIEVEMENTS] Failed to get Play Games ID for: %s", localId)
            return null
        }
    }

    /**
     * Sync achievement unlock to Google Play Games Services.
     * Only works if ENABLE_PLAY_GAMES is true and user is signed in.
     */
    private fun syncToPlayGames(achievementId: String) {
        if (!BuildConfig.ENABLE_PLAY_GAMES) {
            return
        }

        val activity = if (currentActivity != null) currentActivity!!.get() else null
        if (activity == null) {
            d("[ACHIEVEMENTS] Cannot sync to Play Games - no activity set")
            return
        }

        try {
            val playGames = PlayGamesManager.getInstance(context)
            playGames.unlockAchievement(activity, achievementId)
            d("[ACHIEVEMENTS] Synced to Play Games: %s", achievementId)
        } catch (e: Exception) {
            e(e, "[ACHIEVEMENTS] Failed to sync to Play Games: %s", achievementId)
        }
    }

    fun isUnlocked(achievementId: String?): Boolean {
        val achievement = achievements!!.get(achievementId)
        return achievement != null && achievement.isUnlocked()
    }

    val allAchievements: MutableList<Achievement?>
        get() = ArrayList<Achievement?>(achievements!!.values)

    val totalCount: Int
        get() = achievements!!.size

    val unlockedCount: Int
        get() {
            var count = 0
            for (achievement in achievements!!.values.filterNotNull()) {
                if (achievement.isUnlocked()) {
                    count++
                }
            }
            return count
        }

    // ========== GAME EVENT HANDLERS ==========
    /**
     * Called when any game is completed (first game achievement)
     */
    fun onFirstGame() {
        unlock("first_game")
    }

    /**
     * Called when a level is completed
     */
    fun onLevelCompleted(
        levelId: Int, playerMoves: Int, optimalMoves: Int,
        hintsUsed: Int, stars: Int, timeMs: Long
    ) {
        // Log the levelId for debugging

        d(
            "[ACHIEVEMENTS] onLevelCompleted called: levelId=%d, levelsCompleted=%d->%d, playerMoves=%d, optimalMoves=%d, hintsUsed=%d, stars=%d, time=%dms",
            levelId,
            levelsCompleted,
            levelsCompleted + 1,
            playerMoves,
            optimalMoves,
            hintsUsed,
            stars,
            timeMs
        )

        val uniqueCompletedLevelsBefore = this.uniqueCompletedLevelCount
        if (uniqueCompletedLevelsBefore <= levelsCompleted) {
            d(
                "[ACHIEVEMENTS][LEVEL] Skipping duplicate level completion for levelId=%d (history count=%d, stored=%d)",
                levelId, uniqueCompletedLevelsBefore, levelsCompleted
            )
            return
        }


        // First game achievement (any game completion)
        unlock("first_game")

        levelsCompleted = uniqueCompletedLevelsBefore
        saveCounter("levels_completed", levelsCompleted)


        // Level progression achievements
        if (levelId >= 1) unlock("level_1_complete")
        unlockIfComplete("level_10_complete")
        unlockIfComplete("level_50_complete")
        unlockIfComplete("level_140_complete")


        // Perfect solution (optimalMoves must be > 0, i.e. solver result available)
        if (optimalMoves > 0 && playerMoves == optimalMoves) {
            perfectSolutions++
            saveCounter("perfect_solutions", perfectSolutions)
            d(
                "[ACHIEVEMENTS][PERFECT] Level %d: perfect solution counted! total=%d (playerMoves=%d == optimalMoves=%d)",
                levelId, perfectSolutions, playerMoves, optimalMoves
            )
            unlockIfComplete("perfect_solutions_5")
            unlockIfComplete("perfect_solutions_10")
            unlockIfComplete("perfect_solutions_50")
        } else if (optimalMoves <= 0) {
            w(
                "[ACHIEVEMENTS][PERFECT] Level %d: optimalMoves=%d (solver not ready?), perfect solution NOT counted!",
                levelId, optimalMoves
            )
        } else {
            d(
                "[ACHIEVEMENTS][PERFECT] Level %d: not perfect (playerMoves=%d, optimalMoves=%d), total=%d",
                levelId, playerMoves, optimalMoves, perfectSolutions
            )
        }


        // Note: no_hints_10 and no_hints_50 removed - hints are not allowed in levels

        // 3 stars achievements
        if (stars >= 3) {
            threeStarLevels++
            saveCounter("three_star_levels", threeStarLevels)


            // 3_star_hard_level only unlocks for levels with 5+ optimal moves
            if (optimalMoves >= 5) {
                unlock("3_star_hard_level")
                threeStarHardLevels++
                saveCounter("three_star_hard_levels", threeStarHardLevels)
                unlockIfComplete("3_star_10_hard_levels")
            }


            // Other 3-star achievements count all levels regardless of move count
            unlockIfComplete("3_star_10_levels")
            unlockIfComplete("3_star_50_levels")
            unlockIfComplete("3_star_all_levels")
        }


        // Speedrun
        if (timeMs < 30000) unlock("speedrun_under_30s")
        if (timeMs < 10000) unlock("speedrun_under_10s")

        d(
            "[ACHIEVEMENTS] Level %d completed: moves=%d/%d, hints=%d, stars=%d, time=%dms",
            levelId, playerMoves, optimalMoves, hintsUsed, stars, timeMs
        )
    }


    /**
     * Called when a random game is completed
     * @param playerMoves number of moves made by the player
     * @param optimalMoves number of optimal moves for the game
     * @param hintsUsed number of hints used in the current game session (history-wide tracking is done via qualifiesForNoHints)
     * @param timeMs time taken to complete the game in milliseconds
     * @param isImpossibleMode true if the game was played in impossible mode
     * @param robotCount number of robots in the game
     * @param targetCount number of targets in the game
     * @param targetsNeeded number of targets needed to complete the game
     * @param isFirstCompletion true if this is the first time this exact map is completed (unique map)
     * @param qualifiesForNoHints true if map qualifies for no-hints achievements (first solve was hint-free)
     * @param wallSignature wall-layout signature for same-walls tracking (may be null)
     */
    fun onRandomGameCompleted(
        playerMoves: Int, optimalMoves: Int, hintsUsed: Int,
        timeMs: Long, isImpossibleMode: Boolean, robotCount: Int,
        targetCount: Int, targetsNeeded: Int,
        isFirstCompletion: Boolean, qualifiesForNoHints: Boolean,
        wallSignature: String?
    ) {
        // Check if hint was used during this game session

        var hintsUsed = hintsUsed
        if (hintUsedInCurrentGame) {
            hintsUsed = max(hintsUsed, 1) // Ensure hintsUsed reflects that a hint was used
            d("[ACHIEVEMENTS] Hint was used during this game session")
        }

        d(
            "[ACHIEVEMENTS] onRandomGameCompleted: isFirstCompletion=%b, qualifiesForNoHints=%b, hintsUsed=%d",
            isFirstCompletion, qualifiesForNoHints, hintsUsed
        )


        // First game
        onFirstGame()


        // Impossible mode - only count if optimal moves >= 17 AND first completion (unique map)
        if (isImpossibleMode && optimalMoves >= 17 && isFirstCompletion) {
            impossibleModeGames++
            saveCounter("impossible_mode_games", impossibleModeGames)
            unlock("impossible_mode_1")
            unlockIfComplete("impossible_mode_5")
            unlockIfComplete("impossible_mode_10")


            // Impossible mode streak (perfect solutions)
            if (playerMoves == optimalMoves) {
                impossibleModeStreak++
                saveCounter("impossible_mode_streak", impossibleModeStreak)
                unlockIfComplete("impossible_mode_streak_5")
                unlockIfComplete("impossible_mode_streak_10")
            } else {
                impossibleModeStreak = 0
                saveCounter("impossible_mode_streak", 0)
            }
            d(
                "[ACHIEVEMENTS] Impossible mode game counted (optimalMoves=%d >= 17, isFirstCompletion=true)",
                optimalMoves
            )
        } else if (isImpossibleMode && optimalMoves >= 17 && !isFirstCompletion) {
            d("[ACHIEVEMENTS] Impossible mode game NOT counted - map already completed before")
        } else {
            d(
                "[ACHIEVEMENTS] Impossible mode game NOT counted (optimalMoves=%d < 17), isImpossibleMode=%b",
                optimalMoves,
                isImpossibleMode
            )
        }


        // Solution length achievements (18-29 moves individually, 30+ as one)
        // Only unlock on FIRST completion AND without hint usage (current + history) AND with optimal play
        if (isFirstCompletion && qualifiesForNoHints && playerMoves == optimalMoves) {
            if (optimalMoves >= 18 && optimalMoves <= 29) {
                unlock("solution_" + optimalMoves + "_moves")
            } else if (optimalMoves >= 30) {
                unlock("solution_30_plus_moves")
            }
        } else if (!isFirstCompletion) {
            d("[ACHIEVEMENTS] Solution length achievements skipped - map already completed before")
        } else if (!qualifiesForNoHints) {
            d("[ACHIEVEMENTS] Solution length achievements skipped - hints were used")
        } else {
            d(
                "[ACHIEVEMENTS] Solution length achievements skipped - not optimal (playerMoves=%d, optimalMoves=%d)",
                playerMoves,
                optimalMoves
            )
        }


        // Multiple targets
        if (targetCount >= 2) unlock("game_2_targets")
        if (targetCount >= 3) unlock("game_3_targets")
        if (targetCount >= 4) unlock("game_4_targets")


        // X of Y targets
        if (targetsNeeded == 2 && targetCount == 2) unlock("game_2_of_2_targets")
        if (targetsNeeded == 2 && targetCount == 3) unlock("game_2_of_3_targets")
        if (targetsNeeded == 2 && targetCount == 4) unlock("game_2_of_4_targets")
        if (targetsNeeded == 3 && targetCount == 3) unlock("game_3_of_3_targets")
        if (targetsNeeded == 3 && targetCount == 4) unlock("game_3_of_4_targets")
        if (targetsNeeded == 4 && targetCount == 4) unlock("game_4_of_4_targets")


        // Fun Challenges
        if (robotCount >= 5) unlock("game_5_robots")


        // Perfect random games - only count UNIQUE maps (first completion)
        if (playerMoves == optimalMoves && isFirstCompletion) {
            perfectRandomGames++
            saveCounter("perfect_random_games", perfectRandomGames)
            unlockIfComplete("perfect_random_games_5")
            unlockIfComplete("perfect_random_games_10")
            unlockIfComplete("perfect_random_games_20")


            // Perfect random games streak (resets on non-optimal)
            perfectRandomGamesStreak++
            saveCounter("perfect_random_games_streak", perfectRandomGamesStreak)
            unlockIfComplete("perfect_random_games_streak_5")
            unlockIfComplete("perfect_random_games_streak_10")
            unlockIfComplete("perfect_random_games_streak_20")
            d(
                "[ACHIEVEMENTS] Perfect game on unique map - total: %d, streak: %d",
                perfectRandomGames,
                perfectRandomGamesStreak
            )
        } else if (playerMoves == optimalMoves && !isFirstCompletion) {
            d("[ACHIEVEMENTS] Perfect game NOT counted - map already completed before")
        } else {
            // Reset streak when non-optimal
            perfectRandomGamesStreak = 0
            saveCounter("perfect_random_games_streak", perfectRandomGamesStreak)
            d("[ACHIEVEMENTS] Non-optimal game - perfect streak reset to 0")
        }


        // Perfect solution with no hints (10+ moves optimal) - only on FIRST completion
        if (playerMoves == optimalMoves && qualifiesForNoHints && optimalMoves >= 10 && isFirstCompletion) {
            unlock("perfect_no_hints_random_1")
            d(
                "[ACHIEVEMENTS] Perfect no hints achievement unlocked - optimal: %d moves, qualifiesForNoHints=true",
                optimalMoves
            )
        }


        // No hints random games - only count UNIQUE maps with qualifiesForNoHints
        if (qualifiesForNoHints && isFirstCompletion) {
            // Cumulative counter (never resets)
            noHintRandomGamesTotal++
            saveCounter("no_hint_random_games_total", noHintRandomGamesTotal)
            unlockIfComplete("no_hints_random_10")
            unlockIfComplete("no_hints_random_50")


            // Streak counter (resets on hint usage)
            noHintRandomGames++
            saveCounter("no_hint_random_games", noHintRandomGames)
            unlockIfComplete("no_hints_streak_random_10")
            unlockIfComplete("no_hints_streak_random_50")
            d(
                "[ACHIEVEMENTS] No hints on unique map - total: %d, streak: %d",
                noHintRandomGamesTotal,
                noHintRandomGames
            )
        } else if (!qualifiesForNoHints) {
            // Reset streak counter when hints were used (on this or previous completion)
            noHintRandomGames = 0
            saveCounter("no_hint_random_games", noHintRandomGames)
            d(
                "[ACHIEVEMENTS] Hints used - no_hint streak reset to 0 (total stays: %d)",
                noHintRandomGamesTotal
            )
        } else if (!isFirstCompletion) {
            d("[ACHIEVEMENTS] No hints NOT counted - map already completed before")
        }


        // Same-walls achievements: count unique position-signatures sharing the same wall layout
        if (wallSignature != null && !wallSignature.isEmpty() && currentActivity != null) {
            val act = currentActivity!!.get()
            if (act != null) {
                val sameWallEntries =
                    findByWallSignature(act, wallSignature)
                val uniquePositions =
                    sameWallEntries.size // each entry = distinct positionSignature
                d(
                    "[ACHIEVEMENTS] same_walls: wallSig=%s uniquePositions=%d",
                    wallSignature.substring(0, min(30, wallSignature.length)), uniquePositions
                )
                if (uniquePositions > sameWallsMaxPositions) {
                    sameWallsMaxPositions = uniquePositions
                    saveCounter("same_walls_max_positions", sameWallsMaxPositions)
                }
                unlockIfComplete("same_walls_2")
                unlockIfComplete("same_walls_10")
                unlockIfComplete("same_walls_100")
            }
        }

        // Speed achievements
        if (timeMs < 20000) unlock("speedrun_random_under_20s")
        if (timeMs < 10000) unlock("speedrun_random_under_10s")
        if (timeMs < 30000) {
            speedrunRandomGamesUnder30s++
            saveCounter("speedrun_random_30s", speedrunRandomGamesUnder30s)
            unlockIfComplete("speedrun_random_5_games_under_30s")
        }

        d(
            "[ACHIEVEMENTS] Random game completed: moves=%d/%d, hints=%d, time=%dms, impossible=%s, robots=%d, targets=%d/%d",
            playerMoves,
            optimalMoves,
            hintsUsed,
            timeMs,
            isImpossibleMode,
            robotCount,
            targetsNeeded,
            targetCount
        )
    }

    /**
     * Track robot-to-robot collision for gimme_five achievement.
     * Called when a robot hits another robot (hit_robot sound plays).
     * 
     * @param movingRobotIndex The index of the robot that moved and hit another (0-4)
     * @param hitRobotIndex The index of the robot that was hit (0-4)
     * @param robotCount Total number of robots in the game
     */
    fun onRobotTouched(movingRobotIndex: Int, hitRobotIndex: Int, robotCount: Int) {
        if (movingRobotIndex < 0 || hitRobotIndex < 0 || movingRobotIndex >= robotCount || hitRobotIndex >= robotCount || robotCount < 2 || robotCount > 5) {
            return
        }

        currentGameRobotCount = robotCount


        // Store the touch pair (normalized so 0-1 and 1-0 are the same)
        val minIndex = min(movingRobotIndex, hitRobotIndex)
        val maxIndex = max(movingRobotIndex, hitRobotIndex)
        val touchPair = minIndex.toString() + "-" + maxIndex

        val isNewTouch = robotTouchPairs.add(touchPair)
        if (isNewTouch) {
            d(
                "[ACHIEVEMENTS] Robot %d touched robot %d (pair: %s)",
                movingRobotIndex,
                hitRobotIndex,
                touchPair
            )
        }


        // Check if all robots have touched each other
        // For n robots, we need n*(n-1)/2 unique pairs
        val requiredPairs = (robotCount * (robotCount - 1)) / 2

        if (robotTouchPairs.size >= requiredPairs) {
            unlock("gimme_five")
            d(
                "[ACHIEVEMENTS] All %d robots have touched each other (%d pairs) - gimme_five unlocked!",
                robotCount, robotTouchPairs.size
            )
        } else {
            d(
                "[ACHIEVEMENTS] Robot touch progress: %d/%d pairs",
                robotTouchPairs.size,
                requiredPairs
            )
        }
    }

    val robotTouchProgress: IntArray?
        /**
         * Get current robot touch progress for debugging/UI.
         * @return Array with [currentPairs, requiredPairs]
         */
        get() {
            val requiredPairs = (currentGameRobotCount * (currentGameRobotCount - 1)) / 2
            return intArrayOf(robotTouchPairs.size, requiredPairs)
        }

    /**
     * Reset robot touch tracking for a new game.
     */
    private fun resetRobotTouchTracking() {
        robotTouchPairs.clear()
        currentGameRobotCount = 0
    }

    /**
     * Called when a new game starts (level or random game).
     * Resets the game session tracking flags and checks daily login achievements.
     */
    fun onNewGameStarted() {
        hintUsedInCurrentGame = false
        resetRobotTouchTracking()


        // Check daily login achievements
        unlockIfComplete("daily_login_7")
        unlockIfComplete("daily_login_30")

        d("[ACHIEVEMENTS] New game started - session flags reset")
    }

    /**
     * For testing only This should only be used in unit tests.
     */
    fun setTestMode(enabled: Boolean) {
        d("[ACHIEVEMENTS] Test mode enabled")
    }

    /**
     * Called when the hint button is pressed during a game.
     * This tracks hint usage for the current game session.
     */
    fun onHintUsed() {
        hintUsedInCurrentGame = true
        d("[ACHIEVEMENTS] Hint used in current game session")
    }


    /**
     * Called when a custom level is created
     */
    fun onCustomLevelCreated() {
        unlock("create_custom_level")
    }

    /**
     * Called when a custom level is solved
     */
    fun onCustomLevelSolved() {
        unlock("solve_custom_level")
    }

    /**
     * Called when a custom level is shared
     */
    fun onCustomLevelShared() {
        unlock("share_custom_level")
    }

    /**
     * Called on daily login - only updates streak counter, doesn't unlock achievements
     * Achievements are unlocked when player starts a game (onNewGameStarted)
     */
    fun onDailyLogin(streakDays: Int) {
        dailyLoginStreak = streakDays
        saveCounter("daily_login_streak", dailyLoginStreak)
        d("[ACHIEVEMENT] Daily login recorded - streak: %d days", streakDays)
    }

    /**
     * Update daily login streak from server sync - keeps AchievementManager in sync with StreakManager
     */
    fun updateDailyLoginStreak(streakDays: Int) {
        val beforeStreak = dailyLoginStreak
        dailyLoginStreak = streakDays
        saveCounter("daily_login_streak", dailyLoginStreak)
        d(
            "[ACHIEVEMENT] Daily login streak updated from sync - streak: %d days (was: %d)",
            streakDays,
            beforeStreak
        )
    }

    /**
     * Called when player starts a new game - check and unlock login streak achievements
     */
    fun checkAndUnlockStreakAchievements() {
        val streakDays = dailyLoginStreak
        unlockIfComplete("daily_login_7")
        unlockIfComplete("daily_login_30")
        d(
            "[ACHIEVEMENT] Checked Login Streak achievements at game start - streak: %d days",
            streakDays
        )
    }

    /**
     * Called when player returns after inactivity
     */
    fun onComebackPlayer(daysAway: Int) {
        if (daysAway >= 30) {
            unlock("comeback_player")
        }
    }

    /**
     * Called when all squares are traversed.
     * 
     * 4 achievements:
     * - traverse_all_squares_1_robot: One robot visits all squares (after goal allowed)
     * - traverse_all_squares_1_robot_goal: One robot visits all squares, goal must be last
     * - traverse_all_squares_all_robots: All robots visit all squares (after goal allowed)
     * - traverse_all_squares_all_robots_goal: All robots visit all squares, goal must be last
     */
    fun onAllSquaresTraversed(
        oneRobot: Boolean, oneRobotGoal: Boolean,
        allRobots: Boolean, allRobotsGoal: Boolean
    ) {
        if (oneRobot) unlock("traverse_all_squares_1_robot")
        if (oneRobotGoal) unlock("traverse_all_squares_1_robot_goal")
        if (allRobots) unlock("traverse_all_squares_all_robots")
        if (allRobotsGoal) unlock("traverse_all_squares_all_robots_goal")
    }

    /**
     * Unlock all achievements (for testing/debug)
     */
    fun unlockAll() {
        for (achievement in achievements!!.values.filterNotNull()) {
            unlock(achievement.id!!)
        }
        d("[ACHIEVEMENTS] All achievements unlocked")
    }

    /**
     * Lock an achievement by ID (for testing/debug)
     */
    fun lock(achievementId: String?) {
        val achievement = achievements!!.get(achievementId)
        if (achievement != null) {
            achievement.setUnlocked(false)
            achievement.unlockedTimestamp = 0
            prefs.edit().putBoolean(achievementId, false).apply()
            d("[ACHIEVEMENTS] Achievement locked: %s", achievementId)
        }
    }

    /**
     * Reset all achievements (for testing)
     */
    fun resetAll() {
        prefs.edit().clear().apply()
        for (achievement in achievements!!.values.filterNotNull()) {
            achievement.setUnlocked(false)
            achievement.unlockedTimestamp = 0
        }
        levelsCompleted = 0
        perfectSolutions = 0
        threeStarLevels = 0
        impossibleModeGames = 0
        impossibleModeStreak = 0
        perfectRandomGames = 0
        perfectRandomGamesStreak = 0
        noHintRandomGames = 0
        noHintRandomGamesTotal = 0
        dailyLoginStreak = 0
        speedrunRandomGamesUnder30s = 0
        sameWallsMaxPositions = 0
    }

    private val uniqueCompletedLevelCount: Int
        get() {
            val activity = if (currentActivity != null) currentActivity!!.get() else null
            if (activity == null) {
                return levelsCompleted
            }
            return getUniqueCompletedLevelCount(activity)
        }

    private val uniqueThreeStarLevelCount: Int
        get() {
            val activity = if (currentActivity != null) currentActivity!!.get() else null
            if (activity == null) {
                return threeStarLevels
            }
            return getUniqueThreeStarLevelCount(activity)
        }

    /**
     * Sync achievement unlock to the server after unlock.
     * No-op if user is not authenticated.
     */
    // ========== SERVER SYNC ==========
    /**
     * Sync all achievements to roboyard.z11.de server.
     * Only syncs if user is logged in.
     */
    fun syncToServer() {
        val apiClient = RoboyardApiClient.getInstance(context)
        if (!apiClient.isLoggedIn) {
            d("[ACHIEVEMENT_SYNC] Not logged in, skipping sync")
            return
        }

        try {
            // Build achievements array
            val achievementsArray = JSONArray()
            for (achievement in achievements!!.values.filterNotNull()) {
                val achievementJson = JSONObject()
                achievementJson.put("id", achievement.id)
                achievementJson.put("unlocked", achievement.isUnlocked())
                achievementJson.put("unlocked_timestamp", achievement.unlockedTimestamp)
                achievementsArray.put(achievementJson)
            }


            // Build stats object
            val stats = JSONObject()
            stats.put("total_games_solved", levelsCompleted + perfectRandomGames)
            stats.put("total_games_solved_no_hints", noHintRandomGamesTotal)
            stats.put("total_perfect_solutions", perfectSolutions + perfectRandomGames)


            // Include streak data for bidirectional sync
            val streakManager = StreakManager.getInstance(context)
            stats.put("daily_login_streak", streakManager.currentStreak)
            stats.put("last_login_date", streakManager.lastLoginDateString)
            stats.put("last_streak_date", streakManager.lastLoginDateString)
            stats.put("longest_streak", streakManager.longestStreak)
            stats.put("longest_streak_date", streakManager.longestStreakDate)
            stats.put("timezone", TimeZone.getDefault().getID())

            // Device / app metadata for server-side rankings and analytics
            // system_language: TRUE device locale, bypassing the app-level Locale override done
            //                  by RoboyardApplication.updateAppContextLocale().
            //                  Reads from Resources.getSystem() which is not affected by app overrides.
            // app_language:    user-chosen language preference from Settings (Preferences.appLanguage).
            val systemLanguage: String? = systemLanguageTag
            val appLanguage = Preferences.appLanguage
            val appVersion = BuildConfig.VERSION_NAME
            val androidVersion = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
            if (systemLanguage != null) stats.put("system_language", systemLanguage)
            if (appLanguage != null && !appLanguage.isEmpty()) stats.put(
                "app_language",
                appLanguage
            )
            stats.put("app_version", appVersion)
            stats.put("android_version", androidVersion)

            d(
                "[ACHIEVEMENT_SYNC_UP] Uploading: streak=%d, last_login_date=%s, longest=%d, timezone=%s, sysLang=%s, appLang=%s, app=%s, android=%s",
                streakManager.currentStreak, streakManager.lastLoginDateString,
                streakManager.longestStreak, TimeZone.getDefault().getID(),
                systemLanguage, appLanguage, appVersion, androidVersion
            )


            // Send to server
            apiClient.syncAchievements(
                achievementsArray,
                stats,
                object : ApiCallback<AchievementSyncResult?> {
                    override fun onSuccess(result: AchievementSyncResult?) {
                        d(
                            "[ACHIEVEMENT_SYNC] Sync successful: %d synced, %d new achievements",
                            result?.syncedCount, result?.newAchievements
                        )
                        // Optional "update available" nudge if server reports a newer version
                        d("[UPDATE_NUDGE] Latest app version: %s", result?.latestAppVersion)
                        if (result?.latestAppVersion != null) {
                            maybeShowUpdateNudge(result.latestAppVersion)
                        }
                    }

                    override fun onError(error: String?) {
                        e("[ACHIEVEMENT_SYNC] Sync failed: %s", error)
                    }
                })
        } catch (e: JSONException) {
            e(e, "[ACHIEVEMENT_SYNC] Failed to build sync request")
        }
    }

    private var pendingNudgeVersion: String? = null

    init {
        this.context = context.getApplicationContext()
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        this.achievements = all
        loadState()
    }

    /**
     * If the server reports a newer published app version than the installed one,
     * show a Toast nudge — but only once per (version, 24h) window to avoid spam.
     */
    private fun maybeShowUpdateNudge(latestAppVersion: String?) {
        val current = BuildConfig.VERSION_NAME
        d("[UPDATE_NUDGE] Checking: current=%s, latest=%s", current, latestAppVersion)

        if (latestAppVersion == null || latestAppVersion.isEmpty()) {
            d("[UPDATE_NUDGE] Skipping: latestAppVersion is null or empty")
            return
        }

        val cmp: Int = compareVersions(current, latestAppVersion)
        d("[UPDATE_NUDGE] Version compare result: %d (negative=update available)", cmp)

        if (cmp >= 0) {
            d("[UPDATE_NUDGE] Skipping: current >= latest (no update needed)")
            return  // we're up to date or ahead (dev builds)
        }

        val lastNudgedVersion = prefs.getString(KEY_LAST_NUDGED_VERSION, null)
        val lastNudgeMs = prefs.getLong(KEY_LAST_NUDGE_MS, 0L)
        val now = System.currentTimeMillis()
        val elapsed = now - lastNudgeMs
        val sameVersion = latestAppVersion == lastNudgedVersion

        d(
            "[UPDATE_NUDGE] Cooldown check: lastNudgedVersion=%s, sameVersion=%b, elapsed=%d ms, cooldown=%d ms",
            lastNudgedVersion, sameVersion, elapsed, NUDGE_COOLDOWN_MS
        )

        // Always store the latest version for credits page (independent of cooldown)
        prefs.edit().putString(KEY_PENDING_NUDGE_VERSION, latestAppVersion).apply()
        d("[UPDATE_NUDGE] Stored pending version for credits: %s", latestAppVersion)

        // Check cooldown - only affects automatic nudge at app start, not credits page
        if (sameVersion && elapsed < NUDGE_COOLDOWN_MS) {
            d(
                "[UPDATE_NUDGE] Cooldown active, storing for later. remaining=%d ms",
                NUDGE_COOLDOWN_MS - elapsed
            )
            pendingNudgeVersion = latestAppVersion
            return
        }

        i("[UPDATE_NUDGE] Conditions met - will show nudge for version %s", latestAppVersion)
        prefs.edit()
            .putString(KEY_LAST_NUDGED_VERSION, latestAppVersion)
            .putLong(KEY_LAST_NUDGE_MS, now)
            .apply()

        val act = if (currentActivity != null) currentActivity!!.get() else null
        if (act == null) {
            d(
                "[UPDATE_NUDGE] No activity available, storing pending nudge for version=%s",
                latestAppVersion
            )
            pendingNudgeVersion = latestAppVersion
            return
        }
        showUpdateNudgeInternal(act, latestAppVersion)
    }

    /**
     * Sync achievements to server after unlocking.
     * Called automatically when an achievement is unlocked.
     */
    private fun syncAfterUnlock() {
        // Delay sync slightly to batch multiple unlocks
        Handler(Looper.getMainLooper()).postDelayed(Runnable { this.syncToServer() }, 2000)
    }

    /**
     * Sync achievements FROM server to local device.
     * Used after login to restore achievements on a new device.
     * Merges server achievements with local ones (union - never removes local achievements).
     * 
     * @param callback Optional callback for sync result
     */
    fun syncFromServer(callback: ApiCallback<Int?>?) {
        val apiClient = RoboyardApiClient.getInstance(context)
        if (!apiClient.isLoggedIn) {
            d("[ACHIEVEMENT_SYNC_DOWN] Not logged in, skipping download")
            if (callback != null) callback.onError("Not logged in")
            return
        }

        d("[ACHIEVEMENT_SYNC_DOWN] Starting achievement download from server")

        apiClient.fetchAchievements(object : ApiCallback<AchievementFetchResult?> {
            override fun onSuccess(result: AchievementFetchResult?) {
                var restoredCount = 0

                try {
                    val serverAchievements = result?.achievements
                    d(
                        "[ACHIEVEMENT_SYNC_DOWN] Received %d achievements from server",
                        serverAchievements!!.length()
                    )

                    for (i in 0..<serverAchievements.length()) {
                        val serverAchievement = serverAchievements.getJSONObject(i)
                        val id = serverAchievement.getString("id")
                        val unlocked = serverAchievement.optBoolean("unlocked", false)
                        val unlockedAt = serverAchievement.optString("unlocked_at", null)

                        if (!unlocked) continue


                        // Check if we have this achievement locally
                        val localAchievement = achievements!!.get(id)
                        if (localAchievement == null) {
                            d("[ACHIEVEMENT_SYNC_DOWN] Unknown achievement ID from server: %s", id)
                            continue
                        }


                        // Only restore if not already unlocked locally
                        if (!localAchievement.isUnlocked()) {
                            var timestamp: Long = 0
                            if (unlockedAt != null && !unlockedAt.isEmpty()) {
                                try {
                                    // Parse ISO 8601 timestamp
                                    val sdf =
                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                                    val date = sdf.parse(unlockedAt)
                                    if (date != null) {
                                        timestamp = date.getTime()
                                    }
                                } catch (e: Exception) {
                                    w(
                                        "[ACHIEVEMENT_SYNC_DOWN] Could not parse timestamp for %s: %s",
                                        id,
                                        unlockedAt
                                    )
                                    timestamp = System.currentTimeMillis()
                                }
                            }

                            localAchievement.setUnlocked(true)
                            localAchievement.unlockedTimestamp =
                                if (timestamp > 0) timestamp else System.currentTimeMillis()


                            // Save to SharedPreferences
                            prefs.edit()
                                .putBoolean(KEY_PREFIX_UNLOCKED + id, true)
                                .putLong(
                                    KEY_PREFIX_TIMESTAMP + id,
                                    localAchievement.unlockedTimestamp
                                )
                                .apply()

                            restoredCount++
                            d("[ACHIEVEMENT_SYNC_DOWN] Restored achievement: %s", id)
                        }
                    }

                    d(
                        "[ACHIEVEMENT_SYNC_DOWN] Download complete: %d achievements restored",
                        restoredCount
                    )


                    // Restore streak data from server (bidirectional)
                    if (result.stats != null) {
                        val serverStreak = result.stats.optInt("daily_login_streak", 0)
                        val serverLongestStreak = result.stats.optInt("longest_streak", 0)
                        // Use last_login_date with fallback to last_streak_date (for users who synced before last_login_date was introduced)
                        var serverLastLoginDate =
                            if (result.stats.isNull("last_login_date")) null else result.stats.optString(
                                "last_login_date",
                                null
                            )
                        if (serverLastLoginDate == null) {
                            serverLastLoginDate =
                                if (result.stats.isNull("last_streak_date")) null else result.stats.optString(
                                    "last_streak_date",
                                    null
                                )
                        }
                        val serverLongestStreakDate =
                            if (result.stats.isNull("longest_streak_date")) null else result.stats.optString(
                                "longest_streak_date",
                                null
                            )
                        StreakManager.getInstance(context).restoreFromServer(
                            serverStreak,
                            serverLastLoginDate,
                            serverLongestStreak,
                            serverLongestStreakDate
                        )
                    }
                } catch (e: JSONException) {
                    e(e, "[ACHIEVEMENT_SYNC_DOWN] Error parsing server achievements")
                    if (callback != null) callback.onError("Error parsing achievements: " + e.message)
                    return
                }

                if (callback != null) callback.onSuccess(restoredCount)
            }

            override fun onError(error: String?) {
                e("[ACHIEVEMENT_SYNC_DOWN] Download failed: %s", error)
                if (callback != null) callback.onError(error)
            }
        })
    }

    companion object {
        private const val PREFS_NAME = "roboyard_achievements"
        private const val KEY_PREFIX_UNLOCKED = "unlocked_"
        private const val KEY_PREFIX_TIMESTAMP = "timestamp_"
        private const val KEY_COUNTER_PREFIX = "counter_"

        private var instance: AchievementManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): AchievementManager {
            if (instance == null) {
                instance = AchievementManager(context)
            }
            return instance!!
        }

        private val systemLanguageTag: String?
            /**
             * Get the true device/system language tag, ignoring any app-level Locale override
             * done via `Locale.setDefault(...)` (e.g. in RoboyardApplication.updateAppContextLocale()).
             * Uses Resources.getSystem() which is backed by the framework config, not the app config.
             */
            get() {
                try {
                    val sysConfig =
                        Resources.getSystem().getConfiguration()
                    val locale: Locale?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val locales = sysConfig.getLocales()
                        if (locales == null || locales.isEmpty()) return null
                        locale = locales.get(0)
                    } else {
                        locale = sysConfig.locale
                    }
                    if (locale == null) return null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        return locale.toLanguageTag()
                    }
                    // Fallback for very old API levels — simple "lang-REGION" join
                    val lang = locale.getLanguage()
                    val country = locale.getCountry()
                    return if (country == null || country.isEmpty()) lang else (lang + "-" + country)
                } catch (e: Exception) {
                    w(e, "[LOCALE] Failed to read system language tag")
                    return null
                }
            }

        // Prefs key for dedup of the update nudge: we store "last_nudged_version"
        private const val KEY_LAST_NUDGED_VERSION = "last_nudged_version"
        private const val KEY_LAST_NUDGE_MS = "last_nudge_ms"
        private val NUDGE_COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000 // once per week per version

        // Pending nudge when no activity available at sync time
        private const val KEY_PENDING_NUDGE_VERSION = "pending_update_nudge_version"

        /**
         * Compare two dotted version strings ("3.14.0" vs "3.15.1").
         * Returns negative if a < b, 0 if equal, positive if a > b.
         * Non-numeric segments are compared lexicographically.
         */
        private fun compareVersions(a: String?, b: String?): Int {
            if (a == null || b == null) return 0
            val pa = a.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val pb = b.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val n = max(pa.size, pb.size)
            for (i in 0..<n) {
                val sa = if (i < pa.size) pa[i] else "0"
                val sb = if (i < pb.size) pb[i] else "0"
                var na: Int
                var nb: Int
                try {
                    na = sa.replace("\\D".toRegex(), "").toInt()
                } catch (e: NumberFormatException) {
                    na = 0
                }
                try {
                    nb = sb.replace("\\D".toRegex(), "").toInt()
                } catch (e: NumberFormatException) {
                    nb = 0
                }
                if (na != nb) return Integer.compare(na, nb)
            }
            return 0
        }
    }
}
