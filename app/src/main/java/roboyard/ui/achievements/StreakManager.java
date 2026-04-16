package roboyard.ui.achievements;

import android.content.Context;
import android.content.SharedPreferences;

import timber.log.Timber;

/**
 * Manages daily login streaks and comeback tracking.
 * 
 * Use setTestMode(true) for quick testing with shortened time periods:
 * - 1 "day" = 10 seconds (instead of 24 hours)
 * - 7 "days" = 70 seconds
 * - 30 "days" = 300 seconds (5 minutes)
 */
public class StreakManager {
    
    private static final String PREFS_NAME = "roboyard_streaks";
    private static final String KEY_LAST_LOGIN_DATE = "last_login_date";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    private static final String KEY_LAST_STREAK_DATE = "last_streak_date";
    private static final String KEY_LAST_POPUP_DATE = "last_popup_date";
    private static final String KEY_TEST_MODE = "test_mode";
    private static final String KEY_LONGEST_STREAK = "longest_streak";
    private static final String KEY_LONGEST_STREAK_DATE = "longest_streak_date";
    
    // Normal mode: 1 day = 24 hours
    private static final long NORMAL_DAY_MS = 24L * 60L * 60L * 1000L;
    // Test mode: 1 "day" = 10 seconds for quick testing
    private static final long TEST_DAY_MS = 10_000L;
    
    // Test mode flag - can be enabled via Settings for quick streak testing
    private boolean testMode = false;
    
    private static StreakManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final AchievementManager achievementManager;
    
    // For testing: allows overriding the current date
    private Long mockTodayDate = null;
    
    private StreakManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.achievementManager = AchievementManager.getInstance(context);
        // Load test mode setting from preferences
        this.testMode = prefs.getBoolean(KEY_TEST_MODE, false);
        Timber.d("[STREAK] Test mode loaded from preferences: %s", testMode);
    }
    
    public static synchronized StreakManager getInstance(Context context) {
        if (instance == null) {
            instance = new StreakManager(context);
        }
        return instance;
    }
    
    /**
     * Record a daily login and update streak
     */
    public StreakUpdate recordDailyLogin() {
        long today = getTodayDate();
        long lastLoginDate = prefs.getLong(KEY_LAST_LOGIN_DATE, 0);
        int currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0);
        
        Timber.d("[STREAK] Recording daily login - today: %d, lastLogin: %d, streak: %d", today, lastLoginDate, currentStreak);
        
        // Check if already logged in today
        if (lastLoginDate == today) {
            Timber.d("[STREAK] Already logged in today, skipping");
            return new StreakUpdate(false, currentStreak, false, false, false);
        }
        
        boolean isContinuation = false;
        boolean isNewStreak = false;
        boolean triggeredComebackAchievement = false;
        
        // Check if this is a new streak or continuation
        long yesterday = today - 1;
        if (lastLoginDate == yesterday) {
            // Continue the streak
            currentStreak++;
            Timber.d("[STREAK] Streak continued: %d days", currentStreak);
            isContinuation = true;
        } else if (lastLoginDate > 0 && lastLoginDate < yesterday) {
            // Streak broken (but user was active before), check for comeback
            long daysAway = today - lastLoginDate;
            if (daysAway > 30) {
                Timber.d("[STREAK] Comeback after %d days away", daysAway);
                achievementManager.onComebackPlayer((int) daysAway);
                triggeredComebackAchievement = true;
            }
            // Start new streak
            currentStreak = 1;
            Timber.d("[STREAK] Streak broken after %d days away, new streak started", daysAway);
            isNewStreak = true;
        } else if (lastLoginDate == 0) {
            // First login ever - don't trigger comeback
            currentStreak = 1;
            Timber.d("[STREAK] First login recorded");
            isNewStreak = true;
        } else {
            // This shouldn't happen, but handle it gracefully
            Timber.w("[STREAK] Unexpected state: today=%d, lastLogin=%d, streak=%d", today, lastLoginDate, currentStreak);
            currentStreak = 1;
            isNewStreak = true;
        }
        
        // Update longest streak if current exceeds it
        int longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0);
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
            Timber.d("[STREAK] New longest streak record: %d days", longestStreak);
        }
        
        // Save updated values
        prefs.edit()
            .putLong(KEY_LAST_LOGIN_DATE, today)
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putLong(KEY_LAST_STREAK_DATE, today)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .putString(KEY_LONGEST_STREAK_DATE, dayNumberToDateString(today))
            .apply();
        
        // Notify achievement manager
        achievementManager.onDailyLogin(currentStreak);
        
        Timber.d("[STREAK] Daily login recorded - new streak: %d days", currentStreak);
        return new StreakUpdate(true, currentStreak, isContinuation, isNewStreak, triggeredComebackAchievement);
    }

    public static class StreakUpdate {
        private final boolean newDayRecorded;
        private final int streakDays;
        private final boolean continuation;
        private final boolean newStreak;
        private final boolean comebackTriggered;

        public StreakUpdate(boolean newDayRecorded, int streakDays, boolean continuation,
                            boolean newStreak, boolean comebackTriggered) {
            this.newDayRecorded = newDayRecorded;
            this.streakDays = streakDays;
            this.continuation = continuation;
            this.newStreak = newStreak;
            this.comebackTriggered = comebackTriggered;
        }
    }
    
    /**
     * Get current streak in days
     */
    public int getCurrentStreak() {
        return prefs.getInt(KEY_CURRENT_STREAK, 0);
    }
    
    
    /**
     * Get stored streak days from SharedPreferences (for debug display)
     */
    public int getStoredStreakDays() {
        return prefs.getInt(KEY_CURRENT_STREAK, 0);
    }
    
    /**
     * Check if the streak popup should be shown today.
     * Returns true only if it hasn't been shown for the current day yet.
     */
    public boolean shouldShowStreakPopupToday() {
        long today = getTodayDate();
        long lastPopupDate = prefs.getLong(KEY_LAST_POPUP_DATE, 0);
        boolean shouldShow = lastPopupDate < today;
        Timber.d("[STREAK_POPUP] shouldShowStreakPopupToday: today=%d, lastPopup=%d, shouldShow=%b", today, lastPopupDate, shouldShow);
        return shouldShow;
    }

    /**
     * Mark the streak popup as shown for today.
     */
    public void markStreakPopupShownToday() {
        long today = getTodayDate();
        prefs.edit().putLong(KEY_LAST_POPUP_DATE, today).apply();
        Timber.d("[STREAK_POPUP] Marked popup shown for day %d", today);
    }

    /**
     * Get today's date as number of "days" since epoch.
     * Uses the device's local timezone so the day changes at local midnight, not UTC midnight.
     * In test mode, a "day" is 10 seconds for quick testing.
     */
    protected long getTodayDate() {
        if (mockTodayDate != null) {
            return mockTodayDate;
        }
        if (testMode) {
            return System.currentTimeMillis() / TEST_DAY_MS;
        }
        long now = System.currentTimeMillis();
        int offsetMs = java.util.TimeZone.getDefault().getOffset(now);
        return (now + offsetMs) / NORMAL_DAY_MS;
    }
    
    /**
     * Enable or disable test mode for quick streak testing.
     * In test mode, 1 "day" = 10 seconds.
     * Note: Changing test mode resets the streak because time units are incompatible.
     */
    public void setTestMode(boolean enabled) {
        boolean wasTestMode = testMode;
        testMode = enabled;
        // Persist test mode setting to preferences
        prefs.edit().putBoolean(KEY_TEST_MODE, enabled).apply();
        
        // Reset streak when switching modes because time units are incompatible
        if (wasTestMode != enabled) {
            resetStreak();
            Timber.d("[STREAK] Streak reset due to test mode change");
        }
        
        Timber.d("[STREAK] Test mode %s - 1 day = %d ms", enabled ? "ENABLED" : "DISABLED", 
                enabled ? TEST_DAY_MS : NORMAL_DAY_MS);
    }
    
    /**
     * Check if test mode is enabled.
     */
    public boolean isTestMode() {
        return testMode;
    }
    
    /**
     * Set mock date for testing (simulates different days)
     */
    public void setMockTodayDate(long daysSinceEpoch) {
        mockTodayDate = daysSinceEpoch;
        Timber.d("[STREAK] Mock date set to: %d", daysSinceEpoch);
    }
    
    /**
     * Clear mock date (for testing)
     */
    public void clearMockTodayDate() {
        mockTodayDate = null;
        Timber.d("[STREAK] Mock date cleared");
    }
    
    /**
     * Get the longest streak ever achieved
     */
    public int getLongestStreak() {
        return prefs.getInt(KEY_LONGEST_STREAK, 0);
    }
    
    /**
     * Get the date when the longest streak was achieved (ISO format)
     */
    public String getLongestStreakDate() {
        return prefs.getString(KEY_LONGEST_STREAK_DATE, null);
    }
    
    /**
     * Convert a day number to ISO date string.
     * Day numbers include local TZ offset, so we subtract it to get UTC millis for formatting.
     */
    private String dayNumberToDateString(long dayNumber) {
        if (testMode) {
            long timestampMs = dayNumber * TEST_DAY_MS;
            return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date(timestampMs));
        }
        long timestampMs = dayNumber * NORMAL_DAY_MS;
        int offsetMs = java.util.TimeZone.getDefault().getOffset(timestampMs);
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date(timestampMs - offsetMs));
    }
    
    /**
     * Get last login date as ISO date string for server sync.
     * Always updates to today first, since syncing means the user is active right now.
     */
    public String getLastLoginDateString() {
        long today = getTodayDate();
        long lastLoginDate = prefs.getLong(KEY_LAST_LOGIN_DATE, 0);
        if (lastLoginDate == 0) return null;
        if (lastLoginDate != today) {
            prefs.edit().putLong(KEY_LAST_LOGIN_DATE, today).apply();
            Timber.d("[STREAK] Updated last_login_date from %d to %d (today) before sync", lastLoginDate, today);
            lastLoginDate = today;
        }
        return dayNumberToDateString(lastLoginDate);
    }
    
    /**
     * Restore streak data from server (bidirectional sync).
     * Resets current streak if server data is stale. Always preserves the highest longest streak.
     */
    public void restoreFromServer(int serverStreak, String serverLastDate, int serverLongestStreak, String serverLongestStreakDate) {
        int localStreak = getCurrentStreak();
        int localLongest = prefs.getInt(KEY_LONGEST_STREAK, 0);
        
        // Always restore longest streak first - takes max of server and local, never loses it
        int maxLongest = Math.max(serverLongestStreak, localLongest);
        if (maxLongest > localLongest) {
            prefs.edit()
                .putInt(KEY_LONGEST_STREAK, maxLongest)
                .putString(KEY_LONGEST_STREAK_DATE, serverLongestStreakDate != null ? serverLongestStreakDate : dayNumberToDateString(getTodayDate()))
                .apply();
            Timber.d("[STREAK_SYNC] Restored longest streak: %d (local was: %d, server: %d)", maxLongest, localLongest, serverLongestStreak);
        }
        
        // Check if current streak data is stale (user was absent for >1 day)
        if (serverStreak > 1 && serverLastDate != null) {
            long serverLastLoginDay = parseDateStringToDayNumber(serverLastDate);
            long today = getTodayDate();
            long daysSinceServerLogin = today - serverLastLoginDay;
            
            Timber.d("[STREAK_SYNC] Checking server data freshness: serverStreak=%d, serverLastDate=%s, daysSince=%d", 
                    serverStreak, serverLastDate, daysSinceServerLogin);
            
            if (daysSinceServerLogin > 1) {
                Timber.d("[STREAK_SYNC] Server data is stale: %d days since last login, resetting server streak to 1", daysSinceServerLogin);
                serverStreak = 1;
            } else {
                Timber.d("[STREAK_SYNC] Server data is fresh (%d days), keeping server streak %d", daysSinceServerLogin, serverStreak);
            }
        }
        
        int maxStreak = Math.max(serverStreak, localStreak);
        
        if (maxStreak != localStreak) {
            prefs.edit()
                .putInt(KEY_CURRENT_STREAK, maxStreak)
                .apply();
            Timber.d("[STREAK_SYNC] Updated current streak to: %d (local: %d, server: %d)", maxStreak, localStreak, serverStreak);
        } else {
            Timber.d("[STREAK_SYNC] Local streak %d is already the maximum (server: %d), keeping local", localStreak, serverStreak);
        }
        
        // Always set last_login_date to today - we are actively syncing right now
        long today = getTodayDate();
        prefs.edit()
            .putLong(KEY_LAST_LOGIN_DATE, today)
            .putLong(KEY_LAST_STREAK_DATE, today)
            .apply();
        Timber.d("[STREAK_SYNC] Set last_login_date to today (%d) for subsequent upload", today);
        
        // Update AchievementManager to keep it in sync
        achievementManager.updateDailyLoginStreak(maxStreak);
        Timber.d("[STREAK_SYNC] AchievementManager dailyLoginStreak updated to: %d", maxStreak);
    }
    
    /**
     * Parse ISO date string to day number (inverse of dayNumberToDateString)
     */
    private long parseDateStringToDayNumber(String dateString) {
        if (dateString == null || "null".equals(dateString) || dateString.isEmpty()) return 0;
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long timestampMs = sdf.parse(dateString).getTime();
            if (testMode) {
                return timestampMs / TEST_DAY_MS;
            }
            int offsetMs = java.util.TimeZone.getDefault().getOffset(timestampMs);
            return (timestampMs + offsetMs) / NORMAL_DAY_MS;
        } catch (Exception e) {
            Timber.w(e, "[STREAK] Failed to parse date string: %s", dateString);
            return 0;
        }
    }
    
    
    /**
     * Reset streak (for testing)
     */
    public void resetStreak() {
        prefs.edit()
            .remove(KEY_LAST_LOGIN_DATE)
            .remove(KEY_CURRENT_STREAK)
            .remove(KEY_LAST_STREAK_DATE)
            .remove(KEY_LAST_POPUP_DATE)
            .remove(KEY_LONGEST_STREAK)
            .remove(KEY_LONGEST_STREAK_DATE)
            .apply();
        Timber.d("[STREAK] Streak reset (including longest streak)");
    }
}
