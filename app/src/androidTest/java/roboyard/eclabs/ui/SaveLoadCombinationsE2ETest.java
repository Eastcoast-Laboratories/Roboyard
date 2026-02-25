package roboyard.eclabs.ui;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * E2E test: Save game in all possible edge-case combinations to reproduce
 * the intermittent "red cross" (❌) bug where targets are lost during save.
 *
 * Tests all save combinations:
 * 1. Fresh random game → Save
 * 2. After viewing hints → Save
 * 3. After activating live move counter → Save
 * 4. After making moves → Save
 * 5. After hints + moves combined → Save
 * 6. Save → Load → Re-save to different slot
 * 7. Save → Load from history tab → Save
 * 8. Level game → Save
 *
 * Each scenario verifies the saved slot does NOT show the red cross indicator.
 *
 * Tags: save, load, serialization, target-validation, game-state, random-game, hints, move-counter, history, level-game, edge-cases
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SaveLoadCombinationsE2ETest {

    private static final String TAG = "[SAVE_COMBO_E2E]";
    private int stepCounter = 0;
    private GameStateManager gameStateManager;

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        stepCounter = 0;
        Timber.d("%s Starting SaveLoadCombinations E2E test", TAG);
        TestHelper.closeAchievementPopupIfPresent();
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
    }

    /**
     * Test 1: Fresh random game → Save immediately (baseline)
     */
    @Test
    public void testSave_freshRandomGame() throws InterruptedException {
        step("Start fresh random game");
        startRandomGame();

        step("Wait for solver");
        Thread.sleep(3000);

        step("Save to slot 1");
        saveToSlot(1);

        step("Verify slot 1 has no red cross");
        verifySavedSlotHasNoRedCross(1);
    }

    /**
     * Test 2: Random game → View hints → Save
     */
    @Test
    public void testSave_afterViewingHints() throws InterruptedException {
        step("Start random game");
        startRandomGame();

        step("Wait for solver to finish");
        Thread.sleep(4000);

        step("Toggle hint button ON to show hints");
        onView(allOf(withId(R.id.hint_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        step("Click next hint to advance hint display");
        try {
            onView(allOf(withId(R.id.next_hint_button), isDisplayed())).perform(click());
            Thread.sleep(500);
            step("Click next hint again");
            onView(allOf(withId(R.id.next_hint_button), isDisplayed())).perform(click());
            Thread.sleep(500);
        } catch (Exception e) {
            Timber.d("%s next_hint_button not visible, continuing with hints toggled on", TAG);
        }

        step("Save to slot 2");
        saveToSlot(2);

        step("Verify slot 2 has no red cross");
        verifySavedSlotHasNoRedCross(2);
    }

    /**
     * Test 3: Random game → Activate live move counter → Save
     */
    @Test
    public void testSave_afterLiveMoveCounter() throws InterruptedException {
        step("Start random game");
        startRandomGame();

        step("Wait for solver to finish");
        Thread.sleep(4000);

        step("Toggle hint button ON to make hint_container visible");
        onView(allOf(withId(R.id.hint_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        step("Toggle live move counter ON");
        try {
            onView(allOf(withId(R.id.live_move_counter_toggle), isDisplayed())).perform(click());
            Thread.sleep(500);
        } catch (Exception e) {
            Timber.d("%s live_move_counter_toggle not visible, continuing", TAG);
        }

        step("Save to slot 3");
        saveToSlot(3);

        step("Verify slot 3 has no red cross");
        verifySavedSlotHasNoRedCross(3);
    }

    /**
     * Test 4: Random game → Make moves on the grid → Save
     */
    @Test
    public void testSave_afterMakingMoves() throws InterruptedException {
        step("Start random game");
        startRandomGame();

        step("Wait for solver to finish");
        Thread.sleep(4000);

        step("Swipe on the game grid to make moves");
        performSwipeMoves();

        step("Save to slot 1");
        saveToSlot(1);

        step("Verify slot 1 has no red cross");
        verifySavedSlotHasNoRedCross(1);
    }

    /**
     * Test 5: Random game → View hints + Make moves → Save
     */
    @Test
    public void testSave_afterHintsAndMoves() throws InterruptedException {
        step("Start random game");
        startRandomGame();

        step("Wait for solver to finish");
        Thread.sleep(4000);

        step("Toggle hint button ON");
        onView(allOf(withId(R.id.hint_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        step("View some hints");
        try {
            onView(allOf(withId(R.id.next_hint_button), isDisplayed())).perform(click());
            Thread.sleep(300);
            onView(allOf(withId(R.id.next_hint_button), isDisplayed())).perform(click());
            Thread.sleep(300);
        } catch (Exception e) {
            Timber.d("%s next_hint_button not visible, continuing", TAG);
        }

        step("Toggle live move counter ON");
        try {
            onView(allOf(withId(R.id.live_move_counter_toggle), isDisplayed())).perform(click());
            Thread.sleep(500);
        } catch (Exception e) {
            Timber.d("%s live_move_counter_toggle not visible, continuing", TAG);
        }

        step("Swipe on the game grid to make moves");
        performSwipeMoves();

        step("Save to slot 1");
        saveToSlot(1);

        step("Verify slot 1 has no red cross");
        verifySavedSlotHasNoRedCross(1);
    }

    /**
     * Test 6: Save → Load from slot → Re-save to different slot
     */
    @Test
    public void testSave_loadAndResave() throws InterruptedException {
        step("Start random game");
        startRandomGame();

        step("Wait for solver to finish");
        Thread.sleep(3000);

        step("Save to slot 1");
        saveToSlot(1);

        step("Verify slot 1 has no red cross after initial save");
        verifySavedSlotHasNoRedCross(1);

        step("Load from slot 1 (fragment is now in load mode after save)");
        loadFromSlot(1);

        step("Wait for game to load");
        Thread.sleep(2000);

        step("Verify game grid is displayed after load");
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));

        step("Wait for solver");
        Thread.sleep(3000);

        step("Save loaded game to slot 2");
        saveToSlot(2);

        step("Verify slot 2 has no red cross after re-save");
        verifySavedSlotHasNoRedCross(2);

        step("Also verify slot 1 still has no red cross");
        verifySavedSlotHasNoRedCross(1);
    }

    /**
     * Test 7: Save → Go to main menu → Load from save slot → Save again
     * (Tests the full round-trip: game → save → main menu → load screen → load → save)
     */
    @Test
    public void testSave_mainMenuLoadAndResave() throws InterruptedException {
        step("Start random game");
        startRandomGame();

        step("Wait for solver to finish");
        Thread.sleep(3000);

        step("Save to slot 1");
        saveToSlot(1);

        step("Verify slot 1 has no red cross");
        verifySavedSlotHasNoRedCross(1);

        step("Go back to game");
        pressBack();
        Thread.sleep(1000);

        step("Go to main menu");
        onView(allOf(withId(R.id.menu_button), isDisplayed())).perform(click());
        Thread.sleep(1000);
        TestHelper.closeAchievementPopupIfPresent();

        step("Open Load Game screen from main menu");
        onView(allOf(withId(R.id.load_game_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        step("Verify slot 1 has no red cross on load screen");
        verifySavedSlotHasNoRedCross(1);

        step("Load from slot 1");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        Thread.sleep(2000);

        step("Verify game grid is displayed after load from main menu");
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));

        step("Wait for solver");
        Thread.sleep(3000);

        step("Re-save loaded game to slot 2");
        saveToSlot(2);

        step("Verify slot 2 has no red cross after re-save via main menu load");
        verifySavedSlotHasNoRedCross(2);
    }

    /**
     * Test 8: Level game → Save
     */
    @Test
    public void testSave_levelGame() throws InterruptedException {
        step("Start a random game first to navigate to GameFragment");
        startRandomGame();

        step("Wait for game to fully load");
        Thread.sleep(2000);

        step("Load Level 1 via GameStateManager");
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.startLevelGame(1);
                Timber.d("%s Level 1 loaded via GameStateManager", TAG);
            }
        });
        Thread.sleep(3000);

        step("Verify game grid is displayed for level");
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));

        step("Wait for solver to finish");
        Thread.sleep(4000);

        step("Save level game to slot 1");
        saveToSlot(1);

        step("Verify slot 1 has no red cross after saving level game");
        verifySavedSlotHasNoRedCross(1);
    }

    // ==================== Helper Methods ====================

    private void step(String description) {
        stepCounter++;
        Timber.d("%s Step %d: %s", TAG, stepCounter, description);
    }

    private void startRandomGame() throws InterruptedException {
        onView(allOf(withId(R.id.ui_button), isDisplayed())).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));
    }

    private void saveToSlot(int slotIndex) throws InterruptedException {
        // Click Save Map button
        onView(allOf(withId(R.id.save_map_button), isDisplayed())).perform(click());
        Thread.sleep(1000);

        // Verify save screen
        onView(withId(R.id.save_slot_recycler_view)).check(matches(isDisplayed()));

        // Click on the requested slot
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(slotIndex, click()));
        Thread.sleep(1500);
    }

    private void loadFromSlot(int slotIndex) throws InterruptedException {
        // After saving, the fragment switches to load mode - just click the slot
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(slotIndex, click()));
        Thread.sleep(2000);
    }

    private void performSwipeMoves() throws InterruptedException {
        // Perform swipe gestures on the game grid to make moves
        // Using general swipes in different directions
        onView(withId(R.id.game_grid_view)).perform(
                androidx.test.espresso.action.ViewActions.swipeUp());
        Thread.sleep(300);
        onView(withId(R.id.game_grid_view)).perform(
                androidx.test.espresso.action.ViewActions.swipeRight());
        Thread.sleep(300);
        onView(withId(R.id.game_grid_view)).perform(
                androidx.test.espresso.action.ViewActions.swipeDown());
        Thread.sleep(300);
        onView(withId(R.id.game_grid_view)).perform(
                androidx.test.espresso.action.ViewActions.swipeLeft());
        Thread.sleep(300);
    }

    /**
     * Verify that a saved slot in the RecyclerView does NOT show the red cross error indicator.
     * The red cross appears when hasTargets() returns false, indicating a corrupt save.
     *
     * @param position The position in the RecyclerView (0=autosave, 1=slot1, etc.)
     */
    private void verifySavedSlotHasNoRedCross(int position) {
        // Scroll to position first to ensure ViewHolder is bound
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.scrollToPosition(position));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // ignore
        }

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

            Timber.d("%s Slot %d: text='%s', textColor=%d (red=%d)",
                    TAG, position, text, textColor, Color.RED);

            assertFalse(
                    "Save slot " + position + " should NOT contain ❌ error indicator. Got: '" + text + "'",
                    text.contains("\u274C"));
            assertFalse(
                    "Save slot " + position + " text should NOT be red (indicates missing targets). Text: '" + text + "'",
                    textColor == Color.RED);
        });
    }

    /**
     * Custom ViewAction to select a tab at a specific position in a TabLayout.
     */
    private static ViewAction selectTabAtPosition(final int position) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Select tab at position " + position;
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view instanceof com.google.android.material.tabs.TabLayout) {
                    com.google.android.material.tabs.TabLayout tabLayout =
                            (com.google.android.material.tabs.TabLayout) view;
                    com.google.android.material.tabs.TabLayout.Tab tab = tabLayout.getTabAt(position);
                    if (tab != null) {
                        tab.select();
                    }
                }
            }
        };
    }
}
