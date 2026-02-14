package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.achievements.AchievementManager;
import roboyard.eclabs.ui.mock.MockMainActivity;

/**
 * UI tests for the AchievementsFragment.
 * Tests that the achievements screen displays correctly and responds to user interaction.
 * 
 * Run with: ./gradlew connectedAndroidTest --tests "roboyard.ui.fragments.AchievementsFragmentTest"
 */
@RunWith(AndroidJUnit4.class)
public class AchievementsFragmentTest {

    private static final String PREFS_NAME = "roboyard_achievements";

    @Rule
    public ActivityScenarioRule<MockMainActivity> activityRule =
            new ActivityScenarioRule<>(MockMainActivity.class);

    private MockMainActivity activity;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Clear achievements before each test
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        // Load the AchievementsFragment
        activityRule.getScenario().onActivity(activity -> {
            this.activity = activity;
            activity.loadFragment(new AchievementsFragment());
        });
    }

    @After
    public void tearDown() {
        // Clean up
        AchievementManager.getInstance(context).resetAll();
    }

    /**
     * Test that the achievements screen title is displayed
     */
    @Test
    public void testAchievementsTitleDisplayed() {
        onView(withId(R.id.title_text))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.achievements_title)));
    }

    /**
     * Test that the back button is displayed
     */
    @Test
    public void testBackButtonDisplayed() {
        onView(withId(R.id.back_button))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.back_button)));
    }

    /**
     * Test that the progress text is displayed
     */
    @Test
    public void testProgressTextDisplayed() {
        onView(withId(R.id.progress_text))
                .check(matches(isDisplayed()));
    }

    /**
     * Test that the achievements container is displayed
     */
    @Test
    public void testAchievementsContainerDisplayed() {
        onView(withId(R.id.achievements_container))
                .check(matches(isDisplayed()));
    }

    /**
     * Test that progress shows 0/53 initially (no achievements unlocked)
     */
    @Test
    public void testInitialProgressShowsZero() {
        // Progress should show "0 / 53" initially
        onView(withId(R.id.progress_text))
                .check(matches(withText("0 / 53")));
    }

    /**
     * Test that unlocking an achievement updates the progress
     */
    @Test
    public void testUnlockingAchievementUpdatesProgress() {
        // Unlock an achievement
        AchievementManager.getInstance(context).unlock("first_game");
        
        // Reload the fragment to see updated progress
        activityRule.getScenario().onActivity(activity -> {
            activity.loadFragment(new AchievementsFragment());
        });
        
        // Progress should now show "1 / 53"
        onView(withId(R.id.progress_text))
                .check(matches(withText("1 / 53")));
    }

    /**
     * Test back button click navigates back
     */
    @Test
    public void testBackButtonClick() {
        // Click the back button
        onView(withId(R.id.back_button)).perform(click());
        
        // The fragment should be removed (we can't easily verify this in the mock setup)
        // But the click should not crash
    }
}
