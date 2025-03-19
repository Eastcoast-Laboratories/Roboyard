package roboyard.eclabs.solver;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.GridElement;
import roboyard.eclabs.ui.GameMove;
import roboyard.pm.ia.GameSolution;
import roboyard.eclabs.solver.SolverStatus;

/**
 * Interface for puzzle solvers in the Roboyard game.
 * Defines the contract that any solver implementation must fulfill,
 * including initialization, solution finding, and status reporting.
 * 
 * This interface allows the game to use different solver implementations
 * while maintaining a consistent API. Currently implemented by SolverDD
 * which uses the DriftingDroids solver.
 *
 * @author Pierre Michel
 * @since 15/04/2015
 * @see roboyard.eclabs.solver.SolverDD
 * @see roboyard.eclabs.solver.SolverStatus
 */
public interface ISolver extends Runnable {

    /**
     * Initialize the solver with a list of grid elements
     * @param elements List of grid elements
     */
    void init(ArrayList<GridElement> elements);
    
    /**
     * Initialize the solver with a 2D array representing the board
     * @param boardData 2D array representing the board
     */
    void init(int[][] boardData);
    
    /**
     * Run the solver in a background thread
     */
    void run();
    
    /**
     * Get the current status of the solver
     * @return Current solver status
     */
    SolverStatus getSolverStatus();
    
    /**
     * Get a specific solution
     * @param num Solution index
     * @return The solution at the specified index
     */
    GameSolution getSolution(int num);
    
    /**
     * Get all available solutions
     * @return List of all solutions
     */
    List<GameSolution> getSolutionList();
    
    /**
     * Get the next move hint for the current board state
     * @param boardData Current board state
     * @return Next move hint
     */
    GameMove getNextMove(int[][] boardData);
    
    /**
     * Check if the solution can be reached in one move
     * @return true if the goal can be reached in one move
     */
    boolean isSolution01();
}
