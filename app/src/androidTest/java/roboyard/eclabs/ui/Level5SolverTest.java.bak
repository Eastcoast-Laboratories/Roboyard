package roboyard.eclabs.ui;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import roboyard.eclabs.util.SolverManager;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GridElement;
import roboyard.pm.ia.GameSolution;
import timber.log.Timber;

import static org.junit.Assert.*;

/**
 * Unit test to verify that Level 5 is solved correctly after MSOT fix.
 * 
 * Level 5 should have a simple 3-move solution, not a 9-move solution.
 * The bug was that getGridElements() was reading targets from board[][] instead of gameElements,
 * causing the solver to receive a different map than what was displayed.
 */
@RunWith(AndroidJUnit4.class)
public class Level5SolverTest {

    private Context context;
    private SolverManager solverManager;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        solverManager = SolverManager.getInstance();
        solverManager.resetInitialization();
        Timber.d("[TEST] Level5SolverTest setup complete");
    }

    /**
     * Test that Level 5 is loaded correctly and solver finds a reasonable solution.
     * 
     * Level 5 map:
     * - Board size: 12x14
     * - Red target at (7,7)
     * - Red robot at (3,1)
     * - Blue robot at (9,2)
     * - Yellow robot at (9,1)
     * - Green robot at (2,11)
     * 
     * Expected: Solver should find a solution with fewer than 9 moves.
     * The previous bug caused a 9-move solution because the solver had wrong map data.
     */
    @Test
    public void testLevel5SolverFindsOptimalSolution() throws InterruptedException {
        Timber.d("[TEST] Starting testLevel5SolverFindsOptimalSolution");
        
        // Load Level 5
        GameState state = GameState.loadLevel(context, 5);
        assertNotNull("Level 5 should load successfully", state);
        
        // Verify level loaded correctly
        assertEquals("Level should be 5", 5, state.getLevelId());
        Timber.d("[TEST] Level 5 loaded with %d game elements", state.getGameElements().size());
        
        // Get grid elements for solver
        ArrayList<GridElement> gridElements = state.getGridElements();
        assertNotNull("Grid elements should not be null", gridElements);
        assertTrue("Grid elements should not be empty", gridElements.size() > 0);
        
        // Log grid elements for debugging
        int wallCount = 0;
        int robotCount = 0;
        int targetCount = 0;
        for (GridElement element : gridElements) {
            String type = element.getType();
            if (type.equals("mh") || type.equals("mv")) {
                wallCount++;
            } else if (type.startsWith("robot_")) {
                robotCount++;
                Timber.d("[TEST] Robot: %s at (%d,%d)", type, element.getX(), element.getY());
            } else if (type.startsWith("target_")) {
                targetCount++;
                Timber.d("[TEST] Target: %s at (%d,%d)", type, element.getX(), element.getY());
            }
        }
        Timber.d("[TEST] Grid elements: %d walls, %d robots, %d targets", wallCount, robotCount, targetCount);
        
        // Verify we have the expected elements
        assertEquals("Should have exactly 4 robots", 4, robotCount);
        assertTrue("Should have at least 1 target", targetCount >= 1);
        
        // Initialize solver
        solverManager.resetInitialization();
        solverManager.initialize(gridElements);
        
        // Create latch to wait for solver completion
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GameSolution> solutionRef = new AtomicReference<>();
        AtomicInteger moveCountRef = new AtomicInteger(-1);
        
        // Set up solver listener
        solverManager.setSolverListener(new SolverManager.SolverListener() {
            @Override
            public void onSolverFinished(boolean success, int numMoves, int numSolutions) {
                Timber.d("[TEST] Solver finished: success=%b, numMoves=%d, numSolutions=%d", 
                        success, numMoves, numSolutions);
                if (success && numSolutions > 0) {
                    moveCountRef.set(numMoves);
                    solutionRef.set(solverManager.getSolution(0));
                }
                latch.countDown();
            }
            
            @Override
            public void onSolverCancelled() {
                Timber.d("[TEST] Solver cancelled");
                latch.countDown();
            }
        });
        
        // Start solver
        Timber.d("[TEST] Starting solver...");
        solverManager.startSolver();
        
        // Wait for solver to complete (max 30 seconds)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue("Solver should complete within 30 seconds", completed);
        
        // Verify solution was found
        int moveCount = moveCountRef.get();
        Timber.d("[TEST] Solution found with %d moves", moveCount);
        
        assertTrue("Solver should find a solution", moveCount > 0);
        
        // The key assertion: solution should be less than 9 moves
        // Before the MSOT fix, it was finding a 9-move solution
        // After the fix, it should find a shorter solution (likely 3-5 moves)
        assertTrue("Solution should be less than 9 moves (was " + moveCount + " moves). " +
                   "If this fails, the MSOT fix is not working correctly.", 
                   moveCount < 9);
        
        Timber.d("[TEST] SUCCESS: Level 5 solved with %d moves (expected < 9)", moveCount);
    }
    
    /**
     * Test that getGridElements() returns consistent data from gameElements.
     * This verifies the MSOT fix is working correctly.
     */
    @Test
    public void testGetGridElementsUsesGameElements() {
        Timber.d("[TEST] Starting testGetGridElementsUsesGameElements");
        
        // Load Level 5
        GameState state = GameState.loadLevel(context, 5);
        assertNotNull("Level 5 should load successfully", state);
        
        // Get grid elements
        ArrayList<GridElement> gridElements = state.getGridElements();
        
        // Count targets in gridElements
        int gridTargetCount = 0;
        for (GridElement element : gridElements) {
            if (element.getType().startsWith("target_")) {
                gridTargetCount++;
                Timber.d("[TEST] GridElement target: %s at (%d,%d)", 
                        element.getType(), element.getX(), element.getY());
            }
        }
        
        // Count targets in gameElements
        int gameTargetCount = 0;
        for (roboyard.eclabs.ui.GameElement element : state.getGameElements()) {
            if (element.getType() == roboyard.eclabs.ui.GameElement.TYPE_TARGET) {
                gameTargetCount++;
                Timber.d("[TEST] GameElement target at (%d,%d) with color %d", 
                        element.getX(), element.getY(), element.getColor());
            }
        }
        
        Timber.d("[TEST] Target counts: gridElements=%d, gameElements=%d", 
                gridTargetCount, gameTargetCount);
        
        // SSOT: gridElements should have same number of targets as gameElements
        assertEquals("GridElements should have same number of targets as gameElements (SSOT)", 
                     gameTargetCount, gridTargetCount);
        
        Timber.d("[TEST] SUCCESS: getGridElements() correctly uses gameElements as source");
    }
}
