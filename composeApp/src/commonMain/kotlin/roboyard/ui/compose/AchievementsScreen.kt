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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import roboyard.logic.achievements.Achievement
import roboyard.logic.achievements.AchievementCategory
import roboyard.logic.achievements.AchievementDefinitions
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.StreakManager
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider

/** Matches Android AchievementsFragment: 10-minute window for the >NEW< badge. */
private const val NEW_ACHIEVEMENT_THRESHOLD_MS = 10 * 60 * 1000L

@Composable
fun AchievementsScreen(
    onBack: () -> Unit = {},
    onProfile: (() -> Unit)? = null,
    profileInitial: String? = null
) {
    val storage = getPlatformStorage()
    val stringProvider = getStringProvider()
    val achievementManager = remember { AchievementManager.getInstance(storage, stringProvider, null) }
    val streakManager = remember { StreakManager.getInstance(storage, achievementManager) }

    val currentLoginStreakDays = streakManager.currentStreak
    val longestLoginStreakDays = streakManager.longestStreak
    val unlocked = achievementManager.unlockedCount
    val total = achievementManager.totalCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header bar with title, back and profile button (matches other screens)
        ScreenHeader(
            title = stringProvider.getString("achievements_title") ?: "Achievements",
            onBack = onBack,
            profileInitial = profileInitial,
            onProfile = onProfile
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Progress text: "unlocked / total"
            Text(
                text = stringProvider.getString(
                    "achievements_progress", unlocked, total
                ) ?: "$unlocked / $total",
                color = Color(0xFF1A1A1A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            var currentCategory: AchievementCategory? = null
            achievementManager.allAchievements.filterNotNull().forEach { achievement ->
                if (achievement.category != currentCategory) {
                    currentCategory = achievement.category
                    AchievementCategoryHeader(
                        category = currentCategory!!,
                        currentStreak = currentLoginStreakDays,
                        longestStreak = longestLoginStreakDays
                    )
                }
                AchievementItem(achievement, achievementManager)
            }
        }
    }
}

@Composable
private fun AchievementCategoryHeader(
    category: AchievementCategory,
    currentStreak: Int,
    longestStreak: Int
) {
    val stringProvider = getStringProvider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.getDisplayName { stringProvider.getString(it) },
            color = Color(0xFF00BCD4), // colorAccent
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal
        )
        // SPECIAL category shows streak info next to the header (matches Android)
        if (category == AchievementCategory.SPECIAL) {
            val streakLabel = if (currentStreak == 1) {
                stringProvider.getString("achievement_login_streak_day_label", currentStreak)
                    ?: "$currentStreak day"
            } else {
                stringProvider.getString("achievement_login_streak_days_label", currentStreak)
                    ?: "$currentStreak days"
            }
            val longestLabel = if (longestStreak == 1) {
                stringProvider.getString("achievement_login_streak_day_label", longestStreak)
                    ?: "$longestStreak day"
            } else {
                stringProvider.getString("achievement_login_streak_days_label", longestStreak)
                    ?: "$longestStreak days"
            }
            Text(
                text = streakLabel + " • " +
                    (stringProvider.getString("achievement_longest_streak_label", longestLabel)
                        ?: "Longest Streak: $longestLabel"),
                color = Color.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    achievementManager: AchievementManager
) {
    val stringProvider = getStringProvider()
    val isUnlocked = achievement.isUnlocked()
    val isNew = isUnlocked && achievement.unlockedTimestamp != 0L &&
        (driftingdroids.model.TimeProvider.currentTimeMillis() - achievement.unlockedTimestamp) <= NEW_ACHIEVEMENT_THRESHOLD_MS

    // Background: gold for new, green for unlocked, gray for locked (matches Android)
    val bgColor = when {
        isNew -> Color(0xFFFFF8E1)
        isUnlocked -> Color(0xFFE8F5E9)
        else -> Color(0xFFF5F5F5)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(bgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon in achievement-colored circle (matches Android AchievementIconHelper)
        val circleColor = Color(AchievementDefinitions.getAchievementColor(achievement.id ?: ""))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(circleColor)
                .alpha(if (isUnlocked) 1f else 0.3f),
            contentAlignment = Alignment.Center
        ) {
            val iconRes = achievementIconResource(achievement.iconDrawableName)
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = achievement.nameKey?.let { stringProvider.getString(it) } ?: achievement.id ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color(0xFF1B5E20) else Color(0xFF9E9E9E)
                )
                if (isNew) {
                    Text(
                        text = " >NEW<",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6F00)
                    )
                }
            }
            Text(
                text = achievement.descriptionKey?.let { stringProvider.getString(it) } ?: "",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }

        // Right-side indicator: checkmark if unlocked, progress counter otherwise
        if (isUnlocked) {
            Text(
                text = "✓",
                fontSize = 24.sp,
                color = Color(0xFF00BCD4)
            )
        } else {
            val progress = achievement.id?.let { achievementManager.getProgress(it) }
            if (progress != null && progress.hasProgress()) {
                val clamped = minOf(progress.current, progress.required)
                Text(
                    text = "$clamped/${progress.required}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}
