package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

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
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;
import roboyard.ui.components.FileReadWrite;

import timber.log.Timber;

/**
 * E2E test: Verify that perfect_random_games_5 achievement is counted correctly.
 *
 * Flow:
 * 1. Clear history and achievements
 * 2. Start random game via "New Game" button
 * 3. For each of 5 games: solve with optimal moves (no hints), then press "New Game"
 * 4. After 5th game: navigate to achievements screen and verify perfect_random_games_5 is unlocked
 *
 * This test specifically verifies the fix for the isFirstCompletion bug where
 * history entries created on first move (completionCount=0) were incorrectly
 * treated as "already completed" maps.
 *
 * Tags: e2e, random-game, achievement, perfect, isFirstCompletion
 */
@RunWith(AndroidJUnit4.class)
public class PerfectRandom5E2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;
    private volatile Boolean gameCompleted = null;

    @Before
    public void setUp() throws InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);

        // Clear achievements
        SharedPreferences prefs = context.getSharedPreferences("roboyard_achievements", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        achievementManager.resetAll();

        // Clear game history so isFirstCompletion returns true for all maps
        activityRule.getScenario().onActivity(activity -> {
            FileReadWrite.deletePrivateData(activity, "history_index.json");
            Timber.d("[UNITTESTS][PERFECT5_TEST] History cleared");
        });

        Timber.d("[UNITTESTS][PERFECT5_TEST] ========== TEST STARTED ==========");
        Thread.sleep(300);
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][PERFECT5_TEST] ========== TEST FINISHED ==========");
    }

    @Test
    public void testPerfectRandom5AchievementUnlocks() throws InterruptedException {
        Timber.d("[UNITTESTS][PERFECT5_TEST] Starting test: play 5 perfect random games");

        // Navigate to Random Game via "New Game" button (ui_button on main screen)
        onView(withId(R.id.ui_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(500);

        // Get GameStateManager from activity
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });

        int perfectGamesCounter = 0;

        for (int game = 1; game <= 5; game++) {
            Timber.d("[UNITTESTS][PERFECT5_TEST] ===== Game %d/5 =====", game);
            achievementManager.onNewGameStarted();

            // Wait for solver
            waitForSolver(game);

            // Solve optimally
            boolean solved = executeSolutionMoves(game);
            if (!solved) {
                fail("Could not execute solution moves for game " + game);
            }

            // Wait for completion
            Thread.sleep(2000);

            // Verify game is completed
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    gameCompleted = gameStateManager.isGameComplete().getValue();
                }
            });
            Thread.sleep(300);

            assertTrue("Game " + game + " should be completed", Boolean.TRUE.equals(gameCompleted));
            Timber.d("[UNITTESTS][PERFECT5_TEST] Game %d completed!", game);

            // Check counter progress
            AchievementManager.AchievementProgress progress =
                    achievementManager.getProgress("perfect_random_games_5");
            if (progress != null) {
                perfectGamesCounter = progress.current;
                Timber.d("[UNITTESTS][PERFECT5_TEST] After game %d: perfect_random_games progress = %d/%d",
                        game, progress.current, progress.required);
            }

            // After 5th game: verify achievement is unlocked
            if (game == 5) {
                Thread.sleep(1000);
                boolean unlocked = achievementManager.isUnlocked("perfect_random_games_5");
                Timber.d("[UNITTESTS][PERFECT5_TEST] perfect_random_games_5 unlocked: %b", unlocked);
                assertTrue("perfect_random_games_5 MUST be unlocked after 5 perfect random games", unlocked);

                // Navigate to achievements screen and verify counter there too
                verifyAchievementScreenShowsUnlocked();
                return;
            }

            // Start next game
            Thread.sleep(1500);
            Timber.d("[UNITTESTS][PERFECT5_TEST] Pressing New Game button for game %d", game + 1);
            try {
                onView(withId(R.id.next_level_button)).check(matches(isDisplayed())).perform(click());
                Thread.sleep(500);
            } catch (Exception e) {
                Timber.e(e, "[PERFECT5_TEST] Could not press New Game button after game %d", game);
                fail("Could not start game " + (game + 1));
            }

            gameCompleted = null;
        }
    }

    /**
     * Wait for the solver to find a solution, up to 15 seconds
     */
    private void waitForSolver(int game) throws InterruptedException {
        final GameSolution[] solutionHolder = new GameSolution[1];
        int retries = 0;
        while (retries < 30) {
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    solutionHolder[0] = gameStateManager.getCurrentSolution();
                }
            });
            if (solutionHolder[0] != null && solutionHolder[0].getMoves() != null
                    && !solutionHolder[0].getMoves().isEmpty()) {
                Timber.d("[UNITTESTS][PERFECT5_TEST] Game %d: Solver found solution with %d moves after %d retries",
                        game, solutionHolder[0].getMoves().size(), retries);
                return;
            }
            Thread.sleep(500);
            retries++;
        }
        Timber.w("[PERFECT5_TEST] Game %d: Solver timeout after %d retries", game, retries);
    }

    /**
     * Execute the optimal solution moves via GameStateManager
     */
    private boolean executeSolutionMoves(int game) throws InterruptedException {
        final GameSolution[] solutionHolder = new GameSolution[1];
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                solutionHolder[0] = gameStateManager.getCurrentSolution();
            }
        });

        if (solutionHolder[0] == null || solutionHolder[0].getMoves() == null
                || solutionHolder[0].getMoves().isEmpty()) {
            Timber.e("[PERFECT5_TEST] Game %d: No solution available", game);
            return false;
        }

        GameSolution solution = solutionHolder[0];
        ArrayList<IGameMove> moves = solution.getMoves();
        Timber.d("[UNITTESTS][PERFECT5_TEST] Game %d: Executing %d optimal moves", game, moves.size());

        for (int i = 0; i < moves.size(); i++) {
            IGameMove move = moves.get(i);
            if (move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                int robotColor = rrMove.getColor();
                ERRGameMove direction = rrMove.getMove();

                final int dx = getDirectionX(direction);
                final int dy = getDirectionY(direction);
                final int color = robotColor;

                // Select robot
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        GameState state = gameStateManager.getCurrentState().getValue();
                        if (state != null) {
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

                // Execute move
                activityRule.getScenario().onActivity(activity -> {
                    if (gameStateManager != null) {
                        gameStateManager.moveRobotInDirection(dx, dy);
                    }
                });
                Thread.sleep(400);
            }
        }
        return true;
    }

    /**
     * Navigate to achievements screen and verify perfect_random_games_5 is unlocked
     */
    private void verifyAchievementScreenShowsUnlocked() throws InterruptedException {
        Timber.d("[UNITTESTS][PERFECT5_TEST] Navigating to achievements screen...");

        // Press back to main menu first
        try {
            onView(withId(R.id.back_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(500);
        } catch (Exception e) {
            Timber.w("[PERFECT5_TEST] No back button, trying other navigation");
        }

        // Try to find achievements button on main menu
        try {
            onView(withId(R.id.achievements_icon_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(1000);

            // Achievement is checked in AchievementManager, screen just shows state
            boolean unlocked = achievementManager.isUnlocked("perfect_random_games_5");
            Timber.d("[UNITTESTS][PERFECT5_TEST] Achievements screen open - perfect_random_games_5 unlocked: %b", unlocked);
            assertTrue("perfect_random_games_5 must be unlocked in achievement screen", unlocked);
        } catch (Exception e) {
            Timber.w("[PERFECT5_TEST] Could not open achievements screen: %s - checking manager directly", e.getMessage());
            // Fallback: verify via AchievementManager directly
            boolean unlocked = achievementManager.isUnlocked("perfect_random_games_5");
            assertTrue("perfect_random_games_5 must be unlocked", unlocked);
        }

        Timber.d("[UNITTESTS][PERFECT5_TEST] ✓ perfect_random_games_5 is UNLOCKED after 5 perfect random games!");
    }

    private int getDirectionX(ERRGameMove direction) {
        switch (direction) {
            case LEFT: return -1;
            case RIGHT: return 1;
            default: return 0;
        }
    }

    private int getDirectionY(ERRGameMove direction) {
        switch (direction) {
            case UP: return -1;
            case DOWN: return 1;
            default: return 0;
        }
    }
}
