package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.logic.core.GameState;
import roboyard.logic.core.Preferences;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * E2E test for Hint Auto-Move modes:
 * - Mode 0 (Manual): User moves robots manually
 * - Mode 1 (Full-Auto): Robot moves automatically when hint is shown
 * - Mode 2 (Semi-Auto): Robot moves when next-hint button is clicked
 */
@RunWith(AndroidJUnit4.class)
public class HintAutoModeE2ETest {
    
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = 
            new ActivityScenarioRule<>(MainActivity.class);
    
    private GameStateManager gameStateManager;
    
    @Before
    public void setUp() throws InterruptedException {
        Timber.d("[HINT_AUTO_MODE_E2E] ========== TEST STARTED ==========");
        
        // Clear history and start fresh
        activityRule.getScenario().onActivity(activity -> {
            try {
                TestHelper.startNewSessionWithEmptyStorage(activity);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(1000);
        
        // Close any popups
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(500);
    }
    
    @After
    public void tearDown() {
        Timber.d("[HINT_AUTO_MODE_E2E] ========== TEST FINISHED ==========");
    }
    
    /**
     * Test Mode 0 (Manual): User must move robots manually
     */
    @Test
    public void testManualMode() throws InterruptedException {
        Timber.d("[HINT_AUTO_MODE_E2E] Testing Manual Mode (Mode 0)");
        
        // Set mode to Manual via Settings UI
        TestHelper.setHintAutoMoveMode(0);
        
        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(3000);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Get initial move count
        final int[] initialMoveCount = new int[1];
        activityRule.getScenario().onActivity(activity -> {
            initialMoveCount[0] = gameStateManager.getMoveCount().getValue();
            Timber.d("[HINT_AUTO_MODE_E2E] Initial move count: %d", initialMoveCount[0]);
        });
        
        // Click hint button to show hints
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(2000);
        
        // Click next hint button multiple times
        for (int i = 0; i < 3; i++) {
            onView(withId(R.id.next_hint_button)).perform(click());
            Thread.sleep(500);
        }
        
        // In Manual mode, move count should NOT increase (robot doesn't move automatically)
        final int[] finalMoveCount = new int[1];
        activityRule.getScenario().onActivity(activity -> {
            finalMoveCount[0] = gameStateManager.getMoveCount().getValue();
            Timber.d("[HINT_AUTO_MODE_E2E] Final move count: %d", finalMoveCount[0]);
        });
        
        assertEquals("Manual mode: move count should not change", 
                initialMoveCount[0], finalMoveCount[0]);
        
        Timber.d("[HINT_AUTO_MODE_E2E] ✓ Manual mode test passed");
    }
    
    /**
     * Test Mode 1 (Full-Auto): Robot moves automatically when hint is shown
     */
    @Test
    public void testFullAutoMode() throws InterruptedException {
        Timber.d("[HINT_AUTO_MODE_E2E] Testing Full-Auto Mode (Mode 1)");
        
        // Set mode to Full-Auto via Settings UI
        TestHelper.setHintAutoMoveMode(1);
        
        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(3000);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Get initial move count
        final int[] initialMoveCount = new int[1];
        activityRule.getScenario().onActivity(activity -> {
            initialMoveCount[0] = gameStateManager.getMoveCount().getValue();
            Timber.d("[HINT_AUTO_MODE_E2E] Initial move count: %d", initialMoveCount[0]);
        });
        
        // Click hint button to show first hint
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(2000);
        
        // Click through ALL pre-hints to reach the first normal hint (where auto-move is triggered)
        // Pre-hints vary by solution length, so click many times to be sure
        // The next-hint button will become invisible when we reach the end
        for (int i = 0; i < 10; i++) {
            try {
                onView(withId(R.id.next_hint_button)).perform(click());
                Thread.sleep(800);
                Timber.d("[HINT_AUTO_MODE_E2E] Clicked next hint %d times", i + 1);
            } catch (Exception e) {
                // Button might not be visible anymore
                Timber.d("[HINT_AUTO_MODE_E2E] Next hint button not visible after %d clicks", i + 1);
                break;
            }
        }
        
        // Now we should be at a normal hint
        // In Full-Auto mode, robot should move automatically after hint is shown
        // Wait for auto-move to complete (100ms delay + animation + auto-advance)
        Thread.sleep(3000);
        
        // Move count should increase by 1 (first hint auto-moved)
        final int[] afterFirstHint = new int[1];
        activityRule.getScenario().onActivity(activity -> {
            afterFirstHint[0] = gameStateManager.getMoveCount().getValue();
            Timber.d("[HINT_AUTO_MODE_E2E] After first hint: %d moves", afterFirstHint[0]);
        });
        
        assertTrue("Full-Auto mode: move count should increase after first hint", 
                afterFirstHint[0] > initialMoveCount[0]);
        
        Timber.d("[HINT_AUTO_MODE_E2E] ✓ Full-Auto mode test passed - robot moved automatically");
    }
    
    /**
     * Test Mode 2 (Semi-Auto): Robot moves only when next-hint button is clicked
     */
    @Test
    public void testSemiAutoMode() throws InterruptedException {
        Timber.d("[HINT_AUTO_MODE_E2E] Testing Semi-Auto Mode (Mode 2)");
        
        // Set mode to Semi-Auto via Settings UI
        TestHelper.setHintAutoMoveMode(2);
        
        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(3000);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Get initial move count
        final int[] initialMoveCount = new int[1];
        activityRule.getScenario().onActivity(activity -> {
            initialMoveCount[0] = gameStateManager.getMoveCount().getValue();
            Timber.d("[HINT_AUTO_MODE_E2E] Initial move count: %d", initialMoveCount[0]);
        });
        
        // Click hint button to show first hint (pre-hint)
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(2000);
        
        // Click through pre-hints to reach normal hints
        // In Semi-Auto mode, clicking next-hint button should move the robot
        final int[] clickCount = new int[1];
        for (int i = 0; i < 10; i++) {
            try {
                onView(withId(R.id.next_hint_button)).perform(click());
                Thread.sleep(800);
                clickCount[0]++;
                Timber.d("[HINT_AUTO_MODE_E2E] Clicked next hint %d times", clickCount[0]);
            } catch (Exception e) {
                Timber.d("[HINT_AUTO_MODE_E2E] Next hint button not visible after %d clicks", clickCount[0]);
                break;
            }
        }
        
        // Check that robot has moved (Semi-Auto: moves on next-hint button click)
        final int[] finalMoveCount = new int[1];
        activityRule.getScenario().onActivity(activity -> {
            finalMoveCount[0] = gameStateManager.getMoveCount().getValue();
            Timber.d("[HINT_AUTO_MODE_E2E] Final move count after %d clicks: %d moves", clickCount[0], finalMoveCount[0]);
        });
        
        assertTrue("Semi-Auto mode: robot should move when next-hint button is clicked", 
                finalMoveCount[0] > initialMoveCount[0]);
        
        Timber.d("[HINT_AUTO_MODE_E2E] ✓ Semi-Auto mode test passed");
    }
}
