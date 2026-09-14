package roboyard.logic.core

import driftingdroids.model.Board

/**
 * Shared board utility functions for checking game state and moving robots.
 * Extracted to avoid duplication between Android app and ComposeApp (DRY).
 */

/**
 * Check if the board is in a solved state (goal robot is on the goal position).
 * @param board The board to check
 * @return true if the board is solved, false otherwise
 */
fun isBoardSolved(board: Board): Boolean {
    val goal = board.getGoal() ?: return false
    val goalRobot = goal.robotNumber
    return if (goalRobot in board.robotPositions.indices) {
        board.robotPositions[goalRobot] == goal.position
    } else {
        board.robotPositions.any { it == goal.position }
    }
}

/**
 * Move a robot in a direction until it hits a wall or another robot.
 * @param board The current board
 * @param robotIndex Index of the robot to move
 * @param direction Direction to move (Board.NORTH/EAST/SOUTH/WEST)
 * @return New board with the robot moved, or null if the robot couldn't move
 */
fun moveRobotOnBoard(board: Board, robotIndex: Int, direction: Int): Board? {
    val currentPos = board.robotPositions[robotIndex]
    var newPos = currentPos
    val directionIncrement = board.directionIncrement[direction]

    // Slide robot until it hits a wall or another robot
    while (true) {
        val nextPos = newPos + directionIncrement
        val x = nextPos % board.width
        val y = nextPos / board.width

        // Check bounds
        if (x < 0 || x >= board.width || y < 0 || y >= board.height) {
            break
        }

        // Check for wall
        if (board.isWall(newPos, direction)) {
            break
        }

        // Check for another robot
        var isBlocked = false
        for (i in board.robotPositions.indices) {
            if (i != robotIndex && board.robotPositions[i] == nextPos) {
                isBlocked = true
                break
            }
        }
        if (isBlocked) {
            break
        }

        newPos = nextPos
    }

    // Check if robot actually moved
    if (newPos == currentPos) {
        return null
    }

    // Create new board with updated robot position
    val newRobots = board.robotPositions.copyOf()
    newRobots[robotIndex] = newPos
    val newBoard = Board.Companion.createClone(board)
    newBoard.setRobots(newRobots)
    return newBoard
}
