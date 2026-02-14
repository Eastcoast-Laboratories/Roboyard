package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
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
import roboyard.ui.achievements.AchievementManager;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GameSolution;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.pm.ia.ricochet.ERRGameMove;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * Debug test for Level 111 to investigate why the solver solution doesn't work.
 * The solution ends with the green robot not at the target.
 * Suspected issue: board dimension not passed correctly to solver, bottom row ignored.
 */
@RunWith(AndroidJUnit4.class)
public class Level111DebugTest {

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
        Timber.d("[LEVEL111_DEBUG] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[LEVEL111_DEBUG] ========== TEST FINISHED ==========");
    }

    @Test
    public void testLevel111_DebugSolverSolution() throws InterruptedException {
        Timber.d("[LEVEL111_DEBUG] Starting Level 111 debug test");
        Thread.sleep(1000);
        
        // Navigate to Level Selection
        try {
            onView(withId(R.id.level_game_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(1000);
        } catch (Exception e) {
            Timber.e(e, "[LEVEL111_DEBUG] Could not click level game button");
            fail("Could not navigate to level selection");
        }
        
        // Unlock all levels: Long press on "Level Selection" title (3 seconds)
        Timber.d("[LEVEL111_DEBUG] Unlocking all levels with long press on title");
        try {
            onView(withId(R.id.level_selection_title)).perform(longClick());
            Thread.sleep(3500); // Wait for 3 second long press
        } catch (Exception e) {
            Timber.w(e, "[LEVEL111_DEBUG] Could not long press title, trying alternative");
        }
        
        // Press back to confirm unlock
        Thread.sleep(500);
        try {
            onView(withId(R.id.back_button)).perform(click());
            Thread.sleep(500);
        } catch (Exception e) {
            Timber.w(e, "[LEVEL111_DEBUG] Could not press back button");
        }
        
        // Navigate back to level selection
        try {
            onView(withId(R.id.level_game_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(1000);
        } catch (Exception e) {
            Timber.e(e, "[LEVEL111_DEBUG] Could not navigate back to level selection");
        }
        
        // Click on level 111
        Timber.d("[LEVEL111_DEBUG] Clicking on Level 111");
        try {
            onView(allOf(withId(R.id.level_button), withText("111"))).check(matches(isDisplayed())).perform(click());
            Thread.sleep(2000);
        } catch (Exception e) {
            Timber.e(e, "[LEVEL111_DEBUG] Could not click level 111 button");
            fail("Could not start level 111");
        }
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Wait for solver to find solution
        Thread.sleep(3000);
        
        // Log board dimensions and solution details
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    Timber.d("[LEVEL111_DEBUG] Board dimensions: %d x %d", state.getWidth(), state.getHeight());
                    Timber.d("[LEVEL111_DEBUG] Level ID: %d", state.getLevelId());
                    
                    // Log robot positions
                    for (GameElement robot : state.getRobots()) {
                        Timber.d("[LEVEL111_DEBUG] Robot color=%d at (%d, %d)", 
                                robot.getColor(), robot.getX(), robot.getY());
                    }
                    
                    // Log target position
                    for (GameElement target : state.getTargets()) {
                        Timber.d("[LEVEL111_DEBUG] Target color=%d at (%d, %d)", 
                                target.getColor(), target.getX(), target.getY());
                    }
                }
                
                GameSolution solution = gameStateManager.getCurrentSolution();
                if (solution != null && solution.getMoves() != null) {
                    Timber.d("[LEVEL111_DEBUG] Solution has %d moves", solution.getMoves().size());
                    for (int i = 0; i < solution.getMoves().size(); i++) {
                        IGameMove move = solution.getMoves().get(i);
                        if (move instanceof RRGameMove) {
                            RRGameMove rrMove = (RRGameMove) move;
                            Timber.d("[LEVEL111_DEBUG] Move %d: Robot %d -> %s", 
                                    i + 1, rrMove.getColor(), rrMove.getMove());
                        }
                    }
                } else {
                    Timber.e("[LEVEL111_DEBUG] No solution found!");
                }
            }
        });
        
        // Execute solution moves and log each step
        Timber.d("[LEVEL111_DEBUG] ===== EXECUTING SOLUTION MOVES =====");
        executeSolutionMovesWithLogging();
        
        // Wait for level completion
        Thread.sleep(2000);
        
        // Check if level is completed
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                levelCompleted = gameStateManager.isGameComplete().getValue();
                Timber.d("[LEVEL111_DEBUG] Level completed: %s", levelCompleted);
                
                // Log final robot positions
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    Timber.d("[LEVEL111_DEBUG] ===== FINAL POSITIONS =====");
                    for (GameElement robot : state.getRobots()) {
                        Timber.d("[LEVEL111_DEBUG] Robot color=%d at (%d, %d)", 
                                robot.getColor(), robot.getX(), robot.getY());
                    }
                    for (GameElement target : state.getTargets()) {
                        Timber.d("[LEVEL111_DEBUG] Target color=%d at (%d, %d)", 
                                target.getColor(), target.getX(), target.getY());
                    }
                }
            }
        });
        
        Thread.sleep(1000);
        
        if (levelCompleted == null || !levelCompleted) {
            Timber.e("[LEVEL111_DEBUG] ⚠️ Level 111 NOT completed - solution is incorrect!");
            Timber.e("[LEVEL111_DEBUG] This confirms the solver issue - likely board dimension problem");
        } else {
            Timber.d("[LEVEL111_DEBUG] ✓ Level 111 completed successfully");
        }
        
        // Don't fail the test - we want to see the logs
        Timber.d("[LEVEL111_DEBUG] Test finished - check logs for analysis");
    }
    
    /**
     * Execute the solution moves with detailed logging
     */
    private void executeSolutionMovesWithLogging() throws InterruptedException {
        final GameSolution[] solutionHolder = new GameSolution[1];
        
        // Get solution from GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                solutionHolder[0] = gameStateManager.getCurrentSolution();
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
            Timber.e("[LEVEL111_DEBUG] Could not get solution after %d retries", retries);
            return;
        }
        
        GameSolution solution = solutionHolder[0];
        ArrayList<IGameMove> moves = solution.getMoves();
        
        Timber.d("[LEVEL111_DEBUG] Executing %d moves", moves.size());
        
        for (int i = 0; i < moves.size(); i++) {
            IGameMove move = moves.get(i);
            if (move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                int robotColor = rrMove.getColor();
                ERRGameMove direction = rrMove.getMove();
                
                // Log before move
                final int moveNum = i + 1;
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        GameState state = gameStateManager.getCurrentState().getValue();
                        if (state != null) {
                            for (GameElement robot : state.getRobots()) {
                                if (robot.getColor() == robotColor) {
                                    Timber.d("[LEVEL111_DEBUG] Move %d: Robot %d at (%d, %d) -> %s", 
                                            moveNum, robotColor, robot.getX(), robot.getY(), direction);
                                    break;
                                }
                            }
                        }
                    }
                });
                
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
                
                // Wait for animation and log after move
                Thread.sleep(500);
                
                final int moveNumAfter = i + 1;
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        GameState state = gameStateManager.getCurrentState().getValue();
                        if (state != null) {
                            for (GameElement robot : state.getRobots()) {
                                if (robot.getColor() == robotColor) {
                                    Timber.d("[LEVEL111_DEBUG] After move %d: Robot %d now at (%d, %d)", 
                                            moveNumAfter, robotColor, robot.getX(), robot.getY());
                                    break;
                                }
                            }
                        }
                    }
                });
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
