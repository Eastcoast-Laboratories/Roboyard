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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import roboyard.logic.core.GridElement
import roboyard.logic.core.GameLogic
import roboyard.logic.core.Preferences
import roboyard.logic.storage.PlatformStorage

// Compose App Version - increment after each session
const val COMPOSE_APP_VERSION = "v1.9"

// Helper function to format time as MM:SS
fun formatTime(elapsedTimeMs: Long): String {
    val seconds = (elapsedTimeMs / 1000) % 60
    val minutes = (elapsedTimeMs / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun GameScreen(
    board: Board,
    onBack: () -> Unit = {},
    onNewGame: () -> Unit = {}
) {
    var moveCount by remember(board) { mutableIntStateOf(0) }
    var squaresMoved by remember(board) { mutableIntStateOf(0) }
    var currentBoard by remember(board) { mutableStateOf(board) }
    val startBoard = remember(board) { Board.Companion.createClone(board).also { it.setRobots(board.robotPositions.copyOf()) } }
    var hintMessage by remember(board) { mutableStateOf<String?>(null) }
    var gameWon by remember(board) { mutableStateOf(false) }
    var solution by remember(board) { mutableStateOf<driftingdroids.model.Solution?>(null) }
    var currentHintStep by remember(board) { mutableIntStateOf(0) }
    var isSolverRunning by remember(board) { mutableStateOf(false) }
    val boardHistory = remember(board) { mutableListOf<Board>() }
    var elapsedTime by remember(board) { mutableLongStateOf(0L) }
    var timerRunning by remember(board) { mutableStateOf(false) }
    var selectedRobotIndex by remember(board) { mutableIntStateOf(0) }
    var accessibilityControlsVisible by remember(board) { mutableStateOf(false) }

    // Timer effect - runs every second when timer is enabled
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerRunning) {
                delay(1000)
                if (timerRunning) {
                    elapsedTime += 1000
                }
            }
        }
    }

    // Start timer when game starts (on first move)
    LaunchedEffect(moveCount) {
        if (moveCount > 0 && !timerRunning && !gameWon) {
            timerRunning = true
        }
    }

    // Stop timer when game is won
    LaunchedEffect(gameWon) {
        if (gameWon) {
            timerRunning = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Game grid at top, full width, maintaining square aspect ratio
        BoardCanvas(
            board = currentBoard,
            onRobotMove = { robotIndex, direction ->
                if (!gameWon) {
                    val oldPos = currentBoard.robotPositions[robotIndex]
                    val newBoard = moveRobot(currentBoard, robotIndex, direction)
                    if (newBoard != null) {
                        // Save current board to history before move (for undo)
                        boardHistory.add(Board.Companion.createClone(currentBoard))
                        val newPos = newBoard.robotPositions[robotIndex]
                        val w = newBoard.width
                        val distance = kotlin.math.abs((newPos % w) - (oldPos % w)) +
                            kotlin.math.abs((newPos / w) - (oldPos / w))
                        currentBoard = newBoard
                        moveCount++
                        squaresMoved += distance
                        hintMessage = null
                        // [GAME_WIN] Check if the goal robot reached its target
                        if (newBoard.goals.isNotEmpty() && isSolved(newBoard)) {
                            gameWon = true
                            // Play win sound and show completion message
                            // Note: Sound playback is platform-specific and will be implemented separately
                            val completionMessage = "Game completed in $moveCount moves!"
                            hintMessage = completionMessage
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(board.width.toFloat() / board.height.toFloat())
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(0.dp))
        )

        // Game info row (move count, squares moved, timer)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDD000000))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Moves: $moveCount",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = "Squares: $squaresMoved",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = formatTime(elapsedTime),
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Accessibility controls container (visible when enabled)
        if (accessibilityControlsVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xDD000000))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Select robot button
                FancyButton(
                    text = "Select Robot",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        selectedRobotIndex = (selectedRobotIndex + 1) % currentBoard.robotPositions.size
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                // Direction buttons
                FancyButton(
                    text = "N",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = moveRobot(currentBoard, selectedRobotIndex, Board.NORTH)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isSolved(newBoard)) {
                                    gameWon = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "S",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = moveRobot(currentBoard, selectedRobotIndex, Board.SOUTH)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isSolved(newBoard)) {
                                    gameWon = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "E",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = moveRobot(currentBoard, selectedRobotIndex, Board.EAST)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isSolved(newBoard)) {
                                    gameWon = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "W",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = moveRobot(currentBoard, selectedRobotIndex, Board.WEST)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isSolved(newBoard)) {
                                    gameWon = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hint container (visible when hint is active)
        if (hintMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xDD000000))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous hint button
                FancyButton(
                    text = "◂",
                    color = FancyButtonColor.HINT,
                    onClick = { },
                    modifier = Modifier.height(32.dp)
                )
                // Hint text
                Text(
                    text = hintMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // Next hint button
                FancyButton(
                    text = "▸",
                    color = FancyButtonColor.HINT,
                    onClick = { },
                    modifier = Modifier.height(32.dp)
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
                .background(Color(0xFF303030))
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(0.dp))
        ) {
            // Accessibility controls placeholder
            // Top row: Announce, North, Select
            // Middle row: West, East
            // Bottom row: Selected Robot, South, Robot Goal
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
                    onClick = {
                        // Save current board state to storage
                        val storage = Preferences.storageProvider?.invoke()
                        if (storage != null) {
                            val saveData = buildString {
                                appendLine("width:${board.width}")
                                appendLine("height:${board.height}")
                                appendLine("moveCount:$moveCount")
                                appendLine("squaresMoved:$squaresMoved")
                                appendLine("robots:${board.robotPositions.joinToString(",")}")
                                // Save goals
                                for (goal in board.goals) {
                                    appendLine("goal:${goal.position},${goal.robotNumber}")
                                }
                            }
                            storage.putString("saved_game", saveData)
                            hintMessage = "Game saved!"
                        } else {
                            hintMessage = "Save failed: no storage"
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = if (isSolverRunning) "Calculating..." else "💡Hint",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        if (isSolverRunning) {
                            // Cancel solver (not implemented for now)
                            return@FancyButton
                        }

                        if (solution == null) {
                            // Calculate solution using SolverIDDFS
                            isSolverRunning = true
                            hintMessage = "Calculating solution..."

                            // Run solver in background thread
                            Thread {
                                try {
                                    val solver = driftingdroids.model.SolverIDDFS(currentBoard)
                                    val solutions = solver.execute()
                                    if (solutions.isNotEmpty()) {
                                        solution = solutions[0]
                                        currentHintStep = 0
                                        val firstMove = solution!!.getNextMove()
                                        if (firstMove != null) {
                                            val directionName = when (firstMove.direction) {
                                                Board.NORTH -> "North"
                                                Board.SOUTH -> "South"
                                                Board.EAST -> "East"
                                                Board.WEST -> "West"
                                                else -> "Unknown"
                                            }
                                            val colorName = when (firstMove.robotNumber) {
                                                0 -> "Pink"
                                                1 -> "Green"
                                                2 -> "Blue"
                                                3 -> "Yellow"
                                                4 -> "Silver"
                                                else -> "Unknown"
                                            }
                                            hintMessage = "Hint: Move $colorName robot $directionName"
                                        } else {
                                            hintMessage = "Already at goal!"
                                        }
                                    } else {
                                        hintMessage = "No solution found"
                                    }
                                } catch (e: Exception) {
                                    hintMessage = "Solver error: ${e.message}"
                                } finally {
                                    isSolverRunning = false
                                }
                            }.start()
                        } else {
                            // Show next hint
                            val nextMove = solution!!.getNextMove()
                            if (nextMove != null) {
                                val directionName = when (nextMove.direction) {
                                    Board.NORTH -> "North"
                                    Board.SOUTH -> "South"
                                    Board.EAST -> "East"
                                    Board.WEST -> "West"
                                    else -> "Unknown"
                                }
                                val colorName = when (nextMove.robotNumber) {
                                    0 -> "Pink"
                                    1 -> "Green"
                                    2 -> "Blue"
                                    3 -> "Yellow"
                                    4 -> "Silver"
                                    else -> "Unknown"
                                }
                                hintMessage = "Hint ${currentHintStep + 1}: Move $colorName robot $directionName"
                                currentHintStep++
                            } else {
                                hintMessage = "All hints shown"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = if (boardHistory.isNotEmpty()) "Undo" else "Back",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        if (boardHistory.isNotEmpty()) {
                            // Undo last move
                            val previousBoard = boardHistory.removeAt(boardHistory.size - 1)
                            currentBoard = previousBoard
                            moveCount--
                            // Recalculate squaresMoved (simplified - in fragment-app this is tracked in history)
                            squaresMoved = maxOf(0, squaresMoved - 1)
                            hintMessage = null
                            gameWon = false
                        } else {
                            // No history, go back to menu
                            onBack()
                        }
                    },
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
                        gameWon = false
                    },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "New Game",
                    color = FancyButtonColor.GREEN,
                    onClick = onNewGame,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

        // [GAME_WIN] Win overlay shown when the puzzle is solved
        if (gameWon) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(BorderStroke(2.dp, Color(0xFF4CAF50)), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Solved!",
                        color = Color(0xFF4CAF50),
                        fontSize = 28.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "Moves: $moveCount   Squares: $squaresMoved",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        FancyButton(
                            text = "Menu",
                            color = FancyButtonColor.GRAY,
                            onClick = onBack,
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        FancyButton(
                            text = "New Game",
                            color = FancyButtonColor.GREEN,
                            onClick = onNewGame,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
            .shadow(elevation = 8.dp, shape = shape)
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
                    text = COMPOSE_APP_VERSION,
                    color = Color(0xFFAAAAAA),
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

/**
 * [GAME_WIN] Checks whether the puzzle is solved: the active goal's robot is on
 * the goal position. For a multi-colored goal (robotNumber == -1) any robot counts.
 */
fun isSolved(board: Board): Boolean {
    val goal = board.getGoal() ?: return false // No goal set, not solved
    val goalRobot = goal.robotNumber
    return if (goalRobot in board.robotPositions.indices) {
        board.robotPositions[goalRobot] == goal.position
    } else {
        board.robotPositions.any { it == goal.position }
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
    val gameState = remember(board) { ComposeGameState(board) }
    // Use the board parameter directly - it will be updated by the parent
    val currentBoard = board

    // Tracking variables matching fragment-app GameGridView
    var hasMovedRobotInCurrentGesture by remember { mutableStateOf(false) }
    var lastMoveX by remember { mutableStateOf(-1) }
    var lastMoveY by remember { mutableStateOf(-1) }
    var pendingMoveDirectionX by remember { mutableStateOf(0) }
    var pendingMoveDirectionY by remember { mutableStateOf(0) }
    var robotActivatedBySwipe by remember { mutableStateOf(false) }
    var startTouchX by remember { mutableStateOf(-1f) }
    var startTouchY by remember { mutableStateOf(-1f) }
    var touchStartGridX by remember { mutableStateOf(-1) }
    var touchStartGridY by remember { mutableStateOf(-1) }
    var touchedRobot by remember { mutableStateOf<Int?>(null) }
    var robotMoveInitiated by remember { mutableStateOf(false) }

    // Constants matching fragment-app GameGridView
    val MIN_SWIPE_DISTANCE = 30f
    val ROBOT_MOVE_THRESHOLD = 50f

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

                        // ACTION_DOWN logic from fragment-app
                        hasMovedRobotInCurrentGesture = false
                        lastMoveX = -1
                        lastMoveY = -1
                        pendingMoveDirectionX = 0
                        pendingMoveDirectionY = 0
                        robotActivatedBySwipe = false

                        // Store initial touch position
                        startTouchX = offset.x
                        startTouchY = offset.y
                        val gridX = ((offset.x - offsetX) / cellSize).toInt()
                        val gridY = ((offset.y - offsetY) / cellSize).toInt()
                        touchStartGridX = gridX
                        touchStartGridY = gridY

                        println("[UI] ACTION_DOWN - Start touch: ($startTouchX, $startTouchY), Grid: ($gridX, $gridY)")
                        println("[UI] ACTION_DOWN - Robot positions: ${board.robotPositions.map { "${it % board.width},${it / board.width}" }}")

                        // Check if a robot was touched at the start (matching fragment-app)
                        var foundRobot = false
                        for (i in board.robotPositions.indices) {
                            val position = board.robotPositions[i]
                            val robotX = position % board.width
                            val robotY = position / board.width
                            val centerX = offsetX + robotX * cellSize + cellSize / 2
                            val centerY = offsetY + robotY * cellSize + cellSize / 2
                            val radius = cellSize * 0.35f

                            println("[UI] ACTION_DOWN - Checking robot $i at ($robotX, $robotY), center: ($centerX, $centerY), radius: $radius, touch: ($startTouchX, $startTouchY)")

                            if (offset.x >= centerX - radius && offset.x <= centerX + radius &&
                                offset.y >= centerY - radius && offset.y <= centerY + radius) {
                                touchedRobot = i
                                foundRobot = true
                                println("[UI] ACTION_DOWN - Robot $i touched at ($robotX, $robotY)")
                                break
                            }
                        }
                        // If no robot was found, ensure touchedRobot is null (matching fragment-app)
                        if (!foundRobot) {
                            touchedRobot = null
                            println("[UI] ACTION_DOWN - No robot touched")
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val cellSize = min(size.width, size.height) / maxOf(board.width, board.height).toFloat()
                        val offsetX = (size.width - board.width * cellSize) / 2
                        val offsetY = (size.height - board.height * cellSize) / 2
                        val gridX = ((change.position.x - offsetX) / cellSize).toInt()
                        val gridY = ((change.position.y - offsetY) / cellSize).toInt()

                        // ACTION_MOVE logic from fragment-app
                        // Check if we just moved over a robot and none was selected before
                        var robotAtCurrentPos: Int? = null
                        for (i in board.robotPositions.indices) {
                            val position = board.robotPositions[i]
                            val robotX = position % board.width
                            val robotY = position / board.width
                            if (robotX == gridX && robotY == gridY) {
                                robotAtCurrentPos = i
                                break
                            }
                        }

                        // Detect a robot if we pass over one and no robot is currently being moved
                        if (touchedRobot == null && robotAtCurrentPos != null && !hasMovedRobotInCurrentGesture && !robotMoveInitiated) {
                            // We found a robot while swiping
                            touchedRobot = robotAtCurrentPos

                            // Update the start position for calculating movement direction
                            startTouchX = change.position.x
                            startTouchY = change.position.y
                            touchStartGridX = gridX
                            touchStartGridY = gridY

                            // Mark that we activated this robot by swiping
                            robotActivatedBySwipe = true

                            println("[UI] ACTION_MOVE - Robot $touchedRobot activated by swipe at ($gridX, $gridY)")

                            // Return without movement - require additional swiping to move
                            return@detectDragGestures
                        }

                        // If we have a touched/selected robot, calculate the movement direction
                        if (touchedRobot != null && startTouchX >= 0 && startTouchY >= 0) {
                            // Calculate the distance moved from the start position
                            val deltaX = change.position.x - startTouchX
                            val deltaY = change.position.y - startTouchY
                            val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

                            // For an activated robot, we need a larger swipe to start moving
                            val movementThreshold = if (robotActivatedBySwipe) ROBOT_MOVE_THRESHOLD else MIN_SWIPE_DISTANCE

                            println("[UI] ACTION_MOVE - Robot $touchedRobot, Distance: $distance, Threshold: $movementThreshold, robotMoveInitiated: $robotMoveInitiated")

                            // Only process if the distance exceeds the threshold
                            if (distance >= movementThreshold) {
                                // Determine the dominant direction (horizontal or vertical)
                                val dx = if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)) {
                                    if (deltaX > 0) 1 else -1
                                } else {
                                    0
                                }
                                val dy = if (kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)) {
                                    if (deltaY > 0) 1 else -1
                                } else {
                                    0
                                }

                                // For swipe gestures, move the robot immediately
                                // But only if no robot is currently moving
                                if (!robotMoveInitiated) {
                                    robotMoveInitiated = true

                                    // Reset the activation flag since we're proceeding with the move
                                    robotActivatedBySwipe = false

                                    val direction = if (dx > 0) Board.EAST else if (dx < 0) Board.WEST else if (dy > 0) Board.SOUTH else Board.NORTH
                                    println("[UI] ACTION_MOVE - Moving robot $touchedRobot direction: $direction (dx=$dx, dy=$dy)")
                                    onRobotMove(touchedRobot!!, direction)

                                    // Record that we've moved a robot in this gesture
                                    hasMovedRobotInCurrentGesture = true

                                    // Reset starting position for next movement
                                    startTouchX = change.position.x
                                    startTouchY = change.position.y

                                    // After a successful move, reset all swipe and activation state.
                                    // This ensures that the robot cannot be activated or moved again
                                    // until the user performs a new ACTION_DOWN on a robot.
                                    touchedRobot = null
                                    startTouchX = -1f
                                    startTouchY = -1f
                                    touchStartGridX = -1
                                    touchStartGridY = -1
                                    pendingMoveDirectionX = 0
                                    pendingMoveDirectionY = 0
                                    robotActivatedBySwipe = false

                                    println("[UI] ACTION_MOVE - Move completed, tracking variables reset")
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        val cellSize = min(size.width, size.height) / maxOf(board.width, board.height).toFloat()
                        val offsetX = (size.width - board.width * cellSize) / 2
                        val offsetY = (size.height - board.height * cellSize) / 2

                        // ACTION_UP logic from fragment-app
                        println("[UI] ACTION_UP - touchedRobot: $touchedRobot, hasMovedRobotInCurrentGesture: $hasMovedRobotInCurrentGesture")

                        // Check if this was a tap (no significant movement)
                        // We need to store the initial touch position to calculate the distance
                        // For now, we'll just reset all tracking variables
                        // The tap logic will be implemented in a future iteration

                        // Reset all tracking variables (matching fragment-app ACTION_UP)
                        touchedRobot = null
                        startTouchX = -1f
                        startTouchY = -1f
                        touchStartGridX = -1
                        touchStartGridY = -1
                        pendingMoveDirectionX = 0
                        pendingMoveDirectionY = 0
                        robotActivatedBySwipe = false
                        robotMoveInitiated = false

                        println("[UI] ACTION_UP - Tracking variables reset")
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

/** Parses color character (r/g/b/y/s) to robot index (0-4). Matches fragment-app parseColorChar logic 1:1 */
private fun parseColorChar(type: String): Int {
    val char = if (type.length == 2) type[1] else type[0]
    return when (char) {
        'r' -> 0 // red (pink)
        'g' -> 1 // green
        'b' -> 2 // blue
        'y' -> 3 // yellow
        's' -> 4 // silver
        else -> -1
    }
}

/**
 * Converts a GridElement list (from GameLogic) to a Board instance.
 * This is used for random game generation with GameLogic.
 * Matches fragment-app GameState.createRandom logic 1:1
 */
fun gridElementsToBoard(gridElements: ArrayList<GridElement>): Board? {
    // Use Preferences for board dimensions
    val width = Preferences.boardSizeWidth
    val height = Preferences.boardSizeHeight

    val board = Board.createBoardFreestyle(null, width, height, 4) ?: return null
    val numRobots = 4
    val robotPositions = IntArray(numRobots) { -1 }

    // Parse walls, targets, robots from GridElements (matching fragment-app GameState.createRandom)
    for (element in gridElements) {
        val type = element.type
        val x = element.x
        val y = element.y

        when (type) {
            "h", "mh" -> {
                board.setWall(x, y, Board.NORTH, true)
                if (y > 0) board.setWall(x, y - 1, Board.SOUTH, true)
            }
            "v", "mv" -> {
                board.setWall(x, y, Board.WEST, true)
                if (x > 0) board.setWall(x - 1, y, Board.EAST, true)
            }
            "target_red" -> {
                val pos = x + y * width
                board.addGoal(pos, 0, 0) // COLOR_PINK = 0
            }
            "target_green" -> {
                val pos = x + y * width
                board.addGoal(pos, 1, 0) // COLOR_GREEN = 1
            }
            "target_blue" -> {
                val pos = x + y * width
                board.addGoal(pos, 2, 0) // COLOR_BLUE = 2
            }
            "target_yellow" -> {
                val pos = x + y * width
                board.addGoal(pos, 3, 0) // COLOR_YELLOW = 3
            }
            "target_silver" -> {
                val pos = x + y * width
                board.addGoal(pos, 4, 0) // COLOR_SILVER = 4
            }
            "target_multi" -> {
                // Multi-color target - skip (not supported in Board.addGoal with -1)
            }
            "robot_red" -> {
                val pos = x + y * width
                robotPositions[0] = pos
            }
            "robot_green" -> {
                val pos = x + y * width
                robotPositions[1] = pos
            }
            "robot_blue" -> {
                val pos = x + y * width
                robotPositions[2] = pos
            }
            "robot_yellow" -> {
                val pos = x + y * width
                robotPositions[3] = pos
            }
            "robot_silver" -> {
                val pos = x + y * width
                robotPositions[4] = pos
            }
        }
    }

    board.setRobots(robotPositions)
    board.setGoalRandom()
    return board
}
