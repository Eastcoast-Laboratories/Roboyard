package roboyard.ui.compose

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import roboyard.logic.core.GridElement
import roboyard.logic.core.Preferences

class BoardConversionValidationTest {

    private var savedWidth = 0
    private var savedHeight = 0
    private var savedX = 0
    private var savedY = 0

    @BeforeTest
    fun setup() {
        savedWidth = Preferences.boardSizeWidth
        savedHeight = Preferences.boardSizeHeight
        savedX = Preferences.boardSizeX
        savedY = Preferences.boardSizeY
        Preferences.boardSizeWidth = 12
        Preferences.boardSizeHeight = 14
        Preferences.boardSizeX = 12
        Preferences.boardSizeY = 14
    }

    @AfterTest
    fun teardown() {
        Preferences.boardSizeWidth = savedWidth
        Preferences.boardSizeHeight = savedHeight
        Preferences.boardSizeX = savedX
        Preferences.boardSizeY = savedY
    }

    @Test
    fun test_incompleteRobotSet_returnsNull() {
        val grid = arrayListOf(
            GridElement(5, 5, "target_red"),
            GridElement(1, 1, "robot_red"),
            GridElement(2, 2, "robot_green"),
            GridElement(3, 3, "robot_blue")
        )
        assertNull(gridElementsToBoard(grid, 12, 14))
    }

    @Test
    fun test_duplicateRobotPositions_returnsNull() {
        val grid = arrayListOf(
            GridElement(5, 5, "target_red"),
            GridElement(1, 1, "robot_red"),
            GridElement(2, 2, "robot_green"),
            GridElement(1, 1, "robot_blue"),
            GridElement(3, 3, "robot_yellow")
        )
        assertNull(gridElementsToBoard(grid, 12, 14))
    }

    @Test
    fun test_noGoal_returnsNull() {
        val grid = arrayListOf(
            GridElement(1, 1, "robot_red"),
            GridElement(2, 2, "robot_green"),
            GridElement(3, 3, "robot_blue"),
            GridElement(4, 4, "robot_yellow")
        )
        assertNull(gridElementsToBoard(grid, 12, 14))
    }

    @Test
    fun test_silverRobotOnFourRobotBoard_returnsNull() {
        val grid = arrayListOf(
            GridElement(5, 5, "target_red"),
            GridElement(1, 1, "robot_red"),
            GridElement(2, 2, "robot_green"),
            GridElement(3, 3, "robot_blue"),
            GridElement(4, 4, "robot_yellow"),
            GridElement(6, 6, "robot_silver")
        )
        assertNull(gridElementsToBoard(grid, 12, 14))
    }

    @Test
    fun test_validGrid_returnsBoard() {
        val grid = arrayListOf(
            GridElement(5, 5, "target_red"),
            GridElement(1, 1, "robot_red"),
            GridElement(2, 2, "robot_green"),
            GridElement(3, 3, "robot_blue"),
            GridElement(4, 4, "robot_yellow")
        )
        val board = gridElementsToBoard(grid, 12, 14)
        assertNotNull(board)
        assertEquals(4, board.robotPositions.size)
        for (pos in board.robotPositions) {
            assertTrue(pos in 0 until board.size)
        }
        assertEquals(4, board.robotPositions.distinct().size)
        val goal = board.getActiveGoals().firstOrNull()
        assertNotNull(goal)
        assertTrue(goal!!.position in 0 until board.size)
    }
}
