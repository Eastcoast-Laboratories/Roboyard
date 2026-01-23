package roboyard.eclabs.achievements;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Tests for StreakManager daily login tracking
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
    
    @Test
    public void testDailyLogin7DaysUnlocksAchievement() {
        // Simulate 7 consecutive daily logins by advancing days
        long startDay = 1000;
        for (int i = 0; i < 7; i++) {
            streakManager.setMockTodayDate(startDay + i);
            streakManager.recordDailyLogin();
        }
        streakManager.clearMockTodayDate();
        
        assertEquals("Streak should be 7", 7, streakManager.getCurrentStreak());
        assertTrue("daily_login_7 achievement should be unlocked", 
                achievementManager.isUnlocked("daily_login_7"));
    }
    
    @Test
    public void testDailyLogin30DaysUnlocksAchievement() {
        // Simulate 30 consecutive daily logins by advancing days
        long startDay = 2000;
        for (int i = 0; i < 30; i++) {
            streakManager.setMockTodayDate(startDay + i);
            streakManager.recordDailyLogin();
        }
        streakManager.clearMockTodayDate();
        
        assertEquals("Streak should be 30", 30, streakManager.getCurrentStreak());
        assertTrue("daily_login_30 achievement should be unlocked", 
                achievementManager.isUnlocked("daily_login_30"));
    }
    
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
}
