package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import driftingdroids.model.Board
import roboyard.logic.storage.getPlatformStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    startBoard: Board? = null,
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
            CircularButton(
                text = null,
                color = CircularButtonColor.TURQUOISE,
                onClick = { },
                modifier = Modifier.size(48.dp)
            )
        }

        // Tab layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                FancyButton(
                    text = tab,
                    color = if (selectedTab == index) FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTab = index },
                    modifier = Modifier.weight(1f).semantics { testTag = "tab_$index" }
                )
            }
        }

        // Save slots or history entries
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp)
        ) {
            if (selectedTab == 2) {
                // History tab
                if (historyEntries.isEmpty()) {
                    Text(
                        text = "No history entries yet",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    historyEntries.forEach { (index, fileName, entry) ->
                        HistoryItem(
                            historyIndex = index,
                            fileName = fileName,
                            mapName = entry?.mapName ?: "",
                            moves = entry?.movesMade ?: 0,
                            time = entry?.playDuration ?: 0,
                            stars = entry?.starsEarned ?: 0,
                            hintsUsed = entry?.isEverUsedHints() ?: false,
                            onClick = {
                                // Load history entry
                                println("[SAVE_LOAD_SCREEN] Loading history entry: $fileName")
                                val saveData = storage.readFile(fileName)
                                val loadedBoard = deserializeBoardFromMainGameFormat(saveData)
                                if (loadedBoard != null) {
                                    onLoadGame(loadedBoard)
                                }
                            },
                            onInfoClick = {
                                selectedEntry = entry
                                showInfoDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Save/Load tabs
                repeat(10) { slotIndex ->
                    val slotNumber = slotIndex + 1
                    val isEmpty = !slotStates[slotIndex]
                    SaveSlotItem(
                        slotNumber = slotNumber,
                        isEmpty = isEmpty,
                        onClick = {
                            if (selectedTab == 0 && boardToSave != null) {
                                // Save game to slot using Main Game format
                                println("[SAVE_LOAD_SCREEN] Saving game to slot $slotNumber")
                                val fileName = "saves/save_$slotNumber.dat"
                                
                                // Serialize board to Main Game format (DRY - use startBoard for consistent signature)
                                val saveData = serializeBoardToMainGameFormat(boardToSave, isLevelGame, startBoard)
                                
                                println("[SAVE_LOAD_SCREEN] Save data: $saveData")
                                val result = storage.writeFile(fileName, saveData)
                                println("[SAVE_LOAD_SCREEN] Write result: $result")
                                
                                if (result) {
                                    // Verify save file contains targets
                                    val savedContent = storage.readFile(fileName)
                                    if (!validateSaveContainsTargets(savedContent, fileName)) {
                                        storage.writeFile(fileName, "") // Delete invalid save
                                        println("[SAVE_LOAD_SCREEN] Save file validation failed: No targets found")
                                    } else {
                                        // Update slot state
                                        val newSlotStates = slotStates.toMutableList()
                                        newSlotStates[slotIndex] = true
                                        slotStates = newSlotStates
                                        println("[SAVE_LOAD_SCREEN] Game saved to slot $slotNumber")
                                    }
                                }
                            } else if (!isEmpty && selectedTab == 1) {
                                // Load game from slot using Main Game format
                                println("[SAVE_LOAD_SCREEN] Loading game from slot $slotNumber")
                                val fileName = "saves/save_$slotNumber.dat"
                                val saveData = storage.readFile(fileName)
                                println("[SAVE_LOAD_SCREEN] Save data: $saveData")
                                
                                val loadedBoard = deserializeBoardFromMainGameFormat(saveData)
                                
                                if (loadedBoard != null) {
                                    println("[SAVE_LOAD_SCREEN] Board created successfully: ${loadedBoard.width}x${loadedBoard.height}")
                                    onLoadGame(loadedBoard)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Back button
        FancyButton(
            text = "Back",
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Info dialog
    if (showInfoDialog && selectedEntry != null) {
        HistoryInfoDialog(
            entry = selectedEntry!!,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
fun SaveSlotItem(
    slotNumber: Int,
    isEmpty: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (isEmpty) "Slot $slotNumber (Empty)" else "Slot $slotNumber",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun HistoryInfoDialog(
    entry: roboyard.logic.core.GameHistoryEntry,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    println("[HISTORY_INFO_DIALOG] entry.bestTime=${entry.bestTime}, entry.bestMoves=${entry.bestMoves}, entry.completionCount=${entry.completionCount}")
    
    val message = buildString {
        append("Completions: ${entry.completionCount}\n")
        append("First started: ${sdf.format(Date(entry.timestamp))}\n")
        if (entry.lastCompletionTimestamp > 0) {
            append("Last played: ${sdf.format(Date(entry.lastCompletionTimestamp))}\n")
        }
        
        val timestamps = entry.getCompletionTimestamps()
        if (timestamps != null && timestamps.size > 1) {
            val isLevelGame = entry.mapName?.startsWith("Level ") == true
            val completionStars = entry.getCompletionStars()
            val completionMoves = entry.getCompletionMoves()
            
            append("\nAll completions:\n")
            for (i in timestamps.indices) {
                append("  ${i + 1}. ${sdf.format(Date(timestamps[i]))}")
                if (isLevelGame) {
                    val stars = if (completionStars != null && i < completionStars.size) {
                        completionStars[i]
                    } else {
                        entry.starsEarned
                    }
                    val moves = if (completionMoves != null && i < completionMoves.size) {
                        completionMoves[i]
                    } else {
                        entry.movesMade
                    }
                    if (stars == 0) {
                        append(" ✓")
                    } else {
                        repeat(stars) { append("★") }
                    }
                    append(" - $moves")
                } else {
                    val moves = if (completionMoves != null && i < completionMoves.size) {
                        completionMoves[i]
                    } else {
                        entry.movesMade
                    }
                    append(" - $moves")
                }
                append("\n")
            }
        }
        
        append("\nBest time: ")
        println("[HISTORY_INFO_DIALOG] Displaying bestTime: ${entry.bestTime}, condition: ${entry.bestTime > 0}")
        if (entry.bestTime > 0) {
            append("${entry.bestTime / 60}m ${entry.bestTime % 60}s")
        } else {
            append("—")
        }
        append("\n")
        
        append("Best moves: ")
        println("[HISTORY_INFO_DIALOG] Displaying bestMoves: ${entry.bestMoves}, condition: ${entry.bestMoves > 0}")
        append(if (entry.bestMoves > 0) entry.bestMoves else "—")
        append("\n")
        
        append("Optimal moves: ")
        if (entry.optimalMoves > 0) {
            append(entry.optimalMoves)
            if (entry.bestMoves > 0 && entry.bestMoves == entry.optimalMoves) {
                append(" ✓ (Perfect)")
            } else if (entry.bestMoves > 0) {
                append(" (+${entry.bestMoves - entry.optimalMoves} extra moves)")
            }
        } else {
            append("—")
        }
        append("\n")
        
        append("\nHint usage (last): ")
        val maxHint = entry.maxHintUsed
        when {
            maxHint < 0 -> append("No hints used")
            maxHint == 0 -> append("Pre-hint viewed")
            else -> append("Up to hint ${maxHint + 1}")
        }
        append("\n")
        
        append("Hints ever used: ")
        append(if (entry.isEverUsedHints()) "Yes" else "No")
        append("\n")
        
        append("Qualifies for no-hints achievement: ")
        append(if (entry.qualifiesForNoHintsAchievement()) "Yes" else "No")
        append("\n")
        
        append("Qualifies for perfect no-hints achievement: ")
        append(if (entry.qualifiesForPerfectNoHintsAchievement()) "Yes" else "No")
        append("\n")
        
        append("Last solved without hints: ")
        val lastNoHints = entry.lastSolvedWithoutHints
        append(if (lastNoHints > 0) sdf.format(Date(lastNoHints)) else "—")
        append("\n")
        
        append("Last perfectly solved without hints: ")
        val lastPerfect = entry.lastPerfectlySolvedWithoutHints
        append(if (lastPerfect > 0) sdf.format(Date(lastPerfect)) else "—")
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.mapName ?: "Unknown Map") },
        text = { Text(message, fontSize = 12.sp) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
