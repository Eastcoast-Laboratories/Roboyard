package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GameElement;

import timber.log.Timber;

/**
 * Reproduces the crash reported in GitHub Issue #67 on level YUBAX.
 * 
 * Bug: Game crashes when following specific move sequence on this map.
 * 
 * Steps to Reproduce:
 * 1. Load the YUBAX map layout
 * 2. Move pink: up, right, up, left
 * 3. Move blue: up, left, up, right (this should crash)
 * 
 * Expected: Game continues normally
 * Actual (before fix): Game crashes
 * 
 * This test verifies the crash is fixed by attempting the exact move sequence.
 * If the game crashes, the test will fail with an exception.
 * 
 * Tags: e2e, crash-reproduction, yubax, issue-67
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.YubaxCrashReproductionTest
 */
@RunWith(AndroidJUnit4.class)
public class YubaxCrashReproductionTest {

    // Exact YUBAX map data exported from the app (deduplicated)
    // board:12,14 - 12 columns, 14 rows
    // h=horizontal wall (above cell row,col), v=vertical wall (left of cell row,col)
    // Robots: rr=red, ry=yellow, rg=green, rb=blue
    // Target: tm=multicolor at (row=3, col=5)
    private static final String YUBAX_MAP_DATA =
            "board:12,14;\n" +
            // horizontal walls - inner
            "h0,6;\n" +
            "h0,9;\n" +
            "h1,8;\n" +
            "h2,3;\n" +
            "h2,6;\n" +
            "h2,11;\n" +
            "h3,4;\n" +
            "h3,10;\n" +
            "h4,3;\n" +
            "h4,12;\n" +
            "h5,6;\n" +
            "h5,8;\n" +
            "h5,10;\n" +
            "h6,6;\n" +
            "h6,8;\n" +
            "h6,12;\n" +
            "h7,3;\n" +
            "h7,10;\n" +
            "h9,7;\n" +
            "h9,9;\n" +
            "h10,2;\n" +
            "h10,5;\n" +
            "h10,12;\n" +
            "h11,4;\n" +
            "h11,11;\n" +
            // vertical walls - inner
            "v2,8;\n" +
            "v2,11;\n" +
            "v3,0;\n" +
            "v3,2;\n" +
            "v3,4;\n" +
            "v3,13;\n" +
            "v4,9;\n" +
            "v5,2;\n" +
            "v5,6;\n" +
            "v5,7;\n" +
            "v5,12;\n" +
            "v6,0;\n" +
            "v7,6;\n" +
            "v7,7;\n" +
            "v7,10;\n" +
            "v8,3;\n" +
            "v9,7;\n" +
            "v9,9;\n" +
            "v9,13;\n" +
            "v10,2;\n" +
            "v10,5;\n" +
            "v10,11;\n" +
            // robots
            "rg0,7;\n" +   // Green at row=0, col=7
            "ry2,4;\n" +   // Yellow at row=2, col=4
            "rr10,10;\n" +  // Red (pink) at row=10, col=10
            "rb11,12;\n" + // Blue at row=11, col=12 - crash robot!
            // target
            "tm3,5;\n";    // Multicolor target at row=3, col=5

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private GameStateManager gameStateManager;

    // Robot color constants
    private static final int COLOR_RED = 0;
    private static final int COLOR_GREEN = 1;
    private static final int COLOR_BLUE = 2;
    private static final int COLOR_YELLOW = 3;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Timber.d("[YUBAX_CRASH_TEST] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        Timber.d("[YUBAX_CRASH_TEST] ========== TEST FINISHED ==========");
    }

    /**
     * Test that reproduces the crash from GitHub Issue #67
     */
    @Test
    public void testYubaxCrashReproduction() throws InterruptedException {
        Timber.d("[YUBAX_CRASH_TEST] Starting YUBAX crash reproduction test");

        // Wait for activity to initialize
        Thread.sleep(2000);

        // Get GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
        });

        // Load the YUBAX map via deep-link
        loadYubaxMap();
        Thread.sleep(3000);

        // Verify map loaded correctly
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            assertNotNull("GameState should not be null", state);
            
            // Verify we have all 4 robots (rg, ry, rr, rb)
            int robotCount = 0;
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_ROBOT) {
                    robotCount++;
                    Timber.d("[YUBAX_CRASH_TEST] Robot found at (%d,%d) color=%d",
                            element.getX(), element.getY(), element.getColor());
                }
            }
            assertEquals("Should have 4 robots", 4, robotCount);
            Timber.d("[YUBAX_CRASH_TEST] Map loaded successfully with %d robots", robotCount);
        });

        // Execute the crash-inducing move sequence
        // Step 1: Move pink (red) up, right, up, left
        Timber.d("[YUBAX_CRASH_TEST] === Moving Pink (Red) robot ===");
        moveRobotByColor(COLOR_RED, 0, -1); // up
        Thread.sleep(500);
        moveRobotByColor(COLOR_RED, 1, 0);  // right
        Thread.sleep(500);
        moveRobotByColor(COLOR_RED, 0, -1); // up
        Thread.sleep(500);
        moveRobotByColor(COLOR_RED, -1, 0); // left
        Thread.sleep(500);

        // Step 2: Move blue up, left, up, right (this should crash)
        Timber.d("[YUBAX_CRASH_TEST] === Moving Blue robot (CRASH SEQUENCE) ===");
        moveRobotByColor(COLOR_BLUE, 0, -1); // up
        Thread.sleep(500);
        moveRobotByColor(COLOR_BLUE, -1, 0); // left
        Thread.sleep(500);
        moveRobotByColor(COLOR_BLUE, 0, -1); // up
        Thread.sleep(500);
        
        // This is the move that causes the crash according to the bug report
        Timber.d("[YUBAX_CRASH_TEST] === FINAL MOVE (right) - this should trigger crash if bug exists ===");
        moveRobotByColor(COLOR_BLUE, 1, 0);  // right
        Thread.sleep(500);

        // If we reach here without exception/crash, the bug is fixed
        Timber.d("[YUBAX_CRASH_TEST] ✓ Test completed without crash - bug appears to be fixed!");

        // Wait 60s to allow manual investigation of the game state
        Timber.d("[YUBAX_CRASH_TEST] Waiting 60s for manual investigation...");
        Thread.sleep(60000);

        // Verify game is still in valid state
        activityRule.getScenario().onActivity(activity -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            assertNotNull("GameState should still be valid after move sequence", state);
            
            // Verify board dimensions are still correct
            assertEquals("Board width should be 12", 12, state.getWidth());
            assertEquals("Board height should be 14", 14, state.getHeight());
            
            Timber.d("[YUBAX_CRASH_TEST] ✓ Game state is valid after all moves");
        });
    }

    /**
     * Load the YUBAX map via deep-link intent
     */
    private void loadYubaxMap() throws InterruptedException {
        Timber.d("[YUBAX_CRASH_TEST] Loading YUBAX map via deep-link");

        activityRule.getScenario().onActivity(activity -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("roboyard://open?data=" + Uri.encode(YUBAX_MAP_DATA));
            intent.setData(uri);
            intent.setClass(activity, MainActivity.class);
            activity.onNewIntent(intent);
            Timber.d("[YUBAX_CRASH_TEST] Deep-link intent sent for YUBAX map");
        });

        Thread.sleep(3000);
    }

    /**
     * Select robot by color and move in direction
     * @param color Robot color constant (COLOR_RED, COLOR_BLUE, etc.)
     * @param dx X direction (-1 = left, 1 = right)
     * @param dy Y direction (-1 = up, 1 = down)
     */
    private void moveRobotByColor(int color, int dx, int dy) {
        final String directionStr = (dy == -1 ? "UP" : dy == 1 ? "DOWN" : dx == -1 ? "LEFT" : "RIGHT");
        final String colorStr = (color == COLOR_RED ? "RED/PINK" : color == COLOR_BLUE ? "BLUE" : 
                                 color == COLOR_GREEN ? "GREEN" : color == COLOR_YELLOW ? "YELLOW" : "SILVER");
        
        Timber.d("[YUBAX_CRASH_TEST] Moving %s %s (dx=%d, dy=%d)", colorStr, directionStr, dx, dy);

        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager == null) {
                Timber.e("[YUBAX_CRASH_TEST] GameStateManager is null!");
                return;
            }

            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) {
                Timber.e("[YUBAX_CRASH_TEST] GameState is null!");
                return;
            }

            // Find robot by color
            GameElement targetRobot = null;
            for (GameElement element : state.getRobots()) {
                if (element.getColor() == color) {
                    targetRobot = element;
                    break;
                }
            }

            if (targetRobot == null) {
                Timber.e("[YUBAX_CRASH_TEST] Could not find robot with color %d", color);
                return;
            }

            // Select the robot
            state.setSelectedRobot(targetRobot);
            Timber.d("[YUBAX_CRASH_TEST] Selected %s robot at (%d,%d)", 
                    colorStr, targetRobot.getX(), targetRobot.getY());
        });

        // Small delay for selection
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Perform the move
        activityRule.getScenario().onActivity(activity -> {
            if (gameStateManager != null) {
                boolean moved = gameStateManager.moveRobotInDirection(dx, dy);
                Timber.d("[YUBAX_CRASH_TEST] Move result: %s", moved ? "SUCCESS" : "BLOCKED/FAILED");
            }
        });
    }
}
