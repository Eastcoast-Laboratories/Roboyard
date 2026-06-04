package roboyard.logic.solver

/**
 * 
 * @author Pierre Michel
 */
enum class ERRGameMove(val direction: Int) {
    NOMOVE(0), UP(1), RIGHT(2), DOWN(4), LEFT(8);

    override fun toString(): String {
        when (this.direction) {
            0 -> return "No Move"
            1 -> return "Up"
            2 -> return "Right"
            4 -> return "Down"
            8 -> return "Left"
            else -> return "Unknown?"
        }
    }
}
