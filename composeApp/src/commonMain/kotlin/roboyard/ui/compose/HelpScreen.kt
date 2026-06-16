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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(
    onBack: () -> Unit = {}
) {
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
                    text = "Help",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HelpSection(
                    title = "How to Play",
                    content = """
                        Ricochet Robots is a puzzle game where you must guide a specific robot to a target position.
                        
                        1. Click and drag a robot in any of the four directions (up, down, left, right).
                        2. The robot will slide until it hits a wall or another robot.
                        3. You cannot stop the robot mid-slide.
                        4. Use the Hint button if you need help finding a solution.
                        5. Reach the goal with the target robot to complete the level.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(16.dp))

                HelpSection(
                    title = "Robot Colors",
                    content = """
                        Red, Green, Blue, Yellow robots are the main pieces.
                        The goal is marked with a hollow circle in the target robot's color.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(16.dp))

                HelpSection(
                    title = "Difficulty Levels",
                    content = """
                        Beginner: Easy puzzles, perfect for learning.
                        Intermediate: Moderate challenge.
                        Advanced: Harder puzzles requiring more planning.
                        Expert: Extremely challenging puzzles.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(16.dp))

                HelpSection(
                    title = "Tips",
                    content = """
                        - Plan your moves ahead - robots slide until they hit something.
                        - Use other robots as stopping points.
                        - Sometimes you need to move multiple robots to set up the final move.
                        - The solver can help you see the optimal solution.
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
fun HelpSection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
