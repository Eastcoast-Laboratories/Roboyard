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
 * E2E test: Complete levels 1-3 to test level completion and achievements
 * Uses the solver solution to execute moves automatically
 *
 * Tags: e2e, level-game, achievement, solver
 */
@RunWith(AndroidJUnit4.class)
public class Level3E2ETest {

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
    public void testComplete3Levels_UnlocksAchievements() throws InterruptedException {
        Timber.d("[UNITTESTS][E2E_3LEVELS] Starting 3 levels completion test");
        
        // Close achievement popup if present
        TestHelper.closeAchievementPopupIfPresent();
        
        // Start Level 1 programmatically
        TestHelper.startLevelGame(activityRule, 1);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Complete levels 1-3
        for (int level = 1; level <= 3; level++) {
            Timber.d("[UNITTESTS][E2E_3LEVELS] ===== Starting Level %d =====", level);
            
            // Complete level using solver via TestHelper
            boolean completed = TestHelper.completeLevelWithSolver(activityRule, gameStateManager, level, "E2E_3LEVELS");
            
            if (!completed) {
                Timber.e("[UNITTESTS][E2E_3LEVELS] Level %d NOT completed!", level);
                fail("Level " + level + " should be completed");
            }
            
            // Check achievement status after each level
            Thread.sleep(1000);
            boolean level1Complete = achievementManager.isUnlocked("level_1_complete");
            Timber.d("[UNITTESTS][E2E_3LEVELS] After Level %d: level_1_complete = %s", level, level1Complete);
            
            // After level 1, level_1_complete should be unlocked
            if (level >= 1) {
                assertTrue("level_1_complete should be unlocked after Level 1", level1Complete);
            }
            
            // If not the last level, click Next Level button
            if (level < 3) {
                Thread.sleep(2000);
                Timber.d("[UNITTESTS][E2E_3LEVELS] Clicking Next Level button");
                try {
                    onView(withId(R.id.next_level_button)).check(matches(isDisplayed())).perform(click());
                    Thread.sleep(500);
                } catch (Exception e) {
                    Timber.e(e, "[E2E_3LEVELS] Could not click Next Level button");
                    fail("Could not click Next Level button after level " + level);
                }
            }
            
            // Reset for next level
            levelCompleted = null;
        }
        
        // Wait for achievements to be processed after Level 3
        Thread.sleep(2000);
        
        Timber.d("[UNITTESTS][E2E_3LEVELS] ===== FINAL ACHIEVEMENT CHECK =====");
        
        // Assertions
        assertTrue("level_1_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        
        Timber.d("[UNITTESTS][E2E_3LEVELS] ✓ Test passed: All 3 levels completed, level_1_complete achievement confirmed unlocked");
    }
}
