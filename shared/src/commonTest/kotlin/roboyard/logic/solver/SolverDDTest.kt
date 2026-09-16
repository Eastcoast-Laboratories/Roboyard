package roboyard.logic.solver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import roboyard.logic.core.GridElement

/**
 * Tests for the shared solver bridge (RRGetMap + SolverDD).
 * Board is 8x8 game cells; outer walls use the level-file encoding:
 * mh at y=0 / y=8 (top/bottom), mv at x=0 / x=8 (left/right).
 */
class SolverDDTest {

    private fun outerWalls(): List<GridElement> {
        val walls = ArrayList<GridElement>()
        for (x in 0 until 8) {
            walls.add(GridElement(x, 0, "mh"))
            walls.add(GridElement(x, 8, "mh"))
        }
        for (y in 0 until 8) {
            walls.add(GridElement(0, y, "mv"))
            walls.add(GridElement(8, y, "mv"))
        }
        return walls
    }

    /**
     * Map where the red robot reaches its target in exactly one move:
     * red at (0,1) slides east, green blocks at (4,1), red stops on
     * the target at (3,1).
     */
    private fun oneMoveElements(): ArrayList<GridElement> {
        val elements = ArrayList(outerWalls())
        elements.add(GridElement(0, 1, "robot_red"))
        elements.add(GridElement(4, 1, "robot_green"))
        elements.add(GridElement(6, 6, "robot_blue"))
        elements.add(GridElement(6, 0, "robot_yellow"))
        elements.add(GridElement(3, 1, "target_red"))
        return elements
    }

    @Test
    fun solverSolvesOneMoveMap() {
        val solver = SolverDD()
        solver.init(oneMoveElements())
        solver.run()

        assertEquals(SolverStatus.solved, solver.getSolverStatus())
        val solutions = solver.getSolutionList()
        assertNotNull(solutions)
        assertTrue(solutions.isNotEmpty())

        val gameSolution = solver.getSolution(0)
        assertNotNull(gameSolution)
        assertTrue(gameSolution.moves.size >= 1)
        assertTrue(solver.isSolution01())
    }

    @Test
    fun cancelBeforeRunYieldsNoSolution() {
        val solver = SolverDD()
        solver.init(oneMoveElements())
        solver.cancel()
        solver.run()

        assertEquals(SolverStatus.noSolution, solver.getSolverStatus())
        assertNull(solver.getSolutionList())
    }

    @Test
    fun missingTargetThrows() {
        val elements = ArrayList(outerWalls())
        elements.add(GridElement(0, 1, "robot_red"))
        elements.add(GridElement(4, 1, "robot_green"))
        elements.add(GridElement(6, 6, "robot_blue"))
        elements.add(GridElement(6, 0, "robot_yellow"))

        val pieces = kotlin.arrayOfNulls<RRPiece>(4)
        assertFailsWith<RuntimeException> {
            RRGetMap.createDDWorld(elements, pieces)
        }
    }

    @Test
    fun asciiMapRoundTrip() {
        val elements = oneMoveElements()
        val ascii = RRGetMap.generateAsciiMap(elements)
        val parsed = RRGetMap.parseAsciiMap(ascii)

        assertNotNull(parsed)

        fun positions(list: List<GridElement>, type: String) =
            list.filter { it.type == type }.map { it.x to it.y }.toSet()

        for (type in listOf("robot_red", "robot_green", "robot_blue", "robot_yellow", "target_red")) {
            assertEquals(positions(elements, type), positions(parsed, type), "positions for $type")
        }
    }
}
