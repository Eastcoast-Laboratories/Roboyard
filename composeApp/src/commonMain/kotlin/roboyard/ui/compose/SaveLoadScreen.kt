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
                                val saveData = serializeBoardToMainGameFormat(boardToSave, isLevelGame, null)
                                
                                println("[SAVE_LOAD_SCREEN] Save data: $saveData")
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
            text = if (isEmpty) "Slot $slotNumber (Empty)" else "Slot $slotNumber - Level 1, 5 moves",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun DebugSettingsScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Debug Settings",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Hint Auto Move Settings
            Text(
                text = "Hint Auto Move Mode",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Current mode: Manual",
                color = Color(0xFFFFFF00),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FancyButton(
                    text = "Manual",
                    color = FancyButtonColor.BLUE,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "Full-Auto",
                    color = FancyButtonColor.BLUE,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "Semi-Auto",
                    color = FancyButtonColor.BLUE,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dummy History Entries
            Text(
                text = "Dummy History Entries",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FancyButton(
                text = "Add 100 Dummy Entries",
                color = FancyButtonColor.GREEN,
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Control
            Text(
                text = "App Control",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FancyButton(
                text = "Restart App",
                color = FancyButtonColor.RED,
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Back button
        FancyButton(
            text = "Back",
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LevelDesignEditorScreen(
    onBack: () -> Unit = {},
    onPlayMap: (Board) -> Unit = {}
) {
    var selectedTool by remember { mutableStateOf("Wall") }
    var selectedTarget by remember { mutableStateOf("None") }
    var boardWidth by remember { mutableStateOf("12") }
    var boardHeight by remember { mutableStateOf("14") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Level Design Editor",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Wall Tool Selection
            Text(
                text = "Wall Tool",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
