package roboyard.ui.compose

import driftingdroids.model.Board
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for robot movement logic in GameScreen
 */
class RobotMovementTest {

    @Test
    fun testMoveRobotNorth() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2) = 2 + 2*5 = 12
        board.setWall(2, 1, Board.NORTH, true) // Wall north of robot at (2, 1)

        // Move robot north (should stop at wall)
        val newBoard = moveRobot(board, 0, Board.NORTH)

        assertNotNull(newBoard, "Move should succeed")
        assertEquals(7, newBoard.robotPositions[0], "Robot should move to (2, 1) = 2 + 1*5 = 7")
    }

    @Test
    fun testMoveRobotSouth() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(7)) // Robot at (2, 1) = 2 + 1*5 = 7
        board.setWall(2, 2, Board.SOUTH, true) // Wall south of robot at (2, 2)

        // Move robot south (should stop at wall)
        val newBoard = moveRobot(board, 0, Board.SOUTH)

        assertNotNull(newBoard, "Move should succeed")
        assertEquals(12, newBoard.robotPositions[0], "Robot should move to (2, 2) = 2 + 2*5 = 12")
    }

    @Test
    fun testMoveRobotEast() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(11)) // Robot at (1, 2) = 1 + 2*5 = 11
        board.setWall(2, 2, Board.EAST, true) // Wall east of robot at (2, 2)

        // Move robot east (should stop at wall)
        val newBoard = moveRobot(board, 0, Board.EAST)

        assertNotNull(newBoard, "Move should succeed")
        assertEquals(12, newBoard.robotPositions[0], "Robot should move to (2, 2) = 2 + 2*5 = 12")
    }

    @Test
    fun testMoveRobotWest() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(13)) // Robot at (3, 2) = 3 + 2*5 = 13
        board.setWall(2, 2, Board.WEST, true) // Wall west of robot at (2, 2)

        // Move robot west (should stop at wall)
        val newBoard = moveRobot(board, 0, Board.WEST)

        assertNotNull(newBoard, "Move should succeed")
        assertEquals(12, newBoard.robotPositions[0], "Robot should move to (2, 2) = 2 + 2*5 = 12")
    }

    @Test
    fun testMoveRobotBlockedByWall() {
        // Create a 5x5 board with a wall
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.setWall(2, 2, Board.NORTH, true) // Wall north of robot

        // Try to move robot north (should be blocked)
        val newBoard = moveRobot(board, 0, Board.NORTH)

        assertNull(newBoard, "Move should fail due to wall")
    }

    @Test
    fun testMoveRobotBlockedByAnotherRobot() {
        // Create a 5x5 board with two robots
        val board = Board.createBoardFreestyle(null, 5, 5, 2) ?: return
        board.setRobots(intArrayOf(12, 7)) // Robot 0 at (2, 2), Robot 1 at (2, 1)

        // Try to move robot 0 north (should be blocked by robot 1)
        val newBoard = moveRobot(board, 0, Board.NORTH)

        assertNull(newBoard, "Move should fail due to another robot")
    }

    @Test
    fun testMoveRobotMultipleTimes() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.setWall(2, 0, Board.NORTH, true) // Wall north of robot at (2, 0)

        // Move robot north twice (robot will slide to wall at (2, 0))
        var newBoard = moveRobot(board, 0, Board.NORTH)
        assertNotNull(newBoard, "First move should succeed")
        assertEquals(2, newBoard.robotPositions[0], "Robot should slide to (2, 0)")

        // Second move should fail (robot already at wall)
        newBoard = moveRobot(newBoard, 0, Board.NORTH)
        assertNull(newBoard, "Second move should fail (robot at wall)")
    }

    @Test
    fun testIsSolvedWithGoal() {
        // Create a board with a goal and robot
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.setWall(2, 1, Board.NORTH, true) // Wall north of robot at (2, 1)
        board.addGoal(7, 0, 0) // Goal at (2, 1) for robot 0
        board.setGoalRandom()

        // Robot not at goal
        assertEquals(false, isSolved(board), "Should not be solved initially")

        // Move robot to goal
        val newBoard = moveRobot(board, 0, Board.NORTH)
        assertNotNull(newBoard, "Move should succeed")

        // Robot at goal
        assertEquals(true, isSolved(newBoard), "Should be solved after moving to goal")
    }

    @Test
    fun testIsSolvedWithNoGoal() {
        // Create a board without a goal
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)

        // No goal set
        assertEquals(false, isSolved(board), "Should not be solved without goal")
    }
}
