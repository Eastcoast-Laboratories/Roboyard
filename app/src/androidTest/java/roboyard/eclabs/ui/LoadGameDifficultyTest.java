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

import roboyard.eclabs.R;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameState;
import roboyard.logic.core.Preferences;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * Test for Load Game Difficulty handling:
 * 1. Start a random game in Beginner mode (min 4 moves)
 * 2. Save the game
 * 3. Change difficulty to Insane (min 10 moves)
 * 4. Load the saved game
 * 5. Verify the game is NOT regenerated (should keep the Beginner game)
 * 6. Verify the displayed difficulty is from the savegame, not from settings
 * 
 * This test ensures that loaded savegames bypass the min/max moves validation
 * and display the correct difficulty from the savegame.
 */
@RunWith(AndroidJUnit4.class)
public class LoadGameDifficultyTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Context context;
    private GameStateManager gameStateManager;
    private int originalDifficulty;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Store original difficulty to restore after test
        originalDifficulty = Preferences.difficulty;
        
        // Set difficulty to Beginner for the initial game
        Preferences.difficulty = Constants.DIFFICULTY_BEGINNER;
        
        Timber.d("[LOAD_GAME_TEST] ========== TEST STARTED ==========");
        Timber.d("[LOAD_GAME_TEST] Original difficulty: %d, set to Beginner: %d", 
                originalDifficulty, Constants.DIFFICULTY_BEGINNER);
    }

    @After
    public void tearDown() {
        // Restore original difficulty
        Preferences.difficulty = originalDifficulty;
        Timber.d("[LOAD_GAME_TEST] Restored difficulty to: %d", originalDifficulty);
        Timber.d("[LOAD_GAME_TEST] ========== TEST FINISHED ==========");
    }

    @Test
    public void testLoadBeginnerGameWithInsaneDifficulty() throws InterruptedException {
        Timber.d("[LOAD_GAME_TEST] Starting test: Load Beginner game with Insane difficulty settings");
        
        // Step 1: Start a new random game in Beginner mode
        Timber.d("[LOAD_GAME_TEST] Step 1: Starting new random game in Beginner mode");
        Thread.sleep(500);
        
        onView(withId(R.id.modern_ui_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(3000); // Wait for game to generate and solver to find solution
        
        // Get GameStateManager reference
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        Thread.sleep(500);
        
        // Verify we have a game state
        final GameState[] gameStateHolder = new GameState[1];
        final int[] solutionMovesHolder = new int[1];
        
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateHolder[0] = gameStateManager.getCurrentState().getValue();
                if (gameStateManager.getCurrentSolution() != null) {
                    solutionMovesHolder[0] = gameStateManager.getCurrentSolution().getMoves().size();
                }
            }
        });
        Thread.sleep(500);
        
        assertNotNull("Game state should not be null", gameStateHolder[0]);
        Timber.d("[LOAD_GAME_TEST] Game created with %d solution moves, difficulty: %d", 
                solutionMovesHolder[0], gameStateHolder[0].getDifficulty());
        
        // Verify the game difficulty is Beginner
        assertEquals("Game difficulty should be Beginner", 
                Constants.DIFFICULTY_BEGINNER, gameStateHolder[0].getDifficulty());
        
        // Step 2: Save the game
        Timber.d("[LOAD_GAME_TEST] Step 2: Saving the game");
        
        // Click save button
        try {
            onView(withId(R.id.save_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(1000);
            Timber.d("[LOAD_GAME_TEST] Save button clicked");
        } catch (Exception e) {
            Timber.e(e, "[LOAD_GAME_TEST] Could not find save button, trying menu");
            // Try alternative save method if direct button not available
        }
        
        // Step 3: Go back to main menu and change difficulty to Insane
        Timber.d("[LOAD_GAME_TEST] Step 3: Changing difficulty to Insane");
        
        // Navigate back to main menu
        try {
            onView(withId(R.id.back_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(500);
        } catch (Exception e) {
            Timber.w(e, "[LOAD_GAME_TEST] Could not click back button");
        }
        
        // Change difficulty to Insane (programmatically since we're testing the load behavior)
        Preferences.difficulty = Constants.DIFFICULTY_INSANE;
        Timber.d("[LOAD_GAME_TEST] Difficulty changed to Insane: %d", Preferences.difficulty);
        
        // Step 4: Load the saved game
        Timber.d("[LOAD_GAME_TEST] Step 4: Loading the saved game");
        
        // Navigate to Load Game screen
        try {
            onView(withId(R.id.load_game_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(1000);
            Timber.d("[LOAD_GAME_TEST] Load Game button clicked");
        } catch (Exception e) {
            Timber.e(e, "[LOAD_GAME_TEST] Could not click Load Game button");
            fail("Could not navigate to Load Game screen");
        }
        
        // Click on the first save slot to load
        Thread.sleep(500);
        
        // Load game programmatically since UI interaction with save slots is complex
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.loadGame(0); // Load from slot 0
                Timber.d("[LOAD_GAME_TEST] loadGame(0) called");
            }
        });
        
        Thread.sleep(3000); // Wait for game to load and solver to run
        
        // Step 5: Verify the game was NOT regenerated
        Timber.d("[LOAD_GAME_TEST] Step 5: Verifying game was not regenerated");
        
        final GameState[] loadedStateHolder = new GameState[1];
        final boolean[] isLoadedFromSaveHolder = new boolean[1];
        final int[] effectiveDifficultyHolder = new int[1];
        
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                loadedStateHolder[0] = gameStateManager.getCurrentState().getValue();
                isLoadedFromSaveHolder[0] = gameStateManager.isLoadedFromSave();
                effectiveDifficultyHolder[0] = gameStateManager.getEffectiveDifficulty();
            }
        });
        Thread.sleep(500);
        
        assertNotNull("Loaded game state should not be null", loadedStateHolder[0]);
        
        // Verify isLoadedFromSave flag is set
        assertTrue("isLoadedFromSave should be true after loading a savegame", 
                isLoadedFromSaveHolder[0]);
        
        // Step 6: Verify the displayed difficulty is from the savegame (Beginner), not from settings (Insane)
        Timber.d("[LOAD_GAME_TEST] Step 6: Verifying displayed difficulty");
        
        assertEquals("Effective difficulty should be Beginner (from savegame), not Insane (from settings)", 
                Constants.DIFFICULTY_BEGINNER, effectiveDifficultyHolder[0]);
        
        Timber.d("[LOAD_GAME_TEST] Loaded game difficulty: %d, effective difficulty: %d, settings difficulty: %d",
                loadedStateHolder[0].getDifficulty(), effectiveDifficultyHolder[0], Preferences.difficulty);
        
        // Verify the game state difficulty matches the original Beginner game
        assertEquals("Loaded game should have Beginner difficulty stored", 
                Constants.DIFFICULTY_BEGINNER, loadedStateHolder[0].getDifficulty());
        
        // Verify settings still show Insane (unchanged)
        assertEquals("Settings difficulty should still be Insane", 
                Constants.DIFFICULTY_INSANE, Preferences.difficulty);
        
        Timber.d("[LOAD_GAME_TEST] ✓ Test passed: Loaded Beginner game with Insane settings, game was not regenerated");
        Timber.d("[LOAD_GAME_TEST] ✓ Effective difficulty correctly shows Beginner from savegame");
    }
    
    @Test
    public void testNewGameResetsLoadedFromSaveFlag() throws InterruptedException {
        Timber.d("[LOAD_GAME_TEST] Starting test: New Game resets isLoadedFromSave flag");
        
        // First, simulate loading a game
        Preferences.difficulty = Constants.DIFFICULTY_BEGINNER;
        
        onView(withId(R.id.modern_ui_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(3000);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        Thread.sleep(500);
        
        // Save the game
        try {
            onView(withId(R.id.save_button)).check(matches(isDisplayed())).perform(click());
            Thread.sleep(1000);
        } catch (Exception e) {
            Timber.w(e, "[LOAD_GAME_TEST] Could not save game");
        }
        
        // Load the game
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.loadGame(0);
            }
        });
        Thread.sleep(2000);
        
        // Verify isLoadedFromSave is true
        final boolean[] isLoadedFromSaveHolder = new boolean[1];
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                isLoadedFromSaveHolder[0] = gameStateManager.isLoadedFromSave();
            }
        });
        
        assertTrue("isLoadedFromSave should be true after loading", isLoadedFromSaveHolder[0]);
        
        // Now start a new game
        Timber.d("[LOAD_GAME_TEST] Starting new game to reset flag");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.startNewGame();
            }
        });
        Thread.sleep(3000);
        
        // Verify isLoadedFromSave is now false
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                isLoadedFromSaveHolder[0] = gameStateManager.isLoadedFromSave();
            }
        });
        
        assertFalse("isLoadedFromSave should be false after starting new game", isLoadedFromSaveHolder[0]);
        
        Timber.d("[LOAD_GAME_TEST] ✓ Test passed: New game correctly resets isLoadedFromSave flag");
    }
}
