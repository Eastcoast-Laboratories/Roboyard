package roboyard.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import driftingdroids.model.Board
import roboyard.logic.core.MapGenerator
import roboyard.logic.core.Preferences
@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var board by remember { mutableStateOf<Board?>(null) }
    var startBoardForSaveLoad by remember { mutableStateOf<Board?>(null) }
    var selectedLevelId by remember { mutableStateOf(1) }
    var isLevelGame by remember { mutableStateOf(false) }
    var isLoadedGame by remember { mutableStateOf(false) }

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
                        isLoadedGame = false // Not a loaded game
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
                            isLoadedGame = isLoadedGame,
                            levelId = selectedLevelId,
                            onBack = {
                                currentScreen = Screen.MainMenu
                                board = null
                                isLoadedGame = false
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
                                isLoadedGame = false // Not a loaded game
                            },
                            onSaveLoad = { currentBoardParam, startBoardParam ->
                                board = currentBoardParam
                                startBoardForSaveLoad = startBoardParam
                                currentScreen = Screen.SaveLoad
                            },
                            onNextLevel = {
                                // Load next level
                                selectedLevelId++
                                currentScreen = Screen.Loading
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
                        startBoard = startBoardForSaveLoad,
                        isLevelGame = isLevelGame,
                        onBack = {
                            currentScreen = Screen.MainMenu
                            startBoardForSaveLoad = null
                        },
                        onLoadGame = { loadedBoard ->
                            board = loadedBoard
                            startBoardForSaveLoad = loadedBoard // Loaded board contains start positions
                            isLevelGame = false // Default to false for loaded games
                            isLoadedGame = true // Mark as loaded game
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
