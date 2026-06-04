package roboyard.logic.core

import java.util.Collections

/**
 * Model class that represents all walls on a game board.
 * This class serves as a single source of truth for wall data.
 */
class WallModel
/**
 * Creates a new wall model with the specified board dimensions.
 * 
 * @param boardWidth The width of the game board
 * @param boardHeight The height of the game board
 */(
    /**
     * Gets the width of the board.
     * 
     * @return The board width
     */
    @JvmField val boardWidth: Int,
    /**
     * Gets the height of the board.
     * 
     * @return The board height
     */
    @JvmField val boardHeight: Int
) {
    private val walls: MutableList<Wall?> = ArrayList<Wall?>()

    /**
     * Adds a wall to the model.
     * 
     * @param x The x-coordinate of the wall
     * @param y The y-coordinate of the wall
     * @param type The type of wall (horizontal or vertical)
     */
    fun addWall(x: Int, y: Int, type: WallType?) {
        val wall = Wall(x, y, type)
        if (!walls.contains(wall)) {
            walls.add(wall)
        }
    }

    /**
     * Gets all walls in the model.
     * 
     * @return An unmodifiable list of all walls
     */
    fun getWalls(): MutableList<Wall?> {
        return Collections.unmodifiableList<Wall?>(walls)
    }


    companion object {
        /**
         * Creates a WallModel from a list of GameElement objects.
         * 
         * @param elements The list of game elements
         * @param width The width of the game board
         * @param height The height of the game board
         * @return A new WallModel containing all walls from the game elements
         */
        @JvmStatic
        fun fromGameElements(
            elements: MutableList<GameElement>,
            width: Int,
            height: Int
        ): WallModel {
            val model = WallModel(width, height)

            for (element in elements) {
                if (element.getType() == Constants.TYPE_HORIZONTAL_WALL) {
                    model.addWall(element.getX(), element.getY(), WallType.HORIZONTAL)
                    // Timber.d("Added horizontal wall at (%d,%d)", element.getX(), element.getY());
                } else if (element.getType() == Constants.TYPE_VERTICAL_WALL) {
                    model.addWall(element.getX(), element.getY(), WallType.VERTICAL)
                    // Timber.d("Added vertical wall at (%d,%d)", element.getX(), element.getY());
                }
            }

            return model
        }

        /**
         * Creates a WallModel from a list of GridElement objects.
         * This is used for compatibility with the older grid element system.
         * 
         * @param elements The list of grid elements
         * @param width The width of the game board
         * @param height The height of the game board
         * @return A new WallModel containing all walls from the grid elements
         */
        @JvmStatic
        fun fromGridElements(
            elements: MutableList<GridElement>,
            width: Int,
            height: Int
        ): WallModel {
            val model = WallModel(width, height)

            for (element in elements) {
                val type = element.getType()
                if ("mh" == type) {
                    model.addWall(element.getX(), element.getY(), WallType.HORIZONTAL)
                    // Timber.d("Added horizontal wall at (%d,%d)", element.getX(), element.getY());
                } else if ("mv" == type) {
                    model.addWall(element.getX(), element.getY(), WallType.VERTICAL)
                    // Timber.d("Added vertical wall at (%d,%d)", element.getX(), element.getY());
                }
            }

            return model
        }
    }
}
