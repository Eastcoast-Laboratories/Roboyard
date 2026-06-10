package roboyard.logic.core

import kotlin.math.max
import kotlin.random.Random
import kotlin.math.min
import roboyard.logic.util.RLog

/**
 * Created by Alain on 04/02/2015.
 * 
 * This class has been modified to use GameLogic internally while maintaining
 * backward compatibility with the original implementation.
 */
class MapGenerator {
    private val log = RLog.tag("MapGenerator")
    private val rand: Random
    private val gameLogic: GameLogic?

    // position of the square in the middle of the game board
    var carrePosX: Int // horizontal position of the top wall of square, starting with 0
    var carrePosY: Int // vertical position of the left wall of the square

    var targetMustBeInCorner: Boolean =
        true // TODO: only works together with generateNewMapEachTime==true (which is set only in Beginner Mode)
    var allowMulticolorTarget: Boolean = true

    /**
     * Gets the current robot count setting
     * @return Number of robots per color (1-4)
     */
    var robotCount: Int = 1 // Default to 1 robot per color
        /**
         * Sets the number of robots per color for map generation
         * @param count Number of robots per color (1-4)
         */
        set(count) {
            field = max(
                1,
                min(Constants.NUM_ROBOTS, count)
            )


            // Pass the robot count to the GameLogic if it exists
            if (gameLogic != null) {
                gameLogic.robotCount = field
            }

            log.d("MapGenerator robot count set to %d", field)
        }

    /**
     * Gets the current target colors setting
     * @return Number of different target colors (1-4)
     */
    var targetColors: Int = Constants.NUM_ROBOTS // Default to 4 different target colors
        /**
         * Sets the number of different target colors for map generation
         * @param count Number of different target colors (1-4)
         */
        set(count) {
            field = max(1, min(4, count))


            // Pass the target colors to the GameLogic if it exists
            if (gameLogic != null) {
                gameLogic.setTargetColors(field)
            }

            log.d("MapGenerator target colors set to %d", field)
        }

    // Wall configuration
    var maxWallsInOneVerticalCol: Int = 2 // Maximum number of walls allowed in one vertical column
    var maxWallsInOneHorizontalRow: Int = 2 // Maximum number of walls allowed in one horizontal row
    var wallsPerQuadrant: Int // Number of walls to place in each quadrant of the board

    var loneWallsAllowed: Boolean = false // walls that are not attached in a 90 deg. angle

    init {
        rand = Random.Default

        // Initialize square position based on current board size
        carrePosX = (Preferences.boardSizeX / 2) - 1
        carrePosY = (Preferences.boardSizeY / 2) - 1

        // Calculate walls per quadrant based on board width
        wallsPerQuadrant = Preferences.boardSizeX / 4 // Default: quarter of board width

        // Get difficulty directly from Preferences
        val level = Preferences.difficulty


        // Initialize GameLogic with the same configuration
        gameLogic = GameLogic(Preferences.boardSizeX, Preferences.boardSizeY, level)


        // Synchronize with GameLogic's static setting
        GameLogic.setgenerateNewMapEachTime(generateNewMapEachTime)

        if (level == Constants.DIFFICULTY_BEGINNER) { // Difficulty Beginner
            // For beginner level
        } else {
            if (level == Constants.DIFFICULTY_ADVANCED) { // Advanced
                // nothing to do
            }

            if (generateNewMapEachTime) {
                // TODO: doesn't work if not generateNewMapEachTime because the position is not remembered above restarts with the same map
                // TODO: does not work with the roboyard in the middle, that is not moved to the new random position
                // random position of square in the middle
                // carrePosX=getRandom(3,Preferences.boardSizeX-5);
                // carrePosY=getRandom(3,Preferences.boardSizeY-5);
            }

            if (level == Constants.DIFFICULTY_INSANE) { // Insane
                allowMulticolorTarget = false
                // target must be in corner is left to "true"
            }

            if (level == Constants.DIFFICULTY_IMPOSSIBLE) { // Impossible
                // Get a completely different wall layout then the target is harder to reach
                loneWallsAllowed = true
                maxWallsInOneVerticalCol = 3
                maxWallsInOneHorizontalRow = 3
                targetMustBeInCorner = false
            }
        }

        if (level == Constants.DIFFICULTY_INSANE || level == Constants.DIFFICULTY_IMPOSSIBLE) {
            targetMustBeInCorner = false

            maxWallsInOneVerticalCol = 5
            maxWallsInOneHorizontalRow = 5
            wallsPerQuadrant = Preferences.boardSizeX / 3
        }
        if (level == Constants.DIFFICULTY_IMPOSSIBLE) {
            wallsPerQuadrant =
                (Preferences.boardSizeX / 2.3).toInt() // for debug, set to 1.3 with lots of walls
        }
        if (Preferences.boardSizeX * Preferences.boardSizeY > 64) {
            // calculate maxWallsInOneVerticalCol and maxWallsInOneHorizontalRow based on board size
        }

        log.d("wallsPerQuadrant: " + wallsPerQuadrant + " Board size: " + Preferences.boardSizeX + "x" + Preferences.boardSizeY)
    }


    fun removeGameElementsFromMap(data: java.util.ArrayList<GridElement>): java.util.ArrayList<GridElement> {
        // Delegate to GameLogic
        return gameLogic!!.removeGameElementsFromMap(data)
    }

    fun translateArraysToMap(
        horizontalWalls: Array<IntArray?>,
        verticalWalls: Array<IntArray?>
    ): java.util.ArrayList<GridElement> {
        // Delegate to GameLogic
        return gameLogic!!.translateArraysToMap(horizontalWalls, verticalWalls)
    }

    fun getRandom(min: Int, max: Int): Int {
        // Delegate to GameLogic
        return gameLogic!!.getRandom(min, max)
    }

    val generatedGameMap: ArrayList<GridElement>?
        /**
         * generates a new map. The map is divided into four quadrants, like in the game ricochet robots. and walls are evenly distributed among all quadrants.
         * for each quadrant there are 2 walls placed at a right-angle to the each border.
         * @return Arraylist with all grid elements that belong to the map
         */
        get() {
            // We'll check the latest value of generateNewMapEachTime from our static variable
            // which is already set by SettingsGameScreen when preferences are changed
            log.d(
                "[WALL STORAGE] class default value for generateNewMapEachTime: %s",
                generateNewMapEachTime
            )

            var data = java.util.ArrayList<GridElement>()


            // Synchronize static settings with GameLogic
            GameLogic.setgenerateNewMapEachTime(generateNewMapEachTime)


            // Check if we should preserve walls
            val wallStorage = WallStorage.getInstance()
            // Update board size to ensure we're using the right storage
            wallStorage.updateCurrentBoardSize()


            // Check if dice button was pressed (one-time override)
            val forceNewMap: Boolean = forceGenerateNewMapOnce
            if (forceGenerateNewMapOnce) {
                log.d("[DICE_BUTTON] Force new map flag is set, generating new map once")
                forceGenerateNewMapOnce = false // Reset the flag after use
            }

            val preserveWalls =
                !Preferences.generateNewMapEachTime && !forceNewMap && wallStorage.hasStoredWalls()
            log.d(
                "[WALL STORAGE] MapGenerator: generateNewMapEachTime: %s, forceNewMap: %s, Preserving walls: %s, hasStoredWalls: %s",
                Preferences.generateNewMapEachTime,
                forceNewMap,
                preserveWalls,
                wallStorage.hasStoredWalls()
            )

            if (preserveWalls) {
                log.d("[WALL STORAGE] Preserving walls from stored configuration")
                // Remove game elements (robots and targets) but keep walls
                if (!data.isEmpty()) {
                    data = removeGameElementsFromMap(data)


                    // Apply stored walls to the map
                    data = wallStorage.applyWallsToElements(data)
                } else {
                    // If no existing map, use the stored walls
                    // Convert stored walls (ArrayList<GridElement?>) to ArrayList<GridElement>
                    val stored = wallStorage.getStoredWalls()
                    for (ge in stored) {
                        if (ge != null) data.add(ge)
                    }
                }


                // IMPORTANT: Ensure all outer walls exist before adding game elements
                // This ensures consistent wall behavior even with preserved walls
                data = gameLogic!!.ensureOuterWalls(data)
                log.d(
                    "[WALL STORAGE] MapGenerator - Verified outer walls in preserved walls: %d elements",
                    data.size
                )


                // Delegate to GameLogic to add robots and targets
                data = gameLogic.addGameElementsToGameMap(data, null, null)
            } else {
                // Generate a completely new map
                val newData = gameLogic!!.generateGameMap(null) ?: return null


                // IMPORTANT: Explicitly ensure all outer walls exist before storing
                data = gameLogic.ensureOuterWalls(newData)
                log.d(
                    "[WALL STORAGE] MapGenerator - Verified outer walls in new map: %d elements",
                    data.size
                )


                // Store the walls for future use if we're not generating new maps each time
                if (!Preferences.generateNewMapEachTime) {
                    wallStorage.storeWalls(data)
                    log.d("[WALL STORAGE] Stored walls for future use")
                }
            }

            return data
        }

    companion object {
        var generateNewMapEachTime: Boolean = true // option in settings

        // Flag to force generating a new map once (used by dice button in game screen)
        // This flag is automatically reset to false after the next map generation
        @JvmField
        var forceGenerateNewMapOnce: Boolean = false
    }
}
