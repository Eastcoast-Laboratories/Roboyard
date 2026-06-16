package roboyard.ui.compose

import androidx.compose.foundation.background
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
import driftingdroids.model.Board
import roboyard.logic.core.LevelLoader

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var board by remember { mutableStateOf<Board?>(null) }
    var selectedLevelId by remember { mutableStateOf(1) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.MainMenu -> MainMenuScreen(
                    onNewRandomGame = {
                        board = Board.createBoardRandom(4)
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
                            onBack = {
                                currentScreen = Screen.MainMenu
                                board = null
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
                        onLoadComplete = { loadedBoard ->
                            board = loadedBoard
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
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onLoadGame = { loadedBoard ->
                            board = loadedBoard
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
    val barBrush = Brush.linearGradient(
        colors = listOf(Color(0xCC000000), Color(0xCC000000)),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header bar with title and profile button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barBrush)
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

        // Scrollable content with fancy buttons
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            FancyButton(
                text = "Play",
                color = FancyButtonColor.GREEN,
                onClick = onNewRandomGame,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            FancyButton(
                text = "Levels",
                color = FancyButtonColor.BLUE,
                onClick = onLevelSelection,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            FancyButton(
                text = "Load Game",
                color = FancyButtonColor.RED,
                onClick = onSaveLoad,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Footer bar with icon buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Black)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barBrush)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularButton(
                text = "©",
                color = CircularButtonColor.YELLOW,
                onClick = onCredits
            )
            CircularButton(
                text = null,
                color = CircularButtonColor.ORANGE,
                onClick = onHelp
            )
            CircularButton(
                text = null,
                color = CircularButtonColor.PURPLE,
                onClick = onAchievements
            )
            CircularButton(
                text = null,
                color = CircularButtonColor.GRAY,
                onClick = onSettings
            )
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
fun LoadingScreen(
    levelId: Int = 1,
    onLoadComplete: (Board) -> Unit = {},
    onBack: () -> Unit = {}
) {
    LaunchedEffect(levelId) {
        val board = LevelLoader.loadLevel(levelId)
        if (board != null) {
            onLoadComplete(board)
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Loading level $levelId...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun LevelSelectionScreen(
    onBack: () -> Unit = {},
    onLevelSelected: (Int) -> Unit = {}
) {
    val totalLevels = 140
    val levels = (1..totalLevels).toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header with title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Level Selection",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Progress bar section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0",
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
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFFD700), RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.width(8.dp))
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

        // Level grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(levels) { levelId ->
                LevelItem(
                    levelId = levelId,
                    onClick = { onLevelSelected(levelId) }
                )
            }
        }

        // Bottom button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 0.dp)
        ) {
            FancyButton(
                text = "BACK",
                color = FancyButtonColor.GRAY,
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LevelItem(
    levelId: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = levelId.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
