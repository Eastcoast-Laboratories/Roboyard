package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import java.util.List;

import roboyard.eclabs.R;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.FileReadWrite;
import roboyard.ui.components.GameHistoryManager;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Global test helper class with common test methods.
 * Use these methods in all new tests instead of reinventing the wheel.
 * 
 * Available methods:
 * - startNewSessionWithEmptyStorage() - Clear all history and start fresh
 * - startAndWait8sForPopupClose() - Wait for achievement/streak popup to close
 * - startRandomGame() - Click "New Random Game" button
 * - startLevelGame(levelId) - Start a specific level
 * - openDebugScreen() - Navigate to Debug Settings via long press
 * - openLevelEditorThroughDebug() - Open Level Editor via Debug Settings
 * - openSettingsAndScrollDown() - Open Settings and scroll to bottom
 * - navigateToSaveLoadScreen() - Navigate to Save/Load screen
 * - navigateToHistoryTab() - Switch to History tab in Save/Load
 * - closeAchievementPopupIfPresent() - Close achievement popup programmatically
 *
 * Tags: test-helper, espresso, navigation, setup, common-methods
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

    // ==================== COMMON TEST METHODS ====================

    /**
     * Clear all history and start with empty storage.
     * Use this at the beginning of tests that need a clean state.
     */
    public static void startNewSessionWithEmptyStorage(Activity activity) throws InterruptedException {
        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        for (GameHistoryEntry e : entries) {
            GameHistoryManager.deleteHistoryEntry(activity, e.getMapPath());
        }
        FileReadWrite.writePrivateData(activity, "history_index.json", "{\"historyEntries\":[]}");
        Timber.d("[TEST_HELPER] History cleared (%d entries removed)", entries.size());
        Thread.sleep(500);
    }

    /**
     * Wait 8 seconds for achievement/streak popup to close.
     * Use this after starting a test to ensure popups don't interfere.
     */
    public static void startAndWait8sForPopupClose() throws InterruptedException {
        Timber.d("[TEST_HELPER] Waiting 8 seconds for popup to close");
        Thread.sleep(8000);
    }

    /**
     * Start a random game by clicking the "New Random Game" button.
     * Verifies that the game grid is displayed.
     */
    public static void startRandomGame() throws InterruptedException {
        Timber.d("[TEST_HELPER] Starting random game");
        onView(withId(R.id.ui_button)).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));
    }

    /**
     * Start a specific level game via GameStateManager.
     * @param activityRule The activity scenario rule
     * @param levelId The level ID to start (1-140)
     */
    public static void startLevelGame(ActivityScenarioRule<MainFragmentActivity> activityRule, int levelId) {
        Timber.d("[TEST_HELPER] Starting level %d", levelId);
        activityRule.getScenario().onActivity(a -> {
            GameStateManager gameStateManager = a.getGameStateManager();
            gameStateManager.startLevelGame(levelId);
            roboyard.ui.fragments.GameFragment gameFragment = new roboyard.ui.fragments.GameFragment();
            a.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, gameFragment)
                    .commit();
        });
    }

    /**
     * Open Debug Settings screen via long press on settings title.
     * Requires being in Settings screen first.
     */
    public static void openDebugScreen() throws InterruptedException {
        Timber.d("[TEST_HELPER] Opening Debug Settings");
        // Click settings icon
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);
        
        // Long press on title for 3.5 seconds
        onView(withId(R.id.title_text)).perform(longPressFor(3500));
        Thread.sleep(1000);
        
        // Verify we're in Debug Settings
        onView(withText("DEBUG SETTINGS")).check(matches(isDisplayed()));
    }

    /**
     * Open Level Editor through Debug Settings.
     * Assumes we're starting from main menu.
     */
    public static void openLevelEditorThroughDebug() throws InterruptedException {
        Timber.d("[TEST_HELPER] Opening Level Editor through Debug Settings");
        openDebugScreen();
        
        // Scroll to and click Level Editor button
        onView(withText("Open Level Editor")).perform(scrollTo(), click());
        Thread.sleep(2000);
    }

    /**
     * Open Settings screen and scroll to bottom.
     */
    public static void openSettingsAndScrollDown() throws InterruptedException {
        Timber.d("[TEST_HELPER] Opening Settings and scrolling down");
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);
        
        // Scroll to bottom (try to find a view that's typically at the bottom)
        try {
            onView(withText("Version")).perform(scrollTo());
        } catch (Exception e) {
            Timber.d("[TEST_HELPER] Could not scroll to Version text");
        }
        Thread.sleep(500);
    }

    /**
     * Navigate to Save/Load screen.
     * @param activityRule The activity scenario rule
     */
    public static void navigateToSaveLoadScreen(ActivityScenarioRule<MainFragmentActivity> activityRule) {
        Timber.d("[TEST_HELPER] Navigating to Save/Load screen");
        activityRule.getScenario().onActivity(a -> {
            roboyard.ui.fragments.SaveGameFragment fragment = new roboyard.ui.fragments.SaveGameFragment();
            a.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit();
        });
    }

    /**
     * Switch to History tab in Save/Load screen.
     * Assumes we're already in Save/Load screen.
     */
    public static void navigateToHistoryTab() throws InterruptedException {
        Timber.d("[TEST_HELPER] Switching to History tab");
        onView(withText("History")).perform(click());
        Thread.sleep(2000);
    }

    /**
     * Custom long press action with configurable duration.
     */
    private static androidx.test.espresso.ViewAction longPressFor(final long durationMs) {
        return new androidx.test.espresso.ViewAction() {
            @Override
            public org.hamcrest.Matcher<android.view.View> getConstraints() {
                return androidx.test.espresso.matcher.ViewMatchers.isDisplayed();
            }
            
            @Override
            public String getDescription() {
                return "Long press for " + durationMs + " milliseconds";
            }
            
            @Override
            public void perform(androidx.test.espresso.UiController uiController, android.view.View view) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                float x = location[0] + view.getWidth() / 2f;
                float y = location[1] + view.getHeight() / 2f;
                
                long downTime = android.os.SystemClock.uptimeMillis();
                android.view.MotionEvent downEvent = android.view.MotionEvent.obtain(
                        downTime, downTime, android.view.MotionEvent.ACTION_DOWN, x, y, 0);
                view.dispatchTouchEvent(downEvent);
                
                uiController.loopMainThreadForAtLeast(durationMs);
                
                long upTime = android.os.SystemClock.uptimeMillis();
                android.view.MotionEvent upEvent = android.view.MotionEvent.obtain(
                        downTime, upTime, android.view.MotionEvent.ACTION_UP, x, y, 0);
                view.dispatchTouchEvent(upEvent);
                
                downEvent.recycle();
                upEvent.recycle();
            }
        };
    }
    
    /**
     * Set hint auto-move mode via Settings UI
     * @param mode 0=Manual, 1=Full-Auto, 2=Semi-Auto
     * @throws InterruptedException if thread is interrupted
     */
    public static void setHintAutoMoveMode(int mode) throws InterruptedException {
        Timber.d("[TEST_HELPER] Setting hint auto-move mode to %d via Settings UI", mode);
        
        // Open Settings
        openSettingsAndScrollDown();
        Thread.sleep(1000);
        
        // Scroll to hint auto-move section
        onView(withId(R.id.hint_auto_move_radio_group)).perform(scrollTo());
        Thread.sleep(500);
        
        // Click the appropriate radio button
        switch (mode) {
            case 0: // Manual
                onView(withId(R.id.hint_auto_move_manual)).perform(click());
                Timber.d("[TEST_HELPER] Clicked Manual mode");
                break;
            case 1: // Full-Auto
                onView(withId(R.id.hint_auto_move_full_auto)).perform(click());
                Timber.d("[TEST_HELPER] Clicked Full-Auto mode");
                break;
            case 2: // Semi-Auto
                onView(withId(R.id.hint_auto_move_semi_auto)).perform(click());
                Timber.d("[TEST_HELPER] Clicked Semi-Auto mode");
                break;
        }
        
        Thread.sleep(500);
        
        // Go back to main screen
        pressBack();
        Thread.sleep(1000);
        
        Timber.d("[TEST_HELPER] Hint auto-move mode set to %d", mode);
    }

    /**
     * Set difficulty via Settings UI.
     * Opens Settings, scrolls to difficulty section, clicks the appropriate radio button, goes back.
     * @param difficultyResId R.id of the difficulty radio button (e.g. R.id.difficulty_impossible)
     * @throws InterruptedException if thread is interrupted
     */
    public static void setDifficulty(int difficultyResId) throws InterruptedException {
        Timber.d("[TEST_HELPER] Setting difficulty via Settings UI");
        
        openSettingsAndScrollDown();
        Thread.sleep(1000);
        
        onView(withId(difficultyResId)).perform(scrollTo(), click());
        Timber.d("[TEST_HELPER] Clicked difficulty radio button");
        Thread.sleep(500);
        
        pressBack();
        Thread.sleep(1000);
        
        Timber.d("[TEST_HELPER] Difficulty set successfully");
    }

    /**
     * Enable Multi-Target game mode via Settings UI.
     * Opens Settings, clicks Multi-Target radio button, sets robot count, goes back.
     * @param robotCount number of robots that must reach targets (1-4)
     * @throws InterruptedException if thread is interrupted
     */
    public static void setMultiTargetMode(int robotCount) throws InterruptedException {
        Timber.d("[TEST_HELPER] Enabling Multi-Target mode with %d robots via Settings UI", robotCount);
        
        openSettingsAndScrollDown();
        Thread.sleep(1000);
        
        // Scroll to and click Multi-Target Mode
        onView(withId(R.id.multi_target_game_mode)).perform(scrollTo(), click());
        Timber.d("[TEST_HELPER] Clicked Multi-Target game mode");
        Thread.sleep(1000);
        
        // Scroll to robot count spinner and set value
        onView(withId(R.id.robot_count_spinner)).perform(scrollTo());
        Thread.sleep(500);
        
        // Set robot count programmatically (spinner index = robotCount - 1)
        final int spinnerIndex = Math.max(0, robotCount - 1);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Activity activity = getActivityFromInstrumentation();
            if (activity != null) {
                android.widget.Spinner spinner = activity.findViewById(R.id.robot_count_spinner);
                if (spinner != null) {
                    spinner.setSelection(spinnerIndex);
                    Timber.d("[TEST_HELPER] Robot count spinner set to index %d (= %d robots)", spinnerIndex, robotCount);
                }
            }
        });
        Thread.sleep(1000);
        
        pressBack();
        Thread.sleep(1000);
        
        Timber.d("[TEST_HELPER] Multi-Target mode enabled with %d robots", robotCount);
    }

    /**
     * Set Standard game mode via Settings UI.
     * Opens Settings, clicks Standard radio button, goes back.
     * @throws InterruptedException if thread is interrupted
     */
    public static void setStandardGameMode() throws InterruptedException {
        Timber.d("[TEST_HELPER] Setting Standard game mode via Settings UI");
        
        openSettingsAndScrollDown();
        Thread.sleep(1000);
        
        onView(withId(R.id.standard_game_mode)).perform(scrollTo(), click());
        Timber.d("[TEST_HELPER] Clicked Standard game mode");
        Thread.sleep(500);
        
        pressBack();
        Thread.sleep(1000);
        
        Timber.d("[TEST_HELPER] Standard game mode set");
    }
}
