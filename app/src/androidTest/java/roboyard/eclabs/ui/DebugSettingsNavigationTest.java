package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
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
 * Espresso UI test for Debug Settings navigation flow:
 * Main Menu -> Settings -> 3s longpress -> Debug Settings -> Back gesture -> Settings
 */
@RunWith(AndroidJUnit4.class)
public class DebugSettingsNavigationTest {
    
    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);
    
    @Test
    public void testDebugSettingsNavigationFlow() throws InterruptedException {
        Timber.d("[TEST] Starting Debug Settings navigation test");
        
        // Step 1: Navigate to Settings from Main Menu
        Timber.d("[TEST] Step 1: Clicking Settings button");
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);
        
        // Verify we're in Settings
        onView(withId(R.id.title_text)).check(matches(isDisplayed()));
        Timber.d("[TEST] Settings screen displayed");
        
        // Step 2: Long press on title for 3+ seconds to open Debug Settings
        Timber.d("[TEST] Step 2: Long pressing title for 3.5 seconds");
        onView(withId(R.id.title_text)).perform(longPressFor(3500));
        Thread.sleep(1000);
        
        // Step 3: Verify Debug Settings is displayed
        Timber.d("[TEST] Step 3: Verifying Debug Settings is displayed");
        onView(withText("DEBUG SETTINGS")).check(matches(isDisplayed()));
        onView(withText("STREAK TESTING")).check(matches(isDisplayed()));
        Timber.d("[TEST] Debug Settings screen displayed");
        
        // Step 4: Press back to return to Settings
        Timber.d("[TEST] Step 4: Pressing back to return to Settings");
        pressBack();
        Thread.sleep(1000);
        
        // Step 5: Verify we're back in Settings
        Timber.d("[TEST] Step 5: Verifying we are back in Settings");
        onView(withId(R.id.title_text)).check(matches(isDisplayed()));
        Timber.d("[TEST] Back in Settings screen");
        
        Timber.d("[TEST] DEBUG SETTINGS NAVIGATION TEST PASSED");
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
                // Get view center coordinates
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                float x = location[0] + view.getWidth() / 2f;
                float y = location[1] + view.getHeight() / 2f;
                
                // Create and dispatch touch down event
                long downTime = android.os.SystemClock.uptimeMillis();
                MotionEvent downEvent = MotionEvent.obtain(
                        downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
                view.dispatchTouchEvent(downEvent);
                
                // Wait for the specified duration
                uiController.loopMainThreadForAtLeast(durationMs);
                
                // Create and dispatch touch up event
                long upTime = android.os.SystemClock.uptimeMillis();
                MotionEvent upEvent = MotionEvent.obtain(
                        downTime, upTime, MotionEvent.ACTION_UP, x, y, 0);
                view.dispatchTouchEvent(upEvent);
                
                // Clean up
                downEvent.recycle();
                upEvent.recycle();
            }
        };
    }
}
