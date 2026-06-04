package roboyard.logic.core

import java.io.Serializable

/**
 * Represents a single move of a robot in the game.
 */
class GameMove(
    @JvmField val robotColor: Int,
    @JvmField val direction: Int,
    @JvmField val startX: Int,
    @JvmField val startY: Int,
    @JvmField var endX: Int,
    @JvmField var endY: Int
) : IGameMove, Serializable {

    fun getRobotColor(): Int = robotColor
    fun getDirection(): Int = direction

    companion object {
        private const val serialVersionUID = 1L
    }
}
