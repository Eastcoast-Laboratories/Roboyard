package roboyard.logic.solver

import driftingdroids.model.Board
import driftingdroids.model.Solution
import driftingdroids.model.Solver
import roboyard.logic.core.Constants
import roboyard.logic.core.GameLogic.Companion.getColor
import roboyard.logic.core.GameSolution
import roboyard.logic.core.GridElement
import roboyard.pm.ia.ricochet.ERRGameMove
import roboyard.pm.ia.ricochet.RRGameMove
import roboyard.pm.ia.ricochet.RRGetMap
import roboyard.pm.ia.ricochet.RRPiece
import timber.log.Timber
import kotlin.math.min

/**
 * Bridge implementation between Roboyard's solver interface and the DriftingDroids solver.
 * This class is responsible for:
 * 1. Converting Roboyard's game state to DriftingDroids board format
 * 2. Running the DriftingDroids solver to find solutions
 * 3. Converting DriftingDroids solutions back to Roboyard's format
 * 
 * @author Pierre Michel
 * @since 08/03/2015
 */
class SolverDD : ISolver {
    private var solverStatus: SolverStatus?
    private var solver: Solver? = null
    private var solutions: MutableList<Solution>? = null
    private val pieces: Array<RRPiece?>
    private var board: Board? = null
    private var solverThread: Thread? = null // Track the solver thread for cancellation

    init {
        solverStatus = SolverStatus.idle
        pieces = kotlin.arrayOfNulls<RRPiece>(Constants.NUM_ROBOTS)
    }

    override fun init(elements: ArrayList<GridElement>) {
        // Reset solver state
        solver = null
        solutions = null
        solverStatus = SolverStatus.idle

        Timber.d(
            "[SOLUTION_SOLVER] SolverDD.init(): Initializing solver with %d grid elements",
            elements.size
        )


        // Log some sample elements to verify data
        if (elements.size > 0) {
            Timber.d("[SOLUTION_SOLVER] SolverDD.init(): First few elements:")
            for (i in 0..<min(5, elements.size)) {
                val element = elements.get(i)
                Timber.d(
                    "[SOLUTION_SOLVER] Element %d: type=%s, position=(%d,%d)",
                    i,
                    element.type,
                    element.x,
                    element.y
                )
            }
        }


        // Search for and log any multi-color targets
        for (element in elements) {
            if (element.type != null && element.type == "target_multi") {
                Timber.d(
                    "[SOLUTION_SOLVER_TARGET] SolverDD.init(): Found multi-color target at position (%d,%d)",
                    element.x, element.y
                )
            }
        }


        // Initialize new board and solver
        Timber.d("[SOLUTION_SOLVER] SolverDD.init(): Creating DD World from elements")
        board = RRGetMap.createDDWorld(elements, pieces)


        // Log robot pieces information
        Timber.d("[SOLUTION_SOLVER] SolverDD.init(): Robot pieces after initialization:")
        for (i in pieces.indices) {
            if (pieces[i] != null) {
                Timber.d(
                    "[SOLUTION_SOLVER] Robot %d: color=%d, position=(%d,%d)",
                    i, pieces[i]!!.getColor(), pieces[i]!!.getX(), pieces[i]!!.getY()
                )
            } else {
                Timber.d("[SOLUTION_SOLVER] Robot %d: null", i)
            }
        }

        solver = Solver.createInstance(board)
        Timber.d("[SOLUTION_SOLVER] SolverDD.init(): Solver created successfully")
    }

    override fun run() {
        // Store reference to current thread for cancellation
        solverThread = Thread.currentThread()
        Timber.d(
            "[SOLUTION_SOLVER] SolverDD.run(): Solver thread started: %s",
            solverThread!!.getName()
        )

        if (solver == null) {
            Timber.d("[SOLUTION_SOLVER] SolverDD.run(): solver is null, aborting")
            return
        }

        // Check if outer walls are complete before running the solver
        if (!outerWallsAreComplete()) {
            Timber.e("[SOLUTION_SOLVER] Incomplete outer walls detected! Aborting solver to prevent crash.")
            solverStatus = SolverStatus.missingData
            return
        }

        solverStatus = SolverStatus.solving
        Timber.d("[SOLUTION_SOLVER] SolverDD.run(): Starting solver with status %s", solverStatus)

        try {
            Timber.d("[SOLUTION_SOLVER] SolverDD.run(): Executing solver")
            solutions = solver!!.execute()
            Timber.d("[SOLUTION_SOLVER] SolverDD.run(): Solver execution complete")

            if (solutions!!.size != 0) {
                val solution = solutions!!.get(0)
                Timber.d(
                    "[SOLUTION_SOLVER] %d solution(s) found; first solution:",
                    solutions!!.size
                )
                Timber.d("[SOLUTION_SOLVER] %s", solution.toString())
                solverStatus = SolverStatus.solved
                Timber.d("[SOLUTION_SOLVER] SolverDD.run(): Status set to %s", solverStatus)
            } else {
                Timber.d("[SOLUTION_SOLVER] SolverDD.run(): No solutions found")
                solverStatus = SolverStatus.noSolution
            }
        } catch (e: InterruptedException) {
            Timber.e(e, "[SOLUTION_SOLVER] SolverDD.run(): Solver interrupted")
            solverStatus = SolverStatus.noSolution
        } finally {
            // Release solver reference to allow GC to reclaim SolverIDDFS instance
            // (includes states[][], obstacles[][], directions[][] and knownStates)
            solver = null
            // Explicit GC: Android ART doesn't shrink the heap automatically.
            // Without this, consecutive solver runs accumulate heap pressure until OOM.
            System.gc()
        }
    }

    override fun getSolverStatus(): SolverStatus? {
        return this.solverStatus
    }

    override fun getSolutionList(): MutableList<Solution>? {
        return this.solutions
    }

    /**
     * Set solutions from a loaded save file
     * This allows re-saving games without re-running the solver
     * @param solutions List of Solution objects to store
     */
    fun setSolutions(solutions: MutableList<Solution>?) {
        this.solutions = solutions
        Timber.d(
            "[SOLUTIONS_SAVE_LOAD] SolverDD.setSolutions(): Set %d solutions",
            if (solutions != null) solutions.size else 0
        )
    }

    /**
     * get the solution number num from the list of found different solutions and add all moves to the result
     * TODO: @param num number of the solution in the solutions list
     * @return GameSolution with all moves in that solution
     */
    override fun getSolution(num: Int): GameSolution? {
        if (solutions == null || num >= solutions!!.size) {
            Timber.d("[SOLUTION_SOLVER] getSolution(%d): Solutions null or index out of range", num)
            return null
        }

        Timber.d(
            "[SOLUTION_SOLVER] getSolution(%d): Creating GameSolution from DriftingDroids solution",
            num
        )
        val s = GameSolution()
        val solution = solutions!!.get(num)
        solution.resetMoves()
        var m = solution.getNextMove()
        var moveCount = 0

        while (m != null) {
            moveCount++
            val mv: ERRGameMove?
            when (m.direction) {
                0 -> mv = ERRGameMove.UP
                1 -> mv = ERRGameMove.RIGHT
                2 -> mv = ERRGameMove.DOWN
                3 -> mv = ERRGameMove.LEFT
                else -> mv = ERRGameMove.NOMOVE
            }
            print(m.direction.toString() + "," + pieces[m.robotNumber]!!.getColor() + ";")
            s.addMove(RRGameMove(pieces[m.robotNumber]!!, mv))
            m = solution.getNextMove()
        }

        Timber.d(
            "[SOLUTION_SOLVER] getSolution(%d): Created GameSolution with %d moves",
            num,
            moveCount
        )
        return s
    }

    /**
     * Cancel the solver execution and interrupt the solver thread
     */
    fun cancel() {
        Timber.d("[SOLUTION_SOLVER] SolverDD.cancel(): Cancelling solver")
        this.solverStatus = SolverStatus.noSolution


        // Interrupt the solver thread to allow graceful termination
        if (solverThread != null && solverThread!!.isAlive()) {
            Timber.d(
                "[SOLUTION_SOLVER] SolverDD.cancel(): Interrupting solver thread: %s",
                solverThread!!.getName()
            )
            solverThread!!.interrupt()
        } else {
            Timber.d("[SOLUTION_SOLVER] SolverDD.cancel(): Solver thread is not running")
        }
    }

    /**
     * Check if the solution can be reached in one move
     * @return true if the target can be reached in one move
     */
    override fun isSolution01(): Boolean {
        return board != null && board!!.isSolution01()
    }

    private fun outerWallsAreComplete(): Boolean {
        if (board == null) {
            Timber.e("[SOLUTION_SOLVER][OUTER WALLS] Cannot check outer walls: board is null")
            return false
        }

        val width = board!!.width
        val height = board!!.height

        Timber.d(
            "[SOLUTION_SOLVER][OUTER WALLS] Checking outer walls for board dimensions %d x %d",
            width,
            height
        )
        // Check top border (horizontal walls)
        for (x in 0..<width) {
            val position = 0 * width + x // y=0, first row
            if (!board!!.isWall(position, Constants.NORTH)) {
                Timber.e("[SOLUTION_SOLVER][OUTER WALLS] Missing top wall at x=%d", x)
                return false
            }
        }


        // Check bottom border (horizontal walls)
        for (x in 0..<width) {
            val position = (height - 1) * width + x // y=height-1, last row
            if (!board!!.isWall(position, Constants.SOUTH)) {
                Timber.e("[SOLUTION_SOLVER][OUTER WALLS] Missing bottom wall at x=%d", x)
                return false
            }
        }


        // Check left border (vertical walls)
        for (y in 0..<height) {
            val position = y * width + 0 // x=0, first column
            if (!board!!.isWall(position, Constants.WEST)) {
                Timber.e("[SOLUTION_SOLVER][OUTER WALLS] Missing left wall at y=%d", y)
                return false
            }
        }


        // Check right border (vertical walls)
        for (y in 0..<height) {
            val position = y * width + (width - 1) // x=width-1, last column
            if (!board!!.isWall(position, Constants.EAST)) {
                Timber.e("[SOLUTION_SOLVER][OUTER WALLS] Missing right wall at y=%d", y)
                return false
            }
        }

        Timber.d("[SOLUTION_SOLVER][OUTER WALLS] All outer walls are present")
        return true
    }
}
