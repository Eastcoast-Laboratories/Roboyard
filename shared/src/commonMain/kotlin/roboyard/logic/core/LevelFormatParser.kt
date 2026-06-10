package roboyard.logic.core

import roboyard.logic.util.RLog


/**
 * Parser for the level format used in Roboyard.
 */
object LevelFormatParser {
    private val log = RLog.tag("LevelFormatParser")

    /**
     * Represents a single entry in a level file.
     * Also used for compact format entries with type/x/y for Java test compatibility.
     */
    data class LevelEntry(
        val id: Int = 0,
        val name: String = "",
        val width: Int = 0,
        val height: Int = 0,
        val difficulty: Int = 0,
        val minMoves: Int = 0,
        val solution: String? = null,
        val mapData: String = "",
        // Compact format fields for Java test compatibility
        @JvmField val type: String = "",
        @JvmField val data: String = "",  // Raw data string
        @JvmField var x: Int = 0,
        @JvmField var y: Int = 0
    ) {
        // Secondary constructor for Java tests: type and data (coordinates as string)
        constructor(entryType: String, entryData: String) : this(type = entryType, data = entryData) {
            // Parse coordinates from data like ":12,14" or "5,7"
            val cleanData = if (entryData.startsWith(":")) entryData.substring(1) else entryData
            val parts = cleanData.split(",")
            if (parts.size >= 2) {
                try {
                    x = parts[0].toInt()
                    y = parts[1].toInt()
                } catch (e: Exception) {
                    // Keep default values if parsing fails
                }
            }
        }
    }

    /**
     * Represents a raw entry parsed from a level or save file.
     */
    data class RawEntry(val type: String, val data: String)

    /**
     * Parse entries from compact format (e.g., "tb9,0;h0,0;rr1,3")
     * Returns LevelEntry objects with type/x/y for Java test compatibility.
     */
    @JvmStatic
    fun parseEntries(content: String): List<LevelEntry> {
        val result = mutableListOf<LevelEntry>()
        if (content.isBlank()) return result

        val lines = content.split("[;\\n]".toRegex()).dropLastWhile { it.isEmpty() }
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            // Find first digit to separate type from coordinates
            var firstDigitIndex = -1
            for (i in line.indices) {
                if (line[i].isDigit()) {
                    firstDigitIndex = i
                    break
                }
            }

            if (firstDigitIndex > 0) {
                val type = line.substring(0, firstDigitIndex).trim()
                val coordStr = line.substring(firstDigitIndex).trim()
                val coords = coordStr.split(",")
                if (coords.size >= 2) {
                    try {
                        val x = coords[0].toInt()
                        val y = coords[1].toInt()
                        result.add(LevelEntry(type = type, x = x, y = y))
                    } catch (e: NumberFormatException) {
                        // Skip invalid entries
                    }
                }
            }
        }
        return result
    }

    /**
     * Serialize entries back to string format.
     * Format: type:x,y; for compact entries
     */
    @JvmStatic
    fun serializeEntries(entries: List<LevelEntry>): String {
        val result = StringBuilder()
        for (entry in entries) {
            // If it's a compact entry (type with x,y), format as type:x,y;
            if (entry.type.isNotEmpty()) {
                result.append(entry.type)
                result.append(":")
                result.append(entry.x)
                result.append(",")
                result.append(entry.y)
                result.append(";")
            }
        }
        return result.toString()
    }

    /**
     * Parse raw entries from a level or save file (detailed format).
     */
    @JvmStatic
    fun parseRawEntries(content: String): List<RawEntry> {
        val entries = mutableListOf<RawEntry>()
        // Split by both ; and \n to handle various formats, then trim each part
        val lines = content.split("[;\\n]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            // Try to find the separator: either a colon or the first digit
            val colonIndex = line.indexOf(':')
            var firstDigitIndex = -1
            for (i in line.indices) {
                if (line[i].isDigit()) {
                    firstDigitIndex = i
                    break
                }
            }

            if (colonIndex != -1 && (firstDigitIndex == -1 || colonIndex < firstDigitIndex)) {
                // Separator is a colon (e.g. "board:10,10" or "solution:ABC")
                val type = line.substring(0, colonIndex).trim()
                val data = line.substring(colonIndex + 1).trim()
                entries.add(RawEntry(type, data))
            } else if (firstDigitIndex != -1) {
                // Separator is the first digit (e.g. "tb8,7" or "h0,0")
                val type = line.substring(0, firstDigitIndex).trim()
                val data = line.substring(firstDigitIndex).trim()
                entries.add(RawEntry(type, data))
            } else {
                // No separator found, but might be a type-only line
                entries.add(RawEntry(line, ""))
            }
        }

        return entries
    }

    /**
     * Dummy parseLevel method for completeness
     */
    fun parseLevel(id: Int): LevelEntry? {
        // This is a simplified version for now
        log.d("Parsing level %d", id)
        return null
    }
}
