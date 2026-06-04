package roboyard.logic.solver

import driftingdroids.model.Solution
import roboyard.logic.core.GameSolution
import roboyard.logic.core.GridElement

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
 * @see SolverDD
 * 
 * @see SolverStatus
 */
interface ISolver : Runnable {
    fun init(elements: ArrayList<GridElement>?)
    override fun run()
    fun getSolverStatus(): SolverStatus?
    fun getSolution(num: Int): GameSolution?
    fun getSolutionList(): MutableList<Solution>?

    /**
     * Check if the solution can be reached in one move
     * @return true if the target can be reached in one move
     */
    fun isSolution01(): Boolean
}
