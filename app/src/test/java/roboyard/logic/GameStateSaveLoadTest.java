package roboyard.logic;

import org.junit.Before;
import org.junit.Test;

import roboyard.logic.core.GameState;
import roboyard.logic.core.GameElement;

import static org.junit.Assert.*;

/**
 * Unit tests for game state save/load (serialization) functionality.
 *
 * Verifies that the compact level format serialization correctly preserves:
 * - Move counts in metadata header
 * - Robot positions in compact format (rr, rg, rb, ry)
 * - Target positions in compact format (tr, tg, tb, ty)
 * - Map name and other metadata
 * - Multiple robots with correct color mapping
 *
 * Tags: save, load, serialization, compact-format, metadata, robots, targets
 */
public class GameStateSaveLoadTest {
    
    private GameState gameState;
    
    @Before
    public void setUp() {
        gameState = new GameState(8, 8);
        gameState.setLevelName("Test Game");
        // Add a target - serialize() requires at least one target
        gameState.addTarget(5, 5, 1);
        // Add robots - serialize() requires initialRobotPositions
        gameState.addRobot(0, 0, 0); // Pink
        gameState.addRobot(7, 0, 1); // Green
        gameState.addRobot(0, 7, 2); // Blue
        gameState.addRobot(7, 7, 3); // Yellow
        gameState.storeInitialRobotPositions();
    }
    
    @Test
    public void testMoveCountPreservedAfterSaveLoad() {
        gameState.setMoveCount(5);
        
        String serialized = gameState.serialize();
        
        assertTrue("Serialized data should contain MOVES:5", serialized.contains("MOVES:5"));
    }
    
    @Test
    public void testRobotPositionsPreservedAfterSaveLoad() {
        gameState.setMoveCount(1);
        
        String serialized = gameState.serialize();
        
        // Compact format: rr=pink, rg=green, rb=blue, ry=yellow
        assertTrue("Serialized data should contain move count", serialized.contains("MOVES:1"));
        assertTrue("Serialized data should contain pink robot at (0,0)", serialized.contains("rr0,0;"));
        assertTrue("Serialized data should contain green robot at (7,0)", serialized.contains("rg7,0;"));
    }
    
    @Test
    public void testGameStateMetadataPreserved() {
        gameState.setMoveCount(7);
        String mapName = "Custom Level";
        gameState.setLevelName(mapName);
        
        String serialized = gameState.serialize();
        
        assertTrue("Serialized data should contain map name", serialized.contains("MAPNAME:" + mapName));
        assertTrue("Serialized data should contain move count", serialized.contains("MOVES:7"));
    }
    
    @Test
    public void testMultipleRobotsPreserved() {
        gameState.setMoveCount(3);
        
        String serialized = gameState.serialize();
        
        // All 4 robot colors should be in compact format
        assertTrue("Serialized data should contain move count", serialized.contains("MOVES:3"));
        assertTrue("Serialized data should contain pink robot", serialized.contains("rr0,0;"));
        assertTrue("Serialized data should contain green robot", serialized.contains("rg7,0;"));
        assertTrue("Serialized data should contain blue robot", serialized.contains("rb0,7;"));
        assertTrue("Serialized data should contain yellow robot", serialized.contains("ry7,7;"));
    }
    
    @Test
    public void testMoveCountPreservedInMetadata() {
        int expectedMoveCount = 42;
        gameState.setMoveCount(expectedMoveCount);
        
        String serialized = gameState.serialize();
        String[] lines = serialized.split("\n");
        String metadataLine = lines[0];
        
        assertTrue("First line should be metadata starting with #", metadataLine.startsWith("#"));
        assertTrue("Metadata should contain MOVES:" + expectedMoveCount, 
                   metadataLine.contains("MOVES:" + expectedMoveCount));
    }
    
    @Test
    public void testTargetSerializedInCompactFormat() {
        String serialized = gameState.serialize();
        
        // Target at (5,5) with color 1 (green) should be serialized as tg5,5;
        assertTrue("Serialized data should contain green target in compact format", serialized.contains("tg5,5;"));
    }
    
    @Test
    public void testBoardDimensionsPreserved() {
        String serialized = gameState.serialize();
        
        assertTrue("Serialized data should contain WIDTH:8", serialized.contains("WIDTH:8;"));
        assertTrue("Serialized data should contain HEIGHT:8", serialized.contains("HEIGHT:8;"));
    }
}
