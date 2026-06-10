package roboyard.logic.core

import android.content.Context
import roboyard.logic.managers.GameStateManager
import roboyard.ui.util.MapIdGenerator
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Serializable
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

/**
 * Represents the state of a game, including the board, robots, targets, and game progress.
 * This class handles loading, saving, and manipulating the game state.
 */
class GameState(
    /**
     * Get board width
     */
    // Board properties
    @JvmField var width: Int,
    @JvmField var height: Int
) : Serializable {

    /**
     * Get map data for minimap generation (used by GameButtonGotoSavedGame)
     * This specifically addresses the getMapData method mentioned in the memory
     */
    val mapData: Array<IntArray?> // 0=empty, 1=wall, 2=target
    private val targetColors: Array<IntArray?> // Color index for targets, -1 if no target

    /**
     * Get all game elements (robots and targets)
     */
    // Game elements (robots and targets)
    @JvmField
    val gameElements: MutableList<GameElement>

    /**
     * Get the level ID
     */
    /**
     * Set the level ID
     */
    // Game information
    @JvmField
    var levelId: Int
    /**
     * Get the level name
     */
    /**
     * Set the level name
     */
    @JvmField
    var levelName: String?
    private var startTime: Long
    /**
     * Get the move count
     */
    /**
     * Set the move count
     */
    @JvmField
    var moveCount: Int
    private var robotCount = 1 // Default to 1 robot per color
    /**
     * Get the saved solutions string from save file
     */
    /**
     * Set the saved solutions string (used when loading from save file)
     */
    @JvmField
    var savedSolutions: String? = null // Serialized solutions from save file
    private var targetColorsCount = Constants.NUM_ROBOTS // Default to 4 different target colors

    /**
     * Check if the game is completed
     * @return true if all targets have robots of matching colors on them
     */
    var isComplete: Boolean = false
        private set

    @Transient
    private var completionHandledThisSession = false

    /**
     * Get the maximum hint index used during this game session.
     * @return -1 if no hints used, 0+ for hint index
     */
    /**
     * Set the maximum hint index used during this game session.
     * @param maxHintUsed The max hint index
     */
    // Hint tracking - tracks max hint used during this game session
    // -1 = no hints, 0+ = hint index (0 = first hint revealing robot colors)
    @JvmField
    var maxHintUsedThisSession: Int = -1

    /**
     * Get the number of hints used in this game
     * @return The hint count
     */
    var hintCount: Int = 0 // Track the number of hints used in this game
        private set
    /**
     * Get the unique map ID for this game
     * @return The 5-letter unique map ID
     */
    /**
     * Set the unique map ID for this game
     * @param uniqueMapId The 5-letter unique map ID
     */
    @JvmField
    var uniqueMapId: String = "" // 5-letter unique ID for map identification

    /**
     * Get the last moved robot
     * @return The robot that was last moved, or null if no robot has been moved
     */
    // Tracking the last move for hint verification
    var lastMovedRobot: GameElement? = null
        /**
         * Set the last moved robot
         * @param robot The robot that was just moved
         */
        set(robot) {
            field = robot
            Timber.d(
                "[MOVE_TRACKING] Last moved robot set to color: %d",
                if (robot != null) robot.color else -1
            )
        }

    /**
     * Get the direction of the last move
     * @return The direction constant from ERRGameMove, or null if no move has been made
     */
    var lastMoveDirection: Int? = null
        /**
         * Set the direction of the last move
         * @param direction The direction constant from ERRGameMove
         */
        set(direction) {
            field = direction
            Timber.d(
                "[MOVE_TRACKING] Last move direction set to: %d",
                if (direction != null) direction else -1
            )
        }

    // Transient properties (not serialized)
    @Transient
    private var selectedRobot: GameElement? = null

    @Transient
    private var gameStateManager: GameStateManager? = null

    // Store initial robot positions for reset functionality
    @JvmField
    var initialRobotPositions: MutableMap<Int?, IntArray?>? = null

    /**
     * Get the predefined solution string from the level file
     * @return The solution string (e.g., "gE gN gE gS gW...") or null if not defined
     */
    /**
     * Set the predefined solution string
     */
    // Predefined solution from level file (for levels that are too complex to solve at runtime)
    @JvmField
    var predefinedSolution: String? = null
    /**
     * Get the predefined number of moves from the level file
     * @return The number of moves or 0 if not defined
     */
    /**
     * Set the predefined number of moves
     */
    @JvmField
    var predefinedNumMoves: Int = 0

    /**
     * Get the difficulty level of this game
     * @return The difficulty level (Constants.DIFFICULTY_*)
     */
    /**
     * Set the difficulty level of this game
     * @param difficulty The difficulty level (Constants.DIFFICULTY_*)
     */
    // Difficulty level when the game was created (for savegame restoration)
    @JvmField
    var difficulty: Int = Constants.DIFFICULTY_BEGINNER

    /**
     * Create a new game state with specified dimensions
     */
    init {
        this.mapData = Array<IntArray?>(height) { IntArray(width) }
        this.targetColors = Array<IntArray?>(height) { IntArray(width) }
        this.gameElements = java.util.ArrayList<GameElement>()
        this.levelId = -1
        this.levelName = "XXXXX"
        this.startTime = System.currentTimeMillis()
        this.moveCount = 0


        // Initialize target colors to -1 (no target)
        for (y in 0..<height) {
            for (x in 0..<width) {
                targetColors[y]!![x] = -1
            }
        }
    }

    /**
     * Get cell type at the specified coordinates
     */
    fun getCellType(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return Constants.TYPE_VERTICAL_WALL // Treat out-of-bounds as vertical walls
        }
        return this.mapData[y]!![x]
    }

    /**
     * Set cell type at the specified coordinates
     */
    fun setCellType(x: Int, y: Int, type: Int) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            this.mapData[y]!![x] = type
        }
    }

    /**
     * Get target color at the specified coordinates
     */
    fun getTargetColor(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return -1
        }
        return targetColors[y]!![x]
    }

    /**
     * Set target color at the specified coordinates
     */
    fun setTargetColor(x: Int, y: Int, color: Int) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            targetColors[y]!![x] = color
        }
    }

    /**
     * Add a horizontal wall at the specified coordinates
     */
    fun addHorizontalWall(x: Int, y: Int) {
        val wall = GameElement(GameElement.TYPE_HORIZONTAL_WALL, x, y)
        gameElements.add(wall)
        setCellType(x, y, Constants.TYPE_HORIZONTAL_WALL)
    }

    /**
     * Add a vertical wall at the specified coordinates
     */
    fun addVerticalWall(x: Int, y: Int) {
        val wall = GameElement(GameElement.TYPE_VERTICAL_WALL, x, y)
        gameElements.add(wall)
        setCellType(x, y, Constants.TYPE_VERTICAL_WALL)
    }

    /**
     * Add a target at the specified coordinates with the given color
     */
    fun addTarget(x: Int, y: Int, color: Int) {
        Timber.d("[TARGET LOADING] Adding target at (%d,%d) with color %d", x, y, color)
        val target = GameElement(GameElement.TYPE_TARGET, x, y)
        target.color = color
        gameElements.add(target)
        setCellType(x, y, Constants.TYPE_TARGET)
        setTargetColor(x, y, color)
        Timber.d(
            "[TARGET LOADING] Target added, current board state at (%d,%d): cellType=%d, targetColor=%d",
            x, y, getCellType(x, y), getTargetColor(x, y)
        )
    }

    /**
     * Add a robot at the specified coordinates
     */
    fun addRobot(x: Int, y: Int, color: Int) {
        val robot = GameElement(GameElement.TYPE_ROBOT, x, y)
        robot.color = color
        gameElements.add(robot)
    }

    /**
     * Get the robot at the specified coordinates
     */
    fun getRobotAt(x: Int, y: Int): GameElement? {
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_ROBOT && element.x == x && element.y == y) {
                return element
            }
        }
        return null
    }

    /**
     * Get the selected robot
     */
    fun getSelectedRobot(): GameElement? {
        return selectedRobot
    }

    /**
     * Set the selected robot
     */
    fun setSelectedRobot(robot: GameElement?) {
        // Deselect previous robot
        if (selectedRobot != null) {
            selectedRobot!!.isSelected = false
        }


        // Select new robot
        if (robot != null) {
            robot.isSelected = true
        }

        selectedRobot = robot
    }

    /**
     * Check if there is a vertical wall at the specified position
     * Vertical walls separate columns (they're placed between x and x+1)
     */
    private fun hasVerticalWall(x: Int, y: Int): Boolean {
        // Check vertical walls from the game elements
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_VERTICAL_WALL && element.x == x && element.y == y) {
                return true
            }
        }
        return false
    }

    /**
     * Check if there is a horizontal wall at the specified position
     * Horizontal walls separate rows (they're placed between y and y+1)
     */
    private fun hasHorizontalWall(x: Int, y: Int): Boolean {
        // Check horizontal walls from the game elements
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_HORIZONTAL_WALL && element.x == x && element.y == y) {
                return true
            }
        }
        return false
    }

    /**
     * Check if the game is complete (all robots on their target positions).
     * 
     * @return true if all robots are on their correct targets.
     */
    fun checkCompletion(): Boolean {
        // Use the areAllRobotsAtTargets method to check if all robots are on their targets
        if (areAllRobotsAtTargets()) {
            // Timber.d("[GOAL DEBUG] Game complete! All robots are on matching targets.");
            this.isComplete = true
            return true
        } else {
            return false
        }
    }

    /**
     * Check if all robots are on targets of their matching color
     * @return True if all robots are on matching targets, false otherwise
     */
    fun areAllRobotsAtTargets(): Boolean {
        // Get all robots in the game
        val robots = this.robots


        // Get all targets in the game
        val targets = this.targets


        // Count how many robots are at their matching targets
        var robotsAtTarget = 0


        // For each robot, check if it's on a target of its matching color
        for (robot in robots) {
            if (isRobotAtTarget(robot)) {
                // Count robots that are on their targets
                robotsAtTarget++
                Timber.d(
                    "[GOAL DEBUG] Robot %d is at target (%d,%d)",
                    robot.color,
                    robot.x,
                    robot.y
                )
            } else {
                Timber.d(
                    "[GOAL DEBUG] Robot %d is NOT at target (%d,%d)",
                    robot.color,
                    robot.x,
                    robot.y
                )
            }
        }


        // Calculate how many robots need to be at targets to win
        // If there are fewer targets than robotCount, we only need as many robots at targets as there are targets
        val requiredRobots = min(robotCount, targets.size)


        // Game is complete when the number of robots at targets matches the required count
        val allRobotsAtTargets = (robotsAtTarget >= requiredRobots)

        Timber.d(
            "[GOAL DEBUG] %d/%d robots at targets (required: %d, total targets: %d, robotCount: %d) -> Game complete: %b",
            robotsAtTarget,
            robots.size,
            requiredRobots,
            targets.size,
            robotCount,
            allRobotsAtTargets
        )

        return allRobotsAtTargets
    }

    /**
     * Check if this level has a predefined solution
     * @return true if a predefined solution exists
     */
    fun hasPredefinedSolution(): Boolean {
        return predefinedSolution != null && !predefinedSolution!!.isEmpty()
    }

    /**
     * Set the completed status
     * @param completed New completion status
     */
    fun setCompleted(completed: Boolean) {
        this.isComplete = completed
        if (!completed) {
            completionHandledThisSession = false
        }
    }

    fun hasCompletionBeenHandledThisSession(): Boolean {
        return completionHandledThisSession
    }

    fun markCompletionHandledThisSession() {
        completionHandledThisSession = true
    }

    /**
     * Record that a hint was viewed during this game session.
     * Only updates if the new hint index is higher than the current max.
     * @param hintIndex The hint index that was viewed (0 = first hint with robot colors)
     */
    fun recordHintUsed(hintIndex: Int) {
        if (hintIndex > maxHintUsedThisSession) {
            maxHintUsedThisSession = hintIndex
            Timber.d("[HINT_TRACKING] Recorded hint usage: maxHintUsed=%d", maxHintUsedThisSession)
        }
    }

    /**
     * Check if any hints were used during this game session.
     * @return true if hints were used
     */
    fun hasUsedHintsThisSession(): Boolean {
        return maxHintUsedThisSession >= 0
    }


    val gridElements: ArrayList<GridElement?>
        /**
         * Get a list of GridElements representing the current board state
         * This is used by the solver to find a solution
         * @return List of GridElements
         */
        get() {
            val elements = java.util.ArrayList<GridElement?>()


            // SSOT: Read ALL elements from gameElements, not from board[][]
            // This ensures the solver receives the same data as the game display

            // Track which robot colors we've already added
            val robotColorsAdded =
                BooleanArray(Constants.MAX_NUM_ROBOTS)


            // Convert all GameElements to GridElements
            for (element in gameElements) {
                var gridElementType: String? = null

                when (element.type) {
                    GameElement.TYPE_HORIZONTAL_WALL -> gridElementType = "mh"
                    GameElement.TYPE_VERTICAL_WALL -> gridElementType = "mv"
                    GameElement.TYPE_TARGET -> {
                        val targetColor = element.color
                        if (targetColor == Constants.COLOR_MULTI) {
                            gridElementType = "target_multi"
                            Timber.d(
                                "[SOLUTION_SOLVER_TARGET] Found multi-color target at (%d,%d) in gameElements",
                                element.x,
                                element.y
                            )
                        } else if (targetColor == 0) {
                            gridElementType = "target_red"
                        } else if (targetColor == 1) {
                            gridElementType = "target_green"
                        } else if (targetColor == 2) {
                            gridElementType = "target_blue"
                        } else if (targetColor == 3) {
                            gridElementType = "target_yellow"
                        } else if (targetColor == 4) {
                            gridElementType = "target_silver"
                        } else {
                            gridElementType = "target_red"
                            Timber.w(
                                "[SOLUTION_SOLVER_TARGET] Unknown target color: %d at (%d,%d), defaulting to red",
                                targetColor,
                                element.x,
                                element.y
                            )
                        }
                    }

                    GameElement.TYPE_ROBOT -> {
                        val robotColor = element.color
                        if (robotColor == 0) {
                            gridElementType = "robot_red"
                        } else if (robotColor == 1) {
                            gridElementType = "robot_green"
                        } else if (robotColor == 2) {
                            gridElementType = "robot_blue"
                        } else if (robotColor == 3) {
                            gridElementType = "robot_yellow"
                        } else if (robotColor == 4) {
                            gridElementType = "robot_silver"
                        } else {
                            gridElementType = "robot_red"
                        }
                        robotColorsAdded[robotColor] = true
                    }
                }

                if (gridElementType != null) {
                    elements.add(GridElement(element.x, element.y, gridElementType))
                }
            }


            // The solver requires exactly 4 robots - add placeholder robots for any missing colors
            // These will be placed in the corners where they won't interfere with gameplay
            val cornerX = intArrayOf(1, width - 2, 1, width - 2)
            val cornerY = intArrayOf(1, 1, height - 2, height - 2)
            var cornerIndex = 0

            for (color in 0..<Constants.NUM_ROBOTS) {
                if (!robotColorsAdded[color]) {
                    // Add a placeholder robot (the solver needs exactly 4 robots)
                    val gridElementType: String?
                    when (color) {
                        0 -> gridElementType = "robot_red"
                        1 -> gridElementType = "robot_green"
                        2 -> gridElementType = "robot_blue"
                        3 -> gridElementType = "robot_yellow"
                        4 -> gridElementType = "robot_silver"
                        else -> gridElementType = "robot_red"
                    }


                    // Find an unoccupied corner to place the placeholder robot
                    var rx = cornerX[cornerIndex]
                    var ry = cornerY[cornerIndex]
                    cornerIndex = (cornerIndex + 1) % 4


                    // Make sure the spot is empty (check gameElements, not board[][])
                    while (isPositionOccupied(rx, ry)) {
                        rx = (rx + 1) % (width - 2) + 1 // Keep within bounds, avoiding edges
                        ry = (ry + 1) % (height - 2) + 1
                    }


                    // Add the placeholder robot to GridElements but not to gameElements
                    // This ensures the solver works but doesn't affect gameplay
                    elements.add(GridElement(rx, ry, gridElementType))
                }
            }

            return elements
        }

    /**
     * Check if a position is occupied by any game element (SSOT: uses gameElements only)
     */
    private fun isPositionOccupied(x: Int, y: Int): Boolean {
        for (element in gameElements) {
            if (element.x == x && element.y == y) {
                // Position is occupied by a wall, target, or robot
                if (element.type == GameElement.TYPE_TARGET ||
                    element.type == GameElement.TYPE_ROBOT
                ) {
                    return true
                }
            }
        }
        return false
    }


    /**
     * Serialize the game state to a string representation for saving to a file
     * @return String representation of the game state
     */
    fun serialize(): String {
        // Synchronize targets from gameElements to board array
        val syncedTargets = synchronizeTargets()
        if (syncedTargets > 0) {
            Timber.d("[SAVE_DATA] Synchronized %d targets before serialization", syncedTargets)
        }

        val sb = StringBuilder()


        // Generate the metadata section
        sb.append("#MAPNAME:").append(levelName)
            .append(";TIME:").append(System.currentTimeMillis() - startTime)
            .append(";MOVES:").append(moveCount)

        if (!uniqueMapId.isEmpty()) {
            sb.append(";UNIQUE_MAP_ID:").append(uniqueMapId)
        }

        sb.append("\n")


        // Add board dimensions
        sb.append("WIDTH:").append(width).append(";\n")
        sb.append("HEIGHT:").append(height).append(";\n")


        // Generate the board representation (walls excluded - they go in WALLS section)
        for (y in 0..<height) {
            for (x in 0..<width) {
                if (x > 0) {
                    sb.append(",")
                }

                val cellType = this.mapData[y]!![x]


                // Skip walls - they are saved in WALLS section to reduce file size
                if (cellType == Constants.TYPE_HORIZONTAL_WALL || cellType == Constants.TYPE_VERTICAL_WALL) {
                    sb.append(Constants.TYPE_EMPTY)
                } else if (cellType == Constants.TYPE_TARGET) {
                    // Targets include their color information
                    sb.append(cellType).append(":").append(targetColors[y]!![x])
                } else {
                    sb.append(cellType)
                }
            }
            sb.append("\n")
        }


        // Save targets in compact format: tcolorX,Y; (e.g., tb8,7;)
        var targetCount = 0
        for (y in 0..<height) {
            for (x in 0..<width) {
                if (this.mapData[y]!![x] == Constants.TYPE_TARGET) {
                    var color: Int = targetColors[y]!![x]
                    if ((color < -1 || color > 4)) {
                        // Target color is invalid (not COLOR_MULTI and not 0-4) - try to recover from gameElements
                        Timber.e(
                            "[SAVE_DATA] Target at (%d,%d) has invalid color %d in targetColors array, recovering from gameElements",
                            x,
                            y,
                            color
                        )
                        for (element in gameElements) {
                            if (element.type == GameElement.TYPE_TARGET && element.x == x && element.y == y) {
                                color = element.color
                                Timber.d(
                                    "[SAVE_DATA] Recovered target color %d from gameElement at (%d,%d)",
                                    color,
                                    x,
                                    y
                                )
                                targetColors[y]!![x] = color
                                break
                            }
                        }
                        if (color < -1 || color > 4) {
                            Timber.e(
                                "[SAVE_DATA] FATAL: Could not recover target color at (%d,%d), gameElements has no matching target",
                                x,
                                y
                            )
                        }
                    }
                    sb.append("t").append(getColorChar(color))
                        .append(x).append(",").append(y).append(";")
                    targetCount++
                    Timber.d("[SAVE_DATA] Serializing target at (%d,%d) with color %d", x, y, color)
                }
            }
        }

        if (targetCount == 0) {
            // This is a fatal error - all Roboyard games MUST have targets
            val t = Throwable()
            Timber.e(t, "[SAVE_DATA] FATAL ERROR: No targets found while serializing game state!")
            throw IllegalStateException("[SAVE_DATA] Cannot save game: no targets found in game state")
        }

        sb.append("\n")


        // Save walls in compact format: hX,Y; and vX,Y;
        // Horizontal walls (y goes to height to include bottom boundary)
        for (y in 0..height) {
            for (x in 0..<width) {
                if (hasHorizontalWall(x, y)) {
                    sb.append("h").append(x).append(",").append(y).append(";")
                }
            }
        }
        // Vertical walls (x goes to width to include right boundary)
        for (y in 0..<height) {
            for (x in 0..width) {
                if (hasVerticalWall(x, y)) {
                    sb.append("v").append(x).append(",").append(y).append(";")
                }
            }
        }

        sb.append("\n")


        // Determine which positions to serialize as initial robot positions.
        // IMPORTANT: initialRobotPositions MUST be set before serializing.
        // This is set by storeInitialRobotPositions() when the game is created.
        if (initialRobotPositions == null || initialRobotPositions!!.isEmpty()) {
            val t = Throwable()
            Timber.e(
                t,
                "[SAVE_DATA] FATAL ERROR: initialRobotPositions not set! Game must call storeInitialRobotPositions() after creation."
            )
            throw IllegalStateException("[SAVE_DATA] Cannot save game: initialRobotPositions not set")
        }
        val positionsToSerialize = initialRobotPositions

        // Save robots in compact format: rcolorX,Y; (e.g., rr1,5;)
        for (entry in positionsToSerialize!!.entries) {
            val robotColor: Int = entry.key!!
            val position: IntArray = entry.value!!
            sb.append("r").append(getColorChar(robotColor))
                .append(position[0]).append(",").append(position[1]).append(";")
        }

        return sb.toString()
    }

    /**
     * Reset robot positions to their starting positions
     * This keeps the same map but moves robots back to where they started
     */
    fun resetRobotPositions() {
        // Store initial robot positions if not already stored
        if (initialRobotPositions == null || initialRobotPositions!!.isEmpty()) {
            Timber.e("[ROBOTS] resetRobotPositions: Cannot reset, initialRobotPositions is null or empty")
            return
        }

        Timber.d(
            "[ROBOTS] resetRobotPositions: Starting reset with %d stored initial positions",
            initialRobotPositions!!.size
        )


        // Get current robot elements
        val currentRobots: MutableList<GameElement> = java.util.ArrayList<GameElement>()
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_ROBOT) {
                currentRobots.add(element)
            }
        }


        // Skip if no robots found
        if (currentRobots.isEmpty()) {
            Timber.e("[ROBOTS] resetRobotPositions: No robots found in current game state")
            return
        }

        Timber.d("[ROBOTS] resetRobotPositions: Found %d robots to reset", currentRobots.size)


        // Reset each robot to its initial position
        for (robot in currentRobots) {
            val robotColor = robot.color
            Timber.d("[ROBOTS] resetRobotPositions: Processing robot with color %d", robotColor)

            if (initialRobotPositions!!.containsKey(robotColor)) {
                val position = initialRobotPositions!!.get(robotColor)
                Timber.d(
                    "[ROBOTS] resetRobotPositions: Resetting robot color %d from (%d, %d) to (%d, %d)",
                    robotColor, robot.x, robot.y, position!![0], position[1]
                )
                robot.x = position[0]
                robot.y = position[1]
            } else {
                Timber.e(
                    "[ROBOTS] resetRobotPositions: No initial position found for robot color %d",
                    robotColor
                )
            }
        }


        // Reset selected robot
        selectedRobot = null


        // Reset move count
        moveCount = 0


        // Reset completion flag
        this.isComplete = false

        Timber.d("[ROBOTS] resetRobotPositions: Reset complete")
    }


    /**
     * Check if a robot has reached its target
     * @param robot The robot to check
     * @return True if the robot is at its matching target, false otherwise
     */
    fun isRobotAtTarget(robot: GameElement?): Boolean {
        if (robot == null || robot.type != GameElement.TYPE_ROBOT) {
            return false
        }

        val robotX = robot.x
        val robotY = robot.y
        val robotColor = robot.color


        // Find matching target
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_TARGET && element.x == robotX && element.y == robotY) {
                // Allow any robot to match a multi-color target
                if (element.color == Constants.COLOR_MULTI) {
                    Timber.d(
                        "[TARGET_MULTI_MATCH] Robot %d matches multi target at (%d,%d)",
                        robot.color,
                        robotX,
                        robotY
                    )
                    return true
                }
                // Otherwise, require color match
                if (element.color == robotColor) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if the given robot is standing on a target that does NOT match its color.
     * Used to show the "wrong robot" hint toast only for the specific robot that just moved.
     * @param robot the robot to check
     * @return true if the robot is on a target of a different (non-multi) color
     */
    fun isRobotOnWrongTarget(robot: GameElement?): Boolean {
        if (robot == null || robot.type != GameElement.TYPE_ROBOT) {
            return false
        }
        val rx = robot.x
        val ry = robot.y
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_TARGET && element.x == rx && element.y == ry) {
                if (element.color != Constants.COLOR_MULTI &&
                    element.color != robot.color
                ) {
                    return true
                }
            }
        }
        return false
    }

    val isAnyWrongRobotOnTarget: Boolean
        /**
         * Check if any robot is on a target that does NOT match its color.
         * Used to show the "wrong robot" hint toast.
         * @return true if at least one wrong-colored robot is standing on any target
         */
        get() {
            for (robot in this.robots) {
                val rx = robot.x
                val ry = robot.y
                for (element in gameElements) {
                    if (element.type == GameElement.TYPE_TARGET && element.x == rx && element.y == ry) {
                        if (element.color != Constants.COLOR_MULTI &&
                            element.color != robot.color
                        ) {
                            return true
                        }
                    }
                }
            }
            return false
        }

    val robots: MutableList<GameElement>
        /**
         * Get all robots in the game state
         * @return List of robot game elements
         */
        get() {
            val robots: MutableList<GameElement> =
                java.util.ArrayList<GameElement>()
            for (element in gameElements) {
                if (element.type == GameElement.TYPE_ROBOT) {
                    robots.add(element)
                }
            }
            return robots
        }

    val target: GameElement?
        /**
         * Get a single target (first one found)
         * @return The first target found, or null if no targets exist
         */
        get() {
            for (element in gameElements) {
                if (element.type == GameElement.TYPE_TARGET) {
                    return element
                }
            }
            return null
        }

    val targets: MutableList<GameElement?>
        /**
         * Get all targets in the game state
         * @return List of target game elements
         */
        get() {
            val targets: MutableList<GameElement?> =
                java.util.ArrayList<GameElement?>()
            for (element in gameElements) {
                if (element.type == GameElement.TYPE_TARGET) {
                    targets.add(element)
                }
            }
            return targets
        }

    /**
     * Sets the number of robots per color to use for this game
     * @param count Number of robots (1-4)
     */
    fun setRobotCount(count: Int) {
        // Ensure count is within valid range
        this.robotCount = max(1, min(Constants.NUM_ROBOTS, count))
        Timber.d("Robot count set to %d", this.robotCount)
    }

    /**
     * Gets the current robot count setting
     * @return Number of robots per color (1-4)
     */
    fun getRobotCount(): Int {
        return robotCount
    }

    /**
     * Sets the number of different target colors to use for this game
     * @param count Number of target colors (1-4)
     */
    fun setTargetColors(count: Int) {
        // Ensure count is within valid range
        this.targetColorsCount = max(1, min(4, count))
        Timber.d("Target colors count set to %d", this.targetColorsCount)
    }

    /**
     * Gets the current target colors count setting
     * @return Number of target colors (1-4)
     */
    fun getTargetColors(): Int {
        return targetColorsCount
    }

    /**
     * Increment the hint count
     */
    fun incrementHintCount() {
        hintCount++
    }

    /**
     * Store initial robot positions for reset functionality
     */
    fun storeInitialRobotPositions() {
        // Initialize the map if it doesn't exist
        if (initialRobotPositions == null) {
            initialRobotPositions = HashMap<Int?, IntArray?>()
            Timber.d("[ROBOTS] storeInitialRobotPositions: Created new initialRobotPositions map")
        } else {
            Timber.d(
                "[ROBOTS] storeInitialRobotPositions: Using existing initialRobotPositions map with %d entries",
                initialRobotPositions!!.size
            )
        }

        var robotCount = 0
        // Loop through all game elements to find robots
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_ROBOT) {
                // Store the robot's position by its color
                val position = intArrayOf(element.x, element.y)
                initialRobotPositions!!.put(element.color, position)
                Timber.d(
                    "[ROBOTS] storeInitialRobotPositions: Stored robot color %d at position (%d, %d)",
                    element.color, position[0], position[1]
                )
                robotCount++
            }
        }

        Timber.d("[ROBOTS] storeInitialRobotPositions: Stored positions for %d robots", robotCount)
    }

    /**
     * Set the GameStateManager reference
     * @param manager The GameStateManager to use
     */
    fun setGameStateManager(manager: GameStateManager?) {
        this.gameStateManager = manager
    }

    fun canRobotMoveTo(robot: GameElement, nextX: Int, nextY: Int): Boolean {
        // Check if the target position is within the board boundaries
        if (nextX < 0 || nextX >= width || nextY < 0 || nextY >= height) {
            return false
        }


        // Check if there's already another robot at the target position
        val otherRobot = getRobotAt(nextX, nextY)
        if (otherRobot != null && otherRobot !== robot) {
            return false
        }


        // Check current robot position
        val currentX = robot.x
        val currentY = robot.y


        // Check for walls in the movement path
        // Moving horizontally
        if (currentY == nextY) {
            // Moving east
            if (currentX < nextX) {
                // Check for vertical walls between current position and target
                for (x in currentX..<nextX) {
                    if (hasVerticalWall(x + 1, currentY)) {
                        return false
                    }
                }
            } else if (currentX > nextX) {
                // Check for vertical walls between current position and target
                for (x in nextX..<currentX) {
                    if (hasVerticalWall(x + 1, currentY)) {
                        return false
                    }
                }
            }
        } else if (currentX == nextX) {
            // Moving south
            if (currentY < nextY) {
                // Check for horizontal walls between current position and target
                for (y in currentY..<nextY) {
                    if (hasHorizontalWall(currentX, y + 1)) {
                        return false
                    }
                }
            } else if (currentY > nextY) {
                // Check for horizontal walls between current position and target
                for (y in nextY..<currentY) {
                    if (hasHorizontalWall(currentX, y + 1)) {
                        return false
                    }
                }
            }
        }


        // If we've made it this far, the move is valid
        return true
    }

    /**
     * Ensure all targets in gameElements are properly reflected in the board array
     * This is crucial to prevent target loss when saving/loading games
     * @return number of targets synchronized
     */
    fun synchronizeTargets(): Int {
        var syncedTargets = 0


        // First log the current state for diagnostic purposes
        var boardTargets = 0
        for (y in 0..<height) {
            for (x in 0..<width) {
                if (this.mapData[y]!![x] == Constants.TYPE_TARGET) {
                    boardTargets++
                }
            }
        }

        var elementTargets = 0
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_TARGET) {
                elementTargets++
            }
        }

        Timber.d(
            "[TARGET SYNC] Before synchronization: %d targets in board, %d targets in gameElements",
            boardTargets, elementTargets
        )


        // Now update the board array to match the gameElements list
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_TARGET) {
                val x = element.x
                val y = element.y
                val color = element.color

                Timber.d(
                    "[TARGET SYNC] GameElement target at (%d,%d) with color %d, board=%d, targetColors=%d",
                    x, y, color,
                    if (x >= 0 && y >= 0 && x < width && y < height) this.mapData[y]!![x] else -999,
                    if (x >= 0 && y >= 0 && x < width && y < height) targetColors[y]!![x] else -999
                )


                // Skip invalid coordinates
                if (x < 0 || y < 0 || x >= width || y >= height) {
                    Timber.e(
                        "[TARGET SYNC] Target at invalid position (%d,%d) with color %d",
                        x,
                        y,
                        color
                    )
                    continue
                }


                // If this target is not reflected in the board array, update it
                if (this.mapData[y]!![x] != Constants.TYPE_TARGET) {
                    Timber.d(
                        "[TARGET SYNC] Updating board at (%d,%d) from %d to %s for target with color %d",
                        x, y, this.mapData[y]!![x], Constants.TYPE_TARGET, color
                    )
                    this.mapData[y]!![x] = Constants.TYPE_TARGET
                    targetColors[y]!![x] = color
                    syncedTargets++
                } else if (targetColors[y]!![x] != color) {
                    // The cell is already a target but the color doesn't match
                    Timber.d(
                        "[TARGET SYNC] Updating target color at (%d,%d) from %d to %d",
                        x, y, targetColors[y]!![x], color
                    )
                    targetColors[y]!![x] = color
                    syncedTargets++
                }


                // Detect invalid color on either side and log for root cause analysis
                // COLOR_MULTI (-1) is valid, so only flag colors < -1
                if (color < -1 || color > 4) {
                    Timber.e(
                        "[TARGET SYNC] GameElement at (%d,%d) has invalid color %d",
                        x,
                        y,
                        color
                    )
                }
                if (targetColors[y]!![x] < -1 || targetColors[y]!![x] > 4) {
                    Timber.e(
                        "[TARGET SYNC] targetColors[%d][%d] has invalid value %d after sync",
                        y,
                        x,
                        targetColors[y]!![x]
                    )
                }
            }
        }

        Timber.d("[TARGET SYNC] Synchronized %d targets", syncedTargets)
        return syncedTargets
    }

    // ========== Map Signature Generation for Unique Map Tracking ==========
    /**
     * Generate a unique signature for the wall layout only.
     * Used for achievements that track same walls with different robot positions.
     * Format matches level file format: 12x14;mh1,0;mh1,3;...mv9,6;
     * @return A string signature representing the wall layout
     */
    fun generateWallSignature(): String {
        val sb = StringBuilder()
        sb.append(width).append("x").append(height).append(";")


        // Collect all walls in sorted order
        val walls: MutableList<String?> = java.util.ArrayList<String?>()
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_HORIZONTAL_WALL) {
                walls.add("mh" + element.x + "," + element.y)
            } else if (element.type == GameElement.TYPE_VERTICAL_WALL) {
                walls.add("mv" + element.x + "," + element.y)
            }
        }
        Collections.sort(walls as MutableList<String>)
        for (wall in walls) {
            sb.append(wall).append(";")
        }
        return sb.toString()
    }

    /**
     * Generate a unique signature for robot and target positions only.
     * @return A string signature representing positions
     */
    fun generatePositionSignature(): String {
        val sb = StringBuilder()


        // Collect initial robot positions in sorted order
        val robots: MutableList<String?> = java.util.ArrayList<String?>()
        if (initialRobotPositions != null) {
            for (entry in initialRobotPositions!!.entries) {
                val pos: IntArray = entry.value!!
                robots.add("R" + entry.key + "@" + pos[0] + "," + pos[1])
            }
        }
        Collections.sort(robots as MutableList<String>)
        for (robot in robots) {
            sb.append(robot).append(";")
        }

        sb.append("|")


        // Collect target positions in sorted order
        val targets: MutableList<String?> = java.util.ArrayList<String?>()
        for (element in gameElements) {
            if (element.type == GameElement.TYPE_TARGET) {
                targets.add("T" + element.color + "@" + element.x + "," + element.y)
            }
        }
        Collections.sort(targets as MutableList<String>)
        for (target in targets) {
            sb.append(target).append(";")
        }

        return sb.toString()
    }

    /**
     * Generate a complete unique signature for the entire map.
     * Combines wall signature and position signature.
     * Two maps with identical signatures are considered the same map.
     * @return A string signature representing the complete map
     */
    fun generateMapSignature(): String {
        return generateWallSignature() + "||" + generatePositionSignature()
    }

    companion object {
        private const val serialVersionUID = 1L
        private const val TAG = "GameState"

        /**
         * Load a saved game from a file
         */
        @JvmStatic
        fun loadSavedGame(context: Context, slotId: Int): GameState? {
            try {
                val saveDir = File(context.getFilesDir(), Constants.SAVE_DIRECTORY)
                if (!saveDir.exists()) {
                    return null
                }

                val fileName =
                    Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION
                val saveFile = File(saveDir, fileName)

                Timber.d(
                    "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Attempting to load game from slot %d with filename: %s",
                    slotId,
                    fileName
                )
                Timber.d(
                    "[GAME_LOAD] Save directory path: %s, exists: %s",
                    saveDir.getAbsolutePath(),
                    saveDir.exists()
                )
                Timber.d(
                    "[GAME_LOAD] Save file path: %s, exists: %s, size: %d bytes",
                    saveFile.getAbsolutePath(),
                    saveFile.exists(),
                    if (saveFile.exists()) saveFile.length() else 0
                )

                if (!saveFile.exists()) {
                    return null
                }

                val saveData = StringBuilder()
                FileInputStream(saveFile).use { fis ->
                    BufferedReader(InputStreamReader(fis)).use { reader ->
                        var line: String?
                        while ((reader.readLine().also { line = it }) != null) {
                            saveData.append(line).append("\n")
                        }
                    }
                }
                Timber.d("Read %d characters from save file", saveData.length)

                val state: GameState? = parseFromSaveData(saveData.toString(), context)


                // Extract and store solutions from metadata if available
                if (state != null) {
                    val metadata = GameStateManager.extractMetadataFromSaveData(saveData.toString())
                    if (metadata != null && metadata.containsKey("SOLUTIONS")) {
                        val solutionsStr = metadata.get("SOLUTIONS")
                        Timber.d(
                            "[SOLUTIONS_SAVE_LOAD] Found SOLUTIONS in metadata: %s",
                            solutionsStr
                        )
                        state.savedSolutions = solutionsStr
                    } else {
                        Timber.d("[SOLUTIONS_SAVE_LOAD] No SOLUTIONS found in save metadata")
                    }
                }


                // Debug: verify that targets were properly loaded
                if (state != null) {
                    var targetCount = 0


                    // First check targets in the gameElements list - this is more reliable
                    // especially for targets at x=0 which may be affected by wall indexing
                    for (element in state.gameElements) {
                        if (element.type == GameElement.TYPE_TARGET) {
                            targetCount++
                            Timber.d(
                                "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Found target in gameElements at (%d,%d) with color %d",
                                element.x, element.y, element.color
                            )
                        }
                    }


                    // Also verify targets in the board array as a secondary check
                    if (targetCount == 0) {
                        for (y in 0..<state.height) {
                            for (x in 0..<state.width) {
                                if (state.getCellType(x, y) == Constants.TYPE_TARGET) {
                                    targetCount++
                                    Timber.d(
                                        "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Found target in board array at (%d,%d) with color %d",
                                        x, y, state.getTargetColor(x, y)
                                    )
                                }
                            }
                        }
                    }

                    Timber.d(
                        "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Loaded GameState has %d targets",
                        targetCount
                    )

                    if (targetCount == 0) {
                        Timber.e(
                            "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] NO TARGETS FOUND after loading save file %s",
                            fileName
                        )
                        // Try to examine the save file contents to debug this issue
                        val contentLines =
                            saveData.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        for (i in 0..<min(contentLines.size, 20)) {
                            Timber.e(
                                "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Line %d: %s",
                                i,
                                contentLines[i]
                            )
                        }
                        // Don't load games without targets - this is a critical error
                        val t = Throwable()
                        Timber.e(
                            t,
                            "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Stack trace for no target found"
                        )
                        throw IllegalStateException("[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Cannot load game: no targets found in save file")
                    }
                }

                return state
            } catch (e: IOException) {
                Timber.e(
                    e,
                    "[GAME_LOAD][SOLUTIONS_SAVE_LOAD] Error loading saved game from slot %d",
                    slotId
                )
                return null
            }
        }

        /**
         * Parse metadata from save data string.
         * Extracts map name, dimensions, move counts, time, and all items.
         * 
         * @param saveData The raw save data string
         * @return Map with keys: mapName, width, height, moveCount, optimalMoveCount, timePlayed, allItems
         */
        @JvmStatic
        fun parseMetadata(saveData: String?): MutableMap<String?, Any?>? {
            val result: MutableMap<String?, Any?> = HashMap<String?, Any?>()

            if (saveData == null || saveData.isEmpty()) return null

            var mapName = "Loaded Game"
            var width = 16
            var height = 16
            var moveCount = 0
            var optimalMoveCount = 0
            var timePlayed: Long = 0
            var currentDifficulty = Constants.DIFFICULTY_BEGINNER

            val lines = saveData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()


            // Collect all items from metadata line and separate lines
            val allItems: MutableList<String> = java.util.ArrayList<String>()
            if (lines.size > 0 && lines[0].startsWith("#")) {
                val metadata =
                    lines[0].substring(1).split(";".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                for (item in metadata) {
                    if (!item.trim { it <= ' ' }.isEmpty()) {
                        allItems.add(item.trim { it <= ' ' })
                    }
                }
            }
            for (i in 1..<lines.size) {
                val line = lines[i].trim { it <= ' ' }
                if (!line.isEmpty()) {
                    allItems.add(line)
                }
            }


            // Parse metadata items
            for (item in allItems) {
                try {
                    if (item.startsWith("MAPNAME:")) {
                        mapName = item.substring("MAPNAME:".length)
                    } else if (item.startsWith("MOVES:")) {
                        moveCount = item.substring("MOVES:".length).toInt()
                    } else if (item.startsWith("OPTIMAL:")) {
                        optimalMoveCount = item.substring("OPTIMAL:".length).toInt()
                    } else if (item.startsWith("TIME:")) {
                        timePlayed = item.substring("TIME:".length).toLong()
                    } else if (item.startsWith("SIZE:")) {
                        val sizeParts = item.substring("SIZE:".length).split(",".toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (sizeParts.size == 2) {
                            width = sizeParts[0].trim { it <= ' ' }.toInt()
                            height = sizeParts[1].trim { it <= ' ' }.toInt()
                        }
                    } else if (item.startsWith("WIDTH:")) {
                        width = item.substring("WIDTH:".length).trim { it <= ' ' }.replace(";", "")
                            .toInt()
                    } else if (item.startsWith("HEIGHT:")) {
                        height =
                            item.substring("HEIGHT:".length).trim { it <= ' ' }.replace(";", "")
                                .toInt()
                    } else if (item.startsWith("DIFFICULTY:")) {
                        currentDifficulty =
                            item.substring("DIFFICULTY:".length).trim { it <= ' ' }.replace(";", "")
                                .toInt()
                    }
                } catch (e: NumberFormatException) {
                    // Ignore parse errors for individual items
                }
            }

            result.put("mapName", mapName)
            result.put("width", width)
            result.put("height", height)
            result.put("moveCount", moveCount)
            result.put("optimalMoveCount", optimalMoveCount)
            result.put("timePlayed", timePlayed)
            result.put("difficulty", currentDifficulty)
            result.put("allItems", allItems)

            return result
        }

        /**
         * Parse a game state from save data
         * @param saveData The save data string
         * @param context The context
         * @return The parsed game state or null if parsing failed
         */
        @JvmStatic
        fun parseFromSaveData(saveData: String, context: Context?): GameState? {
            try {
                // Use central metadata parser (DRY)
                val metadata: MutableMap<String?, Any?>? = parseMetadata(saveData)
                if (metadata == null) {
                    Timber.e("[TARGET LOADING] Failed to parse save data metadata")
                    return null
                }

                var width = metadata.get("width") as Int
                var height = metadata.get("height") as Int
                val mapName = metadata.get("mapName") as String?
                val moveCount = metadata.get("moveCount") as Int
                val timePlayed = metadata.get("timePlayed") as Long
                val difficulty = metadata.get("difficulty") as Int

                Timber.d(
                    "[TARGET LOADING] Parsing save data: %dx%d, %s, difficulty: %d",
                    width,
                    height,
                    mapName,
                    difficulty
                )

                var inRobotsSection = false
                var inInitialPositionsSection = false
                var boardDataStarted = false
                var inTargetSection = false


                // Create the game state with the parsed dimensions
                var state = GameState(width, height)
                state.levelName = mapName
                state.moveCount = moveCount
                state.difficulty = difficulty
                state.startTime = System.currentTimeMillis() - timePlayed

                val lines =
                    saveData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()


                // Parse board data
                var boardLine = 0
                var wallsAdded = 0
                var targetsAdded = 0


                // Check if this is compact format (no WIDTH/HEIGHT lines, targets/robots/walls on single lines)
                // Supports both old format (mh, mv, tb, rb, ry, rg, rs) and new format (h, v, t, r)
                var isCompactFormat = false
                for (line in lines) {
                    // Check for new format patterns: h0,0; v0,0; tb8,7; rr1,5;
                    // Check for old format patterns: mh0,0; mv0,0; target_blue8,7; robot_red1,5;
                    if (line.matches("^[hmvtr].*\\d+,\\d+;.*".toRegex()) ||
                        line.matches(".*[mh|mv|tb|tg|tr|ty|ts|rb|rg|rr|ry|rs|target_|robot_].*\\d+,\\d+;.*".toRegex())
                    ) {
                        isCompactFormat = true
                        break
                    }
                }

                if (isCompactFormat) {
                    // Parse new compact format using central parser (handles comments and line breaks)
                    val entries = LevelFormatParser.parseRawEntries(saveData)


                    // First pass: extract board dimensions (may override metadata dimensions)
                    for (entry in entries) {
                        if (entry.type == "board") {
                            val cleanData =
                                if (entry.data.startsWith(":")) entry.data.substring(1) else entry.data
                            val parts =
                                cleanData.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (parts.size == 2) {
                                try {
                                    width = parts[0].toInt()
                                    height = parts[1].toInt()
                                    state = GameState(width, height)
                                    state.levelName = mapName
                                    state.moveCount = moveCount
                                    state.startTime = System.currentTimeMillis() - timePlayed
                                    Timber.d(
                                        "[BOARD_SIZE_DEBUG] parseFromSaveData compact format board size: %dx%d",
                                        width,
                                        height
                                    )
                                } catch (e: NumberFormatException) {
                                    Timber.e(
                                        e,
                                        "[BOARD_SIZE_DEBUG] Error parsing board dimensions from compact format"
                                    )
                                }
                            }
                            break
                        }
                    }


                    // Second pass: parse all other entries
                    for (entry in entries) {
                        val type = entry.type
                        val data = entry.data

                        try {
                            // Skip board entry (already handled)
                            if (type == "board") continue

                            if (type.startsWith("t")) {
                                // Target: tcolorX,Y; (e.g., tb8,7; parsed as type=tb, data=8,7)
                                // or legacy: target_colorX,Y; (e.g., target_blue8,7; parsed as type=target_blue, data=8,7)
                                var colorId = -1
                                var coords = data

                                if (type.length == 2 && type[0] == 't') {
                                    // New format: Color is second char of type
                                    colorId = parseColorChar(type[1])
                                } else if (type.startsWith("target_")) {
                                    // Legacy format: target_colorname
                                    val colorName = type.substring(7)
                                    colorId = parseColorName(colorName)
                                } else if (type.length == 1 && data.length >= 2) {
                                    // Compact format: tcolorX,Y; (color is first char of data)
                                    colorId = parseColorChar(data[0])
                                    coords = data.substring(1)
                                }

                                if (colorId >= -1) { // -1 = COLOR_MULTI, 0-4 = normal colors
                                    val parts =
                                        coords.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                            .toTypedArray()
                                    if (parts.size == 2) {
                                        val x = parts[0].toInt()
                                        val y = parts[1].toInt()
                                        state.addTarget(x, y, colorId)
                                        targetsAdded++
                                    }
                                }
                            } else if (type.startsWith("r")) {
                                // Robot: rcolorX,Y; (e.g., rr1,5; parsed as type=rr, data=1,5)
                                // or legacy: robot_colorX,Y; (e.g., robot_red1,5; parsed as type=robot_red, data=1,5)
                                var colorId = -1
                                var coords = data

                                if (type.length == 2 && type[0] == 'r') {
                                    // New format: Color is second char of type
                                    colorId = parseColorChar(type[1])
                                } else if (type.startsWith("robot_")) {
                                    // Legacy format: robot_colorname
                                    val colorName = type.substring(6)
                                    colorId = parseColorName(colorName)
                                } else if (type.length == 1 && data.length >= 2) {
                                    // Compact format: rcolorX,Y; (color is first char of data)
                                    colorId = parseColorChar(data[0])
                                    coords = data.substring(1)
                                }

                                if (colorId >= 0) {
                                    val parts =
                                        coords.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                            .toTypedArray()
                                    if (parts.size == 2) {
                                        val x = parts[0].toInt()
                                        val y = parts[1].toInt()
                                        state.addRobot(x, y, colorId)
                                    }
                                }
                            } else if (type == "h") {
                                // Horizontal wall: hX,Y; (e.g., h0,0;)
                                val parts = data.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                                if (parts.size == 2) {
                                    val x = parts[0].toInt()
                                    val y = parts[1].toInt()
                                    state.addHorizontalWall(x, y)
                                    wallsAdded++
                                }
                            } else if (type == "v") {
                                // Vertical wall: vX,Y; (e.g., v0,0;)
                                val parts = data.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                                if (parts.size == 2) {
                                    val x = parts[0].toInt()
                                    val y = parts[1].toInt()
                                    state.addVerticalWall(x, y)
                                    wallsAdded++
                                }
                            }
                        } catch (e: NumberFormatException) {
                            Timber.e(
                                "Error parsing compact format entry '%s:%s': %s",
                                type,
                                data,
                                e.message
                            )
                        }
                    }


                    // Store initial robot positions
                    state.storeInitialRobotPositions()
                    Timber.d(
                        "[SAVE_LOAD] Successfully parsed compact format save data: %d targets, %d walls",
                        targetsAdded,
                        wallsAdded
                    )
                    return state
                }


                // Legacy format parsing (old saves with WIDTH/HEIGHT, ROBOTS, INITIAL_POSITIONS, WALLS, TARGET_SECTION)
                // Skip metadata and dimension lines
                for (i in 1..<lines.size) {
                    val line = lines[i]

                    if (line.startsWith("WIDTH:") || line.startsWith("HEIGHT:")) {
                        continue
                    }


                    // Check for section markers
                    if (line == "ROBOTS:") {
                        inRobotsSection = true
                        inInitialPositionsSection = false
                        boardDataStarted = false // Exit board data mode
                        continue
                    } else if (line == "INITIAL_POSITIONS:") {
                        inRobotsSection = false
                        inInitialPositionsSection = true
                        boardDataStarted = false // Exit board data mode
                        continue
                    } else if (line == "WALLS:") {
                        // New section for wall data - authoritative source for walls
                        inRobotsSection = false
                        inInitialPositionsSection = false
                        boardDataStarted = false // Exit board data mode
                        // Remove wall GameElements already added from board data to avoid duplicates
                        // WALLS section is the authoritative source, same pattern as TARGET_SECTION
                        var removedWalls = 0
                        val wallIt = state.gameElements.iterator()
                        while (wallIt.hasNext()) {
                            val t = wallIt.next().type
                            if (t == GameElement.TYPE_HORIZONTAL_WALL || t == GameElement.TYPE_VERTICAL_WALL) {
                                wallIt.remove()
                                removedWalls++
                            }
                        }
                        wallsAdded -= removedWalls
                        Timber.d(
                            "[MAPSIG] Entering WALLS section, removed %d duplicate wall GameElements from board data",
                            removedWalls
                        )
                        continue
                    } else if (line == "TARGET_SECTION:") {
                        // Entering targets section, exit other modes
                        inRobotsSection = false
                        inInitialPositionsSection = false
                        boardDataStarted = false
                        Timber.d("[TARGET LOADING] Entering TARGET_SECTION section")
                        inTargetSection = true
                        // Remove any target GameElements already added from board data parsing
                        // to avoid duplicates — TARGET_SECTION is the authoritative source
                        var removedCount = 0
                        val it = state.gameElements.iterator()
                        while (it.hasNext()) {
                            if (it.next().type == GameElement.TYPE_TARGET) {
                                it.remove()
                                removedCount++
                            }
                        }
                        if (removedCount > 0) {
                            Timber.d(
                                "[TARGET LOADING] Removed %d duplicate target GameElements from board data (TARGET_SECTION takes precedence)",
                                removedCount
                            )
                            targetsAdded -= removedCount
                        }
                        continue
                    } else if (line.startsWith("TARGET_SECTION:") && line.length > 15) {
                        // Format: TARGET_SECTION:x,y,color (each TARGET_SECTION entry is on its own line)
                        val targetData =
                            line.substring("TARGET_SECTION:".length).split(",".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (targetData.size >= 3) {
                            val x = targetData[0].toInt()
                            val y = targetData[1].toInt()
                            val color = targetData[2].toInt()

                            Timber.d(
                                "[TARGET LOADING] Processing target data from TARGET_SECTION entry: (%d,%d) with color %d",
                                x,
                                y,
                                color
                            )


                            // We directly set the cell type and color in the board data structures
                            // to avoid any synchronization issues
                            state.mapData[y]!![x] = Constants.TYPE_TARGET
                            state.targetColors[y]!![x] = color


                            // Also add as a game element for rendering
                            val target = GameElement(GameElement.TYPE_TARGET, x, y)
                            target.color = color
                            state.gameElements.add(target)

                            targetsAdded++
                            Timber.d(
                                "[TARGET LOADING] Added target at (%d,%d) with color %d from TARGET_SECTION section",
                                x,
                                y,
                                color
                            )


                            // Verify that the target was added correctly by directly querying the data structures
                            if (y >= 0 && y < state.height && x >= 0 && x < state.width) {
                                Timber.d(
                                    "[TARGET LOADING] Verification - Board value at (%d,%d): %d",
                                    x,
                                    y,
                                    state.mapData[y]!![x]
                                )
                                Timber.d(
                                    "[TARGET LOADING] Verification - Target color at (%d,%d): %d",
                                    x,
                                    y,
                                    state.targetColors[y]!![x]
                                )
                            } else {
                                Timber.e(
                                    "[TARGET LOADING] ERROR - Target at (%d,%d) is out of bounds (width=%d, height=%d)",
                                    x, y, state.width, state.height
                                )
                            }
                        } else {
                            Timber.e(
                                "[TARGET LOADING] ERROR - Invalid TARGET_SECTION format: %s",
                                line
                            )
                        }
                        continue
                    }


                    // Process WALLS section data
                    if (!inRobotsSection && !inInitialPositionsSection && !boardDataStarted && line.contains(
                            ","
                        ) &&
                        (line.startsWith("H,") || line.startsWith("V,"))
                    ) {
                        // Format: type,x,y
                        val wallData =
                            line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (wallData.size >= 3) {
                            val wallType: String? = wallData[0]
                            val x = wallData[1].toInt()
                            val y = wallData[2].toInt()

                            if ("H" == wallType) {
                                state.addHorizontalWall(x, y)
                                wallsAdded++
                                //Timber.d("Added horizontal wall at (%d,%d) from WALLS section", x, y);
                            } else if ("V" == wallType) {
                                state.addVerticalWall(x, y)
                                wallsAdded++
                                // Timber.d("Added vertical wall at (%d,%d) from WALLS section", x, y);
                            }
                        }
                        continue
                    }

                    if (!boardDataStarted && line.contains(",")) {
                        boardDataStarted = true
                        Timber.d("Started parsing board data at line %d", i)
                    }

                    if (boardDataStarted && boardLine < state.height) {
                        // Parse this line of board data
                        val cells =
                            line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        Timber.d(
                            "[TARGET LOADING] Parsing board line %d with %d cells",
                            boardLine,
                            cells.size
                        )

                        for (x in 0..<min(state.width, cells.size)) {
                            val cellData = cells[x]


                            // Don't skip empty cells, they might be important for column 0
                            if (cellData.isEmpty()) {
                                Timber.d(
                                    "Empty cell data at (%d,%d), treating as empty cell",
                                    x,
                                    boardLine
                                )
                                continue
                            }

                            try {
                                if (cellData.contains(":")) {
                                    // This is a target cell with color
                                    val targetParts =
                                        cellData.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                            .toTypedArray()
                                    val cellType = targetParts[0].toInt()
                                    val targetColor = targetParts[1].toInt()

                                    Timber.d(
                                        "[TARGET LOADING] Found target cell at (%d,%d) with type %d and color %d",
                                        x,
                                        boardLine,
                                        cellType,
                                        targetColor
                                    )

                                    if (cellType == Constants.TYPE_TARGET) {
                                        state.addTarget(x, boardLine, targetColor)
                                        targetsAdded++
                                        Timber.d(
                                            "[TARGET LOADING] Added target at (%d,%d) with color %d from board data",
                                            x,
                                            boardLine,
                                            targetColor
                                        )
                                    } else {
                                        Timber.d(
                                            "[LOAD/SAVE] unexpected target cellType: %d",
                                            cellType
                                        )
                                    }
                                } else {
                                    val cellType = cellData.toInt()

                                    // Timber.d("Found cell at (%d,%d) with type %d", x, boardLine, cellType);
                                    if (cellType == Constants.TYPE_HORIZONTAL_WALL) {
                                        state.addHorizontalWall(x, boardLine)
                                        wallsAdded++
                                        // Timber.d("Added horizontal wall at (%d,%d)", x, boardLine);
                                    } else if (cellType == Constants.TYPE_VERTICAL_WALL) {
                                        state.addVerticalWall(x, boardLine)
                                        wallsAdded++
                                        // Timber.d("Added vertical wall at (%d,%d)", x, boardLine);
                                    } else if (cellType == Constants.TYPE_EMPTY) {
                                        // Empty cell, nothing to do
                                    } else if (cellType != Constants.TYPE_TARGET && cellType != Constants.TYPE_ROBOT) {
                                        // Only log unknown cell types that aren't targets or robots
                                        Timber.d(
                                            "[LOAD/SAVE] unknown cellType: %d at (%d,%d)",
                                            cellType,
                                            x,
                                            boardLine
                                        )
                                    }
                                }
                            } catch (e: NumberFormatException) {
                                Timber.e(
                                    "Error parsing cell data '%s' at (%d,%d): %s",
                                    cellData,
                                    x,
                                    boardLine,
                                    e.message
                                )
                            }
                        }
                        boardLine++
                    }


                    // Process ROBOTS section data
                    if (inRobotsSection && line.contains(",")) {
                        val robotData =
                            line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (robotData.size >= 3) {
                            val x = robotData[0].toInt()
                            val y = robotData[1].toInt()
                            val color = robotData[2].toInt()
                            state.addRobot(x, y, color)
                            Timber.d("Added robot at (%d,%d) with color %d", x, y, color)
                        }
                        continue
                    }

                    // Process INITIAL_POSITIONS section data
                    if (inInitialPositionsSection && line.contains(",")) {
                        // Initialize the initialRobotPositions map if it doesn't exist
                        if (state.initialRobotPositions == null) {
                            state.initialRobotPositions = HashMap<Int?, IntArray?>()
                        }

                        val positionData =
                            line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (positionData.size >= 3) {
                            val x = positionData[0].toInt()
                            val y = positionData[1].toInt()
                            val color = positionData[2].toInt()


                            // Store the initial position for the robot
                            state.initialRobotPositions!!.put(color, intArrayOf(x, y))
                            Timber.d(
                                "Added initial position for robot color %d at (%d,%d)",
                                color,
                                x,
                                y
                            )
                        }
                        continue
                    }


                    // Skip dimension lines and empty lines
                    if (line.startsWith("WIDTH:") || line.startsWith("HEIGHT:") || line.trim { it <= ' ' }
                            .isEmpty()) {
                        continue
                    }
                }


                // If we have initial positions but no robots have been reset to them yet,
                // reset the robots to their initial positions
                if (state.initialRobotPositions != null && !state.initialRobotPositions!!.isEmpty()) {
                    state.resetRobotPositions()
                    Timber.d("Reset robots to their initial positions after loading")
                } else {
                    // If we don't have initial positions saved in the file, store the current positions as initial
                    state.storeInitialRobotPositions()
                    Timber.d("No initial positions found in save file, storing current positions as initial")
                }


                // Log a summary of the parsing results
                Timber.d("[TARGET LOADING] Parsing complete - Summary:")
                Timber.d("[TARGET LOADING] - Map name: %s", state.levelName)
                Timber.d("[TARGET LOADING] - Board dimensions: %dx%d", state.width, state.height)
                Timber.d("[TARGET LOADING] - Targets added: %d", targetsAdded)
                Timber.d("[TARGET LOADING] - Game elements count: %d", state.gameElements.size)


                // Count targets in game elements as a verification
                var targetElementsCount = 0
                for (element in state.gameElements) {
                    if (element.type == GameElement.TYPE_TARGET) {
                        targetElementsCount++
                    }
                }
                Timber.d(
                    "[TARGET LOADING] - Target elements in gameElements list: %d",
                    targetElementsCount
                )


                // Count targets in board array as another verification
                var targetsInBoard = 0
                for (y in 0..<state.height) {
                    for (x in 0..<state.width) {
                        if (state.mapData[y]!![x] == Constants.TYPE_TARGET) {
                            targetsInBoard++
                        }
                    }
                }
                Timber.d("[TARGET LOADING] - Targets in board array: %d", targetsInBoard)


                // If we detect a mismatch, log a warning
                if (targetsAdded != targetElementsCount || targetsAdded != targetsInBoard) {
                    Timber.w(
                        "[TARGET LOADING] WARNING - Target count mismatch: targetsAdded=%d, targetElementsCount=%d, targetsInBoard=%d",
                        targetsAdded, targetElementsCount, targetsInBoard
                    )
                }


                // Adjust robotCount to match the actual number of targets in this save/external map
                val actualTargets = max(targetElementsCount, targetsInBoard)
                if (actualTargets > 0) {
                    state.setRobotCount(actualTargets)
                    Timber.d(
                        "[TARGET LOADING] Set robotCount to %d based on actual target count",
                        actualTargets
                    )
                }

                Timber.d(
                    "Successfully parsed game state from save data: %d walls, %d targets",
                    wallsAdded,
                    targetsAdded
                )
                return state
            } catch (e: Exception) {
                Timber.e(e, "Error parsing save data: %s", e.message)
                return null
            }
        }

        /**
         * Load a level from assets
         */
        @JvmStatic
        fun loadLevel(context: Context, levelId: Int): GameState {
            Timber.d("Loading level %d from assets", levelId)

            try {
                // Construct the level file path
                val levelFilePath = "Maps/level_" + levelId + ".txt"


                // Read the level file content
                var levelContent = ""
                context.getAssets().open(levelFilePath).use { `is` ->
                    BufferedReader(
                        InputStreamReader(`is`)
                    ).use { reader ->
                        val sb = StringBuilder()
                        var line: String?
                        while ((reader.readLine().also { line = it }) != null) {
                            sb.append(line).append("\n")
                        }
                        levelContent = sb.toString()
                        Timber.d("Successfully read level file: %s", levelFilePath)
                    }
                }

                // Parse the level content
                val state: GameState = parseLevel(context, levelContent, levelId)
                state.levelId = levelId
                state.levelName = "Level " + levelId


                // Initialize the solver with the grid elements
                Timber.d(
                    "Level %d loaded successfully with %d grid elements",
                    levelId, state.gridElements.size
                )

                return state
            } catch (e: IOException) {
                Timber.e(e, "Error loading level %d: %s", levelId, e.message)
                // Don't fall back to random level - throw an exception instead
                throw RuntimeException("Failed to load level " + levelId, e)
            }
        }

        /**
         * Parse level content from a level file
         * Uses central LevelFormatParser for DRY principle
         * Supports comments (#) and optional line breaks
         */
        @JvmStatic
        fun parseLevel(context: Context?, levelContent: String?, levelId: Int): GameState {
            // Default board size
            var width = 14
            var height = 14

            // Parse entries using central parser (handles comments and line breaks)
            val entries = LevelFormatParser.parseRawEntries(levelContent ?: "")

            // First pass: extract board dimensions
            for (entry in entries) {
                if (entry.type == "board") {
                    val data = entry.data
                    val cleanData = if (data.startsWith(":")) data.substring(1) else data
                    val parts = cleanData.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    if (parts.size == 2) {
                        try {
                            width = parts[0].trim().toInt()
                            height = parts[1].trim().toInt()
                            Timber.d("[BOARD_SIZE_DEBUG] Level %d has board size: %dx%d", levelId, width, height)
                        } catch (e: NumberFormatException) {
                            Timber.e(e, "[BOARD_SIZE_DEBUG] Error parsing board dimensions")
                        }
                    }
                    break
                }
            }

            // Create the game state with correctly identified dimensions
            var state = GameState(width, height)

            // Track if we have at least one target
            var hasTarget = false

            // Second pass: parse all other entries
            for (entry in entries) {
                val type = entry.type
                val data = entry.data

                try {
                    // Skip board dimensions (already handled)
                    if (type == "board") continue


                    // Parse horizontal walls - new compact format (h) or legacy format (mh)
                    if (type == "h" || type == "mh") {
                        val parts =
                            data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (parts.size == 2) {
                            val x = parts[0].toInt()
                            val y = parts[1].toInt()
                            state.addHorizontalWall(x, y)
                        }
                        continue
                    }


                    // Parse vertical walls - new compact format (v) or legacy format (mv)
                    if (type == "v" || type == "mv") {
                        val parts =
                            data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (parts.size == 2) {
                            val x = parts[0].toInt()
                            val y = parts[1].toInt()
                            state.addVerticalWall(x, y)
                        }
                        continue
                    }


                    // Parse targets - new compact format (t) or legacy format (target_color)
                    if (type.startsWith("t")) {
                        var colorId = -1
                        var coords = data

                        if (type.length == 2 && type[0] == 't') {
                            // Compact format: tcolorX,Y; (e.g., tb8,7; parsed as type=tb, data=8,7)
                            // Color is second char of type
                            colorId = parseColorChar(type[1])
                        } else if (type.length == 1) {
                            // Compact format: tcolorX,Y; (e.g., t b8,7;)
                            // First char of data is color, rest is coordinates
                            if (data.length >= 2) {
                                colorId = parseColorChar(data[0])
                                coords = data.substring(1)
                            }
                        } else if (type.startsWith("target_")) {
                            // Legacy format: target_color (e.g., target_blue)
                            val colorName = type.substring(7)
                            colorId = parseColorName(colorName)
                        }

                        if (colorId >= -1) { // -1 = COLOR_MULTI, 0-4 = normal colors
                            val parts = coords.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            if (parts.size == 2) {
                                val x = parts[0].toInt()
                                val y = parts[1].toInt()
                                state.addTarget(x, y, colorId)
                                hasTarget = true
                            }
                        }
                        continue
                    }


                    // Parse robots - new compact format (r) or legacy format (robot_color)
                    if (type.startsWith("r")) {
                        var colorId = -1
                        var coords = data

                        if (type.length == 2 && type[0] == 'r') {
                            // Compact format: rcolorX,Y; (e.g., rr1,5; parsed as type=rr, data=1,5)
                            // Color is second char of type
                            colorId = parseColorChar(type[1])
                        } else if (type.length == 1) {
                            // Compact format: rcolorX,Y; (e.g., r r1,5;)
                            // First char of data is color, rest is coordinates
                            if (data.length >= 2) {
                                colorId = parseColorChar(data[0])
                                coords = data.substring(1)
                            }
                        } else if (type.startsWith("robot_")) {
                            // Legacy format: robot_color (e.g., robot_red)
                            val colorName = type.substring(6)
                            colorId = parseColorName(colorName)
                        }

                        if (colorId >= 0) {
                            val parts = coords.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            if (parts.size == 2) {
                                val x = parts[0].toInt()
                                val y = parts[1].toInt()
                                state.addRobot(x, y, colorId)
                            }
                        }
                        continue
                    }


                    // Parse predefined solution
                    if (type == "solution") {
                        state.predefinedSolution = data
                        Timber.d("[LEVEL LOADING] Found predefined solution: %s", data)
                        continue
                    }


                    // Parse predefined number of moves
                    if (type == "num_moves") {
                        try {
                            val numMoves = data.toInt()
                            state.predefinedNumMoves = numMoves
                            Timber.d("[LEVEL LOADING] Found predefined num_moves: %d", numMoves)
                        } catch (e: NumberFormatException) {
                            Timber.e(e, "[LEVEL LOADING] Error parsing num_moves: %s", data)
                        }
                        continue
                    }
                } catch (e: NumberFormatException) {
                    Timber.e(e, "[LEVEL LOADING] Error parsing entry %s:%s", type, data)
                }
            }


            // If no target was found, throw an exception
            // This prevents the NullPointerException in the solver
            if (!hasTarget && !state.gameElements.isEmpty()) {
//            Timber.d("[LEVEL LOADING] Generated ASCII map:\n%s", roboyard.logic.solver.RRGetMap.generateAsciiMap(state.getGameElements()));

                Timber.e("[LEVEL LOADING] No target found in level")
                val t = Throwable()
                Timber.e(t, "[LEVEL LOADING] Stack trace for no target found")
                throw IllegalStateException("[LEVEL LOADING] Level has no target, cannot create a valid game state")
            }


            // Adjust robotCount to match the actual number of targets in this level
            val targets = state.targets
            if (!targets.isEmpty()) {
                state.setRobotCount(targets.size)
                Timber.d(
                    "[LEVEL LOADING] Set robotCount from %d to %d based on actual target count",
                    Preferences.robotCount,
                    targets.size
                )
            }


            // Store initial robot positions for reset functionality
            state.storeInitialRobotPositions()

            return state
        }

        /**
         * Helper method to convert color ID to single character for compact format
         */
        private fun getColorChar(colorId: Int): Char {
            when (colorId) {
                -1 -> return 'm' // multi (COLOR_MULTI)
                0 -> return 'r' // red
                1 -> return 'g' // green
                2 -> return 'b' // blue
                3 -> return 'y' // yellow
                4 -> return 's' // silver
                else -> return '?'
            }
        }

        /**
         * Helper method to convert single character back to color ID
         */
        private fun parseColorChar(colorChar: Char): Int {
            when (colorChar) {
                'm' -> return -1 // multi (COLOR_MULTI)
                'r' -> return 0 // red
                'g' -> return 1 // green
                'b' -> return 2 // blue
                'y' -> return 3 // yellow
                's' -> return 4 // silver
                else -> return -2 // unknown color (not COLOR_MULTI)
            }
        }

        /**
         * Helper method to convert color name string to color ID (legacy format support)
         */
        private fun parseColorName(color: String): Int {
            if (color == "red") return 0
            else if (color == "green") return 1
            else if (color == "blue") return 2
            else if (color == "yellow") return 3
            else if (color == "silver") return 4
            return -1
        }

        /**
         * Converts difficulty integer to string for the original GridGameView class
         */
        private fun difficultyIntToString(difficulty: Int): String {
            when (difficulty) {
                Constants.DIFFICULTY_BEGINNER -> return "Beginner"
                Constants.DIFFICULTY_ADVANCED -> return "Intermediate"
                Constants.DIFFICULTY_INSANE -> return "Advanced"
                Constants.DIFFICULTY_IMPOSSIBLE -> return "Expert"
                else -> return "Beginner"
            }
        }

        /**
         * Create a random game state
         */
        @JvmStatic
        fun createRandom(): GameState {
            // Set the global difficulty level first so difficulty is consistent
            val difficultyString: String = difficultyIntToString(Preferences.difficulty)


            // Log initial board size and requested size
            Timber.tag(TAG)
                .d("[BOARD_SIZE_DEBUG] createRandom called with size: " + Preferences.boardSizeX + "x" + Preferences.boardSizeY)
            Timber.tag(TAG).d(
                "[BOARD_SIZE_DEBUG] Current board size from Preferences: " +
                        Preferences.boardSizeX + "x" + Preferences.boardSizeY
            )


            // Save current board dimensions and set them for game generation
            // Ensure board dimensions are never zero to prevent ArrayIndexOutOfBoundsException
            var boardSizeX = Preferences.boardSizeWidth
            var boardSizeY = Preferences.boardSizeHeight


            // Safety check: ensure board dimensions are valid (at least 4x4)
            if (boardSizeX < 4 || boardSizeY < 4) {
                Timber.tag(TAG).e(
                    "[BOARD_SIZE_DEBUG] Invalid board dimensions: %dx%d, using default 16x16",
                    boardSizeX,
                    boardSizeY
                )
                boardSizeX = 16
                boardSizeY = 16


                // Also update the Preferences to fix the issue permanently
                Preferences.setBoardSize(boardSizeX, boardSizeY)
            }


            // Board size is now managed via Preferences only, removed MainActivity dependency
            // Log the board size being used for map generation
            Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Using board size: %dx%d", boardSizeX, boardSizeY)


            // Create new game state with specified dimensions
            val state = GameState(boardSizeX, boardSizeY)


            // Store the current difficulty level in the game state for savegame restoration
            state.difficulty = Preferences.difficulty
            Timber.tag(TAG).d("[DIFFICULTY] Set game difficulty to %d", Preferences.difficulty)

            // Use MapGenerator instead of directly using GameLogic
            Timber.tag(TAG).d(
                "[BOARD_SIZE_DEBUG] Creating MapGenerator with dimensions: " +
                        boardSizeX + "x" + boardSizeY
            )

            // Create MapGenerator instance
            val mapGenerator = MapGenerator()


            val robotCountFromPrefs = Preferences.robotCount
            val targetColorsFromPrefs = Preferences.targetColors

            state.robotCount = robotCountFromPrefs
            state.targetColorsCount = targetColorsFromPrefs


            // Pass the values to the MapGenerator
            mapGenerator.robotCount = state.robotCount
            mapGenerator.targetColors = state.targetColorsCount

            Timber.tag(TAG).d(
                "[PREFERENCES] Using robotCount=%d, targetColors=%d from static Preferences",
                state.robotCount, state.targetColorsCount
            )


            // Generate a new game map
            val gridElements = mapGenerator.generatedGameMap

            Timber.tag(TAG)
                .d("[BOARD_SIZE_DEBUG] MapGenerator generated " + gridElements?.size + " grid elements")

            if (gridElements == null) {
                Timber.tag(TAG).e("[BOARD_SIZE_DEBUG] MapGenerator returned null gridElements!")
                return state
            }

            // Process grid elements to create game state
            for (element in gridElements) {
                if (element == null) continue
                val type = element.type ?: continue
                val x = element.x
                val y = element.y

                // Handle all wall types
                if (type == "mh") {
                    // Add horizontal wall (between rows)
                    state.addHorizontalWall(x, y)
                } else if (type == "mv") {
                    // Add vertical wall (between columns)
                    state.addVerticalWall(x, y)
                } else if (type == "target_red") {
                    // Add target as a GameElement (TYPE_TARGET) and also mark the cell as a target
                    state.addTarget(x, y, Constants.COLOR_PINK)
                    Timber.d(
                        "[COLOR_MAPPING] Added %s target at (%d,%d) with color ID %d (%s)",
                        type,
                        x,
                        y,
                        Constants.COLOR_PINK,
                        GameLogic.getColorName(Constants.COLOR_PINK, true)
                    )
                } else if (type == "target_green") {
                    state.addTarget(x, y, Constants.COLOR_GREEN)
                    Timber.d(
                        "[COLOR_MAPPING] Added green target at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_GREEN
                    )
                } else if (type == "target_blue") {
                    state.addTarget(x, y, Constants.COLOR_BLUE)
                    Timber.d(
                        "[COLOR_MAPPING] Added blue target at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_BLUE
                    )
                } else if (type == "target_yellow") {
                    state.addTarget(x, y, Constants.COLOR_YELLOW)
                    Timber.d(
                        "[COLOR_MAPPING] Added yellow target at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_YELLOW
                    )
                } else if (type == "target_silver") {
                    state.addTarget(x, y, Constants.COLOR_SILVER)
                    Timber.d(
                        "[COLOR_MAPPING] Added silver target at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_SILVER
                    )
                } else if (type == "target_multi") {
                    // Multi-color target - we'll use pink as default
                    state.addTarget(x, y, Constants.COLOR_MULTI)
                    Timber.d(
                        "[COLOR_MAPPING] Added multi-color target at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_PINK
                    )
                } else if (type == "robot_red") {
                    // Both pink and red map to COLOR_PINK (index 0) - pink is the actual game color, red is used in solver
                    state.addRobot(x, y, Constants.COLOR_PINK)
                    Timber.d(
                        "[COLOR_MAPPING] Added %s robot at (%d,%d) with color ID %d (%s)",
                        type,
                        x,
                        y,
                        Constants.COLOR_PINK,
                        GameLogic.getColorName(Constants.COLOR_PINK, true)
                    )
                } else if (type == "robot_green") {
                    state.addRobot(x, y, Constants.COLOR_GREEN)
                    Timber.d(
                        "[COLOR_MAPPING] Added green robot at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_GREEN
                    )
                } else if (type == "robot_blue") {
                    state.addRobot(x, y, Constants.COLOR_BLUE)
                    Timber.d(
                        "[COLOR_MAPPING] Added blue robot at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_BLUE
                    )
                } else if (type == "robot_yellow") {
                    state.addRobot(x, y, Constants.COLOR_YELLOW)
                    Timber.d(
                        "[COLOR_MAPPING] Added yellow robot at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_YELLOW
                    )
                } else if (type == "robot_silver") {
                    state.addRobot(x, y, Constants.COLOR_SILVER)
                    Timber.d(
                        "[COLOR_MAPPING] Added silver robot at (%d,%d) with color ID %d",
                        x,
                        y,
                        Constants.COLOR_SILVER
                    )
                }
            }

            state.levelName = "Random Game " + System.currentTimeMillis() % 1000


            // Store initial robot positions for reset functionality
            state.storeInitialRobotPositions()
            Timber.d("[ROBOTS] Stored initial robot positions for new random game")


            // Generate a unique map ID for this random game
            val stateGridElements = state.gridElements
            val uniqueId = MapIdGenerator.generateUniqueId(stateGridElements)
            state.uniqueMapId = uniqueId
            state.levelName = uniqueId // Use the unique ID as the level name

            Timber.d("GameState: Created random game with unique ID: %s", uniqueId)

            return state
        }

        /**
         * Create a GameState from a list of GridElements (e.g. from ASCII map parsing).
         * Determines board dimensions from the elements automatically.
         * 
         * @param gridElements The grid elements to create the state from
         * @return A new GameState populated with the given elements
         */
        @JvmStatic
        fun createFromGridElements(gridElements: java.util.ArrayList<GridElement>): GameState {
            // Determine board dimensions from elements.
            // Walls (mh/mv) can have coordinates equal to the board size (border walls),
            // so we use non-wall elements (robots, targets) to determine the playable area,
            // and wall coordinates only as fallback.
            var maxCellX = -1
            var maxCellY = -1
            var maxWallX = 0
            var maxWallY = 0
            for (element in gridElements) {
                val type = element.type
                if (type == "mh" || type == "mv") {
                    maxWallX = max(maxWallX, element.x)
                    maxWallY = max(maxWallY, element.y)
                } else {
                    maxCellX = max(maxCellX, element.x)
                    maxCellY = max(maxCellY, element.y)
                }
            }
            // Border walls sit at coordinate = boardSize (e.g. right wall at x=18 for 18-wide board).
            // So boardSize = max wall coordinate. Cell elements are 0-indexed within the board.
            val width = max(maxWallX, if (maxCellX >= 0) maxCellX + 1 else 0)
            val height = max(maxWallY, if (maxCellY >= 0) maxCellY + 1 else 0)
            Timber.d(
                "[ASCII_IMPORT] Board size from elements: %dx%d (maxCell=%d,%d maxWall=%d,%d)",
                width, height, maxCellX, maxCellY, maxWallX, maxWallY
            )

            val state = GameState(width, height)

            for (element in gridElements) {
                val type = element.type ?: continue
                val x = element.x
                val y = element.y

                if (type == "mh") {
                    state.addHorizontalWall(x, y)
                } else if (type == "mv") {
                    state.addVerticalWall(x, y)
                } else if (type == "target_red") {
                    state.addTarget(x, y, Constants.COLOR_PINK)
                } else if (type == "target_green") {
                    state.addTarget(x, y, Constants.COLOR_GREEN)
                } else if (type == "target_blue") {
                    state.addTarget(x, y, Constants.COLOR_BLUE)
                } else if (type == "target_yellow") {
                    state.addTarget(x, y, Constants.COLOR_YELLOW)
                } else if (type == "target_silver") {
                    state.addTarget(x, y, Constants.COLOR_SILVER)
                } else if (type == "target_multi") {
                    state.addTarget(x, y, Constants.COLOR_MULTI)
                } else if (type == "robot_red" || type == "robot_pink") {
                    state.addRobot(x, y, Constants.COLOR_PINK)
                } else if (type == "robot_green") {
                    state.addRobot(x, y, Constants.COLOR_GREEN)
                } else if (type == "robot_blue") {
                    state.addRobot(x, y, Constants.COLOR_BLUE)
                } else if (type == "robot_yellow") {
                    state.addRobot(x, y, Constants.COLOR_YELLOW)
                } else if (type == "robot_silver") {
                    state.addRobot(x, y, Constants.COLOR_SILVER)
                }
            }

            state.levelName = "Imported Map"
            state.storeInitialRobotPositions()

            val stateGridElements = state.gridElements
            val uniqueId = MapIdGenerator.generateUniqueId(stateGridElements)
            state.uniqueMapId = uniqueId

            Timber.d(
                "[ASCII_IMPORT] Created GameState %dx%d with %d elements, ID: %s",
                width, height, stateGridElements.size, uniqueId
            )
            return state
        }
    }
}
