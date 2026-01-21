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
 * E2E test: Complete levels 1-10 to unlock 3_star_10_levels achievement
 * Uses the solver solution to execute moves automatically
 */
@RunWith(AndroidJUnit4.class)
public class Level10E2ETest {

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
        Timber.d("[E2E_10LEVELS] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[E2E_10LEVELS] ========== TEST FINISHED ==========");
    }

    @Test
    public void testComplete10Levels_Unlocks3Star10LevelsAchievement() throws InterruptedException {
        Timber.d("[E2E_10LEVELS] Starting 10 levels completion test");
        Thread.sleep(200);
        
        // Navigate to Level 1
        onView(withId(R.id.level_game_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(200);
        onView(allOf(withId(R.id.level_button), withText("1"))).check(matches(isDisplayed())).perform(click());
        Thread.sleep(500);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Complete levels 1-11 (to ensure achievement is unlocked and visible)
        for (int level = 1; level <= 11; level++) {
            Timber.d("[E2E_10LEVELS] ===== Starting Level %d =====", level);
            
            // Wait for solver to find solution
            Thread.sleep(2000);
            
            // Execute solution moves
            executeSolutionMoves(level);
            
            // Wait for level completion
            Thread.sleep(2000);
            
            // Check if level is completed
            final int currentLevel = level;
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    levelCompleted = gameStateManager.isGameComplete().getValue();
                    Timber.d("[E2E_10LEVELS] Level %d completed: %s", currentLevel, levelCompleted);
                }
            });
            
            Thread.sleep(500);
            
            if (levelCompleted == null || !levelCompleted) {
                Timber.e("[E2E_10LEVELS] Level %d NOT completed!", level);
                fail("Level " + level + " should be completed");
            }
            
            // Check achievement status after each level
            Thread.sleep(1000);
            boolean achievement3Star10Unlocked = achievementManager.isUnlocked("3_star_10_levels");
            Timber.d("[E2E_10LEVELS] After Level %d: 3_star_10_levels = %s", level, achievement3Star10Unlocked);
            
            // ASSERTION: Achievement should NOT be unlocked before Level 10
            if (level < 10) {
                assertFalse("3_star_10_levels should NOT be unlocked before Level 10 (currently at Level " + level + ")", 
                        achievement3Star10Unlocked);
            }
            
            // After Level 10, check if 3_star_10_levels achievement is unlocked
            if (level == 10) {
                Timber.d("[E2E_10LEVELS] ===== CHECKING ACHIEVEMENT AFTER LEVEL 10 =====");
                
                // Wait longer for achievement to be processed
                for (int wait = 0; wait < 5; wait++) {
                    Thread.sleep(2000);
                    boolean achievement3Star10Check = achievementManager.isUnlocked("3_star_10_levels");
                    Timber.d("[E2E_10LEVELS] Achievement check %d/5: 3_star_10_levels = %s", wait + 1, achievement3Star10Check);
                    
                    if (achievement3Star10Check) {
                        Timber.d("[E2E_10LEVELS] ✓ VISUAL CONFIRMATION: 3_star_10_levels achievement UNLOCKED after %d seconds!", (wait + 1) * 2);
                        break;
                    }
                }
            }
            
            // If not the last level, click Next Level button
            if (level < 11) {
                Thread.sleep(2000);
                Timber.d("[E2E_10LEVELS] Clicking Next Level button");
                try {
                    onView(withId(R.id.next_level_button)).check(matches(isDisplayed())).perform(click());
                    Thread.sleep(500);
                } catch (Exception e) {
                    Timber.e(e, "[E2E_10LEVELS] Could not click Next Level button");
                    fail("Could not click Next Level button after level " + level);
                }
            }
            
            // Reset for next level
            levelCompleted = null;
        }
        
        // Wait for achievements to be processed after Level 11
        Thread.sleep(5000);
        
        Timber.d("[E2E_10LEVELS] ===== FINAL ACHIEVEMENT CHECK =====");
        
        // Assertions
        assertTrue("level_1_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_1_complete"));
        assertTrue("level_10_complete achievement should be unlocked", 
                achievementManager.isUnlocked("level_10_complete"));
        assertTrue("3_star_10_levels achievement should be unlocked after completing 10 levels with 3 stars", 
                achievementManager.isUnlocked("3_star_10_levels"));
        
        Timber.d("[E2E_10LEVELS] ✓ Test passed: All 11 levels completed, 3_star_10_levels achievement confirmed unlocked");
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
                    Timber.d("[E2E_10LEVELS] Level %d: Found solution with %d moves", 
                            level, solutionHolder[0].getMoves().size());
                } else {
                    Timber.w("[E2E_10LEVELS] Level %d: No solution found yet", level);
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
            Timber.e("[E2E_10LEVELS] Level %d: Could not get solution after %d retries", level, retries);
            fail("Could not get solution for level " + level);
            return;
        }
        
        GameSolution solution = solutionHolder[0];
        ArrayList<IGameMove> moves = solution.getMoves();
        
        Timber.d("[E2E_10LEVELS] Level %d: Executing %d moves", level, moves.size());
        
        for (int i = 0; i < moves.size(); i++) {
            IGameMove move = moves.get(i);
            if (move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                int robotColor = rrMove.getColor();
                ERRGameMove direction = rrMove.getMove();
                
                Timber.d("[E2E_10LEVELS] Move %d: Robot %d -> %s", i + 1, robotColor, direction);
                
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
                                    Timber.d("[E2E_10LEVELS] Selected robot with color %d", color);
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
