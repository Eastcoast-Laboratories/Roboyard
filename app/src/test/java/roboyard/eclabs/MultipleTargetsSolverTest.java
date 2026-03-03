package roboyard.eclabs;

import org.junit.Test;
import org.junit.Before;

import driftingdroids.model.Board;
import driftingdroids.model.Move;
import driftingdroids.model.Solver;
import driftingdroids.model.Solution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TDD tests for multiple targets support in DriftingDroid solver.
 * These tests define the expected behavior BEFORE implementation.
 *
 * Tags: multi-target, solver, tdd, driftingdroids
 */
public class MultipleTargetsSolverTest {

    /**
     * Baseline test: Single goal still works after changes.
     * Green robot must reach its goal.
     */
    @Test
    public void testSingleGoal_GreenRobot() throws Exception {
        // Create a small 8x8 board with 4 robots
        Board board = Board.createBoardFreestyle(null, 8, 8, 4);
        board.removeGoals();

        // Place robots: pink(0), green(1), blue(2), yellow(3)
        board.setRobot(0, 0, false);  // pink at (0,0)
        board.setRobot(1, 1, false);  // green at (1,0)
        board.setRobot(2, 2, false);  // blue at (2,0)
        board.setRobot(3, 3, false);  // yellow at (3,0)

        // Add one goal: green robot (1) at position (1,7) = 1 + 7*8 = 57
        board.addGoal(57, 1, Board.GOAL_CIRCLE);
        board.setGoal(57);

        assertNotNull("Goal should be set", board.getGoal());
        assertEquals("Goal position should be 57", 57, board.getGoal().position);
        assertEquals("Goal robot should be 1 (green)", 1, board.getGoal().robotNumber);

        // Solve
        Solver solver = Solver.createInstance(board);
        List<Solution> solutions = solver.execute();

        assertNotNull("Solutions list should not be null", solutions);
        assertFalse("Should find at least one solution", solutions.isEmpty());

        System.out.println("[MULTI_TARGET_TEST] Single goal test: found " + solutions.size() + " solution(s)");
        System.out.println("[MULTI_TARGET_TEST] Solution: " + solutions.get(0).toString());
    }

    /**
     * Core feature test: Two goals with different robots.
     * Green robot AND yellow robot must both reach their goals.
     */
    @Test
    public void testTwoGoals_GreenAndYellow() throws Exception {
        // Create a small 8x8 board with 4 robots
        Board board = Board.createBoardFreestyle(null, 8, 8, 4);
        board.removeGoals();

        // Place robots: pink(0) at (0,0), green(1) at (1,0), blue(2) at (2,0), yellow(3) at (3,0)
        board.setRobot(0, 0, false);  // pink at (0,0) = position 0
        board.setRobot(1, 1, false);  // green at (1,0) = position 1
        board.setRobot(2, 2, false);  // blue at (2,0) = position 2
        board.setRobot(3, 3, false);  // yellow at (3,0) = position 3

        // Add two goals:
        // Green robot (1) goal at (1,7) = 1 + 7*8 = 57
        board.addGoal(57, 1, Board.GOAL_CIRCLE);
        // Yellow robot (3) goal at (3,7) = 3 + 7*8 = 59
        board.addGoal(59, 3, Board.GOAL_TRIANGLE);

        // Set primary goal (first one) for existing solver code
        board.setGoal(57);

        // Set active goals for multi-goal mode
        List<Board.Goal> activeGoals = new ArrayList<>();
        for (Board.Goal g : board.getGoals()) {
            if (g.position == 57 || g.position == 59) {
                activeGoals.add(g);
            }
        }
        assertEquals("Should have 2 active goals", 2, activeGoals.size());
        board.setActiveGoals(activeGoals);

        // Verify active goals are set
        List<Board.Goal> retrievedGoals = board.getActiveGoals();
        assertEquals("Should retrieve 2 active goals", 2, retrievedGoals.size());

        // Solve
        Solver solver = Solver.createInstance(board);
        List<Solution> solutions = solver.execute();

        assertNotNull("Solutions list should not be null", solutions);
        assertFalse("Should find at least one solution for 2 goals", solutions.isEmpty());

        // Verify solution details
        Solution solution = solutions.get(0);
        System.out.println("[MULTI_TARGET_TEST] Two goals test: found " + solutions.size() + " solution(s)");
        System.out.println("[MULTI_TARGET_TEST] Solution: " + solution.toMovelistString());
        System.out.println("[MULTI_TARGET_TEST] Solution moves: " + solution.size());

        // Replay solution to get final robot positions
        int[] positions = board.getRobotPositions().clone();
        solution.resetMoves();
        Move move;
        while ((move = solution.getNextMove()) != null) {
            positions[move.robotNumber] = move.newPosition;
            System.out.println("[MULTI_TARGET_TEST]   Move: robot " + move.robotNumber + " " + move.strOldNewPosition());
        }
        System.out.println("[MULTI_TARGET_TEST] Final positions:");
        for (int i = 0; i < positions.length; i++) {
            System.out.println("[MULTI_TARGET_TEST]   Robot " + i + " at position " + positions[i] + " (" + (positions[i] % 8) + "," + (positions[i] / 8) + ")");
        }

        // Verify both robots are at their goal positions
        assertEquals("Green robot (1) should be at goal position 57", 57, positions[1]);
        assertEquals("Yellow robot (3) should be at goal position 59", 59, positions[3]);
    }

    /**
     * Edge case: Three goals with different robots.
     */
    @Test
    public void testThreeGoals_GreenYellowBlue() throws Exception {
        Board board = Board.createBoardFreestyle(null, 8, 8, 4);
        board.removeGoals();

        // Place robots
        board.setRobot(0, 0, false);  // pink at (0,0)
        board.setRobot(1, 1, false);  // green at (1,0)
        board.setRobot(2, 2, false);  // blue at (2,0)
        board.setRobot(3, 3, false);  // yellow at (3,0)

        // Add three goals:
        board.addGoal(57, 1, Board.GOAL_CIRCLE);    // Green at (1,7)
        board.addGoal(58, 2, Board.GOAL_SQUARE);    // Blue at (2,7)
        board.addGoal(59, 3, Board.GOAL_TRIANGLE);  // Yellow at (3,7)

        board.setGoal(57);

        List<Board.Goal> activeGoals = new ArrayList<>();
        for (Board.Goal g : board.getGoals()) {
            if (g.position == 57 || g.position == 58 || g.position == 59) {
                activeGoals.add(g);
            }
        }
        assertEquals("Should have 3 active goals", 3, activeGoals.size());
        board.setActiveGoals(activeGoals);

        Solver solver = Solver.createInstance(board);
        List<Solution> solutions = solver.execute();

        assertNotNull("Solutions list should not be null", solutions);
        assertFalse("Should find at least one solution for 3 goals", solutions.isEmpty());

        System.out.println("[MULTI_TARGET_TEST] Three goals test: found " + solutions.size() + " solution(s)");
    }
}
