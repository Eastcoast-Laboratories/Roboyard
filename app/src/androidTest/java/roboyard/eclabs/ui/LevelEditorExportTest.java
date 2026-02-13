package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Espresso test for the Level Editor "Save to Sourcecode" feature.
 * 
 * Prerequisites:
 *   python3 dev/scripts/level_receiver.py   (must be running on host)
 * 
 * Test flow:
 *   Main Menu -> Settings -> 3.5s longpress -> Debug Settings -> Open Level Editor
 *   -> Export Level Text -> verify "Save to Sourcecode" button appears
 *   -> click Save to Sourcecode -> verify success toast
 */
@RunWith(AndroidJUnit4.class)
public class LevelEditorExportTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    @Test
    public void testExportDialogShowsSaveToSourcecodeButton() throws InterruptedException {
        Timber.d("[TEST_EXPORT] Starting Save to Sourcecode button visibility test");

        // Step 1: Navigate to Settings
        Timber.d("[TEST_EXPORT] Step 1: Clicking Settings button");
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);

        // Step 2: Long press title for 3.5s to open Debug Settings
        Timber.d("[TEST_EXPORT] Step 2: Long pressing title for 3.5 seconds");
        onView(withId(R.id.title_text)).perform(longPressFor(3500));
        Thread.sleep(1000);

        // Step 3: Open Level Editor
        Timber.d("[TEST_EXPORT] Step 3: Opening Level Editor");
        onView(withText("Open Level Editor")).perform(click());
        Thread.sleep(2000);

        // Step 4: Verify editor is displayed
        Timber.d("[TEST_EXPORT] Step 4: Verifying Level Editor is displayed");
        onView(withId(R.id.board_preview_container)).check(matches(isDisplayed()));

        // Step 5: Click Export button (needs scroll - button is below visible area)
        Timber.d("[TEST_EXPORT] Step 5: Clicking Export Level Text button");
        onView(withId(R.id.export_level_button)).perform(scrollTo(), click());
        Thread.sleep(1000);

        // Step 6: Verify the Export dialog is displayed (Copy to Clipboard button should be visible)
        Timber.d("[TEST_EXPORT] Step 6: Verifying Export dialog is displayed");
        onView(withText("Copy to Clipboard")).check(matches(isDisplayed()));
        Timber.d("[TEST_EXPORT] Export dialog displayed with Copy to Clipboard button");

        // Step 7: Wait for receiver check (3s timeout + margin)
        Timber.d("[TEST_EXPORT] Step 7: Waiting for receiver check (4 seconds)");
        Thread.sleep(4000);

        // Step 8: Check if Save to Sourcecode button is visible
        // This will only pass if level_receiver.py is running on the host
        try {
            onView(withText("\uD83D\uDCBE Save to Sourcecode")).check(matches(isDisplayed()));
            Timber.d("[TEST_EXPORT] Save to Sourcecode button IS visible - receiver is reachable");

            // Step 9: Wait 4 seconds so the user can see the button on screen
            Timber.d("[TEST_EXPORT] Step 9: Waiting 4 seconds so button is visible on screen");
            Thread.sleep(4000);

            // Step 10: Click Save to Sourcecode
            Timber.d("[TEST_EXPORT] Step 10: Clicking Save to Sourcecode");
            onView(withText("\uD83D\uDCBE Save to Sourcecode")).perform(click());
            Thread.sleep(3000);

            // Step 10: Verify button text changed to success
            Timber.d("[TEST_EXPORT] Step 10: Verifying save succeeded");
            onView(withText("\u2713 Saved to Sourcecode")).check(matches(isDisplayed()));
            Timber.d("[TEST_EXPORT] SAVE TO SOURCECODE TEST PASSED - level saved successfully!");

        } catch (Exception e) {
            Timber.e("[TEST_EXPORT] Save to Sourcecode button NOT visible. Is level_receiver.py running?");
            Timber.e("[TEST_EXPORT] Error: %s", e.getMessage());
            throw new AssertionError(
                "Save to Sourcecode button not visible. " +
                "Make sure level_receiver.py is running: python3 dev/scripts/level_receiver.py\n" +
                "Original error: " + e.getMessage());
        }
    }

    @Test
    public void testExportDialogAlwaysHasCopyButton() throws InterruptedException {
        Timber.d("[TEST_EXPORT] Starting Export dialog basic test");

        // Navigate to Level Editor
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.title_text)).perform(longPressFor(3500));
        Thread.sleep(1000);
        onView(withText("Open Level Editor")).perform(click());
        Thread.sleep(2000);

        // Click Export (needs scroll - button is below visible area)
        onView(withId(R.id.export_level_button)).perform(scrollTo(), click());
        Thread.sleep(1000);

        // Verify basic dialog elements
        onView(withText("Copy to Clipboard")).check(matches(isDisplayed()));
        onView(withText("Level Text Format")).check(matches(isDisplayed()));
        Timber.d("[TEST_EXPORT] EXPORT DIALOG BASIC TEST PASSED");
    }

    /**
     * Custom ViewAction for long press with specific duration
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
