package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import roboyard.logic.achievements.Achievement
import roboyard.logic.ui.getStringProvider

/** Matches Android AchievementPopup.STREAK_POPUP_ID. */
const val STREAK_POPUP_ID = "daily_streak_popup"

/**
 * Achievement unlock popup — Compose equivalent of Android AchievementPopup.
 * Tap anywhere to dismiss. Also used for the daily streak popup (the streak
 * pseudo-achievement uses streak_popup_* string keys, like Android).
 */
@Composable
fun AchievementUnlockPopup(
    achievements: List<Achievement>,
    onDismiss: () -> Unit
) {
    if (achievements.isEmpty()) return
    val stringProvider = remember { getStringProvider() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .background(Color(0xFF2D2D2D), RoundedCornerShape(16.dp))
                .border(
                    androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)),
                    RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            achievements.forEach { a ->
                val isStreak = a.id == STREAK_POPUP_ID
                Text(
                    text = if (isStreak) stringProvider.getString("streak_popup_title")
                        ?: "Daily streak"
                    else stringProvider.getString("achievement_unlocked")
                        ?: "Achievement unlocked!",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = a.nameKey?.let { key ->
                        val args = a.descriptionFormatArgs
                        if (args != null)
                            stringProvider.getString(key, *args.map { it ?: "" }.toTypedArray()) ?: key
                        else stringProvider.getString(key) ?: key
                    } ?: a.id ?: "",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                a.descriptionKey?.let { descKey ->
                    val args = a.descriptionFormatArgs
                    Text(
                        text = if (args != null)
                            stringProvider.getString(descKey, *args.map { it ?: "" }.toTypedArray()) ?: ""
                        else stringProvider.getString(descKey) ?: "",
                        color = Color(0xFFBBBBBB),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
