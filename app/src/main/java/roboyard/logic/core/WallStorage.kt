package roboyard.logic.core

import android.content.Context
import roboyard.logic.core.Preferences.context
import timber.log.Timber

/**
 * Stores and manages wall configurations for preserving walls across game changes.
 * This class allows the game to maintain the same wall layout when resetting robots
 * or starting a new game, based on user preferences.
 */
class WallStorage  // Private constructor for singleton pattern
private constructor() {
    // Stored wall elements
    private val storedWalls = ArrayList<GridElement>()

    // Current board size
    private var currentBoardWidth = 0
    private var currentBoardHeight = 0

    /**
     * Store wall elements from a list of grid elements
     * @param elements List of grid elements containing walls and other elements
     */
    fun storeWalls(elements: MutableList<GridElement>?) {
        storedWalls.clear()

        if (elements == null || elements.isEmpty()) {
            Timber.tag(TAG).d("No elements to store")
            return
        }


        // Extract only wall elements (horizontal and vertical walls)
        for (element in elements) {
            val type = element.type
            if ("mh" == type || "mv" == type) {
                storedWalls.add(element)
            }
        }


        // Update current board size
        updateCurrentBoardSize()

        Timber.tag(TAG).d(
            "[WALL STORAGE] Stored %d wall elements for board size %dx%d",
            storedWalls.size, currentBoardWidth, currentBoardHeight
        )


        // Save to persistent storage
        saveWallsToDisk()
    }

    /**
     * Update the current board size from Preferences
     */
    fun updateCurrentBoardSize() {
        val newWidth = Preferences.boardSizeWidth
        val newHeight = Preferences.boardSizeHeight


        // If board size has changed, clear the in-memory walls
        if (currentBoardWidth != newWidth || currentBoardHeight != newHeight) {
            Timber.tag(TAG).d(
                "[WALL STORAGE] Board size changed from %dx%d to %dx%d, clearing in-memory walls",
                currentBoardWidth, currentBoardHeight, newWidth, newHeight
            )
            storedWalls.clear()
        }

        currentBoardWidth = newWidth
        currentBoardHeight = newHeight
    }

    /**
     * Save walls to disk for persistence across app restarts
     */
    private fun saveWallsToDisk() {
        val context = context
        if (context == null) {
            Timber.tag(TAG).e("Cannot save walls: context is null")
            return
        }

        updateCurrentBoardSize()
        val key = getWallsKey(currentBoardWidth, currentBoardHeight)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()


        // Convert walls to a string representation
        val wallsData = gridElementsToString(storedWalls)

        editor.putString(key, wallsData)
        editor.apply()

        Timber.tag(TAG).d(
            "[WALL STORAGE] Saved %d walls to disk for board size %dx%d: String: %s",
            storedWalls.size, currentBoardWidth, currentBoardHeight, wallsData
        )
    }

    /**
     * Convert GridElements to a string for storage
     */
    private fun gridElementsToString(elements: ArrayList<GridElement>): String {
        val sb = StringBuilder()
        var count = 0


        // Count walls by type and position for debugging
        var topWalls = 0
        var bottomWalls = 0
        var leftWalls = 0
        var rightWalls = 0
        var otherWalls = 0
        var horizontalWalls = 0
        var verticalWalls = 0

        for (element in elements) {
            // Only store walls (mh, mv)
            if (element.type == "mh" || element.type == "mv") {
                if (count > 0) {
                    sb.append(";")
                }
                sb.append(element.type).append(",").append(element.x).append(",")
                    .append(element.y)
                count++


                // Count wall types for debugging
                if (element.type == "mh") {
                    horizontalWalls++
                    if (element.y == 0) {
                        topWalls++
                    } else if (element.y == currentBoardHeight) {
                        bottomWalls++
                    } else {
                        otherWalls++
                    }
                } else if (element.type == "mv") {
                    verticalWalls++
                    if (element.x == 0) {
                        leftWalls++
                    } else if (element.x == currentBoardWidth) {
                        rightWalls++
                    } else {
                        otherWalls++
                    }
                }
            }
        }

        Timber.d(
            "[WALL STORAGE] Wall count by position: top=%d, bottom=%d, left=%d, right=%d, other=%d, horizontal=%d, vertical=%d",
            topWalls, bottomWalls, leftWalls, rightWalls, otherWalls, horizontalWalls, verticalWalls
        )

        return sb.toString()
    }

    /**
     * Load walls from disk for the current board size
     */
    fun loadStoredWalls() {
        val context = context
        if (context == null) {
            Timber.tag(TAG).e("Cannot load walls: context is null")
            return
        }

        updateCurrentBoardSize()
        val key = getWallsKey(currentBoardWidth, currentBoardHeight)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wallsData: String = prefs.getString(key, "")!!

        storedWalls.clear()

        if (wallsData.isEmpty()) {
            Timber.tag(TAG).d(
                "No saved walls found for board size %dx%d",
                currentBoardWidth, currentBoardHeight
            )
            return
        }


        // Parse the string representation back to GridElements
        val wallEntries =
            wallsData.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (entry in wallEntries) {
            if (entry.isEmpty()) continue

            val parts = entry.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 3) continue

            try {
                val type: String? = parts[0]
                val x = parts[1].toInt()
                val y = parts[2].toInt()

                val wall = GridElement(x, y, type)
                storedWalls.add(wall)
            } catch (e: NumberFormatException) {
                Timber.tag(TAG).e("Error parsing wall data: %s", e.message)
            }
        }

        Timber.tag(TAG).d(
            "Loaded %d walls from disk for board size %dx%d",
            storedWalls.size, currentBoardWidth, currentBoardHeight
        )
    }

    /**
     * Get the key for storing walls based on board size
     */
    private fun getWallsKey(width: Int, height: Int): String {
        return KEY_WALLS_PREFIX + width + "x" + height
    }

    /**
     * Clear stored walls for a specific board size
     * @param width Board width
     * @param height Board height
     */
    fun clearStoredWallsForBoardSize(width: Int, height: Int) {
        val context = context
        if (context == null) {
            Timber.tag(TAG).e("Cannot clear walls: context is null")
            return
        }

        val key = getWallsKey(width, height)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(key)
        editor.apply()


        // If this is the current board size, also clear the in-memory walls
        if (width == currentBoardWidth && height == currentBoardHeight) {
            storedWalls.clear()
        }

        Timber.tag(TAG).d("Cleared stored walls for board size %dx%d", width, height)
    }

    /**
     * Store wall elements for a specific board size (not necessarily the current one).
     * Used when loading a savegame with a different board size.
     * @param elements List of grid elements containing walls and other elements
     * @param boardWidth Board width of the savegame
     * @param boardHeight Board height of the savegame
     */
    fun storeWallsForBoardSize(
        elements: MutableList<GridElement>?,
        boardWidth: Int,
        boardHeight: Int
    ) {
        if (elements == null || elements.isEmpty()) {
            Timber.tag(TAG)
                .d("[WALL STORAGE] No elements to store for %dx%d", boardWidth, boardHeight)
            return
        }

        // If this matches the current board size, use the normal storeWalls path
        if (boardWidth == currentBoardWidth && boardHeight == currentBoardHeight) {
            storeWalls(elements)
            return
        }

        // Extract only wall elements
        val walls = ArrayList<GridElement>()
        for (element in elements) {
            val type = element.type
            if ("mh" == type || "mv" == type) {
                walls.add(element)
            }
        }

        // Save directly to disk for this board size
        val context = context
        if (context == null) {
            Timber.tag(TAG).e("[WALL STORAGE] Cannot save walls: context is null")
            return
        }

        val key = getWallsKey(boardWidth, boardHeight)
        val sb = StringBuilder()
        var count = 0
        for (wall in walls) {
            if (count > 0) sb.append(";")
            sb.append(wall.type).append(",").append(wall.x).append(",")
                .append(wall.y)
            count++
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, sb.toString()).apply()

        Timber.tag(TAG).d(
            "[WALL STORAGE] Stored %d walls to disk for board size %dx%d (different from current %dx%d)",
            count, boardWidth, boardHeight, currentBoardWidth, currentBoardHeight
        )
    }

    /**
     * Check if there are stored walls available
     * @return true if walls are stored, false otherwise
     */
    fun hasStoredWalls(): Boolean {
        // If in-memory walls are empty, try loading from disk
        if (storedWalls.isEmpty()) {
            loadStoredWalls()
        }

        return !storedWalls.isEmpty()
    }

    /**
     * Get the stored wall elements
     * @return List of stored wall elements
     */
    fun getStoredWalls(): ArrayList<GridElement?> {
        // If in-memory walls are empty, try loading from disk
        if (storedWalls.isEmpty()) {
            loadStoredWalls()
        }

        return ArrayList<GridElement?>(storedWalls)
    }


    /**
     * Apply stored walls to a list of grid elements
     * @param elements Original grid elements
     * @return Updated grid elements with walls applied
     */
    fun applyWallsToElements(elements: ArrayList<GridElement>): ArrayList<GridElement> {
        // If no stored walls, return original elements
        if (storedWalls.isEmpty()) {
            // Try to load from disk
            loadStoredWalls()


            // If still empty after loading, return original elements
            if (storedWalls.isEmpty()) {
                Timber.tag(TAG).d("No stored walls to apply")
                return elements
            }
        }


        // Ensure the stored walls match the current board size
        var wallsMatchBoardSize = true
        for (wall in storedWalls) {
            if (wall.x >= currentBoardWidth || wall.y >= currentBoardHeight) {
                wallsMatchBoardSize = false
                Timber.tag(TAG).w(
                    "[WALL STORAGE] Stored wall at (%d,%d) is outside current board size %dx%d",
                    wall.x, wall.y, currentBoardWidth, currentBoardHeight
                )
                break
            }
        }


        // If walls don't match board size, clear them and return original elements
        if (!wallsMatchBoardSize) {
            Timber.tag(TAG)
                .d("[WALL STORAGE] Stored walls don't match current board size, clearing and generating new map")
            storedWalls.clear()
            clearStoredWallsForBoardSize(currentBoardWidth, currentBoardHeight)
            return elements
        }


        // Create a copy of the original elements
        val result = ArrayList<GridElement>(elements)


        // Add stored walls
        result.addAll(storedWalls)
        Timber.tag(TAG).d("Applied %d stored walls to grid elements", storedWalls.size)

        return result
    }

    companion object {
        private const val TAG = "WallStorage"
        private const val PREFS_NAME = "WallStoragePrefs"
        private const val KEY_WALLS_PREFIX = "walls_"

        private var instance: WallStorage? = null

        /**
         * Get the singleton instance of WallStorage
         * @return The WallStorage instance
         */
        @JvmStatic
        @Synchronized
        fun getInstance(): WallStorage {
            if (instance == null) {
                instance = WallStorage()
            }
            return instance!!
        }
    }
}
