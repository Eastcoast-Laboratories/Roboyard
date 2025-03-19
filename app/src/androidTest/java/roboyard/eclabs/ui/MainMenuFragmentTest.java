package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;

/**
 * UI test for the MainMenuFragment
 */
@RunWith(AndroidJUnit4.class)
public class MainMenuFragmentTest {

    private FragmentScenario<MainMenuFragment> fragmentScenario;

    @Before
    public void setUp() {
        // Launch the fragment in isolation
        fragmentScenario = FragmentScenario.launchInContainer(MainMenuFragment.class);
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
}
