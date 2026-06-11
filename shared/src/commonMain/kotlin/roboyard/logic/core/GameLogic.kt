package roboyard.logic.core

import kotlin.math.max
import kotlin.random.Random
import kotlin.math.min
import roboyard.logic.util.RLog

/**
 * A UI-agnostic class that contains the core game logic for map generation.
 */
class GameLogic @JvmOverloads constructor(// Board dimensions
    private val boardWidth: Int,
    private val boardHeight: Int,
    difficultyLevel: Int,
    private val rand: Random = Random.Default
) {
    private val log = RLog.tag("GameLogic")

    companion object {
        private val companionLog = RLog.tag("GameLogic")

        val DIFFICULTY_BEGINNER: Int = Constants.DIFFICULTY_BEGINNER
        val DIFFICULTY_ADVANCED: Int = Constants.DIFFICULTY_ADVANCED
        val DIFFICULTY_INSANE: Int = Constants.DIFFICULTY_INSANE
        val DIFFICULTY_IMPOSSIBLE: Int = Constants.DIFFICULTY_IMPOSSIBLE

        private var generateNewMapEachTime = true // option in settings

        /**
         * Set whether to generate a new map each time
         */
        @JvmStatic
        fun setgenerateNewMapEachTime(value: Boolean) {
            generateNewMapEachTime = value
        }

        /**
         * Convert a color ID to its corresponding name
         * @param colorId The color ID from Constants
         * @param capitalize Whether to capitalize the first letter of the color name
         * @return The color name as a string
         */
        @JvmStatic
        fun getColorName(colorId: Int, capitalize: Boolean): String {
            val name: String?
            when (colorId) {
                Constants.COLOR_PINK -> name = "pink"
                Constants.COLOR_GREEN -> name = "green"
                Constants.COLOR_BLUE -> name = "blue"
                Constants.COLOR_YELLOW -> name = "yellow"
                Constants.COLOR_SILVER -> name = "silver"
                Constants.COLOR_RED -> name = "red"
                Constants.COLOR_BROWN -> name = "brown"
                Constants.COLOR_ORANGE -> name = "orange"
                Constants.COLOR_WHITE -> name = "white"
                Constants.COLOR_MULTI -> name = "multi"
                else -> {
                    companionLog.w("[COLOR] Unknown color ID: %d", colorId)
                    throw IllegalArgumentException("Unknown color ID: " + colorId)
                }
            }

            return if (capitalize) capitalizeFirstLetter(name) else name
        }

        /**
         * Helper method to capitalize the first letter of a string
         */
        private fun capitalizeFirstLetter(str: String?): String {
            if (str == null || str.isEmpty()) return str ?: ""
            return str.substring(0, 1).uppercase() + str.substring(1)
        }

        /**
         * Get the color ID from an object type string ("robot_X" or "target_X")
         * @param objectType The object type string
         * @return The color ID from Constants
         */
        private fun getColorIdFromObjectType(objectType: String): Int {
            val colorName = objectType.substring(objectType.indexOf('_') + 1)
            return getColorIdFromName(colorName)
        }

        /**
         * Get the color ID from a color name
         * @param colorName The color name (e.g., "red", "blue", "multi")
         * @return The color ID from Constants
         */
        private fun getColorIdFromName(colorName: String): Int {
            return when (colorName.lowercase()) {
                "pink" -> return Constants.COLOR_PINK
                "green" -> return Constants.COLOR_GREEN
                "blue" -> return Constants.COLOR_BLUE
                "yellow" -> return Constants.COLOR_YELLOW
                "silver" -> return Constants.COLOR_SILVER
                "red" -> return Constants.COLOR_RED
                "brown" -> return Constants.COLOR_BROWN
                "orange" -> return Constants.COLOR_ORANGE
                "white" -> return Constants.COLOR_WHITE
                "multi" -> return Constants.COLOR_MULTI
                else -> {
                    companionLog.w("[COLOR] Unknown color name: %s", colorName)
                    throw IllegalArgumentException("Unknown color name: " + colorName)
                }
            }
        }

        /**
         * Get the object type string ("robot_X" or "target_X") from color ID
         * @param colorId The color ID from Constants
         * @param isRobot Whether this is a robot (true) or target (false)
         * @return The object type string
         */
        private fun getObjectType(colorId: Int, isRobot: Boolean): String {
            val colorName = getColorName(colorId, false)
            return if (isRobot) "robot_$colorName" else "target_$colorName"
        }

        /**
         * Get the color (RGB value) from an object type string
         * @param objectType The object type string (e.g., "robot_red", "target_blue")
         * @return The RGB color value
         */
        @JvmStatic
        fun getColor(objectType: String): Int {
            val colorId: Int = getColorIdFromObjectType(objectType)
            // Special case for multi-colored targets
            if (colorId == Constants.COLOR_MULTI) {
                return 0xFFFFFFFF.toInt() // Default color for multi-target (WHITE)
            }
            if (colorId >= 0 && colorId < Constants.colors_rgb.size) {
                return Constants.colors_rgb[colorId]
            }
            companionLog.w("[COLOR] Invalid color ID: %d from objectType: %s", colorId, objectType)
            throw IllegalArgumentException("getColor: Invalid color ID: " + colorId + " from objectType: " + objectType)
        }

        /**
         * Check if any targets exist in the gridElements list
         * @param gridElements The list of grid elements to check
         * @return true if at least one target is found, false otherwise
         */
        fun hasTargets(gridElements: ArrayList<GridElement>?): Boolean {
            if (gridElements == null) {
                companionLog.e("[TARGET CHECK] gridElements is null!")
                return false
            }

            companionLog.d("[TARGET CHECK] Checking %d grid elements for targets", gridElements.size)
            var targetCount = 0
            for (element in gridElements) {
                val type = element.type
                if (type != null && type.startsWith("target_")) {
                    targetCount++
                    companionLog.d(
                        "[TARGET CHECK] Found target of type %s at position (%d,%d)",
                        type, element.x, element.y
                    )
                }
            }

            companionLog.d("[TARGET CHECK] Found %d targets", targetCount)
            return targetCount > 0
        }

        /**
         * Check if debug logging is enabled
         * @return true if debug logging is enabled
         */
        @JvmStatic
        fun hasDebugLogging(): Boolean {
            // For now, always return false to minimize log output
            return false
        }
    }

    // position of the square in the middle of the game board
    private val carrePosX: Int // horizontal position of the top wall of square, starting with 0
    private val carrePosY: Int // vertical position of the left wall of the square

    private var targetMustBeInCorner = true
    private var allowMulticolorTarget = true

    // Wall configuration
    private var maxWallsInOneVerticalCol =
        2 // Maximum number of walls allowed in one vertical column
    private var maxWallsInOneHorizontalRow =
        2 // Maximum number of walls allowed in one horizontal row
    private var wallsPerQuadrant: Int // Number of walls to place in each quadrant of the board

    private var loneWallsAllowed = false // walls that are not attached in a 90 deg. angle

    // Current difficulty level
    private var currentLevel: Int

    /**
     * Get the current robot count setting
     * @return Number of robots per color (1-4)
     */
    // Configuration for multiple targets
    var robotCount: Int = 1 // Default to 1 robot per color
        /**
         * Set the number of robots per color to be generated on the map
         * @param count Number of robots (1-4)
         */
        set(count) {
            // Ensure count is between 1 and 4
            field = max(
                1,
                min(Constants.NUM_ROBOTS, count)
            )
            companionLog.d("Robot count set to %d", field)
        }
    private var targetColors =
        1 // Anzahl der verschiedenen Zielfarben (1-4) (overridden by Preferences )

    // Configuration for the simplified board generation
    private val placeWallsInCorners = true
    private val placeWallsOnEdges = true
    private val placeWallsInMiddleSquare = false
    private val minCornerWalls = 4
    private val minEdgeWalls = 4
    private val minTotalWalls = 10
    private val maxTotalWalls = 20

    /**
     * Constructor that allows using the same random number generator
     */
    /**
     * Constructor for GameLogic with specified board dimensions and difficulty level
     * 
     * @param boardWidth Width of the game board
     * @param boardHeight Height of the game board
     * @param difficultyLevel Difficulty level for the game
     */
    init {
        this.currentLevel = difficultyLevel


        // Initialize square position based on current board size
        carrePosX = (boardWidth / 2) - 1
        carrePosY = (boardHeight / 2) - 1

        // Calculate walls per quadrant based on board width
        // Ensure at least 1 wall per quadrant, but not more than board width / 4
        wallsPerQuadrant = max(1, boardWidth / 4) // Default: quarter of board width
        companionLog.d(
            "[GAME LOGIC] Board size: %dx%d, walls per quadrant: %d",
            boardWidth, boardHeight, wallsPerQuadrant
        )

        // Apply difficulty settings
        applyDifficultySettings(difficultyLevel)
    }

    /**
     * Apply difficulty settings based on the level
     */
    private fun applyDifficultySettings(level: Int) {
        // Default: targets must be in corners with two walls
        targetMustBeInCorner = true


        // Store the current level for use in other methods
        currentLevel = level


        // Use configurable preference for multicolor target, or fall back to difficulty-based setting
        allowMulticolorTarget = Preferences.allowMulticolorTarget

        companionLog.d(
            "[DIFFICULTY] Setting difficulty level %d (BEGINNER=%d, ADVANCED=%d, INSANE=%d, IMPOSSIBLE=%d)",
            level,
            DIFFICULTY_BEGINNER,
            DIFFICULTY_ADVANCED,
            DIFFICULTY_INSANE,
            DIFFICULTY_IMPOSSIBLE
        )

        if (level == DIFFICULTY_BEGINNER) {
            // For beginner level - targets must be in corners
            targetMustBeInCorner = true
            companionLog.d("[DIFFICULTY] Using BEGINNER settings (targets in corners only)")
        } else if (level == DIFFICULTY_ADVANCED) {
            // For Advanced difficulty, targets can be in random positions
            targetMustBeInCorner = false
            companionLog.d("[DIFFICULTY] Using ADVANCED settings with mixed target placement")

            maxWallsInOneVerticalCol = 3
            maxWallsInOneHorizontalRow = 3
            wallsPerQuadrant = (boardWidth / 3.3).toInt()

            loneWallsAllowed = true
        } else {
            // Keep targetMustBeInCorner = true
            companionLog.d("[DIFFICULTY] Using INSANE or IMPOSSIBLE settings")

            loneWallsAllowed = true


            // For Insane and Impossible difficulties, targets can appear anywhere except the center
            targetMustBeInCorner = false
            companionLog.d("[DIFFICULTY] Using INSANE/IMPOSSIBLE settings, targets fully random")

            maxWallsInOneVerticalCol = 5
            maxWallsInOneHorizontalRow = 5
            wallsPerQuadrant = boardWidth / 3
        }

        if (level == DIFFICULTY_IMPOSSIBLE) {
            companionLog.d("[DIFFICULTY] Using IMPOSSIBLE settings")
            wallsPerQuadrant = (boardWidth / 2.3).toInt()
        }

        if (boardWidth * boardHeight > 64) {
            // calculate maxWallsInOneVerticalCol and maxWallsInOneHorizontalRow based on board size
        }

        companionLog.d(
            "[DIFFICULTY] Final settings: targetMustBeInCorner=%b, allowMulticolorTarget=%b, maxWallsInOneVerticalCol=%d, maxWallsInOneHorizontalRow=%d, wallsPerQuadrant=%d, boardSize=%dx%d",
            targetMustBeInCorner,
            allowMulticolorTarget,
            maxWallsInOneVerticalCol,
            maxWallsInOneHorizontalRow,
            wallsPerQuadrant,
            boardWidth,
            boardHeight
        )
    }

    /**
     * Get a random number between min and max (inclusive)
     * Adds safety checks to ensure min <= max
     */
    fun getRandom(min: Int, max: Int): Int {
        // Add safety check to prevent IllegalArgumentException
        if (min > max) {
            companionLog.w(
                "[GAME LOGIC] Invalid random range: min(%d) > max(%d). returning max(%d).",
                min,
                max,
                max
            )
            return max
        }

        return rand.nextInt((max - min) + 1) + min
    }

    /**
     * Removes game elements (robots and targets) from a map
     */
    fun removeGameElementsFromMap(data: ArrayList<GridElement>): ArrayList<GridElement> {
        val gameElementTypes = arrayOf<String?>(
            "robot_green",
            "robot_yellow",
            "robot_red",
            "robot_blue",  // robots
            "target_green",
            "target_yellow",
            "target_red",
            "target_blue",
            "target_multi" // targets (cible)
        )
        val iterator = data.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (gameElementTypes.contains(e.type)) {
                iterator.remove()
            }
        }
        return data
    }

    /**
     * Convert wall arrays to a list of GridElements
     */
    fun translateArraysToMap(
        horizontalWalls: Array<IntArray?>,
        verticalWalls: Array<IntArray?>
    ): ArrayList<GridElement> {
        var data = ArrayList<GridElement>()

        for (x in 0..boardWidth) {
            for (y in 0..boardHeight) {
                if (horizontalWalls[x]!![y] == 1) {
                    data.add(GridElement(x, y, "mh"))
                }
                if (verticalWalls[x]!![y] == 1) {
                    data.add(GridElement(x, y, "mv"))
                }
            }
        }

        // Ensure all outer walls exist in the grid data
        data = ensureOuterWalls(data)
        companionLog.d(
            "[WALL STORAGE] translateArraysToMap - %d GridElements after ensuring outer walls",
            data.size
        )

        return data
    }

    /**
     * Ensure all outer walls exist in the map data
     * This is critical for consistent wall behavior when walls are preserved
     */
    fun ensureOuterWalls(data: ArrayList<GridElement>): ArrayList<GridElement> {
        companionLog.d("[WALL STORAGE] ensureOuterWalls called for board %dx%d", boardWidth, boardHeight)
        val newData = ArrayList<GridElement>(data)
        // Check each outer wall position and add if missing
        val horizontalTopExists = BooleanArray(boardWidth)
        val horizontalBottomExists = BooleanArray(boardWidth)
        val verticalLeftExists = BooleanArray(boardHeight)
        val verticalRightExists = BooleanArray(boardHeight)

        // First pass: check which outer walls already exist
        for (element in data) {
            if (element.type == "mh") {
                // Horizontal walls
                if (element.y == 0) {
                    horizontalTopExists[element.x] = true
                    companionLog.d("[WALL STORAGE] Horizontal top wall found at (%d,0)", element.x)
                } else if (element.y == boardHeight) {
                    horizontalBottomExists[element.x] = true
                    companionLog.d(
                        "[WALL STORAGE] Horizontal bottom wall found at (%d,%d)",
                        element.x,
                        boardHeight
                    )
                }
            } else if (element.type == "mv") {
                // Vertical walls
                if (element.x == 0) {
                    verticalLeftExists[element.y] = true
                    companionLog.d("[WALL STORAGE] Vertical left wall found at (0,%d)", element.y)
                } else if (element.x == boardWidth) {
                    verticalRightExists[element.y] = true
                    companionLog.d(
                        "[WALL STORAGE] Vertical right wall found at (%d,%d)",
                        boardWidth,
                        element.y
                    )
                }
            }
        }

        var missingWalls = 0

        // Add missing top walls
        for (x in 0..<boardWidth) {
            if (!horizontalTopExists[x]) {
                newData.add(GridElement(x, 0, "mh"))
                companionLog.d("[WALL STORAGE] missing top wall at (%d,0)", x)
                missingWalls++
            }
        }

        // Add missing bottom walls
        for (x in 0..<boardWidth) {
            if (!horizontalBottomExists[x]) {
                newData.add(GridElement(x, boardHeight, "mh"))
                companionLog.d("[WALL STORAGE] missing bottom wall at (%d,%d)", x, boardHeight)
                missingWalls++
            }
        }

        // Add missing left walls
        for (y in 0..<boardHeight) {
            if (!verticalLeftExists[y]) {
                newData.add(GridElement(0, y, "mv"))
                companionLog.d("[WALL STORAGE] missing left wall at (0,%d)", y)
                missingWalls++
            }
        }

        // Add missing right walls
        for (y in 0..<boardHeight) {
            if (!verticalRightExists[y]) {
                newData.add(GridElement(boardWidth, y, "mv"))
                companionLog.d("[WALL STORAGE] missing right wall at (%d,%d)", boardWidth, y)
                missingWalls++
            }
        }

        companionLog.d("[WALL STORAGE] %d missing outer walls", missingWalls)

        return newData
        // return data; // send back the original data
        // return newData; this would send back the new data with the missing walls, but i'd rather just show the error messages and find out, why they are missing
    }

    /**
     * Add game elements (robots and targets) to a map
     */
    fun addGameElementsToGameMap(
        data: ArrayList<GridElement>?,
        horizontalWalls: Array<IntArray?>?,
        verticalWalls: Array<IntArray?>?
    ): ArrayList<GridElement> {
        var horizontalWalls = horizontalWalls
        var verticalWalls = verticalWalls
        companionLog.d(
            "[TARGET PLACEMENT] INITIAL CHECK: currentLevel=%d, targetMustBeInCorner=%b (DIFF_INSANE=%d, DIFF_IMPOSSIBLE=%d)",
            currentLevel, targetMustBeInCorner, DIFFICULTY_INSANE, DIFFICULTY_IMPOSSIBLE
        )

        var abandon: Boolean

        companionLog.d(
            "[TARGET PLACEMENT] Starting target placement with difficulty=%d, targetMustBeInCorner=%b",
            currentLevel, targetMustBeInCorner
        )


        // TODO: find a way for the solver to also check for multicolor targets (nice-to-have)
        // Multi-target mode validation: multi-colored targets not allowed when targetColors > 1
        val isMultiTargetMode = (targetColors > 1)
        if (isMultiTargetMode && allowMulticolorTarget) {
            allowMulticolorTarget = false
            companionLog.w(
                "[TARGET_MULTI] Multi-colored target disabled: multi-target mode active (targetColors=%d)",
                targetColors
            )
        }


        // Use our color management methods to generate target and robot type strings
        val typesOfTargets: Array<String?>?
        if (allowMulticolorTarget && !isMultiTargetMode) {
            // Include multi-color target if allowed
            typesOfTargets =
                arrayOfNulls<String>(Constants.NUM_ROBOTS + 1) // standard targets + multi-colored target
            for (i in 0..<Constants.NUM_ROBOTS) {
                typesOfTargets[i] = getObjectType(i, false) // false = target
            }
            typesOfTargets[Constants.NUM_ROBOTS] =
                "target_multi" // Add multi-target at the last index
            companionLog.d("[TARGET_MULTI] Multi-color target INCLUDED in available targets")
        } else {
            // Exclude multi-color target if not allowed
            typesOfTargets = arrayOfNulls<String>(Constants.NUM_ROBOTS) // standard targets only
            for (i in 0..<Constants.NUM_ROBOTS) {
                typesOfTargets[i] = getObjectType(i, false) // false = target
            }
            companionLog.d("[TARGET_MULTI] Multi-color target EXCLUDED from available targets")
        }

        val typesOfRobots = arrayOfNulls<String>(Constants.NUM_ROBOTS)
        for (i in 0..<Constants.NUM_ROBOTS) {
            typesOfRobots[i] = getObjectType(i, true) // true = robot
        }
        // workaround for backward compatibility
        typesOfRobots[0] = "robot_red"
        typesOfTargets[0] = "target_red"

        // debug: set target to pink (red)
        // typesOfTargets = new String[1]; typesOfTargets[0] = "target_red";

        // Store all positions of game elements to avoid overlapping
        val allElements = ArrayList<GridElement>()


        // Create targets based on targetCount and targetColors settings
        // We'll create targets for each color (or multi-color) up to the targetColors limit
        val maxTargetTypes =
            typesOfTargets.size // Use the actual array length which already accounts for allowMulticolorTarget
        var targetTypesCount = min(targetColors, maxTargetTypes) // Limit to targetColors
        targetTypesCount = max(1, targetTypesCount) // Ensure at least one target is always created

        companionLog.d(
            "[TARGET GENERATION] targetColors=%d, maxTargetTypes=%d, targetTypesCount=%d",
            targetColors, maxTargetTypes, targetTypesCount
        )


        // Create an array of indices to use for target types, and shuffle it to randomize which colors are used
        val targetTypeIndices = IntArray(typesOfTargets.size)
        for (i in typesOfTargets.indices) {
            targetTypeIndices[i] = i
        }


        // Shuffle the array to randomize which colors are used when targetColors < NUM_ROBOTS
        shuffleIntArray(targetTypeIndices)


        // Only use the first targetTypesCount elements from the shuffled array
        companionLog.d(
            "[TARGET] Will use %d different target types out of %d possible types",
            targetTypesCount,
            maxTargetTypes
        )


        // If horizontalWalls and verticalWalls are null, create empty arrays
        if (horizontalWalls == null || verticalWalls == null) {
            companionLog.d("[WALL STORAGE] Creating empty wall arrays for target placement")
            horizontalWalls = Array<IntArray?>(boardWidth + 1) { IntArray(boardHeight + 1) }
            verticalWalls = Array<IntArray?>(boardWidth + 1) { IntArray(boardHeight + 1) }


            // Extract wall information from data if available
            if (data != null) {
                for (element in data) {
                    val type = element.type
                    val x = element.x
                    val y = element.y

                    if ("mh" == type && x < boardWidth && y < boardHeight) {
                        horizontalWalls[x]!![y] = 1
                    } else if ("mv" == type && x < boardWidth && y < boardHeight) {
                        verticalWalls[x]!![y] = 1
                    }
                }
            }
        }

        for (i in 0..<targetTypesCount) {
            val targetType = targetTypeIndices[i]


            // For each target type, create exactly one target
            var targetX: Int
            var targetY: Int
            var useCornerPlacement = targetMustBeInCorner


            // For ADVANCED difficulty with targetMustBeInCorner=false, we use a 50% probability
            // For INSANE/IMPOSSIBLE difficulty with targetMustBeInCorner=false, always random placement
            if (!targetMustBeInCorner && currentLevel == DIFFICULTY_ADVANCED) {
                // For Advanced difficulty, use 50% probability for corner placement
                val randomChoice = getRandom(0, 1)
                companionLog.d(
                    "[TARGET PLACEMENT] DECISION at LINE 385: randomChoice=%d for 50%% probability",
                    randomChoice
                )
                if (randomChoice == 0) {
                    useCornerPlacement = true
                    companionLog.d(
                        "[TARGET PLACEMENT] Target %d will use corner placement (50%% probability)",
                        i
                    )
                } else {
                    companionLog.d(
                        "[TARGET PLACEMENT] Target %d will use random placement (50%% probability)",
                        i
                    )
                }
            } else {
                companionLog.d(
                    "[TARGET PLACEMENT] TARGET=%d MODE=%s FINAL_CHECK: mustBeInCorner=%b, useCornerPlacement=%b, difficulty=%d",
                    i, if (useCornerPlacement) "corner-only" else "fully-random",
                    targetMustBeInCorner, useCornerPlacement, currentLevel
                )
            }

            do {
                abandon = false
                targetX = getRandom(0, boardWidth - 1)
                targetY = getRandom(0, boardHeight - 1)

                companionLog.d(
                    "[TARGET PLACEMENT] Generate position at LINE 384: position=(%d,%d), useCornerPlacement=%b",
                    targetX, targetY, useCornerPlacement
                )


                // Check corner walls if required
                if (useCornerPlacement) {
                    // For a corner, we need at least one horizontal wall AND at least one vertical wall
                    val hasHorizontalWall =
                        (horizontalWalls[targetX]!![targetY] == 1 || horizontalWalls[targetX]!![targetY + 1] == 1)
                    val hasVerticalWall =
                        (verticalWalls[targetX]!![targetY] == 1 || verticalWalls[targetX + 1]!![targetY] == 1)

                    companionLog.d(
                        "[TARGET PLACEMENT] CORNER CHECK at LINE 422: position=(%d,%d), hasHWall=%b, hasVWall=%b",
                        targetX, targetY, hasHorizontalWall, hasVerticalWall
                    )


                    // Debug wall values directly
                    companionLog.d(
                        "[TARGET PLACEMENT] WALL VALUES: h1=%d, h2=%d, v1=%d, v2=%d",
                        horizontalWalls[targetX]!![targetY],
                        horizontalWalls[targetX]!![targetY + 1],
                        verticalWalls[targetX]!![targetY],
                        verticalWalls[targetX + 1]!![targetY]
                    )


                    // We need both a horizontal and vertical wall to form a corner
                    if (!hasHorizontalWall || !hasVerticalWall) {
                        abandon = true
                        companionLog.d(
                            "[TARGET PLACEMENT] Position (%d,%d) abandoned - not in corner (h=%b, v=%b), LINE 395",
                            targetX, targetY, hasHorizontalWall, hasVerticalWall
                        )
                    } else {
                        companionLog.d(
                            "[TARGET PLACEMENT] Position (%d,%d) is a valid corner (h=%b, v=%b)",
                            targetX, targetY, hasHorizontalWall, hasVerticalWall
                        )
                    }
                } else {
                    // If we're NOT using corner placement, let's verify that corners are actually being allowed
                    companionLog.d(
                        "[TARGET PLACEMENT] Using random placement at LINE 436 - position=(%d,%d)",
                        targetX,
                        targetY
                    )
                }


                // Check if in the center square - always avoid the center square regardless of difficulty
                if ((targetX == carrePosX && targetY == carrePosY)
                    || (targetX == carrePosX && targetY == carrePosY + 1)
                    || (targetX == carrePosX + 1 && targetY == carrePosY)
                    || (targetX == carrePosX + 1 && targetY == carrePosY + 1)
                ) {
                    abandon = true
                    companionLog.d(
                        "[TARGET PLACEMENT] Position (%d,%d) abandoned - in center square",
                        targetX,
                        targetY
                    )
                }


                // Check if position is already occupied by another element
                for (element in allElements) {
                    if (element.x == targetX && element.y == targetY) {
                        abandon = true
                        companionLog.d(
                            "[TARGET PLACEMENT] Position (%d,%d) abandoned - already occupied",
                            targetX,
                            targetY
                        )
                        break
                    }
                }
            } while (abandon)


            // Create and add the target
            val newTarget = GridElement(targetX, targetY, typesOfTargets[targetType])
            data!!.add(newTarget)
            allElements.add(newTarget)

            companionLog.d(
                "[TARGET PLACEMENT] PLACEMENT_COMPLETE: target=%d at position=(%d,%d) of type=%s",
                i, targetX, targetY, typesOfTargets[targetType]
            )
        }


        // Create robots
        for (currentRobotType in typesOfRobots) {
            var cX: Int
            var cY: Int

            do {
                abandon = false
                cX = getRandom(0, boardWidth - 1)
                cY = getRandom(0, boardHeight - 1)


                // Check if position is already occupied
                for (element in allElements) {
                    if (element.x == cX && element.y == cY) {
                        abandon = true
                        break
                    }
                }


                // Check if in the center square - always avoid the center square regardless of difficulty
                if ((cX == carrePosX && cY == carrePosY)
                    || (cX == carrePosX && cY == carrePosY + 1)
                    || (cX == carrePosX + 1 && cY == carrePosY)
                    || (cX == carrePosX + 1 && cY == carrePosY + 1)
                ) abandon = true // robot was inside square
            } while (abandon)


            // Create and add the robot
            val newRobot = GridElement(cX, cY, currentRobotType)
            data!!.add(newRobot)
            allElements.add(newRobot)

            companionLog.d("Added robot %s at position %d,%d", currentRobotType, cX, cY)
        }

        return data!!
    }

    private fun shuffleIntArray(array: IntArray) {
        // Use Fisher-Yates algorithm to shuffle the array
        for (i in array.size - 1 downTo 1) {
            val index = rand.nextInt(i + 1)
            // Simple swap
            val temp = array[index]
            array[index] = array[i]
            array[i] = temp
        }
    }


    /**
     * Generate a new map with walls, robots, and targets
     */
    fun generateGameMap(existingMap: ArrayList<GridElement>?): ArrayList<GridElement>? {
        var existingMap = existingMap
        companionLog.d("[WALLS] Using generateNewMapEachTime: %s", Preferences.generateNewMapEachTime)


        // Check if we should preserve walls from the existing map
        val wallStorage = WallStorage.getInstance()
        val preserveWalls = !Preferences.generateNewMapEachTime && wallStorage.hasStoredWalls()
        companionLog.d(
            "[WALL STORAGE] GameLogic: generateNewMapEachTime: %s, Preserving walls: %s, hasStoredWalls: %s",
            Preferences.generateNewMapEachTime,
            preserveWalls,
            wallStorage.hasStoredWalls()
        )
        // If this is the first time generating a map or we're not preserving walls, generate everything new
        if (existingMap == null || existingMap.isEmpty() || Preferences.generateNewMapEachTime) {
            companionLog.d("[WALLS] Generating completely new map")


            // Generate a new map based on board size
            if (boardWidth <= 8 || boardHeight <= 8) {
                // For small boards, use the simplified generation algorithm
                existingMap = generateSimpleGameMap3(null)
            } else {
                // For larger boards, use the standard generation algorithm
                existingMap = generateStandardGameMap()
            }


            // Store the walls for future use if we're not generating new maps each time
            if (!Preferences.generateNewMapEachTime) {
                wallStorage.storeWalls(existingMap)
                companionLog.d("[WALLS][WALL STORAGE] Stored walls for future use")
            }

            return existingMap
        } else {
            // We have an existing map and should preserve walls
            var data: ArrayList<GridElement>?

            if (preserveWalls) {
                companionLog.d("[WALLS][WALL STORAGE] Preserving walls from stored configuration")
                // Remove game elements (robots and targets) but keep walls
                data = removeGameElementsFromMap(existingMap)


                // Apply stored walls to the map
                data = wallStorage.applyWallsToElements(data)
            } else {
                // Remove all game elements including walls
                data = ArrayList<GridElement>()


                // Generate a new map based on board size
                if (boardWidth <= 8 || boardHeight <= 8) {
                    // For small boards, use the simplified generation algorithm
                    data = generateSimpleGameMap3(null)
                } else {
                    // For larger boards, use the standard generation algorithm
                    data = generateStandardGameMap()
                }


                // Store the walls for future use
                if (!Preferences.generateNewMapEachTime) {
                    wallStorage.storeWalls(data)
                    companionLog.d("[WALLS][WALL STORAGE] Stored new walls for future use")
                }
            }


            // Add game elements (robots and targets) to the map
            data = addGameElementsToGameMap(data, null, null)

            return data
        }
    }

    /**
     * Generate a standard game map for normal-sized boards
     * This is extracted from the original generateGameMap method
     */
    private fun generateStandardGameMap(): ArrayList<GridElement> {
        val horizontalWalls = Array<IntArray?>(boardWidth + 1) { IntArray(boardHeight + 1) }
        val verticalWalls = Array<IntArray?>(boardWidth + 1) { IntArray(boardHeight + 1) }

        var temp = 0
        var countX = 0
        var countY = 0

        var restart: Boolean
        // Add a counter to limit the number of restarts
        var restartCount = 0
        val maxRestarts = 50 // Maximum number of times we'll restart before relaxing constraints
        val restartRelaxThreshold = 20 // Number of restarts before we start relaxing constraints

        // Original difficulty constraints
        val originalMaxWallsInOneHorizontalRow = maxWallsInOneHorizontalRow
        val originalMaxWallsInOneVerticalCol = maxWallsInOneVerticalCol
        var tolerance = 0
        do {
            restart = false


            // If we've had to restart multiple times, gradually relax the constraints
            if (restartCount > restartRelaxThreshold) {
                // this happens on small maps like 12x12
                // Add 1 to the allowed walls per row/column for each restart beyond 20
                tolerance = (restartCount - restartRelaxThreshold) / 5
                maxWallsInOneHorizontalRow = originalMaxWallsInOneHorizontalRow + tolerance
                maxWallsInOneVerticalCol = originalMaxWallsInOneVerticalCol + tolerance
                companionLog.d(
                    "Relaxing wall constraints after %d restarts: h=%d, v=%d",
                    restartCount, maxWallsInOneHorizontalRow, maxWallsInOneVerticalCol
                )
            }

            //We initialize with no walls
            for (x in 0..<boardWidth + 1) for (y in 0..<boardHeight + 1) {
                verticalWalls[x]!![y] = 0
                horizontalWalls[x]!![y] = verticalWalls[x]!![y]
            }

            //Creation of the borders
            for (x in 0..<boardWidth) {
                horizontalWalls[x]!![0] = 1 // Top border
                horizontalWalls[x]!![boardHeight] =
                    1 // Bottom border (important: use boardHeight to place at edge)
            }
            for (y in 0..<boardHeight) {
                verticalWalls[0]!![y] = 1 // Left border
                verticalWalls[boardWidth]!![y] =
                    1 // Right border (important: use boardWidth to place at edge)
            }

            // right-angled Walls near the left border
            horizontalWalls[0]!![getRandom(2, 7)] = 1
            do {
                temp = getRandom(
                    boardHeight / 2,
                    boardHeight - 2
                ) // Adjust to ensure walls are visible
            } while (horizontalWalls[0]!![temp - 1] == 1 || horizontalWalls[0]!![temp] == 1 || horizontalWalls[0]!![temp + 1] == 1)
            horizontalWalls[0]!![temp] = 1

            // right-angled Walls near the right border
            horizontalWalls[boardWidth - 1]!![getRandom(2, 7)] =
                1 // Position one cell to the left of border
            do {
                temp = getRandom(
                    boardHeight / 2,
                    boardHeight - 2
                ) // Adjust to ensure walls are visible
            } while (horizontalWalls[boardWidth - 1]!![temp - 1] == 1 || horizontalWalls[boardWidth - 1]!![temp] == 1 || horizontalWalls[boardWidth - 1]!![temp + 1] == 1)
            horizontalWalls[boardWidth - 1]!![temp] = 1

            // right-angled Walls near the top border
            verticalWalls[getRandom(2, boardWidth / 2 - 1)]!![0] = 1
            do {
                temp =
                    getRandom(boardWidth / 2, boardWidth - 2) // Adjust to ensure walls are visible
            } while (verticalWalls[temp - 1]!![0] == 1 || verticalWalls[temp]!![0] == 1 || verticalWalls[temp + 1]!![0] == 1)
            verticalWalls[temp]!![0] = 1

            // right-angled Walls near the bottom border
            verticalWalls[getRandom(2, boardWidth / 2 - 1)]!![boardHeight - 1] =
                1 // Position one cell above the border
            do {
                temp = getRandom(8, boardWidth - 2) // Adjust to ensure walls are visible
            } while (verticalWalls[temp - 1]!![boardHeight - 1] == 1 || verticalWalls[temp]!![boardHeight - 1] == 1 || verticalWalls[temp + 1]!![boardHeight - 1] == 1)
            verticalWalls[temp]!![boardHeight - 1] = 1

            //Drawing the middle square (carré)
            horizontalWalls[carrePosX + 1]!![carrePosY] = 1
            horizontalWalls[carrePosX]!![carrePosY] = horizontalWalls[carrePosX + 1]!![carrePosY]
            horizontalWalls[carrePosX + 1]!![carrePosY + 2] = 1
            horizontalWalls[carrePosX]!![carrePosY + 2] =
                horizontalWalls[carrePosX + 1]!![carrePosY + 2]
            verticalWalls[carrePosX]!![carrePosY + 1] = 1
            verticalWalls[carrePosX]!![carrePosY] = verticalWalls[carrePosX]!![carrePosY + 1]
            verticalWalls[carrePosX + 2]!![carrePosY + 1] = 1
            verticalWalls[carrePosX + 2]!![carrePosY] =
                verticalWalls[carrePosX + 2]!![carrePosY + 1]

            // Loop to place walls in each quadrant of the board
            // The board is divided into 4 quadrants, and we try to place an equal number of walls in each
            // Each wall consists of two parts placed at right angles to form an L-shape
            for (k in 0..<wallsPerQuadrant * 4 + boardWidth / 2) {
                var abandon = false
                var tempX: Int
                var tempY: Int
                var tempXv = 0
                var tempYv = 0

                var compteLoop1: Long = 0
                do {
                    compteLoop1++
                    abandon = false

                    //Choice of random coordinates in each quadrant of the game board
                    if (k < wallsPerQuadrant) {
                        // top-left quadrant
                        tempX = getRandom(1, boardWidth / 2 - 1)
                        tempY = getRandom(1, boardHeight / 2 - 1)
                    } else if (k < wallsPerQuadrant * 2) {
                        // top-right quadrant
                        tempX = getRandom(
                            boardWidth / 2,
                            boardWidth - 2
                        ) // Use boardWidth-2 to stay visible
                        tempY = getRandom(1, boardHeight / 2 - 1)
                    } else if (k < wallsPerQuadrant * 3) {
                        // bottom-left quadrant
                        tempX = getRandom(1, boardWidth / 2 - 1)
                        tempY = getRandom(
                            boardHeight / 2,
                            boardHeight - 2
                        ) // Use boardHeight-2 to stay visible
                    } else if (k < wallsPerQuadrant * 4) {
                        // bottom-right quadrant
                        tempX = getRandom(
                            boardWidth / 2,
                            boardWidth - 2
                        ) // Use boardWidth-2 to stay visible
                        tempY = getRandom(
                            boardHeight / 2,
                            boardHeight - 2
                        ) // Use boardHeight-2 to stay visible
                    } else {
                        // bonus walls
                        tempX = getRandom(1, boardWidth - 2) // Use boardWidth-2 to stay visible
                        tempY = getRandom(1, boardHeight - 2) // Use boardHeight-2 to stay visible
                    }

                    if (horizontalWalls[tempX]!![tempY] == 1 // already chosen
                        || horizontalWalls[tempX - 1]!![tempY] == 1 // left
                        || horizontalWalls[tempX + 1]!![tempY] == 1 // right
                        || horizontalWalls[tempX]!![tempY - 1] == 1 // directly above
                        || horizontalWalls[tempX]!![tempY + 1] == 1 // directly below
                    ) abandon = true

                    if (verticalWalls[tempX]!![tempY] == 1 // already chosen
                        || verticalWalls[tempX + 1]!![tempY] == 1 // left
                        || verticalWalls[tempX]!![tempY - 1] == 1 // above
                        || verticalWalls[tempX + 1]!![tempY - 1] == 1 // diagonal right-above
                    ) abandon = true

                    if (!abandon) {
                        //We count the number of walls in the same row/column
                        countY = 0
                        countX = countY

                        for (x in 1..<boardWidth) {
                            if (horizontalWalls[x]!![tempY] == 1) countX++
                        }

                        for (y in 1..<boardHeight) {
                            if (horizontalWalls[tempX]!![y] == 1) countY++
                        }

                        if (tempY == carrePosY || tempY == carrePosY + 2) {
                            countX -= 2
                        }
                        if (countX >= maxWallsInOneHorizontalRow || countY >= maxWallsInOneVerticalCol) {
                            // companionLog.d("[GAME LOGIC] There are too many walls in the same row/column, we abandon");
                            abandon = true
                        }
                    }

                    if (!abandon) {
                        //Choice of the 2nd wall of the corner being drawn
                        tempXv = tempX + getRandom(0, 1)
                        tempYv = tempY - getRandom(0, 1)

                        //We check that it does not fall on or near existing walls
                        if (verticalWalls[tempXv]!![tempYv] == 1 || verticalWalls[tempXv - 1]!![tempYv] == 1 || verticalWalls[tempXv + 1]!![tempYv] == 1) abandon =
                            true
                        if (verticalWalls[tempXv]!![tempYv - 1] == 1 || verticalWalls[tempXv]!![tempYv + 1] == 1) abandon =
                            true

                        if (horizontalWalls[tempXv]!![tempYv] == 1 || horizontalWalls[tempXv - 1]!![tempYv] == 1) abandon =
                            true

                        if (horizontalWalls[tempXv]!![tempYv - 1] == 1 || horizontalWalls[tempXv - 1]!![tempYv - 1] == 1) abandon =
                            true

                        if (verticalWalls[tempXv - 1]!![tempYv - 1] == 1 || verticalWalls[tempXv - 1]!![tempYv + 1] == 1) abandon =
                            true

                        if (verticalWalls[tempXv + 1]!![tempYv + 1] == 1 || verticalWalls[tempXv + 1]!![tempYv - 1] == 1) abandon =
                            true

                        if (!abandon) {
                            //We count the number of walls in the same row/column
                            countY = 0
                            countX = countY

                            for (x in 1..<boardWidth) {
                                if (verticalWalls[x]!![tempYv] == 1) countX++
                            }

                            for (y in 1..<boardHeight) {
                                if (verticalWalls[tempXv]!![y] == 1) countY++
                            }

                            if (tempXv == carrePosX || tempXv == carrePosX + 2) {
                                countY -= 2
                            }
                            if (countX >= maxWallsInOneHorizontalRow || countY >= maxWallsInOneVerticalCol)  //If there are too many walls in the same row/column, we abandon
                                abandon = true
                        }
                    }

                    if (compteLoop1 > 1000) {
                        companionLog.d(
                            "Wall creation restarted, too many loops (%d), tolerance: %d",
                            restartCount,
                            tolerance
                        )
                        restart = true
                    }
                } while (abandon && !restart)
                var skiponewall = false
                if (loneWallsAllowed && getRandom(0, 4) == 1) {
                    skiponewall = true
                }
                if (skiponewall) {
                    if (getRandom(0, 1) == 1) {
                        horizontalWalls[tempX]!![tempY] = 1
                    } else {
                        verticalWalls[tempXv]!![tempYv] = 1
                    }
                } else {
                    horizontalWalls[tempX]!![tempY] = 1
                    verticalWalls[tempXv]!![tempYv] = 1
                }
            }
        } while (restart && restartCount++ < maxRestarts)

        // Convert wall arrays to grid elements
        var data = translateArraysToMap(horizontalWalls, verticalWalls)


        // Add game elements (robots and targets)
        data = addGameElementsToGameMap(data, horizontalWalls, verticalWalls)

        return data
    }


    /**
     * Generate a simplified game map for small boards (8x8) (version 3)
     */
    private fun generateSimpleGameMap3(existingMap: ArrayList<GridElement>?): ArrayList<GridElement> {
        // Create a simple wall pattern for small boards
        val horizontalWalls = Array<IntArray?>(boardWidth + 1) { IntArray(boardHeight + 1) }
        val verticalWalls = Array<IntArray?>(boardWidth + 1) { IntArray(boardHeight + 1) }

        // Clear the existing map data to start fresh
        val data = ArrayList<GridElement?>()
        if (existingMap != null) {
            data.addAll(removeGameElementsFromMap(existingMap))
        }

        // IMPORTANT: Always explicitly set border walls - don't rely on automatic addition
        // This ensures consistent behavior especially with saved walls
        companionLog.d(
            "[WALL STORAGE] Explicitly setting ALL outer border walls for board %dx%d",
            boardWidth,
            boardHeight
        )


        // Set horizontal border walls (top and bottom)
        for (x in 0..<boardWidth) {
            horizontalWalls[x]!![0] = 1 // Top border
            horizontalWalls[x]!![boardHeight] = 1 // Bottom border
            companionLog.d(
                "[WALL STORAGE] Setting horizontal border walls at (%d,0) and (%d,%d)",
                x,
                x,
                boardHeight
            )
        }


        // Set vertical border walls (left and right)
        for (y in 0..<boardHeight) {
            verticalWalls[0]!![y] = 1 // Left border
            verticalWalls[boardWidth]!![y] = 1 // Right border
            companionLog.d(
                "[WALL STORAGE] Setting vertical border walls at (0,%d) and (%d,%d)",
                y,
                boardWidth,
                y
            )
        }

        // Create a center square similar to the original game but simpler
        val centerX = boardWidth / 2 - 1
        val centerY = boardHeight / 2 - 1
        horizontalWalls[centerX + 1]!![centerY] = 1
        horizontalWalls[centerX]!![centerY] = horizontalWalls[centerX + 1]!![centerY]
        horizontalWalls[centerX + 1]!![centerY + 2] = 1
        horizontalWalls[centerX]!![centerY + 2] = horizontalWalls[centerX + 1]!![centerY + 2]
        verticalWalls[centerX]!![centerY + 1] = 1
        verticalWalls[centerX]!![centerY] = verticalWalls[centerX]!![centerY + 1]
        verticalWalls[centerX + 2]!![centerY + 1] = 1
        verticalWalls[centerX + 2]!![centerY] = verticalWalls[centerX + 2]!![centerY + 1]

        // CRITICAL: Add four right-angled walls at the outer borders - one on each side
        // These form the characteristic right angles with the border walls

        // Calculate random positions at least 2 squares from corners
        // Top edge (row 0) - vertical wall
        verticalWalls[getRandom(2, boardWidth - 2)]!![0] = 1
        // Right edge (col boardWidth-1) - horizontal wall
        horizontalWalls[boardWidth - 1]!![getRandom(2, boardHeight - 2)] = 1
        // Bottom edge (row boardHeight-1) - vertical wall
        verticalWalls[getRandom(2, boardWidth - 2)]!![boardHeight - 1] = 1
        // Left edge (col 0) - horizontal wall
        horizontalWalls[0]!![getRandom(2, boardHeight - 2)] = 1


        // Determine the number of additional walls based on difficulty (10-20 walls total)
        // We already placed 8 walls for the center square, so add between 2-12 more
        val baseWallCount = 4 // Base number of additional walls
        val difficultyBonus = currentLevel * 3 // Difficulty adds 0, 3, 6, or 9 walls
        val randomBonus = getRandom(0, 3) // Add 0-3 random walls regardless of difficulty

        val additionalWalls = baseWallCount + difficultyBonus + randomBonus
        var wallsToPlace = min(additionalWalls, maxTotalWalls - 8)
        wallsToPlace = max(wallsToPlace, minTotalWalls - 8) // Ensure minimum walls

        companionLog.d(
            "[GAME LOGIC] Adding %d additional walls (total: %d) for difficulty level %d",
            wallsToPlace, wallsToPlace + 8, currentLevel
        )

        // 1. First place corner walls (where walls touch at corners)
        var cornerWallsPlaced = 0
        if (placeWallsInCorners) {
            // Define corner wall pairs (horizontal wall, connecting vertical wall)
            val cornerPositions = arrayOf<Array<IntArray>?>( // Top-left corner walls
                arrayOf<IntArray>(intArrayOf(1, 1), intArrayOf(1, 1)),
                arrayOf<IntArray>(intArrayOf(2, 1), intArrayOf(2, 1)),  // Top-right corner walls
                arrayOf<IntArray>(intArrayOf(5, 1), intArrayOf(6, 1)),
                arrayOf<IntArray>(intArrayOf(6, 1), intArrayOf(6, 1)),  // Bottom-left corner walls
                arrayOf<IntArray>(intArrayOf(1, 6), intArrayOf(1, 5)),
                arrayOf<IntArray>(intArrayOf(2, 6), intArrayOf(2, 5)),  // Bottom-right corner walls
                arrayOf<IntArray>(intArrayOf(5, 6), intArrayOf(6, 5)),
                arrayOf<IntArray>(intArrayOf(6, 6), intArrayOf(6, 5))
            )


            // Shuffle corner positions
            shuffle3DArray(cornerPositions)


            // Place corner walls until we meet the minimum
            var i = 0
            while (i < cornerPositions.size && cornerWallsPlaced < minCornerWalls) {
                val hPos = cornerPositions[i]!![0]
                val vPos = cornerPositions[i]!![1]


                // Only place if positions are empty
                if (horizontalWalls[hPos[0]]!![hPos[1]] == 0 && verticalWalls[vPos[0]]!![vPos[1]] == 0) {
                    horizontalWalls[hPos[0]]!![hPos[1]] = 1
                    verticalWalls[vPos[0]]!![vPos[1]] = 1
                    cornerWallsPlaced += 2 // We placed two walls
                    wallsToPlace -= 2
                    companionLog.d(
                        "[GAME LOGIC] Placed corner walls at H(%d,%d) and V(%d,%d)",
                        hPos[0], hPos[1], vPos[0], vPos[1]
                    )
                }
                i++
            }
        }


        // 2. Next place edge walls
        var edgeWallsPlaced = 0
        if (placeWallsOnEdges && wallsToPlace > 0) {
            // Define edge wall positions (alternating horizontal and vertical)
            val edgePositions = arrayOf<IntArray>( // Top edge (horizontal walls)
                intArrayOf(1, 0),
                intArrayOf(2, 0),
                intArrayOf(3, 0),
                intArrayOf(4, 0),
                intArrayOf(5, 0),
                intArrayOf(6, 0),  // Bottom edge (horizontal walls)
                intArrayOf(1, boardHeight - 1),
                intArrayOf(2, boardHeight - 1),
                intArrayOf(3, boardHeight - 1),
                intArrayOf(4, boardHeight - 1),
                intArrayOf(5, boardHeight - 1),
                intArrayOf(6, boardHeight - 1),  // Left edge (vertical walls)
                intArrayOf(0, 1),
                intArrayOf(0, 2),
                intArrayOf(0, 3),
                intArrayOf(0, 4),
                intArrayOf(0, 5),
                intArrayOf(0, 6),  // Right edge (vertical walls)
                intArrayOf(boardWidth - 1, 1),
                intArrayOf(boardWidth - 1, 2),
                intArrayOf(boardWidth - 1, 3),
                intArrayOf(boardWidth - 1, 4),
                intArrayOf(boardWidth - 1, 5),
                intArrayOf(boardWidth - 1, 6)
            )

            val isVertical = booleanArrayOf(
                false, false, false, false, false, false,
                false, false, false, false, false, false,
                true, true, true, true, true, true,
                true, true, true, true, true, true
            )


            // Shuffle edge positions (keeping track of which are vertical)
            shuffleArrayWithFlags(edgePositions, isVertical)


            // Place edge walls until we meet the minimum
            var i = 0
            while (i < edgePositions.size && edgeWallsPlaced < minEdgeWalls && wallsToPlace > 0) {
                val pos = edgePositions[i]
                val vertical = isVertical[i]


                // Ensure the positions are within bounds
                if (pos[0] >= 0 && pos[0] < boardWidth + 1 && pos[1] >= 0 && pos[1] < boardHeight + 1) {
                    // Only place if position is empty
                    if ((vertical && verticalWalls[pos[0]]!![pos[1]] == 0) ||
                        (!vertical && horizontalWalls[pos[0]]!![pos[1]] == 0)
                    ) {
                        if (vertical) {
                            verticalWalls[pos[0]]!![pos[1]] = 1
                        } else {
                            horizontalWalls[pos[0]]!![pos[1]] = 1
                        }

                        edgeWallsPlaced++
                        wallsToPlace--
                        companionLog.d(
                            "[GAME LOGIC] Placed %s edge wall at (%d,%d)",
                            if (vertical) "vertical" else "horizontal", pos[0], pos[1]
                        )
                    }
                }
                i++
            }
        }


        // 3. Finally, place any remaining walls in non-corner, non-edge, non-center positions
        if (wallsToPlace > 0) {
            // Define potential wall positions that won't block the game
            // Avoid the center square (carré) which is at positions (3,3), (3,4), (4,3), (4,4)
            val potentialHorizontalWalls = arrayOf<IntArray?>(
                intArrayOf(1, 2),
                intArrayOf(2, 2),
                intArrayOf(5, 2),
                intArrayOf(6, 2),  // Top area positions
                intArrayOf(1, 5),
                intArrayOf(2, 5),
                intArrayOf(5, 5),
                intArrayOf(6, 5),  // Bottom area positions
                intArrayOf(2, 1),
                intArrayOf(2, 6),
                intArrayOf(5, 1),
                intArrayOf(5, 6),  // Side area positions
                intArrayOf(3, 1),
                intArrayOf(4, 1),
                intArrayOf(3, 6),
                intArrayOf(4, 6),  // Mid-edge positions
                intArrayOf(1, 3),
                intArrayOf(1, 4),
                intArrayOf(6, 3),
                intArrayOf(6, 4),  // More side positions
                intArrayOf(2, 3),
                intArrayOf(2, 4),
                intArrayOf(5, 3),
                intArrayOf(5, 4) // Near center positions
            )

            val potentialVerticalWalls = arrayOf<IntArray?>(
                intArrayOf(1, 2),
                intArrayOf(1, 5),
                intArrayOf(6, 2),
                intArrayOf(6, 5),  // Corner area positions
                intArrayOf(2, 1),
                intArrayOf(2, 6),
                intArrayOf(5, 1),
                intArrayOf(5, 6),  // Edge-adjacent positions
                intArrayOf(1, 3),
                intArrayOf(1, 4),
                intArrayOf(6, 3),
                intArrayOf(6, 4),  // More side positions
                intArrayOf(2, 2),
                intArrayOf(2, 5),
                intArrayOf(5, 2),
                intArrayOf(5, 5),  // Internal corner positions
                intArrayOf(2, 3),
                intArrayOf(2, 4),
                intArrayOf(5, 3),
                intArrayOf(5, 4) // Near center positions
            )

            // Shuffle both arrays
            shuffleArray(potentialHorizontalWalls)
            shuffleArray(potentialVerticalWalls)


            // Place walls alternating between horizontal and vertical
            var additionalWallsPlaced = 0
            val maxAttempts = potentialHorizontalWalls.size + potentialVerticalWalls.size

            var i = 0
            while (i < maxAttempts && additionalWallsPlaced < wallsToPlace) {
                if (i % 2 == 0 && i / 2 < potentialHorizontalWalls.size) {
                    // Place horizontal wall
                    val x = potentialHorizontalWalls[i / 2]!![0]
                    val y = potentialHorizontalWalls[i / 2]!![1]


                    // Skip if in center square and we don't want walls there
                    if (!placeWallsInMiddleSquare && isCenterSquare(x, y)) {
                        i++
                        continue
                    }


                    // Ensure the indices are valid for our array size
                    if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                        // Only place if position is empty
                        if (horizontalWalls[x]!![y] == 0) {
                            horizontalWalls[x]!![y] = 1
                            additionalWallsPlaced++
                            companionLog.d("[GAME LOGIC] Placed horizontal wall at %d,%d", x, y)
                        }
                    }
                } else if (i % 2 == 1 && (i - 1) / 2 < potentialVerticalWalls.size) {
                    // Place vertical wall
                    val x = potentialVerticalWalls[(i - 1) / 2]!![0]
                    val y = potentialVerticalWalls[(i - 1) / 2]!![1]


                    // Skip if in center square and we don't want walls there
                    if (!placeWallsInMiddleSquare && isCenterSquare(x, y)) {
                        i++
                        continue
                    }


                    // Ensure the indices are valid for our array size
                    if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                        // Only place if position is empty
                        if (verticalWalls[x]!![y] == 0) {
                            verticalWalls[x]!![y] = 1
                            additionalWallsPlaced++
                            companionLog.d("[GAME LOGIC] Placed vertical wall at %d,%d", x, y)
                        }
                    }
                }
                i++
            }

            companionLog.d(
                "[GAME LOGIC] Placed %d additional walls beyond corners and edges",
                additionalWallsPlaced
            )
        }

        // Add game elements (robots and targets)
        var result = translateArraysToMap(horizontalWalls, verticalWalls)
        result = addGameElementsToGameMap(result, horizontalWalls, verticalWalls)

        return result
    }

    /**
     * Check if a position is within the center square (carré)
     */
    private fun isCenterSquare(x: Int, y: Int): Boolean {
        val centerX = boardWidth / 2 - 1
        val centerY = boardHeight / 2 - 1
        return (x >= centerX && x <= centerX + 2 && y >= centerY && y <= centerY + 2)
    }

    /**
     * Shuffle an array using Fisher-Yates algorithm
     */
    private fun shuffleArray(array: Array<IntArray?>) {
        for (i in array.size - 1 downTo 1) {
            val index = rand.nextInt(i + 1)
            // Simple swap
            val temp = array[index]
            array[index] = array[i]
            array[i] = temp
        }
    }

    /**
     * Shuffle an array along with a corresponding array of flags
     * Both arrays must be the same length
     */
    private fun shuffleArrayWithFlags(array: Array<IntArray>, flags: BooleanArray) {
        if (array.size != flags.size) {
            companionLog.e("[GAME LOGIC] Cannot shuffle arrays of different lengths")
            return
        }

        for (i in array.size - 1 downTo 1) {
            val index = rand.nextInt(i + 1)
            // Swap the position arrays
            val tempArray: IntArray? = array[index]
            array[index] = array[i]
            array[i] = tempArray!!


            // Swap the corresponding flags
            val tempFlag = flags[index]
            flags[index] = flags[i]
            flags[i] = tempFlag
        }
    }

    /**
     * Shuffle a 3D array using Fisher-Yates algorithm
     */
    private fun shuffle3DArray(array: Array<Array<IntArray>?>) {
        for (i in array.size - 1 downTo 1) {
            val index = rand.nextInt(i + 1)
            // Simple swap
            val temp = array[index]
            array[index] = array[i]
            array[i] = temp
        }
    }


    /**
     * Set the number of different target colors to be generated on the map
     * @param count Number of different target colors (1-4)
     */
    fun setTargetColors(count: Int) {
        // Ensure count is between 1 and 4
        this.targetColors = max(1, min(Constants.NUM_ROBOTS, count))
        companionLog.d("Target colors set to %d", this.targetColors)
    }

    /**
     * Get the current target colors setting
     * @return Number of different target colors (1-4)
     */
    fun getTargetColors(): Int {
        return targetColors
    }
}
