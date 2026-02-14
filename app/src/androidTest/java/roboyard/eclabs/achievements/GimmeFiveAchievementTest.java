package roboyard.eclabs.achievements;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.GameStateManager;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GameElement;

import timber.log.Timber;

import java.util.List;

/**
 * Test for the gimme_five achievement.
 * 
 * The gimme_five achievement requires all robots in a game to touch each other at least once.
 * For n robots, we need n*(n-1)/2 unique touch pairs.
 * 
 * Example: For 3 robots (0, 1, 2), we need 3 pairs: 0-1, 0-2, 1-2
 * 
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.achievements.GimmeFiveAchievementTest
 */
@RunWith(AndroidJUnit4.class)
public class GimmeFiveAchievementTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        
        // Reset achievements before each test
        achievementManager.resetAll();
        
        Timber.d("[GIMME_FIVE_TEST] Setup complete, achievements reset");
    }

    @After
    public void tearDown() {
        // Reset achievements after each test
        achievementManager.resetAll();
    }

    /**
     * Test that the gimme_five achievement is unlocked when all robots touch each other.
     * This test simulates robot touches directly via the AchievementManager.
     */
    @Test
    public void testGimmeFiveWithTwoRobots() {
        Timber.d("[GIMME_FIVE_TEST] Testing gimme_five with 2 robots");
        
        // Start a new game to reset tracking
        achievementManager.onNewGameStarted();
        
        // For 2 robots, we need 1 pair: 0-1
        assertFalse("gimme_five should not be unlocked yet", 
                achievementManager.isUnlocked("gimme_five"));
        
        // Robot 0 touches robot 1
        achievementManager.onRobotTouched(0, 1, 2);
        
        // Check progress
        int[] progress = achievementManager.getRobotTouchProgress();
        assertEquals("Should have 1 pair", 1, progress[0]);
        assertEquals("Should need 1 pair for 2 robots", 1, progress[1]);
        
        // Achievement should be unlocked
        assertTrue("gimme_five should be unlocked after all robots touched", 
                achievementManager.isUnlocked("gimme_five"));
        
        Timber.d("[GIMME_FIVE_TEST] Test passed: gimme_five unlocked with 2 robots");
    }

    /**
     * Test gimme_five with 3 robots.
     * For 3 robots, we need 3 pairs: 0-1, 0-2, 1-2
     */
    @Test
    public void testGimmeFiveWithThreeRobots() {
        Timber.d("[GIMME_FIVE_TEST] Testing gimme_five with 3 robots");
        
        // Start a new game to reset tracking
        achievementManager.onNewGameStarted();
        
        // For 3 robots, we need 3 pairs
        assertFalse("gimme_five should not be unlocked yet", 
                achievementManager.isUnlocked("gimme_five"));
        
        // Robot 0 touches robot 1
        achievementManager.onRobotTouched(0, 1, 3);
        assertFalse("gimme_five should not be unlocked yet (1/3 pairs)", 
                achievementManager.isUnlocked("gimme_five"));
        
        // Robot 0 touches robot 2
        achievementManager.onRobotTouched(0, 2, 3);
        assertFalse("gimme_five should not be unlocked yet (2/3 pairs)", 
                achievementManager.isUnlocked("gimme_five"));
        
        // Robot 1 touches robot 2
        achievementManager.onRobotTouched(1, 2, 3);
        
        // Check progress
        int[] progress = achievementManager.getRobotTouchProgress();
        assertEquals("Should have 3 pairs", 3, progress[0]);
        assertEquals("Should need 3 pairs for 3 robots", 3, progress[1]);
        
        // Achievement should be unlocked
        assertTrue("gimme_five should be unlocked after all robots touched", 
                achievementManager.isUnlocked("gimme_five"));
        
        Timber.d("[GIMME_FIVE_TEST] Test passed: gimme_five unlocked with 3 robots");
    }

    /**
     * Test gimme_five with 4 robots.
     * For 4 robots, we need 6 pairs: 0-1, 0-2, 0-3, 1-2, 1-3, 2-3
     */
    @Test
    public void testGimmeFiveWithFourRobots() {
        Timber.d("[GIMME_FIVE_TEST] Testing gimme_five with 4 robots");
        
        // Start a new game to reset tracking
        achievementManager.onNewGameStarted();
        
        // For 4 robots, we need 6 pairs
        assertFalse("gimme_five should not be unlocked yet", 
                achievementManager.isUnlocked("gimme_five"));
        
        // Touch all pairs
        achievementManager.onRobotTouched(0, 1, 4); // 1/6
        achievementManager.onRobotTouched(0, 2, 4); // 2/6
        achievementManager.onRobotTouched(0, 3, 4); // 3/6
        achievementManager.onRobotTouched(1, 2, 4); // 4/6
        achievementManager.onRobotTouched(1, 3, 4); // 5/6
        
        assertFalse("gimme_five should not be unlocked yet (5/6 pairs)", 
                achievementManager.isUnlocked("gimme_five"));
        
        achievementManager.onRobotTouched(2, 3, 4); // 6/6
        
        // Check progress
        int[] progress = achievementManager.getRobotTouchProgress();
        assertEquals("Should have 6 pairs", 6, progress[0]);
        assertEquals("Should need 6 pairs for 4 robots", 6, progress[1]);
        
        // Achievement should be unlocked
        assertTrue("gimme_five should be unlocked after all robots touched", 
                achievementManager.isUnlocked("gimme_five"));
        
        Timber.d("[GIMME_FIVE_TEST] Test passed: gimme_five unlocked with 4 robots");
    }

    /**
     * Test that duplicate touches don't count twice.
     */
    @Test
    public void testDuplicateTouchesIgnored() {
        Timber.d("[GIMME_FIVE_TEST] Testing duplicate touches are ignored");
        
        // Start a new game to reset tracking
        achievementManager.onNewGameStarted();
        
        // For 3 robots, we need 3 pairs
        
        // Robot 0 touches robot 1 multiple times
        achievementManager.onRobotTouched(0, 1, 3);
        achievementManager.onRobotTouched(0, 1, 3); // Duplicate
        achievementManager.onRobotTouched(1, 0, 3); // Same pair, different order
        
        // Check progress - should still be 1 pair
        int[] progress = achievementManager.getRobotTouchProgress();
        assertEquals("Should have 1 pair (duplicates ignored)", 1, progress[0]);
        
        assertFalse("gimme_five should not be unlocked yet (only 1/3 pairs)", 
                achievementManager.isUnlocked("gimme_five"));
        
        Timber.d("[GIMME_FIVE_TEST] Test passed: duplicate touches ignored");
    }

    /**
     * Test that new game resets touch tracking.
     */
    @Test
    public void testNewGameResetsTracking() {
        Timber.d("[GIMME_FIVE_TEST] Testing new game resets tracking");
        
        // Start a new game
        achievementManager.onNewGameStarted();
        
        // Touch some robots
        achievementManager.onRobotTouched(0, 1, 3);
        achievementManager.onRobotTouched(0, 2, 3);
        
        // Check progress
        int[] progress = achievementManager.getRobotTouchProgress();
        assertEquals("Should have 2 pairs", 2, progress[0]);
        
        // Start a new game - should reset tracking
        achievementManager.onNewGameStarted();
        
        // Check progress - should be reset
        progress = achievementManager.getRobotTouchProgress();
        assertEquals("Should have 0 pairs after new game", 0, progress[0]);
        
        Timber.d("[GIMME_FIVE_TEST] Test passed: new game resets tracking");
    }

    /**
     * Test gimme_five with 5 robots (maximum).
     * For 5 robots, we need 10 pairs.
     */
    @Test
    public void testGimmeFiveWithFiveRobots() {
        Timber.d("[GIMME_FIVE_TEST] Testing gimme_five with 5 robots");
        
        // Start a new game to reset tracking
        achievementManager.onNewGameStarted();
        
        // For 5 robots, we need 10 pairs: 5*(5-1)/2 = 10
        assertFalse("gimme_five should not be unlocked yet", 
                achievementManager.isUnlocked("gimme_five"));
        
        // Touch all pairs
        int pairCount = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                achievementManager.onRobotTouched(i, j, 5);
                pairCount++;
                Timber.d("[GIMME_FIVE_TEST] Pair %d: robot %d touched robot %d", pairCount, i, j);
            }
        }
        
        // Check progress
        int[] progress = achievementManager.getRobotTouchProgress();
        assertEquals("Should have 10 pairs", 10, progress[0]);
        assertEquals("Should need 10 pairs for 5 robots", 10, progress[1]);
        
        // Achievement should be unlocked
        assertTrue("gimme_five should be unlocked after all robots touched", 
                achievementManager.isUnlocked("gimme_five"));
        
        Timber.d("[GIMME_FIVE_TEST] Test passed: gimme_five unlocked with 5 robots");
    }

    /**
     * Integration test: Start a random game and simulate robot movements.
     * This test uses the actual GameStateManager to load a game and move robots.
     */
    @Test
    public void testGimmeFiveInRandomGame() throws InterruptedException {
        Timber.d("[GIMME_FIVE_TEST] Testing gimme_five in random game");
        
        // Wait for activity to load
        Thread.sleep(2000);
        
        // Get the GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
        });
        
        // Start a new random game
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                gameStateManager.startNewGame();
                Timber.d("[GIMME_FIVE_TEST] New random game started");
            }
        });
        
        Thread.sleep(2000);
        
        // Get the game state and check robot count
        final int[] robotCount = {0};
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    List<GameElement> robots = state.getRobots();
                    if (robots != null) {
                        robotCount[0] = robots.size();
                        Timber.d("[GIMME_FIVE_TEST] Game has %d robots", robotCount[0]);
                    }
                }
            }
        });
        
        // Verify we have at least 2 robots
        assertTrue("Game should have at least 2 robots", robotCount[0] >= 2);
        
        // Simulate all robot touches directly via AchievementManager
        // (In a real game, this would happen when robots collide)
        achievementManager.onNewGameStarted();
        
        for (int i = 0; i < robotCount[0]; i++) {
            for (int j = i + 1; j < robotCount[0]; j++) {
                achievementManager.onRobotTouched(i, j, robotCount[0]);
            }
        }
        
        // Verify achievement is unlocked
        assertTrue("gimme_five should be unlocked after all robots touched", 
                achievementManager.isUnlocked("gimme_five"));
        
        Timber.d("[GIMME_FIVE_TEST] Test passed: gimme_five works in random game context");
    }
}
