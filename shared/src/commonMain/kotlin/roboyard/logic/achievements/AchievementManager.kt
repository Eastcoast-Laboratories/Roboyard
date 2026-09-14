package roboyard.logic.achievements

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import roboyard.logic.achievements.AchievementDefinitions.all
import roboyard.logic.achievements.AchievementDefinitions.getPlayGamesResourceKey
import roboyard.logic.core.Preferences
import roboyard.logic.managers.GameHistoryManager.findByWallSignature
import roboyard.logic.managers.GameHistoryManager.getUniqueCompletedLevelCount
import roboyard.logic.managers.GameHistoryManager.getUniqueThreeStarLevelCount
import roboyard.logic.platform.PlatformInfo
import roboyard.logic.platform.PlayGamesClient
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.ui.StringProvider
import roboyard.logic.ui.UiNotifier
import roboyard.logic.util.DateUtils
import roboyard.logic.util.RLog
import kotlin.math.max
import kotlin.math.min

/**
 * Manages achievement unlocking, storage, and retrieval.
 */
class AchievementManager private constructor(
    private val storage: PlatformStorage,
    private val stringProvider: StringProvider?,
    uiNotifier: UiNotifier?
) : roboyard.logic.achievements.AchievementCallback {
    private val log = RLog.tag("AchievementManager")

    private val achievements: MutableMap<String?, Achievement?>?
    private var unlockListener: AchievementUnlockListener? = null
    private var uiNotifier: UiNotifier? = uiNotifier

    // Platform-specific clients (set by the platform factory)
    var syncClient: AchievementSyncClient? = null
    var playGamesClient: PlayGamesClient? = null
    var streakDataProvider: StreakDataProvider? = null

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

    /**
     * Set a platform-agnostic UI notifier for showing messages (e.g., update nudges).
     * Use this on all platforms (Android, iOS, Desktop) instead of setCurrentActivity.
     */
    fun setUiNotifier(notifier: UiNotifier?) {
        this.uiNotifier = notifier
        // Show any pending update nudge now that we have a notifier
        if (notifier != null && pendingNudgeVersion != null) {
            showUpdateNudgeInternal(pendingNudgeVersion)
            pendingNudgeVersion = null
        }
    }

    /**
     * Clear UI notifier to prevent memory leaks when activity is destroyed
     */
    fun clearUiNotifier() {
        this.uiNotifier = null
    }

    /**
     * Show update nudge on credits page - always shows, no cooldown.
     * Should be called when opening the credits/about screen.
     */
    fun showUpdateNudgeForCredits() {
        log.d("[UPDATE_NUDGE_CREDITS] Called")
        val latestAppVersion = storage.getString(KEY_PENDING_NUDGE_VERSION, null)
        log.d("[UPDATE_NUDGE_CREDITS] Pending version from storage: %s", latestAppVersion)
        if (latestAppVersion == null) {
            log.d("[UPDATE_NUDGE_CREDITS] No pending version stored, checking fallback...")
            return
        }
        val current = PlatformInfo.getAppVersionName()
        log.d("[UPDATE_NUDGE_CREDITS] Comparing: current=%s, latest=%s", current, latestAppVersion)
        if (compareVersions(current, latestAppVersion) >= 0) {
            log.d("[UPDATE_NUDGE_CREDITS] App is up to date, not showing nudge")
            return  // up to date
        }
        log.i("[UPDATE_NUDGE_CREDITS] Showing nudge for version %s", latestAppVersion)
        showUpdateNudgeInternal(latestAppVersion)
    }

    private fun showUpdateNudgeInternal(version: String?) {
        val message = stringProvider?.getString("update_available_nudge", version ?: "")
        if (message != null) {
            uiNotifier?.showMessage(message)
        } else {
            log.d("[UPDATE_NUDGE] No StringProvider/UiNotifier available, cannot show nudge: version=%s", version)
        }
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
        val migrated = storage.getBoolean("orphaned_keys_migrated", false)
        if (migrated) return

        // Using storage for migration
        var migratedCount = 0

        for (id in achievements!!.keys) {
            // Check for orphaned keys: id + "_unlocked" (wrong format)
            val wrongUnlockedKey = id + "_unlocked"
            val wrongTimestampKey = id + "_timestamp"
            val correctUnlockedKey = KEY_PREFIX_UNLOCKED + id
            val correctTimestampKey = KEY_PREFIX_TIMESTAMP + id

            // Check if orphaned key exists using storage
            val orphanedValue = storage.getString(wrongUnlockedKey, null)
            if (orphanedValue != null) {
                val unlocked = storage.getBoolean(wrongUnlockedKey, false)
                val timestamp = storage.getLong(wrongTimestampKey, 0)

                // Only migrate if the correct key doesn't already have a value
                if (unlocked && !storage.getBoolean(correctUnlockedKey, false)) {
                    storage.putBoolean(correctUnlockedKey, true)
                    storage.putLong(correctTimestampKey, timestamp)
                    migratedCount++
                }

                // Remove orphaned keys
                storage.remove(wrongUnlockedKey)
                storage.remove(wrongTimestampKey)
            }
        }

        storage.putBoolean("orphaned_keys_migrated", true)

        if (migratedCount > 0) {
            log.d("[ACHIEVEMENTS] Migrated %d orphaned sync keys to correct format", migratedCount)
        }
    }

    private fun loadState() {
        // One-time migration: fix orphaned keys from buggy syncFromServer() 
        // that wrote "id_unlocked" instead of "unlocked_id"
        migrateOrphanedSyncKeys()


        // Load unlock status for all achievements
        for (achievement in achievements!!.values.filterNotNull()) {
            val unlocked = storage.getBoolean(KEY_PREFIX_UNLOCKED + achievement.id, false)
            val timestamp = storage.getLong(KEY_PREFIX_TIMESTAMP + achievement.id, 0)
            achievement.setUnlocked(unlocked)
            achievement.unlockedTimestamp = timestamp
        }


        // Load counters
        levelsCompleted = storage.getInt(KEY_COUNTER_PREFIX + "levels_completed", 0)
        perfectSolutions = storage.getInt(KEY_COUNTER_PREFIX + "perfect_solutions", 0)
        threeStarLevels = storage.getInt(KEY_COUNTER_PREFIX + "three_star_levels", 0)
        threeStarHardLevels = storage.getInt(KEY_COUNTER_PREFIX + "three_star_hard_levels", 0)
        impossibleModeGames = storage.getInt(KEY_COUNTER_PREFIX + "impossible_mode_games", 0)
        impossibleModeStreak = storage.getInt(KEY_COUNTER_PREFIX + "impossible_mode_streak", 0)
        perfectRandomGames = storage.getInt(KEY_COUNTER_PREFIX + "perfect_random_games", 0)
        perfectRandomGamesStreak =
            storage.getInt(KEY_COUNTER_PREFIX + "perfect_random_games_streak", 0)
        noHintRandomGames = storage.getInt(KEY_COUNTER_PREFIX + "no_hint_random_games", 0)
        noHintRandomGamesTotal = storage.getInt(KEY_COUNTER_PREFIX + "no_hint_random_games_total", 0)
        dailyLoginStreak = storage.getInt(KEY_COUNTER_PREFIX + "daily_login_streak", 0)
        speedrunRandomGamesUnder30s = storage.getInt(KEY_COUNTER_PREFIX + "speedrun_random_30s", 0)
        sameWallsMaxPositions = storage.getInt(KEY_COUNTER_PREFIX + "same_walls_max_positions", 0)

        log.d(
            "[ACHIEVEMENTS] Loaded state: %d achievements, %d unlocked",
            achievements.size, this.unlockedCount
        )
    }

    private fun saveCounter(key: String?, value: Int) {
        storage.putInt(KEY_COUNTER_PREFIX + key, value)
    }

    /**
     * Unlock an achievement by ID.
     * @return true if newly unlocked, false if already unlocked
     */
    fun unlock(achievementId: String): Boolean {
        val achievement = achievements!!.get(achievementId)
        if (achievement == null) {
            log.w("[ACHIEVEMENTS] Unknown achievement: %s", achievementId)
            return false
        }

        if (achievement.isUnlocked()) {
            return false // Already unlocked
        }

        achievement.setUnlocked(true)
        val timestamp = System.currentTimeMillis()
        achievement.unlockedTimestamp = timestamp


        // Save to storage
        storage.putBoolean(KEY_PREFIX_UNLOCKED + achievementId, true)
        storage.putLong(KEY_PREFIX_TIMESTAMP + achievementId, timestamp)

        log.d("[ACHIEVEMENTS] Unlocked: %s", achievementId)


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
                log.w("[ACHIEVEMENTS] Unknown achievement ID: %s", localId)
                return null
            }

            val value = stringProvider?.getString(resourceKey)
            if (value == null) {
                log.w("[ACHIEVEMENTS] String resource not found: %s", resourceKey)
                return null
            }

            return value
        } catch (e: Exception) {
            log.e(e, "[ACHIEVEMENTS] Failed to get Play Games ID for: %s", localId)
            return null
        }
    }

    /**
     * Sync achievement unlock to Google Play Games Services.
     * Only works if Play Games is enabled and a PlayGamesClient is set.
     */
    private fun syncToPlayGames(achievementId: String) {
        if (!PlatformInfo.isPlayGamesEnabled()) {
            return
        }

        val client = playGamesClient
        if (client == null) {
            log.d("[ACHIEVEMENTS] Cannot sync to Play Games - no PlayGamesClient set")
            return
        }

        try {
            client.unlockAchievement(achievementId)
            log.d("[ACHIEVEMENTS] Synced to Play Games: %s", achievementId)
        } catch (e: Exception) {
            log.e(e, "[ACHIEVEMENTS] Failed to sync to Play Games: %s", achievementId)
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

        log.d(
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
            log.d(
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
            log.d(
                "[ACHIEVEMENTS][PERFECT] Level %d: perfect solution counted! total=%d (playerMoves=%d == optimalMoves=%d)",
                levelId, perfectSolutions, playerMoves, optimalMoves
            )
            unlockIfComplete("perfect_solutions_5")
            unlockIfComplete("perfect_solutions_10")
            unlockIfComplete("perfect_solutions_50")
        } else if (optimalMoves <= 0) {
            log.w(
                "[ACHIEVEMENTS][PERFECT] Level %d: optimalMoves=%d (solver not ready?), perfect solution NOT counted!",
                levelId, optimalMoves
            )
        } else {
            log.d(
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

        log.d(
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
            log.d("[ACHIEVEMENTS] Hint was used during this game session")
        }

        log.d(
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
            log.d(
                "[ACHIEVEMENTS] Impossible mode game counted (optimalMoves=%d >= 17, isFirstCompletion=true)",
                optimalMoves
            )
        } else if (isImpossibleMode && optimalMoves >= 17 && !isFirstCompletion) {
            log.d("[ACHIEVEMENTS] Impossible mode game NOT counted - map already completed before")
        } else {
            log.d(
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
            log.d("[ACHIEVEMENTS] Solution length achievements skipped - map already completed before")
        } else if (!qualifiesForNoHints) {
            log.d("[ACHIEVEMENTS] Solution length achievements skipped - hints were used")
        } else {
            log.d(
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
            log.d(
                "[ACHIEVEMENTS] Perfect game on unique map - total: %d, streak: %d",
                perfectRandomGames,
                perfectRandomGamesStreak
            )
        } else if (playerMoves == optimalMoves && !isFirstCompletion) {
            log.d("[ACHIEVEMENTS] Perfect game NOT counted - map already completed before")
        } else {
            // Reset streak when non-optimal
            perfectRandomGamesStreak = 0
            saveCounter("perfect_random_games_streak", perfectRandomGamesStreak)
            log.d("[ACHIEVEMENTS] Non-optimal game - perfect streak reset to 0")
        }


        // Perfect solution with no hints (10+ moves optimal) - only on FIRST completion
        if (playerMoves == optimalMoves && qualifiesForNoHints && optimalMoves >= 10 && isFirstCompletion) {
            unlock("perfect_no_hints_random_1")
            log.d(
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
            log.d(
                "[ACHIEVEMENTS] No hints on unique map - total: %d, streak: %d",
                noHintRandomGamesTotal,
                noHintRandomGames
            )
        } else if (!qualifiesForNoHints) {
            // Reset streak counter when hints were used (on this or previous completion)
            noHintRandomGames = 0
            saveCounter("no_hint_random_games", noHintRandomGames)
            log.d(
                "[ACHIEVEMENTS] Hints used - no_hint streak reset to 0 (total stays: %d)",
                noHintRandomGamesTotal
            )
        } else if (!isFirstCompletion) {
            log.d("[ACHIEVEMENTS] No hints NOT counted - map already completed before")
        }


        // Same-walls achievements: count unique position-signatures sharing the same wall layout
        if (wallSignature != null && !wallSignature.isEmpty()) {
            val sameWallEntries =
                findByWallSignature(storage, wallSignature)
            val uniquePositions =
                sameWallEntries.size // each entry = distinct positionSignature
            log.d(
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

        // Speed achievements
        if (timeMs < 20000) unlock("speedrun_random_under_20s")
        if (timeMs < 10000) unlock("speedrun_random_under_10s")
        if (timeMs < 30000) {
            speedrunRandomGamesUnder30s++
            saveCounter("speedrun_random_30s", speedrunRandomGamesUnder30s)
            unlockIfComplete("speedrun_random_5_games_under_30s")
        }

        log.d(
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
            log.d(
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
            log.d(
                "[ACHIEVEMENTS] All %d robots have touched each other (%d pairs) - gimme_five unlocked!",
                robotCount, robotTouchPairs.size
            )
        } else {
            log.d(
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

        log.d("[ACHIEVEMENTS] New game started - session flags reset")
    }

    /**
     * For testing only This should only be used in unit tests.
     */
    fun setTestMode(enabled: Boolean) {
        log.d("[ACHIEVEMENTS] Test mode enabled")
    }

    /**
     * Called when the hint button is pressed during a game.
     * This tracks hint usage for the current game session.
     */
    fun onHintUsed() {
        hintUsedInCurrentGame = true
        log.d("[ACHIEVEMENTS] Hint used in current game session")
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
    override fun onDailyLogin(streakDays: Int) {
        dailyLoginStreak = streakDays
        saveCounter("daily_login_streak", dailyLoginStreak)
        log.d("[ACHIEVEMENT] Daily login recorded - streak: %d days", streakDays)
    }

    /**
     * Update daily login streak from server sync - keeps AchievementManager in sync with StreakManager
     */
    override fun updateDailyLoginStreak(streakDays: Int) {
        val beforeStreak = dailyLoginStreak
        dailyLoginStreak = streakDays
        saveCounter("daily_login_streak", dailyLoginStreak)
        log.d(
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
        log.d(
            "[ACHIEVEMENT] Checked Login Streak achievements at game start - streak: %d days",
            streakDays
        )
    }

    /**
     * Called when player returns after inactivity
     */
    override fun onComebackPlayer(daysAway: Int) {
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
        log.d("[ACHIEVEMENTS] All achievements unlocked")
    }

    /**
     * Lock an achievement by ID (for testing/debug)
     */
    fun lock(achievementId: String?) {
        if (achievementId == null) return
        val achievement = achievements!!.get(achievementId)
        if (achievement != null) {
            achievement.setUnlocked(false)
            achievement.unlockedTimestamp = 0
            storage.putBoolean(achievementId, false)
            log.d("[ACHIEVEMENTS] Achievement locked: %s", achievementId)
        }
    }

    /**
     * Reset all achievements (for testing)
     */
    fun resetAll() {
        storage.clear()
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
        get() = getUniqueCompletedLevelCount(storage)

    private val uniqueThreeStarLevelCount: Int
        get() = getUniqueThreeStarLevelCount(storage)

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
        val client = syncClient
        if (client == null || !client.isLoggedIn) {
            log.d("[ACHIEVEMENT_SYNC] Not logged in or no sync client, skipping sync")
            return
        }

        try {
            // Build achievements array
            val achievementsArray = JsonArray()
            for (achievement in achievements!!.values.filterNotNull()) {
                val achievementJson = JsonObject()
                achievementJson.addProperty("id", achievement.id)
                achievementJson.addProperty("unlocked", achievement.isUnlocked())
                achievementJson.addProperty("unlocked_timestamp", achievement.unlockedTimestamp)
                achievementsArray.add(achievementJson)
            }


            // Build stats object
            val stats = JsonObject()
            stats.addProperty("total_games_solved", levelsCompleted + perfectRandomGames)
            stats.addProperty("total_games_solved_no_hints", noHintRandomGamesTotal)
            stats.addProperty("total_perfect_solutions", perfectSolutions + perfectRandomGames)


            // Include streak data for bidirectional sync
            val streakManager = streakDataProvider
            if (streakManager != null) {
                stats.addProperty("daily_login_streak", streakManager.currentStreak)
                stats.addProperty("last_login_date", streakManager.lastLoginDateString)
                stats.addProperty("last_streak_date", streakManager.lastLoginDateString)
                stats.addProperty("longest_streak", streakManager.longestStreak)
                stats.addProperty("longest_streak_date", streakManager.longestStreakDate)
            }
            stats.addProperty("timezone", DateUtils.getTimezoneId())

            // Device / app metadata for server-side rankings and analytics
            // system_language: TRUE device locale, bypassing the app-level Locale override done
            //                  by RoboyardApplication.updateAppContextLocale().
            // app_language:    user-chosen language preference from Settings (Preferences.appLanguage).
            val systemLanguage: String? = PlatformInfo.getSystemLanguageTag()
            val appLanguage = Preferences.appLanguage
            val appVersion = PlatformInfo.getAppVersionName()
            val osVersion = PlatformInfo.getOsVersionString()
            if (systemLanguage != null) stats.addProperty("system_language", systemLanguage)
            if (appLanguage != null && !appLanguage.isEmpty()) stats.addProperty(
                "app_language",
                appLanguage
            )
            stats.addProperty("app_version", appVersion)
            stats.addProperty("android_version", osVersion)

            log.d(
                "[ACHIEVEMENT_SYNC_UP] Uploading: streak=%s, last_login_date=%s, longest=%s, timezone=%s, sysLang=%s, appLang=%s, app=%s, android=%s",
                streakManager?.currentStreak, streakManager?.lastLoginDateString,
                streakManager?.longestStreak, DateUtils.getTimezoneId(),
                systemLanguage, appLanguage, appVersion, osVersion
            )


            // Send to server
            client.syncAchievements(
                achievementsArray.toString(),
                stats.toString(),
                object : AchievementSyncCallback {
                    override fun onSuccess(syncedCount: Int, newAchievements: Int, latestAppVersion: String?) {
                        log.d(
                            "[ACHIEVEMENT_SYNC] Sync successful: %d synced, %d new achievements",
                            syncedCount, newAchievements
                        )
                        // Optional "update available" nudge if server reports a newer version
                        log.d("[UPDATE_NUDGE] Latest app version: %s", latestAppVersion)
                        if (latestAppVersion != null) {
                            maybeShowUpdateNudge(latestAppVersion)
                        }
                    }

                    override fun onError(error: String?) {
                        log.e("[ACHIEVEMENT_SYNC] Sync failed: %s", error)
                    }
                })
        } catch (e: Exception) {
            log.e(e, "[ACHIEVEMENT_SYNC] Failed to build sync request")
        }
    }

    private var pendingNudgeVersion: String? = null

    init {
        this.achievements = all
        loadState()
    }

    /**
     * If the server reports a newer published app version than the installed one,
     * show a Toast nudge — but only once per (version, 24h) window to avoid spam.
     */
    private fun maybeShowUpdateNudge(latestAppVersion: String?) {
        val current = PlatformInfo.getAppVersionName()
        log.d("[UPDATE_NUDGE] Checking: current=%s, latest=%s", current, latestAppVersion)

        if (latestAppVersion == null || latestAppVersion.isEmpty()) {
            log.d("[UPDATE_NUDGE] Skipping: latestAppVersion is null or empty")
            return
        }

        val cmp: Int = compareVersions(current, latestAppVersion)
        log.d("[UPDATE_NUDGE] Version compare result: %d (negative=update available)", cmp)

        if (cmp >= 0) {
            log.d("[UPDATE_NUDGE] Skipping: current >= latest (no update needed)")
            return  // we're up to date or ahead (dev builds)
        }

        val lastNudgedVersion = storage.getString(KEY_LAST_NUDGED_VERSION, null)
        val lastNudgeMs = storage.getLong(KEY_LAST_NUDGE_MS, 0L)
        val now = System.currentTimeMillis()
        val elapsed = now - lastNudgeMs
        val sameVersion = latestAppVersion == lastNudgedVersion

        log.d(
            "[UPDATE_NUDGE] Cooldown check: lastNudgedVersion=%s, sameVersion=%b, elapsed=%d ms, cooldown=%d ms",
            lastNudgedVersion, sameVersion, elapsed, NUDGE_COOLDOWN_MS
        )

        // Always store the latest version for credits page (independent of cooldown)
        storage.putString(KEY_PENDING_NUDGE_VERSION, latestAppVersion)
        log.d("[UPDATE_NUDGE] Stored pending version for credits: %s", latestAppVersion)

        // Check cooldown - only affects automatic nudge at app start, not credits page
        if (sameVersion && elapsed < NUDGE_COOLDOWN_MS) {
            log.d(
                "[UPDATE_NUDGE] Cooldown active, storing for later. remaining=%d ms",
                NUDGE_COOLDOWN_MS - elapsed
            )
            pendingNudgeVersion = latestAppVersion
            return
        }

        log.i("[UPDATE_NUDGE] Conditions met - will show nudge for version %s", latestAppVersion)
        storage.putString(KEY_LAST_NUDGED_VERSION, latestAppVersion)
        storage.putLong(KEY_LAST_NUDGE_MS, now)

        if (uiNotifier == null) {
            log.d(
                "[UPDATE_NUDGE] No UiNotifier available, storing pending nudge for version=%s",
                latestAppVersion
            )
            pendingNudgeVersion = latestAppVersion
            return
        }
        showUpdateNudgeInternal(latestAppVersion)
    }

    /**
     * Sync achievements to server after unlocking.
     * Called automatically when an achievement is unlocked.
     */
    private fun syncAfterUnlock() {
        // Delay sync slightly to batch multiple unlocks
        CoroutineScope(Dispatchers.Main).launch { delay(2000); this@AchievementManager.syncToServer() }
    }

    /**
     * Sync achievements FROM server to local device.
     * Used after login to restore achievements on a new device.
     * Merges server achievements with local ones (union - never removes local achievements).
     * 
     * @param callback Optional callback for sync result
     */
    fun syncFromServer(callback: AchievementSyncCallback? = null) {
        val client = syncClient
        if (client == null || !client.isLoggedIn) {
            log.d("[ACHIEVEMENT_SYNC_DOWN] Not logged in or no sync client, skipping download")
            callback?.onError("Not logged in")
            return
        }

        log.d("[ACHIEVEMENT_SYNC_DOWN] Starting achievement download from server")

        client.fetchAchievements(object : AchievementFetchCallback {
            override fun onSuccess(achievementsJson: String, statsJson: String?) {
                var restoredCount = 0

                try {
                    val serverAchievements = JsonParser.parseString(achievementsJson).asJsonArray
                    log.d(
                        "[ACHIEVEMENT_SYNC_DOWN] Received %d achievements from server",
                        serverAchievements.size()
                    )

                    for (i in 0..<serverAchievements.size()) {
                        val serverAchievement = serverAchievements[i].asJsonObject
                        val id = serverAchievement.get("id").asString
                        val unlocked = if (serverAchievement.has("unlocked")) serverAchievement.get("unlocked").asBoolean else false
                        val unlockedAt = if (serverAchievement.has("unlocked_at") && !serverAchievement.get("unlocked_at").isJsonNull) serverAchievement.get("unlocked_at").asString else null

                        if (!unlocked) continue


                        // Check if we have this achievement locally
                        val localAchievement = achievements!!.get(id)
                        if (localAchievement == null) {
                            log.d("[ACHIEVEMENT_SYNC_DOWN] Unknown achievement ID from server: %s", id)
                            continue
                        }


                        // Only restore if not already unlocked locally
                        if (!localAchievement.isUnlocked()) {
                            var timestamp: Long = 0
                            if (unlockedAt != null && !unlockedAt.isEmpty()) {
                                timestamp = DateUtils.parseTimestampIso(unlockedAt)
                                if (timestamp == 0L) {
                                    log.w(
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


                            // Save to storage
                            storage.putBoolean(KEY_PREFIX_UNLOCKED + id, true)
                            storage.putLong(
                                KEY_PREFIX_TIMESTAMP + id,
                                localAchievement.unlockedTimestamp
                            )

                            restoredCount++
                            log.d("[ACHIEVEMENT_SYNC_DOWN] Restored achievement: %s", id)
                        }
                    }

                    log.d(
                        "[ACHIEVEMENT_SYNC_DOWN] Download complete: %d achievements restored",
                        restoredCount
                    )


                    // Restore streak data from server (bidirectional)
                    if (statsJson != null) {
                        val stats = JsonParser.parseString(statsJson).asJsonObject
                        val serverStreak = if (stats.has("daily_login_streak")) stats.get("daily_login_streak").asInt else 0
                        val serverLongestStreak = if (stats.has("longest_streak")) stats.get("longest_streak").asInt else 0
                        // Use last_login_date with fallback to last_streak_date (for users who synced before last_login_date was introduced)
                        var serverLastLoginDate: String? = null
                        if (stats.has("last_login_date") && !stats.get("last_login_date").isJsonNull) {
                            serverLastLoginDate = stats.get("last_login_date").asString
                        }
                        if (serverLastLoginDate == null && stats.has("last_streak_date") && !stats.get("last_streak_date").isJsonNull) {
                            serverLastLoginDate = stats.get("last_streak_date").asString
                        }
                        val serverLongestStreakDate: String? =
                            if (stats.has("longest_streak_date") && !stats.get("longest_streak_date").isJsonNull) stats.get("longest_streak_date").asString else null
                        streakDataProvider?.restoreFromServer(
                            serverStreak,
                            serverLastLoginDate,
                            serverLongestStreak,
                            serverLongestStreakDate
                        )
                    }
                } catch (e: Exception) {
                    log.e(e, "[ACHIEVEMENT_SYNC_DOWN] Error parsing server achievements")
                    callback?.onError("Error parsing achievements: " + e.message)
                    return
                }

                callback?.onSuccess(restoredCount, 0, null)
            }

            override fun onError(error: String?) {
                log.e("[ACHIEVEMENT_SYNC_DOWN] Download failed: %s", error)
                callback?.onError(error)
            }
        })
    }

    companion object {
        // Using AndroidStorage instead of direct SharedPreferences
        private const val KEY_PREFIX_UNLOCKED = "unlocked_"
        private const val KEY_PREFIX_TIMESTAMP = "timestamp_"
        private const val KEY_COUNTER_PREFIX = "counter_"

        private var instance: AchievementManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(storage: PlatformStorage, stringProvider: StringProvider? = null, uiNotifier: UiNotifier? = null): AchievementManager {
            if (instance == null) {
                instance = AchievementManager(storage, stringProvider, uiNotifier)
            }
            return instance!!
        }

        // Storage key for dedup of the update nudge: we store "last_nudged_version"
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
