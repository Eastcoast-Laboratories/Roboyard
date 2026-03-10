package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
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
import roboyard.logic.core.GameState;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E test: Reproduce board size reset bug in Level 2
 * 
 * Scenario:
 * 1. Start Level 1 (10x10)
 * 2. Make a move
 * 3. Click Next Level -> Level 2 (12x10)
 * 4. Make a move
 * 5. Click BACK (undo)
 * 6. Verify board is still 12x10 (NOT reset to 10x10)
 *
 * Tags: e2e, level-game, board-size, undo, bug-reproduction
 */
@RunWith(AndroidJUnit4.class)
public class BoardSizeResetLevel2E2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Timber.d("[UNITTESTS][BOARD_SIZE_E2E] ========== TEST STARTED ==========");
        
        // Wait for achievement/streak popup to close
        TestHelper.startAndWait8sForPopupClose();
    }

    @After
    public void tearDown() {
        Timber.d("[UNITTESTS][BOARD_SIZE_E2E] ========== TEST FINISHED ==========");
    }

    @Test
    public void testBoardSizeNotResetAfterUndoInLevel2() throws InterruptedException {
        Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Starting test: Board size should not reset on undo in Level 2");
        
        // Close any remaining popups
        TestHelper.closeAchievementPopupIfPresent();
        
        // Start Level 1 using TestHelper
        TestHelper.startLevelGame(activityRule, 1);
        Thread.sleep(2000);
        
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        // Verify Level 1 board size (10x10)
        final int[] level1Size = new int[2];
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                level1Size[0] = state.getWidth();
                level1Size[1] = state.getHeight();
                Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Level 1 board size: %dx%d", level1Size[0], level1Size[1]);
            }
        });
        
        assertEquals("Level 1 should be 10 wide", 10, level1Size[0]);
        assertEquals("Level 1 should be 10 high", 10, level1Size[1]);
        
        // COMPLETE Level 1 by executing solution (real user behavior)
        Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Completing Level 1...");
        Thread.sleep(3000); // Wait for solver
        assertTrue("Solution should be executed for Level 1",
                TestHelper.executeSolutionMoves(activityRule, gameStateManager, 1, "BOARD_SIZE_E2E"));
        Thread.sleep(2000);
        
        // Close achievement popup
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(500);
        
        // Click Next Level button (real user behavior)
        Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Clicking Next Level button");
        onView(withId(R.id.next_level_button)).perform(click());
        Thread.sleep(2000);
        
        // Verify Level 2 board size (10x12)
        final int[] level2Size = new int[2];
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                level2Size[0] = state.getWidth();
                level2Size[1] = state.getHeight();
                Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Level 2 board size: %dx%d", level2Size[0], level2Size[1]);
            }
        });
        
        assertEquals("Level 2 should be 10 wide", 10, level2Size[0]);
        assertEquals("Level 2 should be 12 high", 12, level2Size[1]);
        
        // Make a move in Level 2
        Thread.sleep(1000);
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                // Move robot right
                gameStateManager.moveRobotInDirection(1, 0);
                Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Made move in Level 2");
            }
        });
        
        Thread.sleep(1000);
        
        // Get board size BEFORE undo
        final int[] sizeBeforeUndo = new int[2];
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                sizeBeforeUndo[0] = state.getWidth();
                sizeBeforeUndo[1] = state.getHeight();
                Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Board size BEFORE undo: %dx%d", sizeBeforeUndo[0], sizeBeforeUndo[1]);
            }
        });
        
        // Click BACK button (undo)
        Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Clicking BACK button (undo)");
        onView(withId(R.id.back_button)).perform(click());
        Thread.sleep(1000);
        
        // Get board size AFTER undo
        final int[] sizeAfterUndo = new int[2];
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                sizeAfterUndo[0] = state.getWidth();
                sizeAfterUndo[1] = state.getHeight();
                Timber.d("[UNITTESTS][BOARD_SIZE_E2E] Board size AFTER undo: %dx%d", sizeAfterUndo[0], sizeAfterUndo[1]);
            }
        });
        
        // CRITICAL ASSERTION: Board size should NOT change after undo
        assertEquals("Board width should NOT change after undo (should stay 10)", 
                10, sizeAfterUndo[0]);
        assertEquals("Board height should NOT change after undo (should stay 12, not reset to 10)", 
                12, sizeAfterUndo[1]);
        
        // Also verify it matches the size before undo
        assertEquals("Board width should match size before undo", 
                sizeBeforeUndo[0], sizeAfterUndo[0]);
        assertEquals("Board height should match size before undo", 
                sizeBeforeUndo[1], sizeAfterUndo[1]);
        
        Timber.d("[UNITTESTS][BOARD_SIZE_E2E] ✓ Test passed: Board size correctly maintained after undo");
    }
}
