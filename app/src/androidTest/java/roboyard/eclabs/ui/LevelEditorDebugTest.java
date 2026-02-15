package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
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
import roboyard.ui.activities.MainFragmentActivity;
import timber.log.Timber;

/**
 * Espresso UI test for Level Editor access via Debug Settings:
 * Main Menu -> Settings -> 3s longpress -> Debug Settings -> Open Level Editor
 * 
 * Also verifies that level files load correctly (level_%d.txt format with underscore).
 */
@RunWith(AndroidJUnit4.class)
public class LevelEditorDebugTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    /**
     * Test that the Level Editor button exists in Debug Settings and opens the editor.
     */
    @Test
    public void testLevelEditorButtonInDebugSettings() throws InterruptedException {
        Timber.d("[TEST_LEVEL_EDITOR] Starting Level Editor debug button test");

        // Step 1: Navigate to Settings
        Timber.d("[TEST_LEVEL_EDITOR] Step 1: Clicking Settings button");
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);

        // Verify Settings screen
        onView(withId(R.id.title_text)).check(matches(isDisplayed()));
        Timber.d("[TEST_LEVEL_EDITOR] Settings screen displayed");

        // Step 2: Long press title for 3.5s to open Debug Settings
        Timber.d("[TEST_LEVEL_EDITOR] Step 2: Long pressing title for 3.5 seconds");
        onView(withId(R.id.title_text)).perform(longPressFor(3500));
        Thread.sleep(1000);

        // Step 3: Verify Debug Settings is displayed with Level Editor section
        Timber.d("[TEST_LEVEL_EDITOR] Step 3: Verifying Debug Settings with Level Editor section");
        onView(withText("DEBUG SETTINGS")).check(matches(isDisplayed()));
        onView(withText("LEVELS")).check(matches(isDisplayed()));
        onView(withText("Open Level Editor")).check(matches(isDisplayed()));
        Timber.d("[TEST_LEVEL_EDITOR] Level Editor section found in Debug Settings");

        // Step 4: Click "Open Level Editor" button
        Timber.d("[TEST_LEVEL_EDITOR] Step 4: Clicking Open Level Editor button");
        onView(withText("Open Level Editor")).perform(click());
        Thread.sleep(2000);

        // Step 5: Verify Level Editor is displayed (check for editor-specific UI elements)
        Timber.d("[TEST_LEVEL_EDITOR] Step 5: Verifying Level Editor is displayed");
        onView(withId(R.id.board_preview_container)).check(matches(isDisplayed()));
        Timber.d("[TEST_LEVEL_EDITOR] Level Editor displayed successfully");

        // Step 6: Press back to return
        Timber.d("[TEST_LEVEL_EDITOR] Step 6: Pressing back");
        pressBack();
        Thread.sleep(1000);

        Timber.d("[TEST_LEVEL_EDITOR] LEVEL EDITOR DEBUG BUTTON TEST PASSED");
    }

    /**
     * Test that the Level Editor can load level 1 without FileNotFoundException.
     * This verifies the fix for the filename format (level_%d.txt instead of level%d.txt).
     */
    @Test
    public void testLevelEditorLoadsLevel1() throws InterruptedException {
        Timber.d("[TEST_LEVEL_EDITOR] Starting Level Editor level loading test");

        // Step 1: Navigate to Settings
        Timber.d("[TEST_LEVEL_EDITOR] Step 1: Clicking Settings button");
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);

        // Step 2: Long press title to open Debug Settings
        Timber.d("[TEST_LEVEL_EDITOR] Step 2: Long pressing title for 3.5 seconds");
        onView(withId(R.id.title_text)).perform(longPressFor(3500));
        Thread.sleep(1000);

        // Step 3: Open Level Editor
        Timber.d("[TEST_LEVEL_EDITOR] Step 3: Opening Level Editor");
        onView(withText("Open Level Editor")).perform(click());
        Thread.sleep(2000);

        // Step 4: Verify the editor loaded with a board (level 1 is loaded by default)
        // The board preview container should be visible and contain the game board
        Timber.d("[TEST_LEVEL_EDITOR] Step 4: Verifying level loaded in editor");
        onView(withId(R.id.board_preview_container)).check(matches(isDisplayed()));

        // Verify the board width/height fields show the correct values for level 1 (10x10)
        onView(withId(R.id.board_width_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.board_height_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.board_width_edit_text)).check(matches(withText("10")));
        onView(withId(R.id.board_height_edit_text)).check(matches(withText("10")));
        Timber.d("[TEST_LEVEL_EDITOR] Level 1 loaded correctly with board size 10x10");

        // Step 5: Press back
        pressBack();
        Thread.sleep(1000);

        Timber.d("[TEST_LEVEL_EDITOR] LEVEL EDITOR LEVEL LOADING TEST PASSED");
    }

    /**
     * Test that the longpress on level selection title no longer opens the editor.
     * The level editor should only be accessible via Debug Settings now.
     */
    @Test
    public void testLongPressOnLevelSelectionTitleDoesNothing() throws InterruptedException {
        Timber.d("[TEST_LEVEL_EDITOR] Starting longpress removal test");

        // Step 1: Navigate to Level Selection (click Level Game button)
        Timber.d("[TEST_LEVEL_EDITOR] Step 1: Clicking Level Game button");
        onView(withId(R.id.level_game_button)).perform(click());
        Thread.sleep(1000);

        // Step 2: Verify Level Selection is displayed
        Timber.d("[TEST_LEVEL_EDITOR] Step 2: Verifying Level Selection screen");
        onView(withId(R.id.level_recycler_view)).check(matches(isDisplayed()));

        // Step 3: Long press on the title - should NOT open Level Editor
        Timber.d("[TEST_LEVEL_EDITOR] Step 3: Long pressing title (should do nothing)");
        onView(withId(R.id.level_selection_title)).perform(longPressFor(2000));
        Thread.sleep(1000);

        // Step 4: Verify we're still on Level Selection (not Level Editor)
        Timber.d("[TEST_LEVEL_EDITOR] Step 4: Verifying still on Level Selection");
        onView(withId(R.id.level_recycler_view)).check(matches(isDisplayed()));
        Timber.d("[TEST_LEVEL_EDITOR] Longpress on title correctly does nothing");

        Timber.d("[TEST_LEVEL_EDITOR] LONGPRESS REMOVAL TEST PASSED");
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
