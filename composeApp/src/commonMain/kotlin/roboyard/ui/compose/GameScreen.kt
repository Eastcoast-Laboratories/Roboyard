package roboyard.ui.compose

import roboyard.logic.core.calculateStars
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.CancellationException
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import driftingdroids.model.Board
import driftingdroids.model.isTrivialPuzzle
import roboyard.logic.core.LevelLoader
import roboyard.logic.core.LevelFormatParser
import roboyard.logic.core.ComposeGameState
import roboyard.logic.storage.getPlatformStorage
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
import roboyard.logic.core.GameLogic
import roboyard.logic.core.Preferences
import roboyard.logic.core.calculateStars
import roboyard.logic.core.saveLevelCompletion
import roboyard.logic.core.formatElapsedTime
import roboyard.logic.core.buildGameWinMessage
import roboyard.logic.core.isBoardSolved
import roboyard.logic.core.moveRobotOnBoard
import roboyard.logic.core.GameController
import roboyard.logic.core.PathTracker
import roboyard.logic.core.HintManager
import roboyard.logic.core.MapGenerationDecision
import roboyard.logic.core.MapGenerationRejectionReason
import roboyard.logic.core.MapGenerationValidator
import roboyard.logic.core.GameElement
import roboyard.logic.managers.GameSession
import roboyard.logic.solver.RRGameMove
import roboyard.logic.audio.SoundManager
import roboyard.logic.audio.getSoundManager
import roboyard.logic.ui.getStringProvider
import roboyard.logic.core.serializeBoard
import roboyard.logic.core.deserializeBoard
import roboyard.logic.storage.PlatformStorage

// Compose App Version - increment after each session
const val COMPOSE_APP_VERSION = "v1.9"

/** Convert ERRGameMove bitmask directions (1/2/4/8) to Board direction constants (NORTH=0..WEST=3). */
private fun errDirToBoardDir(direction: Int): Int {
    return when (direction) {
        1 -> Board.NORTH
        2 -> Board.EAST
        4 -> Board.SOUTH
        8 -> Board.WEST
        else -> Board.NORTH
    }
}

// Helper function to handle game win logic (DRY - delegates to shared buildGameWinMessage)
fun handleGameWin(
    moveCount: Int,
    isLevelGame: Boolean = false,
    optimalMoves: Int? = null,
    stars: Int = 0
): String = buildGameWinMessage(moveCount, isLevelGame, optimalMoves, stars)

@Composable
fun GameScreen(
    session: GameSession,
    isLevelGame: Boolean = false,
    isLoadedGame: Boolean = false,
    levelId: Int = 1,
    onBack: () -> Unit = {},
    onNewGame: () -> Unit = {},
    onSaveLoad: () -> Unit = {},
    onNextLevel: () -> Unit = {}
) {
    val storage = remember { getPlatformStorage() }
    val stringProvider = remember { getStringProvider() }
    val soundManager = remember { getSoundManager() }
    val hintManager = remember { HintManager(stringProvider) }
    val pathTracker = remember { PathTracker() }

    // Session-observed state (StateFlow mirrors the Android LiveData fields)
    val gameState by session.currentState.collectAsState()
    val stateRevision by session.stateRevision.collectAsState()
    val gameEpoch by session.gameCounter.collectAsState()
    val moveCount by session.moveCount.collectAsState()
    val squaresMoved by session.squaresMoved.collectAsState()
    val gameWon by session.isGameComplete.collectAsState()
    val isSolverRunning by session.isSolverRunning.collectAsState()
    val sessionSolution by session.solutionFlow.collectAsState()
    val wrongRobotAtTarget by session.wrongRobotAtTarget.collectAsState()

    // Derived Board used purely as render model (least churn per plan)
    val renderBoard = remember(stateRevision) {
        gameState?.let { gridElementsToBoard(ArrayList(it.gridElements.filterNotNull()), it.width, it.height) }
    }
    val startBoard = remember(gameEpoch) {
        renderBoard?.let { b ->
            Board.createClone(b).also { clone ->
                val posMap = gameState?.initialRobotPositions
                if (posMap != null) {
                    val arr = IntArray(b.robotPositions.size) { i ->
                        val p = posMap[i]
                        if (p != null) p[1] * b.width + p[0] else b.robotPositions[i]
                    }
                    clone.setRobots(arr)
                }
            }
        }
    }

    var hintMessage by remember(gameEpoch) { mutableStateOf<String?>(null) }
    var hintContainerVisible by remember(gameEpoch) { mutableStateOf(false) }
    var pendingAutoAdvance by remember(gameEpoch) { mutableStateOf(false) }
    var maxHintUsed by remember(gameEpoch) { mutableIntStateOf(-1) }
    var elapsedTime by remember(gameEpoch) { mutableLongStateOf(0L) }
    var timerRunning by remember(gameEpoch) { mutableStateOf(false) }
    var selectedRobotHasMoved by remember(gameEpoch) { mutableStateOf(false) }
    var accessibilityControlsVisible by remember { mutableStateOf(Preferences.accessibilityMode) }
    var generationFallback by remember { mutableStateOf<MapGenerationDecision?>(null) }
    var lastAutosaveTime by remember { mutableLongStateOf(0L) }
    var autosaveRunning by remember { mutableStateOf(false) }

    val selectedRobotIndex = gameState?.getSelectedRobot()?.color ?: -1

    val AUTOSAVE_INTERVAL_MS = 60 * 1000 // 60 seconds (same as main game)

    // Wire session hooks for Compose visuals (reverse-move undo, fallback dialog)
    LaunchedEffect(session) {
        session.onReverseMoveUndo = { _, undoneRobotColor ->
            pathTracker.undoLastPathSegment(undoneRobotColor)
        }
        session.onMapFallbackAccepted = { attempts, moves ->
            generationFallback = MapGenerationDecision(
                accepted = true,
                shouldRetry = false,
                usedFallback = true,
                attempt = attempts,
                moveCount = moves,
                rejectionReason = MapGenerationRejectionReason.NO_SOLUTION
            )
        }
    }

    // Autosave control: start on first move, stop on win (same as main game)
    LaunchedEffect(moveCount, gameWon) {
        if (moveCount > 0 && !autosaveRunning && !gameWon) {
            autosaveRunning = true
            lastAutosaveTime = System.currentTimeMillis()
        }
        if (gameWon) {
            autosaveRunning = false
        }
    }

    // Autosave timer - writes via GameSession.saveGame (Android format, slot 0)
    LaunchedEffect(autosaveRunning) {
        if (autosaveRunning) {
            while (autosaveRunning) {
                delay(1000)
                if (autosaveRunning &&
                    System.currentTimeMillis() - lastAutosaveTime >= AUTOSAVE_INTERVAL_MS
                ) {
                    session.saveGame(0, isAutoSave = true)
                    println("[AUTOSAVE] Autosaved to slot 0")
                    lastAutosaveTime = System.currentTimeMillis()
                }
            }
        }
    }

    // Initialize HintManager when the session solver produced a solution
    LaunchedEffect(sessionSolution) {
        val sol = sessionSolution ?: return@LaunchedEffect
        if (sol.moves.isNotEmpty()) {
            val moves = sol.moves.mapNotNull { m ->
                (m as? RRGameMove)?.let { Pair(it.color, errDirToBoardDir(it.direction)) }
            }
            hintManager.initialize(moves, isLevelGame, levelId)
        }
    }

    // Helper: get localized robot color name for button text (matches Android)
    fun getRobotColorName(colorIndex: Int): String {
        val key = when (colorIndex) {
            0 -> "color_red"
            1 -> "color_green"
            2 -> "color_blue"
            3 -> "color_yellow"
            4 -> "color_silver"
            5 -> "color_pink"
            6 -> "color_brown"
            7 -> "color_orange"
            8 -> "color_white"
            else -> return "Robot"
        }
        return stringProvider.getString(key) ?: when (colorIndex) {
            0 -> "Red"; 1 -> "Green"; 2 -> "Blue"; 3 -> "Yellow"; 4 -> "Silver"
            else -> "Robot"
        }
    }

    // Helper: get localized direction name for button text (matches Android)
    fun getDirectionName(direction: Int): String {
        val key = when (direction) {
            Board.NORTH -> "direction_north"
            Board.SOUTH -> "direction_south"
            Board.EAST -> "direction_east"
            Board.WEST -> "direction_west"
            else -> return ""
        }
        return stringProvider.getString(key) ?: when (direction) {
            Board.NORTH -> "North"; Board.SOUTH -> "South"
            Board.EAST -> "East"; Board.WEST -> "West"
            else -> ""
        }
    }

    // Helper: get FancyButtonColor matching robot color (matches Android tint)
    fun getRobotButtonColor(colorIndex: Int): FancyButtonColor {
        return when (colorIndex) {
            0 -> FancyButtonColor.RED    // red
            1 -> FancyButtonColor.GREEN  // green
            2 -> FancyButtonColor.BLUE   // blue
            3 -> FancyButtonColor.YELLOW // yellow
            else -> FancyButtonColor.GRAY // silver/pink/brown/etc
        }
    }

    // Helper: get hint container background color based on robot color (matches Android)
    fun getHintBackgroundColor(robotColorIndex: Int): Color {
        return when (robotColorIndex) {
            0 -> Color(0xFFff80e0) // pink — stronger
            1 -> Color(0xFFb5f874) // green
            2 -> Color(0xFF4080ff) // blue — stronger
            3 -> Color(0xFFfffe71) // yellow
            4 -> Color(0xFFc0c0c0) // silver
            5 -> Color(0xFFf77070) // red
            6 -> Color(0xFFa0522d) // brown
            7 -> Color(0xFFffa77f) // orange
            8 -> Color(0xFFf0f0f0) // white
            else -> Color(0xFFfffe71) // default — yellowish like Android
        }
    }

    var currentHintStep by remember(gameEpoch) { mutableIntStateOf(0) }
    var currentHintRobot by remember(gameEpoch) { mutableIntStateOf(-1) }
    var currentHintDirection by remember(gameEpoch) { mutableIntStateOf(-1) }
    var currentHintRobotColor by remember(gameEpoch) { mutableIntStateOf(-1) }

    // Helper: update hint display from HintManager (only GUI logic here, no hint logic)
    fun updateHintDisplay() {
        val hintData = hintManager.getHintForDisplay()
        if (hintData != null) {
            hintMessage = hintData.text
            currentHintRobotColor = hintData.robotColorForBackground
            val regularHint = hintManager.getRegularHint()
            if (regularHint != null) {
                currentHintRobot = regularHint.first
                currentHintDirection = regularHint.second
            } else {
                currentHintRobot = -1
                currentHintDirection = -1
            }
        } else {
            hintMessage = null
            currentHintRobotColor = -1
            currentHintRobot = -1
            currentHintDirection = -1
        }
    }

    var hintsUsed by remember(gameEpoch) { mutableIntStateOf(0) }

    // Record that a hint was shown (session stats + GameState tracking + history)
    fun recordHintShown() {
        val step = hintManager.getCurrentHintStep()
        maxHintUsed = maxOf(maxHintUsed, step)
        hintsUsed++
        session.recordHintShown(step)
        session.saveToHistoryNow("hint_shown_$step")
    }

    fun requestManualNewGame() {
        generationFallback = null
        onNewGame()
    }

    // Auto-advance hint after 1s delay when player follows the hint (matches Android)
    LaunchedEffect(pendingAutoAdvance) {
        if (pendingAutoAdvance) {
            delay(1000)
            if (hintManager.hasNextHint()) {
                hintManager.nextHint()
                updateHintDisplay()
                recordHintShown()
            } else {
                hintMessage = stringProvider.getString("all_hints_shown") ?: "All hints shown"
            }
            pendingAutoAdvance = false
        }
    }

    // Timer effect - runs every 500ms when timer is enabled (matches Android)
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerRunning) {
                delay(500)
                if (timerRunning) {
                    elapsedTime += 500
                    // Session handles history threshold + view_1_hour achievement
                    session.updateGameTimer()
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

    // Stop timer when game is won and show completion message (matches Android)
    LaunchedEffect(gameWon) {
        if (gameWon) {
            timerRunning = false
            session.saveToHistoryNow("completed")
            soundManager.playSound("win")
            val optimalMoves = sessionSolution?.moves?.size ?: 0
            val stars = calculateStars(moveCount, optimalMoves, maxHintUsed)
            val finalStars = if (stars < 1 && isLevelGame && levelId <= 10) 1 else stars
            hintContainerVisible = true
            hintMessage = if (isLevelGame) {
                val starStr = buildString { repeat(finalStars) { append("\u2605 ") } }.trim()
                if (starStr.isEmpty()) "Level $levelId Complete!" else "Level $levelId Complete! $starStr"
            } else {
                if (moveCount == optimalMoves && optimalMoves > 0) {
                    "Perfect! You found the optimal solution!"
                } else if (optimalMoves > 0) {
                    "Completed in $moveCount moves (optimal: $optimalMoves)"
                } else {
                    "Completed in $moveCount moves!"
                }
            }
        }
    }

    val board = renderBoard
    val startB = startBoard
    if (board == null || startB == null) {
        // Map is still being generated / loaded
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringProvider.getString("ai_calculating") ?: "Calculating...",
                color = Color.White,
                fontSize = 18.sp
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Game grid at top, full width, maintaining square aspect ratio
        androidx.compose.runtime.key(board.robotPositions.contentHashCode()) {
            BoardCanvas(
                board = board,
                startBoard = startB,
                pathTracker = pathTracker,
                selectedRobotIndex = selectedRobotIndex,
                selectedRobotHasMoved = selectedRobotHasMoved,
                onRobotSelected = { robotIndex ->
                    session.selectRobotByColor(robotIndex)
                    selectedRobotHasMoved = false
                },
                onRobotMove = { robotIndex, direction ->
                    val pathSizeBefore = session.pathHistory.size
                    val robot = session.selectRobotByColor(robotIndex)
                    if (robot != null) {
                        val oldX = robot.x
                        val oldY = robot.y
                        if (session.moveRobot(direction)) {
                            if (session.pathHistory.size == pathSizeBefore) {
                                // Forward move — UI owns path rendering
                                val moved = session.currentState.value?.gameElements
                                    ?.firstOrNull { it.type == GameElement.TYPE_ROBOT && it.color == robotIndex }
                                if (moved != null) {
                                    pathTracker.addPathSegment(robotIndex, oldX, oldY, moved.x, moved.y)
                                    session.addPathToHistory(robotIndex, oldX, oldY, moved.x, moved.y)
                                }
                                // Play move sound (matches Android SoundManager)
                                soundManager.playSound("move")
                                selectedRobotHasMoved = true

                                // Check if player followed the current hint (auto-advance)
                                if (hintMessage != null && robotIndex == currentHintRobot && direction == currentHintDirection) {
                                    pendingAutoAdvance = true
                                } else if (hintMessage != null) {
                                    hintMessage = null
                                }
                            }
                            // else: reverse-move undo — session popped pathHistory
                            // and onReverseMoveUndo updated the pathTracker
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(board.width.toFloat() / board.height.toFloat())
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(0.dp))
            )
        }

        // Hint container (between grid and info row, matches Android position)
        AnimatedVisibility(
            visible = hintContainerVisible && hintMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val hintBgColor = if (currentHintRobotColor >= 0) getHintBackgroundColor(currentHintRobotColor) else getHintBackgroundColor(-1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(hintBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FancyButton(
                    text = "\u2715",
                    color = FancyButtonColor.GRAY,
                    onClick = {
                        hintContainerVisible = false
                        hintMessage = null
                    },
                    modifier = Modifier.height(32.dp).width(32.dp).padding(end = 4.dp)
                )
                FancyButton(
                    text = "\u25C2",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        if (hintManager.hasPrevHint()) {
                            hintManager.prevHint()
                            updateHintDisplay()
                        }
                    },
                    modifier = Modifier.height(32.dp)
                )
                Text(
                    text = hintMessage ?: "",
                    color = Color(0xFF1A1A1A),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                val optMoves = sessionSolution?.moves?.size ?: 0
                if (optMoves > 0) {
                    val optimalButtonColor = when (optMoves % 5) {
                        0 -> FancyButtonColor.RED
                        1 -> FancyButtonColor.GREEN
                        2 -> FancyButtonColor.YELLOW
                        3 -> FancyButtonColor.BLUE
                        4 -> FancyButtonColor.GRAY
                        else -> FancyButtonColor.HINT
                    }
                    FancyButton(
                        text = optMoves.toString(),
                        color = optimalButtonColor,
                        onClick = {
                            if (hintManager.hasNextHint()) {
                                hintManager.nextHint()
                                updateHintDisplay()
                                recordHintShown()
                            }
                        },
                        modifier = Modifier.height(32.dp).width(48.dp)
                    )
                }
                FancyButton(
                    text = "\u25B8",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        if (hintManager.hasNextHint()) {
                            hintManager.nextHint()
                            updateHintDisplay()
                            recordHintShown()
                        }
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

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
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 10.sp)) { append("Moves: ") }
                    withStyle(SpanStyle(fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { append("$moveCount") }
                },
                color = Color.White
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 9.sp)) { append("Squares: ") }
                    withStyle(SpanStyle(fontSize = 14.sp)) { append("$squaresMoved") }
                },
                color = Color.White
            )
            Text(
                text = formatElapsedTime(elapsedTime),
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
                val robotColorName = if (selectedRobotIndex >= 0) getRobotColorName(selectedRobotIndex) else "Robot"
                val robotButtonColor = if (selectedRobotIndex >= 0) getRobotButtonColor(selectedRobotIndex) else FancyButtonColor.BLUE
                FancyButton(
                    text = robotColorName,
                    color = robotButtonColor,
                    onClick = {
                        val robotCount = gameState?.getRobotCount() ?: 4
                        session.selectRobotByColor((selectedRobotIndex + 1) % robotCount)
                        selectedRobotHasMoved = false
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.NORTH)}",
                    color = robotButtonColor,
                    onClick = { session.moveRobot(Board.NORTH) },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.SOUTH)}",
                    color = robotButtonColor,
                    onClick = { session.moveRobot(Board.SOUTH) },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.EAST)}",
                    color = robotButtonColor,
                    onClick = { session.moveRobot(Board.EAST) },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.WEST)}",
                    color = robotButtonColor,
                    onClick = { session.moveRobot(Board.WEST) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Flexible space pushes the buttons to the bottom
        Spacer(modifier = Modifier.weight(1f))

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
                // Dice button: visible only when generateNewMapEachTime=false and not in level game (matches Android)
                if (!isLevelGame && !Preferences.generateNewMapEachTime) {
                    FancyButton(
                        text = "\uD83C\uDFB2",
                        color = FancyButtonColor.GRAY,
                        onClick = {
                            requestManualNewGame()
                        },
                        modifier = Modifier.weight(1f).padding(end = 3.dp)
                    )
                }
                // Save Map button: hidden in level games, disabled during solver (matches Android)
                if (!isLevelGame && !isSolverRunning) {
                    FancyButton(
                        text = "Save Map",
                        color = FancyButtonColor.RED,
                        onClick = {
                            onSaveLoad()
                        },
                        modifier = Modifier.weight(1f).padding(end = 3.dp)
                    )
                }
                FancyButton(
                    text = if (hintContainerVisible) "\u274C Hint" else if (isSolverRunning) "Calculating..." else "\uD83D\uDCA1Hint",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        println("[HINT] Hint button clicked, isSolverRunning=$isSolverRunning, solution=$sessionSolution")

                        // If hint container is visible, toggle OFF (matches Android)
                        if (hintContainerVisible) {
                            hintContainerVisible = false
                            hintMessage = null
                            currentHintRobotColor = -1
                            currentHintRobot = -1
                            currentHintDirection = -1
                            hintManager.resetStep()
                            return@FancyButton
                        }

                        // Toggle ON: show hint container
                        hintContainerVisible = true

                        if (sessionSolution == null || sessionSolution!!.moves.isEmpty()) {
                            hintMessage = if (isSolverRunning) {
                                stringProvider.getString("ai_calculating") ?: "Calculating..."
                            } else {
                                stringProvider.getString("no_solution_found") ?: "No solution found"
                            }
                        } else {
                            println("[HINT] Solution exists, showing current hint")
                            hintContainerVisible = true
                            if (!hintManager.hasSolution()) {
                                val moves = sessionSolution!!.moves.mapNotNull { m ->
                                    (m as? RRGameMove)?.let { Pair(it.color, errDirToBoardDir(it.direction)) }
                                }
                                hintManager.initialize(moves, isLevelGame, levelId)
                            }
                            updateHintDisplay()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = if (session.canUndo()) "Undo" else "Back",
                    color = if (session.canUndo()) FancyButtonColor.YELLOW else FancyButtonColor.GREEN,
                    onClick = {
                        if (session.canUndo()) {
                            val lastPathEntry = session.removeLastPathFromHistory()
                            if (session.undoLastMove()) {
                                lastPathEntry?.let { pathTracker.undoLastPathSegment(it[0]) }
                                hintMessage = null
                            }
                        } else {
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
                    text = if (gameWon) "Retry" else "Reset",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        session.resetGame()
                        pathTracker.clearPaths()
                        session.clearPathHistory()
                        hintManager.reset()
                        hintMessage = null
                        hintContainerVisible = false
                        selectedRobotHasMoved = false
                    },
                    modifier = Modifier.weight(1f)
                )
                // New Game button: hidden in level games (matches Android), shows "Next Level"/"New Random Game" on completion
                if (!isLevelGame || gameWon) {
                    FancyButton(
                        text = if (gameWon) {
                            if (isLevelGame) "Next Level" else "New Random Game"
                        } else "New Game",
                        color = FancyButtonColor.GREEN,
                        onClick = {
                            if (gameWon) {
                                if (isLevelGame) onNextLevel() else requestManualNewGame()
                            } else requestManualNewGame()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    }

    generationFallback?.let { decision ->
        MapGenerationFallbackDialog(
            minMoves = Preferences.minSolutionMoves,
            maxMoves = Preferences.maxSolutionMoves,
            actualMoves = decision.moveCount,
            attempts = decision.attempt,
            onDismiss = { generationFallback = null }
        )
    }

    // Completion is shown inline via hintMessage (matches Android — no dialog)
    // The bottom buttons (Menu/Retry/Next Level) handle navigation when game is won
}


@Composable
internal fun MapGenerationFallbackDialog(
    minMoves: Int,
    maxMoves: Int,
    actualMoves: Int?,
    attempts: Int,
    onDismiss: () -> Unit
) {
    val stringProvider = getStringProvider()
    val unknownText = stringProvider.getString("map_generation_unknown_moves") ?: "unknown"
    val actualText = actualMoves?.toString() ?: unknownText
    val title = stringProvider.getString("map_generation_failed_title") ?: "No suitable map found"
    val message = if (maxMoves >= 99) {
        stringProvider.getString("map_generation_failed_min_message", minMoves, attempts, actualText)
            ?: "No map with at least {0} moves could be generated after {1} attempts. The last generated map ({2} moves) will be used."
                .replace("{0}", minMoves.toString())
                .replace("{1}", attempts.toString())
                .replace("{2}", actualText)
    } else {
        stringProvider.getString("map_generation_failed_range_message", minMoves, maxMoves, attempts, actualText)
            ?: "No map with {0} to {1} moves could be generated after {2} attempts. The last generated map ({3} moves) will be used."
                .replace("{0}", minMoves.toString())
                .replace("{1}", maxMoves.toString())
                .replace("{2}", attempts.toString())
                .replace("{3}", actualText)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
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
/**
 * Format elapsed time in milliseconds to mm:ss or hh:mm:ss format
 * Matches Main Game format exactly
 */
@Composable
fun BoardCanvas(
    board: Board,
    startBoard: Board,
    onRobotMove: (robotIndex: Int, direction: Int) -> Unit = { _, _ -> },
    onRobotSelected: (robotIndex: Int) -> Unit = {},
    pathTracker: PathTracker? = null,
    selectedRobotIndex: Int = -1,
    selectedRobotHasMoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gameState = remember(board) { ComposeGameState(board) }
    // Use the board parameter directly - it will be updated by the parent
    // Since board is passed as a parameter, Compose will recompose when it changes
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
        imageResource(Res.drawable.robot_pink_right),   // 0 = red (pink) - matches LevelLoader
        imageResource(Res.drawable.robot_green_right),  // 1 = green
        imageResource(Res.drawable.robot_blue_right),   // 2 = blue
        imageResource(Res.drawable.robot_yellow_right),  // 3 = yellow
        imageResource(Res.drawable.robot_silver_right)  // 4 = silver
    )
    val targetSprites = listOf(
        imageResource(Res.drawable.target_pink),   // 0 = red (pink) - matches LevelLoader
        imageResource(Res.drawable.target_green),  // 1 = green
        imageResource(Res.drawable.target_blue),   // 2 = blue
        imageResource(Res.drawable.target_yellow),  // 3 = yellow
        imageResource(Res.drawable.target_silver)  // 4 = silver
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
                // Tap-to-move: tap a robot to select it, then tap an empty cell to move
                // Matches Android GameGridView.onTouchEvent ACTION_UP tap handling
                detectTapGestures(
                    onTap = { offset ->
                        val cellSize = min(size.width, size.height) / maxOf(board.width, board.height).toFloat()
                        val offsetX = (size.width - board.width * cellSize) / 2
                        val offsetY = (size.height - board.height * cellSize) / 2
                        val gridX = ((offset.x - offsetX) / cellSize).toInt()
                        val gridY = ((offset.y - offsetY) / cellSize).toInt()

                        // Bounds check
                        if (gridX < 0 || gridX >= board.width || gridY < 0 || gridY >= board.height) return@detectTapGestures

                        // Check if a robot is at the tap position
                        var clickedRobot = -1
                        for (i in board.robotPositions.indices) {
                            val pos = board.robotPositions[i]
                            if (pos % board.width == gridX && pos / board.width == gridY) {
                                clickedRobot = i
                                break
                            }
                        }

                        if (clickedRobot >= 0) {
                            // Tap on a robot — select it (matches Android: select robot on tap)
                            if (selectedRobotIndex != clickedRobot) {
                                onRobotSelected(clickedRobot)
                            }
                        } else if (selectedRobotIndex >= 0) {
                            // Tap on empty cell with a robot selected — determine direction and move
                            // Matches Android GameGridView.onTouchEvent lines 1456-1495
                            val robotPos = board.robotPositions[selectedRobotIndex]
                            val robotX = robotPos % board.width
                            val robotY = robotPos / board.width
                            var dx = 0
                            var dy = 0

                            if (robotX == gridX || robotY == gridY) {
                                // Direct movement along row or column
                                if (robotX == gridX) {
                                    // Moving vertically
                                    dy = if (gridY > robotY) 1 else -1
                                } else {
                                    // Moving horizontally
                                    dx = if (gridX > robotX) 1 else -1
                                }
                            } else {
                                // Diagonal tap — dominant axis wins
                                val deltaX = gridX - robotX
                                val deltaY = gridY - robotY
                                if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)) {
                                    dx = if (deltaX > 0) 1 else -1
                                } else {
                                    dy = if (deltaY > 0) 1 else -1
                                }
                            }

                            if (dx != 0 || dy != 0) {
                                // Convert to direction constant
                                val direction = when {
                                    dy < 0 -> Board.NORTH
                                    dy > 0 -> Board.SOUTH
                                    dx > 0 -> Board.EAST
                                    dx < 0 -> Board.WEST
                                    else -> -1
                                }
                                if (direction >= 0) {
                                    onRobotMove(selectedRobotIndex, direction)
                                }
                            }
                        }
                    }
                )
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
                                // Notify parent that a robot was selected (for scale animation)
                                onRobotSelected(i)
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
                // SOUTH (horizontal wall at bottom edge of cell x,y)
                if (board.walls[2][position]) {
                    val isCenter = (y == cWallY + 1 && (x == cWallX || x == cWallX + 1))
                    if (!isCenter) {
                        drawImageScaled(
                            wallH,
                            cellX - wallOffset,
                            cellY + cellSize - wallThickness / 2,
                            cellSize + 2 * wallOffset,
                            wallThickness
                        )
                    }
                }
                // EAST (vertical wall at right edge of cell x,y)
                if (board.walls[1][position]) {
                    val isCenter = (x == cWallX + 1 && (y == cWallY || y == cWallY + 1))
                    if (!isCenter) {
                        drawImageScaled(
                            wallV,
                            cellX + cellSize - wallThickness / 2,
                            cellY - wallOffset,
                            wallThickness,
                            cellSize + 2 * wallOffset
                        )
                    }
                }
                // outer NORTH
                if (y == 0 && board.walls[0][position]) {
                    drawImageScaled(
                        wallH,
                        cellX - wallOffset,
                        cellY - wallThickness / 2,
                        cellSize + 2 * wallOffset,
                        wallThickness
                    )
                }
                // outer WEST
                if (x == 0 && board.walls[3][position]) {
                    drawImageScaled(
                        wallV,
                        cellX - wallThickness / 2,
                        cellY - wallOffset,
                        wallThickness,
                        cellSize + 2 * wallOffset
                    )
                }
            }
        }

        // 5. Robot movement paths (drawn under robots, above grid)
        if (pathTracker != null && pathTracker.hasPaths()) {
            val pathStrokeWidth = cellSize * PathTracker.PATH_STROKE_WIDTH_RATIO
            val perpOffsetStep = cellSize * PathTracker.PERPENDICULAR_OFFSET_STEP_RATIO
            val robotPaths = pathTracker.getRobotPaths()
            for ((robotColor, path) in robotPaths) {
                if (path.size < 2) continue
                val baseOffset = pathTracker.getBaseOffset(robotColor)
                val baseOffsetX = baseOffset[0] * cellSize
                val baseOffsetY = baseOffset[1] * cellSize
                // Robot path colors (50% alpha, matches Android GameGridView)
                val pathColor = when (robotColor) {
                    0 -> Color(0x80FF69B4.toInt()) // PINK (red)
                    1 -> Color(0x8000B100.toInt()) // GREEN
                    2 -> Color(0x800000FF.toInt()) // BLUE
                    3 -> Color(0x80B1B100.toInt()) // YELLOW
                    4 -> Color(0x80C0C0C0.toInt()) // SILVER
                    else -> Color(0x80B1B1B1.toInt()) // default gray
                }
                for (i in 1 until path.size) {
                    val pos = path[i]
                    val prevPos = path[i - 1]
                    val x = offsetX + (pos[0] * cellSize) + cellSize / 2
                    val y = offsetY + (pos[1] * cellSize) + cellSize / 2
                    val prevX = offsetX + (prevPos[0] * cellSize) + cellSize / 2
                    val prevY = offsetY + (prevPos[1] * cellSize) + cellSize / 2
                    // Calculate perpendicular offset for stacked segments
                    val dx = x - prevX
                    val dy = y - prevY
                    val length = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (length > 0.001f) {
                        val perpX = -dy / length
                        val perpY = dx / length
                        val count = pathTracker.getSegmentCount(robotColor, prevPos[0], prevPos[1], pos[0], pos[1])
                        val perpOffset = (count - 1) * perpOffsetStep
                        drawLine(
                            color = pathColor,
                            start = androidx.compose.ui.geometry.Offset(prevX + baseOffsetX + perpX * perpOffset, prevY + baseOffsetY + perpY * perpOffset),
                            end = androidx.compose.ui.geometry.Offset(x + baseOffsetX + perpX * perpOffset, y + baseOffsetY + perpY * perpOffset),
                            strokeWidth = pathStrokeWidth
                        )
                    } else {
                        drawLine(
                            color = pathColor,
                            start = androidx.compose.ui.geometry.Offset(prevX + baseOffsetX, prevY + baseOffsetY),
                            end = androidx.compose.ui.geometry.Offset(x + baseOffsetX, y + baseOffsetY),
                            strokeWidth = pathStrokeWidth
                        )
                    }
                }
            }
        }

        // 6. Robots using the color sprites with selection scale (matches Android GameGridView)
        // Android scales: 1.5x (initial click) → 1.3x (after first move) → 1.1x (default)
        val defaultRobotScale = 1.1f
        val initialSelectedRobotScale = 1.5f
        val selectedRobotScale = 1.3f
        for (i in board.robotPositions.indices) {
            val position = board.robotPositions[i]
            val robotX = position % board.width
            val robotY = position / board.width
            val sprite = if (i in robotSprites.indices) robotSprites[i] else robotSprites.last()
            // Match Android scale logic:
            // - Selected and not moved yet: 1.5x (initial click pop)
            // - Selected and has moved: 1.3x (regular selected scale)
            // - Not selected: 1.1x (default)
            val robotScale = when {
                i == selectedRobotIndex && !selectedRobotHasMoved -> initialSelectedRobotScale
                i == selectedRobotIndex && selectedRobotHasMoved -> selectedRobotScale
                else -> defaultRobotScale
            }
            val robotInset = (robotScale - 1f) * cellSize / 2f
            drawImageScaled(
                sprite,
                offsetX + robotX * cellSize - robotInset,
                offsetY + robotY * cellSize - robotInset,
                cellSize * robotScale,
                cellSize * robotScale
            )
        }

        // Note: Ghost robots (semi-transparent start positions) are NOT drawn.
        // Android GameGridView does not have this feature.
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

/** Draws an [ImageBitmap] scaled with alpha transparency. */
private fun DrawScope.drawImageScaledWithAlpha(
    image: ImageBitmap,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    alpha: Float
) {
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        alpha = alpha
    )
}

