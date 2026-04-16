package roboyard.eclabs.achievements;

import roboyard.ui.achievements.AchievementManager;
import roboyard.ui.achievements.StreakManager;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Tests for StreakManager daily login tracking.
 * Verifies streak increments, resets, and achievement integration for consecutive daily logins.
 *
 * Tags: streak, daily-login, achievement-manager, instrumented
 */
@RunWith(AndroidJUnit4.class)
public class StreakManagerTest {
    
    private Context context;
    private StreakManager streakManager;
    private AchievementManager achievementManager;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        streakManager = StreakManager.getInstance(context);
        achievementManager = AchievementManager.getInstance(context);
        
        // Reset for clean test
        streakManager.resetStreak();
        achievementManager.resetAll();
    }
    
    @After
    public void tearDown() {
        streakManager.resetStreak();
        achievementManager.resetAll();
    }
    
    @Test
    public void testFirstDailyLogin() {
        streakManager.recordDailyLogin();
        assertEquals("First login should create streak of 1", 1, streakManager.getCurrentStreak());
    }
    
    // NOTE: This test is disabled - daily login achievements are tested in AchievementManagerTest
    // @Test
    // public void testDailyLogin7DaysUnlocksAchievement() {
    //     // Simulate 7 consecutive daily logins by advancing days
    //     long startDay = 1000;
    //     for (int i = 0; i < 7; i++) {
    //         streakManager.setMockTodayDate(startDay + i);
    //         streakManager.recordDailyLogin();
    //     }
    //     streakManager.clearMockTodayDate();
    //     
    //     // Trigger achievement check by starting a game
    //     achievementManager.onNewGameStarted();
    //     
    //     assertEquals("Streak should be 7", 7, streakManager.getCurrentStreak());
    //     assertTrue("daily_login_7 achievement should be unlocked", 
    //             achievementManager.isUnlocked("daily_login_7"));
    // }
    
    // NOTE: This test is disabled - daily login achievements are tested in AchievementManagerTest
    // @Test
    // public void testDailyLogin30DaysUnlocksAchievement() {
    //     // Simulate 30 consecutive daily logins by advancing days
    //     long startDay = 2000;
    //     for (int i = 0; i < 30; i++) {
    //         streakManager.setMockTodayDate(startDay + i);
    //         streakManager.recordDailyLogin();
    //     }
    //     streakManager.clearMockTodayDate();
    //     
    //     // Trigger achievement check by starting a game
    //     achievementManager.onNewGameStarted();
    //     
    //     assertEquals("Streak should be 30", 30, streakManager.getCurrentStreak());
    //     assertTrue("daily_login_30 achievement should be unlocked", 
    //             achievementManager.isUnlocked("daily_login_30"));
    // }
    
    @Test
    public void testDuplicateDailyLoginIgnored() {
        long today = 3000;
        streakManager.setMockTodayDate(today);
        streakManager.recordDailyLogin();
        int firstStreak = streakManager.getCurrentStreak();
        
        // Try to record login again on same day
        streakManager.recordDailyLogin();
        int secondStreak = streakManager.getCurrentStreak();
        
        streakManager.clearMockTodayDate();
        
        assertEquals("Duplicate login on same day should not increase streak", 
                firstStreak, secondStreak);
    }
    
    @Test
    public void testStreakPopupOnlyOncePerDay() {
        long day1 = 5000;
        streakManager.setMockTodayDate(day1);
        
        // Should show popup on first check
        assertTrue("Popup should show on first check of the day",
                streakManager.shouldShowStreakPopupToday());
        
        // Mark as shown
        streakManager.markStreakPopupShownToday();
        
        // Should NOT show again on same day
        assertFalse("Popup should NOT show again on same day",
                streakManager.shouldShowStreakPopupToday());
        
        // Advance to next day
        streakManager.setMockTodayDate(day1 + 1);
        
        // Should show again on new day
        assertTrue("Popup should show on new day",
                streakManager.shouldShowStreakPopupToday());
        
        // Mark as shown on day 2
        streakManager.markStreakPopupShownToday();
        
        // Should NOT show again on day 2
        assertFalse("Popup should NOT show again on day 2",
                streakManager.shouldShowStreakPopupToday());
        
        streakManager.clearMockTodayDate();
    }
    
    @Test
    public void testStreakPopupPersistsAcrossInstances() {
        long day1 = 6000;
        streakManager.setMockTodayDate(day1);
        
        // Mark popup as shown
        streakManager.markStreakPopupShownToday();
        assertFalse("Popup should not show after marking",
                streakManager.shouldShowStreakPopupToday());
        
        // Simulate app restart by getting a fresh instance check
        // (SharedPreferences persists, so same instance still reads persisted value)
        assertFalse("Popup should still not show (persisted in storage)",
                streakManager.shouldShowStreakPopupToday());
        
        streakManager.clearMockTodayDate();
    }
    
    @Test
    public void testComebackAfter30DaysInactivity() {
        long day1 = 4000;
        long day32 = day1 + 31;  // 31 days later
        
        // First login
        streakManager.setMockTodayDate(day1);
        streakManager.recordDailyLogin();
        
        // Login after 31 days away
        streakManager.setMockTodayDate(day32);
        streakManager.recordDailyLogin();
        
        streakManager.clearMockTodayDate();
        
        assertTrue("comeback_player achievement should be unlocked after 30+ days away", 
                achievementManager.isUnlocked("comeback_player"));
    }
    
    @Test
    public void testStreakLossAndLongestStreakPreservation() {
        long day1 = 7000;
        long day2 = day1 + 1;
        long day4 = day1 + 3;  // Skip day 3 (lose streak)
        
        // Build a 5-day streak
        streakManager.setMockTodayDate(day1);
        streakManager.recordDailyLogin();  // Streak: 1
        assertEquals("Streak should be 1 on day 1", 1, streakManager.getCurrentStreak());
        assertEquals("Longest should be 1", 1, streakManager.getLongestStreak());
        
        streakManager.setMockTodayDate(day2);
        streakManager.recordDailyLogin();  // Streak: 2
        assertEquals("Streak should be 2 on day 2", 2, streakManager.getCurrentStreak());
        assertEquals("Longest should be 2", 2, streakManager.getLongestStreak());
        
        // Skip day 3, login on day 4 - should lose streak
        streakManager.setMockTodayDate(day4);
        streakManager.recordDailyLogin();  // Streak: 1 (reset)
        assertEquals("Streak should reset to 1 after skipping day", 1, streakManager.getCurrentStreak());
        assertEquals("Longest streak should be preserved at 2", 2, streakManager.getLongestStreak());
        
        // Continue streak to day 5
        streakManager.setMockTodayDate(day4 + 1);
        streakManager.recordDailyLogin();  // Streak: 2
        assertEquals("Streak should be 2 on new streak", 2, streakManager.getCurrentStreak());
        assertEquals("Longest should still be 2", 2, streakManager.getLongestStreak());
        
        streakManager.clearMockTodayDate();
    }
    
    @Test
    public void testServerSyncTakesMaximumStreak() {
        long day1 = 8000;
        
        // Build local streak of 5
        streakManager.setMockTodayDate(day1);
        for (int i = 0; i < 5; i++) {
            streakManager.setMockTodayDate(day1 + i);
            streakManager.recordDailyLogin();
        }
        assertEquals("Local streak should be 5", 5, streakManager.getCurrentStreak());
        assertEquals("Local longest should be 5", 5, streakManager.getLongestStreak());
        
        // Server has lower streak (3) - should keep local
        streakManager.restoreFromServer(3, "2023-01-01", 3, "2023-01-01");
        assertEquals("Should keep higher local streak (5)", 5, streakManager.getCurrentStreak());
        assertEquals("Should keep higher local longest (5)", 5, streakManager.getLongestStreak());
        
        // Server has higher streak (8) - should update to server
        streakManager.restoreFromServer(8, "2023-01-02", 8, "2023-01-02");
        assertEquals("Should update to higher server streak (8)", 8, streakManager.getCurrentStreak());
        assertEquals("Should update longest to server streak (8)", 8, streakManager.getLongestStreak());
        
        // AchievementManager should also be updated
        achievementManager.checkAndUnlockStreakAchievements();
        // Verify achievement progress reflects the updated streak
        assertEquals("AchievementManager should have updated streak", 8, 
                achievementManager.getProgress("daily_login_7").current);
        
        streakManager.clearMockTodayDate();
    }
    
    @Test
    public void testServerSyncResetsStreakAfterLongAbsence() {
        long day1 = 9000;
        
        // Build local streak first to establish longest streak
        streakManager.setMockTodayDate(day1);
        for (int i = 0; i < 11; i++) {
            streakManager.setMockTodayDate(day1 + i);
            streakManager.recordDailyLogin();
        }
        assertEquals("Local streak should be 11", 11, streakManager.getCurrentStreak());
        assertEquals("Local longest should be 11", 11, streakManager.getLongestStreak());
        
        // Get the date when streak was 11 (today in mock time)
        String streakDate = streakManager.getLastLoginDateString();
        
        // Advance 90 days into future (simulate long absence)
        streakManager.setMockTodayDate(day1 + 90);
        
        // Server has old streak data (11) from 90 days ago, longest=11
        streakManager.restoreFromServer(11, streakDate, 11, streakDate);
        
        // Should reset to 1 due to long absence
        assertEquals("Streak should reset to 1 after 90 days absence", 1, streakManager.getCurrentStreak());
        
        // Longest streak should still be preserved (it was 11)
        assertEquals("Longest streak should be preserved at 11", 11, streakManager.getLongestStreak());
        
        streakManager.clearMockTodayDate();
    }
    
    @Test
    public void testServerSyncKeepsStreakForRecentData() {
        long day1 = 10000;
        
        // Simulate recent server data (streak 5, yesterday)
        streakManager.setMockTodayDate(day1);
        String yesterdayDate = streakManager.getLastLoginDateString();
        
        // Advance to today (1 day later)
        streakManager.setMockTodayDate(day1 + 1);
        
        // Server has recent streak data (5) from yesterday
        streakManager.restoreFromServer(5, yesterdayDate, 5, yesterdayDate);
        
        // Should keep streak 5 (only 1 day gap)
        assertEquals("Streak should be preserved for 1 day gap", 5, streakManager.getCurrentStreak());
        
        streakManager.clearMockTodayDate();
    }
}
