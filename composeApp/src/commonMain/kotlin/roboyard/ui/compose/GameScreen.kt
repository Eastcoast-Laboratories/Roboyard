package roboyard.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import driftingdroids.model.Board
import roboyard.logic.core.LevelLoader
import roboyard.logic.core.LevelFormatParser
import roboyard.logic.core.ComposeGameState
import roboyard.logic.storage.getPlatformStorage
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
    var squaresMoved by remember { mutableIntStateOf(0) }
    var currentBoard by remember { mutableStateOf(board) }
    val startBoard = remember { Board.Companion.createClone(board).also { it.setRobots(board.robotPositions.copyOf()) } }
    var hintMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Game grid at top, full width, maintaining square aspect ratio
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
                .fillMaxWidth()
                .aspectRatio(currentBoard.width.toFloat() / currentBoard.height.toFloat())
        )

        // Hint container (visible when hint is active)
        if (hintMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xDD000000))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = hintMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        // Game info card below the board
        GameInfoCard(
            moveCount = moveCount,
            squaresMoved = squaresMoved,
            difficulty = "Beginner",
            timer = "00:00",
            hintMessage = hintMessage
        )

        // Flexible space pushes the buttons to the bottom
        Spacer(modifier = Modifier.weight(1f))

        // Accessibility section (hidden by default)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
        ) {
            // Accessibility controls placeholder
        }

        // Bottom button container (two rows of fancy buttons)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp)
            ) {
                FancyButton(
                    text = "Save Map",
                    color = FancyButtonColor.RED,
                    onClick = { },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = "💡Hint",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        val solver = SolverIDDFS(currentBoard)
                        val solutions = solver.execute()
                        hintMessage = if (solutions.isNotEmpty()) {
                            val firstMove = solutions[0].getNextMove()
                            if (firstMove != null) {
                                val directionName = when (firstMove.direction) {
                                    Board.NORTH -> "North"
                                    Board.EAST -> "East"
                                    Board.SOUTH -> "South"
                                    Board.WEST -> "West"
                                    else -> "Unknown"
                                }
                                "Hint: Move robot ${firstMove.robotNumber} $directionName"
                            } else "Already at goal!"
                        } else "No solution found"
                    },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = "Back",
                    color = FancyButtonColor.HINT,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                FancyButton(
                    text = "Menu",
                    color = FancyButtonColor.GRAY,
                    onClick = onBack,
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = "Reset",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        currentBoard = Board.Companion.createClone(startBoard).also {
                            it.setRobots(startBoard.robotPositions.copyOf())
                        }
                        moveCount = 0
                        squaresMoved = 0
                        hintMessage = null
                    },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = "New Game",
                    color = FancyButtonColor.GREEN,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Game information card shown below the board, matching the original
 * fragment-app 3-block layout: Left (60%) for Moves/Squares/Difficulty,
 * Center (20%) for Optimal Moves button (hidden), Right (30%) for Timer/Map ID.
 */
@Composable
fun GameInfoCard(
    moveCount: Int,
    squaresMoved: Int,
    difficulty: String,
    timer: String,
    hintMessage: String?
) {
    val cardBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D)),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBrush, shape)
            .border(BorderStroke(2.dp, Color(0xFF404040)), shape)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Left block (60%): Moves, Squares, Difficulty
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(end = 16.dp, bottom = 4.dp)
            ) {
                Text("Moves: $moveCount", color = Color(0xFFEEEEEE), fontSize = 10.sp)
                Text("Squares: $squaresMoved", color = Color(0xFFEEEEEE), fontSize = 8.sp, modifier = Modifier.padding(top = 2.dp))
                Text("Difficulty: $difficulty", color = Color(0xFFEEEEEE), fontSize = 8.sp, modifier = Modifier.padding(top = 2.dp))
            }
            // Center block (20%): Optimal Moves button (hidden)
            Box(modifier = Modifier.weight(0.2f)) {
                // Hidden optimal moves button
            }
            // Right block (30%): Timer, Map ID, Close button
            Column(
                modifier = Modifier.weight(0.3f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = timer,
                    color = Color(0xFFEEEEEE),
                    fontSize = 16.sp
                )
                Text(
                    text = "",
                    color = Color(0xFFEEEEEE),
                    fontSize = 8.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                // Close button placeholder
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 4.dp)
                )
            }
        }
        hintMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFF4CAF50),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
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
    val gameState = remember { ComposeGameState(board) }
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
        modifier = modifier
            .semantics {
                contentDescription = "Game board with ${board.width}x${board.height} cells, ${board.robotPositions.size} robots, and ${board.goals.size} targets"
            }
            .pointerInput(Unit) {
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
        //    plus the green grid stroke around each cell (#4ae600, 3px stroke)
        val gridStrokeColor = Color(0xFF4AE600)
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val cellX = offsetX + x * cellSize
                val cellY = offsetY + y * cellSize
                val rotation = tileRotations[x + y * board.width].toFloat()
                rotate(rotation, pivot = Offset(cellX + cellSize / 2, cellY + cellSize / 2)) {
                    drawImageScaled(gridTile, cellX, cellY, cellSize, cellSize)
                }
                // Green grid line around the cell (matches gridPaint in GameGridView)
                drawRect(
                    color = gridStrokeColor,
                    topLeft = Offset(cellX, cellY),
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = 3f)
                )
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

        // 4. Walls using the mh/mv drawables, matching WallRenderer's thickness
        //    (0.6 * cellSize) and overhang (0.24 * cellSize), skipping the center cross.
        val wallThickness = cellSize * 0.6f
        val wallOffset = cellSize * 0.24f
        val cWallX = board.width / 2 - 1
        val cWallY = board.height / 2 - 1
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val position = x + y * board.width
                val cellX = offsetX + x * cellSize
                val cellY = offsetY + y * cellSize
                // NORTH (horizontal wall at top edge of cell x,y)
                if (board.walls[0][position]) {
                    val isCenter = (y == cWallY + 1 && (x == cWallX || x == cWallX + 1))
                    if (!isCenter) {
                        drawImageScaled(
                            wallH,
                            cellX - wallOffset,
                            cellY - wallThickness / 2,
                            cellSize + 2 * wallOffset,
                            wallThickness
                        )
                    }
                }
                // WEST (vertical wall at left edge of cell x,y)
                if (board.walls[3][position]) {
                    val isCenter = (x == cWallX + 1 && (y == cWallY || y == cWallY + 1))
                    if (!isCenter) {
                        drawImageScaled(
                            wallV,
                            cellX - wallThickness / 2,
                            cellY - wallOffset,
                            wallThickness,
                            cellSize + 2 * wallOffset
                        )
                    }
                }
                // outer SOUTH
                if (y == board.height - 1 && board.walls[2][position]) {
                    drawImageScaled(
                        wallH,
                        cellX - wallOffset,
                        cellY + cellSize - wallThickness / 2,
                        cellSize + 2 * wallOffset,
                        wallThickness
                    )
                }
                // outer EAST
                if (x == board.width - 1 && board.walls[1][position]) {
                    drawImageScaled(
                        wallV,
                        cellX + cellSize - wallThickness / 2,
                        cellY - wallOffset,
                        wallThickness,
                        cellSize + 2 * wallOffset
                    )
                }
            }
        }

        // 5. Robots using the color sprites at DEFAULT_ROBOT_SCALE (1.1 * cellSize)
        val robotScale = 1.1f
        val robotInset = (robotScale - 1f) * cellSize / 2f
        for (i in board.robotPositions.indices) {
            val position = board.robotPositions[i]
            val robotX = position % board.width
            val robotY = position / board.width
            val sprite = if (i in robotSprites.indices) robotSprites[i] else robotSprites.last()
            drawImageScaled(
                sprite,
                offsetX + robotX * cellSize - robotInset,
                offsetY + robotY * cellSize - robotInset,
                cellSize * robotScale,
                cellSize * robotScale
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

/** Serializes a Board to a string format for saving. */
fun serializeBoard(board: Board): String {
    val sb = StringBuilder()
    sb.append("board:${board.width},${board.height};")
    
    // Serialize walls
    for (y in 0 until board.height) {
        for (x in 0 until board.width) {
            val pos = x + y * board.width
            if (board.walls[0][pos]) sb.append("h$x,$y;")
            if (board.walls[3][pos]) sb.append("v$x,$y;")
        }
    }
    
    // Serialize targets
    for (goal in board.goals) {
        val colorChar = when (goal.robotNumber) {
            0 -> 'p'
            1 -> 'g'
            2 -> 'b'
            3 -> 'y'
            4 -> 's'
            else -> 'm'
        }
        sb.append("t$colorChar${goal.x},${goal.y};")
    }
    
    // Serialize robots
    for (i in board.robotPositions.indices) {
        val pos = board.robotPositions[i]
        val x = pos % board.width
        val y = pos / board.width
        val colorChar = when (i) {
            0 -> 'p'
            1 -> 'g'
            2 -> 'b'
            3 -> 'y'
            4 -> 's'
            else -> 'p'
        }
        sb.append("r$colorChar$x,$y;")
    }
    
    return sb.toString()
}

/** Deserializes a Board from a string format for loading. */
fun deserializeBoard(data: String): Board? {
    val entries: List<LevelFormatParser.RawEntry> = LevelFormatParser.parseRawEntries(data)
    var width = 14
    var height = 14
    
    // First pass: extract board dimensions
    for (entry in entries) {
        if (entry.type == "board") {
            val data = entry.data
            val cleanData = if (data.startsWith(":")) data.substring(1) else data
            val parts = cleanData.split(",").map { it.trim() }
            if (parts.size == 2) {
                width = parts[0].toIntOrNull() ?: 14
                height = parts[1].toIntOrNull() ?: 14
            }
            break
        }
    }
    
    val board = Board.createBoardFreestyle(null, width, height, 4) ?: return null
    val numRobots = 4
    val robotPositions = IntArray(numRobots) { -1 }
    var robotIndex = 0
    
    // Second pass: parse walls, targets, robots
    for (entry in entries) {
        val type = entry.type
        val data = entry.data
        
        if (type == "board") continue
        
        val parts = data.split(",").map { it.trim() }
        if (parts.size < 2) continue
        val x = parts[0].toIntOrNull() ?: continue
        val y = parts[1].toIntOrNull() ?: continue
        
        when {
            type == "h" || type == "mh" -> {
                board.setWall(x, y, Board.NORTH, true)
                if (y > 0) board.setWall(x, y - 1, Board.SOUTH, true)
            }
            type == "v" || type == "mv" -> {
                board.setWall(x, y, Board.WEST, true)
                if (x > 0) board.setWall(x - 1, y, Board.EAST, true)
            }
            type.startsWith("t") -> {
                val colorId = parseColorChar(type)
                if (colorId >= -1) {
                    val pos = x + y * width
                    board.addGoal(pos, colorId, 0)
                }
            }
            type.startsWith("r") -> {
                val colorId = parseColorChar(type)
                if (colorId >= 0 && robotIndex < numRobots) {
                    robotPositions[robotIndex] = x + y * width
                    robotIndex++
                }
            }
        }
    }
    
    board.setRobots(robotPositions)
    return board
}

/** Parses color character (p/g/b/y/s) to robot index (0-4). */
private fun parseColorChar(type: String): Int {
    val char = if (type.length == 2) type[1] else type[0]
    return when (char) {
        'p' -> 0 // pink
        'g' -> 1 // green
        'b' -> 2 // blue
        'y' -> 3 // yellow
        's' -> 4 // silver
        else -> -1
    }
}
