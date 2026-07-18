package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;
import roboyard.logic.managers.GameStateManager;
import roboyard.ui.activities.MainActivity;

/**
 * Regression test for rapid back press during robot movement.
 *
 * Ensures the in-game back button is disabled for 1 second after a move starts.
 */
@RunWith(AndroidJUnit4.class)
public class BackButtonMoveCooldownTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() {
        TestHelper.closeAchievementPopupIfPresent();
        TestHelper.startLevelGame(activityRule, 1);

        activityRule.getScenario().onActivity(activity -> gameStateManager = activity.getGameStateManager());
    }

    private boolean selectBlueAndMove(int dx, int dy) {
        final boolean[] result = {false};
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) return;
            for (GameElement el : state.gameElements) {
                if (el.type == GameElement.TYPE_ROBOT && el.color == Constants.COLOR_BLUE) {
                    state.setSelectedRobot(el);
                    break;
                }
            }
            result[0] = gameStateManager.moveRobotInDirection(dx, dy);
        });
        return result[0];
    }

    @Test
    public void backButtonIsTemporarilyDisabledAfterMoveStarts() throws Exception {
        boolean moved = selectBlueAndMove(-1, 0);
        assertTrue("Expected initial move to succeed", moved);

        final int[] moveCountAfterMove = {-1};
        activityRule.getScenario().onActivity(activity ->
                moveCountAfterMove[0] = gameStateManager.getMoveCount().getValue());

        onView(withId(R.id.back_button)).check(matches(not(isEnabled())));

        Thread.sleep(1200);
        onView(withId(R.id.back_button)).check(matches(isEnabled())).perform(click());
        Thread.sleep(250);

        final int[] moveCountAfterUndo = {-1};
        activityRule.getScenario().onActivity(activity ->
                moveCountAfterUndo[0] = gameStateManager.getMoveCount().getValue());

        assertTrue("Undo should reduce move count after cooldown",
                moveCountAfterUndo[0] < moveCountAfterMove[0]);
    }
}
