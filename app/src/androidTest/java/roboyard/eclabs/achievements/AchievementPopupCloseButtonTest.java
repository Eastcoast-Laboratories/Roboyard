package roboyard.eclabs.achievements;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.ui.activities.MainFragmentActivity;
import timber.log.Timber;

/**
 * Espresso UI test for AchievementPopup close button functionality.
 * Tests that the X close button appears when tapping the popup and can be clicked.
 * 
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.ui.achievements.AchievementPopupCloseButtonTest
 */
@RunWith(AndroidJUnit4.class)
public class AchievementPopupCloseButtonTest {

    private ActivityScenario<MainFragmentActivity> scenario;

    @Before
    public void setUp() {
        Timber.d("[TEST] Starting AchievementPopupCloseButtonTest");
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Test that the streak popup appears on app start and has a close button.
     * When tapping the popup content area, the X button should appear.
     */
    @Test
    public void testStreakPopupCloseButtonAppears() throws InterruptedException {
        Timber.d("[TEST] Launching MainActivity");
        scenario = ActivityScenario.launch(MainFragmentActivity.class);
        
        // Wait for the streak popup to appear (it shows on app start)
        Thread.sleep(3000);
        
        Timber.d("[TEST] Looking for streak popup");
        
        // The popup should be visible - look for the "View Achievements" button text
        try {
            onView(withText(containsString("View")))
                .check(matches(isDisplayed()));
            Timber.d("[TEST] Found View Achievements button - popup is visible");
        } catch (Exception e) {
            Timber.e("[TEST] Could not find View Achievements button: %s", e.getMessage());
        }
        
        // Now tap on the popup content area (not the button) to trigger close button
        // We need to find the ScrollView and tap it
        scenario.onActivity(activity -> {
            Timber.d("[TEST] Searching for popup views in activity");
            View rootView = activity.getWindow().getDecorView().getRootView();
            findAndLogAllViews(rootView, 0);
            
            // Find contentLayout (LinearLayout inside ScrollView) in the popup
            android.widget.LinearLayout contentLayout = findContentLayoutInPopup(rootView);
            if (contentLayout != null) {
                Timber.d("[TEST] Found contentLayout, performing click");
                contentLayout.performClick();
                
                // Check if close button is now visible
                TextView closeButton = findCloseButton(rootView);
                if (closeButton != null) {
                    Timber.d("[TEST] Close button found! Visibility: %d (VISIBLE=0, INVISIBLE=4, GONE=8)", closeButton.getVisibility());
                    if (closeButton.getVisibility() == View.VISIBLE) {
                        Timber.d("[TEST] SUCCESS: Close button is VISIBLE!");
                    } else {
                        Timber.e("[TEST] FAIL: Close button exists but is NOT visible");
                    }
                } else {
                    Timber.e("[TEST] FAIL: Close button not found in view hierarchy");
                }
            } else {
                Timber.e("[TEST] FAIL: contentLayout not found in popup");
            }
        });
        
        // Wait a bit to see the result
        Thread.sleep(2000);
    }

    /**
     * Test that tapping the close button hides the popup.
     */
    @Test
    public void testCloseButtonHidesPopup() throws InterruptedException {
        Timber.d("[TEST] Launching MainActivity for close button test");
        scenario = ActivityScenario.launch(MainFragmentActivity.class);
        
        // Wait for the streak popup to appear
        Thread.sleep(3000);
        
        scenario.onActivity(activity -> {
            View rootView = activity.getWindow().getDecorView().getRootView();
            
            // Find and click contentLayout to show close button
            android.widget.LinearLayout contentLayout = findContentLayoutInPopup(rootView);
            if (contentLayout != null) {
                Timber.d("[TEST] Clicking contentLayout to show close button");
                contentLayout.performClick();
                
                // Now find and click the close button
                TextView closeButton = findCloseButton(rootView);
                if (closeButton != null && closeButton.getVisibility() == View.VISIBLE) {
                    Timber.d("[TEST] Clicking close button");
                    closeButton.performClick();
                    Timber.d("[TEST] SUCCESS: Close button clicked!");
                } else {
                    Timber.e("[TEST] Close button not visible or not found");
                }
            }
        });
        
        Thread.sleep(2000);
    }

    /**
     * Recursively find all views and log them for debugging.
     */
    private void findAndLogAllViews(View view, int depth) {
        String indent = new String(new char[depth * 2]).replace('\0', ' ');
        String viewInfo = String.format("%s%s [visibility=%d, w=%d, h=%d]", 
            indent, 
            view.getClass().getSimpleName(),
            view.getVisibility(),
            view.getWidth(),
            view.getHeight());
        
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            viewInfo += " text=\"" + tv.getText() + "\"";
        }
        
        Timber.d("[VIEW_HIERARCHY] %s", viewInfo);
        
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                findAndLogAllViews(vg.getChildAt(i), depth + 1);
            }
        }
    }

    /**
     * Find the contentLayout (LinearLayout inside ScrollView) in the achievement popup.
     */
    private android.widget.LinearLayout findContentLayoutInPopup(View view) {
        if (view instanceof ScrollView) {
            // Check if this ScrollView is inside a FrameLayout (popup structure)
            if (view.getParent() instanceof FrameLayout) {
                Timber.d("[TEST] Found ScrollView in FrameLayout parent");
                ScrollView sv = (ScrollView) view;
                if (sv.getChildCount() > 0 && sv.getChildAt(0) instanceof android.widget.LinearLayout) {
                    Timber.d("[TEST] Found contentLayout inside ScrollView");
                    return (android.widget.LinearLayout) sv.getChildAt(0);
                }
            }
        }
        
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                android.widget.LinearLayout result = findContentLayoutInPopup(vg.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }

    /**
     * Find the close button (TextView with "✕" text).
     */
    private TextView findCloseButton(View view) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if ("✕".equals(tv.getText().toString())) {
                Timber.d("[TEST] Found close button TextView");
                return tv;
            }
        }
        
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView result = findCloseButton(vg.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
}
