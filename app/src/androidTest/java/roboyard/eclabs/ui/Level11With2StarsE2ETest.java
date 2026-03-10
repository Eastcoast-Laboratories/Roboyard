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
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GameSolution;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E test: Complete 11 levels where Level 1 gets only 2 stars (not 3)
 * Verifies that 3_star_10_levels is NOT unlocked after level 10 (since not all levels have 3 stars)
 *
 * Tags: e2e, level-game, achievement, 3-star, negative-test
 */
@RunWith(AndroidJUnit4.class)
public class Level11With2StarsE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;
    private volatile Boolean levelCompleted = null;

    @Before
    public void setUp() throws InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][E2E_2STARS] ========== TEST STARTED ==========");
        
        // Wait for achievement/streak popup to close
        TestHelper.startAndWait8sForPopupClose();
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][E2E_2STARS] ========== TEST FINISHED ==========");
    }

    @Test
    public void testLevel11With2StarsInLevel1_3StarAchievementNotUnlocked() throws InterruptedException {
        Timber.d("[UNITTESTS][E2E_2STARS] Starting test: 11 levels with only 2 stars in level 1");
        
        // Close any remaining popups
        TestHelper.closeAchievementPopupIfPresent();
        
        // Use TestHelper to start level 1
        TestHelper.startLevelGame(activityRule, 1);
        Thread.sleep(2000);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Complete levels 1-11
        for (int level = 1; level <= 11; level++) {
            Timber.d("[UNITTESTS][E2E_2STARS] ===== Starting Level %d =====", level);
            
            // Wait for solver to find solution
            Thread.sleep(3000);
            
            // Close any popups that might appear between levels
            if (level > 1) {
                TestHelper.closeAchievementPopupIfPresent();
            }
            
            // For level 1, make a wrong move first to get only 2 stars
            if (level == 1) {
                Timber.d("[UNITTESTS][E2E_2STARS] Level 1: Making wrong move to get only 2 stars");
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        GameState state = gameStateManager.getCurrentState().getValue();
                        if (state != null) {
                            // Find and select blue robot (color 0)
                            for (GameElement element : state.getRobots()) {
                                if (element.getColor() == 0) {
                                    state.setSelectedRobot(element);
                                    Timber.d("[UNITTESTS][E2E_2STARS] Selected blue robot");
                                    break;
                                }
                            }
                        }
                    }
                });
                
                Thread.sleep(100);
                
                // Move robot DOWN (wrong move to reduce stars)
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        gameStateManager.moveRobotInDirection(0, 1);
                        Timber.d("[UNITTESTS][E2E_2STARS] Moved blue robot DOWN (wrong move)");
                    }
                });
                
                Thread.sleep(2000);
            }
            
            // Execute solution moves via TestHelper
            assertTrue("Solution should be executed for level " + level,
                    TestHelper.executeSolutionMoves(activityRule, gameStateManager, level, "E2E_2STARS"));
            
            // Wait for level completion
            Thread.sleep(2000);
            
            // Check if level is completed and get stars
            final int currentLevel = level;
            final int[] starsEarned = {0};
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    levelCompleted = gameStateManager.isGameComplete().getValue();
                    // Calculate stars
                    int playerMoves = gameStateManager.getMoveCount().getValue() != null ? 
                            gameStateManager.getMoveCount().getValue() : 0;
                    int optimalMoves = 0;
                    GameSolution solution = gameStateManager.getCurrentSolution();
                    if (solution != null && solution.getMoves() != null && solution.getMoves().size() > 0) {
                        optimalMoves = solution.getMoves().size();
                    }
                    int hintsUsed = gameStateManager.getCurrentState().getValue() != null ?
                            gameStateManager.getCurrentState().getValue().getHintCount() : 0;
                    starsEarned[0] = gameStateManager.calculateStars(playerMoves, optimalMoves, hintsUsed);
                    
                    Timber.d("[UNITTESTS][E2E_2STARS] Level %d completed: %s, stars: %d", currentLevel, levelCompleted, starsEarned[0]);
                }
            });
            
            Thread.sleep(500);
            
            if (levelCompleted == null || !levelCompleted) {
                Timber.e("[E2E_2STARS] Level %d NOT completed!", level);
                fail("Level " + level + " should be completed");
            }
            
            // Check achievement status after level 10
            if (level == 10) {
                Thread.sleep(2000);
                boolean threeStar10 = achievementManager.isUnlocked("3_star_10_levels");
                Timber.d("[UNITTESTS][E2E_2STARS] After Level 10: 3_star_10_levels = %s (should be false - only 9 levels with 3 stars)", threeStar10);
                assertFalse("3_star_10_levels should NOT be unlocked yet (only 9 levels with 3 stars)", threeStar10);
            }
            
            // If not the last level, click Next Level button
            if (level < 11) {
                Thread.sleep(3000);
                Timber.d("[UNITTESTS][E2E_2STARS] Clicking Next Level button");
                
                // Close achievement popup before clicking Next Level
                TestHelper.closeAchievementPopupIfPresent();
                Thread.sleep(500);
                
                try {
                    onView(withId(R.id.next_level_button)).check(matches(isDisplayed())).perform(click());
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Timber.e(e, "[E2E_2STARS] Could not click Next Level button");
                    fail("Could not click Next Level button after level " + level);
                }
            }
            
            // Reset for next level
            levelCompleted = null;
        }
        
        // Wait for achievements to be processed
        Thread.sleep(5000);
        
        Timber.d("[UNITTESTS][E2E_2STARS] ===== FINAL CHECK =====");
        
        // Final assertion: 3_star_10_levels SHOULD be unlocked after 11 levels (10 with 3 stars)
        boolean threeStar10Final = achievementManager.isUnlocked("3_star_10_levels");
        Timber.d("[UNITTESTS][E2E_2STARS] Final check: 3_star_10_levels = %s (should be true after 11 levels)", threeStar10Final);
        assertTrue("3_star_10_levels should be unlocked after 11 levels (10 with 3 stars)", threeStar10Final);
        
        Timber.d("[UNITTESTS][E2E_2STARS] ✓ Test passed: 3_star_10_levels correctly unlocked after 11 levels");
    }
}
