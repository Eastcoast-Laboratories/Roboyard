package roboyard.eclabs.ui;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * Regression test for the reverse-move undo bug:
 * After a reverse move triggers an undo, subsequent robot moves should still work.
 *
 * Scenario:
 * 1. Load Level 1
 * 2. Select blue robot, move LEFT
 * 3. Move UP
 * 4. Move DOWN (reverse of UP -> triggers undo)
 * 5. Verify: move count decremented, robot can still be moved (move RIGHT works)
 *
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.ReverseMoveUndoBugTest
 */
@RunWith(AndroidJUnit4.class)
public class ReverseMoveUndoBugTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        Thread.sleep(200);

        // Navigate to Level 1
        onView(withId(R.id.level_game_button)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(200);
        onView(allOf(withId(R.id.level_button), withText("1"))).check(matches(isDisplayed())).perform(click());
        Thread.sleep(500);

        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });

        assertNotNull("GameStateManager must not be null", gameStateManager);
    }

    /**
     * Helper: select the blue robot and move it in the given direction on the UI thread.
     */
    private boolean selectBlueAndMove(int dx, int dy) throws InterruptedException {
        final boolean[] result = {false};
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) return;

            // Find and select blue robot
            for (GameElement el : state.getGameElements()) {
                if (el.getType() == GameElement.TYPE_ROBOT && el.getColor() == Constants.COLOR_BLUE) {
                    state.setSelectedRobot(el);
                    break;
                }
            }

            result[0] = gameStateManager.moveRobotInDirection(dx, dy);
            Timber.d("[UNDO_BUG_TEST] moveRobotInDirection(%d,%d) returned %b", dx, dy, result[0]);
        });
        Thread.sleep(800);
        return result[0];
    }

    @Test
    public void testReverseMoveUndoDoesNotBlockSubsequentMoves() throws InterruptedException {
        Timber.d("[UNDO_BUG_TEST] ===== START testReverseMoveUndoDoesNotBlockSubsequentMoves =====");

        // Step 1: Move blue robot LEFT
        boolean movedLeft = selectBlueAndMove(-1, 0);
        Timber.d("[UNDO_BUG_TEST] Move LEFT result: %b", movedLeft);

        final int[] moveCountAfterLeft = {-1};
        activityRule.getScenario().onActivity(activity -> {
            moveCountAfterLeft[0] = gameStateManager.getMoveCount().getValue();
            Timber.d("[UNDO_BUG_TEST] Move count after LEFT: %d", moveCountAfterLeft[0]);
        });

        // Step 2: Move blue robot UP
        boolean movedUp = selectBlueAndMove(0, -1);
        Timber.d("[UNDO_BUG_TEST] Move UP result: %b", movedUp);

        final int[] moveCountAfterUp = {-1};
        final int[] blueXAfterUp = {-1};
        final int[] blueYAfterUp = {-1};
        activityRule.getScenario().onActivity(activity -> {
            moveCountAfterUp[0] = gameStateManager.getMoveCount().getValue();
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                for (GameElement el : state.getGameElements()) {
                    if (el.getType() == GameElement.TYPE_ROBOT && el.getColor() == Constants.COLOR_BLUE) {
                        blueXAfterUp[0] = el.getX();
                        blueYAfterUp[0] = el.getY();
                        break;
                    }
                }
            }
            Timber.d("[UNDO_BUG_TEST] Move count after UP: %d, blue robot at (%d,%d)",
                    moveCountAfterUp[0], blueXAfterUp[0], blueYAfterUp[0]);
        });

        // Step 3: Move blue robot DOWN (reverse of UP -> should trigger undo)
        boolean movedDown = selectBlueAndMove(0, 1);
        Timber.d("[UNDO_BUG_TEST] Move DOWN (reverse-undo) result: %b", movedDown);

        final int[] moveCountAfterUndo = {-1};
        final int[] blueXAfterUndo = {-1};
        final int[] blueYAfterUndo = {-1};
        activityRule.getScenario().onActivity(activity -> {
            moveCountAfterUndo[0] = gameStateManager.getMoveCount().getValue();
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                for (GameElement el : state.getGameElements()) {
                    if (el.getType() == GameElement.TYPE_ROBOT && el.getColor() == Constants.COLOR_BLUE) {
                        blueXAfterUndo[0] = el.getX();
                        blueYAfterUndo[0] = el.getY();
                        break;
                    }
                }
            }
            Timber.d("[UNDO_BUG_TEST] Move count after DOWN-undo: %d, blue robot at (%d,%d)",
                    moveCountAfterUndo[0], blueXAfterUndo[0], blueYAfterUndo[0]);
        });

        // Verify: move count should have decreased (undo happened)
        if (movedUp) {
            assertTrue("Move count should have decreased after reverse-move undo",
                    moveCountAfterUndo[0] < moveCountAfterUp[0]);
        }

        // Step 4: THE CRITICAL STEP - move RIGHT after the undo
        // This is where the bug manifests: robotAnimationInProgress stays true, blocking all moves
        boolean movedRight = selectBlueAndMove(1, 0);
        Timber.d("[UNDO_BUG_TEST] Move RIGHT after undo result: %b", movedRight);

        final int[] moveCountAfterRight = {-1};
        final int[] blueXAfterRight = {-1};
        final int[] blueYAfterRight = {-1};
        activityRule.getScenario().onActivity(activity -> {
            moveCountAfterRight[0] = gameStateManager.getMoveCount().getValue();
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                for (GameElement el : state.getGameElements()) {
                    if (el.getType() == GameElement.TYPE_ROBOT && el.getColor() == Constants.COLOR_BLUE) {
                        blueXAfterRight[0] = el.getX();
                        blueYAfterRight[0] = el.getY();
                        break;
                    }
                }
            }
            Timber.d("[UNDO_BUG_TEST] Move count after RIGHT: %d, blue robot at (%d,%d)",
                    moveCountAfterRight[0], blueXAfterRight[0], blueYAfterRight[0]);
        });

        // THE KEY ASSERTION: after the undo, the robot must still be movable
        // If the bug is present, moveRobotInDirection returns false and move count stays the same
        assertTrue("Robot must be movable after reverse-move undo (bug: robotAnimationInProgress stuck at true)",
                movedRight);
        assertTrue("Move count must have increased after RIGHT move post-undo",
                moveCountAfterRight[0] > moveCountAfterUndo[0]);

        Timber.d("[UNDO_BUG_TEST] ===== TEST PASSED =====");
    }
}
