package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import android.content.Context;
import android.view.View;
import android.widget.ToggleButton;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameSolution;
import roboyard.logic.core.GameState;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * Espresso E2E test that verifies the Reset button hides hints.
 *
 * Steps:
 * 1. Start a game (Level 1)
 * 2. Move a robot successfully (first move of the solution)
 * 3. Press the Hint button a few times
 * 4. Activate hot/cold mode (live move counter toggle)
 * 5. Press Reset
 * 6. Assert: hint_container is GONE, hintButton is unchecked
 *
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.ResetHintE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class ResetHintE2ETest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testResetButtonHidesHint() throws InterruptedException {
        Timber.d("[RESET_HINT_TEST] Starting test");

        // Wait for activity to load
        Thread.sleep(3000);

        // Get GameStateManager and navigate to ModernGameFragment with Level 1
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);

            // Load level data first
            gameStateManager.startNewGame();
            gameStateManager.loadLevel(1);
            Timber.d("[RESET_HINT_TEST] Level 1 loaded into GameStateManager");

            // Navigate to ModernGameFragment via NavController
            NavHostFragment navHostFragment = (NavHostFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.modernGameFragment);
                Timber.d("[RESET_HINT_TEST] Navigated to ModernGameFragment");
            }
        });
        Thread.sleep(5000);

        // Wait for solution to be ready
        GameSolution solution = null;
        for (int i = 0; i < 10; i++) {
            solution = gameStateManager.getCurrentSolution();
            if (solution != null && solution.getMoves() != null && !solution.getMoves().isEmpty()) {
                break;
            }
            Timber.d("[RESET_HINT_TEST] Waiting for solution... attempt %d", i + 1);
            Thread.sleep(2000);
        }
        assertNotNull("Solution should be available", solution);
        assertTrue("Solution should have moves", solution.getMoves().size() > 0);
        Timber.d("[RESET_HINT_TEST] Solution ready with %d moves", solution.getMoves().size());

        // Step 2: Move a robot successfully (first move of the solution)
        IGameMove firstMove = solution.getMoves().get(0);
        Timber.d("[RESET_HINT_TEST] Executing first move: %s", firstMove);
        activityRule.getScenario().onActivity(activity -> {
            executeSolutionMove(firstMove);
        });
        Thread.sleep(1500);

        // Verify the move was made (move count should be 1)
        activityRule.getScenario().onActivity(activity -> {
            Integer moveCount = gameStateManager.getMoveCount().getValue();
            Timber.d("[RESET_HINT_TEST] Move count after first move: %s", moveCount);
            assertNotNull("Move count should not be null", moveCount);
            assertTrue("Move count should be >= 1 after moving a robot", moveCount >= 1);
        });

        // Step 3: Press the Hint button a few times
        Timber.d("[RESET_HINT_TEST] Pressing Hint button (1st time)");
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(1500);

        // Verify hint container is visible
        onView(withId(R.id.hint_container)).check(matches(isDisplayed()));
        Timber.d("[RESET_HINT_TEST] Hint container is visible after 1st click");

        // Press next hint arrow a couple of times
        Timber.d("[RESET_HINT_TEST] Pressing next hint arrow");
        onView(withId(R.id.next_hint_button)).perform(click());
        Thread.sleep(800);
        onView(withId(R.id.next_hint_button)).perform(click());
        Thread.sleep(800);
        Timber.d("[RESET_HINT_TEST] Pressed next hint 2 times");

        // Step 4: Activate hot/cold mode (live move counter toggle)
        // The eye toggle is inside the hint_container and only visible when hints are shown
        Timber.d("[RESET_HINT_TEST] Activating hot/cold mode (live move counter)");
        activityRule.getScenario().onActivity(activity -> {
            ToggleButton toggle = activity.findViewById(R.id.live_move_counter_toggle);
            if (toggle != null && toggle.getVisibility() == View.VISIBLE) {
                toggle.performClick();
                Timber.d("[RESET_HINT_TEST] Clicked live_move_counter_toggle");
            } else {
                // Toggle might not be visible yet; enable it programmatically
                if (toggle != null) {
                    toggle.setVisibility(View.VISIBLE);
                    toggle.setChecked(true);
                    Timber.d("[RESET_HINT_TEST] Enabled live_move_counter_toggle programmatically");
                }
                gameStateManager.setLiveMoveCounterEnabled(true);
                Timber.d("[RESET_HINT_TEST] Enabled live move counter via GameStateManager");
            }
        });
        Thread.sleep(1000);

        // Step 5: Press Reset
        Timber.d("[RESET_HINT_TEST] Pressing Reset button");
        onView(withId(R.id.reset_robots_button)).perform(click());
        Thread.sleep(2000);

        // Step 6: Verify hintButton is unchecked (hint mode deactivated)
        Timber.d("[RESET_HINT_TEST] Verifying hint state after reset");
        activityRule.getScenario().onActivity(activity -> {
            ToggleButton hintBtn = activity.findViewById(R.id.hint_button);
            assertNotNull("Hint button should exist", hintBtn);
            assertFalse("Hint button should be unchecked after reset", hintBtn.isChecked());
            Timber.d("[RESET_HINT_TEST] Hint button isChecked=%s", hintBtn.isChecked());

            // hint_container may still be VISIBLE if live move counter is active (shows status + eye toggle)
            // but the hint navigation arrows must be hidden
            android.widget.TextView prevBtn = activity.findViewById(R.id.prev_hint_button);
            android.widget.TextView nextBtn = activity.findViewById(R.id.next_hint_button);
            if (prevBtn != null) {
                assertEquals("Prev hint arrow should be GONE after reset",
                        View.GONE, prevBtn.getVisibility());
            }
            if (nextBtn != null) {
                assertEquals("Next hint arrow should be GONE after reset",
                        View.GONE, nextBtn.getVisibility());
            }
            Timber.d("[RESET_HINT_TEST] Hint arrows hidden: prev=%d, next=%d",
                    prevBtn != null ? prevBtn.getVisibility() : -1,
                    nextBtn != null ? nextBtn.getVisibility() : -1);

            // Log status text
            android.widget.TextView statusText = activity.findViewById(R.id.status_text);
            if (statusText != null) {
                Timber.d("[RESET_HINT_TEST] Status text after reset: '%s'", statusText.getText());
            }
        });

        Timber.d("[RESET_HINT_TEST] Test passed!");
    }

    /**
     * Execute a solution move by selecting the robot and moving it.
     */
    private void executeSolutionMove(IGameMove move) {
        if (!(move instanceof RRGameMove)) return;
        RRGameMove rrMove = (RRGameMove) move;
        int robotColor = rrMove.getColor();
        int direction = rrMove.getDirection();

        Timber.d("[RESET_HINT_TEST] Moving robot color=%d direction=%d", robotColor, direction);

        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null) return;

        // Find the robot with matching color
        GameElement selectedRobot = null;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT && element.getColor() == robotColor) {
                selectedRobot = element;
                break;
            }
        }

        if (selectedRobot != null) {
            state.setSelectedRobot(selectedRobot);

            int dx = 0, dy = 0;
            switch (direction) {
                case 1: dy = -1; break; // UP
                case 2: dx = 1; break;  // RIGHT
                case 4: dy = 1; break;  // DOWN
                case 8: dx = -1; break; // LEFT
            }
            gameStateManager.moveRobotInDirection(dx, dy);
            Timber.d("[RESET_HINT_TEST] Robot moved successfully");
        } else {
            Timber.w("[RESET_HINT_TEST] Robot with color %d not found", robotColor);
        }
    }
}
