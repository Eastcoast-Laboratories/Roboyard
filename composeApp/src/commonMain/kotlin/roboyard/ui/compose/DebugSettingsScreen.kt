package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.StreakManager
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.core.Preferences
import roboyard.logic.core.ResourceLoader
import roboyard.logic.managers.GameHistoryManager
import roboyard.logic.managers.LevelCompletionManager
import roboyard.logic.network.DeepLinkHandler
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider

/**
 * Debug settings screen — port of Android DebugSettingsFragment.
 * Accessible by long-pressing (4 seconds) the settings title.
 */
@Composable
fun DebugSettingsScreen(
    onBack: () -> Unit = {},
    onOpenLevelEditor: () -> Unit = {},
    onApplyDeepLinkState: ((roboyard.logic.core.GameState) -> Unit)? = null,
    onStartRandomGame: (() -> Unit)? = null
) {
    val storage = remember { getPlatformStorage() }
    val stringProvider = remember { getStringProvider() }
    val achievementManager = remember { AchievementManager.getInstance(storage, stringProvider, null) }
    val streakManager = remember { StreakManager.getInstance(storage, achievementManager) }
    val levelCompletionManager = remember { LevelCompletionManager.getInstance(storage) }
    val scope = remember { CoroutineScope(Dispatchers.Default) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    fun toast(msg: String) { toastMessage = msg }

    var testModeOn by remember { mutableStateOf(streakManager.isTestMode()) }
    var streakRefresh by remember { mutableIntStateOf(0) }
    var hintMode by remember { mutableIntStateOf(Preferences.hintAutoMoveMode) }
    var altLayout by remember {
        mutableStateOf(storage.getBoolean(KEY_ALT_LAYOUT, false))
    }
    var starInput by remember { mutableStateOf("139") }
    var statsRefresh by remember { mutableIntStateOf(0) }
    var showAchievementSelector by remember { mutableStateOf(false) }
    var confirmResetAchievements by remember { mutableStateOf(false) }
    var confirmResetLevels by remember { mutableStateOf(false) }
    var confirmAddDummyHistory by remember { mutableStateOf(false) }
    var showLogViewer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a1a))
            .padding(16.dp)
    ) {
        Text(
            text = "DEBUG SETTINGS",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .desktopVerticalScroll()
        ) {
            // ---- STREAK TESTING ----
            DebugSectionTitle("STREAK TESTING")

            FancyButton(
                text = if (testModeOn) "TEST MODE: ON" else "TEST MODE: OFF",
                color = if (testModeOn) FancyButtonColor.GREEN else FancyButtonColor.GRAY,
                onClick = {
                    val newState = !streakManager.isTestMode()
                    streakManager.setTestMode(newState)
                    testModeOn = newState
                    toast("Test mode " + (if (newState) "ENABLED (1 day = 10s, streak reset)" else "DISABLED (streak reset)"))
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            Text(
                "Simulate daily logins (use in test mode):",
                color = Color(0xFFCCCCCC), fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(1, 3, 7, 30).forEach { days ->
                    FancyButton(
                        text = "${days}d",
                        color = FancyButtonColor.BLUE,
                        onClick = {
                            if (!streakManager.isTestMode()) {
                                toast("Enable test mode first!")
                            } else {
                                toast("Simulating $days daily logins (no achievements)... wait ${days * 10} seconds")
                                streakManager.recordDailyLogin()
                                scope.launch {
                                    repeat(days - 1) {
                                        delay(10_000)
                                        streakManager.recordDailyLogin()
                                        streakRefresh++
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                }
            }
            // streakRefresh read to recompose after simulated logins
            @Suppress("UNUSED_EXPRESSION") streakRefresh
            Text(
                "Current streak: ${streakManager.currentStreak} days",
                color = Color(0xFFFFFF00), fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Stored streak days: ${streakManager.storedStreakDays}",
                color = Color(0xFFCCCCCC), fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            // ---- ACHIEVEMENT MANAGEMENT ----
            DebugSectionTitle("ACHIEVEMENT MANAGEMENT")

            FancyButton(
                text = "Unlock All Achievements",
                color = FancyButtonColor.GREEN,
                onClick = {
                    achievementManager.unlockAll()
                    toast("All achievements unlocked")
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
            FancyButton(
                text = "Reset All Achievements",
                color = FancyButtonColor.RED,
                onClick = { confirmResetAchievements = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
            FancyButton(
                text = "Toggle Individual Achievement",
                color = FancyButtonColor.BLUE,
                onClick = { showAchievementSelector = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )

            // ---- LEVELS ----
            DebugSectionTitle("LEVELS")

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "Stars to unlock:",
                    color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                        .align(androidx.compose.ui.Alignment.CenterVertically)
                )
                TextField(
                    value = starInput,
                    onValueChange = { starInput = it.filter(Char::isDigit).take(3) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.4f)
                )
            }
            FancyButton(
                text = "Unlock Stars",
                color = FancyButtonColor.GREEN,
                onClick = {
                    val numStars = starInput.toIntOrNull()
                    if (numStars == null || numStars <= 0 || numStars > 140) {
                        toast("Please enter a number between 1 and 140")
                    } else {
                        levelCompletionManager.unlockStars(numStars)
                        toast("$numStars stars unlocked")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
            FancyButton(
                text = "Reset All Levels",
                color = FancyButtonColor.RED,
                onClick = { confirmResetLevels = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
            FancyButton(
                text = "Open Level Editor",
                color = FancyButtonColor.BLUE,
                onClick = onOpenLevelEditor,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )

            // ---- UI DESIGN ----
            DebugSectionTitle("UI DESIGN")

            FancyButton(
                text = "ALT LAYOUT: " + (if (altLayout) "ON" else "OFF"),
                color = if (altLayout) FancyButtonColor.GREEN else FancyButtonColor.GRAY,
                onClick = {
                    val newVal = !altLayout
                    storage.putBoolean(KEY_ALT_LAYOUT, newVal)
                    altLayout = newVal
                    toast("Alternative layout " + (if (newVal) "ENABLED" else "DISABLED") + " (restart game screen)")
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )

            // ---- HINT AUTO MOVE ----
            DebugSectionTitle("HINT AUTO MOVE")

            val modeNames = listOf("Manual", "Full-Auto", "Semi-Auto")
            Text(
                "Current mode: ${modeNames.getOrElse(hintMode) { "?" }}",
                color = Color(0xFFFFFF00), fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    Preferences.HINT_AUTO_MOVE_MANUAL,
                    Preferences.HINT_AUTO_MOVE_FULL_AUTO,
                    Preferences.HINT_AUTO_MOVE_SEMI_AUTO
                )
                modes.forEachIndexed { index, mode ->
                    FancyButton(
                        text = modeNames[index],
                        color = if (hintMode == mode) FancyButtonColor.GREEN else FancyButtonColor.GRAY,
                        onClick = {
                            Preferences.setHintAutoMoveMode(mode)
                            hintMode = mode
                            toast("Hint auto move mode set to: ${modeNames[index]}")
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                }
            }

            // ---- MEMORY STATISTICS ----
            DebugSectionTitle("MEMORY STATISTICS")

            // statsRefresh triggers recomposition when the refresh button is clicked
            @Suppress("UNUSED_EXPRESSION") statsRefresh
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2a2a2a))
                    .padding(8.dp)
            ) {
                val runtime = Runtime.getRuntime()
                val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                val maxMemory = runtime.maxMemory() / 1024 / 1024
                Text(
                    "Total Memory: $usedMemory MB / $maxMemory MB (%.1f%%)".format(usedMemory * 100.0 / maxMemory),
                    color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                val historyEntries = GameHistoryManager.getHistoryEntries(storage)
                var historyMemoryBytes = 0L
                for (entry in historyEntries) {
                    historyMemoryBytes += entry.getMapPath().length
                    entry.mapName?.let { historyMemoryBytes += it.length }
                    entry.boardSize?.let { historyMemoryBytes += it.length }
                    historyMemoryBytes += 200
                }
                val historyMemoryKB = historyMemoryBytes / 1024
                val historyMemoryMB = historyMemoryKB / 1024
                Text(
                    if (historyMemoryMB > 0)
                        "History Entries: ${historyEntries.size} ($historyMemoryMB MB)"
                    else
                        "History Entries: ${historyEntries.size} ($historyMemoryKB KB)",
                    color = Color(0xFFCCCCCC), fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
                Text(
                    "Achievements Unlocked: ${achievementManager.unlockedCount}",
                    color = Color(0xFFCCCCCC), fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
                Text(
                    "Level Stars: ${levelCompletionManager.totalStars}/139",
                    color = Color(0xFFCCCCCC), fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
                FancyButton(
                    text = "Refresh Statistics",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        statsRefresh++
                        toast("Statistics refreshed")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            // ---- HISTORY TESTING ----
            DebugSectionTitle("HISTORY TESTING")

            FancyButton(
                text = "Add $DUMMY_ENTRIES_COUNT Dummy History Entries",
                color = FancyButtonColor.BLUE,
                onClick = { confirmAddDummyHistory = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )

            // ---- SERVER ----
            DebugSectionTitle("SERVER")

            // Configurable API base URL (dev backend testing; default: production)
            var apiUrlInput by remember {
                mutableStateOf(
                    storage.getString(RoboyardApiClient.KEY_API_BASE_URL, null)
                        ?: RoboyardApiClient.DEFAULT_BASE_URL
                )
            }
            OutlinedTextField(
                value = apiUrlInput,
                onValueChange = { apiUrlInput = it },
                label = { Text("API Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                FancyButton(
                    text = "Save URL",
                    color = FancyButtonColor.GREEN,
                    onClick = {
                        storage.putString(RoboyardApiClient.KEY_API_BASE_URL, apiUrlInput.trim())
                        toast("API URL: ${apiUrlInput.trim()}")
                    },
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                FancyButton(
                    text = "Local",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        apiUrlInput = "http://127.0.0.1:8000"
                        storage.putString(RoboyardApiClient.KEY_API_BASE_URL, apiUrlInput)
                        toast("API URL: $apiUrlInput")
                    },
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                FancyButton(
                    text = "Prod",
                    color = FancyButtonColor.RED,
                    onClick = {
                        apiUrlInput = RoboyardApiClient.DEFAULT_BASE_URL
                        storage.remove(RoboyardApiClient.KEY_API_BASE_URL)
                        toast("API URL: $apiUrlInput")
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Deep link tester: paste a share URL to open the map (Android gets these via intents)
            var deepLinkInput by remember { mutableStateOf("") }
            OutlinedTextField(
                value = deepLinkInput,
                onValueChange = { deepLinkInput = it },
                label = { Text("Shared map link (roboyard:// or https URL)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
            FancyButton(
                text = "Open Link",
                color = FancyButtonColor.BLUE,
                onClick = {
                    when (val result = DeepLinkHandler.parse(deepLinkInput.trim())) {
                        is DeepLinkHandler.DeepLinkResult.Map -> {
                            if (onApplyDeepLinkState != null) {
                                onApplyDeepLinkState(result.gameState)
                            } else {
                                toast("Parsed map: ${result.gameState.levelName}")
                            }
                        }
                        is DeepLinkHandler.DeepLinkResult.Random -> {
                            if (onStartRandomGame != null) onStartRandomGame()
                            else toast("Random game link")
                        }
                        is DeepLinkHandler.DeepLinkResult.UnsupportedVersion ->
                            toast("Link requires newer app version (ver ${result.version})")
                        is DeepLinkHandler.DeepLinkResult.MapTooLarge ->
                            toast("Map too large")
                        is DeepLinkHandler.DeepLinkResult.Invalid ->
                            toast("Invalid link: no usable map data")
                        is DeepLinkHandler.DeepLinkResult.NotADeepLink ->
                            toast("Not a roboyard link")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )

            // ---- APP CONTROL ----
            DebugSectionTitle("APP CONTROL")

            // View logs — desktop equivalent of Android's logcat viewer (last 500 lines)
            FancyButton(
                text = "View Logs",
                color = FancyButtonColor.BLUE,
                onClick = { showLogViewer = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
            FancyButton(
                text = "Restart App",
                color = FancyButtonColor.RED,
                onClick = {
                    toast("Restarting app...")
                    // Desktop: relaunch the JVM process
                    scope.launch {
                        delay(500)
                        try {
                            val javaBin = System.getProperty("java.home") + "/bin/java"
                            val cmd = arrayListOf(
                                javaBin, "-cp", System.getProperty("java.class.path"),
                                "roboyard.MainKt"
                            )
                            ProcessBuilder(cmd).start()
                            kotlin.system.exitProcess(0)
                        } catch (e: Exception) {
                            println("[DEBUG] Restart failed: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }

        FancyButton(
            text = "◂ " + (stringProvider.getString("back_button") ?: "Back"),
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }

    // ---- Dialogs ----

    if (confirmResetAchievements) {
        DebugConfirmDialog(
            title = "Reset Achievements",
            message = "Are you sure you want to reset all achievements?",
            confirmLabel = "Reset",
            onConfirm = {
                achievementManager.resetAll()
                confirmResetAchievements = false
                toast("All achievements reset")
            },
            onDismiss = { confirmResetAchievements = false }
        )
    }

    if (confirmResetLevels) {
        DebugConfirmDialog(
            title = "Reset Levels",
            message = "Are you sure you want to reset all level progress?",
            confirmLabel = "Reset",
            onConfirm = {
                levelCompletionManager.resetAll()
                confirmResetLevels = false
                toast("All levels reset")
            },
            onDismiss = { confirmResetLevels = false }
        )
    }

    if (confirmAddDummyHistory) {
        DebugConfirmDialog(
            title = "Add Dummy History",
            message = "This will add $DUMMY_ENTRIES_COUNT test history entries using level maps. Continue?",
            confirmLabel = "Add",
            onConfirm = {
                confirmAddDummyHistory = false
                toast("Adding $DUMMY_ENTRIES_COUNT dummy entries...")
                scope.launch {
                    val added = addDummyHistoryEntries(storage, DUMMY_ENTRIES_COUNT)
                    toast("Added $added entries")
                }
            },
            onDismiss = { confirmAddDummyHistory = false }
        )
    }

    if (showAchievementSelector) {
        AlertDialog(
            onDismissRequest = { showAchievementSelector = false },
            title = { Text("Toggle Achievements") },
            text = {
                Column(modifier = Modifier.desktopVerticalScroll()) {
                    DEBUG_ACHIEVEMENT_IDS.forEach { achievementId ->
                        val unlocked = achievementManager.isUnlocked(achievementId)
                        TextButton(onClick = {
                            if (unlocked) {
                                achievementManager.lock(achievementId)
                                toast("$achievementId LOCKED")
                            } else {
                                achievementManager.unlock(achievementId)
                                toast("$achievementId UNLOCKED")
                            }
                        }) {
                            Text((if (unlocked) "✓ " else "○ ") + achievementId)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAchievementSelector = false }) { Text("Close") }
            }
        )
    }

    if (showLogViewer) {
        LogViewerDialog(onDismiss = { showLogViewer = false })
    }

    // Toast overlay
    toastMessage?.let { msg ->
        androidx.compose.runtime.LaunchedEffect(msg) {
            delay(2500)
            if (toastMessage == msg) toastMessage = null
        }
        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            Text(
                text = msg,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xDD333333), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private const val DUMMY_ENTRIES_COUNT = 20
private const val KEY_ALT_LAYOUT = "use_alternative_layout"

private val DEBUG_ACHIEVEMENT_IDS = listOf(
    "first_game", "level_1_complete", "level_10_complete",
    "3_star_hard_level", "3_star_10_levels",
    "speedrun_under_30s", "speedrun_random_5_games_under_30s",
    "daily_login_7", "daily_login_30", "comeback_player",
    "perfect_random_games_10", "no_hints_streak_random_10",
    "play_10_move_games_all_resolutions",
    "play_12_move_games_all_resolutions",
    "play_15_move_games_all_resolutions"
)

@Composable
private fun DebugSectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF00FF00),
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun DebugConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Adds dummy history entries based on bundled level maps.
 * Port of Android DebugSettingsFragment.addDummyHistoryEntries.
 * @return number of entries successfully added
 */
private fun addDummyHistoryEntries(
    storage: roboyard.logic.storage.PlatformStorage,
    count: Int
): Int {
    val existingEntries = GameHistoryManager.getHistoryEntries(storage)
    var nextTestNumber = 1
    for (entry in existingEntries) {
        val mapName = entry.mapName
        if (mapName != null && mapName.startsWith("Test")) {
            mapName.substring(4).toIntOrNull()?.let { num ->
                if (num >= nextTestNumber) nextTestNumber = num + 1
            }
        }
    }

    // Cycle through bundled level maps (1..140)
    var added = 0
    for (i in 0 until count) {
        val levelId = (i % 140) + 1
        val content = ResourceLoader.loadLevelContent(levelId) ?: continue
        val currentTestNumber = nextTestNumber + i
        val fileName = "test_${currentTestNumber}_level_${levelId}.txt"
        if (!storage.writeFile(fileName, content)) continue

        val entry = GameHistoryEntry()
        entry.setMapPath(storage.getFilePath(fileName))
        entry.mapName = "Test$currentTestNumber"
        entry.timestamp = System.currentTimeMillis() - (i * 60000L)
        entry.playDuration = (30..330).random()
        entry.movesMade = (10..60).random()
        entry.optimalMoves = (5..35).random()
        entry.boardSize = "12x12"
        entry.difficulty = i % 4
        entry.completionCount = if (i % 3 == 0) 1 else 0

        if (GameHistoryManager.addHistoryEntry(storage, entry)) added++
    }
    return added
}
