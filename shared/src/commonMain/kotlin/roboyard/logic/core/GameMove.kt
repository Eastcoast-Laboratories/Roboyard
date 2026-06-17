package roboyard.logic.core


/**
 * Represents a single move of a robot in the game.
 */
class GameMove constructor(
 val robotColor: Int,
 val direction: Int,
 val startX: Int,
 val startY: Int,
 var endX: Int,
 var endY: Int,
 val robotId: Int = 0
) : IGameMove {

    // Secondary constructor for Java tests: robotId, direction, distance
    constructor(robotId: Int, direction: Int, distance: Int) : this(
        robotColor = -1, // unknown color as expected by tests
        direction = direction,
        startX = 0,
        startY = 0,
        endX = when (direction) {
            0 -> 0 // UP: same X
            1 -> distance // RIGHT: X + distance
            2 -> 0 // DOWN: same X
            3 -> -distance // LEFT: X - distance
            else -> 0
        },
        endY = when (direction) {
            0 -> -distance // UP: Y - distance
            1 -> 0 // RIGHT: same Y
            2 -> distance // DOWN: Y + distance
            3 -> 0 // LEFT: same Y
            else -> 0
        },
        robotId = robotId
    )

    // Java-compatible field aliases
 val fromX: Int = startX
 val fromY: Int = startY
 val toX: Int = endX
 val toY: Int = endY

    fun getRobotColor(): Int = robotColor
    
    /**
     * Calculate direction from coordinates (for Java test compatibility).
     * Returns UP(0), RIGHT(1), DOWN(2), or LEFT(3) based on coordinate changes.
     */
    fun getDirection(): Int {
        val dx = toX - fromX
        val dy = toY - fromY
        return when {
            dx > 0 -> RIGHT
            dx < 0 -> LEFT
            dy > 0 -> DOWN
            else -> UP
        }
    }
    
    fun getDistance(): Int = kotlin.math.abs(toX - fromX) + kotlin.math.abs(toY - fromY)

    fun getRobotColorName(): String {
        return when (direction) {
            0 -> "Red"
            1 -> "Green"
            2 -> "Blue"
            3 -> "Yellow"
            4 -> "Silver"
            else -> "Unknown"
        }
    }

    override fun toString(): String {
        val dirName = when (getDirection()) {
            UP -> "Up"
            RIGHT -> "Right"
            DOWN -> "Down"
            LEFT -> "Left"
            else -> "Unknown"
        }
        return "${getRobotColorName()} $dirName (${getDistance()})"
    }

    companion object {
        private const val serialVersionUID = 1L

        // Direction constants for Java compatibility
 val UP: Int = 0
 val RIGHT: Int = 1
 val DOWN: Int = 2
 val LEFT: Int = 3
    }
}
