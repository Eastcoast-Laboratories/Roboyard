package roboyard.eclabs.ui;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainActivity;
import timber.log.Timber;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * E2E test: Save and Load a random game.
 *
 * Flow: Main Menu -> Play (random game) -> Save Map -> Slot 1 -> back to game ->
 *       Menu -> Load Game -> Slot 1 -> verify game loaded correctly.
 *
 * Verifies:
 * - Game can be saved to a slot without errors
 * - Saved slot does NOT show red cross (target validation)
 * - Saved slot name text does NOT contain error indicator
 * - Game can be loaded from that slot
 * - Loaded game displays the game grid
 * - Level name matches between save and load
 *
 * Tags: save, load, serialization, target-validation, game-state, random-game
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SaveLoadE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        Timber.d("[SAVE_LOAD_E2E] Starting SaveLoad E2E test");
        TestHelper.closeAchievementPopupIfPresent();
    }

    @Test
    public void testSaveAndLoadRandomGame() throws InterruptedException {
        // Step 1: Start random game
        Timber.d("[SAVE_LOAD_E2E] Step 1: Click Play to start random game");
        onView(allOf(withId(R.id.ui_button), isDisplayed())).perform(click());
        Thread.sleep(2000);

        // Step 2: Verify game grid
        Timber.d("[SAVE_LOAD_E2E] Step 2: Verify game grid is displayed");
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));

        // Step 3: Read map name before saving
        final String[] mapNameBefore = new String[1];
        onView(withId(R.id.unique_map_id_text)).check((view, noViewFoundException) -> {
            if (view instanceof TextView) {
                mapNameBefore[0] = ((TextView) view).getText().toString();
                Timber.d("[SAVE_LOAD_E2E] Map name before save: '%s'", mapNameBefore[0]);
            }
        });

        // Step 4: Wait for solver to finish so save button is enabled
        Timber.d("[SAVE_LOAD_E2E] Step 3: Waiting for solver to finish");
        Thread.sleep(3000);

        // Step 5: Click Save Map button
        Timber.d("[SAVE_LOAD_E2E] Step 4: Click Save Map button");
        onView(allOf(withId(R.id.save_map_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        // Step 6: Verify save screen
        Timber.d("[SAVE_LOAD_E2E] Step 5: Verify save screen is displayed");
        onView(withId(R.id.save_slot_recycler_view)).check(matches(isDisplayed()));

        // Step 7: Save to Slot 1 (index 1, since index 0 is auto-save)
        Timber.d("[SAVE_LOAD_E2E] Step 6: Click on Slot 1 to save");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        Thread.sleep(1500);

        // Step 8: After saving, fragment switches to load mode.
        // Verify the saved slot does NOT have a red cross (no target validation error).
        Timber.d("[SAVE_LOAD_E2E] Step 7: Verify saved slot has no red cross error indicator");
        verifySavedSlotHasNoRedCross(1);

        // Step 9: Press back to return to game
        Timber.d("[SAVE_LOAD_E2E] Step 8: Press back to return to game");
        pressBack();
        Thread.sleep(1000);

        // Step 10: Go back to main menu
        Timber.d("[SAVE_LOAD_E2E] Step 9: Click Menu button to return to main menu");
        onView(allOf(withId(R.id.menu_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        TestHelper.closeAchievementPopupIfPresent();

        // Step 11: Click Load Game
        Timber.d("[SAVE_LOAD_E2E] Step 10: Click Load Game button");
        onView(allOf(withId(R.id.load_game_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        // Step 12: Verify load screen and no red cross on Slot 1
        Timber.d("[SAVE_LOAD_E2E] Step 11: Verify load screen and slot integrity");
        onView(withId(R.id.save_slot_recycler_view)).check(matches(isDisplayed()));
        verifySavedSlotHasNoRedCross(1);

        // Step 13: Load from Slot 1
        Timber.d("[SAVE_LOAD_E2E] Step 12: Click on Slot 1 to load");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        Thread.sleep(2000);

        // Step 14: Verify game grid displayed after load
        Timber.d("[SAVE_LOAD_E2E] Step 13: Verify game grid is displayed after load");
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));

        // Step 15: Verify map name matches
        final String[] mapNameAfter = new String[1];
        onView(withId(R.id.unique_map_id_text)).check((view, noViewFoundException) -> {
            if (view instanceof TextView) {
                mapNameAfter[0] = ((TextView) view).getText().toString();
                Timber.d("[SAVE_LOAD_E2E] Map name after load: '%s'", mapNameAfter[0]);
            }
        });

        assertNotNull("Map name before save should not be null", mapNameBefore[0]);
        assertNotNull("Map name after load should not be null", mapNameAfter[0]);
        assertEquals("Map name must match after save/load cycle", mapNameBefore[0], mapNameAfter[0]);

        Timber.d("[SAVE_LOAD_E2E] PASSED: game saved and loaded, map name matches: '%s'", mapNameBefore[0]);
    }

    /**
     * Verify that a saved slot in the RecyclerView does NOT show the red cross error indicator.
     * The red cross appears when hasTargets() returns false, indicating a corrupt save.
     *
     * @param position The position in the RecyclerView (0=autosave, 1=slot1, etc.)
     */
    private void verifySavedSlotHasNoRedCross(int position) {
        onView(withId(R.id.save_slot_recycler_view)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            assertNotNull("ViewHolder for position " + position + " should exist", holder);

            TextView nameText = holder.itemView.findViewById(R.id.name_text);
            assertNotNull("name_text should exist in slot view", nameText);

            String text = nameText.getText().toString();
            int textColor = nameText.getCurrentTextColor();

            Timber.d("[SAVE_LOAD_E2E] Slot %d: text='%s', textColor=%d (red=%d)",
                    position, text, textColor, Color.RED);

            assertFalse(
                    "Save slot should NOT contain error indicator. Got: '" + text + "'",
                    text.contains("\u274C"));
            assertFalse(
                    "Save slot text should NOT be red (indicates missing targets). Text: '" + text + "'",
                    textColor == Color.RED);
        });
    }
}
