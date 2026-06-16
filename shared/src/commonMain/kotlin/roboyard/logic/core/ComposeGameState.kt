package roboyard.logic.core

import driftingdroids.model.Board
import driftingdroids.model.SolverIDDFS
import driftingdroids.model.Solution

/**
 * Simple game state for Compose UI.
 * Manages board, moves, undo/redo, and solver hints.
 */
class ComposeGameState(val board: Board) {
    var moveCount: Int = 0
        private set
    
    private val moveHistory: MutableList<IntArray> = mutableListOf()
    private val redoStack: MutableList<IntArray> = mutableListOf()
    
    private var currentSolution: Solution? = null
    private var solver: SolverIDDFS? = null
    
    /**
     * Execute a move: move robot in direction until it hits a wall.
     * Returns true if move was valid (robot moved), false otherwise.
     */
    fun moveRobot(robotIndex: Int, direction: Int): Boolean {
        val startPos = board.robotPositions[robotIndex]
        val endPos = calculateEndPosition(startPos, direction)
        
        if (endPos != startPos) {
            board.setRobot(robotIndex, endPos, true)
            moveHistory.add(intArrayOf(robotIndex, direction, startPos, endPos))
            moveCount++
            redoStack.clear()
            return true
        }
        return false
    }
    
    /**
     * Calculate the end position when moving a robot in a direction.
     * Robot moves until it hits a wall or another robot.
     */
    private fun calculateEndPosition(startPos: Int, direction: Int): Int {
        var currentPos = startPos
        val width = board.width
        val height = board.height
        
        while (true) {
            val nextPos = when (direction) {
                Board.NORTH -> currentPos - width
                Board.SOUTH -> currentPos + width
                Board.EAST -> currentPos + 1
                Board.WEST -> currentPos - 1
                else -> currentPos
            }
            
            // Check bounds
            val x = nextPos % width
            val y = nextPos / width
            if (x < 0 || x >= width || y < 0 || y >= height) {
                break
            }
            
            // Check wall
            if (board.isWall(nextPos, direction)) {
                break
            }
            
            // Check if next position is occupied by another robot
            var occupied = false
            for (i in board.robotPositions.indices) {
                if (board.robotPositions[i] == nextPos) {
                    occupied = true
                    break
                }
            }
            if (occupied) {
                break
            }
            
            currentPos = nextPos
        }
        
        return currentPos
    }
    
    /**
     * Undo the last move.
     */
    fun undo(): Boolean {
        if (moveHistory.isEmpty()) return false
        
        val lastMove = moveHistory.removeLast()
        val robotIndex = lastMove[0]
        val startPos = lastMove[2]
        
        board.setRobot(robotIndex, startPos, true)
        moveCount--
        redoStack.add(lastMove)
        return true
    }
    
    /**
     * Redo the last undone move.
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        
        val move = redoStack.removeLast()
        val robotIndex = move[0]
        val endPos = move[3]
        
        board.setRobot(robotIndex, endPos, true)
        moveHistory.add(move)
        moveCount++
        return true
    }
    
    /**
     * Get a hint from the solver.
     * Returns robot index and direction for the next move, or null if no solution found.
     */
    fun getHint(): IntArray? {
        if (currentSolution == null) {
            solveBoard()
        }
        
        // For now, return null as hint implementation requires more work
        // This will be implemented when integrating the full solver
        return null
    }
    
    /**
     * Solve the current board.
     */
    private fun solveBoard() {
        solver = SolverIDDFS(board)
        // The solver will be integrated later
        currentSolution = null
    }
    
    /**
     * Check if the current position is a goal.
     * Check if any robot is on its target position.
     */
    fun isGoal(): Boolean {
        // Check if any robot is on its target
        for (goal in board.goals) {
            val robotIndex = goal.robotNumber
            if (robotIndex >= 0 && robotIndex < board.robotPositions.size) {
                if (board.robotPositions[robotIndex] == goal.position) {
                    return true
                }
            }
        }
        return false
    }
}
