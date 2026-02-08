package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import android.widget.TextView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E test: Verify that the game timer persists across navigation to menu/save and back.
 * 
 * Steps:
 * 1. Start a random game
 * 2. Wait 5 seconds so timer shows ~00:05
 * 3. Click "Menu" button to navigate away
 * 4. Press back (back gesture) to return to the game
 * 5. Verify timer continues from ~5s (not reset to 00:00)
 */
@RunWith(AndroidJUnit4.class)
public class TimerPersistenceE2ETest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    @Test
    public void testTimerPersistsAfterMenuAndBack() throws InterruptedException {
        Timber.d("[E2E_TIMER] ========== TEST STARTED ==========");

        // Step 1: Navigate to New Game (Random Game)
        Timber.d("[E2E_TIMER] Step 1: Starting random game");
        Thread.sleep(2000);
        // Click New Game button programmatically (streak popup may keep it INVISIBLE)
        activityRule.getScenario().onActivity(activity -> {
            android.widget.Button btn = activity.findViewById(R.id.modern_ui_button);
            if (btn != null) {
                btn.setVisibility(android.view.View.VISIBLE);
                btn.performClick();
                Timber.d("[E2E_TIMER] Clicked New Game button programmatically");
            }
        });
        Thread.sleep(3000);

        // Step 2: Wait 5 seconds for timer to accumulate
        Timber.d("[E2E_TIMER] Step 2: Waiting 5 seconds for timer to accumulate");
        Thread.sleep(5000);

        // Read timer value before navigation
        final String[] timerBefore = new String[1];
        activityRule.getScenario().onActivity(activity -> {
            TextView timerView = activity.findViewById(R.id.game_timer);
            if (timerView != null) {
                timerBefore[0] = timerView.getText().toString();
                Timber.d("[E2E_TIMER] Timer before menu: %s", timerBefore[0]);
            }
        });
        Thread.sleep(200);

        assertNotNull("Timer text should not be null before navigation", timerBefore[0]);
        assertNotEquals("Timer should not be 00:00 after 5 seconds", "00:00", timerBefore[0]);
        Timber.d("[E2E_TIMER] Timer before navigation: %s", timerBefore[0]);

        // Step 3: Click Save Map button to navigate away (uses fragment replace)
        Timber.d("[E2E_TIMER] Step 3: Clicking Save Map button");
        try {
            onView(withId(R.id.save_map_button)).perform(scrollTo(), click());
        } catch (Exception e) {
            // If scrollTo fails, try direct click
            onView(withId(R.id.save_map_button)).perform(click());
        }
        Thread.sleep(1000);

        // Step 4: Press back to return to the game
        Timber.d("[E2E_TIMER] Step 4: Pressing back to return to game");
        androidx.test.espresso.Espresso.pressBack();
        Thread.sleep(2000);

        // Step 5: Read timer value after returning - it should be >= what it was before
        final String[] timerAfter = new String[1];
        activityRule.getScenario().onActivity(activity -> {
            TextView timerView = activity.findViewById(R.id.game_timer);
            if (timerView != null) {
                timerAfter[0] = timerView.getText().toString();
                Timber.d("[E2E_TIMER] Timer after back: %s", timerAfter[0]);
            }
        });
        Thread.sleep(200);

        assertNotNull("Timer text should not be null after returning", timerAfter[0]);
        assertNotEquals("Timer should not be 00:00 after returning from menu", "00:00", timerAfter[0]);

        // Parse timer values to compare (format: mm:ss)
        int secondsBefore = parseTimerSeconds(timerBefore[0]);
        int secondsAfter = parseTimerSeconds(timerAfter[0]);

        Timber.d("[E2E_TIMER] Seconds before: %d, seconds after: %d", secondsBefore, secondsAfter);

        // Timer after should be >= timer before (it continued counting)
        assertTrue("Timer after returning (" + timerAfter[0] + " = " + secondsAfter + "s) should be >= timer before menu (" + 
                timerBefore[0] + " = " + secondsBefore + "s)",
                secondsAfter >= secondsBefore);

        // Timer should be at least 5 seconds (we waited 5s before navigating)
        assertTrue("Timer should show at least 5 seconds after returning, but shows: " + timerAfter[0],
                secondsAfter >= 5);

        Timber.d("[E2E_TIMER] ✓ Timer persisted: before=%s (%ds), after=%s (%ds)",
                timerBefore[0], secondsBefore, timerAfter[0], secondsAfter);
        Timber.d("[E2E_TIMER] ========== TEST PASSED ==========");
    }

    /**
     * Parse timer string "mm:ss" to total seconds
     */
    private int parseTimerSeconds(String timerText) {
        if (timerText == null || !timerText.contains(":")) {
            return 0;
        }
        String[] parts = timerText.split(":");
        try {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
