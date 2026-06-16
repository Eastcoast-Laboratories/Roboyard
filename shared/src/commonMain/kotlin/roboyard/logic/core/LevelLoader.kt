package roboyard.logic.core

import driftingdroids.model.Board

/**
 * Loads level files from shared resources and creates Board instances.
 * Levels are stored in shared/src/commonMain/resources/Maps/level_X.txt
 */
object LevelLoader {

    /**
     * Loads a level by ID (1-140) from shared resources.
     * Returns null if the level file cannot be found or parsed.
     */
    fun loadLevel(levelId: Int): Board? {
        val content = loadLevelContent(levelId) ?: return null
        return parseLevelToBoard(content)
    }

    /**
     * Loads the raw content of a level file from shared resources.
     */
    private fun loadLevelContent(levelId: Int): String? {
        val resourcePath = "Maps/level_$levelId.txt"
        val inputStream = javaClass.classLoader?.getResourceAsStream(resourcePath)
            ?: return null
        return inputStream.use { it.bufferedReader().readText() }
    }

    /**
     * Parses level content (board:W,H; hX,Y; vX,Y; tcolorX,Y; rcolorX,Y;) into a Board.
     * This matches the fragment-app GameState.parseLevel logic 1:1
     */
    private fun parseLevelToBoard(content: String): Board? {
        val entries = LevelFormatParser.parseRawEntries(content)
        var width = 14
        var height = 14

        // First pass: extract board dimensions
        for (entry in entries) {
            if (entry.type == "board") {
                val data = entry.data
                val cleanData = if (data.startsWith(":")) data.substring(1) else data
                val parts = cleanData.split(",").map { it.trim() }
                if (parts.size == 2) {
                    width = parts[0].toIntOrNull() ?: 14
                    height = parts[1].toIntOrNull() ?: 14
                }
                break
            }
        }

        val board = Board.createBoardFreestyle(null, width, height, 4) ?: return null
        val numRobots = 4
        val robotPositions = IntArray(numRobots) { -1 }

        // Second pass: parse walls, targets, robots
        for (entry in entries) {
            val type = entry.type
            val data = entry.data

            if (type == "board") continue

            val parts = data.split(",").map { it.trim() }
            if (parts.size < 2) continue
            val x = parts[0].toIntOrNull() ?: continue
            val y = parts[1].toIntOrNull() ?: continue

            when {
                type == "h" || type == "mh" -> {
                    board.setWall(x, y, Board.NORTH, true)
                    if (y > 0) board.setWall(x, y - 1, Board.SOUTH, true)
                }
                type == "v" || type == "mv" -> {
                    board.setWall(x, y, Board.WEST, true)
                    if (x > 0) board.setWall(x - 1, y, Board.EAST, true)
                }
                type.startsWith("t") -> {
                    val colorId = parseColorChar(type)
                    if (colorId >= -1) {
                        val pos = x + y * width
                        board.addGoal(pos, colorId, 0)
                    }
                }
                type.startsWith("r") -> {
                    val colorId = parseColorChar(type)
                    if (colorId >= 0 && colorId < numRobots) {
                        val pos = x + y * width
                        robotPositions[colorId] = pos
                    }
                }
            }
        }

        board.setRobots(robotPositions)
        return board
    }

    /**
     * Parses color character (p/g/b/y/s) to robot index (0-4).
     */
    private fun parseColorChar(type: String): Int {
        val char = if (type.length == 2) type[1] else type[0]
        return when (char) {
            'p' -> 0 // pink
            'g' -> 1 // green
            'b' -> 2 // blue
            'y' -> 3 // yellow
            's' -> 4 // silver
            else -> -1
        }
    }
}
