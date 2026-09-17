package roboyard.logic.managers

import roboyard.logic.core.GameState
import roboyard.logic.network.urlEncodeUtf8
import roboyard.logic.util.RLog

/**
 * Shared helpers to convert save-file data into the roboyard.z11.de share format.
 * Ported from Android SaveGameFragment so Desktop/Compose can share maps the same way.
 */
object ShareMapHelper {
    private val log = RLog.tag("SHARE")

    /** Result of parsing save data for sharing. Public fields for Java test access. */
    class ShareParseResult(
        @JvmField val formattedData: String,
        @JvmField val mapName: String,
        @JvmField val width: Int,
        @JvmField val height: Int,
        @JvmField val wallCount: Int,
        @JvmField val targetCount: Int,
        @JvmField val robotCount: Int,
        @JvmField val numMoves: Int
    )

    /** Check if a wall is a border wall. */
    @JvmStatic
    fun isBorderWall(x: Int, y: Int, width: Int, height: Int): Boolean {
        // Note: In Roboyard, walls are indexed starting at -1
        // Walls at x=0 or y=0 are NOT border walls, they are valid game elements
        // Only consider walls at the absolute edge (which would be at -1 if walls were indexed properly)
        // Since we don't have access to -1 coordinates here, we don't filter any walls
        return false // Don't filter out any walls - we need them all
    }

    /** Get the color name for a robot color constant. */
    @JvmStatic
    fun getRobotColorName(color: Int): String {
        return when (color) {
            -1 -> "multi"    // Multicolor target (any robot can reach it)
            0 -> "pink"      // Constants.COLOR_PINK
            1 -> "green"     // Constants.COLOR_GREEN
            2 -> "blue"      // Constants.COLOR_BLUE
            3 -> "yellow"    // Constants.COLOR_YELLOW
            4 -> "silver"    // Constants.COLOR_SILVER
            5 -> "red"       // Constants.COLOR_RED
            6 -> "brown"     // Constants.COLOR_BROWN
            7 -> "orange"    // Constants.COLOR_ORANGE
            8 -> "white"     // Constants.COLOR_WHITE
            9 -> "multi"     // Constants.COLOR_MULTI (for multi target)
            else -> "unknown"
        }
    }

    /** Inverse of GameState.getColorChar - maps compact color char to share-URL color name. */
    @JvmStatic
    fun getColorNameFromChar(colorChar: Char): String {
        return when (colorChar) {
            'm' -> "multi"
            'r' -> "red"
            'g' -> "green"
            'b' -> "blue"
            'y' -> "yellow"
            's' -> "silver"
            else -> "unknown"
        }
    }

    /** Extract MAPNAME from the metadata header line of save data. */
    @JvmStatic
    fun getMapNameFromSaveData(saveData: String?): String {
        try {
            if (saveData != null && saveData.isNotEmpty()) {
                val lines = saveData.split("\n")
                if (lines.isNotEmpty() && lines[0].startsWith("#")) {
                    val metadata = lines[0].substring(1).split(";")
                    for (item in metadata) {
                        if (item.startsWith("MAPNAME:")) {
                            return item.substring("MAPNAME:".length)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e, "[SHARE] Error getting map name from save data")
        }
        return "Shared Map"
    }

    /**
     * Parse wall data from save file format and add to formatted data.
     * Used by buildMapDataForShare (section-based WALLS: format).
     */
    private fun parseAndAddWalls(
        lines: Array<String>, width: Int, height: Int,
        formattedData: StringBuilder, wallEntries: MutableSet<String>
    ) {
        var inWallsSection = false

        for (line in lines) {
            val trimmedLine = line.trim()

            if (trimmedLine == "WALLS:") {
                inWallsSection = true
                continue
            } else if (trimmedLine == "TARGET_SECTION:" || trimmedLine == "ROBOTS:" || trimmedLine == "BOARD:") {
                inWallsSection = false
                continue
            }

            if (inWallsSection && trimmedLine.isNotEmpty()) {
                val parts = trimmedLine.split(",")
                if (parts.size >= 3) {
                    try {
                        if (parts[0] == "H" || parts[0] == "V") {
                            // Format 'H,y,x' or 'V,y,x'
                            if (parts[1].matches(Regex("\\d+")) && parts[2].matches(Regex("\\d+"))) {
                                val y = parts[1].toInt()
                                val x = parts[2].replace(";", "").toInt()
                                if (isBorderWall(x, y, width, height)) continue

                                val wallEntry = if (parts[0] == "H") "\nmh$y,$x;" else "\nmv$y,$x;"
                                if (wallEntries.add(wallEntry)) {
                                    formattedData.append(wallEntry)
                                }
                            }
                        } else if (parts[0].matches(Regex("\\d+")) && parts[1].matches(Regex("\\d+"))) {
                            // Format 'x,y,direction'
                            val x = parts[0].toInt()
                            val y = parts[1].toInt()
                            val direction = parts[2].replace(";", "")
                            if (isBorderWall(x, y, width, height)) continue

                            val wallEntry = if (direction == "h") "\nmh$y,$x;" else "\nmv$y,$x;"
                            if (wallEntries.add(wallEntry)) {
                                formattedData.append(wallEntry)
                            }
                        }
                    } catch (e: NumberFormatException) {
                        log.e(e, "[SHARE] Error parsing wall: %s", trimmedLine)
                    }
                }
            }
        }
    }

    /**
     * Build map data string for sharing from raw save-file content.
     * Handles the section-based format (WALLS:, ROBOTS:, TARGET_SECTION:).
     * Returns null when the data cannot be parsed.
     */
    @JvmStatic
    fun buildMapDataForShare(saveData: String?): String? {
        try {
            if (saveData == null || saveData.isEmpty()) {
                return null
            }

            val formattedData = StringBuilder()
            val lines = saveData.split("\n").toTypedArray()

            var mapName = "Shared Map"
            var width = 12
            var height = 12
            var numMoves = 0

            // Extract metadata from first line
            if (lines.isNotEmpty() && lines[0].startsWith("#")) {
                val metadata = lines[0].substring(1).split(";")
                for (item in metadata) {
                    if (item.startsWith("MAPNAME:")) {
                        mapName = item.substring("MAPNAME:".length)
                    } else if (item.startsWith("SIZE:")) {
                        val size = item.substring("SIZE:".length).split(",")
                        if (size.size == 2) {
                            width = size[0].toInt()
                            height = size[1].toInt()
                        }
                    } else if (item.startsWith("MOVES:")) {
                        try {
                            numMoves = item.substring("MOVES:".length).toInt()
                        } catch (e: NumberFormatException) {
                            // Ignore
                        }
                    }
                }
            }

            formattedData.append("name:").append(mapName).append(";")
            formattedData.append("num_moves:").append(numMoves).append(";")
            formattedData.append("solution:board:").append(width).append(",").append(height).append(";")

            var inTargetsSection = false
            var inRobotsSection = false
            val targetEntries = HashSet<String>()
            val wallEntries = HashSet<String>()
            val robotEntries = HashSet<String>()

            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine == "TARGET_SECTION:") {
                    inTargetsSection = true
                    inRobotsSection = false
                } else if (trimmedLine == "ROBOTS:") {
                    inTargetsSection = false
                    inRobotsSection = true
                } else if (inTargetsSection && trimmedLine.startsWith("TARGET_SECTION:") && trimmedLine.length > 15) {
                    val targetData = trimmedLine.substring("TARGET_SECTION:".length)
                    val parts = targetData.split(",")
                    if (parts.size >= 3 && parts[0].matches(Regex("\\d+")) &&
                        parts[1].matches(Regex("\\d+")) && parts[2].matches(Regex("-?\\d+"))) {
                        val x = parts[0].toInt()
                        val y = parts[1].toInt()
                        val color = parts[2].replace(";", "").toInt()
                        val colorName = getRobotColorName(color)
                        val targetEntry = "\ntarget_$colorName$x,$y;"
                        if (targetEntries.add(targetEntry)) {
                            formattedData.append(targetEntry)
                        }
                    }
                } else if (inRobotsSection && trimmedLine.isNotEmpty() && trimmedLine != "ROBOTS:") {
                    val parts = trimmedLine.split(",")
                    if (parts.size >= 3 && parts[0].matches(Regex("\\d+")) &&
                        parts[1].matches(Regex("\\d+")) && parts[2].matches(Regex("\\d+"))) {
                        val x = parts[0].toInt()
                        val y = parts[1].toInt()
                        val color = parts[2].replace(";", "").toInt()
                        val colorName = getRobotColorName(color)
                        val robotEntry = "\nrobot_$colorName$x,$y;"
                        if (robotEntries.add(robotEntry)) {
                            formattedData.append(robotEntry)
                        }
                    }
                }
            }

            parseAndAddWalls(lines, width, height, formattedData, wallEntries)

            return formattedData.toString()
        } catch (e: Exception) {
            log.e(e, "[SHARE] Error building map data for share")
            return null
        }
    }

    /**
     * Parse save data string into formatted share data (metadata/compact format).
     * Returns ShareParseResult with parsed data, or null on failure.
     */
    @JvmStatic
    fun parseSaveDataForShare(saveData: String?): ShareParseResult? {
        val metadata = GameState.parseMetadata(saveData) ?: return null

        val mapName = metadata["mapName"] as String
        val width = metadata["width"] as Int
        val height = metadata["height"] as Int
        val moveCount = metadata["moveCount"] as Int
        val optimalMoveCount = metadata["optimalMoveCount"] as Int
        @Suppress("UNCHECKED_CAST")
        val allItems = metadata["allItems"] as List<String>

        val formattedData = StringBuilder()
        formattedData.append("name:").append(mapName).append(";")
        val numMoves = if (optimalMoveCount > 0) optimalMoveCount else moveCount
        formattedData.append("num_moves:").append(numMoves).append(";")
        formattedData.append("solution:board:").append(width).append(",").append(height).append(";")

        var wallCount = 0
        var targetCount = 0
        var robotCount = 0
        val wallEntries = HashSet<String>()
        val targetEntries = HashSet<String>()
        val robotEntries = HashSet<String>()

        // Parse board elements from metadata items
        for (item in allItems) {
            try {
                var data = item
                if (data.startsWith("||")) data = data.substring(2)
                else if (data.startsWith("|")) data = data.substring(1)

                if (data.startsWith("MAPNAME:") || data.startsWith("MOVES:") ||
                    data.startsWith("OPTIMAL:") || data.startsWith("SIZE:") ||
                    data.startsWith("WIDTH:") || data.startsWith("HEIGHT:") ||
                    data.startsWith("MAX_HINT_USED:") || data.startsWith("SOLVED:") ||
                    data.startsWith("DIFFICULTY:") || data.startsWith("TIME:")) {
                    continue
                }

                // Save format concatenates multiple entries per line, e.g.
                //   "tm4,15;tr10,4;"      (all targets)
                //   "h0,1;h2,3;v5,7;"     (all walls)
                //   "rr1,5;rg2,3;rb4,8;"  (all robots)
                // Split by ; and parse each piece.
                for (piece in data.split(";")) {
                    if (piece.isEmpty()) continue

                    // Compact robot: r{colorChar}{x},{y}  e.g. rr3,4
                    if (piece.matches(Regex("r[a-z]\\d+,\\d+"))) {
                        val colorChar = piece[1]
                        val coords = piece.substring(2).split(",")
                        val x = coords[0].toInt()
                        val y = coords[1].toInt()
                        val colorName = getColorNameFromChar(colorChar)
                        val robotEntry = "\nrobot_$colorName$x,$y;"
                        if (robotEntries.add(robotEntry)) {
                            formattedData.append(robotEntry)
                            robotCount++
                        }
                        continue
                    }

                    // Compact target: t{colorChar}{x},{y}  e.g. tm4,15
                    if (piece.matches(Regex("t[a-z]\\d+,\\d+"))) {
                        val colorChar = piece[1]
                        val coords = piece.substring(2).split(",")
                        val x = coords[0].toInt()
                        val y = coords[1].toInt()
                        val colorName = getColorNameFromChar(colorChar)
                        val targetEntry = "\ntarget_$colorName$x,$y;"
                        if (targetEntries.add(targetEntry)) {
                            formattedData.append(targetEntry)
                            targetCount++
                        }
                        continue
                    }

                    // Compact wall: h{x},{y} or v{x},{y}
                    if (piece.matches(Regex("[hv]\\d+,\\d+"))) {
                        val type = "m" + piece[0]
                        val parts = piece.substring(1).split(",")
                        val coordX = parts[0].toInt()
                        val coordY = parts[1].toInt()
                        if (!isBorderWall(coordX, coordY, width, height)) {
                            val wallEntry = "\n$type$coordX,$coordY;"
                            if (wallEntries.add(wallEntry)) {
                                formattedData.append(wallEntry)
                                wallCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.e(e, "[SHARE] Error parsing item in static method: %s", item)
            }
        }

        return ShareParseResult(
            formattedData.toString(), mapName, width, height,
            wallCount, targetCount, robotCount, numMoves
        )
    }

    /**
     * Build the browser share URL for formatted map data.
     * Uses the configured API base URL so dev backends receive the share too.
     */
    @JvmStatic
    fun buildShareUrl(baseUrl: String, formattedData: String): String {
        return "$baseUrl/share_map?data=" + urlEncodeUtf8(formattedData)
    }
}
