package roboyard.logic.solver

import roboyard.logic.core.IGameMove

/**
 * Represents a single move in the game.
 * A move consists of a robot piece and its movement direction.
 * The robot will move in the specified direction until it hits
 * a wall or another robot.
 * 
 * This class implements the IGameMove interface and is used by both
 * the game logic and the solver to track robot movements.
 * 
 * @author Pierre Michel
 * @author Alain Caillaud
 * @see IGameMove
 * 
 * @see RRPiece
 * 
 * @see ERRGameMove
 */
class RRGameMove(private val actor: RRPiece, @JvmField val move: ERRGameMove) : IGameMove {
    val color: Int
        get() = this.actor.color

    val direction: Int
        get() = this.move.direction

    override fun toString(): String {
        return String.format("%d -> %s", this.actor.id, this.move.toString())
    }
}
