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
public class SolverManager implements Runnable {
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
        Timber.d("[SOLUTION_SOLVER] new SolverManager(): Creating DD World");
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
        
        solverThread = new Thread(this, "solver");
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
     * Gets the list of solutions
     * @return The list of solutions
     */
    public List<Solution> getSolutionList() {
        return solver.getSolutionList();
    }
    
    /**
     * Checks if the solution is a single move solution
     * @return True if the solution is a single move, false otherwise
     */
    public boolean isSolution01() {
        return solver.isSolution01();
    }
    
    /**
     * Cancels the solver
     */
    public void cancel() {
        if (solver instanceof SolverDD) {
            ((SolverDD)solver).cancel();
        }
    }
    
    /**
     * Runnable implementation for executing solver in a thread
     */
    @Override
    public void run() {
        Timber.d("[SOLUTION SOLVER] SolverManager.run() - Starting solver");
        try {
            startSolverInternal();
        } catch (Exception e) {
            Timber.e(e, "[SOLUTION SOLVER] Error running solver");
        }
    }
    
    /**
     * Internal implementation of the solver start logic
     */
    private void startSolverInternal() {
        try {
            Timber.d("[SOLUTION SOLVER] Starting solver with status: %s", solver.getSolverStatus());
            solver.run();
            // Check if the solver found a solution
            if (solver.getSolverStatus().isFinished()) {
                // Process solver results
                int numSolutions = solver.getSolutionList() != null ? solver.getSolutionList().size() : 0;
                Timber.d("[SOLUTION SOLVER] Solver finished, found %d solutions", numSolutions);
                
                boolean success = numSolutions > 0;
                if (success) {
                    // Get the first solution and process it
                    currentSolution = solver.getSolution(0);
                    int moveCount = 0;
                    if (currentSolution != null && currentSolution.getMoves() != null) {
                        moveCount = currentSolution.getMoves().size();
                        Timber.d("[SOLUTION SOLVER] First solution has %d moves: %s", 
                            moveCount, currentSolution.getMoves());
                    } else {
                        Timber.w("[SOLUTION SOLVER] Solution or moves is null!");
                    }
                    
                    // Call the listener with the results
                    if (listener != null) {
                        Timber.d("[SOLUTION SOLVER] Notifying listener of successful solution");
                        listener.onSolverFinished(true, moveCount, numSolutions);
                    } else {
                        Timber.w("[SOLUTION SOLVER] Listener is null, cannot notify of solution");
                    }
                } else {
                    // No solution found
                    Timber.d("[SOLUTION SOLVER] No solution found");
                    if (listener != null) {
                        listener.onSolverFinished(false, 0, 0);
                    } else {
                        Timber.w("[SOLUTION SOLVER] Listener is null, cannot notify of failure");
                    }
                }
            } else {
                Timber.d("[SOLUTION SOLVER] Solver did not finish properly, status: %s", 
                    solver.getSolverStatus());
                if (listener != null) {
                    listener.onSolverCancelled();
                }
            }
        } catch (Exception e) {
            Timber.e(e, "[SOLUTION SOLVER] Exception in solver processing");
            if (listener != null) {
                listener.onSolverCancelled();
            }
        }
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
