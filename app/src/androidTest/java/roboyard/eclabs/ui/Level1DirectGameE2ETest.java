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
 * Simple E2E test that:
 * 1. Clicks "Level Game" button
 * 2. Clicks Level 1 button
 * 3. Moves blue robot UP and RIGHT
 * 
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.Level1DirectGameE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class Level1DirectGameE2ETest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Context context;
    private GameStateManager gameStateManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AchievementManager.getInstance(context).resetAll();
        
        Timber.d("[E2E_SIMPLE] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        AchievementManager.getInstance(context).resetAll();
        Timber.d("[E2E_SIMPLE] ========== TEST FINISHED ==========");
    }

    /**
     * Simple test: Click Level Game, Click Level 1, Move robot UP and RIGHT
     */
    @Test
    public void testLevel1SimpleNavigation() throws InterruptedException {
        Timber.d("[E2E_SIMPLE] STEP 1: App starting - waiting for main menu");
        Thread.sleep(200);
        
        Timber.d("[E2E_SIMPLE] STEP 2: Clicking 'Level Game' button");
        try {
            onView(withId(R.id.level_game_button))
                    .check(matches(isDisplayed()))
                    .perform(click());
            Timber.d("[E2E_SIMPLE] ✓ Clicked Level Game button");
        } catch (Exception e) {
            Timber.e(e, "[E2E_SIMPLE] ERROR: Could not find level_game_button");
            throw e;
        }
        
        Thread.sleep(200);
        
        Timber.d("[E2E_SIMPLE] STEP 3: Clicking Level 1 button");
        try {
            onView(allOf(withId(R.id.level_button), withText("1")))
                    .check(matches(isDisplayed()))
                    .perform(click());
            Timber.d("[E2E_SIMPLE] ✓ Clicked Level 1 button");
        } catch (Exception e) {
            Timber.e(e, "[E2E_SIMPLE] ERROR: Could not find level_button with text '1'");
            throw e;
        }
        
        Thread.sleep(300);
        
        Timber.d("[E2E_SIMPLE] STEP 4: Getting GameStateManager");
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
            Timber.d("[E2E_SIMPLE] ✓ GameStateManager obtained");
        });
        
        Timber.d("[E2E_SIMPLE] STEP 5: Moving blue robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
                Timber.d("[E2E_SIMPLE] ✓ Robot moved UP");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_SIMPLE] STEP 6: Moving blue robot RIGHT");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(1, 0);
                Timber.d("[E2E_SIMPLE] ✓ Robot moved RIGHT");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_SIMPLE] ========== TEST COMPLETE ==========");
    }

    /**
     * Test with WRONG moves: UP and LEFT - should FAIL to demonstrate assertion works
     */
    @Test
    public void testLevel1WrongSolution() throws InterruptedException {
        Timber.d("[E2E_WRONG] STEP 1: App starting - waiting for main menu");
        Thread.sleep(200);
        
        Timber.d("[E2E_WRONG] STEP 2: Clicking 'Level Game' button");
        try {
            onView(withId(R.id.level_game_button))
                    .check(matches(isDisplayed()))
                    .perform(click());
            Timber.d("[E2E_WRONG] ✓ Clicked Level Game button");
        } catch (Exception e) {
            Timber.e(e, "[E2E_WRONG] ERROR: Could not find level_game_button");
            throw e;
        }
        
        Thread.sleep(200);
        
        Timber.d("[E2E_WRONG] STEP 3: Clicking Level 1 button");
        try {
            onView(allOf(withId(R.id.level_button), withText("1")))
                    .check(matches(isDisplayed()))
                    .perform(click());
            Timber.d("[E2E_WRONG] ✓ Clicked Level 1 button");
        } catch (Exception e) {
            Timber.e(e, "[E2E_WRONG] ERROR: Could not find level_button with text '1'");
            throw e;
        }
        
        Thread.sleep(300);
        
        Timber.d("[E2E_WRONG] STEP 4: Getting GameStateManager");
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
            Timber.d("[E2E_WRONG] ✓ GameStateManager obtained");
        });
        
        Timber.d("[E2E_WRONG] STEP 5: Moving blue robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
                Timber.d("[E2E_WRONG] ✓ Robot moved UP");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_WRONG] STEP 6: Moving blue robot LEFT (WRONG - should be RIGHT)");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(-1, 0);
                Timber.d("[E2E_WRONG] ✓ Robot moved LEFT (WRONG DIRECTION)");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_WRONG] STEP 7: Checking if level is completed");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                Boolean isComplete = gameStateManager.isGameComplete().getValue();
                Timber.d("[E2E_WRONG] Game complete: %s", isComplete);
                
                if (isComplete != null && isComplete) {
                    Timber.d("[E2E_WRONG] ✗ LEVEL COMPLETED - This should NOT happen!");
                } else {
                    Timber.d("[E2E_WRONG] ✓ LEVEL NOT COMPLETED - This is EXPECTED with LEFT move!");
                }
                
                // This assertion should FAIL because LEFT is wrong
                assertTrue("Level should be completed - but LEFT move is wrong, so this FAILS", 
                        isComplete != null && isComplete);
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_WRONG] ========== TEST COMPLETE ==========");
    }
}
