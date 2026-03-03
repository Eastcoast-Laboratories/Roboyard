package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainFragmentActivity;

import timber.log.Timber;

/**
 * E2E test: Multi-target mode with Impossible difficulty - stress test solver with OOM prevention.
 * 
 * This test verifies that the solver does not crash with OutOfMemoryError when:
 * - Multi-target mode is enabled (2 robots must reach 2 targets)
 * - Difficulty is set to Impossible (17+ moves)
 * - 25 consecutive random games are generated
 * 
 * The test is VISIBLE in the emulator - you can watch it run.
 *
 * Steps:
 * 1. Open Settings
 * 2. Set Multi-Target mode (2 robots, 2 targets)
 * 3. Set difficulty to Impossible
 * 4. Start random game
 * 5. Wait 20s for solver
 * 6. Press "New Game" button
 * 7. Repeat steps 5-6 for 25 games total
 * 8. Verify no crash (OOM)
 *
 * Tags: e2e, multi-target, difficulty, impossible, solver, oom, new-game, memory, stress-test
 */
@RunWith(AndroidJUnit4.class)
public class ImpossibleDifficultyNewGameTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    @Before
    public void setup() throws InterruptedException {
        Timber.d("[TEST] ========== ImpossibleDifficultyNewGameTest STARTED ==========");
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(1000);
    }

    @Test
    public void testMultiTargetImpossibleDifficulty25Games() throws InterruptedException {
        Timber.d("[TEST] Starting Multi-Target Impossible Difficulty 25 Games Test");

        // Step 1: Enable Multi-Target mode with 2 robots via TestHelper
        TestHelper.setMultiTargetMode(2);
        TestHelper.closeAchievementPopupIfPresent();

        // Step 2: Set Difficulty to Impossible via TestHelper
        TestHelper.setDifficulty(R.id.difficulty_impossible);
        TestHelper.closeAchievementPopupIfPresent();

        // Step 3: Start first random game
        Timber.d("[TEST] Starting first random game");
        TestHelper.startRandomGame();
        Thread.sleep(20000); // Wait 20s for solver to complete

        // Verify game grid is displayed
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));
        Timber.d("[TEST] Game 1/25 completed successfully");

        // Step 4: Press "New Game" button 24 more times (total 25 games)
        for (int i = 2; i <= 25; i++) {
            Timber.d("[TEST] ========== Starting Game %d/25 ==========", i);
            
            // Press New Game button
            onView(withId(R.id.new_map_button)).perform(click());
            Timber.d("[TEST] Pressed New Game button for game %d", i);
            
            // Wait 20s for map generation and solver
            Thread.sleep(20000);
            
            // Verify game grid is still displayed (no crash)
            onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));
            Timber.d("[TEST] Game %d/25 completed successfully", i);
        }

        Timber.d("[TEST] ========== TEST COMPLETED: 25 games without crash! ==========");
    }
}
