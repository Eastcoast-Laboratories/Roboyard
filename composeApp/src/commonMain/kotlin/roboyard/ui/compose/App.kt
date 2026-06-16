package roboyard.ui.compose

import androidx.compose.foundation.BorderStroke
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
import driftingdroids.model.Board
import roboyard.logic.core.LevelLoader
import roboyard.logic.core.GameLogic
import roboyard.logic.core.Preferences

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var board by remember { mutableStateOf<Board?>(null) }
    var selectedLevelId by remember { mutableStateOf(1) }

    // Initialize GameLogic for random game generation
    val gameLogic = remember {
        GameLogic(
            Preferences.boardSizeWidth,
            Preferences.boardSizeHeight,
            Preferences.difficulty
        )
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.MainMenu -> MainMenuScreen(
                    onNewRandomGame = {
                        // Use GameLogic to generate random game map
                        val gridElements = gameLogic.generateGameMap(null)
                        board = if (gridElements != null) {
                            gridElementsToBoard(gridElements)
                        } else {
                            // Fallback to standard random board if GameLogic fails
                            Board.createBoardRandom(4)
                        }
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
                            },
                            onNewGame = {
                                // Use GameLogic to generate random game map
                                val gridElements = gameLogic.generateGameMap(null)
                                board = if (gridElements != null) {
                                    gridElementsToBoard(gridElements)
                                } else {
                                    // Fallback to standard random board if GameLogic fails
                                    Board.createBoardRandom(4)
                                }
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

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    var soundEnabled by remember { mutableStateOf(true) }
    var fullscreenEnabled by remember { mutableStateOf(false) }
    var highContrastEnabled by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Scrollable settings content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Sound Section
            Text(
                text = "Sound",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                RadioButtonWithLabel(
                    text = "Yes",
                    selected = soundEnabled,
                    onClick = { soundEnabled = true }
                )
                RadioButtonWithLabel(
                    text = "No",
                    selected = !soundEnabled,
                    onClick = { soundEnabled = false }
                )
            }

            // Fullscreen Section
            Text(
                text = "Fullscreen",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                RadioButtonWithLabel(
                    text = "Yes",
                    selected = fullscreenEnabled,
                    onClick = { fullscreenEnabled = true }
                )
                RadioButtonWithLabel(
                    text = "No",
                    selected = !fullscreenEnabled,
                    onClick = { fullscreenEnabled = false }
                )
            }

            // High Contrast Mode Section
            Text(
                text = "High Contrast Mode",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                RadioButtonWithLabel(
                    text = "Yes",
                    selected = highContrastEnabled,
                    onClick = { highContrastEnabled = true }
                )
                RadioButtonWithLabel(
                    text = "No",
                    selected = !highContrastEnabled,
                    onClick = { highContrastEnabled = false }
                )
            }

            // Accessibility Mode Section (secret: black on black)
            Text(
                text = "Accessibility Mode:",
                color = Color(0xFF333333),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                RadioButtonWithLabel(
                    text = "On",
                    selected = accessibilityEnabled,
                    onClick = { accessibilityEnabled = true },
                    textColor = Color(0xFF333333)
                )
                RadioButtonWithLabel(
                    text = "Off",
                    selected = !accessibilityEnabled,
                    onClick = { accessibilityEnabled = false },
                    textColor = Color(0xFF333333)
                )
            }

            // Data Export/Import Section
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF444444)
                    )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Data",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Export or import all your game data including preferences, achievements, level progress, and save games.",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            FancyButton(
                text = "Export Data",
                color = FancyButtonColor.BLUE,
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            FancyButton(
                text = "Import Data",
                color = FancyButtonColor.BLUE,
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            FancyButton(
                text = "Reset Data",
                color = FancyButtonColor.RED,
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            FancyButton(
                text = "View Logs",
                color = FancyButtonColor.GRAY,
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Back button
        FancyButton(
            text = "Back",
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun RadioButtonWithLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    textColor: Color = Color.White
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White
            )
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp
        )
    }
}

@Composable
fun HelpScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Scrollable help content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Card header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE9ECEF), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .border(BorderStroke(1.dp, Color(0xFFDEE2E6)), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "How to Play",
                    color = Color(0xFF212529),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Card body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(BorderStroke(1.dp, Color(0xFFDEE2E6)), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Roboyard is a puzzle game where you guide robots to their matching targets.\n\n" +
                               "Drag a robot in any direction to slide it until it hits a wall or another robot.\n\n" +
                               "Goal: Match each robot to its target of the same color.\n\n" +
                               "Try to solve each level in the minimum number of moves!",
                        color = Color(0xFF212529),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Back button
        FancyButton(
            text = "← Back",
            color = FancyButtonColor.BLUE,
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
            .background(Color(0xFFF8FAFC))
    ) {
        // Scrollable credits content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Card header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE9ECEF), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .border(BorderStroke(1.dp, Color(0xFFDEE2E6)), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Credits",
                    color = Color(0xFF212529),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Card body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(BorderStroke(1.dp, Color(0xFFDEE2E6)), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // Based on Section
                    Text(
                        text = "Based on",
                        color = Color(0xFF212529),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ricochet Robots®",
                        color = Color(0xFF212529),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Imprint/privacy policy Section
                    Text(
                        text = "Imprint / Privacy Policy",
                        color = Color(0xFF212529),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "roboyard.z11.de/impressum",
                        color = Color(0xFF0D6EFD),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Open Source Section
                    Text(
                        text = "Open Source",
                        color = Color(0xFF212529),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "github.com/Eastcoast-Laboratories/Roboyard",
                        color = Color(0xFF0D6EFD),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Version: 1.0.0 (Build 1)",
                        color = Color(0xFF6C757D),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Contact Us Section
                    Text(
                        text = "Contact Us",
                        color = Color(0xFF212529),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "eclabs.de/#kontakt",
                        color = Color(0xFF0D6EFD),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Created by Section
                    Text(
                        text = "Created by",
                        color = Color(0xFF212529),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Alain Caillaud",
                        color = Color(0xFF212529),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Pierre Michel",
                        color = Color(0xFF212529),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = "Ruben Barkow-Kuder",
                        color = Color(0xFF212529),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Back button
        FancyButton(
            text = "← Back",
            color = FancyButtonColor.BLUE,
            onClick = onBack,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun SaveLoadScreen(
    onBack: () -> Unit = {},
    onLoadGame: (Board) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Save", "Load", "History")

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
            repeat(10) { slotIndex ->
                SaveSlotItem(
                    slotNumber = slotIndex + 1,
                    isEmpty = true,
                    onClick = { }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
            .background(Color.White)
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
                text = "Back",
                color = FancyButtonColor.BLUE,
                onClick = onBack,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = "Achievements",
                color = Color(0xFF333333),
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
            color = Color(0xFF666666),
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
