package roboyard.logic.solver

import driftingdroids.model.Board
import driftingdroids.model.Board.Goal
import roboyard.logic.core.Constants
import roboyard.logic.core.GameLogic.Companion.getColor
import roboyard.logic.core.GameLogic.Companion.getColorName
import roboyard.logic.core.GridElement
import roboyard.logic.core.WallModel.Companion.fromGridElements
import roboyard.logic.core.WallType
import roboyard.ui.activities.MainActivity
import timber.log.Timber
import kotlin.math.max

/**
 * Utility class for converting between Roboyard's game elements and DriftingDroids board format.
 * This class serves as the main conversion layer between the two systems, handling:
 * - Grid cell conversion
 * - Robot piece placement
 * - Wall and obstacle mapping
 * - target position translation
 * 
 * @see Board
 * 
 * @see roboyard.eclabs.GridElement
 */
object RRGetMap {
    /**
     * Create a virtual world in memory for the DD solver (IDDFS)
     * adds all game elements to that virtual world so the solver can solve it with the current data
     * 
     * @param gridElements game elements
     * @param pieces
     * @return
     */
    fun createDDWorld(gridElements: ArrayList<GridElement>, pieces: Array<RRPiece?>): Board {
        // Find the board dimensions from the GridElements
        var maxX = 0
        var maxY = 0
        for (element in gridElements) {
            val gridElement = element
            maxX = max(maxX, gridElement.x)
            maxY = max(maxY, gridElement.y)
        }


        // Add 1 to get width/height from max coordinates
        val boardWidth = maxX + 1
        val boardHeight = maxY + 1


        // Log the actual board dimensions we're using
        Timber.d("[SOLUTION_SOLVER] createDDWorld: Using board dimensions " + boardWidth + "x" + boardHeight + " (MainActivity dimensions: " + MainActivity.boardSizeX + "x" + MainActivity.boardSizeY + "")

        // Generate the ASCII map for debugging
        val asciiMap = generateAsciiMap(gridElements)
        Timber.d(asciiMap)
        // Create the board with dimensions from GridElements
        // IMPORTANT: Use boardWidth/boardHeight calculated from GridElements, not MainActivity dimensions
        // The GridElements may have coordinates up to boardWidth-1, so we need a board of that size
        // Using MainActivity dimensions caused walls at x=12 to wrap around to x=0 of the next row
        val board = Board.createBoardFreestyle(null, boardWidth, boardHeight, Constants.NUM_ROBOTS)
        board.removeGoals()

        Timber.d(
            "[SOLUTION_SOLVER] Board created with width=%d, height=%d",
            board.width,
            board.height
        )

        // Color mappings for robots and targets
        val colors: MutableMap<String?, Int?> = HashMap<String?, Int?>()

        // Robot and target indices for game logic
        // Using Constants class values
        colors.put("robot_red", Constants.COLOR_PINK) // Constants.COLOR_PINK
        colors.put("robot_green", Constants.COLOR_GREEN) // Constants.COLOR_GREEN
        colors.put("robot_blue", Constants.COLOR_BLUE) // Constants.COLOR_BLUE
        colors.put("robot_yellow", Constants.COLOR_YELLOW) // Constants.COLOR_YELLOW
        colors.put("robot_silver", Constants.COLOR_SILVER)

        colors.put("target_red", Constants.COLOR_PINK) // Constants.COLOR_PINK
        colors.put("target_green", Constants.COLOR_GREEN) // Constants.COLOR_GREEN
        colors.put("target_blue", Constants.COLOR_BLUE) // Constants.COLOR_BLUE
        colors.put("target_yellow", Constants.COLOR_YELLOW) // Constants.COLOR_YELLOW
        colors.put("target_silver", Constants.COLOR_SILVER) // Constants.COLOR_SILVER
        // Explicitly setting multi-color target to use wildcard value (-1)
        // This tells the DriftingDroids solver that any robot can match this target
        colors.put("target_multi", Constants.COLOR_MULTI)

        var robotCounter = 0
        var targetFound = false
        val targetInfoList: MutableList<IntArray> = ArrayList<IntArray>() // [position, colorIndex]


        // CRITICAL CHANGE: First process all non-wall elements (targets, robots) before walls
        // This ensures targets get priority over walls at the same position
        for (element in gridElements) {
            val gridElement = element
            val type = gridElement.type
            val x = gridElement.x
            val y = gridElement.y
            val position = y * board.width + x


            // Skip walls - we'll handle them separately after targets
            if (type == "mh" || type == "mv") {
                continue
            }


            // Handle targets (both colored and multi-colored)
            if (type == "target_red" || type == "target_green" ||
                type == "target_blue" || type == "target_yellow" ||
                type == "target_silver" || type == "target_multi" ||
                type == "target_pink"
            ) { // Added target_pink here

                val targetColor: Int = colors.getOrDefault(type, Constants.COLOR_PINK)!!
                board.addGoal(position, targetColor, 1)
                targetFound = true
                targetInfoList.add(intArrayOf(position, targetColor))


                // Set this as the active target
                board.setGoal(position)
                Timber.d(
                    "[SOLUTION_SOLVER_TARGET] Setting goal at position %d (%d,%d) for robot color %d",
                    position, x, y, targetColor
                )
            } else if (type!!.startsWith("target_")) {
                Timber.w("[SOLUTION_SOLVER_TARGET] Unknown target type: %s", type)
            }


            // Handle robots of different colors
            if (type == "robot_red" || type == "robot_green" ||
                type == "robot_blue" || type == "robot_yellow" ||
                type == "robot_silver" || type == "robot_pink"
            ) { // Added robot_pink

                // Get the color index from the GameLogic

                val colorIndex: Int =
                    colors.getOrDefault(type, robotCounter % Constants.NUM_ROBOTS)!!


                // Map color indices to valid piece array indices (0-3)
                // This is needed because the solver only supports 4 robots max
                val mappedIndex: Int
                if (colorIndex >= 0 && colorIndex < Constants.NUM_ROBOTS) {
                    // Standard robots (0-3) use their own indices
                    mappedIndex = colorIndex
                } else {
                    // For extra robots (indices >= Constants.NUM_ROBOTS), map them to one of the standard robots
                    // This is a temporary solution - we log a warning to highlight the issue
                    // PINK = 0, GREEN = 1, BLUE = 2, YELLOW = 3, SILVER = 4
                    mappedIndex = robotCounter % Constants.NUM_ROBOTS
                    Timber.w(
                        "[COLOR_MAPPING] Mapped non-standard robot color %d (%s) to standard color index %d",
                        colorIndex, getColorName(colorIndex, true), mappedIndex
                    )
                }

                Timber.d(
                    "[HINT_SYSTEM] Creating robot piece for %s with colorIndex=%d (mapped to %d) with RGB color %d",
                    type, colorIndex, mappedIndex, getColor(type)
                )

                pieces[mappedIndex] = RRPiece(x, y, colorIndex, robotCounter)
                robotCounter++
            }
        }

        // Now process walls - AFTER we've identified target positions
        // Create a wall model from the grid elements
        val wallModel = fromGridElements(gridElements, boardWidth, boardHeight)


        // Process each wall in the model and set it on the board
        // to understand setWall: driftingdroids solver uses "N" and "W" to represent horizontal and vertical walls
        // e.g. walls[direction][x + y * this.width] = true;
        // direction is "N" for horizontal and "W" for vertical
        // the y position is stored in the same key as multiple of the width of the board
        for (wall in wallModel.getWalls()) {
            val x = wall!!.x
            val y = wall.y
            val position = y * board.width + x

            if (wall.type == WallType.HORIZONTAL) {
                board.setWall(
                    position,
                    "N",
                    true
                ) // treated as "N" of the current field in driftingdroids solver

                Timber.d(
                    "[SOLUTION_SOLVER] Setting horizontal wall at position %d (x=%d, y=%d)",
                    position,
                    x,
                    y
                )
            } else if (wall.type == WallType.VERTICAL) {
                board.setWall(
                    position,
                    "W",
                    true
                ) // treated as "W" of the current field in driftingdroids solver

                Timber.d(
                    "[SOLUTION_SOLVER] Setting vertical wall at position %d (x=%d, y=%d)",
                    position,
                    x,
                    y
                )
            }
        }


        // Force outer walls to be present - essential for the solver
        var missingWallCountHorizontal = 0
        var missingWallCountVertical = 0

        for (x in 0..<board.width) {
            // Top border
            val topPosition = 0 + x
            if (!board.isWall(topPosition, Constants.NORTH)) {
                board.setWall(topPosition, "N", true)
                Timber.w(
                    "[SOLUTION_SOLVER][WALLS] Adding missing top horizontal border wall at position (%d,0)",
                    x
                )
                missingWallCountHorizontal++
            }
            // Bottom border
            val bottomWallY = (board.height - 1) * board.width
            val bottomPosition = bottomWallY + x
            if (!board.isWall(bottomPosition, Constants.SOUTH)) {
                board.setWall(bottomPosition, "N", true)
                Timber.w(
                    "[SOLUTION_SOLVER][WALLS] Adding missing bottom horizontal border wall at position (%d,%d)",
                    x,
                    board.height - 1
                )
                missingWallCountHorizontal++
            }
        }

        for (y in 0..<board.height) {
            // Left vertical border
            val verticalWallY = y * board.width
            val leftPosition = 0 + verticalWallY
            if (!board.isWall(leftPosition, Constants.WEST)) {
                board.setWall(leftPosition, "W", true)
                Timber.w(
                    "[SOLUTION_SOLVER][WALLS] Adding missing left vertical border wall at position (0,%d)",
                    y
                )
                missingWallCountVertical++
            }
            // Right border
            val rightWallX = board.width - 1
            val rightPosition = rightWallX + verticalWallY
            if (!board.isWall(rightPosition, Constants.EAST)) {
                board.setWall(rightPosition, "W", true)
                Timber.w(
                    "[SOLUTION_SOLVER][WALLS] Adding missing right vertical border wall at position (%d,%d)",
                    board.width - 1,
                    y
                )
                missingWallCountVertical++
            }
        }

        if (missingWallCountHorizontal > 0 || missingWallCountVertical > 0) {
            Timber.w(
                "[SOLUTION_SOLVER][WALLS] Added %d missing outer walls to ensure solver stability, horizontal:%d, vertical:%d",
                missingWallCountHorizontal + missingWallCountVertical,
                missingWallCountHorizontal,
                missingWallCountVertical
            )
        }

        // Set robot positions on the board
        // Must be exactly 4 robots as the solver expects this
        for (i in 0..<Constants.NUM_ROBOTS) {
            // Check if the piece exists at this position
            if (pieces[i] == null) {
                Timber.e(
                    "[ROBOT_MAPPING][ERRROR] Fatal: Missing robot at position %d. Creating a dummy robot at (0,0)",
                    i
                )
                // Create a dummy piece at position (0,0) as a fallback
                // This ensures the solver can proceed but may not produce correct solutions
                pieces[i] = RRPiece(0, 0, i, i)
            }

            val position = pieces[i]!!.y * board.width + pieces[i]!!.x
            Timber.d(
                "[ROBOT_MAPPING] Setting robot %d at board position %d (x=%d, y=%d)",
                i, position, pieces[i]!!.x, pieces[i]!!.y
            )
            board.setRobot(i, position, false)

            // Verify that setRobot succeeded
            if (!board.setRobot(i, position, false)) {
                Timber.e(
                    "[ROBOT_MAPPING][ERRROR] FATAL: Could not set robot %d at position %d (%d,%d). Position may be occupied or invalid.",
                    i, position, pieces[i]!!.x, pieces[i]!!.y
                )
            }
        }


        // If no target was found, throw an exception
        // This prevents the NullPointerException in Board.isSolution01()
        if (!targetFound) {
            throw RuntimeException("[SOLUTION_SOLVER] No target found in level")
        }

        // Multi-goal support: if more than 1 target found, set activeGoals
        if (targetInfoList.size > 1) {
            // Validate: multi-colored targets not allowed in multi-goal mode
            for (info in targetInfoList) {
                val color = info[1]
                if (color == Constants.COLOR_MULTI) {
                    Timber.e(
                        "[SOLUTION_SOLVER] Multi-colored target not allowed in multi-goal mode. " +
                                "Each robot must have a specific colored target. It was added target with ID " + color
                    )
                }
            }

            val activeGoals: MutableList<Goal> = ArrayList<Goal>()
            for (info in targetInfoList) {
                val pos = info[0]
                val color = info[1]
                for (g in board.getGoals()) {
                    if (g.position == pos && g.robotNumber == color) {
                        activeGoals.add(g)
                        break
                    }
                }
            }
            if (activeGoals.size > 1) {
                board.setActiveGoals(activeGoals)
                Timber.d("[SOLUTION_SOLVER] Multi-goal mode: set %d active goals", activeGoals.size)
                for (g in activeGoals) {
                    Timber.d(
                        "[SOLUTION_SOLVER]   Goal: robot=%d position=%d (%d,%d)",
                        g.robotNumber,
                        g.position,
                        g.position % board.width,
                        g.position / board.width
                    )
                }
            }
        }

        return board
    }

    /**
     * Generate an ASCII representation of the game board from the provided grid elements
     * @param gridElements List of grid elements to represent
     * @return ASCII string representation of the board
     */
    fun generateAsciiMap(gridElements: ArrayList<GridElement>?): String {
        if (gridElements == null || gridElements.isEmpty()) {
            return "[ASCII_MAP] Empty or null grid elements provided."
        }


        // Determine actual board dimensions from grid elements (including border walls at +1)
        var maxX = 0
        var maxY = 0
        for (element in gridElements) {
            maxX = max(maxX, element.x)
            maxY = max(maxY, element.y)
        }
        val mapWidth = maxX + 1
        val mapHeight = maxY + 1


        // Create a double-width ASCII map to separate walls from robots/targets
        // For each grid cell (x,y), we'll use:
        // - asciiMap[x*2][y] for vertical walls
        // - asciiMap[x*2+1][y] for robots and targets
        val asciiMap = Array<Array<String?>?>(2 * mapWidth + 1) { arrayOfNulls<String>(mapHeight) }


        // Track cells that have both a robot and a horizontal wall
        val cellContents: MutableMap<String?, String?> = HashMap<String?, String?>()


        // First pass: collect all elements by position to handle overlaps
        for (element in gridElements) {
            val x = element.x
            val y = element.y
            val key = x.toString() + "," + y

            if (element.type == "mh") {
                // Remember this position has a horizontal wall
                val prevContent = cellContents.getOrDefault(key, "")
                cellContents.put(key, prevContent + "mh,")
            } else if (element.type == "mv") {
                // Vertical walls go in their own position
                asciiMap[x * 2]!![y] = "|"
            } else {
                // Remember this position has a robot or target
                val prevContent = cellContents.getOrDefault(key, "")
                cellContents.put(key, prevContent + element.type + ",")
            }
        }


        // Process collected cell contents to handle overlaps
        for (entry in cellContents.entries) {
            val coords =
                entry.key!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val x = coords[0].toInt()
            val y = coords[1].toInt()
            val contents: String = entry.value!!

            val hasHorizontalWall = contents.contains("mh,")


            // Handle special cases for robot + horizontal wall combinations
            if (hasHorizontalWall) {
                if (contents.contains("robot_")) {
                    // robots with horizontal wall - use combining overline
                    if (contents.contains("robot_yellow")) {
                        asciiMap[x * 2 + 1]!![y] = "y̅" // y with overline
                    } else if (contents.contains("robot_red")) {
                        asciiMap[x * 2 + 1]!![y] = "r̅" // r with overline
                    } else if (contents.contains("robot_pink")) {
                        asciiMap[x * 2 + 1]!![y] = "p̅" // p with overline
                    } else if (contents.contains("robot_blue")) {
                        asciiMap[x * 2 + 1]!![y] = "b̅" // b with overline
                    } else if (contents.contains("robot_green")) {
                        asciiMap[x * 2 + 1]!![y] = "g̅" // g with overline
                    } else if (contents.contains("robot_silver")) {
                        asciiMap[x * 2 + 1]!![y] = "s̅" // s with overline
                    } else {
                        // Unknown robot
                        asciiMap[x * 2 + 1]!![y] = "u̅" // u with overline
                    }
                } else if (contents.contains("target_")) {
                    // targets with horizontal wall
                    if (contents.contains("target_yellow")) {
                        asciiMap[x * 2 + 1]!![y] = "Y̅" // Y with overline
                    } else if (contents.contains("target_red")) {
                        asciiMap[x * 2 + 1]!![y] = "R̅" // R with overline
                    } else if (contents.contains("target_pink")) {
                        asciiMap[x * 2 + 1]!![y] = "P̅" // P with overline
                    } else if (contents.contains("target_blue")) {
                        asciiMap[x * 2 + 1]!![y] = "B̅" // B with overline
                    } else if (contents.contains("target_green")) {
                        asciiMap[x * 2 + 1]!![y] = "G̅" // G with overline
                    } else if (contents.contains("target_multi")) {
                        asciiMap[x * 2 + 1]!![y] = "M̅" // M with overline
                    } else if (contents.contains("target_silver")) {
                        asciiMap[x * 2 + 1]!![y] = "S̅" // S with overline
                    } else {
                        // Unknown target
                        asciiMap[x * 2 + 1]!![y] = "U̅" // U with overline
                    }
                } else {
                    // Just a horizontal wall an overline
                    asciiMap[x * 2 + 1]!![y] = "‾" // UTF8 overline
                }
            } else {
                // No horizontal wall, just set the character based on element type
                if (contents.contains("robot_")) {
                    if (contents.contains("robot_yellow")) {
                        asciiMap[x * 2 + 1]!![y] = "y"
                    } else if (contents.contains("robot_red")) {
                        asciiMap[x * 2 + 1]!![y] = "r"
                    } else if (contents.contains("robot_pink")) {
                        asciiMap[x * 2 + 1]!![y] = "p"
                    } else if (contents.contains("robot_blue")) {
                        asciiMap[x * 2 + 1]!![y] = "b"
                    } else if (contents.contains("robot_green")) {
                        asciiMap[x * 2 + 1]!![y] = "g"
                    } else if (contents.contains("robot_silver")) {
                        asciiMap[x * 2 + 1]!![y] = "s"
                    } else {
                        // Unknown robot
                        asciiMap[x * 2 + 1]!![y] = "u"
                    }
                } else if (contents.contains("target_")) {
                    if (contents.contains("target_yellow")) {
                        asciiMap[x * 2 + 1]!![y] = "Y"
                    } else if (contents.contains("target_red")) {
                        asciiMap[x * 2 + 1]!![y] = "R"
                    } else if (contents.contains("target_blue")) {
                        asciiMap[x * 2 + 1]!![y] = "B"
                    } else if (contents.contains("target_green")) {
                        asciiMap[x * 2 + 1]!![y] = "G"
                    } else if (contents.contains("target_silver")) {
                        asciiMap[x * 2 + 1]!![y] = "S"
                    } else if (contents.contains("target_multi")) {
                        asciiMap[x * 2 + 1]!![y] = "M"
                    } else {
                        // Unknown target
                        asciiMap[x * 2 + 1]!![y] = "U"
                    }
                }
            }
        }

        // Build the ASCII map as a string
        val result = StringBuilder("[ASCII_MAP] Board state:\n")


        // Add column headers (X coordinates)
        result.append("   ") // Space for row labels
        for (x in 0..<mapWidth) {
            result.append(String.format("%2d", x))
        }
        result.append("\n")


        // Add each row with row header (Y coordinate)
        for (y in 0..<mapHeight) {
            result.append(String.format("%2d ", y))

            for (x in 0..<mapWidth) {
                // Add vertical wall or space
                val vWall = if (asciiMap[x * 2]!![y] != null) asciiMap[x * 2]!![y] else " "
                result.append(vWall)


                // Add cell content or space
                val cell = if (asciiMap[x * 2 + 1]!![y] != null) asciiMap[x * 2 + 1]!![y] else "."
                result.append(cell)
            }
            result.append("\n")
        }

        return result.toString()
    }

    /**
     * Parse an ASCII map (as generated by generateAsciiMap) back into GridElement list.
     * Supports robots (r,g,b,y,p,s), targets (R,G,B,Y,P,S,M), walls (|,‾),
     * and combined robot/target + horizontal wall characters (e.g. r̅, G̅).
     * 
     * @param asciiMap The ASCII map string (with or without the "[ASCII_MAP]" prefix)
     * @return ArrayList of GridElement, or null if parsing failed
     */
    @JvmStatic
    fun parseAsciiMap(asciiMap: String?): ArrayList<GridElement>? {
        if (asciiMap == null || asciiMap.isEmpty()) {
            Timber.e("[ASCII_PARSE] Input is null or empty")
            return null
        }

        val elements = ArrayList<GridElement>()

        // Split into lines and find the data lines (skip header/prefix lines)
        val allLines = asciiMap.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val dataLines: MutableList<String> = ArrayList<String>()
        var foundHeader = false

        for (line in allLines) {
            val trimmed = line.trim { it <= ' ' }
            // Skip empty lines and the "[ASCII_MAP]" prefix line
            if (trimmed.isEmpty() || trimmed.startsWith("[ASCII_MAP]")) continue
            // Skip column header line (starts with spaces then digits)
            if (!foundHeader && trimmed.matches("^\\s*\\d.*".toRegex())) {
                // Could be column header or data line - check if it starts with a number followed by space
                // Column header: "    0 1 2 3 ..."  Data line: " 0 |. . ..."
                // Column headers have many numbers in sequence
                val parts =
                    trimmed.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var allNumbers = true
                for (p in parts) {
                    if (!p.isEmpty() && !p.matches("\\d+".toRegex())) {
                        allNumbers = false
                        break
                    }
                }
                if (allNumbers && parts.size > 3) {
                    foundHeader = true
                    continue  // Skip column header
                }
            }
            // Data lines start with a row number
            if (trimmed.matches("^\\d+\\s.*".toRegex())) {
                dataLines.add(trimmed)
            }
        }

        if (dataLines.isEmpty()) {
            Timber.e("[ASCII_PARSE] No data lines found in ASCII map")
            return null
        }

        Timber.d("[ASCII_PARSE] Found %d data lines", dataLines.size)

        // Map characters to element types
        // Lowercase = robots, Uppercase = targets
        val robotMap: MutableMap<Char?, String?> = HashMap<Char?, String?>()
        robotMap.put('r', "robot_red")
        robotMap.put('g', "robot_green")
        robotMap.put('b', "robot_blue")
        robotMap.put('y', "robot_yellow")
        robotMap.put('p', "robot_pink")
        robotMap.put('s', "robot_silver")

        val targetMap: MutableMap<Char?, String?> = HashMap<Char?, String?>()
        targetMap.put('R', "target_red")
        targetMap.put('G', "target_green")
        targetMap.put('B', "target_blue")
        targetMap.put('Y', "target_yellow")
        targetMap.put('P', "target_pink")
        targetMap.put('S', "target_silver")
        targetMap.put('M', "target_multi")

        for (line in dataLines) {
            // Extract row number (first number in the line)
            val spaceIdx = line.indexOf(' ')
            if (spaceIdx < 0) continue
            val y: Int
            try {
                y = line.substring(0, spaceIdx).trim { it <= ' ' }.toInt()
            } catch (e: NumberFormatException) {
                continue
            }

            // The rest of the line contains cell data
            // Format: for each cell x, two characters: vWall + cellContent
            // vWall = "|" or " ", cellContent = robot/target/wall/"."
            val cellData = line.substring(spaceIdx + 1)

            var x = 0
            var i = 0
            while (i < cellData.length) {
                // First char: vertical wall indicator
                val vWallChar = cellData.get(i)
                i++

                if (vWallChar == '|') {
                    elements.add(GridElement(x, y, "mv"))
                }

                if (i >= cellData.length) break

                // Second char: cell content
                val cellChar = cellData.get(i)
                i++

                // Check for combining overline (U+0305) following the character
                var hasOverline = false
                if (i < cellData.length && cellData.get(i) == '\u0305') {
                    hasOverline = true
                    i++ // consume the combining character
                }

                if (hasOverline) {
                    // Character + overline = element + horizontal wall
                    elements.add(GridElement(x, y, "mh"))
                }

                if (cellChar == '\u203E' || cellChar == '‾') {
                    // Standalone overline = horizontal wall only
                    elements.add(GridElement(x, y, "mh"))
                } else if (robotMap.containsKey(cellChar)) {
                    elements.add(GridElement(x, y, robotMap.get(cellChar)))
                    if (!hasOverline) {
                        // no extra action needed
                    }
                } else if (targetMap.containsKey(cellChar)) {
                    elements.add(GridElement(x, y, targetMap.get(cellChar)))
                } else if (cellChar == '.' || cellChar == ' ') {
                    // Empty cell, nothing to add
                }

                // else: unknown character, skip
                x++
            }
        }

        Timber.d("[ASCII_PARSE] Parsed %d elements from ASCII map", elements.size)
        return elements
    }
}
