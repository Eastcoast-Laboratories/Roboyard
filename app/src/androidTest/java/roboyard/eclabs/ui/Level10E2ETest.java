package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.achievements.AchievementManager;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E test: Complete levels 1-10 to unlock 3_star_10_levels achievement
 * Uses the solver solution to execute moves automatically
 *
 * Tags: e2e, level-game, achievement, 3-star, solver
 */
@RunWith(AndroidJUnit4.class)
public class Level10E2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;
    private volatile Boolean levelCompleted = null;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][E2E_10LEVELS] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][E2E_10LEVELS] ========== TEST FINISHED ==========");
    }

    @Test
    public void testComplete10Levels_Unlocks3Star10LevelsAchievement() throws InterruptedException {
        Timber.d("[UNITTESTS][E2E_10LEVELS] Starting 10 levels completion test");
        
        // Close achievement popup if present
        TestHelper.closeAchievementPopupIfPresent();
        
        // Start Level 1 programmatically
        TestHelper.startLevelGame(activityRule, 1);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Complete levels 1-11 (to ensure achievement is unlocked and visible)
        for (int level = 1; level <= 11; level++) {
            Timber.d("[UNITTESTS][E2E_10LEVELS] ===== Starting Level %d =====", level);
            
            // Complete level using solver via TestHelper
            boolean completed = TestHelper.completeLevelWithSolver(activityRule, gameStateManager, level, "E2E_10LEVELS");
            
            if (!completed) {
                Timber.e("[UNITTESTS][E2E_10LEVELS] Level %d NOT completed!", level);
                fail("Level " + level + " should be completed");
            }
            
            // Check achievement status after each level
            Thread.sleep(1000);
            boolean achievement3Star10Unlocked = achievementManager.isUnlocked("3_star_10_levels");
            Timber.d("[UNITTESTS][E2E_10LEVELS] After Level %d: 3_star_10_levels = %s", level, achievement3Star10Unlocked);
            
            // ASSERTION: Achievement should NOT be unlocked before Level 10
            if (level < 10) {
                assertFalse("3_star_10_levels should NOT be unlocked before Level 10 (currently at Level " + level + ")", 
                        achievement3Star10Unlocked);
            }
            
            // After Level 10, check if 3_star_10_levels achievement is unlocked
            if (level == 10) {
                Timber.d("[UNITTESTS][E2E_10LEVELS] ===== CHECKING ACHIEVEMENT AFTER LEVEL 10 =====");
                
                // Wait longer for achievement to be processed
                for (int wait = 0; wait < 5; wait++) {
                    Thread.sleep(2000);
                    boolean achievement3Star10Check = achievementManager.isUnlocked("3_star_10_levels");
                    Timber.d("[UNITTESTS][E2E_10LEVELS] Achievement check %d/5: 3_star_10_levels = %s", wait + 1, achievement3Star10Check);
                    
                    if (achievement3Star10Check) {
                        Timber.d("[UNITTESTS][E2E_10LEVELS] ✓ VISUAL CONFIRMATION: 3_star_10_levels achievement UNLOCKED after %d seconds!", (wait + 1) * 2);
                        break;
                    }
                }
            }
            
            // If not the last level, click Next Level button
            if (level < 11) {
                Thread.sleep(2000);
                Timber.d("[UNITTESTS][E2E_10LEVELS] Clicking Next Level button");
                try {
                    onView(withId(R.id.next_level_button)).check(matches(isDisplayed())).perform(click());
                    Thread.sleep(500);
                } catch (Exception e) {
                    Timber.e(e, "[E2E_10LEVELS] Could not click Next Level button");
                    fail("Could not click Next Level button after level " + level);
                }
            }
            
            // Reset for next level
            levelCompleted = null;
        }
        
        // Wait for achievements to be processed after Level 11
        Thread.sleep(5000);
        
        Timber.d("[UNITTESTS][E2E_10LEVELS] ===== FINAL ACHIEVEMENT CHECK =====");
        
        // Assertions
        assertTrue("level_1_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        assertTrue("level_10_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_10_complete"));
        assertTrue("3_star_10_levels achievement should be unlocked after completing 10 levels with 3 stars", 
                achievementManager.isUnlocked("3_star_10_levels"));
        
        Timber.d("[UNITTESTS][E2E_10LEVELS] ✓ Test passed: All 11 levels completed, 3_star_10_levels achievement confirmed unlocked");
    }
}
