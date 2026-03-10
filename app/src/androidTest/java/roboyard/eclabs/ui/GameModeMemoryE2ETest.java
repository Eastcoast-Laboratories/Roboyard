package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
 * E2E test for Game Mode Memory feature.
 * 
 * Test scenario:
 * 1. Close streak popup (if shown)
 * 2. Navigate to level menu
 * 3. Start Level 1
 * 4. Solve Level 1 (move up, then right) - "Next Level" button appears
 * 5. Click "View Achievements" in the achievement popup
 * 6. Press back button
 * 7. Verify: "Next Level" button is VISIBLE (level completion state preserved)
 * 8. Verify: No auto-regeneration (not in random game mode)
 *
 * Tags: e2e, game-mode, level-game, achievements, navigation, next-level
 * 
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.GameModeMemoryE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class GameModeMemoryE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private GameStateManager gameStateManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Clear achievements to ensure fresh state
        AchievementManager.getInstance(context).resetAll();
        
        // Clear game mode memory SharedPreferences
        context.getSharedPreferences("game_mode_memory", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    @After
    public void tearDown() {
        AchievementManager.getInstance(context).resetAll();
    }

    /**
     * Main E2E test for game mode memory.
     * Tests that after viewing achievements and pressing back, the level completion state is preserved.
     */
    @Test
    public void testGameModeMemoryAfterViewingAchievements() throws InterruptedException {
        Timber.d("[UNITTESTS][GAME_MODE_E2E] Starting Game Mode Memory E2E test");
        
        // Wait for app to load
        Thread.sleep(3000);
        
        // Get GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
        });
        
        // Close achievement popup if present
        TestHelper.closeAchievementPopupIfPresent();
        
        // Start Level 1 programmatically
        Timber.d("[UNITTESTS][GAME_MODE_E2E] Step 2+3: Starting Level 1 via TestHelper");
        TestHelper.startLevelGame(activityRule, 1);
        Thread.sleep(1000);
        
        // Verify we're in level 1
        activityRule.getScenario().onActivity(activity -> {
            assertTrue("Should be in level game mode", gameStateManager.isInLevelGame());
            assertEquals("Should be in level 1", 1, gameStateManager.getCurrentLevelId());
            Timber.d("[UNITTESTS][GAME_MODE_E2E] Verified: in level 1");
        });
        
        // Step 4: Solve Level 1 using TestHelper
        Timber.d("[UNITTESTS][GAME_MODE_E2E] Step 4: Solving Level 1");
        boolean completed = TestHelper.completeLevelWithSolver(activityRule, gameStateManager, 1, "GAME_MODE_E2E");
        assertTrue("Level 1 should be completed", completed);
        
        // Wait for completion UI
        Thread.sleep(2000);
        
        // Verify level is complete
        activityRule.getScenario().onActivity(activity -> {
            Boolean isComplete = gameStateManager.isGameComplete().getValue();
            assertTrue("Level 1 should be complete", isComplete != null && isComplete);
            Timber.d("[UNITTESTS][GAME_MODE_E2E] Level 1 completed!");
        });
        
        // Wait for achievement popup
        Thread.sleep(2000);
        
        // Step 5: Click "View achievements" in the popup
        Timber.d("[UNITTESTS][GAME_MODE_E2E] Step 5: Clicking 'View achievements'");
        try {
            onView(withText("View achievements")).perform(click());
            Thread.sleep(2000);
            Timber.d("[UNITTESTS][GAME_MODE_E2E] Clicked 'View achievements'");
        } catch (Exception e) {
            Timber.e(e, "[GAME_MODE_E2E] Could not find 'View achievements' button");
            fail("Could not find 'View achievements' button in achievement popup");
        }
        
        // Step 6: Press back button in achievements screen
        Timber.d("[UNITTESTS][GAME_MODE_E2E] Step 6: Pressing back button");
        try {
            onView(withId(R.id.back_button)).perform(click());
            Thread.sleep(2000);
            Timber.d("[UNITTESTS][GAME_MODE_E2E] Clicked back button");
        } catch (Exception e) {
            Timber.w(e, "[GAME_MODE_E2E] Could not find back button, using system back");
            pressBack();
            Thread.sleep(2000);
        }
        
        // Step 7: Verify "Next Level" button is VISIBLE
        Timber.d("[UNITTESTS][GAME_MODE_E2E] Step 7: Verifying 'Next Level' button is visible");
        
        // Verify we're still in level game mode and level is complete
        activityRule.getScenario().onActivity(activity -> {
            assertTrue("Should still be in level game mode", gameStateManager.isInLevelGame());
            assertEquals("Should still be in level 1", 1, gameStateManager.getCurrentLevelId());
            
            Boolean isComplete = gameStateManager.isGameComplete().getValue();
            assertTrue("Level should still be complete (next level button visible)", isComplete != null && isComplete);
            
            Timber.d("[UNITTESTS][GAME_MODE_E2E] Verified: still in level 1, isComplete=%b", isComplete);
        });
        
        // Verify Next Level button is visible in the UI
        try {
            onView(withId(R.id.next_level_button)).check(matches(isDisplayed()));
            Timber.d("[UNITTESTS][GAME_MODE_E2E] 'Next Level' button is VISIBLE - TEST PASSED!");
        } catch (Exception e) {
            Timber.e(e, "[GAME_MODE_E2E] 'Next Level' button is NOT visible");
            fail("'Next Level' button should be visible after returning from achievements");
        }
        
        // Step 8: Verify no auto-regeneration happened
        Timber.d("[UNITTESTS][GAME_MODE_E2E] Step 8: Verifying no auto-regeneration");
        activityRule.getScenario().onActivity(activity -> {
            assertTrue("Should not have regenerated to random game", gameStateManager.isInLevelGame());
            Timber.d("[UNITTESTS][GAME_MODE_E2E] No auto-regeneration - still in level game mode");
        });
        
        Timber.d("[UNITTESTS][GAME_MODE_E2E] TEST PASSED: Game mode memory works correctly!");
    }

}
