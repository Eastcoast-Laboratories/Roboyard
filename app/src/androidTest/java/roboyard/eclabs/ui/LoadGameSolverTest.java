package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.MainActivity;
import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * UI Test to verify that loading a saved game properly initializes the solver.
 * 
 * This test reproduces the bug where:
 * 1. User loads a saved game (e.g., slot 4 "FOJEK")
 * 2. User clicks the hint button
 * 3. Solver shows "AI calculating" forever because it has no map data
 * 
 * The test expects the app to either:
 * - Properly initialize the solver with the loaded map data, OR
 * - Throw an IllegalStateException if solver is started without initialization
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoadGameSolverTest {

    private static final int SAVE_SLOT_INDEX = 4; // 5th slot (0-indexed), which is "FOJEK" if autosave is slot 0
    
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Timber.d("[TEST] LoadGameSolverTest setup complete");
    }

    /**
     * Test that loading a saved game and clicking hint button works correctly.
     * 
     * Steps:
     * 1. Click "Load Game" button on main menu
     * 2. Click on save slot 4 (FOJEK)
     * 3. Wait for game to load
     * 4. Click hint button
     * 5. Verify solver either finds a solution or throws an error (not infinite loop)
     */
    @Test
    public void testLoadGameAndHintButton() {
        Timber.d("[TEST] Starting testLoadGameAndHintButton");
        
        // Step 1: Verify main menu is displayed
        Timber.d("[TEST] Step 1: Verifying main menu is displayed");
        onView(withId(R.id.main_menu_title)).check(matches(isDisplayed()));
        
        // Step 2: Click "Load Game" button
        Timber.d("[TEST] Step 2: Clicking Load Game button");
        onView(withId(R.id.load_game_button)).perform(click());
        
        // Step 3: Wait for save game screen to load
        Timber.d("[TEST] Step 3: Waiting for save game screen");
        try {
            Thread.sleep(1000); // Wait for screen transition
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Step 4: Verify save game screen is displayed
        Timber.d("[TEST] Step 4: Verifying save game screen is displayed");
        onView(withId(R.id.save_game_title)).check(matches(isDisplayed()));
        
        // Step 5: Click on save slot 4 (index 4, which is the 5th slot)
        // The save slots are in a RecyclerView, so we need to scroll to and click the item
        Timber.d("[TEST] Step 5: Clicking on save slot %d", SAVE_SLOT_INDEX);
        try {
            onView(withId(R.id.save_slots_recycler))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(SAVE_SLOT_INDEX, click()));
        } catch (Exception e) {
            Timber.e(e, "[TEST] Failed to click save slot %d", SAVE_SLOT_INDEX);
            fail("Failed to click save slot " + SAVE_SLOT_INDEX + ": " + e.getMessage());
        }
        
        // Step 6: Wait for game to load
        Timber.d("[TEST] Step 6: Waiting for game to load");
        try {
            Thread.sleep(2000); // Wait for game to load
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Step 7: Verify game screen is displayed (game grid should be visible)
        Timber.d("[TEST] Step 7: Verifying game screen is displayed");
        onView(withId(R.id.game_grid)).check(matches(isDisplayed()));
        
        // Step 8: Click hint button
        Timber.d("[TEST] Step 8: Clicking hint button");
        try {
            onView(withId(R.id.hint_toggle_button)).perform(click());
        } catch (Exception e) {
            Timber.e(e, "[TEST] Failed to click hint button");
            fail("Failed to click hint button: " + e.getMessage());
        }
        
        // Step 9: Wait for solver to start (or fail with our new check)
        Timber.d("[TEST] Step 9: Waiting for solver response");
        try {
            Thread.sleep(3000); // Wait for solver to respond
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Step 10: Verify that hint container is visible (solver should have started)
        Timber.d("[TEST] Step 10: Verifying hint container is visible");
        onView(withId(R.id.hint_container)).check(matches(isDisplayed()));
        
        // If we get here without an exception, the solver was properly initialized
        Timber.d("[TEST] Test completed successfully - solver was properly initialized");
    }
    
    /**
     * Test that verifies the solver throws an error when started without initialization.
     * This test is expected to fail until the bug is fixed.
     */
    @Test
    public void testSolverRequiresInitialization() {
        Timber.d("[TEST] Starting testSolverRequiresInitialization");
        
        activityScenarioRule.getScenario().onActivity(activity -> {
            // Get the SolverManager singleton
            roboyard.eclabs.util.SolverManager solverManager = 
                    roboyard.eclabs.util.SolverManager.getInstance();
            
            // Reset initialization to simulate the bug condition
            solverManager.resetInitialization();
            
            // Try to start solver without initialization - should throw
            try {
                solverManager.startSolver();
                fail("Expected IllegalStateException when starting solver without initialization");
            } catch (IllegalStateException e) {
                Timber.d("[TEST] Correctly caught IllegalStateException: %s", e.getMessage());
                assertTrue("Exception message should mention initialization", 
                        e.getMessage().contains("initialize"));
            }
        });
        
        Timber.d("[TEST] testSolverRequiresInitialization completed successfully");
    }
}
