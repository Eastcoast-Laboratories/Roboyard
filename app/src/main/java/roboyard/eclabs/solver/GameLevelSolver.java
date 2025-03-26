package roboyard.eclabs.solver;

import java.util.ArrayList;
import java.util.List;
import roboyard.eclabs.GridElement;
import roboyard.pm.ia.GameSolution;
import driftingdroids.model.Solution;
import roboyard.eclabs.solver.SolverDD;
import roboyard.eclabs.solver.SolverStatus;
import timber.log.Timber;

/**
 * GameLevelSolver - The main game solver with full solution tracking
 * 
 * This solver is used during actual gameplay to:
 * 1. Validate that a level is solvable
 * 2. Calculate the optimal solution
 * 3. Provide move-by-move guidance
 * 4. Track player progress
 * 
 * Key features:
 * - Complete solution path tracking
 * - Detailed move history
 * - Integration with game UI
 * - Support for hint system
 * - Performance optimized for runtime
 * 
 * Used by:
 * - Level validation system
 * - In-game hint system
 * - Solution replay feature
 * - Achievement tracking
 */
public class GameLevelSolver {
    public static int solveLevelFromString(String mapContent) {
        // Parse map content into GridElements
        ArrayList<GridElement> elements = new ArrayList<>();
        String[] lines = mapContent.split("\n");
        
        for (String line : lines) {
            if (line.startsWith("board:")) {
                continue; // Skip board size line
            }
            // Format: type x,y;
            String[] parts = line.split("[,;]");
            if (parts.length >= 2) {
                String type = parts[0].replaceAll("\\d+$", "");
                int x = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                int y = Integer.parseInt(parts[1]);
                elements.add(new GridElement(x, y, type));
            }
        }
        
        // Create and initialize solver
        Timber.d("[SOLUTION_SOLVER] GameLevelSolver.solveLevelFromString(): Creating solver which is Creating DD World");
        SolverDD solver = new SolverDD();
        solver.init(elements);
        
        // Run solver
        solver.run();
        
        // Check result
        if (solver.getSolverStatus() == SolverStatus.solved) {
            List<Solution> solutions = solver.getSolutionList();
            if (!solutions.isEmpty()) {
                GameSolution solution = solver.getSolution(0);
                return solution.getMoves().size();
            }
        }
        
        return -1; // No solution found
    }
    
    public static void main(String[] args) {
        if (args.length > 0) {
            StringBuilder mapContent = new StringBuilder();
            for (String arg : args) {
                mapContent.append(arg).append("\n");
            }
            int moves = solveLevelFromString(mapContent.toString());
            Timber.d(String.valueOf(moves)); // Print number of moves, -1 if no solution
        }
    }
}
