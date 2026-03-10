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
 * E2E test: Complete Level 1 slowly (>30s) - only level_1_complete achievement
 *
 * Tags: e2e, level-game, achievement, slow, timer
 */
@RunWith(AndroidJUnit4.class)
public class Level1SlowE2ETest {

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
        Timber.d("[UNITTESTS][E2E_SLOW] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][E2E_SLOW] ========== TEST FINISHED ==========");
    }

    @Test
    public void testLevel1SlowCompletion_OnlyLevelAchievement() throws InterruptedException {
        Timber.d("[UNITTESTS][E2E_SLOW] Starting slow completion test (>30s)");
        
        // Close achievement popup if present
        TestHelper.closeAchievementPopupIfPresent();
        
        // Start Level 1 programmatically
        TestHelper.startLevelGame(activityRule, 1);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Move DOWN (wrong direction first)
        Timber.d("[UNITTESTS][E2E_SLOW] Moving robot DOWN");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, 1);
            }
        });
        Thread.sleep(2000);
        
        // Wait 28 seconds to exceed 30s threshold
        Timber.d("[UNITTESTS][E2E_SLOW] Waiting 28 seconds to exceed speedrun threshold...");
        Thread.sleep(28000);
        
        // Move UP
        Timber.d("[UNITTESTS][E2E_SLOW] Moving robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
            }
        });
        Thread.sleep(2000);
        
        // Move RIGHT to complete
        Timber.d("[UNITTESTS][E2E_SLOW] Moving robot RIGHT to complete level");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(1, 0);
            }
        });
        Thread.sleep(2000);
        
        // Check level completion
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                levelCompleted = gameStateManager.isGameComplete().getValue();
            }
        });
        
        Thread.sleep(3000);
        
        // Assertions
        assertTrue("Level should be completed", levelCompleted != null && levelCompleted);
        assertTrue("level_1_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        assertFalse("speedrun_under_30s should NOT be unlocked (took >30s)", 
                achievementManager.isUnlocked("speedrun_under_30s"));
        assertFalse("speedrun_under_10s should NOT be unlocked (took >30s)", 
                achievementManager.isUnlocked("speedrun_under_10s"));
        
        Timber.d("[UNITTESTS][E2E_SLOW] ✓ Test passed: Level completed, only level_1_complete unlocked");
    }
}
