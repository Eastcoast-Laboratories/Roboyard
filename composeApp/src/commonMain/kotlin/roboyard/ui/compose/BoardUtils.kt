package roboyard.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
/**
 * Generate a unique signature for the wall layout only.
 * Used for achievements that track same walls with different robot positions.
 * Format matches level file format: 12x14;mh1,0;mh1,3;...mv9,6;
 */
fun generateWallSignature(board: Board): String {
    val sb = StringBuilder()
    sb.append(board.width).append("x").append(board.height).append(";")

    // Collect all walls in sorted order
    val walls = mutableListOf<String>()
    for (y in 0 until board.height) {
        for (x in 0 until board.width) {
            val position = y * board.width + x
            // Check for horizontal (SOUTH) walls
            if (board.isWall(position, 2)) {
                walls.add("mh$x,$y")
            }
            // Check for vertical (EAST) walls
            if (board.isWall(position, 1)) {
                walls.add("mv$x,$y")
            }
        }
    }
    walls.sort()
    for (wall in walls) {
        sb.append(wall).append(";")
    }
    return sb.toString()
}

/**
 * Generate a unique signature for robot and target positions only.
 * Used for achievements that track same positions with different wall layouts.
 * Format: 12x14;Rb3,4;Rg7,8;...Tb2,5;Tg9,10;...
 */
fun generatePositionSignature(board: Board): String {
    val sb = StringBuilder()
    sb.append(board.width).append("x").append(board.height).append(";")

    // Collect all robots in sorted order
    val robots = mutableListOf<String>()
    for (i in board.robotPositions.indices) {
        val position = board.robotPositions[i]
        val x = position % board.width
        val y = position / board.width
        val colorChar = when (i) {
            0 -> 'r' // red (pink) - matches LevelLoader.parseColorChar
            1 -> 'g' // green
            2 -> 'b' // blue
            3 -> 'y' // yellow
            4 -> 's' // silver
            else -> 'm'
        }
        robots.add("R${colorChar}$x,$y")
    }
    robots.sort()
    for (robot in robots) {
        sb.append(robot).append(";")
    }

    // Collect all targets in sorted order
    val targets = mutableListOf<String>()
    for (goal in board.goals) {
        val x = goal.position % board.width
        val y = goal.position / board.width
        val colorChar = when (goal.robotNumber) {
            0 -> 'r' // red (pink) - matches LevelLoader.parseColorChar
            1 -> 'g' // green
            2 -> 'b' // blue
            3 -> 'y' // yellow
            4 -> 's' // silver
            else -> 'm'
        }
        targets.add("T${colorChar}$x,$y")
    }
    targets.sort()
    for (target in targets) {
        sb.append(target).append(";")
    }

    return sb.toString()
}

/**
 * Generate a complete unique signature for the entire map.
 * Combines wall signature and position signature.
 * Two maps with identical signatures are considered the same map.
 * @param board The current board state
 * @param startBoard Optional start board to use for robot positions (instead of current positions)
 */
fun generateMapSignature(board: Board, startBoard: Board? = null): String {
    val positionBoard = startBoard ?: board
    return generateWallSignature(board) + "||" + generatePositionSignature(positionBoard)
}

/**
 * Generate a unique 5-letter string from an input string (DRY - from MapIdGenerator in main app)
 * The resulting string alternates between consonants and vowels for better readability
 * 
 * @param input The input string to hash
 * @return A 5-letter unique ID string
 */
fun generateUnique5LetterFromString(input: String): String {
    try {
        // Create SHA-256 hash
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        
        // Define vowels and consonants
        val vowels = charArrayOf('A', 'E', 'I', 'O', 'U')
        val consonants = charArrayOf('B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z')
        
        // Convert hash bytes to 5-letter string, alternating between consonants and vowels
        val uniqueString = StringBuilder()
        for (i in 0 until 5) {
            val index = Math.abs(hashBytes[i].toInt()) % (if (i % 2 == 0) consonants.size else vowels.size)
            val letter = if (i % 2 == 0) consonants[index] else vowels[index]
            uniqueString.append(letter)
        }
        
        return uniqueString.toString()
    } catch (e: Exception) {
        println("[MAP_ID_GENERATOR] Failed to generate unique ID: ${e.message}")
        return "ERROR"
    }
}

/**
 * Generate a map name from map signature and game type (DRY - from main app)
 * For Level Games: "Level $levelId"
 * For Random Games: 5-character hash from map signature using SHA-256
 */
fun generateMapNameFromSignature(mapSig: String, isLevelGame: Boolean, levelId: Int? = null): String {
    if (isLevelGame && levelId != null) {
        return "Level $levelId"
    }
    
    // Generate 5-character hash from map signature for random games (DRY - use SHA-256 like main app)
    return generateUnique5LetterFromString(mapSig)
}

/**
 * Serialize a Board to Main Game format for save/load compatibility
 * DRY - Random Games shall run like Level Games with consistent map signature
 */
fun serializeBoardToMainGameFormat(board: Board, isLevelGame: Boolean, startBoard: Board? = null): String {
    val sb = StringBuilder()
    
    // Generate map signature for unique map tracking (DRY - use startBoard for consistent signature)
    val mapSig = generateMapSignature(board, startBoard)
    
    // Generate the metadata section with additional tags
    sb.append("#MAPNAME:Random")
        .append(";TIME:0")
        .append(";MOVES:0")
        .append(";DIFFICULTY:1") // Default difficulty
        .append(";SIZE:").append(board.width).append(",").append(board.height)
        .append(";SOLVED:false")
        .append(";MAX_HINT_USED:-1") // No hints used by default
        .append(";MAP_SIG:").append(mapSig)
        .append("\n")
    
    // Add board dimensions
    sb.append("WIDTH:").append(board.width).append(";\n")
    sb.append("HEIGHT:").append(board.height).append(";\n")
    
    // Generate the board representation (walls excluded - they go in WALLS section)
    // Use startBoard robot positions if available (DRY - Random Games shall run like Level Games)
    val robotPositionsToUse = startBoard?.robotPositions ?: board.robotPositions
    
    for (y in 0 until board.height) {
        for (x in 0 until board.width) {
            if (x > 0) {
                sb.append(",")
            }
            
            val position = y * board.width + x
            
            // Check if this position has a robot (use start positions for consistent history)
            val hasRobot = robotPositionsToUse.contains(position)
            
            // Check if this position has a target
            val goal = board.goals.find { it.position == position }
            
            when {
                hasRobot -> sb.append(4) // TYPE_ROBOT
                goal != null -> {
                    // Target with color
                    val color = goal.robotNumber
                    sb.append(3).append(":").append(color) // TYPE_TARGET with color
                }
                else -> sb.append(0) // TYPE_EMPTY
            }
        }
        sb.append("\n")
    }
    
    // Save targets in compact format: tcolorX,Y; (e.g., tb8,7;)
    for (goal in board.goals) {
        val x = goal.position % board.width
        val y = goal.position / board.width
        val color = goal.robotNumber
        val colorChar = when (color) {
            0 -> 'b' // blue
            1 -> 'g' // green
            2 -> 'r' // red (pink)
            3 -> 'y' // yellow
            4 -> 's' // silver
            else -> 'm' // multi
        }
        sb.append("t").append(colorChar).append(x).append(",").append(y).append(";")
    }
    
    sb.append("\n")
    
    // Save walls in compact format: hX,Y; and vX,Y;
    // Horizontal walls (y goes to height to include bottom boundary)
    for (y in 0..board.height) {
        for (x in 0 until board.width) {
            // Check horizontal wall at position (x, y) - this is the wall between (x, y) and (x, y+1)
            // For the bottom boundary (y = height), we need to check if there's a wall at the bottom of the last row
            val position = if (y < board.height) y * board.width + x else (board.height - 1) * board.width + x
            if (y < board.height && board.isWall(position, 2)) { // SOUTH wall = horizontal
                sb.append("h").append(x).append(",").append(y).append(";")
            }
        }
    }
    // Vertical walls (x goes to width to include right boundary)
    for (y in 0 until board.height) {
        for (x in 0..board.width) {
            // Check vertical wall at position (x, y) - this is the wall between (x-1, y) and (x, y)
            // For the right boundary (x = width), we need to check if there's a wall at the right of the last column
            val position = if (x < board.width) y * board.width + x else y * board.width + (board.width - 1)
            if (x < board.width && board.isWall(position, 1)) { // EAST wall = vertical
                sb.append("v").append(x).append(",").append(y).append(";")
            }
        }
    }
    
    sb.append("\n")
    
    // Save robots in compact format: rcolorX,Y; (e.g., rr1,5;)
    for (i in board.robotPositions.indices) {
        val position = board.robotPositions[i]
        val x = position % board.width
        val y = position / board.width
        val colorChar = when (i) {
            0 -> 'b' // blue
            1 -> 'g' // green
            2 -> 'r' // red (pink)
            3 -> 'y' // yellow
            4 -> 's' // silver
            else -> 'm' // multi
        }
        sb.append("r").append(colorChar).append(x).append(",").append(y).append(";")
    }
    
    return sb.toString()
}

/**
 * Deserialize a Board from Main Game format
 */
private fun deserializeBoardFromMainGameFormat(saveData: String): Board? {
    val lines = saveData.lines()
    var width = 0
    var height = 0
    var boardData = mutableListOf<String>()
    var targetsData = ""
    var wallsData = ""
    var robotsData = ""
    
    for (line in lines) {
        when {
            line.startsWith("WIDTH:") -> width = line.substringAfter("WIDTH:").substringBefore(";").toInt()
            line.startsWith("HEIGHT:") -> height = line.substringAfter("HEIGHT:").substringBefore(";").toInt()
            line.startsWith("#") -> { /* Skip metadata */ }
            line.startsWith("t") -> targetsData += line
            line.startsWith("h") || line.startsWith("v") -> wallsData += line
            line.startsWith("r") -> robotsData += line
            else -> boardData.add(line)
        }
    }
    
    if (width == 0 || height == 0) {
        println("[DESERIALIZE] Invalid dimensions: width=$width, height=$height")
        return null
    }
    
    // Parse robots from ROBOTS section (preferred) or from board data (fallback)
    val robotPositions = mutableListOf<Int>()
    val robotColors = mutableListOf<Int>()
    
    if (robotsData.isNotEmpty()) {
        // Parse robots from ROBOTS section: rcolorX,Y;
        val robotPattern = Regex("r([a-z])(\\d+),(\\d+);")
        robotPattern.findAll(robotsData).forEach { match ->
            val colorChar = match.groupValues[1][0]
            val rx = match.groupValues[2].toInt()
            val ry = match.groupValues[3].toInt()
            val robotNumber = when (colorChar) {
                'b' -> 0
                'g' -> 1
                'r' -> 2
                'y' -> 3
                's' -> 4
                else -> 0
            }
            robotPositions.add(ry * width + rx)
            robotColors.add(robotNumber)
        }
    } else {
        // Fallback: parse robots from board data
        for (y in 0 until height) {
            if (y >= boardData.size) break
            val row = boardData[y].split(",")
            for (x in 0 until width) {
                if (x >= row.size) break
                val cell = row[x]
                when {
                    cell.startsWith("4") -> { // TYPE_ROBOT
                        robotPositions.add(y * width + x)
                        robotColors.add(0) // Default color
                    }
                }
            }
        }
    }
    
    // Parse targets
    val goalData = mutableListOf<Triple<Int, Int, Int>>() // x, y, robotNumber
    val targetPattern = Regex("t([a-z])(\\d+),(\\d+);")
    targetPattern.findAll(targetsData).forEach { match ->
        val colorChar = match.groupValues[1][0]
        val tx = match.groupValues[2].toInt()
        val ty = match.groupValues[3].toInt()
        val robotNumber = when (colorChar) {
            'b' -> 0
            'g' -> 1
            'r' -> 2
            'y' -> 3
            's' -> 4
            else -> 0
        }
        goalData.add(Triple(tx, ty, robotNumber))
    }
    
    if (robotPositions.isEmpty()) {
        println("[DESERIALIZE] No robots found in save data")
        return null
    }
    
    // Create board
    val newBoard = Board.createBoardFreestyle(null, width, height, robotPositions.size)
    if (newBoard == null) {
        println("[DESERIALIZE] Failed to create board")
        return null
    }
    
    // Set robot positions
    for (i in robotPositions.indices) {
        newBoard.robotPositions[i] = robotPositions[i]
    }
    
    // Set goals
    for ((tx, ty, robotNumber) in goalData) {
        newBoard.addGoal(ty * width + tx, robotNumber, robotNumber)
    }
    newBoard.setGoalRandom()
    
    // Parse and set walls
    val hWallPattern = Regex("h(\\d+),(\\d+);")
    val vWallPattern = Regex("v(\\d+),(\\d+);")
    
    hWallPattern.findAll(wallsData).forEach { match ->
        val wx = match.groupValues[1].toInt()
        val wy = match.groupValues[2].toInt()
        // hX,Y; means horizontal wall at (x, y) - prevents movement from (x, y) to SOUTH
        // Set SOUTH wall at position (wx, wy)
        newBoard.setWall(wx, wy, 2, true) // Set SOUTH wall
        // Also set NORTH wall at position (wx, wy+1) for consistency with gridElementsToBoard
        if (wy + 1 < newBoard.height) {
            newBoard.setWall(wx, wy + 1, 0, true) // Set NORTH wall
        }
    }
    
    vWallPattern.findAll(wallsData).forEach { match ->
        val wx = match.groupValues[1].toInt()
        val wy = match.groupValues[2].toInt()
        // vX,Y; means vertical wall at (x, y) - prevents movement from (x-1, y) to (x, y)
        // Set EAST wall at position (wx, wy)
        newBoard.setWall(wx, wy, 1, true) // Set EAST wall
        // Also set WEST wall at position (wx+1, wy) for consistency with gridElementsToBoard
        if (wx + 1 < newBoard.width) {
            newBoard.setWall(wx + 1, wy, 3, true) // Set WEST wall
        }
    }
    
    println("[DESERIALIZE] Board created: ${newBoard.width}x${newBoard.height}, robots: ${newBoard.robotPositions.joinToString(",")}, goals: ${newBoard.goals.size}")
    
    return newBoard
}

/**
 * Save a board to history using Main Game format
 */
private fun saveToHistory(board: Board, storage: PlatformStorage): Boolean {
    try {
        // Get next available history index
        val historyIndex = getNextHistoryIndex(storage)
        val historyFileName = "history_$historyIndex.txt"
        
        // Serialize board to Main Game format
        val saveData = serializeBoardToMainGameFormat(board, false)
        
        // Write to history file
        val result = storage.writeFile(historyFileName, saveData)
        
        if (result) {
            println("[HISTORY] Saved to history: $historyFileName")
        }
        
        return result
    } catch (e: Exception) {
        println("[HISTORY] Error saving to history: ${e.message}")
        return false
    }
}

/**
 * Get the next available history index
 */
private fun getNextHistoryIndex(storage: PlatformStorage): Int {
    var index = 1
    while (storage.fileExists("history_$index.txt")) {
        index++
    }
    return index
}

/**
 * Get all history entries including autosave, sorted by timestamp (newest first)
 * Uses GameHistoryManager for history entries
 * Returns list of Triple with (index, fileName, entry)
 */
private fun getHistoryEntries(storage: PlatformStorage): List<Triple<Int, String, roboyard.logic.core.GameHistoryEntry?>> {
    val entries = mutableListOf<Triple<Int, String, roboyard.logic.core.GameHistoryEntry?>>()
    
    // Add autosave entry first (index 0, no GameHistoryEntry)
    if (storage.fileExists("saves/save_0.dat")) {
        entries.add(Triple(0, "saves/save_0.dat", null))
    }
    
    // Add history entries using GameHistoryManager
    try {
        roboyard.logic.managers.GameHistoryManager.initialize(storage)
        val historyEntries = roboyard.logic.managers.GameHistoryManager.getHistoryEntries(storage)
        for (entry in historyEntries) {
            val index = entry.getHistoryIndex()
            entries.add(Triple(index, entry.getMapPath(), entry))
        }
    } catch (e: Exception) {
        println("[SAVE_LOAD_SCREEN] Error loading history entries: ${e.message}")
    }
    
    // Sort by timestamp (newest first) - like the default in main app SaveGameFragment
    // TODO: spinner option and pagination
    val sortedEntries = entries.sortedByDescending { it.third?.timestamp ?: 0L }
    
    return sortedEntries
}

/**
 * Validate that save file contains targets
 */
private fun validateSaveContainsTargets(saveData: String, fileName: String): Boolean {
    val hasTargets = saveData.contains("t") || saveData.contains("3:")
    if (!hasTargets) {
        println("[SAVE_VERIFICATION] Save file $fileName does not contain targets")
    }
    return hasTargets
}

@Composable
fun HistoryItem(
    historyIndex: Int,
    fileName: String,
    mapName: String = "",
    moves: Int = 0,
    time: Int = 0,
    stars: Int = 0,
    hintsUsed: Boolean = false,
    onClick: () -> Unit = {},
    onInfoClick: () -> Unit = {}
) {
    val displayName = if (historyIndex == 0) {
        "Autosave"
    } else {
        mapName.ifEmpty { "History #$historyIndex" }
    }
    
    val timeStr = if (time > 0) {
        val minutes = time / 60
        val seconds = time % 60
        String.format("%d:%02d", minutes, seconds)
    } else {
        "--:--"
    }
    
    val hintsIndicator = if (hintsUsed) " (H)" else ""
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (stars > 0) {
                    Text(
                        text = "★".repeat(stars),
                        color = Color.Yellow,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                CircularButton(
                    text = "i",
                    color = CircularButtonColor.GRAY,
                    onClick = onInfoClick,
                    modifier = Modifier.semantics { testTag = "infoButton_$historyIndex" }
                )
            }
            Text(
                text = fileName,
                color = Color.LightGray,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Moves: $moves",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Time: $timeStr$hintsIndicator",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SaveLoadScreen(
    boardToSave: Board? = null,
    isLevelGame: Boolean = false,
    onBack: () -> Unit = {},
    onLoadGame: (Board) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Save", "Load", "History")
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<roboyard.logic.core.GameHistoryEntry?>(null) }
    
    // Check for saved games
    val storage = remember { getPlatformStorage() }
    var hasSavedGames by remember { mutableStateOf(false) }
    var slotStates by remember { mutableStateOf(List(10) { false }) }
    var historyEntries by remember { mutableStateOf<List<Triple<Int, String, roboyard.logic.core.GameHistoryEntry?>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        hasSavedGames = storage.hasSavedGames()
        println("[SAVE_LOAD_SCREEN] hasSavedGames: $hasSavedGames")
        // Check each slot
        val newSlotStates = mutableListOf<Boolean>()
        for (i in 1..10) {
            val fileName = "saves/save_$i.dat"
            val exists = storage.fileExists(fileName)
            newSlotStates.add(exists)
            println("[SAVE_LOAD_SCREEN] Slot $i ($fileName): exists=$exists")
        }
        slotStates = newSlotStates
        
        // Load history entries
        historyEntries = getHistoryEntries(storage)
        println("[SAVE_LOAD_SCREEN] History entries: ${historyEntries.size}")
        for ((index, fileName, entry) in historyEntries) {
            println("[SAVE_LOAD_SCREEN] Entry $index: ${entry?.mapName}, bestTime=${entry?.bestTime}, bestMoves=${entry?.bestMoves}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Title and profile button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (selectedTab) {
                    0 -> "Select slot to save game"
                    1 -> "Select slot to load game"
                    else -> "Game History"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
