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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Width:",
                    color = Color(0xFFCCCCCC),
                    fontSize = 16.sp
                )
                Text(
                    text = boardWidth,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Height:",
                    color = Color(0xFFCCCCCC),
                    fontSize = 16.sp
                )
                Text(
                    text = boardHeight,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export/Import Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FancyButton(
                    text = "Export Level",
                    color = FancyButtonColor.RED,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "Import ASCII",
                    color = FancyButtonColor.BLUE,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Play Map Button
            FancyButton(
                text = "Play Map",
                color = FancyButtonColor.GREEN,
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cancel Button
        FancyButton(
            text = "Cancel",
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AchievementsScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top row with back button, title, and profile button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FancyButton(
                text = "BACK",
                color = FancyButtonColor.GRAY,
                onClick = onBack,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = "Achievements",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            CircularButton(
                text = null,
                color = CircularButtonColor.TURQUOISE,
                onClick = { },
                modifier = Modifier.size(48.dp)
            )
        }

        // Progress text
        Text(
            text = "0 / 0 Unlocked",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        // Scrollable achievements list
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            repeat(5) { index ->
                AchievementItem(
                    title = "Achievement $index",
                    description = "Description for achievement $index",
                    unlocked = false,
                    progress = "0 / 10"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun LoadingScreen(
    levelId: Int,
    onLoadComplete: (Board) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(levelId) {
        isLoading = true
        errorMessage = null
        try {
            // Load level using LevelLoader (same as fragment-app GameState.loadLevel)
            val board: Board? = withContext(Dispatchers.IO) {
                LevelLoader.loadLevel(levelId)
            }
            if (board != null) {
                onLoadComplete(board)
            } else {
                errorMessage = "Level not found"
            }
        } catch (e: Exception) {
            errorMessage = "Error loading level: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Text(
                    text = "Loading Level $levelId...",
                    color = Color.White,
                    fontSize = 24.sp
                )
            } else if (errorMessage != null) {
                val errorMsg = errorMessage ?: "Unknown error"
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
fun AchievementItem(
    title: String,
    description: String,
    unlocked: Boolean,
    progress: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (unlocked) Color(0xFFE8F5E9) else Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, if (unlocked) Color(0xFF4CAF50) else Color(0xFFE0E0E0)), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
