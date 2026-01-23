package roboyard.eclabs.achievements;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import timber.log.Timber;

/**
 * Test to verify that achievement counters are properly reset and don't carry over between tests
 */
@RunWith(AndroidJUnit4.class)
public class AchievementResetTest {

    private Context context;
    private AchievementManager achievementManager;
    private static final String PREFS_NAME = "roboyard_achievements";

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        Timber.d("[RESET_TEST] setUp complete - all achievements reset");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[RESET_TEST] tearDown complete - all achievements reset");
    }

    /**
     * Simulate the scenario from the logs:
     * First test completes 5 levels, then reset
     * Second test should start fresh with counters at 0
     */
    @Test
    public void testCountersResetBetweenTests() {
        Timber.d("[RESET_TEST] ===== FIRST SCENARIO: Complete 5 levels =====");
        
        // Complete 5 levels with perfect solutions and no hints
        for (int i = 1; i <= 5; i++) {
            Timber.d("[RESET_TEST] Completing level %d", i);
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        
        // Verify achievements are NOT unlocked yet
        assertFalse("level_10_complete should NOT be unlocked after 5 levels",
                achievementManager.isUnlocked("level_10_complete"));
        assertFalse("perfect_solutions_10 should NOT be unlocked after 5 perfect solutions",
                achievementManager.isUnlocked("perfect_solutions_10"));
        
        Timber.d("[RESET_TEST] ✓ After 5 levels: achievements NOT unlocked");
        
        // Reset (simulating end of first test)
        Timber.d("[RESET_TEST] Resetting achievements");
        achievementManager.resetAll();
        
        // Verify reset worked
        assertFalse("level_10_complete should be locked after reset",
                achievementManager.isUnlocked("level_10_complete"));
        assertFalse("perfect_solutions_10 should be locked after reset",
                achievementManager.isUnlocked("perfect_solutions_10"));
        
        Timber.d("[RESET_TEST] ✓ After reset: all achievements locked");
        
        // Now simulate second test starting fresh
        Timber.d("[RESET_TEST] ===== SECOND SCENARIO: Complete 5 more levels (should start fresh) =====");
        
        // Complete 5 more levels
        for (int i = 1; i <= 5; i++) {
            Timber.d("[RESET_TEST] Completing level %d (second scenario)", i);
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
            
            // Verify achievements are still NOT unlocked
            assertFalse("level_10_complete should NOT be unlocked after level " + i + " in second scenario",
                    achievementManager.isUnlocked("level_10_complete"));
            assertFalse("perfect_solutions_10 should NOT be unlocked after level " + i + " in second scenario",
                    achievementManager.isUnlocked("perfect_solutions_10"));
        }
        
        Timber.d("[RESET_TEST] ✓ After 5 more levels: achievements still NOT unlocked");
        
        // Complete 5 more levels to reach 10 total
        Timber.d("[RESET_TEST] ===== COMPLETING 5 MORE LEVELS TO REACH 10 =====");
        for (int i = 6; i <= 10; i++) {
            Timber.d("[RESET_TEST] Completing level %d (to reach 10)", i);
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        
        // Now achievements should be unlocked
        assertTrue("level_10_complete should be unlocked after 10 levels",
                achievementManager.isUnlocked("level_10_complete"));
        assertTrue("perfect_solutions_10 should be unlocked after 10 perfect solutions",
                achievementManager.isUnlocked("perfect_solutions_10"));
        
        Timber.d("[RESET_TEST] ✓ After 10 levels: all achievements unlocked");
    }

    /**
     * Test that SharedPreferences are properly cleared
     */
    @Test
    public void testSharedPreferencesClearedOnReset() {
        // Complete 5 levels
        for (int i = 1; i <= 5; i++) {
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        
        // Verify counters are saved in SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int levelsCompleted = prefs.getInt("counter_levels_completed", -1);
        int perfectSolutions = prefs.getInt("counter_perfect_solutions", -1);
        int noHintLevels = prefs.getInt("counter_no_hint_levels", -1);
        
        Timber.d("[RESET_TEST] Before reset: levelsCompleted=%d, perfectSolutions=%d, noHintLevels=%d",
                levelsCompleted, perfectSolutions, noHintLevels);
        
        assertEquals("levelsCompleted should be 5", 5, levelsCompleted);
        assertEquals("perfectSolutions should be 5", 5, perfectSolutions);
        assertEquals("noHintLevels should be 5", 5, noHintLevels);
        
        // Reset
        achievementManager.resetAll();
        
        // Verify counters are cleared from SharedPreferences
        levelsCompleted = prefs.getInt("counter_levels_completed", -1);
        perfectSolutions = prefs.getInt("counter_perfect_solutions", -1);
        noHintLevels = prefs.getInt("counter_no_hint_levels", -1);
        
        Timber.d("[RESET_TEST] After reset: levelsCompleted=%d, perfectSolutions=%d, noHintLevels=%d",
                levelsCompleted, perfectSolutions, noHintLevels);
        
        assertEquals("levelsCompleted should be cleared", -1, levelsCompleted);
        assertEquals("perfectSolutions should be cleared", -1, perfectSolutions);
        assertEquals("noHintLevels should be cleared", -1, noHintLevels);
    }
}
