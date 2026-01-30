package roboyard.eclabs.achievements;

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
    private static final String KEY_TEST_MODE = "test_mode";
    
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
        
        // Save updated values
        prefs.edit()
            .putLong(KEY_LAST_LOGIN_DATE, today)
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putLong(KEY_LAST_STREAK_DATE, today)
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

        public boolean isNewDayRecorded() {
            return newDayRecorded;
        }

        public int getStreakDays() {
            return streakDays;
        }

        public boolean isContinuation() {
            return continuation;
        }

        public boolean isNewStreak() {
            return newStreak;
        }

        public boolean isComebackTriggered() {
            return comebackTriggered;
        }
    }
    
    /**
     * Get current streak in days
     */
    public int getCurrentStreak() {
        return prefs.getInt(KEY_CURRENT_STREAK, 0);
    }
    
    /**
     * Get last login date (as day number since epoch)
     */
    public long getLastLoginDate() {
        return prefs.getLong(KEY_LAST_LOGIN_DATE, 0);
    }
    
    /**
     * Get stored streak days from SharedPreferences (for debug display)
     */
    public int getStoredStreakDays() {
        return prefs.getInt(KEY_CURRENT_STREAK, 0);
    }
    
    /**
     * Get today's date as number of "days" since epoch.
     * In test mode, a "day" is 10 seconds for quick testing.
     */
    private long getTodayDate() {
        if (mockTodayDate != null) {
            return mockTodayDate;
        }
        long dayDuration = testMode ? TEST_DAY_MS : NORMAL_DAY_MS;
        return System.currentTimeMillis() / dayDuration;
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
     * Reset streak (for testing)
     */
    public void resetStreak() {
        prefs.edit()
            .remove(KEY_LAST_LOGIN_DATE)
            .remove(KEY_CURRENT_STREAK)
            .remove(KEY_LAST_STREAK_DATE)
            .apply();
        Timber.d("[STREAK] Streak reset");
    }
}
