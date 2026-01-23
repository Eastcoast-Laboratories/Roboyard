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
 * Debug test to reproduce the issue from the logs where achievements unlock after Level 5
 */
@RunWith(AndroidJUnit4.class)
public class AchievementDebugTest {

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
        Timber.d("[DEBUG_TEST] setUp complete");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[DEBUG_TEST] tearDown complete");
    }

    /**
     * Reproduce the exact scenario from the logs:
     * Complete levels 1-5 with perfect solutions and no hints
     * After level 5, check which achievements are unlocked
     */
    @Test
    public void testReproduceLogsScenario() {
        Timber.d("[DEBUG_TEST] ===== REPRODUCING LOG SCENARIO =====");
        
        // Complete levels 1-5 exactly as shown in logs
        for (int level = 1; level <= 5; level++) {
            Timber.d("[DEBUG_TEST] ===== LEVEL %d =====", level);
            
            // Log before completion
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int levelsCompletedBefore = prefs.getInt("counter_levels_completed", 0);
            int perfectSolutionsBefore = prefs.getInt("counter_perfect_solutions", 0);
            int noHintLevelsBefore = prefs.getInt("counter_no_hint_levels", 0);
            
            Timber.d("[DEBUG_TEST] BEFORE: levelsCompleted=%d, perfectSolutions=%d, noHintLevels=%d",
                    levelsCompletedBefore, perfectSolutionsBefore, noHintLevelsBefore);
            
            // Complete level with perfect solution and no hints
            achievementManager.onLevelCompleted(level, 3, 3, 0, 3, 4500);
            
            // Log after completion
            int levelsCompletedAfter = prefs.getInt("counter_levels_completed", 0);
            int perfectSolutionsAfter = prefs.getInt("counter_perfect_solutions", 0);
            int noHintLevelsAfter = prefs.getInt("counter_no_hint_levels", 0);
            
            Timber.d("[DEBUG_TEST] AFTER: levelsCompleted=%d, perfectSolutions=%d, noHintLevels=%d",
                    levelsCompletedAfter, perfectSolutionsAfter, noHintLevelsAfter);
            
            // Check achievements
            boolean level10Complete = achievementManager.isUnlocked("level_10_complete");
            boolean perfect10 = achievementManager.isUnlocked("perfect_solutions_10");
            boolean threeStar10 = achievementManager.isUnlocked("3_star_10_levels");
            
                    level10Complete, perfect10, noHints10, threeStar10);
            
            // CRITICAL: Assert that achievements are NOT unlocked
            assertFalse("level_10_complete should NOT be unlocked after level " + level,
                    level10Complete);
            assertFalse("perfect_solutions_10 should NOT be unlocked after level " + level,
                    perfect10);
                    noHints10);
            assertFalse("3_star_10_levels should NOT be unlocked after level " + level,
                    threeStar10);
        }
        
        Timber.d("[DEBUG_TEST] ✓ Test passed - no achievements unlocked after level 5");
    }

    /**
     * Check if there's an issue with the singleton instance
     */
    @Test
    public void testSingletonInstance() {
        Timber.d("[DEBUG_TEST] ===== TESTING SINGLETON INSTANCE =====");
        
        // Get instance multiple times
        AchievementManager instance1 = AchievementManager.getInstance(context);
        AchievementManager instance2 = AchievementManager.getInstance(context);
        
        // They should be the same object
        assertSame("Singleton instances should be the same", instance1, instance2);
        
        Timber.d("[DEBUG_TEST] ✓ Singleton instance is correct");
    }

    /**
     * Check if there's an issue with the counter incrementation
     */
    @Test
    public void testCounterIncrementation() {
        Timber.d("[DEBUG_TEST] ===== TESTING COUNTER INCREMENTATION =====");
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Check initial counters
        int initialLevels = prefs.getInt("counter_levels_completed", 0);
        int initialPerfect = prefs.getInt("counter_perfect_solutions", 0);
        int initialNoHints = prefs.getInt("counter_no_hint_levels", 0);
        
        Timber.d("[DEBUG_TEST] Initial: levels=%d, perfect=%d, noHints=%d",
                initialLevels, initialPerfect, initialNoHints);
        
        assertEquals("Initial levels should be 0", 0, initialLevels);
        assertEquals("Initial perfect should be 0", 0, initialPerfect);
        assertEquals("Initial noHints should be 0", 0, initialNoHints);
        
        // Complete one level
        achievementManager.onLevelCompleted(1, 3, 3, 0, 3, 4500);
        
        // Check counters after first level
        int afterLevel1Levels = prefs.getInt("counter_levels_completed", 0);
        int afterLevel1Perfect = prefs.getInt("counter_perfect_solutions", 0);
        int afterLevel1NoHints = prefs.getInt("counter_no_hint_levels", 0);
        
        Timber.d("[DEBUG_TEST] After level 1: levels=%d, perfect=%d, noHints=%d",
                afterLevel1Levels, afterLevel1Perfect, afterLevel1NoHints);
        
        assertEquals("After level 1, levels should be 1", 1, afterLevel1Levels);
        assertEquals("After level 1, perfect should be 1", 1, afterLevel1Perfect);
        assertEquals("After level 1, noHints should be 1", 1, afterLevel1NoHints);
        
        // Complete 9 more levels
        for (int i = 2; i <= 10; i++) {
            achievementManager.onLevelCompleted(i, 3, 3, 0, 3, 4500);
        }
        
        // Check counters after 10 levels
        int afterLevel10Levels = prefs.getInt("counter_levels_completed", 0);
        int afterLevel10Perfect = prefs.getInt("counter_perfect_solutions", 0);
        int afterLevel10NoHints = prefs.getInt("counter_no_hint_levels", 0);
        
        Timber.d("[DEBUG_TEST] After level 10: levels=%d, perfect=%d, noHints=%d",
                afterLevel10Levels, afterLevel10Perfect, afterLevel10NoHints);
        
        assertEquals("After level 10, levels should be 10", 10, afterLevel10Levels);
        assertEquals("After level 10, perfect should be 10", 10, afterLevel10Perfect);
        assertEquals("After level 10, noHints should be 10", 10, afterLevel10NoHints);
        
        // Check that achievements are now unlocked
        assertTrue("level_10_complete should be unlocked after 10 levels",
                achievementManager.isUnlocked("level_10_complete"));
        assertTrue("perfect_solutions_10 should be unlocked after 10 perfect solutions",
                achievementManager.isUnlocked("perfect_solutions_10"));
        
        Timber.d("[DEBUG_TEST] ✓ Counter incrementation is correct");
    }
}
