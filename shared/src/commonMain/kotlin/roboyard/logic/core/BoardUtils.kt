package roboyard.logic.core

import driftingdroids.model.Board

/**
 * Shared board utility functions for checking game state and moving robots.
 * Extracted to avoid duplication between Android app and ComposeApp (DRY).
 */

/**
 * Check if the board is in a solved state.
 * Matches Android app's GameState.areAllRobotsAtTargets() logic:
 * - Checks ALL robots against ALL targets (not just the goal robot)
 * - A robot is "at target" if it's on a target of matching color, or on a multi-color target
 * - Game is complete when enough robots are at targets (min of robotCount and goals.size)
 * @param board The board to check
 * @return true if the board is solved, false otherwise
 */
fun isBoardSolved(board: Board): Boolean {
    if (board.goals.isEmpty()) return false

    var robotsAtTarget = 0
    for (robotIndex in board.robotPositions.indices) {
        val robotPos = board.robotPositions[robotIndex]
        val robotX = robotPos % board.width
        val robotY = robotPos / board.width

        for (goal in board.goals) {
            if (goal.x == robotX && goal.y == robotY) {
                // Multi-color target (robotNumber < 0) matches any robot
                if (goal.robotNumber < 0 || goal.robotNumber == robotIndex) {
                    robotsAtTarget++
                    break
                }
            }
        }
    }

    val requiredRobots = minOf(board.robotPositions.size, board.goals.size)
    return robotsAtTarget >= requiredRobots
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
