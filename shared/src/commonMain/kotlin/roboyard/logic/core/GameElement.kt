package roboyard.logic.core

import roboyard.logic.util.RLog

/**
 * Represents a game element such as a robot or target.
 */
class GameElement
/**
 * Create a new game element
 * @param type Element type
 * @param x Initial X position
 * @param y Initial Y position
 */(
    /**
     * Get the element type
     * @return Element type (TYPE_ROBOT or TYPE_TARGET)
     */
    // Element properties
    @JvmField val type: Int,
    /**
     * Set the X position
     * @param x New X position
     */
    @JvmField var x: Int,
    /**
     * Set the Y position
     * @param y New Y position
     */
    @JvmField var y: Int
) {
    private val log = RLog.tag("GameElement")

    @JvmField
    var color: Int = 0 // 0=red, 1=green, 2=blue, 3=yellow

    var isSelected: Boolean = false

    /**
     * Get the horizontal direction the robot is facing (for the display of the right robot image)
     * @return 1 for right, -1 for left
     */
    var directionX: Int = 1 // Default direction (1=right, -1=left)
        /**
         * Set the horizontal direction the robot is facing
         * @param direction 1 for right, -1 for left
         */
        set(direction) {
            if (direction != 0) {
                field = if (direction > 0) 1 else -1
            }
        }

    // Transient properties for animation (not serialized)
    @Transient
    var animationX: Float = 0f
        private set

    @Transient
    var animationY: Float = 0f
        private set

    @Transient
    private var hasAnimationPosition = false

    @Transient
    private var animationPositionSet = false

    val isRobot: Boolean
        get() = type == TYPE_ROBOT

    /**
     * Check if this element has an animation position set
     * @return true if animation position is set
     */
    fun hasAnimationPosition(): Boolean {
        // Only return true if animationX and animationY are explicitly set
        return animationPositionSet
    }

    /**
     * Set the animation position for this element
     * @param x X position in grid coordinates
     * @param y Y position in grid coordinates
     */
    fun setAnimationPosition(x: Float, y: Float) {
        // Log the position change for debugging
        if (GameLogic.hasDebugLogging()) {
            log.d(
                "[ANIM] Set animation position for %s robot: (%.2f,%.2f)",
                GameLogic.getColorName(this.color, true), x, y
            )
        }


        // Validate inputs to avoid setting invalid positions
        if (x.isNaN() || y.isNaN() || x.isInfinite() || y.isInfinite()) {
            log.e("[ANIM] Attempted to set invalid animation position: (%.2f,%.2f)", x, y)
            return
        }

        this.animationX = x
        this.animationY = y
        this.hasAnimationPosition = true
        this.animationPositionSet = true
    }

    /**
     * Clear the animation position
     */
    fun clearAnimationPosition() {
        animationPositionSet = false
    }

    companion object {
        private const val serialVersionUID = 1L

        // Element types
        const val TYPE_ROBOT: Int = 1
        const val TYPE_TARGET: Int = 2
        const val TYPE_HORIZONTAL_WALL: Int = 3
        const val TYPE_VERTICAL_WALL: Int = 4

        // Color constants
        const val COLOR_RED: Int = 0
        const val COLOR_GREEN: Int = 1
        const val COLOR_BLUE: Int = 2
        const val COLOR_YELLOW: Int = 3
        const val COLOR_SILVER: Int = 4
    }
}
