package roboyard.ui.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import roboyard.logic.core.GameElement
import roboyard.logic.core.GameState

/**
 * Utility class to generate minimap thumbnails of game boards.
 * This addresses the minimap display issue mentioned in the memory.
 */
class MinimapGenerator private constructor() {

    private val wallPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private val emptyPaint = Paint().apply {
        color = Color.rgb(200, 240, 200)
        style = Paint.Style.FILL
    }

    private val targetPaint = Paint().apply {
        color = Color.rgb(40, 40, 80)
        style = Paint.Style.FILL
    }

    private val robotPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    /**
     * Generate a minimap bitmap from a game state
     *
     * @param context Application context
     * @param state Game state to render
     * @param width Width of the minimap in pixels
     * @param height Height of the minimap in pixels
     * @return Bitmap containing the minimap
     */
    fun generateMinimap(context: Context, state: GameState?, width: Int, height: Int): Bitmap? {
        if (state == null) {
            return null
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(200, 240, 200))

        val cellWidth = width.toFloat() / state.width
        val cellHeight = height.toFloat() / state.height
        val cellSize = minOf(cellWidth, cellHeight)

        val offsetX = (width - (cellSize * state.width)) / 2
        val offsetY = (height - (cellSize * state.height)) / 2

        for (y in 0 until state.height) {
            for (x in 0 until state.width) {
                val left = offsetX + (x * cellSize)
                val top = offsetY + (y * cellSize)
                val right = left + cellSize
                val bottom = top + cellSize

                when (state.getCellType(x, y)) {
                    1 -> canvas.drawRect(left, top, right, bottom, wallPaint)
                    else -> canvas.drawRect(left, top, right, bottom, emptyPaint)
                }
            }
        }

        val gridPaint = Paint().apply {
            color = Color.rgb(140, 164, 140)
            strokeWidth = 1.0f
            isAntiAlias = true
        }

        for (x in 0..state.width) {
            val lineX = offsetX + (x * cellSize)
            canvas.drawLine(lineX, offsetY, lineX, offsetY + (cellSize * state.height), gridPaint)
        }

        for (y in 0..state.height) {
            val lineY = offsetY + (y * cellSize)
            canvas.drawLine(offsetX, lineY, offsetX + (cellSize * state.width), lineY, gridPaint)
        }

        val centerX = (state.width / 2) - 1
        val centerY = (state.height / 2) - 1
        val carreePaint = Paint().apply {
            color = Color.rgb(0, 100, 0)
            style = Paint.Style.FILL
        }

        val carreeLeft = offsetX + (centerX * cellSize)
        val carreeTop = offsetY + (centerY * cellSize)
        val carreeRight = carreeLeft + (2 * cellSize)
        val carreeBottom = carreeTop + (2 * cellSize)
        canvas.drawRect(carreeLeft, carreeTop, carreeRight, carreeBottom, carreePaint)

        val targetXPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1.5f, cellSize * 0.15f)
            isAntiAlias = true
        }
        for (element in state.gameElements) {
            if (element.type == GameElement.TYPE_TARGET) {
                val left = offsetX + (element.x * cellSize)
                val top = offsetY + (element.y * cellSize)
                val right = left + cellSize
                val bottom = top + cellSize
                val pad = cellSize * 0.2f

                when (element.color) {
                    0 -> targetXPaint.color = Color.rgb(255, 100, 150)
                    1 -> targetXPaint.color = Color.rgb(0, 180, 0)
                    2 -> targetXPaint.color = Color.rgb(50, 50, 255)
                    3 -> targetXPaint.color = Color.rgb(200, 200, 0)
                    else -> targetXPaint.color = Color.MAGENTA
                }

                canvas.drawLine(left + pad, top + pad, right - pad, bottom - pad, targetXPaint)
                canvas.drawLine(right - pad, top + pad, left + pad, bottom - pad, targetXPaint)
            }
        }

        val wallLinePaint = Paint().apply {
            color = Color.rgb(104, 131, 54)
            strokeWidth = maxOf(2.0f, cellSize * 0.2f)
            isAntiAlias = true
        }

        for (element in state.gameElements) {
            when (element.type) {
                GameElement.TYPE_HORIZONTAL_WALL -> {
                    val wallX1 = offsetX + (element.x * cellSize)
                    val wallY = offsetY + (element.y * cellSize)
                    val wallX2 = wallX1 + cellSize
                    canvas.drawLine(wallX1, wallY, wallX2, wallY, wallLinePaint)
                }
                GameElement.TYPE_VERTICAL_WALL -> {
                    val wallX = offsetX + (element.x * cellSize)
                    val wallY1 = offsetY + (element.y * cellSize)
                    val wallY2 = wallY1 + cellSize
                    canvas.drawLine(wallX, wallY1, wallX, wallY2, wallLinePaint)
                }
            }
        }

        for (element in state.gameElements) {
            if (element.type == GameElement.TYPE_ROBOT) {
                val centerXRobot = offsetX + ((element.x + 0.5f) * cellSize)
                val centerYRobot = offsetY + ((element.y + 0.5f) * cellSize)
                val radius = cellSize * 0.4f

                when (element.color) {
                    0 -> robotPaint.color = Color.rgb(255, 105, 180)
                    1 -> robotPaint.color = Color.rgb(0, 100, 0)
                    2 -> robotPaint.color = Color.BLUE
                    3 -> robotPaint.color = Color.YELLOW
                    else -> robotPaint.color = Color.MAGENTA
                }

                canvas.drawCircle(centerXRobot, centerYRobot, radius, robotPaint)
            }
        }

        return bitmap
    }

    companion object {
        @Volatile
        private var instance: MinimapGenerator? = null

        /**
         * Get the singleton instance of MinimapGenerator
         */
        @JvmStatic
        @Synchronized
        fun getInstance(): MinimapGenerator {
            return instance ?: MinimapGenerator().also { instance = it }
        }
    }
}
