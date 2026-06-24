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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import driftingdroids.model.Board
import roboyard.logic.storage.getPlatformStorage

@Composable
fun SaveLoadScreen(
    boardToSave: Board? = null,
    isLevelGame: Boolean = false,
    onBack: () -> Unit = {},
    onLoadGame: (Board) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Save", "Load")
    val storage = remember { getPlatformStorage() }
    var slotStates by remember { mutableStateOf(List(10) { false }) }

    LaunchedEffect(Unit) {
        val newSlotStates = mutableListOf<Boolean>()
        for (i in 1..10) {
            val fileName = "saves/save_$i.dat"
            val exists = storage.fileExists(fileName)
            newSlotStates.add(exists)
        }
        slotStates = newSlotStates
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = when (selectedTab) {
                0 -> "Select slot to save game"
                else -> "Select slot to load game"
            },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                Button(
                    onClick = { selectedTab = index },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text(tab)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            repeat(10) { slotIndex ->
                val slotNumber = slotIndex + 1
                val isEmpty = !slotStates[slotIndex]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                        .clickable {
                            if (selectedTab == 0 && boardToSave != null) {
                                val fileName = "saves/save_$slotNumber.dat"
                                val saveData = serializeBoardToMainGameFormat(boardToSave, isLevelGame, null)
                                storage.writeFile(fileName, saveData)
                            } else if (!isEmpty && selectedTab == 1) {
                                val fileName = "saves/save_$slotNumber.dat"
                                val saveData = storage.readFile(fileName)
                                val loadedBoard = deserializeBoardFromMainGameFormat(saveData)
                                if (loadedBoard != null) {
                                    onLoadGame(loadedBoard)
                                }
                            }
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (isEmpty) "Slot $slotNumber (Empty)" else "Slot $slotNumber",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
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
