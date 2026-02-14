package roboyard.eclabs.util;

import java.util.ArrayList;
import java.util.List;

import driftingdroids.model.Solution;

import roboyard.eclabs.solver.GameLevelSolver;
import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;
import roboyard.eclabs.solver.SolverStatus;
import roboyard.logic.core.GridElement;
import roboyard.pm.ia.GameSolution;
import timber.log.Timber;

/**
 * A utility class that manages the initialization and execution of the game solver.
 * This class encapsulates the solver functionality in a UI-agnostic way,
 * making it reusable across different UI implementations.
 * Implements the singleton pattern to ensure only one solver instance exists.
 */
public class SolverManager implements Runnable {
    // Singleton instance
    private static SolverManager instance = null;
    
    private final ISolver solver;
    private Thread solverThread;
    private boolean isSolved = false;
    private int solutionMoves = 0;
    private int numDifferentSolutionsFound = 0;
    private GameSolution currentSolution;
    private SolverListener listener;
    // Track initialization state
    private boolean isInitialized = false;
    
    // Predefined solution from level file (for levels too complex to solve at runtime)
    private String predefinedSolution = null;
    private int predefinedNumMoves = 0;
    
    // Unique solver invocation ID for log tracing
    private static final Object solverIdLock = new Object();
    private static long solverInvocationCounter = 0;
    private long solverInvocationId = 0;
    
    // Each time the manager is obtained, ensure the counter is assigned a unique value
    public static synchronized void ensureUniqueInvocationId() {
        synchronized (solverIdLock) {
            solverInvocationCounter++;
            Timber.d("[SOLUTION_SOLVER][COUNTER_DEBUG] Incremented counter to: %d", solverInvocationCounter);
        }
    }
    
    public static long getCurrentSolverInvocationId() {
        return Thread.currentThread() instanceof SolverThread ? ((SolverThread)Thread.currentThread()).solverInvocationId : -1;
    }
    
    // Custom thread to hold the ID
    private static class SolverThread extends Thread {
        final long solverInvocationId;
        SolverThread(Runnable target, long solverInvocationId) {
            super(target, "solver-" + solverInvocationId);
            this.solverInvocationId = solverInvocationId;
            Timber.d("[SOLUTION_SOLVER][THREAD_DEBUG] Created SolverThread with ID: %d", solverInvocationId);
        }
    }

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
     * Gets the singleton instance of SolverManager, creating it if necessary
     * @return The singleton instance
     */
    public static synchronized SolverManager getInstance() {
        if (instance == null) {
            Timber.d("[SOLUTION_SOLVER] SolverManager.getInstance(): Creating singleton instance");
            instance = new SolverManager();
        }
        // Always increment the counter when getting the instance
        ensureUniqueInvocationId();
        Timber.d("[SOLUTION_SOLVER][INSTANCE_DEBUG] Returning singleton instance with counter: %d", solverInvocationCounter);
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private SolverManager() {
        Timber.d("[SOLUTION_SOLVER] SolverManager(): Getting solver from GameLevelSolver");
        // Use the solver instance from GameLevelSolver to avoid creating duplicate instances
        this.solver = GameLevelSolver.getSolverInstance();
    }
    
    /**
     * Sets the listener for solver events
     * @param listener The listener to receive events
     */
    public void setListener(SolverListener listener) {
        this.listener = listener;
    }
    
    /**
     * Gets the current listener
     * @return The current listener, or null if none is set
     */
    public SolverListener getListener() {
        return this.listener;
    }
    
    /**
     * Initializes the solver with the given grid elements if not already initialized with the same elements
     * @param gridElements The grid elements to initialize the solver with
     */
    public synchronized void initialize(ArrayList<GridElement> gridElements) {
        if (!isInitialized) {
            Timber.d("[SOLUTION_SOLVER] SolverManager.initialize(): Initializing solver with %d elements", gridElements.size());
            this.solver.init(gridElements);
            this.isSolved = false;
            this.isInitialized = true;
        } else {
            Timber.d("[SOLUTION_SOLVER] SolverManager.initialize(): Solver already initialized, skipping");
        }
    }
    
    /**
     * Resets the initialization state to force re-initialization on next call
     */
    public void resetInitialization() {
        Timber.d("[SOLUTION_SOLVER] SolverManager.resetInitialization(): Resetting initialization state");
        this.isInitialized = false;
        this.predefinedSolution = null;
        this.predefinedNumMoves = 0;
    }
    
    /**
     * Set a predefined solution from the level file
     * @param solution The solution string (e.g., "gE gN gE gS gW...")
     * @param numMoves The number of moves in the solution
     */
    public void setPredefinedSolution(String solution, int numMoves) {
        this.predefinedSolution = solution;
        this.predefinedNumMoves = numMoves;
        Timber.d("[SOLUTION_SOLVER] SolverManager.setPredefinedSolution(): Set predefined solution with %d moves", numMoves);
    }
    
    /**
     * Check if a predefined solution is available
     * @return true if a predefined solution exists
     */
    public boolean hasPredefinedSolution() {
        return predefinedSolution != null && !predefinedSolution.isEmpty();
    }
    
    /**
     * Starts the solver in a background thread
     */
    public void startSolver() {
        // FATAL CHECK: Abort if solver was never initialized with map data
        if (!isInitialized) {
            Timber.e("[SOLUTION_SOLVER] FATAL: startSolver() called but solver was never initialized with map data!");
            Timber.e("[SOLUTION_SOLVER] This indicates a bug: the map was not passed to the solver before trying to calculate a solution.");
            throw new IllegalStateException("[SOLUTION_SOLVER] Cannot start solver: no map data was provided. Call initialize() first.");
        }
        
        if (solverThread != null && solverThread.isAlive()) {
            Timber.d("[SOLUTION_SOLVER][ID:%d] SolverManager.startSolver() - Solver thread is already running", solverInvocationId);
            return;
        }
        // Assign a unique ID for this solver run, per-thread, always increments
        synchronized (solverIdLock) {
            solverInvocationId = solverInvocationCounter;
            Timber.d("[SOLUTION_SOLVER][START_DEBUG] Assigning invocation ID: %d from counter: %d", 
                    solverInvocationId, solverInvocationCounter);
        }
        solverThread = new SolverThread(this, solverInvocationId);
        Timber.d("[SOLUTION_SOLVER][ID:%d] SolverManager.startSolver() - Starting solver thread with name: %s", 
                solverInvocationId, solverThread.getName());
        solverThread.start();
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
        // Determine the current invocation ID based on thread type
        long idForLog;
        if (Thread.currentThread() instanceof SolverThread) {
            idForLog = ((SolverThread)Thread.currentThread()).solverInvocationId;
            Timber.d("[SOLUTION_SOLVER][RUN_DEBUG] Running in SolverThread with ID: %d", idForLog);
        } else {
            // We're running on a thread that's not one of our SolverThreads
            // This happens when GameStateManager calls run() directly on an executor
            synchronized (solverIdLock) {
                solverInvocationId = solverInvocationCounter;
                idForLog = solverInvocationId;
                Timber.d("[SOLUTION_SOLVER][RUN_DEBUG] Running in external thread (%s), using ID: %d", 
                        Thread.currentThread().getName(), idForLog);
            }
        }
        
        Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] SolverManager.run() - Starting solver (thread: %s)", 
                idForLog, Thread.currentThread().getName());
        try {
            if (!isInitialized) {
                Timber.w("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] SolverManager.run() - Solver not initialized, cannot run", idForLog);
                if (listener != null) {
                    listener.onSolverCancelled();
                }
                return;
            }
            startSolverInternal(idForLog);
        } catch (Exception e) {
            Timber.e(e, "[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Error running solver: %s", idForLog, e.getMessage());
        }
    }
    
    /**
     * Internal implementation of the solver start logic
     */
    private void startSolverInternal(long idForLog) {
        try {
            // Check if we have a predefined solution - use it instead of running the solver
            if (hasPredefinedSolution()) {
                Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Using predefined solution with %d moves", idForLog, predefinedNumMoves);
                currentSolution = parsePredefinedSolution(predefinedSolution);
                if (currentSolution != null && currentSolution.getMoves() != null) {
                    int moveCount = currentSolution.getMoves().size();
                    Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Parsed predefined solution with %d moves", idForLog, moveCount);
                    if (listener != null) {
                        listener.onSolverFinished(true, moveCount, 1);
                    }
                    return;
                } else {
                    Timber.w("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Failed to parse predefined solution, falling back to solver", idForLog);
                }
            }
            
            Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Starting solver with status: %s", idForLog, solver.getSolverStatus());
            solver.run();
            Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Solver.run() completed, checking status...", idForLog);
            // Check if the solver found a solution
            if (solver.getSolverStatus().isFinished()) {
                // Process solver results
                int numSolutions = solver.getSolutionList() != null ? solver.getSolutionList().size() : 0;
                Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Solver finished, found %d solutions", idForLog, numSolutions);
                
                boolean success = numSolutions > 0;
                if (success) {
                    // Get the first solution and process it
                    currentSolution = solver.getSolution(0);
                    int moveCount = 0;
                    if (currentSolution != null && currentSolution.getMoves() != null) {
                        moveCount = currentSolution.getMoves().size();
                        Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] First solution has %d moves", idForLog, moveCount);
                    } else {
                        Timber.w("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Solution or moves is null!", idForLog);
                    }
                    
                    // Call the listener with the results
                    if (listener != null) {
                        Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] About to notify listener of successful solution: %s", idForLog, listener.getClass().getName());
                        listener.onSolverFinished(true, moveCount, numSolutions);
                        Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Successfully notified listener of solution", idForLog);
                    } else {
                        Timber.w("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Listener is null, cannot notify of solution", idForLog);
                    }
                } else {
                    // No solution found
                    Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] No solution found", idForLog);
                    if (listener != null) {
                        Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] About to notify listener of no solution", idForLog);
                        listener.onSolverFinished(false, 0, 0);
                        Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Successfully notified listener of no solution", idForLog);
                    } else {
                        Timber.w("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Listener is null, cannot notify of failure", idForLog);
                    }
                }
            } else {
                Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Solver did not finish properly, status: %s", idForLog, solver.getSolverStatus());
                if (listener != null) {
                    Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] About to notify listener of solver cancellation", idForLog);
                    listener.onSolverCancelled();
                    Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Successfully notified listener of cancellation", idForLog);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Exception in solver processing: %s", idForLog, e.getMessage());
            if (listener != null) {
                try {
                    Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] About to notify listener of solver exception", idForLog);
                    listener.onSolverCancelled();
                    Timber.d("[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Successfully notified listener of exception", idForLog);
                } catch (Exception callbackEx) {
                    Timber.e(callbackEx, "[SOLUTION_SOLVER][ID:%d][DIAGNOSTIC] Exception in callback handling: %s", idForLog, callbackEx.getMessage());
                }
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
    
    /**
     * Parse a predefined solution string into a GameSolution
     * Format: "gE gN gE gS gW gN yN rS rE rS rW rS rW rN gW yW yN yE yN bW bN bE rN gE gN bN"
     * Each move is: [robot color letter][direction letter]
     * Robot colors: r=red(0), g=green(1), b=blue(2), y=yellow(3)
     * Directions: N=up, S=down, E=right, W=left
     * 
     * @param solutionStr The solution string to parse
     * @return A GameSolution with the parsed moves, or null if parsing fails
     */
    private GameSolution parsePredefinedSolution(String solutionStr) {
        if (solutionStr == null || solutionStr.isEmpty()) {
            return null;
        }
        
        GameSolution solution = new GameSolution();
        String[] moves = solutionStr.trim().split("\\s+");
        
        for (String move : moves) {
            if (move.length() != 2) {
                Timber.w("[SOLUTION_SOLVER] Invalid move format: %s", move);
                continue;
            }
            
            char robotChar = move.charAt(0);
            char dirChar = move.charAt(1);
            
            // Parse robot color
            int robotColor;
            switch (robotChar) {
                case 'r': robotColor = 0; break; // red
                case 'g': robotColor = 1; break; // green
                case 'b': robotColor = 2; break; // blue
                case 'y': robotColor = 3; break; // yellow
                default:
                    Timber.w("[SOLUTION_SOLVER] Unknown robot color: %c", robotChar);
                    continue;
            }
            
            // Parse direction
            roboyard.pm.ia.ricochet.ERRGameMove direction;
            switch (dirChar) {
                case 'N': direction = roboyard.pm.ia.ricochet.ERRGameMove.UP; break;
                case 'S': direction = roboyard.pm.ia.ricochet.ERRGameMove.DOWN; break;
                case 'E': direction = roboyard.pm.ia.ricochet.ERRGameMove.RIGHT; break;
                case 'W': direction = roboyard.pm.ia.ricochet.ERRGameMove.LEFT; break;
                default:
                    Timber.w("[SOLUTION_SOLVER] Unknown direction: %c", dirChar);
                    continue;
            }
            
            // Create a RRPiece for this robot color (position 0,0 is placeholder, color and id are what matter)
            roboyard.pm.ia.ricochet.RRPiece piece = new roboyard.pm.ia.ricochet.RRPiece(0, 0, robotColor, robotColor);
            
            // Add the move to the solution
            solution.addMove(new roboyard.pm.ia.ricochet.RRGameMove(piece, direction));
        }
        
        Timber.d("[SOLUTION_SOLVER] Parsed predefined solution: %d moves", solution.getMoves().size());
        return solution;
    }
}
