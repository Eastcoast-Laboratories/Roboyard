package roboyard.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import driftingdroids.model.Board
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.storage.getPlatformStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SaveGame(val id: Int, val level: Int, val moves: Int, val date: String)

@Composable
fun SaveLoadScreen(
    onBack: () -> Unit = {},
    onLoadGame: (Board) -> Unit = {}
) {
    val storage = remember { getPlatformStorage() }
    val savedGames = remember { mutableStateListOf<SaveGame>() }
    var currentBoard by remember { mutableStateOf<Board?>(null) }

    LaunchedEffect(Unit) {
        loadSavedGames(storage, savedGames)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.semantics {
                        contentDescription = "Back to main menu"
                    }
                ) {
                    Text("Back")
                }
                Text(
                    text = "Save / Load",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedGames) { saveGame ->
                    SaveGameCard(
                        saveGame = saveGame,
                        onLoad = {
                            val board = loadBoardFromStorage(storage, saveGame.id)
                            if (board != null) {
                                onLoadGame(board)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SaveGameCard(
    saveGame: SaveGame,
    onLoad: () -> Unit
) {
    Card(
        onClick = onLoad,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Level ${saveGame.level}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Moves: ${saveGame.moves}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = saveGame.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onLoad) {
                Text("Load")
            }
        }
    }
}

fun generateSavedGames(): List<SaveGame> {
    return listOf(
        SaveGame(1, 5, 12, "2025-06-15 14:30"),
        SaveGame(2, 8, 18, "2025-06-15 15:45"),
        SaveGame(3, 12, 25, "2025-06-14 20:10"),
        SaveGame(4, 3, 8, "2025-06-14 18:20"),
        SaveGame(5, 15, 32, "2025-06-13 16:55")
    )
}

fun loadSavedGames(storage: PlatformStorage, savedGames: MutableList<SaveGame>) {
    savedGames.clear()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    for (i in 1..10) {
        val fileName = "save_$i.txt"
        if (storage.fileExists(fileName)) {
            val content = storage.readFile(fileName)
            val lines = content.split("\n")
            var level = 0
            var moves = 0
            var timestamp = System.currentTimeMillis()
            
            for (line in lines) {
                if (line.startsWith("LEVEL:")) {
                    level = line.substringAfter("LEVEL:").substringBefore(";").toIntOrNull() ?: 0
                } else if (line.startsWith("MOVES:")) {
                    moves = line.substringAfter("MOVES:").substringBefore(";").toIntOrNull() ?: 0
                } else if (line.startsWith("TIMESTAMP:")) {
                    timestamp = line.substringAfter("TIMESTAMP:").substringBefore(";").toLongOrNull() ?: System.currentTimeMillis()
                }
            }
            
            val date = dateFormat.format(Date(timestamp))
            savedGames.add(SaveGame(i, level, moves, date))
        }
    }
}

fun loadBoardFromStorage(storage: PlatformStorage, saveId: Int): Board? {
    val fileName = "save_$saveId.txt"
    if (!storage.fileExists(fileName)) return null
    
    val content = storage.readFile(fileName)
    return deserializeBoard(content)
}

fun saveBoardToStorage(storage: PlatformStorage, board: Board, saveId: Int, level: Int, moves: Int) {
    val fileName = "save_$saveId.txt"
    val boardData = serializeBoard(board)
    val timestamp = System.currentTimeMillis()
    
    val saveData = StringBuilder()
    saveData.append("LEVEL:$level;")
    saveData.append("MOVES:$moves;")
    saveData.append("TIMESTAMP:$timestamp;")
    saveData.append("\n")
    saveData.append(boardData)
    
    storage.writeFile(fileName, saveData.toString())
}
