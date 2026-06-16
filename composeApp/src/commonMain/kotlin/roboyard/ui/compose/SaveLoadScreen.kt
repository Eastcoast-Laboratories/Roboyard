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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class SaveGame(val id: Int, val level: Int, val moves: Int, val date: String)

@Composable
fun SaveLoadScreen(
    onBack: () -> Unit = {},
    onLoadGame: (Int) -> Unit = {}
) {
    val savedGames = generateSavedGames()

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
                Button(onClick = onBack) {
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
                        onLoad = { onLoadGame(saveGame.id) }
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
