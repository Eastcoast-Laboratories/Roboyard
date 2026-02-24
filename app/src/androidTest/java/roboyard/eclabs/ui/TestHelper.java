package roboyard.eclabs.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import timber.log.Timber;

/**
 * Shared test utilities for Espresso UI tests.
 */
public class TestHelper {

    /**
     * Close achievement popup if visible by programmatically removing it from the view hierarchy.
     * This avoids Espresso click issues that can cause unintended navigation.
     */
    public static void closeAchievementPopupIfPresent() {
        try {
            Thread.sleep(800);
            // Try to find and dismiss the popup by looking for views with high elevation (popup uses Z=1000)
            // The popup is added to the root FrameLayout, so we can find it there
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                try {
                    Activity activity = getActivityFromInstrumentation();
                    if (activity == null) return;
                    
                    ViewGroup rootView = activity.findViewById(android.R.id.content);
                    if (rootView == null) return;
                    
                    boolean removed = removeHighElevationPopups(rootView);
                    
                    if (removed) {
                        // Restore visibility of main menu buttons hidden by popup
                        restoreInvisibleButtons(rootView);
                    }
                } catch (Exception e) {
                    Timber.d("[TEST_HELPER] Error removing popup: %s", e.getMessage());
                }
            });
            Thread.sleep(300);
        } catch (Exception e) {
            Timber.d("[TEST_HELPER] No achievement popup to close: %s", e.getMessage());
        }
    }

    /**
     * Recursively find and remove views with very high elevation (achievement popups use Z=1000).
     * @return true if any popup was removed
     */
    private static boolean removeHighElevationPopups(ViewGroup parent) {
        boolean removed = false;
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            if (child.getElevation() >= 900 || child.getZ() >= 900) {
                Timber.d("[TEST_HELPER] Removing popup view with elevation=%.0f Z=%.0f", 
                    child.getElevation(), child.getZ());
                parent.removeView(child);
                removed = true;
            } else if (child instanceof ViewGroup) {
                if (removeHighElevationPopups((ViewGroup) child)) {
                    removed = true;
                }
            }
        }
        return removed;
    }

    /**
     * Restore visibility of buttons that were hidden by popup (streak/achievement).
     * The popup hides main menu buttons by setting them INVISIBLE.
     */
    private static void restoreInvisibleButtons(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() == View.INVISIBLE && child instanceof android.widget.Button) {
                child.setVisibility(View.VISIBLE);
                Timber.d("[TEST_HELPER] Restored button visibility: %s", child.toString().substring(0, Math.min(80, child.toString().length())));
            }
            if (child instanceof ViewGroup) {
                restoreInvisibleButtons((ViewGroup) child);
            }
        }
    }

    private static Activity getActivityFromInstrumentation() {
        final Activity[] activity = new Activity[1];
        java.util.Collection<Activity> activities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
        if (!activities.isEmpty()) {
            activity[0] = activities.iterator().next();
        }
        return activity[0];
    }
}
