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
        val lines = content.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (line in lines) {
            if (line.isBlank()) continue

            // Find the first digit to separate type from data
            var firstDigitIndex = -1
            for (i in line.indices) {
                if (line[i].isDigit()) {
                    firstDigitIndex = i
                    break
                }
            }

            if (firstDigitIndex != -1) {
                val type = line.substring(0, firstDigitIndex)
                val data = line.substring(firstDigitIndex)
                entries.add(RawEntry(type, data))
            } else if (line.contains(":")) {
                val parts = line.split(":".toRegex(), limit = 2).toTypedArray()
                entries.add(RawEntry(parts[0], parts[1]))
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
