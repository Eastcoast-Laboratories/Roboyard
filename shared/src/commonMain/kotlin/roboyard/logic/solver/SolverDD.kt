package roboyard.logic.solver

import driftingdroids.model.Board
import driftingdroids.model.Solution
import driftingdroids.model.Solver
import kotlin.concurrent.Volatile
import kotlin.math.min
import roboyard.logic.core.Constants
import roboyard.logic.core.GameSolution
import roboyard.logic.core.GridElement
import roboyard.logic.platform.requestGc
import roboyard.logic.util.RLog

/**
 * Bridge implementation between Roboyard's solver interface and the DriftingDroids solver.
 * This class is responsible for:
 * 1. Converting Roboyard's game state to DriftingDroids board format
 * 2. Running the DriftingDroids solver to find solutions
 * 3. Converting DriftingDroids solutions back to Roboyard's format
 *
 * Shared KMP version: logging goes through RLog and cancellation is
 * cooperative (Solver.requestCancel()) because commonMain has no threads.
 *
 * @author Pierre Michel
 * @since 08/03/2015
 */
open class SolverDD : ISolver {
    private val log = RLog.tag("SolverDD")
    private var solverStatus: SolverStatus?
    private var solver: Solver? = null
    private var solutions: MutableList<Solution>? = null
    private val pieces: Array<RRPiece?>
    private var board: Board? = null

    @Volatile
    private var cancelRequested = false

    init {
        solverStatus = SolverStatus.idle
        pieces = kotlin.arrayOfNulls<RRPiece>(Constants.NUM_ROBOTS)
    }

    override fun init(elements: ArrayList<GridElement>?) {
        // Reset solver state
        solver = null
        solutions = null
        solverStatus = SolverStatus.idle
        cancelRequested = false

        log.d(
            "[SOLUTION_SOLVER] SolverDD.init(): Initializing solver with %d grid elements",
            elements?.size ?: 0
        )


        // Log some sample elements to verify data
        if (elements != null && elements.size > 0) {
            log.d("[SOLUTION_SOLVER] SolverDD.init(): First few elements:")
            for (i in 0 until min(5, elements.size)) {
                val element = elements[i]
                log.d(
                    "[SOLUTION_SOLVER] Element %d: type=%s, position=(%d,%d)",
                    i,
                    element.type,
                    element.x,
                    element.y
                )
            }
        }


        // Search for and log any multi-color targets
        if (elements != null) {
            for (element in elements) {
                if (element.type != null && element.type == "target_multi") {
                    log.d(
                        "[SOLUTION_SOLVER_TARGET] SolverDD.init(): Found multi-color target at position (%d,%d)",
                        element.x, element.y
                    )
                }
            }
        }


        // Initialize new board and solver
        log.d("[SOLUTION_SOLVER] SolverDD.init(): Creating DD World from elements")
        if (elements != null) {
            board = RRGetMap.createDDWorld(elements, pieces)
        }


        // Log robot pieces information
        log.d("[SOLUTION_SOLVER] SolverDD.init(): Robot pieces after initialization:")
        for (i in pieces.indices) {
            if (pieces[i] != null) {
                log.d(
                    "[SOLUTION_SOLVER] Robot %d: color=%d, position=(%d,%d)",
                    i, pieces[i]!!.color, pieces[i]!!.x, pieces[i]!!.y
                )
            } else {
                log.d("[SOLUTION_SOLVER] Robot %d: null", i)
            }
        }

        solver = board?.let { Solver.createInstance(it) }
        log.d("[SOLUTION_SOLVER] SolverDD.init(): Solver created successfully")
    }

    override fun run() {
        log.d("[SOLUTION_SOLVER] SolverDD.run(): Solver run started")

        if (solver == null) {
            log.d("[SOLUTION_SOLVER] SolverDD.run(): solver is null, aborting")
            return
        }

        if (cancelRequested) {
            log.d("[SOLUTION_SOLVER] SolverDD.run(): cancel was requested before start, aborting")
            solverStatus = SolverStatus.noSolution
            return
        }

        // Check if outer walls are complete before running the solver
        if (!outerWallsAreComplete()) {
            log.e("[SOLUTION_SOLVER] Incomplete outer walls detected! Aborting solver to prevent crash.")
            solverStatus = SolverStatus.missingData
            return
        }

        solverStatus = SolverStatus.solving
        log.d("[SOLUTION_SOLVER] SolverDD.run(): Starting solver with status %s", solverStatus)

        try {
            log.d("[SOLUTION_SOLVER] SolverDD.run(): Executing solver")
            if (cancelRequested) solver?.requestCancel()
            solutions = solver!!.execute().toMutableList()
            log.d("[SOLUTION_SOLVER] SolverDD.run(): Solver execution complete")

            if (solutions!!.size != 0) {
                val solution = solutions!!.get(0)
                log.d(
                    "[SOLUTION_SOLVER] %d solution(s) found; first solution:",
                    solutions!!.size
                )
                log.d("[SOLUTION_SOLVER] %s", solution.toString())
                solverStatus = SolverStatus.solved
                log.d("[SOLUTION_SOLVER] SolverDD.run(): Status set to %s", solverStatus)
            } else {
                log.d("[SOLUTION_SOLVER] SolverDD.run(): No solutions found")
                solverStatus = SolverStatus.noSolution
            }
        } catch (e: Exception) {
            log.e(e, "[SOLUTION_SOLVER] SolverDD.run(): Solver aborted")
            solverStatus = SolverStatus.noSolution
        } finally {
            // Release solver reference to allow GC to reclaim SolverIDDFS instance
            // (includes states[][], obstacles[][], directions[][] and knownStates)
            solver = null
            // Explicit GC: Android ART doesn't shrink the heap automatically.
            // Without this, consecutive solver runs accumulate heap pressure until OOM.
            requestGc()
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
        log.d(
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
            log.d("[SOLUTION_SOLVER] getSolution(%d): Solutions null or index out of range", num)
            return null
        }

        log.d(
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
            when (m!!.direction) {
                0 -> mv = ERRGameMove.UP
                1 -> mv = ERRGameMove.RIGHT
                2 -> mv = ERRGameMove.DOWN
                3 -> mv = ERRGameMove.LEFT
                else -> mv = ERRGameMove.NOMOVE
            }
            print(m!!.direction.toString() + "," + pieces[m!!.robotNumber]!!.color + ";")
            s.addMove(RRGameMove(pieces[m!!.robotNumber]!!, mv))
            m = solution.getNextMove()
        }

        log.d(
            "[SOLUTION_SOLVER] getSolution(%d): Created GameSolution with %d moves",
            num,
            moveCount
        )
        return s
    }

    /**
     * Cancel the solver execution.
     * Requests cooperative cancellation on the running solver and marks
     * the status so callers observe a non-finished result.
     */
    fun cancel() {
        log.d("[SOLUTION_SOLVER] SolverDD.cancel(): Cancelling solver")
        cancelRequested = true
        this.solverStatus = SolverStatus.noSolution
        solver?.requestCancel()
    }

    /**
     * Check if the solution can be reached in one move
     * @return true if the target can be reached in one move
     */
    override fun isSolution01(): Boolean {
        return board != null && board!!.isSolution01
    }

    private fun outerWallsAreComplete(): Boolean {
        if (board == null) {
            log.e("[SOLUTION_SOLVER][OUTER WALLS] Cannot check outer walls: board is null")
            return false
        }

        val width = board!!.width
        val height = board!!.height

        log.d(
            "[SOLUTION_SOLVER][OUTER WALLS] Checking outer walls for board dimensions %d x %d",
            width,
            height
        )
        // Check top border (horizontal walls)
        for (x in 0..<width) {
            val position = 0 * width + x // y=0, first row
            if (!board!!.isWall(position, Constants.NORTH)) {
                log.e("[SOLUTION_SOLVER][OUTER WALLS] Missing top wall at x=%d", x)
                return false
            }
        }


        // Check bottom border (horizontal walls)
        for (x in 0..<width) {
            val position = (height - 1) * width + x // y=height-1, last row
            if (!board!!.isWall(position, Constants.SOUTH)) {
                log.e("[SOLUTION_SOLVER][OUTER WALLS] Missing bottom wall at x=%d", x)
                return false
            }
        }


        // Check left border (vertical walls)
        for (y in 0..<height) {
            val position = y * width + 0 // x=0, first column
            if (!board!!.isWall(position, Constants.WEST)) {
                log.e("[SOLUTION_SOLVER][OUTER WALLS] Missing left wall at y=%d", y)
                return false
            }
        }


        // Check right border (vertical walls)
        for (y in 0..<height) {
            val position = y * width + (width - 1) // x=width-1, last column
            if (!board!!.isWall(position, Constants.EAST)) {
                log.e("[SOLUTION_SOLVER][OUTER WALLS] Missing right wall at y=%d", y)
                return false
            }
        }

        log.d("[SOLUTION_SOLVER][OUTER WALLS] All outer walls are present")
        return true
    }
}
