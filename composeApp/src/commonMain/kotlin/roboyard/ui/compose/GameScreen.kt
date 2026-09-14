package roboyard.ui.compose

import roboyard.logic.core.calculateStars
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
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
import driftingdroids.model.isTrivialPuzzle
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
import roboyard.logic.audio.SoundManager
import roboyard.logic.audio.getSoundManager
import roboyard.logic.ui.getStringProvider
import roboyard.logic.core.serializeBoard
import roboyard.logic.core.deserializeBoard
import roboyard.logic.storage.PlatformStorage

// Compose App Version - increment after each session
const val COMPOSE_APP_VERSION = "v1.9"

// Helper function to handle game win logic (DRY - delegates to shared buildGameWinMessage)
fun handleGameWin(
    moveCount: Int,
    isLevelGame: Boolean = false,
    optimalMoves: Int? = null,
    stars: Int = 0
): String = buildGameWinMessage(moveCount, isLevelGame, optimalMoves, stars)

@Composable
fun GameScreen(
    board: Board,
    isLevelGame: Boolean = false,
    isLoadedGame: Boolean = false,
    levelId: Int = 1,
    onBack: () -> Unit = {},
    onNewGame: () -> Unit = {},
    onSaveLoad: (Board, Board) -> Unit = { _, _ -> },
    onNextLevel: () -> Unit = {}
) {
    val storage = remember { getPlatformStorage() }
    val levelCompletionManager = remember { roboyard.logic.managers.LevelCompletionManager.getInstance() }

    var moveCount by remember(board) { mutableIntStateOf(0) }
    var squaresMoved by remember(board) { mutableIntStateOf(0) }
    var currentBoard by remember(board) { mutableStateOf(board) }
    // Store robot start positions separately to ensure they don't change
    val robotStartPositions = remember(board) { board.robotPositions.copyOf() }
    val startBoard = remember(board) { Board.Companion.createClone(board).also { it.setRobots(robotStartPositions) } }
    var hintMessage by remember(board) { mutableStateOf<String?>(null) }
    var gameWon by remember(board) { mutableStateOf(false) }
    var showCompletionDialog by remember(board) { mutableStateOf(false) }
    var maxHintUsed by remember(board) { mutableIntStateOf(-1) } // Track max hint used this session
    var isHistorySaved by remember(board) { mutableStateOf(false) }
    var gameStartTime by remember(board) { mutableLongStateOf(System.currentTimeMillis()) }
    var totalPlayTime by remember(board) { mutableIntStateOf(0) }
    var lastAutosaveTime by remember(board) { mutableLongStateOf(0L) }
    var autosaveRunning by remember(board) { mutableStateOf(false) }
    
    // Autosave interval (same as in main game)
    val AUTOSAVE_INTERVAL_MS = 60 * 1000 // 60 seconds
    
    // Autosave functionality
    LaunchedEffect(moveCount, gameWon) {
        // Start autosave when game starts (after first move)
        if (moveCount > 0 && !autosaveRunning && !gameWon) {
            autosaveRunning = true
            lastAutosaveTime = System.currentTimeMillis()
        }
        
        // Stop autosave when game is won
        if (gameWon) {
            autosaveRunning = false
        }
    }
    
    // Autosave function
    fun autosaveGame() {
        // Only autosave if game is in progress, not solved, and NOT a level game
        if (!gameWon && !isLevelGame) {
            val storage = getPlatformStorage()
            val saveData = buildString {
                appendLine("#MAPNAME:Random")
                appendLine(";TIME:$totalPlayTime")
                appendLine(";MOVES:$moveCount")
                appendLine(";DIFFICULTY:1")
                appendLine(";SIZE:${currentBoard.width},${currentBoard.height}")
                appendLine(";SOLVED:$gameWon")
                appendLine(";MAX_HINT_USED:$maxHintUsed")
                appendLine("WIDTH:${currentBoard.width};")
                appendLine("HEIGHT:${currentBoard.height};")
                
                // Board data
                for (y in 0 until currentBoard.height) {
                    for (x in 0 until currentBoard.width) {
                        if (x > 0) append(",")
                        val position = y * currentBoard.width + x
                        val hasRobot = currentBoard.robotPositions.contains(position)
                        val goal = currentBoard.goals.find { it.position == position }
                        when {
                            hasRobot -> append(4)
                            goal != null -> append(3).append(":").append(goal.robotNumber)
                            else -> append(0)
                        }
                    }
                    appendLine()
                }
                
                // Targets
                for (goal in currentBoard.goals) {
                    val x = goal.position % currentBoard.width
                    val y = goal.position / currentBoard.width
                    val colorChar = when (goal.robotNumber) {
                        0 -> 'b'
                        1 -> 'g'
                        2 -> 'r'
                        3 -> 'y'
                        4 -> 's'
                        else -> 'm'
                    }
                    append("t").append(colorChar).append(x).append(",").append(y).append(";")
                }
                appendLine()
                
                // Walls
                for (y in 0..currentBoard.height) {
                    for (x in 0 until currentBoard.width) {
                        val position = if (y < currentBoard.height) y * currentBoard.width + x else (currentBoard.height - 1) * currentBoard.width + x
                        if (y < currentBoard.height && currentBoard.isWall(position, 2)) {
                            append("h").append(x).append(",").append(y).append(";")
                        }
                    }
                }
                for (y in 0 until currentBoard.height) {
                    for (x in 0..currentBoard.width) {
                        val position = if (x < currentBoard.width) y * currentBoard.width + x else y * currentBoard.width + (currentBoard.width - 1)
                        if (x < currentBoard.width && currentBoard.isWall(position, 1)) {
                            append("v").append(x).append(",").append(y).append(";")
                        }
                    }
                }
                appendLine()
                
                // Robots
                for (i in currentBoard.robotPositions.indices) {
                    val position = currentBoard.robotPositions[i]
                    val x = position % currentBoard.width
                    val y = position / currentBoard.width
                    val colorChar = when (i) {
                        0 -> 'b'
                        1 -> 'g'
                        2 -> 'r'
                        3 -> 'y'
                        4 -> 's'
                        else -> 'm'
                    }
                    append("r").append(colorChar).append(x).append(",").append(y).append(";")
                }
            }
            
            storage.writeFile("saves/save_0.dat", saveData)
            println("[AUTOSAVE] Autosaved to slot 0 after ${AUTOSAVE_INTERVAL_MS / 1000} seconds")
        }
    }
    
    // Autosave timer effect - runs every second when autosave is enabled
    LaunchedEffect(autosaveRunning) {
        if (autosaveRunning) {
            while (autosaveRunning) {
                delay(1000)
                if (autosaveRunning) {
                    val currentTime = System.currentTimeMillis()
                    // Check if we should perform autosave
                    if (currentTime - lastAutosaveTime >= AUTOSAVE_INTERVAL_MS) {
                        autosaveGame()
                        lastAutosaveTime = currentTime
                    }
                }
            }
        }
    }
    var solution by remember(board) { mutableStateOf<driftingdroids.model.Solution?>(null) }
    var currentHintStep by remember(board) { mutableIntStateOf(0) }
    var currentHintRobot by remember(board) { mutableIntStateOf(-1) }
    var currentHintDirection by remember(board) { mutableIntStateOf(-1) }
    var isSolverRunning by remember(board) { mutableStateOf(false) }
    val boardHistory = remember(board) { mutableListOf<Board>() }
    val gameController = remember(board) { GameController() }
    val pathTracker = remember(board) { PathTracker() }
    val stringProvider = remember(board) { getStringProvider() }
    val hintManager = remember(board) { HintManager(stringProvider) }

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
    val soundManager = remember(board) { getSoundManager() }
    var elapsedTime by remember(board) { mutableLongStateOf(0L) }
    var timerRunning by remember(board) { mutableStateOf(false) }
    var selectedRobotIndex by remember(board) { mutableIntStateOf(-1) }
    var selectedRobotHasMoved by remember(board) { mutableStateOf(false) }
    var accessibilityControlsVisible by remember(board) { mutableStateOf(false) }
    var hintsUsed by remember(board) { mutableIntStateOf(0) }
    var regenerationCount by remember(board) { mutableIntStateOf(0) }
    var allowRegeneration by remember(board) { mutableStateOf(true) }
    
    // Reset history tracking when board changes (new game started)
    LaunchedEffect(board) {
        isHistorySaved = false
        gameStartTime = System.currentTimeMillis()
        totalPlayTime = 0
    }
    
    // Maximum auto-regeneration attempts (same as in main game)
    val MAX_AUTO_REGENERATIONS = 999
    
    // History save threshold (same as in main game)
    val HISTORY_SAVE_THRESHOLD = 30 // seconds

    // Get next available history index (simplified version)
    fun getNextHistoryIndex(): Int {
        val storage = Preferences.storageProvider?.invoke() ?: return 0
        var maxIndex = 0
        for (i in 0..1000) {
            if (storage.fileExists("history_$i.txt")) {
                maxIndex = i
            }
        }
        return maxIndex + 1
    }

    // Save to history function using GameHistoryManager
    fun saveToHistory() {
        try {
            val storage = Preferences.storageProvider?.invoke()
            if (storage == null) {
                println("[HISTORY] No storage available")
                return
            }
            
            // Initialize GameHistoryManager
            roboyard.logic.managers.GameHistoryManager.initialize(storage)

            // Generate map signatures for matching
            val wallSig = roboyard.ui.compose.generateWallSignature(currentBoard)
            val posSig = roboyard.ui.compose.generatePositionSignature(startBoard)
            val mapSig = roboyard.ui.compose.generateMapSignature(currentBoard, startBoard)

            println("[HISTORY] saveToHistory: wallSig=$wallSig")
            println("[HISTORY] saveToHistory: posSig=$posSig")
            println("[HISTORY] saveToHistory: mapSig=$mapSig")
            
            // Check if map already exists in history
            val existingEntry = roboyard.logic.managers.GameHistoryManager.findByMapSignature(storage, mapSig)
            
            val historyFileName: String
            val mapName: String
            
            if (existingEntry != null) {
                // Map already exists - use existing file
                historyFileName = existingEntry.getMapPath()
                mapName = existingEntry.mapName ?: "Unknown Map"
                println("[HISTORY] Map already exists in history, updating existing entry: $mapName")
            } else {
                // New map - get next available history index
                val historyIndex = roboyard.logic.managers.GameHistoryManager.getNextHistoryIndex(storage)
                historyFileName = roboyard.logic.managers.GameHistoryManager.indexToPath(historyIndex)
                
                // Generate map name (DRY - use generateMapNameFromSignature)
                mapName = generateMapNameFromSignature(mapSig, isLevelGame, if (isLevelGame) levelId else null)
                println("[HISTORY] New map, creating history entry: $mapName")
            }
            
            // Serialize board to Main Game format (DRY - use startBoard for consistent signature)
            val saveData = serializeBoardToMainGameFormat(currentBoard, isLevelGame, startBoard)
            
            // Write to history file
            val result = storage.writeFile(historyFileName, saveData)
            
            if (result) {
                println("[HISTORY] Saved game to history: $historyFileName")
                
                // Create or update history entry
                val entry: roboyard.logic.core.GameHistoryEntry
                val actualMoveCount = if (gameWon) moveCount else 0
                val optMoves = solution?.size() ?: 0
                
                if (existingEntry != null) {
                    // Update existing entry
                    entry = existingEntry
                    
                    // Update completion data if game is complete
                    if (gameWon) {
                        // Calculate stars for this completion using StarRating.kt (DRY)
                        val currentAttemptStars = calculateStars(actualMoveCount, optMoves, maxHintUsed)
                        
                        // For beginner levels (1-10), always earn at least 1 star
                        val finalStars = if (currentAttemptStars < 1 && isLevelGame && levelId <= 10) {
                            1
                        } else {
                            currentAttemptStars
                        }
                        
                        println("[HISTORY] Calculated stars: $finalStars (moves=$actualMoveCount, optimal=$optMoves, hints=$maxHintUsed)")
                        
                        entry.recordCompletion(
                            ((System.currentTimeMillis() - gameStartTime) / 1000).toInt(),
                            actualMoveCount,
                            finalStars
                        )
                        
                        // If completed without hints, record the no-hints timestamp
                        if (maxHintUsed < 0 && actualMoveCount > 0) {
                            val isOptimal = optMoves > 0 && actualMoveCount == optMoves
                            entry.recordSolvedWithoutHints(isOptimal)
                        }
                    }
                    
                    // Update hint tracking
                    if (maxHintUsed >= 0) {
                        entry.recordHintUsed(maxHintUsed)
                        entry.markEverUsedHints()
                    }
                    
                    println("[HISTORY] Updated existing history entry: $mapName")
                } else {
                    // Create new entry
                    entry = roboyard.logic.core.GameHistoryEntry(
                        historyFileName,
                        mapName,
                        System.currentTimeMillis(),
                        totalPlayTime,
                        actualMoveCount,
                        optMoves,
                        "${currentBoard.width}x${currentBoard.height}",
                        null
                    )
                    
                    // Set difficulty
                    entry.difficulty = 1 // Default to beginner for now
                    
                    // Set map signatures for unique map tracking
                    entry.wallSignature = wallSig
                    entry.positionSignature = posSig
                    entry.mapSignature = mapSig
                    
                    // Set hint tracking
                    entry.maxHintUsed = maxHintUsed
                    entry.setSolvedWithoutHints(maxHintUsed < 0)
                    if (maxHintUsed >= 0) {
                        entry.markEverUsedHints()
                    }
                    
                    // If game is complete, calculate and record stars
                    if (gameWon) {
                        // Calculate stars for this completion using StarRating.kt (DRY)
                        val currentAttemptStars = calculateStars(actualMoveCount, optMoves, maxHintUsed)
                        
                        // For beginner levels (1-10), always earn at least 1 star
                        val finalStars = if (currentAttemptStars < 1 && isLevelGame && levelId <= 10) {
                            1
                        } else {
                            currentAttemptStars
                        }
                        
                        println("[HISTORY] Calculated stars for new entry: $finalStars (moves=$actualMoveCount, optimal=$optMoves, hints=$maxHintUsed)")
                        
                        entry.recordCompletion(
                            ((System.currentTimeMillis() - gameStartTime) / 1000).toInt(),
                            actualMoveCount,
                            finalStars
                        )
                        
                        // If completed without hints, record the timestamp
                        if (maxHintUsed < 0 && actualMoveCount > 0) {
                            val isOptimal = optMoves > 0 && actualMoveCount == optMoves
                            entry.recordSolvedWithoutHints(isOptimal)
                        }
                    }

                    println("[HISTORY] Created new history entry: $mapName")
                }

                // Save the entry directly using addHistoryEntry (handles both new and updated entries)
                // This ensures recordCompletion changes are persisted
                println("[HISTORY] Calling addHistoryEntry: mapName=${entry.mapName}, movesMade=${entry.movesMade}, bestMoves=${entry.bestMoves}, bestTime=${entry.bestTime}, completionCount=${entry.completionCount}")
                val saved = roboyard.logic.managers.GameHistoryManager.addHistoryEntry(storage, entry)
                if (saved) {
                    println("[HISTORY] Saved history entry: ${entry.mapName}")
                } else {
                    println("[HISTORY] Failed to save history entry")
                }
            } else {
                println("[HISTORY] Failed to save game to history: $historyFileName")
            }
        } catch (e: Exception) {
            println("[HISTORY] Error saving to history: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Update hint tracking in the existing history entry for the current map.
     * Called when hint status changes after the initial history save.
     * Also updates move count if game is completed after hints were shown.
     */
    fun updateHintTrackingInHistory() {
        try {
            val storage = Preferences.storageProvider?.invoke()
            if (storage == null) {
                println("[HISTORY] No storage available for updateHintTrackingInHistory")
                return
            }

            // Generate map signature
            val mapSig = roboyard.ui.compose.generateMapSignature(currentBoard, startBoard)
            println("[HISTORY] updateHintTrackingInHistory: mapSig=$mapSig, isComplete=$gameWon")

            if (mapSig.isEmpty()) {
                println("[HISTORY] Map signature is empty, cannot update hint tracking")
                return
            }

            // Load the full list once - we will modify it in-place and save it back
            // Reload index to ensure newly created entries are found
            roboyard.logic.managers.GameHistoryManager.initialize(storage)
            val allEntries = roboyard.logic.managers.GameHistoryManager.getHistoryEntries(storage)
            var existing: roboyard.logic.core.GameHistoryEntry? = null
            for (e in allEntries) {
                if (mapSig == e.mapSignature) {
                    existing = e
                    break
                }
            }

            if (existing == null) {
                println("[HISTORY] updateHintTracking: Map signature not found in history: $mapSig")
                return
            }

            println("[HISTORY] Found existing entry: ${existing.mapName}")

            // Update hint tracking if hints were used
            if (!existing.hasUsedHints() && maxHintUsed >= 0) {
                existing.recordHintUsed(maxHintUsed)
                println("[HISTORY] Updated hint tracking in existing entry: maxHintUsed=$maxHintUsed")
            }

            // Update completion data if game is complete
            if (gameWon) {
                val actualMoveCount = moveCount
                val optMoves = solution?.size() ?: 0
                
                // Calculate stars for this completion
                val currentAttemptStars = if (optMoves > 0) {
                    when {
                        actualMoveCount <= optMoves -> 3
                        actualMoveCount <= optMoves * 2 -> 2
                        else -> 1
                    }
                } else {
                    1
                }

                existing.recordCompletion(
                    ((System.currentTimeMillis() - gameStartTime) / 1000).toInt(),
                    actualMoveCount,
                    currentAttemptStars
                )
                println("[HISTORY] Updated completion: moves=$actualMoveCount, stars=$currentAttemptStars")

                // If completed without hints, record the no-hints timestamp
                if (maxHintUsed < 0 && actualMoveCount > 0) {
                    val isOptimal = optMoves > 0 && actualMoveCount == optMoves
                    existing.recordSolvedWithoutHints(isOptimal)
                    println("[HISTORY] recordSolvedWithoutHints: isOptimal=$isOptimal, moves=$actualMoveCount, optimal=$optMoves")
                }
            }

            // Mark everUsedHints if hints were used
            if (maxHintUsed >= 0) {
                existing.markEverUsedHints()
            }

            // Save the same list we modified (not a freshly-read copy from disk)
            roboyard.logic.managers.GameHistoryManager.saveHistoryIndex(storage, allEntries)
            println("[HISTORY] Saved updated history entry: completionCount=${existing.completionCount}, maxHintUsed=${existing.maxHintUsed}, everUsedHints=${existing.isEverUsedHints()}")
        } catch (e: Exception) {
            println("[HISTORY] Error updating hint tracking: ${e.message}")
            e.printStackTrace()
        }
    }

    // Save to history immediately, bypassing the time threshold (same as main game)
    // Called when a hint is shown, live move counter is activated, or map is completed
    fun saveToHistoryNow(reason: String) {
        println("[HISTORY] saveToHistoryNow called: reason=$reason, isHistorySaved=$isHistorySaved, maxHintUsed=$maxHintUsed")
        if (!isHistorySaved) {
            // Immediate save triggered by: reason
            println("[HISTORY] First save triggered by: $reason")
            saveToHistory()
            isHistorySaved = true
        } else {
            // Already saved - just update hint tracking in existing entry
            println("[HISTORY] Already saved, updating hint tracking for: $reason")
            updateHintTrackingInHistory()
        }
    }

    // Save game to slot function (same as main game)
    fun saveGame(slotId: Int): Boolean {
        try {
            val storage = Preferences.storageProvider?.invoke()
            if (storage == null) {
                println("[SAVE_GAME] ERROR: storage is null")
                return false
            }
            
            // Save slot file name (same as main game)
            val saveFileName = "saves/save_$slotId.dat"
            println("[SAVE_GAME] Attempting to save to: $saveFileName")
            
            // Serialize board to save data format
            val saveData = buildString {
                appendLine("width:${currentBoard.width}")
                appendLine("height:${currentBoard.height}")
                appendLine("robots:${currentBoard.robotPositions.joinToString(",")}")
                for (goal in currentBoard.goals) {
                    appendLine("goal:${goal.position},${goal.robotNumber}")
                }
                appendLine("moveCount:$moveCount")
                appendLine("isLevelGame:$isLevelGame")
                appendLine("timestamp:${System.currentTimeMillis()}")
                appendLine("gameWon:$gameWon")
            }
            
            println("[SAVE_GAME] Save data: $saveData")
            val result = storage.writeFile(saveFileName, saveData)
            println("[SAVE_GAME] Write result: $result")
            println("[SAVE_GAME] File exists after write: ${storage.fileExists(saveFileName)}")
            return result
        } catch (e: Exception) {
            println("[SAVE_GAME] ERROR: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    // Load game from slot function (same as main game)
    fun loadGame(slotId: Int): Boolean {
        try {
            val storage = Preferences.storageProvider?.invoke()
            if (storage == null) {
                println("[LOAD_GAME] ERROR: storage is null")
                return false
            }
            
            // Save slot file name (same as main game)
            val saveFileName = "saves/save_$slotId.dat"
            println("[LOAD_GAME] Attempting to load from: $saveFileName")
            
            if (!storage.fileExists(saveFileName)) {
                println("[LOAD_GAME] ERROR: File does not exist: $saveFileName")
                return false
            }
            
            println("[LOAD_GAME] File exists, reading...")
            val saveData = storage.readFile(saveFileName)
            println("[LOAD_GAME] Save data: $saveData")
            val lines = saveData.lines()
            println("[LOAD_GAME] Number of lines: ${lines.size}")
            
            var width = 0
            var height = 0
            var robots = ""
            var moveCount = 0
            var isLevelGame = false
            var gameWon = false
            
            for (line in lines) {
                println("[LOAD_GAME] Processing line: $line")
                when {
                    line.startsWith("width:") -> width = line.substringAfter("width:").toInt()
                    line.startsWith("height:") -> height = line.substringAfter("height:").toInt()
                    line.startsWith("robots:") -> robots = line.substringAfter("robots:")
                    line.startsWith("moveCount:") -> moveCount = line.substringAfter("moveCount:").toInt()
                    line.startsWith("isLevelGame:") -> isLevelGame = line.substringAfter("isLevelGame:").toBoolean()
                    line.startsWith("gameWon:") -> gameWon = line.substringAfter("gameWon:").toBoolean()
                }
            }
            
            println("[LOAD_GAME] Parsed: width=$width, height=$height, robots=$robots, moveCount=$moveCount, isLevelGame=$isLevelGame, gameWon=$gameWon")
            
            // Reconstruct board from save data using createBoardFreestyle
            val robotPositions = robots.split(",").map { it.toInt() }
            val numRobots = robotPositions.size
            println("[LOAD_GAME] Creating board with $numRobots robots")
            val newBoard = Board.createBoardFreestyle(null, width, height, numRobots)
            
            if (newBoard == null) {
                println("[LOAD_GAME] ERROR: Board creation failed")
                return false
            }
            
            // Set robot positions
            for (i in robotPositions.indices) {
                newBoard.robotPositions[i] = robotPositions[i]
            }
            
            println("[LOAD_GAME] Board created successfully: ${newBoard.width}x${newBoard.height}")
            currentBoard = newBoard
            return true
        } catch (e: Exception) {
            println("[LOAD_GAME] ERROR: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    // Timer effect - runs every 500ms when timer is enabled (matches Android)
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerRunning) {
                delay(500)
                if (timerRunning) {
                    elapsedTime += 500
                    
                    // Update totalPlayTime (same as main game)
                    val elapsedSeconds = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()
                    totalPlayTime = elapsedSeconds
                    
                    // Check for history save threshold (same as main game)
                    if (!isHistorySaved) {
                        if (totalPlayTime >= HISTORY_SAVE_THRESHOLD) {
                            isHistorySaved = true
                            saveToHistory()
                        }
                    }
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

    // Stop timer when game is won and show completion dialog
    LaunchedEffect(gameWon) {
        gameController.isGameComplete = gameWon
        if (gameWon) {
            timerRunning = false
            showCompletionDialog = true
        }
    }

    // Start solver automatically when game starts (to calculate optimal moves)
    LaunchedEffect(board) {
        if (solution == null && !isSolverRunning) {
            isSolverRunning = true
            Thread {
                try {
                    var currentRegenerationCount = 0
                    val minRequiredMoves = Preferences.minSolutionMoves
                    val maxRequiredMoves = Preferences.maxSolutionMoves

                    // Only validate difficulty for random games (not level games or loaded games)
                    val shouldValidateDifficulty = !isLevelGame && !isLoadedGame

                    while (currentRegenerationCount <= MAX_AUTO_REGENERATIONS && allowRegeneration && shouldValidateDifficulty) {
                        val solver = driftingdroids.model.SolverIDDFS(currentBoard)
                        val solutions = solver.execute()

                        if (solutions.isEmpty() || solutions[0].size() <= 0) {
                            // No solution found - regenerate map
                            currentRegenerationCount++
                            if (currentRegenerationCount <= MAX_AUTO_REGENERATIONS) {
                                onNewGame()
                                return@Thread
                            }
                            break
                        }

                        val moveCount = solutions[0].size()

                        // Quick check for trivial puzzles (1 move or already solved) before difficulty validation
                        if (currentBoard.isTrivialPuzzle()) {
                            println("[TRIVIAL_CHECK] Detected trivial puzzle, regenerating without running solver")
                            currentRegenerationCount++
                            if (currentRegenerationCount <= MAX_AUTO_REGENERATIONS) {
                                onNewGame()
                                return@Thread
                            }
                            break
                        }

                        // Check if solution is too easy or too hard
                        if (moveCount < minRequiredMoves || moveCount > maxRequiredMoves) {
                            currentRegenerationCount++
                            if (currentRegenerationCount <= MAX_AUTO_REGENERATIONS) {
                                onNewGame()
                                return@Thread
                            }
                            break
                        }

                        // Map is valid - accept it
                        solution = solutions[0]
                        break
                    }

                    // For level games or loaded games, just accept the solution without validation
                    if (!shouldValidateDifficulty && solution == null) {
                        val solver = driftingdroids.model.SolverIDDFS(currentBoard)
                        val solutions = solver.execute()
                        if (solutions.isNotEmpty() && solutions[0].size() > 0) {
                            solution = solutions[0]
                        }
                    }
                } catch (e: Exception) {
                    // Solver error - ignore, hints will still work
                } finally {
                    isSolverRunning = false
                }
            }.start()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Game grid at top, full width, maintaining square aspect ratio
        androidx.compose.runtime.key(currentBoard.robotPositions.contentHashCode()) {
            BoardCanvas(
                board = currentBoard,
                startBoard = startBoard,
                pathTracker = pathTracker,
                selectedRobotIndex = selectedRobotIndex,
                selectedRobotHasMoved = selectedRobotHasMoved,
                onRobotSelected = { robotIndex ->
                    // When a robot is touched, select it and reset moved state
                    if (selectedRobotIndex != robotIndex) {
                        selectedRobotIndex = robotIndex
                        selectedRobotHasMoved = false
                    }
                },
                onRobotMove = { robotIndex, direction ->
                if (!gameWon) {
                    val oldPos = currentBoard.robotPositions[robotIndex]
                    val newBoard = gameController.moveRobotWithCooldown(currentBoard, robotIndex, direction)
                    if (newBoard != null) {
                        // Save current board to history before move (for undo)
                        boardHistory.add(Board.Companion.createClone(currentBoard))
                        val newPos = newBoard.robotPositions[robotIndex]
                        val w = newBoard.width
                        val distance = kotlin.math.abs((newPos % w) - (oldPos % w)) +
                            kotlin.math.abs((newPos / w) - (oldPos / w))
                        // Track path for rendering (matches Android GameGridView)
                        pathTracker.addPathSegment(
                            robotIndex,
                            oldPos % w, oldPos / w,
                            newPos % w, newPos / w
                        )
                        currentBoard = newBoard

                        // Play move sound (matches Android SoundManager)
                        soundManager.playSound("move")

                        // Check if this is the first move (same as main game)
                        val wasFirstMove = (moveCount == 0)
                        
                        moveCount++
                        squaresMoved += distance
                        // Mark that the selected robot has moved (for scale animation)
                        selectedRobotHasMoved = true
                        
                        // Save history immediately on first move (same as main game)
                        if (wasFirstMove && !isHistorySaved) {
                            isHistorySaved = true
                            Thread {
                                try {
                                    saveToHistory()
                                } catch (e: Exception) {
                                    // Error saving history on first move
                                }
                            }.start()
                        }
                        
                        // Check if player followed the current hint (auto-advance via HintManager)
                        if (hintMessage != null && robotIndex == currentHintRobot && direction == currentHintDirection) {
                            // Player followed the hint, show next hint automatically via HintManager
                            if (hintManager.hasNextHint()) {
                                hintManager.nextHint()
                                hintMessage = hintManager.getFullHintText()
                                val regularHint = hintManager.getRegularHint()
                                if (regularHint != null) {
                                    currentHintRobot = regularHint.first
                                    currentHintDirection = regularHint.second
                                } else {
                                    currentHintRobot = -1
                                    currentHintDirection = -1
                                }
                                maxHintUsed = maxOf(maxHintUsed, hintManager.getCurrentHintStep())
                                saveToHistoryNow("hint_shown_${hintManager.getCurrentHintStep()}")
                            } else {
                                hintMessage = "All hints shown"
                            }
                        } else if (hintMessage != null) {
                            // Player made a different move, clear hint
                            hintMessage = null
                        }
                        
                        // [GAME_WIN] Check if the goal robot reached its target
                        if (newBoard.goals.isNotEmpty() && isBoardSolved(newBoard)) {
                            gameWon = true
                            // Save to history immediately on completion (same as main game)
                            saveToHistoryNow("completed")
                            // Play win sound (matches Android SoundManager)
                            soundManager.playSound("win")
                            val optimalMoves = solution?.size() ?: 0
                            val stars = calculateStars(moveCount, optimalMoves, hintsUsed)
                            
                            // Save level completion data if this is a level game
                            if (isLevelGame) {
                                roboyard.logic.core.saveLevelCompletion(levelCompletionManager, levelId, moveCount, hintsUsed, optimalMoves, stars, squaresMoved, elapsedTime)
                            }
                            
                            val completionMessage = handleGameWin(moveCount, isLevelGame = isLevelGame, optimalMoves = optimalMoves, stars = stars)
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
                // Select robot button
                val robotColorName = if (selectedRobotIndex >= 0) getRobotColorName(selectedRobotIndex) else "Robot"
                val robotButtonColor = if (selectedRobotIndex >= 0) getRobotButtonColor(selectedRobotIndex) else FancyButtonColor.BLUE
                FancyButton(
                    text = robotColorName,
                    color = robotButtonColor,
                    onClick = {
                        selectedRobotIndex = (selectedRobotIndex + 1) % currentBoard.robotPositions.size
                        selectedRobotHasMoved = false
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                // Direction buttons (text shows "ColorName DirectionName" like Android)
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.NORTH)}",
                    color = robotButtonColor,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = gameController.moveRobotWithCooldown(currentBoard, selectedRobotIndex, Board.NORTH)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isBoardSolved(newBoard)) {
                                    gameWon = true
                                    val optimalMoves = solution?.size() ?: 0
                                    val stars = roboyard.logic.core.calculateStars(moveCount, optimalMoves, hintsUsed)
                                    
                                    // Save level completion data if this is a level game
                                    if (isLevelGame) {
                                        roboyard.logic.core.saveLevelCompletion(levelCompletionManager, levelId, moveCount, hintsUsed, optimalMoves, stars, squaresMoved, elapsedTime)
                                    }
                                    
                                    val completionMessage = handleGameWin(moveCount, isLevelGame = isLevelGame, optimalMoves = optimalMoves, stars = stars)
                                    hintMessage = completionMessage
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.SOUTH)}",
                    color = robotButtonColor,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = gameController.moveRobotWithCooldown(currentBoard, selectedRobotIndex, Board.SOUTH)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isBoardSolved(newBoard)) {
                                    gameWon = true
                                    val optimalMoves = solution?.size() ?: 0
                                    val stars = roboyard.logic.core.calculateStars(moveCount, optimalMoves, hintsUsed)
                                    
                                    // Save level completion data if this is a level game
                                    if (isLevelGame) {
                                        roboyard.logic.core.saveLevelCompletion(levelCompletionManager, levelId, moveCount, hintsUsed, optimalMoves, stars, squaresMoved, elapsedTime)
                                    }
                                    
                                    val completionMessage = handleGameWin(moveCount, isLevelGame = isLevelGame, optimalMoves = optimalMoves, stars = stars)
                                    hintMessage = completionMessage
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.EAST)}",
                    color = robotButtonColor,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = gameController.moveRobotWithCooldown(currentBoard, selectedRobotIndex, Board.EAST)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isBoardSolved(newBoard)) {
                                    gameWon = true
                                    val optimalMoves = solution?.size() ?: 0
                                    val stars = roboyard.logic.core.calculateStars(moveCount, optimalMoves, hintsUsed)
                                    
                                    // Save level completion data if this is a level game
                                    if (isLevelGame) {
                                        roboyard.logic.core.saveLevelCompletion(levelCompletionManager, levelId, moveCount, hintsUsed, optimalMoves, stars, squaresMoved, elapsedTime)
                                    }
                                    
                                    val completionMessage = handleGameWin(moveCount, isLevelGame = isLevelGame, optimalMoves = optimalMoves, stars = stars)
                                    hintMessage = completionMessage
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FancyButton(
                    text = "$robotColorName ${getDirectionName(Board.WEST)}",
                    color = robotButtonColor,
                    onClick = {
                        if (!gameWon) {
                            val newBoard = gameController.moveRobotWithCooldown(currentBoard, selectedRobotIndex, Board.WEST)
                            if (newBoard != null) {
                                boardHistory.add(Board.Companion.createClone(currentBoard))
                                currentBoard = newBoard
                                moveCount++
                                squaresMoved++
                                hintMessage = null
                                if (newBoard.goals.isNotEmpty() && isBoardSolved(newBoard)) {
                                    gameWon = true
                                    val optimalMoves = solution?.size() ?: 0
                                    val stars = roboyard.logic.core.calculateStars(moveCount, optimalMoves, hintsUsed)
                                    
                                    // Save level completion data if this is a level game
                                    if (isLevelGame) {
                                        roboyard.logic.core.saveLevelCompletion(levelCompletionManager, levelId, moveCount, hintsUsed, optimalMoves, stars, squaresMoved, elapsedTime)
                                    }
                                    
                                    val completionMessage = handleGameWin(moveCount, isLevelGame = isLevelGame, optimalMoves = optimalMoves, stars = stars)
                                    hintMessage = completionMessage
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hint container (visible when hint is active)
        if (hintMessage != null || solution != null) {
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
                    onClick = {
                        if (hintManager.hasPrevHint()) {
                            hintManager.prevHint()
                            hintMessage = hintManager.getFullHintText()
                            // Update current hint robot/direction for auto-advance
                            val regularHint = hintManager.getRegularHint()
                            if (regularHint != null) {
                                currentHintRobot = regularHint.first
                                currentHintDirection = regularHint.second
                            } else {
                                currentHintRobot = -1
                                currentHintDirection = -1
                            }
                        }
                    },
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
                // Optimal moves button (shows when solution is available)
                if (solution != null) {
                    FancyButton(
                        text = solution!!.size().toString(),
                        color = FancyButtonColor.HINT,
                        onClick = { },
                        modifier = Modifier.height(32.dp).width(48.dp)
                    )
                }
                // Next hint button
                FancyButton(
                    text = "▸",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        if (hintManager.hasNextHint()) {
                            hintManager.nextHint()
                            hintMessage = hintManager.getFullHintText()
                            // Update current hint robot/direction for auto-advance
                            val regularHint = hintManager.getRegularHint()
                            if (regularHint != null) {
                                currentHintRobot = regularHint.first
                                currentHintDirection = regularHint.second
                            } else {
                                currentHintRobot = -1
                                currentHintDirection = -1
                            }
                            // Save to history when a hint is shown
                            maxHintUsed = maxOf(maxHintUsed, hintManager.getCurrentHintStep())
                            saveToHistoryNow("hint_shown_${hintManager.getCurrentHintStep()}")
                        }
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        // Game info card below the board
        GameInfoCard(
            moveCount = moveCount,
            squaresMoved = squaresMoved,
            difficulty = "Beginner",
            timer = formatElapsedTime(elapsedTime),
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
                        // Navigate to SaveLoadScreen to select save slot
                        onSaveLoad(currentBoard, startBoard)
                    },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = if (hintMessage != null) "❌ Hint" else if (isSolverRunning) "Calculating..." else "💡Hint",
                    color = FancyButtonColor.HINT,
                    onClick = {
                        println("[HINT] Hint button clicked, isSolverRunning=$isSolverRunning, solution=${solution}")
                        if (isSolverRunning) {
                            println("[HINT] Solver already running, returning")
                            return@FancyButton
                        }

                        if (solution == null) {
                            // Calculate solution using SolverIDDFS
                            println("[HINT] Solution is null, starting solver")
                            isSolverRunning = true
                            hintMessage = "Calculating solution..."

                            // Run solver in background thread
                            Thread {
                                try {
                                    val solver = driftingdroids.model.SolverIDDFS(currentBoard)
                                    val solutions = solver.execute()
                                    if (solutions.isNotEmpty() && solutions[0].size() > 0) {
                                        solution = solutions[0]
                                        // Initialize HintManager with pre-hints (matches Android app)
                                        hintManager.initialize(solution, isLevelGame, levelId)
                                        currentHintStep = 0
                                        // Show first hint (pre-hint or regular hint)
                                        hintMessage = hintManager.getFullHintText()
                                        // Update current hint robot/direction for auto-advance
                                        val regularHint = hintManager.getRegularHint()
                                        if (regularHint != null) {
                                            currentHintRobot = regularHint.first
                                            currentHintDirection = regularHint.second
                                        } else {
                                            currentHintRobot = -1
                                            currentHintDirection = -1
                                        }
                                        maxHintUsed = maxOf(maxHintUsed, 0)
                                        saveToHistoryNow("hint_shown_0")
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
                            // Show next hint using HintManager
                            println("[HINT] Solution exists, showing next hint via HintManager")
                            if (hintManager.hasNextHint()) {
                                hintManager.nextHint()
                                hintMessage = hintManager.getFullHintText()
                                val regularHint = hintManager.getRegularHint()
                                if (regularHint != null) {
                                    currentHintRobot = regularHint.first
                                    currentHintDirection = regularHint.second
                                } else {
                                    currentHintRobot = -1
                                    currentHintDirection = -1
                                }
                                maxHintUsed = maxOf(maxHintUsed, hintManager.getCurrentHintStep())
                                saveToHistoryNow("hint_shown_${hintManager.getCurrentHintStep()}")
                            } else {
                                hintMessage = "All hints shown"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 3.dp)
                )
                FancyButton(
                    text = if (gameController.getPathHistorySize() > 0) "Undo" else "Back",
                    color = if (gameController.getPathHistorySize() > 0) FancyButtonColor.YELLOW else FancyButtonColor.GREEN,
                    onClick = {
                        if (gameController.getPathHistorySize() > 0) {
                            // Get the last path entry BEFORE undoing (undoLastMove removes it from history)
                            val lastPathEntry = gameController.getPathHistoryList().lastOrNull()
                            // Undo last move using GameController (matches Android app behavior)
                            val undoneBoard = gameController.undoLastMove(currentBoard)
                            if (undoneBoard != null && lastPathEntry != null) {
                                // Undo last path segment for the correct robot (matches Android GameGridView.undoLastPathSegment)
                                pathTracker.undoLastPathSegment(lastPathEntry[0])
                                currentBoard = undoneBoard
                                moveCount--
                                squaresMoved = maxOf(0, squaresMoved - 1)
                                hintMessage = null
                                gameWon = false
                            } else {
                                // Fallback to board history if GameController undo fails
                                if (boardHistory.isNotEmpty()) {
                                    val previousBoard = boardHistory.removeAt(boardHistory.size - 1)
                                    pathTracker.clearPaths()
                                    currentBoard = previousBoard
                                    moveCount--
                                    squaresMoved = maxOf(0, squaresMoved - 1)
                                    hintMessage = null
                                    gameWon = false
                                }
                            }
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
                    text = if (gameWon) "Retry" else "Reset",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        currentBoard = Board.Companion.createClone(startBoard).also {
                            it.setRobots(startBoard.robotPositions.copyOf())
                        }
                        moveCount = 0
                        squaresMoved = 0
                        hintMessage = null
                        gameWon = false
                        gameController.reset()
                        hintManager.reset()
                        pathTracker.clearPaths()
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
    }

    // Completion dialog with retry button
    if (showCompletionDialog) {
        val optimalMoves = solution?.size() ?: 0
        val stars = calculateStars(moveCount, optimalMoves, maxHintUsed)
        val finalStars = if (stars < 1 && isLevelGame && levelId <= 10) {
            1
        } else {
            stars
        }

        val title = if (isLevelGame) {
            "Level Complete"
        } else {
            "Random Game Complete"
        }

        val message = buildString {
            if (isLevelGame) {
                append("Level $levelId completed in $moveCount moves.\n")
                append("Stars: ")
                repeat(finalStars) { append("★ ") }
                if (finalStars == 0) append("✓")
            } else {
                append("Completed in $moveCount moves.\n")
                if (moveCount == optimalMoves && optimalMoves > 0) {
                    append("Perfect Solution!")
                } else if (optimalMoves > 0) {
                    append("Optimal: $optimalMoves moves")
                }
            }
        }

        val nextButtonText = if (isLevelGame) {
            "Next Level"
        } else {
            "New Random Game"
        }

        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Menu button - return to main menu
                    Button(onClick = {
                        showCompletionDialog = false
                        onBack()
                    }) {
                        Text("Menu")
                    }
                    // Retry button - reset board to start state
                    Button(onClick = {
                        showCompletionDialog = false
                        currentBoard = Board.Companion.createClone(startBoard).also {
                            it.setRobots(startBoard.robotPositions.copyOf())
                        }
                        moveCount = 0
                        squaresMoved = 0
                        gameWon = false
                        timerRunning = false
                        elapsedTime = 0L
                        hintMessage = null
                        maxHintUsed = -1
                        hintsUsed = 0
                        currentHintStep = 0
                        solution = null
                        isSolverRunning = false
                        boardHistory.clear()
                        gameController.reset()
                        hintManager.reset()
                        pathTracker.clearPaths()
                        gameStartTime = System.currentTimeMillis()
                        totalPlayTime = 0
                        isHistorySaved = false
                    }) {
                        Text("Retry")
                    }
                    // Next Level / New Game button
                    Button(onClick = {
                        showCompletionDialog = false
                        if (isLevelGame) {
                            onNextLevel()
                        } else {
                            onNewGame()
                        }
                    }) {
                        Text(nextButtonText)
                    }
                }
            }
        )
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

