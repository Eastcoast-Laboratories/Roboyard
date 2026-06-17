package roboyard.logic.core


/**
 * Represents a basic move in the game.
 */
class Move(
 val robotColor: Int,
 val direction: Int
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
