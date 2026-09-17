package roboyard.logic.network

import java.net.URLDecoder
import roboyard.logic.core.Constants
import roboyard.logic.core.GameElement
import roboyard.logic.core.GameState
import roboyard.logic.core.MapObjects
import roboyard.logic.util.RLog

/**
 * Shared deep link parser for roboyard:// and https://roboyard.z11.de links.
 * Platform-independent: parses the URL, converts web-format map data to the
 * app save format and produces a GameState. Callers handle navigation,
 * difficulty override and error display.
 *
 * Result of parsing a deep link:
 * - [DeepLinkResult.Random]: start a random game
 * - [DeepLinkResult.Map]: apply the contained GameState (difficulty >= 0 overrides)
 * - [DeepLinkResult.UnsupportedVersion]: server sent a newer protocol version
 * - [DeepLinkResult.MapTooLarge]: map exceeded size limits
 * - [DeepLinkResult.Invalid]: link looked like a deep link but had no usable data
 * - [DeepLinkResult.NotADeepLink]: URL is not a roboyard deep link
 */
object DeepLinkHandler {

    /** Current supported deep-link protocol version. Increment when breaking changes are introduced. */
    const val DEEPLINK_API_VERSION = 1

    // Size limits for deep-link map data to prevent denial-of-service via crafted links
    private const val DEEPLINK_MAX_WIDTH = 64
    private const val DEEPLINK_MAX_HEIGHT = 64
    private const val DEEPLINK_MAX_CELLS = 5000
    private const val DEEPLINK_MAX_ROBOTS = 16
    private const val DEEPLINK_MAX_TARGETS = 16

    private val log = RLog.tag("DeepLink")

    sealed class DeepLinkResult {
        object Random : DeepLinkResult()
        class Map(val gameState: GameState, val difficulty: Int) : DeepLinkResult()
        class UnsupportedVersion(val version: Int) : DeepLinkResult()
        object MapTooLarge : DeepLinkResult()
        object Invalid : DeepLinkResult()
        object NotADeepLink : DeepLinkResult()
    }

    /** Check if a URL is a roboyard deep link. */
    fun isValidDeepLink(url: String): Boolean {
        return url.startsWith("roboyard://") || url.contains("roboyard.z11.de")
    }

    /**
     * Parse a deep link URL.
     * @return the result describing what the caller should do
     */
    fun parse(url: String): DeepLinkResult {
        if (!isValidDeepLink(url)) return DeepLinkResult.NotADeepLink
        log.d("[DEEPLINK] Received deep link: %s", url)

        val scheme = schemeOf(url)
        val host = hostOf(url)
        val path = pathOf(url)

        // Random game deep link: roboyard://random or https://roboyard.z11.de/random
        if ((scheme == "roboyard" && host == "random") || path == "/random") {
            log.d("[DEEPLINK] Random game deep link detected")
            return DeepLinkResult.Random
        }

        val params = queryParams(url)

        // Version check: if server sends a higher version than we support, redirect to menu
        val verStr = params["ver"]
        if (!verStr.isNullOrEmpty()) {
            try {
                val ver = verStr.toInt()
                if (ver > DEEPLINK_API_VERSION) {
                    log.w("[DEEPLINK] Unsupported deep-link version %d (max %d)", ver, DEEPLINK_API_VERSION)
                    return DeepLinkResult.UnsupportedVersion(ver)
                }
            } catch (e: NumberFormatException) {
                log.e("[DEEPLINK] Invalid ver parameter: %s", verStr)
            }
        }

        // Extract parameters - name & difficulty may be separate query params or embedded in data
        var mapData = params["data"]
        var mapName = params["name"]
        var difficultyStr = params["difficulty"]

        // The play button on roboyard.z11.de embeds &name=...&difficulty=... inside the data value.
        // Extract them from mapData if not found as separate query parameters.
        if (mapData != null && mapData.contains("&name=")) {
            val nameIdx = mapData.indexOf("&name=")
            val tail = mapData.substring(nameIdx) // "&name=ZUQAV&difficulty=..."
            mapData = mapData.substring(0, nameIdx) // pure map string

            for (param in tail.split("&")) {
                if (param.startsWith("name=") && mapName == null) {
                    var value = param.substring("name=".length)
                    try {
                        value = URLDecoder.decode(value, "UTF-8")
                    } catch (ignored: Exception) {
                    }
                    if (value.isNotEmpty()) mapName = value
                } else if (param.startsWith("difficulty=") && difficultyStr == null) {
                    difficultyStr = param.substring("difficulty=".length)
                }
            }
            log.d("[DEEPLINK] Extracted embedded params - name: %s, difficulty: %s", mapName, difficultyStr)
        }

        if (mapData != null) {
            log.d("[DEEPLINK_RAW] Raw map data length: %d", mapData.length)
            log.d("[DEEPLINK_RAW] Map data preview: %s", mapData.take(100))
        } else {
            log.e("[DEEPLINK_RAW] Map data is null")
        }

        var difficulty = -1
        if (!difficultyStr.isNullOrEmpty()) {
            try {
                difficulty = difficultyStr.toInt()
                log.d("[DEEPLINK] Parsed difficulty: %d", difficulty)
            } catch (e: NumberFormatException) {
                log.e("[DEEPLINK] Invalid difficulty value: %s", difficultyStr)
            }
        }

        if (mapData.isNullOrEmpty()) {
            log.w("[DEEPLINK] No map data found in deep link")
            return DeepLinkResult.Invalid
        }

        log.d("[DEEPLINK] Extracted map data: %s", mapData.take(50))

        // Web format (starts with "name:" or contains "mh"/"mv" wall markers) needs conversion
        var effectiveMapData = mapData
        if (mapData.startsWith("name:") || mapData.contains("mh") || mapData.contains("mv")) {
            log.d("[DEEPLINK_FORMAT] Detected web format, converting to app format")
            val converted = convertWebFormatToAppFormat(mapData)
                ?: return DeepLinkResult.MapTooLarge
            log.d("[DEEPLINK_CONVERT] Converted map data preview: %s", converted.take(100))

            // Extract map name from web format if not provided as parameter
            if (mapName == null && mapData.startsWith("name:")) {
                val endIndex = mapData.indexOf(";")
                if (endIndex > 5) {
                    mapName = mapData.substring(5, endIndex)
                    log.d("[DEEPLINK] Extracted map name from web format: %s", mapName)
                }
            }
            effectiveMapData = converted
        }

        val gameState = parseMapData(effectiveMapData, mapName) ?: return DeepLinkResult.Invalid
        return DeepLinkResult.Map(gameState, difficulty)
    }

    /**
     * Parse serialized map data into a GameState, applying the optional map name.
     */
    private fun parseMapData(mapData: String, mapName: String?): GameState? {
        return try {
            val gameState = GameState.parseFromSaveData(mapData) ?: run {
                log.e("[DEEPLINK_PROCESS] Failed to parse map data")
                return null
            }

            var robotCount = 0
            var targetCount = 0
            var wallCount = 0
            for (element in gameState.gameElements) {
                when (element.type) {
                    GameElement.TYPE_ROBOT -> robotCount++
                    GameElement.TYPE_TARGET -> targetCount++
                    GameElement.TYPE_HORIZONTAL_WALL, GameElement.TYPE_VERTICAL_WALL -> wallCount++
                }
            }
            log.d(
                "[DEEPLINK_ELEMENTS] Game state contains: %d robots, %d targets, %d walls",
                robotCount, targetCount, wallCount
            )

            if (!mapName.isNullOrEmpty()) {
                gameState.levelName = mapName
                log.d("[DEEPLINK_PROCESS] Set custom map name: %s", mapName)
            } else {
                // No name provided: generate "Web <hash>" like random maps do
                val uniqueId = MapObjects.generateUniqueId(gameState.gridElements)
                gameState.levelName = "Web $uniqueId"
                gameState.uniqueMapId = uniqueId
                log.d("[DEEPLINK_PROCESS] Generated web map name: %s", gameState.levelName)
            }
            gameState
        } catch (e: Exception) {
            log.e(e, "[DEEPLINK_PROCESS] Error processing map data: %s", e.message)
            null
        }
    }

    // ---------- URL helpers (platform-independent replacement for android.net.Uri) ----------

    private fun schemeOf(url: String): String? =
        url.substringBefore("://", "").takeIf { it.isNotEmpty() && url.contains("://") }

    private fun hostOf(url: String): String? {
        val after = url.substringAfter("://", "")
        if (after.isEmpty()) return null
        return after.substringBefore("/").substringBefore("?")
    }

    private fun pathOf(url: String): String? {
        val after = url.substringAfter("://", "")
        if (after.isEmpty() || !after.contains("/")) return null
        return "/" + after.substringAfter("/").substringBefore("?")
    }

    private fun queryParams(url: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val query = url.substringAfter("?", "")
        if (query.isEmpty() || query == url) return result
        for (pair in query.split("&")) {
            val idx = pair.indexOf('=')
            if (idx <= 0) continue
            val key = pair.substring(0, idx)
            val raw = pair.substring(idx + 1)
            result[key] = try {
                URLDecoder.decode(raw, "UTF-8")
            } catch (e: Exception) {
                raw
            }
        }
        return result
    }

    // ---------- Web format → app format conversion (ported from MainActivity) ----------

    /**
     * Convert the web format map data to the app format.
     * Web format: "name:NAME;num_moves:N;solution:board:W,H;mh0,0;mv0,1;...robot_red10,4;..."
     * App format: The format expected by GameState.parseFromSaveData()
     *
     * @return the map data in app format, or null if size limits were exceeded
     */
    fun convertWebFormatToAppFormat(webFormatData: String): String? {
        log.d("[DEEPLINK_CONVERT] Converting web format to app format")

        val appFormat = StringBuilder()
        var mapName = "Web Map"
        var width = 16
        var height = 16

        // Parse the web format - split by newlines first, then by semicolons
        val parts = mutableListOf<String>()
        for (line in webFormatData.split("\\n".toRegex())) {
            for (part in line.split(";")) {
                if (part.trim().isNotEmpty()) {
                    parts.add(part.trim())
                }
            }
        }

        log.d("[DEEPLINK_CONVERT] Parsed %d parts from web format", parts.size)

        // Extract map name and board dimensions
        for (part in parts) {
            if (part.startsWith("name:")) {
                mapName = part.substring(5)
                log.d("[DEEPLINK_CONVERT] Found map name: %s", mapName)
            } else if (part.contains("board:")) {
                val boardIndex = part.indexOf("board:")
                val dimensionsStr = part.substring(boardIndex + 6)
                val dimensions = dimensionsStr.split(",")
                if (dimensions.size == 2) {
                    try {
                        width = dimensions[0].toInt()
                        height = dimensions[1].toInt()
                        log.d("[DEEPLINK_CONVERT] Found board dimensions: %dx%d", width, height)
                    } catch (e: NumberFormatException) {
                        log.e("[DEEPLINK_CONVERT] Error parsing board dimensions: %s", e.message)
                    }
                }
            }
        }

        // Validate board dimensions before allocating any data structures
        if (width <= 2 || height <= 2 || width > DEEPLINK_MAX_WIDTH || height > DEEPLINK_MAX_HEIGHT
            || width.toLong() * height > DEEPLINK_MAX_CELLS
        ) {
            log.e(
                "[DEEPLINK_CONVERT] Board dimensions out of range: %dx%d (max %dx%d, max cells %d)",
                width, height, DEEPLINK_MAX_WIDTH, DEEPLINK_MAX_HEIGHT, DEEPLINK_MAX_CELLS
            )
            return null
        }

        // Start building the app format
        appFormat.append("#MAPNAME:").append(mapName)
            .append(";TIME:0;MOVES:0;UNIQUE_MAP_ID:WEBMP\n")
        appFormat.append("WIDTH:").append(width).append(";\n")
        appFormat.append("HEIGHT:").append(height).append(";\n")

        // Create empty board
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x > 0) appFormat.append(",")
                appFormat.append("0")
            }
            appFormat.append("\n")
        }

        // Process targets - important to add at least one target as the app requires this
        val targetParts = parts.filter { it.startsWith("target_") }
        if (targetParts.size > DEEPLINK_MAX_TARGETS) {
            log.e("[DEEPLINK_CONVERT] Too many targets (max %d)", DEEPLINK_MAX_TARGETS)
            return null
        }

        appFormat.append("TARGET_SECTION:\n")
        // If no targets found, add a default one to prevent app errors
        if (targetParts.isEmpty()) {
            log.e("[DEEPLINK_CONVERT] No targets found in web format! Adding a default target.")
            appFormat.append("TARGET_SECTION:8,8,0\n")
        } else {
            for (part in targetParts) {
                try {
                    // Format: target_colorX,Y
                    val colorPart = part.substring(7) // Skip "target_"
                    val digitPos = colorPart.indexOfFirst { it.isDigit() }
                    if (digitPos == -1) {
                        log.e("[DEEPLINK_TARGET_PARSE] Could not find coordinate digits in: %s", colorPart)
                        continue
                    }
                    val colorStr = colorPart.substring(0, digitPos)
                    val coords = colorPart.substring(digitPos).split(",")
                    if (coords.size == 2) {
                        val x = coords[0].toInt()
                        val y = coords[1].toInt()
                        appFormat.append("TARGET_SECTION:").append(x).append(",")
                            .append(y).append(",").append(getColorId(colorStr)).append("\n")
                    }
                } catch (e: Exception) {
                    log.e("[DEEPLINK_CONVERT] Error parsing target: %s - %s", part, e.message)
                }
            }
        }

        // Process walls
        appFormat.append("WALLS:\n")

        // Collect existing horizontal/vertical wall coords to avoid duplicates
        // when auto-adding perimeter walls below.
        val existingH = mutableSetOf<String>()
        val existingV = mutableSetOf<String>()
        for (part in parts) {
            if (part.startsWith("mh")) existingH.add(part.substring(2))
            else if (part.startsWith("mv")) existingV.add(part.substring(2))
        }

        // Auto-add MISSING perimeter walls. Maps that already include perimeter
        // walls render with gridWidth=width+1 (because setGridElements uses maxX+1).
        // Without perimeter walls the board ends up rendered one column/row too small.
        var addedPerimeter = 0
        for (x in 0 until width) {
            val topKey = "$x,0"
            val bottomKey = "$x,$height"
            if (existingH.add(topKey)) {
                appFormat.append("H,").append(x).append(",0\n")
                addedPerimeter++
            }
            if (existingH.add(bottomKey)) {
                appFormat.append("H,").append(x).append(",").append(height).append("\n")
                addedPerimeter++
            }
        }
        for (y in 0 until height) {
            val leftKey = "0,$y"
            val rightKey = "$width,$y"
            if (existingV.add(leftKey)) {
                appFormat.append("V,0,").append(y).append("\n")
                addedPerimeter++
            }
            if (existingV.add(rightKey)) {
                appFormat.append("V,").append(width).append(",").append(y).append("\n")
                addedPerimeter++
            }
        }
        log.d("[BOARD_SIZE_DEBUG] Auto-added %d missing perimeter walls", addedPerimeter)

        // Parse horizontal walls (mhX,Y)
        for (part in parts) {
            if (part.startsWith("mh")) {
                try {
                    val coords = part.substring(2).split(",")
                    if (coords.size == 2) {
                        appFormat.append("H,").append(coords[0].toInt()).append(",")
                            .append(coords[1].toInt()).append("\n")
                    }
                } catch (e: Exception) {
                    log.e("[DEEPLINK_CONVERT] Error parsing horizontal wall: %s - %s", part, e.message)
                }
            }
        }

        // Parse vertical walls (mvX,Y)
        for (part in parts) {
            if (part.startsWith("mv")) {
                try {
                    val coords = part.substring(2).split(",")
                    if (coords.size == 2) {
                        appFormat.append("V,").append(coords[0].toInt()).append(",")
                            .append(coords[1].toInt()).append("\n")
                    }
                } catch (e: Exception) {
                    log.e("[DEEPLINK_CONVERT] Error parsing vertical wall: %s - %s", part, e.message)
                }
            }
        }

        // Process robots
        val robotParts = parts.filter { it.startsWith("robot_") }
        if (robotParts.size > DEEPLINK_MAX_ROBOTS) {
            log.e("[DEEPLINK_CONVERT] Too many robots (max %d)", DEEPLINK_MAX_ROBOTS)
            return null
        }

        appFormat.append("ROBOTS:\n")
        // If no robots found, add a default one to prevent app errors
        if (robotParts.isEmpty()) {
            log.e("[DEEPLINK_CONVERT] No robots found in web format! Adding a default robot.")
            appFormat.append("4,4,0\n")
        } else {
            appendRobots(appFormat, robotParts)
        }

        // Add initial positions section (same as robots)
        appFormat.append("INITIAL_POSITIONS:\n")
        if (robotParts.isEmpty()) {
            appFormat.append("4,4,0\n")
        } else {
            appendRobots(appFormat, robotParts)
        }

        val result = appFormat.toString()
        log.d("[DEEPLINK_CONVERT] Conversion complete, generated app format with length: %d", result.length)
        return result
    }

    /** Append "x,y,colorId" lines for each robot_colorX,Y part. */
    private fun appendRobots(appFormat: StringBuilder, robotParts: List<String>) {
        for (part in robotParts) {
            try {
                val colorPart = part.substring(6) // Skip "robot_"
                val digitPos = colorPart.indexOfFirst { it.isDigit() }
                if (digitPos == -1) continue
                val colorStr = colorPart.substring(0, digitPos)
                val coords = colorPart.substring(digitPos).split(",")
                if (coords.size == 2) {
                    appFormat.append(coords[0].toInt()).append(",")
                        .append(coords[1].toInt()).append(",")
                        .append(getColorId(colorStr)).append("\n")
                }
            } catch (e: Exception) {
                log.e("[DEEPLINK_CONVERT] Error parsing robot: %s - %s", part, e.message)
            }
        }
    }

    /**
     * Convert color name to color ID.
     * @return Color ID (0-4, or -1 for multi)
     */
    private fun getColorId(colorName: String): Int {
        return when (colorName.lowercase()) {
            "multi" -> Constants.COLOR_MULTI
            "red", "pink" -> Constants.COLOR_PINK // Red is pink in our system
            "green" -> Constants.COLOR_GREEN
            "blue" -> Constants.COLOR_BLUE
            "yellow" -> Constants.COLOR_YELLOW
            "silver" -> Constants.COLOR_SILVER
            else -> Constants.COLOR_GREEN // Default to green
        }
    }
}
