package roboyard.logic.core

import java.io.Serializable

/**
 * Represents a basic move in the game.
 */
class Move(
    @JvmField val robotColor: Int,
    @JvmField val direction: Int
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
