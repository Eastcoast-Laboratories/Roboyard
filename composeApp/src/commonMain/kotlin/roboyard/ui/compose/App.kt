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
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var board by remember { mutableStateOf<Board?>(null) }
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
                            onSaveLoad = {
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
                        isLevelGame = isLevelGame,
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onLoadGame = { loadedBoard ->
                            board = loadedBoard
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
