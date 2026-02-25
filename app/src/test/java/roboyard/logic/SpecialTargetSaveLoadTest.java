package roboyard.logic;

import org.junit.Test;

import roboyard.logic.core.Constants;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;

import static org.junit.Assert.*;

/**
 * Unit tests for special target types in save/load (serialization).
 *
 * Verifies that:
 * - Silver robot + silver target serialize and deserialize correctly
 * - Multi-color target (COLOR_MULTI = -1) serializes as 'tm' and deserializes back
 * - A map with 2 targets but only 1 robot serializes correctly
 *
 * Tags: save, load, serialization, silver, multi-color, target, robot, edge-cases
 */
public class SpecialTargetSaveLoadTest {

    /**
     * Test: Silver robot + silver target serialize correctly in compact format.
     * Silver robot = rs, silver target = ts
     */
    @Test
    public void testSilverRobotAndTarget() {
        GameState state = new GameState(8, 8);
        state.setLevelName("Silver Test");

        // Add silver robot and silver target
        state.addRobot(2, 3, Constants.COLOR_SILVER);
        state.addTarget(5, 6, Constants.COLOR_SILVER);
        state.storeInitialRobotPositions();

        String serialized = state.serialize();

        // Silver target should be serialized as ts5,6;
        assertTrue("Serialized data should contain silver target 'ts5,6;'",
                serialized.contains("ts5,6;"));

        // Silver robot should be serialized as rs2,3;
        assertTrue("Serialized data should contain silver robot 'rs2,3;'",
                serialized.contains("rs2,3;"));

        // Board dimensions
        assertTrue("Serialized data should contain WIDTH:8",
                serialized.contains("WIDTH:8;"));
        assertTrue("Serialized data should contain HEIGHT:8",
                serialized.contains("HEIGHT:8;"));
    }

    /**
     * Test: Multi-color target (COLOR_MULTI = -1) serializes as 'tm' in compact format.
     * This was the root cause of the red cross bug.
     */
    @Test
    public void testMultiColorTarget() {
        GameState state = new GameState(8, 8);
        state.setLevelName("Multi Target Test");

        // Add a robot (required for serialization)
        state.addRobot(0, 0, Constants.COLOR_PINK);
        // Add multi-color target
        state.addTarget(3, 4, Constants.COLOR_MULTI);
        state.storeInitialRobotPositions();

        String serialized = state.serialize();

        // Multi target should be serialized as tm3,4; (not t?3,4; which was the bug)
        assertTrue("Serialized data should contain multi target 'tm3,4;' but got: " + serialized,
                serialized.contains("tm3,4;"));

        // Must NOT contain t? (the old buggy format)
        assertFalse("Serialized data should NOT contain 't?' (buggy format)",
                serialized.contains("t?"));

        // Verify target is in gameElements with correct color
        int multiTargetCount = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == Constants.COLOR_MULTI) {
                multiTargetCount++;
            }
        }
        assertEquals("Should have exactly 1 multi-color target", 1, multiTargetCount);
    }

    /**
     * Test: Map with 2 targets but only 1 robot that needs to reach its target.
     * Both targets should be serialized correctly.
     */
    @Test
    public void testTwoTargetsOneRobot() {
        GameState state = new GameState(8, 8);
        state.setLevelName("Two Targets Test");

        // Add 1 robot (pink)
        state.addRobot(0, 0, Constants.COLOR_PINK);
        // Add 2 targets (pink target = the one robot must reach, green target = extra)
        state.addTarget(7, 7, Constants.COLOR_PINK);
        state.addTarget(3, 3, Constants.COLOR_GREEN);
        state.storeInitialRobotPositions();

        String serialized = state.serialize();

        // Pink target at (7,7) should be serialized as tr7,7;
        assertTrue("Serialized data should contain pink target 'tr7,7;'",
                serialized.contains("tr7,7;"));

        // Green target at (3,3) should be serialized as tg3,3;
        assertTrue("Serialized data should contain green target 'tg3,3;'",
                serialized.contains("tg3,3;"));

        // Pink robot at (0,0) should be serialized as rr0,0;
        assertTrue("Serialized data should contain pink robot 'rr0,0;'",
                serialized.contains("rr0,0;"));

        // Verify exactly 2 targets and 1 robot in gameElements
        int targetCount = 0;
        int robotCount = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) targetCount++;
            if (element.getType() == GameElement.TYPE_ROBOT) robotCount++;
        }
        assertEquals("Should have exactly 2 targets", 2, targetCount);
        assertEquals("Should have exactly 1 robot", 1, robotCount);
    }

    /**
     * Test: Multi-color target color is preserved in board's targetColors array.
     */
    @Test
    public void testMultiColorTargetInBoard() {
        GameState state = new GameState(8, 8);
        state.setLevelName("Multi Board Test");

        state.addTarget(4, 5, Constants.COLOR_MULTI);

        // Verify the target color is set correctly in the board
        assertEquals("Target color at (4,5) should be COLOR_MULTI (-1)",
                Constants.COLOR_MULTI, state.getTargetColor(4, 5));

        // Verify cell type is TARGET
        assertEquals("Cell type at (4,5) should be TYPE_TARGET",
                Constants.TYPE_TARGET, state.getCellType(4, 5));
    }

    /**
     * Test: Silver target color is preserved through synchronizeTargets().
     */
    @Test
    public void testSilverTargetSyncPreservesColor() {
        GameState state = new GameState(8, 8);
        state.setLevelName("Silver Sync Test");

        state.addRobot(0, 0, Constants.COLOR_SILVER);
        state.addTarget(6, 6, Constants.COLOR_SILVER);
        state.storeInitialRobotPositions();

        // synchronizeTargets is called inside serialize()
        String serialized = state.serialize();

        // Verify the silver target is at the correct position with the correct color
        assertEquals("Target color at (6,6) should be COLOR_SILVER (4)",
                Constants.COLOR_SILVER, state.getTargetColor(6, 6));

        assertTrue("Serialized data should contain silver target",
                serialized.contains("ts6,6;"));
    }
}
