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

/**
 * Build the "possible moves" accessibility announcement for a robot — a full
 * port of Android GameFragment.announcePossibleMoves. For each direction it
 * reports how far the robot can slide and what stops it (edge, wall, or
 * another robot, optionally at its target).
 *
 * @param state Current game state
 * @param robot The robot to describe
 * @param stringProvider Localized strings (a11y keys)
 * @param colorName Robot color name lookup (e.g. HintManager/GameFragment style)
 * @return The announcement text, or null if no robot/state
 */
fun buildPossibleMovesAnnouncement(
    state: GameState?,
    robot: GameElement?,
    stringProvider: roboyard.logic.ui.StringProvider?,
    colorName: (Int) -> String
): String? {
    if (state == null || robot == null) return null
    fun s(key: String, fallback: String) = stringProvider?.getString(key) ?: fallback

    val x = robot.x
    val y = robot.y
    val sb = StringBuilder()
    sb.append(s("possible_moves_a11y", "Possible moves")).append(": ")

    // Per direction: dx, dy, distance-key, no-movement-key
    val dirs = listOf(
        intArrayOf(1, 0) to ("squares_east" to "no_movement_east"),
        intArrayOf(-1, 0) to ("squares_west" to "no_movement_west"),
        intArrayOf(0, -1) to ("squares_north" to "no_movement_north"),
        intArrayOf(0, 1) to ("squares_south" to "no_movement_south")
    )

    val edgeText = s("edge_a11y", "the edge")
    val wallText = s("wall_a11y", "a wall")

    for ((dxy, keys) in dirs) {
        var distance = 0
        var obstacle = edgeText
        var i = x + dxy[0]
        var j = y + dxy[1]
        while (i in 0 until state.width && j in 0 until state.height) {
            if (state.canRobotMoveTo(robot, i, j)) {
                distance++
            } else {
                val blocker = state.getRobotAt(i, j)
                obstacle = if (blocker != null) {
                    var name = colorName(blocker.color)
                    if (state.isRobotAtTarget(blocker)) {
                        name += " " + s("target_reached_a11y", "at its target")
                    }
                    name
                } else {
                    wallText
                }
                break
            }
            i += dxy[0]
            j += dxy[1]
        }
        if (distance > 0) {
            val untilString = when (obstacle) {
                edgeText -> s("until_masculine", "until")
                wallText -> s("until_feminine", "until")
                else -> s("until", "until")
            }
            sb.append(distance).append(" ")
                .append(s(keys.first, "squares")).append(" ")
                .append(untilString).append(" ").append(obstacle).append(", ")
        } else {
            sb.append(s(keys.second, "no movement")).append(", ")
        }
    }
    // Android ends the announcement with a period, not a trailing comma
    return sb.toString().trimEnd(',', ' ') + "."
}
