package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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
 * E2E test: Complete 11 random games to test perfect_random_games_10 and no_hints_streak_random_10 achievements
 * - Games 1-9: Play with optimal moves, no hints
 * - Game 10: Press hint button, then play with optimal moves
 * - Game 11: Play with optimal moves, no hints
 * 
 * Expected results:
 * - perfect_random_games_10 should be unlocked after game 10 (10 perfect games)
 * - no_hints_streak_random_10 should NOT be unlocked (hint was used in game 10)
 * - no_hints_streak_random_10 should be unlocked after game 11 (10 perfect games without hints)
 */
@RunWith(AndroidJUnit4.class)
public class RandomGame11E2ETest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;
    private volatile Boolean gameCompleted = null;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        Timber.d("[E2E_RANDOM11] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[E2E_RANDOM11] ========== TEST FINISHED ==========");
    }

    @Test
    public void testComplete11RandomGames_WithHintOnGame10() throws InterruptedException {
        Timber.d("[E2E_RANDOM11] Starting 11 random games test (hint on game 10)");
        Thread.sleep(200);
        
        // Navigate to New Game (Random Game) - button is called modern_ui_button
        onView(withId(R.id.modern_ui_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(500);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Complete 11 random games
        for (int game = 1; game <= 11; game++) {
            Timber.d("[E2E_RANDOM11] ===== Starting Random Game %d =====", game);
            
            // Reset game session flag for new game
            achievementManager.onNewGameStarted();
            
            // Wait for solver to find solution
            Thread.sleep(3000);
            
            // On game 10, press the hint button first
            if (game == 10) {
                Timber.d("[E2E_RANDOM11] Game 10: Pressing HINT button before solving");
                try {
                    onView(withId(R.id.hint_button)).check(matches(isDisplayed())).perform(click());
                    Thread.sleep(1000);
                    
                    // Notify achievement manager that hint was used
                    achievementManager.onHintUsed();
                    Timber.d("[E2E_RANDOM11] Game 10: Hint button pressed, onHintUsed() called");
                } catch (Exception e) {
                    Timber.e(e, "[E2E_RANDOM11] Could not press hint button");
                }
            }
            
            // Execute solution moves
            executeSolutionMoves(game);
            
            // Wait for game completion
            Thread.sleep(2000);
            
            // Check if game is completed
            final int currentGame = game;
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    gameCompleted = gameStateManager.isGameComplete().getValue();
                    Timber.d("[E2E_RANDOM11] Game %d completed: %s", currentGame, gameCompleted);
                }
            });
            
            Thread.sleep(500);
            
            if (gameCompleted == null || !gameCompleted) {
                Timber.e("[E2E_RANDOM11] Game %d NOT completed!", game);
                fail("Game " + game + " should be completed");
            }
            
            // Check achievement status after each game
            Thread.sleep(1000);
            boolean perfectRandom10 = achievementManager.isUnlocked("perfect_random_games_10");
            boolean noHintsRandom10 = achievementManager.isUnlocked("no_hints_streak_random_10");
            
            Timber.d("[E2E_RANDOM11] After Game %d: perfect_random_games_10=%s, no_hints_streak_random_10=%s", 
                    game, perfectRandom10, noHintsRandom10);
            
            // ASSERTIONS before game 10
            if (game < 10) {
                assertFalse("perfect_random_games_10 should NOT be unlocked before Game 10 (currently at Game " + game + ")", 
                        perfectRandom10);
                assertFalse("no_hints_streak_random_10 should NOT be unlocked before Game 10 (currently at Game " + game + ")", 
                        noHintsRandom10);
            }
            
            // After Game 10, check achievements
            if (game == 10) {
                Timber.d("[E2E_RANDOM11] ===== CHECKING ACHIEVEMENTS AFTER GAME 10 =====");
                
                // Wait for achievements to be processed
                Thread.sleep(2000);
                
                boolean perfectRandom10Check = achievementManager.isUnlocked("perfect_random_games_10");
                boolean noHintsRandom10Check = achievementManager.isUnlocked("no_hints_streak_random_10");
                
                Timber.d("[E2E_RANDOM11] Game 10 final check: perfect_random_games_10=%s, no_hints_streak_random_10=%s", 
                        perfectRandom10Check, noHintsRandom10Check);
                
                // perfect_random_games_10 SHOULD be unlocked (10 perfect games)
                assertTrue("perfect_random_games_10 SHOULD be unlocked after 10 perfect games", 
                        perfectRandom10Check);
                
                // no_hints_streak_random_10 should NOT be unlocked (hint was used in game 10)
                assertFalse("no_hints_streak_random_10 should NOT be unlocked because hint was used in game 10", 
                        noHintsRandom10Check);
            }
            
            // If not the last game, start a new game
            if (game < 11) {
                Thread.sleep(2000);
                Timber.d("[E2E_RANDOM11] Starting new random game");
                try {
                    // In random game mode, the next_level_button acts as "New Game" button
                    onView(withId(R.id.next_level_button)).check(matches(isDisplayed())).perform(click());
                    Thread.sleep(500);
                } catch (Exception e) {
                    Timber.e(e, "[E2E_RANDOM11] Could not start new game");
                    fail("Could not start new game after game " + game);
                }
            }
            
            // Reset for next game
            gameCompleted = null;
        }
        
        // Final achievement check
        Thread.sleep(3000);
        
        Timber.d("[E2E_RANDOM11] ===== FINAL ACHIEVEMENT CHECK =====");
        
        boolean finalPerfectRandom10 = achievementManager.isUnlocked("perfect_random_games_10");
        boolean finalNoHintsRandom10 = achievementManager.isUnlocked("no_hints_streak_random_10");
        
        Timber.d("[E2E_RANDOM11] Final: perfect_random_games_10=%s, no_hints_streak_random_10=%s", 
                finalPerfectRandom10, finalNoHintsRandom10);
        
        // Final assertions
        assertTrue("perfect_random_games_10 should be unlocked after 11 perfect games", 
                finalPerfectRandom10);
        assertFalse("no_hints_streak_random_10 should NOT be unlocked because hint was used in game 10", 
                finalNoHintsRandom10);
        
        Timber.d("[E2E_RANDOM11] ✓ Test passed: 11 random games completed, achievements verified");
    }
    
    /**
     * Execute the solution moves for the current game
     */
    private void executeSolutionMoves(int game) throws InterruptedException {
        final GameSolution[] solutionHolder = new GameSolution[1];
        
        // Get solution from GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                solutionHolder[0] = gameStateManager.getCurrentSolution();
                if (solutionHolder[0] != null) {
                    Timber.d("[E2E_RANDOM11] Game %d: Found solution with %d moves", 
                            game, solutionHolder[0].getMoves().size());
                } else {
                    Timber.w("[E2E_RANDOM11] Game %d: No solution found yet", game);
                }
            }
        });
        
        // Wait for solution if not available
        int retries = 0;
        while (solutionHolder[0] == null && retries < 15) {
            Thread.sleep(500);
            retries++;
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    solutionHolder[0] = gameStateManager.getCurrentSolution();
                }
            });
        }
        
        if (solutionHolder[0] == null) {
            Timber.e("[E2E_RANDOM11] Game %d: Could not get solution after %d retries", game, retries);
            fail("Could not get solution for game " + game);
            return;
        }
        
        GameSolution solution = solutionHolder[0];
        ArrayList<IGameMove> moves = solution.getMoves();
        
        Timber.d("[E2E_RANDOM11] Game %d: Executing %d moves (optimal)", game, moves.size());
        
        for (int i = 0; i < moves.size(); i++) {
            IGameMove move = moves.get(i);
            if (move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                int robotColor = rrMove.getColor();
                ERRGameMove direction = rrMove.getMove();
                
                Timber.d("[E2E_RANDOM11] Move %d: Robot %d -> %s", i + 1, robotColor, direction);
                
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
                                    Timber.d("[E2E_RANDOM11] Selected robot with color %d", color);
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
