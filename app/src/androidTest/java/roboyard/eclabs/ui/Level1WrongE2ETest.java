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
 * E2E test: Wrong moves - verify level is NOT completed (test should PASS)
 */
@RunWith(AndroidJUnit4.class)
public class Level1WrongE2ETest {

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
        Timber.d("[E2E_WRONG] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[E2E_WRONG] ========== TEST FINISHED ==========");
    }

    @Test
    public void testLevel1WrongMoves_LevelNotCompleted() throws InterruptedException {
        Timber.d("[E2E_WRONG] Starting wrong moves test");
        Thread.sleep(200);
        
        // Navigate to Level 1
        onView(withId(R.id.level_game_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(200);
        onView(allOf(withId(R.id.level_button), withText("1"))).check(matches(isDisplayed())).perform(click());
        Thread.sleep(300);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
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
        
        Thread.sleep(3000);
        
        // Assertions - level should NOT be completed
        assertFalse("Level should NOT be completed with wrong moves", 
                levelCompleted != null && levelCompleted);
        assertFalse("level_1_complete should NOT be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        
        Timber.d("[E2E_WRONG] ✓ Test passed: Level correctly NOT completed with wrong moves");
    }
}
