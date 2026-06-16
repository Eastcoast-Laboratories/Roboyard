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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class Level(val id: Int, val name: String, val difficulty: String, val completed: Boolean = false)

@Composable
fun LevelSelectionScreen(
    onBack: () -> Unit = {},
    onLevelSelected: (Int) -> Unit = {}
) {
    val levels = generateLevels()

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
                    text = "Select Level",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(80.dp)) // Balance layout
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(levels) { level ->
                    LevelCard(
                        level = level,
                        onClick = { onLevelSelected(level.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelCard(
    level: Level,
    onClick: () -> Unit
) {
    val difficultyColor = when (level.difficulty) {
        "Beginner" -> Color(0xFF4CAF50)
        "Intermediate" -> Color(0xFF2196F3)
        "Advanced" -> Color(0xFFFF9800)
        "Expert" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (level.completed) Color(0xFFE8F5E9) else Color(0xFFFFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${level.id}",
                style = MaterialTheme.typography.headlineSmall,
                color = difficultyColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = level.difficulty,
                style = MaterialTheme.typography.labelSmall,
                color = difficultyColor
            )
            if (level.completed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

fun generateLevels(): List<Level> {
    val levels = mutableListOf<Level>()
    
    // Beginner levels (1-35)
    for (i in 1..35) {
        levels.add(Level(i, "Level $i", "Beginner", completed = i <= 5))
    }
    
    // Intermediate levels (36-70)
    for (i in 36..70) {
        levels.add(Level(i, "Level $i", "Intermediate", completed = false))
    }
    
    // Advanced levels (71-105)
    for (i in 71..105) {
        levels.add(Level(i, "Level $i", "Advanced", completed = false))
    }
    
    // Expert levels (106-140)
    for (i in 106..140) {
        levels.add(Level(i, "Level $i", "Expert", completed = false))
    }
    
    return levels
}
