package roboyard.eclabs.ui;

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

import roboyard.ui.activities.MainActivity;
import roboyard.ui.achievements.AchievementManager;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GameSolution;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * Debug test for Level 111 to investigate why the solver solution doesn't work.
 * The solution ends with the green robot not at the target.
 * Suspected issue: board dimension not passed correctly to solver, bottom row ignored.
 *
 * Tags: debug, level-game, solver, board-dimensions
 */
@RunWith(AndroidJUnit4.class)
public class Level111DebugTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;
    private volatile Boolean levelCompleted = null;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][LEVEL111_DEBUG] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][LEVEL111_DEBUG] ========== TEST FINISHED ==========");
    }

    @Test
    public void testLevel111_DebugSolverSolution() throws InterruptedException {
        Timber.d("[UNITTESTS][LEVEL111_DEBUG] Starting Level 111 debug test");
        
        // Close achievement popup if present
        TestHelper.closeAchievementPopupIfPresent();
        
        // Start Level 111 programmatically (bypasses need to unlock levels)
        TestHelper.startLevelGame(activityRule, 111);
        
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
                    Timber.d("[UNITTESTS][LEVEL111_DEBUG] Board dimensions: %d x %d", state.getWidth(), state.getHeight());
                    Timber.d("[UNITTESTS][LEVEL111_DEBUG] Level ID: %d", state.getLevelId());
                    
                    // Log robot positions
                    for (GameElement robot : state.getRobots()) {
                        Timber.d("[UNITTESTS][LEVEL111_DEBUG] Robot color=%d at (%d, %d)", 
                                robot.getColor(), robot.getX(), robot.getY());
                    }
                    
                    // Log target position
                    for (GameElement target : state.getTargets()) {
                        Timber.d("[UNITTESTS][LEVEL111_DEBUG] Target color=%d at (%d, %d)", 
                                target.getColor(), target.getX(), target.getY());
                    }
                }
                
                GameSolution solution = gameStateManager.getCurrentSolution();
                if (solution != null && solution.getMoves() != null) {
                    Timber.d("[UNITTESTS][LEVEL111_DEBUG] Solution has %d moves", solution.getMoves().size());
                    for (int i = 0; i < solution.getMoves().size(); i++) {
                        IGameMove move = solution.getMoves().get(i);
                        if (move instanceof RRGameMove) {
                            RRGameMove rrMove = (RRGameMove) move;
                            Timber.d("[UNITTESTS][LEVEL111_DEBUG] Move %d: Robot %d -> %s", 
                                    i + 1, rrMove.getColor(), rrMove.getMove());
                        }
                    }
                } else {
                    Timber.e("[LEVEL111_DEBUG] No solution found!");
                }
            }
        });
        
        // Execute solution moves via TestHelper
        Timber.d("[UNITTESTS][LEVEL111_DEBUG] ===== EXECUTING SOLUTION MOVES =====");
        TestHelper.executeSolutionMoves(activityRule, gameStateManager, 111, "LEVEL111_DEBUG");
        
        // Wait for level completion
        Thread.sleep(2000);
        
        // Check if level is completed
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                levelCompleted = gameStateManager.isGameComplete().getValue();
                Timber.d("[UNITTESTS][LEVEL111_DEBUG] Level completed: %s", levelCompleted);
                
                // Log final robot positions
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    Timber.d("[UNITTESTS][LEVEL111_DEBUG] ===== FINAL POSITIONS =====");
                    for (GameElement robot : state.getRobots()) {
                        Timber.d("[UNITTESTS][LEVEL111_DEBUG] Robot color=%d at (%d, %d)", 
                                robot.getColor(), robot.getX(), robot.getY());
                    }
                    for (GameElement target : state.getTargets()) {
                        Timber.d("[UNITTESTS][LEVEL111_DEBUG] Target color=%d at (%d, %d)", 
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
            Timber.d("[UNITTESTS][LEVEL111_DEBUG] ✓ Level 111 completed successfully");
        }
        
        // Don't fail the test - we want to see the logs
        Timber.d("[UNITTESTS][LEVEL111_DEBUG] Test finished - check logs for analysis");
    }
}
