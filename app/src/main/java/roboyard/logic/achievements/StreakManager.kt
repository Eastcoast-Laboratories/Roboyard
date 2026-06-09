package roboyard.logic.achievements

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.w
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Manages daily login streaks and comeback tracking.
 * 
 * Use setTestMode(true) for quick testing with shortened time periods:
 * - 1 "day" = 10 seconds (instead of 24 hours)
 * - 7 "days" = 70 seconds
 * - 30 "days" = 300 seconds (5 minutes)
 */
class StreakManager private constructor(context: Context) {
    // Test mode flag - can be enabled via Settings for quick streak testing
    private var testMode = false

    private val context: Context?
    private val prefs: SharedPreferences
    private val achievementManager: AchievementManager

    // For testing: allows overriding the current date
    private var mockTodayDate: Long? = null

    init {
        this.context = context.getApplicationContext()
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        this.achievementManager = AchievementManager.getInstance(context)
        // Load test mode setting from preferences
        this.testMode = prefs.getBoolean(KEY_TEST_MODE, false)
        d("[STREAK] Test mode loaded from preferences: %s", testMode)
    }

    /**
     * Record a daily login and update streak
     */
    fun recordDailyLogin(): StreakUpdate {
        val today = this.todayDate
        val lastLoginDate = prefs.getLong(KEY_LAST_LOGIN_DATE, 0)
        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)

        d(
            "[STREAK] Recording daily login - today: %d, lastLogin: %d, streak: %d",
            today,
            lastLoginDate,
            currentStreak
        )


        // Check if already logged in today
        if (lastLoginDate == today) {
            d("[STREAK] Already logged in today, skipping")
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
            d("[STREAK] Streak continued: %d days", currentStreak)
            isContinuation = true
        } else if (lastLoginDate > 0 && lastLoginDate < yesterday) {
            // Streak broken (but user was active before), check for comeback
            val daysAway = today - lastLoginDate
            if (daysAway > 30) {
                d("[STREAK] Comeback after %d days away", daysAway)
                achievementManager.onComebackPlayer(daysAway.toInt())
                triggeredComebackAchievement = true
            }
            // Start new streak
            currentStreak = 1
            d("[STREAK] Streak broken after %d days away, new streak started", daysAway)
            isNewStreak = true
        } else if (lastLoginDate == 0L) {
            // First login ever - don't trigger comeback
            currentStreak = 1
            d("[STREAK] First login recorded")
            isNewStreak = true
        } else {
            // This shouldn't happen, but handle it gracefully
            w(
                "[STREAK] Unexpected state: today=%d, lastLogin=%d, streak=%d",
                today,
                lastLoginDate,
                currentStreak
            )
            currentStreak = 1
            isNewStreak = true
        }


        // Update longest streak if current exceeds it
        var longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak
            d("[STREAK] New longest streak record: %d days", longestStreak)
        }


        // Save updated values
        prefs.edit()
            .putLong(KEY_LAST_LOGIN_DATE, today)
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putLong(KEY_LAST_STREAK_DATE, today)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .putString(KEY_LONGEST_STREAK_DATE, dayNumberToDateString(today))
            .apply()


        // Notify achievement manager
        achievementManager.onDailyLogin(currentStreak)

        d("[STREAK] Daily login recorded - new streak: %d days", currentStreak)
        return StreakUpdate(
            true,
            currentStreak,
            isContinuation,
            isNewStreak,
            triggeredComebackAchievement
        )
    }

    class StreakUpdate(
        val isNewDayRecorded: Boolean, val streakDays: Int, private val continuation: Boolean,
        private val newStreak: Boolean, private val comebackTriggered: Boolean
    )

    val currentStreak: Int
        /**
         * Get current streak in days
         */
        get() = prefs.getInt(KEY_CURRENT_STREAK, 0)


    val storedStreakDays: Int
        /**
         * Get stored streak days from SharedPreferences (for debug display)
         */
        get() = prefs.getInt(KEY_CURRENT_STREAK, 0)

    /**
     * Check if the streak popup should be shown today.
     * Returns true only if it hasn't been shown for the current day yet.
     */
    fun shouldShowStreakPopupToday(): Boolean {
        val today = this.todayDate
        val lastPopupDate = prefs.getLong(KEY_LAST_POPUP_DATE, 0)
        val shouldShow = lastPopupDate < today
        d(
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
        prefs.edit().putLong(KEY_LAST_POPUP_DATE, today).apply()
        d("[STREAK_POPUP] Marked popup shown for day %d", today)
    }

    protected val todayDate: Long
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
            val offsetMs = TimeZone.getDefault().getOffset(now)
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
        prefs.edit().putBoolean(KEY_TEST_MODE, enabled).apply()


        // Reset streak when switching modes because time units are incompatible
        if (wasTestMode != enabled) {
            resetStreak()
            d("[STREAK] Streak reset due to test mode change")
        }

        d(
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
        d("[STREAK] Mock date set to: %d", daysSinceEpoch)
    }

    /**
     * Clear mock date (for testing)
     */
    fun clearMockTodayDate() {
        mockTodayDate = null
        d("[STREAK] Mock date cleared")
    }

    val longestStreak: Int
        /**
         * Get the longest streak ever achieved
         */
        get() = prefs.getInt(KEY_LONGEST_STREAK, 0)

    val longestStreakDate: String?
        /**
         * Get the date when the longest streak was achieved (ISO format)
         */
        get() = prefs.getString(KEY_LONGEST_STREAK_DATE, null)

    /**
     * Convert a day number to ISO date string.
     * Day numbers include local TZ offset, so we subtract it to get UTC millis for formatting.
     */
    private fun dayNumberToDateString(dayNumber: Long): String {
        if (testMode) {
            val timestampMs: Long = dayNumber * TEST_DAY_MS
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestampMs))
        }
        val timestampMs: Long = dayNumber * NORMAL_DAY_MS
        val offsetMs = TimeZone.getDefault().getOffset(timestampMs)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestampMs - offsetMs))
    }

    val lastLoginDateString: String?
        /**
         * Get last login date as ISO date string for server sync.
         */
        get() {
            val lastLoginDate =
                prefs.getLong(KEY_LAST_LOGIN_DATE, 0)
            if (lastLoginDate == 0L) return null
            d(
                "[STREAK_SYNC_DATE] Returning stored last_login_date=%d (%s) without mutating streak state",
                lastLoginDate, dayNumberToDateString(lastLoginDate)
            )
            return dayNumberToDateString(lastLoginDate)
        }

    /**
     * Restore streak data from server (bidirectional sync).
     * Resets current streak if server data is stale. Always preserves the highest longest streak.
     */
    fun restoreFromServer(
        serverStreak: Int,
        serverLastDate: String?,
        serverLongestStreak: Int,
        serverLongestStreakDate: String?
    ) {
        var serverStreak = serverStreak
        val localStreak = this.currentStreak
        val localLongest = prefs.getInt(KEY_LONGEST_STREAK, 0)


        // Always restore longest streak first - takes max of server and local, never loses it
        val maxLongest = max(serverLongestStreak, localLongest)
        if (maxLongest > localLongest) {
            prefs.edit()
                .putInt(KEY_LONGEST_STREAK, maxLongest)
                .putString(
                    KEY_LONGEST_STREAK_DATE,
                    if (serverLongestStreakDate != null) serverLongestStreakDate else dayNumberToDateString(
                        this.todayDate
                    )
                )
                .apply()
            d(
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

            d(
                "[STREAK_SYNC] Checking server data freshness: serverStreak=%d, serverLastDate=%s, daysSince=%d",
                serverStreak, serverLastDate, daysSinceServerLogin
            )

            if (daysSinceServerLogin > 1) {
                d(
                    "[STREAK_SYNC] Server data is stale: %d days since last login, resetting server streak to 1",
                    daysSinceServerLogin
                )
                serverStreak = 1
            } else {
                d(
                    "[STREAK_SYNC] Server data is fresh (%d days), keeping server streak %d",
                    daysSinceServerLogin,
                    serverStreak
                )
            }
        }

        val maxStreak = max(serverStreak, localStreak)

        if (maxStreak != localStreak) {
            prefs.edit()
                .putInt(KEY_CURRENT_STREAK, maxStreak)
                .apply()
            d(
                "[STREAK_SYNC] Updated current streak to: %d (local: %d, server: %d)",
                maxStreak,
                localStreak,
                serverStreak
            )
        } else {
            d(
                "[STREAK_SYNC] Local streak %d is already the maximum (server: %d), keeping local",
                localStreak,
                serverStreak
            )
        }

        d("[STREAK_SYNC_DATE] Server restore kept stored last_login_date unchanged; recordDailyLogin owns day advancement")


        // Update AchievementManager to keep it in sync
        achievementManager.updateDailyLoginStreak(maxStreak)
        d("[STREAK_SYNC] AchievementManager dailyLoginStreak updated to: %d", maxStreak)
    }

    /**
     * Parse ISO date string to day number (inverse of dayNumberToDateString)
     */
    private fun parseDateStringToDayNumber(dateString: String?): Long {
        if (dateString == null || "null" == dateString || dateString.isEmpty()) return 0

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timestampMs = sdf.parse(dateString).getTime()
            if (testMode) {
                return timestampMs / TEST_DAY_MS
            }
            val offsetMs = TimeZone.getDefault().getOffset(timestampMs)
            return (timestampMs + offsetMs) / NORMAL_DAY_MS
        } catch (e: Exception) {
            w(e, "[STREAK] Failed to parse date string: %s", dateString)
            return 0
        }
    }


    /**
     * Reset streak (for testing)
     */
    fun resetStreak() {
        prefs.edit()
            .remove(KEY_LAST_LOGIN_DATE)
            .remove(KEY_CURRENT_STREAK)
            .remove(KEY_LAST_STREAK_DATE)
            .remove(KEY_LAST_POPUP_DATE)
            .remove(KEY_LONGEST_STREAK)
            .remove(KEY_LONGEST_STREAK_DATE)
            .apply()
        d("[STREAK] Streak reset (including longest streak)")
    }

    companion object {
        private const val PREFS_NAME = "roboyard_streaks"
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

        @Synchronized
        fun getInstance(context: Context): StreakManager {
            if (instance == null) {
                instance = StreakManager(context)
            }
            return instance!!
        }
    }
}
