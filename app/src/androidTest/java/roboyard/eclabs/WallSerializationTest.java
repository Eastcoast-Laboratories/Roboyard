package roboyard.eclabs;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import roboyard.eclabs.ui.GameElement;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameState;
import timber.log.Timber;

/**
 * Instrumented tests for wall serialization and deserialization.
 * Verifies that outer boundary walls (at grid+1 positions) are correctly
 * saved and loaded, especially the right and bottom edges.
 *
 * Run with: ./gradlew connectedAndroidTest --tests "roboyard.eclabs.WallSerializationTest"
 */
@RunWith(AndroidJUnit4.class)
public class WallSerializationTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * Create a minimal game state with outer boundary walls and a target+robot.
     */
    private GameState createTestGameState(int width, int height) {
        GameState state = new GameState(width, height);
        state.setLevelName("WallTest");

        // Add all outer boundary walls
        // Top: horizontal walls at y=0
        for (int x = 0; x < width; x++) {
            state.addHorizontalWall(x, 0);
        }
        // Bottom: horizontal walls at y=height
        for (int x = 0; x < width; x++) {
            state.addHorizontalWall(x, height);
        }
        // Left: vertical walls at x=0
        for (int y = 0; y < height; y++) {
            state.addVerticalWall(0, y);
        }
        // Right: vertical walls at x=width
        for (int y = 0; y < height; y++) {
            state.addVerticalWall(width, y);
        }

        // Add one inner wall for good measure
        state.addHorizontalWall(3, 5);
        state.addVerticalWall(7, 2);

        // Add a robot and target (required for serialize)
        state.addTarget(1, 1, Constants.COLOR_GREEN);
        GameElement robot = new GameElement(GameElement.TYPE_ROBOT, 4, 4);
        robot.setColor(Constants.COLOR_GREEN);
        state.getGameElements().add(robot);
        state.setRobotCount(1);
        state.storeInitialRobotPositions();

        return state;
    }

    // ==================== SERIALIZE TESTS ====================

    @Test
    public void testSerializeContainsBottomWalls() {
        int width = 12;
        int height = 14;
        GameState state = createTestGameState(width, height);
        String serialized = state.serialize();

        // Bottom boundary walls are at y=height
        for (int x = 0; x < width; x++) {
            String expected = "H," + x + "," + height;
            assertTrue("Serialized data should contain bottom wall " + expected + "\nSerialized:\n" + serialized,
                    serialized.contains(expected));
        }
    }

    @Test
    public void testSerializeContainsRightWalls() {
        int width = 12;
        int height = 14;
        GameState state = createTestGameState(width, height);
        String serialized = state.serialize();

        // Right boundary walls are at x=width
        for (int y = 0; y < height; y++) {
            String expected = "V," + width + "," + y;
            assertTrue("Serialized data should contain right wall " + expected + "\nSerialized:\n" + serialized,
                    serialized.contains(expected));
        }
    }

    @Test
    public void testSerializeContainsTopWalls() {
        int width = 12;
        int height = 14;
        GameState state = createTestGameState(width, height);
        String serialized = state.serialize();

        // Top boundary walls are at y=0
        for (int x = 0; x < width; x++) {
            String expected = "H," + x + ",0";
            assertTrue("Serialized data should contain top wall " + expected,
                    serialized.contains(expected));
        }
    }

    @Test
    public void testSerializeContainsLeftWalls() {
        int width = 12;
        int height = 14;
        GameState state = createTestGameState(width, height);
        String serialized = state.serialize();

        // Left boundary walls are at x=0
        for (int y = 0; y < height; y++) {
            String expected = "V,0," + y;
            assertTrue("Serialized data should contain left wall " + expected,
                    serialized.contains(expected));
        }
    }

    @Test
    public void testSerializeContainsInnerWalls() {
        GameState state = createTestGameState(12, 14);
        String serialized = state.serialize();

        assertTrue("Should contain inner horizontal wall H,3,5",
                serialized.contains("H,3,5"));
        assertTrue("Should contain inner vertical wall V,7,2",
                serialized.contains("V,7,2"));
    }

    // ==================== ROUNDTRIP TESTS ====================

    @Test
    public void testRoundtripPreservesAllWalls() {
        int width = 12;
        int height = 14;
        GameState original = createTestGameState(width, height);
        String serialized = original.serialize();

        Timber.d("[WALL_TEST] Serialized data:\n%s", serialized);

        // Parse back
        GameState loaded = GameState.parseFromSaveData(serialized, context);
        assertNotNull("Parsed state should not be null", loaded);

        // Count walls by type
        int origHWalls = 0, origVWalls = 0;
        int loadedHWalls = 0, loadedVWalls = 0;

        for (GameElement e : original.getGameElements()) {
            if (e.getType() == GameElement.TYPE_HORIZONTAL_WALL) origHWalls++;
            if (e.getType() == GameElement.TYPE_VERTICAL_WALL) origVWalls++;
        }
        for (GameElement e : loaded.getGameElements()) {
            if (e.getType() == GameElement.TYPE_HORIZONTAL_WALL) loadedHWalls++;
            if (e.getType() == GameElement.TYPE_VERTICAL_WALL) loadedVWalls++;
        }

        Timber.d("[WALL_TEST] Original: %d H-walls, %d V-walls", origHWalls, origVWalls);
        Timber.d("[WALL_TEST] Loaded:   %d H-walls, %d V-walls", loadedHWalls, loadedVWalls);

        assertEquals("Horizontal wall count should match", origHWalls, loadedHWalls);
        assertEquals("Vertical wall count should match", origVWalls, loadedVWalls);
    }

    @Test
    public void testRoundtripPreservesRightBoundaryWalls() {
        int width = 12;
        int height = 14;
        GameState original = createTestGameState(width, height);
        String serialized = original.serialize();
        GameState loaded = GameState.parseFromSaveData(serialized, context);
        assertNotNull(loaded);

        // Check that right boundary walls (x=width) exist in loaded state
        Set<String> rightWalls = new HashSet<>();
        for (GameElement e : loaded.getGameElements()) {
            if (e.getType() == GameElement.TYPE_VERTICAL_WALL && e.getX() == width) {
                rightWalls.add(e.getX() + "," + e.getY());
            }
        }

        Timber.d("[WALL_TEST] Right boundary walls found: %s", rightWalls);

        for (int y = 0; y < height; y++) {
            String key = width + "," + y;
            assertTrue("Right boundary wall at " + key + " should exist after roundtrip",
                    rightWalls.contains(key));
        }
    }

    @Test
    public void testRoundtripPreservesBottomBoundaryWalls() {
        int width = 12;
        int height = 14;
        GameState original = createTestGameState(width, height);
        String serialized = original.serialize();
        GameState loaded = GameState.parseFromSaveData(serialized, context);
        assertNotNull(loaded);

        // Check that bottom boundary walls (y=height) exist in loaded state
        Set<String> bottomWalls = new HashSet<>();
        for (GameElement e : loaded.getGameElements()) {
            if (e.getType() == GameElement.TYPE_HORIZONTAL_WALL && e.getY() == height) {
                bottomWalls.add(e.getX() + "," + e.getY());
            }
        }

        Timber.d("[WALL_TEST] Bottom boundary walls found: %s", bottomWalls);

        for (int x = 0; x < width; x++) {
            String key = x + "," + height;
            assertTrue("Bottom boundary wall at " + key + " should exist after roundtrip",
                    bottomWalls.contains(key));
        }
    }

    // ==================== WALL COUNT TESTS ====================

    @Test
    public void testExpectedWallCount() {
        int width = 12;
        int height = 14;
        GameState state = createTestGameState(width, height);

        // Expected: top(12) + bottom(12) + left(14) + right(14) + 2 inner = 54
        int expectedH = width + width + 1; // top + bottom + 1 inner
        int expectedV = height + height + 1; // left + right + 1 inner

        int hCount = 0, vCount = 0;
        for (GameElement e : state.getGameElements()) {
            if (e.getType() == GameElement.TYPE_HORIZONTAL_WALL) hCount++;
            if (e.getType() == GameElement.TYPE_VERTICAL_WALL) vCount++;
        }

        assertEquals("Horizontal walls: top(" + width + ") + bottom(" + width + ") + 1 inner",
                expectedH, hCount);
        assertEquals("Vertical walls: left(" + height + ") + right(" + height + ") + 1 inner",
                expectedV, vCount);
    }

    // ==================== DIFFERENT BOARD SIZES ====================

    @Test
    public void testRoundtrip8x8() {
        verifyRoundtripForSize(8, 8);
    }

    @Test
    public void testRoundtrip16x16() {
        verifyRoundtripForSize(16, 16);
    }

    @Test
    public void testRoundtrip12x14() {
        verifyRoundtripForSize(12, 14);
    }

    @Test
    public void testRoundtrip22x22() {
        verifyRoundtripForSize(22, 22);
    }

    private void verifyRoundtripForSize(int width, int height) {
        GameState original = createTestGameState(width, height);
        String serialized = original.serialize();
        GameState loaded = GameState.parseFromSaveData(serialized, context);
        assertNotNull("Parsed state should not be null for " + width + "x" + height, loaded);

        // Count all walls
        int origWalls = 0, loadedWalls = 0;
        for (GameElement e : original.getGameElements()) {
            if (e.getType() == GameElement.TYPE_HORIZONTAL_WALL || e.getType() == GameElement.TYPE_VERTICAL_WALL) {
                origWalls++;
            }
        }
        for (GameElement e : loaded.getGameElements()) {
            if (e.getType() == GameElement.TYPE_HORIZONTAL_WALL || e.getType() == GameElement.TYPE_VERTICAL_WALL) {
                loadedWalls++;
            }
        }

        Timber.d("[WALL_TEST] %dx%d: original=%d walls, loaded=%d walls", width, height, origWalls, loadedWalls);
        assertEquals("Wall count should match for " + width + "x" + height, origWalls, loadedWalls);

        // Verify right boundary
        boolean hasRightWall = false;
        for (GameElement e : loaded.getGameElements()) {
            if (e.getType() == GameElement.TYPE_VERTICAL_WALL && e.getX() == width) {
                hasRightWall = true;
                break;
            }
        }
        assertTrue("Should have right boundary walls (x=" + width + ") for " + width + "x" + height, hasRightWall);

        // Verify bottom boundary
        boolean hasBottomWall = false;
        for (GameElement e : loaded.getGameElements()) {
            if (e.getType() == GameElement.TYPE_HORIZONTAL_WALL && e.getY() == height) {
                hasBottomWall = true;
                break;
            }
        }
        assertTrue("Should have bottom boundary walls (y=" + height + ") for " + width + "x" + height, hasBottomWall);
    }
}
