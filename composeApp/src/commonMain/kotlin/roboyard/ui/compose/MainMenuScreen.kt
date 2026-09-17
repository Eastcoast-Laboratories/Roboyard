package roboyard.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.achievements_tropy
import roboyard.composeapp.generated.resources.help
import roboyard.composeapp.generated.resources.ic_user_profile
import roboyard.composeapp.generated.resources.settings_grid
import roboyard.composeapp.generated.resources.title_bg_optimized
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider

@Composable
fun MainMenuScreen(
    session: roboyard.logic.managers.GameSession? = null,
    onNewRandomGame: () -> Unit = {},
    onLevelSelection: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHelp: () -> Unit = {},
    onCredits: () -> Unit = {},
    onSaveLoad: () -> Unit = {},
    onAchievements: () -> Unit = {},
    onLevelEditor: () -> Unit = {},
    onProfile: () -> Unit = {},
    profileInitial: String? = null
) {
    val stringProvider = getStringProvider()
    val storage = getPlatformStorage()
    var hasSavedGames by remember { mutableStateOf(false) }
    var popupQueue by remember { mutableStateOf<List<roboyard.logic.achievements.Achievement>>(emptyList()) }

    val achievementManager = remember {
        roboyard.logic.achievements.AchievementManager.getInstance(storage)
    }
    val streakManager = remember {
        roboyard.logic.achievements.StreakManager.getInstance(storage, achievementManager)
    }

    // Android onViewCreated: cancel solver + stop map regeneration on menu entry
    LaunchedEffect(Unit) {
        session?.cancelSolver()
        session?.stopRegeneration()
        hasSavedGames = storage.hasSavedGames()
    }

    // Android onResume: record daily login and show the streak popup once per day
    LaunchedEffect(Unit) {
        val update = streakManager.recordDailyLogin()
        if (streakManager.shouldShowStreakPopupToday()) {
            val days = update.streakDays.coerceAtLeast(streakManager.currentStreak)
            val headlineKey = if (days >= 31) "streak_popup_day_31_headline"
                else "streak_popup_day_${days}_headline"
            val messageKey = if (days == 1) "streak_popup_day_1_message"
                else "streak_popup_message"
            val streakAchievement = roboyard.logic.achievements.Achievement(
                STREAK_POPUP_ID,
                headlineKey,
                messageKey,
                roboyard.logic.achievements.AchievementCategory.PROGRESSION,
                "icon_46_flame"
            ).apply { setDescriptionFormatArgs(days) }
            popupQueue = popupQueue + streakAchievement
            streakManager.markStreakPopupShownToday()
        }
    }

    // Android onResume/onPause: show achievement popups while the menu is visible
    androidx.compose.runtime.DisposableEffect(Unit) {
        achievementManager.setUnlockListener(object :
            roboyard.logic.achievements.AchievementManager.AchievementUnlockListener {
            override fun onAchievementUnlocked(achievement: roboyard.logic.achievements.Achievement?) {
                if (achievement != null) popupQueue = popupQueue + achievement
            }
        })
        onDispose { achievementManager.setUnlockListener(null) }
    }

    // Android parity: while a streak popup is shown, Level/Load buttons are hidden
    val streakPopupVisible = popupQueue.any { it.id == STREAK_POPUP_ID }

    val barBrush = Brush.linearGradient(
        colors = listOf(Color(0xCC000000), Color(0xCC000000)),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image (same as main game)
        Image(
            painter = painterResource(Res.drawable.title_bg_optimized),
            contentDescription = "Background image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header bar with title and profile button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBrush)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (stringProvider.getString("main_menu_title") ?: "Roboyard").uppercase(),
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
                    // Profile button: user initial when logged in, else user icon (matches Android)
                    IconCircularButton(
                        text = profileInitial,
                        icon = if (profileInitial == null) Res.drawable.ic_user_profile else null,
                        color = CircularButtonColor.TURQUOISE,
                        contentDescription = profileInitial
                            ?: (stringProvider.getString("profile_a11y") ?: "User profile"),
                        onClick = onProfile,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
            }

            // Scrollable content with fancy buttons
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .desktopVerticalScroll()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Button container with 70% width
                Column(
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    FancyButton(
                        text = stringProvider.getString("new_random_game") ?: "New Random Game",
                        color = FancyButtonColor.GREEN,
                        onClick = onNewRandomGame,
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "newRandomGameButton" }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Hidden while a streak popup is visible (Android parity)
                    if (!streakPopupVisible) {
                        FancyButton(
                            text = stringProvider.getString("level_game") ?: "Level Game",
                            color = FancyButtonColor.BLUE,
                            onClick = onLevelSelection,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // Load Game button - hidden when no saved games (same as main game)
                    if (hasSavedGames && !streakPopupVisible) {
                        FancyButton(
                            text = stringProvider.getString("load_game") ?: "Load Game",
                            color = FancyButtonColor.RED,
                            onClick = onSaveLoad,
                            modifier = Modifier.fillMaxWidth().semantics { testTag = "loadGameButton" }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

        // Achievement / streak popups (Android AchievementPopup parity)
        AchievementUnlockPopup(
            achievements = popupQueue,
            onDismiss = { popupQueue = emptyList() }
        )
                    FancyButton(
                        text = stringProvider.getString("level_design_editor") ?: "Level Design Editor",
                        color = FancyButtonColor.PURPLE,
                        onClick = onLevelEditor,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Footer bar with icon buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Credits button - © symbol
                    CircularButton(
                        text = "©",
                        color = CircularButtonColor.YELLOW,
                        onClick = onCredits,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Help button
                    IconCircularButton(
                        icon = Res.drawable.help,
                        color = CircularButtonColor.ORANGE,
                        contentDescription = stringProvider.getString("help_a11y") ?: "How to Play",
                        onClick = onHelp,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Achievements button
                    IconCircularButton(
                        icon = Res.drawable.achievements_tropy,
                        color = CircularButtonColor.PURPLE,
                        contentDescription = stringProvider.getString("achievements_title") ?: "Achievements",
                        onClick = onAchievements,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Settings button
                    IconCircularButton(
                        icon = Res.drawable.settings_grid,
                        color = CircularButtonColor.GRAY,
                        contentDescription = stringProvider.getString("settings_a11y") ?: "Game settings",
                        onClick = onSettings,
                        modifier = Modifier.size(48.dp).padding(8.dp).semantics { testTag = "settingsButton" }
                    )
                }
            }
        }
    }
}
