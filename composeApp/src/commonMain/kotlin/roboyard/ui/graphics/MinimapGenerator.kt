package roboyard.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import driftingdroids.model.Board

/**
 * Utility class to generate minimap thumbnails of game boards.
 * This is a Compose-based version that works across platforms.
 */
object MinimapGenerator {

    /**
     * Draw a minimap on the given DrawScope
     *
     * @param scope DrawScope to draw on
     * @param board Board to render
     * @param width Width of the minimap in pixels
     * @param height Height of the minimap in pixels
     */
    fun drawMinimap(scope: DrawScope, board: Board?, width: Float, height: Float) {
        if (board == null) {
            return
        }

        val cellWidth = width / board.width
        val cellHeight = height / board.height
        val cellSize = minOf(cellWidth, cellHeight)

        val offsetX = (width - (cellSize * board.width)) / 2
        val offsetY = (height - (cellSize * board.height)) / 2

        // Draw background
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val left = offsetX + (x * cellSize)
                val top = offsetY + (y * cellSize)
                val right = left + cellSize
                val bottom = top + cellSize

                val cellColor = Color(200, 240, 200)
                scope.drawRect(cellColor, Offset(left, top), androidx.compose.ui.geometry.Size(right - left, bottom - top))
            }
        }

        // Draw grid lines
        val gridColor = Color(140, 164, 140)
        for (x in 0..board.width) {
            val lineX = offsetX + (x * cellSize)
            scope.drawLine(gridColor, Offset(lineX, offsetY), Offset(lineX, offsetY + (cellSize * board.height)))
        }

        for (y in 0..board.height) {
            val lineY = offsetY + (y * cellSize)
            scope.drawLine(gridColor, Offset(offsetX, lineY), Offset(offsetX + (cellSize * board.width), lineY))
        }

        // Draw center square (carree)
        val centerX = (board.width / 2) - 1
        val centerY = (board.height / 2) - 1
        val carreeColor = Color(0, 100, 0)
        val carreeLeft = offsetX + (centerX * cellSize)
        val carreeTop = offsetY + (centerY * cellSize)
        val carreeRight = carreeLeft + (2 * cellSize)
        val carreeBottom = carreeTop + (2 * cellSize)
        scope.drawRect(carreeColor, Offset(carreeLeft, carreeTop), androidx.compose.ui.geometry.Size(carreeRight - carreeLeft, carreeBottom - carreeTop))

        // Draw goals (targets)
        for (goal in board.goals) {
            val left = offsetX + (goal.x * cellSize)
            val top = offsetY + (goal.y * cellSize)
            val right = left + cellSize
            val bottom = top + cellSize
            val pad = cellSize * 0.2f

            val targetColor = when (goal.robotNumber) {
                0 -> Color(255, 100, 150)
                1 -> Color(0, 180, 0)
                2 -> Color(50, 50, 255)
                3 -> Color(200, 200, 0)
                else -> Color.Magenta
            }

            scope.drawLine(targetColor, Offset(left + pad, top + pad), Offset(right - pad, bottom - pad))
            scope.drawLine(targetColor, Offset(right - pad, top + pad), Offset(left + pad, bottom - pad))
        }

        // Draw walls
        val wallColor = Color(104, 131, 54)
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val pos = x + y * board.width
                // Check for horizontal walls (NORTH direction)
                if (board.walls[0][pos]) {
                    val wallX1 = offsetX + (x * cellSize)
                    val wallY = offsetY + (y * cellSize)
                    val wallX2 = wallX1 + cellSize
                    scope.drawLine(wallColor, Offset(wallX1, wallY), Offset(wallX2, wallY))
                }
                // Check for vertical walls (WEST direction)
                if (board.walls[3][pos]) {
                    val wallX = offsetX + (x * cellSize)
                    val wallY1 = offsetY + (y * cellSize)
                    val wallY2 = wallY1 + cellSize
                    scope.drawLine(wallColor, Offset(wallX, wallY1), Offset(wallX, wallY2))
                }
            }
        }

        // Draw robots
        for (i in board.robotPositions.indices) {
            val pos = board.robotPositions[i]
            val x = pos % board.width
            val y = pos / board.width
            val centerXRobot = offsetX + ((x + 0.5f) * cellSize)
            val centerYRobot = offsetY + ((y + 0.5f) * cellSize)
            val radius = cellSize * 0.4f

            val robotColor = when (i) {
                0 -> Color(255, 105, 180)
                1 -> Color(0, 100, 0)
                2 -> Color.Blue
                3 -> Color.Yellow
                else -> Color.Magenta
            }

            scope.drawCircle(robotColor, radius, Offset(centerXRobot, centerYRobot))
        }
    }
}
