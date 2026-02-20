package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainFragmentActivity;
import timber.log.Timber;

/**
 * Espresso UI test for the Alternative Layout feature.
 * Verifies that the alt layout uses correct widget types (Button/ToggleButton)
 * so ModernGameFragment works without any code changes.
 *
 * Flow:
 * 1. Main Menu -> Settings -> 3.5s longpress -> Debug Settings
 * 2. Enable "ALT LAYOUT" toggle
 * 3. Back -> Main Menu -> Start Random Game
 * 4. Verify game screen with alt layout buttons visible
 * 5. Cleanup: disable alt layout after test
 */
@RunWith(AndroidJUnit4.class)
public class AlternativeLayoutTest {

    private static final String PREFS_NAME = "roboyard_prefs";
    private static final String KEY_ALT_LAYOUT = "use_alternative_layout";

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    @After
    public void tearDown() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ALT_LAYOUT, false).apply();
        Timber.d("[TEST_ALT_LAYOUT] Teardown: reset use_alternative_layout to false");
    }

    @Test
    public void testAlternativeLayoutButtonsAndToggle() throws InterruptedException {
        Timber.d("[TEST_ALT_LAYOUT] Starting alternative layout test");

        // Step 1: Navigate to Settings
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.title_text)).check(matches(isDisplayed()));

        // Step 2: Long press title 3.5s to open Debug Settings
        onView(withId(R.id.title_text)).perform(longPressFor(3500));
        Thread.sleep(1000);

        // Step 3: Verify Debug Settings with UI DESIGN section
        onView(withText("DEBUG SETTINGS")).check(matches(isDisplayed()));
        onView(withText("UI DESIGN")).check(matches(isDisplayed()));

        // Step 4: Click the ALT LAYOUT toggle - scroll to it first
        onView(withText(containsString("ALT LAYOUT"))).perform(scrollTo(), click());
        Thread.sleep(500);

        // Verify via SharedPreferences (avoids allCaps/scroll issues with Button text)
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean altLayoutEnabled = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ALT_LAYOUT, false);
        if (!altLayoutEnabled) {
            throw new AssertionError("[TEST_ALT_LAYOUT] ALT LAYOUT preference was not set to true after clicking toggle");
        }
        Timber.d("[TEST_ALT_LAYOUT] ALT LAYOUT is now ON (verified via SharedPreferences)");

        // Step 5: Back to Settings, then back to Main Menu
        pressBack();
        Thread.sleep(500);
        pressBack();
        Thread.sleep(500);

        // Step 6: Start a Random Game
        onView(withId(R.id.modern_ui_button)).perform(click());
        Thread.sleep(2000);

        // Step 7: Verify game screen with game_grid_view
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));
        Timber.d("[TEST_ALT_LAYOUT] game_grid_view displayed in alt layout");

        // Step 8: Verify bottom bar buttons (hint|back|new_map|save|reset)
        onView(withId(R.id.hint_button)).check(matches(isDisplayed()));
        onView(withId(R.id.back_button)).check(matches(isDisplayed()));
        onView(withId(R.id.new_map_button)).check(matches(isDisplayed()));
        onView(withId(R.id.save_map_button)).check(matches(isDisplayed()));
        onView(withId(R.id.reset_robots_button)).check(matches(isDisplayed()));
        Timber.d("[TEST_ALT_LAYOUT] All bottom bar buttons visible");

        // Step 9: Eye-toggle (hot/cold) must be visible in top bar
        onView(withId(R.id.live_move_counter_toggle)).check(matches(isDisplayed()));
        Timber.d("[TEST_ALT_LAYOUT] Eye-toggle visible in top bar");

        // Step 10: Move counter visible
        onView(withId(R.id.move_count_text)).check(matches(isDisplayed()));
        Timber.d("[TEST_ALT_LAYOUT] Move counter visible");

        Timber.d("[TEST_ALT_LAYOUT] ALTERNATIVE LAYOUT TEST PASSED");
    }

    /**
     * Custom ViewAction for long press with specific duration.
     * Reused from LevelEditorDebugTest / DebugSettingsNavigationTest.
     */
    private static ViewAction longPressFor(final long durationMs) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Long press for " + durationMs + " milliseconds";
            }

            @Override
            public void perform(UiController uiController, View view) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                float x = location[0] + view.getWidth() / 2f;
                float y = location[1] + view.getHeight() / 2f;

                long downTime = android.os.SystemClock.uptimeMillis();
                MotionEvent downEvent = MotionEvent.obtain(
                        downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
                view.dispatchTouchEvent(downEvent);

                uiController.loopMainThreadForAtLeast(durationMs);

                long upTime = android.os.SystemClock.uptimeMillis();
                MotionEvent upEvent = MotionEvent.obtain(
                        downTime, upTime, MotionEvent.ACTION_UP, x, y, 0);
                view.dispatchTouchEvent(upEvent);

                downEvent.recycle();
                upEvent.recycle();
            }
        };
    }
}
