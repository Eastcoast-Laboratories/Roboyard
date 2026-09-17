package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import roboyard.logic.managers.GameSession
import roboyard.logic.managers.SyncManager
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.platform.openAutoLoginUrl
import roboyard.logic.platform.openUrl
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider

@Composable
fun App(
    onFullscreenChanged: (Boolean) -> Unit = {},
    pendingDeepLink: String? = null
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var selectedLevelId by remember { mutableStateOf(1) }
    var isLevelGame by remember { mutableStateOf(false) }
    var isLoadedGame by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var authRefresh by remember { mutableStateOf(0) }
    var saveLoadMode by remember { mutableStateOf(false) } // true = save mode, false = load mode

    val storage = getPlatformStorage()
    val stringProvider = getStringProvider()

    // Single shared game session for the whole app (mirrors Android GameStateManager)
    val appScope = rememberCoroutineScope()
    val session = remember { GameSession(storage, appScope) }

    // UiNotifier (Android Toast parity): one Compose notifier shared by
    // GameSession and AchievementManager so messages surface as toasts
    val uiNotifier = remember { ComposeUiNotifier() }
    session.uiNotifier = uiNotifier
    LaunchedEffect(Unit) {
        roboyard.logic.achievements.AchievementManager.getInstance(storage, stringProvider)
            .setUiNotifier(uiNotifier)
    }

    // Android parity: profile button opens the web profile page when logged in,
    // otherwise the login dialog (BaseGameFragment.openProfilePage)
    val apiClient = RoboyardApiClient.getInstance(storage)
    val openProfilePage: () -> Unit = {
        if (apiClient.isLoggedIn) {
            openAutoLoginUrl(apiClient.buildAutoLoginUrl(apiClient.baseUrl + "/profile"))
        } else {
            showAuthDialog = true
        }
    }
    val profileInitial = remember(authRefresh) {
        if (apiClient.isLoggedIn) {
            (apiClient.userName ?: apiClient.userEmail)?.firstOrNull()?.uppercase()
        } else null
    }

    // Handle a deep link passed on the command line (Android intent parity)
    LaunchedEffect(pendingDeepLink) {
        val url = pendingDeepLink ?: return@LaunchedEffect
        when (val result = roboyard.logic.network.DeepLinkHandler.parse(url)) {
            is roboyard.logic.network.DeepLinkHandler.DeepLinkResult.Map -> {
                session.setGameState(result.gameState)
                isLevelGame = false
                isLoadedGame = false
                currentScreen = Screen.Game
            }
            is roboyard.logic.network.DeepLinkHandler.DeepLinkResult.Random -> {
                session.startNewGame()
                isLevelGame = false
                isLoadedGame = false
                currentScreen = Screen.Game
            }
            is roboyard.logic.network.DeepLinkHandler.DeepLinkResult.UnsupportedVersion -> {
                println("[DEEPLINK] Unsupported link version ${result.version} — staying on main menu")
            }
            else -> {
                println("[DEEPLINK] Ignoring deep link (result=$result)")
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.MainMenu -> MainMenuScreen(
                    session = session,
                    onNewRandomGame = {
                        // Android MainMenuFragment parity: record daily login,
                        // reset per-game achievement flags, resume autosave if
                        // present and settings still match, else start new game
                        roboyard.logic.achievements.StreakManager.getInstance(
                            storage,
                            roboyard.logic.achievements.AchievementManager.getInstance(storage)
                        ).recordDailyLogin()
                        roboyard.logic.achievements.AchievementManager.getInstance(storage)
                            .onNewGameStarted()
                        val autosaveFile = "${roboyard.logic.core.Constants.SAVE_DIRECTORY}/" +
                            "${roboyard.logic.core.Constants.SAVE_FILENAME_PREFIX}0" +
                            roboyard.logic.core.Constants.SAVE_FILENAME_EXTENSION
                        if (storage.fileExists(autosaveFile) && session.autosaveSettingsMatch()) {
                            session.loadGame(0)
                            if (session.currentState.value != null) {
                                isLevelGame = false
                                isLoadedGame = true
                                currentScreen = Screen.Game
                                return@MainMenuScreen
                            }
                        }
                        session.startNewGame()
                        isLevelGame = false
                        isLoadedGame = false
                        currentScreen = Screen.Game
                    },
                    onLevelSelection = {
                        // Android parity: daily login recorded on level game start
                        roboyard.logic.achievements.StreakManager.getInstance(
                            storage,
                            roboyard.logic.achievements.AchievementManager.getInstance(storage)
                        ).recordDailyLogin()
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
                        saveLoadMode = false // Main menu opens Load Game mode (Android parity)
                        currentScreen = Screen.SaveLoad
                    },
                    onAchievements = {
                        currentScreen = Screen.Achievements
                    },
                    onLevelEditor = {
                        currentScreen = Screen.LevelDesignEditor
                    },
                    onProfile = openProfilePage,
                    profileInitial = profileInitial
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
                            saveLoadMode = true // In-game save button opens Save mode (Android parity)
                            currentScreen = Screen.SaveLoad
                        },
                        onNextLevel = {
                            selectedLevelId++
                            session.startLevelGame(selectedLevelId)
                            isLevelGame = true
                            isLoadedGame = false
                        },
                        onProfile = openProfilePage
                    )
                }
                Screen.LevelSelection -> {
                    LevelSelectionScreen(
                        session = session,
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onLevelSelected = { levelId: Int ->
                            selectedLevelId = levelId
                            session.startLevelGame(levelId)
                            isLevelGame = true
                            isLoadedGame = false
                            currentScreen = Screen.Game
                        },
                        onProfile = openProfilePage,
                        profileInitial = profileInitial,
                        onLevelEditor = {
                            currentScreen = Screen.LevelDesignEditor
                        }
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        onBack = {
                            currentScreen = Screen.MainMenu
                        },
                        onFullscreenChanged = onFullscreenChanged,
                        onDebugSettings = {
                            currentScreen = Screen.DebugSettings
                        }
                    )
                }
                Screen.DebugSettings -> {
                    DebugSettingsScreen(
                        onBack = {
                            currentScreen = Screen.Settings
                        },
                        onOpenLevelEditor = {
                            currentScreen = Screen.LevelDesignEditor
                        },
                        onApplyDeepLinkState = { state ->
                            session.setGameState(state)
                            isLevelGame = false
                            isLoadedGame = false
                            currentScreen = Screen.Game
                        },
                        onStartRandomGame = {
                            session.startNewGame()
                            isLevelGame = false
                            isLoadedGame = false
                            currentScreen = Screen.Game
                        }
                    )
                }
                Screen.LevelDesignEditor -> {
                    LevelDesignEditorScreen(
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
                        session = session,
                        saveMode = saveLoadMode,
                        isLevelGame = isLevelGame,
                        onProfile = openProfilePage,
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
                        },
                        onProfile = openProfilePage,
                        profileInitial = profileInitial
                    )
                }
            }

            if (showAuthDialog) {
                LoginDialog(
                    storage = storage,
                    stringProvider = stringProvider,
                    syncManager = SyncManager.getExistingInstance(),
                    onDismiss = { showAuthDialog = false },
                    onLoginSuccess = { authRefresh++ }
                )
            }

            // Toast overlay for UiNotifier messages (Android Toast parity)
            val toastMessage by uiNotifier.messages.collectAsState()
            if (toastMessage != null) {
                LaunchedEffect(toastMessage) {
                    kotlinx.coroutines.delay(3500)
                    uiNotifier.dismiss()
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    androidx.compose.material3.Text(
                        text = toastMessage!!,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .padding(bottom = 48.dp)
                            .background(
                                androidx.compose.ui.graphics.Color(0xDD333333),
                                androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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
    DebugSettings,
    LevelDesignEditor,
    Help,
    Credits,
    SaveLoad,
    Achievements
}
