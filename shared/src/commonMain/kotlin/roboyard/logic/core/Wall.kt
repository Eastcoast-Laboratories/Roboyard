package roboyard.logic.core

/**
 * Represents a wall in the game board.
 * A wall is defined by its position (x,y) and type (horizontal or vertical).
 */
class Wall
/**
 * Creates a new wall at the specified position with the specified type.
 * 
 * @param x The x-coordinate of the wall
 * @param y The y-coordinate of the wall
 * @param type The type of wall (horizontal or vertical)
 */(
    /**
     * Gets the x-coordinate of the wall.
     * 
     * @return The x-coordinate
     */
    @JvmField val x: Int,
    /**
     * Gets the y-coordinate of the wall.
     * 
     * @return The y-coordinate
     */
    @JvmField val y: Int,
    /**
     * Gets the type of the wall.
     * 
     * @return The wall type (horizontal or vertical)
     */
    @JvmField val type: WallType?
) {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val wall = o as Wall

        if (x != wall.x) return false
        if (y != wall.y) return false
        return type == wall.type
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + (if (type != null) type.hashCode() else 0)
        return result
    }

    override fun toString(): String {
        return "Wall{" +
                "x=" + x +
                ", y=" + y +
                ", type=" + type +
                '}'
    }
}
