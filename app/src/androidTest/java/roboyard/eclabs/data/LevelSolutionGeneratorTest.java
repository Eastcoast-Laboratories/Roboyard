package roboyard.eclabs.data;

import static org.junit.Assert.*;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import driftingdroids.model.Solution;
import roboyard.eclabs.solver.SolverDD;
import roboyard.eclabs.solver.SolverStatus;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GridElement;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;

/**
 * Test that solves all 140 levels and generates solution data.
 * 
 * This test:
 * 1. Loads each level from assets
 * 2. Runs the solver to find optimal solutions
 * 3. Records the move count and solution string
 * 4. Outputs the data in a format that can be copied into LevelSolutionData.java
 * 
 * Run with: ./gradlew connectedAndroidTest --tests "roboyard.eclabs.data.LevelSolutionGeneratorTest"
 * 
 * The output will be in logcat with tag "LEVEL_SOLUTION_DATA"
 */
@RunWith(AndroidJUnit4.class)
public class LevelSolutionGeneratorTest {

    private static final String TAG = "LEVEL_SOLUTION_DATA";
    private Context context;
    private static final int MAX_LEVEL = 140;
    private static final int SOLVER_TIMEOUT_SECONDS = 260;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * Data class to hold solution results
     */
    private static class LevelResult {
        int levelId;
        int optimalMoves;
        int solutionCount;
        String solution;
        boolean solved;
        String error;
        
        LevelResult(int levelId) {
            this.levelId = levelId;
            this.solved = false;
            this.solutionCount = 0;
        }
    }

    /**
     * Main test that generates solution data for all levels
     */
    @Test
    public void generateAllLevelSolutions() {
        List<LevelResult> results = new ArrayList<>();
        
        Log.i(TAG, "========== STARTING LEVEL SOLUTION GENERATION ==========");
        Log.i(TAG, "Solving levels 1 to " + MAX_LEVEL + "...");
        
        for (int levelId = 1; levelId <= MAX_LEVEL; levelId++) {
            LevelResult result = solveLevel(levelId);
            results.add(result);
            
            if (result.solved) {
                Log.i(TAG, "Level " + levelId + ": " + result.optimalMoves + " moves (" + 
                    result.solutionCount + " solutions)");
            } else {
                Log.e(TAG, "Level " + levelId + ": FAILED - " + result.error);
            }
        }
        
        // Output summary
        outputSummary(results);
        
        // Output Java code for LevelSolutionData.java
        outputJavaCode(results);
        
        // Verify all levels were solved
        int solvedCount = 0;
        for (LevelResult result : results) {
            if (result.solved) solvedCount++;
        }
        
        Log.i(TAG, "========== COMPLETED: " + solvedCount + "/" + MAX_LEVEL + " levels solved ==========");
        
        assertEquals("All levels should be solved", MAX_LEVEL, solvedCount);
    }

    /**
     * Solve a single level and return the result
     */
    private LevelResult solveLevel(int levelId) {
        LevelResult result = new LevelResult(levelId);
        
        try {
            // Load the level
            GameState gameState = GameState.loadLevel(context, levelId);
            if (gameState == null) {
                result.error = "Failed to load level";
                return result;
            }
            
            // Get grid elements for solver
            ArrayList<GridElement> gridElements = gameState.getGridElements();
            if (gridElements == null || gridElements.isEmpty()) {
                result.error = "No grid elements in level";
                return result;
            }
            
            // Create and initialize solver
            SolverDD solver = new SolverDD();
            solver.init(gridElements);
            
            // Run solver with timeout
            final CountDownLatch latch = new CountDownLatch(1);
            
            Thread solverThread = new Thread(() -> {
                solver.run();
                latch.countDown();
            });
            
            solverThread.start();
            
            // Wait for solver with timeout
            boolean completed = latch.await(SOLVER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                solver.cancel();
                solverThread.interrupt();
                result.error = "Solver timeout after " + SOLVER_TIMEOUT_SECONDS + " seconds";
                return result;
            }
            
            // Get solution
            if (solver.getSolverStatus() == SolverStatus.solved) {
                List<Solution> solutions = solver.getSolutionList();
                if (solutions != null && !solutions.isEmpty()) {
                    result.solutionCount = solutions.size();
                    GameSolution gameSolution = solver.getSolution(0);
                    if (gameSolution != null) {
                        result.optimalMoves = gameSolution.getMoves().size();
                        result.solution = formatSolution(gameSolution);
                        result.solved = true;
                    } else {
                        result.error = "Solver returned null GameSolution";
                    }
                } else {
                    result.error = "Solver returned empty solution list";
                }
            } else {
                result.error = "Solver status: " + solver.getSolverStatus();
            }
            
        } catch (Exception e) {
            result.error = "Exception: " + e.getMessage();
            Log.e(TAG, "Error solving level " + levelId, e);
        }
        
        return result;
    }

    /**
     * Format a solution into a string representation
     */
    private String formatSolution(GameSolution solution) {
        StringBuilder sb = new StringBuilder();
        ArrayList<IGameMove> moves = solution.getMoves();
        
        for (int i = 0; i < moves.size(); i++) {
            if (i > 0) sb.append(",");
            IGameMove move = moves.get(i);
            sb.append(move.toString());
        }
        
        return sb.toString();
    }

    /**
     * Output summary statistics
     */
    private void outputSummary(List<LevelResult> results) {
        Log.i(TAG, "========== SUMMARY ==========");
        
        int[] moveCounts = new int[31]; // 0-30+ moves
        int minMoves = Integer.MAX_VALUE;
        int maxMoves = 0;
        int totalMoves = 0;
        int solvedCount = 0;
        int levelsWithFivePlusMoves = 0;
        
        for (LevelResult result : results) {
            if (result.solved) {
                solvedCount++;
                totalMoves += result.optimalMoves;
                minMoves = Math.min(minMoves, result.optimalMoves);
                maxMoves = Math.max(maxMoves, result.optimalMoves);
                
                int bucket = Math.min(result.optimalMoves, 30);
                moveCounts[bucket]++;
                
                if (result.optimalMoves >= 5) {
                    levelsWithFivePlusMoves++;
                }
            }
        }
        
        Log.i(TAG, "Solved: " + solvedCount + "/" + results.size() + " levels");
        Log.i(TAG, "Move range: " + minMoves + " - " + maxMoves);
        Log.i(TAG, "Average moves: " + String.format("%.1f", (double) totalMoves / solvedCount));
        Log.i(TAG, "Levels with 5+ moves: " + levelsWithFivePlusMoves);
        
        // Distribution
        Log.i(TAG, "Move distribution:");
        for (int i = 1; i <= 30; i++) {
            if (moveCounts[i] > 0) {
                String label = (i == 30) ? "30+" : String.valueOf(i);
                Log.i(TAG, "  " + label + " moves: " + moveCounts[i] + " levels");
            }
        }
    }

    /**
     * Output Java code that can be copied into LevelSolutionData.java
     */
    private void outputJavaCode(List<LevelResult> results) {
        Log.i(TAG, "========== JAVA CODE FOR LevelSolutionData.java ==========");
        Log.i(TAG, "// Copy this into initializeLevelData() method:");
        Log.i(TAG, "");
        
        for (LevelResult result : results) {
            if (result.solved) {
                Log.i(TAG, "addLevel(" + result.levelId + ", " + result.optimalMoves + ", \"" + 
                    result.solution + "\");");
            } else {
                Log.i(TAG, "// Level " + result.levelId + ": UNSOLVED - " + result.error);
            }
        }
        
        Log.i(TAG, "");
        Log.i(TAG, "========== END JAVA CODE ==========");
    }

    /**
     * Test a single level (for debugging)
     */
    @Test
    public void testSingleLevel() {
        int testLevelId = 1;
        LevelResult result = solveLevel(testLevelId);
        
        Log.i(TAG, "Single level test: Level " + testLevelId);
        if (result.solved) {
            Log.i(TAG, "  Optimal moves: " + result.optimalMoves);
            Log.i(TAG, "  Solution count: " + result.solutionCount);
            Log.i(TAG, "  Solution: " + result.solution);
        } else {
            Log.e(TAG, "  Failed: " + result.error);
        }
        
        assertTrue("Level should be solved", result.solved);
    }
}
