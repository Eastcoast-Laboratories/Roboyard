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
@Composable
fun LevelSelectionScreen(
    onBack: () -> Unit = {},
    onLevelSelected: (Int) -> Unit = {}
) {
    val totalLevels = 140
    val levels = (1..totalLevels).toList()
    val levelCompletionManager = remember { roboyard.logic.managers.LevelCompletionManager.getInstance() }
    val totalStars = remember { levelCompletionManager.totalStars }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background color (placeholder for bg_level_screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4CAF50))
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with title and profile button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Level Selection",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x80000000),
                            offset = Offset(1f, 1f),
                            blurRadius = 3f
                        )
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                CircularButton(
                    text = null,
                    color = CircularButtonColor.TURQUOISE,
                    onClick = { },
                    modifier = Modifier.size(40.dp)
                )
            }

            // Progress bar section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalStars",
                    color = Color(0xFFFFD700),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x80000000),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Star icon placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFFD700), RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .background(Color(0xFF4A90E2), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0f)
                            .fillMaxHeight()
                            .background(Color(0xFFFFC107), RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "0 / $totalLevels",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Level grid (3 columns in portrait, 6 in landscape)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(levels) { levelId ->
                    val levelData = remember { levelCompletionManager.getLevelCompletionData(levelId) }
                    val isUnlocked = levelId >= roboyard.logic.core.Constants.CUSTOM_LEVEL_START_ID || 
                            (roboyard.logic.core.Constants.STARS_PER_LEVEL * (levelId - 1) <= totalStars)
                    LevelItem(
                        levelId = levelId,
                        stars = levelData?.getCompletionStars() ?: 0,
                        isUnlocked = isUnlocked,
                        onClick = { if (isUnlocked) onLevelSelected(levelId) }
                    )
                }
            }
        }
    }
}
@Composable
fun LevelItem(
    levelId: Int,
    stars: Int = 0,
    isUnlocked: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor = if (isUnlocked) Color(0xFF2C2C2C) else Color(0xFF1A1A1A)
    val textColor = if (isUnlocked) Color.White else Color.Gray
    
    // Load level board for minimap generation
    var board by remember { mutableStateOf<Board?>(null) }
    
    LaunchedEffect(levelId) {
        if (isUnlocked && levelId < 141) {
            try {
                board = LevelLoader.loadLevel(levelId)
            } catch (e: Exception) {
                // Failed to load level
            }
        }
    }
    
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(enabled = isUnlocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (board != null && isUnlocked) {
                // Show minimap
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(4.dp)
                ) {
                    MinimapGenerator.drawMinimap(this, board, size.width, size.height)
                }
            } else if (!isUnlocked) {
                Text(
                    text = "🔒",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = levelId.toString(),
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (stars > 0 && isUnlocked) {
                Row {
                    repeat(stars) {
                        Text(
                            text = "★",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
