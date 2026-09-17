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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import driftingdroids.model.Board
import driftingdroids.model.TimeProvider
import driftingdroids.model.isTrivialPuzzle
import roboyard.logic.core.LevelLoader
import roboyard.logic.core.LevelFormatParser
import roboyard.logic.core.ComposeGameState
import roboyard.logic.storage.getPlatformStorage
import driftingdroids.model.Solution
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.ic_alt_back
import roboyard.composeapp.generated.resources.ic_alt_forward
import roboyard.composeapp.generated.resources.ic_alt_hint
import roboyard.composeapp.generated.resources.ic_alt_menu
import roboyard.composeapp.generated.resources.ic_alt_profile
import roboyard.composeapp.generated.resources.ic_alt_replay
import roboyard.composeapp.generated.resources.ic_alt_save
import roboyard.composeapp.generated.resources.ic_alt_save_add
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
import roboyard.logic.achievements.Achievement
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.StreakManager
import roboyard.logic.managers.GameHistoryManager
import roboyard.logic.managers.GameSession
import roboyard.logic.managers.LevelCompletionManager
import roboyard.logic.core.Constants
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

/** Animated robot transition driven by GameSession.moveAnimator (matches Android RobotAnimationManager). */
class RobotMoveAnim(
    val robotColor: Int,
    val fromX: Int, val fromY: Int,
    val toX: Int, val toY: Int,
    val progress: Animatable<Float, AnimationVector1D>
)

/**
 * FancyButton with Android's long-press cooldown: when [cooldown] is true the button
 * must be held for [cooldownMs] while a circular progress sweeps; releasing early
 * cancels and fades the progress out (matches Android startCircularProgressAnimation).
 */
@Composable
private fun CooldownFancyButton(
    text: String,
    color: FancyButtonColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cooldown: Boolean = false,
    cooldownMs: Int = 1200
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    Box(modifier = modifier) {
        FancyButton(
            text = text,
            color = color,
            onClick = {},
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(percent = 50))
                .pointerInput(cooldown, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            if (!cooldown) {
                                onClick()
                                tryAwaitRelease()
                                return@detectTapGestures
                            }
                            val job = scope.launch {
                                progress.animateTo(
                                    1f,
                                    tween(durationMillis = cooldownMs, easing = LinearEasing)
                                )
                                // Held for the full duration — trigger the action
                                onClick()
                            }
                            tryAwaitRelease()
                            if (job.isActive) {
                                job.cancel()
                                // Fade the progress out briefly (matches Android)
                                progress.animateTo(0f, tween(durationMillis = 300))
                            }
                        }
                    )
                }
        )
        if (progress.value > 0f) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            ) {
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
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
    onNextLevel: () -> Unit = {},
    onProfile: () -> Unit = {}
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
    var hintArrowActive by remember(gameEpoch) { mutableStateOf(false) }
    var maxHintUsed by remember(gameEpoch) { mutableIntStateOf(-1) }
    var elapsedTime by remember(gameEpoch) { mutableLongStateOf(session.uiTimerElapsedMs) }
    var timerRunning by remember(gameEpoch) { mutableStateOf(false) }

    // Android onPause: persist the UI timer into the session so it survives
    // leaving and re-entering the game screen
    DisposableEffect(gameEpoch) {
        onDispose { session.saveUiTimerElapsed(elapsedTime) }
    }
    var selectedRobotHasMoved by remember(gameEpoch) { mutableStateOf(false) }
    var accessibilityControlsVisible by remember { mutableStateOf(Preferences.accessibilityMode) }
    var generationFallback by remember { mutableStateOf<MapGenerationDecision?>(null) }
    var lastAutosaveTime by remember { mutableLongStateOf(0L) }
    var autosaveRunning by remember { mutableStateOf(false) }

    // Phase 1 state — matches GameFragment fields
    val scope = rememberCoroutineScope()
    val achievementManager = remember { AchievementManager.getInstance(storage, stringProvider, null) }
    var pendingAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var announcement by remember(gameEpoch) { mutableStateOf<String?>(null) }
    var robotMoveAnim by remember { mutableStateOf<RobotMoveAnim?>(null) }
    var showPrevLevelDialog by remember { mutableStateOf(false) }
    var showLastLevelDialog by remember { mutableStateOf(false) }
    var liveCounterEnabled by remember(gameEpoch) { mutableStateOf(Preferences.liveMoveCounterEnabled) }
    var lastCompletedLevelId by remember { mutableIntStateOf(-1) }
    var lastCompletedTime by remember { mutableLongStateOf(0L) }
    var lastAutoHintClickTime by remember { mutableLongStateOf(0L) }
    var gameStartElapsed by remember(gameEpoch) { mutableLongStateOf(TimeProvider.currentTimeMillis()) }
    val liveCounterText by session.liveMoveCounterText.collectAsState()
    val liveSolverCalculating by session.liveSolverCalculating.collectAsState()

    val selectedRobotIndex = gameState?.getSelectedRobot()?.color ?: -1

    val AUTOSAVE_INTERVAL_MS = 60 * 1000 // 60 seconds (same as main game)
    val AUTO_HINT_COOLDOWN_MS = 1000L // matches Android AUTO_HINT_COOLDOWN_MS
    val BUTTON_COOLDOWN_MS = 1200 // matches Android BUTTON_COOLDOWN_MS

    // Toast-style announcement (Android uses Toast.makeText)
    fun announce(text: String?) {
        if (!text.isNullOrEmpty()) announcement = text
    }
    LaunchedEffect(announcement) {
        if (announcement != null) {
            delay(2500)
            announcement = null
        }
    }

    // Helper: get localized robot color name for button text (matches Android)
    // Robot color key — matches Android getLocalizedRobotColorNameByGridElement:
    // 0=pink, 1=green, 2=blue, 3=yellow, 4=silver, 5=red, 6=brown, 7=orange, 8=white
    fun getRobotColorKey(colorIndex: Int): String? = when (colorIndex) {
        0 -> "pink"; 1 -> "green"; 2 -> "blue"; 3 -> "yellow"; 4 -> "silver"
        5 -> "red"; 6 -> "brown"; 7 -> "orange"; 8 -> "white"
        else -> null
    }

    fun getRobotColorName(colorIndex: Int): String {
        val colorKey = getRobotColorKey(colorIndex) ?: return "Robot"
        val key = "color_$colorKey"
        return stringProvider.getString(key) ?: when (colorIndex) {
            0 -> "Pink"; 1 -> "Green"; 2 -> "Blue"; 3 -> "Yellow"; 4 -> "Silver"
            else -> "Robot"
        }
    }


    // Move via accessibility direction button — announces failure like Android
    // moveRobotInDirection ("cannot move in this direction"); success is
    // announced in onRobotMoveCompleted via the robot description
    fun a11yMove(direction: Int) {
        if (!session.moveRobot(direction)) {
            announce(stringProvider.getString("cannot_move_in_this_direction") ?: "Cannot move in this direction")
        }
    }

    // Android announceGameStart: target + selected robot position + adjacent walls
    fun buildGameStartAnnouncement(): String {
        val st = gameState ?: return ""
        val sb = StringBuilder()
        st.gameElements.firstOrNull { it.type == GameElement.TYPE_TARGET }
            ?.let { target ->
                sb.append(getRobotColorName(target.color)).append(" ")
                    .append(stringProvider.getString("target_a11y", target.x + 1, target.y + 1)
                        ?: "Goal at position ${target.x + 1}, ${target.y + 1}")
                    .append(". ")
            }
        val robot = st.getSelectedRobot()
        if (robot != null) {
            sb.append(getRobotColorName(robot.color))
                .append(" robot ${robot.x + 1},${robot.y + 1}")
            val walls = mutableListOf<String>()
            if (!st.canRobotMoveTo(robot, robot.x + 1, robot.y) && robot.x + 1 < st.width && st.getRobotAt(robot.x + 1, robot.y) == null) walls.add("east")
            if (!st.canRobotMoveTo(robot, robot.x - 1, robot.y) && robot.x - 1 >= 0 && st.getRobotAt(robot.x - 1, robot.y) == null) walls.add("west")
            if (!st.canRobotMoveTo(robot, robot.x, robot.y - 1) && robot.y - 1 >= 0 && st.getRobotAt(robot.x, robot.y - 1) == null) walls.add("north")
            if (!st.canRobotMoveTo(robot, robot.x, robot.y + 1) && robot.y + 1 < st.height && st.getRobotAt(robot.x, robot.y + 1) == null) walls.add("south")
            if (walls.isNotEmpty()) sb.append(", walls ").append(walls.joinToString(", "))
            sb.append(".")
        }
        return sb.toString()
    }

    // Wire session hooks for Compose visuals — mirrors Android GameFragment/GameGridView wiring
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
        // Robot move animation — RobotAnimationManager equivalent
        session.moveAnimator = { robot, fromX, fromY, toX, toY, onComplete ->
            val distance = kotlin.math.abs(toX - fromX) + kotlin.math.abs(toY - fromY)
            val anim = Animatable(0f)
            robotMoveAnim = RobotMoveAnim(robot.color, fromX, fromY, toX, toY, anim)
            scope.launch {
                try {
                    anim.animateTo(1f, tween(durationMillis = (distance * 45).coerceIn(120, 400)))
                } finally {
                    robotMoveAnim = null
                    onComplete()
                }
            }
        }
        // Move completed — Android GameGridView path update + handleRobotMovementSounds
        session.onRobotMoveCompleted = { state, robot, oldX, oldY ->
            pathTracker.addPathSegment(robot.color, oldX, oldY, robot.x, robot.y)
            session.addPathToHistory(robot.color, oldX, oldY, robot.x, robot.y)
            selectedRobotHasMoved = true
            val hitRobot = session.lastMoveHitRobotElement
            when {
                hitRobot != null -> {
                    soundManager.playSound("hit_robot", robot.color, hitRobot.color)
                    achievementManager.onRobotTouched(robot.color, hitRobot.color, state.robots.size)
                }
                session.lastMoveHitWall -> soundManager.playSound("hit_wall")
                else -> soundManager.playSound("move")
            }
            // Announce robot position after every move (Android GameGridView
            // announceForAccessibility(getRobotDescription)) — only in accessibility mode
            if (Preferences.accessibilityMode) {
                // German uses adjective color forms (color_*_adj), matching Android
                val colorWord = if (Preferences.appLanguage == "de") {
                    stringProvider.getString("color_${getRobotColorKey(robot.color)}_adj")
                        ?: getRobotColorName(robot.color)
                } else getRobotColorName(robot.color)
                val desc = StringBuilder()
                    .append(colorWord).append(" ")
                    .append(stringProvider.getString("robot_position_a11y") ?: "robot at position")
                    .append(" ").append(robot.x + 1).append(",").append(robot.y + 1)
                state.gameElements.firstOrNull {
                    it.type == GameElement.TYPE_TARGET && it.color == robot.color
                }?.let { target ->
                    desc.append(". ")
                        .append(stringProvider.getString("target_position_a11y") ?: "Its goal is at position")
                        .append(" ").append(target.x + 1).append(",").append(target.y + 1)
                }
                // announcePossibleMoves — free distance + obstacle per direction
                roboyard.logic.core.buildPossibleMovesAnnouncement(
                    state, robot, stringProvider
                ) { getRobotColorName(it) }?.let { desc.append(". ").append(it) }
                announce(desc.toString())
            }
            // checkIfMoveMatchesHint — auto-advance in Manual/Full-Auto, not in Semi-Auto
            if (hintContainerVisible && Preferences.hintAutoMoveMode != Preferences.HINT_AUTO_MOVE_SEMI_AUTO) {
                val movedDir = state.lastMoveDirection
                val regularHint = hintManager.getRegularHint()
                if (movedDir != null && movedDir >= 0 && regularHint != null &&
                    robot.color == regularHint.first &&
                    errDirToBoardDir(movedDir) == regularHint.second) {
                    // Clear the arrow immediately — a new one appears after the 1s delay
                    hintArrowActive = false
                    pendingAutoAdvance = true
                }
            }
        }
        achievementManager.setUnlockListener(object : AchievementManager.AchievementUnlockListener {
            override fun onAchievementUnlocked(achievement: Achievement?) {
                if (achievement != null) pendingAchievements = pendingAchievements + achievement
            }
        })
        achievementManager.streakDataProvider = StreakManager.getInstance(storage, achievementManager)
        achievementManager.checkAndUnlockStreakAchievements()
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
                    // Android autosave(): skip level games (levelId > 0)
                    if ((gameState?.levelId ?: 0) <= 0) {
                        session.saveGame(0, isAutoSave = true)
                        println("[AUTOSAVE] Autosaved to slot 0")
                    }
                    lastAutosaveTime = System.currentTimeMillis()
                }
            }
        }
    }

    // Android onViewCreated/onResume: rebuild drawn robot paths from the session
    // path history (reconstructPathsFromHistory) and auto-select the robot
    // matching the target color (selectRobotWithTargetColor)
    LaunchedEffect(gameEpoch, gameState != null) {
        if (gameState != null) {
            pathTracker.clearPaths()
            session.pathHistory.forEach { entry ->
                if (entry.size >= 5) {
                    pathTracker.addPathSegment(entry[0], entry[1], entry[2], entry[3], entry[4])
                }
            }
            session.selectRobotWithTargetColor()
            // Android onResume: announceGameStart — visual toast only in
            // accessibility mode (TalkBack equivalent on desktop)
            if (Preferences.accessibilityMode) {
                announce(buildGameStartAnnouncement())
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
    // fromAutoAdvance: true when reached via auto-advance after a matching move —
    // Android shows the direction arrow for the first regular hint or after
    // auto-advance; manual navigation clears it, and it never shows in Semi-Auto mode.
    fun updateHintDisplay(fromAutoAdvance: Boolean = false) {
        val hintData = hintManager.getHintForDisplay()
        if (hintData != null) {
            hintMessage = hintData.text
            currentHintRobotColor = hintData.robotColorForBackground
            // Select the hinted robot on the board (matches Android selectRobotByColor)
            if (hintData.robotColorToSelect >= 0) {
                session.selectRobotByColor(hintData.robotColorToSelect)
                selectedRobotHasMoved = false
            }
            val regularHint = hintManager.getRegularHint()
            if (regularHint != null) {
                currentHintRobot = regularHint.first
                currentHintDirection = regularHint.second
                val regularIndex = hintManager.getCurrentHintStep() - hintManager.getTotalPreHintSteps()
                hintArrowActive = (regularIndex == 0 || fromAutoAdvance) &&
                    Preferences.hintAutoMoveMode != Preferences.HINT_AUTO_MOVE_SEMI_AUTO &&
                    !session.isGameComplete.value
            } else {
                currentHintRobot = -1
                currentHintDirection = -1
                hintArrowActive = false
            }
        } else {
            hintMessage = null
            currentHintRobotColor = -1
            currentHintRobot = -1
            currentHintDirection = -1
            hintArrowActive = false
        }
    }

    var hintsUsed by remember(gameEpoch) { mutableIntStateOf(0) }

    // Record that a hint was shown — only real (regular) hints count, matching
    // Android showNormalHint: hintCount + recordHintUsed + onHintUsed + history save
    fun recordHintShown() {
        val step = hintManager.getCurrentHintStep()
        maxHintUsed = maxOf(maxHintUsed, step)
        if (hintManager.isRegularHintStep()) {
            hintsUsed++
            session.recordHintShown(step - hintManager.getTotalPreHintSteps())
            achievementManager.onHintUsed()
            session.saveToHistoryNow("hint_shown_${step - hintManager.getTotalPreHintSteps()}")
        }
    }

    // Auto-move robot when a regular hint is shown in Full-Auto mode
    // (matches Android executeHintAutoMove — 100ms delay, skipped when game complete)
    fun maybeAutoMoveHint() {
        if (Preferences.hintAutoMoveMode != Preferences.HINT_AUTO_MOVE_FULL_AUTO) return
        if (session.isGameComplete.value) return
        val hint = hintManager.getRegularHint() ?: return
        scope.launch {
            delay(100)
            if (session.isGameComplete.value) return@launch
            session.selectRobotByColor(hint.first)
            session.moveRobot(hint.second)
        }
    }

    // Advance to next hint — Android showNextHint: Semi-Auto 1s cooldown,
    // level restrictions live inside HintManager
    fun showNextHint() {
        if (hintManager.getCurrentHintStep() >= hintManager.getTotalPreHintSteps() - 1 &&
            Preferences.hintAutoMoveMode == Preferences.HINT_AUTO_MOVE_SEMI_AUTO) {
            val now = TimeProvider.currentTimeMillis()
            if (now - lastAutoHintClickTime < AUTO_HINT_COOLDOWN_MS) return
            lastAutoHintClickTime = now
        }
        if (hintManager.nextHint()) {
            updateHintDisplay()
            recordHintShown()
            maybeAutoMoveHint()
        }
    }

    fun showPrevHint() {
        if (hintManager.prevHint()) {
            updateHintDisplay()
            maybeAutoMoveHint()
        }
    }

    fun requestManualNewGame() {
        generationFallback = null
        onNewGame()
    }

    // Filtered history entries (level games excluded) — matches Android getFilteredHistoryEntries
    fun filteredHistoryEntries(): List<roboyard.logic.core.GameHistoryEntry> {
        return GameHistoryManager.getHistoryEntries(storage).filter {
            val mapName = it.mapName
            mapName == null || !mapName.matches(Regex("(?i)^Level\\s+\\d+.*"))
        }
    }

    // Back button action — full port of Android handleBackButtonClick
    fun handleBackButtonClick() {
        if (gameWon) {
            announce(stringProvider.getString("nothing_to_undo") ?: "Nothing to undo")
            return
        }
        val currentLevelId = gameState?.levelId ?: -1

        if (isLevelGame && currentLevelId > 0 && moveCount == 0) {
            // showBackToPreviousLevelDialog — level 1 goes straight to level selection
            if (currentLevelId <= 1) onBack() else showPrevLevelDialog = true
            return
        }
        if (session.isLoadedFromHistory && moveCount == 0) {
            session.loadPreviousHistoryEntry()
            return
        }
        // Random game with history entries — load most recent entry
        if (!session.isLoadedFromSave && !isLevelGame && moveCount == 0) {
            val entries = filteredHistoryEntries()
            if (entries.isNotEmpty()) {
                session.loadHistoryEntry(entries[0].getMapPath())
                return
            }
        }
        // Hint step-back when hints are visible (Android falls through to undo afterwards)
        if (hintContainerVisible && hintManager.getCurrentHintStep() > 0) {
            showPrevHint()
        }
        // Standard undo
        if (session.undoLastMove()) {
            val lastPath = session.removeLastPathFromHistory()
            lastPath?.let { pathTracker.undoLastPathSegment(it[0]) }
            hintMessage = null
        } else {
            announce(stringProvider.getString("nothing_to_undo") ?: "Nothing to undo")
        }
    }

    // New Map / Next Level button action — port of Android handleNewMapButtonClick
    fun handleNewMapButtonClick() {
        // Reset achievement game-session flags (matches Android onNewGameStarted)
        achievementManager.onNewGameStarted()
        if (isLevelGame) {
            val state = gameState ?: return
            val currentLevelId = state.levelId
            val nextLevelId = currentLevelId + 1
            if (currentLevelId >= 140) {
                showLastLevelDialog = true
                return
            }
            val totalStars = LevelCompletionManager.getInstance(storage).totalStars
            if (nextLevelId - 1 <= totalStars) {
                pendingAchievements = emptyList()
                session.startLevelGame(nextLevelId)
                pathTracker.clearPaths()
                elapsedTime = 0
                hintManager.reset()
                hintMessage = null
                hintContainerVisible = false
            } else {
                val starsNeeded = nextLevelId - 1 - totalStars
                announce(stringProvider.getString("level_not_unlocked", starsNeeded)
                    ?: "You need $starsNeeded more star(s) to unlock")
            }
            return
        }
        if (session.isLoadedFromHistory) {
            if (!session.loadNextHistoryEntry()) {
                announce(stringProvider.getString("no_more_history_entries") ?: "No more history entries")
                session.clearLoadedFromHistoryFlag()
            }
            return
        }
        // Random game: warn on map dimension mismatch, then start new game
        if (!Preferences.generateNewMapEachTime) {
            gameState?.let { s ->
                if (s.width != Preferences.boardSizeWidth || s.height != Preferences.boardSizeHeight) {
                    announce(stringProvider.getString(
                        "map_dimensions_mismatch", s.width, s.height,
                        Preferences.boardSizeWidth, Preferences.boardSizeHeight
                    ) ?: "Map dimensions do not fit settings - generating new walls")
                }
            }
        }
        pendingAchievements = emptyList()
        pathTracker.clearPaths()
        session.cancelSolver()
        session.resetMoveCountsAndHistory()
        session.startGame()
        elapsedTime = 0
        timerRunning = false
        hintManager.reset()
        hintMessage = null
        hintContainerVisible = false
        selectedRobotHasMoved = false
    }

    // Auto-advance hint after 1s delay when player follows the hint (matches Android)
    LaunchedEffect(pendingAutoAdvance) {
        if (pendingAutoAdvance) {
            delay(1000)
            if (hintManager.hasNextHint()) {
                hintManager.nextHint()
                updateHintDisplay(fromAutoAdvance = true)
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

    // Wrong-robot-on-target toast — once per robot color per game (matches Android observer)
    LaunchedEffect(wrongRobotAtTarget) {
        if (wrongRobotAtTarget >= 0 && !session.hasWrongRobotToastBeenShownFor(wrongRobotAtTarget)) {
            session.markWrongRobotToastShownFor(wrongRobotAtTarget)
            announce(stringProvider.getString("wrong_robot_on_target") ?: "Wrong robot on target")
        }
    }

    // Restore live-move-counter preference on each new game (matches Android observer setup):
    // level games force it off; random games restore the persisted preference
    LaunchedEffect(gameEpoch, isLevelGame) {
        if (isLevelGame) {
            session.setLiveMoveCounterEnabled(false)
            liveCounterEnabled = false
        } else {
            session.setLiveMoveCounterEnabled(liveCounterEnabled)
            if (liveCounterEnabled) session.triggerLiveSolver()
        }
    }

    // Start timer when game starts (on first move)
    LaunchedEffect(moveCount) {
        if (moveCount > 0 && !timerRunning && !gameWon) {
            timerRunning = true
        }
    }

    // Stop timer when game is won — full Android completion flow:
    // win sound, stars, completion message, achievements, history save (matches GameFragment)
    LaunchedEffect(gameWon) {
        if (gameWon) {
            timerRunning = false
            accessibilityControlsVisible = false
            soundManager.playSound("win")
            val state = session.currentState.value ?: return@LaunchedEffect
            val optimalMoves = sessionSolution?.moves?.size ?: session.lastSolutionMinMoves
            val hintsUsed = state.hintCount
            val elapsed = TimeProvider.currentTimeMillis() - gameStartElapsed
            hintContainerVisible = true

            if (isLevelGame && state.levelId > 0) {
                var stars = session.calculateStars(moveCount, optimalMoves, hintsUsed)
                if (stars < 1 && state.levelId <= Constants.MIN_STAR_GUARANTEE_LEVEL) stars = 1
                val starStr = buildString { repeat(stars) { append("\u2605 ") } }.trim()
                hintMessage = (stringProvider.getString("level_complete") ?: "Level Complete!") + " " + starStr
                announcement = hintMessage
                if (!state.hasCompletionBeenHandledThisSession()) {
                    state.markCompletionHandledThisSession()
                    session.saveToHistoryNow("completed")
                    val now = TimeProvider.currentTimeMillis()
                    if (state.levelId != lastCompletedLevelId || now - lastCompletedTime > 1000) {
                        achievementManager.onLevelCompleted(
                            state.levelId, moveCount, optimalMoves, hintsUsed, stars, elapsed
                        )
                        lastCompletedLevelId = state.levelId
                        lastCompletedTime = now
                    }
                }
            } else {
                // Random game completion message (matches Android updateStatusText block)
                var completionMessage = stringProvider.getString("random_game_complete") ?: "Level Complete!"
                val sol = sessionSolution
                if (sol != null && sol.moves.isNotEmpty() && hintManager.getCurrentHintStep() < 4) {
                    if (moveCount == optimalMoves) {
                        if (hintManager.getCurrentHintStep() == 0) {
                            completionMessage += " \n" +
                                (stringProvider.getString("perfect_solution") ?: "Perfect! You found the optimal solution!")
                        }
                    } else {
                        val extra = when {
                            optimalMoves < 1 ->
                                (stringProvider.getString("no_solution_found") ?: "No solution found") + "!"
                            moveCount == 1 ->
                                stringProvider.getString("pre_hint_less_than_1") ?: "You found a better solution than the A.I.!"
                            else ->
                                stringProvider.getString("pre_hint_less_than_x", moveCount)
                                    ?: "The A.I. found a solution in less than $moveCount moves"
                        }
                        completionMessage += " \n" + extra
                    }
                }
                hintMessage = completionMessage
                announcement = completionMessage
                if (!state.hasCompletionBeenHandledThisSession()) {
                    state.markCompletionHandledThisSession()
                    val mapSignature = state.generateMapSignature()
                    val isFirstCompletion = GameHistoryManager.isFirstCompletion(storage, mapSignature)
                    session.saveToHistoryNow("completed")
                    var qualifiesForNoHints = !state.hasUsedHintsThisSession()
                    if (!isFirstCompletion) {
                        GameHistoryManager.findByMapSignature(storage, mapSignature)?.let {
                            qualifiesForNoHints = it.qualifiesForNoHintsAchievement()
                        }
                    }
                    achievementManager.onRandomGameCompleted(
                        moveCount, optimalMoves, hintsUsed, elapsed,
                        Preferences.difficulty == Constants.DIFFICULTY_IMPOSSIBLE,
                        state.robots.size, state.targets.size, state.getRobotCount(),
                        isFirstCompletion, qualifiesForNoHints, state.generateWallSignature()
                    )
                }
            }
        }
    }

    val board = renderBoard
    val startB = startBoard
    if (board == null || startB == null) {
        // Map is still being generated / loaded — matches Android showSolverCalculationStatus:
        // counter " (restarts/lastMoves)" after >3 restarts, green ✓ keep-map button after >1
        val restartCount by session.solverRestartCountFlow.collectAsState()
        val lastMoves by session.lastSolutionMinMovesFlow.collectAsState()
        var keepMapChosen by remember(gameEpoch) { mutableStateOf(false) }
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val counterInfo = if (restartCount > 3) {
                    " ($restartCount" + (if (lastMoves > 0) "/$lastMoves" else "") + ")"
                } else ""
                Text(
                    text = (if (keepMapChosen) {
                        stringProvider.getString("keep_map") ?: "Play this map. Calculating..."
                    } else {
                        stringProvider.getString("ai_calculating") ?: "Calculating..."
                    }) + counterInfo,
                    color = Color.White,
                    fontSize = 18.sp
                )
                if (restartCount > 1 && !keepMapChosen) {
                    FancyButton(
                        text = "✓",
                        color = FancyButtonColor.GREEN,
                        onClick = {
                            session.keepCurrentMapDespiteDifficulty()
                            keepMapChosen = true
                        },
                        modifier = Modifier.padding(start = 8.dp).height(32.dp)
                    )
                }
            }
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    // Landscape layout like Android layout-land: grid on one side, controls on the other.
    // gridOnLeft persists via the same "landscape_grid_left" pref key Android uses.
    val isLandscapeLayout = maxWidth > maxHeight
    var gridOnLeft by remember { mutableStateOf(storage.getBoolean("landscape_grid_left", true)) }
    // Android fragment_game_alt: icon-based portrait layout toggled via debug settings
    val useAltLayout = remember { storage.getBoolean("use_alternative_layout", false) }
    // Android game_info_close_button: hides the game info row until the screen is recreated
    var gameInfoVisible by remember { mutableStateOf(true) }

    @Composable
    fun boardArea(boardModifier: Modifier) {
        // Game grid maintaining aspect ratio
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
                    val robot = session.selectRobotByColor(robotIndex)
                    if (robot != null) {
                        // Forward move: moveAnimator + onRobotMoveCompleted hooks handle
                        // animation, path tracking, sounds, achievements and hint auto-advance.
                        // Reverse move: session pops pathHistory and fires onReverseMoveUndo.
                        session.moveRobot(direction)
                    }
                },
                robotMoveAnim = robotMoveAnim,
                hintRobotColor = if (hintArrowActive && hintContainerVisible) currentHintRobot else -1,
                hintDirection = if (hintArrowActive && hintContainerVisible) currentHintDirection else -1,
                modifier = boardModifier
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(0.dp))
            )
        }
    }

    @Composable
    fun altIconButton(
        icon: org.jetbrains.compose.resources.DrawableResource,
        contentDescription: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        active: Boolean = false
    ) {
        Box(
            modifier = modifier
                .height(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (active) Color(0xFFFFEB3B) else Color.Transparent)
                .clickable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    @Composable
    fun ColumnScope.controlsArea(altLayout: Boolean = false) {
        // Hint container (matches Android: prev ◂ | status text | 👁 live toggle | ▸ next)
        // Visible when the hint toggle is checked OR the live move counter is enabled
        val liveOnlyMode = liveCounterEnabled && !hintContainerVisible
        val liveStatusText = if (liveSolverCalculating) "…" else liveCounterText
        val hintStatusText = if (hintContainerVisible) hintMessage ?: "" else liveStatusText
        AnimatedVisibility(
            visible = (hintContainerVisible && hintMessage != null) || (liveOnlyMode && liveStatusText.isNotEmpty()),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val hintBgColor = if (currentHintRobotColor >= 0) getHintBackgroundColor(currentHintRobotColor) else getHintBackgroundColor(-1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (liveOnlyMode) Color(0xFFDD000000) else hintBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // prev/next arrows only while the hint toggle is checked (Android hides them in live-only mode)
                if (hintContainerVisible && hintManager.hasPrevHint()) {
                    FancyButton(
                        text = "\u25C2",
                        color = FancyButtonColor.HINT,
                        onClick = { showPrevHint() },
                        modifier = Modifier.height(32.dp)
                    )
                }
                Text(
                    text = hintStatusText,
                    color = if (liveOnlyMode) Color.White else Color(0xFF1A1A1A),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // Live move counter toggle — visible from the exact-solution pre-hint onwards
                // or while enabled; never in level games (matches Android)
                if (!isLevelGame && (liveCounterEnabled || hintManager.isExactSolutionHintStep())) {
                    FancyButton(
                        text = if (liveCounterEnabled) "\uD83D\uDC41" else "\uD83D\uDC41\u200D\uD83D\uDDE8",
                        color = FancyButtonColor.HINT,
                        onClick = {
                            val checked = !liveCounterEnabled
                            liveCounterEnabled = checked
                            session.setLiveMoveCounterEnabled(checked)
                            if (checked) {
                                gameState?.recordHintUsed(0)
                                session.saveToHistoryNow("live_move")
                                session.triggerLiveSolver()
                            }
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .semantics {
                                contentDescription = stringProvider.getString("live_move_counter_label_a11y") ?: "Show remaining moves"
                            }
                    )
                }
                if (hintContainerVisible && hintManager.hasNextHint()) {
                    FancyButton(
                        text = "\u25B8",
                        color = FancyButtonColor.HINT,
                        onClick = { showNextHint() },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        if (altLayout) {
            // Alt layout info bar (fragment_game_alt): [moves column] [optimal] [timer]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$squaresMoved",
                        color = Color(0xFFEEEEEE),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringProvider.getString("moves_label") ?: "Moves",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val optMoves = sessionSolution?.moves?.size ?: 0
                    if (hintContainerVisible && hintManager.isExactSolutionHintStep() && optMoves > 0) {
                        FancyButton(
                            text = optMoves.toString(),
                            color = FancyButtonColor.GREEN,
                            onClick = { showNextHint() },
                            modifier = Modifier.height(32.dp).width(48.dp)
                        )
                    }
                }
                Text(
                    text = formatElapsedTime(elapsedTime),
                    color = Color(0xFFEEEEEE),
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
            }
        } else if (gameInfoVisible) {
        // Game info row — Android 3-block layout: left (Moves/Squares/Difficulty),
        // center (optimal moves button, shown from exact-solution hint), right (Timer, MapID, Dice)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDD000000))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(0.55f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 10.sp)) { append((stringProvider.getString("moves_label") ?: "Moves") + ": ") }
                        withStyle(SpanStyle(fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { append("$moveCount") }
                    },
                    color = Color.White
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 9.sp)) { append((stringProvider.getString("squares_label") ?: "Squares") + ": ") }
                        withStyle(SpanStyle(fontSize = 14.sp)) { append("$squaresMoved") }
                    },
                    color = Color.White
                )
                Text(
                    text = session.localizedDifficultyString,
                    color = Color.White,
                    fontSize = 8.sp
                )
            }
            // Optimal moves button — appears when the exact-solution pre-hint is shown (Android updateOptimalMovesButton)
            Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.Center) {
                val optMoves = sessionSolution?.moves?.size ?: 0
                if (hintContainerVisible && hintManager.isExactSolutionHintStep() && optMoves > 0) {
                    FancyButton(
                        text = optMoves.toString(),
                        color = FancyButtonColor.HINT,
                        onClick = { showNextHint() },
                        modifier = Modifier.height(32.dp).width(48.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(0.25f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatElapsedTime(elapsedTime),
                    color = Color.White,
                    fontSize = 14.sp
                )
                // unique map id / level text — clickable to show next hint (matches Android)
                val mapIdText = when {
                    isLevelGame && levelId > 0 -> stringProvider.getString("level_id_text", levelId) ?: "Level $levelId"
                    !gameState?.levelName.isNullOrEmpty() -> gameState!!.levelName!!
                    else -> stringProvider.getString("unique_map_id", gameState?.uniqueMapId ?: "") ?: (gameState?.uniqueMapId ?: "")
                }
                Text(
                    text = mapIdText,
                    color = Color.White,
                    fontSize = 8.sp,
                    modifier = Modifier.clickable {
                        if (hintContainerVisible) showNextHint()
                    }
                )
                // Dice button — only in random games when generateNewMapEachTime=false (matches Android)
                if (!isLevelGame && !Preferences.generateNewMapEachTime) {
                    Text(
                        text = "\uD83C\uDFB2",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable {
                                roboyard.logic.core.MapGenerator.forceGenerateNewMapOnce = true
                                pathTracker.clearPaths()
                                session.resetMoveCountsAndHistory()
                                session.startGame()
                                elapsedTime = 0
                                timerRunning = false
                                hintManager.reset()
                                hintMessage = null
                                hintContainerVisible = false
                            }
                    )
                }
            }
            // Layout direction toggle — landscape only (Android layout_toggle_button, ⇄/⇆)
            if (isLandscapeLayout) {
                Text(
                    text = if (gridOnLeft) "⇄" else "⇆",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable {
                            gridOnLeft = !gridOnLeft
                            storage.putBoolean("landscape_grid_left", gridOnLeft)
                        }
                )
            }
            // Android game_info_close_button: 16dp ✕ hides the info row and
            // also closes the hint container + unchecks the hint toggle
            Text(
                text = "✕",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable {
                        gameInfoVisible = false
                        if (hintContainerVisible) {
                            hintContainerVisible = false
                            hintMessage = null
                            currentHintRobotColor = -1
                            currentHintRobot = -1
                            currentHintDirection = -1
                            hintArrowActive = false
                            hintManager.resetStep()
                        }
                    }
                    .semantics { contentDescription = "Hide game info" }
            )
        }
        }

        // Accessibility controls — Android accessibility_controls.xml 3-row layout:
        // Row1: [Announce] [North] [Select Robot]   Row2: [West] [East]
        // Row3: [txtSelectedRobot] [South] [txtRobotGoal]
        if (accessibilityControlsVisible) {
            val selectedRobot = gameState?.getSelectedRobot()
            val robotColorName = if (selectedRobotIndex >= 0) getRobotColorName(selectedRobotIndex) else "Robot"
            val robotButtonColor = if (selectedRobotIndex >= 0) getRobotButtonColor(selectedRobotIndex) else FancyButtonColor.BLUE
            val goalElement = gameState?.gameElements?.firstOrNull {
                it.type == GameElement.TYPE_TARGET && it.color == selectedRobot?.color
            }
            val selectedRobotText = if (selectedRobot != null) {
                stringProvider.getString("robot_selected_info", robotColorName, selectedRobot.x, selectedRobot.y)
                    ?: "Selected: $robotColorName robot at position (${selectedRobot.x}, ${selectedRobot.y})"
            } else {
                stringProvider.getString("no_robot_selected") ?: "No robot selected"
            }
            val robotGoalText = when {
                selectedRobot == null -> ""
                goalElement != null -> stringProvider.getString(
                    "robot_target_info", robotColorName, goalElement.x, goalElement.y
                ) ?: "$robotColorName goal: (${goalElement.x}, ${goalElement.y})"
                else -> stringProvider.getString("no_target_for_robot") ?: "No goal for this robot"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF303030))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FancyButton(
                        text = stringProvider.getString("announce") ?: "Announce",
                        color = FancyButtonColor.HINT,
                        onClick = { announce(buildGameStartAnnouncement()) },
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    FancyButton(
                        text = if (selectedRobotIndex >= 0) "$robotColorName ${getDirectionName(Board.NORTH)}" else getDirectionName(Board.NORTH),
                        color = robotButtonColor,
                        onClick = { a11yMove(Board.NORTH) },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    FancyButton(
                        text = stringProvider.getString("next_robot") ?: "Next Robot",
                        color = FancyButtonColor.HINT,
                        onClick = {
                            // cycleThroughRobots — announce selection + goal + possible moves (Android parity)
                            val robotCount = gameState?.getRobotCount() ?: 4
                            val nextIndex = (selectedRobotIndex + 1) % robotCount
                            val nextRobot = session.selectRobotByColor(nextIndex)
                            selectedRobotHasMoved = false
                            if (nextRobot != null) {
                                val name = getRobotColorName(nextIndex)
                                val sb = StringBuilder(
                                    stringProvider.getString("robot_selected_a11y", name)
                                        ?: "$name robot selected"
                                ).append(". ")
                                val st = session.currentState.value
                                st?.gameElements?.firstOrNull {
                                    it.type == GameElement.TYPE_TARGET && it.color == nextRobot.color
                                }?.let { target ->
                                    sb.append(stringProvider.getString("target_a11y", target.x + 1, target.y + 1)
                                        ?: "Goal at ${target.x + 1},${target.y + 1}").append(". ")
                                }
                                roboyard.logic.core.buildPossibleMovesAnnouncement(
                                    st, nextRobot, stringProvider
                                ) { getRobotColorName(it) }?.let { sb.append(it) }
                                announce(sb.toString())
                            }
                        },
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FancyButton(
                        text = if (selectedRobotIndex >= 0) "$robotColorName ${getDirectionName(Board.WEST)}" else getDirectionName(Board.WEST),
                        color = robotButtonColor,
                        onClick = { a11yMove(Board.WEST) },
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    FancyButton(
                        text = if (selectedRobotIndex >= 0) "$robotColorName ${getDirectionName(Board.EAST)}" else getDirectionName(Board.EAST),
                        color = robotButtonColor,
                        onClick = { a11yMove(Board.EAST) },
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedRobotText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        maxLines = 3
                    )
                    FancyButton(
                        text = if (selectedRobotIndex >= 0) "$robotColorName ${getDirectionName(Board.SOUTH)}" else getDirectionName(Board.SOUTH),
                        color = robotButtonColor,
                        onClick = { a11yMove(Board.SOUTH) },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = robotGoalText,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        maxLines = 3
                    )
                }
            }
        }

        // Flexible space pushes the buttons to the bottom
        Spacer(modifier = Modifier.weight(1f))

        // Shared button actions — used by both the text-button layout and the alt icon bar
        val hintToggleAction = {
            if (hintContainerVisible) {
                // Toggle OFF (matches Android unchecking the toggle)
                hintContainerVisible = false
                hintMessage = null
                currentHintRobotColor = -1
                currentHintRobot = -1
                currentHintDirection = -1
                hintArrowActive = false
                hintManager.resetStep()
            } else {
                hintContainerVisible = true
                if (sessionSolution == null || sessionSolution!!.moves.isEmpty()) {
                    hintMessage = if (isSolverRunning) {
                        stringProvider.getString("ai_calculating") ?: "Calculating..."
                    } else {
                        stringProvider.getString("no_solution_found") ?: "No solution found"
                    }
                } else {
                    if (!hintManager.hasSolution()) {
                        val moves = sessionSolution!!.moves.mapNotNull { m ->
                            (m as? RRGameMove)?.let { Pair(it.color, errDirToBoardDir(it.direction)) }
                        }
                        hintManager.initialize(moves, isLevelGame, levelId)
                    }
                    updateHintDisplay()
                    maybeAutoMoveHint()
                }
            }
            Unit
        }
        val resetAction = {
            session.resetGame()
            pathTracker.clearPaths()
            session.clearPathHistory()
            hintManager.reset()
            hintMessage = null
            hintContainerVisible = false
            selectedRobotHasMoved = false
            elapsedTime = 0
            // Android reset button: move sound + accessibility announcement
            soundManager.playSound("move")
            if (Preferences.accessibilityMode) {
                announce(stringProvider.getString("robots_reset") ?: "Robots reset to starting positions")
            }
        }
        val solverRegenerating = isSolverRunning && sessionSolution == null

        if (altLayout) {
            // Alt bottom bar (fragment_game_alt): icon buttons — hint, back, new map, save, reset
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    altIconButton(
                        icon = Res.drawable.ic_alt_hint,
                        contentDescription = stringProvider.getString("hint_button_a11y") ?: "Hint",
                        onClick = { if (!solverRegenerating) hintToggleAction() },
                        modifier = Modifier.weight(1f),
                        active = hintContainerVisible
                    )
                    val atStart = moveCount == 0
                    val backNeedsCooldown = atStart && !isLevelGame &&
                        !session.isLoadedFromHistory && !session.isLoadedFromSave
                    if (backNeedsCooldown) {
                        CooldownFancyButton(
                            text = "◂",
                            color = FancyButtonColor.HINT,
                            cooldown = true,
                            onClick = { handleBackButtonClick() },
                            modifier = Modifier.weight(1f).height(64.dp)
                        )
                    } else {
                        altIconButton(
                            icon = Res.drawable.ic_alt_back,
                            contentDescription = stringProvider.getString("back_description") ?: "Back",
                            onClick = { handleBackButtonClick() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!isLevelGame) {
                        val isHistoryGame = session.isLoadedFromHistory
                        val hasNext = isHistoryGame && session.hasNextHistoryEntry()
                        CooldownFancyButton(
                            text = "▸",
                            color = FancyButtonColor.GREEN,
                            enabled = !isHistoryGame || hasNext,
                            cooldown = true,
                            onClick = { handleNewMapButtonClick() },
                            modifier = Modifier.weight(1f).height(64.dp)
                        )
                        altIconButton(
                            icon = Res.drawable.ic_alt_save_add,
                            contentDescription = stringProvider.getString("save_map_button_a11y") ?: "Save map",
                            onClick = { if (!isSolverRunning) onSaveLoad() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    altIconButton(
                        icon = Res.drawable.ic_alt_replay,
                        contentDescription = stringProvider.getString("restart_button_a11y") ?: "Restart",
                        onClick = { resetAction() },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (gameWon && pendingAchievements.isEmpty()) {
                    FancyButton(
                        text = if (isLevelGame) {
                            stringProvider.getString("next_level") ?: "Next Level"
                        } else {
                            stringProvider.getString("new_random_game_button") ?: "New Random Game"
                        },
                        color = FancyButtonColor.GREEN,
                        onClick = { handleNewMapButtonClick() },
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
                    )
                }
            }
        } else {
        // Bottom button container — matches Android layout:
        // Row 1: [Save Map] [Hint] [Back]  Row 2: [Menu] [Reset] [New Map]
        // Row 3 (completion only): full-width Next Level / New Random Game
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
                // Save Map button: hidden in level games, disabled during solver (matches Android)
                if (!isLevelGame) {
                    FancyButton(
                        text = stringProvider.getString("button_save_map") ?: "Save Map",
                        color = FancyButtonColor.RED,
                        enabled = !isSolverRunning,
                        onClick = { onSaveLoad() },
                        modifier = Modifier.weight(1f).padding(end = 3.dp)
                    )
                }
                // Hint toggle button (Android ToggleButton: 💡Hint / ❌ Hint);
                // disabled while the solver is generating a map with no solution yet;
                // Android forces the toggle checked with "Cancel" text during regeneration
                FancyButton(
                    enabled = !solverRegenerating,
                    text = if (hintContainerVisible || solverRegenerating) {
                        stringProvider.getString("hint_cancel_button") ?: "\u274C Hint"
                    } else {
                        stringProvider.getString("hint_button") ?: "\uD83D\uDCA1Hint"
                    },
                    color = FancyButtonColor.HINT,
                    onClick = { hintToggleAction() },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                // Back button — text always "Back"; color encodes the action (Android updateBackButtonColor)
                val atStart = moveCount == 0
                val backColor = when {
                    !atStart -> FancyButtonColor.HINT // yellow-ish: undo
                    isLevelGame || session.isLoadedFromHistory -> FancyButtonColor.GREEN
                    !session.isLoadedFromSave && filteredHistoryEntries().isNotEmpty() -> FancyButtonColor.GREEN
                    else -> FancyButtonColor.GRAY
                }
                // Long-press required for random games at start (not level/history/save) — matches Android
                val backNeedsCooldown = atStart && !isLevelGame &&
                    !session.isLoadedFromHistory && !session.isLoadedFromSave
                CooldownFancyButton(
                    text = "\u25C2 " + (stringProvider.getString("button_back_game") ?: "Back"),
                    color = backColor,
                    cooldown = backNeedsCooldown,
                    onClick = { handleBackButtonClick() },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                FancyButton(
                    text = stringProvider.getString("button_menu") ?: "Menu",
                    color = FancyButtonColor.GRAY,
                    onClick = onBack,
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = if (gameWon) {
                        stringProvider.getString("retry_button") ?: "Retry"
                    } else {
                        stringProvider.getString("button_reset") ?: "Reset"
                    },
                    color = FancyButtonColor.BLUE,
                    onClick = { resetAction() },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                // New Map button: hidden in level games (Android sets GONE);
                // history games show "Next Game" (disabled at last entry)
                if (!isLevelGame) {
                    val isHistoryGame = session.isLoadedFromHistory
                    val hasNext = isHistoryGame && session.hasNextHistoryEntry()
                    CooldownFancyButton(
                        text = if (isHistoryGame) {
                            stringProvider.getString("button_next_game") ?: "Next Game"
                        } else {
                            stringProvider.getString("button_new_game") ?: "New Game"
                        },
                        color = FancyButtonColor.GREEN,
                        enabled = !isHistoryGame || hasNext,
                        cooldown = true,
                        onClick = { handleNewMapButtonClick() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Full-width completion button (Android next_level_button, initially invisible;
            // hidden while an achievement popup is showing — Android does the same)
            if (gameWon && pendingAchievements.isEmpty()) {
                FancyButton(
                    text = if (isLevelGame) {
                        stringProvider.getString("next_level") ?: "Next Level"
                    } else {
                        stringProvider.getString("new_random_game_button") ?: "New Random Game"
                    },
                    color = FancyButtonColor.GREEN,
                    onClick = { handleNewMapButtonClick() },
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
                )
            }
        }
        }
    }

    // Alt top bar (fragment_game_alt): menu/save icons, green move counter,
    // live-move toggle, profile icon — all 48dp on a #222222 bar
    @Composable
    fun altTopBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            altIconButton(
                icon = Res.drawable.ic_alt_menu,
                contentDescription = stringProvider.getString("menu_button_a11y") ?: "Menu",
                onClick = onBack,
                modifier = Modifier.height(48.dp)
            )
            altIconButton(
                icon = Res.drawable.ic_alt_save,
                contentDescription = stringProvider.getString("save_map_button_a11y") ?: "Save map",
                onClick = { if (!isSolverRunning) onSaveLoad() },
                modifier = Modifier.height(48.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 4.dp)
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$moveCount",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Live move counter toggle (LiveModeToggleButtonAlt — always visible in alt layout)
            if (!isLevelGame) {
                Text(
                    text = if (liveCounterEnabled) "👁" else "👁‍🗨",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable {
                            val checked = !liveCounterEnabled
                            liveCounterEnabled = checked
                            session.setLiveMoveCounterEnabled(checked)
                            if (checked) {
                                gameState?.recordHintUsed(0)
                                session.saveToHistoryNow("live_move")
                                session.triggerLiveSolver()
                            }
                        }
                        .semantics {
                            contentDescription = stringProvider.getString("live_move_counter_label_a11y") ?: "Show remaining moves"
                        }
                )
            }
            altIconButton(
                icon = Res.drawable.ic_alt_profile,
                contentDescription = "Profile",
                onClick = onProfile,
                modifier = Modifier.height(48.dp)
            )
        }
    }

    // Layout dispatch — portrait: board above controls (Android layout-port);
    // alt portrait (fragment_game_alt): icon top bar + board + icon bottom bar;
    // landscape: board and controls side-by-side, side chosen by gridOnLeft
    // (Android layout-land / layout-land grid_left variants)
    if (isLandscapeLayout) {
        Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val boardModifier = Modifier.aspectRatio(board.width.toFloat() / board.height.toFloat())
            if (gridOnLeft) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    boardArea(boardModifier)
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    controlsArea()
                }
            } else {
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    controlsArea()
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    boardArea(boardModifier)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (useAltLayout) {
                altTopBar()
            }
            boardArea(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(board.width.toFloat() / board.height.toFloat())
            )
            controlsArea(altLayout = useAltLayout)
        }
    }
    }

    // Toast-style announcement overlay (Android Toast equivalent)
    announcement?.let { msg ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = msg,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 120.dp)
                    .background(Color(0xCC333333), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    // Achievement unlock popup (Android AchievementPopup equivalent — tap to dismiss)
    AchievementUnlockPopup(
        achievements = pendingAchievements,
        onDismiss = { pendingAchievements = emptyList() }
    )

    // Back-to-previous-level confirmation (Android showBackToPreviousLevelDialog)
    if (showPrevLevelDialog) {
        val prevId = (gameState?.levelId ?: 2) - 1
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrevLevelDialog = false },
            text = {
                Text(stringProvider.getString("confirm_level_change", prevId)
                    ?: "Go back to level $prevId?")
            },
            confirmButton = {
                Button(onClick = {
                    showPrevLevelDialog = false
                    session.startLevelGame(prevId)
                    pathTracker.clearPaths()
                    elapsedTime = 0
                    hintManager.reset()
                    hintMessage = null
                    hintContainerVisible = false
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showPrevLevelDialog = false }) {
                    Text(stringProvider.getString("cancel") ?: "Cancel")
                }
            }
        )
    }

    // Last-level dialog (Android: level 140 → offer profile page for custom maps)
    if (showLastLevelDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLastLevelDialog = false },
            title = { Text("Last Level") },
            text = {
                Text(stringProvider.getString("last_level_message")
                    ?: "That was the last level!")
            },
            confirmButton = {
                Button(onClick = {
                    showLastLevelDialog = false
                    onProfile()
                }) { Text(stringProvider.getString("yes") ?: "Yes") }
            },
            dismissButton = {
                Button(onClick = { showLastLevelDialog = false }) {
                    Text(stringProvider.getString("no") ?: "No")
                }
            }
        )
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
    robotMoveAnim: RobotMoveAnim? = null,
    hintRobotColor: Int = -1,
    hintDirection: Int = -1,
    modifier: Modifier = Modifier
) {
    val gameState = remember(board) { ComposeGameState(board) }
    // Use the board parameter directly - it will be updated by the parent
    // Since board is passed as a parameter, Compose will recompose when it changes
    val currentBoard = board

    // Gesture handlers run in long-lived pointerInput coroutines keyed on Unit;
    // always read the latest board/selection via rememberUpdatedState so taps and
    // drags never see stale robot positions or a stale selection.
    val gestureBoard by rememberUpdatedState(board)
    val gestureSelectedRobot by rememberUpdatedState(selectedRobotIndex)

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
                        val tapBoard = gestureBoard
                        val tapSelected = gestureSelectedRobot
                        // Android GameGridView.onMeasure: cellSize = min(cellWidth, cellHeight)
                        // so the board always fills the full canvas extent
                        val cellSize = min(size.width / tapBoard.width, size.height / tapBoard.height).toFloat()
                        val offsetX = (size.width - tapBoard.width * cellSize) / 2
                        val offsetY = (size.height - tapBoard.height * cellSize) / 2
                        val gridX = ((offset.x - offsetX) / cellSize).toInt()
                        val gridY = ((offset.y - offsetY) / cellSize).toInt()

                        // Bounds check
                        if (gridX < 0 || gridX >= tapBoard.width || gridY < 0 || gridY >= tapBoard.height) return@detectTapGestures

                        // Check if a robot is at the tap position
                        var clickedRobot = -1
                        for (i in tapBoard.robotPositions.indices) {
                            val pos = tapBoard.robotPositions[i]
                            if (pos % tapBoard.width == gridX && pos / tapBoard.width == gridY) {
                                clickedRobot = i
                                break
                            }
                        }

                        if (clickedRobot >= 0) {
                            // Tap on a robot — select it (matches Android: select robot on tap)
                            if (tapSelected != clickedRobot) {
                                onRobotSelected(clickedRobot)
                            }
                        } else if (tapSelected >= 0) {
                            // Tap on empty cell with a robot selected — determine direction and move
                            // Matches Android GameGridView.onTouchEvent lines 1456-1495
                            val robotPos = tapBoard.robotPositions[tapSelected]
                            val robotX = robotPos % tapBoard.width
                            val robotY = robotPos / tapBoard.width
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
                                    onRobotMove(tapSelected, direction)
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // Raw touch handling matching Android GameGridView.onTouchEvent:
                // awaitFirstDown reports the true ACTION_DOWN position, whereas
                // detectDragGestures.onDragStart only fires after the touch slop
                // is crossed and reports a slop-shifted position (which pushed
                // the robot hit test off by up to a full cell).
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    run {
                        val offset = down.position
                        val dragBoard = gestureBoard
                        val cellSize = min(size.width / dragBoard.width, size.height / dragBoard.height).toFloat()
                        val offsetX = (size.width - dragBoard.width * cellSize) / 2
                        val offsetY = (size.height - dragBoard.height * cellSize) / 2

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

                        println("[UI] ACTION_DOWN - Start touch: ($startTouchX, $startTouchY), Grid: ($gridX, $gridY), canvas=(${size.width}x${size.height}), cell=$cellSize, offset=($offsetX, $offsetY)")
                        println("[UI] ACTION_DOWN - Robot positions: ${dragBoard.robotPositions.map { "${it % dragBoard.width},${it / dragBoard.width}" }}")

                        // Check if a robot was touched at the start — cell-based hit
                        // test matching Android GameGridView.getRobotAt(gridX, gridY)
                        touchedRobot = null
                        for (i in dragBoard.robotPositions.indices) {
                            val position = dragBoard.robotPositions[i]
                            if (position % dragBoard.width == gridX && position / dragBoard.width == gridY) {
                                touchedRobot = i
                                println("[UI] ACTION_DOWN - Robot $i touched at ($gridX, $gridY)")
                                // Notify parent that a robot was selected (for scale animation)
                                onRobotSelected(i)
                                break
                            }
                        }
                        if (touchedRobot == null) {
                            println("[UI] ACTION_DOWN - No robot touched")
                        }
                    }

                    // ACTION_MOVE — process every position change of the tracked
                    // pointer, matching Android onTouchEvent (no touch slop)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        println("[UI] RAW_EVENT - id=${change.id} pressed=${change.pressed} pos=${change.position} posChanged=${change.positionChanged()}")
                        if (!change.pressed) break
                        if (!change.positionChanged()) continue
                        run {
                        val dragBoard = gestureBoard
                        val cellSize = min(size.width / dragBoard.width, size.height / dragBoard.height).toFloat()
                        val offsetX = (size.width - dragBoard.width * cellSize) / 2
                        val offsetY = (size.height - dragBoard.height * cellSize) / 2
                        val gridX = ((change.position.x - offsetX) / cellSize).toInt()
                        val gridY = ((change.position.y - offsetY) / cellSize).toInt()

                        // ACTION_MOVE logic from fragment-app
                        // Check if we just moved over a robot and none was selected before
                        var robotAtCurrentPos: Int? = null
                        for (i in dragBoard.robotPositions.indices) {
                            val position = dragBoard.robotPositions[i]
                            val robotX = position % dragBoard.width
                            val robotY = position / dragBoard.width
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
                            return@run
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
                        }
                    }

                    // ACTION_UP — reset all tracking variables (matching
                    // fragment-app onTouchEvent ACTION_UP)
                    println("[UI] ACTION_UP - touchedRobot: $touchedRobot, hasMovedRobotInCurrentGesture: $hasMovedRobotInCurrentGesture")

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
            }
    ) {
        // Android GameGridView.onMeasure: cellSize = min(cellWidth, cellHeight)
        // so the board always fills the full canvas extent
        val cellSize = min(size.width / board.width, size.height / board.height).toFloat()
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
            // Animated position while a move animation is in flight (RobotAnimationManager equivalent)
            val anim = robotMoveAnim
            val drawX: Float
            val drawY: Float
            if (anim != null && anim.robotColor == i) {
                val p = anim.progress.value
                drawX = anim.fromX + (anim.toX - anim.fromX) * p
                drawY = anim.fromY + (anim.toY - anim.fromY) * p
            } else {
                drawX = robotX.toFloat()
                drawY = robotY.toFloat()
            }
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
                offsetX + drawX * cellSize - robotInset,
                offsetY + drawY * cellSize - robotInset,
                cellSize * robotScale,
                cellSize * robotScale
            )
        }

        // 7. Hint direction arrow next to the hinted robot (Android GameGridView.drawHintArrow)
        if (hintRobotColor >= 0 && hintDirection >= 0 && hintRobotColor in board.robotPositions.indices) {
            val hintPos = board.robotPositions[hintRobotColor]
            val robotX = hintPos % board.width
            val robotY = hintPos / board.width
            val cx = offsetX + robotX * cellSize + cellSize / 2f
            val cy = offsetY + robotY * cellSize + cellSize / 2f
            val r = cellSize * 0.28f   // triangle size (matches Android)
            val gap = cellSize * 0.9f  // distance from robot center to triangle center
            val arrowColor = when (hintRobotColor) {
                0 -> Color(0xDCFF69B4.toInt()) // pink (red)
                1 -> Color(0xDC00B100.toInt()) // green
                2 -> Color(0xDC0000FF.toInt()) // blue
                3 -> Color(0xDCB1B100.toInt()) // yellow
                4 -> Color(0xDCC0C0C0.toInt()) // silver
                else -> Color(0xDCFFFFFF.toInt())
            }
            val tri = androidx.compose.ui.graphics.Path()
            // Board direction encoding: NORTH=0, EAST=1, SOUTH=2, WEST=3
            when (hintDirection) {
                driftingdroids.model.Board.EAST -> { // right
                    tri.moveTo(cx + gap + r, cy)
                    tri.lineTo(cx + gap - r, cy - r)
                    tri.lineTo(cx + gap - r, cy + r)
                }
                driftingdroids.model.Board.WEST -> { // left
                    tri.moveTo(cx - gap - r, cy)
                    tri.lineTo(cx - gap + r, cy - r)
                    tri.lineTo(cx - gap + r, cy + r)
                }
                driftingdroids.model.Board.NORTH -> { // up
                    tri.moveTo(cx, cy - gap - r)
                    tri.lineTo(cx - r, cy - gap + r)
                    tri.lineTo(cx + r, cy - gap + r)
                }
                driftingdroids.model.Board.SOUTH -> { // down
                    tri.moveTo(cx, cy + gap + r)
                    tri.lineTo(cx - r, cy + gap - r)
                    tri.lineTo(cx + r, cy + gap - r)
                }
            }
            tri.close()
            drawPath(tri, arrowColor)
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

