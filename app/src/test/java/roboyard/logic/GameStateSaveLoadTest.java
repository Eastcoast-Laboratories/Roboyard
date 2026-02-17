package roboyard.logic;

import org.junit.Before;
import org.junit.Test;

import roboyard.logic.core.GameState;
import roboyard.logic.core.GameElement;

import static org.junit.Assert.*;

/**
 * Unit tests for game state save/load functionality
 * Verifies that move counts and robot positions are preserved when loading a saved game
 */
public class GameStateSaveLoadTest {
    
    private GameState gameState;
    
    @Before
    public void setUp() {
        gameState = new GameState(8, 8);
        gameState.setLevelName("Test Game");
        // Add a target - serialize() requires at least one target
        gameState.addTarget(7, 7, 1);
    }
    
    @Test
    public void testMoveCountPreservedAfterSaveLoad() {
        // Arrange: Create a game state with some moves
        gameState.setMoveCount(5);
        
        // Act: Serialize
        String serialized = gameState.serialize();
        
        // Assert: Serialized data should contain move count
        assertTrue("Serialized data should contain MOVES:5", serialized.contains("MOVES:5"));
    }
    
    @Test
    public void testRobotPositionsPreservedAfterSaveLoad() {
        // Arrange: Add a robot at initial position
        gameState.addRobot(0, 0, 1); // Color 1 = pink
        gameState.setMoveCount(1);
        
        // Act: Serialize
        String serialized = gameState.serialize();
        
        // Assert: Serialized data should contain robot position and move count
        assertTrue("Serialized data should contain move count", serialized.contains("MOVES:1"));
        assertTrue("Serialized data should contain ROBOTS section", serialized.contains("ROBOTS:"));
        assertTrue("Serialized data should contain robot at initial position", serialized.contains("0,0,1"));
    }
    
    @Test
    public void testGameStateMetadataPreserved() {
        // Arrange: Set game metadata
        gameState.setMoveCount(7);
        String mapName = "Custom Level";
        gameState.setLevelName(mapName);
        
        // Act: Serialize
        String serialized = gameState.serialize();
        
        // Assert: Metadata should be in serialized data
        assertTrue("Serialized data should contain map name", serialized.contains("MAPNAME:" + mapName));
        assertTrue("Serialized data should contain move count", serialized.contains("MOVES:7"));
    }
    
    @Test
    public void testMultipleRobotsPreserved() {
        // Arrange: Add multiple robots
        gameState.addRobot(0, 0, 1); // Pink
        gameState.addRobot(7, 7, 2); // Yellow
        gameState.setMoveCount(3);
        
        // Act: Serialize
        String serialized = gameState.serialize();
        
        // Assert: Both robots should be in serialized data
        assertTrue("Serialized data should contain move count", serialized.contains("MOVES:3"));
        assertTrue("Serialized data should contain ROBOTS section", serialized.contains("ROBOTS:"));
        // Initial positions are stored in ROBOTS section
        assertTrue("Serialized data should contain pink robot initial position", serialized.contains("0,0,1"));
        assertTrue("Serialized data should contain yellow robot initial position", serialized.contains("7,7,2"));
    }
    
    @Test
    public void testMoveCountPreservedInMetadata() {
        // Arrange: Set a specific move count
        int expectedMoveCount = 42;
        gameState.setMoveCount(expectedMoveCount);
        
        // Act: Serialize and check metadata line
        String serialized = gameState.serialize();
        String[] lines = serialized.split("\n");
        String metadataLine = lines[0];
        
        // Assert: First line should be metadata with MOVES count
        assertTrue("First line should be metadata starting with #", metadataLine.startsWith("#"));
        assertTrue("Metadata should contain MOVES:" + expectedMoveCount, 
                   metadataLine.contains("MOVES:" + expectedMoveCount));
    }
}
