package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.MainActivity;
import roboyard.eclabs.R;

/**
 * UI Test for the SaveGameFragment to verify that minimaps are properly displayed in save slots
 * and history entries, addressing previous issues with minimap caching and refreshing.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SaveGameFragmentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;

    @Before
    public void setup() {
        // Get context for string resources
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Navigate to the main menu
        onView(withId(R.id.main_menu_title)).check(matches(isDisplayed()));
    }

    /**
     * Verify that the Save Game screen loads properly
     */
    @Test
    public void testSaveGameScreenLoads() {
        // Click the load game button to navigate to the save game screen
        onView(withId(R.id.load_game_button)).perform(click());
        
        // Verify that the save game screen title is displayed
        onView(withId(R.id.save_game_title))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.save_game_screen_title)));
        
        // Verify that the save slots container is displayed
        onView(withId(R.id.save_slots_container))
                .check(matches(isDisplayed()));
    }

    /**
     * Verify that the save game screen has tabs to switch between save slots and history
     */
    @Test
    public void testSaveGameTabs() {
        // Navigate to save game screen
        onView(withId(R.id.load_game_button)).perform(click());
        
        // Verify that the save slots tab is displayed and selected by default
        onView(withId(R.id.save_slots_tab))
                .check(matches(isDisplayed()));
        
        // Verify that the history tab is displayed
        onView(withId(R.id.history_tab))
                .check(matches(isDisplayed()));
        
        // Click the history tab
        onView(withId(R.id.history_tab)).perform(click());
        
        // Verify that history container is now displayed
        onView(withId(R.id.history_container))
                .check(matches(isDisplayed()));
        
        // Verify that save slots container is now hidden
        onView(withId(R.id.save_slots_container))
                .check(matches(not(isDisplayed())));
    }

    /**
     * Test saving a game and verifying the minimap is displayed
     * This test checks the fix for the issue where minimaps weren't displaying
     * when saving to a new slot
     */
    @Test
    public void testSaveGameWithMinimap() {
        // Start a new game first
        onView(withId(R.id.new_game_button)).perform(click());
        
        // Create a game action to ensure there's game state (like moving a robot)
        // This would vary based on your game implementation
        
        // Navigate to save game screen
        onView(withId(R.id.save_button)).perform(click());
        
        // Use a save slot
        onView(withId(R.id.save_slot_1)).perform(click());
        
        // Verify confirmation dialog appears
        onView(withText(R.string.save_confirm_title))
                .check(matches(isDisplayed()));
        
        // Confirm save
        onView(withId(android.R.id.button1)).perform(click()); // Click "Yes"
        
        // Verify minimap is displayed in the save slot
        // This checks if the minimap cache is properly updated
        onView(withId(R.id.save_slot_1_minimap))
                .check(matches(isDisplayed()));
    }

    /**
     * Test history entries display minimaps correctly
     * This checks the fix for GameButtonGotoHistoryGame minimap issues
     */
    @Test
    public void testHistoryEntriesWithMinimap() {
        // Start a new game
        onView(withId(R.id.new_game_button)).perform(click());
        
        // Make some game moves to generate history
        // Implementation depends on your game logic
        
        // Navigate to save game screen
        onView(withId(R.id.save_button)).perform(click());
        
        // Switch to history tab
        onView(withId(R.id.history_tab)).perform(click());
        
        // Verify history entries are displayed
        onView(withId(R.id.history_list))
                .check(matches(isDisplayed()));
        
        // Verify at least the most recent history entry has a minimap
        onView(withId(R.id.history_entry_1_minimap))
                .check(matches(isDisplayed()));
    }
}
