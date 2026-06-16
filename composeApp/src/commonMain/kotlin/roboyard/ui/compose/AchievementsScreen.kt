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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val unlocked: Boolean = false,
    val icon: String = "🏆"
)

@Composable
fun AchievementsScreen(
    onBack: () -> Unit = {}
) {
    val achievements = generateAchievements()

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
                    text = "Achievements",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            val unlockedCount = achievements.count { it.unlocked }
            Text(
                text = "Unlocked: $unlockedCount / ${achievements.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementCard(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) {
                Color(0xFFE8F5E9)
            } else {
                Color(0xFF2C2C2C)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = achievement.icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (achievement.unlocked) {
                        Color(0xFF1B5E20)
                    } else {
                        Color(0xFF757575)
                    }
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (achievement.unlocked) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color(0xFF757575)
                    }
                )
            }
            if (achievement.unlocked) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

fun generateAchievements(): List<Achievement> {
    return listOf(
        Achievement(
            id = "first_win",
            name = "First Victory",
            description = "Complete your first level",
            unlocked = true,
            icon = "🎯"
        ),
        Achievement(
            id = "speed_demon",
            name = "Speed Demon",
            description = "Complete a level in under 10 moves",
            unlocked = true,
            icon = "⚡"
        ),
        Achievement(
            id = "level_master",
            name = "Level Master",
            description = "Complete 50 levels",
            unlocked = false,
            icon = "🎖️"
        ),
        Achievement(
            id = "perfect_score",
            name = "Perfect Score",
            description = "Complete 10 levels with optimal moves",
            unlocked = false,
            icon = "💯"
        ),
        Achievement(
            id = "streak_10",
            name = "Hot Streak",
            description = "Complete 10 levels in a row",
            unlocked = false,
            icon = "🔥"
        ),
        Achievement(
            id = "expert_solver",
            name = "Expert Solver",
            description = "Complete an Expert difficulty level",
            unlocked = false,
            icon = "🧠"
        ),
        Achievement(
            id = "all_robots",
            name = "Robot Wrangler",
            description = "Use all 5 robots in one solution",
            unlocked = false,
            icon = "🤖"
        ),
        Achievement(
            id = "no_hints",
            name = "Independent",
            description = "Complete 20 levels without hints",
            unlocked = false,
            icon = "🚫"
        )
    )
}
