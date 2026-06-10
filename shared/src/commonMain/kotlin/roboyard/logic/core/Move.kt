package roboyard.logic.core


/**
 * Represents a basic move in the game.
 */
class Move(
    @JvmField val robotColor: Int,
    @JvmField val direction: Int
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
