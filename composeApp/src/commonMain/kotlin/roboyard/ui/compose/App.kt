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
import androidx.compose.ui.unit.dp
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
                        onLevelSelected = { levelId ->
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
                        onLoadGame = { gameId ->
                            // TODO: Load specific game
                            board = Board.createBoardRandom(4)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Roboyard",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Ricochet Robots",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        MenuButton(text = "New Random Game", onClick = onNewRandomGame)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Level Selection", onClick = onLevelSelection)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Save / Load", onClick = onSaveLoad)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Achievements", onClick = onAchievements)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Settings", onClick = onSettings)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Help", onClick = onHelp)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Credits", onClick = onCredits)
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
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
