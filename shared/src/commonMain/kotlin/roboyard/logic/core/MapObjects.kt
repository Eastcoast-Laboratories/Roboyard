package roboyard.logic.core

import roboyard.logic.util.RLog


/**
 * Helper class for managing map objects and their properties.
 */
object MapObjects {
    private val log = RLog.tag("MapObjects")

    /**
     * Map object types for reference
     */
    const val TYPE_ROBOT: String = "robot"
    const val TYPE_TARGET: String = "target"
    const val TYPE_WALL: String = "wall"

    /**
     * Get the base type of a map object (robot, target, or wall)
     * @param objectType The full object type string (e.g., "robot_blue")
     * @return The base type string
     */
    fun getBaseType(objectType: String?): String {
        if (objectType == null) return "unknown"

        return if (objectType.startsWith("robot_")) {
            TYPE_ROBOT
        } else if (objectType.startsWith("target_")) {
            TYPE_TARGET
        } else if (objectType == "mh" || objectType == "mv") {
            TYPE_WALL
        } else {
            "unknown"
        }
    }

    /**
     * Get all available target types
     * @return List of target type strings
     */
    val allTargetTypes: MutableList<String>
        get() {
            val targets = mutableListOf<String>()
            targets.add("target_pink")
            targets.add("target_green")
            targets.add("target_blue")
            targets.add("target_yellow")
            targets.add("target_silver")
            targets.add("target_multi")
            return targets
        }

    /**
     * Get all available robot types
     * @return List of robot type strings
     */
    val allRobotTypes: MutableList<String>
        get() {
            val robots = mutableListOf<String>()
            robots.add("robot_pink")
            robots.add("robot_green")
            robots.add("robot_blue")
            robots.add("robot_yellow")
            robots.add("robot_silver")
            return robots
        }

    /**
     * Check if an object type is a robot
     * @param objectType The object type string
     * @return true if it's a robot
     */
    fun isRobot(objectType: String?): Boolean {
        return objectType != null && objectType.startsWith("robot_")
    }

    /**
     * Check if an object type is a target
     * @param objectType The object type string
     * @return true if it's a target
     */
    fun isTarget(objectType: String?): Boolean {
        return objectType != null && objectType.startsWith("target_")
    }

    /**
     * Check if an object type is a wall
     * @param objectType The object type string
     * @return true if it's a wall
     */
    fun isWall(objectType: String?): Boolean {
        return objectType == "mh" || objectType == "mv"
    }

    /**
     * Extract map data from a string
     */
    @JvmStatic
    fun extractDataFromString(data: String?): ArrayList<GridElement> = extractDataFromString(data, false)

    @JvmStatic
    fun extractDataFromString(data: String?, someFlag: Boolean): ArrayList<GridElement> {
        val result = ArrayList<GridElement>()
        if (data.isNullOrBlank()) return result

        // Compact code to full name mapping
        val compactCodeMap = mapOf(
            // Targets
            "tb" to "target_blue",
            "tr" to "target_red",
            "tg" to "target_green",
            "ty" to "target_yellow",
            "ts" to "target_silver",
            "tm" to "target_multi",
            "tp" to "target_pink",
            // Robots
            "rr" to "robot_red",
            "rb" to "robot_blue",
            "rg" to "robot_green",
            "ry" to "robot_yellow",
            "rs" to "robot_silver",
            "rp" to "robot_pink"
        )

        // Split data by semicolon
        val entries = data.split(";").filter { it.isNotBlank() }
        for (entry in entries) {
            // Very simple parsing for GridElement(x,y,type)
            // Expecting format like "mh1,2" or "robot_blue5,6" or "tb8,7"
            try {
                // Find where the numbers start
                val firstDigitIndex = entry.indexOfFirst { it.isDigit() }
                if (firstDigitIndex != -1) {
                    val type = entry.substring(0, firstDigitIndex)
                    // Convert compact code to full name if needed
                    val fullType = compactCodeMap[type] ?: type
                    val coords = entry.substring(firstDigitIndex).split(",")
                    if (coords.size == 2) {
                        result.add(GridElement(coords[0].toInt(), coords[1].toInt(), fullType))
                    }
                }
            } catch (e: Exception) {
                log.e("Error parsing map object data: $entry")
            }
        }

        return result
    }

    /**
     * Generate a unique 5-letter identifier from a string.
     * Deterministic - same input produces same output.
     * Alternates vowels and consonants.
     */
    @JvmStatic
    fun generateUnique5LetterFromString(input: String?): String {
        if (input.isNullOrEmpty()) return "aaaaa"
        
        val vowels = "aeiou"
        val consonants = "bcdfghjklmnpqrstvwxyz"
        
        // Simple hash of the input
        var hash = 0
        for (char in input) {
            hash = (hash * 31 + char.code) and 0x7FFFFFFF // Keep positive
        }
        
        // Generate 5 letters alternating consonant/vowel starting with consonant
        val result = StringBuilder()
        var useConsonant = true
        for (i in 0..4) {
            if (useConsonant) {
                result.append(consonants[hash % consonants.length])
                hash /= consonants.length
            } else {
                result.append(vowels[hash % vowels.length])
                hash /= vowels.length
            }
            useConsonant = !useConsonant
        }
        
        return result.toString()
    }
}
