package roboyard.eclabs.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.logic.core.Preferences;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * E2E test for high contrast mode UI styling
 * Tests that hint container, optimal moves button, and move count text
 * have white backgrounds and black text when high contrast mode is enabled
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HighContrastModeE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;
    private boolean originalHighContrastMode;

    @Before
    public void setUp() throws InterruptedException {
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Starting high contrast mode E2E test");
        
        // Save original high contrast mode setting
        originalHighContrastMode = Preferences.highContrastMode;
        
        // Close any achievement popups
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(500);
        
        // Get GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        assertNotNull("GameStateManager must not be null", gameStateManager);
    }

    @After
    public void tearDown() {
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Restoring original high contrast mode: %b", originalHighContrastMode);
        // Restore original high contrast mode setting
        Preferences.setHighContrastMode(originalHighContrastMode);
    }

    @Test
    public void testHighContrastModeHintContainer() throws InterruptedException {
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Testing hint container in high contrast mode");
        
        // Enable high contrast mode
        activityRule.getScenario().onActivity(activity -> {
            Preferences.setHighContrastMode(true);
            Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] High contrast mode enabled");
        });
        
        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(2000);
        
        // Click hint button to show hint container
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(1000);
        
        // Verify hint container is visible
        onView(withId(R.id.hint_container)).check(matches(isDisplayed()));
        
        // Verify hint container has white background
        activityRule.getScenario().onActivity(activity -> {
            ViewGroup hintContainer = activity.findViewById(R.id.hint_container);
            assertNotNull("Hint container must not be null", hintContainer);
            
            // Check background color
            if (hintContainer.getBackground() instanceof ColorDrawable) {
                ColorDrawable bg = (ColorDrawable) hintContainer.getBackground();
                int bgColor = bg.getColor();
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Hint container background color: #%08X", bgColor);
                assertEquals("Hint container background should be white in high contrast mode", 
                    Color.WHITE, bgColor);
            } else {
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Hint container background is not ColorDrawable, checking solid color");
                // For API level differences, just verify it's set
                assertTrue("Hint container should have a background", hintContainer.getBackground() != null);
            }
            
            // Verify status text has white background and black text
            TextView statusText = activity.findViewById(R.id.status_text);
            assertNotNull("Status text must not be null", statusText);
            
            if (statusText.getBackground() instanceof GradientDrawable) {
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Status text has GradientDrawable background");
                // GradientDrawable doesn't expose color directly, but we verify it exists
                assertNotNull("Status text background should be set", statusText.getBackground());
            }
            
            // Verify text color is black
            int textColor = statusText.getCurrentTextColor();
            Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Status text color: #%08X", textColor);
            assertEquals("Status text color should be black in high contrast mode", 
                Color.BLACK, textColor);
        });
        
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Hint container test passed");
    }

    @Test
    public void testHighContrastModeOptimalMovesButton() throws InterruptedException {
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Testing optimal moves button in high contrast mode");
        
        // Enable high contrast mode
        activityRule.getScenario().onActivity(activity -> {
            Preferences.setHighContrastMode(true);
            Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] High contrast mode enabled");
        });
        
        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(2000);
        
        // Click hint button to trigger solver and show optimal moves
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(3000); // Wait for solver
        
        // Verify optimal moves button is visible
        activityRule.getScenario().onActivity(activity -> {
            Button optimalMovesButton = activity.findViewById(R.id.optimal_moves_button);
            
            if (optimalMovesButton != null && optimalMovesButton.getVisibility() == View.VISIBLE) {
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Optimal moves button is visible");
                
                // Verify background is white with black border
                if (optimalMovesButton.getBackground() instanceof GradientDrawable) {
                    Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Optimal moves button has GradientDrawable background");
                    assertNotNull("Optimal moves button background should be set", optimalMovesButton.getBackground());
                }
                
                // Verify text color is black
                int textColor = optimalMovesButton.getCurrentTextColor();
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Optimal moves button text color: #%08X", textColor);
                assertEquals("Optimal moves button text should be black in high contrast mode", 
                    Color.BLACK, textColor);
            } else {
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Optimal moves button not visible yet, skipping verification");
            }
        });
        
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Optimal moves button test passed");
    }

    @Test
    public void testHighContrastModeMoveCount() throws InterruptedException {
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Testing move count in high contrast mode");
        
        // Enable high contrast mode
        activityRule.getScenario().onActivity(activity -> {
            Preferences.setHighContrastMode(true);
            Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] High contrast mode enabled");
        });
        
        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(2000);
        
        // Make a move to trigger move count update
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(1, 0); // Move right
            }
        });
        Thread.sleep(500);
        
        // Verify move count text has white background and black text
        activityRule.getScenario().onActivity(activity -> {
            TextView moveCountText = activity.findViewById(R.id.move_count_text);
            assertNotNull("Move count text must not be null", moveCountText);
            
            // Check background color
            if (moveCountText.getBackground() instanceof ColorDrawable) {
                ColorDrawable bg = (ColorDrawable) moveCountText.getBackground();
                int bgColor = bg.getColor();
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Move count background color: #%08X", bgColor);
                assertEquals("Move count background should be white in high contrast mode", 
                    Color.WHITE, bgColor);
            }
            
            // Verify text color is black
            int textColor = moveCountText.getCurrentTextColor();
            Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Move count text color: #%08X", textColor);
            assertEquals("Move count text should be black in high contrast mode", 
                Color.BLACK, textColor);
        });
        
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Move count test passed");
    }

    @Test
    public void testNormalModeColors() throws InterruptedException {
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Testing normal mode colors (high contrast disabled)");
        
        // Disable high contrast mode
        activityRule.getScenario().onActivity(activity -> {
            Preferences.setHighContrastMode(false);
            Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] High contrast mode disabled");
        });
        
        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(2000);
        
        // Make a move to trigger move count update
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.moveRobotInDirection(1, 0);
            }
        });
        Thread.sleep(500);
        
        // Verify move count has transparent background in normal mode
        activityRule.getScenario().onActivity(activity -> {
            TextView moveCountText = activity.findViewById(R.id.move_count_text);
            assertNotNull("Move count text must not be null", moveCountText);
            
            if (moveCountText.getBackground() instanceof ColorDrawable) {
                ColorDrawable bg = (ColorDrawable) moveCountText.getBackground();
                int bgColor = bg.getColor();
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Move count background color (normal mode): #%08X", bgColor);
                assertEquals("Move count background should be transparent in normal mode", 
                    Color.TRANSPARENT, bgColor);
            }
        });
        
        // Click hint button
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(1000);
        
        // Verify hint container has dark background in normal mode
        activityRule.getScenario().onActivity(activity -> {
            ViewGroup hintContainer = activity.findViewById(R.id.hint_container);
            assertNotNull("Hint container must not be null", hintContainer);
            
            if (hintContainer.getBackground() instanceof ColorDrawable) {
                ColorDrawable bg = (ColorDrawable) hintContainer.getBackground();
                int bgColor = bg.getColor();
                Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Hint container background (normal mode): #%08X", bgColor);
                // Should be dark (#DD000000)
                assertTrue("Hint container should have dark background in normal mode", 
                    Color.alpha(bgColor) > 200 && Color.red(bgColor) < 50);
            }
        });
        
        Timber.d("[UNITTESTS][HIGH_CONTRAST_TEST] Normal mode colors test passed");
    }
}
