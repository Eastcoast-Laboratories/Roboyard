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
 * Test to verify achievement counters are correct and achievements unlock at the right time
 */
@RunWith(AndroidJUnit4.class)
public class AchievementCounterTest {

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
     * Test that level_10_complete is NOT unlocked before 10 levels
     */
    @Test
    public void testLevel10CompleteNotUnlockedBefore10Levels() {
        // Complete levels 1-5 with perfect solutions and no hints
        for (int i = 1; i <= 5; i++) {
            Timber.d("[TEST] Completing level %d with perfect solution and no hints", i);
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
            
            // Check that level_10_complete is NOT unlocked yet
            assertFalse("level_10_complete should NOT be unlocked after level " + i,
                    achievementManager.isUnlocked("level_10_complete"));
            
            // Check that perfect_solutions_10 is NOT unlocked yet
            assertFalse("perfect_solutions_10 should NOT be unlocked after level " + i,
                    achievementManager.isUnlocked("perfect_solutions_10"));
            
        }
        
        Timber.d("[TEST] All assertions passed - achievements NOT unlocked before level 10");
    }

    /**
     * Test that level_10_complete IS unlocked after 10 levels
     */
    @Test
    public void testLevel10CompleteUnlockedAfter10Levels() {
        // Complete levels 1-10 with perfect solutions and no hints
        for (int i = 1; i <= 10; i++) {
            Timber.d("[TEST] Completing level %d", i);
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        
        // Check that all achievements are unlocked
        assertTrue("level_10_complete should be unlocked after 10 levels",
                achievementManager.isUnlocked("level_10_complete"));
        assertTrue("perfect_solutions_10 should be unlocked after 10 perfect solutions",
                achievementManager.isUnlocked("perfect_solutions_10"));
        
        Timber.d("[TEST] All assertions passed - achievements unlocked after level 10");
    }

    /**
     * Test that perfect_solutions_10 is NOT unlocked if not all solutions are perfect
     */
    @Test
    public void testPerfectSolutions10NotUnlockedWithNonOptimalMoves() {
        // Complete 9 levels with perfect solutions
        for (int i = 1; i <= 9; i++) {
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        
        // Complete 10th level with NON-optimal moves
        achievementManager.onLevelCompleted(10, 7, 5, 0, 3, 10000);
        
        // Check that perfect_solutions_10 is NOT unlocked
        assertFalse("perfect_solutions_10 should NOT be unlocked with non-optimal moves",
                achievementManager.isUnlocked("perfect_solutions_10"));
        
        Timber.d("[TEST] Assertion passed - perfect_solutions_10 NOT unlocked with non-optimal moves");
    }

    // Note: testNoHints10NotUnlockedWithHints removed - no_hints_10/50 achievements are removed (hints not allowed in levels)
}
