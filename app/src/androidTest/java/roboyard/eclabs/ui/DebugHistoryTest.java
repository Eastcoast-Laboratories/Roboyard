package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.GameHistoryManager;
import timber.log.Timber;

/**
 * Espresso UI test for Debug Screen History functionality:
 * - Navigate to Debug Settings
 * - Create 100 dummy history entries
 * - Verify memory statistics are displayed
 * 
 * Tags: ui, debug, history, espresso
 */
@RunWith(AndroidJUnit4.class)
public class DebugHistoryTest {
    
    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);
    
    @Test
    public void testDebugHistoryCreationAndMemoryStats() throws InterruptedException {
        Timber.d("[DEBUG_TEST] Starting Debug History test");
        
        // Get initial history count
        int[] initialCount = {0};
        activityRule.getScenario().onActivity(activity -> {
            initialCount[0] = GameHistoryManager.getHistoryEntries(activity).size();
            Timber.d("[DEBUG_TEST] Initial history count: %d", initialCount[0]);
        });
        
        // Step 1: Navigate to Settings
        Timber.d("[DEBUG_TEST] Step 1: Navigating to Settings");
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);
        
        // Step 2: Long press on title to access Debug Settings (5 seconds)
        Timber.d("[DEBUG_TEST] Step 2: Long pressing title for 5 seconds");
        onView(withId(R.id.title_text)).perform(longPressFor(5000));
        Thread.sleep(1000);
        
        // Step 3: Verify Debug Settings screen is displayed
        Timber.d("[DEBUG_TEST] Step 3: Verifying Debug Settings screen");
        onView(withText("DEBUG SETTINGS")).check(matches(isDisplayed()));
        
        // Step 4: Scroll to History Testing section
        Timber.d("[DEBUG_TEST] Step 4: Scrolling to History Testing section");
        Thread.sleep(1000);
        
        // Step 5: Click "Add 100 Dummy History Entries" button
        Timber.d("[DEBUG_TEST] Step 5: Clicking Add 100 Dummy History Entries");
        onView(withText("Add 100 Dummy History Entries")).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click());
        Thread.sleep(500);
        
        // Step 6: Confirm dialog
        Timber.d("[DEBUG_TEST] Step 6: Confirming dialog");
        Thread.sleep(1000); // Wait for dialog to appear
        onView(withText("Add")).check(matches(isDisplayed())).perform(click());
        Thread.sleep(2000); // Wait for thread to start
        Timber.d("[DEBUG_TEST] Waiting for entries to be created...");
        Thread.sleep(20000); // Wait for entries to be created (100 entries takes ~15-20 seconds)
        
        // Step 7: Verify entries were created
        Timber.d("[DEBUG_TEST] Step 7: Verifying entries were created");
        int[] finalCount = {0};
        activityRule.getScenario().onActivity(activity -> {
            finalCount[0] = GameHistoryManager.getHistoryEntries(activity).size();
            Timber.d("[DEBUG_TEST] Final history count: %d", finalCount[0]);
            
            int added = finalCount[0] - initialCount[0];
            if (added < 100) {
                throw new AssertionError("Expected 100 entries to be added, but only " + added + " were added");
            }
        });
        
        // Step 8: Scroll to and click Refresh Statistics
        Timber.d("[DEBUG_TEST] Step 8: Refreshing statistics");
        onView(withText("Refresh Statistics")).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click());
        Thread.sleep(2000);
        
        // Step 9: Verify memory stats are updated (should show > 0 entries)
        Timber.d("[DEBUG_TEST] Step 9: Verifying updated memory stats");
        activityRule.getScenario().onActivity(activity -> {
            int historyCount = GameHistoryManager.getHistoryEntries(activity).size();
            Timber.d("[DEBUG_TEST] History entries after refresh: %d", historyCount);
            if (historyCount == 0) {
                throw new AssertionError("Memory stats show 0 entries but " + finalCount[0] + " should exist");
            }
        });
        
        Timber.d("[DEBUG_TEST] DEBUG HISTORY TEST PASSED");
    }
    
    /**
     * Custom long press action with configurable duration
     */
    private static ViewAction longPressFor(long durationMs) {
        return new ViewAction() {
            @Override
            public org.hamcrest.Matcher<android.view.View> getConstraints() {
                return androidx.test.espresso.matcher.ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return "long press for " + durationMs + "ms";
            }

            @Override
            public void perform(androidx.test.espresso.UiController uiController, android.view.View view) {
                float x = view.getWidth() / 2.0f;
                float y = view.getHeight() / 2.0f;

                long startTime = System.currentTimeMillis();
                android.view.MotionEvent down = android.view.MotionEvent.obtain(
                        startTime,
                        startTime,
                        android.view.MotionEvent.ACTION_DOWN,
                        x, y, 0
                );
                view.dispatchTouchEvent(down);
                down.recycle();

                uiController.loopMainThreadForAtLeast(durationMs);

                long endTime = System.currentTimeMillis();
                android.view.MotionEvent up = android.view.MotionEvent.obtain(
                        startTime,
                        endTime,
                        android.view.MotionEvent.ACTION_UP,
                        x, y, 0
                );
                view.dispatchTouchEvent(up);
                up.recycle();
            }
        };
    }
}
