package roboyard.logic.network

import android.content.Context
import driftingdroids.model.Board
import roboyard.logic.core.LevelFormatParser

/**
 * Android implementation of DeepLinkHandler.
 * Parses deep links from roboyard.z11.de and converts them to Boards.
 */
class AndroidDeepLinkHandler(private val context: Context) : DeepLinkHandler {
    
    companion object {
        private const val DEEPLINK_API_VERSION = 1
        private const val DEEPLINK_MAX_WIDTH = 64
        private const val DEEPLINK_MAX_HEIGHT = 64
        private const val DEEPLINK_MAX_CELLS = 5000
        private const val DEEPLINK_MAX_ROBOTS = 16
        private const val DEEPLINK_MAX_TARGETS = 16
    }
    
    override fun parseDeepLink(url: String): Board? {
        // Check if this is a random game deep link
        if (url.contains("random")) {
            // Random games are handled by generating a random board
            return Board.createBoardRandom(4)
        }
        
        // Extract map data from URL
        val mapData = extractMapData(url) ?: return null
        
        // Convert web format to app format if needed
        val appFormat = if (mapData.startsWith("name:") || mapData.contains("mh") || mapData.contains("mv")) {
            convertWebFormatToAppFormat(mapData) ?: return null
        } else {
            mapData
        }
        
        // Parse the app format into a Board
        return parseAppFormatToBoard(appFormat)
    }
    
    override fun isValidDeepLink(url: String): Boolean {
        return url.startsWith("roboyard://") || url.contains("roboyard.z11.de")
    }
    
    private fun extractMapData(url: String): String? {
        // Simple extraction - in production, use proper URL parsing
        val dataStart = url.indexOf("data=")
        if (dataStart == -1) return null
        
        val dataEnd = url.indexOf("&", dataStart)
        val dataStr = if (dataEnd == -1) {
            url.substring(dataStart + 5)
        } else {
            url.substring(dataStart + 5, dataEnd)
        }
        
        return java.net.URLDecoder.decode(dataStr, "UTF-8")
    }
    
    private fun convertWebFormatToAppFormat(webFormat: String): String? {
        // Simplified conversion - in production, use the full logic from MainActivity
        // For now, return the web format as-is and parse it directly
        return webFormat
    }
    
    private fun parseAppFormatToBoard(appFormat: String): Board? {
        // Parse the format using LevelFormatParser
        val entries = LevelFormatParser.parseRawEntries(appFormat)
        var width = 14
        var height = 14
        
        // Extract board dimensions
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
        
        // Validate dimensions
        if (width <= 2 || height <= 2 || width > DEEPLINK_MAX_WIDTH || height > DEEPLINK_MAX_HEIGHT) {
            return null
        }
        
        val board = Board.createBoardFreestyle(null, width, height, 4) ?: return null
        val numRobots = 4
        val robotPositions = IntArray(numRobots) { -1 }
        var robotIndex = 0
        
        // Parse walls, targets, robots
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

actual fun getDeepLinkHandler(): DeepLinkHandler {
    throw IllegalStateException("AndroidDeepLinkHandler requires Context. Use AndroidDeepLinkHandler(context) directly.")
}
