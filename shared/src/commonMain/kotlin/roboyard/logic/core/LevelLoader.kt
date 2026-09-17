package roboyard.logic.core

import driftingdroids.model.Board
import roboyard.logic.storage.PlatformStorage

/**
 * Loads level files from shared resources and creates Board instances.
 * Levels are stored in shared/src/commonMain/resources/Maps/level_X.txt
 */
object LevelLoader {

    /**
     * Discovers all available level IDs: built-in levels from bundled "Maps"
     * assets plus custom levels (custom_level_N.txt) in private storage.
     * Matches Android LevelSelectionFragment.loadAvailableLevels.
     */
    fun listAvailableLevelIds(storage: PlatformStorage): List<Int> {
        val ids = mutableListOf<Int>()
        try {
            for (file in storage.listAssetFiles("Maps")) {
                if (file.startsWith("level_") && file.endsWith(".txt")) {
                    file.substring(6, file.length - 4).toIntOrNull()?.let { ids.add(it) }
                }
            }
            for (file in storage.listFiles("custom_level_", ".txt")) {
                file.substring(13, file.length - 4).toIntOrNull()?.let { ids.add(it) }
            }
            ids.sort()
        } catch (e: Exception) {
            System.err.println("[LEVEL_LOADER] Error listing available levels: ${e.message}")
        }
        return ids
    }

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
     * Platform-specific implementation via ResourceLoader.
     */
    private fun loadLevelContent(levelId: Int): String? {
        return ResourceLoader.loadLevelContent(levelId)
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
                        board.setGoal(pos)
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
     * Parses color character (r/g/b/y/s) to robot index (0-4).
     * Matches fragment-app parseColorChar logic 1:1
     */
    private fun parseColorChar(type: String): Int {
        val char = if (type.length == 2) type[1] else type[0]
        return when (char) {
            'r' -> 0 // red (pink)
            'g' -> 1 // green
            'b' -> 2 // blue
            'y' -> 3 // yellow
            's' -> 4 // silver
            else -> -1
        }
    }
}
