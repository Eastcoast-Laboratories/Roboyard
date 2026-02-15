package roboyard.ui.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import roboyard.logic.solver.SolverDD;
import roboyard.logic.core.GridElement;
import roboyard.logic.core.GameSolution;
import timber.log.Timber;

/**
 * A non-singleton solver manager dedicated to the live move counter feature.
 * Runs a separate SolverDD instance so it does not interfere with the main solver.
 */
public class LiveSolverManager {

    public interface LiveSolverListener {
        void onLiveSolverFinished(int optimalMoves, GameSolution solution);
        void onLiveSolverFailed();
    }

    private final SolverDD solver;
    private final ExecutorService executor;
    private Future<?> currentTask;
    private volatile boolean cancelled = false;

    public LiveSolverManager() {
        this.solver = new SolverDD();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "live-solver");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Solve the current board state asynchronously.
     * Cancels any previously running live solve.
     */
    public void solveAsync(ArrayList<GridElement> gridElements, LiveSolverListener listener) {
        cancel();
        cancelled = false;

        Timber.d("[LIVE_SOLVER] Starting live solve with %d elements", gridElements.size());

        currentTask = executor.submit(() -> {
            try {
                solver.init(gridElements);
                solver.run();

                if (cancelled) {
                    Timber.d("[LIVE_SOLVER] Cancelled before result delivery");
                    return;
                }

                if (solver.getSolverStatus().isFinished()) {
                    int numSolutions = solver.getSolutionList() != null ? solver.getSolutionList().size() : 0;
                    if (numSolutions > 0) {
                        GameSolution solution = solver.getSolution(0);
                        int moves = (solution != null && solution.getMoves() != null) ? solution.getMoves().size() : 0;
                        if (solver.isSolution01()) {
                            moves = 1;
                        }
                        Timber.d("[LIVE_SOLVER] Found solution with %d moves", moves);
                        if (!cancelled && listener != null) {
                            listener.onLiveSolverFinished(moves, solution);
                        }
                    } else {
                        Timber.d("[LIVE_SOLVER] No solution found");
                        if (!cancelled && listener != null) {
                            listener.onLiveSolverFailed();
                        }
                    }
                } else {
                    Timber.d("[LIVE_SOLVER] Solver did not finish");
                    if (!cancelled && listener != null) {
                        listener.onLiveSolverFailed();
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "[LIVE_SOLVER] Error during live solve: %s", e.getMessage());
                if (!cancelled && listener != null) {
                    listener.onLiveSolverFailed();
                }
            }
        });
    }

    /**
     * Cancel any running live solve.
     */
    public void cancel() {
        cancelled = true;
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            Timber.d("[LIVE_SOLVER] Cancelled running live solve");
        }
        if (solver != null) {
            solver.cancel();
        }
    }

    /**
     * Shut down the executor. Call when the feature is no longer needed.
     */
    public void shutdown() {
        cancel();
        executor.shutdownNow();
        Timber.d("[LIVE_SOLVER] Shut down");
    }
}
