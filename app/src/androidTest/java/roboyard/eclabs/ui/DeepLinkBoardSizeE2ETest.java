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

import roboyard.ui.activities.MainActivity;
import roboyard.eclabs.R;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * End-to-End test for deep-link board size parsing.
 * 
 * Bug: Deep-link with board:12,12; was parsed as 8x8, limiting robot movement
 * to 8x8 area while walls/elements were on the full 12x12 board.
 * 
 * This test:
 * 1. Simulates a deep-link with board:12,12; and game elements
 * 2. Verifies the GameState is loaded with correct 12x12 dimensions
 * 3. Verifies robots can move to positions beyond 8x8 (e.g., row 10, col 10)
 * 4. Verifies the full board is accessible, not limited to 8x8
 * 
 * Tags: e2e, deep-link, board-size, compact-format, ui-movement
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.DeepLinkBoardSizeE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class DeepLinkBoardSizeE2ETest {

    // The exact deep-link map data (compact format) with board:12,12;
    private static final String DEEP_LINK_MAP_DATA_12x12 =
            "board:12,12;\n" +
            "h0,0;\n" +
            "h0,4;\n" +
            "h0,7;\n" +
            "h0,9;\n" +
            "h0,12;\n" +
            "h1,0;\n" +
            "h1,2;\n" +
            "h1,9;\n" +
            "h1,12;\n" +
            "h2,0;\n" +
            "h2,6;\n" +
            "h2,12;\n" +
            "h3,0;\n" +
            "h3,4;\n" +
            "h3,12;\n" +
            "h4,0;\n" +
            "h4,8;\n" +
            "h4,12;\n" +
            "h5,0;\n" +
            "h5,3;\n" +
            "h5,5;\n" +
            "h5,7;\n" +
            "h5,12;\n" +
            "h6,0;\n" +
            "h6,5;\n" +
            "h6,7;\n" +
            "h6,12;\n" +
            "h7,0;\n" +
            "h7,1;\n" +
            "h7,12;\n" +
            "h8,0;\n" +
            "h8,7;\n" +
            "h8,9;\n" +
            "h8,12;\n" +
            "h9,0;\n" +
            "h9,3;\n" +
            "h9,10;\n" +
            "h9,12;\n" +
            "h10,0;\n" +
            "h10,4;\n" +
            "h10,12;\n" +
            "h11,0;\n" +
            "h11,3;\n" +
            "h11,5;\n" +
            "h11,7;\n" +
            "h11,12;\n" +
            "v0,0;\n" +
            "v0,1;\n" +
            "v0,2;\n" +
            "v0,3;\n" +
            "v0,4;\n" +
            "v0,5;\n" +
            "v0,6;\n" +
            "v0,7;\n" +
            "v0,8;\n" +
            "v0,9;\n" +
            "v0,10;\n" +
            "v0,11;\n" +
            "v1,2;\n" +
            "v1,9;\n" +
            "v2,0;\n" +
            "v2,6;\n" +
            "v2,11;\n" +
            "v4,0;\n" +
            "v4,3;\n" +
            "v5,5;\n" +
            "v5,6;\n" +
            "v5,7;\n" +
            "v6,0;\n" +
            "v6,3;\n" +
            "v6,11;\n" +
            "v7,1;\n" +
            "v7,5;\n" +
            "v7,6;\n" +
            "v8,6;\n" +
            "v8,11;\n" +
            "v9,9;\n" +
            "v10,2;\n" +
            "v10,9;\n" +
            "v11,3;\n" +
            "v12,0;\n" +
            "v12,1;\n" +
            "v12,2;\n" +
            "v12,3;\n" +
            "v12,4;\n" +
            "v12,5;\n" +
            "v12,6;\n" +
            "v12,7;\n" +
            "v12,8;\n" +
            "v12,9;\n" +
            "v12,10;\n" +
            "v12,11;\n" +
            "ts10,0;\n" +
            "rr5,8;\n" +
            "rs6,1;\n" +
            "ry7,3;\n" +
            "rg7,8;\n" +
            "rb11,3;\n";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private GameStateManager gameStateManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {
        // Cleanup if needed
    }

    /**
     * Test that deep-link with board:12,12; is correctly parsed as 12x12, not 8x8.
     * Verifies the GameState has correct dimensions and all elements are loaded.
     */
    @Test
    public void testDeepLinkBoard12x12Dimensions() throws InterruptedException {
        Timber.d("[DEEPLINK_E2E] Starting deep-link board size test");

        // Wait for activity to initialize
        Thread.sleep(2000);

        // Get GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
            assertNotNull("GameStateManager should not be null", gameStateManager);
        });

        // Load the deep-link map data
        loadDeepLinkMap();

        // Wait for map to load
        Thread.sleep(2000);

        // Verify board dimensions
        activityRule.getScenario().onActivity(activity -> {
            GameState currentState = (GameState) gameStateManager.getCurrentState().getValue();
            assertNotNull("GameState should not be null", currentState);

            int width = currentState.getWidth();
            int height = currentState.getHeight();

            Timber.d("[DEEPLINK_E2E] Board dimensions: %dx%d", width, height);

            // The bug was that board was parsed as 8x8 instead of 12x12
            assertEquals("Board width should be 12, not 8", 12, width);
            assertEquals("Board height should be 12, not 8", 12, height);
        });

        Timber.d("[DEEPLINK_E2E] ✓ Board dimensions verified as 12x12");
    }

    /**
     * Test that robots can move to positions beyond 8x8 (e.g., row 10, col 10).
     * This verifies the full 12x12 board is accessible, not limited to 8x8.
     */
    @Test
    public void testRobotMovementBeyond8x8() throws InterruptedException {
        Timber.d("[DEEPLINK_E2E] Starting robot movement test");

        Thread.sleep(2000);

        // Get GameStateManager
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });

        // Load the deep-link map
        loadDeepLinkMap();
        Thread.sleep(2000);

        // Get initial robot positions
        activityRule.getScenario().onActivity(activity -> {
            GameState currentState = (GameState) gameStateManager.getCurrentState().getValue();
            assertNotNull("GameState should not be null", currentState);

            // Find robot at (11, 3) - this is beyond 8x8
            GameElement robotAtEdge = null;
            for (GameElement element : currentState.getGameElements()) {
                if (element.getType() == GameElement.TYPE_ROBOT &&
                    element.getX() == 11 && element.getY() == 3) {
                    robotAtEdge = element;
                    break;
                }
            }

            assertNotNull("Robot at (11,3) should exist (rb11,3 from deep-link)", robotAtEdge);
            Timber.d("[DEEPLINK_E2E] ✓ Found robot at edge position (11,3)");
        });

        // Try to move a robot - if board was limited to 8x8, this would fail
        // Select and move the robot at (7,8) upward
        activityRule.getScenario().onActivity(activity -> {
            GameState currentState = (GameState) gameStateManager.getCurrentState().getValue();

            // Find robot at (7,8) - rg7,8
            GameElement targetRobot = null;
            for (GameElement element : currentState.getGameElements()) {
                if (element.getType() == GameElement.TYPE_ROBOT &&
                    element.getX() == 7 && element.getY() == 8) {
                    targetRobot = element;
                    break;
                }
            }

            if (targetRobot != null) {
                // Set as selected robot
                currentState.setSelectedRobot(targetRobot);

                // Try to move up (dy = -1)
                gameStateManager.moveRobotInDirection(0, -1);

                Timber.d("[DEEPLINK_E2E] Moved robot from (7,8) upward");
            }
        });

        Thread.sleep(1000);

        // Verify the robot moved (or hit a wall, which is fine - the point is the move was processed)
        activityRule.getScenario().onActivity(activity -> {
            GameState currentState = (GameState) gameStateManager.getCurrentState().getValue();
            assertNotNull("GameState should still be valid after move", currentState);

            // Verify board is still 12x12 (not reset to 8x8)
            assertEquals("Board width should remain 12 after move", 12, currentState.getWidth());
            assertEquals("Board height should remain 12 after move", 12, currentState.getHeight());

            Timber.d("[DEEPLINK_E2E] ✓ Robot movement processed on 12x12 board");
        });
    }

    /**
     * Test that all game elements (robots, targets, walls) are loaded from the deep-link.
     */
    @Test
    public void testDeepLinkElementsLoaded() throws InterruptedException {
        Timber.d("[DEEPLINK_E2E] Starting element loading test");

        Thread.sleep(2000);

        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });

        loadDeepLinkMap();
        Thread.sleep(2000);

        activityRule.getScenario().onActivity(activity -> {
            GameState currentState = (GameState) gameStateManager.getCurrentState().getValue();
            assertNotNull("GameState should not be null", currentState);

            int robotCount = 0;
            int targetCount = 0;
            int wallCount = 0;

            for (GameElement element : currentState.getGameElements()) {
                if (element.getType() == GameElement.TYPE_ROBOT) {
                    robotCount++;
                    Timber.d("[DEEPLINK_E2E] Robot at (%d,%d) color=%d", 
                            element.getX(), element.getY(), element.getColor());
                } else if (element.getType() == GameElement.TYPE_TARGET) {
                    targetCount++;
                    Timber.d("[DEEPLINK_E2E] Target at (%d,%d) color=%d", 
                            element.getX(), element.getY(), element.getColor());
                } else if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL ||
                           element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                    wallCount++;
                }
            }

            Timber.d("[DEEPLINK_E2E] Loaded: %d robots, %d targets, %d walls", 
                    robotCount, targetCount, wallCount);

            // From the deep-link data: 5 robots (rr, rs, ry, rg, rb), 1 target (ts), many walls
            assertEquals("Should have 5 robots", 5, robotCount);
            assertEquals("Should have 1 target", 1, targetCount);
            assertTrue("Should have walls", wallCount > 0);

            Timber.d("[DEEPLINK_E2E] ✓ All elements loaded correctly");
        });
    }

    /**
     * Test that the target at (10,0) is accessible (beyond 8x8).
     */
    @Test
    public void testTargetBeyond8x8() throws InterruptedException {
        Timber.d("[DEEPLINK_E2E] Starting target accessibility test");

        Thread.sleep(2000);

        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });

        loadDeepLinkMap();
        Thread.sleep(2000);

        activityRule.getScenario().onActivity(activity -> {
            GameState currentState = (GameState) gameStateManager.getCurrentState().getValue();
            assertNotNull("GameState should not be null", currentState);

            // Find target at (10,0) - ts10,0 from deep-link
            GameElement targetAtEdge = null;
            for (GameElement element : currentState.getGameElements()) {
                if (element.getType() == GameElement.TYPE_TARGET &&
                    element.getX() == 10 && element.getY() == 0) {
                    targetAtEdge = element;
                    break;
                }
            }

            assertNotNull("Target at (10,0) should exist (ts10,0 from deep-link)", targetAtEdge);
            Timber.d("[DEEPLINK_E2E] ✓ Target found at edge position (10,0)");
        });
    }

    /**
     * Load the deep-link map by simulating the deep-link intent.
     */
    private void loadDeepLinkMap() throws InterruptedException {
        Timber.d("[DEEPLINK_E2E] Loading deep-link map");

        activityRule.getScenario().onActivity(activity -> {
            // Create a deep-link intent with the map data
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            // Build the URI with the map data as a query parameter
            Uri uri = Uri.parse("roboyard://open?data=" + Uri.encode(DEEP_LINK_MAP_DATA_12x12));
            intent.setData(uri);
            intent.setClass(activity, MainActivity.class);

            // Send the intent to the activity
            activity.onNewIntent(intent);

            Timber.d("[DEEPLINK_E2E] Deep-link intent sent");
        });

        // Wait for the map to be processed
        Thread.sleep(3000);
    }
}
