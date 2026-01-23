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
 * Test to check if onLevelCompleted is called multiple times or if achievements are unlocked incorrectly
 */
@RunWith(AndroidJUnit4.class)
public class AchievementCallCountTest {

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
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
    }

    /**
     * Test: Complete level 5 and check if achievements are incorrectly unlocked
     */
    @Test
    public void testLevel5DoesNotUnlockLevel10Achievements() {
        Timber.d("[CALL_COUNT_TEST] Completing levels 1-5");
        
        for (int i = 1; i <= 5; i++) {
            Timber.d("[CALL_COUNT_TEST] Level %d: calling onLevelCompleted(levelId=%d, moves=3, optimal=3, hints=0, stars=3, time=4500)", i, i);
            achievementManager.onLevelCompleted(i, 3, 3, 0, 3, 4500);
            
            // Check achievements after each level
            boolean level10 = achievementManager.isUnlocked("level_10_complete");
            boolean perfect10 = achievementManager.isUnlocked("perfect_solutions_10");
            boolean threeStar10 = achievementManager.isUnlocked("3_star_10_levels");
            
                    i, level10, perfect10, noHints10, threeStar10);
            
            // These should all be false
            if (level10 || perfect10 || noHints10 || threeStar10) {
                Timber.e("[CALL_COUNT_TEST] ERROR: Achievements unlocked too early after level %d!", i);
                        level10, perfect10, noHints10, threeStar10);
            }
            
            assertFalse("level_10_complete should NOT be unlocked after level " + i, level10);
            assertFalse("perfect_solutions_10 should NOT be unlocked after level " + i, perfect10);
            assertFalse("3_star_10_levels should NOT be unlocked after level " + i, threeStar10);
        }
        
        Timber.d("[CALL_COUNT_TEST] ✓ Test passed - no achievements unlocked after level 5");
    }

    /**
     * Test: Check if the issue is with how counters are loaded from SharedPreferences
     */
    @Test
    public void testCounterLoadingFromPreferences() {
        Timber.d("[CALL_COUNT_TEST] ===== TESTING COUNTER LOADING =====");
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Manually set counters to 10 in SharedPreferences (simulating a previous test run)
        prefs.edit()
                .putInt("counter_levels_completed", 10)
                .putInt("counter_perfect_solutions", 10)
                .putInt("counter_no_hint_levels", 10)
                .apply();
        
        Timber.d("[CALL_COUNT_TEST] Manually set counters to 10 in SharedPreferences");
        
        // Reset the singleton to force reload from SharedPreferences
        achievementManager.resetAll();
        
        // Check if achievements are unlocked (they should be false because resetAll clears them)
        boolean level10 = achievementManager.isUnlocked("level_10_complete");
        boolean perfect10 = achievementManager.isUnlocked("perfect_solutions_10");
        
                level10, perfect10, noHints10);
        
        // These should be false because resetAll clears everything
        assertFalse("level_10_complete should be false after resetAll", level10);
        assertFalse("perfect_solutions_10 should be false after resetAll", perfect10);
        
        Timber.d("[CALL_COUNT_TEST] ✓ Test passed - resetAll properly clears achievements");
    }

    /**
     * Test: Check if the issue is with the levelId parameter
     */
    @Test
    public void testLevelIdParameter() {
        Timber.d("[CALL_COUNT_TEST] ===== TESTING LEVEL ID PARAMETER =====");
        
        // Complete level 10 directly
        Timber.d("[CALL_COUNT_TEST] Calling onLevelCompleted(levelId=10, moves=3, optimal=3, hints=0, stars=3, time=4500)");
        achievementManager.onLevelCompleted(10, 3, 3, 0, 3, 4500);
        
        // Check if 3_star_10_levels is unlocked
        boolean threeStar10 = achievementManager.isUnlocked("3_star_10_levels");
        Timber.d("[CALL_COUNT_TEST] After level 10: 3_star_10_levels=%s", threeStar10);
        
        assertTrue("3_star_10_levels should be unlocked when levelId >= 10", threeStar10);
        
        // Now complete level 5 and check if it's still unlocked (it should be)
        Timber.d("[CALL_COUNT_TEST] Calling onLevelCompleted(levelId=5, moves=3, optimal=3, hints=0, stars=3, time=4500)");
        achievementManager.onLevelCompleted(5, 3, 3, 0, 3, 4500);
        
        threeStar10 = achievementManager.isUnlocked("3_star_10_levels");
        Timber.d("[CALL_COUNT_TEST] After level 5: 3_star_10_levels=%s", threeStar10);
        
        assertTrue("3_star_10_levels should still be unlocked", threeStar10);
        
        Timber.d("[CALL_COUNT_TEST] ✓ Test passed - levelId parameter works correctly");
    }
}
