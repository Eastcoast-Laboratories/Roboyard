package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.eclabs.ui.mock.MockMainActivity;

/**
 * UI test for the MainMenuFragment
 */
@RunWith(AndroidJUnit4.class)
public class MainMenuFragmentTest {

    @Rule
    public ActivityScenarioRule<MockMainActivity> activityRule =
            new ActivityScenarioRule<>(MockMainActivity.class);
    
    private MockMainActivity activity;

    @Before
    public void setUp() {
        // Get the activity and load the MainMenuFragment
        activityRule.getScenario().onActivity(activity -> {
            this.activity = activity;
            activity.loadFragment(new MainMenuFragment());
        });
    }

    /**
     * Test that the main menu UI elements are displayed with the correct labels
     */
    @Test
    public void testMainMenuElementsDisplayed() {
        // Verify that all buttons are displayed with correct text
        onView(withId(R.id.new_game_button))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.new_game)));

        onView(withId(R.id.load_game_button))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.load_game)));

        onView(withId(R.id.settings_button))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.settings)));

        onView(withId(R.id.help_button))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.help)));

        onView(withId(R.id.exit_button))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.exit)));
    }
    
    /**
     * Test navigation to the game screen when clicking 'New Game'
     */
    @Test
    public void testNewGameNavigation() {
        // Click the new game button
        onView(withId(R.id.new_game_button)).perform(click());
        
        // Verify that the game state manager was called to start a new game
        // This is simplified since we can't easily verify navigation in this test setup
        // We would need additional test infrastructure to verify fragment transactions
    }
    
    /**
     * Test navigation to load game screen when clicking 'Load Game'
     */
    @Test
    public void testLoadGameNavigation() {
        // Click the load game button
        onView(withId(R.id.load_game_button)).perform(click());
        
        // Similar to above, we would need more test infrastructure to verify navigation
    }
}
