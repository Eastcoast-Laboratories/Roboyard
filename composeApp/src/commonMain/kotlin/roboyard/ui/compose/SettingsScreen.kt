package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import roboyard.logic.core.Constants
import roboyard.logic.core.Preferences
import roboyard.logic.core.SettingsManager
import roboyard.logic.core.SettingsState
import roboyard.logic.ui.getStringProvider

private val DescriptionGray = Color(0xFFAAAAAA)
private val SecretGray = Color(0xFF333333)
private val DropdownBackground = Color(0xFF333333)

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onFullscreenChanged: (Boolean) -> Unit = {}
) {
    var settings by remember { mutableStateOf(SettingsManager.currentState()) }
    val stringProvider = remember { getStringProvider() }

    fun s(key: String, fallback: String): String = stringProvider.getString(key) ?: fallback

    LaunchedEffect(Unit) {
        println("[SETTINGS_SCREEN] Opened with state=$settings")
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .semantics { testTag = "settingsScreen" }
    ) {
        val isLandscape = maxWidth > maxHeight
        val displayRatio = if (maxWidth.value > 0f) maxHeight.value / maxWidth.value else 1.778f
        val boardSizes = remember(maxWidth, maxHeight) {
            SettingsManager.validBoardSizes(displayRatio, isLandscape)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Text(
                    text = s("settings_title", "Settings"),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                )

                SettingDropdownRow(
                    label = s("language_settings_label", "Language:"),
                    options = SettingsManager.LANGUAGES,
                    selectedLabel = SettingsManager.LANGUAGES
                        .firstOrNull { it.code == settings.appLanguage }?.displayName
                        ?: settings.appLanguage,
                    optionLabel = { it.displayName },
                    onSelect = { settings = SettingsManager.setAppLanguage(it.code) }
                )

                if (settings.accessibilityMode) {
                    SettingDropdownRow(
                        label = s("accessibility_language_label", "TalkBack Language:"),
                        options = SettingsManager.TALKBACK_LANGUAGES,
                        selectedLabel = talkbackLanguageLabel(settings.talkbackLanguage) { k, f -> s(k, f) },
                        optionLabel = { option ->
                            if (option.code == "same") {
                                s("language_same_as_app", option.displayName)
                            } else {
                                option.displayName
                            }
                        },
                        onSelect = { settings = SettingsManager.setTalkbackLanguage(it.code) }
                    )
                }

                SettingDropdownRow(
                    label = s("settings_board_size", "Board Size:"),
                    options = boardSizes,
                    selectedLabel = "${settings.boardSizeWidth}x${settings.boardSizeHeight}",
                    optionLabel = { it.toString() },
                    onSelect = { settings = SettingsManager.setBoardSize(it.width, it.height) }
                )

                SettingsLabel(s("settings_difficulty", "Difficulty Level:"))
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    DifficultyOption(Constants.DIFFICULTY_BEGINNER, s("difficulty_beginner", "Beginner"), settings) {
                        settings = SettingsManager.setDifficulty(it)
                    }
                    DifficultyOption(Constants.DIFFICULTY_ADVANCED, s("difficulty_advanced", "Advanced"), settings) {
                        settings = SettingsManager.setDifficulty(it)
                    }
                    DifficultyOption(Constants.DIFFICULTY_INSANE, s("difficulty_insane", "Insane"), settings) {
                        settings = SettingsManager.setDifficulty(it)
                    }
                    DifficultyOption(Constants.DIFFICULTY_IMPOSSIBLE, s("difficulty_impossible", "Impossible"), settings) {
                        settings = SettingsManager.setDifficulty(it)
                    }
                }

                SettingsLabel(s("settings_puzzle_parameters", "Num Moves:"))
                StepperSetting(
                    label = s("settings_min_solution_moves", "Min:"),
                    valueText = settings.minSolutionMoves.toString(),
                    onMinus = { settings = SettingsManager.setMinSolutionMoves(settings.minSolutionMoves - 1) },
                    onPlus = { settings = SettingsManager.setMinSolutionMoves(settings.minSolutionMoves + 1) }
                )
                StepperSetting(
                    label = s("settings_max_solution_moves", "Max:"),
                    valueText = if (settings.maxSolutionMoves >= 40) "∞" else settings.maxSolutionMoves.toString(),
                    onMinus = {
                        val newMax = if (settings.maxSolutionMoves >= 99) 39 else settings.maxSolutionMoves - 1
                        settings = SettingsManager.setMaxSolutionMoves(newMax)
                    },
                    onPlus = { settings = SettingsManager.setMaxSolutionMoves(settings.maxSolutionMoves + 1) }
                )

                Text(
                    text = s("settings_allow_multicolor_target", "Allow Multicolor Goal:"),
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                YesNoSetting(
                    selected = settings.allowMulticolorTarget,
                    yesText = s("settings_yes", "Yes"),
                    noText = s("settings_no", "No"),
                    onSelect = { settings = SettingsManager.setAllowMulticolorTarget(it) }
                )

                SettingsLabel(s("settings_new_map_each_time", "Generate New Map Each Time:"))
                YesNoSetting(
                    selected = settings.generateNewMapEachTime,
                    yesText = s("settings_yes", "Yes"),
                    noText = s("settings_no", "No"),
                    onSelect = { settings = SettingsManager.setGenerateNewMapEachTime(it) }
                )

                SettingsLabel(s("settings_hint_auto_move", "Hint Auto-Move:"))
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    RadioSetting(
                        text = s("settings_hint_auto_move_manual", "Manual (move robots yourself)"),
                        selected = settings.hintAutoMoveMode == Preferences.HINT_AUTO_MOVE_MANUAL,
                        onClick = { settings = SettingsManager.setHintAutoMoveMode(Preferences.HINT_AUTO_MOVE_MANUAL) }
                    )
                    if (settings.hintAutoMoveMode == Preferences.HINT_AUTO_MOVE_FULL_AUTO) {
                        RadioSetting(
                            text = s("settings_hint_auto_move_full_auto", "Full-Auto (robot moves automatically)"),
                            selected = true,
                            onClick = { settings = SettingsManager.setHintAutoMoveMode(Preferences.HINT_AUTO_MOVE_FULL_AUTO) }
                        )
                    }
                    RadioSetting(
                        text = s("settings_hint_auto_move_semi_auto", "Semi-Auto (move on next-hint button)"),
                        selected = settings.hintAutoMoveMode == Preferences.HINT_AUTO_MOVE_SEMI_AUTO,
                        onClick = { settings = SettingsManager.setHintAutoMoveMode(Preferences.HINT_AUTO_MOVE_SEMI_AUTO) }
                    )
                }

                SettingsLabel(s("settings_game_mode", "Game Mode:"))
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    RadioSetting(
                        text = s("settings_standard_game", "Standard Game"),
                        selected = settings.gameMode == Constants.GAME_MODE_STANDARD,
                        onClick = { settings = SettingsManager.setGameMode(Constants.GAME_MODE_STANDARD) }
                    )
                    RadioSetting(
                        text = s("settings_multi_target_mode", "Multi-Goal Mode (beta)"),
                        selected = settings.gameMode == Constants.GAME_MODE_MULTI_TARGET,
                        onClick = { settings = SettingsManager.setGameMode(Constants.GAME_MODE_MULTI_TARGET) }
                    )
                }

                if (settings.gameMode == Constants.GAME_MODE_MULTI_TARGET) {
                    SettingsLabel(s("settings_robots_reach_targets", "Goal Settings:"))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        DropdownSetting(
                            options = (1..Constants.NUM_ROBOTS).toList(),
                            selectedLabel = settings.robotCount.toString(),
                            optionLabel = { it.toString() },
                            onSelect = { settings = SettingsManager.setRobotCount(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = s("settings_out_of", "of"),
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        DropdownSetting(
                            options = (1..Constants.NUM_ROBOTS).toList(),
                            selectedLabel = settings.targetColors.toString(),
                            optionLabel = { it.toString() },
                            onSelect = { settings = SettingsManager.setTargetColors(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = s("settings_targets", "Goals"),
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                SettingsLabel(s("settings_sound", "Sound Effects:"))
                YesNoSetting(
                    selected = settings.soundEnabled,
                    yesText = s("settings_on", "On"),
                    noText = s("settings_off", "Off"),
                    onSelect = { settings = SettingsManager.setSoundEnabled(it) }
                )

                SettingsLabel(s("settings_background_sound", "Background Sound:"))
                Slider(
                    value = settings.backgroundSoundVolume.toFloat(),
                    onValueChange = { settings = SettingsManager.setBackgroundSoundVolume(it.roundToInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                SettingsLabel(s("settings_sound_effects_volume", "Sound Effects Volume:"))
                Slider(
                    value = settings.soundEffectsVolume.toFloat(),
                    onValueChange = { settings = SettingsManager.setSoundEffectsVolume(it.roundToInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                SettingsLabel(s("settings_fullscreen", "Fullscreen Mode:"))
                YesNoSetting(
                    selected = settings.fullscreenEnabled,
                    yesText = s("settings_yes", "Yes"),
                    noText = s("settings_no", "No"),
                    onSelect = {
                        settings = SettingsManager.setFullscreenEnabled(it)
                        onFullscreenChanged(it)
                    }
                )

                SettingsLabel(s("settings_high_contrast_mode", "High Contrast Mode:"))
                YesNoSetting(
                    selected = settings.highContrastMode,
                    yesText = s("settings_yes", "Yes"),
                    noText = s("settings_no", "No"),
                    onSelect = { settings = SettingsManager.setHighContrastMode(it) }
                )

                SettingsLabel(
                    text = s("settings_accessibility_mode", "Accessibility Mode:"),
                    color = SecretGray
                )
                Row(modifier = Modifier.background(Color.Black).padding(bottom = 24.dp)) {
                    RadioSetting(
                        text = s("settings_on", "On"),
                        selected = settings.accessibilityMode,
                        onClick = { settings = SettingsManager.setAccessibilityMode(true) },
                        textColor = SecretGray
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    RadioSetting(
                        text = s("settings_off", "Off"),
                        selected = !settings.accessibilityMode,
                        onClick = { settings = SettingsManager.setAccessibilityMode(false) },
                        textColor = SecretGray
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.semantics { testTag = "settingsBackButton" }
                ) {
                    Text(s("settings_back", "Back"))
                }
            }
        }
    }
}

private fun talkbackLanguageLabel(code: String, s: (String, String) -> String): String {
    val option = SettingsManager.TALKBACK_LANGUAGES.firstOrNull { it.code == code }
        ?: return s("language_same_as_app", "Same as app language")
    return if (option.code == "same") {
        s("language_same_as_app", option.displayName)
    } else {
        option.displayName
    }
}

@Composable
private fun SettingsLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Text(
        text = text,
        color = color,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun <T> SettingDropdownRow(
    label: String,
    options: List<T>,
    selectedLabel: String,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        DropdownSetting(
            options = options,
            selectedLabel = selectedLabel,
            optionLabel = optionLabel,
            onSelect = onSelect
        )
    }
}

@Composable
private fun <T> DropdownSetting(
    options: List<T>,
    selectedLabel: String,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(DropdownBackground, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = selectedLabel, color = Color.White, fontSize = 20.sp)
            Text(
                text = " ▼",
                color = DescriptionGray,
                fontSize = 16.sp
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DropdownBackground)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), color = Color.White, fontSize = 20.sp) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun RadioSetting(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    textColor: Color = Color.White
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = textColor,
                unselectedColor = textColor
            )
        )
        Text(text = text, color = textColor, fontSize = 20.sp)
    }
}

@Composable
private fun DifficultyOption(
    difficulty: Int,
    text: String,
    settings: SettingsState,
    onSelect: (Int) -> Unit
) {
    RadioSetting(
        text = text,
        selected = settings.difficulty == difficulty,
        onClick = { onSelect(difficulty) }
    )
}

@Composable
private fun YesNoSetting(
    selected: Boolean,
    yesText: String,
    noText: String,
    onSelect: (Boolean) -> Unit
) {
    Row(modifier = Modifier.padding(bottom = 24.dp)) {
        RadioSetting(
            text = yesText,
            selected = selected,
            onClick = { onSelect(true) }
        )
        Spacer(modifier = Modifier.width(32.dp))
        RadioSetting(
            text = noText,
            selected = !selected,
            onClick = { onSelect(false) }
        )
    }
}

@Composable
private fun StepperSetting(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        StepperButton("-", onMinus)
        Text(
            text = valueText,
            color = Color.White,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(60.dp)
                .padding(horizontal = 4.dp)
        )
        StepperButton("+", onPlus)
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(DropdownBackground, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
