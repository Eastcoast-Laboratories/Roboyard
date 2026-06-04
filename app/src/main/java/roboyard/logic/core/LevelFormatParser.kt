package roboyard.logic.core

import timber.log.Timber

/**
 * Parser for the level format used in Roboyard.
 */
object LevelFormatParser {
    /**
     * Represents a single entry in a level file.
     */
    data class LevelEntry(
        val id: Int,
        val name: String,
        val width: Int,
        val height: Int,
        val difficulty: Int,
        val minMoves: Int,
        val solution: String?,
        val mapData: String
    )

    /**
     * Represents a raw entry parsed from a level or save file.
     */
    data class RawEntry(val type: String, val data: String)

    /**
     * Parse raw entries from a level or save file.
     */
    fun parseEntries(content: String): List<RawEntry> {
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
        Timber.d("Parsing level %d", id)
        return null
    }
}
