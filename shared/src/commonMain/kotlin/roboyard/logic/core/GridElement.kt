package roboyard.logic.core


/**
 * Represents a single element in the game grid (wall, robot, or target).
 * This class is used for map generation and data exchange.
 */
class GridElement(
    /**
     * Get the X coordinate
     * @return X coordinate
     */
    @JvmField var x: Int,
    /**
     * Get the Y coordinate
     * @return Y coordinate
     */
    @JvmField var y: Int,
    /**
     * Get the element type string
     * @return Type string (e.g., "mh", "robot_blue", "target_red")
     */
    @JvmField var type: String?
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
