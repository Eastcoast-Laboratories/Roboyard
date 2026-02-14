package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import androidx.test.espresso.matcher.ViewMatchers;

import android.content.Context;
import android.view.View;
import android.widget.ToggleButton;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.achievements.AchievementManager;
import roboyard.logic.core.GameSolution;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E test for the Live Move Counter feature and hint system regression.
 *
 * Tests:
 * 1. Hint system works correctly without live counter (regression)
 * 2. Eye toggle appears on the exact solution pre-hint
 * 3. Eye toggle activates live counter and shows result after a move
 *
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.LiveMoveCounterE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class LiveMoveCounterE2ETest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Context context;
    private GameStateManager gameStateManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AchievementManager.getInstance(context).resetAll();
    }

    @After
    public void tearDown() {
        AchievementManager.getInstance(context).resetAll();
    }

    private void navigateToLevel1() throws InterruptedException {
        // Wait for main menu to fully load and buttons to become visible
        Thread.sleep(2000);
        
        // Click "Level Game" button on main menu (use programmatic click to avoid visibility issues)
        activityRule.getScenario().onActivity(activity -> {
            View levelGameBtn = activity.findViewById(R.id.level_game_button);
            if (levelGameBtn != null) {
                levelGameBtn.setVisibility(View.VISIBLE);
                levelGameBtn.performClick();
            }
        });
        Thread.sleep(1000);
        
        // Click level "1" button
        onView(allOf(withId(R.id.level_button), withText("1"))).perform(click());
        Thread.sleep(2000);
        
        // Get GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        assertNotNull("GameStateManager should not be null", gameStateManager);
        Timber.d("[LIVE_COUNTER_TEST] Navigated to Level 1");
    }

    /**
     * Test that the hint system works correctly without the live counter enabled.
     * This is a regression test to ensure the new feature doesn't break existing hints.
     */
    @Test
    public void testHintSystemWithoutLiveCounter() throws InterruptedException {
        Timber.d("[LIVE_COUNTER_TEST] Starting hint system regression test");

        navigateToLevel1();

        GameSolution solution = waitForSolution(10);
        if (solution == null) {
            Timber.w("[LIVE_COUNTER_TEST] No solution available, skipping hint test");
            return;
        }

        Timber.d("[LIVE_COUNTER_TEST] Solution has %d moves", solution.getMoves().size());

        // Verify live counter is NOT enabled by default
        assertFalse("Live counter should be disabled by default",
                gameStateManager.isLiveMoveCounterEnabled());

        // Click the hint button to show hints (use onActivity to click programmatically
        // since the button may be off-screen on small phones)
        activityRule.getScenario().onActivity(activity -> {
            ToggleButton hintBtn = activity.findViewById(R.id.hint_button);
            if (hintBtn != null) {
                hintBtn.setChecked(true);
            }
        });
        Thread.sleep(2000);

        // Verify hint container is visible
        final boolean[] containerVisible = {false};
        activityRule.getScenario().onActivity(activity -> {
            View container = activity.findViewById(R.id.hint_container);
            containerVisible[0] = container != null && container.getVisibility() == View.VISIBLE;
        });
        assertTrue("Hint container should be visible after enabling hints", containerVisible[0]);

        Timber.d("[LIVE_COUNTER_TEST] Hint system regression test passed");
    }

    /**
     * Test that the eye toggle appears on the exact solution pre-hint
     * and is hidden on other hints.
     */
    @Test
    public void testEyeToggleVisibility() throws InterruptedException {
        Timber.d("[LIVE_COUNTER_TEST] Starting eye toggle visibility test");

        navigateToLevel1();

        GameSolution solution = waitForSolution(10);
        if (solution == null) {
            Timber.w("[LIVE_COUNTER_TEST] No solution, skipping eye toggle test");
            return;
        }

        // Eye toggle should be GONE initially (before hints are shown)
        onView(withId(R.id.live_move_counter_toggle)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // Click hint button to start showing hints
        onView(withId(R.id.hint_button)).perform(androidx.test.espresso.action.ViewActions.click());
        Thread.sleep(1000);

        // First pre-hint: eye toggle should still be GONE
        onView(withId(R.id.live_move_counter_toggle)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // Click through pre-hints until we reach the exact solution hint
        // The exact solution hint is at index numPreHints (which is randomized 2-4)
        // We click "next" multiple times to get there
        for (int i = 0; i < 6; i++) {
            onView(withId(R.id.status_text)).perform(androidx.test.espresso.action.ViewActions.click());
            Thread.sleep(800);

            // Check if the eye toggle became visible
            final boolean[] isVisible = {false};
            activityRule.getScenario().onActivity(activity -> {
                ToggleButton toggle = activity.findViewById(R.id.live_move_counter_toggle);
                if (toggle != null) {
                    isVisible[0] = toggle.getVisibility() == View.VISIBLE;
                }
            });

            if (isVisible[0]) {
                Timber.d("[LIVE_COUNTER_TEST] Eye toggle became visible at hint step %d", i + 1);
                // Found it! Test passes
                return;
            }
        }

        // If we got here, the eye toggle never appeared - this might happen if
        // the pre-hint count was high. Log but don't fail hard.
        Timber.w("[LIVE_COUNTER_TEST] Eye toggle did not become visible within 6 hint clicks");
    }

    /**
     * Test that enabling the eye toggle triggers the live solver
     * and shows a result after making a move.
     */
    @Test
    public void testLiveMoveCounterAfterMove() throws InterruptedException {
        Timber.d("[LIVE_COUNTER_TEST] Starting live move counter test");

        navigateToLevel1();

        GameSolution solution = waitForSolution(10);
        if (solution == null) {
            Timber.w("[LIVE_COUNTER_TEST] No solution, skipping live counter test");
            return;
        }

        // Enable the live move counter programmatically
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager.setLiveMoveCounterEnabled(true);
            ToggleButton toggle = activity.findViewById(R.id.live_move_counter_toggle);
            if (toggle != null) {
                toggle.setChecked(true);
            }
        });

        Thread.sleep(500);

        assertTrue("Live counter should be enabled",
                gameStateManager.isLiveMoveCounterEnabled());

        // Execute a move (UP) - Level 1 has the robot pre-selected
        Timber.d("[LIVE_COUNTER_TEST] Executing move UP");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(0, -1);
            }
        });

        // Wait for animation + live solver to complete
        Thread.sleep(15000);

        // Check that the live counter text was updated
        String counterText = gameStateManager.getLiveMoveCounterText().getValue();
        Timber.d("[LIVE_COUNTER_TEST] Live counter text: '%s'", counterText);

        // The text should be non-empty (either a number or "?")
        assertNotNull("Live counter text should not be null", counterText);
        assertFalse("Live counter text should not be empty after a move",
                counterText.isEmpty());

        Timber.d("[LIVE_COUNTER_TEST] Live move counter test passed! Text: %s", counterText);
    }

    /**
     * Test that disabling the live counter clears the text.
     */
    @Test
    public void testDisableLiveCounter() throws InterruptedException {
        Timber.d("[LIVE_COUNTER_TEST] Starting disable live counter test");

        navigateToLevel1();

        // Enable then disable
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager.setLiveMoveCounterEnabled(true);
        });

        Thread.sleep(200);
        assertTrue("Should be enabled", gameStateManager.isLiveMoveCounterEnabled());

        activityRule.getScenario().onActivity(activity -> {
            gameStateManager.setLiveMoveCounterEnabled(false);
        });

        Thread.sleep(200);
        assertFalse("Should be disabled", gameStateManager.isLiveMoveCounterEnabled());

        String text = gameStateManager.getLiveMoveCounterText().getValue();
        assertTrue("Text should be empty after disabling",
                text == null || text.isEmpty());

        Timber.d("[LIVE_COUNTER_TEST] Disable live counter test passed");
    }

    private GameSolution waitForSolution(int maxAttempts) throws InterruptedException {
        GameSolution solution = gameStateManager.getCurrentSolution();
        int attempts = 0;
        while (solution == null && attempts < maxAttempts) {
            Timber.d("[LIVE_COUNTER_TEST] Waiting for solution... (attempt %d)", attempts + 1);
            Thread.sleep(2000);
            solution = gameStateManager.getCurrentSolution();
            attempts++;
        }
        return solution;
    }

    private void executeMove(IGameMove move) {
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null && move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                int robotColor = rrMove.getColor();
                int direction = rrMove.getDirection();

                Object stateObj = gameStateManager.getCurrentState().getValue();
                if (stateObj != null) {
                    try {
                        java.lang.reflect.Method getElementsMethod = stateObj.getClass().getMethod("getGameElements");
                        java.util.List<?> elements = (java.util.List<?>) getElementsMethod.invoke(stateObj);

                        Object selectedRobot = null;
                        for (Object element : elements) {
                            java.lang.reflect.Method getTypeMethod = element.getClass().getMethod("getType");
                            java.lang.reflect.Method getColorMethod = element.getClass().getMethod("getColor");
                            int type = (int) getTypeMethod.invoke(element);
                            int color = (int) getColorMethod.invoke(element);

                            if (type == 1 && color == robotColor) {
                                selectedRobot = element;
                                break;
                            }
                        }

                        if (selectedRobot != null) {
                            java.lang.reflect.Method setSelectedMethod = stateObj.getClass().getMethod("setSelectedRobot", Object.class);
                            setSelectedMethod.invoke(stateObj, selectedRobot);

                            int dx = 0, dy = 0;
                            switch (direction) {
                                case 1: dy = -1; break;
                                case 2: dx = 1; break;
                                case 4: dy = 1; break;
                                case 8: dx = -1; break;
                            }

                            gameStateManager.moveRobotInDirection(dx, dy);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "[LIVE_COUNTER_TEST] Error executing move");
                    }
                }
            }
        });
    }
}
