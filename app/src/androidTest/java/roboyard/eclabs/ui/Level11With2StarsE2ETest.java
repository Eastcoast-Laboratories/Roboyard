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

import java.util.ArrayList;

import roboyard.eclabs.R;
import roboyard.eclabs.achievements.AchievementManager;
import roboyard.eclabs.ui.GameElement;
import roboyard.logic.core.GameState;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.pm.ia.ricochet.ERRGameMove;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E test: Complete 11 levels where Level 1 gets only 2 stars (not 3)
 * Verifies that 3_star_10_levels is NOT unlocked after level 10 (since not all levels have 3 stars)
 */
@RunWith(AndroidJUnit4.class)
public class Level11With2StarsE2ETest {

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
        Timber.d("[E2E_2STARS] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[E2E_2STARS] ========== TEST FINISHED ==========");
    }

    @Test
    public void testLevel11With2StarsInLevel1_3StarAchievementNotUnlocked() throws InterruptedException {
        Timber.d("[E2E_2STARS] Starting test: 11 levels with only 2 stars in level 1");
        Thread.sleep(200);
        
        // Navigate to Level 1
        onView(withId(R.id.level_game_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(200);
        onView(allOf(withId(R.id.level_button), withText("1"))).check(matches(isDisplayed())).perform(click());
        Thread.sleep(500);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Complete levels 1-11
        for (int level = 1; level <= 11; level++) {
            Timber.d("[E2E_2STARS] ===== Starting Level %d =====", level);
            
            // Wait for solver to find solution
            Thread.sleep(2000);
            
            // For level 1, make a wrong move first to get only 2 stars
            if (level == 1) {
                Timber.d("[E2E_2STARS] Level 1: Making wrong move to get only 2 stars");
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        GameState state = gameStateManager.getCurrentState().getValue();
                        if (state != null) {
                            // Find and select blue robot (color 0)
                            for (GameElement element : state.getRobots()) {
                                if (element.getColor() == 0) {
                                    state.setSelectedRobot(element);
                                    Timber.d("[E2E_2STARS] Selected blue robot");
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
                        Timber.d("[E2E_2STARS] Moved blue robot DOWN (wrong move)");
                    }
                });
                
                Thread.sleep(2000);
            }
            
            // Execute solution moves
            executeSolutionMoves(level);
            
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
                    
                    Timber.d("[E2E_2STARS] Level %d completed: %s, stars: %d", currentLevel, levelCompleted, starsEarned[0]);
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
                Timber.d("[E2E_2STARS] After Level 10: 3_star_10_levels = %s (should be false)", threeStar10);
                assertFalse("3_star_10_levels should NOT be unlocked (level 1 has only 2 stars)", threeStar10);
            }
            
            // If not the last level, click Next Level button
            if (level < 11) {
                Thread.sleep(2000);
                Timber.d("[E2E_2STARS] Clicking Next Level button");
                try {
                    onView(withId(R.id.next_level_button)).check(matches(isDisplayed())).perform(click());
                    Thread.sleep(500);
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
        
        Timber.d("[E2E_2STARS] ===== FINAL CHECK =====");
        
        // Final assertion: 3_star_10_levels should NOT be unlocked
        boolean threeStar10Final = achievementManager.isUnlocked("3_star_10_levels");
        Timber.d("[E2E_2STARS] Final check: 3_star_10_levels = %s (should be false)", threeStar10Final);
        assertFalse("3_star_10_levels should NOT be unlocked when not all 10 levels have 3 stars", threeStar10Final);
        
        Timber.d("[E2E_2STARS] ✓ Test passed: 3_star_10_levels correctly NOT unlocked");
    }
    
    /**
     * Execute the solution moves for the current level
     */
    private void executeSolutionMoves(int level) throws InterruptedException {
        final GameSolution[] solutionHolder = new GameSolution[1];
        
        // Get solution from GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                solutionHolder[0] = gameStateManager.getCurrentSolution();
                if (solutionHolder[0] != null) {
                    Timber.d("[E2E_2STARS] Level %d: Found solution with %d moves", 
                            level, solutionHolder[0].getMoves().size());
                } else {
                    Timber.w("[E2E_2STARS] Level %d: No solution found yet", level);
                }
            }
        });
        
        // Wait for solution if not available
        int retries = 0;
        while (solutionHolder[0] == null && retries < 10) {
            Thread.sleep(500);
            retries++;
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    solutionHolder[0] = gameStateManager.getCurrentSolution();
                }
            });
        }
        
        if (solutionHolder[0] == null) {
            Timber.e("[E2E_2STARS] Level %d: Could not get solution after %d retries", level, retries);
            fail("Could not get solution for level " + level);
            return;
        }
        
        GameSolution solution = solutionHolder[0];
        ArrayList<IGameMove> moves = solution.getMoves();
        
        Timber.d("[E2E_2STARS] Level %d: Executing %d moves", level, moves.size());
        
        for (int i = 0; i < moves.size(); i++) {
            IGameMove move = moves.get(i);
            if (move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                int robotColor = rrMove.getColor();
                ERRGameMove direction = rrMove.getMove();
                
                // Select robot and move
                final int dx = getDirectionX(direction);
                final int dy = getDirectionY(direction);
                final int color = robotColor;
                
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        GameState state = gameStateManager.getCurrentState().getValue();
                        if (state != null) {
                            // Find and select robot by color
                            for (GameElement element : state.getRobots()) {
                                if (element.getColor() == color) {
                                    state.setSelectedRobot(element);
                                    break;
                                }
                            }
                        }
                    }
                });
                
                Thread.sleep(100);
                
                // Move robot
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        gameStateManager.moveRobotInDirection(dx, dy);
                    }
                });
                
                // Wait for animation
                Thread.sleep(500);
            }
        }
    }
    
    /**
     * Get X direction from ERRGameMove
     */
    private int getDirectionX(ERRGameMove direction) {
        switch (direction) {
            case LEFT: return -1;
            case RIGHT: return 1;
            default: return 0;
        }
    }
    
    /**
     * Get Y direction from ERRGameMove
     */
    private int getDirectionY(ERRGameMove direction) {
        switch (direction) {
            case UP: return -1;
            case DOWN: return 1;
            default: return 0;
        }
    }
}
