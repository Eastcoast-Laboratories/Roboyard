package roboyard.eclabs.ui;

import static org.junit.Assert.*;

import android.app.Activity;
import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameSolution;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.GameHistoryManager;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.ui.components.GameStateManager;
import roboyard.ui.components.LevelCompletionManager;

import timber.log.Timber;

/**
 * Test to verify that stars are correctly saved to local history after level completion.
 * Automatically completes Level 1 and verifies the history entry has correct stars and moves.
 *
 * Tags: history-sync, stars, level-game, e2e
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.HistorySyncStarsTest
 */
@RunWith(AndroidJUnit4.class)
public class HistorySyncStarsTest {
    private static final String TAG = "HistorySyncStarsTest";

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule = new ActivityScenarioRule<>(MainFragmentActivity.class);

    private GameStateManager gameStateManager;
    private Activity testActivity;

    @Before
    public void setup() {
        Log.d(TAG, "=== Test started ===");
        activityRule.getScenario().onActivity(activity -> {
            testActivity = activity;
            gameStateManager = new androidx.lifecycle.ViewModelProvider(activity).get(GameStateManager.class);
            gameStateManager.setActivity(activity);
        });
    }

    @Test
    public void testStarsAreSyncedAfterLevelCompletion() throws InterruptedException {
        Log.d(TAG, "Starting automatic Level 1 completion test");

        // Load Level 1 and start solver
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager.loadLevel(1);
            roboyard.ui.util.SolverManager.getInstance().startSolver();
            Timber.d("[SYNC_TEST] Level 1 loaded, solver started");
        });
        Thread.sleep(2000);

        // Wait for solver solution (up to 10 seconds)
        GameSolution solution = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            final GameSolution[] holder = new GameSolution[1];
            activityRule.getScenario().onActivity(activity -> {
                holder[0] = gameStateManager.getCurrentSolution();
            });
            solution = holder[0];
            if (solution != null && solution.getMoves() != null && !solution.getMoves().isEmpty()) {
                Timber.d("[SYNC_TEST] Solution found with %d moves", solution.getMoves().size());
                break;
            }
            Thread.sleep(1000);
        }
        assertNotNull("Solver should find a solution for Level 1", solution);
        assertNotNull("Solution should have moves", solution.getMoves());
        assertFalse("Solution should not be empty", solution.getMoves().isEmpty());

        // Execute solution moves
        for (int i = 0; i < solution.getMoves().size(); i++) {
            IGameMove move = solution.getMoves().get(i);
            Timber.d("[SYNC_TEST] Executing move %d/%d: %s", i + 1, solution.getMoves().size(), move);

            activityRule.getScenario().onActivity(activity -> {
                if (move instanceof RRGameMove) {
                    RRGameMove rrMove = (RRGameMove) move;
                    roboyard.logic.core.GameState state = gameStateManager.getCurrentState().getValue();
                    if (state != null) {
                        GameElement selectedRobot = null;
                        for (GameElement element : state.getGameElements()) {
                            if (element.getType() == GameElement.TYPE_ROBOT && element.getColor() == rrMove.getColor()) {
                                selectedRobot = element;
                                break;
                            }
                        }
                        if (selectedRobot != null) {
                            state.setSelectedRobot(selectedRobot);
                            int dx = 0, dy = 0;
                            switch (rrMove.getDirection()) {
                                case 1: dy = -1; break; // UP
                                case 2: dx = 1; break;  // RIGHT
                                case 4: dy = 1; break;  // DOWN
                                case 8: dx = -1; break; // LEFT
                            }
                            gameStateManager.moveRobotInDirection(dx, dy);
                        }
                    }
                }
            });
            Thread.sleep(1200);

            // Check if game is complete
            Boolean isComplete = gameStateManager.isGameComplete().getValue();
            if (isComplete != null && isComplete) {
                Timber.d("[SYNC_TEST] Level completed after move %d", i + 1);
                break;
            }
        }

        // Wait for completion processing and history save
        Thread.sleep(3000);

        // Verify game is complete
        Boolean isComplete = gameStateManager.isGameComplete().getValue();
        assertTrue("Level 1 should be completed", isComplete != null && isComplete);
        Timber.d("[SYNC_TEST] Level 1 completed!");

        // Verify LevelCompletionData has stars
        final int[] starsHolder = new int[1];
        activityRule.getScenario().onActivity(activity -> {
            LevelCompletionManager lcm = LevelCompletionManager.getInstance(activity);
            starsHolder[0] = lcm.getLevelCompletionData(1).getStars();
            Timber.d("[SYNC_TEST] LevelCompletionData stars for Level 1: %d", starsHolder[0]);
        });
        assertTrue("Level 1 should have at least 1 star", starsHolder[0] > 0);

        // Verify history entry has correct stars and moves
        final int[] histStars = new int[1];
        final int[] histMoves = new int[1];
        final boolean[] histFound = new boolean[1];
        activityRule.getScenario().onActivity(activity -> {
            List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
            for (GameHistoryEntry entry : entries) {
                if ("Level 1".equals(entry.getMapName())) {
                    histFound[0] = true;
                    histStars[0] = entry.getStarsEarned();
                    histMoves[0] = entry.getMovesMade();
                    Timber.d("[SYNC_TEST] History entry 'Level 1': stars=%d, moves=%d", histStars[0], histMoves[0]);
                    break;
                }
            }
        });

        assertTrue("History entry for Level 1 should exist", histFound[0]);
        assertTrue("History entry should have stars > 0", histStars[0] > 0);
        assertTrue("History entry should have moves > 0", histMoves[0] > 0);

        Log.d(TAG, String.format("=== RESULTS: stars=%d, moves=%d ===", histStars[0], histMoves[0]));
        Log.d(TAG, "=== Test completed successfully ===");
    }
}
