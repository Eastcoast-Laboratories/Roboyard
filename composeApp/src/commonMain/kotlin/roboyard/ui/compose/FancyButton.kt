package roboyard.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.DrawableResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.ic_user_profile

/**
 * Color variants for the fancy gradient buttons, matching the original
 * button_fancy_*.xml ripple drawables from the fragment-app branch.
 */
enum class FancyButtonColor(
    val startColor: Color,
    val endColor: Color,
    val strokeColor: Color,
    val textColor: Color
) {
    RED(Color(0xFFFF6B6B), Color(0xFFE55555), Color(0xFFFF8A8A), Color.White),
    GREEN(Color(0xFF50C878), Color(0xFF3A9B5C), Color(0xFF66D68A), Color.White),
    BLUE(Color(0xFF4A90E2), Color(0xFF357ABD), Color(0xFF5BA3F5), Color.White),
    GRAY(Color(0xFF6C7B7F), Color(0xFF556065), Color(0xFF8A9499), Color.White),
    HINT(Color(0xFFFFE082), Color(0xFFFFCC02), Color(0xFFFFB300), Color(0xFF1A1A1A)),
    PURPLE(Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFFBA68C8), Color.White),
    YELLOW(Color(0xFFFFC107), Color(0xFFFFA000), Color(0xFFFFD54F), Color.Black)
}

/**
 * Color variants for circular buttons in header/footer, matching
 * circular_button_*.xml drawables from the fragment-app branch.
 */
enum class CircularButtonColor(
    val fillColor: Color,
    val strokeColor: Color,
    val textColor: Color
) {
    TURQUOISE(Color(0xFF00BCD4), Color.Black, Color.White),
    YELLOW(Color(0xFFFFC107), Color.Black, Color.Black),
    ORANGE(Color(0xFFFF9800), Color.Black, Color.White),
    PURPLE(Color(0xFF9C27B0), Color.Black, Color.White),
    RED(Color(0xFFF44336), Color.Black, Color.White),
    GRAY(Color(0xFF757575), Color.Black, Color.White)
}

/**
 * A fully-rounded gradient button matching the original fancy buttons.
 * Uses a 135-degree diagonal gradient, a 1dp border, and pill-shaped corners.
 */
@Composable
fun FancyButton(
    text: String,
    color: FancyButtonColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // 135-degree gradient: top-left (start) to bottom-right (end)
    val brush = Brush.linearGradient(
        colors = listOf(color.startColor, color.endColor),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )
    val shape = RoundedCornerShape(percent = 50)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(
                if (isPressed) {
                    Brush.linearGradient(
                        colors = listOf(color.startColor.copy(alpha = 0.8f), color.endColor.copy(alpha = 0.8f)),
                        start = Offset(0f, 0f),
                        end = Offset.Infinite
                    )
                } else {
                    brush
                },
                shape
            )
            .border(BorderStroke(1.dp, color.strokeColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 8.dp)
            .shadow(elevation = 6.dp, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color.textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A circular button with an optional icon image or text — matches Android's
 * icon buttons (help/achievements/settings/profile) in header and footer.
 */
@Composable
fun IconCircularButton(
    text: String? = null,
    icon: org.jetbrains.compose.resources.DrawableResource? = null,
    color: CircularButtonColor,
    contentDescription: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(color.fillColor, shape)
            .border(BorderStroke(2.dp, color.strokeColor), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                painter = org.jetbrains.compose.resources.painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp)
            )
        } else text?.let {
            Text(
                text = it,
                color = color.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
        }
    }
}

/**
 * Shared screen header matching the Android fragments: back button, centered title,
 * and a profile button on the right (initial when logged in, else user icon).
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    profileInitial: String? = null,
    onProfile: (() -> Unit)? = null,
    titleColor: Color = Color(0xFF333333)
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FancyButton(
            text = "◂ " + (roboyard.logic.ui.getStringProvider().getString("back_button") ?: "Back"),
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.height(40.dp)
        )
        Text(
            text = title,
            color = titleColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        if (onProfile != null) {
            IconCircularButton(
                text = profileInitial,
                icon = if (profileInitial == null) Res.drawable.ic_user_profile else null,
                color = CircularButtonColor.TURQUOISE,
                contentDescription = profileInitial
                    ?: (roboyard.logic.ui.getStringProvider().getString("profile_a11y") ?: "User profile"),
                onClick = onProfile,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * A circular button matching the original circular_button_*.xml drawables.
 * Used for header/footer icon buttons.
 */
@Composable
fun CircularButton(
    text: String? = null,
    color: CircularButtonColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(color.fillColor, shape)
            .border(BorderStroke(2.dp, color.strokeColor), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        text?.let {
            Text(
                text = it,
                color = color.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
        }
    }
}
