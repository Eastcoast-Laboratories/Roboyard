package roboyard.logic.core

import driftingdroids.model.Board

/**
 * Shared board serialization/deserialization for save/load functionality.
 * Extracted to avoid duplication between Android app and ComposeApp (DRY).
 */

/**
 * Serialize a Board to a string format for saving.
 * Format: board:W,H;hX,Y;vX,Y;t<color>X,Y;r<color>X,Y;
 * Color chars: p=pink(0), g=green(1), b=blue(2), y=yellow(3), s=silver(4), m=multi
 */
fun serializeBoard(board: Board): String {
    val sb = StringBuilder()
    sb.append("board:${board.width},${board.height};")

    // Serialize walls
    for (y in 0 until board.height) {
        for (x in 0 until board.width) {
            val pos = x + y * board.width
            if (board.walls[0][pos]) sb.append("h$x,$y;")
            if (board.walls[3][pos]) sb.append("v$x,$y;")
        }
    }

    // Serialize targets
    for (goal in board.goals) {
        val colorChar = robotColorChar(goal.robotNumber)
        sb.append("t$colorChar${goal.x},${goal.y};")
    }

    // Serialize robots
    for (i in board.robotPositions.indices) {
        val pos = board.robotPositions[i]
        val x = pos % board.width
        val y = pos / board.width
        val colorChar = robotColorChar(i)
        sb.append("r$colorChar$x,$y;")
    }

    return sb.toString()
}

/**
 * Deserialize a Board from a string format for loading.
 * @param data The serialized board string
 * @return The deserialized Board, or null if parsing failed
 */
fun deserializeBoard(data: String): Board? {
    val entries: List<LevelFormatParser.RawEntry> = LevelFormatParser.parseRawEntries(data)
    var width = 14
    var height = 14

    // First pass: extract board dimensions
    for (entry in entries) {
        if (entry.type == "board") {
            val entryData = entry.data
            val cleanData = if (entryData.startsWith(":")) entryData.substring(1) else entryData
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
    var robotIndex = 0

    // Second pass: parse walls, targets, robots
    for (entry in entries) {
        val type = entry.type
        val entryData = entry.data

        if (type == "board") continue

        val parts = entryData.split(",").map { it.trim() }
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
                val colorId = parseBoardColorChar(type)
                if (colorId >= -1) {
                    val pos = x + y * width
                    board.addGoal(pos, colorId, 0)
                }
            }
            type.startsWith("r") -> {
                val colorId = parseBoardColorChar(type)
                if (colorId >= 0 && robotIndex < numRobots) {
                    robotPositions[robotIndex] = x + y * width
                    robotIndex++
                }
            }
        }
    }

    board.setRobots(robotPositions)
    return board
}

/**
 * Get the color character for a robot index.
 * 0=pink, 1=green, 2=blue, 3=yellow, 4=silver, else=pink
 */
private fun robotColorChar(robotNumber: Int): Char = when (robotNumber) {
    0 -> 'p'
    1 -> 'g'
    2 -> 'b'
    3 -> 'y'
    4 -> 's'
    else -> 'm'
}

/**
 * Parse a color character from a type string (e.g., "tp" -> 0, "rg" -> 1).
 * @param type The type string starting with 't' or 'r' followed by a color char
 * @return Robot index 0-4, or -1 if unknown, or -1 for multi-target
 */
private fun parseBoardColorChar(type: String): Int {
    val char = if (type.length == 2) type[1] else type[0]
    return when (char) {
        'r' -> 0 // red (pink)
        'p' -> 0 // pink (alias for red)
        'g' -> 1 // green
        'b' -> 2 // blue
        'y' -> 3 // yellow
        's' -> 4 // silver
        'm' -> -1 // multi-target
        else -> -1
    }
}
