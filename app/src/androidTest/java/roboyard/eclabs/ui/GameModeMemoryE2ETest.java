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
import roboyard.eclabs.achievements.AchievementManager;
import roboyard.logic.core.GameState;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
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
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.GameModeMemoryE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class GameModeMemoryE2ETest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

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
        Timber.d("[GAME_MODE_E2E] Starting Game Mode Memory E2E test");
        
        // Wait for app to load
        Thread.sleep(3000);
        
        // Get GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
        });
        
        // Step 1: Close streak popup if shown
        closeStreakPopupIfPresent();
        Thread.sleep(1000);
        
        // Step 2: Click on "Level Game" button to go to level selection
        Timber.d("[GAME_MODE_E2E] Step 2: Clicking Level Game button");
        try {
            onView(withId(R.id.level_game_button)).perform(click());
            Thread.sleep(2000);
            Timber.d("[GAME_MODE_E2E] Clicked Level Game button");
        } catch (Exception e) {
            Timber.w(e, "[GAME_MODE_E2E] Could not click Level Game button");
        }
        
        // Step 3: Click on Level 1
        Timber.d("[GAME_MODE_E2E] Step 3: Clicking Level 1");
        try {
            onView(withText("1")).perform(click());
            Thread.sleep(3000);
            Timber.d("[GAME_MODE_E2E] Clicked Level 1");
        } catch (Exception e) {
            Timber.w(e, "[GAME_MODE_E2E] Could not click Level 1, loading directly");
            activityRule.getScenario().onActivity(activity -> {
                gameStateManager.startLevelGame(1);
            });
            Thread.sleep(3000);
        }
        
        // Verify we're in level 1
        activityRule.getScenario().onActivity(activity -> {
            assertTrue("Should be in level game mode", gameStateManager.isInLevelGame());
            assertEquals("Should be in level 1", 1, gameStateManager.getCurrentLevelId());
            Timber.d("[GAME_MODE_E2E] Verified: in level 1");
        });
        
        // Wait for solution to be calculated
        Thread.sleep(3000);
        
        // Step 4: Solve Level 1 using the solution
        Timber.d("[GAME_MODE_E2E] Step 4: Solving Level 1");
        
        // Get and execute the solution
        final GameSolution[] solutionHolder = new GameSolution[1];
        activityRule.getScenario().onActivity(activity -> {
            solutionHolder[0] = gameStateManager.getCurrentSolution();
        });
        
        GameSolution solution = solutionHolder[0];
        if (solution != null && solution.getMoves() != null) {
            Timber.d("[GAME_MODE_E2E] Solution has %d moves", solution.getMoves().size());
            
            for (int i = 0; i < solution.getMoves().size(); i++) {
                IGameMove move = solution.getMoves().get(i);
                Timber.d("[GAME_MODE_E2E] Executing move %d: %s", i + 1, move);
                
                executeMove(move);
                Thread.sleep(1500);
                
                // Check if game is complete
                final boolean[] isComplete = {false};
                activityRule.getScenario().onActivity(activity -> {
                    Boolean complete = gameStateManager.isGameComplete().getValue();
                    isComplete[0] = complete != null && complete;
                });
                
                if (isComplete[0]) {
                    Timber.d("[GAME_MODE_E2E] Level completed after move %d", i + 1);
                    break;
                }
            }
        } else {
            Timber.w("[GAME_MODE_E2E] No solution available, cannot complete test");
            fail("No solution available for Level 1");
        }
        
        // Wait for completion UI
        Thread.sleep(2000);
        
        // Verify level is complete
        activityRule.getScenario().onActivity(activity -> {
            Boolean isComplete = gameStateManager.isGameComplete().getValue();
            assertTrue("Level 1 should be complete", isComplete != null && isComplete);
            Timber.d("[GAME_MODE_E2E] Level 1 completed!");
        });
        
        // Wait for achievement popup
        Thread.sleep(2000);
        
        // Step 5: Click "View achievements" in the popup
        Timber.d("[GAME_MODE_E2E] Step 5: Clicking 'View achievements'");
        try {
            onView(withText("View achievements")).perform(click());
            Thread.sleep(2000);
            Timber.d("[GAME_MODE_E2E] Clicked 'View achievements'");
        } catch (Exception e) {
            Timber.e(e, "[GAME_MODE_E2E] Could not find 'View achievements' button");
            fail("Could not find 'View achievements' button in achievement popup");
        }
        
        // Step 6: Press back button in achievements screen
        Timber.d("[GAME_MODE_E2E] Step 6: Pressing back button");
        try {
            onView(withId(R.id.back_button)).perform(click());
            Thread.sleep(2000);
            Timber.d("[GAME_MODE_E2E] Clicked back button");
        } catch (Exception e) {
            Timber.w(e, "[GAME_MODE_E2E] Could not find back button, using system back");
            pressBack();
            Thread.sleep(2000);
        }
        
        // Step 7: Verify "Next Level" button is VISIBLE
        Timber.d("[GAME_MODE_E2E] Step 7: Verifying 'Next Level' button is visible");
        
        // Verify we're still in level game mode and level is complete
        activityRule.getScenario().onActivity(activity -> {
            assertTrue("Should still be in level game mode", gameStateManager.isInLevelGame());
            assertEquals("Should still be in level 1", 1, gameStateManager.getCurrentLevelId());
            
            Boolean isComplete = gameStateManager.isGameComplete().getValue();
            assertTrue("Level should still be complete (next level button visible)", isComplete != null && isComplete);
            
            Timber.d("[GAME_MODE_E2E] Verified: still in level 1, isComplete=%b", isComplete);
        });
        
        // Verify Next Level button is visible in the UI
        try {
            onView(withId(R.id.next_level_button)).check(matches(isDisplayed()));
            Timber.d("[GAME_MODE_E2E] 'Next Level' button is VISIBLE - TEST PASSED!");
        } catch (Exception e) {
            Timber.e(e, "[GAME_MODE_E2E] 'Next Level' button is NOT visible");
            fail("'Next Level' button should be visible after returning from achievements");
        }
        
        // Step 8: Verify no auto-regeneration happened
        Timber.d("[GAME_MODE_E2E] Step 8: Verifying no auto-regeneration");
        activityRule.getScenario().onActivity(activity -> {
            assertTrue("Should not have regenerated to random game", gameStateManager.isInLevelGame());
            Timber.d("[GAME_MODE_E2E] No auto-regeneration - still in level game mode");
        });
        
        Timber.d("[GAME_MODE_E2E] TEST PASSED: Game mode memory works correctly!");
    }

    /**
     * Execute a single move from the solution.
     */
    private void executeMove(IGameMove move) {
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null && move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                int robotColor = rrMove.getColor();
                int direction = rrMove.getDirection();
                
                Timber.d("[GAME_MODE_E2E] Move: Robot color=%d, direction=%d", robotColor, direction);
                
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    // Find and select the robot
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == 1 && element.getColor() == robotColor) {
                            state.setSelectedRobot(element);
                            Timber.d("[GAME_MODE_E2E] Selected robot color=%d", robotColor);
                            break;
                        }
                    }
                    
                    // Convert direction to dx, dy
                    int dx = 0, dy = 0;
                    switch (direction) {
                        case 1: dy = -1; break; // UP
                        case 2: dx = 1; break;  // RIGHT
                        case 4: dy = 1; break;  // DOWN
                        case 8: dx = -1; break; // LEFT
                    }
                    
                    Timber.d("[GAME_MODE_E2E] Moving robot in direction dx=%d, dy=%d", dx, dy);
                    gameStateManager.moveRobotInDirection(dx, dy);
                }
            }
        });
    }

    /**
     * Close the streak popup if it's shown.
     */
    private void closeStreakPopupIfPresent() {
        Timber.d("[GAME_MODE_E2E] Step 1: Checking for streak popup");
        try {
            Thread.sleep(1000);
            // Click the close button (X) on the streak popup
            onView(withText("✕")).perform(click());
            Timber.d("[GAME_MODE_E2E] Closed streak popup");
        } catch (Exception e) {
            Timber.d("[GAME_MODE_E2E] No streak popup found or already closed");
        }
    }
}
