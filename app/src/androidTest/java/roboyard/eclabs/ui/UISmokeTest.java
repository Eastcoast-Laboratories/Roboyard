package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import roboyard.eclabs.R;
import roboyard.logic.core.Preferences;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Fast UI smoke test that exercises the most important screens and actions
 * without any long waits. Sets min=2 / max=20 moves so the first generated
 * map is accepted immediately.
 *
 * Covers: Settings, Random Game, Achievements, Save/Load, History tab,
 *         Level selection, Debug Settings, back navigation.
 *
 * Target runtime: < 30 seconds.
 */
@RunWith(AndroidJUnit4.class)
public class UISmokeTest {

    private static final String TAG = "[UI_SMOKE]";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        Timber.d(TAG + " setUp: configuring fast game generation");
        AtomicReference<Activity> actRef = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> {
            actRef.set(a);
            gameStateManager = ((MainActivity) a).getGameStateManager();
            // Set min=2 max=20 so map generation accepts the first result
            Preferences.setMinSolutionMoves(2);
            Preferences.setMaxSolutionMoves(20);
        });
        assertNotNull("GameStateManager must not be null", gameStateManager);
        // Close any achievement/streak popup
        TestHelper.closeAchievementPopupIfPresent();
    }

    @Test
    public void smokeTestAllScreens() throws InterruptedException {
        step("1", "Main menu is visible");
        onView(withId(R.id.ui_button)).check(matches(isDisplayed()));

        // --- Settings ---
        step("2", "Open Settings");
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(800);

        step("3", "Verify difficulty section");
        onView(withId(R.id.difficulty_beginner)).perform(scrollTo());
        onView(withId(R.id.difficulty_beginner)).check(matches(isDisplayed()));

        step("3b", "Verify puzzle parameter controls");
        onView(withId(R.id.min_solution_moves_input)).perform(scrollTo());
        onView(withId(R.id.min_solution_moves_input)).check(matches(isDisplayed()));
        onView(withId(R.id.max_solution_moves_input)).perform(scrollTo());
        onView(withId(R.id.max_solution_moves_input)).check(matches(isDisplayed()));

        step("4", "Back to main menu");
        pressBack();
        Thread.sleep(500);
        onView(withId(R.id.ui_button)).check(matches(isDisplayed()));

        // --- Random Game ---
        step("5", "Start random game");
        onView(withId(R.id.ui_button)).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));

        step("6", "Verify game controls are present");
        onView(withId(R.id.hint_button)).check(matches(isDisplayed()));
        onView(withId(R.id.reset_robots_button)).check(matches(isDisplayed()));

        step("7", "Back to main menu from game via menu button");
        onView(withId(R.id.menu_button)).perform(click());
        Thread.sleep(1000);
        TestHelper.closeAchievementPopupIfPresent();
        onView(withId(R.id.ui_button)).check(matches(isDisplayed()));

        // --- Achievements ---
        step("8", "Open Achievements screen");
        onView(withId(R.id.achievements_icon_button)).perform(click());
        Thread.sleep(800);
        onView(withId(R.id.achievements_container)).check(matches(isDisplayed()));

        step("9", "Back from Achievements");
        onView(withId(R.id.back_button)).perform(click());
        Thread.sleep(500);
        TestHelper.closeAchievementPopupIfPresent();
        onView(withId(R.id.ui_button)).check(matches(isDisplayed()));

        // --- Save/Load ---
        step("10", "Navigate to Save/Load screen");
        TestHelper.navigateToSaveLoadScreen(activityRule);
        Thread.sleep(800);
        onView(withId(R.id.save_slot_recycler_view)).check(matches(isDisplayed()));

        // --- History tab ---
        step("11", "Switch to History tab");
        TestHelper.navigateToHistoryTab();
        Thread.sleep(500);

        step("12", "Back to main menu via programmatic navigation");
        activityRule.getScenario().onActivity(a -> {
            a.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, new roboyard.ui.fragments.MainMenuFragment())
                .commit();
        });
        Thread.sleep(800);
        TestHelper.closeAchievementPopupIfPresent();

        // --- Level Selection ---
        step("13", "Open Level Selection");
        onView(withId(R.id.level_game_button)).perform(click());
        Thread.sleep(800);

        step("14", "Back from Level Selection");
        pressBack();
        Thread.sleep(800);
        TestHelper.closeAchievementPopupIfPresent();

        // --- Debug Settings ---
        step("15", "Open Debug Settings via long press");
        TestHelper.openDebugScreen();
        Thread.sleep(500);
        onView(withText("DEBUG SETTINGS")).check(matches(isDisplayed()));

        step("16", "Back from Debug Settings");
        pressBack();
        Thread.sleep(800);

        step("PASS", "UI Smoke Test completed successfully");
    }

    private void step(String id, String msg) {
        Timber.d("[UNITTESTS][SMOKE_TEST] %s [%s] %s", TAG, id, msg);
        System.out.println(TAG + " [" + id + "] " + msg);
    }
}
