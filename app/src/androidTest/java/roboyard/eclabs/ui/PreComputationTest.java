package roboyard.eclabs.ui;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import roboyard.logic.core.GridElement;
import roboyard.logic.core.GameSolution;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.logic.solver.SolverDD;
import roboyard.ui.util.LiveSolverManager;
import timber.log.Timber;

/**
 * Instrumented test for the pre-computation cache of next possible moves.
 *
 * Uses a specific 19x19 map with known 21-move solution from initial position:
 * solution: yE yS yW yS bE bN gS gE rW rS rE gS yW rS rE gN rW bW rS gE gS
 *
 * Robots initial: r=(5,4), g=(4,5), b=(12,17), y=(10,1)
 * Target: G=(11,14) (green)
 *
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.PreComputationTest
 */
@RunWith(AndroidJUnit4.class)
public class PreComputationTest {

    private Context context;
    private ArrayList<GridElement> mapElements;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mapElements = buildTestMap();
    }

    /**
     * Build the 19x19 test map with robots, target, and walls from the ASCII map.
     */
    private ArrayList<GridElement> buildTestMap() {
        ArrayList<GridElement> elements = new ArrayList<>();

        // Robots INITIAL position:
        // PINK=0(red), GREEN=1, BLUE=2, YELLOW=3
        // In solver: robot_red maps to COLOR_PINK=0
        elements.add(new GridElement(5, 4, "robot_red"));     // r at (5,4)
        elements.add(new GridElement(4, 5, "robot_green"));   // g at (4,5)
        elements.add(new GridElement(12, 17, "robot_blue"));  // b at (12,17)
        elements.add(new GridElement(10, 1, "robot_yellow")); // y at (10,1) INITIAL

        // Target: green at (11,14)
        elements.add(new GridElement(11, 14, "target_green"));

        // Border walls (outer edges) - the solver adds missing ones automatically,
        // but we add them explicitly for the 19x19 board
        for (int x = 0; x < 19; x++) {
            elements.add(new GridElement(x, 0, "mh"));  // top border
            elements.add(new GridElement(x, 18, "mh")); // bottom border (at y=18 south side)
        }
        for (int y = 0; y < 19; y++) {
            elements.add(new GridElement(0, y, "mv"));   // left border
            elements.add(new GridElement(18, y, "mv"));  // right border
        }

        // Internal walls parsed from the ASCII map
        // Format: mh at (x,y) = horizontal wall on north side of cell (x,y)
        //         mv at (x,y) = vertical wall on west side of cell (x,y)

        // Row 0: |‾ ‾ ‾ ‾ ‾ ‾ ‾|‾ ‾ ‾ ‾ ‾ ‾ ‾ ‾ ‾|‾ ‾|
        elements.add(new GridElement(7, 0, "mv"));  // | between col 6 and 7
        elements.add(new GridElement(16, 0, "mv")); // | between col 15 and 16

        // Row 2: . . ‾|. .|‾ . . . . . . .|. . . . .
        elements.add(new GridElement(3, 2, "mh"));  // ‾ above (2,2) → mh at (2,2)... 
        // Actually let me re-read the map more carefully.
        // The ASCII format is: |vwall cell vwall cell ...
        // ‾ after a cell means horizontal wall on south side of previous row / north side of current
        // | after a cell means vertical wall on east side

        // Let me re-parse more carefully from the ASCII map:
        // Row 2: |. . ‾|. .|‾ . . . . . . .|. . . . .|
        //   (2,2) has ‾ on top → mh at (2,2)
        //   (3,2) has | on right → mv at (4,2)... no wait
        // 
        // The ASCII map format from generateAsciiMap:
        //   For each cell (x,y): vWall(x) + cell(x) 
        //   vWall = "|" if mv at (x,y), else " "
        //   cell = "‾" if mh at (x,y), else robot/target/dot

        // So: mv at (x,y) means | appears before cell (x,y) = wall on west side of (x,y)
        //     mh at (x,y) means ‾ appears in cell (x,y) = wall on north side of (x,y)

        // Row 2: |. . ‾|. .|‾ . . . . . . .|. . . . .|
        // Position analysis (each cell is 2 chars: vwall+content):
        // x=0:|.  x=1: .  x=2: ‾  x=3:|.  x=4: .  x=5:|‾  x=6: .  x=7: .  x=8: .  x=9: .  x=10: .  x=11: .  x=12: .  x=13:|.  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(2, 2, "mh"));   // ‾ at cell (2,2)
        elements.add(new GridElement(3, 2, "mv"));   // | before cell (3,2)
        elements.add(new GridElement(5, 2, "mv"));   // | before cell (5,2)
        elements.add(new GridElement(5, 2, "mh"));   // ‾ at cell (5,2)
        elements.add(new GridElement(13, 2, "mv"));  // | before cell (13,2)

        // Row 3: |‾ . . . . . .|. . . ‾|. ‾ . . .|‾ .|
        elements.add(new GridElement(0, 3, "mh"));   // ‾ at cell (0,3)
        elements.add(new GridElement(7, 3, "mv"));   // | before cell (7,3)... wait
        // Let me re-read: |‾ . . . . . .|. . . ‾|. ‾ . . .|‾ .| 
        // x=0:|‾  x=1: .  x=2: .  x=3: .  x=4: .  x=5: .  x=6: .  x=7:|.  x=8: .  x=9: .  x=10: ‾  x=11:|.  x=12: ‾  x=13: .  x=14: .  x=15: .  x=16:|‾  x=17: .|
        elements.add(new GridElement(7, 3, "mv"));
        elements.add(new GridElement(10, 3, "mh"));
        elements.add(new GridElement(11, 3, "mv"));
        elements.add(new GridElement(12, 3, "mh"));
        elements.add(new GridElement(16, 3, "mv"));
        elements.add(new GridElement(16, 3, "mh"));

        // Row 4: |. ‾|. ‾|r . . . ‾|. . . . . . . . .|
        // x=0:|.  x=1: ‾  x=2:|.  x=3: ‾  x=4:|r  x=5: .  x=6: .  x=7: .  x=8: ‾  x=9:|.  x=10: .  x=11: .  x=12: .  x=13: .  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(1, 4, "mh"));
        elements.add(new GridElement(2, 4, "mv"));
        elements.add(new GridElement(3, 4, "mh"));
        elements.add(new GridElement(4, 4, "mv"));
        elements.add(new GridElement(8, 4, "mh"));
        elements.add(new GridElement(9, 4, "mv"));

        // Row 5: |. . . . g .|. . . . ‾|. . . . . . .|
        // x=0:|.  x=1: .  x=2: .  x=3: .  x=4: g  x=5: .  x=6:|.  x=7: .  x=8: .  x=9: .  x=10: ‾  x=11:|.  x=12: .  x=13: .  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(6, 5, "mv"));
        elements.add(new GridElement(10, 5, "mh"));
        elements.add(new GridElement(11, 5, "mv"));

        // Row 6: |. . . . . ‾ . . .|‾ . . . .|. ‾|. ‾|
        // x=0:|.  x=1: .  x=2: .  x=3: .  x=4: .  x=5: ‾  x=6: .  x=7: .  x=8: .  x=9:|‾  x=10: .  x=11: .  x=12: .  x=13: .  x=14:|.  x=15: ‾  x=16:|.  x=17: ‾|
        elements.add(new GridElement(5, 6, "mh"));
        elements.add(new GridElement(9, 6, "mv"));
        elements.add(new GridElement(9, 6, "mh"));
        elements.add(new GridElement(14, 6, "mv"));
        elements.add(new GridElement(15, 6, "mh"));
        elements.add(new GridElement(16, 6, "mv"));
        elements.add(new GridElement(17, 6, "mh"));

        // Row 7: |. . .|‾ . . . ‾ . . . .|. ‾ . . . .|
        // x=0:|.  x=1: .  x=2: .  x=3:|‾  x=4: .  x=5: .  x=6: .  x=7: ‾  x=8: .  x=9: .  x=10: .  x=11: .  x=12:|.  x=13: ‾  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(3, 7, "mv"));
        elements.add(new GridElement(3, 7, "mh"));
        elements.add(new GridElement(7, 7, "mh"));
        elements.add(new GridElement(12, 7, "mv"));
        elements.add(new GridElement(13, 7, "mh"));

        // Row 8: |. . . . . . . .|‾ ‾|. . ‾ . . . . .|
        // x=0:|.  x=1: .  x=2: .  x=3: .  x=4: .  x=5: .  x=6: .  x=7: .  x=8:|‾  x=9: ‾  x=10:|.  x=11: .  x=12: ‾  x=13: .  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(8, 8, "mv"));
        elements.add(new GridElement(8, 8, "mh"));
        elements.add(new GridElement(9, 8, "mh"));
        elements.add(new GridElement(10, 8, "mv"));
        elements.add(new GridElement(12, 8, "mh"));

        // Row 9: |. . . ‾|. .|. .|. .|. . . . ‾ . ‾ .|
        // x=0:|.  x=1: .  x=2: .  x=3: ‾  x=4:|.  x=5: .  x=6:|.  x=7: .  x=8:|.  x=9: .  x=10:|.  x=11: .  x=12: .  x=13: .  x=14: ‾  x=15: .  x=16: ‾  x=17: .|
        elements.add(new GridElement(3, 9, "mh"));
        elements.add(new GridElement(4, 9, "mv"));
        elements.add(new GridElement(6, 9, "mv"));
        elements.add(new GridElement(8, 9, "mv"));
        elements.add(new GridElement(10, 9, "mv"));
        elements.add(new GridElement(14, 9, "mh"));
        elements.add(new GridElement(16, 9, "mh"));

        // Row 10: |. ‾|. . . . ‾ . ‾ ‾ . . . . .|. . .|
        // x=0:|.  x=1: ‾  x=2:|.  x=3: .  x=4: .  x=5: .  x=6: ‾  x=7: .  x=8: ‾  x=9: ‾  x=10: .  x=11: .  x=12: .  x=13: .  x=14: .  x=15:|.  x=16: .  x=17: .|
        elements.add(new GridElement(1, 10, "mh"));
        elements.add(new GridElement(2, 10, "mv"));
        elements.add(new GridElement(6, 10, "mh"));
        elements.add(new GridElement(8, 10, "mh"));
        elements.add(new GridElement(9, 10, "mh"));
        elements.add(new GridElement(15, 10, "mv"));

        // Row 11: |‾ . . .|. ‾ . . . . . .|. . ‾ . . .|
        // x=0:|‾  x=1: .  x=2: .  x=3: .  x=4:|.  x=5: ‾  x=6: .  x=7: .  x=8: .  x=9: .  x=10: .  x=11: .  x=12:|.  x=13: .  x=14: ‾  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(0, 11, "mh"));
        elements.add(new GridElement(4, 11, "mv"));
        elements.add(new GridElement(5, 11, "mh"));
        elements.add(new GridElement(12, 11, "mv"));
        elements.add(new GridElement(14, 11, "mh"));

        // Row 12: |. .|‾ . ‾ . ‾|. . . . ‾ . . . . . ‾|
        // x=0:|.  x=1: .  x=2:|‾  x=3: .  x=4: ‾  x=5: .  x=6: ‾  x=7:|.  x=8: .  x=9: .  x=10: .  x=11: ‾  x=12: .  x=13: .  x=14: .  x=15: .  x=16: .  x=17: ‾|
        elements.add(new GridElement(2, 12, "mv"));
        elements.add(new GridElement(2, 12, "mh"));
        elements.add(new GridElement(4, 12, "mh"));
        elements.add(new GridElement(6, 12, "mh"));
        elements.add(new GridElement(7, 12, "mv"));
        elements.add(new GridElement(11, 12, "mh"));
        elements.add(new GridElement(17, 12, "mh"));

        // Row 13: |. . . . .|. . . . ‾|. . .|‾ . . . .|
        // x=0:|.  x=1: .  x=2: .  x=3: .  x=4: .  x=5:|.  x=6: .  x=7: .  x=8: .  x=9: ‾  x=10:|.  x=11: .  x=12: .  x=13:|‾  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(5, 13, "mv"));
        elements.add(new GridElement(9, 13, "mh"));
        elements.add(new GridElement(10, 13, "mv"));
        elements.add(new GridElement(13, 13, "mv"));
        elements.add(new GridElement(13, 13, "mh"));

        // Row 14: |. . .|. ‾ . . . . . . G . . . . . .|
        // x=0:|.  x=1: .  x=2: .  x=3:|.  x=4: ‾  x=5: .  x=6: .  x=7: .  x=8: .  x=9: .  x=10: .  x=11: G  x=12: .  x=13: .  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(3, 14, "mv"));
        elements.add(new GridElement(4, 14, "mh"));

        // Row 15: |. . . ‾ . . . . ‾|. .|. . . . .|‾ .|
        // x=0:|.  x=1: .  x=2: .  x=3: ‾  x=4: .  x=5: .  x=6: .  x=7: .  x=8: ‾  x=9:|.  x=10: .  x=11:|.  x=12: .  x=13: .  x=14: .  x=15: .  x=16:|‾  x=17: .|
        elements.add(new GridElement(3, 15, "mh"));
        elements.add(new GridElement(8, 15, "mh"));
        elements.add(new GridElement(9, 15, "mv"));
        elements.add(new GridElement(11, 15, "mv"));
        elements.add(new GridElement(16, 15, "mv"));
        elements.add(new GridElement(16, 15, "mh"));

        // Row 16: |. .|‾ . . . . . . . . ‾ . . . . . .|
        // x=0:|.  x=1: .  x=2:|‾  x=3: .  x=4: .  x=5: .  x=6: .  x=7: .  x=8: .  x=9: .  x=10: .  x=11: ‾  x=12: .  x=13: .  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(2, 16, "mv"));
        elements.add(new GridElement(2, 16, "mh"));
        elements.add(new GridElement(11, 16, "mh"));

        // Row 17: |. . . .|. . . . . . .|. b . . . . .|
        // x=0:|.  x=1: .  x=2: .  x=3: .  x=4:|.  x=5: .  x=6: .  x=7: .  x=8: .  x=9: .  x=10: .  x=11:|.  x=12: b  x=13: .  x=14: .  x=15: .  x=16: .  x=17: .|
        elements.add(new GridElement(4, 17, "mv"));
        elements.add(new GridElement(11, 17, "mv"));

        return elements;
    }

    /**
     * Verify the solver finds a 21-move solution for this map.
     */
    @Test
    public void testSolverFinds21MoveSolution() {
        Timber.d("[PRECOMP_TEST] Testing solver with 19x19 map");
        
        // Log robot positions for debugging
        for (GridElement e : mapElements) {
            if (e.getType().startsWith("robot_")) {
                Timber.d("[PRECOMP_TEST] Robot %s at (%d,%d)", e.getType(), e.getX(), e.getY());
            }
        }

        SolverDD solver = new SolverDD();
        solver.init(mapElements);
        solver.run();

        assertTrue("Solver should finish", solver.getSolverStatus().isFinished());
        assertNotNull("Solution list should not be null", solver.getSolutionList());
        assertTrue("Should have at least one solution", solver.getSolutionList().size() > 0);

        GameSolution solution = solver.getSolution(0);
        assertNotNull("First solution should not be null", solution);
        int moves = solution.getMoves().size();
        Timber.d("[PRECOMP_TEST] Solution found with %d moves", moves);
        
        // Log ALL moves using toString
        if (solution.getMoves() != null && solution.getMoves().size() > 0) {
            for (int i = 0; i < solution.getMoves().size(); i++) {
                IGameMove m = solution.getMoves().get(i);
                Timber.d("[PRECOMP_TEST] Move %d: %s (class: %s)", i, m.toString(), m.getClass().getSimpleName());
            }
        }
        
        assertEquals("Solution should have 21 moves", 21, moves);
    }

    /**
     * Test that the LiveSolverManager correctly solves from the initial position.
     */
    @Test
    public void testLiveSolverFromInitialPosition() throws InterruptedException {
        Timber.d("[PRECOMP_TEST] Testing LiveSolverManager from initial position");

        LiveSolverManager liveSolver = new LiveSolverManager();
        CountDownLatch latch = new CountDownLatch(1);
        final int[] result = {-1};

        liveSolver.solveAsync(mapElements, new LiveSolverManager.LiveSolverListener() {
            @Override
            public void onLiveSolverFinished(int optimalMoves, GameSolution solution) {
                result[0] = optimalMoves;
                Timber.d("[PRECOMP_TEST] LiveSolver result: %d moves", optimalMoves);
                latch.countDown();
            }

            @Override
            public void onLiveSolverFailed() {
                Timber.e("[PRECOMP_TEST] LiveSolver failed");
                latch.countDown();
            }
        });

        assertTrue("LiveSolver should complete within 60 seconds", latch.await(60, TimeUnit.SECONDS));
        assertEquals("LiveSolver should find 21-move solution", 21, result[0]);

        liveSolver.shutdown();
    }

    /**
     * Test pre-computation: after solving the initial state (21 moves),
     * simulate moving yellow East (first move of the solution: yE)
     * and verify the solver finds 20 remaining moves.
     */
    @Test
    public void testPreComputationAfterFirstMove() throws InterruptedException {
        Timber.d("[PRECOMP_TEST] Testing pre-computation after first move (yE)");

        // First solve from initial position
        LiveSolverManager liveSolver = new LiveSolverManager();
        CountDownLatch latch1 = new CountDownLatch(1);
        final int[] initialResult = {-1};

        liveSolver.solveAsync(mapElements, new LiveSolverManager.LiveSolverListener() {
            @Override
            public void onLiveSolverFinished(int optimalMoves, GameSolution solution) {
                initialResult[0] = optimalMoves;
                latch1.countDown();
            }
            @Override
            public void onLiveSolverFailed() { latch1.countDown(); }
        });
        assertTrue("Initial solve should complete", latch1.await(60, TimeUnit.SECONDS));
        assertEquals("Solution should be 21 moves", 21, initialResult[0]);

        // Now simulate yE (yellow moves East from (10,1) to (17,1) - slides to right wall)
        ArrayList<GridElement> postMoveElements = new ArrayList<>();
        for (GridElement e : mapElements) {
            if (e.getType().equals("robot_yellow")) {
                postMoveElements.add(new GridElement(17, 1, "robot_yellow"));
                Timber.d("[PRECOMP_TEST] Yellow moved East: (10,1) → (17,1)");
            } else {
                postMoveElements.add(e);
            }
        }

        // Solve from post-yE position
        CountDownLatch latch2 = new CountDownLatch(1);
        final int[] postMoveResult = {-1};

        liveSolver.solveAsync(postMoveElements, new LiveSolverManager.LiveSolverListener() {
            @Override
            public void onLiveSolverFinished(int optimalMoves, GameSolution solution) {
                postMoveResult[0] = optimalMoves;
                Timber.d("[PRECOMP_TEST] Post-yE result: %d moves remaining", optimalMoves);
                latch2.countDown();
            }
            @Override
            public void onLiveSolverFailed() { latch2.countDown(); }
        });
        assertTrue("Post-move solve should complete", latch2.await(60, TimeUnit.SECONDS));
        assertEquals("After yE, should need 20 remaining moves", 20, postMoveResult[0]);

        liveSolver.shutdown();
    }

    /**
     * Test that pre-computation sequentially solves multiple robots in multiple directions.
     * Mirrors the real GameStateManager.preComputeNextMoves() logic:
     * - Sequential execution (one solver at a time)
     * - 60s timeout per solver
     * - Detailed [PRECOMP] logging for each robot+direction
     *
     * Tests 4 specific hypothetical moves (one per robot) to verify that
     * the solver runs for each robot, not just the first one.
     * Each solve can take 25s+, so total test time can be ~2-4 minutes.
     */
    @Test
    public void testSequentialPreComputationMultipleRobots() {
        Timber.d("[PRECOMP_TEST] === Starting sequential pre-computation test ===");
        Timber.d("[PRECOMP_TEST] Testing that solver runs for MULTIPLE robots sequentially");

        // 4 hypothetical moves: one per robot, each sliding to a board edge
        // These simulate what preComputeNextMoves() would compute
        // Robot positions: r=(5,4), g=(4,5), b=(12,17), y=(10,1)
        String[][] testMoves = {
            // {robotType, robotName, direction, newX, newY}
            {"robot_red",    "red",    "E", "17", "4"},   // red slides East to wall
            {"robot_green",  "green",  "S", "4",  "17"},  // green slides South to wall
            {"robot_blue",   "blue",   "E", "17", "17"},  // blue slides East to wall
            {"robot_yellow", "yellow", "E", "17", "1"},   // yellow slides East to wall (first move of solution)
        };

        int solved = 0;
        int timeout = 0;
        long totalStart = System.currentTimeMillis();

        for (int i = 0; i < testMoves.length; i++) {
            String robotType = testMoves[i][0];
            String robotName = testMoves[i][1];
            String direction = testMoves[i][2];
            int newX = Integer.parseInt(testMoves[i][3]);
            int newY = Integer.parseInt(testMoves[i][4]);

            Timber.d("[PRECOMP_TEST] [%d/%d] Solving: %s %s → (%d,%d)...",
                    i + 1, testMoves.length, robotName, direction, newX, newY);
            long solveStart = System.currentTimeMillis();

            // Build hypothetical state with this robot moved
            ArrayList<GridElement> hypothetical = new ArrayList<>();
            for (GridElement e : mapElements) {
                if (e.getType().equals(robotType)) {
                    hypothetical.add(new GridElement(newX, newY, robotType));
                } else {
                    hypothetical.add(e);
                }
            }

            // Solve with 60s timeout (same as real pre-computation)
            SolverDD solver = new SolverDD();
            solver.init(hypothetical);

            java.util.concurrent.ExecutorService solverExec = java.util.concurrent.Executors.newSingleThreadExecutor();
            java.util.concurrent.Future<?> future = solverExec.submit(() -> solver.run());
            boolean completed = false;
            try {
                future.get(60, TimeUnit.SECONDS);
                completed = true;
            } catch (java.util.concurrent.TimeoutException te) {
                future.cancel(true);
                long elapsed = System.currentTimeMillis() - solveStart;
                Timber.w("[PRECOMP_TEST] [%d/%d] TIMEOUT after %dms: %s %s → (%d,%d)",
                        i + 1, testMoves.length, elapsed, robotName, direction, newX, newY);
                timeout++;
            } catch (Exception ex) {
                Timber.e(ex, "[PRECOMP_TEST] Solver error");
            } finally {
                solverExec.shutdownNow();
            }

            long solveElapsed = System.currentTimeMillis() - solveStart;

            if (completed && solver.getSolverStatus().isFinished()) {
                int numSolutions = solver.getSolutionList() != null ? solver.getSolutionList().size() : 0;
                if (numSolutions > 0) {
                    GameSolution solution = solver.getSolution(0);
                    int moves = (solution != null && solution.getMoves() != null) ? solution.getMoves().size() : 0;
                    if (solver.isSolution01()) moves = 1;
                    solved++;
                    Timber.d("[PRECOMP_TEST] [%d/%d] Solved in %dms: %s %s → (%d,%d) = %d moves",
                            i + 1, testMoves.length, solveElapsed, robotName, direction, newX, newY, moves);
                } else {
                    Timber.d("[PRECOMP_TEST] [%d/%d] No solution in %dms: %s %s → (%d,%d)",
                            i + 1, testMoves.length, solveElapsed, robotName, direction, newX, newY);
                }
            }
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        Timber.d("[PRECOMP_TEST] === Finished: %d solved, %d timeout, total %dms ===",
                solved, timeout, totalElapsed);

        // Verify that at least 2 different robots were solved (not just the first one)
        assertTrue("Should have solved at least 2 robots (got " + solved + ")", solved >= 2);
    }
}
