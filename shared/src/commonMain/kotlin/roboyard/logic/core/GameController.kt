package roboyard.logic.core

import driftingdroids.model.Board
import roboyard.logic.util.RLog

/**
 * Shared game controller for robot movement with cooldown, undo, and path history.
 * Matches the Android app's GameStateManager.moveRobotInDirection behavior.
 *
 * Both the Android app and ComposeApp use this class for consistent move behavior.
 */
class GameController {

    private val log = RLog.tag("GameController")

    companion object {
        /** Move cooldown in milliseconds — matches Android MOVE_COOLDOWN_MS */
        const val MOVE_COOLDOWN_MS: Long = 400
    }

    /** Path history for undo: each entry is [color, fromX, fromY, toX, toY] */
    private val pathHistory: ArrayList<IntArray> = ArrayList()

    /** Last move timestamp for cooldown */
    private var lastMoveTime: Long = 0

    /** Whether the game is complete (affects undo behavior) */
    var isGameComplete: Boolean = false

    /**
     * Move a robot in a direction, with move cooldown.
     * Matches Android GameStateManager.moveRobotInDirection behavior.
     *
     * @param board The current board
     * @param robotIndex Index of the robot to move
     * @param direction Direction to move (Board.NORTH/EAST/SOUTH/WEST)
     * @return New board with the robot moved, or null if the move was blocked (cooldown, wall, etc.)
     */
    fun moveRobotWithCooldown(board: Board, robotIndex: Int, direction: Int): Board? {
        // Check if move cooldown is active
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMoveTime < MOVE_COOLDOWN_MS) {
            log.d("[MOVE_COOLDOWN] Move blocked: %dms remaining", MOVE_COOLDOWN_MS - (currentTime - lastMoveTime))
            return null
        }

        val result = moveRobotOnBoard(board, robotIndex, direction)
        if (result != null) {
            // Record move in path history
            val fromX = board.robotPositions[robotIndex] % board.width
            val fromY = board.robotPositions[robotIndex] / board.width
            val toX = result.robotPositions[robotIndex] % result.width
            val toY = result.robotPositions[robotIndex] / result.height
            addPathToHistory(robotIndex, fromX, fromY, toX, toY)

            // Activate cooldown
            lastMoveTime = currentTime
            log.d("[MOVE_COOLDOWN] Move completed, cooldown activated for %dms", MOVE_COOLDOWN_MS)
        }
        return result
    }

    /**
     * Check if moving the robot in the given direction would be an undo of the last move.
     * Matches Android GameStateManager undo detection logic.
     *
     * @param board The current board
     * @param robotIndex Index of the robot to move
     * @param direction Direction to move
     * @return true if this move would undo the last move
     */
    fun isUndoMove(board: Board, robotIndex: Int, direction: Int): Boolean {
        if (isGameComplete || pathHistory.isEmpty()) return false

        val lastPath = pathHistory[pathHistory.size - 1]
        val lastColor = lastPath[0]
        val lastFromX = lastPath[1]
        val lastFromY = lastPath[2]
        val lastToX = lastPath[3]
        val lastToY = lastPath[4]

        // Only applies to the same robot
        if (lastColor != robotIndex) return false

        val robotX = board.robotPositions[robotIndex] % board.width
        val robotY = board.robotPositions[robotIndex] / board.width

        // Robot must be at the last move's destination
        if (robotX != lastToX || robotY != lastToY) return false

        // Calculate where the robot would slide to
        val dx: Int
        val dy: Int
        when (direction) {
            Board.NORTH -> { dx = 0; dy = -1 }
            Board.SOUTH -> { dx = 0; dy = 1 }
            Board.EAST -> { dx = 1; dy = 0 }
            Board.WEST -> { dx = -1; dy = 0 }
            else -> return false
        }

        // Simulate slide to find end position
        var endX = robotX
        var endY = robotY
        if (dx != 0) {
            val step = if (dx > 0) 1 else -1
            var i = robotX + step
            while (i >= 0 && i < board.width) {
                if (canRobotMoveTo(board, robotIndex, i, robotY)) {
                    endX = i
                } else {
                    break
                }
                i += step
            }
        }
        if (dy != 0) {
            val step = if (dy > 0) 1 else -1
            var i = robotY + step
            while (i >= 0 && i < board.height) {
                if (canRobotMoveTo(board, robotIndex, robotX, i)) {
                    endY = i
                } else {
                    break
                }
                i += step
            }
        }

        // If the robot would land exactly on its previous position, it's an undo
        return endX == lastFromX && endY == lastFromY
    }

    /**
     * Undo the last move. Returns the board with the robot moved back.
     * @param board The current board
     * @return New board with the last move undone, or null if no moves to undo
     */
    fun undoLastMove(board: Board): Board? {
        if (pathHistory.isEmpty()) return null

        val lastPath = removeLastPathFromHistory() ?: return null
        val color = lastPath[0]
        val fromX = lastPath[1]
        val fromY = lastPath[2]

        // Move robot back to its previous position
        val newRobots = board.robotPositions.copyOf()
        newRobots[color] = fromX + fromY * board.width
        val newBoard = Board.Companion.createClone(board)
        newBoard.setRobots(newRobots)
        return newBoard
    }

    /**
     * Add a path entry to history.
     * Matches Android GameStateManager.addPathToHistory.
     */
    fun addPathToHistory(color: Int, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        pathHistory.add(intArrayOf(color, fromX, fromY, toX, toY))
    }

    /**
     * Remove and return the last path entry from history.
     * Matches Android GameStateManager.removeLastPathFromHistory.
     */
    fun removeLastPathFromHistory(): IntArray? {
        if (pathHistory.isEmpty()) return null
        return pathHistory.removeAt(pathHistory.size - 1)
    }

    /**
     * Clear all path history.
     * Matches Android GameStateManager.clearPathHistory.
     */
    fun clearPathHistory() {
        pathHistory.clear()
    }

    /**
     * Get the number of moves in path history.
     */
    fun getPathHistorySize(): Int = pathHistory.size

    /**
     * Get the path history list (for path tracking on undo).
     */
    fun getPathHistoryList(): List<IntArray> = pathHistory.toList()

    /**
     * Check if a robot can move to a position (not blocked by walls or other robots).
     * Matches Android GameState.canRobotMoveTo logic.
     */
    private fun canRobotMoveTo(board: Board, robotIndex: Int, x: Int, y: Int): Boolean {
        if (x < 0 || x >= board.width || y < 0 || y >= board.height) return false
        val pos = x + y * board.width

        // Check for another robot at this position
        for (i in board.robotPositions.indices) {
            if (i != robotIndex && board.robotPositions[i] == pos) {
                return false
            }
        }
        return true
    }

    /**
     * Reset the controller for a new game.
     */
    fun reset() {
        pathHistory.clear()
        lastMoveTime = 0
        isGameComplete = false
    }
}
