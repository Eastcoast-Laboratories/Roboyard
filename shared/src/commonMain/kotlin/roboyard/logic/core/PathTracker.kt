package roboyard.logic.core

import driftingdroids.model.Board
import roboyard.logic.util.RLog

/**
 * Shared robot path tracking for movement visualization.
 * Matches Android GameGridView path rendering: colored lines per robot,
 * with base offset and perpendicular offset for stacked segments.
 *
 * Both Android and ComposeApp use this class for consistent path rendering.
 */
class PathTracker {

    private val log = RLog.tag("PathTracker")

    companion object {
        /** Path stroke width as ratio of cell size (matches Android PATH_STROKE_WIDTH_RATIO) */
        const val PATH_STROKE_WIDTH_RATIO = 0.15f

        /** Perpendicular offset step for stacked segments (ratio of cellSize) */
        const val PERPENDICULAR_OFFSET_STEP_RATIO = 0.05f

        /** Base offset range for robot paths (ratio of cellSize) */
        const val BASE_OFFSET_RATIO = 0.20f

        /** Ghost robot alpha for starting position markers */
        const val GHOST_ALPHA = 0.20f
    }

    /** Maps robot color index → list of [x, y] positions */
    private val robotPaths: MutableMap<Int, MutableList<IntArray>> = mutableMapOf()

    /** Per-robot base offset [offsetX, offsetY] in pixels (relative to cellSize) */
    private val robotBaseOffsets: MutableMap<Int, FloatArray> = mutableMapOf()

    /** Tracks how many times each segment was traversed: robotColor → "x1,y1:x2,y2" → count */
    private val segmentCounts: MutableMap<Int, MutableMap<String, Int>> = mutableMapOf()

    /**
     * Add a path segment for a robot.
     * @param robotColor Robot color index
     * @param fromX Start X
     * @param fromY Start Y
     * @param toX End X
     * @param toY End Y
     */
    fun addPathSegment(robotColor: Int, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        val path = robotPaths.getOrPut(robotColor) { mutableListOf() }

        // If path is empty, add the starting position
        if (path.isEmpty()) {
            path.add(intArrayOf(fromX, fromY))
        }

        // Add the destination
        path.add(intArrayOf(toX, toY))

        // Track segment traversal count
        val segments = segmentCounts.getOrPut(robotColor) { mutableMapOf() }
        val segmentKey = "$fromX,$fromY:$toX,$toY"
        segments[segmentKey] = (segments[segmentKey] ?: 0) + 1

        // Initialize base offset if not set (random ±BASE_OFFSET_RATIO * cellSize)
        if (robotColor !in robotBaseOffsets) {
            val offsetX = (Math.random() * 2 - 1).toFloat() * BASE_OFFSET_RATIO
            val offsetY = (Math.random() * 2 - 1).toFloat() * BASE_OFFSET_RATIO
            robotBaseOffsets[robotColor] = floatArrayOf(offsetX, offsetY)
        }

        log.d("[PATH] Added segment for robot $robotColor: ($fromX,$fromY) → ($toX,$toY), path size=${path.size}")
    }

    /**
     * Undo the last path segment for a robot.
     * @param robotColor Robot color index
     * @return true if a segment was removed
     */
    fun undoLastPathSegment(robotColor: Int): Boolean {
        val path = robotPaths[robotColor] ?: return false
        if (path.size < 2) {
            robotPaths.remove(robotColor)
            return false
        }

        // Remove last point
        val removed = path.removeAt(path.size - 1)
        val prevPos = path[path.size - 1]

        // Decrement segment count
        val segments = segmentCounts[robotColor]
        if (segments != null) {
            val segmentKey = "${prevPos[0]},${prevPos[1]}:${removed[0]},${removed[1]}"
            val count = segments[segmentKey]
            if (count != null && count > 1) {
                segments[segmentKey] = count - 1
            } else {
                segments.remove(segmentKey)
            }
        }

        // If only one point left, remove the path
        if (path.size <= 1) {
            robotPaths.remove(robotColor)
        }

        log.d("[PATH] Undid segment for robot $robotColor, path size=${path.size}")
        return true
    }

    /**
     * Undo the last path segment across all robots (for global undo).
     * Uses pathHistory to determine which robot to undo.
     * @param pathHistory The GameController's pathHistory
     * @return true if a segment was removed
     */
    fun undoLastPathSegmentFromHistory(pathHistory: List<IntArray>): Boolean {
        if (pathHistory.isEmpty()) return false
        val lastMove = pathHistory.last()
        val robotColor = lastMove[0]
        return undoLastPathSegment(robotColor)
    }

    /**
     * Clear all robot paths.
     */
    fun clearPaths() {
        robotPaths.clear()
        robotBaseOffsets.clear()
        segmentCounts.clear()
        log.d("[PATH] Cleared all paths")
    }

    /**
     * Reconstruct all paths from path history.
     * Called when the screen is recreated or game is loaded.
     * @param pathHistory The GameController's pathHistory: each entry is [color, fromX, fromY, toX, toY]
     */
    fun reconstructFromHistory(pathHistory: List<IntArray>) {
        clearPaths()
        for (entry in pathHistory) {
            val color = entry[0]
            val fromX = entry[1]
            val fromY = entry[2]
            val toX = entry[3]
            val toY = entry[4]
            addPathSegment(color, fromX, fromY, toX, toY)
        }
        log.d("[PATH] Reconstructed ${pathHistory.size} path segments from history")
    }

    /**
     * Get all robot paths for rendering.
     * @return Map of robotColor → list of [x, y] positions
     */
    fun getRobotPaths(): Map<Int, List<IntArray>> = robotPaths.toMap()

    /**
     * Get base offset for a robot.
     * @param robotColor Robot color index
     * @return [offsetX, offsetY] in cellSize ratios, or [0, 0] if not set
     */
    fun getBaseOffset(robotColor: Int): FloatArray = robotBaseOffsets[robotColor] ?: floatArrayOf(0f, 0f)

    /**
     * Get segment traversal count.
     * @param robotColor Robot color index
     * @param fromX Start X
     * @param fromY Start Y
     * @param toX End X
     * @param toY End Y
     * @return Number of times this segment was traversed
     */
    fun getSegmentCount(robotColor: Int, fromX: Int, fromY: Int, toX: Int, toY: Int): Int {
        val segments = segmentCounts[robotColor] ?: return 1
        return segments["$fromX,$fromY:$toX,$toY"] ?: 1
    }

    /**
     * Check if any paths exist.
     */
    fun hasPaths(): Boolean = robotPaths.isNotEmpty()

    /**
     * Get total number of path segments across all robots.
     */
    fun getTotalSegments(): Int = robotPaths.values.sumOf { maxOf(0, it.size - 1) }
}
