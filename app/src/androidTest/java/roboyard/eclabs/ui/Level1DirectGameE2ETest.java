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
     * Test with CORRECT moves: UP and RIGHT - should PASS
     */
    @Test
    public void testLevel1CorrectSolution() throws InterruptedException {
        Timber.d("[E2E_SIMPLE] STEP 1: App starting");
        Thread.sleep(2000);
        
        Timber.d("[E2E_SIMPLE] STEP 2: Getting GameStateManager");
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
            Timber.d("[E2E_SIMPLE] ✓ GameStateManager obtained");
        });
        
        Timber.d("[E2E_SIMPLE] STEP 3: Loading Level 1");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.startNewGame();
                gameStateManager.loadLevel(1);
                Timber.d("[E2E_SIMPLE] ✓ Level 1 loaded");
            }
        });
        
        Thread.sleep(3000);
        
        Timber.d("[E2E_SIMPLE] STEP 4: Moving blue robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
                Timber.d("[E2E_SIMPLE] ✓ Robot moved UP");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_SIMPLE] STEP 5: Moving blue robot RIGHT (CORRECT)");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(1, 0);
                Timber.d("[E2E_SIMPLE] ✓ Robot moved RIGHT");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_SIMPLE] STEP 6: Checking if level is completed");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                Boolean isComplete = gameStateManager.isGameComplete().getValue();
                Timber.d("[E2E_SIMPLE] Game complete: %s", isComplete);
                
                if (isComplete != null && isComplete) {
                    Timber.d("[E2E_SIMPLE] ✓ LEVEL COMPLETED - Robot reached the goal!");
                } else {
                    Timber.d("[E2E_SIMPLE] ✗ LEVEL NOT COMPLETED - Robot did not reach the goal!");
                }
                
                // Assert that the level is completed
                assertTrue("Level should be completed - robot must reach the goal", 
                        isComplete != null && isComplete);
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_SIMPLE] ========== TEST COMPLETE - SUCCESS ==========");
    }

    /**
     * Test with WRONG moves: UP and LEFT - should FAIL
     */
    @Test
    public void testLevel1WrongSolution() throws InterruptedException {
        Timber.d("[E2E_WRONG] STEP 1: App starting");
        Thread.sleep(2000);
        
        Timber.d("[E2E_WRONG] STEP 2: Getting GameStateManager");
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
            Timber.d("[E2E_WRONG] ✓ GameStateManager obtained");
        });
        
        Timber.d("[E2E_WRONG] STEP 3: Loading Level 1");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.startNewGame();
                gameStateManager.loadLevel(1);
                Timber.d("[E2E_WRONG] ✓ Level 1 loaded");
            }
        });
        
        Thread.sleep(3000);
        
        Timber.d("[E2E_WRONG] STEP 4: Moving blue robot UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
                Timber.d("[E2E_WRONG] ✓ Robot moved UP");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_WRONG] STEP 5: Moving blue robot LEFT (WRONG - should be RIGHT)");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(-1, 0);
                Timber.d("[E2E_WRONG] ✓ Robot moved LEFT (WRONG DIRECTION)");
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_WRONG] STEP 6: Checking if level is completed");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                Boolean isComplete = gameStateManager.isGameComplete().getValue();
                Timber.d("[E2E_WRONG] Game complete: %s", isComplete);
                
                if (isComplete != null && isComplete) {
                    Timber.d("[E2E_WRONG] ✓ LEVEL COMPLETED - Robot reached the goal!");
                } else {
                    Timber.d("[E2E_WRONG] ✗ LEVEL NOT COMPLETED - Robot did not reach the goal!");
                }
                
                // Assert that the level is completed - this should FAIL because we moved LEFT
                assertTrue("Level should be completed - robot must reach the goal (this will FAIL with LEFT move)", 
                        isComplete != null && isComplete);
            }
        });
        
        Thread.sleep(2000);
        
        Timber.d("[E2E_WRONG] ========== TEST COMPLETE ==========");
    }
}
