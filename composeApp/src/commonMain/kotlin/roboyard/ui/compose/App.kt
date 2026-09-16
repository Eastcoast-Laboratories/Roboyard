package roboyard.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import roboyard.logic.managers.GameSession
import roboyard.logic.storage.getPlatformStorage

@Composable
fun App(onFullscreenChanged: (Boolean) -> Unit = {}) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var selectedLevelId by remember { mutableStateOf(1) }
    var isLevelGame by remember { mutableStateOf(false) }
    var isLoadedGame by remember { mutableStateOf(false) }

    // Single shared game session for the whole app (mirrors Android GameStateManager)
    val appScope = rememberCoroutineScope()
    val session = remember { GameSession(getPlatformStorage(), appScope) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.MainMenu -> MainMenuScreen(
                    onNewRandomGame = {
                        session.startNewGame()
                        isLevelGame = false
                        isLoadedGame = false
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
                    GameScreen(
                        session = session,
                        isLevelGame = isLevelGame,
                        isLoadedGame = isLoadedGame,
                        levelId = selectedLevelId,
                        onBack = {
                            session.stopRegeneration()
                            currentScreen = Screen.MainMenu
                            isLoadedGame = false
                        },
                        onNewGame = {
                            session.startNewGame()
                            isLevelGame = false
                            isLoadedGame = false
                        },
                        onSaveLoad = {
                            currentScreen = Screen.SaveLoad
                        },
                        onNextLevel = {
                            selectedLevelId++
                            session.startLevelGame(selectedLevelId)
                            isLevelGame = true
                            isLoadedGame = false
                        }
                    )
                }
                Screen.LevelSelection -> {
                    LevelSelectionScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onLevelSelected = { levelId: Int ->
                            selectedLevelId = levelId
                            session.startLevelGame(levelId)
                            isLevelGame = true
                            isLoadedGame = false
                            currentScreen = Screen.Game
                        }
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onFullscreenChanged = onFullscreenChanged
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
                        session = session,
                        isLevelGame = isLevelGame,
                        onBack = {
                            // Return to the running game if one is active, else main menu
                            currentScreen = if (session.currentState.value != null) {
                                Screen.Game
                            } else {
                                Screen.MainMenu
                            }
                        },
                        onLoadGame = { slotId ->
                            session.loadGame(slotId)
                            isLevelGame = false
                            isLoadedGame = true
                            currentScreen = Screen.Game
                        },
                        onLoadHistory = { path ->
                            session.loadHistoryEntry(path)
                            isLevelGame = false
                            isLoadedGame = true
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
    Settings,
    Help,
    Credits,
    SaveLoad,
    Achievements
}
