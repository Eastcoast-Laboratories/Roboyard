package roboyard.logic.solver

import roboyard.logic.core.GridElement
import timber.log.Timber

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
object GameLevelSolver {
    // Single static solver instance to be used across the entire app
    @JvmStatic
    @get:Synchronized
    var solverInstance: SolverDD? = null
        /**
         * Get the singleton SolverDD instance, creating it if necessary
         */
        get() {
            if (field == null) {
                Timber.d("[SOLUTION_SOLVER] GameLevelSolver.getSolverInstance(): Creating SolverDD instance")
                field = SolverDD()
            }
            return field
        }
        private set

    fun solveLevelFromString(mapContent: String): Int {
        // Parse map content into GridElements
        val elements = ArrayList<GridElement>()
        val lines = mapContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (line in lines) {
            if (line.startsWith("board:")) {
                continue  // Skip board size line
            }
            // Format: type x,y;
            val parts = line.split("[,;]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size >= 2) {
                val type = parts[0].replace("\\d+$".toRegex(), "")
                val x = parts[0].replace("[^0-9]".toRegex(), "").toInt()
                val y = parts[1].toInt()
                elements.add(GridElement(x, y, type))
            }
        }


        // Get the solver instance and initialize it
        Timber.d("[SOLUTION_SOLVER] GameLevelSolver.solveLevelFromString(): Using singleton solver instance")
        val solver = solverInstance!!
        solver.init(elements)


        // Run solver
        solver.run()


        // Check result
        if (solver.getSolverStatus() == SolverStatus.solved) {
            val solutions = solver.getSolutionList()
            if (!solutions!!.isEmpty()) {
                val solution = solver.getSolution(0)
                return solution!!.moves.size
            }
        }

        return -1 // No solution found
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size > 0) {
            val mapContent = StringBuilder()
            for (arg in args) {
                mapContent.append(arg).append("\n")
            }
            val moves = solveLevelFromString(mapContent.toString())
            Timber.d(moves.toString()) // Print number of moves, -1 if no solution
        }
    }
}
