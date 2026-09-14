package roboyard.ui.compose

import driftingdroids.model.Board
import roboyard.logic.core.moveRobotOnBoard
import roboyard.logic.core.isBoardSolved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for robot movement logic in GameScreen
 * Tests the fragment-app gesture logic 1:1
 */
class RobotMovementTest {

    @Test
    fun testMoveRobotNorth() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2) = 2 + 2*5 = 12
        board.setWall(2, 1, Board.NORTH, true) // Wall north of robot at (2, 1)

        // Move robot north (should stop at wall)
        val newBoard = moveRobotOnBoard(board, 0, Board.NORTH)

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
        val newBoard = moveRobotOnBoard(board, 0, Board.SOUTH)

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
        val newBoard = moveRobotOnBoard(board, 0, Board.EAST)

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
        val newBoard = moveRobotOnBoard(board, 0, Board.WEST)

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
        val newBoard = moveRobotOnBoard(board, 0, Board.NORTH)

        assertNull(newBoard, "Move should fail due to wall")
    }

    @Test
    fun testMoveRobotBlockedByAnotherRobot() {
        // Create a 5x5 board with two robots
        val board = Board.createBoardFreestyle(null, 5, 5, 2) ?: return
        board.setRobots(intArrayOf(12, 7)) // Robot 0 at (2, 2), Robot 1 at (2, 1)

        // Try to move robot 0 north (should be blocked by robot 1)
        val newBoard = moveRobotOnBoard(board, 0, Board.NORTH)

        assertNull(newBoard, "Move should fail due to another robot")
    }

    @Test
    fun testMoveRobotMultipleTimes() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.setWall(2, 0, Board.NORTH, true) // Wall north of robot at (2, 0)

        // Move robot north twice (robot will slide to wall at (2, 0))
        var newBoard = moveRobotOnBoard(board, 0, Board.NORTH)
        assertNotNull(newBoard, "First move should succeed")
        assertEquals(2, newBoard.robotPositions[0], "Robot should slide to (2, 0)")

        // Second move should fail (robot already at wall)
        newBoard = moveRobotOnBoard(newBoard, 0, Board.NORTH)
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
        assertEquals(false, isBoardSolved(board), "Should not be solved initially")

        // Move robot to goal
        val newBoard = moveRobotOnBoard(board, 0, Board.NORTH)
        assertNotNull(newBoard, "Move should succeed")

        // Robot at goal
        assertEquals(true, isBoardSolved(newBoard), "Should be solved after moving to goal")
    }

    @Test
    fun testIsSolvedWithNoGoal() {
        // Create a board without a goal
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)

        // No goal set
        assertEquals(false, isBoardSolved(board), "Should not be solved without goal")
    }

    /**
     * Test that simulates the fragment-app gesture logic 1:1
     * This test simulates: ACTION_DOWN -> ACTION_MOVE -> ACTION_UP
     */
    @Test
    fun testGestureLogicMultipleMoves() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.setWall(2, 0, Board.NORTH, true) // Wall north of robot at (2, 0)

        println("[TEST] Initial robot position: (${board.robotPositions[0] % board.width}, ${board.robotPositions[0] / board.width})")

        // Simulate fragment-app gesture logic
        var currentBoard = board

        // Gesture 1: ACTION_DOWN -> ACTION_MOVE (north) -> ACTION_UP
        // ACTION_DOWN: Reset tracking variables
        var hasMovedRobotInCurrentGesture = false
        var robotActivatedBySwipe = false
        var robotMoveInitiated = false
        var touchedRobot: Int? = 0 // Robot 0 is touched

        println("[TEST] Gesture 1: ACTION_DOWN - Robot ${touchedRobot} touched")

        // ACTION_MOVE: Calculate distance and move robot
        val deltaX = 0f
        val deltaY = -100f // Swipe north
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
        val MIN_SWIPE_DISTANCE = 30f

        println("[TEST] Gesture 1: ACTION_MOVE - Distance: $distance, Threshold: $MIN_SWIPE_DISTANCE")

        if (distance >= MIN_SWIPE_DISTANCE && !robotMoveInitiated) {
            robotMoveInitiated = true
            robotActivatedBySwipe = false

            val direction = if (deltaY < 0) Board.NORTH else Board.SOUTH
            println("[TEST] Gesture 1: ACTION_MOVE - Moving robot $touchedRobot direction: $direction (NORTH)")

            val newBoard = moveRobotOnBoard(currentBoard, touchedRobot!!, direction)

            if (newBoard != null) {
                hasMovedRobotInCurrentGesture = true
                currentBoard = newBoard

                println("[TEST] Gesture 1: ACTION_MOVE - Move succeeded. New position: (${currentBoard.robotPositions[0] % currentBoard.width}, ${currentBoard.robotPositions[0] / currentBoard.width})")

                // Reset all tracking variables after successful move (matching fragment-app)
                touchedRobot = null
                robotMoveInitiated = false
                robotActivatedBySwipe = false
            } else {
                println("[TEST] Gesture 1: ACTION_MOVE - Move failed")
            }
        }

        // ACTION_UP: Reset all tracking variables
        touchedRobot = null
        hasMovedRobotInCurrentGesture = false
        robotMoveInitiated = false
        robotActivatedBySwipe = false

        println("[TEST] Gesture 1: ACTION_UP - Tracking variables reset")

        // Verify first move succeeded
        assertEquals(2, currentBoard.robotPositions[0], "Robot should move to (2, 0)")
        println("[TEST] Gesture 1: Verified - Robot at (2, 0)")

        // Gesture 2: ACTION_DOWN -> ACTION_MOVE (east) -> ACTION_UP
        // ACTION_DOWN: Reset tracking variables
        hasMovedRobotInCurrentGesture = false
        robotActivatedBySwipe = false
        robotMoveInitiated = false
        touchedRobot = 0 // Robot 0 is touched again

        println("[TEST] Gesture 2: ACTION_DOWN - Robot ${touchedRobot} touched")

        // ACTION_MOVE: Calculate distance and move robot
        val deltaX2 = 100f // Swipe east
        val deltaY2 = 0f
        val distance2 = kotlin.math.sqrt(deltaX2 * deltaX2 + deltaY2 * deltaY2)

        println("[TEST] Gesture 2: ACTION_MOVE - Distance: $distance2, Threshold: $MIN_SWIPE_DISTANCE")

        if (distance2 >= MIN_SWIPE_DISTANCE && !robotMoveInitiated) {
            robotMoveInitiated = true
            robotActivatedBySwipe = false

            val direction = if (deltaX2 > 0) Board.EAST else Board.WEST
            println("[TEST] Gesture 2: ACTION_MOVE - Moving robot $touchedRobot direction: $direction (EAST)")

            val newBoard = moveRobotOnBoard(currentBoard, touchedRobot!!, direction)

            if (newBoard != null) {
                hasMovedRobotInCurrentGesture = true
                currentBoard = newBoard

                println("[TEST] Gesture 2: ACTION_MOVE - Move succeeded. New position: (${currentBoard.robotPositions[0] % currentBoard.width}, ${currentBoard.robotPositions[0] / currentBoard.width})")

                // Reset all tracking variables after successful move (matching fragment-app)
                touchedRobot = null
                robotMoveInitiated = false
                robotActivatedBySwipe = false
            } else {
                println("[TEST] Gesture 2: ACTION_MOVE - Move failed")
            }
        }

        // ACTION_UP: Reset all tracking variables
        touchedRobot = null
        hasMovedRobotInCurrentGesture = false
        robotMoveInitiated = false
        robotActivatedBySwipe = false

        println("[TEST] Gesture 2: ACTION_UP - Tracking variables reset")

        // Verify second move succeeded (robot slides to right edge at (4, 0))
        assertEquals(4, currentBoard.robotPositions[0], "Robot should move to (4, 0)")
        println("[TEST] Gesture 2: Verified - Robot at (4, 0)")

        // Gesture 3: ACTION_DOWN -> ACTION_MOVE (south) -> ACTION_UP
        // ACTION_DOWN: Reset tracking variables
        hasMovedRobotInCurrentGesture = false
        robotActivatedBySwipe = false
        robotMoveInitiated = false
        touchedRobot = 0 // Robot 0 is touched again

        println("[TEST] Gesture 3: ACTION_DOWN - Robot ${touchedRobot} touched")

        // ACTION_MOVE: Calculate distance and move robot
        val deltaX3 = 0f
        val deltaY3 = 100f // Swipe south
        val distance3 = kotlin.math.sqrt(deltaX3 * deltaX3 + deltaY3 * deltaY3)

        println("[TEST] Gesture 3: ACTION_MOVE - Distance: $distance3, Threshold: $MIN_SWIPE_DISTANCE")

        if (distance3 >= MIN_SWIPE_DISTANCE && !robotMoveInitiated) {
            robotMoveInitiated = true
            robotActivatedBySwipe = false

            val direction = if (deltaY3 > 0) Board.SOUTH else Board.NORTH
            println("[TEST] Gesture 3: ACTION_MOVE - Moving robot $touchedRobot direction: $direction (SOUTH)")

            val newBoard = moveRobotOnBoard(currentBoard, touchedRobot!!, direction)

            if (newBoard != null) {
                hasMovedRobotInCurrentGesture = true
                currentBoard = newBoard

                println("[TEST] Gesture 3: ACTION_MOVE - Move succeeded. New position: (${currentBoard.robotPositions[0] % currentBoard.width}, ${currentBoard.robotPositions[0] / currentBoard.width})")

                // Reset all tracking variables after successful move (matching fragment-app)
                touchedRobot = null
                robotMoveInitiated = false
                robotActivatedBySwipe = false
            } else {
                println("[TEST] Gesture 3: ACTION_MOVE - Move failed")
            }
        }

        // ACTION_UP: Reset all tracking variables
        touchedRobot = null
        hasMovedRobotInCurrentGesture = false
        robotMoveInitiated = false
        robotActivatedBySwipe = false

        println("[TEST] Gesture 3: ACTION_UP - Tracking variables reset")

        // Verify third move succeeded (robot slides to bottom edge at (4, 4))
        assertEquals(24, currentBoard.robotPositions[0], "Robot should move to (4, 4)")
        println("[TEST] Gesture 3: Verified - Robot at (4, 4)")
        println("[TEST] Test completed successfully - Robot moved 3 times in sequence")
    }

    /**
     * Test that simulates the fragment-app gesture logic with robot activation by swipe
     */
    @Test
    fun testGestureLogicRobotActivationBySwipe() {
        // Create a simple 5x5 board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.setWall(2, 0, Board.NORTH, true) // Wall north of robot at (2, 0)

        var currentBoard = board

        // Simulate fragment-app gesture logic with robot activation by swipe
        var hasMovedRobotInCurrentGesture = false
        var robotActivatedBySwipe = false
        var robotMoveInitiated = false
        var touchedRobot: Int? = null

        // ACTION_DOWN: Touch empty space (not on robot)
        touchedRobot = null

        // ACTION_MOVE: Swipe over robot (robot activation by swipe)
        // Simulate passing over robot at (2, 2)
        touchedRobot = 0 // Robot found while swiping
        robotActivatedBySwipe = true

        // Continue swiping in north direction
        val deltaX = 0f
        val deltaY = -100f // Swipe north
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
        val ROBOT_MOVE_THRESHOLD = 50f

        if (distance >= ROBOT_MOVE_THRESHOLD && !robotMoveInitiated) {
            robotMoveInitiated = true
            robotActivatedBySwipe = false

            val direction = if (deltaY < 0) Board.NORTH else Board.SOUTH
            val newBoard = moveRobotOnBoard(currentBoard, touchedRobot!!, direction)

            if (newBoard != null) {
                hasMovedRobotInCurrentGesture = true
                currentBoard = newBoard

                // Reset all tracking variables after successful move (matching fragment-app)
                touchedRobot = null
                robotMoveInitiated = false
                robotActivatedBySwipe = false
            }
        }

        // ACTION_UP: Reset all tracking variables
        touchedRobot = null
        hasMovedRobotInCurrentGesture = false
        robotMoveInitiated = false
        robotActivatedBySwipe = false

        // Verify move succeeded
        assertEquals(2, currentBoard.robotPositions[0], "Robot should move to (2, 0)")
    }
}
