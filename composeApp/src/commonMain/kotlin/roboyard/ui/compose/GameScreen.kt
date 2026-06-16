package roboyard.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt
import driftingdroids.model.Board
import roboyard.logic.core.LevelLoader
import driftingdroids.model.SolverIDDFS
import driftingdroids.model.Solution
import org.jetbrains.compose.resources.imageResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.grid_tiles
import roboyard.composeapp.generated.resources.roboyard
import roboyard.composeapp.generated.resources.mh
import roboyard.composeapp.generated.resources.mv
import roboyard.composeapp.generated.resources.robot_pink_left
import roboyard.composeapp.generated.resources.robot_pink_right
import roboyard.composeapp.generated.resources.robot_green_left
import roboyard.composeapp.generated.resources.robot_green_right
import roboyard.composeapp.generated.resources.robot_blue_left
import roboyard.composeapp.generated.resources.robot_blue_right
import roboyard.composeapp.generated.resources.robot_yellow_left
import roboyard.composeapp.generated.resources.robot_yellow_right
import roboyard.composeapp.generated.resources.robot_silver_left
import roboyard.composeapp.generated.resources.robot_silver_right
import roboyard.composeapp.generated.resources.target_pink
import roboyard.composeapp.generated.resources.target_green
import roboyard.composeapp.generated.resources.target_blue
import roboyard.composeapp.generated.resources.target_yellow
import roboyard.composeapp.generated.resources.target_silver
import roboyard.composeapp.generated.resources.target_multi

@Composable
fun GameScreen(
    board: Board,
    onBack: () -> Unit = {}
) {
    var moveCount by remember { mutableIntStateOf(0) }
    var currentBoard by remember { mutableStateOf(board) }
    var hintMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C2C2C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Button(onClick = onBack) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val solver = SolverIDDFS(currentBoard)
                val solutions = solver.execute()
                if (solutions.isNotEmpty()) {
                    val solution = solutions[0]
                    val firstMove = solution.getNextMove()
                    if (firstMove != null) {
                        val directionName = when (firstMove.direction) {
                            Board.NORTH -> "North"
                            Board.EAST -> "East"
                            Board.SOUTH -> "South"
                            Board.WEST -> "West"
                            else -> "Unknown"
                        }
                        hintMessage = "Hint: Move robot ${firstMove.robotNumber} $directionName"
                    } else {
                        hintMessage = "Already at goal!"
                    }
                } else {
                    hintMessage = "No solution found"
                }
            }) {
                Text("Hint")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Robots: ${currentBoard.numRobots} | Moves: $moveCount",
                color = Color.White,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        hintMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFF4CAF50),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        BoardCanvas(
            board = currentBoard,
            onRobotMove = { robotIndex, direction ->
                val newBoard = moveRobot(currentBoard, robotIndex, direction)
                if (newBoard != null) {
                    currentBoard = newBoard
                    moveCount++
                    hintMessage = null
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

fun moveRobot(board: Board, robotIndex: Int, direction: Int): Board? {
    val currentPos = board.robotPositions[robotIndex]
    var newPos = currentPos
    val directionIncrement = board.directionIncrement[direction]

    // Slide robot until it hits a wall or another robot
    while (true) {
        val nextPos = newPos + directionIncrement
        val x = nextPos % board.width
        val y = nextPos / board.width

        // Check bounds
        if (x < 0 || x >= board.width || y < 0 || y >= board.height) {
            break
        }

        // Check for wall
        if (board.isWall(newPos, direction)) {
            break
        }

        // Check for another robot
        var isBlocked = false
        for (i in board.robotPositions.indices) {
            if (i != robotIndex && board.robotPositions[i] == nextPos) {
                isBlocked = true
                break
            }
        }
        if (isBlocked) {
            break
        }

        newPos = nextPos
    }

    // Check if robot actually moved
    if (newPos == currentPos) {
        return null
    }

    // Create new board with updated robot position
    val newRobots = board.robotPositions.copyOf()
    newRobots[robotIndex] = newPos
    val newBoard = Board.Companion.createClone(board)
    newBoard.setRobots(newRobots)
    return newBoard
}

@Composable
fun BoardCanvas(
    board: Board,
    onRobotMove: (robotIndex: Int, direction: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var dragStartRobot by remember { mutableStateOf<Int?>(null) }
    var dragStartPos by remember { mutableStateOf<Offset?>(null) }

    // Load shared PNG assets (same drawables as the Android GameGridView)
    val gridTile = imageResource(Res.drawable.grid_tiles)
    val centerLogo = imageResource(Res.drawable.roboyard)
    val wallH = imageResource(Res.drawable.mh)
    val wallV = imageResource(Res.drawable.mv)
    val robotSprites = listOf(
        imageResource(Res.drawable.robot_pink_right),
        imageResource(Res.drawable.robot_green_right),
        imageResource(Res.drawable.robot_blue_right),
        imageResource(Res.drawable.robot_yellow_right),
        imageResource(Res.drawable.robot_silver_right)
    )
    val targetSprites = listOf(
        imageResource(Res.drawable.target_pink),
        imageResource(Res.drawable.target_green),
        imageResource(Res.drawable.target_blue),
        imageResource(Res.drawable.target_yellow),
        imageResource(Res.drawable.target_silver)
    )
    val targetMulti = imageResource(Res.drawable.target_multi)

    // Per-tile rotations (0/90/180/270) chosen once, like the Android renderer
    val tileRotations = remember(board.width, board.height) {
        IntArray(board.width * board.height) { (it * 90) % 360 }
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val cellSize = min(size.width, size.height) / maxOf(board.width, board.height).toFloat()
                    val offsetX = (size.width - board.width * cellSize) / 2
                    val offsetY = (size.height - board.height * cellSize) / 2

                    for (i in board.robotPositions.indices) {
                        val position = board.robotPositions[i]
                        val robotX = position % board.width
                        val robotY = position / board.width
                        val centerX = offsetX + robotX * cellSize + cellSize / 2
                        val centerY = offsetY + robotY * cellSize + cellSize / 2
                        val radius = cellSize * 0.35f

                        if (offset.x >= centerX - radius && offset.x <= centerX + radius &&
                            offset.y >= centerY - radius && offset.y <= centerY + radius) {
                            dragStartRobot = i
                            dragStartPos = offset
                            break
                        }
                    }
                },
                onDragEnd = {
                    dragStartRobot = null
                    dragStartPos = null
                },
                onDragCancel = {
                    dragStartRobot = null
                    dragStartPos = null
                },
                onDrag = { change, dragAmount ->
                    val robotIndex = dragStartRobot
                    val startPos = dragStartPos
                    if (robotIndex != null && startPos != null) {
                        val totalDrag = change.position - startPos
                        val threshold = 30f

                        if (totalDrag.x > threshold && kotlin.math.abs(totalDrag.y) < threshold) {
                            onRobotMove(robotIndex, Board.EAST)
                            dragStartRobot = null
                            dragStartPos = null
                        } else if (totalDrag.x < -threshold && kotlin.math.abs(totalDrag.y) < threshold) {
                            onRobotMove(robotIndex, Board.WEST)
                            dragStartRobot = null
                            dragStartPos = null
                        } else if (totalDrag.y > threshold && kotlin.math.abs(totalDrag.x) < threshold) {
                            onRobotMove(robotIndex, Board.SOUTH)
                            dragStartRobot = null
                            dragStartPos = null
                        } else if (totalDrag.y < -threshold && kotlin.math.abs(totalDrag.x) < threshold) {
                            onRobotMove(robotIndex, Board.NORTH)
                            dragStartRobot = null
                            dragStartPos = null
                        }
                    }
                }
            )
        }
    ) {
        val cellSize = min(size.width, size.height) / maxOf(board.width, board.height).toFloat()
        val offsetX = (size.width - board.width * cellSize) / 2
        val offsetY = (size.height - board.height * cellSize) / 2

        // 1. Grid tiles (rotated per cell, like gridTileDrawable in GameGridView)
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val cellX = offsetX + x * cellSize
                val cellY = offsetY + y * cellSize
                val rotation = tileRotations[x + y * board.width].toFloat()
                rotate(rotation, pivot = Offset(cellX + cellSize / 2, cellY + cellSize / 2)) {
                    drawImageScaled(gridTile, cellX, cellY, cellSize, cellSize)
                }
            }
        }

        // 2. Center logo in the 2x2 carree (matches backgroundLogo placement)
        val centerX0 = board.width / 2 - 1
        val centerY0 = board.height / 2 - 1
        if (centerX0 >= 0 && centerY0 >= 0) {
            drawImageScaled(
                centerLogo,
                offsetX + centerX0 * cellSize,
                offsetY + centerY0 * cellSize,
                cellSize * 2,
                cellSize * 2
            )
        }

        // 3. Targets (drawn before robots so robots sit on top)
        for (goal in board.goals) {
            val sprite = if (goal.robotNumber in targetSprites.indices) {
                targetSprites[goal.robotNumber]
            } else {
                targetMulti
            }
            drawImageScaled(
                sprite,
                offsetX + goal.x * cellSize,
                offsetY + goal.y * cellSize,
                cellSize,
                cellSize
            )
        }

        // 4. Walls using the mh/mv drawables (draw NORTH+WEST per cell, plus outer SOUTH/EAST)
        val wallThickness = cellSize * 0.18f
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val position = x + y * board.width
                val cellX = offsetX + x * cellSize
                val cellY = offsetY + y * cellSize
                if (board.walls[0][position]) { // NORTH
                    drawImageScaled(wallH, cellX, cellY - wallThickness / 2, cellSize, wallThickness)
                }
                if (board.walls[3][position]) { // WEST
                    drawImageScaled(wallV, cellX - wallThickness / 2, cellY, wallThickness, cellSize)
                }
                if (y == board.height - 1 && board.walls[2][position]) { // outer SOUTH
                    drawImageScaled(wallH, cellX, cellY + cellSize - wallThickness / 2, cellSize, wallThickness)
                }
                if (x == board.width - 1 && board.walls[1][position]) { // outer EAST
                    drawImageScaled(wallV, cellX + cellSize - wallThickness / 2, cellY, wallThickness, cellSize)
                }
            }
        }

        // 5. Robots using the color sprites
        for (i in board.robotPositions.indices) {
            val position = board.robotPositions[i]
            val robotX = position % board.width
            val robotY = position / board.width
            val sprite = if (i in robotSprites.indices) robotSprites[i] else robotSprites.last()
            val pad = cellSize * 0.08f
            drawImageScaled(
                sprite,
                offsetX + robotX * cellSize + pad,
                offsetY + robotY * cellSize + pad,
                cellSize - 2 * pad,
                cellSize - 2 * pad
            )
        }
    }
}

/** Draws an [ImageBitmap] scaled into the given destination rectangle. */
private fun DrawScope.drawImageScaled(
    image: ImageBitmap,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(width.roundToInt(), height.roundToInt())
    )
}
