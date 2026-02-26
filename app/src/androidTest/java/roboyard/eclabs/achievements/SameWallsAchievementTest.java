package roboyard.eclabs.achievements;

import static org.junit.Assert.*;

import android.app.Activity;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import roboyard.logic.core.GameHistoryEntry;
import roboyard.ui.achievements.AchievementManager;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.FileReadWrite;
import roboyard.ui.components.GameHistoryManager;

import timber.log.Timber;

/**
 * Tests for same_walls achievements (2, 10, 100).
 *
 * same_walls_2:   Solve the same wall layout with 2 different robot positions
 * same_walls_10:  Solve the same wall layout with 10 different robot positions
 * same_walls_100: Solve the same wall layout with 100 different robot positions
 *
 * Root cause fixed: AchievementManager.currentActivity was never set, so
 * findByWallSignature could never run. Fixed by adding setCurrentActivity() setter
 * and calling it from GameFragment before onRandomGameCompleted.
 *
 * Strategy: All history writes AND onRandomGameCompleted calls run on Main thread
 * (via activityRule.getScenario().onActivity) with CountDownLatch for synchronization.
 *
 * Tags: achievements, same_walls, history, wall-signature, e2e
 */
@RunWith(AndroidJUnit4.class)
public class SameWallsAchievementTest {

    private static final String PREFS_NAME = "roboyard_achievements";
    private static final String SHARED_WALL_SIGNATURE = "12x14;mh0,0;mh1,0;mh2,0;mh3,0;mh4,0;mh5,0;";

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Context context;
    private AchievementManager achievementManager;

    @Before
    public void setUp() throws InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Clear achievements
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();

        // Clear history on Main thread and set activity reference
        runOnMainThreadSync(act -> {
            FileReadWrite.deletePrivateData(act, "history_index.json");
            achievementManager.setCurrentActivity(act);
            Timber.d("[SAME_WALLS_TEST] History cleared, activity set");
        });

        Timber.d("[SAME_WALLS_TEST] setUp complete");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
    }

    /**
     * Run code on the Main thread and wait for it to complete.
     */
    private void runOnMainThreadSync(java.util.function.Consumer<Activity> action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        activityRule.getScenario().onActivity(act -> {
            action.accept(act);
            latch.countDown();
        });
        latch.await();
    }

    /**
     * Add a completed history entry AND call onRandomGameCompleted on the Main thread.
     * This mirrors what GameFragment does: saveToHistoryNow then onRandomGameCompleted.
     */
    private void addHistoryAndCompleteGame(int positionIndex, String wallSig) throws InterruptedException {
        runOnMainThreadSync(act -> {
            String positionSignature = "R0@" + positionIndex + ",3;R1@" + (positionIndex + 1) + ",7;|T0@5,5;";
            String mapSignature = wallSig + "||" + positionSignature;

            GameHistoryEntry entry = new GameHistoryEntry(
                    "history_test_" + positionIndex + ".txt",
                    "Test Map " + positionIndex,
                    System.currentTimeMillis(),
                    30, 5, 5, "12x14", null);
            entry.setWallSignature(wallSig);
            entry.setPositionSignature(positionSignature);
            entry.setMapSignature(mapSignature);
            entry.recordCompletion(30, 5);
            GameHistoryManager.addHistoryEntry(act, entry);
            Timber.d("[SAME_WALLS_TEST] Added history entry #%d: wallSig=%s", positionIndex, wallSig);

            // Mirror GameFragment: setCurrentActivity + onRandomGameCompleted after saveToHistoryNow
            achievementManager.setCurrentActivity(act);
            achievementManager.onNewGameStarted();
            achievementManager.onRandomGameCompleted(5, 5, 0, 15000, false, 4, 1, 1, true, false, wallSig);
        });
    }

    /**
     * Test that same_walls_2 unlocks after 2 unique positions with same wall layout.
     * After game 1: 1 position → NOT unlocked.
     * After game 2: 2 positions → UNLOCKED.
     */
    @Test
    public void testSameWalls2Unlocks() throws InterruptedException {
        Timber.d("[SAME_WALLS_TEST] ===== testSameWalls2Unlocks =====");

        // Game 1: 1 unique position
        addHistoryAndCompleteGame(0, SHARED_WALL_SIGNATURE);

        AchievementManager.AchievementProgress progress1 = achievementManager.getProgress("same_walls_2");
        Timber.d("[SAME_WALLS_TEST] After game 1: progress=%d/%d, unlocked=%b",
                progress1 != null ? progress1.current : -1,
                progress1 != null ? progress1.required : -1,
                achievementManager.isUnlocked("same_walls_2"));

        assertFalse("same_walls_2 should NOT unlock after only 1 position",
                achievementManager.isUnlocked("same_walls_2"));

        // Game 2: 2 unique positions → should unlock
        addHistoryAndCompleteGame(1, SHARED_WALL_SIGNATURE);

        AchievementManager.AchievementProgress progress2 = achievementManager.getProgress("same_walls_2");
        Timber.d("[SAME_WALLS_TEST] After game 2: progress=%d/%d, unlocked=%b",
                progress2 != null ? progress2.current : -1,
                progress2 != null ? progress2.required : -1,
                achievementManager.isUnlocked("same_walls_2"));

        assertTrue("same_walls_2 MUST be unlocked after 2 positions with same wall layout",
                achievementManager.isUnlocked("same_walls_2"));

        Timber.d("[SAME_WALLS_TEST] ✓ same_walls_2 UNLOCKED after 2 unique positions!");
    }

    /**
     * Test that same_walls_2 does NOT unlock when 2 games have DIFFERENT wall signatures.
     */
    @Test
    public void testSameWalls2RequiresSameWalls() throws InterruptedException {
        Timber.d("[SAME_WALLS_TEST] ===== testSameWalls2RequiresSameWalls =====");

        String wallSigA = SHARED_WALL_SIGNATURE;
        String wallSigB = "12x14;mh0,0;mh1,0;mh9,9;mh10,10;";

        addHistoryAndCompleteGame(0, wallSigA);
        addHistoryAndCompleteGame(1, wallSigB);

        assertFalse("same_walls_2 should NOT unlock with 2 different wall signatures",
                achievementManager.isUnlocked("same_walls_2"));

        Timber.d("[SAME_WALLS_TEST] ✓ same_walls_2 correctly NOT unlocked with different walls");
    }

    /**
     * Verify the progress counter tracks sameWallsMaxPositions correctly.
     * After 3 games with same wall: progress should be >= 3, same_walls_2 unlocked.
     */
    @Test
    public void testSameWallsProgressCounter() throws InterruptedException {
        Timber.d("[SAME_WALLS_TEST] ===== testSameWallsProgressCounter =====");

        for (int i = 0; i < 3; i++) {
            addHistoryAndCompleteGame(i, SHARED_WALL_SIGNATURE);
        }

        AchievementManager.AchievementProgress progress2 = achievementManager.getProgress("same_walls_2");
        AchievementManager.AchievementProgress progress10 = achievementManager.getProgress("same_walls_10");

        assertNotNull("same_walls_2 progress should not be null", progress2);
        assertNotNull("same_walls_10 progress should not be null", progress10);

        Timber.d("[SAME_WALLS_TEST] same_walls_2 progress: %d/%d", progress2.current, progress2.required);
        Timber.d("[SAME_WALLS_TEST] same_walls_10 progress: %d/%d", progress10.current, progress10.required);

        assertTrue("sameWallsMaxPositions should be >= 3 after 3 unique positions",
                progress2.current >= 3);
        assertTrue("same_walls_2 should be unlocked when 3 positions found",
                achievementManager.isUnlocked("same_walls_2"));

        Timber.d("[SAME_WALLS_TEST] ✓ progress counter tracks correctly (sameWallsMaxPositions=%d)",
                progress2.current);
    }
}
