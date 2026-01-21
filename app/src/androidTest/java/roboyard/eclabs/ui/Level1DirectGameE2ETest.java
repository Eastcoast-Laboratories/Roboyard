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
import roboyard.eclabs.achievements.AchievementManager;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E tests for Level 1 with proper assertions.
 * 
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.Level1DirectGameE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class Level1DirectGameE2ETest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;
    private volatile Boolean levelCompleted = null;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        
        Timber.d("[E2E] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[E2E] ========== TEST FINISHED ==========");
    }

    /**
     * Helper: Navigate to Level 1 via UI clicks
     */
    private void navigateToLevel1() throws InterruptedException {
        Timber.d("[E2E] Clicking 'Level Game' button");
        onView(withId(R.id.level_game_button))
                .check(matches(isDisplayed()))
                .perform(click());
        
        Thread.sleep(200);
        
        Timber.d("[E2E] Clicking Level 1 button");
        onView(allOf(withId(R.id.level_button), withText("1")))
                .check(matches(isDisplayed()))
                .perform(click());
        
        Thread.sleep(300);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
    }

    /**
     * Test 1: Complete Level 1 slowly (31s) - only level_1_complete achievement
     * Moves: DOWN, UP, RIGHT (takes 31 seconds total)
     */
    @Test
    public void testLevel1SlowCompletion_OnlyLevelAchievement() throws InterruptedException {
        Timber.d("[E2E_SLOW] Starting slow completion test (31s)");
        Thread.sleep(200);
        
        navigateToLevel1();
        
        // Move DOWN (wrong direction first)
        Timber.d("[E2E_SLOW] Moving robot DOWN");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, 1);
            }
        });
        Thread.sleep(2000);
        
        // Wait 25 seconds to exceed 30s threshold
        Timber.d("[E2E_SLOW] Waiting 25 seconds to exceed speedrun threshold...");
        Thread.sleep(25000);
        
        // Move UP
        Timber.d("[E2E_SLOW] Moving robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
            }
        });
        Thread.sleep(2000);
        
        // Move RIGHT to complete
        Timber.d("[E2E_SLOW] Moving robot RIGHT to complete level");
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
        
        Thread.sleep(500);
        
        // Assertions
        assertTrue("Level should be completed", levelCompleted != null && levelCompleted);
        assertTrue("level_1_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        assertFalse("speedrun_under_30s should NOT be unlocked (took >30s)", 
                achievementManager.isUnlocked("speedrun_under_30s"));
        assertFalse("speedrun_under_10s should NOT be unlocked (took >30s)", 
                achievementManager.isUnlocked("speedrun_under_10s"));
        
        Timber.d("[E2E_SLOW] ✓ Test passed: Level completed, only level_1_complete unlocked");
    }

    /**
     * Test 2: Complete Level 1 fast (<10s) - both speedrun achievements
     * Moves: UP, RIGHT (fast)
     */
    @Test
    public void testLevel1FastCompletion_BothSpeedrunAchievements() throws InterruptedException {
        Timber.d("[E2E_FAST] Starting fast completion test (<10s)");
        Thread.sleep(200);
        
        navigateToLevel1();
        
        // Move UP immediately
        Timber.d("[E2E_FAST] Moving robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
            }
        });
        Thread.sleep(2000);
        
        // Move RIGHT to complete
        Timber.d("[E2E_FAST] Moving robot RIGHT to complete level");
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
        
        Thread.sleep(500);
        
        // Assertions
        assertTrue("Level should be completed", levelCompleted != null && levelCompleted);
        assertTrue("level_1_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        assertTrue("speedrun_under_30s should be unlocked (took <10s)", 
                achievementManager.isUnlocked("speedrun_under_30s"));
        assertTrue("speedrun_under_10s should be unlocked (took <10s)", 
                achievementManager.isUnlocked("speedrun_under_10s"));
        
        Timber.d("[E2E_FAST] ✓ Test passed: Level completed, all speedrun achievements unlocked");
    }

    /**
     * Test 3: Wrong moves - verify level is NOT completed (test should PASS)
     * Moves: UP, LEFT (wrong direction)
     */
    @Test
    public void testLevel1WrongMoves_LevelNotCompleted() throws InterruptedException {
        Timber.d("[E2E_WRONG] Starting wrong moves test");
        Thread.sleep(200);
        
        navigateToLevel1();
        
        // Move UP
        Timber.d("[E2E_WRONG] Moving robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
            }
        });
        Thread.sleep(2000);
        
        // Move LEFT (WRONG - should be RIGHT)
        Timber.d("[E2E_WRONG] Moving robot LEFT (wrong direction)");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(-1, 0);
            }
        });
        Thread.sleep(2000);
        
        // Check level completion
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                levelCompleted = gameStateManager.isGameComplete().getValue();
            }
        });
        
        Thread.sleep(500);
        
        // Assertions - level should NOT be completed
        assertFalse("Level should NOT be completed with wrong moves", 
                levelCompleted != null && levelCompleted);
        assertFalse("level_1_complete should NOT be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        
        Timber.d("[E2E_WRONG] ✓ Test passed: Level correctly NOT completed with wrong moves");
    }
}
