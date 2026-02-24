package roboyard.eclabs;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import roboyard.logic.core.GameState;
import roboyard.logic.core.GameElement;

/**
 * Unit tests for GameState map signature generation.
 *
 * Verifies wall signatures, position signatures, and full map signatures
 * used for unique map tracking and achievement progress.
 *
 * Tags: map-signature, walls, positions, unique-map-tracking, achievements
 */
public class MapSignatureTest {

    private GameState gameState;

    @Before
    public void setUp() {
        gameState = new GameState(16, 16);
    }

    @Test
    public void testWallSignatureIncludesDimensions() {
        String signature = gameState.generateWallSignature();
        assertTrue(signature.startsWith("16x16;"));
    }

    @Test
    public void testWallSignatureWithNoWalls() {
        String signature = gameState.generateWallSignature();
        assertEquals("16x16;", signature);
    }

    @Test
    public void testWallSignatureWithWalls() {
        gameState.addHorizontalWall(5, 3);
        gameState.addVerticalWall(7, 8);
        
        String signature = gameState.generateWallSignature();
        
        assertTrue(signature.contains("mh5,3;"));
        assertTrue(signature.contains("mv7,8;"));
    }

    @Test
    public void testWallSignatureIsSorted() {
        // Add walls in non-sorted order
        gameState.addVerticalWall(10, 5);
        gameState.addHorizontalWall(2, 1);
        gameState.addVerticalWall(3, 2);
        
        String signature = gameState.generateWallSignature();
        
        // Should be sorted: mh2,1 < mv10,5 < mv3,2 (alphabetically)
        int mh21 = signature.indexOf("mh2,1;");
        int mv105 = signature.indexOf("mv10,5;");
        int mv32 = signature.indexOf("mv3,2;");
        
        assertTrue(mh21 < mv105);
        assertTrue(mv105 < mv32);
    }

    @Test
    public void testPositionSignatureWithRobots() {
        gameState.addRobot(5, 5, 0);
        gameState.addRobot(10, 10, 1);
        gameState.storeInitialRobotPositions();
        
        String signature = gameState.generatePositionSignature();
        
        assertTrue(signature.contains("R0@5,5;"));
        assertTrue(signature.contains("R1@10,10;"));
    }

    @Test
    public void testPositionSignatureWithTargets() {
        gameState.addTarget(8, 8, 0);
        gameState.addTarget(12, 12, 1);
        
        String signature = gameState.generatePositionSignature();
        
        assertTrue(signature.contains("T0@8,8;"));
        assertTrue(signature.contains("T1@12,12;"));
    }

    @Test
    public void testPositionSignatureSeparatesRobotsAndTargets() {
        gameState.addRobot(5, 5, 0);
        gameState.storeInitialRobotPositions();
        gameState.addTarget(8, 8, 0);
        
        String signature = gameState.generatePositionSignature();
        
        // Should have | separator between robots and targets
        assertTrue(signature.contains("|"));
        int robotPart = signature.indexOf("R0@5,5;");
        int separator = signature.indexOf("|");
        int targetPart = signature.indexOf("T0@8,8;");
        
        assertTrue(robotPart < separator);
        assertTrue(separator < targetPart);
    }

    @Test
    public void testMapSignatureCombinesWallsAndPositions() {
        gameState.addHorizontalWall(3, 4);
        gameState.addRobot(5, 5, 0);
        gameState.storeInitialRobotPositions();
        gameState.addTarget(8, 8, 0);
        
        String mapSignature = gameState.generateMapSignature();
        String wallSignature = gameState.generateWallSignature();
        String positionSignature = gameState.generatePositionSignature();
        
        assertEquals(wallSignature + "||" + positionSignature, mapSignature);
    }

    @Test
    public void testIdenticalMapsHaveSameSignature() {
        // Create first map
        GameState map1 = new GameState(16, 16);
        map1.addHorizontalWall(5, 3);
        map1.addRobot(2, 2, 0);
        map1.storeInitialRobotPositions();
        map1.addTarget(10, 10, 0);
        
        // Create identical second map
        GameState map2 = new GameState(16, 16);
        map2.addHorizontalWall(5, 3);
        map2.addRobot(2, 2, 0);
        map2.storeInitialRobotPositions();
        map2.addTarget(10, 10, 0);
        
        assertEquals(map1.generateMapSignature(), map2.generateMapSignature());
    }

    @Test
    public void testDifferentWallsHaveDifferentSignature() {
        GameState map1 = new GameState(16, 16);
        map1.addHorizontalWall(5, 3);
        
        GameState map2 = new GameState(16, 16);
        map2.addHorizontalWall(6, 3);
        
        assertNotEquals(map1.generateWallSignature(), map2.generateWallSignature());
    }

    @Test
    public void testDifferentRobotPositionsHaveDifferentSignature() {
        GameState map1 = new GameState(16, 16);
        map1.addRobot(2, 2, 0);
        map1.storeInitialRobotPositions();
        
        GameState map2 = new GameState(16, 16);
        map2.addRobot(3, 3, 0);
        map2.storeInitialRobotPositions();
        
        assertNotEquals(map1.generatePositionSignature(), map2.generatePositionSignature());
    }

    @Test
    public void testSameWallsDifferentPositionsShareWallSignature() {
        GameState map1 = new GameState(16, 16);
        map1.addHorizontalWall(5, 3);
        map1.addRobot(2, 2, 0);
        map1.storeInitialRobotPositions();
        
        GameState map2 = new GameState(16, 16);
        map2.addHorizontalWall(5, 3);
        map2.addRobot(8, 8, 0);
        map2.storeInitialRobotPositions();
        
        // Same walls
        assertEquals(map1.generateWallSignature(), map2.generateWallSignature());
        // Different positions
        assertNotEquals(map1.generatePositionSignature(), map2.generatePositionSignature());
        // Different full map
        assertNotEquals(map1.generateMapSignature(), map2.generateMapSignature());
    }

    @Test
    public void testDifferentBoardSizesHaveDifferentSignature() {
        GameState map1 = new GameState(16, 16);
        GameState map2 = new GameState(14, 14);
        
        assertNotEquals(map1.generateWallSignature(), map2.generateWallSignature());
    }
}
