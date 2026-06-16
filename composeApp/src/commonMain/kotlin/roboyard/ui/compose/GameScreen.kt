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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.min
import driftingdroids.model.Board
import driftingdroids.model.SolverIDDFS
import driftingdroids.model.Solution

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

        // Draw grid cells
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val cellX = offsetX + x * cellSize
                val cellY = offsetY + y * cellSize

                // Draw cell background
                drawRect(
                    color = Color(0xFF3C3C3C),
                    topLeft = Offset(cellX, cellY),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )

                // Draw cell border
                drawRect(
                    color = Color(0xFF4C4C4C),
                    topLeft = Offset(cellX, cellY),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Draw walls
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val position = x + y * board.width
                val cellX = offsetX + x * cellSize
                val cellY = offsetY + y * cellSize

                // North wall
                if (board.walls[0][position]) {
                    drawLine(
                        color = Color(0xFF888888),
                        start = Offset(cellX, cellY),
                        end = Offset(cellX + cellSize, cellY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
                // East wall
                if (board.walls[1][position]) {
                    drawLine(
                        color = Color(0xFF888888),
                        start = Offset(cellX + cellSize, cellY),
                        end = Offset(cellX + cellSize, cellY + cellSize),
                        strokeWidth = 3.dp.toPx()
                    )
                }
                // South wall
                if (board.walls[2][position]) {
                    drawLine(
                        color = Color(0xFF888888),
                        start = Offset(cellX, cellY + cellSize),
                        end = Offset(cellX + cellSize, cellY + cellSize),
                        strokeWidth = 3.dp.toPx()
                    )
                }
                // West wall
                if (board.walls[3][position]) {
                    drawLine(
                        color = Color(0xFF888888),
                        start = Offset(cellX, cellY),
                        end = Offset(cellX, cellY + cellSize),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        // Draw robots
        for (i in board.robotPositions.indices) {
            val position = board.robotPositions[i]
            val robotX = position % board.width
            val robotY = position / board.width
            val centerX = offsetX + robotX * cellSize + cellSize / 2
            val centerY = offsetY + robotY * cellSize + cellSize / 2
            val radius = cellSize * 0.35f

            val robotColor = when (i) {
                0 -> Color(0xFFE74C3C) // Red
                1 -> Color(0xFF2ECC71) // Green
                2 -> Color(0xFF3498DB) // Blue
                3 -> Color(0xFFF1C40F) // Yellow
                4 -> Color(0xFF9B59B6) // Silver/Purple
                else -> Color(0xFF95A5A6)
            }

            drawCircle(
                color = robotColor,
                radius = radius,
                center = Offset(centerX, centerY)
            )
        }

        // Draw goal
        val goal = board.getGoal()
        if (goal != null) {
            val goalX = offsetX + goal.x * cellSize + cellSize / 2
            val goalY = offsetY + goal.y * cellSize + cellSize / 2
            val goalRadius = cellSize * 0.25f

            val goalColor = when (goal.robotNumber) {
                0 -> Color(0xFFE74C3C)
                1 -> Color(0xFF2ECC71)
                2 -> Color(0xFF3498DB)
                3 -> Color(0xFFF1C40F)
                4 -> Color(0xFF9B59B6)
                else -> Color(0xFF95A5A6)
            }

            // Draw goal as a hollow circle with the robot's color
            drawCircle(
                color = goalColor,
                radius = goalRadius,
                center = Offset(goalX, goalY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
