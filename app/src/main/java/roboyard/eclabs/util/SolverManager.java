package roboyard.eclabs.util;

import java.util.ArrayList;
import java.util.List;

import driftingdroids.model.Solution;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;
import roboyard.eclabs.solver.SolverStatus;
import roboyard.pm.ia.GameSolution;
import timber.log.Timber;

/**
 * A utility class that manages the initialization and execution of the game solver.
 * This class encapsulates the solver functionality in a UI-agnostic way,
 * making it reusable across different UI implementations.
 */
public class SolverManager {
    private ISolver solver;
    private Thread solverThread;
    private boolean isSolved = false;
    private int solutionMoves = 0;
    private int numDifferentSolutionsFound = 0;
    private GameSolution currentSolution;
    private SolverListener listener;

    /**
     * Interface for receiving solver events
     */
    public interface SolverListener {
        /**
         * Called when the solver has finished finding a solution
         * @param success Whether a solution was found
         * @param solutionMoves Number of moves in the solution (0 if no solution)
         * @param numSolutions Number of different solutions found
         */
        void onSolverFinished(boolean success, int solutionMoves, int numSolutions);
        
        /**
         * Called when the solver is cancelled
         */
        void onSolverCancelled();
    }
    
    /**
     * Creates a new SolverManager instance
     */
    public SolverManager() {
        this.solver = new SolverDD();
    }
    
    /**
     * Sets the listener for solver events
     * @param listener The listener to receive events
     */
    public void setListener(SolverListener listener) {
        this.listener = listener;
    }
    
    /**
     * Initializes the solver with the given grid elements
     * @param gridElements The grid elements to initialize the solver with
     */
    public void initialize(ArrayList<GridElement> gridElements) {
        this.solver.init(gridElements);
        this.isSolved = false;
    }
    
    /**
     * Starts the solver in a background thread
     */
    public void startSolver() {
        if (solverThread != null && solverThread.isAlive()) {
            Timber.d("Solver thread is already running");
            return;
        }
        
        solverThread = new Thread(solver, "solver");
        solverThread.start();
    }
    
    /**
     * Checks if the solver has finished and processes the result if it has
     * @return True if the solver has finished, false otherwise
     */
    public boolean checkSolverStatus() {
        if (!isSolved && solver.getSolverStatus().isFinished()) {
            isSolved = true;
            numDifferentSolutionsFound = solver.getSolutionList() != null ? solver.getSolutionList().size() : 0;
            
            if (numDifferentSolutionsFound > 0) {
                currentSolution = solver.getSolution(0);
                solutionMoves = 0;
                if (currentSolution != null && currentSolution.getMoves() != null) {
                    solutionMoves = currentSolution.getMoves().size();
                }
                
                if (solver.isSolution01()) {
                    solutionMoves = 1;
                }
                
                if (listener != null) {
                    listener.onSolverFinished(true, solutionMoves, numDifferentSolutionsFound);
                }
                return true;
            } else {
                if (listener != null) {
                    listener.onSolverFinished(false, 0, 0);
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cancels the solver execution
     */
    public void cancelSolver() {
        if (solver != null) {
            if (solver instanceof SolverDD) {
                ((SolverDD)solver).cancel();
            }
            if (listener != null) {
                listener.onSolverCancelled();
            }
        }
    }
    
    /**
     * Gets the current solution
     * @return The current solution, or null if no solution is available
     */
    public GameSolution getCurrentSolution() {
        return currentSolution;
    }
    
    /**
     * Gets a specific solution by index
     * @param index The index of the solution to get
     * @return The solution at the specified index, or null if no solution is available
     */
    public GameSolution getSolution(int index) {
        if (solver != null && solver.getSolutionList() != null && index < solver.getSolutionList().size()) {
            return solver.getSolution(index);
        }
        return null;
    }
    
    /**
     * Gets the number of moves in the current solution
     * @return The number of moves in the current solution
     */
    public int getSolutionMoves() {
        return solutionMoves;
    }
    
    /**
     * Gets the number of different solutions found
     * @return The number of different solutions found
     */
    public int getNumDifferentSolutionsFound() {
        return numDifferentSolutionsFound;
    }
    
    /**
     * Checks if the solver has found a solution
     * @return True if a solution has been found, false otherwise
     */
    public boolean isSolved() {
        return isSolved;
    }
    
    /**
     * Gets the underlying solver instance
     * @return The solver instance
     */
    public ISolver getSolver() {
        return solver;
    }
}
