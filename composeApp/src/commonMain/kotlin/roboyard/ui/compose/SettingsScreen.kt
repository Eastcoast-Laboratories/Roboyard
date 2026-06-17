package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import roboyard.logic.core.Constants
import roboyard.logic.core.Preferences
import roboyard.logic.storage.getPlatformStorage

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val storage = remember { getPlatformStorage() }
    
    // Load current preferences
    var robotCount by remember { mutableIntStateOf(Preferences.robotCount) }
    var targetColors by remember { mutableIntStateOf(Preferences.targetColors) }
    var soundEnabled by remember { mutableStateOf(Preferences.soundEnabled) }
    var difficulty by remember { mutableIntStateOf(Preferences.difficulty) }
    var boardSizeWidth by remember { mutableIntStateOf(Preferences.boardSizeWidth) }
    var boardSizeHeight by remember { mutableIntStateOf(Preferences.boardSizeHeight) }
    var generateNewMap by remember { mutableStateOf(Preferences.generateNewMap) }
    var accessibilityMode by remember { mutableStateOf(Preferences.accessibilityMode) }
    var appLanguage by remember { mutableStateOf(Preferences.appLanguage ?: "en") }
    var talkbackLanguage by remember { mutableStateOf(Preferences.talkbackLanguage ?: "same") }
    var gameMode by remember { mutableIntStateOf(Preferences.gameMode) }
    var fullscreenEnabled by remember { mutableStateOf(Preferences.fullscreenEnabled) }
    var minSolutionMoves by remember { mutableIntStateOf(Preferences.minSolutionMoves) }
    var maxSolutionMoves by remember { mutableIntStateOf(Preferences.maxSolutionMoves) }
    var allowMulticolorTarget by remember { mutableStateOf(Preferences.allowMulticolorTarget) }
    var highContrastMode by remember { mutableStateOf(Preferences.highContrastMode) }
    var backgroundSoundVolume by remember { mutableIntStateOf(Preferences.backgroundSoundVolume) }
    var hintAutoMoveEnabled by remember { mutableStateOf(Preferences.hintAutoMoveEnabled) }
    var hintAutoMoveMode by remember { mutableIntStateOf(Preferences.hintAutoMoveMode) }
    var soundEffectsVolume by remember { mutableIntStateOf(Preferences.soundEffectsVolume) }
    
    // Valid board sizes - exactly as in the original game
    val validBoardSizes = listOf(
        intArrayOf(8, 7),
        intArrayOf(8, 8),
        intArrayOf(8, 12),
        intArrayOf(10, 8),
        intArrayOf(10, 10),
        intArrayOf(10, 12),
        intArrayOf(10, 14),
        intArrayOf(12, 12),
        intArrayOf(12, 14),
        intArrayOf(12, 16),
        intArrayOf(12, 18),
        intArrayOf(14, 14),
        intArrayOf(14, 16),
        intArrayOf(14, 18),
        intArrayOf(16, 14),
        intArrayOf(16, 16),
        intArrayOf(16, 18),
        intArrayOf(16, 20),
        intArrayOf(16, 22),
        intArrayOf(18, 18),
        intArrayOf(18, 20),
        intArrayOf(18, 22)
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )
        
        // Language Section
        SettingsSection(title = "Language") {
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text(appLanguage.uppercase())
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("en", "de", "fr", "es").forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.uppercase()) },
                            onClick = {
                                appLanguage = lang
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // Talkback Language Section (only shown when accessibility is enabled)
        if (accessibilityMode) {
            SettingsSection(title = "Talkback Language") {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expanded = true }) {
                        Text(talkbackLanguage.uppercase())
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("same", "en", "de", "fr", "es").forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.uppercase()) },
                                onClick = {
                                    talkbackLanguage = lang
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Board Size Section
        SettingsSection(title = "Board Size") {
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text("${boardSizeWidth}x${boardSizeHeight}")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    validBoardSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text("${size[0]}x${size[1]}") },
                            onClick = {
                                boardSizeWidth = size[0]
                                boardSizeHeight = size[1]
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // Difficulty Section
        SettingsSection(title = "Difficulty") {
            RadioGroup(
                options = listOf("Beginner", "Advanced", "Insane", "Impossible"),
                selectedOption = when (difficulty) {
                    Constants.DIFFICULTY_BEGINNER -> "Beginner"
                    Constants.DIFFICULTY_ADVANCED -> "Advanced"
                    Constants.DIFFICULTY_INSANE -> "Insane"
                    Constants.DIFFICULTY_IMPOSSIBLE -> "Impossible"
                    else -> "Beginner"
                },
                onOptionSelected = { option ->
                    val previousDifficulty = difficulty
                    difficulty = when (option) {
                        "Beginner" -> Constants.DIFFICULTY_BEGINNER
                        "Advanced" -> Constants.DIFFICULTY_ADVANCED
                        "Insane" -> Constants.DIFFICULTY_INSANE
                        "Impossible" -> Constants.DIFFICULTY_IMPOSSIBLE
                        else -> Constants.DIFFICULTY_BEGINNER
                    }
                    
                    // Adjust puzzle parameters based on difficulty
                    when (difficulty) {
                        Constants.DIFFICULTY_BEGINNER -> {
                            minSolutionMoves = 4
                            maxSolutionMoves = 6
                            allowMulticolorTarget = true
                            generateNewMap = true
                        }
                        Constants.DIFFICULTY_ADVANCED -> {
                            minSolutionMoves = 6
                            maxSolutionMoves = 10
                            allowMulticolorTarget = false
                            generateNewMap = true
                        }
                        Constants.DIFFICULTY_INSANE -> {
                            minSolutionMoves = 10
                            maxSolutionMoves = 99
                            allowMulticolorTarget = false
                            generateNewMap = false
                        }
                        Constants.DIFFICULTY_IMPOSSIBLE -> {
                            minSolutionMoves = 17
                            maxSolutionMoves = 99
                            allowMulticolorTarget = false
                            // Keep current generateNewMapEachTime setting for impossible difficulty
                        }
                    }
                    
                    // Adjust board size for beginner mode
                    if (difficulty == Constants.DIFFICULTY_BEGINNER && previousDifficulty != Constants.DIFFICULTY_BEGINNER) {
                        boardSizeWidth = 12
                        boardSizeHeight = 14
                    }
                }
            )
        }
        
        // Puzzle Parameters Section
        SettingsSection(title = "Puzzle Parameters") {
            // Min Solution Moves
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Min: $minSolutionMoves", color = Color.White)
                Row {
                    Button(onClick = { if (minSolutionMoves > 1) minSolutionMoves-- }) {
                        Text("-")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (minSolutionMoves < 20) minSolutionMoves++ }) {
                        Text("+")
                    }
                }
            }
            
            // Max Solution Moves
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Max: $maxSolutionMoves", color = Color.White)
                Row {
                    Button(onClick = { if (maxSolutionMoves > minSolutionMoves) maxSolutionMoves-- }) {
                        Text("-")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (maxSolutionMoves < 99) maxSolutionMoves++ }) {
                        Text("+")
                    }
                }
            }
            
            // Allow Multicolor Target
            RadioGroup(
                options = listOf("Yes", "No"),
                selectedOption = if (allowMulticolorTarget) "Yes" else "No",
                onOptionSelected = { option ->
                    allowMulticolorTarget = (option == "Yes")
                }
            )
        }
        
        // New Map Section
        SettingsSection(title = "Generate New Map") {
            RadioGroup(
                options = listOf("Yes", "No"),
                selectedOption = if (generateNewMap) "Yes" else "No",
                onOptionSelected = { option ->
                    generateNewMap = (option == "Yes")
                }
            )
        }
        
        // Hint Auto Move Section
        SettingsSection(title = "Hint Auto Move") {
            RadioGroup(
                options = listOf("Manual", "Full-Auto", "Semi-Auto"),
                selectedOption = when (hintAutoMoveMode) {
                    Preferences.HINT_AUTO_MOVE_MANUAL -> "Manual"
                    Preferences.HINT_AUTO_MOVE_FULL_AUTO -> "Full-Auto"
                    Preferences.HINT_AUTO_MOVE_SEMI_AUTO -> "Semi-Auto"
                    else -> "Manual"
                },
                onOptionSelected = { option ->
                    hintAutoMoveMode = when (option) {
                        "Manual" -> Preferences.HINT_AUTO_MOVE_MANUAL
                        "Full-Auto" -> Preferences.HINT_AUTO_MOVE_FULL_AUTO
                        "Semi-Auto" -> Preferences.HINT_AUTO_MOVE_SEMI_AUTO
                        else -> Preferences.HINT_AUTO_MOVE_MANUAL
                    }
                }
            )
        }
        
        // Game Mode Section
        SettingsSection(title = "Game Mode") {
            RadioGroup(
                options = listOf("Standard", "Multi-Target"),
                selectedOption = when (gameMode) {
                    Constants.GAME_MODE_STANDARD -> "Standard"
                    Constants.GAME_MODE_MULTI_TARGET -> "Multi-Target"
                    else -> "Standard"
                },
                onOptionSelected = { option ->
                    gameMode = when (option) {
                        "Standard" -> Constants.GAME_MODE_STANDARD
                        "Multi-Target" -> Constants.GAME_MODE_MULTI_TARGET
                        else -> Constants.GAME_MODE_STANDARD
                    }
                }
            )
        }
        
        // Target Colors Section (only shown in Multi-Target mode)
        if (gameMode == Constants.GAME_MODE_MULTI_TARGET) {
            SettingsSection(title = "Target Colors") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Colors: $targetColors", color = Color.White)
                    Row {
                        Button(onClick = { if (targetColors > 1) targetColors-- }) {
                            Text("-")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { if (targetColors < 5) targetColors++ }) {
                            Text("+")
                        }
                    }
                }
            }
        }
        
        // Robot Count Section
        SettingsSection(title = "Robot Count") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Robots: $robotCount", color = Color.White)
                Row {
                    Button(onClick = { if (robotCount > 1) robotCount-- }) {
                        Text("-")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (robotCount < 5) robotCount++ }) {
                        Text("+")
                    }
                }
            }
        }
        
        // Sound Section
        SettingsSection(title = "Sound") {
            RadioGroup(
                options = listOf("On", "Off"),
                selectedOption = if (soundEnabled) "On" else "Off",
                onOptionSelected = { option ->
                    soundEnabled = (option == "On")
                }
            )
        }
        
        // Accessibility Section
        SettingsSection(title = "Accessibility Mode") {
            RadioGroup(
                options = listOf("On", "Off"),
                selectedOption = if (accessibilityMode) "On" else "Off",
                onOptionSelected = { option ->
                    accessibilityMode = (option == "On")
                }
            )
        }
        
        // Fullscreen Section
        SettingsSection(title = "Fullscreen") {
            RadioGroup(
                options = listOf("On", "Off"),
                selectedOption = if (fullscreenEnabled) "On" else "Off",
                onOptionSelected = { option ->
                    fullscreenEnabled = (option == "On")
                }
            )
        }
        
        // High Contrast Mode Section
        SettingsSection(title = "High Contrast Mode") {
            RadioGroup(
                options = listOf("Yes", "No"),
                selectedOption = if (highContrastMode) "Yes" else "No",
                onOptionSelected = { option ->
                    highContrastMode = (option == "Yes")
                }
            )
        }
        
        // Background Sound Volume Section
        SettingsSection(title = "Background Sound Volume") {
            Slider(
                value = backgroundSoundVolume.toFloat(),
                onValueChange = { backgroundSoundVolume = it.toInt() },
                valueRange = 0f..10f,
                steps = 10
            )
            Text("Volume: $backgroundSoundVolume", color = Color.White)
        }
        
        // Sound Effects Volume Section
        SettingsSection(title = "Sound Effects Volume") {
            Slider(
                value = soundEffectsVolume.toFloat(),
                onValueChange = { soundEffectsVolume = it.toInt() },
                valueRange = 0f..100f,
                steps = 100
            )
            Text("Volume: $soundEffectsVolume", color = Color.White)
        }
        
        // Save Button
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                // Save all preferences
                storage?.putInt("robot_count", robotCount)
                storage?.putInt("target_colors", targetColors)
                storage?.putBoolean("sound_enabled", soundEnabled)
                storage?.putInt("difficulty", difficulty)
                storage?.putInt("boardSizeX", boardSizeWidth)
                storage?.putInt("boardSizeY", boardSizeHeight)
                storage?.putBoolean("generate_new_map", generateNewMap)
                storage?.putBoolean("accessibility_mode", accessibilityMode)
                storage?.putString("app_language", appLanguage)
                storage?.putString("talkback_language", talkbackLanguage)
                storage?.putInt("game_mode", gameMode)
                storage?.putBoolean("fullscreen_enabled", fullscreenEnabled)
                storage?.putInt("min_solution_moves", minSolutionMoves)
                storage?.putInt("max_solution_moves", maxSolutionMoves)
                storage?.putBoolean("allow_multicolor_target", allowMulticolorTarget)
                storage?.putBoolean("high_contrast_mode", highContrastMode)
                storage?.putInt("background_sound_volume", backgroundSoundVolume)
                storage?.putBoolean("hint_auto_move_enabled", hintAutoMoveEnabled)
                storage?.putInt("hint_auto_move_mode", hintAutoMoveMode)
                storage?.putInt("sound_effects_volume", soundEffectsVolume)
                
                // Reload preferences
                Preferences.initialize(storage, accessibilityMode)
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
        
        // Back Button
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun RadioGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == selectedOption),
                    onClick = { onOptionSelected(option) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option,
                    color = Color.White
                )
            }
        }
    }
}
