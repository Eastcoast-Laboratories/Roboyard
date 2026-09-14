package roboyard.logic.achievements

import roboyard.logic.storage.PlatformStorage
import roboyard.logic.util.DateUtils
import roboyard.logic.util.RLog
import kotlin.math.max

/**
 * Manages daily login streaks and comeback tracking.
 *
 * Use setTestMode(true) for quick testing with shortened time periods:
 * - 1 "day" = 10 seconds (instead of 24 hours)
 * - 7 "days" = 70 seconds
 * - 30 "days" = 300 seconds (5 minutes)
 */
class StreakManager private constructor(
    private val storage: PlatformStorage,
    private val achievementCallback: AchievementCallback?
) : StreakDataProvider {
    private val log = RLog.tag("StreakManager")

    // Test mode flag - can be enabled via Settings for quick streak testing
    private var testMode = false

    // For testing: allows overriding the current date
    private var mockTodayDate: Long? = null

    init {
        // Load test mode setting from storage
        this.testMode = storage.getBoolean(KEY_TEST_MODE, false)
        log.d("[STREAK] Test mode loaded from storage: %s", testMode)
    }

    /**
     * Record a daily login and update streak
     */
    fun recordDailyLogin(): StreakUpdate {
        val today = this.todayDate
        val lastLoginDate = storage.getLong(KEY_LAST_LOGIN_DATE, 0)
        var currentStreak = storage.getInt(KEY_CURRENT_STREAK, 0)

        log.d(
            "[STREAK] Recording daily login - today: %d, lastLogin: %d, streak: %d",
            today,
            lastLoginDate,
            currentStreak
        )

        // Check if already logged in today
        if (lastLoginDate == today) {
            log.d("[STREAK] Already logged in today, skipping")
            return StreakUpdate(false, currentStreak, false, false, false)
        }

        var isContinuation = false
        var isNewStreak = false
        var triggeredComebackAchievement = false

        // Check if this is a new streak or continuation
        val yesterday = today - 1
        if (lastLoginDate == yesterday) {
            // Continue the streak
            currentStreak++
            log.d("[STREAK] Streak continued: %d days", currentStreak)
            isContinuation = true
        } else if (lastLoginDate > 0 && lastLoginDate < yesterday) {
            // Streak broken (but user was active before), check for comeback
            val daysAway = today - lastLoginDate
            if (daysAway > 30) {
                log.d("[STREAK] Comeback after %d days away", daysAway)
                achievementCallback?.onComebackPlayer(daysAway.toInt())
                triggeredComebackAchievement = true
            }
            // Start new streak
            currentStreak = 1
            log.d("[STREAK] Streak broken after %d days away, new streak started", daysAway)
            isNewStreak = true
        } else if (lastLoginDate == 0L) {
            // First login ever - don't trigger comeback
            currentStreak = 1
            log.d("[STREAK] First login recorded")
            isNewStreak = true
        } else {
            // This shouldn't happen, but handle it gracefully
            log.w(
                "[STREAK] Unexpected state: today=%d, lastLogin=%d, streak=%d",
                today,
                lastLoginDate,
                currentStreak
            )
            currentStreak = 1
            isNewStreak = true
        }

        // Update longest streak if current exceeds it
        var longestStreak = storage.getInt(KEY_LONGEST_STREAK, 0)
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak
            log.d("[STREAK] New longest streak record: %d days", longestStreak)
        }

        // Save updated values
        storage.putLong(KEY_LAST_LOGIN_DATE, today)
        storage.putInt(KEY_CURRENT_STREAK, currentStreak)
        storage.putLong(KEY_LAST_STREAK_DATE, today)
        storage.putInt(KEY_LONGEST_STREAK, longestStreak)
        storage.putString(KEY_LONGEST_STREAK_DATE, dayNumberToDateString(today))

        // Notify achievement manager
        achievementCallback?.onDailyLogin(currentStreak)

        log.d("[STREAK] Daily login recorded - new streak: %d days", currentStreak)
        return StreakUpdate(
            true,
            currentStreak,
            isContinuation,
            isNewStreak,
            triggeredComebackAchievement
        )
    }

    class StreakUpdate(
        val isNewDayRecorded: Boolean, @JvmField val streakDays: Int, private val continuation: Boolean,
        private val newStreak: Boolean, private val comebackTriggered: Boolean
    )

    override val currentStreak: Int
        /**
         * Get current streak in days
         */
        get() = storage.getInt(KEY_CURRENT_STREAK, 0)

    val storedStreakDays: Int
        /**
         * Get stored streak days from storage (for debug display)
         */
        get() = storage.getInt(KEY_CURRENT_STREAK, 0)

    /**
     * Check if the streak popup should be shown today.
     * Returns true only if it hasn't been shown for the current day yet.
     */
    fun shouldShowStreakPopupToday(): Boolean {
        val today = this.todayDate
        val lastPopupDate = storage.getLong(KEY_LAST_POPUP_DATE, 0)
        val shouldShow = lastPopupDate < today
        log.d(
            "[STREAK_POPUP] shouldShowStreakPopupToday: today=%d, lastPopup=%d, shouldShow=%b",
            today,
            lastPopupDate,
            shouldShow
        )
        return shouldShow
    }

    /**
     * Mark the streak popup as shown for today.
     */
    fun markStreakPopupShownToday() {
        val today = this.todayDate
        storage.putLong(KEY_LAST_POPUP_DATE, today)
        log.d("[STREAK_POPUP] Marked popup shown for day %d", today)
    }

    val todayDate: Long
        /**
         * Get today's date as number of "days" since epoch.
         * Uses the device's local timezone so the day changes at local midnight, not UTC midnight.
         * In test mode, a "day" is 10 seconds for quick testing.
         */
        get() {
            if (mockTodayDate != null) {
                return mockTodayDate!!
            }
            if (testMode) {
                return System.currentTimeMillis() / TEST_DAY_MS
            }
            val now = System.currentTimeMillis()
            val offsetMs = DateUtils.getTimezoneOffsetMs(now)
            return (now + offsetMs) / NORMAL_DAY_MS
        }

    /**
     * Enable or disable test mode for quick streak testing.
     * In test mode, 1 "day" = 10 seconds.
     * Note: Changing test mode resets the streak because time units are incompatible.
     */
    fun setTestMode(enabled: Boolean) {
        val wasTestMode = testMode
        testMode = enabled
        // Persist test mode setting to preferences
        storage.putBoolean(KEY_TEST_MODE, enabled)

        // Reset streak when switching modes because time units are incompatible
        if (wasTestMode != enabled) {
            resetStreak()
            log.d("[STREAK] Streak reset due to test mode change")
        }

        log.d(
            "[STREAK] Test mode %s - 1 day = %d ms", if (enabled) "ENABLED" else "DISABLED",
            if (enabled) TEST_DAY_MS else NORMAL_DAY_MS
        )
    }

    /**
     * Check if test mode is enabled.
     */
    fun isTestMode(): Boolean {
        return testMode
    }

    /**
     * Set mock date for testing (simulates different days)
     */
    fun setMockTodayDate(daysSinceEpoch: Long) {
        mockTodayDate = daysSinceEpoch
        log.d("[STREAK] Mock date set to: %d", daysSinceEpoch)
    }

    /**
     * Clear mock date (for testing)
     */
    fun clearMockTodayDate() {
        mockTodayDate = null
        log.d("[STREAK] Mock date cleared")
    }

    override val longestStreak: Int
        /**
         * Get the longest streak ever achieved
         */
        get() = storage.getInt(KEY_LONGEST_STREAK, 0)

    override val longestStreakDate: String?
        /**
         * Get the date when the longest streak was achieved (ISO format)
         */
        get() = storage.getString(KEY_LONGEST_STREAK_DATE, null)

    /**
     * Convert a day number to ISO date string.
     * Day numbers include local TZ offset, so we subtract it to get UTC millis for formatting.
     */
    private fun dayNumberToDateString(dayNumber: Long): String {
        if (testMode) {
            val timestampMs: Long = dayNumber * TEST_DAY_MS
            return DateUtils.formatDateIso(timestampMs)
        }
        val timestampMs: Long = dayNumber * NORMAL_DAY_MS
        val offsetMs = DateUtils.getTimezoneOffsetMs(timestampMs)
        return DateUtils.formatDateIso(timestampMs - offsetMs)
    }

    override val lastLoginDateString: String?
        /**
         * Get last login date as ISO date string for server sync.
         */
        get() {
            val lastLoginDate = storage.getLong(KEY_LAST_LOGIN_DATE, 0)
            if (lastLoginDate == 0L) return null
            log.d(
                "[STREAK_SYNC_DATE] Returning stored last_login_date=%d (%s) without mutating streak state",
                lastLoginDate, dayNumberToDateString(lastLoginDate)
            )
            return dayNumberToDateString(lastLoginDate)
        }

    /**
     * Restore streak data from server (bidirectional sync).
     * Resets current streak if server data is stale. Always preserves the highest longest streak.
     */
    override fun restoreFromServer(
        serverStreak: Int,
        serverLastDate: String?,
        serverLongestStreak: Int,
        serverLongestStreakDate: String?
    ) {
        var serverStreak = serverStreak
        val localStreak = this.currentStreak
        val localLongest = storage.getInt(KEY_LONGEST_STREAK, 0)

        // Always restore longest streak first - takes max of server and local, never loses it
        val maxLongest = max(serverLongestStreak, localLongest)
        if (maxLongest > localLongest) {
            storage.putInt(KEY_LONGEST_STREAK, maxLongest)
            storage.putString(
                KEY_LONGEST_STREAK_DATE,
                serverLongestStreakDate ?: dayNumberToDateString(this.todayDate)
            )
            log.d(
                "[STREAK_SYNC] Restored longest streak: %d (local was: %d, server: %d)",
                maxLongest,
                localLongest,
                serverLongestStreak
            )
        }

        // Check if current streak data is stale (user was absent for >1 day)
        if (serverStreak > 1 && serverLastDate != null) {
            val serverLastLoginDay = parseDateStringToDayNumber(serverLastDate)
            val today = this.todayDate
            val daysSinceServerLogin = today - serverLastLoginDay

            log.d(
                "[STREAK_SYNC] Checking server data freshness: serverStreak=%d, serverLastDate=%s, daysSince=%d",
                serverStreak, serverLastDate, daysSinceServerLogin
            )

            if (daysSinceServerLogin > 1) {
                log.d(
                    "[STREAK_SYNC] Server data is stale: %d days since last login, resetting server streak to 1",
                    daysSinceServerLogin
                )
                serverStreak = 1
            } else {
                log.d(
                    "[STREAK_SYNC] Server data is fresh (%d days), keeping server streak %d",
                    daysSinceServerLogin,
                    serverStreak
                )
            }
        }

        val maxStreak = max(serverStreak, localStreak)

        if (maxStreak != localStreak) {
            storage.putInt(KEY_CURRENT_STREAK, maxStreak)
            log.d(
                "[STREAK_SYNC] Updated current streak to: %d (local: %d, server: %d)",
                maxStreak,
                localStreak,
                serverStreak
            )
        } else {
            log.d(
                "[STREAK_SYNC] Local streak %d is already the maximum (server: %d), keeping local",
                localStreak,
                serverStreak
            )
        }

        log.d("[STREAK_SYNC_DATE] Server restore kept stored last_login_date unchanged; recordDailyLogin owns day advancement")

        // Update AchievementManager to keep it in sync
        achievementCallback?.updateDailyLoginStreak(maxStreak)
        log.d("[STREAK_SYNC] AchievementManager dailyLoginStreak updated to: %d", maxStreak)
    }

    /**
     * Parse ISO date string to day number (inverse of dayNumberToDateString)
     */
    private fun parseDateStringToDayNumber(dateString: String?): Long {
        if (dateString == null || "null" == dateString || dateString.isEmpty()) return 0

        try {
            val timestampMs = DateUtils.parseDateIso(dateString)
            if (testMode) {
                return timestampMs / TEST_DAY_MS
            }
            val offsetMs = DateUtils.getTimezoneOffsetMs(timestampMs)
            return (timestampMs + offsetMs) / NORMAL_DAY_MS
        } catch (e: Exception) {
            log.w("[STREAK] Failed to parse date string: %s", dateString)
            return 0
        }
    }

    /**
     * Reset streak (for testing)
     */
    fun resetStreak() {
        storage.remove(KEY_LAST_LOGIN_DATE)
        storage.remove(KEY_CURRENT_STREAK)
        storage.remove(KEY_LAST_STREAK_DATE)
        storage.remove(KEY_LAST_POPUP_DATE)
        storage.remove(KEY_LONGEST_STREAK)
        storage.remove(KEY_LONGEST_STREAK_DATE)
        log.d("[STREAK] Streak reset (including longest streak)")
    }

    companion object {
        // Keys are prefixed automatically by storage implementation
        private const val KEY_LAST_LOGIN_DATE = "last_login_date"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LAST_STREAK_DATE = "last_streak_date"
        private const val KEY_LAST_POPUP_DATE = "last_popup_date"
        private const val KEY_TEST_MODE = "test_mode"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LONGEST_STREAK_DATE = "longest_streak_date"

        // Normal mode: 1 day = 24 hours
        private val NORMAL_DAY_MS = 24L * 60L * 60L * 1000L

        // Test mode: 1 "day" = 10 seconds for quick testing
        private const val TEST_DAY_MS = 10000L

        private var instance: StreakManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(storage: PlatformStorage, achievementCallback: AchievementCallback?): StreakManager {
            if (instance == null) {
                instance = StreakManager(storage, achievementCallback)
            }
            return instance!!
        }
    }
}
