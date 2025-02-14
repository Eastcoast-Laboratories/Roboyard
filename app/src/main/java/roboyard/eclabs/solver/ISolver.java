package roboyard.eclabs.solver;

import java.util.ArrayList;
import java.util.List;

import driftingdroids.model.*;
import roboyard.eclabs.GridElement;
import roboyard.pm.ia.GameSolution;

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

    void init(ArrayList<GridElement> elements);
    void run();
    SolverStatus getSolverStatus();
    GameSolution getSolution(int num);
    List<Solution> getSolutionList();
}
