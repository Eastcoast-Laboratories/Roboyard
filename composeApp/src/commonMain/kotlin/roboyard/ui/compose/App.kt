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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import roboyard.logic.core.LevelLoader
import roboyard.logic.core.Preferences
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.storage.getPlatformStorage
import roboyard.ui.graphics.MinimapGenerator
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import driftingdroids.model.Board
import roboyard.logic.core.GameLogic
import roboyard.logic.core.MapGenerator
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var board by remember { mutableStateOf<Board?>(null) }
    var selectedLevelId by remember { mutableStateOf(1) }
    var isLevelGame by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.MainMenu -> MainMenuScreen(
                    onNewRandomGame = {
                        // Use MapGenerator to generate random game map (same as fragment-app)
                        val mapGenerator = MapGenerator()
                        mapGenerator.robotCount = Preferences.robotCount
                        mapGenerator.targetColors = Preferences.targetColors
                        val gridElements = mapGenerator.generatedGameMap
                        board = if (gridElements != null) {
                            gridElementsToBoard(gridElements)
                        } else {
                            // Fallback to standard random board if MapGenerator fails
                            Board.createBoardRandom(4)
                        }
                        isLevelGame = false
                        currentScreen = Screen.Game
                    },
                    onLevelSelection = {
                        currentScreen = Screen.LevelSelection
                    },
                    onSettings = {
                        currentScreen = Screen.Settings
                    },
                    onHelp = {
                        currentScreen = Screen.Help
                    },
                    onCredits = {
                        currentScreen = Screen.Credits
                    },
                    onSaveLoad = {
                        currentScreen = Screen.SaveLoad
                    },
                    onAchievements = {
                        currentScreen = Screen.Achievements
                    }
                )
                Screen.Game -> {
                    board?.let { currentBoard ->
                        GameScreen(
                            board = currentBoard,
                            isLevelGame = isLevelGame,
                            levelId = selectedLevelId,
                            onBack = {
                                currentScreen = Screen.MainMenu
                                board = null
                            },
                            onNewGame = {
                                // Use MapGenerator to generate random game map (same as fragment-app)
                                val mapGenerator = MapGenerator()
                                mapGenerator.robotCount = Preferences.robotCount
                                mapGenerator.targetColors = Preferences.targetColors
                                val gridElements = mapGenerator.generatedGameMap
                                board = if (gridElements != null) {
                                    gridElementsToBoard(gridElements)
                                } else {
                                    // Fallback to standard random board if MapGenerator fails
                                    Board.createBoardRandom(4)
                                }
                            },
                            onSaveLoad = {
                                currentScreen = Screen.SaveLoad
                            }
                        )
                    }
                }
                Screen.LevelSelection -> {
                    LevelSelectionScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onLevelSelected = { levelId: Int ->
                            selectedLevelId = levelId
                            currentScreen = Screen.Loading
                        }
                    )
                }
                Screen.Loading -> {
                    LoadingScreen(
                        levelId = selectedLevelId,
                        onLoadComplete = { loadedBoard: Board ->
                            board = loadedBoard
                            isLevelGame = true
                            currentScreen = Screen.Game
                        },
                        onBack = {
                            currentScreen = Screen.MainMenu
                        }
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        }
                    )
                }
                Screen.Help -> {
                    HelpScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        }
                    )
                }
                Screen.Credits -> {
                    CreditsScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        }
                    )
                }
                Screen.SaveLoad -> {
                    SaveLoadScreen(
                        boardToSave = board,
                        isLevelGame = isLevelGame,
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onLoadGame = { loadedBoard ->
                            board = loadedBoard
                            isLevelGame = false // Default to false for loaded games
                            currentScreen = Screen.Game
                        }
                    )
                }
                Screen.Achievements -> {
                    AchievementsScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        }
                    )
                }
            }
        }
    }
}

enum class Screen {
    MainMenu,
    Game,
    LevelSelection,
    Loading,
    Settings,
    Help,
    Credits,
    SaveLoad,
    Achievements
}

@Composable
fun MainMenuScreen(
    onNewRandomGame: () -> Unit = {},
    onLevelSelection: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHelp: () -> Unit = {},
    onCredits: () -> Unit = {},
    onSaveLoad: () -> Unit = {},
    onAchievements: () -> Unit = {}
) {
    var hasSavedGames by remember { mutableStateOf(false) }

    // Check if there are saved games (same logic as main game)
    LaunchedEffect(Unit) {
        val storage = getPlatformStorage()
        hasSavedGames = storage.hasSavedGames()
    }

    val barBrush = Brush.linearGradient(
        colors = listOf(Color(0xCC000000), Color(0xCC000000)),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image (same as main game)
        Image(
            painter = painterResource(Res.drawable.title_bg_optimized),
            contentDescription = "Background image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header bar with title and profile button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBrush)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROBOYARD",
                        color = Color.White,
                        fontSize = 39.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.TURQUOISE,
                        onClick = { },
                        modifier = Modifier.size(48.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
            }

            // Scrollable content with fancy buttons
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Button container with 70% width
                Column(
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    FancyButton(
                        text = "New Random Game",
                        color = FancyButtonColor.GREEN,
                        onClick = onNewRandomGame,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FancyButton(
                        text = "Level Game",
                        color = FancyButtonColor.BLUE,
                        onClick = onLevelSelection,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Load Game button - always visible (same as main game)
                    FancyButton(
                        text = "Load Game",
                        color = FancyButtonColor.RED,
                        onClick = onSaveLoad,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Footer bar with icon buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Credits button - X symbol
                    CircularButton(
                        text = "©",
                        color = CircularButtonColor.YELLOW,
                        onClick = onCredits,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Help button
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.ORANGE,
                        onClick = onHelp,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Achievements button
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.PURPLE,
                        onClick = onAchievements,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Settings button
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.GRAY,
                        onClick = onSettings,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics {
                contentDescription = text
            }
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

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

@Composable
fun HelpScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "How to Play",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Goal section
            Text(
                text = "Goal",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Move the colored robot to its matching target.",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Movement section
            Text(
                text = "Movement",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Robots slide until they hit a wall or another robot.\n\nTap a robot to select it, then swipe in the direction you want it to move.",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Controls section
            Text(
                text = "Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Tap a robot to select it.\n\nSwipe in any direction to move the selected robot.\n\nUse the Menu button to access settings.\n\nUse the Reset button to restart the current level.",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Tips section
            Text(
                text = "Tips",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Plan your moves carefully.\n\nUse other robots as barriers.\n\nTry to solve each level in the minimum number of moves.",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        FancyButton(
            text = "BACK",
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun CreditsScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Credits",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Version section
            Text(
                text = "Version",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "v1.9",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Based on section
            Text(
                text = "Based on",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Ricochet Robots®",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Links section
            Text(
                text = "Links",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Imprint",
                color = Color(0xFF0000FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Open Source",
                color = Color(0xFF0000FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Contact",
                color = Color(0xFF0000FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        FancyButton(
            text = "BACK",
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Serialize a Board to Main Game format for save/load compatibility
 */
private fun serializeBoardToMainGameFormat(board: Board, isLevelGame: Boolean): String {
    val sb = StringBuilder()
    
    // Generate map signature for unique map tracking
    val mapSig = generateMapSignature(board)
    
    // Generate the metadata section with additional tags
    sb.append("#MAPNAME:Random")
        .append(";TIME:0")
        .append(";MOVES:0")
        .append(";DIFFICULTY:1") // Default difficulty
        .append(";SIZE:").append(board.width).append(",").append(board.height)
        .append(";SOLVED:false")
        .append(";MAX_HINT_USED:-1") // No hints used by default
        .append(";MAP_SIG:").append(mapSig)
        .append("\n")
    
    // Add board dimensions
    sb.append("WIDTH:").append(board.width).append(";\n")
    sb.append("HEIGHT:").append(board.height).append(";\n")
    
    // Generate the board representation (walls excluded - they go in WALLS section)
    for (y in 0 until board.height) {
        for (x in 0 until board.width) {
            if (x > 0) {
                sb.append(",")
            }
            
            val position = y * board.width + x
            
            // Check if this position has a robot
            val hasRobot = board.robotPositions.contains(position)
            
            // Check if this position has a target
            val goal = board.goals.find { it.position == position }
            
            when {
                hasRobot -> sb.append(4) // TYPE_ROBOT
                goal != null -> {
                    // Target with color
                    val color = goal.robotNumber
                    sb.append(3).append(":").append(color) // TYPE_TARGET with color
                }
                else -> sb.append(0) // TYPE_EMPTY
            }
        }
        sb.append("\n")
    }
    
    // Save targets in compact format: tcolorX,Y; (e.g., tb8,7;)
    for (goal in board.goals) {
        val x = goal.position % board.width
        val y = goal.position / board.width
        val color = goal.robotNumber
        val colorChar = when (color) {
            0 -> 'b' // blue
            1 -> 'g' // green
            2 -> 'r' // red (pink)
            3 -> 'y' // yellow
            4 -> 's' // silver
            else -> 'm' // multi
        }
        sb.append("t").append(colorChar).append(x).append(",").append(y).append(";")
    }
    
    sb.append("\n")
    
    // Save walls in compact format: hX,Y; and vX,Y;
    // Horizontal walls (y goes to height to include bottom boundary)
    for (y in 0..board.height) {
        for (x in 0 until board.width) {
            val position = y * board.width + x
            if (board.isWall(position, 0)) { // NORTH wall = horizontal
                sb.append("h").append(x).append(",").append(y).append(";")
            }
        }
    }
    // Vertical walls (x goes to width to include right boundary)
    for (y in 0 until board.height) {
        for (x in 0..board.width) {
            val position = y * board.width + x
            if (board.isWall(position, 3)) { // WEST wall = vertical
                sb.append("v").append(x).append(",").append(y).append(";")
            }
        }
    }
    
    sb.append("\n")
    
    // Save robots in compact format: rcolorX,Y; (e.g., rr1,5;)
    for (i in board.robotPositions.indices) {
        val position = board.robotPositions[i]
        val x = position % board.width
        val y = position / board.width
        val colorChar = when (i) {
            0 -> 'b' // blue
            1 -> 'g' // green
            2 -> 'r' // red (pink)
            3 -> 'y' // yellow
            4 -> 's' // silver
            else -> 'm' // multi
        }
        sb.append("r").append(colorChar).append(x).append(",").append(y).append(";")
    }
    
    return sb.toString()
}

/**
 * Generate a unique map signature for history lookup
 */
private fun generateMapSignature(board: Board): String {
    val sb = StringBuilder()
    sb.append(board.width).append("x").append(board.height)
    sb.append(":")
    for (robotPos in board.robotPositions) {
        sb.append(robotPos).append(",")
    }
    sb.append(":")
    for (goal in board.goals) {
        sb.append(goal.position).append(",").append(goal.robotNumber).append(";")
    }
    return sb.toString()
}

/**
 * Deserialize a Board from Main Game format
 */
private fun deserializeBoardFromMainGameFormat(saveData: String): Board? {
    val lines = saveData.lines()
    var width = 0
    var height = 0
    var boardData = mutableListOf<String>()
    var targetsData = ""
    var wallsData = ""
    var robotsData = ""
    
    for (line in lines) {
        when {
            line.startsWith("WIDTH:") -> width = line.substringAfter("WIDTH:").substringBefore(";").toInt()
            line.startsWith("HEIGHT:") -> height = line.substringAfter("HEIGHT:").substringBefore(";").toInt()
            line.startsWith("#") -> { /* Skip metadata */ }
            line.startsWith("t") -> targetsData += line
            line.startsWith("h") || line.startsWith("v") -> wallsData += line
            line.startsWith("r") -> robotsData += line
            else -> boardData.add(line)
        }
    }
    
    if (width == 0 || height == 0) {
        println("[DESERIALIZE] Invalid dimensions: width=$width, height=$height")
        return null
    }
    
    // Parse robots from ROBOTS section (preferred) or from board data (fallback)
    val robotPositions = mutableListOf<Int>()
    val robotColors = mutableListOf<Int>()
    
    if (robotsData.isNotEmpty()) {
        // Parse robots from ROBOTS section: rcolorX,Y;
        val robotPattern = Regex("r([a-z])(\\d+),(\\d+);")
        robotPattern.findAll(robotsData).forEach { match ->
            val colorChar = match.groupValues[1][0]
            val rx = match.groupValues[2].toInt()
            val ry = match.groupValues[3].toInt()
            val robotNumber = when (colorChar) {
                'b' -> 0
                'g' -> 1
                'r' -> 2
                'y' -> 3
                's' -> 4
                else -> 0
            }
            robotPositions.add(ry * width + rx)
            robotColors.add(robotNumber)
        }
    } else {
        // Fallback: parse robots from board data
        for (y in 0 until height) {
            if (y >= boardData.size) break
            val row = boardData[y].split(",")
            for (x in 0 until width) {
                if (x >= row.size) break
                val cell = row[x]
                when {
                    cell.startsWith("4") -> { // TYPE_ROBOT
                        robotPositions.add(y * width + x)
                        robotColors.add(0) // Default color
                    }
                }
            }
        }
    }
    
    // Parse targets
    val goalData = mutableListOf<Triple<Int, Int, Int>>() // x, y, robotNumber
    val targetPattern = Regex("t([a-z])(\\d+),(\\d+);")
    targetPattern.findAll(targetsData).forEach { match ->
        val colorChar = match.groupValues[1][0]
        val tx = match.groupValues[2].toInt()
        val ty = match.groupValues[3].toInt()
        val robotNumber = when (colorChar) {
            'b' -> 0
            'g' -> 1
            'r' -> 2
            'y' -> 3
            's' -> 4
            else -> 0
        }
        goalData.add(Triple(tx, ty, robotNumber))
    }
    
    if (robotPositions.isEmpty()) {
        println("[DESERIALIZE] No robots found in save data")
        return null
    }
    
    // Create board
    val newBoard = Board.createBoardFreestyle(null, width, height, robotPositions.size)
    if (newBoard == null) {
        println("[DESERIALIZE] Failed to create board")
        return null
    }
    
    // Set robot positions
    for (i in robotPositions.indices) {
        newBoard.robotPositions[i] = robotPositions[i]
    }
    
    // Set goals
    for ((tx, ty, robotNumber) in goalData) {
        newBoard.addGoal(ty * width + tx, robotNumber, robotNumber)
    }
    newBoard.setGoalRandom()
    
    // Parse and set walls
    val hWallPattern = Regex("h(\\d+),(\\d+);")
    val vWallPattern = Regex("v(\\d+),(\\d+);")
    
    hWallPattern.findAll(wallsData).forEach { match ->
        val wx = match.groupValues[1].toInt()
        val wy = match.groupValues[2].toInt()
        newBoard.setWall(wx, wy, 0, true) // Set NORTH wall
    }
    
    vWallPattern.findAll(wallsData).forEach { match ->
        val wx = match.groupValues[1].toInt()
        val wy = match.groupValues[2].toInt()
        newBoard.setWall(wx, wy, 3, true) // Set WEST wall
    }
    
    println("[DESERIALIZE] Board created: ${newBoard.width}x${newBoard.height}, robots: ${newBoard.robotPositions.joinToString(",")}, goals: ${newBoard.goals.size}")
    
    return newBoard
}

/**
 * Save a board to history using Main Game format
 */
private fun saveToHistory(board: Board, storage: PlatformStorage): Boolean {
    try {
        // Get next available history index
        val historyIndex = getNextHistoryIndex(storage)
        val historyFileName = "history_$historyIndex.txt"
        
        // Serialize board to Main Game format
        val saveData = serializeBoardToMainGameFormat(board, false)
        
        // Write to history file
        val result = storage.writeFile(historyFileName, saveData)
        
        if (result) {
            println("[HISTORY] Saved to history: $historyFileName")
        }
        
        return result
    } catch (e: Exception) {
        println("[HISTORY] Error saving to history: ${e.message}")
        return false
    }
}

/**
 * Get the next available history index
 */
private fun getNextHistoryIndex(storage: PlatformStorage): Int {
    var index = 1
    while (storage.fileExists("history_$index.txt")) {
        index++
    }
    return index
}

/**
 * Get all history entries
 */
private fun getHistoryEntries(storage: PlatformStorage): List<Pair<Int, String>> {
    val entries = mutableListOf<Pair<Int, String>>()
    var index = 1
    while (storage.fileExists("history_$index.txt")) {
        entries.add(Pair(index, "history_$index.txt"))
        index++
    }
    return entries
}

/**
 * Validate that save file contains targets
 */
private fun validateSaveContainsTargets(saveData: String, fileName: String): Boolean {
    val hasTargets = saveData.contains("t") || saveData.contains("3:")
    if (!hasTargets) {
        println("[SAVE_VERIFICATION] Save file $fileName does not contain targets")
    }
    return hasTargets
}

@Composable
fun HistoryItem(
    historyIndex: Int,
    fileName: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "History #$historyIndex",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = fileName,
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SaveLoadScreen(
    boardToSave: Board? = null,
    isLevelGame: Boolean = false,
    onBack: () -> Unit = {},
    onLoadGame: (Board) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Save", "Load", "History")
    
    // Check for saved games
    val storage = remember { getPlatformStorage() }
    var hasSavedGames by remember { mutableStateOf(false) }
    var slotStates by remember { mutableStateOf(List(10) { false }) }
    var historyEntries by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    
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
                    modifier = Modifier.weight(1f)
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
                    historyEntries.forEach { (index, fileName) ->
                        HistoryItem(
                            historyIndex = index,
                            fileName = fileName,
                            onClick = {
                                // Load history entry
                                println("[SAVE_LOAD_SCREEN] Loading history entry: $fileName")
                                val saveData = storage.readFile(fileName)
                                val loadedBoard = deserializeBoardFromMainGameFormat(saveData)
                                if (loadedBoard != null) {
                                    onLoadGame(loadedBoard)
                                }
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
                                
                                // Serialize board to Main Game format
                                val saveData = serializeBoardToMainGameFormat(boardToSave, isLevelGame)
                                
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
                FancyButton(
                    text = "Wall",
                    color = if (selectedTool == "Wall") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTool = "Wall" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "Eraser",
                    color = if (selectedTool == "Eraser") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTool = "Eraser" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Tool Selection
            Text(
                text = "Target Tool",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FancyButton(
                    text = "None",
                    color = if (selectedTarget == "None") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "None" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "R",
                    color = if (selectedTarget == "R") FancyButtonColor.RED else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "R" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "G",
                    color = if (selectedTarget == "G") FancyButtonColor.GREEN else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "G" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "B",
                    color = if (selectedTarget == "B") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "B" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "Y",
                    color = if (selectedTarget == "Y") FancyButtonColor.YELLOW else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "Y" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "S",
                    color = if (selectedTarget == "S") FancyButtonColor.GRAY else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "S" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "M",
                    color = if (selectedTarget == "M") FancyButtonColor.PURPLE else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "M" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Board Preview
            Text(
                text = "Board Preview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Board preview will appear here",
                    color = Color(0xFF888888),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Board Size Configuration
            Text(
                text = "Board Size",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
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
                color = if (unlocked) Color(0xFF2E7D32) else Color(0xFF757575),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color(0xFF616161),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = progress,
                color = Color(0xFF9E9E9E),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
